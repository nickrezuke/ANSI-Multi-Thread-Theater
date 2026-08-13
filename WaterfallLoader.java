// TODO: Make this Waterfall look better...

public class WaterfallLoader extends Loader {
    private static final StatusStage[] CASCADE_STAGES = {
            new StatusStage(25, "Painting tiered evergreen pine canopies:"),
            new StatusStage(50, "Carving texturized granite cliff ledges:"),
            new StatusStage(75, "Anchoring wet-glare river log obstacles:"),
            new StatusStage(100, "Cinemagraph Cascade Overhaul Complete!")
    };

    private double timeClock = 0.0;
    private final int width = 120;
    private final int height = 36;

    // Meticulously matched pixel art color registers
    private static final int[] RGB_SKY = { 182, 218, 222 }; // Pale Pastel Clean Blue
    private static final int[] RGB_ROCK_BASE = { 75, 68, 72 }; // Deep Velvet Slate Grey
    private static final int[] RGB_ROCK_LIT = { 118, 102, 98 }; // Shadowed Granite Face
    private static final int[] RGB_MOSS = { 158, 164, 55 }; // Yellow-Green Moss Turf
    private static final int[] RGB_PINE_NEEDLE = { 40, 82, 42 }; // Deep Evergreen Canopy
    private static final int[] RGB_PINE_WOOD = { 95, 52, 38 }; // Rich Red-Brown Pine Trunks
    private static final int[] RGB_WATER_DARK = { 65, 115, 150 }; // Rich Deep Flow Blue
    private static final int[] RGB_WATER_FOAM = { 232, 238, 225 }; // Off-White Waterfall Glare
    private static final int[] RGB_LOG_WOOD = { 82, 48, 32 }; // Wet Log Dark Brown

