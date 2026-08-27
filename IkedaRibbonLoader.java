// TODO: Come up with some better initial conditions or some better ways to varry the constants... The math is all there but our visual perspective is sometimes cool sometimes boring

import java.util.Random;

public class IkedaRibbonLoader extends Loader {
    private static final StatusStage[] IKEDA_STAGES = {
        new StatusStage(25, "Tuning ring cavity mirrors:"),
        new StatusStage(50, "Simulating non-linear refraction:"),
        new StatusStage(75, "Tracing chaotic laser ribbons:"),
        new StatusStage(100, "Ikeda Laser Core Operational!")
    };
    
    private final int width = 136;
    private final int height = 34;
    private final int totalSize = width * height; 
    private final double[] ribbonDensity;
    private final Random rand = new Random();
    
    // Increased particle count to keep ribbons thick on the larger screen
    private static final int TRAJECTORY_COUNT = 128;
    private final double[] particleX = new double[TRAJECTORY_COUNT];
    private final double[] particleY = new double[TRAJECTORY_COUNT];
    
    private double timeClock = 0.0;

    public IkedaRibbonLoader() {
        super(IKEDA_STAGES, 136, 34);
        this.ribbonDensity = new double[totalSize];
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        for (int i = 0; i < totalSize; i++) {
            ribbonDensity[i] = 0.0;
        }
        for (int i = 0; i < TRAJECTORY_COUNT; i++) {
            particleX[i] = -0.5 + (rand.nextDouble() * 2.0);
            particleY[i] = -1.5 + (rand.nextDouble() * 2.0);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.025; 

        // Slower fade (0.75 instead of 0.65) to create longer motion trails 
        // across the larger screen area
        for (int i = 0; i < totalSize; i++) {
            ribbonDensity[i] *= 0.75; 
        }

        // B) DYNAMIC CONSTANTS 
        // Dissipation: Makes the attractor "breathe" in and out
        double B = 0.85 + 0.1 * Math.cos(timeClock * 0.2); 
        
        // Non-linear refraction: Curls and uncurls the fractal arms
        double C = 6.0 + 1.5 * Math.sin(timeClock * 0.35);
        
        // Phase shift: Rotates the attractor (from your original code)
        double R = 0.8 + 0.4 * Math.sin(timeClock * 0.5);

        // Input drive (usually 1.0): Warps and stretches the shape horizontally
        double U = 1.0 + 0.25 * Math.sin(timeClock * 0.15);

        for (int strand = 0; strand < TRAJECTORY_COUNT; strand++) {
            double px = particleX[strand];
            double py = particleY[strand];

            // Increased steps per frame to bridge the gaps on a wider screen
            for (int step = 0; step < 25; step++) {
                double currentRadiusSq = px * px + py * py;
                double t = R - (C / (1.0 + currentRadiusSq));
                double cosT = Math.cos(t);
                double sinT = Math.sin(t);

                // Replaced the hardcoded 1.0 with our dynamic U variable
                double nextX = U + B * (px * cosT - py * sinT);
                double nextY = B * (px * sinT + py * cosT);
                
                px = nextX;
                py = nextY;

                // Updated Screen Mapping: Scaled up and re-centered for 136x34
                int screenX = (int) (62.0 + (px * 28.0));
                int screenY = (int) (17.0 - (py * 7.5));

                if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) {
                    int idx = screenX + width * screenY;
                    ribbonDensity[idx] += 0.25; 
                }
            }
            
            particleX[strand] = px;
            particleY[strand] = py;

            // Failsafe respawn for particles ejected by the shifting variables
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
                    // Starfield (spread out slightly more for the larger screen)
                    if ((x + y * 7) % 31 == 0 && (x * y) % 7 == 3) {
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