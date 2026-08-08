public class BouncingSpinnerLoader extends Loader {
    private static final StatusStage[] SPINNER_STAGES = {
        new StatusStage(25, "Booting graphical indicator sub-systems:"),
        new StatusStage(50, "Calibrating elastic perimeter boundary collisions:"),
        new StatusStage(75, "Allocating 256-color randomized hue tables:"),
        new StatusStage(100, "Bouncing OS Spinner Active!")
    };

    // 8-Step micro-granular shading scale to represent the spinning wheel trail tail decay
    private static final char[] DIAL_SHADES = { '\u2588', '\u2593', '\u2592', '\u2591', '\u25A0', '\u25AA', '\u00B7', ' ' };

    // Bouncing physics mechanics parameters
    private double posX = 40.0;
    private double posY = 11.0;
    private double velX = 0.30;
    private double velY = 0.19;
    private static final double SPINNER_RADIUS = 3.8;

    private double timeClock = 0.0;
    private int activeColorIndex = 45;

    // Look-up tables containing 12 specific high-saturation retro aesthetic colors
    private static final int[] COLOR_POOL = { 51, 201, 226, 46, 196, 214, 129, 87, 118, 208, 45, 198 };

    public BouncingSpinnerLoader() {
        // This uses 80x22 specifically
        super(SPINNER_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.30; // Drives the high-speed sector rotation ticks

        int width = 80;
        int height = 22;

        // --- STEP 1: ADVANCE 2D COLLISION PHYSICS VEHICLE ---
        posX += velX;
        posY += velY;

        // Compute container wall boundary limits factoring in font spacing aspect ratios
        double leftWallLimit  = SPINNER_RADIUS * 1.85;
        double rightWallLimit = width - (SPINNER_RADIUS * 1.85);
        double topWallLimit    = SPINNER_RADIUS;
        double btmWallLimit   = height - SPINNER_RADIUS;

        boolean hitWall = false;

        // Horizontal axis collision tracking
        if (posX <= leftWallLimit) {
            posX = leftWallLimit;
            velX *= -1.0;
            hitWall = true;
        } else if (posX >= rightWallLimit) {
            posX = rightWallLimit;
            velX *= -1.0;
            hitWall = true;
        }

        // Vertical axis collision tracking
        if (posY <= topWallLimit) {
            posY = topWallLimit;
            velY *= -1.0;
            hitWall = true;
        } else if (posY >= btmWallLimit) {
            posY = btmWallLimit;
            velY *= -1.0;
            hitWall = true;
        }

        // TRIGGER ON-COLLISION SPECTRUM COLOR FLIP
        if (hitWall) {
            // Seed a clean random array index based on local coordinate timestamps
            int nextPoolIndex = (int)(Math.abs(Math.sin(posX) * 100.0) + Math.abs(Math.cos(posY) * 50.0)) % COLOR_POOL.length;
            int selectedColor = COLOR_POOL[nextPoolIndex];
            
            // Prevent choosing the exact same color code back-to-back
            if (selectedColor == activeColorIndex) {
                selectedColor = COLOR_POOL[(nextPoolIndex + 1) % COLOR_POOL.length];
            }
            activeColorIndex = selectedColor;
        }

        // --- STEP 2: CLEAR REFRESH BACKGROUND CONSOLE SURFACE ---
        String bgColorCode = "\u001B[38;5;234m"; // Dark slate backdrop charcoal texture
        for (int i = 0; i < width * height; i++) {
            outputBuffer[i] = bgColorCode + "\u00B7" + RESET; // Fill space with fine matrix guide dots
            zBuffer[i] = 0.001; // Low depth baseline
        }

        // --- STEP 3: PROCEDURAL COMPONENT SPINNER INDICATOR RASTERIZER ---
        String spinnerColorString = "\u001B[38;5;" + activeColorIndex + "m";
        int spinningTickOffset = ((int)Math.floor(timeClock / 2.0)) % 8;

        // Iterate a micro-step scan wheel field centered exactly over the current moving physics position
        for (double dy = -SPINNER_RADIUS; dy <= SPINNER_RADIUS; dy += 0.25) {
            for (double dx = -SPINNER_RADIUS * 1.85; dx <= SPINNER_RADIUS * 1.85; dx += 0.25) {
                
                int targetXp = (int) Math.round(posX + dx);
                int targetYp = (int) Math.round(posY + dy);

                if (targetXp >= 0 && targetXp < width && targetYp >= 0 && targetYp < height) {
                    int index = targetXp + width * targetYp;

                    // Compresses X input to evaluate mathematically flawless circular wheel perimeters
                    double cx = dx / 1.85;
                    double radialRadius = Math.sqrt(cx * cx + dy * dy);

                    // Restrict rendering points inside a tight ring torus belt geometry band
                    if (radialRadius >= 2.0 && radialRadius <= 3.6) {
                        if (0.95 > zBuffer[index]) {
                            zBuffer[index] = 0.95;

                            // Calculate sector radian position to distribute the 8 loading spokes
                            double angle = Math.atan2(dy, cx);
                            double normalizedAngle = (angle + Math.PI) / (2.0 * Math.PI); // Maps to [0.0, 1.0]

                            // Snap the angle coordinate straight onto one of 8 discrete sector indices
                            int sectorIndex = (int) Math.floor(normalizedAngle * 8.0) % 8;

                            // Calculate tail decay shading weight relative to active tracking rotation ticks
                            int shadingDistanceIndex = (sectorIndex - spinningTickOffset + 8) % 8;
                            
                            // Invert index so the leading pulse spoke stays at full 100% white density saturation
                            shadingDistanceIndex = 7 - shadingDistanceIndex;

                            char renderChar = DIAL_SHADES[shadingDistanceIndex];
                            
                            // High-intensity leading spoke is forced to pure white block core lines
                            String spokeColor = spinnerColorString;
                            if (shadingDistanceIndex == 0) {
                                spokeColor = "\u001B[38;5;255m"; // Pure White core flash hot-spot
                            }

                            outputBuffer[index] = spokeColor + renderChar + RESET;
                        }
                    }
                }
            }
        }
    }
}
