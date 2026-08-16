public class AppleLoader extends Loader {
    private static final StatusStage[] APPLE_STAGES = {
            new StatusStage(25, "Growing apple orchard:"),
            new StatusStage(50, "Ripening under the sun:"),
            new StatusStage(75, "Polishing apple skin:"),
            new StatusStage(100, "Freshly Picked!")
    };

    // --- Skin pattern styles used to distinguish varieties beyond flat color ---
    private enum SkinPattern {
        SOLID, // near-uniform color (very subtle noise only)
        STRIPED, // vertical candy-stripe bands (Gala, Fuji, faint on Red Delicious)
        MOTTLED, // soft irregular blotches (Honeycrisp)
        FRECKLED_RUSSET, // fine dark freckling + rough russet cap near the stem (Golden Delicious)
        SPECKLED_LENTICEL, // scattered light lenticel dots over a dark base (Granny Smith, Cosmic Crisp)
        ONE_SIDED_BLUSH // sun-facing cheek blush fading to the base color (Pink Lady)
    }

    // Randomized configuration attributes (per-variety)
    private int[] baseColor;
    private int[] accentColor;
    private SkinPattern pattern;
    private double patternIntensity;
    private double stripeCount;

    private double baseRadius;
    private double heightScale;
    private double skew;
    private double shapeExponent;
    private double ribAmplitude;
    private static final int RIB_COUNT = 5;

    private int[] stemColorRGB = { 101, 67, 33 };
    private int[] leafColorRGB = { 45, 125, 55 };

    private double A = 0;
    private double B = 0;

    public AppleLoader() {
        super(APPLE_STAGES, 80, 22);
    }

    public AppleLoader(int w, int h) {
        super(APPLE_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // Choose a unique variety at runtime (1 to 9)
        int variant = (int) (Math.random() * 9) + 1;
        switch (variant) {
            case 1: // --- RED DELICIOUS ---
                baseColor = new int[] { 140, 15, 20 };
                accentColor = new int[] { 92, 10, 15 };
                pattern = SkinPattern.STRIPED;
                patternIntensity = 0.18;
                stripeCount = 7;
                baseRadius = 1.6;
                heightScale = 1.28;
                skew = 0.24;
                shapeExponent = 0.85;
                ribAmplitude = 0.14;
                break;
            case 2: // --- GRANNY SMITH ---
                baseColor = new int[] { 112, 178, 58 };
                accentColor = new int[] { 232, 236, 195 };
                pattern = SkinPattern.SPECKLED_LENTICEL;
                patternIntensity = 0.05; // dot chance
                baseRadius = 1.85;
                heightScale = 0.92;
                skew = 0.0;
                shapeExponent = 1.0;
                ribAmplitude = 0.03;
                break;
            case 3: // --- GOLDEN DELICIOUS ---
                baseColor = new int[] { 219, 186, 64 };
                accentColor = new int[] { 124, 88, 46 };
                pattern = SkinPattern.FRECKLED_RUSSET;
                patternIntensity = 0.5;
                baseRadius = 1.55;
                heightScale = 1.38;
                skew = 0.06;
                shapeExponent = 0.95;
                ribAmplitude = 0.02;
                break;
            case 4:
            default: // --- COSMIC CRISP ---
                baseColor = new int[] { 112, 18, 28 };
                accentColor = new int[] { 234, 216, 184 };
                pattern = SkinPattern.SPECKLED_LENTICEL;
                patternIntensity = 0.09;
                baseRadius = 1.9;
                heightScale = 1.0;
                skew = 0.05;
                shapeExponent = 1.0;
                ribAmplitude = 0.05;
                break;
            case 5: // --- GALA ---
                baseColor = new int[] { 226, 178, 72 };
                accentColor = new int[] { 196, 64, 42 };
                pattern = SkinPattern.STRIPED;
                patternIntensity = 0.55;
                stripeCount = 10;
                baseRadius = 1.75;
                heightScale = 1.05;
                skew = 0.12;
                shapeExponent = 0.9;
                ribAmplitude = 0.04;
                break;
            case 6: // --- HONEYCRISP ---
                baseColor = new int[] { 228, 48, 48 };
                accentColor = new int[] { 198, 252, 108 };
                pattern = SkinPattern.MOTTLED;
                patternIntensity = 0.6;
                baseRadius = 1.95;
                heightScale = 0.98;
                skew = 0.0;
                shapeExponent = 1.0;
                ribAmplitude = 0.03;
                break;
            case 7: // --- FUJI ---
                baseColor = new int[] { 206, 196, 128 };
                accentColor = new int[] { 188, 78, 88 };
                pattern = SkinPattern.STRIPED;
                patternIntensity = 0.7;
                stripeCount = 14;
                baseRadius = 1.9;
                heightScale = 1.0;
                skew = 0.05;
                shapeExponent = 1.0;
                ribAmplitude = 0.03;
                break;
            case 8: // --- PINK LADY ---
                baseColor = new int[] { 178, 196, 96 };
                accentColor = new int[] { 214, 86, 118 };
                pattern = SkinPattern.ONE_SIDED_BLUSH;
                patternIntensity = 1.0;
                baseRadius = 1.6;
                heightScale = 1.2;
                skew = 0.18;
                shapeExponent = 0.88;
                ribAmplitude = 0.06;
                break;
            case 9: // --- MCINTOSH ---
                baseColor = new int[] { 228, 48, 48 };
                accentColor = new int[] { 158, 176, 86 };
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
                // fight each other. Both naturally taper to ~0 at the calyx pole (phi=0) and
                // the stem pole (phi=pi).
                double radial = baseRadius * Math.pow(Math.max(0, sinPhi), shapeExponent)
                        * (1.0 - skew * cosPhi);

                // Five-point calyx "crown": real apples show faint pentagonal ribbing flaring
                // out near the blossom end (phi -> 0, opposite the stem at phi -> pi).
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

    // Determines the unshaded skin RGB at a given surface
    // coordinate based on the variety's pattern.
    private int[] computeSkinColor(double theta, double phi, double sinPhi, double cosPhi) {
        switch (pattern) {
            case STRIPED: {
                return computeStripedSkin(theta, phi);
            }
            case MOTTLED: {
                return computeMottledSkin(theta, phi);
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
                return computeLenticelSkin(theta, phi, sinPhi);
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

    // Renders irregular, narrow color streaks running roughly pole-to-pole. Real
    // striped varieties (Gala, Fuji) show thin, uneven streaks over a lighter base
    // rather than bold, evenly-spaced bands -- evenly-spaced full-intensity bands
    // is what reads as a beach ball. Each "sector" (there are `stripeCount` of them
    // around the circumference) gets its own randomized streak
    // offset/width/brightness so the pattern feels organic instead of mechanically
    // regular, and neighboring sectors are checked so streaks aren't clipped at
    // sector boundaries.
    private int[] computeStripedSkin(double theta, double phi) {
        int stripeCountInt = Math.max(1, (int) Math.round(stripeCount));
        double sectorWidth = (2 * Math.PI) / stripeCountInt;
        double pos = theta / sectorWidth;
        int sector = (int) Math.floor(pos);

        double bestAccent = 0.0;
        for (int d = -1; d <= 1; d++) {
            int s = sector + d;
            int wrappedS = ((s % stripeCountInt) + stripeCountInt) % stripeCountInt;

            double offset = hash(wrappedS * 4.3 + 1.1, 0.0); // streak position within its sector
            double width = 0.10 + 0.16 * hash(wrappedS * 7.9 + 2.7, 0.0); // narrow streak, not a half-sector band
            double strength = 0.55 + 0.45 * hash(wrappedS * 2.1 + 5.3, 0.0);

            // A gentle wave keeps streaks from being ruler-straight, and per-band noise
            // along phi breaks each streak into shorter, discontinuous segments (real apple
            // streaks rarely run the full stem-to-calyx length unbroken).
            double wave = 0.15 * Math.sin(phi * 4.0 + wrappedS * 2.6);
            double band = Math.floor(phi * 5.0);
            double lengthNoise = hash(wrappedS * 6.1 + 3.3, band);
            double lengthFade = smoothstep(0.15, 0.45, lengthNoise);

            double center = s + offset + wave; // unwrapped `s` keeps distance continuous across the theta seam
            double dist = Math.abs(pos - center);
            double edge = 1.0 - smoothstep(width * 0.5, width, dist);

            double accent = edge * strength * lengthFade;
            if (accent > bestAccent)
                bestAccent = accent;
        }

        double t = clamp01(bestAccent) * patternIntensity;
        return lerp(baseColor, accentColor, t);
    }

    // Blotchy, irregular mottling (Honeycrisp). Plain layered sin/cos noise reads
    // as "wavy" because every octave shares the same theta/phi grid, so their
    // zero-crossings line up into a visible interference pattern. Warping the
    // sampling coordinates with a coarser, incommensurate noise field first breaks
    // that alignment; a handful of randomly placed, irregularly-sized soft-edged
    // "blotch" kernels then give the fadey, splotchy look real mottling has, rather
    // than a mathematical ripple.
    private int[] computeMottledSkin(double theta, double phi) {
        double warpT = 0.35 * Math.sin(phi * 2.3 + 0.6) + 0.25 * Math.sin(theta * 1.7 - phi * 1.1 + 2.0);
        double warpP = 0.30 * Math.sin(theta * 2.1 + 1.4) + 0.20 * Math.sin(phi * 1.3 + theta * 0.9 - 0.7);
        double wTheta = theta + warpT;
        double wPhi = phi + warpP;

        double accum = 0.0;
        final int BLOTCHES = 9;
        for (int i = 0; i < BLOTCHES; i++) {
            double seed = i * 17.13;
            double cTheta = hash(seed, 1.0) * 2 * Math.PI;
            double cPhi = 0.15 * Math.PI + hash(seed, 2.0) * 0.7 * Math.PI;
            double size = 0.55 + 0.65 * hash(seed, 3.0); // irregular blotch radius

            double dt = wTheta - cTheta;
            if (dt > Math.PI)
                dt -= 2 * Math.PI;
            if (dt < -Math.PI)
                dt += 2 * Math.PI;
            double dp = (wPhi - cPhi) * 1.6; // slight vertical stretch

            double dist = Math.sqrt(dt * dt + dp * dp) / size;
            double falloff = 1.0 - smoothstep(0.4, 1.0, dist); // soft, fadey edge
            accum += falloff;
        }

        double t = clamp01(accum * 0.55) * patternIntensity;
        t = t * t * (3 - 2 * t); // extra smoothing so blotches blend rather than threshold sharply
        return lerp(baseColor, accentColor, t);
    }

    // Renders small, roughly circular lenticel dots on a coarse jittered grid with
    // a soft radial falloff. A coherent dot footprint (several adjacent samples) is
    // what makes these read as scattered dots; thresholding an independent random
    // value per surface sample (the old approach) has no spatial coherence and
    // aliases into flickering "confetti" noise at this render resolution.
    private int[] computeLenticelSkin(double theta, double phi, double sinPhi) {
        final int cellsTheta = 16;
        final int cellsPhi = 11;
        double cellTheta = (2 * Math.PI) / cellsTheta;
        double cellPhi = Math.PI / cellsPhi;

        int baseCti = (int) Math.floor(theta / cellTheta);
        int baseCpi = (int) Math.floor(phi / cellPhi);

        double best = 1.0; // smallest normalized distance to any nearby lenticel center found

        for (int dCti = -1; dCti <= 1; dCti++) {
            for (int dCpi = -1; dCpi <= 1; dCpi++) {
                int cti = baseCti + dCti;
                int cpi = baseCpi + dCpi;
                if (cpi < 0 || cpi >= cellsPhi)
                    continue; // no wrap at the poles
                int wrappedCti = ((cti % cellsTheta) + cellsTheta) % cellsTheta;

                double seedRoll = hash(wrappedCti * 3.7 + 0.5, cpi * 5.3 + 1.5);
                if (seedRoll <= (1.0 - patternIntensity))
                    continue; // this cell has no lenticel

                double jitterT = hash(wrappedCti * 9.1 + 4.0, cpi * 2.3 + 7.0);
                double jitterP = hash(wrappedCti * 2.9 + 8.0, cpi * 6.7 + 1.0);
                double centerTheta = (cti + 0.15 + 0.7 * jitterT) * cellTheta;
                double centerPhi = (cpi + 0.15 + 0.7 * jitterP) * cellPhi;

                double dt = theta - centerTheta;
                if (dt > Math.PI)
                    dt -= 2 * Math.PI;
                if (dt < -Math.PI)
                    dt += 2 * Math.PI;
                // Scale the theta component by sinPhi so dots stay round instead of stretching
                // near the poles, where a fixed angular width covers less physical distance.
                double dtPhysical = dt * Math.max(sinPhi, 0.15);
                double dp = phi - centerPhi;

                double size = 0.06 + 0.05 * hash(wrappedCti * 1.3 + cpi * 7.1, cpi * 4.4 + cti * 0.7);
                double dist = Math.sqrt(dtPhysical * dtPhysical + dp * dp) / size;

                if (dist < best)
                    best = dist;
            }
        }

        if (best < 1.0) {
            double edge = 1.0 - smoothstep(0.55, 1.0, best);
            return lerp(baseColor, accentColor, clamp01(edge));
        }
        return baseColor;
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
        double D = 1.0 / (z2 + 5.0);
        int x = (int) (40 + 36 * D * x2); // Terminal cell aspect compensation (cells ~1.8x taller than wide)
        int y = (int) (11 + 20 * D * y2);
        int o = x + window_width * y;

        // Vector calculations for lighting
        double nx2, ny2, nz1;
        if (!isAccessory) {
            // Approximate outward surface normal (standard rounded-fruit normal; the
            // shape's skew/ribbing perturbations are subtle enough that this reads
            // correctly at this resolution without a full analytic derivative).
            double nx = Math.sin(phi) * Math.cos(theta);
            double ny = Math.cos(phi);
            double nz = Math.sin(phi) * Math.sin(theta);

            double ny1 = ny * cosA - nz * sinA;
            nz1 = ny * sinA + nz * cosA;
            nx2 = nx * cosB - ny1 * sinB;
            ny2 = nx * sinB + ny1 * cosB;
        } else {
            nx2 = 0.0;
            ny2 = 1.0;
            nz1 = 0.0;
        }

        // Fixed overhead illumination vector
        double L = nx2 * 0.0 + ny2 * 0.8 - nz1 * 0.6;

        if (window_height > y && y > 0 && x > 0 && window_width > x && D > (zBuffer[o] + 0.0001)) {
            zBuffer[o] = D;

            double Lc = clamp01((L + 1.0) / 2.0);
            // Modulate the true color by lighting (not just the ASCII density ramp) so
            // shading reads as real 3D shading rather than a flat-colored silhouette.
            double shade = 0.45 + 0.65 * Lc;
            int r = (int) clampByte(colorRGB[0] * shade);
            int g = (int) clampByte(colorRGB[1] * shade);
            int b = (int) clampByte(colorRGB[2] * shade);
            String colorCode = "\u001B[38;2;" + r + ";" + g + ";" + b + "m";

            int charIndex = (int) (Math.round(Lc * 11));
            if (charIndex < 0)
                charIndex = 0;
            if (charIndex > 11)
                charIndex = 11;

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
        return new int[] {
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