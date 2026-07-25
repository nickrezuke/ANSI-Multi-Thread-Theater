public class CubeLoaderB extends Loader {
    private int blockVariant = -1; // 1 = Grass, 2 = Companion Cube, 3 = Rubik's Cube
    private double angle = 0.0;

    public CubeLoaderB() {
        int variant = (int) (Math.random() * 3) + 1;
        StatusStage[] CUBE_STAGES;

        switch (variant) {
            case 1: // Minecraft Grass Block
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(10, "Generating Chunks:"),
                        new StatusStage(20, "Growing Forests:"),
                        new StatusStage(30, "Populating Villages:"),
                        new StatusStage(40, "Scattering Ore Deposits:"),
                        new StatusStage(50, "Heating up the Nether:"),
                        new StatusStage(60, "Building Strongholds:"),
                        new StatusStage(70, "Filling in Loot Chests:"),
                        new StatusStage(80, "Determining World Spawn:"),
                        new StatusStage(90, "Initializing Player:"),
                        new StatusStage(99, "Removing Herobrine:"),
                        new StatusStage(100, "World Generation Complete!")
                };
                break;
            case 2: // Companion Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(8, "Calibrating Scientific Intruments:"),
                        new StatusStage(28, "Designing Test Chamber:"),
                        new StatusStage(55, "Synthesizing Test Subjects:"),
                        new StatusStage(75, "Activating Observation Deck:"),
                        new StatusStage(96, "Calibrating Portal Devices:"),
                        new StatusStage(98, "Rejecting Morality Upgrade:"),
                        new StatusStage(100, "Test Chamber Ready!")
                };
                break;
            case 3: // Rubik's Cube
            default: // TODO: Auto default to this but come up with something later... maybe
                     // introduce new variants??
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(12, "Scrambling:"),
                        new StatusStage(24, "Rotating:"),
                        new StatusStage(36, "Scrambling:"),
                        new StatusStage(48, "Rotating:"),
                        new StatusStage(60, "Scrambling:"),
                        new StatusStage(72, "Rotating:"),
                        new StatusStage(84, "Scrambling:"),
                        new StatusStage(96, "Rotating:"),
                        new StatusStage(100, "Scramble Solved!")
                };
                break;
        }
        super(CUBE_STAGES);
        blockVariant = variant;
    }

    @Override
    protected void initialize() {
        // We should have already determined which one 
        // to use, but just in case we didn't yet:
        if (this.blockVariant == -1) {
            // Randomly select between 1 (Grass), 2 (Companion Cube), and 3 (Rubik's Cube)
            this.blockVariant = (int) (Math.random() * 3) + 1;
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

    private VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        // High-frequency deterministic hash pattern accent noise
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        if (variant == 1) { // ==================== MINECRAFT GRASS BLOCK ====================
            if (face == 3) { // Top Green
                return (noise == 0) ? new VoxelTexel(75, 145, 35, '#') : new VoxelTexel(95, 175, 40, 'X');
            } else if (face == 2) { // Bottom Dirt
                return (noise == 0) ? new VoxelTexel(100, 70, 45, ';') : new VoxelTexel(125, 88, 55, ':');
            } else { // Sides
                int grassThreshold = 4 + (x % 3 == 0 ? 1 : 0) + ((x * 7) % 2);
                if (y < grassThreshold) {
                    return (noise == 0) ? new VoxelTexel(75, 145, 35, '#') : new VoxelTexel(95, 175, 40, 'X');
                } else {
                    return (noise == 0) ? new VoxelTexel(100, 70, 45, ';') : new VoxelTexel(125, 88, 55, ':');
                }
            }

        } else if (variant == 2) { // ==================== PORTAL COMPANION CUBE ====================
            // 1. Central Pink/White Heart Art Matrix
            boolean isHeart = false;
            if (x >= 6 && x <= 9 && y >= 6 && y <= 9) {
                if (y == 6 && (x == 6 || x == 9))
                    isHeart = true;
                if (y == 7 && (x >= 5 && x <= 10))
                    isHeart = true;
                if (y == 8 && (x >= 6 && x <= 9))
                    isHeart = true;
                if (y == 9 && (x == 7 || x == 8))
                    isHeart = true;
            }
            if (isHeart)
                return new VoxelTexel(245, 105, 155, 'O'); // Hot Pink

            // 2. Central Core Circle Background Ring
            boolean isCenterPlaza = (x >= 5 && x <= 10 && y >= 5 && y <= 10);
            if (isCenterPlaza)
                return new VoxelTexel(215, 215, 215, '='); // Soft White Ring Base

            // 3. Dual-Tone Outer Protective Corner Brackets
            boolean isCornerBezel = (x <= 3 || x >= 12) && (y <= 3 || y >= 12);
            boolean isEdgeBar = (x <= 1 || x >= 14 || y <= 1 || y >= 14);

            if (isCornerBezel || isEdgeBar) {
                return new VoxelTexel(165 - noise * 10, 165 - noise * 10, 170 - noise * 10, '#'); // Light Gray Trim
            }

            // 4. Central Inset Core Plating
            return new VoxelTexel(75 + noise * 8, 75 + noise * 8, 80 + noise * 8, ';'); // Dark Alloy Backplate

        } else { // ==================== CLASSIC 3X3 RUBIK'S CUBE ====================
            // Render 16x16 grid gridlines framing three 4x4 matrix tiles along each path
            if (x == 0 || x == 5 || x == 10 || x == 15 || y == 0 || y == 5 || y == 10 || y == 15) {
                return new VoxelTexel(15, 15, 20, '#'); // True Black Inset Grid Borders
            }

            // Segment the face index positions into cleanly separated sub-quadrants
            int row = (y < 5) ? 0 : (y < 10) ? 1 : 2;
            int col = (x < 5) ? 0 : (x < 10) ? 1 : 2;

            // Deterministic color assignment hash per cell square variant layout
            int colorHash = Math.abs((row * 7 + col * 13 + face * 19)) % 6;

            switch (colorHash) {
                case 0:
                    return new VoxelTexel(235, 30, 30, 'X'); // Radiant Red
                case 1:
                    return new VoxelTexel(30, 95, 240, 'O'); // Deep Blue
                case 2:
                    return new VoxelTexel(245, 130, 20, '='); // Bright Orange
                case 3:
                    return new VoxelTexel(40, 210, 60, '$'); // Neon Green
                case 4:
                    return new VoxelTexel(250, 250, 250, ';'); // Pure Ceramic White
                default:
                    return new VoxelTexel(240, 230, 25, '%'); // Vivid Canary Yellow
            }
        }
    }

    private static class VoxelTexel {
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
