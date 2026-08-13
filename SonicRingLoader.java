public class SonicRingLoader extends Loader { 
    private static final StatusStage[] RING_STAGES = { 
        new StatusStage(15, "Collecting chaos emerald energy:"), 
        new StatusStage(40, "Forging golden metallic boundaries:"), 
        new StatusStage(65, "Calibrating specular gleam vectors:"), 
        new StatusStage(85, "Spawning loop coordinates:"), 
        new StatusStage(100, "Sonic Gold Ring Active!") 
    }; 
    
    private double yaw = 0; // Pure horizontal spin rotation accumulator 
    
    public SonicRingLoader() { 
        super(RING_STAGES, 80, 22); 
    } 
    
    @Override 
    protected void initialize() { 
        this.yaw = 0; 
    } 
    
    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        renderSingleRing(-3.2, 0, outputBuffer, zBuffer); // Left Ring 
        renderSingleRing( 0.0, 1, outputBuffer, zBuffer); // Center Ring 
        renderSingleRing( 3.2, 2, outputBuffer, zBuffer); // Right Ring 
        
        // About how fast Sonic's rings spin...
        yaw += 0.07; 
    } 
    
    private void renderSingleRing(double xOffset, int ringIndex, String[] outputBuffer, double[] zBuffer) { 
        double targetRadius = 1.1; 
        double tubeThickness = 0.20; 
        
        // Ring locked to a static vertical inclination profile 
        double pitch = Math.toRadians(15.0); 
        double sinP = Math.sin(pitch), cosP = Math.cos(pitch); 
        
        // As we look left or right, our perspective on the rotation of the rings makes
        // it look like they are out of phase, due to our relative viewing angle.
        // This counteracts that perspective warping by applying a linear phase shift offset
        double currentYaw = yaw - (xOffset * 0.11);
        double sinY = Math.sin(currentYaw), cosY = Math.cos(currentYaw); 
        
        for (int tIndex = 0; tIndex < 120; tIndex++) { 
            double theta = tIndex * (2.0 * Math.PI / 120.0); 
            for (int pIndex = 0; pIndex < 180; pIndex++) { 
                double phi = pIndex * (2.0 * Math.PI / 180.0); 
                
                double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta); 
                double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi); 
                
                // Local Torus Coordinates 
                double h = tubeThickness * cosPhi + targetRadius; 
                double lx = cosTheta * h; 
                double ly = sinTheta * h; 
                double lz = tubeThickness * sinPhi; 
                
                // Step 1: Spin around vertical Y-axis (Horizontal Yaw Spin) 
                double rx1 = lx * cosY - lz * sinY; 
                double ry1 = ly; 
                double rz1 = lx * sinY + lz * cosY; 
                
                // Inject horizontal X offset to slide rings side-by-side 
                double rx = rx1 + xOffset; 
                double ry = ry1 * cosP - rz1 * sinP; 
                double rz = ry1 * sinP + rz1 * cosP; 
                
                // Deep camera zoom back step to comfortably handle the full row architecture 
                double ooz = 1.0 / (rz + 8.8); 
                
                // Adjusted multipliers to 80 (X) and 35 (Y) for crisp widescreen containment 
                int x = (int) (40 + 80 * ooz * rx); 
                int y = (int) (11 + 35 * ooz * ry); 
                int o = x + 80 * y; 
                
                // Surface normal generation for shading calculations 
                double nx = cosTheta * cosPhi; 
                double ny = sinTheta * cosPhi; 
                double nz = sinPhi; 
                
                // Rotate normals identically to keep lighting accurate 
                double nxb = nx * cosY - nz * sinY; 
                double nzb1 = nx * sinY + nz * cosY; 
                double nny = ny * cosP - nzb1 * sinP; 
                
                if (y >= 0 && y < 22 && x >= 0 && x < 80 && ooz > (zBuffer[o] + 0.0001)) { 
                    zBuffer[o] = ooz; 
                    
                    // Light source positioned over shoulder 
                    double normalLight = (nxb * 0.5 + nny * -0.5 + 0.707) / 1.414; 
                    normalLight = Math.max(0.0, Math.min(1.0, normalLight)); 
                    
                    String lString = " .:-=+#%@"; 
                    int charIndex = (int) (normalLight * (lString.length() - 1)); 
                    char asciiChar = lString.charAt(charIndex); 
                    
                    double specularHighlight = Math.pow(normalLight, 6.0); 
                    
                    int rBase = 210, gBase = 140, bBase = 10; 
                    int rDiff = 255, gDiff = 215, bDiff = 0; 
                    int rSpec = 255, gSpec = 255, bSpec = 230; 
                    
                    int r = (int) ((1.0 - normalLight) * rBase + normalLight * rDiff + specularHighlight * (rSpec - rDiff)); 
                    int g = (int) ((1.0 - normalLight) * gBase + normalLight * gDiff + specularHighlight * (gSpec - gDiff)); 
                    int b = (int) ((1.0 - normalLight) * bBase + normalLight * bDiff + specularHighlight * (bSpec - bDiff)); 
                    
                    r = Math.max(0, Math.min(255, r)); 
                    g = Math.max(0, Math.min(255, g)); 
                    b = Math.max(0, Math.min(255, b)); 
                    
                    String goldColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b); 
                    outputBuffer[o] = goldColor + asciiChar + RESET; 
                } 
            } 
        } 
    } 
}
