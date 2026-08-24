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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // Base values tracking structural colors and characters. A very soft
                // vertical gradient keeps the open sky from reading as a flat, dead fill.
                double skyT = y / (double) height;
                int[] pixelRGB = {
                        (int) (RGB_SKY[0] * (0.88 + 0.12 * skyT)),
                        (int) (RGB_SKY[1] * (0.88 + 0.12 * skyT)),
                        (int) (RGB_SKY[2] * (0.92 + 0.08 * skyT))
                };
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
                // Each tree is built from stacked triangular tiers that reset to a point
                // at the top of every tier and flare back out toward its bottom. That
                // notch between tiers is what reads as "layered evergreen branches"
                // instead of one solid, shapeless green blob.
                double[] pineTrunkX = { 9.0, 26.0, 94.0, 113.0 };
                for (int t = 0; t < pineTrunkX.length; t++) {
                    double tx = pineTrunkX[t];
                    double dx = x - tx;

                    double treeTopY = 3.0 + (t % 2); // 3 or 4
                    double treeBaseY = 20.0 + (t % 3) * 2.0; // a little height variety per tree
                    double treeHeight = treeBaseY - treeTopY;
                    int tierCount = 3;
                    double tierHeight = treeHeight / tierCount;

                    if (y >= treeTopY && y < treeBaseY && Math.abs(dx) < 9.0) {
                        double yInTree = y - treeTopY;
                        int tierIndex = Math.min(tierCount - 1, (int) (yInTree / tierHeight));
                        double yInTier = yInTree - tierIndex * tierHeight;
                        double tierMaxHalfWidth = (tierIndex + 1) * 2.6;
                        double halfWidthHere = (yInTier / tierHeight) * tierMaxHalfWidth;

                        if (Math.abs(dx) <= halfWidthHere) {
                            zDepth = 0.90;
                            boolean isEdge = Math.abs(dx) > halfWidthHere - 0.8;

                            if (Math.abs(dx) < 0.6) {
                                // Soft sunlit highlight running down the tree's centerline
                                pixelRGB = new int[] {
                                        Math.min(255, (int) (RGB_PINE_NEEDLE[0] * 1.3)),
                                        Math.min(255, (int) (RGB_PINE_NEEDLE[1] * 1.25)),
                                        Math.min(255, (int) (RGB_PINE_NEEDLE[2] * 1.15))
                                };
                                pixelChar = '▓';
                            } else if (isEdge) {
                                pixelRGB = new int[] { (int) (RGB_PINE_NEEDLE[0] * 0.7), (int) (RGB_PINE_NEEDLE[1] * 0.7),
                                        (int) (RGB_PINE_NEEDLE[2] * 0.7) };
                                pixelChar = '▓';
                            } else {
                                pixelRGB = RGB_PINE_NEEDLE;
                                pixelChar = '█';
                            }
                        }
                    }

                    // A short trunk peeking out just beneath the lowest canopy tier
                    if (Math.abs(dx) < 1.0 && y >= treeBaseY - 1 && y < treeBaseY + 3) {
                        zDepth = 0.90;
                        pixelRGB = RGB_PINE_WOOD;
                        pixelChar = '█';
                    }
                }

                // -------------------------------------------------------------
                // PHASING PASS 4: LOWER RIVER BASIN POOL BOUNDS
                // -------------------------------------------------------------
                if (y >= basinY && zDepth < 0.85) {
                    zDepth = 0.50;

                    // Concentric ripples expanding outward from the point where the falls
                    // strike the pool, plus a lighter secondary layer for texture. Two
                    // independent high-frequency waves multiplied together (the old
                    // approach) interfere into a grid; distance-based rings read as water.
                    double impactX = (fallLeftX + fallRightX) / 2.0;
                    double rdx = x - impactX;
                    double rdy = (y - basinY) * 2.2; // cells are taller than they are wide
                    double dist = Math.sqrt(rdx * rdx + rdy * rdy);

                    double primaryRipple = Math.sin(dist * 0.55 - timeClock * 3.2);
                    double secondaryRipple = 0.4 * Math.sin(dist * 1.3 - timeClock * 4.4 + x * 0.05);
                    double waterRipple = primaryRipple + secondaryRipple;

                    pixelRGB = RGB_WATER_DARK;
                    pixelChar = '▒';

                    if (waterRipple > 0.9) {
                        pixelRGB = RGB_WATER_FOAM;
                        pixelChar = '█';
                    } else if (waterRipple > 0.35) {
                        pixelRGB = RGB_WATER_FOAM;
                        pixelChar = '▓';
                    } else if (waterRipple < -0.5) {
                        pixelChar = '░';
                    }
                }

                // -------------------------------------------------------------
                // PHASING PASS 5: DETAILED HIGH-POWER FALLING WATERFALL
                // -------------------------------------------------------------
                boolean inWaterfallZone = x >= fallLeftX && x <= fallRightX && y >= shelfY && y <= basinY;

                if (inWaterfallZone && 0.95 > zDepth) {
                    zDepth = 0.95;

                    // Give each column its own pseudo-random phase and speed so the
                    // strands don't line up into a repeating woven texture.
                    double colSeedRaw = Math.sin(x * 12.9898) * 43758.5453;
                    double colPhase = (colSeedRaw - Math.floor(colSeedRaw)) * Math.PI * 2.0;
                    double colSpeed = 5.0 + 2.0 * Math.sin(x * 0.7);

                    // The key fix: shift the actual texture downward over time (not just
                    // its brightness), so the streak pattern itself travels down the
                    // channel instead of staying fixed while only flickering in place.
                    double flowY = y - timeClock * colSpeed;

                    double strand = Math.sin(flowY * 0.85 + colPhase)
                            + 0.5 * Math.sin(flowY * 2.1 - colPhase * 1.6 + x * 0.15);

                    if (strand > 0.55 || x == fallLeftX || x == fallRightX) {
                        pixelRGB = RGB_WATER_FOAM;
                        pixelChar = '█'; // Bright falling foam strand
                    } else if (strand > 0.0) {
                        pixelRGB = RGB_WATER_FOAM;
                        pixelChar = '▓'; // Lit water catching the light
                    } else if (strand > -0.6) {
                        pixelRGB = RGB_WATER_DARK;
                        pixelChar = '▒'; // Mid-tone flowing water
                    } else {
                        pixelRGB = RGB_WATER_DARK;
                        pixelChar = '░'; // Deep undertone currents
                    }

                    // A faint independent sparkle layer, still riding on flowY so it
                    // drifts downward with everything else rather than static-flickering.
                    double sparkle = Math.sin(x * 3.1 + flowY * 1.4);
                    if (sparkle > 0.92) {
                        pixelRGB = RGB_WATER_FOAM;
                        pixelChar = '█';
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