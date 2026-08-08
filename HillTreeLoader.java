import java.util.Random;

public class HillTreeLoader extends Loader {
    private static final StatusStage[] ARBOR_STAGES = {
            new StatusStage(25, "Seeding meadow..."),
            new StatusStage(50, "Growing leaf canopy..."),
            new StatusStage(75, "Hanging rope swing..."),
            new StatusStage(100, "Tree Swing Ready!")
    };

    private double timeClock = 0.0;
    private final Random rand = new Random();

    // Leaf particle tracking registers to simulate individual falling leaves
    private static final int LEAF_COUNT = 2;
    private final double[] leafX = new double[LEAF_COUNT];
    private final double[] leafY = new double[LEAF_COUNT];
    private final double[] leafSpeed = new double[LEAF_COUNT];
    private final double[] leafSwayOffset = new double[LEAF_COUNT];

    // TrueColor ANSI Gradients for the clear daylight sky background
    private static final int[] SKY_TOP = { 102, 178, 255 };
    private static final int[] SKY_MID = { 153, 204, 255 };
    private static final int[] SKY_BTM = { 204, 229, 255 };

    // Organic pure ASCII palette configurations
    private static final String COLOR_LEAVES = "\u001B[38;2;34;139;34m"; // Forest Green
    private static final String COLOR_TRUNK = "\u001B[38;2;139;69;19m"; // Saddle Brown
    private static final String COLOR_ROPE = "\u001B[38;2;210;180;140m"; // Tan Wood Fiber
    private static final String COLOR_APPLE = "\u001B[38;2;240;30;30m"; // Crimson Red Apple
    private static final String COLOR_MEADOW = "\u001B[38;2;50;205;50m"; // Lime Meadow Green
    private static final String COLOR_FLOWER = "\u001B[38;2;255;215;0m"; // Golden Yellow Flower
    private static final String COLOR_WHITE_F = "\u001B[38;2;245;245;250m"; // White Daisy Petal

