abstract public class TexelCubeLoader extends Loader {
    private double angle = 0.0;

    public TexelCubeLoader(StatusStage[] stages) {
        // These typically use 80x22 if none else was specified
        super(stages, 80, 22);
    }

    public TexelCubeLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
    }

    @Override
    protected void initialize() {
        // Do nothing for now, but this can be overridden if needed
    }

    /**
     * The resolution (width and height, in texels) of the square texture space passed to
     * {@link #getCubeTexel(int, int, int)}. Override this in a subclass to work in a
     * higher-resolution texture space for finer detail (borders, symbols, etc.) — the x/y
     * coordinates handed to getCubeTexel scale automatically with whatever you return here.
     * Defaults to 16 so any existing subclass that doesn't override this keeps behaving
     * exactly as before.
     */
    protected int getTextureResolution() {
        return 16;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double rX = angle * 0.4;
        double rY = angle * 0.7;
        double rZ = angle * 0.2;

        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);
        double cosZ = Math.cos(rZ), sinZ = Math.sin(rZ);

        int textureResolution = getTextureResolution();

        for (int face = 0; face < 6; face++) {
            double nx = 0, ny = 0, nz = 0;
            switch (face) {
                case 0:
                    nz = -1;
                    break; // Back
                case 1:
                    nz = 1;
                    break; // Front
                case 2:
                    ny = -1;
                    break; // Bottom
                case 3:
                    ny = 1;
                    break; // Top
                case 4:
                    nx = -1;
                    break; // Left
                case 5:
                    nx = 1;
                    break; // Right
            }

            double nz3 = -nx * sinY + (ny * sinX + nz * cosX) * cosY;
            if (nz3 > 0) {
                continue;
            }

            for (double u = 0; u <= 1.0; u += 0.02) {
                for (double v = 0; v <= 1.0; v += 0.02) {

                    double uc = 2.0 * u - 1.0;
                    double vc = 2.0 * v - 1.0;
                    double x = 0, y = 0, z = 0;

                    switch (face) {
                        case 0:
                            x = uc;
                            y = vc;
                            z = -1;
                            break;
                        case 1:
                            x = uc;
                            y = vc;
                            z = 1;
                            break;
                        case 2:
                            x = uc;
                            y = -1;
                            z = vc;
                            break;
                        case 3:
                            x = uc;
                            y = 1;
                            z = vc;
                            break;
                        case 4:
                            x = -1;
                            y = uc;
                            z = vc;
                            break;
                        case 5:
                            x = 1;
                            y = uc;
                            z = vc;
                            break;
                    }

                    // --- 3D ROTATION ---
                    double y1 = y * cosX - z * sinX;
                    double z1 = y * sinX + z * cosX;
                    double x1 = x;

                    double x2 = x1 * cosY + z1 * sinY;
                    double z2 = -x1 * sinY + z1 * cosY;
                    double y2 = y1;

                    double x3 = x2 * cosZ - y2 * sinZ;
                    double y3 = x2 * sinZ + y2 * cosZ;
                    double z3 = z2;

                    // --- PERSPECTIVE PROJECTION ---
                    double distanceToCamera = 3.1;
                    double ooz = 1.0 / (z3 + distanceToCamera);

                    int xp = (int) (40 + 40 * ooz * x3);
                    int yp = (int) (11 - 18 * ooz * y3);

                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                        int index = xp + 80 * yp;

                        if (ooz > zBuffer[index] + 0.0001) {
                            zBuffer[index] = ooz;

                            double texU = u;
                            double texV = v;

                            // Standardized face layout alignment tracking
                            switch (face) {
                                case 0:
                                    texU = u;
                                    texV = 1.0 - v;
                                    break;
                                case 1:
                                    texU = 1.0 - u;
                                    texV = 1.0 - v;
                                    break;
                                case 4:
                                    texU = 1.0 - v;
                                    texV = 1.0 - u;
                                    break;
                                case 5:
                                    texU = v;
                                    texV = 1.0 - u;
                                    break;
                                case 2:
                                case 3:
                                    texU = u;
                                    texV = v;
                                    break;
                            }

                            int texX = (int) (texU * textureResolution);
                            int texY = (int) (texV * textureResolution);

                            texX = Math.max(0, Math.min(textureResolution - 1, texX));
                            texY = Math.max(0, Math.min(textureResolution - 1, texY));

                            VoxelTexel texel = getCubeTexel(face, texX, texY);

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", texel.r, texel.g, texel.b);
                            outputBuffer[index] = colorCode + texel.character + RESET;
                        }
                    }
                }
            }
        }
        angle += 0.025;
    }

    // This is where the fun happens.... Define this in your own class!!
    // Look at the code for TexelMinecraftGrassBlock.java, notice how this
    // method is the only thing needed to define to create your own custom cube...
    protected abstract VoxelTexel getCubeTexel(int face, int x, int y);

    // The actual Texels we deal with
    protected static class VoxelTexel {
        final int r, g, b;
        final char character;

        VoxelTexel(int r, int g, int b, char character) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.character = character;
        }
    }
}