public class FrutigerAeroLoader extends Loader {
    private static final StatusStage[] AERO_STAGES = {
        new StatusStage(25, "Initializing glossy glass shaders:"),
        new StatusStage(50, "Generating organic eco-hill terrains:"),
        new StatusStage(75, "Spawning volumetric oxygen bubble matrices:"),
        new StatusStage(100, "Frutiger Aero Eco-Futurism Protocol Active!")
    };

    private double timeClock = 0.0;

    // High-Saturation 256-Color Vibrant Frutiger Aero Base Values
    private static final int[] RGB_SKY_TOP   = { 51,  153, 255 }; // Crisp Sky Blue
    private static final int[] RGB_SKY_MID   = { 102, 204, 255 }; // Glossy Cyan
    private static final int[] RGB_SKY_BTM   = { 153, 235, 255 }; // Blinding Clean Aqua
    private static final int[] RGB_BUBBLE    = { 240, 250, 255 }; // Pristine White Gloss Core

    // Glossy Tropical Fish Palette Registers
    private static final int[] RGB_FISH_CORE = { 255, 110, 20  }; // High-Gloss Aero Orange
    private static final int[] RGB_FISH_BELLY = { 255, 215, 0   }; // Vibrant Electric Yellow

    private static final String COLOR_HILL_BACK = "\u001B[38;5;76m";  // Soft Aurora Green
    private static final String COLOR_HILL_FRNT = "\u001B[38;5;40m";  // Vivid Eco Grass Green

    public FrutigerAeroLoader() {
        // This uses 80x22 specifically
        super(AERO_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.035; // Controls hills, bubbles, and fish movement tracking

        int width = 80;
        int height = 22;

        // Calculate the horizontal loop position for the swimming fish
        // Moves from right to left cleanly across the canvas grid
        double fishCenterX = width - ((timeClock * 14.0) % (width + 20));
        double fishCenterY = 7.0 + 2.5 * Math.sin(timeClock * 0.6); // Gentle vertical swimming glide path

        for (int y = 0; y < height; y++) {
            // Pre-calculate the exact static sky background color at this row for blending
            int[] currentSkyRGB = RGB_SKY_BTM;
            if (y < 5)       currentSkyRGB = RGB_SKY_TOP;
            else if (y < 9)  currentSkyRGB = RGB_SKY_MID;

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // --- LAYER 1: FOREGROUND ECO-HILL (Z-Depth: 0.80) ---
                double frontHillWave = 15.0 + 3.0 * Math.sin(x * 0.08 + timeClock * 0.4);
                if (y >= (int) frontHillWave) {
                    if (0.80 > zBuffer[index]) {
                        zBuffer[index] = 0.80;
                        char hillChar = (y == (int) frontHillWave) ? '\u2592' : '\u2588';
                        outputBuffer[index] = COLOR_HILL_FRNT + hillChar + RESET;
                    }
                    continue;
                }

                // --- LAYER 2: PROCEDURAL GLOSSY TROPICAL FISH (Z-Depth: 0.72) ---
                // Set cleanly behind foreground hills (0.80) but in front of bubbles (0.65)
                double fx = (x - fishCenterX) * 0.6; // Scale X aspect ratio for console typography
                double fy = y - fishCenterY;

                // Analytical profile formula for a teardrop body structure + triangular tail fin
                boolean isFishBody = (fx * fx + fy * fy) < 1.6;
                boolean isFishTail = (fx > 1.2 && fx < 3.2) && (Math.abs(fy) < (fx - 1.0) * 0.85);

                if ((isFishBody || isFishTail) && 0.72 > zBuffer[index]) {
                    zBuffer[index] = 0.72;

                    // Compute smooth internal body gradients and anti-aliased edge blends
                    double fishEdgeWeight = 1.0;
                    int[] targetFishRGB = RGB_FISH_CORE;

                    if (isFishBody) {
                        double bodyDist = Math.sqrt(fx * fx + fy * fy);
                        fishEdgeWeight = Math.max(0.0, 1.0 - Math.max(0.0, (bodyDist - 0.6) / 1.0));
                        
                        // Internal belly lighting gradient (Orange blending down to Yellow)
                        double bellyGradiant = Math.max(0.0, Math.min(1.0, (fy + 0.8) / 1.6));
                        targetFishRGB = new int[]{
                            (int)(RGB_FISH_CORE[0] * (1.0 - bellyGradiant) + RGB_FISH_BELLY[0] * bellyGradiant),
                            (int)(RGB_FISH_CORE[1] * (1.0 - bellyGradiant) + RGB_FISH_BELLY[1] * bellyGradiant),
                            (int)(RGB_FISH_CORE[2] * (1.0 - bellyGradiant) + RGB_FISH_BELLY[2] * bellyGradiant)
                        };
                    } else {
                        // Translucent fin falloff blending toward the outer sky
                        fishEdgeWeight = Math.max(0.0, 1.0 - (fx - 1.2) / 2.0);
                    }

                    int fr = (int) (currentSkyRGB[0] * (1.0 - fishEdgeWeight) + targetFishRGB[0] * fishEdgeWeight);
                    int fg = (int) (currentSkyRGB[1] * (1.0 - fishEdgeWeight) + targetFishRGB[1] * fishEdgeWeight);
                    int fb = (int) (currentSkyRGB[2] * (1.0 - fishEdgeWeight) + targetFishRGB[2] * fishEdgeWeight);

                    String fishColor = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, fr)), Math.max(0, Math.min(255, fg)), Math.max(0, Math.min(255, fb)));
                    outputBuffer[index] = fishColor + "\u2588" + RESET;
                    continue;
                }

