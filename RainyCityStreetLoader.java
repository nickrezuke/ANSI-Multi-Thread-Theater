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

    private static final int[] RGB_SKY = { 12, 10, 22 }; 
    private static final int[] RGB_BUILDING = { 22, 18, 32 }; 
    private static final int[] RGB_NEON_A = { 235, 35, 110 }; 
    private static final int[] RGB_NEON_B = { 35, 185, 225 }; 
    private static final int[] RGB_NEON_C = { 230, 160, 25 }; 
    private static final int[] RGB_ASPHALT = { 16, 14, 24 }; 
    private static final int[] RGB_PEDESTRIAN = { 8, 8, 12 }; 

    private static final double RAIN_PRIMARY_TIME_SPEED = 22.0; 
    private static final double RAIN_SECONDARY_TIME_SPEED = 14.0; 
    private static final double WAVE_WARP_TIME_SPEED = 1.8; 
    private static final double SPLASH_TIME_SPEED = 0.9; 

    private static final int MAX_PEDESTRIANS = 3;
    private final int[] cachedPedX = new int[MAX_PEDESTRIANS];
    private final int[] cachedPedColorIdx = new int[MAX_PEDESTRIANS]; // 0=A, 1=B, 2=C
    private final int[] cachedPedDirection = new int[MAX_PEDESTRIANS];

    public RainyCityStreetLoader() {
        // This one is specific
        super(RAIN_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.05; 

        int groundY = 24;

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

                    double rainMask = Math.sin((x * 0.75 + y * 1.5) - timeClock * RAIN_PRIMARY_TIME_SPEED)
                            * Math.cos((x * 0.25 - y * 1.2) + timeClock * RAIN_SECONDARY_TIME_SPEED);

                    double rainIntensity = smoothstep(0.50, 0.72, rainMask);
                    if (rainIntensity > 0.0) {
                        double blend = 0.20 * rainIntensity;
                        pixelRGB = new int[] {
                                Math.min(255, (int) (pixelRGB[0] * (1.0 - blend) + 110 * blend)),
                                Math.min(255, (int) (pixelRGB[1] * (1.0 - blend) + 130 * blend)),
                                Math.min(255, (int) (pixelRGB[2] * (1.0 - blend) + 160 * blend))
                        };
                    }
                }

                // -------------------------------------------------------------
                // LAYER 2: WET ASPHALT WITH MIRROR NEON & ENTITY REFLECTIONS
                // -------------------------------------------------------------
                if (y >= groundY) {
                    zDepth = 0.50;

                    int reflectedY = groundY - (y - groundY) - 1;
                    reflectedY = Math.max(0, Math.min(groundY - 1, reflectedY));

                    double waveWarp = 2.2 * Math.sin(y * 1.2 - timeClock * WAVE_WARP_TIME_SPEED) * Math.cos(x * 0.35);
                    int sampleX = Math.max(0, Math.min(width - 1, x + (int) Math.round(waveWarp)));

                    int[] reflectRGB = getSkySceneRGB(sampleX, reflectedY);

                    boolean hitPedestrianReflection = false;
                    int pBaseY = groundY + 2;
                    int reflectedPedY = pBaseY + (pBaseY - y) - 4;

                    for (int pId = 0; pId < MAX_PEDESTRIANS; pId++) {
                        int pBaseX = cachedPedX[pId];
                        int dx = sampleX - pBaseX;
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

                    double mixRatio = hitPedestrianReflection ? 0.65 : 0.40;
                    pixelRGB = new int[] {
                            Math.max(0, (int) (reflectRGB[0] * mixRatio + RGB_ASPHALT[0] * (1.0 - mixRatio))),
                            Math.max(0, (int) (reflectRGB[1] * mixRatio + RGB_ASPHALT[1] * (1.0 - mixRatio))),
                            Math.max(0, (int) (reflectRGB[2] * mixRatio + RGB_ASPHALT[2] * (1.0 - mixRatio)))
                    };

                    double splashNoise = Math.sin(x * 9.3 + y * 6.4 + timeClock * SPLASH_TIME_SPEED);
                    double splashIntensity = smoothstep(0.88, 0.97, splashNoise);
                    if (splashIntensity > 0.0) {
                        pixelRGB = new int[] {
                                Math.min(255, pixelRGB[0] + (int) (45 * splashIntensity)),
                                Math.min(255, pixelRGB[1] + (int) (50 * splashIntensity)),
                                Math.min(255, pixelRGB[2] + (int) (65 * splashIntensity))
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
                if (zDepth > zBuffer[index]) {
                    zBuffer[index] = zDepth;
                    String colorString = String.format("\u001B[38;2;%d;%d;%dm", pixelRGB[0], pixelRGB[1], pixelRGB[2]);
                    outputBuffer[index] = colorString + "█" + RESET;
                }
            }
        }
    }

    private int[] getSkySceneRGB(int x, int y) {
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

    private static double clamp01(double v) {
        if (v < 0.0) {
            return 0.0;
        }
        if (v > 1.0) {
            return 1.0;
        }
        return v;
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }
}