// TODO: Make the Sydney Opera House look better, this isn't good

public class SydneyOperaHouseLoader extends Loader {
    private static final StatusStage[] SYDNEY_STAGES = {
            new StatusStage(25, "Mapping spherical shell geometry segments:"),
            new StatusStage(50, "Weaving interlocking structural tile ribs:"),
            new StatusStage(75, "Calibrating harbor water surface reflections:"),
            new StatusStage(100, "Sydney Harbor Twilight Show Operational!")
    };

    // Architectural typography symbols for the sails and environment
    private static final char CH_RIB = '\u2588'; // █ Heavy ridge framework lines
    private static final char CH_TILE = '\u2591'; // ░ Glazed ceramic tile shell skin
    private static final char CH_WATER = '\u2248'; // ≈ Rippling harbor waves texture

    // Ceramic tile coloration states (White & Cream glazed shells)
    private static final int[] TILE_BASE = { 235, 230, 220 };
    private static final int[] TILE_SHADOW = { 150, 145, 140 };

    // Deep harbor ocean water tones
    private static final int[] WATER_DAY = { 20, 60, 95 };
    private static final int[] WATER_NIGHT = { 8, 14, 28 };

    // Vivid twilight sky gradient endpoints
    private static final int[] SKY_TOP = { 15, 10, 35 };
    private static final int[] SKY_BOTTOM = { 220, 90, 45 }; // Vibrant harbor sunset glow

    // Spectacular stage floodlight register (Pulsing Magenta/Cyan show)
    private static final int[] FLOODLIGHT = { 240, 30, 160 };

    private double timeClock = 0.0;
    private double rotationY = -0.3; // Slight static offset angle for optimal profile viewing
    private static final double CAMERA_DISTANCE = 3.6;

