abstract public class TexelCubeLoader extends Loader {
    private int blockVariant = -1;
    private double angle = 0.0;

    private static int getRandomVariant() {
        // Right now there are 15 variants
        return (int) (Math.random() * 15) + 1;
    }

    public TexelCubeLoader() {
        this(getRandomVariant());
    }

    public TexelCubeLoader(StatusStage[] stages) {
        super(stages);
    }

    public TexelCubeLoader(int variant) {
        StatusStage[] TEXEL_CUBE_STAGES;

        switch (variant) {
            case 7: // Mario Block Cases
                TEXEL_CUBE_STAGES = new StatusStage[] {
                        new StatusStage(16, "Starting Level:"),
                        new StatusStage(30, "Stomping Enemies:"),
                        new StatusStage(42, "Kicking Shells:"),
                        new StatusStage(65, "Collecting Coins:"),
                        new StatusStage(85, "Utilizing Power-Ups:"),
                        new StatusStage(96, "Sliding Down Flagpole:"),
                        new StatusStage(100, "Level Complete!:")
                };
                break;

            default: // default (should not happen)
                TEXEL_CUBE_STAGES = new StatusStage[] {
                        new StatusStage(98, "Loading:"),
                        new StatusStage(100, "Loading Complete!:")
                };
                break;
        }
        super(TEXEL_CUBE_STAGES);
        blockVariant = variant;
    }

    @Override
    protected void initialize() {
        // We should have already determined which one
        // to use, but just in case we didn't yet:
        if (this.blockVariant == -1) {
            // Randomly select between
            this.blockVariant = getRandomVariant();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double rX = angle * 0.4;
        double rY = angle * 0.7;
        double rZ = angle * 0.2;

        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);
        double cosZ = Math.cos(rZ), sinZ = Math.sin(rZ);

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

                            int texX = (int) (texU * 16);
                            int texY = (int) (texV * 16);

                            texX = Math.max(0, Math.min(15, texX));
                            texY = Math.max(0, Math.min(15, texY));

                            VoxelTexel texel = getCubeTexel(blockVariant, face, texX, texY);

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", texel.r, texel.g, texel.b);
                            outputBuffer[index] = colorCode + texel.character + RESET;
                        }
                    }
                }
            }
        }
        angle += 0.025;
    }

    protected abstract VoxelTexel getCubeTexel(int variant, int face, int x, int y);

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
