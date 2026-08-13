public class ChromeCheckerboardSphereLoader extends Loader {
    private static final StatusStage[] REFLECTIVE_STAGES = {
        new StatusStage(25, "Tracing infinite ground floor:"),
        new StatusStage(50, "Generating reflection vector fields:"),
        new StatusStage(75, "Interpolating fresnel rim highlights:"),
        new StatusStage(100, "Ray-Traced Mirror Core Engaged!")
    };

    private double timeClock = 0.0;
    private int width = 80;
    private int height = 22;
    private static final double CAMERA_DISTANCE = 1.6;

    public ChromeCheckerboardSphereLoader() {
        // Defaults to 80x22
        super(REFLECTIVE_STAGES, 80, 22);
    }

    public ChromeCheckerboardSphereLoader(int w, int h) {
        super(REFLECTIVE_STAGES, w, h);
        width = w;
        height = h;
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.025; 

        String shadingRamp = " .:-=+*#%@▓";

        double sphereX = 0.5 * Math.sin(timeClock * 0.4);
        double sphereY = -0.2 - 0.25 * Math.abs(Math.sin(timeClock * 0.6)); 
        double sphereZ = 0.0;
        double sphereRadius = 0.85;

        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;
        double floorLevel = 0.65;

        for (int y = 0; y < height; y++) {
            double screenY = ((double) y / height) * 2.0 - 1.0;
            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                double screenX = (((double) x / width) * 2.0 - 1.0) * 2.3;

                double rayX = 0.0, rayY = 0.0, rayZ = -CAMERA_DISTANCE; 
                double dirX = screenX, dirY = screenY, dirZ = 1.8; 
                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= len; dirY /= len; dirZ /= len;

                double ocX = rayX - sphereX;
                double ocY = rayY - sphereY;
                double ocZ = rayZ - sphereZ;
                double b = ocX * dirX + ocY * dirY + ocZ * dirZ;
                double c = (ocX * ocX + ocY * ocY + ocZ * ocZ) - (sphereRadius * sphereRadius);
                double discriminant = b * b - c;
                boolean hitSphere = false;

                if (discriminant >= 0) {
                    double t = -b - Math.sqrt(discriminant);
                    if (t > 0) {
                        hitSphere = true;
                        double depthZ = 1.0 / t;
                        if (depthZ > zBuffer[idx]) {
                            zBuffer[idx] = depthZ;

                            double hitX = rayX + dirX * t;
                            double hitY = rayY + dirY * t;
                            double hitZ = rayZ + dirZ * t;

                            double nx = (hitX - sphereX) / sphereRadius;
                            double ny = (hitY - sphereY) / sphereRadius;
                            double nz = (hitZ - sphereZ) / sphereRadius;

                            double dotVN = dirX * nx + dirY * ny + dirZ * nz;
                            double refX = dirX - 2.0 * dotVN * nx;
                            double refY = dirY - 2.0 * dotVN * ny;
                            double refZ = dirZ - 2.0 * dotVN * nz;

                            double specDot = refX * lightX + refY * lightY + refZ * lightZ;
                            double spec = (specDot > 0) ? Math.pow(specDot, 24) : 0;

                            int r = 160, g = 165, bColor = 175; 
                            if (refY > 0.001) { 
                                double tFloor = (floorLevel - hitY) / refY;
                                if (tFloor > 0) {
                                    double fx = hitX + refX * tFloor;
                                    double fz = hitZ + refZ * tFloor;
                                    int checkX = (int) (Math.floor(fx * 1.5));
                                    int checkZ = (int) (Math.floor(fz * 1.5 + timeClock * 0.4));
                                    if ((checkX + checkZ) % 2 == 0) {
                                        r = 75; g = 80; bColor = 90; 
                                    } else {
                                        r = 210; g = 215; bColor = 220; 
                                    }
                                    double fog = Math.min(1.0, tFloor * 0.12);
                                    r = (int) (r * (1.0 - fog) + 130 * fog);
                                    g = (int) (g * (1.0 - fog) + 135 * fog);
                                    bColor = (int) (bColor * (1.0 - fog) + 145 * fog);
                                }
                            }

                            r += (int) (spec * 255); g += (int) (spec * 255); bColor += (int) (spec * 255);
                            r = Math.max(0, Math.min(255, r));
                            g = Math.max(0, Math.min(255, g));
                            bColor = Math.max(0, Math.min(255, bColor));

                            int shadeIdx = (int) ((0.2 + spec * 0.8) * (shadingRamp.length() - 1));
                            char renderChar = shadingRamp.charAt(Math.max(0, Math.min(shadingRamp.length() - 1, shadeIdx)));
                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, bColor);
                            outputBuffer[idx] = colorCode + renderChar + RESET;
                        }
                    }
                }

                if (!hitSphere) {
                    if (dirY > 0.005) {
                        double tFloor = (floorLevel - rayY) / dirY;
                        double depthZ = 1.0 / tFloor;
                        if (depthZ > zBuffer[idx]) {
                            zBuffer[idx] = depthZ;
                            double fx = rayX + dirX * tFloor;
                            double fz = rayZ + dirZ * tFloor;

                            int checkX = (int) (Math.floor(fx * 1.5));
                            int checkY = (int) (Math.floor(fz * 1.5 + timeClock * 0.4)); 
                            int r, g, bColor;

                            if ((checkX + checkY) % 2 == 0) {
                                r = 60; g = 65; bColor = 75; 
                            } else {
                                r = 185; g = 190; bColor = 195; 
                            }

                            double lightDistToFloor = (floorLevel - sphereY);
                            double shadowCenterX = sphereX - (lightX / -lightY) * lightDistToFloor;
                            double shadowCenterZ = sphereZ - (lightZ / -lightY) * lightDistToFloor * 0.6;

                            double dx = fx - shadowCenterX;
                            double dz = fz - shadowCenterZ;
                            double distToShadowCenter = Math.sqrt(dx * dx + dz * dz);
                            
                            double shadowLimit = sphereRadius * 1.2;
                            if (distToShadowCenter < shadowLimit) {
                                double shadowFactor = 0.25 + 0.75 * (distToShadowCenter / shadowLimit);
                                r *= shadowFactor; g *= shadowFactor; bColor *= shadowFactor;
                            }

                            double horizonFog = Math.min(1.0, tFloor * 0.09);
                            r = (int) (r * (1.0 - horizonFog) + 25 * horizonFog);
                            g = (int) (g * (1.0 - horizonFog) + 25 * horizonFog);
                            bColor = (int) (bColor * (1.0 - horizonFog) + 30 * horizonFog);

                            r = Math.max(0, Math.min(255, r));
                            g = Math.max(0, Math.min(255, g));
                            bColor = Math.max(0, Math.min(255, bColor));

                            char renderChar = shadingRamp.charAt(shadingRamp.length() - 1);

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, bColor);
                            outputBuffer[idx] = colorCode + renderChar + RESET;
                        }
                    } else {
                        if ((x + y * 13) % 31 == 0) {
                            outputBuffer[idx] = "\u001B[38;2;65;70;85m.\u001B[0m";
                        } else {
                            outputBuffer[idx] = " ";
                        }
                        zBuffer[idx] = 0.0;
                    }
                }
            }
        }
    }
}
