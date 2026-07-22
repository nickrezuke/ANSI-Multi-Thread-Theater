public class CubeLoader extends Loader {
    private static final StatusStage[] CUBE_STAGES = {
        new StatusStage(30, "Mining blocks:"),
        new StatusStage(60, "Forging gold:"),
        new StatusStage(90, "Placing ? mark:"),
        new StatusStage(100, "Power-up Ready!")
    };

    // Shading characters for each of the 6 distinct faces
    private static final char[] FACE_SHADES = { '#', 'X', 'O', '=', ';', ':' };

    // Arrays to store 24-bit TrueColor RGB strings for each face
    private final String[] blockShades = new String[6];
    private final String[] questionShades = new String[6];
    private double angle = 0.0;

    public CubeLoader() {
        super(CUBE_STAGES);
    }

    @Override
    protected void initialize() {
        // Randomly select between the 4 iconic Super Mario 64 block variations
        int variant = (int) (Math.random() * 4) + 1;

        // Base RGB values for the brightest face of each block type
        int baseR, baseG, baseB;
        switch (variant) {
            case 1: // --- GOLD ITEM BLOCK ---
                baseR = 255; baseG = 180; baseB = 30;
                break;
            case 2: // --- RED WING CAP BLOCK ---
                baseR = 240; baseG = 40; baseB = 40;
                break;
            case 3: // --- GREEN METAL CAP BLOCK ---
                baseR = 30; baseG = 180; baseB = 60;
                break;
            case 4: // --- BLUE VANISH CAP BLOCK ---
                baseR = 40; baseG = 100; baseB = 230;
                break;
            default: // --- DEFAULT ERROR BLOCK?? ---
                baseR = 135; baseG = 135; baseB = 135;
                break;
        }

        // Subtly scale the base RGB values across the 6 cube faces
        double[] faceLuminanceFactors = { 1.0, 0.94, 0.88, 0.82, 0.76, 0.70 };
        for (int i = 0; i < 6; i++) {
            double factor = faceLuminanceFactors[i];
            // Calculate dimmed background block colors
            int r = (int) (baseR * factor);
            int g = (int) (baseG * factor);
            int b = (int) (baseB * factor);
            blockShades[i] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);

            // Calculate dimmed white (?) text colors
            int qw = (int) (255 * factor);
            questionShades[i] = String.format("\u001B[38;2;%d;%d;%dm", qw, qw, qw);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double rX = angle * 0.4; // Pitch (Up/Down tilt)
        double rY = angle * 0.7; // Yaw (Horizontal spin)
        double rZ = angle * 0.2; // Roll (Bank)
        
        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);
        double cosZ = Math.cos(rZ), sinZ = Math.sin(rZ);

        // Loop through all 6 faces of the cube
        for (int face = 0; face < 6; face++) {
            // ==================== BACK-FACE CULLING ====================
            double nx = 0, ny = 0, nz = 0;
            switch (face) {
                case 0: nz = -1; break; // Back Face
                case 1: nz = 1;  break; // Front Face
                case 2: ny = -1; break; // Bottom Face
                case 3: ny = 1;  break; // Top Face
                case 4: nx = -1; break; // Left Face
                case 5: nx = 1;  break; // Right Face
            }

            double nz3 = -nx * sinY + (ny * sinX + nz * cosX) * cosY;
            if (nz3 > 0) {
                continue; 
            }
            // =======================================================================

            char shadeChar = FACE_SHADES[face];

            // Plot points across the surface grid of the current face
            for (double u = 0; u <= 1.0; u += 0.02) {
                for (double v = 0; v <= 1.0; v += 0.02) {
                    
                    double uc = 2.0 * u - 1.0;
                    double vc = 2.0 * v - 1.0;
                    double x = 0, y = 0, z = 0;

                    // Mathematically position the 6 perfect square planes of the cube
                    switch (face) {
                        case 0: x = uc; y = vc; z = -1; break; // Back Face
                        case 1: x = uc; y = vc; z = 1;  break; // Front Face
                        case 2: x = uc; y = -1; z = vc; break; // Bottom Face
                        case 3: x = uc; y = 1;  z = vc; break; // Top Face
                        case 4: x = -1; y = uc; z = vc; break; // Left Face
                        case 5: x = 1;  y = uc; z = vc; break; // Right Face
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
                    int yp = (int) (11 + 18 * ooz * y3);

                    // --- Z-BUFFER RENDERING ---
                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                        int index = xp + 80 * yp;

                        if (ooz > zBuffer[index] + 0.0001) {
                            zBuffer[index] = ooz;

                            String activeColor = blockShades[face];
                            char renderChar = shadeChar;

                            // Dynamic UV alignment mapping variables
                            double texU = u;
                            double texV = v;
                            boolean includeQuestionMark = false;

                            // Set up texture parameters based on which face is being rendered
                            switch (face) {
                                case 0: // Back Face
                                    includeQuestionMark = true;
                                    break;
                                case 1: // Front Face
                                    texU = 1.0 - u;
                                    includeQuestionMark = true;
                                    break;
                                case 4: // Left Face
                                    texU = 1.0 - v;
                                    texV = u;
                                    includeQuestionMark = true;
                                    break;
                                case 5: // Right Face
                                    texU = v;
                                    texV = u;
                                    includeQuestionMark = true;
                                    break;
                                case 2: // Bottom Face
                                case 3: // Top Face
                                    // Use standard u/v mapping; omit the question mark symbol
                                    includeQuestionMark = false;
                                    break;
                            }

                            // Inject texture maps onto the screen coordinates
                            if (isWhiteTexturePixel(texU, texV, includeQuestionMark)) {
                                activeColor = questionShades[face];
                                renderChar = '?';
                            }

                            outputBuffer[index] = activeColor + renderChar + RESET;
                        }
                    }
                }
            }
        }
        angle += 0.04;
    }

    private boolean isWhiteTexturePixel(double u, double v, boolean includeQuestionMark) {
        // 1. --- CENTRAL QUESTION MARK TEXTURE ---
        if (includeQuestionMark) {
            if (v >= 0.22 && v <= 0.32 && u >= 0.30 && u <= 0.70) return true;
            if (u >= 0.60 && u <= 0.70 && v >= 0.32 && v <= 0.52) return true;
            if (v >= 0.45 && v <= 0.55 && u >= 0.45 && u <= 0.65) return true;
            if (u >= 0.45 && u <= 0.55 && v >= 0.55 && v <= 0.68) return true;
            if (u >= 0.45 && u <= 0.55 && v >= 0.76 && v <= 0.86) return true;
        }

        // 2. --- CHECKERBOARD BORDER CHECK ---
        boolean isBorderZone = (u <= 0.06 || u >= 0.94 || v <= 0.06 || v >= 0.94);
        if (isBorderZone) {
            int uCheck = (int) (u * 8);
            int vCheck = (int) (v * 8);
            if ((uCheck + vCheck) % 2 == 0) {
                return true;
            }
        }

        // 3. --- FOUR CORNER RIVETS/SCREWS ---
        if (u >= 0.09 && u <= 0.13 && v >= 0.09 && v <= 0.13) return true; // Top-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.09 && v <= 0.13) return true; // Top-Right
        if (u >= 0.09 && u <= 0.13 && v >= 0.87 && v <= 0.91) return true; // Bottom-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.87 && v <= 0.91) return true; // Bottom-Right

        return false;
    }
}
