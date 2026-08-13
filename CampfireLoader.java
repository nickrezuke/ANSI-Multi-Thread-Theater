// TODO: Improve the Fire sparks, Stars Twinkling, Person Outline etc.

public class CampfireLoader extends Loader {
    private static final StatusStage[] CAMP_STAGES = {
            new StatusStage(25, "Dimming starry night sky matrices:"),
            new StatusStage(50, "Stoking combustible firewood embers:"),
            new StatusStage(75, "Projecting warm radial illumination fields:"),
            new StatusStage(100, "Campfire Rest Horizon Synchronized!")
    };

    private double timeClock = 0.0;
    private final int width = 120;
    private final int height = 36;

    // Highly-saturated retro pixel art color palette registers
    private static final int[] RGB_SKY_TOP = { 12, 14, 32 }; // Dark Midnight Navy
    private static final int[] RGB_SKY_BTM = { 28, 22, 40 }; // Dusty Hazy Purple
    private static final int[] RGB_MOUNTAIN = { 18, 16, 26 }; // Near-Black Silhouette Ridge
    private static final int[] RGB_FIRE_CORE = { 255, 235, 120 };// Blazing Specular Yellow
    private static final int[] RGB_FIRE_GLOW = { 245, 110, 25 }; // Warm Radiant Aero Orange
    private static final int[] RGB_SHADOW_TONE = { 40, 42, 68 }; // Cool Ambient Night Shadow
    private static final int[] RGB_KNIGHT = { 15, 15, 22 }; // Deep Armor Core Silhouette

