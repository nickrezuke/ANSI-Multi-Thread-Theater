// TODO: Increase accuracy of this simulation / viewing resolution of Leopard, Cheetah, Giraffe, to be larger to see details???  Changing the simulation will affect the patterns but find some way to "zoom in" over time??

import java.util.Random;

public class ReactionDiffusionLoader extends Loader {

    private static final StatusStage[] REFLECTIVE_STAGES = {
        new StatusStage(25, "Seeding biological DNA markers:"),
        new StatusStage(55, "Synthesizing skin tissue matrix:"),
        new StatusStage(85, "Mutating reaction-diffusion coat:"),
        new StatusStage(100, "Animal Morphology Pattern Stable!")
    };

    // TERMINAL VIEW RES
    private static final int DISPLAY_W = 140;
    private static final int DISPLAY_H = 42;

    // HIGH-RES MATH RESOLUTION (Exactly 2x terminal size to prevent overcrowding artifacts)
    private static final int MATH_W = DISPLAY_W * 2;
    private static final int MATH_H = DISPLAY_H * 2;

    private double[][] gridU = new double[MATH_H][MATH_W];
    private double[][] gridV = new double[MATH_H][MATH_W];
    private double[][] nextU = new double[MATH_H][MATH_W];
    private double[][] nextV = new double[MATH_H][MATH_W];

    private static final double Du = 1.000;

    // Per-animal V diffusion rate, empirically tuned against THIS scheme's
    // particular Laplacian weights/magnitude (published Gray-Scott parameter
    // tables assume a totally different diffusion scale, so they don't port
    // over directly - these were found by simulating each candidate and
    // measuring actual coverage/connectivity rather than guessing).
    private static final double DV_STRIPE = 0.45; // Zebra - thin elongated stripes
    private static final double DV_ROSETTE = 0.50; // Leopard - soft, rounded rosette blobs
    private static final double DV_SPOT = 0.35; // Cheetah - compact, sharp-edged small spots
    private static final double DV_CORAL = 0.50; // Giraffe - reticulated network

    // Re-balanced isotropic weights for high-res mesh grid tracking
    private static final double LAP_EW = 10.0 / 29.0;
    private static final double LAP_NS = 5.0 / 58.0;
    private static final double LAP_DIAG = 1.0 / 29.0;
    private static final double LAP_CENTER = -(2 * LAP_EW + 2 * LAP_NS + 4 * LAP_DIAG);

    private double activeFeed;
    private double activeKill;
    private double activeDv;
    private int variant;

    public ReactionDiffusionLoader() {
        super(REFLECTIVE_STAGES, DISPLAY_W, DISPLAY_H);
    }

    @Override
    protected void initialize() {
        variant = (int) (Math.random() * 4);
        Random rand = new Random();

        for (int y = 0; y < MATH_H; y++) {
            for (int x = 0; x < MATH_W; x++) {
                gridU[y][x] = 1.0;
                gridV[y][x] = 0.0;
            }
        }

        switch (variant) {
            case 0: // ZEBRA STRIPES - tuned for long, connected, high-contrast bands
                activeFeed = 0.029;
                activeKill = 0.057;
                activeDv = DV_STRIPE;
                seedStripeBands(rand, 7);
                break;
            case 1: // LEOPARD ROSETTES - Pearson type lambda: mitotic spots that
                     // pack into a steady state with dark grain-boundary rings
                activeFeed = 0.030;
                activeKill = 0.062;
                activeDv = DV_ROSETTE;
                seedClusters(rand, 22, 3, 7);
                break;
            case 2: // CHEETAH SPOTS - same stable type-lambda family as leopard,
                     // just denser/smaller seeding so spots stay solid dots
                     // instead of growing large enough to hollow out into rings
                activeFeed = 0.030;
                activeKill = 0.063;
                activeDv = DV_SPOT;
                seedClusters(rand, 140, 1, 1);
                break;
            case 3: // GIRAFFE VEINS - Pearson type delta: reinforces the seeded
                     // Voronoi cell walls into a stable network instead of
                     // reorganizing them into zebra-style stripes
                activeFeed = 0.042;
                activeKill = 0.059;
                activeDv = DV_CORAL;
                seedVoronoiNetwork(rand, 18);
                break;
        }

        for (int y = 0; y < MATH_H; y++) {
            for (int x = 0; x < MATH_W; x++) {
                if (gridV[y][x] > 0.0) {
                    gridV[y][x] += (rand.nextDouble() - 0.5) * 0.02;
                }
            }
        }
    }

