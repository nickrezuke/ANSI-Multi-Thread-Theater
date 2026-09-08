public class IceCreamConeLoader extends Loader {
    private static final StatusStage[] ICE_CREAM_STAGES = {
        new StatusStage(20, "Baking waffle cones:"),
        new StatusStage(50, "Scooping flavors:"),
        new StatusStage(80, "Sculpting fluff rims:"),
        new StatusStage(100, "Cone is Ready!")
    };

    private static final int THETA_STEPS = 200;
    private static final int PHI_STEPS = 200;

    // ---- Cone geometry -------------------------------------------------
    // A molded "cake cone" profile instead of a mathematically perfect
    // frustum: a flared, rolled rim lip at the top, a gently concave taper
    // through the body, and a small blunt tip instead of a razor point.
    // See coneRadiusAt() for the actual curve.
    private static final double CONE_RIM_Y = 2.0;
    private static final double CONE_TIP_Y = 5.6;
    private static final double CONE_RIM_RADIUS = 1.28;
    private static final double RIM_LIP_FRACTION = 0.05; // fraction of cone height that's the rolled lip
    private static final double RIM_FLARE_AMOUNT = 0.18;  // how much wider the lip flares vs. the body
    private static final double CONE_TIP_MIN_RADIUS = 0.06; // blunt tip floor, not a perfect point
    
    private static final double SCOOP_SQUASH = 0.82;
    private static final double SCOOP_R = 1.38;

    private static final double SCOOP_BOTTOM_Y = 1.55;

    private static final double[][] SPRINKLES = {
        { 0.20, 2.79, 0 }, { 0.60, 2.59, 1 }, { 1.00, 2.84, 2 }, { 1.35, 2.54, 3 }, { 1.70, 2.72, 0 },
        { 2.10, 2.56, 1 }, { 2.50, 2.81, 2 }, { 2.90, 2.64, 3 }, { 3.30, 2.74, 0 }, { 3.70, 2.54, 1 },
        { 4.10, 2.82, 2 }, { 4.50, 2.59, 3 }, { 4.90, 2.69, 0 }, { 5.30, 2.54, 1 }, { 5.70, 2.79, 2 },
        { 6.10, 2.64, 3 }, { 0.40, 2.29, 0 }, { 0.90, 2.19, 1 }, { 1.50, 2.34, 2 }, { 2.00, 2.14, 3 },
        { 2.60, 2.32, 0 }, { 3.20, 2.22, 1 }, { 3.80, 2.36, 2 }, { 4.40, 2.09, 3 }, { 5.00, 2.29, 0 },
        { 5.60, 2.19, 1 }, { 0.10, 1.99, 2 }, { 1.90, 1.94, 3 }
    };

    private static final double SPRINKLE_THETA_TOL = 0.15;
    private static final double SPRINKLE_PHI_TOL = 0.12;
    private static final double ONE_PI = Math.PI;
    private static final double TWO_PI = 2.0 * ONE_PI;

    private final int[][] sprinkleMap = new int[PHI_STEPS][THETA_STEPS];

    // Neapolitan color palette
    private final String scoopStrawberryColor = "\u001B[38;5;211m";    // Strawberry Pink
    private final String scoopVanillaColor = "\u001B[38;5;255m";    // Vanilla White
    private final String scoopChocolateColor = "\u001B[38;5;130m"; // Chocolate Brown
    private String scoopColor; // The one we pick
    private String[] sprinkleColors;

    private double rotationAngle = 0; // Continuous spin around central vertical axis

    private static final double CAMERA_DISTANCE_FAR = 6.1;
    private static final double CAMERA_DISTANCE_NEAR = 4.1;
    private static final double TILT_FAR = 0.22;
    private static final double TILT_NEAR = 0.36;
    private static final double Y_OFFSET_FAR = 2.0;
    private static final double Y_OFFSET_NEAR = 1.9; // centers the rim/sprinkle seam on screen

    // Set once per frame in renderGeometry, read by projectPoint.
    private double cameraDistance = CAMERA_DISTANCE_FAR;
    private double cameraTilt = TILT_FAR;
    private double cameraYOffset = Y_OFFSET_FAR;

    public IceCreamConeLoader() {
        super(ICE_CREAM_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        sprinkleColors = new String[] {
            "\u001B[38;5;206m", "\u001B[38;5;51m", "\u001B[38;5;220m", "\u001B[38;5;46m"
        };

        for (int[] row : sprinkleMap) {
            java.util.Arrays.fill(row, -1);
        }

        switch((int)(Math.random() * 3)) {
            case 0:
                scoopColor = scoopChocolateColor; break;
            case 1:
                scoopColor = scoopVanillaColor; break;
            case 2:
                scoopColor = scoopStrawberryColor; break;
        }

        // Build the sprinkle map once: for every grid cell, 
        // find the nearest fixed sprinkle within tolerance
        // (with theta wraparound so sprinkles near 0/2*PI still connect).
        for (int pIndex = 0; pIndex < PHI_STEPS; pIndex++) {
            double phi = pIndex * (Math.PI / PHI_STEPS);
            for (int tIndex = 0; tIndex < THETA_STEPS; tIndex++) {
                double theta = tIndex * (TWO_PI / THETA_STEPS);
                for (int i = 0; i < SPRINKLES.length; i++) {
                    double dTheta = Math.abs(theta - SPRINKLES[i][0]);
                    double dPhi = Math.abs(phi - SPRINKLES[i][1]);
                    if (dTheta > ONE_PI) {
                        dTheta = TWO_PI - dTheta;
                    }
                    if (dTheta < SPRINKLE_THETA_TOL && dPhi < SPRINKLE_PHI_TOL) {
                        sprinkleMap[pIndex][tIndex] = (int) SPRINKLES[i][2];
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        rotationAngle += 0.03;

        double zoomFactor = (Math.cos(rotationAngle) + 1.0) / 2.0;

        cameraDistance = CAMERA_DISTANCE_FAR - zoomFactor * (CAMERA_DISTANCE_FAR - CAMERA_DISTANCE_NEAR);
        cameraTilt = TILT_FAR * (1.0 - zoomFactor) + TILT_NEAR * zoomFactor;
        cameraYOffset = Y_OFFSET_FAR * (1.0 - zoomFactor) + Y_OFFSET_NEAR * zoomFactor;

        for (int tIndex = 0; tIndex < THETA_STEPS; tIndex++) {
            double theta = tIndex * (TWO_PI / THETA_STEPS);

            for (int pIndex = 0; pIndex < PHI_STEPS; pIndex++) {
                double phi = pIndex * (Math.PI / PHI_STEPS);

                // ==========================================
                // ZONE 1: THE WAFFLE CONE
                // ==========================================
                renderCone(theta, pIndex, outputBuffer, zBuffer);

                // ==========================================
                // ZONE 2: CHOCOLATE SCOOP (Bottom, sits in the cone's rim)
                // ==========================================
                renderIceCreamScoop(phi, theta, SCOOP_BOTTOM_Y, SCOOP_R, scoopColor,
                        sprinkleMap[pIndex][tIndex], outputBuffer, zBuffer);

            }
        }
    }

    private void renderCone(double theta, int pIndex, String[] outputBuffer, double[] zBuffer) {
        double coneHeight = CONE_TIP_Y - CONE_RIM_Y;
        double t = pIndex / (double) PHI_STEPS; // 0 at the rim, 1 at the tip
        double coneY = CONE_RIM_Y + t * coneHeight;
        double currentRadius = coneRadiusAt(t);
        boolean isRimLip = t < RIM_LIP_FRACTION;

        double cx0 = currentRadius * Math.cos(theta);
        double cy0 = coneY;
        double cz0 = currentRadius * Math.sin(theta);

        String coneColor;
        if (isRimLip) {
            // The rolled rim lip reads as one continuous toasted edge band,
            // distinct from the crosshatched body below it.
            coneColor = scoopColor;
        } else {
            // Diamond waffle crosshatch: darker toasted ridges where the two
            // diagonal wave sets cross.
            double wafflePattern = Math.sin(theta * 12 + coneY * 5) * Math.sin(theta * 12 - coneY * 5);
            coneColor = wafflePattern > 0.25 ? "\u001B[38;5;130m" : "\u001B[38;5;173m";
        }

        // Outward normal tilted slightly down along the cone's slanted wall.
        projectPoint(cx0, cy0, cz0, Math.cos(theta), -0.35, Math.sin(theta), coneColor, outputBuffer, zBuffer);
    }

    /**
     * Molded cake-cone radius profile: a flared rolled lip right at the rim
     * (wider than the body just beneath it -- the single biggest cue that
     * reads as a cone's cup rather than a party hat), then a gently concave
     * taper through the body, ending in a small blunt tip instead of a
     * perfect mathematical point.
     */
    private double coneRadiusAt(double t) {
        if (t < RIM_LIP_FRACTION) {
            double p = t / RIM_LIP_FRACTION;
            double flare = 1.0 + RIM_FLARE_AMOUNT * (1.0 - p);
            return CONE_RIM_RADIUS * flare;
        }
        double bodyT = (t - RIM_LIP_FRACTION) / (1.0 - RIM_LIP_FRACTION);
        double taper = Math.pow(1.0 - bodyT, 0.82);
        double barrel = 1.0 + 0.09 * Math.sin(Math.PI * bodyT) * (1.0 - bodyT * 0.5);
        double r = CONE_RIM_RADIUS * taper * barrel;
        return Math.max(r, CONE_TIP_MIN_RADIUS);
    }

    private void renderIceCreamScoop(double phi, double theta, double centerY, double radius, String baseColor,
                                     int sprinkleColorIdx, String[] outputBuffer, double[] zBuffer) {
        double r = radius;

        if (phi > (Math.PI - 0.2)) {
            double d = Math.PI - phi;
            r += 0.16 * (0.2 - d) / 0.2;
        }

        double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);

        double sx = r * sinPhi * Math.cos(theta);
        double sy = centerY + SCOOP_SQUASH * r * cosPhi;
        double sz = r * sinPhi * Math.sin(theta);

        // Approximate surface normal from the unsquashed sphere -- good
        // enough for cheap, pleasant shading.
        double nx = sinPhi * Math.cos(theta);
        double ny = cosPhi;
        double nz = sinPhi * Math.sin(theta);

        String color = baseColor;
        if (sprinkleColorIdx != -1) {
            color = sprinkleColors[sprinkleColorIdx];
        }

        projectPoint(sx, sy, sz, nx, ny, nz, color, outputBuffer, zBuffer);
    }

    private void projectPoint(double x0, double y0, double z0, double nx, double ny, double nz,
                              String color, String[] outputBuffer, double[] zBuffer) {
        // --- STEP 0: FRAME THE DWELL TARGET ---
        // Shifting world-space y before anything else re-centers whichever
        // point the current dwell wants front-and-center on screen.
        double worldY = y0 - cameraYOffset;

        // --- STEP 1: TWIRL AROUND CENTRAL VERTICAL AXIS (Y-AXIS) ---
        double sinR = Math.sin(rotationAngle), cosR = Math.cos(rotationAngle);
        double x1 = x0 * cosR - z0 * sinR;
        double y1 = worldY;
        double z1 = x0 * sinR + z0 * cosR;

        double nx1 = nx * cosR - nz * sinR;
        double ny1 = ny;
        double nz1 = nx * sinR + nz * cosR;

        // --- STEP 2: APPLY CAMERA TILT (PITCH AROUND X-AXIS) ---
        double sinA = Math.sin(cameraTilt), cosA = Math.cos(cameraTilt);
        double x2 = x1;
        double y2 = y1 * cosA - z1 * sinA;
        double z2 = y1 * sinA + z1 * cosA;

        double nx2 = nx1;
        double ny2 = ny1 * cosA - nz1 * sinA;
        double nz2 = ny1 * sinA + nz1 * cosA;

        // --- STEP 3: PERSPECTIVE PROJECTION & SHADING ---
        double D = 1.0 / (z2 + cameraDistance);

        int x = (int) (40 + 35 * D * x2 * 1.4); // Aspect ratio balance multiplier
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