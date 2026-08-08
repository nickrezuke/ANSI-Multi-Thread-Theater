public class TextFallLoader extends Loader {
    private static final StatusStage[] TEXTFALL_STAGES = {
        new StatusStage(15, "Initializing construct:"),
        new StatusStage(40, "Establishing secure proxy:"),
        new StatusStage(65, "Bypassing mainframe firewall:"),
        new StatusStage(85, "Injecting digital rain vectors:"),
        new StatusStage(100, "System Override Complete!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // Code symbol pool (Safe 16-bit primitives: numbers, symbols, and sharp characters)
    private static final String GLYPHS = "0123456789$+-*%=<>!&_#@XYZ?ΔΩΨΞ";

    // Layer-Based Stream System
    private final double[] heads = new double[WIDTH];
    private final double[] speeds = new double[WIDTH];
    private final int[] lengths = new int[WIDTH];
    private final int[] layerDepth = new int[WIDTH]; // 0 = Background, 1 = Midground, 2 = Foreground

    // Dynamic grid memory to prevent character jittering (stores persistent text per cell)
    private final char[][] gridGlyphs = new char[HEIGHT][WIDTH];

    // High-Saturation 256-Color Gradient Systems
    private static final String COLOR_HEAD = "\u001B[38;5;231m"; // Blazing White Head

    // Foreground Stream Colors (Fast, thick, bright emerald)
    private static final String FG_HIGH = "\u001B[38;5;46m";  // Bright Neon Green
    private static final String FG_MID  = "\u001B[38;5;28m";  // Standard Green
    private static final String FG_LOW  = "\u001B[38;5;22m";  // Dark Green

    // Background Stream Colors (Slow, thin, dim ghostly greens)
    private static final String BG_HIGH = "\u001B[38;5;28m";  // Muted Green Head
    private static final String BG_MID  = "\u001B[38;5;22m";  // Dim Green Body
    private static final String BG_LOW  = "\u001B[38;5;234m"; // Dark Charcoal Green Tail

    public TextFallLoader() {
        // This uses 80x22 specifically
        super(TEXTFALL_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // Initialize the tracking arrays
        for (int i = 0; i < WIDTH; i++) {
            resetColumn(i, -Math.random() * HEIGHT);
        }
        for (char[] row : gridGlyphs) {
            for(int i = 0; i < row.length; i++) {
                row[i] = ' ';
            }
        }
    }

    private void resetColumn(int col, double startingY) {
        heads[col] = startingY;
        
        // Randomly split the column into one of three visual depths
        double depthRoll = Math.random();
        if (depthRoll > 0.65) {
            layerDepth[col] = 2; // FOREGROUND: Very Fast, Long Trails
            speeds[col] = 0.28 + Math.random() * 0.15;
            lengths[col] = 12 + (int) (Math.random() * 8);
        } else if (depthRoll > 0.30) {
            layerDepth[col] = 1; // MIDGROUND: Normal Speed, Medium Trails
            speeds[col] = 0.14 + Math.random() * 0.10;
            lengths[col] = 8 + (int) (Math.random() * 6);
        } else {
            layerDepth[col] = 0; // BACKGROUND: Slow, Short Ghost Trails
            speeds[col] = 0.05 + Math.random() * 0.05;
            lengths[col] = 4 + (int) (Math.random() * 4);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;

        // 1. Advance stream ticks and manage boundary resets
        for (int x = 0; x < width; x++) {
            heads[x] += speeds[x];
            
            if (heads[x] - lengths[x] > height) {
                resetColumn(x, -Math.random() * 5);
            }
        }

        // 2. Render Canvas Processing Loop
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int o = x + width * y;
                
                double currentHeadY = heads[x];
                int trailLength = lengths[x];
                int depth = layerDepth[x];

                // Check if the current pixel grid coordinate lies inside an active drop stream
                if (y <= currentHeadY && y > currentHeadY - trailLength) {
                    
                    // Assign a pseudo-depth based on the stream's tier layer
                    // Foreground streams (2.0) cleanly stamp directly over background streams (1.0)
                    double streamZ = (double) depth + 1.0;

                    if (streamZ > zBuffer[o]) {
                        zBuffer[o] = streamZ;

                        // Shimmer effect: 90% chance to hold text, 10% chance to mutate character
                        // This prevents full-screen jittering and mimics crisp streaming text terminals
                        if (gridGlyphs[y][x] == ' ' || Math.random() < 0.10) {
                            gridGlyphs[y][x] = GLYPHS.charAt((int) (Math.random() * GLYPHS.length()));
                        }
                        char activeGlyph = gridGlyphs[y][x];

                        // Gradient processing pipeline based on distance from the stream head
                        int trailingDistance = (int) currentHeadY - y;
                        String chosenColor;

                        if (depth == 2) { // --- FOREGROUND COLOR CHANNEL SPECTRUM ---
                            if (trailingDistance == 0)      chosenColor = COLOR_HEAD;
                            else if (trailingDistance < 3)  chosenColor = FG_HIGH;
                            else if (trailingDistance < trailLength * 0.5) chosenColor = FG_MID;
                            else                            chosenColor = FG_LOW;
                        } else { // --- BACKGROUND/MIDGROUND GHOST SPECTRUM ---
                            if (trailingDistance == 0)      chosenColor = FG_HIGH; // Muted head
                            else if (trailingDistance < trailLength * 0.4) chosenColor = BG_HIGH;
                            else if (trailingDistance < trailLength * 0.7) chosenColor = BG_MID;
                            else                            chosenColor = BG_LOW;
                        }

                        outputBuffer[o] = chosenColor + activeGlyph + RESET;
                    }
                }
            }
        }
    }
}
