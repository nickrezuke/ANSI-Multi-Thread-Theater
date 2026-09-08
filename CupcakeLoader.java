public class CupcakeLoader extends Loader {

    private static final StatusStage[] CUPCAKE_STAGES = {
            new StatusStage(20, "Mixing batter:"),
            new StatusStage(35, "Baking cake:"),
            new StatusStage(55, "Cooling down:"),
            new StatusStage(75, "Piping frosting:"),
            new StatusStage(95, "Adding sprinkles:"),
            new StatusStage(100, "Boxed & Ready!")
    };

    private static final String LUMINANCE_CHARS = ":;=!*#$@▒▓█";

    // Surface bounds for the cupcake profile
    private static final double Y_MIN = -1.2;
    private static final double Y_MAX = 1.5;
    private static final double Y_STEP = 0.045;
    private static final double THETA_STEP = 0.035;
    private static final int THETA_STEPS = (int) Math.ceil((2.0 * Math.PI) / THETA_STEP);
    private static final int Y_STEPS = (int) Math.ceil((Y_MAX - Y_MIN) / Y_STEP);

    private final int[][] sprinkleMap = new int[THETA_STEPS][Y_STEPS];
    
    private String frostingColor;
    private String cakeColor;
    private String[] sprinkleColors;
    private String[][] cellCache;

    // A is the tilt (pitch) to view the top frosting, B is the continuous spin (yaw)
    private double A = Math.PI / -6.0; 
    private double B = 0.0;

    public CupcakeLoader() {
        super(CUPCAKE_STAGES, 80, 24); // Slightly taller for the dome
    }

    public CupcakeLoader(int w, int h) {
        super(CUPCAKE_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // Setup visual flavor styling for iconic cupcakes
        switch ((int) (Math.random() * 6) + 1) {
            case 1: // --- 1. THE "HOSTESS" ---
                frostingColor = "\u001B[38;5;52m"; // Dark Chocolate
                cakeColor = "\u001B[38;5;234m"; // Devil's Food / Dark Grey
                sprinkleColors = new String[]{"\u001B[38;5;231m", "\u001B[38;5;255m", "\u001B[38;5;250m"}; // White Loops
                break;

            case 2: // --- 2. CLASSIC RED VELVET ---
                frostingColor = "\u001B[38;5;231m"; // Cream Cheese White
                cakeColor = "\u001B[38;5;88m"; // Deep Crimson
                sprinkleColors = new String[]{"\u001B[38;5;160m", "\u001B[38;5;52m", "\u001B[38;5;231m"};
                break;

            case 3: // --- 3. 90s FUNFETTI ---
                frostingColor = "\u001B[38;5;231m"; // Vanilla White
                cakeColor = "\u001B[38;5;222m"; // Golden Yellow
                sprinkleColors = new String[]{"\u001B[38;5;51m", "\u001B[38;5;201m", "\u001B[38;5;226m"}; // CMY Rainbow
                break;

            case 4: // --- 4. MINT CHOCOLATE CHIP ---
                frostingColor = "\u001B[38;5;119m"; // Mint Green
                cakeColor = "\u001B[38;5;94m"; // Dark Cocoa
                sprinkleColors = new String[]{"\u001B[38;5;52m", "\u001B[38;5;234m", "\u001B[38;5;48m"}; // Choc Chips & Mint
                break;

            case 5: // --- 5. FAIRY / COTTON CANDY ---
                frostingColor = "\u001B[38;5;218m"; // Pastel Pink
                cakeColor = "\u001B[38;5;117m"; // Pastel Blue
                sprinkleColors = new String[]{"\u001B[38;5;135m", "\u001B[38;5;231m", "\u001B[38;5;227m"};
                break;

            case 6:
            default: // --- 6. THE POP-ART ---
                frostingColor = "\u001B[38;5;205m"; // Neon Pink
                cakeColor = "\u001B[38;5;214m"; // Deep Yellow
                sprinkleColors = new String[]{"\u001B[38;5;51m", "\u001B[38;5;46m", "\u001B[38;5;231m"}; // Cyan, Green, White
                break;
        }

        // Build the color/char lookup cache for rapid framerates
        String[] palette = { cakeColor, frostingColor, sprinkleColors[0], sprinkleColors[1], sprinkleColors[2] };
        cellCache = new String[palette.length][LUMINANCE_CHARS.length()];
        for (int c = 0; c < palette.length; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                // RESET is assumed inherited from the Loader base class
                cellCache[c][ch] = palette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }

        // Generate static sprinkle distribution on the frosting
        for (int t = 0; t < THETA_STEPS; t++) {
            for (int y = 0; y < Y_STEPS; y++) {
                sprinkleMap[t][y] = -1;
                double actualY = Y_MIN + y * Y_STEP;
                // Distribute sprinkles only on the upper hemisphere of the frosting
                if (actualY > 0.2 && Math.random() < 0.04) {
                    sprinkleMap[t][y] = (int) (Math.random() * 3);
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A); // Tilt calculations
        double sinA = Math.sin(A);
        double cosB = Math.cos(B); // Spin calculations
        double sinB = Math.sin(B);

        for (int tIndex = 0; tIndex < THETA_STEPS; tIndex++) {
            double theta = tIndex * THETA_STEP;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);

            for (int yIndex = 0; yIndex < Y_STEPS; yIndex++) {
                double y = Y_MIN + yIndex * Y_STEP;
                double r, dr_dy;
                boolean isFrosting = y > 0.0;

                if (!isFrosting) {
                    // Base wrapper with a sine-wave ripple
                    double wrapperRipple = 0.04 * Math.sin(40 * theta);
                    r = 1.0 + 0.25 * y + wrapperRipple; 
                    dr_dy = 0.25; 
                } else {
                    // Frosting dome with a dynamic swirl pattern
                    double swirl = 0.08 * Math.sin(5 * theta + 5 * y);
                    r = 1.0 - (y * y / 2.25) + swirl;
                    dr_dy = -2.0 * y / 2.25 + 0.4 * Math.cos(5 * theta + 5 * y);
                }

                // Compute raw normals for 3D lighting
                double len = Math.sqrt(1.0 + dr_dy * dr_dy);
                double nx = cosTheta / len;
                double ny = -dr_dy / len;
                double nz = sinTheta / len;

                // Raw 3D positions
                double x3d = r * cosTheta;
                double y3d = y;
                double z3d = r * sinTheta;

                // 1. Spin around Y-axis (Yaw)
                double x1 = x3d * cosB - z3d * sinB;
                double z1 = x3d * sinB + z3d * cosB;
                double y1 = y3d;
                
                // 2. Tilt around X-axis (Pitch)
                double y2 = y1 * cosA - z1 * sinA;
                double z2 = y1 * sinA + z1 * cosA;
                double x2 = x1;

                // Apply the exact same 2-step transformations to the normal vectors
                double nx1 = nx * cosB - nz * sinB;
                double nz1 = nx * sinB + nz * cosB;
                double ny1 = ny;

                double ny2 = ny1 * cosA - nz1 * sinA;
                double nz2 = ny1 * sinA + nz1 * cosA;
                double nx2 = nx1;

                // Perspective projection with dynamic "breathing" zoom bound to the tilt
                double distance = 1.4 + Math.cos(A) * 1.6; 
                double z_proj = z2 + distance;
                double D = 1.0 / z_proj;

                int screen_x = (int) (window_width / 2.0 + 35 * D * x2 * 1.4);
                int screen_y = (int) (window_height / 2.0 + 2 - 18 * D * y2);
                int o = screen_x + window_width * screen_y;

                // Lighting calculation (Light pointing top-right-inward)
                double L_x = 0.4;
                double L_y = 0.7;
                double L_z = -0.5;
                double luminance = nx2 * L_x + ny2 * L_y + nz2 * L_z;

                // Buffer output if rendering point is in frame & closer to camera
                if (screen_y >= 0 && screen_y < window_height && screen_x >= 0 && screen_x < window_width && D > (zBuffer[o] + 0.0001)) {
                    zBuffer[o] = D;
                    
                    // Smoothly map luminance [-1.0, 1.0] to array indices for better shadows
                    int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                    charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));

                    int colorIndex = isFrosting ? 1 : 0; 
                    if (isFrosting) {
                        int sprinkleColorIndex = sprinkleMap[tIndex][yIndex];
                        if (sprinkleColorIndex != -1) {
                            colorIndex = 2 + sprinkleColorIndex; 
                        }
                    }
                    outputBuffer[o] = cellCache[colorIndex][charIndex];
                }
            }
        }
        
        // Spin and undulating tilt logic for the next frame
        A += 0.0075 * Math.sin(B); // Gentle undulating tilt
        B += 0.025;                // Continuous spin
    }
}