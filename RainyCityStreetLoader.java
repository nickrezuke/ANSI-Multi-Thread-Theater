// TODO: The rain / reflection is too fast and jittery at times, fix that

public class RainyCityStreetLoader extends Loader {
    private static final StatusStage[] RAIN_STAGES = {
            new StatusStage(20, "Erecting neon skyscraper outlines:"),
            new StatusStage(45, "Igniting vertical storefront light grids:"),
            new StatusStage(70, "Simulating vertical wet rain streak vectors:"),
            new StatusStage(100, "Rainy City Street Simulation Synchronized!")
    };

    private double timeClock = 0.0;
    private final int width = 120;
    private final int height = 36;

    // Meticulously matched low-saturation urban neon color registers
    private static final int[] RGB_SKY = { 12, 10, 22 }; // Dark Cyberpunk Indigo
    private static final int[] RGB_BUILDING = { 22, 18, 32 }; // Clean Skyscraper Concrete
    private static final int[] RGB_NEON_A = { 235, 35, 110 }; // Soft Cyberpunk Magenta
    private static final int[] RGB_NEON_B = { 35, 185, 225 }; // Muted Neon Cyan
    private static final int[] RGB_NEON_C = { 230, 160, 25 }; // Warm Amber Window Glow
    private static final int[] RGB_ASPHALT = { 16, 14, 24 }; // Deep Wet Asphalt
    private static final int[] RGB_PEDESTRIAN = { 8, 8, 12 }; // Deep Ambient Silhouette Shield

    // Entity tracking structure to cache walking pedestrians for reflection
    // calculations
    private static final int MAX_PEDESTRIANS = 3;
    private final int[] cachedPedX = new int[MAX_PEDESTRIANS];
    private final int[] cachedPedColorIdx = new int[MAX_PEDESTRIANS]; // 0=A, 1=B, 2=C
    private final int[] cachedPedDirection = new int[MAX_PEDESTRIANS];

