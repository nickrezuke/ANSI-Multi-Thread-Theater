

public class ToyTrainLoader extends Loader {

    private static final StatusStage[] TRAIN_STAGES = {
            new StatusStage(20, "Casting classic red boiler & chassis:"),
            new StatusStage(40, "Fitting flared smokestack & steam domes:"),
            new StatusStage(60, "Building engineer's cab & curved roof:"),
            new StatusStage(80, "Attaching sloped cowcatcher & drive wheels:"),
            new StatusStage(100, "All aboard! Express locomotive ready to roll.")
    };

    private static final String LUMINANCE_CHARS = "#%@$&WM#O";

    private String trainRed;
    private String trainBlack;
    private String trainGold;
    private String trainSilver;
    private String[][] cellCache;

    private double A = 1.8; // Pitch angle
    private double B = 2.0; // Yaw angle

    public ToyTrainLoader() {
        super(TRAIN_STAGES, 80, 22);
    }

    public ToyTrainLoader(int w, int h) {
        super(TRAIN_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        trainRed    = "\u001B[38;5;196m"; // Express Locomotive Red
        trainBlack  = "\u001B[38;5;235m"; // Chassis, Roof & Smokestack Stem
        trainGold   = "\u001B[38;5;220m"; // Boiler Trim Bands & Brass Accents
        trainSilver = "\u001B[38;5;255m"; // Cowcatcher Slats & Window Glass

        String[] fullPalette = { trainRed, trainBlack, trainGold, trainSilver };
        cellCache = new String[fullPalette.length][LUMINANCE_CHARS.length()];
        for (int c = 0; c < fullPalette.length; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = fullPalette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinB = Math.sin(B), cosB = Math.cos(B);

        // ==========================================
        // 1. BLACK CHASSIS BASE FRAME
        // ==========================================
        for (double x = -2.2; x <= 1.2; x += 0.05) {
            for (double y = -0.85; y <= 0.85; y += 0.05) {
                for (double z = -0.45; z <= -0.2; z += 0.05) {
                    drawPoint(x, y, z, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
                }
            }
        }

        // ==========================================
        // 2. RED BOILER CYLINDER & GOLD TRIM RINGS
        // ==========================================
        double boilerRadius = 0.62;
        double boilerCenterZ = 0.35;

        for (double bx = -0.8; bx <= 1.1; bx += 0.03) {
            // Determine if this slice is a Gold accent band
            boolean isGoldBand = (Math.abs(bx - (-0.4)) < 0.04 || 
                                  Math.abs(bx - 0.1) < 0.04 || 
                                  Math.abs(bx - 0.6) < 0.04);

            int colorIdx = isGoldBand ? 2 : 0;

            for (double angle = 0; angle < 2 * Math.PI; angle += 0.15) {
                double ny = Math.cos(angle);
                double nz = Math.sin(angle);
                double by = boilerRadius * ny;
                double bz = boilerCenterZ + boilerRadius * nz;

                drawPoint(bx, by, bz, 0.0, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, colorIdx);
            }
        }

        // Front Face of Boiler (Black disc plate)
        for (double r = 0; r <= boilerRadius; r += 0.04) {
            for (double angle = 0; angle < 2 * Math.PI; angle += 0.2) {
                double fy = r * Math.cos(angle);
                double fz = boilerCenterZ + r * Math.sin(angle);
                drawPoint(1.1, fy, fz, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }

        // ==========================================
        // 3. ENGINEER'S CAB & OVERHANGING ROOF
        // ==========================================
        double cabStartX = -2.1, cabEndX = -0.8;
        double cabW = 1.6;
        double cabTopZ = 0.95;

        // Cab Outer Shell (Red)
        for (double cx = cabStartX; cx <= cabEndX; cx += 0.05) {
            for (double cz = -0.2; cz <= cabTopZ; cz += 0.04) {
                // Side Walls
                drawPoint(cx, -cabW / 2, cz, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
                drawPoint(cx,  cabW / 2, cz, 0.0,  1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }
        // Rear Wall of Cab
        for (double cy = -cabW / 2; cy <= cabW / 2; cy += 0.02) {
            for (double cz = -0.2; cz <= cabTopZ + 0.072; cz += 0.04) { // Extra bit to cover the gap
                drawPoint(cabStartX, cy, cz, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // Side Glass Windows (Silver panes inset into cab sides)
        for (double cx = -1.7; cx <= -1.0; cx += 0.02) {
            for (double cz = 0.35; cz <= 0.75; cz += 0.02) {
                drawPoint(cx, -cabW / 2 - 0.01, cz, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3);
                drawPoint(cx,  cabW / 2 + 0.01, cz, 0.0,  1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3);
            }
        }

        // Curved Overhanging Roof (Black)
        for (double rx = cabStartX - 0.1; rx <= cabEndX + 0.1; rx += 0.04) {
            for (double ry = -cabW / 2 - 0.1; ry <= cabW / 2 + 0.1; ry += 0.04) {
                double normY = ry / (cabW / 2.0 + 0.1);
                double roofArcZ = cabTopZ + 0.2 * (1.0 - normY * normY);
                drawPoint(rx, ry, roofArcZ, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }

        // ==========================================
        // 4. SMOKESTACK & STEAM DOMES
        // ==========================================
        // Flared Red/Black Smokestack (Front)
        double stackX = 0.7;
        for (double sz = 0.95; sz <= 1.75; sz += 0.04) {
            // Flared funnel at the top
            double t = (sz - 0.95) / 0.8;
            double sRadius = (sz > 1.35) ? 0.2 + 0.28 * (t - 0.5) : 0.2;
            int sColor = (sz > 1.35) ? 0 : 1; // Red funnel top, black stem

            for (double sa = 0; sa < 2 * Math.PI; sa += 0.2) {
                double sx = stackX + sRadius * Math.cos(sa);
                double sy = sRadius * Math.sin(sa);
                drawPoint(sx, sy, sz, Math.cos(sa), Math.sin(sa), 0.2, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, sColor);
            }
        }

        // Steam & Sand Domes (Middle Brass/Black Domes)
        double[] domeX = {0.1, -0.3};
        for (double dx : domeX) {
            for (double dz = 0.95; dz <= 1.25; dz += 0.04) {
                double dRadius = 0.18 * (1.0 - 0.3 * (dz - 0.95));
                for (double da = 0; da < 2 * Math.PI; da += 0.25) {
                    double dy = dRadius * Math.sin(da);
                    double dpx = dx + dRadius * Math.cos(da);
                    drawPoint(dpx, dy, dz, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                }
            }
        }

        // ==========================================
        // 5. HEADLIGHTS & COW CATCHER
        // ==========================================
        // Main Center Headlight
        for (double hr = 0; hr <= 0.22; hr += 0.03) {
            for (double ha = 0; ha < 2 * Math.PI; ha += 0.25) {
                double hy = hr * Math.cos(ha);
                double hz = boilerCenterZ + hr * Math.sin(ha);
                drawPoint(1.18, hy, hz, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3);
            }
        }

        // Sloped Cowcatcher (Front Cattle Pusher Grill)
        for (double step = 0; step <= 1.0; step += 0.05) {
            double cx = 1.1 + step * 0.7;
            double cz = -0.1 - step * 0.35;
            double currentSpread = 0.85 * (1.0 - step * 0.2);

            for (double cy = -currentSpread; cy <= currentSpread; cy += 0.06) {
                // Alternating silver grill slats
                int slatColor = ((int)(cy * 20) % 2 == 0) ? 3 : 1;
                drawPoint(cx, cy, cz, 1.0, 0.0, -0.5, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, slatColor);
            }
        }

        // ==========================================
        // 6. WHEELS & SIDE CONNECTING RODS
        // ==========================================
        double[] wheelPositionsX = {-1.4, -0.5, 0.4};
        double wheelR = 0.38;

        for (double wx : wheelPositionsX) {
            for (double side : new double[]{-0.88, 0.88}) {
                double sideSign = Math.signum(side);

                // Wheel Disc (Black)
                for (double wr = 0; wr <= wheelR; wr += 0.04) {
                    for (double wa = 0; wa < 2 * Math.PI; wa += 0.2) {
                        double px = wx + wr * Math.cos(wa);
                        double pz = -0.45 + wr * Math.sin(wa);
                        drawPoint(px, side, pz, 0.0, sideSign, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
                    }
                }
            }
        }

        // Red Side Coupling Rods (Connecting wheel centers)
        // Red Side Coupling Rods (Simulating a rotating eccentric crank/driving rod)
        // We use a time-varying phase angle based on B
        double crankRadius = 0.15; // The radius of the crank pin rotation around the wheel center
        double crankAngle = B * 20.0; // Speed multiplier for the wheel rotation
        
        double offsetX = crankRadius * -Math.cos(crankAngle);
        double offsetZ = crankRadius * Math.sin(crankAngle);

        // Instead of rendering a static horizontal bar, we trace the rod 
        // offset dynamically by (offsetX, offsetZ) per longitudinal slice, 
        // or apply the circular displacement to the whole bar assembly.
        for (double rx = -1.45; rx <= 0.45; rx += 0.03) {
            for (double rz = -0.48; rz <= -0.42; rz += 0.03) {
                // Apply the sin/cos offsets to make the rod pivot/revolve
                double dynamicZ = rz + offsetZ;
                double dynamicX = rx + offsetX * 1.2; // slight longitudinal shift if desired

                drawPoint(dynamicX, -0.93, dynamicZ, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
                drawPoint(dynamicX,  0.93, dynamicZ, 0.0,  1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // Dynamic Continuous Rotation
        A += 0.002 * Math.sin(B);
        B += 0.012;
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz, double sinA, double cosA,
            double sinB, double cosB, String[] outputBuffer, double[] zBuffer, int colorIndex) {
        double x1 = x * cosB - y * sinB;
        double y1 = x * sinB + y * cosB;
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;
        double distance = 3.7;
        double ooZ = 1.0 / (z2 + distance);
        int xp = (int) (window_width / 2.0 + 36 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 16 * ooZ * y2);
        int bufferIndex = xp + window_width * yp;

        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            double nx1 = nx * cosB - ny * sinB;
            double ny1 = nx * sinB + ny * cosB;
            double ny2 = ny1 * cosA - nz * sinA;
            double nz2 = ny1 * sinA + nz * cosA;
            double nx2 = nx1;
            double luminance = nx2 * 0.3 + ny2 * 0.3 + nz2 * 0.9;

            if (ooZ > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooZ;
                int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));
                outputBuffer[bufferIndex] = cellCache[colorIndex][charIndex];
            }
        }
    }
}