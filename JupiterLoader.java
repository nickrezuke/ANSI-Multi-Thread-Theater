// TODO: Fix the storm spot.  Its not accurate

import java.util.Random;

public class JupiterLoader extends Loader {
    private static final StatusStage[] JUPITER_STAGES = {
            new StatusStage(30, "Calibrating gas giant mass profiles:"),
            new StatusStage(65, "Streaming counter-rotating fluid belts:"),
            new StatusStage(90, "Simulating the Great Red Spot vortex:"),
            new StatusStage(100, "Jupiter Atmospheric Matrix Active!")
    };

    private static final int MAX_STARS = 45;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];

    // Dynamic timeline variable driving our continuous planetary spin
    private double jupiterRotationAngle = 0.0;

    // Authentic warm Jupiter cream and deep brick-red base color palette
    private static final int JUPITER_CREAM_R = 225;
    private static final int JUPITER_CREAM_G = 200;
    private static final int JUPITER_CREAM_B = 170;

    private static final int JUPITER_RED_R = 185;
    private static final int JUPITER_RED_G = 85;
    private static final int JUPITER_RED_B = 55;

    public JupiterLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
    }

    public JupiterLoader() {
        super(JUPITER_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.jupiterRotationAngle = 0.0;

        // Procedurally generate a fixed random star field that skips text margins
        Random rand = new Random(1111);
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

        // Step 2: COMPUTE GLOBAL SUNLIGHT AND AXIAL MATRICES
        jupiterRotationAngle += 0.004; // Cinematic slow spin rate
        jupiterRotationAngle %= (2.0 * Math.PI);

        // Jupiter has a tiny axial tilt (only 3.1 degrees), making its bands nearly
        // perfectly horizontal
        double axialTilt = Math.toRadians(3.1);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // High-visibility directional light source targeting the near face
        double lightX = 0.45;
        double lightY = -0.80;
        double lightZ = 0.40;

        double cameraDistance = 2.6;
        double sphereRadius = 1.0;
        double flattenFactor = 0.935; // Authentic oblate flattening ratio for Jupiter

        // Step 3: Render the Texturized 3D Jupiter Spheroid
        for (double theta = 0.01; theta < Math.PI; theta += 0.015) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            // Compute exact relative latitude in degrees (-90 at South Pole to +90 at North
            // Pole)
            double latDeg = Math.toDegrees((Math.PI / 2.0) - theta);

            // DYNAMIC COUNTER-ROTATING JET STREAM LINES:
            // Different latitudes on Jupiter spin at different rates and directions!
            // We set up alternating bands based on sine-wave latitude slots
            double bandDirection = (Math.sin(latDeg * 0.18) > 0) ? 1.0 : -1.0;
            double localizedSpin = jupiterRotationAngle * (1.0 + 0.5 * Math.sin(latDeg * 0.12)) * bandDirection;

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.015) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                // Convert spherical steps to raw oblate space coordinates
                double lx = sphereRadius * sinTheta * cosPhi;
                double ly = sphereRadius * sinTheta * sinPhi;
                double lz = sphereRadius * cosTheta * flattenFactor; // Squashed vertically

                // Extract exact mathematical Longitude Angle (-PI to +PI)
                double rawLong = Math.atan2(sinPhi, cosPhi);

                // Inject the dynamic counter-rotating jet longitude variable
                double animatedLong = rawLong - localizedSpin;
                if (animatedLong < -Math.PI)
                    animatedLong += 2.0 * Math.PI;
                if (animatedLong > Math.PI)
                    animatedLong -= 2.0 * Math.PI;
                double lonDeg = Math.toDegrees(animatedLong);

                // Apply the minor 3.1-degree tilt rotation matrix (Roll around depth axis)
                double rx = lx * cosTilt - lz * sinTilt;
                double ry = ly;
                double rz = lx * sinTilt + lz * cosTilt;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz);

                // Filter points facing the camera lens (ry < 0)
                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22 && ry < 0) {
                    int index = xp + 80 * yp;

                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        double nx = rx / sphereRadius;
                        double ny = ry / sphereRadius;
                        double nz = rz / (sphereRadius * flattenFactor * flattenFactor);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (nLen > 0) {
                            nx /= nLen;
                            ny /= nLen;
                            nz /= nLen;
                        }

                        // -------------------------------------------------------------
                        // NASA CALIBRATED ATMOSPHERIC VORTEX MATRIX (The Great Red Spot)
                        // -------------------------------------------------------------
                        // The Great Red Spot is permanently located at 22 degrees South Latitude.
                        // We track an ellipsoidal distance bounding box around this location.
                        double spotLatCenter = -22.0;
                        double spotLonCenter = 45.0; // Fixed local texture landing zone

                        // Measure elliptical distance to the center of the storm vortex
                        double dLat = latDeg - spotLatCenter;
                        double dLon = lonDeg - spotLonCenter;
                        // Handle texture wrap boundaries seamlessly for longitudes
                        if (dLon > 180.0)
                            dLon -= 360.0;
                        if (dLon < -180.0)
                            dLon += 360.0;

                        // Ellipse formula: (dLon / radiusLon)^2 + (dLat / radiusLat)^2
                        // The Red Spot is roughly twice as wide as it is tall
                        double spotVortex = Math.pow(dLon / 18.0, 2) + Math.pow(dLat / 9.0, 2);

                        // -------------------------------------------------------------
                        // HIGH-CONTRAST ATMOSPHERIC SHADER
                        // -------------------------------------------------------------
                        // Generate rich horizontal gas band turbulence waves
                        double bandNoise = Math.sin(latDeg * 0.35) * 0.4
                                + Math.sin(latDeg * 0.12) * 0.3
                                + 0.1 * Math.sin(lonDeg * 0.08) * Math.cos(latDeg * 0.2); // Soft whorl texture

                        // Map the structural band weight factor [0.0 to 1.0]
                        double bandWeight = 0.5 + 0.5 * bandNoise;

                        // Base illumination calculations
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);
                        double finalLuminance = 0.32 + 0.68 * Math.pow(baseLight, 1.4); // High ambient floor

                        String palette = " .:-=+#&%@";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        // Color Interpolation Blending Block
                        int outR, outG, outB;

                        if (spotVortex < 1.0) {
                            // INSIDE THE GREAT RED SPOT VORTEX: Bright Crimson-Brick Orange Core
                            double edgeFade = 1.0 - spotVortex; // 1 at center, 0 at edge boundary

                            // Blend from a deep dark perimeter trench to a vivid crimson center peak
                            outR = (int) ((JUPITER_RED_R * 1.2 * edgeFade + JUPITER_RED_R * 0.7 * (1.0 - edgeFade))
                                    * finalLuminance);
                            outG = (int) ((JUPITER_RED_G * 0.6 * edgeFade + JUPITER_RED_G * 0.8 * (1.0 - edgeFade))
                                    * finalLuminance);
                            outB = (int) ((JUPITER_RED_B * 0.5 * edgeFade + JUPITER_RED_B * 0.9 * (1.0 - edgeFade))
                                    * finalLuminance);
                        } else if (spotVortex < 1.45) {
                            // THE STORM TURBULENCE ZONE: Distorted, swirly white/cream wake surrounding the
                            // storm
                            double blend = (spotVortex - 1.0) / 0.45;
                            outR = (int) ((240 * (1.0 - blend) + JUPITER_CREAM_R * blend) * finalLuminance);
                            outG = (int) ((220 * (1.0 - blend) + JUPITER_CREAM_G * blend) * finalLuminance);
                            outB = (int) ((190 * (1.0 - blend) + JUPITER_CREAM_B * blend) * finalLuminance);
                        } else {// STANDARD PLANETARY BELTS: Blend between bright cream zones and dark
                                // orange-red belts
                            outR = (int) ((JUPITER_CREAM_R * (1.0 - bandWeight) + JUPITER_RED_R * 1.05 * bandWeight)
                                    * finalLuminance);
                            outG = (int) ((JUPITER_CREAM_G * (1.0 - bandWeight) + JUPITER_RED_G * 0.85 * bandWeight)
                                    * finalLuminance);
                            outB = (int) ((JUPITER_CREAM_B * (1.0 - bandWeight) + JUPITER_RED_B * 0.75 * bandWeight)
                                    * finalLuminance);
                        } // Apply subtle cooling filters near polar vortex tips (Muted blue-gray rock
                          // hue)
                        if (latDeg > 60.0 || latDeg < -60.0) {
                            double poleWeight = (Math.abs(latDeg) - 60.0) / 30.0; // 0 to 1 at pole tip
                            outR = (int) (outR * (1.0 - poleWeight * 0.25));
                            outG = (int) (outG * (1.0 - poleWeight * 0.10));
                            outB = (int) (outB * (1.0 + poleWeight * 0.15)); // Add soft blue polar mist
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