    public HillTreeLoader() {
        // This uses 80x22 specifically
        super(ARBOR_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;

        // Spawn initial randomized falling leaf particle positions inside the tree's
        // coordinates
        for (int i = 0; i < LEAF_COUNT; i++) {
            resetLeafParticle(i);
            leafY[i] = 4 + rand.nextInt(10);
        }

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    private void resetLeafParticle(int index) {
        // Leaves detach from the main left branch cluster coordinates
        this.leafX[index] = 14 + rand.nextInt(18);
        this.leafY[index] = 4.0;
        this.leafSpeed[index] = 0.08 + rand.nextDouble() * 0.08;
        this.leafSwayOffset[index] = rand.nextDouble() * Math.PI * 2.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Advance independent timeline registers to drive wind and physics wave
        // variables
        timeClock += 0.035;

        int width = 80;
        int height = 22;

        // Calculate dynamic global physics offsets driven by the master clock
        double swingAngle = 0.32 * Math.sin(timeClock * 1.1); // Harmonic rope swing sweep arc

        // Update tracking registers for drifting leaf particles
        for (int i = 0; i < LEAF_COUNT; i++) {
            leafY[i] += leafSpeed[i];
            leafX[i] += 0.12 * Math.sin(timeClock * 1.4 + leafSwayOffset[i]);

            // If a leaf hits the meadow line or wanders off screen, recycle it back up to
            // the branches
            double currentHillFloor = 14.0 + 2.5 * Math.sin(leafX[i] * 0.04);
            if (leafY[i] >= currentHillFloor || leafX[i] < 0 || leafX[i] >= width) {
                resetLeafParticle(i);
            }
        }

        // Master canvas rasterization loop grid
        for (int y = 0; y < height; y++) {

            // Precompute background sky gradients at this specific row for smooth blending
            int[] currentSkyRGB = SKY_BTM;
            if (y < 6)
                currentSkyRGB = SKY_TOP;
            else if (y < 12)
                currentSkyRGB = SKY_MID;

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // -------------------------------------------------------------
                // LAYER 1: THE STATIC MEADOW HILL TERRAIN (Z-Depth: 0.85)
                // -------------------------------------------------------------
                double hillWave = 14.0 + 2.5 * Math.sin(x * 0.04);
                if (y >= (int) hillWave) {
                    if (0.85 > zBuffer[index]) {
                        zBuffer[index] = 0.85;

                        // We check fixed intervals to populate a carpet of scattered static flowers.
                        if (y == (int) hillWave && (x % 7 == 2 || x == 11 || x == 47 || x == 68)) {
                            // Alternate between yellow wild blooms and crisp white meadow daisies
                            String fColor = (x % 2 == 0) ? COLOR_FLOWER : COLOR_WHITE_F;
                            outputBuffer[index] = fColor + "\u273F" + RESET;
                        } else {
                            char groundChar = (y == (int) hillWave) ? '▒' : '█';
                            outputBuffer[index] = COLOR_MEADOW + groundChar + RESET;
                        }
                    }
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 2: THE DANGLING ROPE SWING (Z-Depth: 0.75)
                // -------------------------------------------------------------
                // Two structural ropes hanging from a solid overhead branch at (25, 5)
                int branchPivotX = 25;
                int branchPivotY = 5;
                int ropeLength = 8;

                if (y >= branchPivotY && y <= branchPivotY + ropeLength) {
                    int deltaY = y - branchPivotY;
                    int currentSwingX = branchPivotX + (int) Math.round(deltaY * swingAngle);

                    if (y == branchPivotY + ropeLength) {
                        // Render the flat wooden seat block connecting the ropes at the bottom base
                        if (Math.abs(x - currentSwingX) <= 2 && 0.75 > zBuffer[index]) {
                            zBuffer[index] = 0.75;
                            outputBuffer[index] = COLOR_TRUNK + "\u2584" + RESET; // Pure ASCII seat line
                            continue;
                        }
                    } else {
                        // Render dual independent rope fibers hanging down side-by-side
                        if ((x == currentSwingX - 1 || x == currentSwingX + 1) && 0.75 > zBuffer[index]) {
                            zBuffer[index] = 0.75;
                            outputBuffer[index] = COLOR_ROPE + "|" + RESET; // Pure ASCII string wire
                            continue;
                        }
                    }
                }

                // -------------------------------------------------------------
                // LAYER 3: THE ORGANIC APPLE TREE (Z-Depth: 0.70)
                // -------------------------------------------------------------
                int trunkBaseX = 14;
                int trunkCeilY = 6;
                int trunkFloorY = (int) (14.0 + 2.5 * Math.sin(trunkBaseX * 0.04));

                // A. Solid Wood Trunk & Heavy Structural Branches
                if (x >= trunkBaseX - 1 && x <= trunkBaseX + 1 && y >= trunkCeilY && y <= trunkFloorY) {
                    if (0.70 > zBuffer[index]) {
                        zBuffer[index] = 0.70;
                        char trunkChar = (x == trunkBaseX) ? '█' : '▓';
                        outputBuffer[index] = COLOR_TRUNK + trunkChar + RESET;
                        continue;
                    }
                }
                // Outward extending structural branch extending horizontally to hold the swing
                // ropes
                if (y == branchPivotY && x >= trunkBaseX && x <= branchPivotX + 2) {
                    if (0.70 > zBuffer[index]) {
                        zBuffer[index] = 0.70;
                        outputBuffer[index] = COLOR_TRUNK + "▓" + RESET; // Pure ASCII horizontal branch
                        continue;
                    }
                }

                // B. Dense Ellipsoidal Leaf Canopy with Interspersed Apples
                double leafRadiusX = 16.0;
                double leafRadiusY = 4.5;
                double dx = x - 20;
                double dy = y - 4;
                double leafEllipse = (dx * dx) / (leafRadiusX * leafRadiusX) + (dy * dy) / (leafRadiusY * leafRadiusY);

                if (leafEllipse <= 1.0) {
                    if (0.70 > zBuffer[index]) {
                        zBuffer[index] = 0.70;

                        // FIX: Procedural Apples are now drawn using pure ASCII lowercase 'o's
                        if ((x % 7 == 0 && y % 3 == 0) || (x == 23 && y == 5) || (x == 11 && y == 3)) {
                            outputBuffer[index] = COLOR_APPLE + "o" + RESET;
                        } else {
                            char leafChar = (leafEllipse > 0.75) ? '░' : (leafEllipse > 0.4) ? '▒' : '▓';
                            outputBuffer[index] = COLOR_LEAVES + leafChar + RESET;
                        }
                    }
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 4: DRIFTING LEAF PARTICLES (Z-Depth: 0.60)
                // -------------------------------------------------------------
                // FIX: Falling leaves are now tracked and drawn using ASCII tildes and
                // backticks
                int fallingLeafIdx = -1;
                for (int l = 0; l < LEAF_COUNT; l++) {
                    if ((int) Math.round(leafX[l]) == x && (int) Math.round(leafY[l]) == y) {
                        fallingLeafIdx = l;
                        break;
                    }
                }
                if (fallingLeafIdx != -1 && 0.60 > zBuffer[index]) {
                    zBuffer[index] = 0.60;
                    char leafASCII = (fallingLeafIdx % 2 == 0) ? '\u2766' : '\u2767';
                    outputBuffer[index] = COLOR_LEAVES + leafASCII + RESET;
                    continue;
                }
                // -------------------------------------------------------------
                // // LAYER 5: BACKGROUND DAYLIGHT SKY BACKDROP (Z-Depth: 0.01)
                // // -------------------------------------------------------------
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;
                    String skyColor = String.format("\u001B[38;2;%d;%d;%dm", currentSkyRGB[0], currentSkyRGB[1],
                            currentSkyRGB[2]);
                    char skyTexture = (y < 6) ? '█' : (y < 12) ? '▓' : '▒';
                    outputBuffer[index] = skyColor + skyTexture + RESET;
                }
            }
        }
    }
}