// TODO: Fix This lmao totally broken....

import java.util.Random;

public class IkedaRibbonLoader extends Loader {
    private static final StatusStage[] IKEDA_STAGES = {
        new StatusStage(25, "Tuning ring cavity mirrors:"),
        new StatusStage(50, "Simulating non-linear refraction:"),
        new StatusStage(75, "Tracing chaotic laser ribbons:"),
        new StatusStage(100, "Ikeda Laser Core Operational!")
    };

    private final int width;
    private final int height;
    private final int totalSize;

    // Canvas density trackers to hold laser particle weights per frame
    private final double[] ribbonDensity;
    private final Random rand = new Random();

    // Chronological system timeline clock to animate laser cavity refraction
    private double timeClock = 0.0;

    public IkedaRibbonLoader() {
        super(IKEDA_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
        this.totalSize = 1760;
        this.ribbonDensity = new double[totalSize];
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        // Zero out the density buffer cache
        for (int i = 0; i < totalSize; i++) {
            ribbonDensity[i] = 0.0;
        }

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Advance independent timeline registers to change the laser cavity indices
        timeClock += 0.008;

        // Clear the canvas density buffer fresh for the active frame step
        for (int i = 0; i < totalSize; i++) {
            ribbonDensity[i] = 0.0;
        }

        // System Constants for the non-linear medium
        double B = 0.9;  // Cavity dissipation factor
        double C = 6.0;  // Laser intensity coefficient
        
        // Dynamic Refraction Angle: Slowly oscillates over time to smoothly bend the ribbons
        double R = 1.0 + 0.15 * Math.sin(timeClock);

        // Core starting point variables for the chaos game loop
        double px = 0.1;
        double py = 0.0;

        // To weave thick, high-fidelity silk trails, calculate 24,000 iterations per frame
        for (int iter = 0; iter < 24000; iter++) {
            // Ikeda Map System Equations
            double currentRadiusSq = px * px + py * py;
            double t = R - (C / (1.0 + currentRadiusSq));
            
            double cosT = Math.cos(t);
            double sinT = Math.sin(t);

            double nextX = 1.0 + B * (px * cosT - py * sinT);
            double nextY = B * (px * sinT + py * cosT);

            px = nextX;
            py = nextY;

            // Map the mathematical plane boundaries into your 80x22 terminal grid
            // Ikeda map points generally occupy x [-1.5 to 2.5] and y [-2.5 to 2.5]
            int screenX = (int) (35.0 + (px * 16.5));
            // Invert the layout rows because terminal array indices count downwards
            int screenY = (int) (11.0 - (py * 3.8 * 2.1)); // Factor in font aspect ratio

            // Verify viewport boundary rails before logging data
            if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) {
                int idx = screenX + width * screenY;
                // Accumulate particle hit weight (clips at 1.0 peak saturation)
                ribbonDensity[idx] = Math.min(1.0, ribbonDensity[idx] + 0.04);
            }
        }

        // Rasterize density tracks directly to your output buffers
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int idx = x + rowOffset;
                double d = ribbonDensity[idx];

                if (d > 0.01) {
                    zBuffer[idx] = d;

                    // Shading Selector String: Choose characters based on laser concentration
                    String palette = " .:-=+*#%@█";
                    int shadeIdx = (int) (d * (palette.length() - 1));
                    shadeIdx = Math.max(0, Math.min(palette.length() - 1, shadeIdx));
                    char renderChar = palette.charAt(shadeIdx);

                    // Dynamic Neon TrueColor Rainbow Shift Formula
                    // We link colors to spatial coordinates (x, y) and timeClock, 
                    // causing color currents to glide through the silk folds organically!
                    double colorOffset = timeClock * 2.5 + (x * 0.04) + (y * 0.08);
                    int r = (int) (128 + 127 * Math.sin(colorOffset));
                    int g = (int) (128 + 127 * Math.sin(colorOffset + 2.0 * Math.PI / 3.0));
                    int b = (int) (128 + 127 * Math.sin(colorOffset + 4.0 * Math.PI / 3.0));

                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));

                    String laserColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                    outputBuffer[idx] = laserColor + renderChar + RESET;
                } else {
                    // Optional backdrop texture: Add a tiny, dim gray pixel dust to give the void space depth
                    if (x % 6 == 0 && y % 3 == 0) {
                        outputBuffer[idx] = "\u001B[38;2;40;45;50m.\u001B[0m";
                    } else {
                        outputBuffer[idx] = " ";
                    }
                }
            }
        }
    }
}
