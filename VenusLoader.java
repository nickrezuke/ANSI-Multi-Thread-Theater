// TODO: Venus is a little boring, anything we can do?

import java.util.Random;

public class VenusLoader extends Loader {
    private static final StatusStage[] VENUS_STAGES = {
            new StatusStage(30, "Synthesizing thick sulfuric acid cloud layers:"),
            new StatusStage(65, "Projecting high-altitude V-shaped wind bands:"),
            new StatusStage(90, "Calibrating runaway greenhouse thermal emissions:"),
            new StatusStage(100, "Venusian Atmospheric Matrix Active!")
    };

    private static final int MAX_STARS = 35;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double venusRotationAngle = 0.0;

    // Scientifically accurate sulfuric cloud palette registers (Cream, Warm Yellow, Pale Orange)
    private static final int[] C_BRIGHT_CREAM  = { 245, 235, 210 }; // High-altitude bright haze
    private static final int[] C_WARM_YELLOW   = { 225, 190, 115 }; // Dominant cloud deck
    private static final int[] C_PALE_ORANGE   = { 185, 135, 70  }; // Darker lower cloud bands/chevrons
    private static final int[] C_LIMB_GLOW     = { 255, 245, 230 }; // Atmospheric limb radiance

    public VenusLoader() {
        super(VENUS_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.venusRotationAngle = 0.0;
        Random rand = new Random(2222);
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
        // Venus has a slow retrograde rotation, represented here by a negative increment
        venusRotationAngle -= 0.003;

        // Venus axial tilt is nearly upright (approx 3.8 degrees)
        double axialTilt = Math.toRadians(3.8);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // Overhead directional spotlight vectors (illuminating from upper right)
        double lightX = 0.60, lightY = -0.70, lightZ = 0.38;
        double cameraDistance = 3.85;
        double sphereRadius = 1.0;
        double flattenFactor = 1.0; // Venus is an almost perfect sphere

        // -------------------------------------------------------------
        // Step 3: Render Uniform Sulfuric Cloud Spheroid Globe
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

                        // Calculate surface coordinates for procedural cloud patterns
                        double currentLon = Math.atan2(uy, ux) + venusRotationAngle;
                        
                        // Venus super-rotation creates distinct V-shaped chevron patterns pointing eastward
                        double chevronPattern = Math.sin(latDeg * 0.08 + currentLon * 3.0) * Math.cos(currentLon * 1.5);
                        double atmosphericBands = Math.sin(latDeg * 0.12) * 0.5 + 0.5;
                        
                        // Combined cloud density factor
                        double cloudMix = 0.6 * atmosphericBands + 0.4 * (chevronPattern * 0.5 + 0.5);

                        // Lighting and atmospheric scattering calculation
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);
                        double viewAngle = Math.abs(ny);
                        
                        // Venus has a very dense atmosphere causing strong limb brightening/haze
                        double limbScattering = 0.55 + 0.45 * Math.pow(viewAngle, 0.6);
                        double finalLuminance = (0.25 + 0.75 * Math.pow(baseLight, 1.15)) * limbScattering;

                        String palette = " .:-=+#%@";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        // Blend between pale orange, warm yellow, and bright cream based on cloud dynamics
                        if (cloudMix < 0.35) {
                            double t = cloudMix / 0.35;
                            outR = (int) (C_PALE_ORANGE[0] * (1.0 - t) + C_WARM_YELLOW[0] * t);
                            outG = (int) (C_PALE_ORANGE[1] * (1.0 - t) + C_WARM_YELLOW[1] * t);
                            outB = (int) (C_PALE_ORANGE[2] * (1.0 - t) + C_WARM_YELLOW[2] * t);
                        } else {
                            double t = (cloudMix - 0.35) / 0.65;
                            outR = (int) (C_WARM_YELLOW[0] * (1.0 - t) + C_BRIGHT_CREAM[0] * t);
                            outG = (int) (C_WARM_YELLOW[1] * (1.0 - t) + C_BRIGHT_CREAM[1] * t);
                            outB = (int) (C_WARM_YELLOW[2] * (1.0 - t) + C_BRIGHT_CREAM[2] * t);
                        }

                        // Enhance brightness near the atmospheric rim to mimic thick cloud scattering
                        if (viewAngle < 0.35) {
                            double rimFactor = (0.35 - viewAngle) / 0.35;
                            outR = (int) (outR * (1.0 - rimFactor) + C_LIMB_GLOW[0] * rimFactor);
                            outG = (int) (outG * (1.0 - rimFactor) + C_LIMB_GLOW[1] * rimFactor);
                            outB = (int) (outB * (1.0 - rimFactor) + C_LIMB_GLOW[2] * rimFactor);
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