    public RainyCityStreetLoader() {
        super(RAIN_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.05; // Deliberate speed tracking for smooth cinemagraph fluid movement

        int groundY = 24; // Horizon boundary line split separating street surface from sky architecture

        // --- PRE-CALCULATE AND CACHE DYNAMIC PEDESTRIAN POSITION MATRICES ---
        for (int pId = 0; pId < MAX_PEDESTRIANS; pId++) {
            double speed = 3.2 + pId * 1.2;
            double direction = (pId % 2 == 0) ? 1.0 : -1.0;
            double px = (direction > 0) ? ((timeClock * speed + pId * 45) % (width + 24)) - 12
                    : width - ((timeClock * speed + pId * 45) % (width + 24)) + 12;
            cachedPedX[pId] = (int) px;
            cachedPedColorIdx[pId] = pId;
            cachedPedDirection[pId] = (int) direction;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                int[] pixelRGB = RGB_SKY;
                double zDepth = 0.01;

                // -------------------------------------------------------------
                // LAYER 1: SKYLINE SCENE BACKGROUND
                // -------------------------------------------------------------
                if (y < groundY) {
                    pixelRGB = getSkySceneRGB(x, y);

                    // Scrolling rain sheet noise mask overlay
                    double rainMask = Math.sin((x * 0.75 + y * 1.5) - timeClock * 22.0)
                            * Math.cos((x * 0.25 - y * 1.2) + timeClock * 14.0);

                    if (rainMask > 0.62) {
                        pixelRGB = new int[] {
                                Math.min(255, (int) (pixelRGB[0] * 0.80 + 110 * 0.20)),
                                Math.min(255, (int) (pixelRGB[1] * 0.80 + 130 * 0.20)),
                                Math.min(255, (int) (pixelRGB[2] * 0.80 + 160 * 0.20))
                        };
                    }
                }

                // -------------------------------------------------------------
                // LAYER 2: WET ASPHALT WITH MIRROR NEON & ENTITY REFLECTIONS
                // -------------------------------------------------------------
                if (y >= groundY) {
                    zDepth = 0.50;

                    // Reverse ray projection lookup: flip coordinates back up into storefront sky
                    // space
                    int reflectedY = groundY - (y - groundY) - 1;
                    reflectedY = Math.max(0, Math.min(groundY - 1, reflectedY));

                    // Continuous horizontal warp creates liquid ripples on the puddle surface
                    double waveWarp = 2.2 * Math.sin(y * 1.2 - timeClock * 5.5) * Math.cos(x * 0.35);
                    int sampleX = Math.max(0, Math.min(width - 1, x + (int) waveWarp));

                    // Gather underlying sky backdrop structure color index
                    int[] reflectRGB = getSkySceneRGB(sampleX, reflectedY);

                    // --- NEW RULE: EVALUATE WALKWAY PEDESTRIAN INVERSE BILLBOARD REFLECTIONS ---
                    boolean hitPedestrianReflection = false;
                    int pBaseY = groundY + 2;
                    int reflectedPedY = pBaseY + (pBaseY - y) - 4; // Map height reflection axis accurately

                    for (int pId = 0; pId < MAX_PEDESTRIANS; pId++) {
                        int pBaseX = cachedPedX[pId];
                        int dx = sampleX - pBaseX; // Evaluates tracking relative to warped ripple coordinates
                        int dy = pBaseY - reflectedPedY;

                        boolean inReflectedUmbrella = (dy == 7 && Math.abs(dx) <= 3) ||
                                (dy == 8 && Math.abs(dx) <= 2) ||
                                (dy == 6 && Math.abs(dx) == 4);

                        boolean inReflectedCoat = (dy >= 1 && dy <= 5 && Math.abs(dx) <= 1) ||
                                (dy >= 2 && dy <= 4 && dx == 2 * cachedPedDirection[pId]);

                        boolean inReflectedHead = (dy == 6 && dx == 0);
                        boolean inReflectedStem = (dx == 0 && dy >= 4 && dy <= 7);

                        if (inReflectedUmbrella) {
                            int[] uRGB = (pId == 0) ? RGB_NEON_A : (pId == 1) ? RGB_NEON_B : RGB_NEON_C;
                            // Inject muted transmissive values for reflection distortion clarity
                            reflectRGB = new int[] { (int) (uRGB[0] * 0.7), (int) (uRGB[1] * 0.7),
                                    (int) (uRGB[2] * 0.7) };
                            hitPedestrianReflection = true;
                            break;
                        } else if (inReflectedCoat || inReflectedHead || inReflectedStem) {
                            reflectRGB = RGB_PEDESTRIAN;
                            hitPedestrianReflection = true;
                            break;
                        }
                    }

                    // Multiplicative blending ties elements down into the deep asphalt floor layers
                    double mixRatio = hitPedestrianReflection ? 0.65 : 0.40; // Emphasize coat shapes slightly
                    pixelRGB = new int[] {
                            Math.max(0, (int) (reflectRGB[0] * mixRatio + RGB_ASPHALT[0] * (1.0 - mixRatio))),
                            Math.max(0, (int) (reflectRGB[1] * mixRatio + RGB_ASPHALT[1] * (1.0 - mixRatio))),
                            Math.max(0, (int) (reflectRGB[2] * mixRatio + RGB_ASPHALT[2] * (1.0 - mixRatio)))
                    };

                    // Sub-pixel ground impact splash ring points catch
                    double splashNoise = Math.sin(x * 9.3 + y * 6.4 + timeClock * 18.0);
                    if (splashNoise > 0.93) {
                        pixelRGB = new int[] {
                                Math.min(255, pixelRGB[0] + 45),
                                Math.min(255, pixelRGB[1] + 50),
                                Math.min(255, pixelRGB[2] + 65)
                        };
                    }
                }

                // -------------------------------------------------------------
                // LAYER 3: ASYMMETRIC FOREGROUND PEDESTRIAN ENTITIES
                // -------------------------------------------------------------
                boolean hitPedestrian = false;
                for (int pId = 0; pId < MAX_PEDESTRIANS; pId++) {
                    int pBaseX = cachedPedX[pId];
                    int pBaseY = groundY + 2;

                    int dx = x - pBaseX;
                    int dy = pBaseY - y;

                    boolean inUmbrella = (dy == 7 && Math.abs(dx) <= 3) ||
                            (dy == 8 && Math.abs(dx) <= 2) ||
                            (dy == 6 && Math.abs(dx) == 4);

                    boolean inCoat = (dy >= 1 && dy <= 5 && Math.abs(dx) <= 1) ||
                            (dy >= 2 && dy <= 4 && dx == 2 * cachedPedDirection[pId]);

                    boolean inHead = (dy == 6 && dx == 0);
                    boolean inStem = (dx == 0 && dy >= 4 && dy <= 7);

                    if (inUmbrella) {
                        if (0.95 > zDepth) {
                            zDepth = 0.95;
                            pixelRGB = (pId == 0) ? RGB_NEON_A : (pId == 1) ? RGB_NEON_B : RGB_NEON_C;
                            hitPedestrian = true;
                            break;
                        }
                    } else if (inCoat || inHead || inStem) {
                        if (0.95 > zDepth) {
                            zDepth = 0.95;
                            pixelRGB = RGB_PEDESTRIAN;
                            hitPedestrian = true;
                            break;
                        }
                    }
                }
                if (hitPedestrian) {
                    String colorString = String.format("\u001B[38;2;%d;%d;%dm", pixelRGB[0], pixelRGB[1], pixelRGB[2]);
                    outputBuffer[index] = colorString + "█" + RESET;
                    continue;
                }
                // Render the finalized canvas data directly down to the screen text buffer
                if (zDepth > zBuffer[index]) {
                    zBuffer[index] = zDepth;
                    String colorString = String.format("\u001B[38;2;%d;%d;%dm", pixelRGB[0], pixelRGB[1], pixelRGB[2]);
                    outputBuffer[index] = colorString + "█" + RESET;
                }
            }
        }
    }

