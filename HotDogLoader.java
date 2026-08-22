// TODO: The ends of the hot dog look a little cut off... Make them round like iconic hot dog look

public class HotDogLoader extends Loader {
    // 1. Hot Dog specific loading stages
    private static final StatusStage[] HOTDOG_STAGES = {
        new StatusStage(20, "Splitting the brioche bun:"),
        new StatusStage(45, "Grilling the frankfurter:"),
        new StatusStage(70, "Snuggling dog into bun:"),
        new StatusStage(90, "Drizzling ketchup & mustard:"),
        new StatusStage(100, "Glazed in Condiments & Ready!")
    };

    private static final String LUMINANCE_CHARS = ".,-~:;=!*#$@";
    
    // Geometry Dimensions
    private static final double DOG_LENGTH = 3.6;       // Total length across X axis
    private static final double DOG_RADIUS = 0.35;       // Sausage thickness
    private static final double WIENER_CURVATURE = 0.08; // How much the frank endpoints smile upward (Z = c * X^2)
    
    private static final double BUN_LENGTH = 3.4;       // Bun is slightly shorter than the dog
    private static final double BUN_RADIUS = 0.65;       // Bun wraps widely around the dog

    // Rotation pivot alignment (Centered on origin, so offset is zero)
    private static final double X_OFFSET = 0.0; 

    private String bunColor;
    private String wienerColor;
    private String mustardColor;
    private String ketchupColor;
    private String[][] cellCache;

    private double A = 5.0 * Math.PI / 7.0; // Start tilted to view down into the bun valley
    private double B = 0.0;

    public HotDogLoader() {
        super(HOTDOG_STAGES, 80, 22);
    }

