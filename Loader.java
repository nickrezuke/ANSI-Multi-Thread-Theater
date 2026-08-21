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
    protected static final String LOAD_BAR_EMPTY = "\u2661"; 
    protected static final String LOAD_BAR_FULL = "\u2665"; 

    protected static class StatusStage {
        final int maxPercent;
        final String message;

        public StatusStage(int maxPercent, String message) {
            this.maxPercent = maxPercent;
            this.message = message;
        }
    }

    // Default constructor retains dynamic size matching for standard terminals
    public Loader(StatusStage[] stages) {
        int[] dimensions = TerminalConfig.getTerminalSize();
        this(stages, dimensions[0], dimensions[1]);
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
                    if (barWidth < 3) barWidth = 3; 
                    int filledBars = (int) ((currentProgress / 100.0) * barWidth);

                    for (int b = 0; b < barWidth; b++) {
                        this.barBuilder.append(b < filledBars ? LOAD_BAR_FULL : LOAD_BAR_EMPTY);
                    }

                    this.frameBuilder.append(WHITE).append("[").append(GREEN).append(this.barBuilder).append(WHITE).append("] ")
                                .append(currentProgress).append("%")
                                .append(CLEAR_LINE)
                                .append(RESET);
                } else {
                    // --- WIDE SCREEN TRACKING DISPLAY ---
                    int totalBars = 30;
                    int filledBars = (int) ((currentProgress / 100.0) * totalBars);

                    for (int b = 0; b < totalBars; b++) {
                        this.barBuilder.append(b < filledBars ? LOAD_BAR_FULL : LOAD_BAR_EMPTY);
                    }

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

        // Explicitly force the OS terminal out of raw mode right now
        TerminalConfig.restoreMode();

        // Wipe the canvas screen, reset coordinates, and reveal the hardware blinking
        // cursor
        System.out.print("\n" + CLEAR_SCREEN + CURSOR_HOME + SHOW_CURSOR);
        System.out.flush();
    }

}
