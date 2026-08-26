public class DNALoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(20, "Extracting genome sequence:"),
        new StatusStage(45, "Synthesizing nucleotides:"),
        new StatusStage(70, "Sequencing double helix:"),
        new StatusStage(90, "Replicating structural bonds:"),
        new StatusStage(100, "Genomic Sequence Stabilized!")
    };

    private double A = 0; // Axis Rotation X
    private double C = 0; // Axis Rotation Y

    private final double cameraTilt = Math.PI / 12.0;

    // Lopsided Phase Offset: 2.1 radians creates the realistic Major/Minor Groove asymmetry
    private static final double ASYMMETRIC_GAP = 2.1; 
    private static final double HELIX_RADIUS = 2.4;
    private static final double STRAND_THICKNESS = 0.45;

    // Element Color Palette
    private static final String COLOR_STRAND = "\u001B[38;5;255m"; // Glowing White Backbone
    private static final String COLOR_A = "\u001B[38;5;196m";      // Adenine (Red)
    private static final String COLOR_T = "\u001B[38;5;46m";       // Thymine (Green)
    private static final String COLOR_C = "\u001B[38;5;226m";      // Cytosine (Yellow)
    private static final String COLOR_G = "\u001B[38;5;21m";       // Guanine (Blue)

    public DNALoader() {
        // This uses 80x22 specifically
        super(STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;

        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinC = Math.sin(C), cosC = Math.cos(C);
        
        // Compute camera tilt trigonometric constants once per frame
        double sinTilt = Math.sin(cameraTilt), cosTilt = Math.cos(cameraTilt);

        int stepCount = 0;

        for (double theta = -6.0; theta < 6.0; theta += 0.05) {
            stepCount++;
            double heightY = theta * 1.1; 

            // ==================== PHASE 1: RENDER 3D BACKBONE STRANDS ====================
            for (double phi = 0; phi < 6.28; phi += 0.15) {
                double cosPhi = Math.cos(phi), sinPhi = Math.sin(phi);

                for (int strand = 0; strand <= 1; strand++) {
                    double currentAngle = (strand == 0) ? theta : theta + ASYMMETRIC_GAP;
                    double cosT = Math.cos(currentAngle), sinT = Math.sin(currentAngle);

                    // Base 3D Tube Coordinates
                    double x3d = (HELIX_RADIUS + STRAND_THICKNESS * cosPhi) * cosT;
                    double z3d = (HELIX_RADIUS + STRAND_THICKNESS * cosPhi) * sinT;
                    double y3d = heightY + STRAND_THICKNESS * sinPhi;

                    // Base Normals
                    double nx = cosPhi * cosT;
                    double nz = cosPhi * sinT;
                    double ny = sinPhi;

                    // Step A: Rotate around Y-axis (C) -> Clean horizontal spin
                    double xRotY = x3d * cosC - z3d * sinC;
                    double zRotY = x3d * sinC + z3d * cosC;
                    double yRotY = y3d;

                    double nxRotY = nx * cosC - nz * sinC;
                    double nzRotY = nx * sinC + nz * cosC;
                    double nyRotY = ny;

                    // Step B: Rotate around X-axis (A) -> Forward tilt perspective
                    double xRotX = xRotY;
                    double yRotX = yRotY * cosA - zRotY * sinA;
                    double zRotX = yRotY * sinA + zRotY * cosA;

                    // Step C: Rotate around Z-axis (cameraTilt) -> Global camera head tilt
                    double xRot = xRotX * cosTilt - yRotX * sinTilt;
                    double yRot = xRotX * sinTilt + yRotX * cosTilt;
                    double zRot = zRotX + 12.0; // Pushed deep into view field after tilt matrix

                    double invDepth = 1.0 / zRot;

                    // 2D Screen Projection
                    int screenX = (int) (40 + 45 * invDepth * xRot);
                    int screenY = (int) (11 + 23 * invDepth * yRot);
                    int bufferIdx = screenX + width * screenY;

                    // Illumination vector calculations matching global changes
                    double rotatedNy = nyRotY * cosA - nzRotY * sinA;
                    double rotatedNz = nyRotY * sinA + nzRotY * cosA;
                    // Apply camera tilt matrix transformation properties to normal maps
                    double finalNx = nxRotY * cosTilt - rotatedNy * sinTilt;
                    double finalNy = nxRotY * sinTilt + rotatedNy * cosTilt;
                    
                    double luminance = finalNx * 0.0 + finalNy * 0.7 - rotatedNz * 0.7;

                    if (screenY > 0 && screenY < height && screenX > 0 && screenX < width) {
                        if (invDepth > zBuffer[bufferIdx]) {
                            zBuffer[bufferIdx] = invDepth;

                            int charIndex = (int) Math.round(8 * luminance);
                            if (charIndex < 0) charIndex = 0;
                            String shadeString = ":;=!*#$@▒▓█";
                            char asciiChar = shadeString.charAt(charIndex >= shadeString.length() ? shadeString.length() - 1 : charIndex);

                            outputBuffer[bufferIdx] = COLOR_STRAND + asciiChar + RESET;
                        }
                    }
                }
            }

            // ==================== PHASE 2: RENDER 3D CONNECTING RUNGS ====================
            if (stepCount % 12 == 0) {
                double xStart = HELIX_RADIUS * Math.cos(theta);
                double zStart = HELIX_RADIUS * Math.sin(theta);
                
                double xEnd = HELIX_RADIUS * Math.cos(theta + ASYMMETRIC_GAP);
                double zEnd = HELIX_RADIUS * Math.sin(theta + ASYMMETRIC_GAP);
                
                double yCenter = theta * 1.1;

                for (double k = 0.05; k < 0.95; k += 0.05) {
                    double x3d = xStart + k * (xEnd - xStart);
                    double z3d = zStart + k * (zEnd - zStart);

                    // Step A: Y-Axis Rotation (C)
                    double xRotY = x3d * cosC - z3d * sinC;
                    double zRotY = x3d * sinC + z3d * cosC;

                    // Step B: X-Axis Rotation (A)
                    double xRotX = xRotY;
                    double yRotX = yCenter * cosA - zRotY * sinA;
                    double zRotX = yCenter * sinA + zRotY * cosA;

                    // Step C: Z-Axis Camera Tilt
                    double xRot = xRotX * cosTilt - yRotX * sinTilt;
                    double yRot = xRotX * sinTilt + yRotX * cosTilt;
                    double zRot = zRotX + 12.0;

                    double invDepth = 1.0 / zRot;

                    int screenX = (int) (40 + 45 * invDepth * xRot);
                    int screenY = (int) (11 + 23 * invDepth * yRot);
                    int bufferIdx = screenX + width * screenY;

                    if (screenY > 0 && screenY < height && screenX > 0 && screenX < width) {
                        if (invDepth > zBuffer[bufferIdx]) {
                            zBuffer[bufferIdx] = invDepth;

                            String rungColor;
                            if ((stepCount / 12) % 2 == 0) {
                                rungColor = (k < 0.5) ? COLOR_A : COLOR_T;
                            } else {
                                rungColor = (k < 0.5) ? COLOR_C : COLOR_G;
                            }

                            char rungChar = (invDepth > 0.08) ? '=' : '-';
                            outputBuffer[bufferIdx] = rungColor + rungChar + RESET;
                        }
                    }
                }
            }
        }

        C += 0.08; // Keep corkscrew rotation consistent
        A += Math.cos(C / 9.0 * Math.PI) * 0.01;
    }
}
