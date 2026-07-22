import java.util.Arrays;

public class RetroWaveLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Booting synthesizer arrays:"),
        new StatusStage(50, "Calibrating vector horizon:"),
        new StatusStage(75, "Modulating neon frequencies:"),
        new StatusStage(100, "Outrun Protocol Active!")
    };

    private double timeOffset = 0;

    // Retrowave Aesthetic Color Palette
    private static final String COLOR_GRID = "\u001B[38;5;201m";    // Hot Neon Pink
    private static final String COLOR_HORIZON = "\u001B[38;5;51m"; // Electric Cyan
    private static final String COLOR_SKY = "\u001B[38;5;55m";     // Deep Synth Purple
    private static final String COLOR_SUN = "\u001B[38;5;214m";     // Outrun Orange/Yellow
    private static final String COLOR_PALM = "\u001B[38;5;34m";     // Cyberpunk Green
    private static final String COLOR_CAR = "\u001B[38;5;196m";     // Sports Car Red
    private static final String COLOR_WHEEL = "\u001B[38;5;236m";   // Dark Gray Wheels
    private static final String COLOR_WINDOW = "\u001B[38;5;81m";   // Cyan Glass Glow

    public RetroWaveLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;
        int horizonY = 10; // Center division horizon split row

        // 1. Precompute exactly which rows should contain scrolling horizontal lines
        boolean[] horizontalLines = new boolean[height];
        for (int i = 0; i < 5; i++) {
            double position = (timeOffset + i * 0.20) % 1.0;
            double floorRowOffset = 1.0 + 10.0 * Math.pow(position, 1.6);
            int matchingY = horizonY + (int) Math.round(floorRowOffset);
            
            if (matchingY > horizonY && matchingY < height) {
                horizontalLines[matchingY] = true;
            }
        }

        // 2. Perform the main canvas rendering loop
        for (int y = 0; y < height; y++) {
            
            // ==================== SKY LAYER (Above Horizon) ====================
            if (y < horizonY) {
                for (int x = 0; x < width; x++) {
                    int o = x + width * y;
                    
                    // Sun Math (Circle centered at X=40, Y=7 with a radius of 6)
                    double dx = (x - 40) / 1.7; // Compensate for text character aspect ratio
                    double dy = y - 7;
                    double sunRadius = Math.sqrt(dx * dx + dy * dy);

                    if (sunRadius < 5.8) {
                        // Create classic Synthwave horizontal slice gaps in the lower part of the sun
                        boolean isSunSlice = (y == 7 && x % 4 == 0) || (y == 8 && x % 2 == 0) || (y == 9);
                        
                        if (!isSunSlice && 0.1 > zBuffer[o]) {
                            zBuffer[o] = 0.1;
                            outputBuffer[o] = COLOR_SUN + "O" + RESET;
                            continue;
                        }
                    }

                    // Left and Right Palm Tree Silhouettes (Hardcoded coordinates for stability)
                    boolean isPalm = false;
                    // Left tree
                    if (x == 12 && y >= 4) isPalm = true; // Trunk
                    if (y == 4 && x >= 9 && x <= 15) isPalm = true; // Fronds
                    if (y == 3 && (x == 10 || x == 11 || x == 13 || x == 14)) isPalm = true;
                    // Right tree
                    if (x == 68 && y >= 4) isPalm = true; // Trunk
                    if (y == 4 && x >= 65 && x <= 71) isPalm = true; // Fronds
                    if (y == 3 && (x == 66 || x == 67 || x == 69 || x == 70)) isPalm = true;

                    if (isPalm && 0.2 > zBuffer[o]) {
                        zBuffer[o] = 0.2;
                        outputBuffer[o] = COLOR_PALM + "Y" + RESET;
                        continue;
                    }

                    // Standard background sky fill
                    if (0.01 > zBuffer[o]) {
                        zBuffer[o] = 0.01;
                        char skyChar = (y > horizonY - 3) ? '-' : '.';
                        outputBuffer[o] = COLOR_SKY + skyChar + RESET;
                    }
                }
                continue;
            }

            // ==================== HORIZON LINE ====================
            if (y == horizonY) {
                for (int x = 0; x < width; x++) {
                    int o = x + width * y;
                    if (0.99 > zBuffer[o]) {
                        zBuffer[o] = 0.99;
                        outputBuffer[o] = COLOR_HORIZON + "~" + RESET;
                    }
                }
                continue;
            }

            // ==================== FLOOR GRID & ROAD LAYER (Below Horizon) ====================
            int dy = y - horizonY;
            boolean isHorizontal = horizontalLines[y];
            double spacing = 3.5 + dy * 0.75;
            
            // Track radial line coordinates
            boolean[] verticalLines = new boolean[width];
            for (int k = -12; k <= 12; k++) {
                int lineX = (int) Math.round(40 + k * spacing);
                if (lineX >= 0 && lineX < width) {
                    verticalLines[lineX] = true;
                }
            }

            for (int x = 0; x < width; x++) {
                int o = x + width * y;
                
                // --- VEHICLE LAYER COMPOSITING (Overlaying the bottom center rows 17-19) ---
                boolean writtenCarPixel = false;
                if (y >= 17 && y <= 19 && x >= 33 && x <= 47) {
                    int carX = x - 33;
                    int carY = y - 17;
                    String carPixel = " ";
                    
                    if (carY == 0) { // Top profile / Roof structure
                        if (carX >= 4 && carX <= 10) carPixel = COLOR_CAR + "_";
                        else if (carX == 3) carPixel = COLOR_CAR + "/";
                        else if (carX == 11) carPixel = COLOR_CAR + "\\";
                    } 
                    else if (carY == 1) { // Mid body & windshield cabin glow
                        if (carX >= 1 && carX <= 2) carPixel = COLOR_CAR + "/";
                        else if (carX >= 3 && carX <= 4) carPixel = COLOR_WINDOW + "#"; // Left window
                        else if (carX >= 5 && carX <= 9) carPixel = COLOR_CAR + "-";
                        else if (carX >= 10 && carX <= 11) carPixel = COLOR_WINDOW + "#"; // Right window
                        else if (carX >= 12 && carX <= 13) carPixel = COLOR_CAR + "\\";
                    } 
                    else if (carY == 2) { // Bottom chassis base layer & wheels
                        if (carX == 0) carPixel = COLOR_CAR + "<";
                        else if (carX == 2 || carX == 3 || carX == 11 || carX == 12) carPixel = COLOR_WHEEL + "O"; // Rear/front tire blocks
                        else if (carX == 14) carPixel = COLOR_CAR + ">";
                        else if (carX > 0 && carX < 14) carPixel = COLOR_CAR + "=";
                    }

                    if (!carPixel.equals(" ") && 0.95 > zBuffer[o]) {
                        zBuffer[o] = 0.95;
                        outputBuffer[o] = carPixel + RESET;
                        writtenCarPixel = true;
                    }
                }
                if (writtenCarPixel) continue; // Skip grid calculations if a piece of the car is right here

                // --- TWO-LANE HIGHWAY SYSTEM ---
                // Calculate the lane boundary markers extending out from the perspective point
                int roadLeftEdge = (int) Math.round(40 - 2.2 * spacing);
                int roadRightEdge = (int) Math.round(40 + 2.2 * spacing);
                int roadCenterLine = 40;

                boolean isRoadEdge = (x == roadLeftEdge || x == roadRightEdge);
                // Make the middle lane stripes dashed by tying their visibility directly to the scrolling lines
                boolean isCenterDivider = (x == roadCenterLine && isHorizontal);

                double pseudoDepth = (double) dy / (height - horizonY);

                if (pseudoDepth > zBuffer[o]) {
                    zBuffer[o] = pseudoDepth;

                    if (isRoadEdge) {
                        outputBuffer[o] = COLOR_HORIZON + "|" + RESET; // Neon road shoulder limits
                    } else if (isCenterDivider) {
                        outputBuffer[o] = COLOR_SUN + ":" + RESET;     // Glowing center strip dashes
                    } else if (x > roadLeftEdge && x < roadRightEdge) {
                        outputBuffer[o] = " ";                         // Clear asphalt driving pavement lanes
                    } else {
                        // --- STANDARD FLOATING BACKGROUND GRID RENDER ---
                        boolean isVertical = verticalLines[x];
                        if (isHorizontal && isVertical) {
                            outputBuffer[o] = COLOR_GRID + "+" + RESET;
                        } else if (isVertical) {
                            outputBuffer[o] = COLOR_GRID + "|" + RESET;
                        } else if (isHorizontal) {
                            outputBuffer[o] = COLOR_GRID + "=" + RESET;
                        } else {
                            outputBuffer[o] = " ";
                        }
                    }
                }
            }
        }

        // Advance timing frame index to animate structural highway mechanics forward
        timeOffset += 0.025;
    }
}
