//TODO: make the ring more accurate??

import java.util.Random;

public class UranusLoader extends Loader {
    private static final StatusStage[] URANUS_STAGES = {
        new StatusStage(30, "Simulating extreme 98-degree axial tilt:"),
        new StatusStage(65, "Extruding vertical charcoal ring ribbons:"),
        new StatusStage(90, "Interpolating methane absorption profiles:"),
        new StatusStage(100, "Uranus Orbital System Operational!")
    };

    private static final int MAX_STARS = 45;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double solarOrbitAngle = 0.0;

    // Authentic high-saturation pale Cyan/Aquamarine gas palette registers
    private static final int URANUS_R = 155;
    private static final int URANUS_G = 225;
    private static final int URANUS_B = 220;

    public UranusLoader() {
        super(URANUS_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.solarOrbitAngle = 0.0;
        Random rand = new Random(5678);
        for (int i = 0; i < MAX_STARS; i++) {
            int rx = rand.nextInt(80);
            int ry = 1 + rand.nextInt(20);
            starPositions[i] = ry * 80 + rx;
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Draw Twinkling Background Starfield
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

        // Step 2: Compute System Timeline Animation Values
        solarOrbitAngle += 0.008; 
        double camTiltX = Math.toRadians(82.0) + 0.05 * Math.cos(solarOrbitAngle * 0.8);
        double camTiltZ = Math.toRadians(8.0) + 0.02 * Math.sin(solarOrbitAngle * 1.5);
        double cosTX = Math.cos(camTiltX), sinTX = Math.sin(camTiltX);
        double cosTZ = Math.cos(camTiltZ), sinTZ = Math.sin(camTiltZ);

        // Directional sunlight vector
        double lightX = 0.58; double lightY = -0.58; double lightZ = 0.58;
        double cameraDistance = 4.5;
        double baseRadius = 0.98;
        double flattenFactor = 0.97; 

        // Step 3: Render Uranus Thin Vertical Ribbon Rings with Keplerian Motion
        for (double rRad = 1.40; rRad <= 2.15; rRad += 0.08) {
            double keplerSpeed = 0.06 / Math.pow(rRad, 1.5);
            double ringOrbitOffset = (currentTime * keplerSpeed) * 0.05;
            double stepSize = 0.004 / rRad;

            for (double phi = 0; phi < 2 * Math.PI; phi += stepSize) {
                double animatedPhi = phi + ringOrbitOffset;
                double cosPhi = Math.cos(animatedPhi);
                double sinPhi = Math.sin(animatedPhi);

                // RESTORED: Kept the exact original, stable horizontal ring generation
                double lx = rRad * cosPhi;
                double ly = -0.1;
                double lz = rRad * sinPhi;

                // Matrix Rotation Transform (Original 3D camera projection)
                double rx = lx * cosTZ - ly * sinTZ;
                double tmpY = lx * sinTZ + ly * cosTZ;
                double ry = tmpY * cosTX - lz * sinTX;
                double rz = tmpY * sinTX + lz * cosTX;

                // --- FIXED: 90 DEGREE CAMERA ROLL TRANSFORMATION ---
                // Rotate the pre-calculated 2D screen coordinates 90 degrees around the viewing sightline.
                // This preserves the exact oblong perspective compression from the original model, 
                // but forces the ring loops to stand up vertically on the screen layout.
                double rolledRx = -rz;
                double rolledRz = rx;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rolledRx);
                int yp = (int) (11 - 36 * ooz * rolledRz);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;
                        double ringNormalX = 0.0; double ringNormalY = -sinTX; double ringNormalZ = cosTX;
                        double ringDot = Math.abs(ringNormalX * lightX + ringNormalY * lightY + ringNormalZ * lightZ);
                        double finalLuminance = 0.22 + 0.55 * ringDot;

                        String ringPalette = " .,-~:;="; 
                        int shadeIndex = (int) (finalLuminance * (ringPalette.length() - 1));
                        char renderChar = ringPalette.charAt(Math.max(0, Math.min(ringPalette.length() - 1, shadeIndex)));

                        int ringR = (int) (65 * finalLuminance);
                        int ringG = (int) (80 * finalLuminance);
                        int ringB = (int) (95 * finalLuminance);

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", ringR, ringG, ringB);
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }

        // Step 4: Render Featureless Methane-Absorbing Spheroid Globe
        for (double theta = 0; theta < Math.PI; theta += 0.015) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.015) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double lx = baseRadius * sinTheta * cosPhi;
                double ly = baseRadius * sinTheta * sinPhi;
                double lz = baseRadius * cosTheta * flattenFactor;

                // Matrix Transformation
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
                        double nx = rx / baseRadius;
                        double ny = ry / baseRadius;
                        double nz = rz / (baseRadius * flattenFactor * flattenFactor);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (nLen > 0) { nx /= nLen; ny /= nLen; nz /= nLen; }

                        double shadowFactor = 1.0;
                        for (double t = 0.1; t < 1.6; t += 0.08) {
                            double rayX = rx + t * lightX; double rayY = ry + t * lightY; double rayZ = rz + t * lightZ;
                            double unRotY = rayY * cosTX + rayZ * sinTX;
                            double unRotZ = -rayY * sinTX + rayZ * cosTX;
                            double unRotX = rayX * cosTZ + unRotY * sinTZ;
                            double finalRingY = -rayX * sinTZ + unRotY * cosTZ;
                            double rayRadius = Math.sqrt(unRotX * unRotX + finalRingY * finalRingY);

                            if (rayRadius >= 1.40 && rayRadius <= 2.15 && Math.abs(unRotZ) < 0.04) {
                                if (Math.floor(rayRadius * 12.5) % 2 == 0) {
                                    shadowFactor = 0.40; 
                                    break;
                                }
                            }
                        }

                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse) * shadowFactor;
                        double finalLuminance = 0.02 + 0.98 * Math.pow(baseLight, 1.2);
                        
                        String globePalette = " .,-~:;=!*#$@";
                        int shadeIndex = (int) (finalLuminance * (globePalette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(globePalette.length() - 1, shadeIndex));
                        char renderChar = globePalette.charAt(shadeIndex);

                        int outR = (int) (URANUS_R * finalLuminance);
                        int outG = (int) (URANUS_G * (finalLuminance * 1.05));
                        double limbDarkening = 0.70 + 0.30 * finalLuminance;
                        int outB = (int) (URANUS_B * (finalLuminance * 1.35 * limbDarkening));

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
