// TODO: Check if z buffer is okay in 4D / 3D like is the yellow ring supposed to be in front?

public class HopfFibrationLoader extends Loader {
    private static final StatusStage[] HOPF_STAGES = {
        new StatusStage(25, "Mapping hyperspherical Hopf fiber coordinates:"),
        new StatusStage(50, "Projecting interlocking 4D circular loops:"),
        new StatusStage(75, "Syncing stereographic torus trajectories:"),
        new StatusStage(100, "Hopf Fibration Matrix Operational!")
    };

    private static final char CH_VERTEX = '\u2591'; // ░ Fiber Intersection Node
    private static final char CH_EDGE = '\u2588';   // █ Unbroken Solid Fiber String

    // Bright Cyberpunk Palette mapping nested tori layers
    private static final int[][] PALETTE = {
        {255, 0, 128},   // Outer Layer: Neon Pink
        {0, 255, 180},   // Middle Layer: Mint Emerald
        {0, 150, 255},   // Inner Layer: Electric Cyan
        {240, 230, 20}   // Core Ring: Cyber Yellow
    };

    private double rotationX = 0.0;
    private double rotationY = 0.0;
    private double rotationZ = 0.0;

    public HopfFibrationLoader() {
        super(HOPF_STAGES);
    }

    @Override
    protected void initialize() {
        // No persistent array allocations needed between frames
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosX = Math.cos(rotationX), sinX = Math.sin(rotationX);
        double cosY = Math.cos(rotationY), sinY = Math.sin(rotationY);
        double cosZ = Math.cos(rotationZ), sinZ = Math.sin(rotationZ);

        // 1. Parameterize the Hopf Fibration
        // eta scales the nested tori layers, xi positions circles along the tori surface,
        // and phi sweeps individual coordinates to draw each closed circular fiber loop.
        int colorIdx = 0;
        for (double eta = 0.25; eta <= Math.PI / 2.0; eta += 0.4) {
            int[] rgb = PALETTE[colorIdx % PALETTE.length];
            String fiberColor = String.format("\u001B[38;2;%d;%d;%dm", rgb[0], rgb[1], rgb[2]);
            colorIdx++;

            for (double xi = 0.0; xi < 2.0 * Math.PI; xi += 0.6) {
                int prevX = -1, prevY = -1;
                double prevDepth = 0.0;

                // Step closely through phi to construct a seamless, closed circular fiber loop
                for (double phi = 0.0; phi <= 2.0 * Math.PI + 0.1; phi += 0.1) {
                    // Map 3D angles to specific 4D hypersphere hyper-coordinates (S³ embedded in R⁴)
                    double w = Math.cos(eta) * Math.cos(phi);
                    double x = Math.cos(eta) * Math.sin(phi);
                    double y = Math.sin(eta) * Math.cos(phi + xi);
                    double z = Math.sin(eta) * Math.sin(phi + xi);

                    // 2. Stereographic Projection from 4D down into 3D Space Coordinates
                    // Projecting from the pole (0, 0, 0, 1) maps S³ flawlessly into R³
                    double factor4D = 1.0 / (1.0001 - w); 
                    double x3D = x * factor4D;
                    double y3D = y * factor4D;
                    double z3D = z * factor4D;

                    // Apply standard multi-axis 3D rotations to the projected configuration
                    double x1 = x3D * cosY - z3D * sinY;
                    double z1 = x3D * sinY + z3D * cosY;
                    double y1 = y3D * cosX - z1 * sinX;
                    double z2 = y3D * sinX + z1 * cosX;
                    double x2 = x1 * cosZ - y1 * sinZ;
                    double y2 = x1 * sinZ + y1 * cosZ;

                    double distFactor = Math.cos(System.currentTimeMillis() / 1600.0) * 8.0;

                    // Orthographic 2D Projection with Character Aspect Ratio Adjustment (2.3x multiplier on X)
                    int xp = (int) (40 + (13 + distFactor) * 2.3 * x2);
                    int yp = (int) (11 + (13 + distFactor) * y2);
                    double depth = z2;

                    // Render lines without diagonal cell gaps using screen-space DDA tracking
                    if (prevX != -1 && (Math.abs(xp - prevX) > 0 || Math.abs(yp - prevY) > 0)) {
                        traceDDALine(outputBuffer, zBuffer, prevX, prevY, xp, yp, prevDepth, depth, fiberColor);
                    } else if (prevX == -1) {
                        drawPixel(outputBuffer, zBuffer, xp, yp, depth, true, fiberColor);
                    }

                    prevX = xp;
                    prevY = yp;
                    prevDepth = depth;
                }
            }
        }

        rotationX += 0.008;
        rotationY += 0.012;
        rotationZ += 0.005;
    }

    /**
     * Rasters continuous, gap-free fibers across the layout buffer using 2D DDA line tracing.
     */
    private void traceDDALine(String[] outputBuffer, double[] zBuffer, int x0, int y0, int x1, int y1, double z0, double z1, String color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);

        if (steps == 0) {
            drawPixel(outputBuffer, zBuffer, x0, y0, z0, false, color);
            return;
        }

        double xInc = (double) (x1 - x0) / steps;
        double yInc = (double) (y1 - y0) / steps;
        double zInc = (z1 - z0) / steps;

        double cx = x0;
        double cy = y0;
        double cz = z0;

        for (int s = 0; s <= steps; s++) {
            int rx = (int) Math.round(cx);
            int ry = (int) Math.round(cy);
            
            // Highlight the connections where loops clip across matching phase steps
            boolean isVertex = (s == 0 || s == steps);

            drawPixel(outputBuffer, zBuffer, rx, ry, cz, isVertex, color);

            cx += xInc;
            cy += yInc;
            cz += zInc;
        }
    }

    private void drawPixel(String[] outputBuffer, double[] zBuffer, int x, int y, double depth, boolean isVertex, String colorCode) {
        if (x >= 0 && x < 80 && y >= 0 && y < 22) {
            int index = x + 80 * y;
            // Depth bias injection guarantees intersection nodes punch through cleanly
            double testingDepth = isVertex ? (depth + 100.0) : depth;

            if (testingDepth > zBuffer[index]) {
                zBuffer[index] = depth;
                char renderChar = isVertex ? CH_VERTEX : CH_EDGE;
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
        }
    }
}