    public HotDogLoader(int w, int h) {
        super(HOTDOG_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // Hot Dog Color Matrix
        bunColor     = "\u001B[38;5;215m"; // Warm Toasted Brioche Bun (Tan/Gold)
        wienerColor  = "\u001B[38;5;131m"; // Savory Grilled Frankfurter (Reddish Brown)
        mustardColor = "\u001B[38;5;220m"; // Neon Yellow Mustard
        ketchupColor = "\u001B[38;5;196m"; // Rich Crimson Ketchup

        String[] fullPalette = { bunColor, wienerColor, mustardColor, ketchupColor };
        cellCache = new String[fullPalette.length][LUMINANCE_CHARS.length()];
        for (int c = 0; c < fullPalette.length; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = fullPalette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinB = Math.sin(B), cosB = Math.cos(B);

        // --- LAYER 1: The Splitted Brioche Bun ---
        // Parameterized as an open half-pipe sweeping along length X and wrapping around angle theta
        for (double x = -BUN_LENGTH/2; x <= BUN_LENGTH/2; x += 0.07) {
            // Sweep an open half-cylinder underneath the dog (from PI to 2*PI is the bottom half shell)
            for (double theta = Math.PI - 0.2; theta <= 2.0 * Math.PI + 0.2; theta += 0.08) {
                double cosT = Math.cos(theta), sinT = Math.sin(theta);
                
                // Outer bun surface
                double bx = x;
                double by = BUN_RADIUS * cosT;
                double bz = BUN_RADIUS * sinT + 0.1; // Shift slightly down

                // Normals for shading
                double nx = 0.0;
                double ny = cosT;
                double nz = sinT;

                // Make the bun ends curve up slightly to hug the dog ends
                bz += WIENER_CURVATURE * 0.7 * (x * x);

                drawPoint(bx, by, bz, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0); // 0 = Bun
            }
        }

        // --- LAYER 2: The Iconic Curved Wiener ---
        // Swept along X length, with cross section tube angles p
        for (double x = -DOG_LENGTH/2; x <= DOG_LENGTH/2; x += 0.05) {
            // Base center coordinates of the sausage string curving quadratically upward on the ends
            double centerX = x;
            double centerY = 0.0;
            double centerZ = WIENER_CURVATURE * (x * x);

            for (double p = 0; p < 2.0 * Math.PI; p += 0.15) {
                double cosP = Math.cos(p), sinP = Math.sin(p);

                // Displace outward from the curved core trajectory line
                double wx = centerX;
                double wy = centerY + DOG_RADIUS * cosP;
                double wz = centerZ + DOG_RADIUS * sinP;

                // Surface normal vectors pointing outward from the tube core
                double nx = 0.0; 
                double ny = cosP;
                double nz = sinP;

                // Cap off the ends gracefully like rounded sausage tips
                if (x <= -DOG_LENGTH/2 + 0.1 || x >= DOG_LENGTH/2 - 0.1) {
                    nx = (x > 0) ? 0.6 : -0.6;
                }

                drawPoint(wx, wy, wz, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1); // 1 = Wiener
            }
        }

        // --- LAYER 3: Interlocking Intertwined Sine Waves (Mustard & Ketchup) ---
        // We sweep along the upper spine of the wiener dog and overlay high-fidelity dynamic squiggles
        for (double x = -DOG_LENGTH/2 + 0.2; x <= DOG_LENGTH/2 - 0.2; x += 0.02) {
            // Core wiener height coordinates at this point
            double wienerZ = WIENER_CURVATURE * (x * x);

            // Frequency controls how many squiggles up the dog. Amplitude is width of drizzle.
            double freq = 5.5; 
            double amp = 0.22;

            // --- MUSTARD TRAJECTORY (Sine Wave) ---
            double mustardY = amp * Math.sin(x * freq);
            // Calculate corresponding Z surface height using circle geometry so condiment sits perfectly on top of tube radius
            double mustardZOffset = Math.sqrt(Math.max(0.01, (DOG_RADIUS * DOG_RADIUS) - (mustardY * mustardY)));
            double mx = x;
            double my = mustardY;
            double mz = wienerZ + mustardZOffset + 0.03; // Bump slightly outward to clear depth buffer

            // Shading normals tilt outwards with the squiggle direction
            double mnx = 0.0;
            double mny = mustardY / DOG_RADIUS;
            double mnz = 0.9;

            // Render Mustard Bead Thickness (little cross sections so the line isn't paper thin)
            for (double r = 0; r < 0.06; r += 0.02) {
                for (double a = 0; a < 2*Math.PI; a += 1.0) {
                    drawPoint(mx + r*Math.cos(a), my + r*Math.sin(a), mz, mnx, mny, mnz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2); // 2 = Mustard
                }
            }

            // --- KETCHUP TRAJECTORY (Cosine Wave - Shifted phase creates the interlocking dance) ---
            double ketchupY = amp * Math.cos(x * freq);
            double ketchupZOffset = Math.sqrt(Math.max(0.01, (DOG_RADIUS * DOG_RADIUS) - (ketchupY * ketchupY)));
            double kx = x;
            double ky = ketchupY;
            double kz = wienerZ + ketchupZOffset + 0.03;

            double knx = 0.0;
            double kny = ketchupY / DOG_RADIUS;
            double knz = 0.9;

            // Render Ketchup Bead Thickness
            for (double r = 0; r < 0.06; r += 0.02) {
                for (double a = 0; a < 2*Math.PI; a += 1.0) {
                    drawPoint(kx + r*Math.cos(a), ky + r*Math.sin(a), kz, knx, kny, knz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3); // 3 = Ketchup
                }
            }
        }

        //A += 0.018 * Math.sin(Math.E * B / Math.PI);
        B += 0.025;
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz,
                           double sinA, double cosA, double sinB, double cosB,
                           String[] outputBuffer, double[] zBuffer, int colorIndex) {
        
        double shiftedX = x - X_OFFSET;

        // Apply 3D Matrix Transformations
        double x1 = shiftedX * cosB - y * sinB;
        double y1 = shiftedX * sinB + y * cosB;
        
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;

        double distance = 3.0; 
        double ooZ = 1.0 / (z2 + distance);

        int xp = (int) (window_width / 2.0 + 36 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 16 * ooZ * y2);

        int bufferIndex = xp + window_width * yp;

        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            double nx1 = nx * cosB - ny * sinB;
            double ny1 = nx * sinB + ny * cosB;
            double ny2 = ny1 * cosA - nz * sinA;
            double nz2 = ny1 * sinA + nz * cosA;
            double nx2 = nx1;

            double luminance = nx2 * 0.3 + ny2 * 0.3 + nz2 * 0.9;

            if (ooZ > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooZ;

                int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));

                outputBuffer[bufferIndex] = cellCache[colorIndex][charIndex];
            }
        }
    }
}
