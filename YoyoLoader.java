import java.util.Arrays;

public class YoyoLoader extends Loader {

    private static final StatusStage[] YOYO_STAGES = {
            new StatusStage(10, "Winding string onto axle..."),
            new StatusStage(30, "Dropping into a sleeper..."),
            new StatusStage(50, "Walking the dog..."),
            new StatusStage(75, "Around the World..."),
            new StatusStage(100, "Snap return back to hand!")
    };

    private double angleX;
    private double angleY = 0.0;
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Palette: Anodized Red Body, Gold Rims, Metallic Silver Axle, Bright Yellow String
    private static final String C_BODY   = "\u001B[38;2;230;35;70m";   // Crimson red shells
    private static final String C_RIM    = "\u001B[38;2;255;200;40m";  // Gold outer rim & radial starburst
    private static final String C_AXLE   = "\u001B[38;2;200;210;225m"; // Silver steel axle
    private static final String C_STRING = "\u001B[38;2;255;255;160m"; // Twisted cotton/poly string
    private static final String RESET    = "\u001B[0m";

    public YoyoLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public YoyoLoader() {
        super(YOYO_STAGES, 40, 22);
        this.width = 40;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        // Pitched downward slightly to reveal depth, string wrap, and profile
        this.angleX = 0.28; 
        this.angleY = 0.35;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;

        // Smooth camera drift around the yo-yo
        angleY += 0.012;

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Key light from top-left front
        double lightX = 0.5, lightY = 0.8, lightZ = -0.5;
        double lightMag = Math.sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ);
        lightX /= lightMag; lightY /= lightMag; lightZ /= lightMag;

        // --- KINEMATICS & ANIMATION ---
        // Smooth vertical oscillation (unwinding to bottom, then rewinding to top)
        double cycle = frameTick * 0.075;
        double yoyoY = 0.1 + 0.92 * Math.cos(cycle);

        // High-speed spin around the central horizontal axle axis
        double spinAngle = frameTick * 0.5;

        double handY = 1.5; // Top hand anchor point

        // 1. VERTICAL SUSPENSION STRING
        renderString(0.0, handY, 0.0, 0.0, yoyoY, 0.0, C_STRING,
                cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. CENTRAL METALLIC AXLE
        double axleWidth = 0.14;
        double axleRadius = 0.09;
        renderAxle(0.0, yoyoY, 0.0, axleWidth, axleRadius, C_AXLE,
                cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. YO-YO HALVES (Butterfly profile shell discs)
        double discRadius = 0.82;
        double halfThickness = 0.32;

        // Left Shell (-X)
        renderHalfShell(-axleWidth / 2.0, yoyoY, 0.0, -1, discRadius, halfThickness, spinAngle,
                cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // Right Shell (+X)
        renderHalfShell(axleWidth / 2.0, yoyoY, 0.0, 1, discRadius, halfThickness, spinAngle,
                cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
    }

    private void renderHalfShell(double startX, double cy, double cz, int sideSign,
                                 double maxRadius, double thickness, double spinAngle,
                                 double cosY, double sinY, double cosX, double sinX,
                                 double lx, double ly, double lz, String[] out, double[] zb) {

        for (double r = 0.08; r <= maxRadius; r += 0.035) {
            // Concave butterfly profile flare (flaring outward along X as radius increases)
            double xOffset = sideSign * thickness * Math.pow(r / maxRadius, 1.4);
            double px_center = startX + xOffset;

            // Surface normal slope calculations
            double dxdr = sideSign * thickness * 1.4 * Math.pow(r / maxRadius, 0.4) / maxRadius;
            double normX = sideSign * 1.0;
            double normR = -dxdr;

            int steps = (int) (32 * (r / maxRadius)) + 8;
            for (int step = 0; step < steps; step++) {
                double u = step * (2.0 * Math.PI / steps);

                // Rotate around central X axis to apply high-speed spin
                double rotU = u + spinAngle;
                double ny = Math.cos(rotU);
                double nz = Math.sin(rotU);

                double px = px_center;
                double py = cy + r * ny;
                double pz = cz + r * nz;

                // Composite normal vector
                double nx = normX;
                double ny_norm = normR * ny;
                double nz_norm = normR * nz;

                // Visual starburst cap/rim pattern to make rotation clearly visible
                String color = C_BODY;
                boolean isRim = r > maxRadius - 0.1;
                boolean isSpoke = Math.sin(4 * u) > 0.6;

                if (isRim || isSpoke) {
                    color = C_RIM;
                }

                projectPoint(px, py, pz, nx, ny_norm, nz_norm, color, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderAxle(double cx, double cy, double cz, double width, double radius, String colorCode,
                            double cosY, double sinY, double cosX, double sinX,
                            double lx, double ly, double lz, String[] out, double[] zb) {

        for (double x = -width / 2.0; x <= width / 2.0; x += 0.03) {
            for (int step = 0; step < 16; step++) {
                double rad = step * (2.0 * Math.PI / 16.0);
                double ny = Math.cos(rad);
                double nz = Math.sin(rad);

                projectPoint(cx + x, cy + radius * ny, cz + radius * nz, 0, ny, nz, colorCode,
                        cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderString(double x1, double y1, double z1, double x2, double y2, double z2, String colorCode,
                             double cosY, double sinY, double cosX, double sinX,
                             double lx, double ly, double lz, String[] out, double[] zb) {

        double dist = Math.hypot(x2 - x1, Math.hypot(y2 - y1, z2 - z1));
        int steps = (int) (dist * 25);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double pz = z1 + (z2 - z1) * t;

            projectPoint(px, py, pz, 0, 1, 0, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                              double cosY, double sinY, double cosX, double sinX,
                              double lx, double ly, double lz, String[] out, double[] zb) {

        // 1. Yaw Rotation (Y axis)
        double r1x = px * cosY - pz * sinY;
        double r1y = py;
        double r1z = px * sinY + pz * cosY;

        // 2. Pitch Rotation (X axis tilt down)
        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // Rotate Normal Vectors
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
        double cameraDepth = rotZ + 3.7;
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        // Map to 2D Terminal Grid
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotY);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                double illuminance = Math.min(1.0, Math.max(0.18, dot + 0.1));

                char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];

                out[idx] = colorCode + glyph + RESET;
            }
        }
    }
}