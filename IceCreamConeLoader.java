// TODO: Improve this....

public class IceCreamConeLoader extends Loader {
    private static final StatusStage[] ICE_CREAM_STAGES = {
        new StatusStage(20, "Baking waffle cones:"),
        new StatusStage(50, "Scooping Neapolitan flavors:"),
        new StatusStage(80, "Sculpting cartoonish fluff rims:"),
        new StatusStage(100, "Three-Scoop Stack Ready!")
    };

    private final int[][] scoop1SprinkleMap = new int[180][180];
    private final int[][] scoop2SprinkleMap = new int[180][180];
    private final int[][] scoop3SprinkleMap = new int[180][180];

    // Neapolitan color palette directly matching your image
    private final String scoopTopColor = "\u001B[38;5;211m";    // Strawberry Pink
    private final String scoopMidColor = "\u001B[38;5;255m";    // Vanilla White
    private final String scoopBottomColor = "\u001B[38;5;130m"; // Chocolate Brown
    private String[] sprinkleColors;

    // Fixed viewing pitch angle so we look slightly down onto the spinning stack
    private final double CAMERA_TILT = 0.25; 
    private double rotationAngle = 0; // Continuous spin around central vertical axis

    public IceCreamConeLoader() {
        super(ICE_CREAM_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        sprinkleColors = new String[] {
            "\u001B[38;5;206m", "\u001B[38;5;51m", "\u001B[38;5;220m", "\u001B[38;5;46m"
        };

        for (int i = 0; i < 180; i++) {
            for (int j = 0; j < 180; j++) {
                scoop1SprinkleMap[i][j] = -1;
                scoop2SprinkleMap[i][j] = -1;
                scoop3SprinkleMap[i][j] = -1;
            }
        }

        // Populate independent sprinkle coordinate configurations
        for (int s = 0; s < 25; s++) {
            int tIdx = (int)(Math.random() * 100) + 20; // Keep sprinkles on upper dome surface
            int pIdx = (int)(Math.random() * 180);
            int colorIdx = (int)(Math.random() * sprinkleColors.length);
            markSprinkleRadius(scoop1SprinkleMap, tIdx, pIdx, colorIdx);
            markSprinkleRadius(scoop2SprinkleMap, tIdx, pIdx, colorIdx);
            markSprinkleRadius(scoop3SprinkleMap, tIdx, pIdx, colorIdx);
        }
    }

    private void markSprinkleRadius(int[][] map, int targetT, int targetP, int colorIdx) {
        for (int dt = -1; dt <= 1; dt++) {
            for (int dp = -2; dp <= 2; dp++) {
                int t = (targetT + dt + 180) % 180;
                int p = (targetP + dp + 180) % 180;
                map[t][p] = colorIdx;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        for (int tIndex = 0; tIndex < 180; tIndex++) {
            double theta = tIndex * (2 * Math.PI / 180);

            for (int pIndex = 0; pIndex < 180; pIndex++) {
                double phi = pIndex * (Math.PI / 180);
                double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);

                // ==========================================
                // ZONE 1: THE WAFFLE CONE
                // ==========================================
                double coneHeight = 3.2;
                double coneTopRadius = 1.5;
                double coneY = 1.9 + (pIndex / 180.0) * coneHeight; 
                double currentRadius = coneTopRadius * (1.0 - ((coneY - 1.9) / coneHeight));

                if (currentRadius >= 0) {
                    double cx0 = currentRadius * Math.cos(theta);
                    double cy0 = coneY;
                    double cz0 = currentRadius * Math.sin(theta);

                    double wafflePattern = Math.sin(theta * 14 + (coneY * 6)) * Math.sin(theta * 14 - (coneY * 6));
                    String coneColor = "\u001B[38;5;173m";
                    if (wafflePattern > 0.3) {
                        coneColor = "\u001B[38;5;130m";
                    }
                    projectPoint(cx0, cy0, cz0, Math.cos(theta), 0.3, Math.sin(theta), coneColor, outputBuffer, zBuffer);
                }

                // ==========================================
                // ZONE 2: CHOCOLATE SCOOP (Bottom)
                // ==========================================
                renderIceCreamScoop(phi, theta, 0.6, scoopBottomColor, scoop1SprinkleMap[pIndex][tIndex], outputBuffer, zBuffer);

                // ==========================================
                // ZONE 3: VANILLA SCOOP (Middle)
                // ==========================================
                renderIceCreamScoop(phi, theta, -0.9, scoopMidColor, scoop2SprinkleMap[pIndex][tIndex], outputBuffer, zBuffer);

                // ==========================================
                // ZONE 4: STRAWBERRY SCOOP (Top)
                // ==========================================
                renderIceCreamScoop(phi, theta, -2.4, scoopTopColor, scoop3SprinkleMap[pIndex][tIndex], outputBuffer, zBuffer);
            }
        }

        // Spin smoothly purely around the vertical axis
        rotationAngle += 0.05;
    }

    private void renderIceCreamScoop(double phi, double theta, double centerY, String baseColor, 
                                     int sprinkleColorIdx, String[] outputBuffer, double[] zBuffer) {
        double baseRadius = 1.3;
        double r = baseRadius;
        
        // STANDARD ICE CREAM GEOMETRY ENGINE: Perfect Sphere Dome + Thick Fluff Rim
        // As phi passes the equator (0.55 * PI), flare outward heavily to make a prominent cartoonish rim lip
        if (phi > (Math.PI * 0.53) && phi < (Math.PI * 0.76)) {
            // Base thickness expansion + wavy sculpt contours matching the physical scoop mold lines
            r += 0.42 + 0.07 * Math.sin(theta * 12); 
        } else if (phi >= (Math.PI * 0.76)) {
            // Taper back in sharply underneath the rim lip to reconnect with the stack column
            r -= 0.1;
        }

        double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);
        double sx = r * sinPhi * Math.cos(theta);
        double sy = centerY + r * cosPhi;
        double sz = r * sinPhi * Math.sin(theta);

        // Surface normal vectors for calculation of light reflection maps
        double nx = sinPhi * Math.cos(theta);
        double ny = cosPhi;
        double nz = sinPhi * Math.sin(theta);

        // Map colors (Switching color strings seamlessly if coordinate matches sprinkle index)
        String color = baseColor;
        if (sprinkleColorIdx != -1 && phi < (Math.PI * 0.53)) {
            color = sprinkleColors[sprinkleColorIdx];
        }

        projectPoint(sx, sy, sz, nx, ny, nz, color, outputBuffer, zBuffer);
    }

    private void projectPoint(double x0, double y0, double z0, double nx, double ny, double nz,
                              String color, String[] outputBuffer, double[] zBuffer) {
        // --- STEP 1: TWIRL AROUND CENTRAL VERTICAL AXIS (Y-AXIS) ---
        double sinR = Math.sin(rotationAngle), cosR = Math.cos(rotationAngle);
        double x1 = x0 * cosR - z0 * sinR;
        double y1 = y0;
        double z1 = x0 * sinR + z0 * cosR;

        double nx1 = nx * cosR - nz * sinR;
        double ny1 = ny;
        double nz1 = nx * sinR + nz * cosR;

        // --- STEP 2: APPLY FIXED CAMERA TILT (PITCH AROUND X-AXIS) ---
        double sinA = Math.sin(CAMERA_TILT), cosA = Math.cos(CAMERA_TILT);
        double x2 = x1;
        double y2 = y1 * cosA - z1 * sinA;
        double z2 = y1 * sinA + z1 * cosA;

        double nx2 = nx1;
        double ny2 = ny1 * cosA - nz1 * sinA;
        double nz2 = ny1 * sinA + nz1 * cosA;

        // --- STEP 3: PERSPECTIVE PROJECTION & SHADING ---
        double distanceOffset = 9.8; 
        double D = 1.0 / (z2 + distanceOffset);
        
        int x = (int) (40 + 35 * D * x2 * 2.0); // Aspect ratio balance multiplier
        int y = (int) (11 + 19 * D * y2);
        int o = x + window_width * y;

        // Constant overhead directional spotlight vector
        double L = nx2 * 0.0 - ny2 * 0.7 - nz2 * 0.7; 

        if (window_height > y && y > 0 && x > 0 && window_width > x && D > (zBuffer[o] + 0.0001)) {
            zBuffer[o] = D;

            int charIndex = (int) (Math.round((L + 1.0) * 5));
            if (charIndex < 0) charIndex = 0;

            String lString = ".,-~:;=!*#$@";
            char asciiChar = lString.charAt(charIndex >= lString.length() ? lString.length() - 1 : charIndex);

            outputBuffer[o] = color + asciiChar + RESET;
        }
    }
}
