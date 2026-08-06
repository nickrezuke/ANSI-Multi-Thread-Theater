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

    // Dynamic timeline tracking our continuous planetary spin
    private double earthRotationAngle = 0.0;

    public EarthLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
    }

    public EarthLoader() {
        super(EARTH_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.earthRotationAngle = 0.0;

        // Procedurally generate a fixed random star field that skips text margins
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
        // Step 1: Draw the Twinkling Background Starfield
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

        // Step 2: COMPUTE ROTATION WITH INTENSITY ADJUSTMENTS
        // FIX 1: Slowed rotation speed down by exactly a factor of 10 (0.035 -> 0.0035)
        earthRotationAngle += 0.0035;

        // FIX 2: Explicit structural wrap guard ensures angles never spill to infinity,
        // completely resolving the permanent "blue ocean planet" disappearance bug!
        earthRotationAngle %= (2.0 * Math.PI);

        // Natural Earth axial tilt inclination (23.5 degrees)
        double axialTilt = Math.toRadians(23.5);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // Broad, high-visibility directional light source targeting the near face
        double lightX = 0.35;
        double lightY = -0.85;
        double lightZ = 0.35;

        double cameraDistance = 3.6;
        double sphereRadius = 1.0;

        // Step 3: Render the Texturized 3D Earth Globe
        for (double theta = 0.01; theta < Math.PI; theta += 0.015) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            // Latitude mapping in radians
            double lat = (Math.PI / 2.0) - theta;

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.015) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                // Local unrotated positions
                double lx = sphereRadius * sinTheta * cosPhi;
                double ly = sphereRadius * sinTheta * sinPhi;
                double lz = sphereRadius * cosTheta;

                // Longitude vector mapping (-PI to +PI)
                double rawLong = Math.atan2(sinPhi, cosPhi);

                // Track our slow horizontal drift rotation step
                double animatedLong = rawLong - earthRotationAngle;
                if (animatedLong < -Math.PI)
                    animatedLong += 2.0 * Math.PI;
                if (animatedLong > Math.PI)
                    animatedLong -= 2.0 * Math.PI;

                // Apply the 23.5-degree global axial tilt rotation matrix (Roll around depth
                // axis)
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
                        // GEOGRAPHY MATRIX: TRUE CONTINENTAL OUTLINES
                        // -------------------------------------------------------------
                        boolean isLand = false;
                        double latDeg = Math.toDegrees(lat);
                        double lonDeg = Math.toDegrees(animatedLong);

                        if (latDeg < -62.0) {
                            // Antarctica Polar Core
                            isLand = true;
                        } else if (lonDeg > -90.0 && lonDeg < -34.0 && latDeg > -56.0 && latDeg < 13.0) {
                            // South America
                            double halfWidth = (latDeg + 56.0) * 1.4;
                            if (Math.abs(lonDeg + 60.0) < halfWidth || latDeg > -15.0) {
                                isLand = true;
                            }
                        } else if (lonDeg > -168.0 && lonDeg < -52.0 && latDeg >= 13.0 && latDeg < 76.0) {
                            // North America
                            if (!(lonDeg < -114.0 && latDeg < 32.0)) { // Clear California Gulf
                                if (latDeg > 48.0 || lonDeg < -74.0 || (lonDeg > -100.0 && latDeg > 24.0)
                                        || lonDeg <= -100.0) {
                                    isLand = true;
                                }
                            }
                        } else if (lonDeg > -22.0 && lonDeg < 52.0 && latDeg > -35.0 && latDeg < 38.0) {
                            // Africa
                            if (latDeg > 6.0) {
                                isLand = (lonDeg > -17.0 && lonDeg < 51.0);
                            } else {
                                double halfWidth = (latDeg + 35.0) * 0.65;
                                isLand = (Math.abs(lonDeg - 21.0) < halfWidth);
                            }
                        } else if (lonDeg >= -12.0 && lonDeg < 168.0 && latDeg >= 10.0 && latDeg < 76.0) {
                            // Eurasia
                            isLand = true;
                            if (latDeg < 24.0 && lonDeg > 66.0 && lonDeg < 94.0) {
                                isLand = (Math.abs(lonDeg - 80.0) < (latDeg - 6.0) * 0.85); // India Triangle
                            }
                            if (latDeg < 32.0 && lonDeg > 34.0 && lonDeg < 62.0) {
                                isLand = true; // Arabian Block
                            }
                        } else if (lonDeg > 112.0 && lonDeg < 154.0 && latDeg > -39.0 && latDeg < -10.0) {
                            // Australia
                            isLand = true;
                        }

                        // -------------------------------------------------------------
                        // HIGH-LUMINANCE WEATHER & TEXTURE SHADER
                        // -------------------------------------------------------------
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);

                        // FIX 3: Boosted background global illumination.
                        // We use a high ambient minimum floor (0.34) and linear light scaling.
                        // This exposes dark zones while showing full crisp contrast on landmasses.
                        double finalLuminance = 0.34 + 0.66 * baseLight;

                        String palette = " .:-=+#%@█";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        if (isLand) {
                            if (latDeg < -62.0 || latDeg > 70.0) {
                                // Polar Caps: Ice White
                                outR = (int) (220 * finalLuminance);
                                outG = (int) (225 * finalLuminance);
                                outB = (int) (235 * finalLuminance);
                            } else {
                                // Dynamic Terrain Vegetation Greens
                                outR = (int) (35 * finalLuminance);
                                outG = (int) (155 * finalLuminance); // Saturated green for clarity
                                outB = (int) (45 * finalLuminance);
                            }
                        } else {
                            // Ocean Water: High-visibility Deep Sapphire Blue
                            outR = (int) (10 * finalLuminance);
                            outG = (int) (55 * finalLuminance);
                            outB = (int) (185 * finalLuminance);
                        }
                        // Rayleigh Scattering Atmosphere Halo Rim
                        double rimFactor = 1.0 - (nx*nx + nz*nz);
                        if (rimFactor > 0.75) {
                            double glow = (rimFactor - 0.75) / 0.25;
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