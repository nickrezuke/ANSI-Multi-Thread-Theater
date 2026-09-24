// TODO: Improve the accuracy of this

import java.util.Arrays;

public class SuperMushroomLoader extends Loader {

    private static final StatusStage[] MUSHROOM_STAGES = {
            new StatusStage(10, "Hitting the ? Block..."),
            new StatusStage(30, "Mushroom emerged..."),
            new StatusStage(50, "Sliding to the right..."),
            new StatusStage(75, "Rebounding off a green pipe..."),
            new StatusStage(100, "Powering up! Super form achieved.")
    };

    private double angleX = -0.2;
    private double angleY = 0.0;
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Super Mario Mushroom Color Palette
    private static final String C_RED    = "\u001B[38;2;220;30;30m";   // Red cap
    private static final String C_WHITE  = "\u001B[38;2;240;240;240m"; // White spots
    private static final String C_STEM   = "\u001B[38;2;245;200;160m"; // Peach/beige stem
    private static final String C_BLACK  = "\u001B[38;2;10;10;10m";    // Black eyes
    private static final String RESET    = "\u001B[0m";

    public SuperMushroomLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public SuperMushroomLoader() {
        super(MUSHROOM_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        // Tilted down slightly to view the top center spot clearly
        this.angleX = -0.25; 
        this.angleY = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;
        // Spin the mushroom and add a cheerful bounce on the Z axis
        angleY += 0.035;
        double zOffset = 0.15 * Math.sin(frameTick * 0.08); 

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Directional lighting
        double lightX = 0.6, lightY = -0.5, lightZ = 0.7;
        double lightMag = Math.sqrt(lightX*lightX + lightY*lightY + lightZ*lightZ);
        lightX /= lightMag; lightY /= lightMag; lightZ /= lightMag;

        // 1. CAP (Massive Upper Dome and Underbelly)
        renderCap(0.0, 0.0, 0.1 + zOffset, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. STEM (Stubby Peach Base with Eyes)
        renderStem(0.0, 0.0, 0.1 + zOffset, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
    }

    private void renderCap(double cx, double cy, double cz,
                           double cosX, double sinX, double cosY, double sinY,
                           double lx, double ly, double lz, String[] out, double[] zb) {
        
        // Drastically increased scale for authentic proportions
        double capRadius = 1.25;

        // Render the top dome of the red cap
        for (double v = 0; v <= Math.PI / 2; v += 0.03) {
            for (double u = 0; u < 2 * Math.PI; u += 0.03) {
                double nx = Math.cos(v) * Math.cos(u);
                double ny = Math.cos(v) * Math.sin(u);
                double nz = Math.sin(v);

                double px = cx + capRadius * nx;
                double py = cy + capRadius * ny;
                // Slightly squashed vertically to form a wide, resting cap
                double pz = cz + capRadius * nz * 0.9; 

                String color = C_RED;
                if (isSpot(nx, ny, nz)) {
                    color = C_WHITE;
                }

                projectPoint(px, py, pz, nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }

        // Render the flat underbelly overhang connecting the wide cap down to the stem
        for (double r = 0.68; r <= capRadius; r += 0.04) {
            for (double u = 0; u < 2 * Math.PI; u += 0.05) {
                double nx = 0, ny = 0, nz = -1;
                double px = cx + r * Math.cos(u);
                double py = cy + r * Math.sin(u);
                
                projectPoint(px, py, cz, nx, ny, nz, C_WHITE, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private boolean isSpot(double nx, double ny, double nz) {
        // Accurate Mario Mushroom spot layout: 1 top center, 4 equally spaced around the sides
        double[][] spots = {
                {0.0, 0.0, 1.0},      // Top Center
                {0.0, -0.866, 0.5},   // Front
                {0.0, 0.866, 0.5},    // Back
                {-0.866, 0.0, 0.5},   // Left
                {0.866, 0.0, 0.5}     // Right
        };

        for (double[] spot : spots) {
            double d = (nx * spot[0] + ny * spot[1] + nz * spot[2]);
            // A strict threshold of 0.86 yields crisp, sparse circular spots
            if (d > 0.86) return true; 
        }
        return false;
    }

    private void renderStem(double cx, double cy, double cz,
                            double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        
        // Wider, stubbier stem to match the massive cap
        double stemRadius = 0.7;

        for (double v = -Math.PI / 2; v <= 0; v += 0.04) {
            for (double u = 0; u < 2 * Math.PI; u += 0.04) {
                double nx = Math.cos(v) * Math.cos(u);
                double ny = Math.cos(v) * Math.sin(u);
                double nz = Math.sin(v);

                double px = cx + stemRadius * nx;
                double py = cy + stemRadius * ny;
                // Base pushed down, but squashed to look planted and stubby
                double pz = cz - 0.2 + stemRadius * nz * 0.7; 

                String color = C_STEM;
                if (isEye(px - cx, py - cy, pz - cz)) {
                    color = C_BLACK;
                }

                projectPoint(px, py, pz, nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private boolean isEye(double dx, double dy, double dz) {
        // Only evaluate on the front face (-Y direction)
        if (dy > -0.1) return false;
        
        // Proportionally adjusted coordinates for the two vertical black oval eyes
        double ex1 = -0.26, ez1 = -0.42;
        double ex2 = 0.26, ez2 = -0.42;
        
        double dx1 = dx - ex1;
        double dz1 = dz - ez1;
        
        double dx2 = dx - ex2;
        double dz2 = dz - ez2;
        
        // Radii squared for the pill-shape bounds
        double width2 = 0.009; 
        double height2 = 0.06; 
        
        if ((dx1*dx1)/width2 + (dz1*dz1)/height2 < 1.0) return true;
        if ((dx2*dx2)/width2 + (dz2*dz2)/height2 < 1.0) return true;
        
        return false;
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                              double cosX, double sinX, double cosY, double sinY,
                              double lx, double ly, double lz, String[] out, double[] zb) {
        
        // 1. World Rotations
        double r1x = px * cosY - py * sinY;
        double r1y = px * sinY + py * cosY;
        double r1z = pz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // Rotate Normal Vectors for shading calculations
        double n1x = nx * cosY - ny * sinY;
        double n1y = nx * sinY + ny * cosY;
        double n1z = nz;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen;
        }

        // 2. Camera Projection & Z-Depth
        // Zoomed the camera in (3.2 instead of 3.8) to fill out the terminal size perfectly
        double cameraDepth = rotY + 3.2; 
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        // Map to 2D Terminal Space
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotZ);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                double illuminance;
                
                if (colorCode.equals(C_BLACK)) {
                    illuminance = 0.95; 
                } else if (colorCode.equals(C_WHITE)) {
                    illuminance = Math.min(1.0, Math.max(0.3, dot) + 0.3);
                } else {
                    illuminance = Math.max(0.2, dot);
                }

                // Solid geometric ASCII Ramp (no spaces to prevent background bleed-through)
                char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];

                out[idx] = colorCode + glyph + RESET;
            }
        }
    }
}