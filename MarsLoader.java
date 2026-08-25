// TODO: Mars is looking boring... maybe add the two moons??

import java.util.Random;

public class MarsLoader extends Loader {
    private static final StatusStage[] MARS_STAGES = {
            new StatusStage(30, "Mapping iron oxide-rich surface regolith:"),
            new StatusStage(65, "Calibrating Tharsis volcanic plateau elevations:"),
            new StatusStage(90, "Detecting polar carbon dioxide ice caps:"),
            new StatusStage(100, "Martian Topographic Matrix Active!")
    };

    private static final int MAX_STARS = 35;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double marsRotationAngle = 0.0;

    // Scientifically accurate Martian palette registers (Rust Red, Dark Basalt, Bright Dust, Polar Ice)
    private static final int[] C_RUST_RED       = { 190, 75, 45  }; // Primary Martian desert regolith
    private static final int[] C_DARK_BASALT    = { 105, 40, 25  }; // Dark volcanic albedo features (e.g., Syrtis Major)
    private static final int[] C_BRIGHT_DUST    = { 225, 140, 85 }; // Bright highlands & dust storm zones
    private static final int[] C_POLAR_ICE      = { 240, 240, 245 }; // Polar ice cap (water/CO2 frost)
    private static final int[] C_ATMOSPHERE_GLOW = { 210, 110, 70 }; // Thin dusty atmospheric limb radiance

    public MarsLoader() {
        super(MARS_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.marsRotationAngle = 0.0;
        Random rand = new Random(4321);
        for (int i = 0; i < MAX_STARS; i++) {
            int rx = rand.nextInt(80);
            int ry = 1 + rand.nextInt(20);
            starPositions[i] = ry * 80 + rx;
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Draw Deep Space Background Starfield
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            int starIdx = starPositions[i];
            double twinkleFactor = Math.sin((currentTime * 0.003) + starPhases[i]);
            char starChar = ' ';
            if (twinkleFactor > 0.85) starChar = '*';
            else if (twinkleFactor > 0.30) starChar = '.';
            else if (twinkleFactor > -0.2) starChar = '·';

            if (starChar != ' ' && starIdx >= 0 && starIdx < 1760) {
                zBuffer[starIdx] = 0.0001;
                outputBuffer[starIdx] = "\u001B[37m" + starChar + RESET;
            }
        }

        // Step 2: Global Planetary Kinematics
        // Mars has a prograde rotation (like Earth), represented by a positive increment
        marsRotationAngle += 0.002;

        // Mars axial tilt is very close to Earth's at approximately 25.2 degrees
        double axialTilt = Math.toRadians(25.2);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // Overhead directional spotlight vectors (illuminating from upper right)
        double lightX = 0.60, lightY = -0.70, lightZ = 0.38;
        double cameraDistance = 3.85;
        double sphereRadius = 1.0;
        double flattenFactor = 1.0; 

        // -------------------------------------------------------------
        // Step 3: Render Procedural Martian Terrain Spheroid Globe
        // -------------------------------------------------------------
        for (double theta = 0.008; theta < Math.PI; theta += 0.008) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);
            double latDeg = Math.toDegrees((Math.PI / 2.0) - theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.008) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double ux = sinTheta * cosPhi;
                double uy = sinTheta * sinPhi;
                double uz = cosTheta;

                double lx = sphereRadius * ux;
                double ly = sphereRadius * uy;
                double lz = sphereRadius * uz * flattenFactor;

                // Apply axial tilt
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
                        double nz = rz / (sphereRadius * flattenFactor * flattenFactor);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (nLen > 0) {
                            nx /= nLen; ny /= nLen; nz /= nLen;
                        }

                        // Calculate surface coordinates for procedural Martian terrain features
                        double currentLon = Math.atan2(uy, ux) + marsRotationAngle;
                        
                        // Check for polar ice caps at high latitudes
                        boolean isPolar = Math.abs(latDeg) > 68.0;

                        // Procedural albedo simulation for Martian dark basalt patches and dust plains
                        double terrainPattern = Math.sin(latDeg * 0.07) * Math.cos(currentLon * 2.5) 
                                              + 0.5 * Math.sin(currentLon * 5.0 - latDeg * 0.03) 
                                              + 0.3 * Math.cos(latDeg * 0.2);
                        double terrainMix = (terrainPattern + 1.5) / 3.0;
                        terrainMix = Math.max(0.0, Math.min(1.0, terrainMix));

                        // Lighting and shading calculation
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);
                        double viewAngle = Math.abs(ny);
                        
                        double finalLuminance = 0.2 + 0.8 * Math.pow(baseLight, 1.2);

                        String palette = " .:-=+#%@";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        // Color selection based on geography (Ice caps vs Terrain mix)
                        if (isPolar) {
                            outR = C_POLAR_ICE[0];
                            outG = C_POLAR_ICE[1];
                            outB = C_POLAR_ICE[2];
                        } else {
                            if (terrainMix < 0.4) {
                                double t = terrainMix / 0.4;
                                outR = (int) (C_DARK_BASALT[0] * (1.0 - t) + C_RUST_RED[0] * t);
                                outG = (int) (C_DARK_BASALT[1] * (1.0 - t) + C_RUST_RED[1] * t);
                                outB = (int) (C_DARK_BASALT[2] * (1.0 - t) + C_RUST_RED[2] * t);
                            } else {
                                double t = (terrainMix - 0.4) / 0.6;
                                outR = (int) (C_RUST_RED[0] * (1.0 - t) + C_BRIGHT_DUST[0] * t);
                                outG = (int) (C_RUST_RED[1] * (1.0 - t) + C_BRIGHT_DUST[1] * t);
                                outB = (int) (C_RUST_RED[2] * (1.0 - t) + C_BRIGHT_DUST[2] * t);
                            }
                        }

                        // Add subtle dusty atmospheric rim haze
                        if (viewAngle < 0.25 && !isPolar) {
                            double rimFactor = (0.25 - viewAngle) / 0.25;
                            outR = (int) (outR * (1.0 - rimFactor * 0.4) + C_ATMOSPHERE_GLOW[0] * (rimFactor * 0.4));
                            outG = (int) (outG * (1.0 - rimFactor * 0.4) + C_ATMOSPHERE_GLOW[1] * (rimFactor * 0.4));
                            outB = (int) (outB * (1.0 - rimFactor * 0.4) + C_ATMOSPHERE_GLOW[2] * (rimFactor * 0.4));
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
}