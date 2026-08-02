public class OctahedralMatrixLoader extends Loader { 
    private static final StatusStage[] OCTAHEDRAL_STAGES = { 
        new StatusStage(25, "Assembling diamond vertex array:"), 
        new StatusStage(50, "Weaving diagonal line lattices:"), 
        new StatusStage(75, "Interpolating internal amber glow:"), 
        new StatusStage(100, "Dual-Axis Core Engaged!") 
    }; 

    // Refined architectural typography symbols for the base panels and lines 
    private static final char CH_LINE = '\u2593'; // ▓ Heavy texture for bright structural lines 
    private static final char CH_BASE = '\u2591'; // ░ Light mesh texture for glowing panel skin 

    // Dual-axis angular velocity tracking parameters 
    private double rotationY = 0.0; 
    private double rotationX = 0.0; 
    private static final double CAMERA_DISTANCE = 3.5; 

    public OctahedralMatrixLoader() { 
        super(OCTAHEDRAL_STAGES); 
    } 

    @Override 
    protected void initialize() { } 

    // Helper class to sort faces dynamically by their camera-space depth (Painter's Algorithm) 
    private static class SortedFace { 
        int faceIndex; 
        double avgZ; 
        SortedFace(int faceIndex, double avgZ) { 
            this.faceIndex = faceIndex; 
            this.avgZ = avgZ; 
        } 
    } 

    /** 
     * Converts HSV color space to RGB components. 
     * Hue sweeps from 0.0 to 1.0 around the full color spectrum wheel. 
     */ 
    private int[] hsvToRgb(double h, double s, double v) { 
        int r = 0, g = 0, b = 0; 
        int i = (int) (h * 6); 
        double f = h * 6 - i; 
        double p = v * (1 - s); 
        double q = v * (1 - f * s); 
        double t = v * (1 - (1 - f) * s); 
        switch (i % 6) { 
            case 0: r = (int)(v*255); g = (int)(t*255); b = (int)(p*255); break; 
            case 1: r = (int)(q*255); g = (int)(v*255); b = (int)(p*255); break; 
            case 2: r = (int)(p*255); g = (int)(v*255); b = (int)(t*255); break; 
            case 3: r = (int)(p*255); g = (int)(q*255); b = (int)(v*255); break; 
            case 4: r = (int)(t*255); g = (int)(p*255); b = (int)(v*255); break; 
            case 5: r = (int)(v*255); g = (int)(p*255); b = (int)(q*255); break; 
        } 
        return new int[]{r, g, b}; 
    } 

    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        // Advance independent axis speeds 
        rotationY += 0.018; 
        rotationX += 0.011; 
        double cosY = Math.cos(rotationY), sinY = Math.sin(rotationY); 
        double cosX = Math.cos(rotationX), sinX = Math.sin(rotationX); 

        // Gentle background illumination loop modifier 
        double lightingPulse = 0.75 + 0.25 * Math.sin(rotationY * 1.5); 

        // 1. Calculate spectrum Hues scaled dynamically between 0.0 and 1.0 
        // Core color maps directly to rotation. Frame color shifts exactly 180 degrees (0.5 offset) 
        double coreHue = (rotationY * 0.25) % 1.0; 
        if (coreHue < 0) coreHue += 1.0; 
        double frameHue = (coreHue + 0.5) % 1.0; 

        // Sample base RGB palette states from the spectrum hues 
        int[] coreColor = hsvToRgb(coreHue, 0.9, 1.0); 
        int[] frameColor = hsvToRgb(frameHue, 0.9, 0.9); 

        // STEP 1: DEFINE THE 8-SIDED OCTAHEDRON GEOMETRY 
        double[][] vertices = { 
            { 0.0, -1.2, 0.0 },  // 0: Top Apex 
            { 0.0, 1.2, 0.0 },   // 1: Bottom Apex 
            { -1.1, 0.0, -1.1 }, // 2: Middle Front-Left 
            { 1.1, 0.0, -1.1 },  // 3: Middle Front-Right 
            { 1.1, 0.0, 1.1 },   // 4: Middle Back-Right 
            { -1.1, 0.0, 1.1 }   // 5: Middle Back-Left 
        }; 

        int[][] faces = { 
            { 0, 2, 3 }, { 0, 3, 4 }, { 0, 4, 5 }, { 0, 5, 2 }, // Top half shell 
            { 1, 3, 2 }, { 1, 4, 3 }, { 1, 5, 4 }, { 1, 2, 5 }  // Bottom half shell 
        }; 

        // STEP 2: PRE-SORT ALL 8 TRIANGULAR FACES BY CAM DEPTH FOR ALPHA BLENDING 
        SortedFace[] sortedFaces = new SortedFace[8]; 
        for (int i = 0; i < faces.length; i++) { 
            int[] faceVertices = faces[i]; 
            double zSum = 0.0; 
            for (int vIdx : faceVertices) { 
                double xl = vertices[vIdx][0]; 
                double yl = vertices[vIdx][1]; 
                double zl = vertices[vIdx][2]; 
                double z_rotY = -xl * sinY + zl * cosY; 
                // Tilt around X axis 
                double rz = yl * sinX + z_rotY * cosX; 
                zSum += rz; 
            } 
            sortedFaces[i] = new SortedFace(i, zSum / 3.0); 
        } 

        // Depth sort back-to-front so transparent mesh segments compile correctly over each other 
        for (int i = 0; i < sortedFaces.length - 1; i++) { 
            for (int j = 0; j < sortedFaces.length - i - 1; j++) { 
                if (sortedFaces[j].avgZ < sortedFaces[j + 1].avgZ) { 
                    var temp = sortedFaces[j]; 
                    sortedFaces[j] = sortedFaces[j + 1]; 
                    sortedFaces[j + 1] = temp; 
                } 
            } 
        } 

        // STEP 3: RENDER THE MATRIX SURFACE WITH INTERIOR GLOW INDICES 
        for (int f = 0; f < sortedFaces.length; f++) { 
            int activeFaceIndex = sortedFaces[f].faceIndex; 
            int[] faceVertices = faces[activeFaceIndex]; 
            double[] v0 = vertices[faceVertices[0]]; 
            double[] v1 = vertices[faceVertices[1]]; 
            double[] v2 = vertices[faceVertices[2]]; 

            // High precision scan line raster step loop across triangle polygons 
            for (double u = 0; u <= 1.0; u += 0.012) { 
                for (double v = 0; v <= 1.0 - u; v += 0.012) { 
                    double w = 1.0 - u - v; 

                    // Generate localized UV coordinate spaces across the face maps 
                    double texU = v / (1.0 - u + 0.0001); 
                    double texV = 1.0 - u; 

                    // Grid wireframe structural thickness controls 
                    double densityScale = 3.5; 
                    double diag1 = (texU * densityScale) + (texV * densityScale); 
                    double diag2 = (texU * densityScale) - (texV * densityScale); 

                    boolean isDiagStrut1 = Math.abs(diag1 - Math.round(diag1)) < 0.14; 
                    boolean isDiagStrut2 = Math.abs(diag2 - Math.round(diag2)) < 0.14; 

                    char renderChar = CH_BASE; 
                    double brightnessFactor = 0.40; 

                    if (isDiagStrut1 || isDiagStrut2) { 
                        renderChar = CH_LINE; 
                        brightnessFactor = 0.80; 
                    } 

                    // Interpolate vector points inside local space 
                    double x = u * v0[0] + v * v1[0] + w * v2[0]; 
                    double y = u * v0[1] + v * v1[1] + w * v2[1]; 
                    double z = u * v0[2] + v * v1[2] + w * v2[2]; 

                    // Transform Matrix Step 1: Horizontal Spin (Y-Axis) 
                    double x1 = x * cosY + z * sinY; 
                    double y1 = y; 
                    double z1 = -x * sinY + z * cosY; 

                    // Transform Matrix Step 2: Vertical Pitch (X-Axis) 
                    double rx = x1; 
                    double ry = y1 * cosX - z1 * sinX; 
                    double rz = y1 * sinX + z1 * cosX; 

                    // Project 3D Space vector directly to 2D screen coordinates 
                    double ooz = 1.0 / (rz + CAMERA_DISTANCE); 
                    int xp = (int) (40 + 54 * ooz * rx * 1.3); 
                    int yp = (int) (11 + 26 * ooz * ry); 

                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) { 
                        int index = xp + 80 * yp; 

                        // Depth check layer verification override 
                        if (ooz > zBuffer[index] + 0.0001) { 
                            zBuffer[index] = ooz; 

                            // Internal gradient mixing logic 
                            double centerGlowDist = Math.abs(y); 
                            double coreWeight = Math.max(0.0, (1.0 - centerGlowDist)) * lightingPulse; 
                            double frameWeight = 1.0 - coreWeight; 

                            // Mix dynamic HSV-derived spectrum palettes instead of static colors 
                            int r = (int) (((frameColor[0] * frameWeight) + (coreColor[0] * coreWeight)) * brightnessFactor); 
                            int g = (int) (((frameColor[1] * frameWeight) + (coreColor[1] * coreWeight)) * brightnessFactor); 
                            int b = (int) (((frameColor[2] * frameWeight) + (coreColor[2] * coreWeight)) * brightnessFactor); 

                            r = Math.max(0, Math.min(255, r)); 
                            g = Math.max(0, Math.min(255, g)); 
                            b = Math.max(0, Math.min(255, b)); 

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b); 
                            outputBuffer[index] = colorCode + renderChar + RESET; 
                        } 
                    } 
                } 
            } 
        } 
    } 
}
