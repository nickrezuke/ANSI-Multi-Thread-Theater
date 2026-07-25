import java.util.Arrays;

public abstract class Loader implements Runnable {
    // These will track the current progress amount
    protected volatile boolean isRunning = true;
    protected volatile int progress = 0;

    protected int goalRate = 10000000; // 10 milliseconds default
    
    private final StatusStage[] stages;
    private final double[] zBuffer = new double[1760];
    private final String[] outputBuffer = new String[1760];

    // Reset ANSI codes
    protected static final String RESET = "\u001B[0m";
    protected static final String GREEN = "\u001B[32m";
    protected static final String WHITE = "\u001B[37m";

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
        // Run any geometry or color configurations once before starting
        initialize();

        // Clear console before starting the animation loop
        System.out.print("\u001b[2J");

        while (isRunning) {
            long startTime = System.nanoTime();

            // Clear buffers for the new frame
            Arrays.fill(outputBuffer, " ");
            Arrays.fill(zBuffer, 0); // Note: Change to double[] if your implementation strictly requires floating-point depth

            // DELEGATE: Let the class (Donut, Cube, etc.) draw onto the buffer
            renderGeometry(outputBuffer, zBuffer);

            // --- PRINT THE RENDERING BUFFER ---
            System.out.print("\u001b[H");
            for (int k = 0; k < 1760; k++) {
                System.out.print(k % 80 > 0 ? outputBuffer[k] : "\n");
            }

            // --- RESOLVE CURRENT STATUS MESSAGE ---
            int currentProgress = this.progress;
            String activeMessage = "Loading...";
            for (StatusStage stage : stages) {
                if (currentProgress <= stage.maxPercent) {
                    activeMessage = stage.message;
                    break;
                }
            }

            // --- BUILD DYNAMIC PROGRESS BAR ---
            int totalBars = 30;
            int filledBars = (int) ((currentProgress / 100.0) * totalBars);
            StringBuilder bar = new StringBuilder();
            for (int b = 0; b < totalBars; b++) {
                bar.append(b < filledBars ? '\u2665' : '\u2661');
            }

            // --- REUSABLE FORMATTED UI PRINT ---
            String formattedStatus = String.format(" %18s", activeMessage);
            System.out.print("\n\n" + WHITE + formattedStatus + "[" + GREEN + bar.toString() + WHITE + "] " + currentProgress + "%" + "\u001B[K" + RESET);

            // --- FRAME RATE REGULATOR ---
            try {
                int elapsedTime = (int) (System.nanoTime() - startTime);
                int sleepTime = goalRate - elapsedTime;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime / 1000000); // Convert from nanos to millis
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Clean up terminal on exit
        System.out.print("\n\u001b[2J\u001b[H");
        System.out.flush();
    }

    // Optional lifecycle hooks for setup
    protected abstract void initialize();

    // The essential contract: Concrete shapes modify the standard buffer sizes
    protected abstract void renderGeometry(String[] outputBuffer, double[] zBuffer); 
}
