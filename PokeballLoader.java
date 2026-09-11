public class PokeballLoader extends Loader {
    private static final StatusStage[] POKEBALL_STAGES = {
            new StatusStage(20, "Identifying wild Pokémon..."),
            new StatusStage(45, "Calibrating capture trajectory..."),
            new StatusStage(70, "Releasing mass condenser..."),
            new StatusStage(90, "Initiating capture sequence..."),
            new StatusStage(100, "Gotcha! Pokémon caught!")
    };

    private static final int[] C_RED   = { 235, 40, 40 };
    private static final int[] C_WHITE = { 240, 240, 240 };
    private static final int[] C_BLACK = { 30, 30, 30 };

    public PokeballLoader() {
        super(POKEBALL_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        setTargetFps(120); // To help capture the quick moving wobble
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        // Game-accurate rhythmic wobble: intense bursts of shaking followed by calm pauses
        double t = currentTime * 0.003;
        double intensityEnvelope = Math.pow(Math.abs(Math.sin(t * 0.4)), 3.0);
        double wobbleRoll = 0.55 * intensityEnvelope * Math.sin(t * 4.5);

        double axialTilt = Math.toRadians(15.0);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        double cosRoll = Math.cos(wobbleRoll);
        double sinRoll = Math.sin(wobbleRoll);

        double lightX = 0.50, lightY = -0.80, lightZ = 0.40;
        double cameraDistance = 3.6;

        for (double theta = 0.01; theta < Math.PI; theta += 0.012) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.012) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double ux = sinTheta * cosPhi;
                double uy = sinTheta * sinPhi;
                double uz = cosTheta;

                // Anchored at uy = -1 (front face pointing towards the camera)
                double angleFromButton = Math.acos(Math.max(-1.0, Math.min(1.0, -uy)));

                double localRadius = 1.0;
                double contourShade = 1.0; 
                int[] baseColor;

                // Center button dot
                if (angleFromButton < 0.16) {
                    localRadius = 1.04;     
                    baseColor = C_WHITE;
                // Extra thick black bezel around the button
                } else if (angleFromButton < 0.36) {
                    localRadius = 0.94;     
                    contourShade = 0.25;
                    baseColor = C_BLACK;
                // Thicker black equatorial belt
                } else if (Math.abs(uz) < 0.13) {
                    localRadius = 0.94;     
                    contourShade = 0.25;
                    baseColor = C_BLACK;
                // Top hemisphere (Red)
                } else if (uz > 0) {
                    baseColor = C_RED;      
                // Bottom hemisphere (White)
                } else {
                    baseColor = C_WHITE;    
                }

                double lx = localRadius * ux;
                double ly = localRadius * uy;
                double lz = localRadius * uz;

                // Apply roll wobble and pitch tilt transformation matrices
                double x_rolled = lx * cosRoll - lz * sinRoll;
                double z_rolled = lx * sinRoll + lz * cosRoll;
                double y_rolled = ly;

                double rx = x_rolled;
                double ry = y_rolled * cosTilt - z_rolled * sinTilt;
                double rz = y_rolled * sinTilt + z_rolled * cosTilt;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 70 * ooz * rx - 15.4 * wobbleRoll);
                int yp = (int) (11 - 32 * ooz * rz);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22 && ry < 0) {
                    int index = xp + 80 * yp;
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        double nx = rx / localRadius;
                        double ny = ry / localRadius;
                        double nz = rz / localRadius;
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (nLen > 0) {
                            nx /= nLen; ny /= nLen; nz /= nLen;
                        }

                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.15, diffuse); 

                        double dotNL = diffuse;
                        double specular = 0.0;
                        if (dotNL > 0) {
                            double ry_spec = 2 * dotNL * ny - lightY;
                            double dotRV = Math.max(0.0, -ry_spec);
                            specular = Math.pow(dotRV, 16.0) * 0.7; 
                        }

                        double finalLuminance = (0.3 + 0.7 * baseLight) * contourShade;
                        finalLuminance = Math.min(1.0, finalLuminance);

                        String palette = " .:-=+*#%@";
                        double visualLuminance = Math.min(1.0, finalLuminance + specular);
                        int shadeIndex = (int) (visualLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR = (int) Math.min(255, baseColor[0] * finalLuminance + specular * 255);
                        int outG = (int) Math.min(255, baseColor[1] * finalLuminance + specular * 255);
                        int outB = (int) Math.min(255, baseColor[2] * finalLuminance + specular * 255);

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", outR, outG, outB);
                        outputBuffer[index] = colorCode + renderChar + "\u001B[0m";
                    }
                }
            }
        }
    }
}