    public WaterfallLoader() {
        super(CASCADE_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.035; // Slow, deliberate clock for smooth fluid motion

        // Bounds defining the central waterfall channel
        int fallLeftX = 49;
        int fallRightX = 67;
        int shelfY = 13;
        int basinY = 29;

        // Position of the horizontal log wedged inside the waterfall channel
        int logY = 22;
        int logLeftX = 46;
        int logRightX = 62;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // Base values tracking structural colors and characters
                int[] pixelRGB = RGB_SKY;
                char pixelChar = '█';
                double zDepth = 0.01;

                
                // -------------------------------------------------------------
                // PHASING PASS 1: BACKGROUND SKY & MIDDLE FOREST RIDGE
                // -------------------------------------------------------------
                double bgForestLine = 11.0 + 2.0 * Math.sin(x * 0.22) * Math.cos(x * 0.08);
                if (y >= (int) bgForestLine) {
                    pixelRGB = new int[] { (int) (RGB_PINE_NEEDLE[0] * 0.75), (int) (RGB_PINE_NEEDLE[1] * 0.75),
                            (int) (RGB_PINE_NEEDLE[2] * 0.75) };
                    pixelChar = '▓';
                    zDepth = 0.20;
                }

                // -------------------------------------------------------------
                // PHASING PASS 2: COZY BACKGROUND GREENERY & LEDGES
                // -------------------------------------------------------------
                double cliffProfileL = 13.0 + Math.pow((x - 12.0) * 0.08, 2);
                double cliffProfileR = 13.0 + Math.pow((x - 104.0) * 0.08, 2);

                boolean inLeftCliff = x < fallLeftX && y >= (int) cliffProfileL;
                boolean inRightCliff = x > fallRightX && y >= (int) cliffProfileR;

                // Flanking lower boulder structures rising up from the bottom corners
                double boulderL = 25.0 + Math.pow((x - 16.0) * 0.16, 2);
                double boulderR = 23.0 + Math.pow((x - 102.0) * 0.14, 2);
                boolean inBoulders = (x < 38 && y >= (int) boulderL) || (x > 82 && y >= (int) boulderR);

                if (inLeftCliff || inRightCliff || inBoulders) {
                    zDepth = 0.80;
                    boolean isTopMoss = (inLeftCliff && y == (int) cliffProfileL) ||
                            (inRightCliff && y == (int) cliffProfileR) ||
                            (x < 38 && y == (int) boulderL) ||
                            (x > 82 && y == (int) boulderR);

                    pixelRGB = isTopMoss ? RGB_MOSS : RGB_ROCK_BASE;
                    pixelChar = isTopMoss ? '▓' : '█';

                    // Inject sharp linear rock fractures into the stone face maps
                    if (!isTopMoss && Math.abs(Math.sin(x * 0.40 - y * 0.28)) > 0.86) {
                        pixelRGB = RGB_ROCK_LIT;
                        pixelChar = '▓';
                    }
                }

                // -------------------------------------------------------------
                // PHASING PASS 3: TOWERING FOREGROUND PINE TREES
                // -------------------------------------------------------------
                double[] pineTrunkX = { 9.0, 26.0, 94.0, 113.0 };
                for (int t = 0; t < pineTrunkX.length; t++) {
                    double tx = pineTrunkX[t];
                    double dx = x - tx;

                    if (Math.abs(dx) < 1.0 && y >= 3) {
                        pixelRGB = RGB_PINE_WOOD;
                        pixelChar = '█';
                        zDepth = 0.90;
                    }

                    double canopyStartPhase = (t * 7.3);
                    double leavesLimitY = 4.0 + Math.abs(dx) * 2.6 + Math.sin(x * 0.4 + canopyStartPhase);
                    if (y >= 2 && y < leavesLimitY && Math.abs(dx) < 8.5) {
                        zDepth = 0.90;
                        pixelRGB = RGB_PINE_NEEDLE;
                        pixelChar = '▓';
                        if ((x + y * 7) % 5 == 0) {
                            pixelRGB = new int[] { (int) (RGB_PINE_NEEDLE[0] * 0.8), (int) (RGB_PINE_NEEDLE[1] * 0.8),
                                    (int) (RGB_PINE_NEEDLE[2] * 0.8) };
                            pixelChar = '█';
                        }
                    }
                }

                // -------------------------------------------------------------
                // PHASING PASS 4: LOWER RIVER BASIN POOL BOUNDS
                // -------------------------------------------------------------
                if (y >= basinY && zDepth < 0.85) {
                    zDepth = 0.50;
                    double waterRipple = Math.sin(x * 0.40 - timeClock * 1.5) * Math.cos(y * 1.1);
                    pixelRGB = RGB_WATER_DARK;
                    pixelChar = '▒';

                    if (waterRipple > 0.65) {
                        pixelRGB = RGB_WATER_FOAM;
                        pixelChar = '█';
                    } else if (waterRipple < -0.3) {
                        pixelChar = '░';
                    }
                }

                // -------------------------------------------------------------
                // PHASING PASS 5: DETAILED HIGH-POWER STATIC WATERFALL
                // -------------------------------------------------------------
                boolean inWaterfallZone = x >= fallLeftX && x <= fallRightX && y >= shelfY && y <= basinY;

                if (inWaterfallZone && 0.95 > zDepth) {
                    zDepth = 0.95;

                    // Form a highly detailed, rigid structural layout for the waterfall artwork
                    // Combining spatial coordinate noise seeds to paint crisp static fluid textures
                    double detailMosaics = Math.sin(x * 1.25) * Math.cos(y * 0.45) + Math.sin(x * 0.35 + y * 0.85);

                    if (detailMosaics > 0.4 || x == fallLeftX || x == fallRightX) {
                        pixelRGB = RGB_WATER_FOAM;
                        pixelChar = '█'; // Solid crystalline white water columns
                    } else if (detailMosaics > -0.2) {
                        pixelRGB = RGB_WATER_DARK;
                        pixelChar = '▓'; // Dense blue inner water streams
                    } else {
                        pixelRGB = RGB_WATER_DARK;
                        pixelChar = '▒'; // Deep undertone currents
                    }

                    // --- CINEMAGRAPH MASK ENGINE VALUE ---
                    // Instead of flashing light and dark channels back and forth, we apply a slow,
                    // continuous scrolling vertical wave mask that gently alters the color tone.
                    // This creates a smooth fluid illusion while preserving the underlying text
                    // artwork.
                    double flowMask = Math.sin(y * 0.48 - timeClock * 4.5) * Math.cos(x * 0.15 + timeClock * 0.5);

                    if (flowMask > 0.3) {
                        // Softly blend towards the bright foam register
                        pixelRGB = new int[] {
                                Math.min(255, (int) (pixelRGB[0] * 0.82 + RGB_WATER_FOAM[0] * 0.18)),
                                Math.min(255, (int) (pixelRGB[1] * 0.82 + RGB_WATER_FOAM[1] * 0.18)),
                                Math.min(255, (int) (pixelRGB[2] * 0.82 + RGB_WATER_FOAM[2] * 0.18))
                        };
                    } else if (flowMask < -0.4) {
                        // Softly deepen down the dark palette channels
                        pixelRGB = new int[] {
                                Math.max(0, (int) (pixelRGB[0] * 0.85)),
                                Math.max(0, (int) (pixelRGB[1] * 0.85)),
                                Math.max(0, (int) (pixelRGB[2] * 0.88))
                        };
                    }

                    // Soften edges dynamically where the waterfall drops into the bottom basin
                    if (y >= basinY - 1) {
                        double bottomSplashNoise = Math.sin(x * 4.5 + timeClock * 16.0);
                        if (bottomSplashNoise > 0.1) {
                            pixelRGB = RGB_WATER_FOAM;
                            pixelChar = (bottomSplashNoise > 0.7) ? '¤' : '·';
                        }
                    }
                }
                // -------------------------------------------------------------
                // PHASING PASS 6: THE SHIMMERING WET RIVER LOG OBSTACLE
                // -------------------------------------------------------------
                // Wedged horizontally right through the center waterfall channel stream
                if (x >= logLeftX && x <= logRightX && y >= logY && y <= logY + 1) {
                    if (0.97 > zBuffer[index]) {
                        // Sits right in the foreground
                        zBuffer[index] = 0.97;
                        pixelRGB = RGB_LOG_WOOD;
                        pixelChar = '█';
                        // --- SHIMMERING WET COATING MECHANIC ---
                        // Top horizontal row of pixels catches the direct cascading water impact.
                        // We run a high-frequency shimmering calculation to make the log cap glisten
                        // with foam.
                        if (y == logY) {
                            double wetShimmer = Math.sin(x * 2.8 + timeClock * 24.0);
                            if (wetShimmer > 0.15) {
                                pixelRGB = RGB_WATER_FOAM;
                                // Splashing water coating
                                pixelChar = (wetShimmer > 0.75) ? '█' : '▓';
                            }
                        } else {
                            // Bottom row: simple wood shadow accents
                            if (x % 3 == 0)
                                pixelChar = '▓';
                        }
                    }
                    continue;
                }
                // Render the finalized layer assets directly to screen output buffers
                if (zDepth > zBuffer[index]) {
                    zBuffer[index] = zDepth;
                    String colorString = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, pixelRGB[0])),
                            Math.max(0, Math.min(255, pixelRGB[1])), Math.max(0, Math.min(255, pixelRGB[2])));
                    outputBuffer[index] = colorString + pixelChar + RESET;
                }
            }
        }
    }
}