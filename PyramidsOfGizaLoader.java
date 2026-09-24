// TODO: Improve the camerawork on this

import java.util.Arrays;

public class PyramidsOfGizaLoader extends Loader {

    private static final StatusStage[] GIZA_STAGES = {
            new StatusStage(10, "Surveying the Giza Plateau..."),
            new StatusStage(30, "Aligning with true north..."),
            new StatusStage(50, "Transporting limestone blocks..."),
            new StatusStage(75, "Capping the Great Pyramid..."),
            new StatusStage(100, "Rendering the ancient wonders.")
    };

    private double angleX;
    private double angleY = 0.0;
    
    private final int width;
    private final int height;

    // Desert sandstone color
    private static final String C_SAND = "\u001B[38;2;225;190;135m";
    private static final String RESET  = "\u001B[0m";

    public PyramidsOfGizaLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public PyramidsOfGizaLoader() {
        super(GIZA_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        // Pitched downward to create an aerial, bird's-eye diagonal perspective
        this.angleX = 0.65; 
        this.angleY = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        // Majestic, steady camera orbit (no vertical bobbing)
        angleY += 0.015; 

        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);
        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);

        // High desert sun directional lighting (coming from above and slightly to the side)
        double lightX = 0.7, lightY = 0.8, lightZ = -0.4;
        double lightMag = Math.sqrt(lightX*lightX + lightY*lightY + lightZ*lightZ);
        lightX /= lightMag; lightY /= lightMag; lightZ /= lightMag;

        // Pyramids are positioned matching the geographical layout of Giza (NE to SW diagonal)
        // 1. Khufu (Great Pyramid) - Northeast (+X, -Z)
        renderPyramid(1.1, 0.0, -1.1, 1.15, 0.73, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
        
        // Khufu's Queens' Pyramids (Three smaller structures to the East)
        renderPyramid(1.85, 0.0, -1.35, 0.15, 0.10, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderPyramid(1.85, 0.0, -1.10, 0.15, 0.10, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderPyramid(1.85, 0.0, -0.85, 0.15, 0.10, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. Khafre - Center
        renderPyramid(0.0, 0.0, 0.0, 1.07, 0.71, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. Menkaure - Southwest (-X, +Z)
        renderPyramid(-1.1, 0.0, 1.1, 0.51, 0.33, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // Menkaure's Queens' Pyramids (Three smaller structures to the South)
        renderPyramid(-0.9, 0.0, 1.55, 0.12, 0.08, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderPyramid(-1.1, 0.0, 1.55, 0.12, 0.08, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderPyramid(-1.3, 0.0, 1.55, 0.12, 0.08, C_SAND, cosY, sinY, cosX, sinX, lightX, lightY, lightZ, outputBuffer, zBuffer);
    }

    private void renderPyramid(double cx, double cy, double cz, double base, double height, String colorCode,
                               double cosY, double sinY, double cosX, double sinX,
                               double lx, double ly, double lz, String[] out, double[] zb) {
        
        double step = 0.012; // Dense step to prevent ASCII gap artifacts on steep angles
        double halfBase = base / 2.0;

        // Pre-calculate slope normal vectors for realistic lighting against the flat triangular faces
        double nLen = Math.hypot(halfBase, height);
        double ny = halfBase / nLen;
        double nxz = height / nLen;

        // Build the pyramid slice-by-slice from the ground up to the tip
        for (double y = 0; y <= height; y += step) {
            double currentHalfWidth = halfBase * (1.0 - (y / height));

            for (double x = -currentHalfWidth; x <= currentHalfWidth; x += step) {
                // Rendering only the perimeter edges creates a visually solid object 
                // from the outside while keeping performance highly optimized.
                
                // Front Face (+Z)
                projectPoint(cx + x, cy + y, cz + currentHalfWidth, 0, ny, nxz, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
                // Back Face (-Z)
                projectPoint(cx + x, cy + y, cz - currentHalfWidth, 0, ny, -nxz, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
                // Right Face (+X)
                projectPoint(cx + currentHalfWidth, cy + y, cz + x, nxz, ny, 0, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
                // Left Face (-X)
                projectPoint(cx - currentHalfWidth, cy + y, cz + x, -nxz, ny, 0, colorCode, cosY, sinY, cosX, sinX, lx, ly, lz, out, zb);
            }
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                              double cosY, double sinY, double cosX, double sinX,
                              double lx, double ly, double lz, String[] out, double[] zb) {
        
        // 1. Yaw Rotation (Spinning around the vertical Y axis)
        double r1x = px * cosY - pz * sinY;
        double r1y = py;
        double r1z = px * sinY + pz * cosY;

        // 2. Pitch Rotation (Tilting the camera DOWN along the X axis)
        double rotX = r1x;
        double rotY = r1y * cosX + r1z * sinX;
        double rotZ = -r1y * sinX + r1z * cosX;

        // 3. Rotate Normal Vectors (Ensures light dynamically strikes the faces as the earth turns)
        double n1x = nx * cosY - nz * sinY;
        double n1y = ny;
        double n1z = nx * sinY + nz * cosY;

        double rotNX = n1x;
        double rotNY = n1y * cosX + n1z * sinX;
        double rotNZ = -n1y * sinX + n1z * cosX;

        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen;
        }

        // 4. Camera Projection & Depth calculation
        double cameraDepth = rotZ + 4.2; 
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        // Aspect ratio mapping (Tuned for standard terminal font sizing)
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotY);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                
                // Base ambient shadow floor with directional light added
                double illuminance = Math.min(1.0, Math.max(0.12, dot + 0.15));

                char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];

                out[idx] = colorCode + glyph + RESET;
            }
        }
    }
}