    private int[] getSkySceneRGB(int x, int y) {
        // FIXED COORDINATES: Moved buildings inward slightly (e.g. 14 -> 20) to fix
        // edge reflection clipping
        boolean neonSignA = (x >= 20 && x <= 29) && (y >= 7 && y <= 22);
        boolean neonSignB = (x >= 56 && x <= 62) && (y >= 4 && y <= 23);
        boolean neonSignC = (x >= 92 && x <= 100) && (y >= 10 && y <= 21);
        if (neonSignA) {
            return (y % 5 == 0 || x == 20 || x == 29) ? RGB_BUILDING : RGB_NEON_A;
        }
        if (neonSignB) {
            return (y % 6 == 1 || x == 56 || x == 62) ? RGB_BUILDING : RGB_NEON_B;
        }
        if (neonSignC) {
            return (x % 3 == 0 && y % 3 == 0) ? RGB_BUILDING : RGB_NEON_C;
        }
        // Mid-ground structural background tower blocks (Also shifted away from
        // boundaries)
        boolean buildingLeft = (x >= 8 && x <= 40) && y >= 6;
        boolean buildingCenter = (x >= 48 && x <= 78) && y >= 2;
        boolean buildingRight = (x >= 86 && x <= 114) && y >= 9;
        if (buildingLeft || buildingCenter || buildingRight) {
            int windowX = x % 5;
            int windowY = y % 4;
            boolean isWindowCell = windowX >= 2 && windowX <= 3 && windowY == 2;
            if (isWindowCell && ((x * 11 + y * 7) % 7 == 2)) {
                return RGB_NEON_C;
            }
            return RGB_BUILDING;
        }
        return RGB_SKY;
    }
}