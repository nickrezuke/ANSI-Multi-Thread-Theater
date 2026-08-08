import java.util.Random;

public class EarthLoader extends Loader {
    private static final StatusStage[] EARTH_STAGES = {
            new StatusStage(30, "Establishing equatorial axis:"),
            new StatusStage(65, "Syncing near-side land mass contours:"),
            new StatusStage(90, "Simulating Rayleigh edge scattering:"),
            new StatusStage(100, "Global Geographic Matrix Online!")
    };

    private static final int MAX_STARS = 45;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];

    private double earthRotationAngle = 0.0;

    // -------------------------------------------------------------------------
    // HIGH-ACCURACY EQUIRECTANGULAR CONTINENT BITMAP (72 Columns x 36 Rows)
    // Map bounds: Lat [90N to 90S], Lon [-180W to 180E]
    // -------------------------------------------------------------------------
    private static final String[] EARTH_BITMAP = {
            "000000000000000000000000000000000000000000000000000000000000000000000000", // 90N
            "000000000000000000001100001111100000000000000000000000000000000000000000", // 85N
            "000000000000000001110011111111110000000000000000000000001000000000000000", // 80N
            "000000000000011010111000011111110000000000000000000011111111110011000000", // 75N
            "100111111111111111110110011110000000000111110111111111111111111111111111", // 70N
            "000111111111111111000110001100000000011111111111111111111111111111111110", // 65N
            "000000000111111111001111000000000000001001111111111111111111111100011000", // 60N
            "000000000011111111111111100000000001011111111111111111111111111100000000", // 55N
            "000000000001111111111110000000000000111111111111111111111111111100000000", // 50N
            "000000000001111111111100000000000011100111001111111111111111110000000000", // 45N
            "000000000001111111111000000000000011000001111111111111111111010000000000", // 40N
            "000000000000111111110000000000000011111000011111111111111111000000000000", // 35N
            "000000000000001110000000000000000111111111111111111111111111000000000000", // 30N
            "000000000000000110000000000000000111111111111111001111111110000000000000", // 25N
            "000000000000000011100000000000000111111111111110000110011100000000000000", // 20N
            "000000000000000000100000000000000111111111111000000100001100000000000000", // 15N
            "000000000000000000001111000000000011111111111100000000000000000000000000", // 10N
            "000000000000000000001111110000000000001111111000000000001011000000000000", // 5N
            "000000000000000000001111111100000000001111110000000000001010000100000000", // 0
            "000000000000000000001111111110000000000111110000000000000000000010000000", // 5S
            "000000000000000000000111111100000000000111110000000000000000001000000000", // 10S
            "000000000000000000000011111100000000001111110100000000000000011110000000", // 15S
            "000000000000000000000011111100000000000111100100000000000001111111000000", // 20S
            "000000000000000000000011110000000000000111100000000000000001111111100000", // 25S
            "000000000000000000000011110000000000000011000000000000000001100111000000", // 30S
            "000000000000000000000111000000000000000000000000000000000000000011000000", // 35S
            "000000000000000000000110000000000000000000000000000000000000000000000000", // 40S
            "000000000000000000000110000000000000000000000000000000000000000000000000", // 45S
            "000000000000000000000100000000000000000000000000000000000000000000000000", // 50S
            "000000000000000000000000000000000000000000000000000000000000000000000000", // 55S
            "000000000000000000000000000000000000000000000000000000000000000000000000", // 60S
            "000000000000000000000000000000000000000000000111100011111111111110000000", // 65S
            "000000000000000000000111000000000111111111111111111111111111111111111100", // 70S
            "000000111111111111111000000000111111111111111111111111111111111111111000", // 75S
            "000001111111111111111110011111111111111111111111111111111111111111111000", // 80S
            "111111111111111111111111111111111111111111111111111111111111111111111111" // 85S
    };

    public EarthLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
    }

    public EarthLoader() {
        super(EARTH_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.earthRotationAngle = 0.0;

        Random rand = new Random(9999);
        for (int i = 0; i < MAX_STARS; i++) {
            int rx = rand.nextInt(80);
            int ry = 1 + rand.nextInt(20);
            starPositions[i] = ry * 80 + rx;
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0;
        }

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Draw the Background Starfield
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            int starIdx = starPositions[i];
            double twinkleFactor = Math.sin((currentTime * 0.004) + starPhases[i]);

            char starChar = ' ';
            if (twinkleFactor > 0.82)
                starChar = '*';
            else if (twinkleFactor > 0.20)
                starChar = '.';
            else if (twinkleFactor > -0.3)
                starChar = '·';

            if (starChar != ' ' && starIdx >= 0 && starIdx < 1760) {
                zBuffer[starIdx] = 0.0001;
                outputBuffer[starIdx] = "\u001B[37m" + starChar + RESET;
            }
        }

        // Step 2: Compute Planetary Spin & Tilt
        earthRotationAngle += 0.0035;
        earthRotationAngle %= (2.0 * Math.PI);

        double axialTilt = Math.toRadians(23.5);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        double lightX = 0.35;
        double lightY = -0.85;
        double lightZ = 0.35;

        double cameraDistance = 3.6;
        double sphereRadius = 1.0;

        // Step 3: Render 3D Earth Globe
        for (double theta = 0.01; theta < Math.PI; theta += 0.015) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            double lat = (Math.PI / 2.0) - theta;

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.015) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double lx = sphereRadius * sinTheta * cosPhi;
                double ly = sphereRadius * sinTheta * sinPhi;
                double lz = sphereRadius * cosTheta;

                double rawLong = Math.atan2(sinPhi, cosPhi);

                double animatedLong = rawLong - earthRotationAngle;
                if (animatedLong < -Math.PI)
                    animatedLong += 2.0 * Math.PI;
                if (animatedLong > Math.PI)
                    animatedLong -= 2.0 * Math.PI;

                double rx = lx * cosTilt - lz * sinTilt;
                double ry = ly;
                double rz = lx * sinTilt + lz * cosTilt;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22 && ry < 0) {
                    int index = xp + 80 * yp;

                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        double nx = rx / sphereRadius;
                        double ny = ry / sphereRadius;
                        double nz = rz / sphereRadius;

                        // -------------------------------------------------------------
                        // EQUIRECTANGULAR BITMAP RASTERIZER
                        // Map Lat/Lon directly to Bitmap Grid Coordinates
                        // -------------------------------------------------------------
                        double latDeg = Math.toDegrees(lat);
                        double lonDeg = Math.toDegrees(animatedLong);

                        int mapRow = (int) ((90.0 - latDeg) / 180.0 * EARTH_BITMAP.length);
                        mapRow = Math.max(0, Math.min(EARTH_BITMAP.length - 1, mapRow));

                        String bitmapLine = EARTH_BITMAP[mapRow];
                        int mapCol = (int) ((lonDeg + 180.0) / 360.0 * bitmapLine.length());
                        mapCol = Math.max(0, Math.min(bitmapLine.length() - 1, mapCol));

                        boolean isLand = bitmapLine.charAt(mapCol) == '1';

                        // -------------------------------------------------------------
                        // SHADING & COLOR ENGINE
                        // -------------------------------------------------------------
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);
                        double finalLuminance = 0.34 + 0.66 * baseLight;

                        String palette = " .:-=+#%@█";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        if (isLand) {
                            if (latDeg < -62.0 || latDeg > 70.0) {
                                // Ice Caps
                                outR = (int) (220 * finalLuminance);
                                outG = (int) (225 * finalLuminance);
                                outB = (int) (235 * finalLuminance);
                            } else {
                                // Vegetation
                                outR = (int) (35 * finalLuminance);
                                outG = (int) (155 * finalLuminance);
                                outB = (int) (45 * finalLuminance);
                            }
                        } else {
                            // Deep Blue Ocean
                            outR = (int) (10 * finalLuminance);
                            outG = (int) (55 * finalLuminance);
                            outB = (int) (185 * finalLuminance);
                        }

                        // Atmosphere Edge Glow
                        double rimFactor = 1.0 - 0.55 * (nx * nx + nz * nz);
                        if (rimFactor > 0.75) {
                            double glow = (rimFactor - 0.75) / 0.4;
                            outR += (int) (40 * glow * finalLuminance);
                            outG += (int) (130 * glow * finalLuminance);
                            outB += (int) (255 * glow * finalLuminance);
                        }

                        outR = Math.max(0, Math.min(255, outR));
                        outG = Math.max(0, Math.min(255, outG));
                        outB = Math.max(0, Math.min(255, outB));

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", outR, outG, outB);
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }
    }
}