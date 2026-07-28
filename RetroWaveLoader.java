public class RetroWaveLoader extends Loader {
    private static final StatusStage[] STAGES = {
            new StatusStage(25, "Booting synthesizer arrays:"),
            new StatusStage(50, "Calibrating vector horizon:"),
            new StatusStage(75, "Modulating neon frequencies:"),
            new StatusStage(100, "Outrun Protocol Active!")
    };

    private double timeOffset = 0;

    // High-Saturation 256-Color Retro Aesthetic Palette
    private static final String COLOR_GRID = "\u001B[38;5;201m"; // Hot Neon Pink
    private static final String COLOR_HORIZON = "\u001B[38;5;51m"; // Electric Cyan
    private static final String COLOR_SKY = "\u001B[38;5;55m"; // Deep Synth Purple
    private static final String COLOR_PINK_SKY = "\u001B[38;5;200m"; // Sunset Magenta
    private static final String COLOR_SUN = "\u001B[38;5;214m"; // Outrun Yellow-Orange
    private static final String COLOR_PALM = "\u001B[38;5;34m"; // Cyber Green
    private static final String COLOR_CAR = "\u001B[38;5;196m"; // Neon Testarossa Red
    private static final String COLOR_WHEEL = "\u001B[38;5;234m"; // Slate Black tires
    private static final String COLOR_WINDOW = "\u001B[38;5;87m"; // High-Gloss Cyan Window

    public RetroWaveLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;
        int horizonY = 10; // Splitting horizon row

        // 1. Calculate dynamic car weaving offset (drifts left and right periodically
        // every ~3.5 seconds)
        int carOffset = (int) Math.round(Math.sin(System.currentTimeMillis() * 0.0018) * 2.0);

        // 2. Precompute scrolling horizontal highway grids using logarithmic
        // perspective
        boolean[] horizontalLines = new boolean[height];
        for (int i = 0; i < 6; i++) {
            double position = (timeOffset + i * 0.18) % 1.0;
            double floorRowOffset = 1.0 + 10.0 * Math.pow(position, 1.8);
            int matchingY = horizonY + (int) Math.round(floorRowOffset);
            if (matchingY > horizonY && matchingY < height) {
                horizontalLines[matchingY] = true;
            }
        }

        // 3. Main Screen Buffering Loop
        for (int y = 0; y < height; y++) {

            // ==================== SKY LAYER (Above Horizon) ====================
            if (y < horizonY) {
                for (int x = 0; x < width; x++) {
                    int o = x + width * y;

                    // A. STABLE PROGRESSIVE SYNTHWAVE SUN (Centered at X=40, Y=7, Radius=6)
                    double dx = (x - 40) / 1.8; // Correct for narrow console pixel widths
                    double dy = y - 7;
                    double sunRadius = Math.sqrt(dx * dx + dy * dy);

                    if (sunRadius < 5.8) {
                        // Math Update: Removed timeOffset from the wave function to stop all
                        // flickering/scrolling
                        double sunYRatio = (y - 1.5) / 12.0;
                        double wavePattern = Math.sin(sunYRatio * 28.0); // Completely static pattern track

                        boolean isSunSliceGap = (y >= 6 && wavePattern < -0.2) || (y >= 8 && wavePattern < 0.2)
                                || (y == 9);

                        if (!isSunSliceGap && 0.15 > zBuffer[o]) {
                            zBuffer[o] = 0.15;
                            outputBuffer[o] = COLOR_SUN + "\u2588" + RESET; // █ Solid neon mass
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
                        if (y == 5 && px <= 4)
                            isPalm = true;
                        if (y == 4 && px <= 3)
                            isPalm = true;
                        if (y == 3 && px >= 2 && px <= 4)
                            isPalm = true;
                        if (y == 2 && px == 3)
                            isPalm = true;
                    }

                    if (isPalm && sunRadius >= 5.8 && 0.3 > zBuffer[o]) {
                        zBuffer[o] = 0.3;
                        outputBuffer[o] = COLOR_PALM + "\u2593" + RESET; // ▓ Rich cyber-mesh palm leaves
                        continue;
                    }

                    // C. DITHERED SKY COLOR GRADIENT
                    if (0.01 > zBuffer[o]) {
                        zBuffer[o] = 0.01;
                        if (y < 4) {
                            outputBuffer[o] = COLOR_SKY + "\u2588" + RESET;
                        } else if (y < 6) {
                            outputBuffer[o] = COLOR_SKY + "\u2593" + RESET;
                        } else if (y < 8) {
                            outputBuffer[o] = COLOR_PINK_SKY + "\u2592" + RESET;
                        } else {
                            outputBuffer[o] = COLOR_PINK_SKY + "\u2591" + RESET;
                        }
                    }
                }
                continue;
            }

            // ==================== NEON VECTOR HORIZON ====================
            if (y == horizonY) {
                for (int x = 0; x < width; x++) {
                    int o = x + width * y;
                    if (0.99 > zBuffer[o]) {
                        zBuffer[o] = 0.99;
                        outputBuffer[o] = COLOR_HORIZON + "\u25AC" + RESET; // ▬ Laser horizon strip
                    }
                }
                continue;
            }

            // ==================== FLOOR GRID & HIGHWAY SYSTEM ====================
            int floorDistY = y - horizonY;
            boolean isHorizontal = horizontalLines[y];
            double spacing = 3.5 + floorDistY * 0.78;

            boolean[] verticalLines = new boolean[width];
            for (int k = -12; k <= 12; k++) {
                int lineX = (int) Math.round(40 + k * spacing);
                if (lineX >= 0 && lineX < width) {
                    verticalLines[lineX] = true;
                }
            }

            for (int x = 0; x < width; x++) {
                int o = x + width * y;

                // A. MOVING VECTOR SPORTS CAR OVERLAY (Boundary shifts left/right dynamically
                // using carOffset)
                boolean isCarPixel = false;
                int startX = 33 + carOffset;
                int endX = 47 + carOffset;

                if (y >= 17 && y <= 19 && x >= startX && x <= endX) {
                    int carX = x - startX;
                    int carY = y - 17;
                    String blockSprite = " ";

                    if (carY == 0) {
                        if (carX >= 4 && carX <= 10)
                            blockSprite = COLOR_CAR + "\u2580"; // ▀
                        else if (carX == 3)
                            blockSprite = COLOR_CAR + "\u2591"; // ░
                        else if (carX == 11)
                            blockSprite = COLOR_CAR + "\u2591"; // ░
                    } else if (carY == 1) {
                        if (carX >= 1 && carX <= 2)
                            blockSprite = COLOR_CAR + "\u2584"; // ▄
                        else if (carX >= 3 && carX <= 4)
                            blockSprite = COLOR_WINDOW + "\u2588"; // █
                        else if (carX >= 5 && carX <= 9)
                            blockSprite = COLOR_CAR + "\u2588"; // █
                        else if (carX >= 10 && carX <= 11)
                            blockSprite = COLOR_WINDOW + "\u2588";// █
                        else if (carX >= 12 && carX <= 13)
                            blockSprite = COLOR_CAR + "\u2584"; // ▄
                    } else if (carY == 2) {
                        if (carX == 2 || carX == 3 || carX == 11 || carX == 12) {
                            blockSprite = COLOR_WHEEL + "\u2588"; // █
                        } else if (carX > 0 && carX < 14) {
                            blockSprite = COLOR_CAR + "\u2584"; // ▄
                        }
                    }

                    if (!blockSprite.equals(" ") && 0.96 > zBuffer[o]) {
                        zBuffer[o] = 0.96;
                        outputBuffer[o] = blockSprite + RESET;
                        isCarPixel = true;
                    }
                }

                if (isCarPixel)
                    continue;

                // B. INFRASTRUCTURE HIGHWAY TRACKS
                int roadLeftEdge = (int) Math.round(40 - 2.3 * spacing);
                int roadRightEdge = (int) Math.round(40 + 2.3 * spacing);
                boolean isRoadEdge = (x == roadLeftEdge || x == roadRightEdge);

                boolean isCenterDivider = (x == 40 && isHorizontal);
                double pseudoDepth = (double) floorDistY / (height - horizonY);

                if (pseudoDepth > zBuffer[o]) {
                    zBuffer[o] = pseudoDepth;

                    if (isRoadEdge) {
                        outputBuffer[o] = COLOR_HORIZON + "\u2588" + RESET;
                    } else if (isCenterDivider) {
                        outputBuffer[o] = COLOR_SUN + "\u2584" + RESET;
                    } else if (x > roadLeftEdge && x < roadRightEdge) {
                        outputBuffer[o] = " ";
                    } else {
                        // C. DISTANT BACKGROUND SCANNING CYBER-GRID
                        boolean isVertical = verticalLines[x];

                        if (isHorizontal && isVertical) {
                            outputBuffer[o] = COLOR_GRID + "\u254B" + RESET; // ╬
                        } else if (isVertical) {
                            outputBuffer[o] = COLOR_GRID + "\u2551" + RESET; // ║
                        } else if (isHorizontal) {
                            outputBuffer[o] = COLOR_GRID + "\u2550" + RESET; // ═
                        } else {
                            outputBuffer[o] = " ";
                        }
                    }
                }
            }
        }
        timeOffset += 0.035;
    }
}