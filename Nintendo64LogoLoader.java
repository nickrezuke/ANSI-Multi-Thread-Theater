// TODO: Fix sizing.  Fix colors.  (Look up original online)

import java.util.Arrays;

public class Nintendo64LogoLoader extends Loader { 
    private static final StatusStage[] N64_STAGES = { 
        new StatusStage(20, "Allocating 16-cube primitive matrices:"), 
        new StatusStage(50, "Splicing primary color face maps:"), 
        new StatusStage(80, "Calculating single-axis spin vectors:"), 
        new StatusStage(100, "Nintendo 64 Logo Core Synchronized!") 
    };

    private double angleY = 0.0; 
    private final int width = 80; 
    private final int height = 22; 
    private static final double CAMERA_DISTANCE = 4.2; 

    // Authentic N64 Color Palette Registers
    private static final int[] RGB_RED = { 235, 30, 40 };      // Front N
    private static final int[] RGB_GREEN = { 30, 175, 50 };    // Right N
    private static final int[] RGB_YELLOW = { 245, 195, 20 };  // Back N
    private static final int[] RGB_BLUE = { 35, 90, 220 };     // Left N

    public Nintendo64LogoLoader() { 
        super(N64_STAGES, 80, 22); 
    }

    @Override 
    protected void initialize() { 
        this.angleY = 0.0; 
    }

    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        Arrays.fill(outputBuffer, " "); 
        
        angleY += 0.022; 
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY); 
        
        double pitch = 0.35; // ~20 degrees downward tilt
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch); 
        
        // Spotlight vector
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408; 

        // Logo geometry sizing
        double hSize = 0.65;     // Half height/width of the bounding frame
        double legW = 0.20;      // Thickness of the individual pillars
        double halfLeg = legW / 2.0;

        // Visual adjustment: Push each N outward from center to ensure crisp separation
        double outwardOffset = 0.02; 

        // Loop over the 4 structural components of the logo
        for (int faceIndex = 0; faceIndex < 4; faceIndex++) {
            int[] baseRGB;
            double faceAngle = 0; // Local rotation to arrange them in a box configuration

            switch (faceIndex) {
                case 0: baseRGB = RGB_RED;    faceAngle = 0; break;                  // Front
                case 1: baseRGB = RGB_GREEN;  faceAngle = Math.PI / 2.0; break;       // Right
                case 2: baseRGB = RGB_YELLOW; faceAngle = Math.PI; break;             // Back
                default: baseRGB = RGB_BLUE;   faceAngle = -Math.PI / 2.0; break;     // Left
            }

            double cosFA = Math.cos(faceAngle), sinFA = Math.sin(faceAngle);

            // Step through each component's local coordinate system space
            // Left Pillar, Right Pillar, and Diagonal Crossbeam
            for (int part = 0; part < 3; part++) {
                double minX, maxX, minZ, maxZ;

                if (part == 0) { // Left Pillar
                    minX = -hSize; maxX = -hSize + legW;
                    minZ = -hSize; maxZ = -hSize + legW;
                } else if (part == 1) { // Right Pillar
                    minX = hSize - legW; maxX = hSize;
                    minZ = -hSize; maxZ = -hSize + legW;
                } else { // Diagonal Crossbeam
                    minX = -hSize + legW; maxX = hSize - legW;
                    minZ = -hSize; maxZ = -hSize + legW;
                }

                // Sample points cleanly within the structural boundaries
                for (double lx = minX; lx <= maxX; lx += 0.025) {
                    for (double ly = -hSize; ly <= hSize; ly += 0.025) {
                        
                        // Enforce the sloped constraint if rendering the diagonal part
                        if (part == 2) {
                            double progress = (lx - (-hSize + legW)) / (hSize * 2.0 - 2.0 * legW);
                            double targetY = hSize - (progress * hSize * 2.0);
                            if (Math.abs(ly - targetY) > 0.16) {
                                continue; // Skip voxels outside the slope line thickness
                            }
                        }

                        for (double lz = minZ; lz <= maxZ; lz += 0.025) {
                            
                            // 1. Shift the primitive geometry outward along its face direction
                            double ox = lx;
                            double oz = lz - outwardOffset; 

                            // 2. Rotate to align component onto its specific box layout quadrant face
                            double fx = ox * cosFA + oz * sinFA;
                            double fy = ly;
                            double fz = -ox * sinFA + oz * cosFA;

                            // Calculate local surface normal markers
                            double nx = 0, ny = 0, nz = -1.0; 
                            if (Math.abs(ly - hSize) < 0.03) ny = 1.0;
                            else if (Math.abs(ly - (-hSize)) < 0.03) ny = -1.0;
                            if (part == 0 && lx < -hSize + 0.02) nx = -1.0;
                            if (part == 1 && lx > hSize - 0.02) nx = 1.0;

                            // Transform normals to match the layout quadrant direction
                            double rnx = nx * cosFA + nz * sinFA;
                            double rny = ny;
                            double rnz = -nx * sinFA + nz * cosFA;

                            // 3. Apply the global rotating spin matrix (Yaw)
                            double x1 = fx * cosY + fz * sinY;
                            double y1 = fy;
                            double z1 = -fx * sinY + fz * cosY;

                            double nx1 = rnx * cosY + rnz * sinY;
                            double ny1 = rny;
                            double nz1 = -rnx * sinY + rnz * cosY;

                            // 4. Apply fixed downward camera tilt matrix (Pitch)
                            double finalX = x1;
                            double finalY = y1 * cosP + z1 * sinP;
                            double finalZ = -y1 * sinP + z1 * cosP;

                            double worldNx = nx1;
                            double worldNy = ny1 * cosP + nz1 * sinP;
                            double worldNz = -ny1 * sinP + nz1 * cosP;

                            // Render target perspective coordinates projection
                            double ooz = 1.0 / (finalZ + CAMERA_DISTANCE);
                            int xp = (int) (40 + 44 * ooz * finalX * 2.6);
                            int yp = (int) (11 - 19 * ooz * finalY * 1.8);

                            if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
                                int idx = xp + width * yp;
                                if (ooz > zBuffer[idx] + 0.0001) {
                                    zBuffer[idx] = ooz;

                                    // Direct surface lighting calculations
                                    double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
                                    double shade = 0.50 + 0.50 * Math.max(0.0, luminance);

                                    int r = (int) (baseRGB[0] * shade);
                                    int g = (int) (baseRGB[1] * shade);
                                    int b = (int) (baseRGB[2] * shade);

                                    r = r < 0 ? 0 : (r > 255 ? 255 : r);
                                    g = g < 0 ? 0 : (g > 255 ? 255 : g);
                                    b = b < 0 ? 0 : (b > 255 ? 255 : b);

                                    outputBuffer[idx] = "\u001B[38;2;" + r + ";" + g + ";" + b + "m█" + RESET;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
