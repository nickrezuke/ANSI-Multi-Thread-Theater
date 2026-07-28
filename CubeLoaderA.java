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

    // Uniform block primitives to guarantee a solid 16-bit voxel texture scale
    private static final char FULL_BLOCK   = '\u2588'; // █ (Solid Core)
    private static final char DENSE_BLOCK  = '\u2593'; // ▓ (Heavy Shading)
    private static final char RIVET_CHAR   = '\u2022'; // • (Dark Recessed Screw Anchor)

    // Base color parameters stored globally across initialization paths
    private int baseR, baseG, baseB;
    private double angle = 0.0;

    public CubeLoaderA() {
        super(CUBE_STAGES);
    }

    @Override
    protected void initialize() {
        // Deterministically seed the color theme once per generation cycle
        switch ((int) (Math.random() * 4) + 1) {
            case 1: // --- BLUE VANISH CAP BOX ---
                baseR = 40;  baseG = 100; baseB = 230; break;
            case 2: // --- RED WING CAP BLOCK ---
                baseR = 240; baseG = 40;  baseB = 40;  break;
            case 3: // --- GREEN METAL CAP BLOCK ---
                baseR = 30;  baseG = 180; baseB = 60;  break;
            case 4: // --- DEFAULT GOLD ITEM BLOCK ---
            default:
                baseR = 245; baseG = 165; baseB = 20;  break; // Standard Mario 64 Item Gold
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

        // Precompute face lighting luminance modifiers inside the runtime loops
        double[] faceLuminance = { 1.0, 0.92, 0.85, 0.78, 0.72, 0.65 };

        for (int face = 0; face < 6; face++) {
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

            double factor = faceLuminance[face];

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

                    double y1 = y * cosX - z * sinX;
                    double z1 = y * sinX + z * cosX;
                    double x2 = x * cosY + z1 * sinY;
                    double z2 = -x * sinY + z1 * cosY;
                    double x3 = x2 * cosZ - y1 * sinZ;
                    double y3 = x2 * sinZ + y1 * cosZ;

                    double distanceToCamera = 3.25;
                    double ooz = 1.0 / (z2 + distanceToCamera);

                    int xp = (int) (40 + 40 * ooz * x3);
                    int yp = (int) (11 - 18 * ooz * y3);

                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                        int index = xp + 80 * yp;
                        if (ooz > zBuffer[index] + 0.0001) {
                            zBuffer[index] = ooz;

                            double texU = 0.0;
                            double texV = 0.0;
                            switch (face) {
                                case 0: texU = (x + 1.0) / 2.0; texV = (y + 1.0) / 2.0; break;
                                case 1: texU = (1.0 - x) / 2.0; texV = (y + 1.0) / 2.0; break;
                                case 2: texU = (x + 1.0) / 2.0; texV = (z + 1.0) / 2.0; break;
                                case 3: texU = (x + 1.0) / 2.0; texV = (1.0 - z) / 2.0; break;
                                case 4: texU = (z + 1.0) / 2.0; texV = (y + 1.0) / 2.0; break;
                                case 5: texU = (1.0 - z) / 2.0; texV = (y + 1.0) / 2.0; break;
                            }

                            int textureType = getTexturePixelType(texU, texV, face);
                            
                            int r = 0, g = 0, b = 0;
                            char renderChar = DENSE_BLOCK; // Base default plate texture

                            if (textureType == 1 || textureType == 2) {
                                // Question Mark Emblem & Checkerboard Border (Pure Stark Ceramic White)
                                r = (int) (245 * factor);
                                g = (int) (245 * factor);
                                b = (int) (245 * factor);
                                renderChar = FULL_BLOCK;
                            } else if (textureType == 3) {
                                // Anchored Rivets (Deep dark recessed drop-shadow accent)
                                r = (int) (50 * factor);
                                g = (int) (35 * factor);
                                b = (int) (20 * factor);
                                renderChar = RIVET_CHAR;
                            } else {
                                // Main Casing Outer Shell Face Plates
                                r = (int) (baseR * factor);
                                g = (int) (baseG * factor);
                                b = (int) (baseB * factor);
                                renderChar = DENSE_BLOCK;
                            }

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                            outputBuffer[index] = colorCode + renderChar + RESET;
                        }
                    }
                }
            }
        }
        angle += 0.025;
    }

    // 0 = background plate, 1 = question mark, 2 = white borders, 3 = screw anchors
    private int getTexturePixelType(double u, double v, int face) {
        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        if (face != 3 && face != 2) {
            // Horizontal correction matrix
            double questionU = (face == 5 || face == 4) ? (1.0 - u) : u;
            // Vertical correction matrix preventing frame-1 headstands
            double questionV = 1.0 - v; 
            
            if (questionV >= 0.22 && questionV <= 0.32 && questionU >= 0.30 && questionU <= 0.70) return 1;
            if (questionU >= 0.60 && questionU <= 0.70 && questionV >= 0.32 && questionV <= 0.52) return 1;
            if (questionV >= 0.45 && questionV <= 0.55 && questionU >= 0.45 && questionU <= 0.65) return 1;
            if (questionU >= 0.45 && questionU <= 0.55 && questionV >= 0.55 && questionV <= 0.68) return 1;
            if (questionU >= 0.45 && questionU <= 0.55 && questionV >= 0.76 && questionV <= 0.86) return 1;
        }

        // Checkerboard perimeter framework
        boolean isBorderZone = (u <= 0.06 || u >= 0.94 || v <= 0.06 || v >= 0.94);
        if (isBorderZone) {
            int uCheck = (int) Math.floor(u * 8.0);
            int vCheck = (int) Math.floor(v * 8.0);
            if (uCheck >= 8) uCheck = 7;
            if (vCheck >= 8) vCheck = 7;
            if ((uCheck + vCheck) % 2 == 0) {
                return 2;
            }
        }

        // Dedicated four corner rivet indicators 
        if (u >= 0.09 && u <= 0.13 && v >= 0.09 && v <= 0.13) return 3; // Top-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.09 && v <= 0.13) return 3; // Top-Right
        if (u >= 0.09 && u <= 0.13 && v >= 0.87 && v <= 0.91) return 3; // Bottom-Left
        if (u >= 0.87 && u <= 0.91 && v >= 0.87 && v <= 0.91) return 3; // Bottom-Right

        return 0;
    }
}
