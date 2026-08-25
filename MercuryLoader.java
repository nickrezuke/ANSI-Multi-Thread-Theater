import java.util.Random;

public class MercuryLoader extends Loader {
    private static final StatusStage[] MERCURY_STAGES = {
            new StatusStage(30, "Mapping ancient impact crater basins:"),
            new StatusStage(65, "Caloris Basin topography calibration:"),
            new StatusStage(90, "Analyzing extreme solar thermal gradients:"),
            new StatusStage(100, "Hermean Topographic Matrix Active!")
    };

    private static final int MAX_STARS = 35;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double mercuryRotationAngle = 0.0;

    // Scientifically accurate Hermean regolith palette registers (Charcoal Shadow, Mid Gray, Bright Highlands, Crater Rays)
    private static final int[] C_DEEP_SHADOW     = { 60, 60, 65   }; // Dark unlit or shadowed crater floors
    private static final int[] C_MID_REGOLITH    = { 125, 125, 130 }; // Standard heavily cratered plains
    private static final int[] C_BRIGHT_HIGHLAND = { 185, 185, 190 }; // Rough, high-albedo crater rim uplands
    private static final int[] C_CRATER_RAY      = { 235, 235, 240 }; // Fresh impact rays and bright ejecta blankets

    public MercuryLoader() {
        super(MERCURY_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.mercuryRotationAngle = 0.0;
        Random rand = new Random(3333);
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
        // Mercury has a slow prograde rotation synchronized with its 3:2 spin-orbit resonance
        mercuryRotationAngle += 0.001;

        // Mercury's axial tilt is virtually zero (approx 0.03 degrees)
        double axialTilt = Math.toRadians(0.03);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // Overhead directional spotlight vectors (illuminating from upper right)
        double lightX = 0.60, lightY = -0.70, lightZ = 0.38;
        double cameraDistance = 3.85;
        double sphereRadius = 1.0;
        double flattenFactor = 1.0; 

        // -------------------------------------------------------------
        // Step 3: Render Heavily Cratered Hermean Terrain Spheroid Globe
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

                        // Calculate surface coordinates for procedural cratered terrain
                        double currentLon = Math.atan2(uy, ux) + mercuryRotationAngle;
                        
                        // Simulate high-density impact craters using intersecting trigonometric frequency waves
                        double craterNoise = Math.sin(latDeg * 0.3) * Math.cos(currentLon * 3.5)
                                           + 0.5 * Math.sin(latDeg * 0.7 - currentLon * 6.0)
                                           + 0.25 * Math.cos(latDeg * 1.2 * currentLon * 2.0);
                        
                        double terrainMix = Math.abs(Math.sin(craterNoise * Math.PI));
                        terrainMix = Math.max(0.0, Math.min(1.0, terrainMix));

                        // Lighting and shading calculation (Mercury has no atmosphere, creating crisp shadows)
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);
                        double finalLuminance = Math.pow(baseLight, 0.95);

                        String palette = " .:-=+#%@";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        // Color selection based on Hermean regolith composition
                        if (terrainMix < 0.3) {
                            double t = terrainMix / 0.3;
                            outR = (int) (C_DEEP_SHADOW[0] * (1.0 - t) + C_MID_REGOLITH[0] * t);
                            outG = (int) (C_DEEP_SHADOW[1] * (1.0 - t) + C_MID_REGOLITH[1] * t);
                            outB = (int) (C_DEEP_SHADOW[2] * (1.0 - t) + C_MID_REGOLITH[2] * t);
                        } else if (terrainMix < 0.75) {
                            double t = (terrainMix - 0.3) / 0.45;
                            outR = (int) (C_MID_REGOLITH[0] * (1.0 - t) + C_BRIGHT_HIGHLAND[0] * t);
                            outG = (int) (C_MID_REGOLITH[1] * (1.0 - t) + C_BRIGHT_HIGHLAND[1] * t);
                            outB = (int) (C_MID_REGOLITH[2] * (1.0 - t) + C_BRIGHT_HIGHLAND[2] * t);
                        } else {
                            double t = (terrainMix - 0.75) / 0.25;
                            outR = (int) (C_BRIGHT_HIGHLAND[0] * (1.0 - t) + C_CRATER_RAY[0] * t);
                            outG = (int) (C_BRIGHT_HIGHLAND[1] * (1.0 - t) + C_CRATER_RAY[1] * t);
                            outB = (int) (C_BRIGHT_HIGHLAND[2] * (1.0 - t) + C_CRATER_RAY[2] * t);
                        }

                        // Apply harsh direct sunlight lighting without atmospheric diffusion rims
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