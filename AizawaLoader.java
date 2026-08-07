public class AizawaLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Seeding spherical chaos:"),
        new StatusStage(55, "Calculating Aizawa curves:"),
        new StatusStage(80, "Trapping occult geometry:"),
        new StatusStage(100, "Vortex Instability Contained!")
    };

    private static final double A = 0.95;
    private static final double B = 0.7;
    private static final double C = 0.6;
    private static final double D = 3.5;
    private static final double E = 0.25;
    private static final double F = 0.1;
    private static final double DT = 0.010; // Precision step size

    private double lx = 0.1, ly = 0.5, lz = 0.1;
    private double vX = 0, vY = 0, vZ = 0;

    private static final int MAX_POINTS = 650;
    private final double[][] historyXyz = new double[MAX_POINTS][3];
    private int historyIndex = 0;
    private int activePointsCount = 0;
    private double angle = 0.0;

    public AizawaLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        activePointsCount = 0;
        historyIndex = 0;
        this.lx = 0.1;
        this.ly = 0.5;
        this.lz = 0.1;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int renderWidth = 54;
        int totalWidth = 80;
        int height = 22;

        for (int step = 0; step < 6; step++) {
            double dx = ((lz - B) * lx - D * ly) * DT;
            double dy = (D * lx + (lz - B) * ly) * DT;
            double dz = (C + A * lz - (Math.pow(lz, 3) / 3.0) - (Math.pow(lx, 2) + Math.pow(ly, 2)) * (1.0 + E * lz) + F * lz * Math.pow(lx, 3)) * DT;

            vX = dx / DT;
            vY = dy / DT;
            vZ = dz / DT;

            lx += dx;
            ly += dy;
            lz += dz;

            historyXyz[historyIndex][0] = lx * 0.45;
            historyXyz[historyIndex][1] = ly * 0.45;
            historyXyz[historyIndex][2] = (lz - 0.8) * 0.45;

            historyIndex = (historyIndex + 1) % MAX_POINTS;
            if (activePointsCount < MAX_POINTS) activePointsCount++;
        }

        angle += 0.015;
        double rX = angle * 0.25;
        double rY = angle * 0.38;
        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);

        for (int i = 0; i < activePointsCount; i++) {
            int lookupIdx = (historyIndex - activePointsCount + i + MAX_POINTS) % MAX_POINTS;
            double cx = historyXyz[lookupIdx][0];
            double cy = historyXyz[lookupIdx][1];
            double cz = historyXyz[lookupIdx][2];

            double y1 = cy * cosX - cz * sinX;
            double z1 = cy * sinX + cz * cosX;
            double x2 = cx * cosY + z1 * sinY;
            double z2 = -cx * sinY + z1 * cosY;

            double distanceToCamera = 1.8;
            double ooz = 1.0 / (z2 + distanceToCamera);

            int projX = (int) (26 + 32 * ooz * 1.8 * x2);
            int projY = (int) (11 - 18 * ooz * y1);

            if (projX >= 0 && projX < renderWidth && projY >= 0 && projY < height) {
                int o = projX + totalWidth * projY;
                
                if (ooz > zBuffer[o]) {
                    zBuffer[o] = ooz;
                    double ageFactor = (double) i / activePointsCount;
                    
                    String colorCode;
                    char trailChar;

                    if (ageFactor > 0.94) {
                        colorCode = "\u001B[38;5;255m";  trailChar = '@';
                    } else if (ageFactor > 0.75) {
                        colorCode = "\u001B[38;5;41m";   trailChar = '*';
                    } else if (ageFactor > 0.45) {
                        colorCode = "\u001B[38;5;28m";  trailChar = '+';
                    } else if (ageFactor > 0.15) {
                        colorCode = "\u001B[38;5;22m";   trailChar = '.';
                    } else {
                        colorCode = "\u001B[38;5;232m";   trailChar = ',';
                    }
                    outputBuffer[o] = colorCode + trailChar + RESET;
                }
            }
        }

        String gray = "\u001B[38;5;244m";
        String green = "\u001B[38;5;112m";
        String white = "\u001B[38;5;255m";

        drawDashboardLine(outputBuffer, 2, "┌──────────────────────┐", white, white);
        drawDashboardLine(outputBuffer, 3, "│ AIZAWA CHAOS TRACKER │", white, white);
        drawDashboardLine(outputBuffer, 4, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 5, String.format("│   %sSPIN-A: %s%+6.1f     │", gray, green, A), gray, white);
        drawDashboardLine(outputBuffer, 6, String.format("│   %sDRAG-B: %s%+6.1f     │", gray, green, B), gray, white);
        drawDashboardLine(outputBuffer, 7, String.format("│  %sSLOPE-C: %s%+6.1f     │", gray, green, C), gray, white);
        drawDashboardLine(outputBuffer, 8, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 9, "│ COCCULT MATH NO_DES  │", white, white);
        drawDashboardLine(outputBuffer, 10, String.format("│      %sX:  %s%+7.1f     │", gray, green, lx), gray, white);
        drawDashboardLine(outputBuffer, 11, String.format("│      %sY:  %s%+7.1f     │", gray, green, ly), gray, white);
        drawDashboardLine(outputBuffer, 12, String.format("│      %sZ:  %s%+7.1f     │", gray, green, lz), gray, white);
        drawDashboardLine(outputBuffer, 13, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 14, "│ TORMENT FLOW VECTOR  │", white, white);
        drawDashboardLine(outputBuffer, 15, String.format("│     %sdX:  %s%+7.1f     │", gray, green, vX), gray, white);
        drawDashboardLine(outputBuffer, 16, String.format("│     %sdY:  %s%+7.1f     │", gray, green, vY), gray, white);
        drawDashboardLine(outputBuffer, 17, String.format("│     %sdZ:  %s%+7.1f     │", gray, green, vZ), gray, white);
        drawDashboardLine(outputBuffer, 18, "└──────────────────────┘", white, white);

        for (int row = 19; row < 22; row++) {
            drawDashboardLine(outputBuffer, row, "                        ", RESET, RESET);
        }
    }

    private void drawDashboardLine(String[] outputBuffer, int row, String content, String textColor, String wallColor) {
        int targetColumn = 56;
        int rowStartIdx = row * 80;
        
        // Strip color codes to extract clean layout alignment offsets
        String cleanContent = content.replaceAll("\u001B\\[[;\\d]*m", "");
        int visualIndex = 0;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            
            // Catch and handle ANSI tags dynamically without incrementing visual position
            if (ch == '\u001B') {
                int endM = content.indexOf('m', i);
                if (endM != -1) {
                    // Inject the raw ANSI sequence directly to the active cell, but don't count it as a column space
                    int currentCellX = targetColumn + visualIndex;
                    if (currentCellX < 80) {
                        outputBuffer[rowStartIdx + currentCellX] = content.substring(i, endM + 1);
                    }
                    i = endM; 
                    continue;
                }
            }

            int currentCellX = targetColumn + visualIndex;
            if (currentCellX >= 80) break;
            int targetBufferIndex = rowStartIdx + currentCellX;

            String activeColor = (ch == '│' || ch == '┌' || ch == '┐' || ch == '├' || ch == '┤' || ch == '└' || ch == '┘' || ch == '─') ? wallColor : textColor;

            if (Character.isDigit(ch) || ch == '.' || ch == '-' || ch == '+') {
                if (!cleanContent.contains("TRACKER") && visualIndex > 2 && visualIndex < 20) {
                    activeColor = "\u001B[38;5;112m"; // Lock color values inside columns
                }
            }

            // Combine the tracked ANSI tag with the character token
            String existingAnsi = (outputBuffer[targetBufferIndex] != null && outputBuffer[targetBufferIndex].startsWith("\u001B")) ? outputBuffer[targetBufferIndex] : "";
            outputBuffer[targetBufferIndex] = existingAnsi + activeColor + ch + RESET;
            visualIndex++;
        }
    }
}
