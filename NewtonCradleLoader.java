import java.util.Arrays;

public class NewtonCradleLoader extends Loader {

    private static final StatusStage[] CRADLE_STAGES = {
            new StatusStage(10, "Assembling desktop chrome frame..."),
            new StatusStage(30, "Suspending high-carbon steel spheres..."),
            new StatusStage(50, "Aligning fine suspension wires..."),
            new StatusStage(75, "Displacing lead sphere..."),
            new StatusStage(100, "Demonstrating conservation of momentum.")
    };

    private double angleX;
    private double angleY = 0.0;
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Palette: Sleek Chrome, Fine Wires, and Mahogany Wooden Base
    private static final String C_CHROME = "\u001B[38;2;220;235;250m"; // Mirror steel spheres & frame
    private static final String C_FRAME  = "\u001B[38;2;120;135;150m"; // Darker steel structure
    private static final String C_WIRE   = "\u001B[38;2;180;190;200m"; // Thin silver suspension lines
    private static final String C_BASE   = "\u001B[38;2;140;70;40m";   // Polished wooden base plate
    private static final String RESET    = "\u001B[0m";

    public NewtonCradleLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public NewtonCradleLoader() {
        super(CRADLE_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        // Angled top-down view to clearly highlight 3D depth, wires, and frame
        this.angleX = -0.22; 
        this.angleY = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;

        // Smooth camera orbit around the desktop cradle
        angleY += 0.008;

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Overhead specular lighting direction
        double lightX = 0.4, lightY = 0.9, lightZ = -0.4;
        double lightMag = Math.sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ);
        lightX /= lightMag; lightY /= lightMag; lightZ /= lightMag;

        // --- PHYSICS & KINEMATICS ---
        double cycle = (frameTick * 0.09) % (2 * Math.PI);
        double maxSwingAngle = 0.65; // Max displacement angle (~37 degrees)
        
        double[] sphereAngles = new double[5];
        if (cycle < Math.PI) {
            // Leftmost sphere swings out and strikes the middle stack
            sphereAngles[0] = -maxSwingAngle * Math.sin(cycle);
        } else {
            // Rightmost sphere receives momentum and swings out
            sphereAngles[4] = maxSwingAngle * Math.sin(cycle - Math.PI);
        }

        // --- GEOMETRY SPECIFICATIONS ---
        double sphereRadius = 0.28;
        double wireLength = 1.5;
        double topRailY = 1.0;
        double railZ = 0.55;
        double sphereSpacing = sphereRadius * 2.0; // Perfect point contact at rest

        // 1. MAHOGANY BASE PLATE
        renderBasePlatform(1.8, 0.9, -1.1, C_BASE, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. CHROME SUPPORT FRAME
        renderFrame(1.5, railZ, topRailY, -1.1, C_FRAME, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. FIVE SUSPENDED SPHERES & WIRES
        for (int i = 0; i < 5; i++) {
            double pivotX = (i - 2) * sphereSpacing;
            double angle = sphereAngles[i];

            // Pendulum arc positioning
            double cx = pivotX + wireLength * Math.sin(angle);
            double cy = topRailY - wireLength * Math.cos(angle);
            double cz = 0.0;

            // Render V-Shaped Suspension Wires (from front/back top rails to top of sphere)
            renderLine(pivotX, topRailY,  railZ, cx, cy + sphereRadius * 0.9, cz, C_WIRE, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
            renderLine(pivotX, topRailY, -railZ, cx, cy + sphereRadius * 0.9, cz, C_WIRE, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

            // Render Chrome Sphere
            renderChromeSphere(cx, cy, cz, sphereRadius, C_CHROME, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }
    }

    private void renderChromeSphere(double cx, double cy, double cz, double r, String colorCode,
                                    double cosY, double sinY, double cosX, double sinX,
                                    double lx, double ly, double lz, String[] out, double[] zb) {
        
        for (double v = -Math.PI / 2; v <= Math.PI / 2; v += 0.08) {
            for (double u = 0; u < 2 * Math.PI; u += 0.08) {
                double nx = Math.cos(v) * Math.cos(u);
                double ny = Math.cos(v) * Math.sin(u);
                double nz = Math.sin(v);

                double px = cx + r * nx;
                double py = cy + r * ny;
                double pz = cz + r * nz;

                projectPoint(px, py, pz, nx, ny, nz, colorCode, true, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderBasePlatform(double halfW, double halfD, double yLevel, String colorCode,
                                     double cosY, double sinY, double cosX, double sinX,
                                     double lx, double ly, double lz, String[] out, double[] zb) {
        
        double thickness = 0.12;
        // Flat top wooden surface
        for (double x = -halfW; x <= halfW; x += 0.06) {
            for (double z = -halfD; z <= halfD; z += 0.06) {
                projectPoint(x, yLevel, z, 0, 1, 0, colorCode, false, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
            }
        }
        // Front bevel edge
        for (double x = -halfW; x <= halfW; x += 0.06) {
            for (double y = yLevel - thickness; y <= yLevel; y += 0.04) {
                projectPoint(x, y, halfD, 0, 0, 1, colorCode, false, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderFrame(double halfW, double railZ, double topY, double baseY, String colorCode,
                             double cosY, double sinY, double cosX, double sinX,
                             double lx, double ly, double lz, String[] out, double[] zb) {
        
        // 4 Vertical Pillars
        renderLine(-halfW, baseY,  railZ, -halfW, topY,  railZ, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
        renderLine( halfW, baseY,  railZ,  halfW, topY,  railZ, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
        renderLine(-halfW, baseY, -railZ, -halfW, topY, -railZ, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
        renderLine( halfW, baseY, -railZ,  halfW, topY, -railZ, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);

        // 2 Top Horizontal Support Bars
        renderLine(-halfW, topY,  railZ, halfW, topY,  railZ, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
        renderLine(-halfW, topY, -railZ, halfW, topY, -railZ, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
    }

    private void renderLine(double x1, double y1, double z1, double x2, double y2, double z2, String colorCode,
                            double cosY, double sinY, double cosX, double sinX,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        
        double dist = Math.hypot(x2 - x1, Math.hypot(y2 - y1, z2 - z1));
        int steps = (int) (dist * 28);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double pz = z1 + (z2 - z1) * t;

            projectPoint(px, py, pz, 0, 1, 0, colorCode, false, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz,
                              String colorCode, boolean isSpecularChrome,
                              double cosY, double sinY, double cosX, double sinX,
                              double lx, double ly, double lz, String[] out, double[] zb) {

        // 1. Yaw Rotation (around vertical Y axis)
        double r1x = px * cosY - pz * sinY;
        double r1y = py;
        double r1z = px * sinY + pz * cosY;

        // 2. Pitch Rotation (downward camera view along X axis)
        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // Rotate Normal Vectors for dynamic reflection
        double n1x = nx * cosY - nz * sinY;
        double n1y = ny;
        double n1z = nx * sinY + nz * cosY;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen;
        }

        // 3. Camera Projection & Z-Depth
        double cameraDepth = rotZ + 3.2;
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        // Map to 2D Terminal Space
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotY);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                double illuminance;

                if (isSpecularChrome) {
                    // High-contrast specular reflection modeling metallic chrome glint
                    double diff = Math.max(0.1, dot);
                    double spec = Math.pow(Math.max(0.0, dot), 8);
                    illuminance = Math.min(1.0, diff * 0.5 + spec * 0.85);
                } else {
                    illuminance = Math.min(1.0, Math.max(0.15, dot + 0.2));
                }

                char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];

                out[idx] = colorCode + glyph + RESET;
            }
        }
    }
}