// TODO: Finish this its in-progress.... maybe make another like it using similar behavior

import java.util.Arrays;

public class ColorBoxLoader extends Loader {

    private static final StatusStage[] FLOWERBOX_STAGES = {
            new StatusStage(10, "Initializing 3D geometric matrix..."),
            new StatusStage(30, "Generating blooming polygon petals..."),
            new StatusStage(50, "Calibrating dynamic folding algorithms..."),
            new StatusStage(80, "Engaging multi-color shader gradients..."),
            new StatusStage(100, "Blooming Flower Box!")
    };

    private int frameTick = 0;
    private final int width;
    private final int height;

    // Palette: Windows 95/98 Retro Flower Box Neon Colors
    private static final String C_PINK    = "\u001B[38;2;255;100;200m";
    private static final String C_PURPLE  = "\u001B[38;2;180;80;255m";
    private static final String C_CYAN    = "\u001B[38;2;0;220;255m";
    private static final String C_YELLOW  = "\u001B[38;2;255;230;50m";
    private static final String C_GREEN   = "\u001B[38;2;50;250;140m";
    private static final String RESET     = "\u001B[0m";

    public ColorBoxLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public ColorBoxLoader() {
        super(FLOWERBOX_STAGES, 120, 30);
        this.width = 120;
        this.height = 30;
    }

    @Override
    protected void initialize() {
        setTargetFps(120);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;

        // Smooth continuous 3D rotation angles (Pitch, Yaw, Roll)
        double pitch = frameTick * 0.012;
        double yaw = frameTick * 0.018;
        double roll = frameTick * 0.008;

        double cP = Math.cos(pitch), sP = Math.sin(pitch);
        double cY = Math.cos(yaw),   sY = Math.sin(yaw);
        double cR = Math.cos(roll),  sR = Math.sin(roll);

        // Directional light source for 3D metallic shading
        double lx = 0.5, ly = -0.7, lz = 0.5;
        double lLen = Math.hypot(lx, Math.hypot(ly, lz));
        lx /= lLen; ly /= lLen; lz /= lLen;

        // Blooming factor: rhythmic expansion/unfolding of the box faces
        double bloomPhase = (Math.sin(frameTick * 0.025) + 1.0) / 2.0; 
        double unfold = 0.4 + bloomPhase * 1.5; // Panel slide distance

        // Gentle floating drift of the whole box in world space
        double driftX = Math.sin(frameTick * 0.015) * 1.5;
        double driftY = Math.cos(frameTick * 0.02) * 1.0;
        double baseZ = 12.0; // Distance from camera

        double boxSize = 2.0;
        double hs = boxSize / 2.0;
        double step = 0.08; 

        // Define the 6 primary faces
        FaceConfig[] faces = {
            new FaceConfig(0, 0, 1, C_PINK),     // Front (+Z)
            new FaceConfig(0, 0, -1, C_PURPLE),  // Back (-Z)
            new FaceConfig(0, 1, 0, C_CYAN),     // Top (+Y)
            new FaceConfig(0, -1, 0, C_YELLOW),  // Bottom (-Y)
            new FaceConfig(1, 0, 0, C_GREEN),    // Right (+X)
            new FaceConfig(-1, 0, 0, C_PINK)     // Left (-X)
        };

        for (FaceConfig fc : faces) {
            // Local face center offset by the bloom expansion distance
            double faceCenterX = fc.nx * (hs + unfold);
            double faceCenterY = fc.ny * (hs + unfold);
            double faceCenterZ = fc.nz * (hs + unfold);

            for (double u = -hs; u <= hs; u += step) {
                for (double v = -hs; v <= hs; v += step) {
                    double localX = faceCenterX;
                    double localY = faceCenterY;
                    double localZ = faceCenterZ;

                    // Map grid coordinates to face plane
                    if (Math.abs(fc.nz) > 0.5) {
                        localX += u; localY += v;
                    } else if (Math.abs(fc.ny) > 0.5) {
                        localX += u; localZ += v;
                    } else {
                        localY += u; localZ += v;
                    }

                    // 1. Rotate local point around box center (0,0,0)
                    // Yaw
                    double x1 = localX * cY + localZ * sY;
                    double y1 = localY;
                    double z1 = -localX * sY + localZ * cY;
                    // Pitch
                    double x2 = x1;
                    double y2 = y1 * cP - z1 * sP;
                    double z2 = y1 * sP + z1 * cP;
                    // Roll
                    double rotX = x2 * cR - y2 * sR;
                    double rotY = x2 * sR + y2 * cR;
                    double rotZ = z2;

                    // Rotate Normal vectors for lighting
                    double nx1 = fc.nx * cY + fc.nz * sY;
                    double ny1 = fc.ny;
                    double nz1 = -fc.nx * sY + fc.nz * cY;
                    double nx2 = nx1;
                    double ny2 = ny1 * cP - nz1 * sP;
                    double nz2 = ny1 * sP + nz1 * cP;
                    double rotNX = nx2 * cR - ny2 * sR;
                    double rotNY = nx2 * sR + ny2 * cR;
                    double rotNZ = nz2;

                    // 2. Translate to world space coordinates
                    double worldX = rotX + driftX;
                    double worldY = rotY + driftY;
                    double worldZ = rotZ + baseZ;

                    if (worldZ <= 0.2) continue;

                    // 3. Perspective projection
                    double D = 1.0 / worldZ;
                    int sx = (int) (width / 2.0 + 75.0 * D * worldX);
                    int sy = (int) (height / 2.0 - 37.0 * D * worldY);

                    if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                        int idx = sx + width * sy;

                        if (D > zBuffer[idx]) {
                            zBuffer[idx] = D;

                            double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                            double illuminance = Math.min(1.0, Math.max(0.15, dot + 0.3));

                            char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                            int rampIdx = (int) (illuminance * (ramp.length - 1));
                            rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                            char glyph = ramp[rampIdx];

                            outputBuffer[idx] = fc.color + glyph + RESET;
                        }
                    }
                }
            }
        }
    }

    private static class FaceConfig {
        double nx, ny, nz;
        String color;
        public FaceConfig(double nx, double ny, double nz, String color) {
            this.nx = nx; this.ny = ny; this.nz = nz; this.color = color;
        }
    }
}