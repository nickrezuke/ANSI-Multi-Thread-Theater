// TODO: Improve the visual clarity of the Hypersphere

public class HypersphereLoader extends Loader {
    private static final StatusStage[] HYPER_STAGES = {
        new StatusStage(25, "Calibrating 4D hyper-polar grid:"),
        new StatusStage(50, "Generating 3-sphere cellular lattice:"),
        new StatusStage(75, "Engaging stereographic hyperspace rotation:"),
        new StatusStage(100, "4D Hypersphere Online!")
    };

    private static final char CH_VERTEX = '\u2591'; // ░ Intersecting polar node
    private static final char CH_EDGE = '\u2588';   // █ Solid unbroken shaded mesh element

    // Bright Cyberpunk Baseline Palettes
    private static final int[] PALETTE_A = {255, 0, 128};  // Hot Neon Pink
    private static final int[] PALETTE_B = {0, 255, 230};  // Electric Cyan

    private double angleXW = 0.0;
    private double angleXY = 0.0;
    private double angleYZ = 0.0;

    public HypersphereLoader() {
        super(HYPER_STAGES);
    }

    @Override
    protected void initialize() {
        // No persistent runtime frame allocations needed across cycles
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosXW = Math.cos(angleXW), sinXW = Math.sin(angleXW);
        double cosXY = Math.cos(angleXY), sinXY = Math.sin(angleXY);
        double cosYZ = Math.cos(angleYZ), sinYZ = Math.sin(angleYZ);

        double radius4D = 1.0;
        double distance4D = 2.2;
        double currentZoom = 28.0;

        // Establish an imaginary 3D light vector positioned at Top-Left-Front (-0.577, 0.577, -0.577)
        double lightX = -0.577, lightY = 0.577, lightZ = -0.577;

        // Step through 4D hyper-polar coordinates
        for (double psi = 0.2; psi < Math.PI; psi += 0.35) {
            double sinPsi = Math.sin(psi);
            double cosPsi = Math.cos(psi);

            for (double theta = 0.15; theta < Math.PI; theta += 0.15) {
                double sinTheta = Math.sin(theta);
                double cosTheta = Math.cos(theta);

                int prevX = -1, prevY = -1;
                double prevDepth = 99999.0; // Synced with new min-sort z-buffer parameters

                for (double phi = 0.0; phi <= 2.0 * Math.PI + 0.1; phi += 0.02) {
                    double sinPhi = Math.sin(phi);
                    double cosPhi = Math.cos(phi);

                    // 1. Raw 4D Hyper-Polar Coordinates
                    double x = radius4D * sinPsi * sinTheta * cosPhi;
                    double y = radius4D * sinPsi * sinTheta * sinPhi;
                    double z = radius4D * sinPsi * cosTheta;
                    double w = radius4D * cosPsi;

                    // 2. Perform 4D X-W Hyper-Rotation
                    double x4D = x * cosXW - w * sinXW;
                    double w4D = x * sinXW + w * cosXW;

                    // 3. 4D Perspective Stereographic Projection down to 3D Space Coordinates
                    double factor4D = 1.0 / (distance4D - w4D * 0.60);
                    double x3D = x4D * factor4D;
                    double y3D = y * factor4D;
                    double z3D = z * factor4D;

                    // 4. Standard 3D Spatial Rotations
                    double x1 = x3D * cosXY - y3D * sinXY;
                    double y1 = x3D * sinXY + y3D * cosXY;
                    double y2 = y1 * cosYZ - z3D * sinYZ;
                    double z2 = y1 * sinYZ + z3D * cosYZ;

                    // 5. 2D Orthographic Projection with Aspect Ratio Correction
                    int xp = (int) (40 + currentZoom * 2.3 * x1);
                    int yp = (int) (11 + currentZoom * y2);
                    
                    // Invert depth mapping for traditional min-sorting
                    double depth = -z2; 

                    // 6. Dynamic 4D Chroma & Lambertian Lighting Shader Engine
                    // Normalize the projected 3D coordinates to simulate surface vectors
                    double len3D = Math.sqrt(x1*x1 + y2*y2 + z2*z2);
                    double nx = len3D > 0 ? x1 / len3D : 0;
                    double ny = len3D > 0 ? y2 / len3D : 0;
                    double nz = len3D > 0 ? z2 / len3D : 0;

                    // Calculate light exposure via standard dot product operations
                    double intensity = nx * lightX + ny * lightY + nz * lightZ;
                    intensity = (intensity + 1.0) / 2.0; // Normalize range to 0.0 - 1.0
                    intensity = 0.3 + 0.7 * intensity;   // Add baseline ambient radiance

                    // Map color ratios dynamically to the active 4D depth tracking element (w4D)
                    double hyperRatio = (w4D + 1.0) / 2.0; 
                    hyperRatio = Math.max(0.0, Math.min(1.0, hyperRatio));

                    int r = (int) ((PALETTE_A[0] + (PALETTE_B[0] - PALETTE_A[0]) * hyperRatio) * intensity);
                    int g = (int) ((PALETTE_A[1] + (PALETTE_B[1] - PALETTE_A[1]) * hyperRatio) * intensity);
                    int b = (int) ((PALETTE_A[2] + (PALETTE_B[2] - PALETTE_A[2]) * hyperRatio) * intensity);
                    
                    String colorCode = String.format("\u001B[38;2;%d;%d;%dm", 
                        Math.max(0, Math.min(255, r)), 
                        Math.max(0, Math.min(255, g)), 
                        Math.max(0, Math.min(255, b))
                    );

                    // Execute seamless continuous path raster lines via DDA Tracing
                    if (prevX != -1 && (Math.abs(xp - prevX) > 0 || Math.abs(yp - prevY) > 0)) {
                        traceDDALine(outputBuffer, zBuffer, prevX, prevY, xp, yp, prevDepth, depth, colorCode);
                    } else if (prevX == -1) {
                        drawPixel(outputBuffer, zBuffer, xp, yp, depth, true, colorCode);
                    }

                    prevX = xp;
                    prevY = yp;
                    prevDepth = depth;
                }
            }
        }

        angleXW += 0.017;
        angleXY += 0.011;
        angleYZ += 0.015;
    }

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
            // Translucent vertex bias adapted for min-sort depth metrics
            double testingDepth = isVertex ? (depth - 100.0) : depth;

            if (testingDepth < zBuffer[index]) {
                zBuffer[index] = depth;
                char renderChar = isVertex ? CH_VERTEX : CH_EDGE;
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
        }
    }
}
