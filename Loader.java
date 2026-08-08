public abstract class Loader implements Runnable {
    // These will track the current progress amount
    protected volatile boolean isRunning = true;
    protected volatile int progress = 0;

    protected static int INTENDED_FRAMERATE = 10000000; // 10 milliseconds default

    protected boolean isRawCanvas = false;

    private final java.util.concurrent.atomic.AtomicBoolean isCleanedUp = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    private final StatusStage[] stages;
    // Define the window
    protected final int window_width;
    protected final int window_height;
    protected final int totalSize;

    private final double[] zBuffer;
    private final String[] outputBuffer;

    // Define some ASCII Codes:
    // Resets all colors, brightness, and text styling modifications
    // back to default terminal settings
    protected static final String RESET = "\u001B[0m";

    // Standard foreground text colors (affected by terminal
    // profile contrast levels)
    // Green progress text/success theme
    protected static final String GREEN = "\u001B[32m";
    // White progress bar text block / light gray text
    protected static final String WHITE = "\u001B[37m";

    // Canvas management & double-buffering performance commands
    // Wipes every character block across the active buffer space
    protected static final String CLEAR_SCREEN = "\u001b[2J";
    // Moves text pointer directly back to coordinates (Row 1, Column 1)
    protected static final String CURSOR_HOME = "\u001b[H";
    // Erases text from current position straight to the right margin line edge
    protected static final String CLEAR_LINE = "\u001B[K";
    // Suppresses hardware caret completely to stop frame rendering stuttering
    protected static final String HIDE_CURSOR = "\u001b[?25l";
    // Safely recovers and brings blinking text terminal inputs back into focus
    protected static final String SHOW_CURSOR = "\u001b[?25h";

    // Chars for the loading bar
    protected static final String LOAD_BAR_EMPTY = "\u2661"; // Empty Space
    protected static final String LOAD_BAR_FULL = "\u2665"; // Complete Space

    protected static class StatusStage {
        final int maxPercent;
        final String message;

        public StatusStage(int maxPercent, String message) {
            this.maxPercent = maxPercent;
            this.message = message;
        }
    }

    // Default constructor retains original 80x22 layout for your other loaders
    public Loader(StatusStage[] stages) {
        this(stages, 80, 22);
    }

    // Overloaded constructor allowing custom dimensions
    public Loader(StatusStage[] stages, int width, int height) {
        this.stages = stages;
        this.window_width = width;
        this.window_height = height;
        this.totalSize = width * height;
        this.zBuffer = new double[totalSize];
        this.outputBuffer = new String[totalSize];
    }

    public void stopLoading() {
        this.isRunning = false;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public void run() {
        // Everything is wrapped in try/finally so that ANY exit path -
        // normal completion (isRunning flipped false by stopLoading()), or an
        // uncaught exception thrown by a specific loader's initialize() or
        // renderGeometry() implementation - still restores the terminal.
        // forceTerminalCleanup() is idempotent (guarded by isCleanedUp), so
        // this is safe even if a Ctrl-C shutdown hook concurrently calls it
        // too.
        try {
            // Ensure standard mode after previous runs
            if (!this.isRawCanvas) {
                TerminalConfig.restoreMode();
            }

            // Run any geometry or color configurations once
            // before starting (defined per class)
            initialize();

            // Wipe Screen and Hide the Cursor
            System.out.print(CLEAR_SCREEN + HIDE_CURSOR);
            System.out.flush();

            while (isRunning) {
                long startTime = System.nanoTime();

                // Clear buffers for the new frame
                for (int i = 0; i < outputBuffer.length; i++) {
                    outputBuffer[i] = " ";
                }
                for (int i = 0; i < zBuffer.length; i++) {
                    zBuffer[i] = 0;
                }

                // Let the class draw onto the buffer
                renderGeometry(outputBuffer, zBuffer);

                // Use a single StringBuilder to build the screen that we print.
                StringBuilder frameBuilder = new StringBuilder();

                // Move cursor to home (top-left) without clearing
                frameBuilder.append(CURSOR_HOME);

                // Append geometry data
                for (int k = 0; k < totalSize; k++) {
                    if (k % window_width == 0 && k > 0) {
                        frameBuilder.append(isRawCanvas ? "\r\n" : "\n");
                    }
                    frameBuilder.append(outputBuffer[k]);
                }

                // Get the specific in-progress text
                int currentProgress = this.progress;
                String activeMessage = "Loading..."; // Default
                for (StatusStage stage : stages) {
                    if (currentProgress <= stage.maxPercent) {
                        activeMessage = stage.message;
                        break;
                    }
                }

                if (window_width * 2 < window_height || window_width <= 60) {
                    // Truncate message to avoid spilling over narrow canvas borders
                    String cleanMsg = activeMessage.trim();
                    if (cleanMsg.length() > window_width) {
                        cleanMsg = cleanMsg.substring(0, Math.max(0, window_width - 3)) + "...";
                    }

                    // Dynamically calculate loading bar width relative to layout size constraints
                    int barWidth = window_width - 7;
                    if (barWidth < 3)
                        barWidth = 3; // Enforce safe minimum footprint layout

                    int filledBars = (int) ((currentProgress / 100.0) * barWidth);
                    StringBuilder bar = new StringBuilder();
                    for (int b = 0; b < barWidth; b++) {
                        bar.append(b < filledBars ? LOAD_BAR_FULL : LOAD_BAR_EMPTY);
                    }

                    // Stack elements vertically on separate rows below the rendered geometry
                    frameBuilder.append(isRawCanvas ? "\r\n\r\n" : "\n\n")
                            .append(WHITE).append(cleanMsg).append(CLEAR_LINE).append(isRawCanvas ? "\r\n" : "\n")
                            .append(WHITE).append("[").append(GREEN).append(bar).append(WHITE).append("] ")
                            .append(currentProgress).append("%")
                            .append(CLEAR_LINE)
                            .append(RESET);
                } else {
                    // Build progress bar indicators
                    int totalBars = 30;
                    int filledBars = (int) ((currentProgress / 100.0) * totalBars);
                    StringBuilder bar = new StringBuilder();
                    for (int b = 0; b < totalBars; b++) {
                        bar.append(b < filledBars ? LOAD_BAR_FULL : LOAD_BAR_EMPTY);
                    }

                    // Format status line output
                    String formattedStatus = String.format(" %18s", activeMessage);
                    frameBuilder.append(isRawCanvas ? "\r\n\r\n" : "\n\n")
                            .append(WHITE).append(formattedStatus)
                            .append("[").append(GREEN).append(bar).append(WHITE).append("] ")
                            .append(currentProgress).append("%")
                            .append(CLEAR_LINE)
                            .append(RESET);
                }

                // Push the entire frame to standard output in a single print statement
                System.out.print(frameBuilder.toString());
                System.out.flush();

                // Frame rate regulation
                try {
                    // Now that its all printed, lets leave it on screen for a while...
                    int elapsedTime = (int) (System.nanoTime() - startTime);
                    int sleepTime = INTENDED_FRAMERATE - elapsedTime;
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime / 1000000); // Unit conversion millis to nanos
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            // Runs on normal completion, on break-out above, AND on any
            // uncaught exception from initialize()/renderGeometry(). Restores
            // terminal mode (if raw), wipes the screen, and shows the cursor
            // again. Safe to call unconditionally - it's a no-op if a Ctrl-C
            // shutdown hook already ran it first.
            forceTerminalCleanup();
        }
    }

    // Defined per Loader
    protected abstract void initialize();

    // Defined per Loader
    protected abstract void renderGeometry(String[] outputBuffer, double[] zBuffer);

    /**
     * Forcibly shuts down the active execution canvas and ensures the native
     * operating system terminal characteristics are cleanly restored.
     * For example, what if we hit Ctrl-C - or a specific loader's
     * initialize()/renderGeometry() throws an unexpected runtime exception.
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