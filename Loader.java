import java.util.Arrays;

public abstract class Loader implements Runnable {
    // These will track the current progress amount
    protected volatile boolean isRunning = true;
    protected volatile int progress = 0;

    protected static int INTENDED_FRAMERATE = 10000000; // 10 milliseconds default

    private final StatusStage[] stages;
    // Define the window
    // Our "window" dimensions are 80x22, so 22 * 80 = 1760
    private final double[] zBuffer = new double[1760];
    private final String[] outputBuffer = new String[1760];

    // Define some ASCII Codes:
    // Resets all colors, brightness, and text styling modifications back to default
    // terminal settings
    protected static final String RESET = "\u001B[0m";

    // Standard foreground text colors (affected by terminal profile contrast
    // levels)
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

    public Loader(StatusStage[] stages) {
        this.stages = stages;
    }

    public void stopLoading() {
        this.isRunning = false;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public void run() {
        // Run any geometry or color configurations once before starting (defined per class)
        initialize();

        // Wipe Screen and Hide the Cursor
        System.out.print(CLEAR_SCREEN + HIDE_CURSOR);
        System.out.flush();

        while (isRunning) {
            long startTime = System.nanoTime();

            // Clear buffers for the new frame
            Arrays.fill(outputBuffer, " ");
            Arrays.fill(zBuffer, 0);

            // Let the class draw onto the buffer
            renderGeometry(outputBuffer, zBuffer);

            // Use a single StringBuilder to build the screen that we print.
            StringBuilder frameBuilder = new StringBuilder();

            // Move cursor to home (top-left) without clearing
            frameBuilder.append(CURSOR_HOME);

            // Append geometry data
            for (int k = 0; k < 1760; k++) {
                if (k % 80 == 0 && k > 0) {
                    frameBuilder.append("\n");
                }
                frameBuilder.append(outputBuffer[k]);
            }

            // Resolve progress text
            int currentProgress = this.progress;
            String activeMessage = "Loading..."; // Default
            for (StatusStage stage : stages) {
                if (currentProgress <= stage.maxPercent) {
                    activeMessage = stage.message;
                    break;
                }
            }

            // Build progress bar indicators
            int totalBars = 30;
            int filledBars = (int) ((currentProgress / 100.0) * totalBars);
            StringBuilder bar = new StringBuilder();
            for (int b = 0; b < totalBars; b++) {
                bar.append(b < filledBars ? LOAD_BAR_FULL : LOAD_BAR_EMPTY);
            }

            // Format status line output
            String formattedStatus = String.format(" %18s", activeMessage);
            frameBuilder.append("\n\n")
                    .append(WHITE).append(formattedStatus)
                    .append("[").append(GREEN).append(bar).append(WHITE).append("] ")
                    .append(currentProgress).append("%")
                    .append(CLEAR_LINE) // Clear line to the right to handle text width shifts cleanly
                    .append(RESET);

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

        // Clean the screen and re-show the cursor
        System.out.print("\n" + CLEAR_SCREEN + CURSOR_HOME + SHOW_CURSOR);
        System.out.flush();
    }

    protected abstract void initialize();

    protected abstract void renderGeometry(String[] outputBuffer, double[] zBuffer);
}