    private void seedStripeBands(Random rand, int bandCount) {
        for (int b = 0; b < bandCount; b++) {
            int bandHeight = 4 + rand.nextInt(3);
            int centerY = (MATH_H * (b + 1)) / (bandCount + 1) + rand.nextInt(9) - 4;
            int bandWidth = (int) (MATH_W * (0.55 + rand.nextDouble() * 0.25));
            int startX = rand.nextInt(Math.max(1, MATH_W - bandWidth));

            for (int y = Math.max(0, centerY - bandHeight); y < Math.min(MATH_H, centerY + bandHeight); y++) {
                for (int x = startX; x < Math.min(MATH_W, startX + bandWidth); x++) {
                    gridU[y][x] = 0.50;
                    gridV[y][x] = 0.25 + rand.nextDouble() * 0.15;
                }
            }
        }
    }

    private void seedClusters(Random rand, int totalDrops, int minSize, int maxSize) {
        for (int i = 0; i < totalDrops; i++) {
            int size = minSize + (maxSize > minSize ? rand.nextInt(maxSize - minSize + 1) : 0);
            int sy = (size + 2) + rand.nextInt(Math.max(1, MATH_H - 2 * (size + 2)));
            int sx = (size + 2) + rand.nextInt(Math.max(1, MATH_W - 2 * (size + 2)));

            for (int dy = -size; dy <= size; dy++) {
                for (int dx = -size; dx <= size; dx++) {
                    gridU[sy + dy][sx + dx] = 0.50;
                    gridV[sy + dy][sx + dx] = 0.25;
                }
            }
        }
    }

    // Giraffe's reticulated pattern - large, distinctly separated patches
    // divided by a thin connected network - doesn't reliably emerge from
    // scattered round seeds or line segments within a loading screen's
    // lifetime (verified: it fragments into a handful of giant blobs with a
    // few disconnected freckles, not a proper cell network). Instead, seed
    // the network directly as a Voronoi diagram (connected by construction:
    // every point sits on the boundary between exactly two nearest cell
    // seeds), and let the reaction only add organic texture on top of a
    // topology that's already correct.
    private void seedVoronoiNetwork(Random rand, int cellCount) {
        int[] px = new int[cellCount];
        int[] py = new int[cellCount];
        for (int i = 0; i < cellCount; i++) {
            px[i] = rand.nextInt(MATH_W);
            py[i] = rand.nextInt(MATH_H);
        }

        for (int y = 0; y < MATH_H; y++) {
            for (int x = 0; x < MATH_W; x++) {
                double best = Double.MAX_VALUE;
                double second = Double.MAX_VALUE;
                for (int i = 0; i < cellCount; i++) {
                    double dx = toroidalDelta(x, px[i], MATH_W);
                    double dy = toroidalDelta(y, py[i], MATH_H);
                    double distSq = dx * dx + dy * dy;
                    if (distSq < best) {
                        second = best;
                        best = distSq;
                    } else if (distSq < second) {
                        second = distSq;
                    }
                }
                // Points where the nearest and second-nearest cell seeds are
                // almost equidistant sit right on a Voronoi cell wall. 2.0
                // (vs. the previous 1.6) draws a thicker starter wall so the
                // type-delta reaction below has a clear seam to lock onto
                // rather than letting thin single-pixel walls dissolve.
                double margin = Math.sqrt(second) - Math.sqrt(best);
                if (margin < 2.0) {
                    gridU[y][x] = 0.50;
                    gridV[y][x] = 0.25 + rand.nextDouble() * 0.05;
                }
            }
        }
    }

