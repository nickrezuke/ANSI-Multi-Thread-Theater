import java.util.Arrays;

public class ApolloLunarModuleLoader extends Loader {
    private static final StatusStage[] LM_STAGES = {
            new StatusStage(15, "Initiating powered descent abort guidance..."),
            new StatusStage(40, "Pitching over. Target landing site in view..."),
            new StatusStage(65, "Altimeter active. 500 feet... 100 feet..."),
            new StatusStage(85, "Contact light! Engine shutdown sequence..."),
            new StatusStage(100, "The Eagle has landed. Tranquility Base here.")
    };

    private double angleX = 0.4;
    private double angleY = 0.0;
    private int frameTick = 0;
    private final int width;
    private final int height;

    // Apollo LM Authentic Color Palette
    private static final String C_GOLD_FOIL = "\u001B[38;2;220;175;35m"; // Kapton Thermal Insulation (Descent Stage)
    private static final String C_SILVER_FOIL = "\u001B[38;2;170;180;190m"; // Aluminized Mylar (Descent Structural
                                                                            // Panels)
    private static final String C_BLACK_PANEL = "\u001B[38;2;45;50;60m"; // Ascent Stage Thermal Panels / Radar
    private static final String C_WHITE_BODY = "\u001B[38;2;240;240;245m"; // Ascent Cabin Hull
    private static final String C_WINDOW = "\u001B[38;2;30;120;180m"; // Triangular Aft/Forward Windows
    private static final String C_GEAR_STRUT = "\u001B[38;2;130;135;145m"; // Landing Struts & Footpads
    private static final String RESET = "\u001B[0m";

