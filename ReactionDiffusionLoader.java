// TODO: Improve the accuracy of these reactions to look more like the animal prints they represent

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
    private static final double DV_DEFAULT = 0.500;
    private static final double DV_LEOPARD = 0.400;

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
        activeDv = DV_DEFAULT;
        switch (variant) {
            case 0: // ZEBRA STRIPES
                activeFeed = 0.035; activeKill = 0.060;
                break;
            case 1: // LEOPARD ROSETTES 
                activeFeed = 0.010; activeKill = 0.048; activeDv = DV_LEOPARD;
                break;
            case 2: // CHEETAH SPOTS
                activeFeed = 0.014; activeKill = 0.050;
                break;
            case 3: // GIRAFFE VEINS
                activeFeed = 0.0545; activeKill = 0.062;
                break;
        }

        for (int y = 0; y < MATH_H; y++) {
            for (int x = 0; x < MATH_W; x++) {
                gridU[y][x] = 1.0; gridV[y][x] = 0.0;
            }
        }

        // Seeds scaled out to match high-resolution layout requirements
        Random rand = new Random();
        int totalDrops = (variant == 0) ? 1 : (variant == 1) ? 24 : (variant == 2) ? 65 : 40;

        if (variant == 0) { // Zebra wave trigger lines
            for (int y = MATH_H/2 - 5; y < MATH_H/2 + 5; y++) {
                for (int x = MATH_W/2 - 40; x < MATH_W/2 + 40; x++) {
                    gridU[y][x] = 0.50; gridV[y][x] = 0.25 + rand.nextDouble() * 0.15;
                }
            }
        } else { // Point-clump initial drops
            for (int i = 0; i < totalDrops; i++) {
                int sy = 6 + rand.nextInt(MATH_H - 12);
                int sx = 12 + rand.nextInt(MATH_W - 24);
                int size = (variant == 1) ? 4 : 2; 
                for (int dy = -size; dy <= size; dy++) {
                    for (int dx = -size; dx <= size; dx++) {
                        gridU[sy+dy][sx+dx] = 0.50; gridV[sy+dy][sx+dx] = 0.25;
                    }
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // FAST FORWARD STEP: Process 40 math computation steps sequentially before redrawing once
        int simulationSubSteps = 40; 
        for (int step = 0; step < simulationSubSteps; step++) {
            runReactionDiffusionStep();
        }

        // --- SUB-SAMPLED 2x2 BLOCK RENDERING PASS ---
        // Down-samples the 280x84 math array onto your 140x42 console canvas
        for (int displayY = 0; displayY < DISPLAY_H; displayY++) {
            for (int displayX = 0; displayX < DISPLAY_W; displayX++) {
                
                // Map display point to the 2x2 quadrant core coordinates in math space
                int mx = displayX * 2;
                int my = displayY * 2;

                // Average the catalyst intensity values across the block quad
                double vVal = (gridV[my][mx] + gridV[my][mx+1] + gridV[my+1][mx] + gridV[my+1][mx+1]) / 4.0;

                int offset = displayX + DISPLAY_W * displayY;
                String colorCode;
                char glyph;

                switch (variant) {
                    case 0: // ZEBRA STRIPES
                        if (vVal > 0.22) { colorCode = "\u001B[38;5;232m"; glyph = '█'; }
                        else { colorCode = "\u001B[38;5;255m"; glyph = '█'; }
                        break;
                    case 1: // PERFECT MULTI-RING LEOPARD ROSETTES
                        if (vVal > 0.34) { colorCode = "\u001B[38;5;234m"; glyph = '█'; }      // Dark outer wall
                        else if (vVal > 0.12) { colorCode = "\u001B[38;5;172m"; glyph = '▓'; } // Rich gold interior centers
                        else { colorCode = "\u001B[38;5;223m"; glyph = '▒'; }                  // Sandy base coat
                        break;
                    case 2: // CHEETAH SPOTS
                        if (vVal > 0.20) { colorCode = "\u001B[38;5;16m"; glyph = '█'; }
                        else { colorCode = "\u001B[38;5;214m"; glyph = '▒'; }
                        break;
                    case 3: // GIRAFFE VEINS
                        if (vVal > 0.22) { colorCode = "\u001B[38;5;230m"; glyph = '░'; }
                        else { colorCode = "\u001B[38;5;94m"; glyph = '█'; }
                        break;
                    default:
                        colorCode = ""; glyph = ' ';
                        break;
                }
                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
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
