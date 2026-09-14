// TODO: Improve this one its just a little boring

public class AquariumLoader extends Loader {
    private static final StatusStage[] AQUA_STAGES = {
        new StatusStage(25, "Filtering aquatic water columns:"),
        new StatusStage(50, "Laying down multicolored gravel substrate:"),
        new StatusStage(75, "Planting dynamic kelp & configuring air stones:"),
        new StatusStage(100, "Aquarium Environment Active!")
    };

    private double timeClock = 0.0;

    // Vibrant Aquarium Water Base Values (Top-down lighting)
    private static final int[] RGB_WATER_TOP = { 60,  180, 240 }; // Bright illuminated surface
    private static final int[] RGB_WATER_MID = { 20,  130, 200 }; // Mid-level tank water
    private static final int[] RGB_WATER_BTM = { 0,   70,  140 }; // Deeper bottom water
    private static final int[] RGB_BUBBLE    = { 220, 245, 255 }; // Pristine White Gloss Core

    // Glossy Tropical Fish Palette Registers (Neon Orange/Yellow)
    private static final int[] RGB_FISH_CORE  = { 255, 110, 20 };
    private static final int[] RGB_FISH_BELLY = { 255, 215, 0  };

    public AquariumLoader() {
        // This uses 80x22 specifically
        super(AQUA_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.035; // Controls water swaying, bubbles, and fish movement

        int width = 80;
        int height = 22;

        // Calculate the horizontal loop position for the swimming fish
        // Moves from right to left cleanly across the canvas grid
        double fishCenterX = width - ((timeClock * 12.0) % (width + 20));
        // Fish swims slightly lower in the tank, gliding between y=10 and y=15
        double fishCenterY = 12.5 + 2.5 * Math.sin(timeClock * 0.6); 

        for (int y = 0; y < height; y++) {
            // Pre-calculate the exact static water background color at this row for blending
            int[] currentWaterRGB = RGB_WATER_BTM;
            if (y < 6)       currentWaterRGB = RGB_WATER_TOP;
            else if (y < 13) currentWaterRGB = RGB_WATER_MID;

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // --- LAYER 1: STATIC AQUARIUM GRAVEL FLOOR (Z-Depth: 0.85) ---
                if (y >= 19) {
                    if (0.85 > zBuffer[index]) {
                        zBuffer[index] = 0.85;
                        
                        // Generate a static, textured mix of gravel colors based on coordinates
                        int noise = (x * 37 + y * 19) % 3;
                        int gr = 140 + noise * 30; // Pebbly browns and grays
                        int gg = 130 + noise * 25;
                        int gb = 120 + noise * 25;
                        
                        String gravelColor = String.format("\u001B[38;2;%d;%d;%dm", gr, gg, gb);
                        char gravelChar = (noise == 0) ? '\u2588' : (noise == 1) ? '\u2593' : '\u2592';
                        outputBuffer[index] = gravelColor + gravelChar + RESET;
                    }
                    continue;
                }

                // --- LAYER 2: DECORATIVE ROCK CAVE (Z-Depth: 0.80 / 0.76) ---
                double caveX = x - 65; // Positioned on the right side
                if (y >= 13 && y <= 18 && Math.abs(caveX) < (y - 11)) {
                    // Check if we are inside the dark cave entrance
                    if (Math.abs(caveX) < 3 && y >= 15) {
                        if (0.76 > zBuffer[index]) {
                            zBuffer[index] = 0.76;
                            outputBuffer[index] = "\u001B[38;2;10;15;25m\u2588" + RESET; // Dark interior
                        }
                    } else {
                        // Rock exterior
                        if (0.80 > zBuffer[index]) {
                            zBuffer[index] = 0.80;
                            int rockShade = 90 + (x % 3) * 10 - (y % 2) * 10;
                            outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm\u2588", rockShade, rockShade, rockShade) + RESET;
                        }
                    }
                    continue;
                }

                // --- LAYER 3: SWAYING KELP PLANTS (Z-Depth: 0.75) ---
                boolean isKelp = false;
                int[] kelpPositions = {12, 15, 35, 38}; // Base X positions for kelp stalks
                for (int kx : kelpPositions) {
                    // Kelp sways more at the top (lower Y), rooted at the bottom (Y=19)
                    double sway = 2.0 * Math.sin((19 - y) * 0.25 + timeClock * 1.5 + kx);
                    if (y >= 7 && y <= 18 && (x == (int)(kx + sway) || x == (int)(kx + sway + 1))) {
                        isKelp = true;
                        break;
                    }
                }

                if (isKelp && 0.75 > zBuffer[index]) {
                    zBuffer[index] = 0.75;
                    // Vibrant underwater green
                    outputBuffer[index] = "\u001B[38;2;34;160;50m\u2593" + RESET;
                    continue;
                }

                // --- LAYER 4: TROPICAL FISH (Z-Depth: 0.72) ---
                double fx = (x - fishCenterX) * 0.6; // Scale X aspect ratio for console typography
                double fy = y - fishCenterY;

                // Analytical profile formula for a teardrop body structure + triangular tail fin
                boolean isFishBody = (fx * fx + fy * fy) < 1.6;
                boolean isFishTail = (fx > 1.2 && fx < 3.2) && (Math.abs(fy) < (fx - 1.0) * 0.85);

                if ((isFishBody || isFishTail) && 0.72 > zBuffer[index]) {
                    zBuffer[index] = 0.72;

                    double fishEdgeWeight = 1.0;
                    int[] targetFishRGB = RGB_FISH_CORE;

                    if (isFishBody) {
                        double bodyDist = Math.sqrt(fx * fx + fy * fy);
                        fishEdgeWeight = Math.max(0.0, 1.0 - Math.max(0.0, (bodyDist - 0.6) / 1.0));
                        
                        // Internal belly lighting gradient (Orange blending down to Yellow)
                        double bellyGradient = Math.max(0.0, Math.min(1.0, (fy + 0.8) / 1.6));
                        targetFishRGB = new int[]{
                            (int)(RGB_FISH_CORE[0] * (1.0 - bellyGradient) + RGB_FISH_BELLY[0] * bellyGradient),
                            (int)(RGB_FISH_CORE[1] * (1.0 - bellyGradient) + RGB_FISH_BELLY[1] * bellyGradient),
                            (int)(RGB_FISH_CORE[2] * (1.0 - bellyGradient) + RGB_FISH_BELLY[2] * bellyGradient)
                        };
                    } else {
                        // Translucent fin falloff blending toward the outer water
                        fishEdgeWeight = Math.max(0.0, 1.0 - (fx - 1.2) / 2.0);
                    }

                    int fr = (int) (currentWaterRGB[0] * (1.0 - fishEdgeWeight) + targetFishRGB[0] * fishEdgeWeight);
                    int fg = (int) (currentWaterRGB[1] * (1.0 - fishEdgeWeight) + targetFishRGB[1] * fishEdgeWeight);
                    int fb = (int) (currentWaterRGB[2] * (1.0 - fishEdgeWeight) + targetFishRGB[2] * fishEdgeWeight);

                    String fishColor = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, fr)), Math.max(0, Math.min(255, fg)), Math.max(0, Math.min(255, fb)));
                    outputBuffer[index] = fishColor + "\u2588" + RESET;
                    continue;
                }