    // Shortest distance between two coordinates on a wrapped (toroidal) axis
    // of the given size - matches the wraparound boundary the reaction step
    // already uses, so cell walls connect seamlessly across the grid edges.
    private static double toroidalDelta(int a, int b, int size) {
        int d = Math.abs(a - b);
        return Math.min(d, size - d);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // FAST FORWARD STEP: Process steps sequentially before redrawing once
        int simulationSubSteps = 30;
        for (int step = 0; step < simulationSubSteps; step++) {
            runReactionDiffusionStep();
        }

        // --- SUB-SAMPLED 2x2 BLOCK RENDERING PASS ---
        // Down-samples the 280x84 math array onto 140x42 console canvas
        for (int displayY = 0; displayY < DISPLAY_H; displayY++) {
            for (int displayX = 0; displayX < DISPLAY_W; displayX++) {

                // Map display point to the 2x2 quadrant core coordinates in math space
                int mx = displayX * 2;
                int my = displayY * 2;

                // Average the catalyst intensity values across the block quad
                double vVal = (gridV[my][mx] + gridV[my][mx + 1] + gridV[my + 1][mx] + gridV[my + 1][mx + 1]) / 4.0;

                int offset = displayX + DISPLAY_W * displayY;
                String colorCode;
                char glyph;

                switch (variant) {
                    case 0: // ZEBRA STRIPES - pure black on warm off-white, high contrast
                        if (vVal > 0.16) { colorCode = rgb(15, 15, 15); glyph = '█'; }
                        else { colorCode = rgb(240, 238, 230); glyph = '█'; }
                        break;
                    case 1: // LEOPARD ROSETTES - dark broken ring, warm gold core, tan base coat
                        if (vVal > 0.36) { colorCode = rgb(48, 30, 18); glyph = '█'; } // Dark umber ring
                        else if (vVal > 0.12) { colorCode = rgb(176, 108, 40); glyph = '▓'; } // Warm gold-brown interior
                        else { colorCode = rgb(224, 186, 128); glyph = '▒'; } // Pale tan base coat
                        break;
                    case 2: // CHEETAH SPOTS - small solid black-brown spots on tawny cream
                        if (vVal > 0.33) { colorCode = rgb(28, 20, 15); glyph = '█'; }
                        else { colorCode = rgb(226, 178, 108); glyph = '░'; }
                        break;
                    case 3: // GIRAFFE VEINS - deep chestnut patches, pale cream reticulation
                        if (vVal > 0.26) { colorCode = rgb(247, 235, 205); glyph = '░'; }
                        else { colorCode = rgb(107, 58, 27); glyph = '█'; }
                        break;
                    default:
                        colorCode = ""; glyph = ' ';
                        break;
                }
                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }

    private static String rgb(int r, int g, int b) {
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
    }

    private void runReactionDiffusionStep() {
        for (int y = 0; y < MATH_H; y++) {
            // Precompute wrapped vertical indices to enforce repeating boundary structures safely
            int yN = (y - 1 + MATH_H) % MATH_H;
            int yS = (y + 1 + MATH_H) % MATH_H;

            for (int x = 0; x < MATH_W; x++) {
                double u = gridU[y][x];
                double v = gridV[y][x];

                int xW = (x - 1 + MATH_W) % MATH_W;
                int xE = (x + 1 + MATH_W) % MATH_W;

                double lapU = (gridU[yN][x] + gridU[yS][x]) * LAP_NS
                            + (gridU[y][xW] + gridU[y][xE]) * LAP_EW
                            + (gridU[yN][xW] + gridU[yN][xE] + gridU[yS][xW] + gridU[yS][xE]) * LAP_DIAG
                            + u * LAP_CENTER;

                double lapV = (gridV[yN][x] + gridV[yS][x]) * LAP_NS
                            + (gridV[y][xW] + gridV[y][xE]) * LAP_EW
                            + (gridV[yN][xW] + gridV[yN][xE] + gridV[yS][xW] + gridV[yS][xE]) * LAP_DIAG
                            + v * LAP_CENTER;

                double reaction = u * v * v;
                double du = (Du * lapU) - reaction + (activeFeed * (1.0 - u));
                double dv = (activeDv * lapV) + reaction - ((activeFeed + activeKill) * v);

                double nextUVal = u + du; double nextVVal = v + dv;
                if (nextUVal < 0.0) nextUVal = 0.0; else if (nextUVal > 1.0) nextUVal = 1.0;
                if (nextVVal < 0.0) nextVVal = 0.0; else if (nextVVal > 1.0) nextVVal = 1.0;

                nextU[y][x] = nextUVal; nextV[y][x] = nextVVal;
            }
        }

        double[][] tempU = gridU; gridU = nextU; nextU = tempU;
        double[][] tempV = gridV; gridV = nextV; nextV = tempV;
    }
}