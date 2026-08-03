public class ThreeBodyLoader extends Loader {
    private static final StatusStage[] STAGES = {
            new StatusStage(25, "Deploying asymmetric stellar bodies:"),
            new StatusStage(55, "Calculating mutual gravitational pull:"),
            new StatusStage(80, "Mapping chaotic n-body trajectory:"),
            new StatusStage(100, "Gravity Simulation Operational!")
    };

    // 1. Core Physics Parameters
    private static final double G = 1.2; // Gravitational Constant
    private static final double DT = 0.004; // Small step size to prevent numerical escape
    private static final int MAX_POINTS = 180; // History path length per body
    private static final double SOFTENING = 0.15; // Prevents infinite force / extreme slingshots during close passes

    // 3 Unique Masses to ensure structural asymmetry
    private static final double[] MASS = { 1.5, 1.0, 0.7 };

    // Asymmetric, non-repeating Initial 3D Positions
    private double[][] pos = {
            { -0.6, 0.5, 0.1 }, // Body 1 (Heavy)
            { 0.5, -0.4, -0.2 }, // Body 2 (Medium)
            { 0.0, 0.1, 0.3 } // Body 3 (Light)
    };

    // Off-balance velocity vectors ensuring ongoing chaotic interaction
    private double[][] vel = {
            { 0.3, -0.2, 0.1 }, // Body 1
            { -0.4, 0.5, -0.1 }, // Body 2
            { 0.1, -0.3, -0.2 } // Body 3
    };

    // Circular ring buffers tracking historical positions for trail rendering
    private final double[][][] historyXyz = new double[3][MAX_POINTS][3];
    private int historyIndex = 0;
    private int activePointsCount = 0;
    private double angle = 0.0;

    // Real-time distance telemetry variables for the dashboard
    private double dist12 = 0.0;
    private double dist23 = 0.0;
    private double dist31 = 0.0;

    public ThreeBodyLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        activePointsCount = 0;
        historyIndex = 0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int renderWidth = 54;
        int totalWidth = 80;
        int height = 22;

        // 2. Physics Simulation: Standard Newtonian Gravity
        // Sub-stepping improves mathematical stability of the differential equations
        // per frame
        for (int step = 0; step < 6; step++) {
            double[][] acc = new double[3][3];

            // Calculate precise mutual distance vectors and accumulations
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (i == j)
                        continue;
                    double dx = pos[j][0] - pos[i][0];
                    double dy = pos[j][1] - pos[i][1];
                    double dz = pos[j][2] - pos[i][2];

                    double distSq = dx * dx + dy * dy + dz * dz;

                    // Track distances for current telemetry presentation
                    if (step == 0) {
                        if ((i == 0 && j == 1) || (i == 1 && j == 0))
                            dist12 = Math.sqrt(distSq);
                        if ((i == 1 && j == 2) || (i == 2 && j == 1))
                            dist23 = Math.sqrt(distSq);
                        if ((i == 2 && j == 0) || (i == 0 && j == 2))
                            dist31 = Math.sqrt(distSq);
                    }

                    // Introduce physics softening to simulate physical volume and avoid
                    // division-by-zero explosions
                    double forceMag = (G * MASS[j]) / Math.pow(distSq + SOFTENING, 1.5);

                    acc[i][0] += forceMag * dx;
                    acc[i][1] += forceMag * dy;
                    acc[i][2] += forceMag * dz;
                }
            }

            // Apply standard Symplectic Euler updates (Velocity -> Position)
            for (int i = 0; i < 3; i++) {
                vel[i][0] += acc[i][0] * DT;
                vel[i][1] += acc[i][1] * DT;
                vel[i][2] += acc[i][2] * DT;

                pos[i][0] += vel[i][0] * DT;
                pos[i][1] += vel[i][1] * DT;
                pos[i][2] += vel[i][2] * DT;

                // Log raw coordinates mapped for visual framing scaling
                historyXyz[i][historyIndex][0] = pos[i][0] * 1.1;
                historyXyz[i][historyIndex][1] = pos[i][1] * 1.1;
                historyXyz[i][historyIndex][2] = pos[i][2] * 1.1;
            }

            historyIndex = (historyIndex + 1) % MAX_POINTS;
            if (activePointsCount < MAX_POINTS)
                activePointsCount++;
        }

        // 3. Coordinate Perspective Transformations (3D Orbital Rotation)
        double rX = 0.4; // Fixed tilt to view the full plane interaction clearly
        double rY = angle * 0.15;
        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);

        // 4. Trail and Body Point Buffer Processing
        for (int body = 0; body < 3; body++) {
            for (int i = 0; i < activePointsCount; i++) {
                int lookupIdx = (historyIndex - activePointsCount + i + MAX_POINTS) % MAX_POINTS;
                double cx = historyXyz[body][lookupIdx][0];
                double cy = historyXyz[body][lookupIdx][1];
                double cz = historyXyz[body][lookupIdx][2];

                // 3D rotation formulas
                double y1 = cy * cosX - cz * sinX;
                double z1 = cy * sinX + cz * cosX;
                double x2 = cx * cosY + z1 * sinY;
                double z2 = -cx * sinY + z1 * cosY;

                double distanceToCamera = 3.0;
                double ooz = 1.0 / (z2 + distanceToCamera);

                int projX = (int) (26 + 32 * ooz * 1.6 * x2);
                int projY = (int) (11 - 18 * ooz * y1);

                if (projX >= 0 && projX < renderWidth && projY >= 0 && projY < height) {
                    int o = projX + totalWidth * projY;
                    if (ooz > zBuffer[o]) {
                        zBuffer[o] = ooz;

                        double ageFactor = (double) i / activePointsCount;
                        String colorCode;
                        char trailChar;

                        if (ageFactor > 0.96) {
                            colorCode = "\u001B[38;5;255m"; // Active stellar mass center core
                            trailChar = (body == 0) ? 'A' : (body == 1) ? 'B' : 'C';
                        } else {
                            if (body == 0) {
                                colorCode = ageFactor > 0.5 ? "\u001B[38;5;220m" : "\u001B[38;5;130m"; // Body A
                                                                                                       // (Yellow/Orange)
                                trailChar = 'o';
                            } else if (body == 1) {
                                colorCode = ageFactor > 0.5 ? "\u001B[38;5;81m" : "\u001B[38;5;25m"; // Body B
                                                                                                     // (Cyan/Blue)
                                trailChar = '·';
                            } else {
                                colorCode = ageFactor > 0.5 ? "\u001B[38;5;201m" : "\u001B[38;5;90m"; // Body C
                                                                                                      // (Pink/Purple)
                                trailChar = '·';
                            }
                        }
                        outputBuffer[o] = colorCode + trailChar + RESET;
                    }
                }
            }
        }

        // 5. Live Telemetry Presentation Panel Layout (Columns 56-79)
        String gray = "\u001B[38;5;244m";
        String yellow = "\u001B[38;5;220m";
        String cyan = "\u001B[38;5;81m";
        String pink = "\u001B[38;5;201m";
        String green = "\u001B[38;5;112m";
        String white = "\u001B[38;5;255m";

        drawDashboardLine(outputBuffer, 0, "                        ", green, white);
        drawDashboardLine(outputBuffer, 1, "┌──────────────────────┐", white, white);
        drawDashboardLine(outputBuffer, 2, "│ NEWTONIAN N-BODY SYS │", white, white);
        drawDashboardLine(outputBuffer, 3, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 4, String.format("│ %sMASS A:  %s%6.2f      │", gray, yellow, MASS[0]), gray,
                white);
        drawDashboardLine(outputBuffer, 5, String.format("│ %sMASS B:  %s%6.2f      │", gray, cyan, MASS[1]), gray,
                white);
        drawDashboardLine(outputBuffer, 6, String.format("│ %sMASS C:  %s%6.2f      │", gray, pink, MASS[2]), gray,
                white);
        drawDashboardLine(outputBuffer, 7, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 8, "│ INTER-STELLAR DIST   │", white, white);
        drawDashboardLine(outputBuffer, 9, String.format("│ %sr (A-B): %s%6.3f      │", gray, green, dist12), gray,
                white);
        drawDashboardLine(outputBuffer, 10, String.format("│ %sr (B-C): %s%6.3f      │", gray, green, dist23), gray,
                white);
        drawDashboardLine(outputBuffer, 11, String.format("│ %sr (C-A): %s%6.3f      │", gray, green, dist31), gray,
                white);
        drawDashboardLine(outputBuffer, 12, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 13, "│ STATS & CONFIG       │", white, white);
        drawDashboardLine(outputBuffer, 14, String.format("│ %sCONST G: %s%6.2f      │", gray, green, G), gray, white);
        drawDashboardLine(outputBuffer, 15, String.format("│ %sSOFTEN:  %s%6.2f      │", gray, green, SOFTENING), gray,
                white);
        drawDashboardLine(outputBuffer, 16, "└──────────────────────┘", white, white);

        for (int row = 17; row < 22; row++) {
            drawDashboardLine(outputBuffer, row, "                        ", RESET, RESET);
        }
        angle += 0.03;
    }

    private void drawDashboardLine(String[] outputBuffer, int row, String content, String textColor, String wallColor) {
        int targetColumn = 56;
        int rowStartIdx = row * 80;
        String cleanContent = content.replaceAll("\u001B\\[[;\\d]*m", "");
        int visualIndex = 0;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\u001B') {
                int endM = content.indexOf('m', i);
                if (endM != -1) {
                    i = endM;
                    continue;
                }
            }
            int currentCellX = targetColumn + visualIndex;
            if (currentCellX >= 80)
                break;
            int targetBufferIndex = rowStartIdx + currentCellX;
            String activeColor = (ch == '│' || ch == '┌' || ch == '┐' || ch == '├' || ch == '┤' || ch == '└'
                    || ch == '┘' || ch == '─') ? wallColor : textColor;
            if (Character.isDigit(ch) || ch == '.' || ch == '-' || ch == '+') {
                if (!cleanContent.contains("N-BODY") && visualIndex > 2 && visualIndex < 20) {
                    // Retain custom dashboard color identities if passed explicitly in textColor
                    // parameters
                    if (!textColor.equals("\u001B[38;5;220m") && !textColor.equals("\u001B[38;5;81m")
                            && !textColor.equals("\u001B[38;5;201m")) {
                        activeColor = "\u001B[38;5;112m";
                    } else {
                        activeColor = textColor;
                    }
                }
            }
            outputBuffer[targetBufferIndex] = activeColor + ch + RESET;
            visualIndex++;
        }
    }
}