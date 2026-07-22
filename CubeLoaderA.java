public class CubeLoaderA extends Loader {
    private static final StatusStage[] CUBE_STAGES = {
        new StatusStage(30, "Stomping Enemies:"),
        new StatusStage(60, "Collecting Coins:"),
        new StatusStage(90, "Grabbing Flagpoles:"),
        new StatusStage(100, "Power-Up Complete!")
    };

    // Shading characters for each of the 6 distinct faces
    private static final char[] FACE_SHADES = { 'X', 'O', '+', '=', ';', ':' };
    
    // Custom characters for the textures
    private static final char QUESTION_CHAR = '?';
    private static final char CHECKER_CHAR = '&';

    // Arrays to store 24-bit TrueColor RGB strings for each face
    private final String[] blockShades = new String[6];
    private final String[] questionShades = new String[6];
    private double angle = 0.0;

    public CubeLoaderA() {
        super(CUBE_STAGES);
    }

    @Override
    protected void initialize() {
        // Pick from the 4 varients
        int variant = (int) (Math.random() * 4) + 1;

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
            default: // Error default
                baseR = 135; baseG = 135; baseB = 135;
                break;
        }

        double[] faceLuminanceFactors = { 1.0, 0.94, 0.88, 0.82, 0.76, 0.70 };
        for (int i = 0; i < 6; i++) {
            double factor = faceLuminanceFactors[i];
            int r = (int) (baseR * factor);
            int g = (int) (baseG * factor);
            int b = (int) (baseB * factor);
            blockShades[i] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);

            int qw = (int) (255 * factor);
            questionShades[i] = String.format("\u001B[38;2;%d;%d;%dm", qw, qw, qw);
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
            // ==================== BACK-FACE CULLING ====================
            double nx = 0, ny = 0, nz = 0;
            switch (face) {
                case 0: nz = -1; break;
                case 1: nz = 1;  break;
                case 2: ny = -1; break;
                case 3: ny = 1;  break;
                case 4: nx = -1; break;
                case 5: nx = 1;  break;
            }

            double nz3 = -nx * sinY + (ny * sinX + nz * cosX) * cosY;
            if (nz3 > 0) {
                continue; 
            }
            // ===========================================================

            char shadeChar = FACE_SHADES[face];

            for (double u = 0; u <= 1.0; u += 0.02) {
                for (double v = 0; v <= 1.0; v += 0.02) {
                    
                    double uc = 2.0 * u - 1.0;
                    double vc = 2.0 * v - 1.0;
                    double x = 0, y = 0, z = 0;

                    switch (face) {
                        case 0: x = uc; y = vc; z = -1; break;
                        case 1: x = uc; y = vc; z = 1;  break;
                        case 2: x = uc; y = -1; z = vc; break;
                        case 3: x = uc; y = 1;  z = vc; break;
                        case 4: x = -1; y = uc; z = vc; break;
                        case 5: x = 1;  y = uc; z = vc; break;
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
                    double distanceToCamera = 3.25;
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

                            double texU = u;
                            double texV = v;
                            boolean includeQuestionMark = false;

                            switch (face) {
                                case 0:
                                    includeQuestionMark = true;
                                    break;
                                case 1:
                                    texU = 1.0 - u;
                                    includeQuestionMark = true;
                                    break;
                                case 4:
                                    texU = 1.0 - v;
                                    texV = u;
                                    includeQuestionMark = true;
                                    break;
                                case 5:
                                    texU = v;
                                    texV = u;
                                    includeQuestionMark = true;
                                    break;
                                case 2:
                                case 3:
                                    includeQuestionMark = false;
                                    break;
                            }

                            // Determine what type of texture pixel sits at this coordinate
                            int textureType = getTexturePixelType(texU, texV, includeQuestionMark);
                            
                            if (textureType == 1) { // Question Mark
                                activeColor = questionShades[face];
                                renderChar = QUESTION_CHAR;
                            } else if (textureType == 2) { // Borders / Screws
                                activeColor = questionShades[face];
                                renderChar = CHECKER_CHAR;
                            }

                            outputBuffer[index] = activeColor + renderChar + RESET;
                        }
                    }
                }
            }
        }
        angle += 0.04;
    }

    /**
     * Maps coordinates to texture components.
     * @return 0 for background, 1 for central question mark, 2 for border/rivets
     */
    private int getTexturePixelType(double u, double v, boolean includeQuestionMark) {
        // 1. --- CENTRAL QUESTION MARK TEXTURE ---
        if (includeQuestionMark) {
            if (v >= 0.22 && v <= 0.32 && u >= 0.30 && u <= 0.70) return 1;
            if (u >= 0.60 && u <= 0.70 && v >= 0.32 && v <= 0.52) return 1;
            if (v >= 0.45 && v <= 0.55 && u >= 0.45 && u <= 0.65) return 1;
            if (u >= 0.45 && u <= 0.55 && v >= 0.55 && v <= 0.68) return 1;
            if (u >= 0.45 && u <= 0.55 && v >= 0.76 && v <= 0.86) return 1;
        }

        // 2. --- CHECKERBOARD BORDER CHECK ---
        boolean isBorderZone = (u <= 0.06 || u >= 0.94 || v <= 0.06 || v >= 0.94);
        if (isBorderZone) {
            int uCheck = (int) (u * 8);
            int vCheck = (int) (v * 8);
            if ((uCheck + vCheck) % 2 == 0) {
                return 2;
            }
        }

        // 3. --- FOUR CORNER RIVETS/SCREWS ---
        if (u >= 0.09 && u <= 0.13 && v >= 0.09 && v <= 0.13) return 2; // Top-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.09 && v <= 0.13) return 2; // Top-Right
        if (u >= 0.09 && u <= 0.13 && v >= 0.87 && v <= 0.91) return 2; // Bottom-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.87 && v <= 0.91) return 2; // Bottom-Right

        return 0;
    }
}
