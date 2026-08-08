public class RingLoader extends Loader {
    private static final StatusStage[] RING_STAGES = {
        new StatusStage(15, "Initializing hyper-drive:"),
        new StatusStage(40, "Bending spatial geometry:"),
        new StatusStage(65, "Chroma-wave synchronization:"),
        new StatusStage(85, "Quantum orbit stabilized:"),
        new StatusStage(100, "Reality Distortion Active!")
    };

    private double A = 0; // X-axis rotation
    private double B = 0; // Y-axis rotation
    private double C = 0; // Time variable / Z-axis rotation for shape shifting

    public RingLoader() {
        // This uses 80x22 specifically
        super(RING_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // No static color setups needed since colors are calculated dynamically in real-time
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Compute dynamic torus radius changes over time (creates a pulsing/morphing thickness loop)
        double morphRadius = 2.4 + 0.4 * Math.sin(C); 
        double tubeThickness = 0.25 + 0.1 * Math.cos(C * 1.5);

        for (int tIndex = 0; tIndex < 180; tIndex++) {
            double theta = tIndex * 0.035;
            for (int pIndex = 0; pIndex < 524; pIndex++) {
                double phi = pIndex * 0.012;
                
                double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
                double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);
                double sinA = Math.sin(A), cosA = Math.cos(A);
                double sinB = Math.sin(B), cosB = Math.cos(B);

                // Apply the morphing geometry constraints
                double h = tubeThickness * cosPhi + morphRadius; 
                double D = 1 / (sinTheta * h * sinA + sinPhi * cosA + 6.0);
                double t = sinTheta * h * cosA - sinPhi * sinA;

                // Fisheye Lens Projection
                double fisheyeFactor = Math.pow(D, 1.75);
                int x = (int) (40 + 190 * fisheyeFactor * (cosTheta * h * cosB - t * sinB));
                int y = (int) (12 + 95 * fisheyeFactor * (cosTheta * h * sinB + t * cosB));
                int o = x + 80 * y;

                // Illumination mapping
                double N_double = 8 * ((sinPhi * sinA - sinTheta * cosPhi * cosA) * cosB - sinTheta * cosPhi * sinA - sinPhi * cosA - cosTheta * cosPhi * sinB);

                if (22 > y && y > 0 && x > 0 && 80 > x && D > (zBuffer[o] + 0.0001)) {
                    zBuffer[o] = D;
                    int charIndex = (int) Math.round(N_double);
                    if (charIndex < 0) charIndex = 0;
                    
                    String lString = ".,-~:;=!*#$@";
                    char asciiChar = lString.charAt(charIndex >= lString.length() ? lString.length() - 1 : charIndex);

                    // Dynamic Rainbow Wave Formula
                    // Uses spatial positioning (x, y) and time (C) to loop through RGB values
                    double colorOffset = C * 2.0 + (x * 0.05) + (y * 0.1);
                    int r = (int) (128 + 127 * Math.sin(colorOffset));
                    int g = (int) (128 + 127 * Math.sin(colorOffset + 2.0 * Math.PI / 3.0));
                    int b = (int) (128 + 127 * Math.sin(colorOffset + 4.0 * Math.PI / 3.0));

                    // Generate standard 24-bit TrueColor ANSI escape code sequence
                    String rainbowColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);

                    outputBuffer[o] = rainbowColor + asciiChar + RESET;
                }
            }
        }
        
        // Advance independent rotation and mutation speeds
        A += 0.02; 
        B += 0.01;
        C += 0.025; 
    }
}
