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

    private double jupiterRotationAngle = Math.PI;

    // Authentic RGB Palette for Jupiter's Belts, Zones, and Storm Core
    private static final int[] C_ZONE_CREAM  = {235, 215, 185};
    private static final int[] C_BELT_BROWN  = {165, 95,  55};
    private static final int[] C_GRS_CORE    = {215, 45,  25};
    private static final int[] C_GRS_HALO    = {245, 230, 210};
    private static final int[] C_POLAR_BLUE  = {110, 125, 145};

    public JupiterLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
    }

    public JupiterLoader() {
        super(JUPITER_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.jupiterRotationAngle = Math.PI;

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
        // 1. Draw Background Stars
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            int starIdx = starPositions[i];
            double twinkleFactor = Math.sin((currentTime * 0.004) + starPhases[i]);

            char starChar = ' ';
            if (twinkleFactor > 0.82) starChar = '*';
            else if (twinkleFactor > 0.20) starChar = '.';
            else if (twinkleFactor > -0.3) starChar = '·';

            if (starChar != ' ' && starIdx >= 0 && starIdx < 1760) {
                zBuffer[starIdx] = 0.0001;
                outputBuffer[starIdx] = "\u001B[37m" + starChar + RESET;
            }
        }

        // 2. Global Planetary Kinematics
        jupiterRotationAngle += 0.005;
        jupiterRotationAngle %= (2.0 * Math.PI);

        double axialTilt = Math.toRadians(3.1);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        double lightX = 0.50, lightY = -0.75, lightZ = 0.42;
        double cameraDistance = 2.6;
        double sphereRadius = 1.0;
        double flattenFactor = 0.935; // Oblate vertical squashing

        // Pre-compute 3D Location Vector of Great Red Spot Center (at 22° S Latitude)
        double spotLatRad = Math.toRadians(-22.0);
        double spotLonRad = Math.toRadians(30.0) + jupiterRotationAngle; // Moves rigidly with rotation
        
        double spotCenterUnboundX = Math.cos(spotLatRad) * Math.cos(spotLonRad);
        double spotCenterUnboundY = Math.cos(spotLatRad) * Math.sin(spotLonRad);
        double spotCenterUnboundZ = Math.sin(spotLatRad);

        // 3. Dense 3D Surface Grid Sampling
        for (double theta = 0.008; theta < Math.PI; theta += 0.008) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);
            double latDeg = Math.toDegrees((Math.PI / 2.0) - theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.008) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                // Unrotated Unit Sphere Coordinates (for storm tracking)
                double ux = sinTheta * cosPhi;
                double uy = sinTheta * sinPhi;
                double uz = cosTheta;

                // Scaled Oblate Coordinates
                double lx = sphereRadius * ux;
                double ly = sphereRadius * uy;
                double lz = sphereRadius * uz * flattenFactor;

                // Calculate local jet stream longitude drift for belt turbulence
                double rigidLong = Math.atan2(uy, ux) - jupiterRotationAngle;
                double jetSpeed = Math.sin(latDeg * 0.22) * 0.45;
                double animatedLong = normalizeAngleRad(rigidLong - jetSpeed);
                double lonDeg = Math.toDegrees(animatedLong);

                // Apply axial tilt
                double rx = lx * cosTilt - lz * sinTilt;
                double ry = ly;
                double rz = lx * sinTilt + lz * cosTilt;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz);

                // Filter for camera facing points
                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22 && ry < 0) {
                    int index = xp + 80 * yp;

                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        // Surface Normals for Lighting
                        double nx = rx / sphereRadius;
                        double ny = ry / sphereRadius;
                        double nz = rz / (sphereRadius * flattenFactor * flattenFactor);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (nLen > 0) { nx /= nLen; ny /= nLen; nz /= nLen; }

                        // -------------------------------------------------------------
                        // 3D GREAT-CIRCLE DISTANCE METRIC (Prevents Distortion Near Limbs)
                        // -------------------------------------------------------------
                        // Dot product gives spherical angular distance from storm center
                        double dotSpot = ux * spotCenterUnboundX + uy * spotCenterUnboundY + uz * spotCenterUnboundZ;
                        dotSpot = Math.max(-1.0, Math.min(1.0, dotSpot));

                        // Project onto local tangent coordinate plane to measure 2:1 elliptical aspect ratio
                        double dLatRad = Math.toRadians(latDeg - (-22.0));
                        double dLonRad = Math.atan2(uy, ux) - spotLonRad;
                        dLonRad = normalizeAngleRad(dLonRad);

                        // Elliptical storm distance (1.0 = storm boundary)
                        double scaledLon = (dLonRad * Math.cos(spotLatRad)) / Math.toRadians(15.0); // ~15 deg width
                        double scaledLat = dLatRad / Math.toRadians(7.5);                           // ~7.5 deg height
                        
                        double spotVortex = scaledLon * scaledLon + scaledLat * scaledLat;

                        // -------------------------------------------------------------
                        // SHADING & COLOR PIPELINE
                        // -------------------------------------------------------------
                        double bandNoise = Math.sin(latDeg * 0.28) * 0.5
                                + 0.25 * Math.sin(latDeg * 0.65 + Math.sin(lonDeg * 0.08))
                                + 0.12 * Math.sin(lonDeg * 0.15) * Math.cos(latDeg * 0.3);

                        double bandWeight = Math.min(1.0, Math.max(0.0, 0.5 + 0.5 * bandNoise));

                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);

                        // Limb Darkening
                        double viewAngle = Math.abs(ny);
                        double limbDarkening = 0.6 + 0.4 * Math.pow(viewAngle, 0.7);

                        double finalLuminance = (0.28 + 0.72 * Math.pow(baseLight, 1.3)) * limbDarkening;

                        String palette = " .:-=+#%@";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        // Anti-aliased storm boundary interpolation
                        if (spotVortex < 1.0) {
                            // Core
                            double coreFade = 1.0 - spotVortex;
                            outR = (int) (C_GRS_CORE[0] * (0.85 + 0.3 * coreFade));
                            outG = (int) (C_GRS_CORE[1] * (0.7 + 0.3 * (1.0 - coreFade)));
                            outB = (int) (C_GRS_CORE[2] * (0.6 + 0.4 * (1.0 - coreFade)));
                        } else if (spotVortex < 1.4) {
                            // Eyewall & Soft Edge Transition
                            double blend = (spotVortex - 1.0) / 0.4;
                            // Smoothstep curve for anti-aliased edges
                            blend = blend * blend * (3.0 - 2.0 * blend);

                            int baseR = (int) (C_ZONE_CREAM[0] * (1.0 - bandWeight) + C_BELT_BROWN[0] * bandWeight);
                            int baseG = (int) (C_ZONE_CREAM[1] * (1.0 - bandWeight) + C_BELT_BROWN[1] * bandWeight);
                            int baseB = (int) (C_ZONE_CREAM[2] * (1.0 - bandWeight) + C_BELT_BROWN[2] * bandWeight);

                            outR = (int) (C_GRS_HALO[0] * (1.0 - blend) + baseR * blend);
                            outG = (int) (C_GRS_HALO[1] * (1.0 - blend) + baseG * blend);
                            outB = (int) (C_GRS_HALO[2] * (1.0 - blend) + baseB * blend);
                        } else {
                            // Belts and Zones
                            outR = (int) (C_ZONE_CREAM[0] * (1.0 - bandWeight) + C_BELT_BROWN[0] * bandWeight);
                            outG = (int) (C_ZONE_CREAM[1] * (1.0 - bandWeight) + C_BELT_BROWN[1] * bandWeight);
                            outB = (int) (C_ZONE_CREAM[2] * (1.0 - bandWeight) + C_BELT_BROWN[2] * bandWeight);
                        }

                        // Polar Caps
                        if (Math.abs(latDeg) > 55.0) {
                            double poleWeight = Math.min(1.0, (Math.abs(latDeg) - 55.0) / 30.0);
                            outR = (int) (outR * (1.0 - poleWeight) + C_POLAR_BLUE[0] * poleWeight);
                            outG = (int) (outG * (1.0 - poleWeight) + C_POLAR_BLUE[1] * poleWeight);
                            outB = (int) (outB * (1.0 - poleWeight) + C_POLAR_BLUE[2] * poleWeight);
                        }

                        outR = Math.max(0, Math.min(255, (int) (outR * finalLuminance)));
                        outG = Math.max(0, Math.min(255, (int) (outG * finalLuminance)));
                        outB = Math.max(0, Math.min(255, (int) (outB * finalLuminance)));

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", outR, outG, outB);
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }
    }

    private double normalizeAngleRad(double angle) {
        while (angle < -Math.PI) angle += 2.0 * Math.PI;
        while (angle > Math.PI)  angle -= 2.0 * Math.PI;
        return angle;
    }
}