    public ApolloLunarModuleLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public ApolloLunarModuleLoader() {
        super(LM_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.angleX = -0.25;
        this.angleY = 2.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;
        angleY += 0.015; // Slow orbital/descent pan
        angleX = -0.25 + 0.13 * Math.sin(frameTick * 0.02); // Gentle hover sway

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Lunar sunlight direction (Top-Right-Front)
        double lightX = 0.6, lightY = -0.5, lightZ = 0.6;

        // 1. DESCENT STAGE (The lower octagonal gold/silver platform)
        renderBox(-0.55, -0.7, -0.55, 0.55, -0.1, 0.55, C_GOLD_FOIL, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);

        // Descent Engine Bell protruding underneath the center
        renderCone(0.0, -0.85, 0.0, 0.3, 0.25, C_SILVER_FOIL, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);

        // 2. FOUR LANDING GEAR LEGS AND FOOTPADS
        renderLandingLeg(-0.55, -0.55, -0.55, -1.1, -1.0, -1.1, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);
        renderLandingLeg(0.55, -0.55, -0.55, 1.1, -1.0, -1.1, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);
        renderLandingLeg(-0.55, -0.55, 0.55, -1.1, -1.0, 1.1, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);
        renderLandingLeg(0.55, -0.55, 0.55, 1.1, -1.0, 1.1, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);

        // 3. ASCENT STAGE (The upper crew cabin)
        renderBox(-0.45, -0.1, -0.4, 0.45, 0.65, 0.4, C_WHITE_BODY, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);

        // Ascent front face (angular sloped cockpit geometry)
        renderWedge(0.45, -0.1, -0.4, 0.75, 0.65, 0.4, C_BLACK_PANEL, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);

        // 4. CREW WINDOWS (Distinctive triangular/trapezoidal layout facing front)
        renderWindow(0.76, 0.2, -0.2, 0.25, 0.25, C_WINDOW, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);
        renderWindow(0.76, 0.2, 0.2, 0.25, 0.25, C_WINDOW, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer,
                zBuffer);

        // 5. TOP DOCKING PORT & RADAR ANTENNA
        renderCylinder(0.0, 0.72, 0.0, 0.15, 0.15, C_WHITE_BODY, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);
        renderLine(0.3, 0.65, 0.3, 0.5, 0.9, 0.3, C_GEAR_STRUT, 1, 1, 1, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                outputBuffer, zBuffer);
    }

    private void renderLandingLeg(double x1, double y1, double z1, double x2, double y2, double z2, double cosX,
            double sinX, double cosY, double sinY, double lx, double ly, double lz, String[] out, double[] zb) {
        // Main structural strut from body to footpad
        renderLine(x1, y1, z1, x2, y2, z2, C_GEAR_STRUT, 0.0, -1.0, 0.0, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        // Circular footpad at the bottom
        renderDisc(x2, y2, z2, 0.2, 0.0, -1.0, 0.0, C_SILVER_FOIL, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
    }

    private void renderBox(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax, String color,
            double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz, String[] out,
            double[] zb) {
        double step = 0.03;
        for (double x = xMin; x <= xMax; x += step) {
            for (double z = zMin; z <= zMax; z += step) {
                projectPoint(x, yMin, z, 0, -1, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Bottom
                projectPoint(x, yMax, z, 0, 1, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Top
            }
        }
        for (double y = yMin; y <= yMax; y += step) {
            for (double z = zMin; z <= zMax; z += step) {
                projectPoint(xMin, y, z, -1, 0, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Left
                projectPoint(xMax, y, z, 1, 0, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Right
            }
        }
        for (double x = xMin; x <= xMax; x += step) {
            for (double y = yMin; y <= yMax; y += step) {
                projectPoint(x, y, zMin, 0, 0, -1, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Front/Back
                projectPoint(x, y, zMax, 0, 0, 1, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderWedge(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax, String color,
            double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz, String[] out,
            double[] zb) {
        double step = 0.03;
        for (double y = yMin; y <= yMax; y += step) {
            for (double z = zMin; z <= zMax; z += step) {
                double t = (y - yMin) / (yMax - yMin);
                double x = xMin + (xMax - xMin) * (1.0 - t * 0.4);
                projectPoint(x, y, z, 1, 0.5, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderWindow(double cx, double cy, double cz, double height, double widthSize, String color,
            double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz, String[] out,
            double[] zb) {
        for (double dy = -height / 2; dy <= height / 2; dy += 0.04) {
            for (double dz = -widthSize / 2; dz <= widthSize / 2; dz += 0.04) {
                projectPoint(cx, cy + dy, cz + dz, 1, 0, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderCone(double cx, double cy, double cz, double baseRadius, double height, String color,
            double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz, String[] out,
            double[] zb) {
        for (double dy = 0; dy <= height; dy += 0.05) {
            double radius = baseRadius * (1.0 - (dy / height));
            for (int step = 0; step < 16; step++) {
                double rad = step * (2.0 * Math.PI / 16.0);
                double nx = Math.cos(rad);
                double nz = Math.sin(rad);
                projectPoint(cx + radius * nx, cy - dy, cz + radius * nz, nx, -0.5, nz, color, cosX, sinX, cosY, sinY,
                        lx, ly, lz, out, zb);
            }
        }
    }

    private void renderCylinder(double cx, double cy, double cz, double radius, double height, String color,
            double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz, String[] out,
            double[] zb) {
        for (double y = cy - height / 2.0; y <= cy + height / 2.0; y += 0.05) {
            for (int step = 0; step < 16; step++) {
                double rad = step * (2.0 * Math.PI / 16.0);
                double nx = Math.cos(rad);
                double nz = Math.sin(rad);
                projectPoint(cx + radius * nx, y, cz + radius * nz, nx, 0.0, nz, color, cosX, sinX, cosY, sinY, lx, ly,
                        lz, out, zb);
            }
        }
    }

    private void renderDisc(double cx, double cy, double cz, double maxRadius, double nx, double ny, double nz,
            String color, double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz,
            String[] out, double[] zb) {
        for (double r = 0; r <= maxRadius; r += 0.04) {
            int steps = (int) (16 * (r / maxRadius)) + 1;
            for (int step = 0; step < steps; step++) {
                double rad = step * (2.0 * Math.PI / steps);
                double px = cx + r * Math.cos(rad);
                double pz = cz + r * Math.sin(rad);
                projectPoint(px, cy, pz, nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderLine(double x1, double y1, double z1, double x2, double y2, double z2, String color, double nx,
            double ny, double nz, double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz,
            String[] out, double[] zb) {
        double dist = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
        int steps = (int) (dist * 20);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            projectPoint(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t, nx, ny, nz, color, cosX, sinX,
                    cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
            double cosX, double sinX, double cosY, double sinY, double lx, double ly, double lz, String[] out,
            double[] zb) {
        // Step 1: Rotate space around Y-axis (Yaw)
        double r1x = px * cosY - pz * sinY;
        double r1y = py;
        double r1z = px * sinY + pz * cosY;
        // Step 2: Rotate space around X-axis (Pitch)
        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;
        // Step 3: Rotate normals around Y-axis
        double n1x = nx * cosY - nz * sinY;
        double n1y = ny;
        double n1z = nx * sinY + nz * cosY;
        // Step 4: Rotate normals around X-axis
        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;
        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen;
            rotNY /= nLen;
            rotNZ /= nLen;
        }
        // Camera Depth: Now rotZ is your depth vector into the screen distance
        double cameraDepth = rotZ + 2.7;
        if (cameraDepth <= 0.1)
            return;
        double D = 1.0 / cameraDepth;
        // Map 3D X to Screen X, and 3D Y to Screen Y
        // 46.0 and 22.0 scale factors handle terminal character aspect ratio warping
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotY - 3);
        // Minus keeps positive Y going UP on terminal screen
        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;
            if (D > zb[idx]) {
                zb[idx] = D;
                // Basic dot product lighting calculation
                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                double illuminance = Math.max(0.15, dot);
                char[] ramp = { ' ', '.', ':', '-', '=', '+', '*', '#', '%', '@', '█' };
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];
                if (glyph != ' ') {
                    out[idx] = colorCode + glyph + RESET;
                }
            }
        }
    }
}