    public SydneyOperaHouseLoader() {
        super(SYDNEY_STAGES);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Slowly oscillate the view angle to showcase depth separation
        rotationY = -0.4 + 0.25 * Math.sin(timeClock * 0.2);
        timeClock += 0.025;

        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // 1. CHRONO ENVIRONMENT MATH
        double nightFactor = 0.5 + 0.5 * Math.sin(timeClock * 0.4);
        double dayFactor = 1.0 - nightFactor;
        double dynamicSpotlightPulse = nightFactor * (0.7 + 0.3 * Math.sin(timeClock * 1.5));

        // STEP 1: RENDER SUNSET-TO-TWILIGHT GRADIENT SKY BACKDROP
        for (int yp = 0; yp < 14; yp++) {
            double skyGrad = (double) yp / 13.0;
            int sr = (int) (SKY_TOP[0] * (1.0 - skyGrad) + SKY_BOTTOM[0] * skyGrad);
            int sg = (int) (SKY_TOP[1] * (1.0 - skyGrad) + SKY_BOTTOM[1] * skyGrad);
            int sb = (int) (SKY_TOP[2] * (1.0 - skyGrad) + SKY_BOTTOM[2] * skyGrad);

            String skyColor = String.format("\u001B[38;2;%d;%d;%dm", sr, sg, sb);
            for (int xp = 0; xp < 80; xp++) {
                outputBuffer[xp + 80 * yp] = skyColor + " " + RESET;
            }
        }

        // STEP 2: RENDER HARBOR WATER BASE WITH ANIMATED SPECULAR REFLECTIONS
        for (int yp = 14; yp < 22; yp++) {
            double waterDepthGrad = (double) (yp - 14) / 7.0;
            int wr = (int) (WATER_DAY[0] * dayFactor + WATER_NIGHT[0] * nightFactor);
            int wg = (int) (WATER_DAY[1] * dayFactor + WATER_NIGHT[1] * nightFactor);
            int wb = (int) (WATER_DAY[2] * dayFactor + WATER_NIGHT[2] * nightFactor);

            String baseWaterColor = String.format("\u001B[38;2;%d;%d;%dm", wr, wg, wb);
            for (int xp = 0; xp < 80; xp++) {
                int index = xp + 80 * yp;

                // Procedural wave ripple function
                double wave = Math.sin(xp * 0.3 + yp * 0.8 + timeClock * 2.0) * Math.cos(xp * 0.1 - timeClock);
                char waterChar = CH_WATER;

                // Add animated pink/magenta spotlight reflections dancing on the harbor waves
                int finalWr = wr;
                int finalWg = wg;
                int finalWb = wb;
                if (wave > 0.45 && dynamicSpotlightPulse > 0.1) {
                    double reflectionFalloff = dynamicSpotlightPulse * 0.4 * ((80.0 - xp) / 80.0);
                    finalWr = Math.min(255, wr + (int) (FLOODLIGHT[0] * reflectionFalloff));
                    finalWg = Math.min(255, wg + (int) (FLOODLIGHT[1] * reflectionFalloff));
                    finalWb = Math.min(255, wb + (int) (FLOODLIGHT[2] * reflectionFalloff));
                    waterChar = '-'; // Smooth specular sheen texture
                }

                String waterColor = String.format("\u001B[38;2;%d;%d;%dm", finalWr, finalWg, finalWb);
                outputBuffer[index] = waterColor + waterChar + RESET;
                zBuffer[index] = 0.001; // Low depth baseline for the sea floor
            }
        }

        // STEP 3: RENDER THE 3 GROUPS OF CONVEX SPHERICAL SAILS (Painter's Algorithm
        // built-in via scale tracking)
        // Shell set configuration: {centerX, centerZ, baseScale, maxElevationAngle,
        // azimuthSpread}
        double[][] shellGroups = {
                { -0.6, -0.2, 0.75, 1.2, 1.1 }, // Main Concert Hall Shell Cluster (Large)
                { 0.5, 0.3, 0.50, 0.9, 1.0 }, // Bennelong Restaurant Shell Cluster (Medium)
                { -1.1, 0.5, 0.28, 0.6, 0.8 } // Smaller auxiliary harbor side shells
        };

        for (double[] group : shellGroups) {
            double cx = group[0];
            double cz = group[1];
            double baseRadius = group[2];
            double maxElevation = group[3];
            double azimuthSpread = group[4];

            // Render 4 cascading layered shells nested behind one another inside each
            // cluster
            for (int shellId = 0; shellId < 4; shellId++) {
                double shellScale = baseRadius * (1.0 - shellId * 0.18);
                double forwardLeaningOffset = shellId * 0.15; // Pitches the shells forward cleanly

                // High precision parametric scanning across the surface of the partial sphere
                // segment
                for (double elevation = 0.0; elevation <= maxElevation; elevation += 0.02) {
                    // Taper azimuth wings inwards towards the apex to build sharp triangular points
                    double currentWingSpan = azimuthSpread * Math.cos((elevation / maxElevation) * (Math.PI / 2.0));

                    for (double azimuth = -currentWingSpan; azimuth <= currentWingSpan; azimuth += 0.02) {

                        // Spherical conversion generating the curved sail surfaces
                        double xLocal = cx + shellScale * Math.cos(elevation) * Math.sin(azimuth)
                                - forwardLeaningOffset;
                        double yLocal = 0.5 - shellScale * Math.sin(elevation); // Inverted coordinate space mapping
                        double zLocal = cz + shellScale * Math.cos(elevation) * Math.cos(azimuth);

                        // Don't render below the granite platform promenade deck base lines
                        if (yLocal > 0.45)
                            continue;

                        // Procedural structural texture lines modeling the prominent concrete roof ribs
                        double ribEquation = Math.sin(azimuth * 18.0);
                        char renderChar = CH_TILE;
                        boolean isStructuralRib = Math.abs(ribEquation) > 0.82;

                        if (isStructuralRib) {
                            renderChar = CH_RIB;
                        }

                        // Perspective space mapping transforms (Rotation around Y Axis)
                        double rx = xLocal * cosY + zLocal * sinY;
                        double ry = yLocal;
                        double rz = -xLocal * sinY + zLocal * cosY;

                        double ooz = 1.0 / (rz + CAMERA_DISTANCE);
                        int xp = (int) (38 + 56 * ooz * rx * 1.8);
                        int yp = (int) (12 + 24 * ooz * ry);

                        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                            int index = xp + 80 * yp;

                            // Depth mask visibility check override
                            if (ooz > zBuffer[index]) {
                                zBuffer[index] = ooz;

                                // 2. MATTE ARCHITECTURAL DIFFUSE SHADING
                                // Calculated relative to a static southwest ambient sun/light source direction
                                // vector
                                double diffuseLighting = 0.4 + 0.6 * (Math.cos(elevation) * Math.cos(azimuth + 0.6));

                                int r = (int) (TILE_SHADOW[0] * (1.0 - diffuseLighting)
                                        + TILE_BASE[0] * diffuseLighting);
                                int g = (int) (TILE_SHADOW[1] * (1.0 - diffuseLighting)
                                        + TILE_BASE[1] * diffuseLighting);
                                int b = (int) (TILE_SHADOW[2] * (1.0 - diffuseLighting)
                                        + TILE_BASE[2] * diffuseLighting);

                                // Blend dynamic glowing twilight art floodlights onto the shells at night hours
                                if (dynamicSpotlightPulse > 0.05) {
                                    // Make the light travel up the vertical height curve of the sails intensely
                                    double heightIlluminationFactor = (0.5 - yLocal) * dynamicSpotlightPulse * 1.2;
                                    r += (int) (FLOODLIGHT[0] * heightIlluminationFactor);
                                    g += (int) (FLOODLIGHT[1] * heightIlluminationFactor);
                                    b += (int) (FLOODLIGHT[2] * heightIlluminationFactor);
                                }

                                r = Math.max(0, Math.min(255, r));
                                g = Math.max(0, Math.min(255, g));
                                b = Math.max(0, Math.min(255, b));

                                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar
                                        + RESET;
                            }
                        }
                    }
                }
            }
        }
    }
}