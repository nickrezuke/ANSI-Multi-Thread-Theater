// TODO Finish this Grey-Scott Diffusion Reaction Model.  Its not rendering properly / visually cool enough

public class ReactionDiffusionLoader extends Loader {
    private static final StatusStage[] REFLECTIVE_STAGES = {
        new StatusStage(25, "Seeding biological DNA markers:"),
        new StatusStage(55, "Synthesizing skin tissue matrix:"),
        new StatusStage(85, "Mutating reaction-diffusion coat:"),
        new StatusStage(100, "Animal Morphology Pattern Stable!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // Dual-buffered fields (U = Feedstock source, V = Catalyst)
    private double[][] gridU = new double[HEIGHT][WIDTH];
    private double[][] gridV = new double[HEIGHT][WIDTH];
    private double[][] nextU = new double[HEIGHT][WIDTH];
    private double[][] nextV = new double[HEIGHT][WIDTH];

    // Standard baseline diffusion rates scaled for stable numerical time-stepping
    private static final double Du = 1.000;
    private static final double Dv = 0.500;

    private double activeFeed;
    private double activeKill;
    private int variant;

    public ReactionDiffusionLoader() {
        super(REFLECTIVE_STAGES);
    }

    @Override
    protected void initialize() {
        variant = (int) (Math.random() * 4);
        variant = 3;
        switch (variant) {
            case 0: // ZEBRA STRIPES (Labyrinthine Turing Waves)
                activeFeed = 0.035; 
                activeKill = 0.060;
                break;
            case 1: // LEOPARD ROSETTES (Interlocking Ring Formations)
                activeFeed = 0.029; 
                activeKill = 0.057;
                break;
            case 2: // CHEETAH SPOTS (Stable, Isolated Circular Points)
                activeFeed = 0.022; 
                activeKill = 0.055;
                break;
            case 3: // GIRAFFE VEINS (Polygonal Networks)
                activeFeed = 0.0545; 
                activeKill = 0.062;
                break;
        }

        // Set baseline chemical environment (U completely full, V completely empty)
        for (int y = 0; y < HEIGHT; y++) {
            for(int i = 0; i < gridU[y].length; i++) {
                gridU[y][i] = 1.0;
            }
            for(int i = 0; i < gridV[y].length; i++) {
                gridV[y][i] = 0.0;
            }
            for(int i = 0; i < nextU[y].length; i++) {
                nextU[y][i] = 1.0;
            }
            for(int i = 0; i < nextV[y].length; i++) {
                nextV[y][i] = 0.0;
            }
        }

        // Seed dense squares of catalyst V to trigger chemical reaction fronts
        int totalDrops = (variant == 2) ? 6 : 12;
        for (int i = 0; i < totalDrops; i++) {
            int seedY = (int) (Math.random() * (HEIGHT - 8)) + 4;
            int seedX = (int) (Math.random() * (WIDTH - 16)) + 8;
            
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    gridU[seedY + dy][seedX + dx] = 0.50;
                    gridV[seedY + dy][seedX + dx] = 0.25;
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Run 10 math cycles per frame to give the reaction visible evolution speed
        int simulationSubSteps = 10; 
        
        for (int step = 0; step < simulationSubSteps; step++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    double u = gridU[y][x];
                    double v = gridV[y][x];
                    
                    // --- FIXED DISCRETE LAPLACIAN RADIAL STENCIL ---
                    // Weights sum to exactly 0.0, completely preventing the math from leaking or blanking out.
                    // Compensates for standard 2:1 vertical terminal text aspect ratios.
                    double lapU = 0.0;
                    double lapV = 0.0;

                    // North/South neighbors (Vertical weight = 0.20)
                    int yN = (y - 1 + HEIGHT) % HEIGHT;
                    int yS = (y + 1 + HEIGHT) % HEIGHT;
                    lapU += (gridU[yN][x] + gridU[yS][x]) * 0.20;
                    lapV += (gridV[yN][x] + gridV[yS][x]) * 0.20;

                    // East/West neighbors (Horizontal weight = 0.25)
                    int xW = (x - 1 + WIDTH) % WIDTH;
                    int xE = (x + 1 + WIDTH) % WIDTH;
                    lapU += (gridU[y][xW] + gridU[y][xE]) * 0.25;
                    lapV += (gridV[y][xW] + gridV[y][xE]) * 0.25;

                    // Diagonal corner neighbors (Corner weight = 0.05)
                    lapU += (gridU[yN][xW] + gridU[yN][xE] + gridU[yS][xW] + gridU[yS][xE]) * 0.05;
                    lapV += (gridV[yN][xW] + gridV[yN][xE] + gridV[yS][xW] + gridV[yS][xE]) * 0.05;

                    // Self correction center point (Center weight = -1.10)
                    lapU += u * -1.10;
                    lapV += v * -1.10;

                    // Gray-Scott Evolution Math equations
                    double reaction = u * v * v;
                    double du = (Du * lapU) - reaction + (activeFeed * (1.0 - u));
                    double dv = (Dv * lapV) + reaction - ((activeFeed + activeKill) * v);

                    // Clamp numerical artifacts safely between operational [0.0, 1.0] margins
                    double nextUVal = u + du;
                    double nextVVal = v + dv;
                    
                    if (nextUVal < 0.0) nextUVal = 0.0; else if (nextUVal > 1.0) nextUVal = 1.0;
                    if (nextVVal < 0.0) nextVVal = 0.0; else if (nextVVal > 1.0) nextVVal = 1.0;

                    nextU[y][x] = nextUVal;
                    nextV[y][x] = nextVVal;
                }
            }

            // Clean, instantaneous double-buffer reference updates
            double[][] tempU = gridU; gridU = nextU; nextU = tempU;
            double[][] tempV = gridV; gridV = nextV; nextV = tempV;
        }

        // --- RENDER SPECIES PHENOTYPE STYLING TO CANVAS BUFFER ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int offset = x + WIDTH * y;
                double vVal = gridV[y][x];
                String colorCode;
                char glyph;

                switch (variant) {
                    case 0: // ZEBRA (Stark monochromatic layout)
                        if (vVal > 0.25) {
                            colorCode = "\u001B[38;5;232m"; // Midnight Black stripes
                            glyph = '█';
                        } else {
                            colorCode = "\u001B[38;5;255m"; // Crisp clean White background hide
                            glyph = '█';
                        }
                        break;

                    case 1: // LEOPARD ROSETTES (Dark rings containing golden cores over sand)
                        if (vVal > 0.35) {
                            colorCode = "\u001B[38;5;235m"; // Dark charcoal outer rosettes rings
                            glyph = '█';
                        } else if (vVal > 0.15) {
                            colorCode = "\u001B[38;5;172m"; // Rich gold inner centers
                            glyph = '▓';
                        } else {
                            colorCode = "\u001B[38;5;223m"; // Light sandy desert tan backing
                            glyph = '▒';
                        }
                        break;

                    case 2: // CHEETAH SPOTS (Solid circular points over deep amber hide fields)
                        if (vVal > 0.22) {
                            colorCode = "\u001B[38;5;16m";  // Pure opaque black dot points
                            glyph = '█';
                        } else {
                            colorCode = "\u001B[38;5;214m"; // Saturated deep amber skin fields
                            glyph = '▒';
                        }
                        break;

                    case 3: // GIRAFFE VEINS (Broad chestnut polygons separated by thin pale veins)
                        if (vVal > 0.20) {
                            colorCode = "\u001B[38;5;94m";  // Massive chestnut brown plates
                            glyph = '█';
                        } else {
                            colorCode = "\u001B[38;5;230m"; // Ivory/cream interstitial network veins
                            glyph = '░';
                        }
                        break;

                    default:
                        colorCode = "";
                        glyph = ' ';
                        break;
                }

                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }
}
