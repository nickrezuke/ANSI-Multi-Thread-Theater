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

    // Authentic N64 Color Palette Registers mapped by normal direction
    private static final int[] RGB_RED = { 225, 15, 30 };       // Front (+Z)
    private static final int[] RGB_YELLOW = { 245, 190, 15 };  // Right (+X)
    private static final int[] RGB_BLUE = { 20, 65, 185 };     // Left (-X)
    private static final int[] RGB_GREEN = { 25, 160, 50 };    // Top / Back (+Y / -Z)

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

        // Corrected logo geometry sizing (4x4x4 grid proportions)
        double hSize = 0.65;     
        double legW = 0.325;     // Exactly 1/4 of total width
        
        double outwardOffset = 0.02; 

        // 4 structural components of the logo layout quad faces
        for (int faceIndex = 0; faceIndex < 4; faceIndex++) {
            double faceAngle = 0;

            switch (faceIndex) {
                case 0: faceAngle = 0; break;                  // Front quadrant
                case 1: faceAngle = Math.PI / 2.0; break;      // Left quadrant
                case 2: faceAngle = Math.PI; break;            // Back quadrant
                default: faceAngle = -Math.PI / 2.0; break;    // Right quadrant
            }

            double cosFA = Math.cos(faceAngle), sinFA = Math.sin(faceAngle);

            for (int part = 0; part < 3; part++) {
                double minX = 0, maxX = 0;
                double minZ = -hSize, maxZ = -hSize + legW;

                if (part == 0) { // Left Pillar
                    minX = -hSize; maxX = -hSize + legW;
                } else if (part == 1) { // Right Pillar
                    minX = hSize - legW; maxX = hSize;
                } else if (part == 2) { // Diagonal Crossbeam bounds
                    minX = -hSize + legW; maxX = hSize - legW;
                }

                for (double lx = minX; lx <= maxX; lx += 0.025) {
                    for (double ly = -hSize; ly <= hSize; ly += 0.025) {
                        
                        double targetX = 0;
                        if (part == 2) {
                            double progressY = (hSize - ly) / (hSize * 2.0); 
                            double startX = -hSize + (legW / 2.0);
                            double endX = hSize - (legW / 2.0);
                            
                            targetX = startX + progressY * (endX - startX);
                            if (Math.abs(lx - targetX) > (legW / 2.0)) {
                                continue; 
                            }
                        }

                        for (double lz = minZ; lz <= maxZ; lz += 0.025) {
                            
                            // 1. Calculate local surface normals
                            double nx = 0, ny = 0, nz = 0;
                            if (Math.abs(lz - minZ) < 0.02) nz = -1.0;
                            else if (Math.abs(lz - maxZ) < 0.02) nz = 1.0;
                            
                            if (Math.abs(ly - hSize) < 0.02) ny = 1.0;
                            else if (Math.abs(ly - (-hSize)) < 0.02) ny = -1.0;

                            if (part == 0 || part == 1) {
                                if (Math.abs(lx - minX) < 0.02) nx = -1.0;
                                else if (Math.abs(lx - maxX) < 0.02) nx = 1.0;
                            } else if (part == 2) {
                                if (lx <= targetX - (legW / 2.0) + 0.02) nx = -1.0;
                                else if (lx >= targetX + (legW / 2.0) - 0.02) nx = 1.0;
                            }
                            
                            if (nx == 0 && ny == 0 && nz == 0) continue; 
                            
                            double mag = Math.sqrt(nx*nx + ny*ny + nz*nz);
                            nx /= mag; ny /= mag; nz /= mag;

                            // 2. Geometry offsets
                            double ox = lx;
                            double oy = ly;
                            double oz = lz - outwardOffset; 

                            if (part == 0) oy += 0.005; 
                            if (part == 1) oy -= 0.005; 

                            // 3. Rotate to box quadrant (Model Space)
                            double fx = ox * cosFA + oz * sinFA;
                            double fy = oy;
                            double fz = -ox * sinFA + oz * cosFA;

                            double rnx = nx * cosFA + nz * sinFA;
                            double rny = ny;
                            double rnz = -nx * sinFA + nz * cosFA;

                            // 4. Determine Face Color based on Model-Space Normal (Locks color to the object geometry)
                            int[] baseRGB;
                            double absRNX = Math.abs(rnx);
                            double absRNY = Math.abs(rny);
                            double absRNZ = Math.abs(rnz);

                            if (absRNZ >= absRNX && absRNZ >= absRNY) {
                                baseRGB = (rnz > 0) ? RGB_RED : RGB_GREEN; 
                            } else if (absRNX >= absRNY && absRNX >= absRNZ) {
                                baseRGB = (rnx > 0) ? RGB_YELLOW : RGB_BLUE; 
                            } else {
                                baseRGB = (rny > 0) ? RGB_GREEN : RGB_BLUE; 
                            }

                            // 5. Spin Yaw (Global Rotation)
                            double x1 = fx * cosY + fz * sinY;
                            double y1 = fy;
                            double z1 = -fx * sinY + fz * cosY;

                            double nx1 = rnx * cosY + rnz * sinY;
                            double ny1 = rny;
                            double nz1 = -rnx * sinY + rnz * cosY;

                            // 6. Camera Pitch
                            double finalX = x1;
                            double finalY = y1 * cosP + z1 * sinP;
                            double finalZ = -y1 * sinP + z1 * cosP;

                            double worldNx = nx1;
                            double worldNy = ny1 * cosP + nz1 * sinP;
                            double worldNz = -ny1 * sinP + nz1 * cosP;

                            // 7. Target Perspective Projection
                            double ooz = 1.0 / (finalZ + CAMERA_DISTANCE);
                            int xp = (int) (40 + 44 * ooz * finalX * 2.6);
                            int yp = (int) (11 - 19 * ooz * finalY * 1.8);

                            if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
                                int idx = xp + width * yp;
                                if (ooz > zBuffer[idx] + 0.0001) {
                                    zBuffer[idx] = ooz;

                                    double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
                                    double shade = 0.50 + 0.50 * Math.max(0.0, luminance);

                                    int r = (int) (baseRGB[0] * shade);
                                    int g = (int) (baseRGB[1] * shade);
                                    int b = (int) (baseRGB[2] * shade);

                                    r = r < 0 ? 0 : (r > 255 ? 255 : r);
                                    g = g < 0 ? 0 : (g > 255 ? 255 : g);
                                    b = b < 0 ? 0 : (b > 255 ? 255 : b);

                                    outputBuffer[idx] = "\u001B[38;2;" + r + ";" + g + ";" + b + "m█\u001B[0m";
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
