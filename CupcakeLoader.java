// TODO: Fix visual clarity / angle of this....

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

    // A is the only rotation variable now (vertical Y-axis)
    private double A = 0;

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
            case 1: // --- 1. RED VELVET ---
                frostingColor = "\u001B[38;5;255m"; // Cream Cheese White
                cakeColor = "\u001B[38;5;88m"; // Deep Red Velvet
                sprinkleColors = new String[]{"\u001B[38;5;196m", "\u001B[38;5;205m", "\u001B[38;5;255m"};
                break;

            case 2: // --- 2. CHOCOLATE PEANUT BUTTER ---
                frostingColor = "\u001B[38;5;214m"; // Peanut Butter
                cakeColor = "\u001B[38;5;94m"; // Dark Chocolate
                sprinkleColors = new String[]{"\u001B[38;5;130m", "\u001B[38;5;226m", "\u001B[38;5;255m"};
                break;

            case 3: // --- 3. FUNFETTI ---
                frostingColor = "\u001B[38;5;255m"; // Vanilla White
                cakeColor = "\u001B[38;5;229m"; // Pale Yellow Cake
                sprinkleColors = new String[]{"\u001B[38;5;51m", "\u001B[38;5;46m", "\u001B[38;5;196m"};
                break;

            case 4: // --- 4. DARK FOREST ---
                frostingColor = "\u001B[38;5;160m"; // Cherry Red
                cakeColor = "\u001B[38;5;236m"; // Very Dark Brown/Black
                sprinkleColors = new String[]{"\u001B[38;5;255m", "\u001B[38;5;205m", "\u001B[38;5;130m"};
                break;

            case 5: // --- 5. LEMON DROP ---
                frostingColor = "\u001B[38;5;229m"; // Pale Lemon
                cakeColor = "\u001B[38;5;226m"; // Bright Yellow
                sprinkleColors = new String[]{"\u001B[38;5;255m", "\u001B[38;5;220m", "\u001B[38;5;214m"};
                break;

            case 6:
            default: // --- 6. HOMER'S STRAWBERRY CLASSIC ---
                frostingColor = "\u001B[38;5;205m"; // Strawberry Pink
                cakeColor = "\u001B[33m"; // Golden Cake
                sprinkleColors = new String[]{"\u001B[38;5;51m", "\u001B[38;5;46m", "\u001B[38;5;255m"};
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
        double cosA = Math.cos(A);
        double sinA = Math.sin(A);

        for (int tIndex = 0; tIndex < THETA_STEPS; tIndex++) {
            double theta = tIndex * THETA_STEP;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);

            for (int yIndex = 0; yIndex < Y_STEPS; yIndex++) {
                double y = Y_MIN + yIndex * Y_STEP;
                double r, dr_dy;
                boolean isFrosting = y > 0.0;

                if (!isFrosting) {
                    // Base wrapper with a sine-wave ripple to simulate paper folds
                    double wrapperRipple = 0.04 * Math.sin(40 * theta);
                    r = 1.0 + 0.25 * y + wrapperRipple; 
                    dr_dy = 0.25; 
                } else {
                    // Frosting dome with a dynamic swirl pattern
                    double swirl = 0.08 * Math.sin(5 * theta + 5 * y);
                    r = 1.0 - (y * y / 2.25) + swirl;
                    dr_dy = -2.0 * y / 2.25 + 0.4 * Math.cos(5 * theta + 5 * y);
                }

                // Compute normals for 3D lighting
                double len = Math.sqrt(1.0 + dr_dy * dr_dy);
                double nx = cosTheta / len;
                double ny = -dr_dy / len;
                double nz = sinTheta / len;

                // Position surface point
                double x3d = r * cosTheta;
                double z3d = r * sinTheta;

                // Rotate cleanly around vertical Y-axis
                double x_rot = x3d * cosA - z3d * sinA;
                double z_rot = x3d * sinA + z3d * cosA;
                
                double nx_rot = nx * cosA - nz * sinA;
                double nz_rot = nx * sinA + nz * cosA;

                // Perspective projection onto standard terminal layout
                double z_proj = z_rot + 4.5;
                double D = 1.0 / z_proj;

                int screen_x = (int) (window_width / 2.0 + 35 * D * x_rot);
                int screen_y = (int) (window_height / 2.0 + 4 - 18 * D * y);
                int o = screen_x + window_width * screen_y;

                // Lighting calculation (Light pointing top-right-inward)
                double L_x = 0.0;
                double L_y = 0.707;
                double L_z = -0.707;
                double luminance = nx_rot * L_x + ny * L_y + nz_rot * L_z;

                // Buffer output if rendering point is in frame & closer to camera
                if (screen_y >= 0 && screen_y < window_height && screen_x >= 0 && screen_x < window_width && D > (zBuffer[o] + 0.0001)) {
                    zBuffer[o] = D;
                    int charIndex = (int) Math.round(luminance * 8.0);
                    
                    // Clamp bounds manually for rendering speed
                    if (charIndex < 0) charIndex = 0;
                    if (charIndex >= LUMINANCE_CHARS.length()) charIndex = LUMINANCE_CHARS.length() - 1;

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
        A += 0.05; // Spin continuously
    }
}