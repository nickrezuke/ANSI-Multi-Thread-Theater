// TODO: Make this more accurate?

import java.util.Arrays;

public class FlowerBoxLoader extends Loader {

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

    public FlowerBoxLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public FlowerBoxLoader() {
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

        // Smooth continuous 3D rotation angles
        double pitch = frameTick * 0.012;
        double yaw = frameTick * 0.018;
        double roll = frameTick * 0.008;

        double cP = Math.cos(pitch), sP = Math.sin(pitch);
        double cY = Math.cos(yaw),   sY = Math.sin(yaw);
        double cR = Math.cos(roll),  sR = Math.sin(roll);

        // Directional light source
        double lx = 0.5, ly = -0.7, lz = 0.5;
        double lLen = Math.hypot(lx, Math.hypot(ly, lz));
        lx /= lLen; ly /= lLen; lz /= lLen;

        // Classic Flower Box Morphing Cycles
        // Cycle drives the loop: Cube -> Bubbled Flower -> Cube -> Spiky Star -> Cube
        double cycle = frameTick * 0.015;
        double blend = (1.0 - Math.cos(cycle * 2.0)) / 2.0; // Pulses 0.0 to 1.0 (0 = Cube, 1 = Morphed)
        double shape = Math.sin(cycle);                     // -1.0 to +1.0 dictates Star vs Flower

        // Drift and Camera
        double driftX = Math.sin(frameTick * 0.015) * 1.5;
        double driftY = Math.cos(frameTick * 0.02) * 1.0;
        double baseZ = 12.0; 

        double boxSize = 3.0; // Slightly larger base box
        double hs = boxSize / 2.0;
        double step = 0.05;   // Denser step size so curved surfaces don't have gaps

        // Define the 6 primary faces
        FaceConfig[] faces = {
            new FaceConfig(0, 0, 1, C_PINK),     
            new FaceConfig(0, 0, -1, C_PURPLE),  
            new FaceConfig(0, 1, 0, C_CYAN),     
            new FaceConfig(0, -1, 0, C_YELLOW),  
            new FaceConfig(1, 0, 0, C_GREEN),    
            new FaceConfig(-1, 0, 0, C_PINK)     
        };

        for (FaceConfig fc : faces) {
            // Keep the base centers strictly at the edges of the cube (no unfolding)
            double faceCenterX = fc.nx * hs;
            double faceCenterY = fc.ny * hs;
            double faceCenterZ = fc.nz * hs;

            for (double u = -hs; u <= hs; u += step) {
                for (double v = -hs; v <= hs; v += step) {
                    double localX = faceCenterX;
                    double localY = faceCenterY;
                    double localZ = faceCenterZ;

                    if (Math.abs(fc.nz) > 0.5) {
                        localX += u; localY += v;
                    } else if (Math.abs(fc.ny) > 0.5) {
                        localX += u; localZ += v;
                    } else {
                        localY += u; localZ += v;
                    }

                    // --- MORPHING LOGIC ---
                    // Calculate distance and normalized vector from the center of the box
                    double len = Math.sqrt(localX*localX + localY*localY + localZ*localZ);
                    double dirX = localX / len;
                    double dirY = localY / len;
                    double dirZ = localZ / len;

                    // 'mag' is highest (1.0) at the center of a face, and lowest (~0.33) at the cube corners.
                    double mag = (dirX*dirX*dirX*dirX) + (dirY*dirY*dirY*dirY) + (dirZ*dirZ*dirZ*dirZ);
                    
                    // Create the flower/star target shape
                    // If shape > 0: Centers push out, corners pull in (Flower)
                    // If shape < 0: Corners push out, centers pull in (Star)
                    double targetR = hs * (1.2 + shape * (mag - 0.6) * 1.8);
                    
                    // Interpolate between the rigid Cube coordinate and the morphed shape
                    double morphedX = localX + ((dirX * targetR) - localX) * blend;
                    double morphedY = localY + ((dirY * targetR) - localY) * blend;
                    double morphedZ = localZ + ((dirZ * targetR) - localZ) * blend;

                    // Blend the normals for lighting. When it's a cube, it has flat sharp shading. 
                    // When it morphs, the normals interpolate towards a sphere for smooth, rounded shading.
                    double blendNormX = fc.nx + (dirX - fc.nx) * blend;
                    double blendNormY = fc.ny + (dirY - fc.ny) * blend;
                    double blendNormZ = fc.nz + (dirZ - fc.nz) * blend;
                    
                    double nLen = Math.sqrt(blendNormX*blendNormX + blendNormY*blendNormY + blendNormZ*blendNormZ);
                    blendNormX /= nLen; blendNormY /= nLen; blendNormZ /= nLen;

                    // --- 1. Rotate morphed local point around box center ---
                    // Yaw
                    double x1 = morphedX * cY + morphedZ * sY;
                    double y1 = morphedY;
                    double z1 = -morphedX * sY + morphedZ * cY;
                    // Pitch
                    double x2 = x1;
                    double y2 = y1 * cP - z1 * sP;
                    double z2 = y1 * sP + z1 * cP;
                    // Roll
                    double rotX = x2 * cR - y2 * sR;
                    double rotY = x2 * sR + y2 * cR;
                    double rotZ = z2;

                    // --- Rotate Blended Normals ---
                    double nx1 = blendNormX * cY + blendNormZ * sY;
                    double ny1 = blendNormY;
                    double nz1 = -blendNormX * sY + blendNormZ * cY;
                    
                    double nx2 = nx1;
                    double ny2 = ny1 * cP - nz1 * sP;
                    double nz2 = ny1 * sP + nz1 * cP;
                    
                    double rotNX = nx2 * cR - ny2 * sR;
                    double rotNY = nx2 * sR + ny2 * cR;
                    double rotNZ = nz2;

                    // --- 2. Translate to world space ---
                    double worldX = rotX + driftX;
                    double worldY = rotY + driftY;
                    double worldZ = rotZ + baseZ;

                    if (worldZ <= 0.2) continue;

                    // --- 3. Perspective projection ---
                    double D = 1.0 / worldZ;
                    int sx = (int) (width / 2.0 + 85.0 * D * worldX);
                    int sy = (int) (height / 2.0 - 42.0 * D * worldY);

                    if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                        int idx = sx + width * sy;

                        if (D > zBuffer[idx]) {
                            zBuffer[idx] = D;

                            double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                            double illuminance = Math.min(1.0, Math.max(0.1, dot + 0.35));

                            char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                            int rampIdx = (int) (illuminance * (ramp.length - 1));
                            rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                            
                            outputBuffer[idx] = fc.color + ramp[rampIdx] + RESET;
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