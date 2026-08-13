public class TropicalIslandLoader extends Loader {
    private static final StatusStage[] TROPICAL_STAGES = {
            new StatusStage(25, "Calibrating sunset horizon gradients:"),
            new StatusStage(50, "Extruding organic palm trunk geometries:"),
            new StatusStage(75, "Simulating rhythmic ocean surface ripples:"),
            new StatusStage(100, "Tropical Paradise Environment Loaded!")
    };

    private double timeClock = 0.0;
    private final int width = 80;
    private final int height = 22;

    // Adjusted Vector Art Palette (Toned down and matched for full blocks)
    private static final int[] RGB_SKY_TOP = { 30, 45, 110 }; // Rich Sunset Indigo Blue
    private static final int[] RGB_SKY_MID = { 210, 95, 30 }; // Muted Burning Orange
    private static final int[] RGB_SKY_BTM = { 230, 175, 60 }; // Clean Light Ochre Yellow
    private static final int[] RGB_OCEAN_DEEP = { 25, 95, 160 }; // Deep Horizon Ocean Blue
    private static final int[] RGB_OCEAN_LIT = { 35, 130, 190 }; // Calm Aqua Ripple Shimmer
    private static final int[] RGB_PALM_WOOD = { 100, 70, 45 }; // Dark Bark Brown
    private static final int[] RGB_PALM_LEAF = { 30, 130, 55 }; // Pure Emerald Canopy Green

    public TropicalIslandLoader() {
        super(TROPICAL_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04; // Master timer clock

        double islandCenterX = 26.0;
        int seaLevelY = 14;

        for (int y = 0; y < height; y++) {
            // Smoothly interpolate the universal vertical background sky gradient
            double verticalPct = (double) y / height;
            int[] skyRGB = RGB_SKY_BTM;
            if (verticalPct < 0.35) {
                double t = verticalPct / 0.35;
                skyRGB = new int[] {
                        (int) (RGB_SKY_TOP[0] * (1.0 - t) + RGB_SKY_MID[0] * t),
                        (int) (RGB_SKY_TOP[1] * (1.0 - t) + RGB_SKY_MID[1] * t),
                        (int) (RGB_SKY_TOP[2] * (1.0 - t) + RGB_SKY_MID[2] * t)
                };
            } else if (verticalPct < 0.65) {
                double t = (verticalPct - 0.35) / 0.30;
                skyRGB = new int[] {
                        (int) (RGB_SKY_MID[0] * (1.0 - t) + RGB_SKY_BTM[0] * t),
                        (int) (RGB_SKY_MID[1] * (1.0 - t) + RGB_SKY_BTM[1] * t),
                        (int) (RGB_SKY_MID[2] * (1.0 - t) + RGB_SKY_BTM[2] * t)
                };
            }

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // -------------------------------------------------------------
                // LAYER 1: TWIN PALM TREES & FRONDS (Z-Depth: 0.90)
                // -------------------------------------------------------------
                boolean hitPalm = false;

                for (int treeId = -1; treeId <= 1; treeId += 2) {
                    if (treeId == 0)
                        continue;

                    double baseIdxX = islandCenterX + treeId * 4.0;
                    double heightScale = (15.0 - y);

                    double trunkX = baseIdxX + (treeId * 0.09 * heightScale * heightScale)
                            + 0.12 * Math.sin(timeClock + y * 0.25);

                    boolean validTrunkHeight = y >= 5 && y <= 15;
                    if (validTrunkHeight && Math.abs(x - trunkX) < 0.85) {
                        if (0.90 > zBuffer[index]) {
                            zBuffer[index] = 0.90;
                            String woodColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_PALM_WOOD[0],
                                    RGB_PALM_WOOD[1], RGB_PALM_WOOD[2]);
                            outputBuffer[index] = woodColor + (y % 2 == 0 ? "▓" : "█") + RESET;
                            hitPalm = true;
                            break;
                        }
                    }

                    double apexY = 5.0;
                    double apexHeightScale = (15.0 - apexY);
                    double leafApexX = baseIdxX + (treeId * 0.09 * apexHeightScale * apexHeightScale)
                            + 0.12 * Math.sin(timeClock + apexY * 0.25);

                    int leafDx = x - (int) leafApexX;
                    int leafDy = y - (int) apexY;

                    if (Math.abs(leafDx) < 7 && Math.abs(leafDy) < 3) {
                        boolean isFrondLine = (leafDy == 0) ||
                                (leafDx == leafDy * 2) ||
                                (leafDx == -leafDy * 2) ||
                                (leafDy == -1 && Math.abs(leafDx) < 3);

                        if (isFrondLine) {
                            if (0.90 > zBuffer[index]) {
                                zBuffer[index] = 0.90;
                                String leafColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_PALM_LEAF[0],
                                        RGB_PALM_LEAF[1], RGB_PALM_LEAF[2]);
                                outputBuffer[index] = leafColor + "█" + RESET;
                                hitPalm = true;
                                break;
                            }
                        }
                    }
                }
                if (hitPalm)
                    continue;

                // -------------------------------------------------------------
                // LAYER 2: THE SANDY MOUND ISLAND (Z-Depth: 0.80)
                // -------------------------------------------------------------
                double islandProfile = 14.5 + Math.pow((x - islandCenterX) * 0.14, 2);
                if (y >= (int) islandProfile && y < 17 && 0.80 > zBuffer[index]) {
                    zBuffer[index] = 0.80;
                    outputBuffer[index] = "\u001B[38;2;225;195;115m█" + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 3: FLAPPING BIRD SILHOUETTES (Z-Depth: 0.70)
                // -------------------------------------------------------------
                boolean hitBird = false;
                for (int bId = 0; bId < 3; bId++) {
                    double birdSpeed = 7.0 + bId * 2.5;
                    double bx = width - ((timeClock * birdSpeed + bId * 25.0) % (width + 16));
                    double by = 2.5 + bId * 2.2 + Math.sin(timeClock * 1.1 + bId);

                    int deltaX = x - (int) bx;
                    int deltaY = y - (int) by;

                    if (Math.abs(deltaX) <= 1 && deltaY == 0) {
                        boolean wingUp = Math.sin(timeClock * 8.5 + bx) > 0.0;
                        char birdChar = (deltaX == 0) ? (wingUp ? 'v' : '^') : ((deltaX == -1 == wingUp) ? '\\' : '/');

                        if (0.70 > zBuffer[index]) {
                            zBuffer[index] = 0.70;
                            outputBuffer[index] = "\u001B[38;2;255;255;255m" + birdChar + RESET;
                            hitBird = true;
                            break;
                        }
                    }
                }
                if (hitBird)
                    continue;

                // -------------------------------------------------------------
                // LAYER 4: THE CALM OCEAN PLANE (Z-Depth: 0.50)
                // -------------------------------------------------------------
                if (y >= seaLevelY && 0.50 > zBuffer[index]) {
                    zBuffer[index] = 0.50;

                    double waterWave = Math
                            .sin(x * 0.45 - (0.85 * (timeClock / 3.0) + 3.0 * Math.sin(timeClock / 3.0)) * 0.7)
                            * Math.cos(y * 1.2);

                    int r = RGB_OCEAN_DEEP[0], g = RGB_OCEAN_DEEP[1], b = RGB_OCEAN_DEEP[2];
                    char oceanChar = '▒';

                    if (waterWave > 0.5) {
                        r = RGB_OCEAN_LIT[0];
                        g = RGB_OCEAN_LIT[1];
                        b = RGB_OCEAN_LIT[2];
                        oceanChar = '█';
                    } else if (waterWave < -0.3) {
                        oceanChar = '░';
                    }

                    String wColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                    outputBuffer[index] = wColor + oceanChar + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 5: SOLAR DISC & SKY BACKDROP (Z-Depth: 0.01)
                // -------------------------------------------------------------
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;

                    double sunDx = x - 64.0;
                    double sunDy = y - 4.0;
                    double distToSun = Math.sqrt(sunDx * sunDx + sunDy * sunDy * 2.3 * 2.3);

                    if (distToSun < 5.2) {
                        outputBuffer[index] = "\u001B[38;2;255;242;120m█" + RESET;
                    } else if (distToSun < 8.0 && Math.sin(distToSun * 3.5 - timeClock * 2.5) > 0.15) {
                        outputBuffer[index] = "\u001B[38;2;240;175;55m█" + RESET;
                    } else {
                        // Array indices explicitly extracted here to fix the black background bug
                        String sColor = String.format("\u001B[38;2;%d;%d;%dm", skyRGB[0], skyRGB[1], skyRGB[2]);
                        outputBuffer[index] = sColor + "█" + RESET;
                    }
                }

            }
        }
    }
}