    public CampfireLoader() {
        super(CAMP_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.055; // Universal frame timer driving flames, stars, and embers

        // Center coordinates of the crackling fire pit on screen
        double fireBaseX = 60.0;
        int fireBaseY = 27;

        for (int y = 0; y < height; y++) {
            // Smoothly calculate vertical background sky colors
            double verticalPct = (double) y / height;
            int[] skyRGB = new int[] {
                    (int) (RGB_SKY_TOP[0] * (1.0 - verticalPct) + RGB_SKY_BTM[0] * verticalPct),
                    (int) (RGB_SKY_TOP[1] * (1.0 - verticalPct) + RGB_SKY_BTM[1] * verticalPct),
                    (int) (RGB_SKY_TOP[2] * (1.0 - verticalPct) + RGB_SKY_BTM[2] * verticalPct)
            };

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // -------------------------------------------------------------
                // LAYER 1: THE DYNAMIC CRACKLING FIRE PIT (Z-Depth: 0.95)
                // -------------------------------------------------------------
                double dx = x - fireBaseX;
                double dy = fireBaseY - y;

                // Fluid, non-linear sine waves generate licking, climbing flame shapes
                double flameWarp = 1.6 * Math.sin(y * 0.6 - timeClock * 14.0) * Math.cos(x * 0.35 + timeClock * 4.0);
                double adjustedDx = dx + flameWarp;

                // Parabolic boundary equation restricting flame column profile heights
                double flameBoundY = 11.5 - (adjustedDx * adjustedDx * 0.45);

                if (dy > 0 && dy < flameBoundY && 0.95 > zBuffer[index]) {
                    zBuffer[index] = 0.95;

                    int[] flameRGB = RGB_FIRE_GLOW;
                    char flameChar = '▓';

                    // Squeeze a hot yellow internal core directly over the base logs
                    double innerCoreBoundY = (flameBoundY * 0.45) - (adjustedDx * adjustedDx * 0.15);
                    if (dy < innerCoreBoundY && Math.abs(adjustedDx) < 2.5) {
                        flameRGB = RGB_FIRE_CORE;
                        flameChar = '█';
                    } else if (dy > flameBoundY * 0.72) {
                        flameChar = '▒'; // Tip trail dissipation
                    }

                    String fColor = String.format("\u001B[38;2;%d;%d;%dm", flameRGB[0], flameRGB[1], flameRGB[2]);
                    outputBuffer[index] = fColor + flameChar + RESET;
                    continue;
                }

                // Spawning floating air ember particles drifting upwards off tips
                if (dy >= flameBoundY && dy < 18 && Math.abs(adjustedDx) < 6.5) {
                    double emberCheck = Math.sin(x * 7.5 + y * 3.2 - timeClock * 18.0);
                    if (emberCheck > 0.92 && 0.95 > zBuffer[index]) {
                        zBuffer[index] = 0.95;
                        String eColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_FIRE_CORE[0], RGB_FIRE_CORE[1],
                                RGB_FIRE_CORE[2]);
                        outputBuffer[index] = eColor + (emberCheck > 0.96 ? "¤" : "·") + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // LAYER 2: THE COZY SLEEPING KNIGHT MASK (Z-Depth: 0.90)
                // -------------------------------------------------------------
                // Rest your character directly on the right side of the campfire
                double knightDx = x - 72.0;
                double knightDy = fireBaseY - y;

                // Slumbering geometric silhouette box outline matches an armored torso leaning
                // back
                boolean inKnightBody = (knightDx >= -2 && knightDx <= 4 && knightDy >= -1 && knightDy <= 3);
                boolean inKnightHelmet = (knightDx >= -1 && knightDx <= 2 && knightDy >= 4 && knightDy <= 6);

                if ((inKnightBody || inKnightHelmet) && 0.90 > zBuffer[index]) {
                    zBuffer[index] = 0.90;

                    // Catch fire light reflections along the left profile edge surfaces
                    boolean isLeftEdge = (x == 70 && y >= 21 && y <= 24) || (x == 69 && y >= 25 && y <= 28);
                    int[] kRGB = isLeftEdge ? RGB_FIRE_GLOW : RGB_KNIGHT;
                    char kChar = isLeftEdge ? '▒' : '█';

                    String kColor = String.format("\u001B[38;2;%d;%d;%dm", kRGB[0], kRGB[1], kRGB[2]);
                    outputBuffer[index] = kColor + kChar + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 3: COAL LOGS & GROUND SOIL MATRICES (Z-Depth: 0.80)
                // -------------------------------------------------------------
                if (y >= fireBaseY && 0.80 > zBuffer[index]) {
                    zBuffer[index] = 0.80;

                    // Dynamic ground lighting: calculate radial field distances from center
                    double groundDistX = x - fireBaseX;
                    double radiusGround = Math
                            .sqrt(groundDistX * groundDistX * 0.45 + (y - fireBaseY) * (y - fireBaseY) * 2.0);

                    int[] groundRGB = RGB_SHADOW_TONE;
                    char groundChar = '▒';

                    // Splicing a cross-pattern stack of burning fuel logs right under fire center
                    boolean isLogStrut = (y == fireBaseY && Math.abs(x - fireBaseX) < 4)
                            || (y == fireBaseY + 1 && Math.abs(x - fireBaseX) < 6);

                    if (isLogStrut) {
                        groundRGB = new int[] { 110, 65, 40 }; // Burning Wood Brown
                        groundChar = '█';
                    } else if (radiusGround < 16.0) {
                        // Blend the ambient fire glow smoothly outwards across the dirt matrix
                        double intensity = Math.max(0.0, 1.0 - (radiusGround / 16.0));
                        // Pulse intensity rhythmically to match the flickering flames
                        intensity *= (0.85 + 0.15 * Math.sin(timeClock * 6.5));

                        groundRGB = new int[] {
                                (int) (RGB_SHADOW_TONE[0] * (1.0 - intensity) + RGB_FIRE_GLOW[0] * intensity),
                                (int) (RGB_SHADOW_TONE[1] * (1.0 - intensity) + RGB_FIRE_GLOW[1] * intensity),
                                (int) (RGB_SHADOW_TONE[2] * (1.0 - intensity) + RGB_FIRE_GLOW[2] * intensity)
                        };
                        groundChar = (intensity > 0.5) ? '█' : '▓';
                    } else if (radiusGround >= 16.0 && (x + y * 3) % 7 == 1) {
                        groundChar = '░'; // Distant dark floor textures
                    }

                    String gColor = String.format("\u001B[38;2;%d;%d;%dm", groundRGB[0], groundRGB[1], groundRGB[2]);
                    outputBuffer[index] = gColor + groundChar + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 4: INVERSE RADIAL AIR AIR GLOW SHADER (Z-Depth: 0.60)
                // -------------------------------------------------------------
                // Evaluates the warm light hemisphere cast upwards through the smoke
                double glowDx = x - fireBaseX;
                double glowDy = (fireBaseY - 2) - y;
                // Account for console character cell scale aspect stretching (~2.3)
                double distToFireCenter = Math.sqrt(glowDx * glowDx * 0.45 + glowDy * glowDy * 2.2);

                if (distToFireCenter < 24.0 && 0.60 > zBuffer[index]) {
                    zBuffer[index] = 0.60;

                    double weight = Math.max(0.0, 1.0 - (distToFireCenter / 24.0));
                    // Inject high-frequency flicker iterations into the ambient field mask
                    weight *= (0.88 + 0.12 * Math.sin(timeClock * 8.0));

                    // Direct three-way linear channel interpolation scaling air colors
                    int r = (int) (skyRGB[0] * (1.0 - weight) + RGB_FIRE_GLOW[0] * weight);
                    int g = (int) (skyRGB[1] * (1.0 - weight) + RGB_FIRE_GLOW[1] * weight);
                    int b = (int) (skyRGB[2] * (1.0 - weight) + RGB_SHADOW_TONE[2] * weight);

                    String airColor = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, min(255, r)),
                            Math.max(0, min(255, g)), Math.max(0, min(255, b)));
                    outputBuffer[index] = airColor + "█" + RESET;
                    continue;
                }
                // -------------------------------------------------------------
                // LAYER 5: MOUNTAIN VALLEY HORIZON PARALLAX (Z-Depth: 0.40)
                // -------------------------------------------------------------
                double mountainWave = 16.5 + 4.5 * Math.sin(x * 0.07 + 1.2) + 1.5 * Math.cos(x * 0.18);
                if (y >= (int) mountainWave && 0.40 > zBuffer[index]) {
                    zBuffer[index] = 0.40;
                    String mColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_MOUNTAIN[0], RGB_MOUNTAIN[1],
                            RGB_MOUNTAIN[2]);
                    outputBuffer[index] = mColor + "█" + RESET;
                    continue;
                }
                // -------------------------------------------------------------
                // LAYER 6: DRIFTING STARRY BACKGROUND BACKDROP (Z-Depth: 0.01)
                // -------------------------------------------------------------
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;
                    // Evaluate static star field matrices tracking slowly with horizontal scrolling
                    // offsets
                    double starNoise = Math.sin((x + timeClock * 0.15) * 45.0) * Math.cos(y * 45.0);
                    if (starNoise > 0.97 && y < 14) {
                        // Twinkling white distant stars
                        outputBuffer[index] = "\u001B[38;2;230;240;255m" + (starNoise > 0.985 ? "*" : ".")
                                + "\u001B[0m";
                    } else {
                        String sColor = String.format("\u001B[38;2;%d;%d;%dm", skyRGB[0], skyRGB[1], skyRGB[2]);
                        outputBuffer[index] = sColor + "█" + RESET;
                    }
                }
            }
        }
    }

    private int min(int a, int b) {
        return Math.min(a, b);
    }
}