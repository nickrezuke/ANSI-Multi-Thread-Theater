// TODO: Fix This lmao totally broken....

import java.util.Random;

public class IkedaRibbonLoader extends Loader {
    private static final StatusStage[] IKEDA_STAGES = {
        new StatusStage(25, "Tuning ring cavity mirrors:"),
        new StatusStage(50, "Simulating non-linear refraction:"),
        new StatusStage(75, "Tracing chaotic laser ribbons:"),
        new StatusStage(100, "Ikeda Laser Core Operational!")
    };

    private final int width = 80;
    private final int height = 22;
    private final int totalSize = 1760;
    private final double[] ribbonDensity;
    private final Random rand = new Random();
    
    // Persistent particles maintain flowing motion paths over time
    private static final int TRAJECTORY_COUNT = 64;
    private final double[] particleX = new double[TRAJECTORY_COUNT];
    private final double[] particleY = new double[TRAJECTORY_COUNT];
    
    private double timeClock = 0.0;

    public IkedaRibbonLoader() {
        super(IKEDA_STAGES, 80, 22);
        this.ribbonDensity = new double[totalSize];
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        for (int i = 0; i < totalSize; i++) {
            ribbonDensity[i] = 0.0;
        }
        // Initialize trajectories persistently once
        for (int i = 0; i < TRAJECTORY_COUNT; i++) {
            particleX[i] = -0.5 + (rand.nextDouble() * 2.0);
            particleY[i] = -1.5 + (rand.nextDouble() * 2.0);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.025; // Speed up system clock

        // Create an organic trailing fade effect by bleeding out previous frames
        for (int i = 0; i < totalSize; i++) {
            ribbonDensity[i] *= 0.65; 
        }

        double B = 0.9;
        double C = 6.0;
        // Dynamically swing R over time to create a morphing laser ribbon
        double R = 0.8 + 0.3 * Math.sin(timeClock * 0.5);

        // Step and update persistent particles across frames
        for (int strand = 0; strand < TRAJECTORY_COUNT; strand++) {
            double px = particleX[strand];
            double py = particleY[strand];

            // Trace shorter paths per frame to make movement tracks readable
            for (int step = 0; step < 15; step++) {
                double currentRadiusSq = px * px + py * py;
                double t = R - (C / (1.0 + currentRadiusSq));
                double cosT = Math.cos(t);
                double sinT = Math.sin(t);

                double nextX = 1.0 + B * (px * cosT - py * sinT);
                double nextY = B * (px * sinT + py * cosT);
                
                px = nextX;
                py = nextY;

                // Center laser ribbons properly inside screen spaces
                int screenX = (int) (28.0 + (px * 18.0));
                int screenY = (int) (11.0 - (py * 5.5));

                if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) {
                    int idx = screenX + width * screenY;
                    ribbonDensity[idx] += 0.25; // Accumulate structural thickness
                }
            }
            
            // Save final orbital step position back into history
            particleX[strand] = px;
            particleY[strand] = py;

            // Periodically respawn stuck particles to maintain full attractor volume
            if (rand.nextDouble() < 0.02 || Math.abs(px) > 10 || Math.abs(py) > 10) {
                particleX[strand] = -0.5 + (rand.nextDouble() * 2.0);
                particleY[strand] = -1.5 + (rand.nextDouble() * 2.0);
            }
        }

        // --- RASTERIZATION PASS ---
        String palette = " .:-=+*#%@█";
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int idx = x + rowOffset;
                double d = ribbonDensity[idx];

                if (d > 0.02) {
                    if (d > 1.0) d = 1.0;
                    
                    int shadeIdx = (int) (d * (palette.length() - 1));
                    char renderChar = palette.charAt(shadeIdx);

                    // Dynamic wave patterns across the color channels
                    double colorOffset = timeClock * 3.0 + (x * 0.05) - (y * 0.08);
                    int r = (int) (140 + 115 * Math.sin(colorOffset));
                    int g = (int) (100 + 155 * Math.sin(colorOffset + 2.0944));
                    int b = (int) (180 + 75 * Math.cos(colorOffset * 0.5));

                    String laserColor = String.format("\u001B[38;2;%d;%d;%dm", 
                        Math.max(0, Math.min(255, r)), 
                        Math.max(0, Math.min(255, g)), 
                        Math.max(0, Math.min(255, b))
                    );
                    
                    outputBuffer[idx] = laserColor + renderChar + RESET;
                    zBuffer[idx] = d; 
                } else {
                    // Starfield backdrop
                    if ((x + y * 7) % 19 == 0 && (x * y) % 7 == 3) {
                        outputBuffer[idx] = "\u001B[38;2;45;50;60m.\u001B[0m";
                    } else {
                        outputBuffer[idx] = " ";
                    }
                    zBuffer[idx] = 0.0;
                }
            }
        }
    }
}
