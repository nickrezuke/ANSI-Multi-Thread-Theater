import java.util.Random;

public class PlutoLoader extends Loader {
    private static final StatusStage[] PLUTO_STAGES = {
            new StatusStage(30, "Mapping Sputnik Planitia (Bright White Heart):"),
            new StatusStage(65, "Rendering Cthulhu Macula (Dark Equatorial Region):"),
            new StatusStage(90, "Applying beige/tan tholin frost to northern latitudes:"),
            new StatusStage(100, "Plutonian Tombaugh Regio Matrix Active!")
    };

    private static final int MAX_STARS = 35;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double plutoRotationAngle = 0.0;

    // Scientifically matched to the iconic New Horizons true/enhanced color composite
    private static final int[] C_HEART_WHITE   = { 245, 235, 225 }; // Bright creamy white of Sputnik Planitia
    private static final int[] C_SANDY_BEIGE   = { 215, 175, 135 }; // Lighter tan/butterscotch northern plains
    private static final int[] C_DARK_MACULA   = { 65, 30, 25    }; // Deep reddish-charcoal of Cthulhu Macula
    private static final int[] C_HAZE_BLUE     = { 135, 170, 195 }; // Faint blue atmospheric scattering haze

    public PlutoLoader() {
        super(PLUTO_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // Start rotation so the Heart is front-and-center for the viewer
        this.plutoRotationAngle = Math.PI; 
        Random rand = new Random(5555);
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
        //plutoRotationAngle -= 0.0015; // more realistic
        plutoRotationAngle -= 0.007; // more visually interesting

        // Extreme axial tilt of ~122.5 degrees
        double axialTilt = Math.toRadians(122.5);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // Overhead directional spotlight vectors
        double lightX = 0.60, lightY = -0.70, lightZ = 0.38;
        double cameraDistance = 3.85;
        double sphereRadius = 0.72;
        double flattenFactor = 1.0; 

        // -------------------------------------------------------------
        // Step 3: Render Dwarf Planet Pluto Spheroid Globe
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

                        // Calculate seamless longitude wrapping for mapping features
                        double currentLon = Math.atan2(uy, ux) + plutoRotationAngle;
                        double lonWrap = currentLon % (Math.PI * 2);
                        if (lonWrap < -Math.PI) lonWrap += Math.PI * 2;
                        if (lonWrap > Math.PI) lonWrap -= Math.PI * 2;
                        
                        // 1. Map the iconic Tombaugh Regio (The Heart)
                        // Tilted & Asymmetrical: Sputnik Planitia (left) dips south, Eastern lobe sits high north
                        double leftLobe = Math.exp(-Math.pow(latDeg - 0.0, 2) / 350.0 - Math.pow(lonWrap + 0.3, 2) * 5.5);
                        double rightLobe = Math.exp(-Math.pow(latDeg - 18.0, 2) / 250.0 - Math.pow(lonWrap - 0.35, 2) * 6.0);
                        double bottomPoint = Math.exp(-Math.pow(latDeg + 25.0, 2) / 300.0 - Math.pow(lonWrap + 0.15, 2) * 4.5);
                        
                        // Combine lobes and apply a smoothstep function to organically blend the edges 
                        double rawHeart = Math.max(leftLobe, Math.max(rightLobe * 0.85, bottomPoint * 0.9));
                        double heartBlend = rawHeart * rawHeart * (3 - 2 * rawHeart); // Soft, non-sticker edges

                        // 2. Map standard terrain noise for the sandy/beige northern regions
                        double terrainPattern = Math.sin(latDeg * 0.15) * Math.cos(currentLon * 2.5) + 0.5 * Math.sin(currentLon * 3.0);
                        double terrainMix = (terrainPattern + 1.5) / 3.0;
                        double baseT = Math.max(0.0, Math.min(1.0, terrainMix));

                        // 3. Map Cthulhu Macula: (Dark equatorial band) - Removed the rectangular box bug!
                        // Uses Gaussian falloff to seamlessly wrap around the back of the planet
                        double maculaLat = Math.exp(-Math.pow(latDeg + 5.0, 2) / 200.0); 
                        double mDist = Math.abs(lonWrap + 1.8);
                        if (mDist > Math.PI) mDist = 2 * Math.PI - mDist; // seamless periodic wrapping
                        double maculaLon = Math.exp(-Math.pow(mDist, 2) / 1.5);
                        
                        // Prevent the dark macula from bleeding into the white heart
                        double maculaIntensity = maculaLat * maculaLon * (1.0 - heartBlend);
                        baseT = baseT * (1.0 - maculaIntensity * 0.9); // Darkens the base terrain

                        // Lighting and shading calculation
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);
                        double viewAngle = Math.abs(ny);
                        
                        double finalLuminance = 0.25 + 0.75 * Math.pow(baseLight, 1.1);

                        String palette = " .:-=+#%@";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        // Apply the accurate NASA palette smoothly
                        int baseR = (int) (C_DARK_MACULA[0] * (1 - baseT) + C_SANDY_BEIGE[0] * baseT);
                        int baseG = (int) (C_DARK_MACULA[1] * (1 - baseT) + C_SANDY_BEIGE[1] * baseT);
                        int baseB = (int) (C_DARK_MACULA[2] * (1 - baseT) + C_SANDY_BEIGE[2] * baseT);

                        // Organically blend the heart frost over the top of the base terrain
                        outR = (int) (baseR * (1 - heartBlend) + C_HEART_WHITE[0] * heartBlend);
                        outG = (int) (baseG * (1 - heartBlend) + C_HEART_WHITE[1] * heartBlend);
                        outB = (int) (baseB * (1 - heartBlend) + C_HEART_WHITE[2] * heartBlend);

                        // Add faint blue atmospheric haze rim
                        if (viewAngle < 0.3) {
                            double rimFactor = (0.3 - viewAngle) / 0.3;
                            outR = (int) (outR * (1.0 - rimFactor * 0.35) + C_HAZE_BLUE[0] * (rimFactor * 0.35));
                            outG = (int) (outG * (1.0 - rimFactor * 0.35) + C_HAZE_BLUE[1] * (rimFactor * 0.35));
                            outB = (int) (outB * (1.0 - rimFactor * 0.35) + C_HAZE_BLUE[2] * (rimFactor * 0.35));
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