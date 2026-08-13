// TODO: Fix the colors.  N color faces should be fully visable from each face.  (Look up original online)

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
    private static final double CAMERA_DISTANCE = 3.9;

    // Canonical N64 Color Palette Registers
    private static final int[] RGB_RED    = { 235, 30, 40 };
    private static final int[] RGB_GREEN  = { 30, 175, 50 };
    private static final int[] RGB_YELLOW = { 245, 195, 20 };
    private static final int[] RGB_BLUE   = { 35, 90, 220 };

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

        // Spin velocity increment tracker
        angleY += 0.022;
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);
        
        // Fixed cinematic downward pitch angle (~20 degrees)
        double pitch = 0.35; 
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch);
        
        // Stationary overhead spotlight
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // Bounding dimensions for the shared multi-faceted outer frame ring
        double halfSize = 0.70; 
        double legHalfW = 0.32; 

        for (double lx = -halfSize; lx <= halfSize; lx += 0.038) {
            for (double ly = -halfSize; ly <= halfSize; ly += 0.038) {
                for (double lz = -halfSize; lz <= halfSize; lz += 0.038) {

                    // 1. CALCULATE INTERIOR CORNER PILLAR SEGMENTS
                    boolean insidePillarFL = (lx >= -halfSize && lx <= -halfSize + legHalfW) && (lz >= -halfSize && lz <= -halfSize + legHalfW);
                    boolean insidePillarFR = (lx >=  halfSize - legHalfW && lx <= halfSize) && (lz >= -halfSize && lz <= -halfSize + legHalfW);
                    boolean insidePillarBR = (lx >=  halfSize - legHalfW && lx <= halfSize) && (lz >=  halfSize - legHalfW && lz <= halfSize);
                    boolean insidePillarBL = (lx >= -halfSize && lx <= -halfSize + legHalfW) && (lz >=  halfSize - legHalfW && lz <= halfSize);
                    
                    boolean insideAnyPillar = insidePillarFL || insidePillarFR || insidePillarBR || insidePillarBL;

                    // 2. CALCULATE 4 FACETED DIAGONAL CROSSBEAMS (CORRECTED SLANTS)
                    
                    // Side A: Front Face Diagonal (Red) - Slopes top-left to bottom-right
                    double progFront = (lx - (-halfSize + legHalfW)) / (halfSize * 2.0 - 2.0 * legHalfW);
                    double descYFront = halfSize - (progFront * halfSize * 2.0);
                    boolean isDiagFront = (lx >= -halfSize + legHalfW && lx <= halfSize - legHalfW) && 
                                          (ly >= descYFront - 0.22 && ly <= descYFront + 0.22) && 
                                          (lz >= -halfSize && lz <= -halfSize + legHalfW);

                    // Side B: Back Face Diagonal (Yellow) - FIXED: Inverted slope to match top-left to bottom-right head-on
                    double progBack = (lx - (-halfSize + legHalfW)) / (halfSize * 2.0 - 2.0 * legHalfW);
                    double descYBack = -halfSize + (progBack * halfSize * 2.0);
                    boolean isDiagBack  = (lx >= -halfSize + legHalfW && lx <= halfSize - legHalfW) && 
                                          (ly >= descYBack - 0.22 && ly <= descYBack + 0.22) && 
                                          (lz >= halfSize - legHalfW && lz <= halfSize);

                    // Side C: Left Face Diagonal (Blue) - Slopes top-left to bottom-right (looking left)
                    double progLeft = (lz - (-halfSize + legHalfW)) / (halfSize * 2.0 - 2.0 * legHalfW);
                    double ascYLeft = -halfSize + (progLeft * halfSize * 2.0);
                    boolean isDiagLeft  = (lz >= -halfSize + legHalfW && lz <= halfSize - legHalfW) && 
                                          (ly >= ascYLeft - 0.22 && ly <= ascYLeft + 0.22) && 
                                          (lx >= -halfSize && lx <= -halfSize + legHalfW);

                    // Side D: Right Face Diagonal (Green) - FIXED: Inverted slope to match top-left to bottom-right head-on
                    double progRight = (lz - (-halfSize + legHalfW)) / (halfSize * 2.0 - 2.0 * legHalfW);
                    double descYRight = halfSize - (progRight * halfSize * 2.0);
                    boolean isDiagRight = (lz >= -halfSize + legHalfW && lz <= halfSize - legHalfW) && 
                                          (ly >= descYRight - 0.22 && ly <= descYRight + 0.22) && 
                                          (lx >= halfSize - legHalfW && lx <= halfSize);

                    boolean insideAnyDiagonal = isDiagFront || isDiagBack || isDiagLeft || isDiagRight;

                    // --- CHRONO MESH RASTERIZATION FILTER ---
                    if (insideAnyPillar || insideAnyDiagonal) {
                        
                        double nx = 0, ny = 0, nz = 0;
                        int[] baseRGB = RGB_RED;

                        // Canonical Color Quadrant Mapping Pass
                        if (lz <= -halfSize + legHalfW) {
                            nz = -1.0; baseRGB = RGB_RED;     // Front = Red
                        } else if (lx >= halfSize - legHalfW) {
                            nx = 1.0;  baseRGB = RGB_GREEN;   // Right = Green
                        } else if (lz >= halfSize - legHalfW) {
                            nz = 1.0;  baseRGB = RGB_YELLOW;  // Back = Yellow
                        } else if (lx <= -halfSize + legHalfW) {
                            nx = -1.0; baseRGB = RGB_BLUE;    // Left = Blue
                        }

                        // Cap off top and bottom surfaces with the signature Yellow plates
                        if (Math.abs(ly - halfSize) < 0.04) { ny = 1.0; baseRGB = RGB_YELLOW; }
                        else if (Math.abs(ly - (-halfSize)) < 0.04) { ny = -1.0; baseRGB = RGB_YELLOW; }

                        // --- 3D ROTATION PROCESSING PIPELINE ---
                        // Pass A: Spin around vertical Y-axis
                        double x1 = lx * cosY + lz * sinY;
                        double y1 = ly;
                        double z1 = -lx * sinY + lz * cosY;

                        double nx1 = nx * cosY + nz * sinY;
                        double ny1 = ny;
                        double nz1 = -nx * sinY + nz * cosY;

                        // Pass B: Fixed downward perspective tilt matrix
                        double finalX = x1;
                        double finalY = y1 * cosP + z1 * sinP;
                        double finalZ = -y1 * sinP + z1 * cosP;

                        double worldNx = nx1;
                        double worldNy = ny1 * cosP + nz1 * sinP;
                        double worldNz = -ny1 * sinP + nz1 * cosP;

                        // Screen space camera projection
                        double ooz = 1.0 / (finalZ + CAMERA_DISTANCE);
                        int xp = (int) (40 + 44 * ooz * finalX * 2.6); 
                        int yp = (int) (11 - 19 * ooz * finalY * 1.8);

                        if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
                            int idx = xp + width * yp;

                            if (ooz > zBuffer[idx] + 0.0001) {
                                zBuffer[idx] = ooz;

                                // Lambertian surface light calculations
                                double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
                                double shade = 0.50 + 0.50 * Math.max(0.0, luminance);

                                int r = (int) (baseRGB[0] * shade);
                                int g = (int) (baseRGB[1] * shade);
                                int b = (int) (baseRGB[2] * shade);

                                String esc = String.format("\u001B[38;2;%d;%d;%dm", 
                                    Math.max(0, Math.min(255, r)), 
                                    Math.max(0, Math.min(255, g)), 
                                    Math.max(0, Math.min(255, b))
                                );
                                outputBuffer[idx] = esc + "█" + RESET;
                            }
                        }
                    }

                }
            }
        }
    }
}
