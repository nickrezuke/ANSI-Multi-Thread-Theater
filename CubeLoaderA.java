public class CubeLoaderA extends Loader {
    // This Cube Creates a Super Mario Bros. style "?" Block, with a question mark
    // on the front faces and a checkerboard border around all faces.
    private static final StatusStage[] CUBE_STAGES = {
            new StatusStage(16, "Starting Level:"),
            new StatusStage(30, "Stomping Enemies:"),
            new StatusStage(42, "Kicking Shells:"),
            new StatusStage(65, "Collecting Coins:"),
            new StatusStage(85, "Utilizing Power-Ups:"),
            new StatusStage(96, "Sliding Down Flagpole:"),
            new StatusStage(100, "Level Complete!:")
    };

    // Shading characters for each of the 6 distinct faces (Light to Dark)
    private static final char[] FACE_SHADES = { 'X', 'O', '+', '=', ';', ':' };

    // Custom characters for the special textures
    private static final char QUESTION_CHAR = '?';
    private static final char CHECKER_CHAR = '&';

    // Arrays to store RGB strings for each face
    private final String[] blockShades = new String[6];
    private final String[] questionShades = new String[6];
    private double angle = 0.0;

    public CubeLoaderA() {
        super(CUBE_STAGES);
    }

    @Override
    protected void initialize() {
        // Pick from the 4 variants of colors for block
        // types and assign R G B values accordingly
        int baseR, baseG, baseB;
        switch ((int) (Math.random() * 4) + 1) {
            case 1: // --- BLUE VANISH CAP BLOCK ---
                baseR = 40;
                baseG = 100;
                baseB = 230;
                break;
            case 2: // --- RED WING CAP BLOCK ---
                baseR = 240;
                baseG = 40;
                baseB = 40;
                break;
            case 3: // --- GREEN METAL CAP BLOCK ---
                baseR = 30;
                baseG = 180;
                baseB = 60;
                break;
            case 4: // --- THE DEFAULT GOLD ITEM BLOCK ---
            default:
                baseR = 255;
                baseG = 180;
                baseB = 30;
                break;
        }

        // Luminance values for the faces (for subtle distinction)
        double[] faceLuminanceFactors = { 1.0, 0.95, 0.90, 0.85, 0.82, 0.78 };

        for (int i = 0; i < 6; i++) {
            // Scale each face by its luminance factor
            double factor = faceLuminanceFactors[i];
            int r = (int) (baseR * factor);
            int g = (int) (baseG * factor);
            int b = (int) (baseB * factor);

            // Set the shade to the luminance scaled value
            blockShades[i] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);

            // Also Shade the "?" in the middle
            int qw = (int) (255 * factor);
            questionShades[i] = String.format("\u001B[38;2;%d;%d;%dm", qw, qw, qw);

            // For now at least, I like the look of it without applying shade to the
            // checkerboard edges...
            // (Skip the borderShades... i didn't even define them actually)
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
                    break;
                case 1:
                    nz = 1;
                    break;
                case 2:
                    ny = -1;
                    break;
                case 3:
                    ny = 1;
                    break;
                case 4:
                    nx = -1;
                    break;
                case 5:
                    nx = 1;
                    break;
            }

            double nz3 = -nx * sinY + (ny * sinX + nz * cosX) * cosY;
            if (nz3 > 0) {
                continue;
            }

            char shadeChar = FACE_SHADES[face];

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

                    // Apply the 3D Rotation
                    double y1 = y * cosX - z * sinX;
                    double z1 = y * sinX + z * cosX;
                    double x1 = x;

                    double x2 = x1 * cosY + z1 * sinY;
                    double z2 = -x1 * sinY + z1 * cosY;
                    double y2 = y1;

                    double x3 = x2 * cosZ - y2 * sinZ;
                    double y3 = x2 * sinZ + y2 * cosZ;
                    double z3 = z2;

                    // --- Apply the Perspective Projection ---
                    double distanceToCamera = 3.25;
                    double ooz = 1.0 / (z3 + distanceToCamera);

                    int xp = (int) (40 + 40 * ooz * x3);
                    int yp = (int) (11 + 18 * ooz * y3);

                    // --- Apply the Z-Buffering ---
                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                        int index = xp + 80 * yp;

                        if (ooz > zBuffer[index] + 0.0001) {
                            zBuffer[index] = ooz;

                            String activeColor = blockShades[face];
                            char renderChar = shadeChar;

                            double texU = 0.0;
                            double texV = 0.0;

                            switch (face) {
                                case 0: // Front Face
                                    texU = (x + 1.0) / 2.0;
                                    texV = (y + 1.0) / 2.0;
                                    break;
                                case 1: // Back Face
                                    texU = (1.0 - x) / 2.0;
                                    texV = (y + 1.0) / 2.0;
                                    break;
                                case 2: // Bottom Face
                                    texU = (x + 1.0) / 2.0;
                                    texV = (z + 1.0) / 2.0;
                                    break;
                                case 3: // Top Face
                                    texU = (x + 1.0) / 2.0;
                                    texV = (1.0 - z) / 2.0;
                                    break;
                                case 4: // Left Face
                                    texU = (z + 1.0) / 2.0;
                                    texV = (y + 1.0) / 2.0;
                                    break;
                                case 5: // Right Face
                                    texU = (1.0 - z) / 2.0;
                                    texV = (y + 1.0) / 2.0;
                                    break;
                            }

                            // Determine what type of texture pixel sits at this coordinate
                            int textureType = getTexturePixelType(texU, texV, face);

                            if (textureType == 1) { // Question Mark
                                activeColor = questionShades[face];
                                renderChar = QUESTION_CHAR;
                            } else if (textureType == 2) { // Checker Borders
                                activeColor = questionShades[face];
                                renderChar = CHECKER_CHAR;
                            }

                            outputBuffer[index] = activeColor + renderChar + RESET;
                        }
                    }
                }
            }
        }

        // Increment rotation for the next render frame
        angle += 0.025;
    }

    // returns 0 for background, 1 for central question mark, 2 for border/rivets
    private int getTexturePixelType(double u, double v, int face) {
        // Clamp bounds to prevent floating-point edge bleeding
        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        // Only include "?" on the 4 side faces, not the top or bottom faces
        if (face != 3 && face != 2) {
            // Flip the question mark horizontally if it's on the Left (4) or Right (5)
            // faces
            // This corrects the mirroring caused by the unified 3D coordinates earlier
            double questionU = (face == 4 || face == 5) ? (1.0 - u) : u;

            if (v >= 0.22 && v <= 0.32 && questionU >= 0.30 && questionU <= 0.70)
                return 1;
            if (questionU >= 0.60 && questionU <= 0.70 && v >= 0.32 && v <= 0.52)
                return 1;
            if (v >= 0.45 && v <= 0.55 && questionU >= 0.45 && questionU <= 0.65)
                return 1;
            if (questionU >= 0.45 && questionU <= 0.55 && v >= 0.55 && v <= 0.68)
                return 1;
            if (questionU >= 0.45 && questionU <= 0.55 && v >= 0.76 && v <= 0.86)
                return 1;
        }

        // Checkerboard outer pattern zone
        boolean isBorderZone = (u <= 0.06 || u >= 0.94 || v <= 0.06 || v >= 0.94);
        if (isBorderZone) {
            // Using Math.floor handles floating point rounding smoothly across faces
            int uCheck = (int) Math.floor(u * 8.0);
            int vCheck = (int) Math.floor(v * 8.0);

            // Adjust bounds slightly so the absolute edge pixels match their outer neighbor
            // blocks
            if (uCheck >= 8)
                uCheck = 7;
            if (vCheck >= 8)
                vCheck = 7;

            if ((uCheck + vCheck) % 2 == 0) {
                return 2;
            }
        }

        // Four Corners (Rivets)
        if (u >= 0.09 && u <= 0.13 && v >= 0.09 && v <= 0.13)
            return 2; // Top-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.09 && v <= 0.13)
            return 2; // Top-Right
        if (u >= 0.09 && u <= 0.13 && v >= 0.87 && v <= 0.91)
            return 2; // Bottom-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.87 && v <= 0.91)
            return 2; // Bottom-Right

        // TODO: If you look closely, the bottom left corner of the cube's faces has a
        // random "&", its probably a floating point artifact or something. FIX THAT!

        // &&&&&XXXX&&&&&XXXX&&&&&XXXX&&&&XXXXX
        // &&&XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX&&&
        // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX&&&
        // &&&XXXXXXXX??????????????XXXXXXXXXXX
        // &&&XXXXXXXX??????????????XXXXXXXXXXX
        // XXXXXXXXXXXXXXXXXXXXX????XXXXXXXX&&&
        // XXXXXXXXXXXXXXXXXXXXX????XXXXXXXX&&&
        // &&&XXXXXXXXXXXXX?????????XXXXXXXXXXX
        // &&&XXXXXXXXXXXXX????XXXXXXXXXXXXXXXX
        // XXXXXXXXXXXXXXXX????XXXXXXXXXXXXX&&&
        // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX&&&
        // &&&XXXXXXXXXXXXX????XXXXXXXXXXXXXXXX
        // &&&XXXXXXXXXXXXX????XXXXXXXXXXXXXXXX
        // XXXX&XXXXXXXXXXXXXXXXXXXXXXXXXX&&&&& <--- 5th char here....
        // XXXXX&&&&XXXXX&&&&XXXXX&&&&XXXX&&&&&

        return 0;
    }

}