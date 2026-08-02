// TODO Finish this one.  Its not rendering properly

import java.util.Arrays;

public class ReactionDiffusionLoader extends Loader {
    private static final StatusStage[] REFLECTIVE_STAGES = {
        new StatusStage(25, "Seeding biological DNA markers:"),
        new StatusStage(55, "Synthesizing skin tissue matrix:"),
        new StatusStage(85, "Mutating reaction-diffusion coat:"),
        new StatusStage(100, "Animal Morphology Pattern Stable!")
    };

    private static final char[] SHADE_RAMP = { '█', '▓', '▒', '░', '⁜', ':', '-', '.', ' ' };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    private double[][] gridU = new double[HEIGHT][WIDTH];
    private double[][] gridV = new double[HEIGHT][WIDTH];
    private double[][] nextU = new double[HEIGHT][WIDTH];
    private double[][] nextV = new double[HEIGHT][WIDTH];

    // Diffusion constants
    private static final double Du = 0.140;  
    private static final double Dv = 0.070;  
    
    // Active parameters configured at initialization
    private double activeFeed; 
    private double activeKill; 
    private String patternName = "";

    public ReactionDiffusionLoader() {
        super(REFLECTIVE_STAGES);
    }

    @Override
    protected void initialize() {
        // Step 1: Select a random animal morphology phenotype preset
        int variant = (int) (Math.random() * 4);
        
        switch (variant) {
            case 0: // ZEBRA STRIPES (Waves/Labyrinths)
                activeFeed = 0.042;
                activeKill = 0.065;
                patternName = "ZEBRA STRIPES";
                break;
            case 1: // LEOPARD SPOTS (Dense, isolated dots)
                activeFeed = 0.0367;
                activeKill = 0.0649;
                patternName = "LEOPARD SPOTS";
                break;
            case 2: // JAGUAR ROSETTES (Hollow rings/double circles)
                activeFeed = 0.022;
                activeKill = 0.055;
                patternName = "JAGUAR ROSETTES";
                break;
            case 3: // PUFFERFISH CORAL (Winding chaotic channels)
                activeFeed = 0.0545;
                activeKill = 0.062;
                patternName = "PUFFERFISH CORAL";
                break;
        }

        // Step 2: Clear the soup environment matrix
        for (int y = 0; y < HEIGHT; y++) {
            Arrays.fill(gridU[y], 1.0);
            Arrays.fill(gridV[y], 0.0);
        }

        // Step 3: Seed random high-frequency clusters across the skin to act as organic hair follicles
        // Symmetrical macro seeding doesn't happen in nature; scattered nodes spark beautiful variance.
        for (int i = 0; i < 18; i++) {
            int seedY = (int) (Math.random() * (HEIGHT - 6)) + 3;
            int seedX = (int) (Math.random() * (WIDTH - 12)) + 6;
            
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    if (Math.random() > 0.3) {
                        gridU[seedY + dy][seedX + dx] = 0.50;
                        gridV[seedY + dy][seedX + dx] = 0.25;
                    }
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int simulationSubSteps = 10;
        
        for (int step = 0; step < simulationSubSteps; step++) {
            // FIX: Ambient Mutation Injection.
            // Spontaneously drops ultra-tiny drops of catalyst V at random locations.
            // This destabilizes local plateaus, causing rows of stripes or spots to migrate
            // and shift dynamically across the terminal instead of completely freezing!
            if (Math.random() > 0.85) {
                int rx = (int)(Math.random() * WIDTH);
                int ry = (int)(Math.random() * HEIGHT);
                gridV[ry][rx] = 0.40;
            }

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    
                    double u = gridU[y][x];
                    double v = gridV[y][x];

                    double lapU = 0.0;
                    double lapV = 0.0;

                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = (y + dy + HEIGHT) % HEIGHT;

                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = (x + dx + WIDTH) % WIDTH;

                            // Text terminal aspect ratio 2.1:1 width-to-height scalar compensation weights
                            double weight = 0.0;
                            if (dy == 0 && dx == 0)                     weight = -1.0; 
                            else if (dy == 0 && Math.abs(dx) == 1)      weight = 0.32; 
                            else if (Math.abs(dy) == 1 && dx == 0)      weight = 0.08; 
                            else if (Math.abs(dy) == 1 && Math.abs(dx) == 1) weight = 0.05; 

                            lapU += gridU[ny][nx] * weight;
                            lapV += gridV[ny][nx] * weight;
                        }
                    }

                    double reactionRate = u * v * v;
                    
                    nextU[y][x] = u + (Du * lapU - reactionRate + activeFeed * (1.0 - u));
                    nextV[y][x] = v + (Dv * lapV + reactionRate - (activeFeed + activeKill) * v);

                    nextU[y][x] = Math.max(0.0, Math.min(1.0, nextU[y][x]));
                    nextV[y][x] = Math.max(0.0, Math.min(1.0, nextV[y][x]));
                }
            }

            double[][] tempU = gridU; gridU = nextU; nextU = tempU;
            double[][] tempV = gridV; gridV = nextV; nextV = tempV;
        }

        // --- RENDER CURRENT CONCENTRATIONS TO CANVAS ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int offset = x + WIDTH * y;
                
                double vConcentration = gridV[y][x];

                int shadeIdx = (int) (vConcentration * 4.5 * (SHADE_RAMP.length - 1));
                shadeIdx = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIdx));
                char glyph = SHADE_RAMP[shadeIdx];

                // Animal Coat Color Palettes mapping concentration ranges
                String colorCode;
                if (vConcentration > 0.28) {
                    // Deep pigment core (Dark brown/black spots/stripes)
                    colorCode = "\u001B[38;5;234m"; 
                } else if (vConcentration > 0.14) {
                    // Intermediate boundary glow
                    if (patternName.equals("LEOPARD SPOTS") || patternName.equals("JAGUAR ROSETTES")) {
                        colorCode = "\u001B[38;5;172m"; // Classic Safari Tawny Tan Gold
                    } else {
                        colorCode = "\u001B[38;5;242m"; // Monochromatic Zebra slate gray transit
                    }
                } else if (vConcentration > 0.04) {
                    // Ambient halo shadow borders
                    colorCode = "\u001B[38;5;237m"; 
                } else {
                    // Dominant background coat color
                    if (patternName.equals("ZEBRA STRIPES")) {
                        colorCode = "\u001B[38;5;255m"; // Stark White hide background
                        glyph = '█';
                    } else {
                        colorCode = "\u001B[38;5;222m"; // Creamy Leopard Sand backdrop hide
                        glyph = '▒';
                    }
                }

                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }

        // Dynamic title frame overlay printing the selected active DNA type onto row 0 corner bounds
        String panelLabel = "[" + patternName + " PHENOTYPE ENGAGED]";
        for (int i = 0; i < panelLabel.length(); i++) {
            if (i < 80) outputBuffer[i] = "\u001B[38;5;248m" + panelLabel.charAt(i) + RESET;
        }
    }
}
