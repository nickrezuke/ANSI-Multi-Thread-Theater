public class PacManSphereLoader extends Loader { 
    private static final StatusStage[] REFLECTIVE_STAGES = { 
        new StatusStage(20, "Munching Cherries:"), 
        new StatusStage(50, "Warping to the other side:"), 
        new StatusStage(70, "Confusing Inky and Clyde:"), 
        new StatusStage(90, "Grabbing the Special Pellet:"), 
        new StatusStage(100, "Level Complete!") 
    }; 

    private static final char[] SHADE_RAMP = { '\u00B7', '\u2022', '\u2058', '\u00A4', '\u205C', ':', '=', '\u2591', '\u2592', '\u2593', '\u2594', '\u2588' }; 
    private double sphereRotation = 2.0; 
    private static final int BASE_R = 255; 
    private static final int BASE_G = 230; 
    private static final int BASE_B = 0; 
    private double mouthOpenFactor = 1.0; 
    private int mouthOpening = -1; 

    public PacManSphereLoader() { 
        super(REFLECTIVE_STAGES); 
    } 

    @Override 
    protected void initialize() { } 

    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        double cameraDistance = 3.5; 
        double sphereRadius = 1.0; 

        double overheadX = 0.577; 
        double overheadY = -0.707; 
        double overheadZ = -0.408; 
        int overheadR = 255, overheadG = 255, overheadB = 220; 

        // STEP 1: RENDER THE SMOOTH 3D SPHERE GEOMETRY 
        for (double theta = 0; theta < Math.PI; theta += 0.02) { 
            double sinTheta = Math.sin(theta); 
            double cosTheta = Math.cos(theta); 
            for (double phi = 0.25 * Math.PI * mouthOpenFactor; phi < ((Math.PI) * (2.0 - (0.25 * mouthOpenFactor))); phi += 0.02) { 
                double sinPhi = Math.sin(phi); 
                double cosPhi = Math.cos(phi); 
                double x = sphereRadius * sinTheta * cosPhi; 
                double y = sphereRadius * sinTheta * sinPhi; 
                double z = sphereRadius * cosTheta; 

                double cosR = Math.cos(sphereRotation), sinR = Math.sin(sphereRotation); 
                double rx = x * cosR - z * sinR; 
                double ry = y; 
                double rz = x * sinR + z * cosR; 
                double ooz = 1.0 / (rz + cameraDistance); 
                int xp = (int) (40 + 75 * ooz * rx); 
                int yp = (int) (11 + 38 * ooz * ry); 

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) { 
                    int index = xp + 80 * yp; 
                    if (ooz > zBuffer[index] + 0.0001) { 
                        zBuffer[index] = ooz; 
                        double nx = rx / sphereRadius; 
                        double ny = ry / sphereRadius; 
                        double nz = rz / sphereRadius; 

                        double viewX = -rx; 
                        double viewY = -ry; 
                        double viewZ = -(rz + cameraDistance); 
                        double distToCam = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ); 
                        if (distToCam > 0) { 
                            viewX /= distToCam; viewY /= distToCam; viewZ /= distToCam; 
                        } 

                        double diffOverhead = nx * overheadX + ny * overheadY + nz * overheadZ; 
                        double specOverhead = 0; 
                        if (diffOverhead < 0) { 
                            diffOverhead = 0; 
                        } else { 
                            double refOverheadX = 2.0 * diffOverhead * nx - overheadX; 
                            double refOverheadY = 2.0 * diffOverhead * ny - overheadY; 
                            double refOverheadZ = 2.0 * diffOverhead * nz - overheadZ; 
                            double specOverheadDot = refOverheadX * viewX + refOverheadY * viewY + refOverheadZ * viewZ; 
                            specOverhead = (specOverheadDot > 0) ? Math.pow(specOverheadDot, 12) : 0; 
                        } 

                        double ambient = 0.25; 
                        int outR = (int) (BASE_R * ambient); 
                        int outG = (int) (BASE_G * ambient); 
                        int outB = (int) (BASE_B * ambient); 

                        outR += (int) (overheadR * (0.4 * diffOverhead + 0.4 * specOverhead)); 
                        outG += (int) (overheadG * (0.4 * diffOverhead + 0.4 * specOverhead)); 
                        outB += (int) (overheadB * (0.4 * diffOverhead + 0.4 * specOverhead)); 

                        outR = Math.max(0, Math.min(255, outR)); 
                        outG = Math.max(0, Math.min(255, outG)); 
                        outB = Math.max(0, Math.min(255, outB)); 

                        double totalIntensity = (0.2 * diffOverhead) + (0.3 * specOverhead); 
                        int shadeIndex = (int) (totalIntensity * (SHADE_RAMP.length - 1)); 
                        shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex)); 
                        char renderChar = SHADE_RAMP[shadeIndex]; 

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", outR, outG, outB); 
                        outputBuffer[index] = colorCode + renderChar + RESET; 
                    } 
                } 
            } 
        } 

        // STEP 2: RENDER THE MOVING WHITE DOT PELLET
        // Only render pellet if the mouth is slightly open to prevent it floating inside a closed body
        if (mouthOpenFactor > 0.15 && mouthOpening < 0) {
            // Parametric distance path: moves from 2.2 units away down into his mouth center (0.3)
            // As mouthOpenFactor grows closer to 0.0 (fully closed), pellet slides inside!
            double pelletDistance = 1.7 - ((1-mouthOpenFactor) * 1.2);
            
            // Map the pellet in front of Pacman's mouth wedge profile alignment
            // Mouth center in your phi sweep sits mathematically along the local X positive/Z neutral profile
            double pelletLocalX = pelletDistance;
            double pelletLocalY = 0.0; // Level height path alignment
            double pelletLocalZ = 0.0;

            // Apply the same rotation matrix so the dot aligns perfectly with his mouth direction
            double cosR = Math.cos(sphereRotation), sinR = Math.sin(sphereRotation);
            double prx = pelletLocalX * cosR - pelletLocalZ * sinR;
            double pry = pelletLocalY;
            double prz = pelletLocalX * sinR + pelletLocalZ * cosR;

            double pOoz = 1.0 / (prz + cameraDistance);
            int px = (int) (40 + 75 * pOoz * prx);
            int py = (int) (11 + 38 * pOoz * pry);

            // Draw a small 2x1 cell white pellet block into the buffer frame
            for (int dy = 0; dy <= 0; dy++) {
                for (int dx = -1; dx <= 0; dx++) {
                    int tx = px + dx;
                    int ty = py + dy;

                    if (tx >= 0 && tx < 80 && ty >= 0 && ty < 22) {
                        int pIndex = tx + 80 * ty;
                        
                        // Z-buffer override layer check: Pellets clip in front of dark backgrounds
                        if (pOoz > zBuffer[pIndex] - 0.05) {
                            zBuffer[pIndex] = pOoz;
                            // Stark bright white ANSI escape sequences
                            outputBuffer[pIndex] = "\u001B[38;5;255m" + "●" + RESET;
                        }
                    }
                }
            }
        }

        sphereRotation += 0.004; 
        mouthOpenFactor += 0.04 * mouthOpening; 
        if (mouthOpenFactor > 1.0 || mouthOpenFactor < 0.0) { 
            mouthOpening *= -1; 
        } 
    } 
}
