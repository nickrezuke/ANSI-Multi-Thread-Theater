// TODO: Add "close" trees to the left and right to make it look like we're behind trees in the forest

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

    // Enhanced Retro Pixel Art Color Palette
    private static final int[] RGB_SKY_TOP = { 8, 10, 26 }; // Deep Midnight Sky
    private static final int[] RGB_SKY_BTM = { 32, 20, 42 }; // Warm Horizon Purple
    private static final int[] RGB_MOUNTAIN_FAR = { 16, 14, 28 }; // Distant Mountain Shadow
    private static final int[] RGB_MOUNTAIN_NEAR = { 24, 20, 36 }; // Middle Ridge Silhouette
    private static final int[] RGB_PINE_DARK = { 10, 22, 18 }; // Pine Forest Base Silhouette
    private static final int[] RGB_PINE_LIGHT = { 22, 42, 32 }; // Forest Highlight Tone

    private static final int[] RGB_FIRE_CORE = { 255, 245, 160 }; // Specular Bright Yellow
    private static final int[] RGB_FIRE_MID = { 255, 120, 20 }; // Intense Flame Orange
    private static final int[] RGB_FIRE_OUTER = { 190, 40, 10 }; // Deep Flame Red

    private static final int[] RGB_SHADOW_TONE = { 22, 24, 38 }; // Dark Night Soil
    private static final int[] RGB_STONE_GRAY = { 65, 68, 80 }; // Campfire Ring Stones
    private static final int[] RGB_KNIGHT_BASE = { 25, 28, 42 }; // Dark Armor / Cloak Shadow
    private static final int[] RGB_KNIGHT_LIGHT = { 220, 130, 45 }; // Dynamic Rim Lighting

    // Framing Pine Forest Layout: { centerX, topY }
    private static final int[][] PINE_TREES = {
            // Far Left Forest Wall
            { -4, 2 }, { 2, 0 }, { 7, 4 }, { 13, 1 }, { 18, 5 }, { 24, 3 }, { 29, 7 }, { 35, 10 }, { 41, 12 },
            { 46, 11 },
            // Center Clearing Horizon
            { 51, 9 }, { 57, 11 }, { 63, 12 }, { 69, 9 }, { 74, 10 },
            // Far Right Forest Wall
            { 79, 11 }, { 85, 8 }, { 90, 7 }, { 96, 2 }, { 101, 5 }, { 107, 1 }, { 112, 4 }, { 118, 0 }, { 123, 3 }
    };

    public CampfireLoader() {
        super(CAMP_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.055;

        // Fireplace anchor coordinates
        double fireBaseX = 60.0;
        int fireBaseY = 27;

        // Organic multi-frequency flicker calculation driving light dynamics
        double flicker = 0.86
                + 0.09 * Math.sin(timeClock * 11.2)
                + 0.05 * Math.cos(timeClock * 19.7)
                + 0.03 * Math.sin(timeClock * 31.4);

        for (int y = 0; y < height; y++) {
            double verticalPct = (double) y / height;
            int[] skyRGB = new int[] {
                    (int) (RGB_SKY_TOP[0] * (1.0 - verticalPct) + RGB_SKY_BTM[0] * verticalPct),
                    (int) (RGB_SKY_TOP[1] * (1.0 - verticalPct) + RGB_SKY_BTM[1] * verticalPct),
                    (int) (RGB_SKY_TOP[2] * (1.0 - verticalPct) + RGB_SKY_BTM[2] * verticalPct)
            };

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // -------------------------------------------------------------
                // LAYER 1: DYNAMIC FLAME & DRIFTING EMBERS (Z-Depth: 0.95)
                // -------------------------------------------------------------
                double dx = x - fireBaseX;
                double dy = fireBaseY - y;

                double flameWarp = 1.8 * Math.sin(y * 0.5 - timeClock * 12.0)
                        * Math.cos(x * 0.3 + timeClock * 5.0);
                double adjustedDx = dx + flameWarp;
                double flameBoundY = (12.0 * flicker) - (adjustedDx * adjustedDx * 0.48);

                if (dy > 0 && dy < flameBoundY && 0.95 > zBuffer[index]) {
                    zBuffer[index] = 0.95;

                    int[] flameRGB = RGB_FIRE_OUTER;
                    char flameChar = '▒';

                    double midCoreBound = flameBoundY * 0.70 - (adjustedDx * adjustedDx * 0.25);
                    double innerCoreBound = flameBoundY * 0.40 - (adjustedDx * adjustedDx * 0.18);

                    if (dy < innerCoreBound && Math.abs(adjustedDx) < 2.2) {
                        flameRGB = RGB_FIRE_CORE;
                        flameChar = '█';
                    } else if (dy < midCoreBound) {
                        flameRGB = RGB_FIRE_MID;
                        flameChar = '▓';
                    } else {
                        flameChar = (dy > flameBoundY * 0.85) ? '░' : '▒';
                    }

                    String fColor = String.format("\u001B[38;2;%d;%d;%dm", flameRGB[0], flameRGB[1], flameRGB[2]);
                    outputBuffer[index] = fColor + flameChar + RESET;
                    continue;
                }

                // Upward-drifting sparks
                if (dy > 2.0 && dy < 22.0 && Math.abs(dx) < (2.0 + dy * 0.35)) {
                    double emberY = y + timeClock * 13.0;
                    double emberX = x + Math.sin(y * 0.4 + timeClock * 3.0) * 0.8;
                    double emberSeed = spatialHash((int) emberX, (int) emberY);

                    if (emberSeed > 0.948 && 0.95 > zBuffer[index]) {
                        zBuffer[index] = 0.95;
                        char eChar = (emberSeed > 0.980) ? '✦' : '·';
                        String eColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_FIRE_CORE[0], RGB_FIRE_MID[1], 40);
                        outputBuffer[index] = eColor + eChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // LAYER 2: STATIONARY SEATED TRAVELER SILHOUETTE (Z-Depth: 0.90)
                // -------------------------------------------------------------
                double kx = x - 75.0;
                double ky = fireBaseY - y;

                boolean isHead = (kx * kx + (ky - 6.0) * (ky - 6.0) <= 1.8);
                boolean isTorso = (kx >= -1.0 && kx <= 3.5 && ky >= 2.0 && ky <= 4.8);
                boolean isCloak = (kx >= 2.0 && kx <= 5.5 && ky >= 0.8 && ky <= 4.5);
                boolean isLegs = (kx >= -4.5 && kx <= 2.0 && ky >= -0.2 && ky <= 1.8);

                if ((isHead || isTorso || isCloak || isLegs) && 0.90 > zBuffer[index]) {
                    zBuffer[index] = 0.90;

                    boolean isFireFacingEdge = (kx <= -3.0 && ky <= 1.8)
                            || (kx <= 0.0 && ky > 1.8 && ky <= 4.8)
                            || (kx <= -0.5 && isHead);

                    int[] kRGB = RGB_KNIGHT_BASE;
                    char kChar = '█';

                    if (isFireFacingEdge) {
                        double glowFactor = Math.max(0.0, flicker);
                        kRGB = blendColors(RGB_KNIGHT_BASE, RGB_KNIGHT_LIGHT, 0.75 * glowFactor);
                        kChar = '▓';
                    } else if (isCloak) {
                        kChar = '▒';
                    }

                    String kColor = String.format("\u001B[38;2;%d;%d;%dm", kRGB[0], kRGB[1], kRGB[2]);
                    outputBuffer[index] = kColor + kChar + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 3: GROUND TEXTURE, STONE RING & LOGS (Z-Depth: 0.80)
                // -------------------------------------------------------------
                if (y >= fireBaseY && 0.80 > zBuffer[index]) {
                    zBuffer[index] = 0.80;

                    double gdx = (x - fireBaseX) * 0.45;
                    double gdy = (y - fireBaseY) * 1.85;
                    double groundRadius = Math.sqrt(gdx * gdx + gdy * gdy);

                    int[] groundRGB = RGB_SHADOW_TONE;
                    char groundChar = '▒';

                    boolean isLog = (y == fireBaseY && Math.abs(x - fireBaseX) < 5)
                            || (y == fireBaseY + 1 && Math.abs(x - fireBaseX) < 7);
                    boolean isStoneRing = (!isLog && groundRadius >= 2.8 && groundRadius <= 4.8 && y <= fireBaseY + 2);

                    if (isLog) {
                        groundRGB = blendColors(new int[] { 90, 50, 30 }, RGB_FIRE_MID, 0.3 * flicker);
                        groundChar = '█';
                    } else if (isStoneRing) {
                        groundRGB = blendColors(RGB_STONE_GRAY, RGB_FIRE_MID, 0.4 * flicker);
                        groundChar = (spatialHash(x, y) > 0.5) ? '▓' : '█';
                    } else {
                        double intensity = Math.max(0.0, 1.0 - (groundRadius / 22.0)) * flicker;
                        groundRGB = blendColors(RGB_SHADOW_TONE, RGB_FIRE_MID, intensity);

                        double gNoise = spatialHash(x, y);
                        if (intensity > 0.55) {
                            groundChar = (gNoise > 0.3) ? '█' : '▓';
                        } else if (intensity > 0.25) {
                            groundChar = (gNoise > 0.6) ? '▓' : ((gNoise > 0.2) ? '▒' : '░');
                        } else if (intensity > 0.08) {
                            groundChar = (gNoise > 0.7) ? '░' : ((gNoise > 0.4) ? '.' : ' ');
                        } else {
                            groundChar = (gNoise > 0.82) ? ',' : ((gNoise > 0.65) ? '.' : ' ');
                        }
                    }

                    String gColor = String.format("\u001B[38;2;%d;%d;%dm", groundRGB[0], groundRGB[1], groundRGB[2]);
                    outputBuffer[index] = gColor + groundChar + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 4: PINE FOREST TREELINE (Z-Depth: 0.70)
                // -------------------------------------------------------------
                boolean isPine = false;
                boolean isTreeTip = false;
                int treeEdgeDistance = 0;
                int hitTx = -1;

                for (int[] tree : PINE_TREES) {
                    int tx = tree[0];
                    int topY = tree[1];

                    if (y >= topY && y < fireBaseY) {
                        int relY = y - topY;

                        int tierH = 4;
                        int tierIndex = relY / tierH;
                        int inTierY = relY % tierH;
                        double halfWidth = 0.6 + tierIndex * 0.9 + inTierY * 0.5;

                        int dxTree = Math.abs(x - tx);
                        if (dxTree <= halfWidth) {
                            isPine = true;
                            hitTx = tx; // RECORD THE TREE CENTER

                            if (relY == 0 && dxTree == 0) {
                                isTreeTip = true;
                            }
                            treeEdgeDistance = (int) (halfWidth - dxTree);
                            break;
                        }
                    }
                }

                if (isPine && 0.70 > zBuffer[index]) {
                    zBuffer[index] = 0.70;

                    double distToFireX = Math.abs(x - fireBaseX);
                    double distToFireY = Math.abs(y - fireBaseY);
                    double fireDist = Math.sqrt(distToFireX * distToFireX + distToFireY * distToFireY * 0.4);

                    // Base radial glow
                    double fireTreeGlow = Math.max(0.0, 1.0 - (fireDist / 45.0)) * 0.42 * flicker;

                    // NEW: True Directional Rim Lighting
                    // If tree is on the left, light the right side (x >= hitTx).
                    // If tree is on the right, light the left side (x <= hitTx).
                    boolean facesFire = (hitTx < fireBaseX && x >= hitTx) || (hitTx > fireBaseX && x <= hitTx);

                    if (facesFire) {
                        // Give the fire-facing side a stronger base glow
                        fireTreeGlow += 0.12 * flicker;

                        // Add a subtle dither only to the illuminated side to simulate bark texture
                        if (x % 2 == 0) {
                            fireTreeGlow += 0.05 * flicker;
                        }
                    }

                    int[] treeRGB = blendColors(RGB_PINE_DARK, RGB_PINE_LIGHT, 0.35 + 0.65 * spatialHash(x, y));
                    treeRGB = blendColors(treeRGB, RGB_FIRE_MID, fireTreeGlow);

                    char treeChar;
                    if (isTreeTip) {
                        treeChar = '▲';
                    } else if (fireTreeGlow > 0.22) {
                        treeChar = (spatialHash(x, y) > 0.4) ? '▓' : '▒';
                    } else if (treeEdgeDistance == 0) {
                        treeChar = '▒';
                    } else {
                        treeChar = ((x + y) % 2 == 0) ? '▓' : '█';
                    }

                    String tColor = String.format("\u001B[38;2;%d;%d;%dm", treeRGB[0], treeRGB[1], treeRGB[2]);
                    outputBuffer[index] = tColor + treeChar + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 5: ATMOSPHERIC AIR GLOW SHADER (Z-Depth: 0.60)
                // Fills sky gap pixels behind the pine branches
                // -------------------------------------------------------------
                double glowDx = (x - fireBaseX) * 0.45;
                double glowDy = ((fireBaseY - 3) - y) * 1.8;
                double distToFire = Math.sqrt(glowDx * glowDx + glowDy * glowDy);

                if (distToFire < 22.0 && 0.60 > zBuffer[index]) {
                    zBuffer[index] = 0.60;

                    double weight = Math.max(0.0, 1.0 - (distToFire / 22.0)) * 0.65 * flicker;
                    int[] airRGB = blendColors(skyRGB, RGB_FIRE_MID, weight);

                    String airColor = String.format("\u001B[38;2;%d;%d;%dm", airRGB[0], airRGB[1], airRGB[2]);
                    outputBuffer[index] = airColor + "█" + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 6: DISTANT MOUNTAIN RIDGES (Z-Depth: 0.40)
                // -------------------------------------------------------------
                double ridgeFar = 14.0 + 3.5 * Math.sin(x * 0.06 + 0.8) + 1.2 * Math.cos(x * 0.14);
                double ridgeNear = 16.5 + 4.0 * Math.sin(x * 0.09 + 2.4) + 1.8 * Math.cos(x * 0.21);

                if (y >= (int) ridgeNear && 0.40 > zBuffer[index]) {
                    zBuffer[index] = 0.40;
                    String mColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_MOUNTAIN_NEAR[0], RGB_MOUNTAIN_NEAR[1],
                            RGB_MOUNTAIN_NEAR[2]);
                    outputBuffer[index] = mColor + "█" + RESET;
                    continue;
                } else if (y >= (int) ridgeFar && 0.40 > zBuffer[index]) {
                    zBuffer[index] = 0.40;
                    String mColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_MOUNTAIN_FAR[0], RGB_MOUNTAIN_FAR[1],
                            RGB_MOUNTAIN_FAR[2]);
                    outputBuffer[index] = mColor + "█" + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 7: STATIONARY TWINKLING STARFIELD & SKY (Z-Depth: 0.01)
                // -------------------------------------------------------------
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;

                    double starVal = spatialHash(x, y);

                    if (starVal > 0.972 && y < 15) {
                        double twinkle = Math.sin(starVal * 100.0 + timeClock * (2.0 + starVal * 3.0));

                        char starChar = (twinkle > 0.6) ? '✦'
                                : ((twinkle > 0.0) ? '★' : ((twinkle > -0.6) ? '*' : '·'));
                        int starBrightness = (int) (160 + 95 * Math.max(0.0, twinkle));

                        outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm%c%s",
                                starBrightness, starBrightness, Math.min(255, starBrightness + 20), starChar, RESET);
                    } else {
                        String sColor = String.format("\u001B[38;2;%d;%d;%dm", skyRGB[0], skyRGB[1], skyRGB[2]);
                        outputBuffer[index] = sColor + "█" + RESET;
                    }
                }
            }
        }
    }

    private double spatialHash(int x, int y) {
        long n = (long) x * 374761393L + (long) y * 668265263L;
        n = (n ^ (n >> 13)) * 1274126177L;
        return ((n ^ (n >> 16)) & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private int[] blendColors(int[] c1, int[] c2, double ratio) {
        double r = Math.max(0.0, Math.min(1.0, ratio));
        return new int[] {
                (int) (c1[0] * (1.0 - r) + c2[0] * r),
                (int) (c1[1] * (1.0 - r) + c2[1] * r),
                (int) (c1[2] * (1.0 - r) + c2[2] * r)
        };
    }
}