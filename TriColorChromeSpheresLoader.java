public class TriColorChromeSpheresLoader extends Loader {
    private static final StatusStage[] REFLECTIVE_STAGES = {
            new StatusStage(25, "Tracing infinite ground floor:"),
            new StatusStage(50, "Generating parallel reflection vector fields:"),
            new StatusStage(75, "Splicing RGB metallic tint profiles:"),
            new StatusStage(100, "Alternating Juggling Matrix Engaged!")
    };

    private double timeClock = 0.0;
    private int width = 80;
    private int height = 22;
    private static final double CAMERA_DISTANCE = 1.6;

    private static final int BALL_COUNT = 3;
    private final double[] sphereX = new double[BALL_COUNT];
    private final double[] sphereY = new double[BALL_COUNT];
    private final double[] sphereZ = new double[BALL_COUNT];
    private final double[] sphereRadius = { 0.50, 0.50, 0.50 };

    // Core chromatic tint scalars applied to the mirror reflections
    private static final double[][] TINT_MODIFIERS = {
            { 1.0, 0.25, 0.25 }, // Sphere 0: High-Gloss Metallic Red
            { 0.25, 1.0, 0.35 }, // Sphere 1: High-Gloss Metallic Green
            { 0.25, 0.45, 1.0 } // Sphere 2: High-Gloss Metallic Blue
    };

    public TriColorChromeSpheresLoader() {
        super(REFLECTIVE_STAGES, 80, 22);
    }

    public TriColorChromeSpheresLoader(int w, int h) {
        super(REFLECTIVE_STAGES, w, h);
        this.width = w;
        this.height = h;
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.004;

        String shadingRamp = " .:-=+*#%@▓";
        double floorLevel = 0.65;
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // --- STEP 1: CALCULATE THE DYNAMIC ALTERNATING JUGGLING TRACKS ---
        // Uses a phase-shifted Infinity/Figure-8 curve layout to force true swapping
        // cross trajectories
        double widthX = 0.95;
        double depthZ = 0.65;
        double centerZ = 0.15;

        for (int i = 0; i < BALL_COUNT; i++) {
            // Offset each ball's entry timeline phase position uniformly by 120-degree
            // divisions (2 * PI / 3)
            double ballPhase = timeClock + (i * 2.0 * Math.PI / 3.0);

            // Lissajous curve parameters create a classic overlapping figure-8 flow
            sphereX[i] = widthX * Math.sin(ballPhase) * -1;
            sphereY[i] = -0.1; // FIXED ELEVATION: Balls lock to an identical vertical plane level

            // Doubling the frequency factor (ballPhase * 2.0) forces the balls to pitch
            // forward and backward independently in an alternating dance sequence
            sphereZ[i] = centerZ - depthZ * Math.sin(ballPhase * 2.0);
        }

        for (int y = 0; y < height; y++) {
            double screenY = ((double) y / height) * 2.0 - 1.0;

            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                double screenX = (((double) x / width) * 2.0 - 1.0) * 2.3;

                // Configure standard setup camera perspective eye rays
                double rayX = 0.0, rayY = 0.0, rayZ = -CAMERA_DISTANCE;
                double dirX = screenX, dirY = screenY, dirZ = 1.8;

                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= len;
                dirY /= len;
                dirZ /= len;

                // -------------------------------------------------------------
                // OBJECT A: MULTI-SPHERE RAY-INTERSECTION TRACKER
                // -------------------------------------------------------------
                int closestSphereIdx = -1;
                double tMin = Double.MAX_VALUE;

                for (int i = 0; i < BALL_COUNT; i++) {
                    double ocX = rayX - sphereX[i];
                    double ocY = rayY - sphereY[i];
                    double ocZ = rayZ - sphereZ[i];

                    double b = ocX * dirX + ocY * dirY + ocZ * dirZ;
                    double c = (ocX * ocX + ocY * ocY + ocZ * ocZ) - (sphereRadius[i] * sphereRadius[i]);
                    double discriminant = b * b - c;

                    if (discriminant >= 0) {
                        double t = -b - Math.sqrt(discriminant);
                        if (t > 0 && t < tMin) {
                            tMin = t;
                            closestSphereIdx = i;
                        }
                    }
                }

                boolean hitSphere = closestSphereIdx != -1;

                if (hitSphere) {
                    depthZ = 1.0 / tMin;

                    if (depthZ > zBuffer[idx]) {
                        zBuffer[idx] = depthZ;

                        double hitX = rayX + dirX * tMin;
                        double hitY = rayY + dirY * tMin;
                        double hitZ = rayZ + dirZ * tMin;

                        // Surface normal vectors mapped to the specific hit sphere index geometry
                        double nx = (hitX - sphereX[closestSphereIdx]) / sphereRadius[closestSphereIdx];
                        double ny = (hitY - sphereY[closestSphereIdx]) / sphereRadius[closestSphereIdx];
                        double nz = (hitZ - sphereZ[closestSphereIdx]) / sphereRadius[closestSphereIdx];

                        // Vector reflection logic: R = V - 2 * (V . N) * N
                        double dotVN = dirX * nx + dirY * ny + dirZ * nz;
                        double refX = dirX - 2.0 * dotVN * nx;
                        double refY = dirY - 2.0 * dotVN * ny;
                        double refZ = dirZ - 2.0 * dotVN * nz;

                        // Intense specular chrome highlight flare calculations
                        double specDot = refX * lightX + refY * lightY + refZ * lightZ;
                        double spec = (specDot > 0) ? Math.pow(specDot, 28) : 0;

                        // Default background chrome reflection environment base
                        int rReflect = 160, gReflect = 165, bReflect = 175;

                        if (refY > 0.001) {
                            double tFloor = (floorLevel - hitY) / refY;
                            if (tFloor > 0) {
                                double fx = hitX + refX * tFloor;
                                double fz = hitZ + refZ * tFloor;

                                int checkX = (int) (Math.floor(fx * 1.5));
                                int checkZ = (int) (Math.floor(fz * 1.5 - timeClock * 5.4));

                                if ((checkX + checkZ) % 2 == 0) {
                                    rReflect = 75;
                                    gReflect = 80;
                                    bReflect = 90;
                                } else {
                                    rReflect = 210;
                                    gReflect = 215;
                                    bReflect = 220;
                                }

                                double fog = Math.min(1.0, tFloor * 0.12);
                                rReflect = (int) (rReflect * (1.0 - fog) + 130 * fog);
                                gReflect = (int) (gReflect * (1.0 - fog) + 135 * fog);
                                bReflect = (int) (bReflect * (1.0 - fog) + 145 * fog);
                            }
                        }

                        // Filter the reflected environment pixel variables down our targeted RGB tint
                        // registers
                        double[] activeTint = TINT_MODIFIERS[closestSphereIdx];
                        int r = (int) (rReflect * activeTint[0]);
                        int g = (int) (gReflect * activeTint[1]);
                        int bColor = (int) (bReflect * activeTint[2]);

                        // Add burning specular highlights over top (Highlights retain pure white glare
                        // properties)
                        r += (int) (spec * 255);
                        g += (int) (spec * 255);
                        bColor += (int) (spec * 255);
                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        bColor = Math.max(0, Math.min(255, bColor));

                        int shadeIdx = (int) ((0.18 + spec * 0.82) * (shadingRamp.length() - 1));
                        char renderChar = shadingRamp.charAt(Math.max(0, Math.min(shadingRamp.length() - 1, shadeIdx)));

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, bColor);
                        outputBuffer[idx] = colorCode + renderChar + RESET;
                    }
                }

                // -------------------------------------------------------------
                // OBJECT B: PERSPECTIVE ASPHALT CHECKERBOARD WITH TRIPLE SHADOWS
                // -------------------------------------------------------------
                if (!hitSphere) {
                    if (dirY > 0.005) {
                        double tFloor = (floorLevel - rayY) / dirY;
                        depthZ = 1.0 / tFloor;

                        if (depthZ > zBuffer[idx]) {
                            zBuffer[idx] = depthZ;

                            double fx = rayX + dirX * tFloor;
                            double fz = rayZ + dirZ * tFloor;

                            int checkX = (int) (Math.floor(fx * 1.5));
                            int checkY = (int) (Math.floor(fz * 1.5 - timeClock * 5.4));

                            int r, g, bColor;
                            if ((checkX + checkY) % 2 == 0) {
                                r = 60;
                                g = 65;
                                bColor = 75;
                            } else {
                                r = 185;
                                g = 190;
                                bColor = 195;
                            }
                            // --- ACCUMULATE DROP SHADOW PENUMBRAS FOR ALL 3 BALLES ---
                            double totalShadowFactor = 1.0;
                            for (int i = 0; i < BALL_COUNT; i++) {
                                double lightDistToFloor = (floorLevel - sphereY[i]);
                                double shadowCenterX = sphereX[i] - (lightX / -lightY) * lightDistToFloor;
                                double shadowCenterZ = sphereZ[i] - (lightZ / -lightY) * lightDistToFloor * 0.6;
                                double dx = fx - shadowCenterX;
                                double dz = fz - shadowCenterZ;
                                double distToShadowCenter = Math.sqrt(dx * dx + dz * dz);
                                double shadowLimit = sphereRadius[i] * 1.25;
                                if (distToShadowCenter < shadowLimit) {
                                    double individualShadow = 0.35 + 0.65 * (distToShadowCenter / shadowLimit);
                                    totalShadowFactor *= individualShadow;
                                }
                            }
                            r *= totalShadowFactor;
                            g *= totalShadowFactor;
                            bColor *= totalShadowFactor;
                            // Linear landscape atmospheric depth fog tracking
                            double horizonFog = Math.min(1.0, tFloor * 0.09);
                            r = (int) (r * (1.0 - horizonFog) + 25 * horizonFog);
                            g = (int) (g * (1.0 - horizonFog) + 25 * horizonFog);
                            bColor = (int) (bColor * (1.0 - horizonFog) + 30 * horizonFog);
                            r = Math.max(0, Math.min(255, r));
                            g = Math.max(0, Math.min(255, g));
                            bColor = Math.max(0, Math.min(255, bColor));
                            char renderChar = shadingRamp.charAt(shadingRamp.length() - 1);
                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, bColor);
                            outputBuffer[idx] = colorCode + renderChar + RESET;
                        }
                    } else {
                        // Background vacuum canvas space
                        if ((x + y * 13) % 31 == 0) {
                            outputBuffer[idx] = "\u001B[38;2;65;70;85m.\u001B[0m";
                        } else {
                            outputBuffer[idx] = " ";
                        }
                        zBuffer[idx] = 0.0;
                    }
                }
            }
        }
    }
}