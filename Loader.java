public abstract class Loader implements Runnable {
    // These track the current progress amount
    protected volatile boolean isRunning = true;
    protected volatile int progress = 0;
    // Per-instance (NOT static/shared) target frame time. Was previously a shared
    // static, meaning any one Loader subclass adjusting it would silently change
    // the framerate of every other Loader instance running in the JVM at the time.
    protected long frameTimeNanos = 16_666_666L; // ~16.66 milliseconds (60 FPS)
    protected boolean isRawCanvas = false;
    private final java.util.concurrent.atomic.AtomicBoolean isCleanedUp = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final StatusStage[] stages;

    // Reference to whatever thread is currently executing run(), captured at the top of
    // run() itself. stopLoading()/forceTerminalCleanup() use this to interrupt a render
    // thread that's parked in the frame-pacing Thread.sleep() below, so shutdown doesn't
    // have to wait out the rest of the current frame period (which, for a slow-ticking
    // ambient loader, could be well over a second) before the loop notices isRunning
    // flipped to false.
    private volatile Thread renderThread;

    // Worst-case number of terminal rows the status footer (blank separator line +
    // message line + progress-bar line) can occupy below the rendered geometry. The
    // narrow-screen layout in appendStatusFooter() is the worst case at 3 rows; the
    // wide-screen layout only uses 2, so reserving 3 always leaves a single harmless
    // blank row in the wide case rather than ever overflowing the terminal.
    protected static final int FOOTER_RESERVED_ROWS = 3;
    
    // Define the window
    protected final int window_width;
    protected final int window_height;
    protected final int totalSize;
    private final double[] zBuffer;
    private final String[] outputBuffer;
    
    // Reusable buffers allocated ONCE in the constructor to ensure 0% GC pressure per frame
    private final StringBuilder frameBuilder;
    private final StringBuilder barBuilder;
    private final char[] messageBuffer;

    // Define some ASCII Codes:
    protected static final String RESET = "\u001B[0m";
    protected static final String GREEN = "\u001B[32m";
    protected static final String WHITE = "\u001B[37m";
    protected static final String CLEAR_SCREEN = "\u001b[2J";
    protected static final String CURSOR_HOME = "\u001b[H";
    protected static final String CLEAR_LINE = "\u001B[K";
    protected static final String HIDE_CURSOR = "\u001b[?25l";
    protected static final String SHOW_CURSOR = "\u001b[?25h";
    
    // Chars for the loading bar
    protected static final String LOAD_BAR_EMPTY = " ";
    protected static final String LOAD_BAR_FULL = "\u2588";

    // Eighth-resolution partial blocks, ordered from 1/8 filled to 7/8 filled, used
    // to
    // render the single "leading edge" cell of the bar so progress isn't rounded
    // down
    // to the nearest whole character. Index i holds the glyph for (i+1)/8 fill:
    // [0]=1/8 U+258F, [1]=2/8 U+258E, [2]=3/8 U+258D, [3]=4/8 U+258C,
    // [4]=5/8 U+258B, [5]=6/8 U+258A, [6]=7/8 U+2589
    // 0/8 falls back to LOAD_BAR_EMPTY and 8/8 rolls over into an extra
    // LOAD_BAR_FULL
    // cell instead (handled in appendProgressBar), so this array only needs 1..7.
    private static final String[] PARTIAL_BLOCKS = {
            "\u258F", "\u258E", "\u258D", "\u258C", "\u258B", "\u258A", "\u2589"
    };

    protected static class StatusStage {
        final int maxPercent;
        final String message;

        public StatusStage(int maxPercent, String message) {
            this.maxPercent = maxPercent;
            this.message = message;
        }
    }

    // Default constructor retains dynamic size matching for standard terminals.
    // Reserves FOOTER_RESERVED_ROWS off the real terminal height so
    // renderGeometry's
    // canvas and the status footer never fight over the same rows - the footer
    // always
    // gets its worst-case 3 rows, and geometry gets everything else. Loaders built
    // with
    // an explicit width/height (e.g. DonutLoader's hardcoded 80x22) skip this path
    // entirely and are unaffected; that height has always meant "pure geometry
    // rows,"
    // and still does.
    public Loader(StatusStage[] stages) {
        this(stages, TerminalConfig.getTerminalSize()[0], reservedTerminalHeight());
    }

    private static int reservedTerminalHeight() {
        int[] dimensions = TerminalConfig.getTerminalSize();
        return Math.max(1, dimensions[1] - FOOTER_RESERVED_ROWS);
    }

    // Overloaded constructor allowing custom dimensions
    public Loader(StatusStage[] stages, int width, int height) {
        this.stages = stages;
        this.window_width = width;
        this.window_height = height;
        this.totalSize = width * height;
        this.zBuffer = new double[totalSize];
        this.outputBuffer = new String[totalSize];

        // Allocate dynamic memory bounds ONCE during instantiation
        int estimatedFrameCapacity = totalSize + height + 512;
        this.frameBuilder = new StringBuilder(estimatedFrameCapacity);
        this.barBuilder = new StringBuilder(width);
        this.messageBuffer = new char[width];
    }

    public void stopLoading() {
        this.isRunning = false;
        // Wake the render thread immediately if it's currently parked in the
        // frame-pacing
        // sleep below, rather than making the caller's loadingThread.join() wait out
        // whatever's left of the current frame period.
        Thread t = this.renderThread;
        if (t != null) {
            t.interrupt();
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Lets a subclass tune its own pacing (e.g. a slower ambient scene, or a
    // faster-ticking game loop) without affecting any other Loader instance.
    protected void setTargetFps(int fps) {
        this.frameTimeNanos = 1_000_000_000L / fps;
    }

    protected void frameTimeNanos(int nanos) {
        this.frameTimeNanos = nanos;
    }

    @Override
    public void run() {
        this.renderThread = Thread.currentThread();
        try {
            // Ensure standard mode after previous runs
            if (!this.isRawCanvas) {
                TerminalConfig.restoreMode();
            }

            // Run subclass geometries
            initialize();

            // Clear Screen and Hide Cursor immediately before starting the render loop
            System.out.print(CLEAR_SCREEN + HIDE_CURSOR);
            System.out.flush();

            while (isRunning) {
                long startTime = System.nanoTime();

                // Clear primitive buffers without re-allocating arrays
                java.util.Arrays.fill(outputBuffer, " ");
                java.util.Arrays.fill(zBuffer, 0.0);

                // Draw frame geometry onto buffers
                renderGeometry(outputBuffer, zBuffer);

                // Reset frame string tracker capacity logical length to 0
                this.frameBuilder.setLength(0);
                this.frameBuilder.append(CURSOR_HOME);

                // Construct text frame grid mapping
                for (int k = 0; k < totalSize; k++) {
                    if (k % window_width == 0 && k > 0) {
                        this.frameBuilder.append(isRawCanvas ? "\r\n" : "\n");
                    }
                    this.frameBuilder.append(outputBuffer[k]);
                }

                // Build and append the progress bar + status message footer
                appendStatusFooter();

                // Push structural bytes cleanly into Standard Console Output
                System.out.print(this.frameBuilder);
                System.out.flush();

                // Frame rate regulation calculations
                long elapsedTime = System.nanoTime() - startTime;
                long sleepTimeNanos = this.frameTimeNanos - elapsedTime;

                if (sleepTimeNanos > 0) {
                    try {
                        long sleepMillis = sleepTimeNanos / 1000000;
                        int sleepNanos = (int) (sleepTimeNanos % 1000000);
                        Thread.sleep(sleepMillis, sleepNanos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            // Fires unconditionally on loop drop out or unhandled child exceptions
            forceTerminalCleanup();
        }
    }

    /**
     * Fills {@code builder} with a {@code barWidth}-cell progress bar representing
     * {@code progressPercent} (0-100) at eighth-block resolution rather than
     * rounding
     * down to the nearest whole cell. The bar is composed of:
     * - some number of fully-filled cells (LOAD_BAR_FULL),
     * - at most one partial "leading edge" cell chosen from PARTIAL_BLOCKS to show
     * the fractional remainder (skipped entirely if the remainder rounds to 0/8,
     * and rolled over into an extra full cell if it rounds up to 8/8),
     * - empty cells (LOAD_BAR_EMPTY) for the rest.
     * This mirrors how the bar already looked, just with finer granularity, so
     * callers
     * don't need to change anything about how they consume {@code builder}.
     */
    private void appendProgressBar(StringBuilder builder, int barWidth, int progressPercent) {
        if (barWidth <= 0) {
            return;
        }

        double exactFilled = (progressPercent / 100.0) * barWidth;
        int fullBlocks = (int) exactFilled;
        double remainder = exactFilled - fullBlocks;

        // Round the fractional remainder to the nearest eighth. Rounding up to a full
        // 8/8 means the partial cell is actually a whole block, so fold it into
        // fullBlocks instead of emitting a "partial" glyph for 100% of a cell.
        int eighths = (int) Math.round(remainder * 8);
        if (eighths >= 8) {
            fullBlocks++;
            eighths = 0;
        }

        // Clamp defensively (e.g. progressPercent > 100 from an over-eager caller)
        // so we never write more cells than barWidth or index past PARTIAL_BLOCKS.
        if (fullBlocks >= barWidth) {
            fullBlocks = barWidth;
            eighths = 0;
        }

        // Begin Underline
        builder.append("\033[4m");

        for (int b = 0; b < fullBlocks; b++) {
            builder.append(LOAD_BAR_FULL);
        }

        int cellsUsed = fullBlocks;
        if (eighths > 0) {
            builder.append(PARTIAL_BLOCKS[eighths - 1]);
            cellsUsed++;
        }

        for (int b = cellsUsed; b < barWidth; b++) {
            builder.append(LOAD_BAR_EMPTY);
        }

        // End Underline
        builder.append("\033[0m");
    }

    /**
     * Builds and appends the progress bar + status message footer onto
     * {@link #frameBuilder},
     * choosing a narrow- or wide-screen layout based on the terminal's current
     * dimensions.
     * Pulled out of run() so the footer's spacing/centering/layout can be iterated
     * on
     * independently of the frame-loop and geometry-compositing logic above it.
     */
    private void appendStatusFooter() {
        // Process progress staging boundaries
        int currentProgress = this.progress;
        String activeMessage = "Loading...";
        for (StatusStage stage : stages) {
            if (currentProgress <= stage.maxPercent) {
                activeMessage = stage.message;
                break;
            }
        }

        this.barBuilder.setLength(0);

        if (window_width * 2 < window_height || window_width <= 60) {
            // --- NARROW SCREEN TRACKING DISPLAY ---
            String cleanMsg = activeMessage.trim();
            int msgLen = cleanMsg.length();
            int maxMsgLen = Math.max(0, window_width - 3);

            if (msgLen > window_width) {
                cleanMsg.getChars(0, maxMsgLen, this.messageBuffer, 0);
                this.frameBuilder.append(isRawCanvas ? "\r\n\r\n" : "\n\n")
                        .append(WHITE).append(this.messageBuffer, 0, maxMsgLen).append("...")
                        .append(CLEAR_LINE).append(isRawCanvas ? "\r\n" : "\n");
            } else {
                this.frameBuilder.append(isRawCanvas ? "\r\n\r\n" : "\n\n")
                        .append(WHITE).append(cleanMsg)
                        .append(CLEAR_LINE).append(isRawCanvas ? "\r\n" : "\n");
            }

            int barWidth = window_width - 7;
            if (barWidth < 0)
                barWidth = 0;
            appendProgressBar(this.barBuilder, barWidth, currentProgress);

            this.frameBuilder.append(WHITE).append("[").append(GREEN).append(this.barBuilder).append(WHITE).append("] ")
                    .append(currentProgress).append("%")
                    .append(CLEAR_LINE)
                    .append(RESET);
        } else {
            // --- WIDE SCREEN TRACKING DISPLAY ---
            int totalBars = 30;
            appendProgressBar(this.barBuilder, totalBars, currentProgress);

            // Manual text padding algorithm drops String.format overhead entirely
            this.frameBuilder.append(isRawCanvas ? "\r\n\r\n" : "\n\n").append(WHITE).append(" ");
            int paddingNeeded = 18 - activeMessage.length();
            for (int i = 0; i < paddingNeeded; i++) {
                this.frameBuilder.append(' ');
            }

            this.frameBuilder.append(activeMessage)
                    .append("[").append(GREEN).append(this.barBuilder).append(WHITE).append("] ")
                    .append(currentProgress).append("%")
                    .append(CLEAR_LINE)
                    .append(RESET);
        }
    }

    protected abstract void initialize();

    protected abstract void renderGeometry(String[] outputBuffer, double[] zBuffer);

    /**
     * Shuts down execution canvases and resets terminal properties back to normal.
     * Includes a bulletproof native platform fallback to guarantee text alignment.
     */
    public final void forceTerminalCleanup() {
        // If it has already been cleaned up by a thread, do nothing
        if (!isCleanedUp.compareAndSet(false, true)) {
            return;
        }

        this.isRunning = false;
        Thread t = this.renderThread;
        if (t != null) {
            t.interrupt();
        }

        // Explicitly force the OS terminal out of raw mode right now
        TerminalConfig.restoreMode();

        // Wipe the canvas screen, reset coordinates,
        // and reveal the blinking cursor
        System.out.print("\n" + CLEAR_SCREEN + CURSOR_HOME + SHOW_CURSOR);
        System.out.flush();
    }

}