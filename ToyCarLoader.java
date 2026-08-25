// TODO: Increase size of the passengar area / window chasis
// TODO: Is the windshield floating just above the other surface? I think i can see between them...

public class ToyCarLoader extends Loader {

    private static final StatusStage[] CAR_STAGES = {
            new StatusStage(20, "Molding bright yellow plastic body:"),
            new StatusStage(45, "Stamping dark front grille & bumpers:"),
            new StatusStage(65, "Fitting tinted windshield glass:"),
            new StatusStage(85, "Mounting 4 rubber tires & white hubcaps:"),
            new StatusStage(100, "Vroom vroom! Ready to roll.")
    };

    private static final String LUMINANCE_CHARS = "#%@$&WM#O";

    private String yellowBody;
    private String blackTrim;
    private String glassColor;
    private String whiteDetail;
    private String redTailLight;
    private String[][] cellCache;

    private double A = 1.7; // Pitch tilt
    private double B = 0.8; // Yaw spin

    public ToyCarLoader() {
        super(CAR_STAGES, 80, 22);
    }

    public ToyCarLoader(int w, int h) {
        super(CAR_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        yellowBody = "\u001B[38;5;220m";   // Bright Toy Yellow
        blackTrim  = "\u001B[38;5;236m";   // Bumper, Grille, Tires
        glassColor = "\u001B[38;5;67m";    // Tinted Window Glass
        whiteDetail= "\u001B[38;5;255m";   // Headlights, Hubcaps
        redTailLight= "\u001B[38;5;196m";  // Rear Tail Lights

        String[] fullPalette = { yellowBody, blackTrim, glassColor, whiteDetail, redTailLight };
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
        // 1. CAR BODY (Yellow Plastic Chassis & Hood)
        // ==========================================
        double bodyLength = 3.8;
        double bodyWidth  = 1.8;

        for (double x = -bodyLength / 2; x <= bodyLength / 2; x += 0.04) {
            double normX = x / (bodyLength / 2.0); // -1.0 (rear) to 1.0 (front)

            // Curved front/rear taper
            double widthFactor = 1.0 - 0.25 * Math.pow(normX, 4);
            double currentWidth = (bodyWidth / 2.0) * widthFactor;

            // Hood/Trunk height envelope (sloped nose at front)
            double heightTop = (normX > 0.2) ? 0.25 - 0.2 * (normX - 0.2) : 0.25;
            double heightBottom = -0.3;

            for (double y = -currentWidth; y <= currentWidth; y += 0.04) {
                // Top Deck (Hood and Trunk surfaces)
                drawPoint(x, y, heightTop, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
                // Underbelly
                drawPoint(x, y, heightBottom, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }

            // Side Flanks (Fenders)
            for (double z = heightBottom; z <= heightTop; z += 0.04) {
                drawPoint(x, -currentWidth, z, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
                drawPoint(x,  currentWidth, z, 0.0,  1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // ==========================================
        // 2. CABIN & TINTED WINDOWS (Roof Bubble)
        // ==========================================
        double cabinStartX = -0.9, cabinEndX = 0.4;
        double cabinW = 1.4, cabinH = 0.75;

        for (double cx = cabinStartX; cx <= cabinEndX; cx += 0.04) {
            double t = (cx - cabinStartX) / (cabinEndX - cabinStartX);
            // Sloped roof bubble curve
            double currentRoofZ = 0.25 + cabinH * Math.sin(t * Math.PI * 0.85 + 0.25);

            for (double cy = -cabinW / 2; cy <= cabinW / 2; cy += 0.04) {
                // Roof Surface (Yellow)
                drawPoint(cx, cy, currentRoofZ, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }

            // Side Glass Windows
            for (double cz = 0.3; cz < currentRoofZ - 0.05; cz += 0.04) {
                drawPoint(cx, -cabinW / 2, cz, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                drawPoint(cx,  cabinW / 2, cz, 0.0,  1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            }
        }

        // Front Windshield (Angled Glass)
        for (double wy = -cabinW / 2 + 0.05; wy <= cabinW / 2 - 0.05; wy += 0.04) {
            for (double step = 0; step <= 1.0; step += 0.08) {
                double wx = cabinEndX + step * 0.35;
                double wz = 0.8 - step * 0.55;
                drawPoint(wx, wy, wz, 1.0, 0.0, 0.5, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            }
        }

        // ==========================================
        // 3. FRONT GRILLE & BUMPERS
        // ==========================================
        // Front Mesh Grille
        for (double gy = -0.45; gy <= 0.45; gy += 0.03) {
            for (double gz = -0.15; gz <= 0.15; gz += 0.03) {
                drawPoint(1.92, gy, gz, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }

        // Front Bumper Bar
        for (double by = -1.0; by <= 1.0; by += 0.04) {
            for (double bz = -0.32; bz <= -0.12; bz += 0.04) {
                drawPoint(2.0, by, bz, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }

        // Rear Bumper Bar
        for (double by = -0.95; by <= 0.95; by += 0.04) {
            for (double bz = -0.32; bz <= -0.12; bz += 0.04) {
                drawPoint(-1.95, by, bz, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }

        // ==========================================
        // 4. HEADLIGHTS & TAIL LIGHTS
        // ==========================================
        // Round Headlight Pods (Front)
        for (double side : new double[]{-0.65, 0.65}) {
            for (double hr = 0; hr <= 0.22; hr += 0.03) {
                for (double ha = 0; ha < 2 * Math.PI; ha += 0.3) {
                    double hy = side + hr * Math.cos(ha);
                    double hz = 0.08 + hr * Math.sin(ha);
                    drawPoint(1.91, hy, hz, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3);
                }
            }
        }

        // Tail Lights (Rear Red Oval)
        for (double side : new double[]{-0.7, 0.7}) {
            for (double ry = side - 0.12; ry <= side + 0.12; ry += 0.03) {
                for (double rz = 0.0; rz <= 0.18; rz += 0.03) {
                    drawPoint(-1.91, ry, rz, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 4);
                }
            }
        }

        // ==========================================
        // 5. FOUR WHEELS & HUBCAPS
        // ==========================================
        double[] wheelX = {1.1, 1.1, -1.1, -1.1};
        double[] wheelY = {1.0, -1.0, 1.1, -1.1};
        double wheelRadius = 0.42;

        for (int i = 0; i < 4; i++) {
            double wx = wheelX[i];
            double wy = wheelY[i];
            double sideSign = Math.signum(wy);

            // Tire Rubber Tread Cylinder
            for (double wr = 0; wr <= wheelRadius; wr += 0.04) {
                for (double wa = 0; wa < 2 * Math.PI; wa += 0.2) {
                    double px = wx + wr * Math.cos(wa);
                    double pz = -0.25 + wr * Math.sin(wa);

                    for (double thickness = 0; thickness <= 0.2; thickness += 0.04) {
                        double py = wy + (thickness * sideSign);
                        // Black Rubber Tire (1) or White Center Hubcap (3)
                        int colorIdx = (wr < 0.22 && thickness > 0.1) ? 3 : 1;
                        drawPoint(px, py, pz, 0.0, sideSign, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, colorIdx);
                    }
                }
            }
        }

        // Dynamic Rotation
        A += 0.0008 * Math.sin(B);
        B += 0.022;
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz, double sinA, double cosA,
            double sinB, double cosB, String[] outputBuffer, double[] zBuffer, int colorIndex) {
        double x1 = x * cosB - y * sinB;
        double y1 = x * sinB + y * cosB;
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;
        double distance = 3.5;
        double ooZ = 1.0 / (z2 + distance);
        int xp = (int) (window_width / 2.0 + 38 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 17 * ooZ * y2);
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