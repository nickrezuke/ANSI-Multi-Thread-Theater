public class LorenzLoader extends Loader { 
    private static final StatusStage[] STAGES = { 
        new StatusStage(25, "Seeding deterministic chaos:"), 
        new StatusStage(55, "Calculating Lorenz vector equations:"), 
        new StatusStage(80, "Mapping strange attractor nodes:"), 
        new StatusStage(100, "Chaos Instability Contained!") 
    }; 

    private double lx = 0.1, ly = 0.0, lz = 0.0; 
    private double vX = 0, vY = 0, vZ = 0;

    private static final double SIGMA = 10.0; 
    private static final double RHO = 28.0; 
    private static final double BETA = 8.0 / 3.0; 
    private static final double DT = 0.0075; 

    private static final int MAX_POINTS = 600; 
    private final double[][] historyXyz = new double[MAX_POINTS][3]; 
    private int historyIndex = 0; 
    private int activePointsCount = 0; 
    private double angle = 0.0; 

    public LorenzLoader() { 
        super(STAGES); 
    } 

    @Override 
    protected void initialize() { 
        activePointsCount = 0; 
        historyIndex = 0; 
    } 

    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        // Hard-clip the 3D attractor field to column 54 to completely clear the panel space
        int renderWidth = 54; 
        int totalWidth = 80;
        int height = 22; 

        // 1. Generate new 3D math points and push them into our raw geometry queue 
        for (int step = 0; step < 6; step++) { 
            double dx = SIGMA * (ly - lx) * DT; 
            double dy = (lx * (RHO - lz) - ly) * DT; 
            double dz = (lx * ly - BETA * lz) * DT; 
            
            vX = dx / DT; vY = dy / DT; vZ = dz / DT;

            lx += dx; ly += dy; lz += dz; 

            historyXyz[historyIndex][0] = lx * 0.07; 
            historyXyz[historyIndex][1] = ly * 0.07; 
            historyXyz[historyIndex][2] = (lz - 25.0) * 0.07; 

            historyIndex = (historyIndex + 1) % MAX_POINTS; 
            if (activePointsCount < MAX_POINTS) activePointsCount++; 
        } 

        // 2. Precompute 3D Rotation Angles 
        double rX = angle * 0.20; 
        double rY = angle * 0.35; 
        double cosX = Math.cos(rX), sinX = Math.sin(rX); 
        double cosY = Math.cos(rY), sinY = Math.sin(rY); 

        // 3. Process the ENTIRE 3D historical path 
        for (int i = 0; i < activePointsCount; i++) { 
            int lookupIdx = (historyIndex - activePointsCount + i + MAX_POINTS) % MAX_POINTS; 
            double cx = historyXyz[lookupIdx][0]; 
            double cy = historyXyz[lookupIdx][1]; 
            double cz = historyXyz[lookupIdx][2]; 

            double y1 = cy * cosX - cz * sinX; 
            double z1 = cy * sinX + cz * cosX; 
            double x2 = cx * cosY + z1 * sinY; 
            double z2 = -cx * sinY + z1 * cosY; 

            double distanceToCamera = 3.5; 
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

                    if (ageFactor > 0.94)      { colorCode = "\u001B[38;5;255m"; trailChar = '@'; } 
                    else if (ageFactor > 0.75) { colorCode = "\u001B[38;5;81m";  trailChar = '*'; } 
                    else if (ageFactor > 0.45) { colorCode = "\u001B[38;5;201m"; trailChar = '+'; } 
                    else if (ageFactor > 0.15) { colorCode = "\u001B[38;5;93m";  trailChar = '.'; } 
                    else                       { colorCode = "\u001B[38;5;54m";  trailChar = ','; } 

                    outputBuffer[o] = colorCode + trailChar + RESET; 
                } 
            } 
        } 

        // 4. OVERLAY LIVE TELEMETRY DASHBOARD PANEL (Columns 56-79 = Exactly 24 Characters wide)
        // All labels are padded to identical target lengths to ensure the right border matches perfectly
        String gray = "\u001B[38;5;244m";
        String green = "\u001B[38;5;112m";
        String white = "\u001B[38;5;255m";
        
        drawDashboardLine(outputBuffer, 0,  "┌──────────────────────┐", white, white);
        drawDashboardLine(outputBuffer, 1,  "│ LORENZ ATTRACTOR     │", white, white);
        drawDashboardLine(outputBuffer, 2,  "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 3,  String.format("│ %sSIGMA: %s%6.2f        │", gray, green, SIGMA), gray, white);
        drawDashboardLine(outputBuffer, 4,  String.format("│ %sRHO:   %s%6.2f        │", gray, green, RHO), gray, white);
        drawDashboardLine(outputBuffer, 5,  String.format("│ %sBETA:  %s1/3 * %4.1f    │", gray, green, BETA * 3.0), gray, white);
        drawDashboardLine(outputBuffer, 6,  "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 7,  "│ STATE VARIABLES      │", white, white);
        drawDashboardLine(outputBuffer, 8,  String.format("│  %sX:  %s%6.2f          │", gray, green, lx), gray, white);
        drawDashboardLine(outputBuffer, 9,  String.format("│  %sY:  %s%6.2f          │", gray, green, ly), gray, white);
        drawDashboardLine(outputBuffer, 10, String.format("│  %sZ:  %s%6.2f          │", gray, green, lz), gray, white);
        drawDashboardLine(outputBuffer, 11, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 12, "│ INSTANT VELOCITY     │", white, white);
        drawDashboardLine(outputBuffer, 13, String.format("│ %sdX: %s%+7.1f          │", gray, green, vX), gray, white);
        drawDashboardLine(outputBuffer, 14, String.format("│ %sdY: %s%+7.1f          │", gray, green, vY), gray, white);
        drawDashboardLine(outputBuffer, 15, String.format("│ %sdZ: %s%+7.1f          │", gray, green, vZ), gray, white);
        drawDashboardLine(outputBuffer, 16, "└──────────────────────┘", white, white);

        // Wipe remaining lower canvas margins to prevent 3D overflow artifacts
        for (int row = 17; row < 22; row++) {
            drawDashboardLine(outputBuffer, row, "                        ", RESET, RESET);
        }

        angle += 0.015; 
    }

    /**
     * Safely injects text cell-by-cell into the framework buffer [1].
     * Separates the text structure from the raw layout mechanics, fixing the misalignment bug [1].
     */
    private void drawDashboardLine(String[] outputBuffer, int row, String content, String textColor, String wallColor) {
        int targetColumn = 56;
        int rowStartIdx = row * 80;
        
        // Strip out any color formatting to find the true text length
        String cleanContent = content.replaceAll("\u001B\\[[;\\d]*m", "");
        int visualIndex = 0;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            // Handle color transitions without losing our spot in the 80-column grid
            if (ch == '\u001B') {
                int endM = content.indexOf('m', i);
                if (endM != -1) {
                    i = endM; // Skip past the color tag
                    continue;
                }
            }

            int currentCellX = targetColumn + visualIndex;
            if (currentCellX >= 80) break;

            int targetBufferIndex = rowStartIdx + currentCellX;
            
            // Choose the correct color based on whether it's a box line or telemetry data
            String activeColor = (ch == '│' || ch == '┌' || ch == '┐' || ch == '├' || ch == '┤' || ch == '└' || ch == '┘' || ch == '─') 
                                 ? wallColor : textColor;

            // Apply specific green highlighting to numbers inside data fields
            if (Character.isDigit(ch) || ch == '.' || ch == '-' || ch == '+') {
                if (!cleanContent.contains("ATTRACTOR") && visualIndex > 2 && visualIndex < 20) {
                    activeColor = "\u001B[38;5;112m"; // Force numbers to green
                }
            }

            // Set exactly ONE visible character per cell index [1]
            outputBuffer[targetBufferIndex] = activeColor + ch + RESET;
            visualIndex++;
        }
    }
}
