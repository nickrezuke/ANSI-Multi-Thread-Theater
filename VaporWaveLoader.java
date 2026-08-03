public class VaporWaveLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Booting aesthetic vapor arrays:"),
        new StatusStage(50, "Calibrating pastel blue vector grid:"),
        new StatusStage(75, "Rendering classic pink horizon sun:"),
        new StatusStage(100, "Vaporwave Protocol Active!")
    };

    private double timeOffset = 0;

    // Soft, Pastel 256-Color Vaporwave Aesthetic Palette
    private static final String COLOR_BLUE_GRID = "\u001B[38;5;45m";   // Soft Electric Blue Grid
    private static final String COLOR_CYAN_OCEAN = "\u001B[38;5;51m";  // Bright Teal/Aqua Waves
    private static final String COLOR_SKY_PINK = "\u001B[38;5;218m";   // Pastel Millennial Pink Sky
    private static final String COLOR_SKY_LAV = "\u001B[38;5;147m";    // Soft Dreamy Lavender Sky
    private static final String COLOR_PINK_SUN = "\u001B[38;5;200m";   // Signature Pink/Magenta Sun
    private static final String COLOR_PALM_SILH = "\u001B[38;5;205m";  // Hot Pink Palm Silhouette

    public VaporWaveLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() { }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Smoothly advances time to drive wave propagation and tidal movements
        timeOffset += 0.025;
        int width = 80;
        int height = 22;
        int horizonY = 10; 
        int groundHeight = height - (horizonY + 1); // Total rows allocated for the floor (11 rows)

        for (int y = 0; y < height; y++) {
            // ==================== SKY LAYER (Above Horizon) ====================
            if (y < horizonY) {
                for (int x = 0; x < width; x++) {
                    int o = x + width * y;

                    // A. THE SIGNATURE VAPORWAVE PINK SUN (Centered at X=40, Y=7, Radius=6)
                    double dx = (x - 40) / 1.8; 
                    double dy = y - 7;
                    double sunRadius = Math.sqrt(dx * dx + dy * dy);

                    if (sunRadius < 5.8) {
                        double sunYRatio = (y - 1.5) / 12.0;
                        double wavePattern = Math.sin(sunYRatio * 28.0);
                        boolean isSunSliceGap = (y >= 6 && wavePattern < -0.2) || (y >= 8 && wavePattern < 0.2) || (y == 9);

                        if (!isSunSliceGap && 0.15 > zBuffer[o]) {
                            zBuffer[o] = 0.15;
                            outputBuffer[o] = COLOR_PINK_SUN + "\u2588" + RESET;
                            continue;
                        }
                    }

                    // B. PROCEDURAL PALM TREE SILHOUETTES
                    boolean isPalm = false;
                    int checkX = (x < 40) ? 13 : 67;
                    int px = Math.abs(x - checkX);

                    if (x == checkX && y >= 4) {
                        isPalm = true;
                    } else if (y <= 5 && y >= 2) {
                        if (y == 5 && px <= 4) isPalm = true;
                        if (y == 4 && px <= 3) isPalm = true;
                        if (y == 3 && px >= 2 && px <= 4) isPalm = true;
                        if (y == 2 && px == 3) isPalm = true;
                    }

                    if (isPalm && sunRadius >= 5.8 && 0.3 > zBuffer[o]) {
                        zBuffer[o] = 0.3;
                        outputBuffer[o] = COLOR_PALM_SILH + "\u2593" + RESET;
                        continue;
                    }

                    // C. LIGHT PASTEL PINK-TO-LAVENDER SKY GRADIENT
                    if (0.01 > zBuffer[o]) {
                        zBuffer[o] = 0.01;
                        if (y < 4) {
                            outputBuffer[o] = COLOR_SKY_LAV + "\u2588" + RESET;
                        } else if (y < 6) {
                            outputBuffer[o] = COLOR_SKY_LAV + "\u2592" + RESET;
                        } else if (y < 8) {
                            outputBuffer[o] = COLOR_SKY_PINK + "\u2592" + RESET;
                        } else {
                            outputBuffer[o] = COLOR_SKY_PINK + "\u2591" + RESET;
                        }
                    }
                }
                continue;
            }

            // ==================== NEON VECTOR HORIZON STRIP ====================
            if (y == horizonY) {
                for (int x = 0; x < width; x++) {
                    int o = x + width * y;
                    if (0.99 > zBuffer[o]) {
                        zBuffer[o] = 0.99;
                        outputBuffer[o] = COLOR_BLUE_GRID + "\u25AC" + RESET;
                    }
                }
                continue;
            }

            // ==================== STATIC BLUE GRID & ROLLING OCEAN ====================
            int floorDistY = y - horizonY;
            double perspectiveRatio = (double) floorDistY / (height - horizonY);
            
            // Perspective spacing for vertical lines
            double spacing = 3.5 + floorDistY * 0.78; 

            // Compute static vertical grid lines
            boolean[] verticalLines = new boolean[width];
            for (int k = -12; k <= 12; k++) {
                int lineX = (int) Math.round(40 + k * spacing);
                if (lineX >= 0 && lineX < width) {
                    verticalLines[lineX] = true;
                }
            }

            // Compute static horizontal grid lines
            boolean isHorizontalLine = (y == 12 || y == 14 || y == 17 || y == 20);

            // CALIBRATED SHORELINE TIDE BOUNDARY
            // Ground elements go from y = 11 to y = 21. 
            // Min tide index (70% full) means water reaches up to row 14 from the horizon.
            // Max tide index (20% full) means water recedes down to row 19 near the bottom.
            // Normalizing a sin wave between 0.0 and 1.0 to map perfectly to this 14-19 row range.
            double tidePercent = 0.30 + ((Math.sin(timeOffset * 1.3) + 1.0) / 2.0) * (0.80 - 0.30);
            int shorelineTideY = horizonY + 1 + (int) Math.round((1.0 - tidePercent) * groundHeight);

            for (int x = 0; x < width; x++) {
                int o = x + width * y;
                double pseudoDepth = perspectiveRatio;

                if (pseudoDepth > zBuffer[o]) {
                    zBuffer[o] = pseudoDepth;

                    // 1. FULL WIDTH OCEAN RANGE: Removed the trapezoid logic entirely.
                    // The ocean now spans all x-coordinates from 0 to width-1, constrained only by the tide line.
                    boolean insideOceanTide = y <= shorelineTideY;

                    if (insideOceanTide) {
                        // ROLLING INTERIOR WAVES
                        double waveFrequency = 6.0 / (perspectiveRatio + 0.1);
                        double waveXComponent = Math.cos(x * 0.25) * 0.7;
                        double waveValue = Math.sin(waveFrequency - timeOffset * 5.0 + waveXComponent);

                        if (waveValue > 0.4) {
                            outputBuffer[o] = COLOR_CYAN_OCEAN + "\u2248" + RESET; // Wave crest
                        } else {
                            outputBuffer[o] = COLOR_CYAN_OCEAN + "\u223C" + RESET; // Liquid water mass
                        }
                    } else {
                        // PERMANENT STATIC SYNTHWAVE GRID (Revealed completely as a sandy matrix)
                        boolean isVerticalLine = verticalLines[x];
                        if (isHorizontalLine && isVerticalLine) {
                            outputBuffer[o] = COLOR_BLUE_GRID + "\u254B" + RESET; // ╬ Intersection
                        } else if (isVerticalLine) {
                            outputBuffer[o] = COLOR_BLUE_GRID + "\u2551" + RESET; // ║ Vertical Line
                        } else if (isHorizontalLine) {
                            outputBuffer[o] = COLOR_BLUE_GRID + "\u2550" + RESET; // ═ Horizontal Line
                        } else {
                            outputBuffer[o] = " "; // Exposed beach sand floor
                        }
                    }
                }
            }
        }
    }
}
