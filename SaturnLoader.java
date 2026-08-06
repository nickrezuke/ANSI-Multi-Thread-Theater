import java.util.Random;

public class SaturnLoader extends Loader {
    private static final StatusStage[] SATURN_STAGES = {
            new StatusStage(30, "Simulating planetary rotation axes:"),
            new StatusStage(65, "Extruding oblate ring vectors:"),
            new StatusStage(90, "Calibrating atmospheric gas bands:"),
            new StatusStage(100, "Saturn Orbital System Operational!")
    };

    private static final int MAX_STARS = 45;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];

    // Dynamic timeline variable driving the solar lighting phase angle change
    private double solarOrbitAngle = 0.0;

    // Authentic dusty butterscotch / ochre Saturnian base color palette
    private static final int SATURN_R = 215;
    private static final int SATURN_G = 190;
    private static final int SATURN_B = 145;

    public SaturnLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
    }

    public SaturnLoader() {
        super(SATURN_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.solarOrbitAngle = 0.0;

        // Procedurally generate a fixed random star field that skips text margins
        Random rand = new Random(5678);
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

        // Step 2: COMPUTE SYSTEM TIMELINE ANIMATION VALUES
        solarOrbitAngle += 0.008; // Drives our slow, majestic planetary system tilt rotation

        // Dynamic continuous system rotation matrices (creates a rolling camera wobble)
        double camTiltX = Math.toRadians(18.0) + 0.04 * Math.sin(solarOrbitAngle * 1.5);
        double camTiltZ = 0.03 * Math.cos(solarOrbitAngle * 2.0); // Subtle axial nod

        double cosTX = Math.cos(camTiltX), sinTX = Math.sin(camTiltX);
        double cosTZ = Math.cos(camTiltZ), sinTZ = Math.sin(camTiltZ);

        // Fixed directional sunlight vector coming from the top-front-right (NASA
        // standard composition)
        double lightX = 0.58;
        double lightY = -0.58;
        double lightZ = 0.58;

        double cameraDistance = 4.5;
        double baseRadius = 0.95;
        double flattenFactor = 0.88; // Authentic oblate flattening ratio

        // Step 3: RENDER THE RING SYSTEM WITH KEPLERIAN MOTION & LIGHT SHEEN
        for (double rRad = 1.35; rRad <= 2.35; rRad += 0.015) {
            // NASA Accuracy: Model both the Cassini Division (wide) and the outer Encke Gap
            // (narrow)
            if (rRad > 1.82 && rRad < 1.94)
                continue; // Cassini Division
            if (rRad > 2.18 && rRad < 2.22)
                continue; // Encke Gap

            // Kepler's Third Law: Inner ring particles orbit faster than outer particles!
            // We shift the particle's tracking angle over time based on 1.0 /
            // Math.pow(rRad, 1.5)
            double keplerSpeed = 0.08 / Math.pow(rRad, 1.5);
            double ringOrbitOffset = (currentTime * keplerSpeed) * 0.05;

            double stepSize = 0.005 / rRad;
            for (double phi = 0; phi < 2 * Math.PI; phi += stepSize) {
                // Apply our dynamic orbital rotation angle to the ring vertices
                double animatedPhi = phi + ringOrbitOffset;
                double cosPhi = Math.cos(animatedPhi);
                double sinPhi = Math.sin(animatedPhi);

                // Raw local flat coordinates of the ring slice
                double lx = rRad * cosPhi;
                double ly = rRad * sinPhi;
                double lz = 0.0;

                // Matrix Transformation: Apply system pitch and yaw tilt configuration
                double rx = lx * cosTZ - ly * sinTZ;
                double tmpY = lx * sinTZ + ly * cosTZ;
                double ry = tmpY * cosTX - lz * sinTX;
                double rz = tmpY * sinTX + lz * cosTX;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;

                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        // Ring Anisotropic Light Sheen calculation
                        double ringNormalX = 0.0, ringNormalY = -sinTX, ringNormalZ = cosTX;
                        double ringDot = Math.abs(ringNormalX * lightX + ringNormalY * lightY + ringNormalZ * lightZ);
                        double finalLuminance = 0.28 + 0.72 * ringDot;

                        // Density tracking palette strings
                        String ringPalette = " .:-=+*#█";
                        int shadeIndex = (int) (finalLuminance * (ringPalette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(ringPalette.length() - 1, shadeIndex));
                        char renderChar = ringPalette.charAt(shadeIndex);

                        // NASA Color grading: Core rings are dense beige, edge rings transition to icy
                        // silver-gray
                        double radialGradient = (rRad - 1.35) / 1.0; // 0 at inner, 1 at outer
                        int ringR = (int) ((SATURN_R * (1.0 - radialGradient * 0.2)) * finalLuminance);
                        int ringG = (int) ((SATURN_G * (1.0 - radialGradient * 0.1)) * finalLuminance);
                        int ringB = (int) ((SATURN_B * (1.0 + radialGradient * 0.3)) * finalLuminance);

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", ringR, ringG, ringB);
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }

        // Step 4: RENDER THE HIGH-ACCURACY OBLATE GLOBE
        for (double theta = 0; theta < Math.PI; theta += 0.015) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.015) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                // Base sphere positions
                double lx = baseRadius * sinTheta * cosPhi;
                double ly = baseRadius * sinTheta * sinPhi;
                double lz = baseRadius * cosTheta * flattenFactor;

                // Matrix Transformation: Apply the exact same structural camera tilt to the
                // planet
                double rx = lx * cosTZ - ly * sinTZ;
                double tmpY = lx * sinTZ + ly * cosTZ;
                double ry = tmpY * cosTX - lz * sinTX;
                double rz = tmpY * sinTX + lz * cosTX;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;

                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        // Calculate spatial normal profiles
                        double nx = rx / baseRadius;
                        double ny = ry / baseRadius;
                        double nz = rz / (baseRadius * flattenFactor * flattenFactor);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (nLen > 0) {
                            nx /= nLen;
                            ny /= nLen;
                            nz /= nLen;
                        }

                        // -------------------------------------------------------------
                        // NASA CALIBRATED ATMOSPHERIC BAND ENGINE
                        // -------------------------------------------------------------
                        // Calculate latitude position. If near the north pole, inject a 6-fold hexagon
                        // wave!
                        double hexWave = 1.0;
                        if (lz > 0.65) {
                            // deforms northern bands into the famous geometric polar storm
                            hexWave += 0.06 * Math.cos(phi * 6.0);
                        }

                        double bandNoise = Math.sin(lz * 15.0 * hexWave) * 0.35
                                + Math.sin(lz * 6.0) * 0.25;
                        double bandMultiplier = 1.0 + 0.15 * bandNoise;

                        // -------------------------------------------------------------
                        // HIGH-FIDELITY PLANETARY RING SHADOW PROJECTION
                        // -------------------------------------------------------------
                        // Trace a ray back toward the light source to see if the rings block the sun.
                        // We project out from the current point along our directional light rays:
                        // vector = point + t * light
                        double shadowFactor = 1.0;
                        for (double t = 0.1; t < 1.8; t += 0.08) {
                            double rayX = rx + t * lightX;
                            double rayY = ry + t * lightY;
                            double rayZ = rz + t * lightZ;

                            // Convert the ray position back out of camera-tilt space to find its flat ring
                            // radius
                            // Inverse rotation around X-axis
                            double unRotY = rayY * cosTX + rayZ * sinTX;
                            double unRotZ = -rayY * sinTX + rayZ * cosTX;
                            // Inverse rotation around Z-axis
                            double unRotX = rayX * cosTZ + unRotY * sinTZ;
                            double finalRingY = -rayX * sinTZ + unRotY * cosTZ;

                            // Measure the flat radial distance. If it falls inside the rings, shadow blocks
                            // the point!
                            double rayRadius = Math.sqrt(unRotX * unRotX + finalRingY * finalRingY);
                            if (rayRadius >= 1.35 && rayRadius <= 2.35 && Math.abs(unRotZ) < 0.06) {
                                // Cassini division lets light leak through, preventing flat shadow lines
                                if (!(rayRadius > 1.82 && rayRadius < 1.94)) {
                                    shadowFactor = 0.18; // Drop lighting by 82% to map a dense, dark shadow trench
                                    break;
                                }
                            }
                        }

                        // Combine standard Lambertian diffuse light with our structural shadow mask
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse) * shadowFactor;
                        double highContrastLight = Math.pow(baseLight, 1.5);
                        double finalLuminance = 0.03 + 0.97 * (highContrastLight * bandMultiplier);
                        String globePalette = " .:-=+#%@█";
                        int shadeIndex = (int) (finalLuminance * (globePalette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(globePalette.length() - 1, shadeIndex));
                        char renderChar = globePalette.charAt(shadeIndex);
                        // Scale RGB vectors safely using our new lighting and band mod coefficients
                        int outR = (int) (SATURN_R * finalLuminance * bandMultiplier);
                        int outG = (int) (SATURN_G * finalLuminance * bandMultiplier);
                        // NASA detail: Saturn's north/south polar vortex caps are noticeably cooler,
                        // blue-green gray rock
                        double polarCooling = (lz > 0.58 || lz < -0.58) ? 1.22 : 0.88;
                        int outB = (int) (SATURN_B * finalLuminance * bandMultiplier * polarCooling);
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