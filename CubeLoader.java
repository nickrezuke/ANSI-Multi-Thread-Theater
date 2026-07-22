import java.util.Arrays;

public class CubeLoader extends Loader {
    private static final StatusStage[] CUBE_STAGES = {
        new StatusStage(30, "Mining blocks:"),
        new StatusStage(60, "Forging gold:"),
        new StatusStage(90, "Placing ? mark:"),
        new StatusStage(100, "Power-up Ready!")
    };

    // Shading characters for each of the 6 distinct faces
    private static final char[] FACE_SHADES = {'#', 'X', 'O', '=', ':', '.'};
    
    private String blockGold;
    private String questionMarkColor;
    private double angle = 0.0;

    public CubeLoader() {
        super(CUBE_STAGES);
    }

    @Override
    protected void initialize() {
        blockGold = "\u001B[38;5;214m";       // Mario Gold Box Color
        questionMarkColor = "\u001B[38;5;255m"; // Pure White Text
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Multi-axis rotation angles to give a true 3D aesthetic
        double rX = angle * 0.4; // Pitch (Up/Down tilt)
        double rY = angle * 0.7; // Yaw (Horizontal spin)
        double rZ = angle * 0.2; // Roll (Bank)

        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);
        double cosZ = Math.cos(rZ), sinZ = Math.sin(rZ);

        // Loop through all 6 faces of the cube
        for (int face = 0; face < 6; face++) {
            char shadeChar = FACE_SHADES[face];

            // Plot points across the surface grid of the current face
            for (double u = 0; u <= 1.0; u += 0.02) {
                for (double v = 0; v <= 1.0; v += 0.02) {
                    
                    // Convert normalized (0 to 1) coordinates to spatial dimensions (-1 to 1)
                    double uc = 2.0 * u - 1.0;
                    double vc = 2.0 * v - 1.0;

                    double x = 0, y = 0, z = 0;

                    // Mathematically position the 6 perfect square planes of the cube
                    switch (face) {
                        case 0: x = uc; y = vc; z = -1; break; // Back Face
                        case 1: x = uc; y = vc; z = 1;  break; // Front Face (With Texture)
                        case 2: x = uc; y = -1; z = vc; break; // Bottom Face
                        case 3: x = uc; y = 1;  z = vc; break; // Top Face
                        case 4: x = -1; y = uc; z = vc; break; // Left Face
                        case 5: x = 1;  y = uc; z = vc; break; // Right Face
                    }

                    // --- 3D ROTATION ---
                    // 1. Rotate around X-axis
                    double y1 = y * cosX - z * sinX;
                    double z1 = y * sinX + z * cosX;
                    double x1 = x;

                    // 2. Rotate around Y-axis
                    double x2 = x1 * cosY + z1 * sinY;
                    double z2 = -x1 * sinY + z1 * cosY;
                    double y2 = y1;

                    // 3. Rotate around Z-axis
                    double x3 = x2 * cosZ - y2 * sinZ;
                    double y3 = x2 * sinZ + y2 * cosZ;
                    double z3 = z2;

                    // --- PERSPECTIVE PROJECTION ---
                    double distanceToCamera = 4.5; 
                    double ooz = 1.0 / (z3 + distanceToCamera); // One Over Z (Depth)

                    // Project onto 80x22 console screen layout
                    // Multiplied by 2.2 to compensate for rectangular terminal font aspect ratio
                    int xp = (int) (40 + 40 * ooz * x3 * 2.2);
                    int yp = (int) (11 + 18 * ooz * y3);

                    // --- Z-BUFFER RENDERING ---
                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                        int idx = xp + 80 * yp;
                        
                        // Only draw if the pixel is closer to the camera lens than the previous one
                        if (ooz > zBuffer[idx]) {
                            zBuffer[idx] = ooz;
                            String activeColor = blockGold;
                            char renderChar = shadeChar;

                            // Inject texture maps onto the front face container
                            if (face == 1) {
                                if (isQuestionMarkPixel(u, v)) {
                                    activeColor = questionMarkColor;
                                    renderChar = '?';
                                }
                            }

                            outputBuffer[idx] = activeColor + renderChar + RESET;
                        }
                    }
                }
            }
        }
        angle += 0.04; // Smooth out speed increments
    }

    private boolean isQuestionMarkPixel(double u, double v) {
        if (v >= 0.22 && v <= 0.32 && u >= 0.30 && u <= 0.70) return true;
        if (u >= 0.60 && u <= 0.70 && v >= 0.32 && v <= 0.52) return true;
        if (v >= 0.45 && v <= 0.55 && u >= 0.45 && u <= 0.65) return true;
        if (u >= 0.45 && u <= 0.55 && v >= 0.55 && v <= 0.68) return true;
        if (u >= 0.45 && u <= 0.55 && v >= 0.76 && v <= 0.86) return true;
        return false;
    }
}