                // --- LAYER 3: INTERPOLATED GLASS BUBBLE MASK (Z-Depth: 0.65) ---
                boolean isBubbleField = false;
                double blendWeight = 0.0;

                for (int stream = 1; stream <= 3; stream++) {
                    int streamBaseX = stream * 22;
                    double swayX = streamBaseX + 6.0 * Math.cos(y * 0.15 + timeClock * stream);
                    double bubbleProgressY = height - ((timeClock * (10.0 + stream) + (stream * 5)) % (height + 4));

                    double dx = x - swayX;
                    double dy = y - bubbleProgressY;
                    double bubbleDist = Math.sqrt(dx * dx + dy * dy);

                    if (bubbleDist < 1.6) {
                        isBubbleField = true;
                        blendWeight = Math.max(blendWeight, 1.0 - Math.max(0.0, (bubbleDist - 0.5) / 1.1));
                    }
                }

                if (isBubbleField && 0.65 > zBuffer[index]) {
                    zBuffer[index] = 0.65;

                    int br = (int) (currentSkyRGB[0] * (1.0 - blendWeight) + RGB_BUBBLE[0] * blendWeight);
                    int bg = (int) (currentSkyRGB[1] * (1.0 - blendWeight) + RGB_BUBBLE[1] * blendWeight);
                    int bb = (int) (currentSkyRGB[2] * (1.0 - blendWeight) + RGB_BUBBLE[2] * blendWeight);

                    String bubbleColor = String.format("\u001B[38;2;%d;%d;%dm", br, bg, bb);
                    outputBuffer[index] = bubbleColor + "\u2588" + RESET;
                    continue;
                }

                // --- LAYER 4: BACKGROUND ECO-HILL (Z-Depth: 0.50) ---
                double backHillWave = 11.0 + 4.0 * Math.sin(x * 0.05 - timeClock * 0.2 + 2.0);
                if (y >= (int) backHillWave) {
                    if (0.50 > zBuffer[index]) {
                        zBuffer[index] = 0.50;
                        char hillChar = (y == (int) backHillWave) ? '\u2591' : '\u2592';
                        outputBuffer[index] = COLOR_HILL_BACK + hillChar + RESET;
                    }
                    continue;
                }

                // --- LAYER 5: GLASSY SKY CANVAS BACKDROP (Z-Depth: 0.01) ---
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;
                    
                    String skyColor = String.format("\u001B[38;2;%d;%d;%dm", currentSkyRGB[0], currentSkyRGB[1], currentSkyRGB[2]);
                    char skyTexture = (y < 5) ? '\u2588' : (y < 9) ? '\u2593' : '\u2591';
                    
                    outputBuffer[index] = skyColor + skyTexture + RESET;
                }
            }
        }
    }
}
