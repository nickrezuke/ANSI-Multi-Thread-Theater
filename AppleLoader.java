// TODO: fix the gala, fuju, cosmic crisp variants.  maybe zoom in?

public class AppleLoader extends Loader {
    private static final StatusStage[] APPLE_STAGES = {
        new StatusStage(25, "Growing apple orchard:"),
        new StatusStage(50, "Ripening under the sun:"),
        new StatusStage(75, "Polishing apple skin:"),
        new StatusStage(100, "Freshly Picked!")
    };

    // --- Skin pattern styles used to distinguish varieties beyond flat color ---
    private enum SkinPattern {
        SOLID,              // near-uniform color (very subtle noise only)
        STRIPED,            // vertical candy-stripe bands (Gala, Fuji, faint on Red Delicious)
        MOTTLED,            // soft irregular blotches (Honeycrisp)
        FRECKLED_RUSSET,    // fine dark freckling + rough russet cap near the stem (Golden Delicious)
        SPECKLED_LENTICEL,  // scattered light lenticel dots over a dark base (Granny Smith, Cosmic Crisp)
        ONE_SIDED_BLUSH     // sun-facing cheek blush fading to the base color (Pink Lady)
    }

    // Randomized configuration attributes (per-variety)
    private int[] baseColor;
    private int[] accentColor;
    private SkinPattern pattern;
    private double patternIntensity;
    private double stripeCount;

    private double baseRadius;
    private double heightScale;
    private double skew;          // >0 shifts the widest point up toward the stem (conical taper)
    private double shapeExponent; // controls how quickly the profile rounds off vs. flattens
    private double ribAmplitude;  // strength of the 5-point calyx "crown" bumps
    private static final int RIB_COUNT = 5; // apples botanically have a 5-point calyx

    private int[] stemColorRGB = {101, 67, 33};
    private int[] leafColorRGB = {45, 125, 55};

    private double A = 0; // Rotation around X-axis (Pitch)
    private double B = 0; // Rotation around Z-axis (Yaw)

