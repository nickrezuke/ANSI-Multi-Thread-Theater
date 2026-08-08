public class NyanCatLoader extends Loader {
    private static final StatusStage[] NYAN_STAGES = {
        new StatusStage(25, "Baking neon pop-tart pastry matrix:"),
        new StatusStage(50, "Extruding multi-channel rainbow tracks:"),
        new StatusStage(75, "Glittering 8-bit cosmic starfields:"),
        new StatusStage(100, "Nyan Cat Hyper-Drive Active!")
    };

    private double timeClock = 0.0;

    // High-Saturation 8-Bit Color Palette Registers
    private static final String COLOR_SPACE_VOID = "\u001B[38;5;18m";  // Deep Cosmic Blue Void
    private static final String COLOR_STAR_GLOW  = "\u001B[38;5;255m"; // Pure White Strobe Stars
    
    // Core Nyan Cat structural assets
    private static final String COLOR_CAT_GRAY   = "\u001B[38;5;244m"; // Russian Gray Cat Fur
    private static final String COLOR_TART_PINK  = "\u001B[38;5;218m"; // Frosting Pink
    private static final String COLOR_TART_CRUST = "\u001B[38;5;180m"; // Golden Pastry Crust
    private static final String COLOR_ROSE_DOT   = "\u001B[38;5;197m"; // Sprinkle Magenta Pink

    // Iconic 6-Color Rainbow Row Assets
    private static final String[] RAINBOW_COLORS = {
        "\u001B[38;5;196m", // 0: Red
        "\u001B[38;5;214m", // 1: Orange
        "\u001B[38;5;226m", // 2: Yellow
        "\u001B[38;5;46m",  // 3: Green
        "\u001B[38;5;21m",  // 4: Deep Blue
        "\u001B[38;5;129m"  // 5: Purple
    };

    // Typographic layout controls
    private static final char CH_SOLID = '\u2588'; // █ Solid 8-bit block sprite element
    private static final char CH_HALF  = '\u2584'; // ▄ Half-height block for detail definitions
    private static final char CH_STAR  = '\u2605'; // ★ Cosmic background star points

    public NyanCatLoader() {
        // This uses 80x22 specifically
        super(NYAN_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.045; // Controls cat bobbing, rainbow waving, and star scrolling speeds

        int width = 80;
        int height = 22;

        // 1. PROCEDURAL LOOPS & SPRITE RENDERING COORDINATES
        // Sits near the right-center space while everything scrolls past leftward
        double catCenterX = 45.0;
        // Cat bobs up and down rhythmically over time
        double catBobY = 10.0 + Math.round(0.6 * Math.sin(timeClock * 2.5));

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // ==================== LAYER 1: NYAN CAT SPRITE MATRIX (Z-Depth: 0.90) ====================
                double cx = x - catCenterX;
                double cy = y - catBobY;

                boolean isCatPixel = false;
                String catSpriteColor = "";
                char catSpriteChar = CH_SOLID;

                // A. Mathematical model carving the iconic rectangular pop-tart body
                boolean insidePastryBody = (cx >= -11 && cx <= 1) && (cy >= -4 && cy <= 3);
                if (insidePastryBody) {
                    isCatPixel = true;
                    boolean isCrustEdge = (cx == -11 || cx == 1 || cy == -4 || cy == 3);
                    
                    if (isCrustEdge) {
                        catSpriteColor = COLOR_TART_CRUST;
                    } else {
                        // Fill pop-tart frosting with random magenta sprinkles via a coordinate map
                        boolean isSprinkle = (cx + cy * 7) % 5 == 0;
                        catSpriteColor = isSprinkle ? COLOR_ROSE_DOT : COLOR_TART_PINK;
                    }
                }
                // B. Carving the gray cat head side profile
                else if ((cx >= 2 && cx <= 9) && (cy >= -2 && cy <= 3)) {
                    isCatPixel = true;
                    catSpriteColor = COLOR_CAT_GRAY;

                    // Inject the pixelated flat black eyes
                    if ((cx == 4 || cx == 7) && cy == 0) {
                        catSpriteColor = "\u001B[38;5;16m"; // Gloss Black
                    }
                    // Inject the pink cheek blush dots
                    else if ((cx == 3 || cx == 8) && cy == 1) {
                        catSpriteColor = COLOR_ROSE_DOT;
                    }
                }
                // C. Pointy cat ears setup
                else if ((cy == -3) && (cx == 3 || cx == 4 || cx == 7 || cx == 8)) {
                    isCatPixel = true;
                    catSpriteColor = COLOR_CAT_GRAY;
                }
                // D. Four running gray legs
                else if (cy == 4 && (cx == -9 || cx == -8 || cx == -2 || cx == -1 || cx == 4 || cx == 5)) {
                    isCatPixel = true;
                    catSpriteColor = COLOR_CAT_GRAY;
                    catSpriteChar = CH_HALF; // Make feet shorter
                }
                // E. Waving gray tail behind the pastry
                else if (cx >= -15 && cx <= -12) {
                    double tailSway = Math.sin(timeClock * 3.0 + cx);
                    if (Math.abs(cy - Math.round(tailSway)) < 1.0) {
                        isCatPixel = true;
                        catSpriteColor = COLOR_CAT_GRAY;
                    }
                }

                if (isCatPixel && 0.90 > zBuffer[index]) {
                    zBuffer[index] = 0.90;
                    outputBuffer[index] = catSpriteColor + catSpriteChar + RESET;
                    continue;
                }

                // ==================== LAYER 2: WAVING NYAN RAINBOW TRAIL (Z-Depth: 0.70) ====================
                // The rainbow trail spans horizontally from the left border up to the pop-tart connection point
                if (x < catCenterX - 10) {
                    // Generate a continuous sine wave oscillation that moves across the X axis
                    double rainbowWaveY = 10.0 + Math.round(1.2 * Math.sin(timeClock * 2.5 + x * 0.28));
                    
                    // The rainbow track is exactly 6 vertical rows tall
                    int rainbowRowOffset = y - (int) rainbowWaveY + 3;

                    if (rainbowRowOffset >= 0 && rainbowRowOffset < 6) {
                        if (0.70 > zBuffer[index]) {
                            zBuffer[index] = 0.70;
                            outputBuffer[index] = RAINBOW_COLORS[rainbowRowOffset] + CH_SOLID + RESET;
                        }
                        continue;
                    }
                }

                // ==================== LAYER 3: SCROLLING COSMIC STARFIELD (Z-Depth: 0.30) ====================
                // Generate a grid of background stars that wrap and scroll horizontally from right to left
                double scrollX = (x + (timeClock * 16.0)) % width;
                // Pseudo-random math grid check to scatter stars evenly across the frame
                double starHash = Math.sin(Math.floor(scrollX) * 41.13 + y * 97.45);
                
                if (starHash > 0.978 && 0.30 > zBuffer[index]) {
                    zBuffer[index] = 0.30;
                    
                    // Simple blinking pulse tracking
                    char starSymbol = (Math.sin(timeClock * 4.0 + x) > 0.0) ? CH_STAR : '\u00B7';
                    outputBuffer[index] = COLOR_STAR_GLOW + starSymbol + RESET;
                    continue;
                }

                // ==================== LAYER 4: DARK COSMIC SPACE BASE VOID (Z-Depth: 0.01) ====================
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;
                    outputBuffer[index] = COLOR_SPACE_VOID + "\u2588" + RESET;
                }
            }
        }
    }
}
