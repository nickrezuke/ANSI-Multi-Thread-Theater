import java.util.Arrays;
import java.util.Random;

public class SpaceShuttleLoader extends Loader {

    private static final StatusStage[] SHUTTLE_STAGES = {
            new StatusStage(20, "Clearing the launch pad gantry..."),
            new StatusStage(45, "Igniting Solid Rocket Boosters..."),
            new StatusStage(75, "Max-Q structural pressure peak..."),
            new StatusStage(90, "Main engine throttle up..."),
            new StatusStage(100, "Orbital Insertion Successful!")
    };

    private double angleX = 0.2;
    private double angleY = 0.0;
    private int frameTick = 0;

    private final int width;
    private final int height;
    private final Random random = new Random();

    // High-Contrast Authentic NASA Color Palette
    private static final String C_BLACK_TILE = "\u001B[38;2;60;65;75m";   // Thermal Protection Tiles (Wings/Nose)
    private static final String C_WHITE_BODY = "\u001B[38;2;240;240;250m"; // White Orbiter Hull
    private static final String C_TANK_ORANGE= "\u001B[38;2;215;85;25m";  // External Fuel Tank (ET)
    private static final String C_SRB_WHITE  = "\u001B[38;2;200;205;215m"; // Solid Rocket Boosters
    private static final String C_ENGINE_BLUE= "\u001B[38;2;0;210;255m";  // Main Engine Mach Diamonds
    private static final String C_FIRE_ORANGE= "\u001B[38;2;255;120;0m";  // Plume Core Flame
    private static final String C_FIRE_YELLOW= "\u001B[38;2;255;220;50m"; // Exhaust Plume Glow