                // --- LAYER 5: INTERPOLATED OXYGEN BUBBLE STREAMS (Z-Depth: 0.65) ---
                boolean isBubbleField = false;
                double blendWeight = 0.0;
                int[] airStones = { 25, 52, 72 }; // X-coordinates where bubbles originate from gravel

                for (int stream = 0; stream < 3; stream++) {
                    int streamBaseX = airStones[stream];
                    // Bubbles rise starting from the gravel layer (Y=19)
                    double bubbleProgressY = 19.0 - ((timeClock * (10.0 + stream) + (stream * 5)) % 22);
                    double swayX = streamBaseX + 2.5 * Math.cos(bubbleProgressY * 0.25 + timeClock * (stream + 1));

                    double dx = x - swayX;
                    double dy = y - bubbleProgressY;
                    double bubbleDist = Math.sqrt(dx * dx + dy * dy);

                    if (bubbleDist < 1.4) {
                        isBubbleField = true;
                        blendWeight = Math.max(blendWeight, 1.0 - Math.max(0.0, (bubbleDist - 0.4) / 1.0));
                    }
                }

                if (isBubbleField && 0.65 > zBuffer[index]) {
                    zBuffer[index] = 0.65;

                    int br = (int) (currentWaterRGB[0] * (1.0 - blendWeight) + RGB_BUBBLE[0] * blendWeight);
                    int bg = (int) (currentWaterRGB[1] * (1.0 - blendWeight) + RGB_BUBBLE[1] * blendWeight);
                    int bb = (int) (currentWaterRGB[2] * (1.0 - blendWeight) + RGB_BUBBLE[2] * blendWeight);

                    String bubbleColor = String.format("\u001B[38;2;%d;%d;%dm", br, bg, bb);
                    outputBuffer[index] = bubbleColor + "\u2588" + RESET;
                    continue;
                }

                // --- LAYER 6: TANK WATER BACKDROP (Z-Depth: 0.01) ---
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;
                    
                    String waterColor = String.format("\u001B[38;2;%d;%d;%dm", currentWaterRGB[0], currentWaterRGB[1], currentWaterRGB[2]);
                    // Creates a slight underwater texture density gradient
                    char waterTexture = (y < 6) ? '\u2588' : (y < 13) ? '\u2593' : '\u2591';
                    
                    outputBuffer[index] = waterColor + waterTexture + RESET;
                }
            }
        }
    }
}