    public AppleLoader() {
        super(APPLE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // Choose a unique, recognizable variety at runtime (1 to 8)
        int variant = (int) (Math.random() * 8) + 1;
        variant = 8; //TODO REMOVE THIS TEST LINE
        switch (variant) {
            case 1: // --- RED DELICIOUS: deep uniform red, tall & conical, 5-point calyx crown ---
                baseColor = new int[]{140, 15, 20};
                accentColor = new int[]{92, 10, 15};
                pattern = SkinPattern.STRIPED;
                patternIntensity = 0.18;
                stripeCount = 7;
                baseRadius = 1.6;
                heightScale = 1.28;
                skew = 0.24;
                shapeExponent = 0.85;
                ribAmplitude = 0.14;
                break;
            case 2: // --- GRANNY SMITH: bright crisp green, round, smooth with faint pale lenticels ---
                baseColor = new int[]{112, 178, 58};
                accentColor = new int[]{232, 236, 195};
                pattern = SkinPattern.SPECKLED_LENTICEL;
                patternIntensity = 0.05; // dot chance
                baseRadius = 1.85;
                heightScale = 0.92;
                skew = 0.0;
                shapeExponent = 1.0;
                ribAmplitude = 0.03;
                break;
            case 3: // --- GOLDEN DELICIOUS: elongated golden yellow, russet cap near stem, fine freckles ---
                baseColor = new int[]{219, 186, 64};
                accentColor = new int[]{124, 88, 46};
                pattern = SkinPattern.FRECKLED_RUSSET;
                patternIntensity = 0.5;
                baseRadius = 1.55;
                heightScale = 1.38;
                skew = 0.06;
                shapeExponent = 0.95;
                ribAmplitude = 0.02;
                break;
            case 4: default: // --- COSMIC CRISP: large dark maroon-red with bright cream "starry" lenticels ---
                baseColor = new int[]{112, 18, 28};
                accentColor = new int[]{234, 216, 184};
                pattern = SkinPattern.SPECKLED_LENTICEL;
                patternIntensity = 0.09;
                baseRadius = 1.9;
                heightScale = 1.0;
                skew = 0.05;
                shapeExponent = 1.0;
                ribAmplitude = 0.05;
                break;
            case 5: // --- GALA: round-conical, bold red-orange candy stripes over yellow ---
                baseColor = new int[]{226, 178, 72};
                accentColor = new int[]{196, 64, 42};
                pattern = SkinPattern.STRIPED;
                patternIntensity = 0.55;
                stripeCount = 10;
                baseRadius = 1.75;
                heightScale = 1.05;
                skew = 0.12;
                shapeExponent = 0.9;
                ribAmplitude = 0.04;
                break;
            case 6: // --- HONEYCRISP: large & round, blotchy red-orange mottling over pale yellow-green ---
                baseColor = new int[]{198, 202, 118};
                accentColor = new int[]{188, 58, 48};
                pattern = SkinPattern.MOTTLED;
                patternIntensity = 0.6;
                baseRadius = 1.95;
                heightScale = 0.98;
                skew = 0.0;
                shapeExponent = 1.0;
                ribAmplitude = 0.03;
                break;
            case 7: // --- FUJI: round, dense pink-red striping over yellow-green ---
                baseColor = new int[]{206, 196, 128};
                accentColor = new int[]{188, 78, 88};
                pattern = SkinPattern.STRIPED;
                patternIntensity = 0.7;
                stripeCount = 14;
                baseRadius = 1.9;
                heightScale = 1.0;
                skew = 0.05;
                shapeExponent = 1.0;
                ribAmplitude = 0.03;
                break;
            case 8: // --- PINK LADY: elongated & conical, sun-facing pink-red cheek over green-yellow ---
                baseColor = new int[]{178, 196, 96};
                accentColor = new int[]{214, 86, 118};
                pattern = SkinPattern.ONE_SIDED_BLUSH;
                patternIntensity = 1.0;
                baseRadius = 1.6;
                heightScale = 1.2;
                skew = 0.18;
                shapeExponent = 0.88;
                ribAmplitude = 0.06;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // --- STEP 1: RENDER MAIN APPLE BODY ---
        for (int tIndex = 0; tIndex < 180; tIndex++) {
            double theta = tIndex * (2 * Math.PI / 180);
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

            for (int pIndex = 0; pIndex < 180; pIndex++) {
                double phi = pIndex * (Math.PI / 180);
                double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);

                // Meridian profile: radial distance from the core axis and height are computed
                // independently (unlike a plain sphere) so shape (skew/taper) and height don't
                // fight each other. Both naturally taper to ~0 at the calyx pole (phi=0) and the
                // stem pole (phi=pi).
                double radial = baseRadius * Math.pow(Math.max(0, sinPhi), shapeExponent)
                        * (1.0 - skew * cosPhi);

                // Five-point calyx "crown": real apples show faint pentagonal ribbing flaring out
                // near the blossom end (phi -> 0, opposite the stem at phi -> pi).
                double ribWeight = Math.pow(Math.max(0, cosPhi), 2.0);
                radial += ribAmplitude * ribWeight * Math.cos(RIB_COUNT * theta);
                radial = Math.max(radial, 0.02);

                double x0 = radial * cosTheta;
                double z0 = radial * sinTheta;
                double y0 = heightScale * baseRadius * cosPhi * 0.82;

                int[] skinColor = computeSkinColor(theta, phi, sinPhi, cosPhi);
                projectAndBufferPoint(x0, y0, z0, theta, phi, skinColor, outputBuffer, zBuffer, false);
            }
        }

        // --- STEP 2: RENDER PROCEDURAL STEM ---
        double stemBaseY = -baseRadius * heightScale * 0.82;
        for (double t = 0; t <= 1.0; t += 0.05) {
            double x0 = 0.3 * Math.pow(t, 2);
            double y0 = stemBaseY - (0.8 * t);
            double z0 = 0.1 * t;

            for (double sTh = 0; sTh < 2 * Math.PI; sTh += 0.8) {
                double thickness = 0.08;
                double sx0 = x0 + Math.cos(sTh) * thickness;
                double sy0 = y0;
                double sz0 = z0 + Math.sin(sTh) * thickness;

                projectAndBufferPoint(sx0, sy0, sz0, 0, 0, stemColorRGB, outputBuffer, zBuffer, true);
            }
        }

        // --- STEP 3: RENDER PROCEDURAL LEAF ---
        double leafAttachY = stemBaseY - 0.5;
        double leafAttachX = 0.3 * Math.pow(0.6, 2);
        double leafAttachZ = 0.1 * 0.6;

        for (double u = 0; u <= 1.0; u += 0.05) {
            double maxW = 0.3 * Math.sin(u * Math.PI);
            for (double v = -maxW; v <= maxW; v += 0.05) {
                double x0 = leafAttachX + (0.8 * u);
                double y0 = leafAttachY - (0.2 * u) + (0.1 * Math.sin(u * Math.PI));
                double z0 = leafAttachZ + v;

                projectAndBufferPoint(x0, y0, z0, 0, 0, leafColorRGB, outputBuffer, zBuffer, true);
            }
        }

        A += 0.04;
        B += 0.02;
    }

    // Determines the unshaded skin RGB at a given surface coordinate based on the variety's pattern.
    private int[] computeSkinColor(double theta, double phi, double sinPhi, double cosPhi) {
        switch (pattern) {
            case STRIPED: {
                double raw = 0.5 + 0.5 * Math.sin(theta * stripeCount + Math.sin(phi * 3.0) * 0.6);
                double t = clamp01((raw - 0.4) / 0.2) * patternIntensity;
                return lerp(baseColor, accentColor, t);
            }
            case MOTTLED: {
                double n = Math.sin(theta * 3.1 + 0.7) * Math.cos(phi * 4.3 + 1.3)
                        + 0.5 * Math.sin(theta * 7.7 + 2.1) * Math.cos(phi * 2.6 + 0.4)
                        + 0.3 * Math.sin(theta * 1.3) * Math.cos(phi * 6.1);
                double norm = clamp01((n + 1.8) / 3.6);
                double t = smoothstep(0.5, 0.78, norm) * patternIntensity;
                return lerp(baseColor, accentColor, t);
            }
            case FRECKLED_RUSSET: {
                int[] c = baseColor;
                double russetRange = 0.9; // radians of phi near the stem pole (phi -> pi)
                double distFromStem = Math.PI - phi;
                double russetWeight = distFromStem < russetRange ? (1.0 - distFromStem / russetRange) : 0.0;
                double patch = hash(theta * 3.0, phi * 3.0);
                if (russetWeight > 0.05 && patch < russetWeight * 0.85 * patternIntensity) {
                    c = accentColor;
                } else {
                    double freckle = hash(theta * 42.0 + 1.7, phi * 42.0 + 3.2);
                    if (freckle > 0.965) {
                        c = lerp(baseColor, accentColor, 0.7);
                    }
                }
                return c;
            }
            case SPECKLED_LENTICEL: {
                double dot = hash(theta * 60.0, phi * 60.0);
                if (dot > (1.0 - patternIntensity)) {
                    double size = hash(theta * 13.0 + 5.0, phi * 13.0 + 9.0);
                    return lerp(baseColor, accentColor, 0.6 + 0.4 * size);
                }
                return baseColor;
            }
            case ONE_SIDED_BLUSH: {
                double sunAzimuth = Math.PI * 0.25;
                double blush = Math.max(0, Math.cos(theta - sunAzimuth));
                double t = clamp01(blush * sinPhi * 1.2) * patternIntensity;
                return lerp(baseColor, accentColor, t);
            }
            case SOLID:
            default:
                return baseColor;
        }
    }

    private void projectAndBufferPoint(double x0, double y0, double z0, double theta, double phi,
                                        int[] colorRGB, String[] outputBuffer, double[] zBuffer, boolean isAccessory) {
        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinB = Math.sin(B), cosB = Math.cos(B);

        // Pitch transform (X-Axis)
        double x1 = x0;
        double y1 = y0 * cosA - z0 * sinA;
        double z1 = y0 * sinA + z0 * cosA;

        // Yaw transform (Z-Axis)
        double x2 = x1 * cosB - y1 * sinB;
        double y2 = x1 * sinB + y1 * cosB;
        double z2 = z1;

        // Depth projection matrix calculations
        double D = 1.0 / (z2 + 7.0);
        int x = (int) (40 + 36 * D * x2); // Terminal cell aspect compensation (cells ~1.8x taller than wide)
        int y = (int) (11 + 20 * D * y2);
        int o = x + window_width * y;

        // Vector calculations for lighting
        double nx2, ny2, nz1;
        if (!isAccessory) {
            // Approximate outward surface normal (standard rounded-fruit normal; the shape's
            // skew/ribbing perturbations are subtle enough that this reads correctly at this
            // resolution without a full analytic derivative).
            double nx = Math.sin(phi) * Math.cos(theta);
            double ny = Math.cos(phi);
            double nz = Math.sin(phi) * Math.sin(theta);

            double ny1 = ny * cosA - nz * sinA;
            nz1 = ny * sinA + nz * cosA;
            nx2 = nx * cosB - ny1 * sinB;
            ny2 = nx * sinB + ny1 * cosB;
        } else {
            nx2 = 0.0; ny2 = 1.0; nz1 = 0.0;
        }

        // Fixed overhead illumination vector
        double L = nx2 * 0.0 + ny2 * 0.8 - nz1 * 0.6;

        if (window_height > y && y > 0 && x > 0 && window_width > x && D > (zBuffer[o] + 0.0001)) {
            zBuffer[o] = D;

            double Lc = clamp01((L + 1.0) / 2.0);
            // Modulate the true color by lighting (not just the ASCII density ramp) so shading
            // reads as real 3D shading rather than a flat-colored silhouette.
            double shade = 0.45 + 0.65 * Lc;
            int r = (int) clampByte(colorRGB[0] * shade);
            int g = (int) clampByte(colorRGB[1] * shade);
            int b = (int) clampByte(colorRGB[2] * shade);
            String colorCode = "\u001B[38;2;" + r + ";" + g + ";" + b + "m";

            int charIndex = (int) (Math.round(Lc * 11));
            if (charIndex < 0) charIndex = 0;
            if (charIndex > 11) charIndex = 11;

            String lString = ".,-~:;=!*#$@";
            char asciiChar = lString.charAt(charIndex);

            outputBuffer[o] = colorCode + asciiChar + RESET;
        }
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double clampByte(double v) {
        return Math.max(0.0, Math.min(255.0, v));
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3 - 2 * t);
    }

    private static int[] lerp(int[] a, int[] b, double t) {
        t = clamp01(t);
        return new int[]{
                (int) (a[0] + (b[0] - a[0]) * t),
                (int) (a[1] + (b[1] - a[1]) * t),
                (int) (a[2] + (b[2] - a[2]) * t)
        };
    }

    private static double hash(double a, double b) {
        double s = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453;
        return s - Math.floor(s);
    }
}