    public SpaceShuttleLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public SpaceShuttleLoader() {
        super(SHUTTLE_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.3;
        this.angleY = -0.4;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear frame output buffer and reset z-buffer
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE); // Correct min-sorting initialization

        frameTick++;
        angleY += 0.025; // Continuous Yaw Rotation
        angleX = 0.25 + 0.1 * Math.sin(frameTick * 0.03); // Subtle Pitch Oscillations

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Normalized Lighting Direction (Top-Right-Front)
        double lightX = 0.577, lightY = -0.577, lightZ = 0.577;

        // 1. RENDER ORANGE EXTERNAL TANK (ET) - Center Axis [0, 0, -0.45]
        renderCylinder(0.0, 0.0, -0.45, 0.42, 3.2, C_TANK_ORANGE,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Ogive Nose Cone of External Tank
        renderCone(0.0, 1.6, -0.45, 0.42, 0.7, C_TANK_ORANGE,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. RENDER TWIN SOLID ROCKET BOOSTERS (SRBs) - Flanking ET
        double srbOffset = 0.65;
        // Left SRB
        renderCylinder(-srbOffset, -0.2, -0.45, 0.18, 3.4, C_SRB_WHITE,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderCone(-srbOffset, 1.5, -0.45, 0.18, 0.5, C_SRB_WHITE,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // Right SRB
        renderCylinder(srbOffset, -0.2, -0.45, 0.18, 3.4, C_SRB_WHITE,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderCone(srbOffset, 1.5, -0.45, 0.18, 0.5, C_SRB_WHITE,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. RENDER SPACE SHUTTLE ORBITER (Mounted on top of External Tank)
        renderOrbiterFuselage(cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderDeltaWings(cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderVerticalTailFin(cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 4. DYNAMIC EXHAUST THRUSTER PLUMES
        renderThrusterPlume(0.0, -1.8, 0.1, 0.35, C_ENGINE_BLUE, C_FIRE_YELLOW, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        renderThrusterPlume(-srbOffset, -1.9, -0.45, 0.25, C_FIRE_ORANGE, C_FIRE_YELLOW, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        renderThrusterPlume(srbOffset, -1.9, -0.45, 0.25, C_FIRE_ORANGE, C_FIRE_YELLOW, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
    }

    private void renderOrbiterFuselage(double cosX, double sinX, double cosY, double sinY,
                                       double lx, double ly, double lz, String[] out, double[] zb) {
        // Cockpit & Nose (Black nose cap tiles)
        for (double y = 0.8; y <= 1.6; y += 0.08) {
            double alpha = (1.6 - y) / 0.8;
            double radius = 0.32 * Math.sin(alpha * Math.PI / 2.0);
            String color = (y > 1.45) ? C_BLACK_TILE : C_WHITE_BODY;
            renderCircleSlice(0.0, y, 0.1, radius, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
        // Main Payload Bay Cargo Fuselage
        for (double y = -1.2; y <= 0.8; y += 0.08) {
            renderCircleSlice(0.0, y, 0.1, 0.32, C_WHITE_BODY, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void renderDeltaWings(double cosX, double sinX, double cosY, double sinY,
                                  double lx, double ly, double lz, String[] out, double[] zb) {
        // Swept Delta Wings (Black Leading Edge Thermal Tiles)
        for (double y = -1.3; y <= 0.2; y += 0.06) {
            double wingSpan = 1.4 * (0.2 - y) / 1.5;
            for (double x = -wingSpan; x <= wingSpan; x += 0.08) {
                if (Math.abs(x) < 0.25) continue; // Skip inner fuselage overlapping points
                boolean isEdge = Math.abs(x) > (wingSpan - 0.12);
                String color = isEdge ? C_BLACK_TILE : C_WHITE_BODY;
                projectShuttlePoint(x, y, 0.0, 0.0, 0.0, 1.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderVerticalTailFin(double cosX, double sinX, double cosY, double sinY,
                                      double lx, double ly, double lz, String[] out, double[] zb) {
        // Tall Vertical Rudder Stabilizer Fin
        for (double z = 0.2; z <= 0.95; z += 0.05) {
            double heightAlpha = (z - 0.2) / 0.75;
            double yFront = -0.8 - (0.3 * heightAlpha);
            double yBack = -1.4;

            for (double y = yBack; y <= yFront; y += 0.06) {
                projectShuttlePoint(0.0, y, z, 1.0, 0.0, 0.0, C_WHITE_BODY, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderCylinder(double cx, double cy, double cz, double radius, double height, String color,
                                double cosX, double sinX, double cosY, double sinY,
                                double lx, double ly, double lz, String[] out, double[] zb) {
        for (double y = cy - height / 2.0; y <= cy + height / 2.0; y += 0.1) {
            renderCircleSlice(cx, y, cz, radius, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void renderCone(double cx, double cy, double cz, double baseRadius, double height, String color,
                            double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        for (double dy = 0; dy <= height; dy += 0.08) {
            double radius = baseRadius * (1.0 - (dy / height));
            renderCircleSlice(cx, cy + dy, cz, radius, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void renderCircleSlice(double cx, double cy, double cz, double radius, String color,
                                  double cosX, double sinX, double cosY, double sinY,
                                  double lx, double ly, double lz, String[] out, double[] zb) {
        for (int step = 0; step < 20; step++) {
            double rad = step * (2.0 * Math.PI / 20.0);
            double nx = Math.cos(rad);
            double nz = Math.sin(rad);
            double px = cx + radius * nx;
            double pz = cz + radius * nz;

            projectShuttlePoint(px, cy, pz, nx, 0.1, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void renderThrusterPlume(double cx, double cy, double cz, double spread, String coreColor, String glowColor,
                                     double cosX, double sinX, double cosY, double sinY, String[] out, double[] zb) {
        char[] plumeChars = {'*', '@', '#', '%', '+'};
        for (double dy = 0; dy < 1.4; dy += 0.12) {
            double currentSpread = spread * (1.0 + dy * 1.2);
            int particles = (int) (12 * (1.4 - dy));

            for (int p = 0; p < particles; p++) {
                double rx = cx + (random.nextDouble() - 0.5) * currentSpread;
                double rz = cz + (random.nextDouble() - 0.5) * currentSpread;
                double ry = cy - dy;

                String color = (dy < 0.4) ? coreColor : glowColor;
                char symbol = plumeChars[random.nextInt(plumeChars.length)];

                projectRawParticle(rx, ry, rz, color, symbol, cosX, sinX, cosY, sinY, out, zb);
            }
        }
    }

    private void projectShuttlePoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                                    double cosX, double sinX, double cosY, double sinY,
                                    double lx, double ly, double lz, String[] out, double[] zb) {
        // 1. Apply World Rotations (Yaw around Y-axis, then Pitch around X-axis)
        double r1x = px * cosY - py * sinY;
        double r1y = px * sinY + py * cosY;
        double r1z = pz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // Rotate Normal Vectors
        double n1x = nx * cosY - ny * sinY;
        double n1y = nx * sinY + ny * cosY;
        double n1z = nz;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        // Normalization
        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen;
        }

        // 2. Perspective Projection & Depth Calculations
        double cameraDepth = rotY + 4.8;
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotZ);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            // Z-Buffer Max-Sorting (Closest geometry wins)
            if (D > zb[idx]) {
                zb[idx] = D;

                // Lighting Exposure Calculation
                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                double illuminance = Math.max(0.25, dot); // Baseline ambient light

                // ASCII Luminance Ramp Selection
                char[] ramp = {' ', '.', ':', '-', '=', '+', '*', '#', '%', '@', '█'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];

                if (glyph != ' ') {
                    out[idx] = colorCode + glyph + RESET;
                }
            }
        }
    }

    private void projectRawParticle(double px, double py, double pz, String colorCode, char symbol,
                                    double cosX, double sinX, double cosY, double sinY, String[] out, double[] zb) {
        double r1x = px * cosY - py * sinY;
        double r1y = px * sinY + py * cosY;
        double r1z = pz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        double cameraDepth = rotY + 4.8;
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotZ);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;
            if (D > zb[idx]) {
                zb[idx] = D;
                out[idx] = colorCode + symbol + RESET;
            }
        }
    }
}