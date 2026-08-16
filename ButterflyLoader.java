public class ButterflyLoader extends Loader {
    private static final StatusStage[] BUTTERFLY_STAGES = {
            new StatusStage(20, "Segmenting chitin thorax cylinders:"),
            new StatusStage(50, "Mounting anatomical wing hinges:"),
            new StatusStage(80, "Splicing species-specific cell vein maps:"),
            new StatusStage(100, "Lepidoptera Component Core Active!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    private static final int[] SIDES = { -1, 1 };

    // Surface tags used to route a point through the correct hinge / coloring logic
    private static final int SURFACE_BODY = 0;
    private static final int SURFACE_FOREWING = 1;
    private static final int SURFACE_HINDWING = 2;
    private static final int SURFACE_ANTENNA = 3;
    private static final int SURFACE_TAIL = 4;

    // Procedural wing pattern families, one per species variant
    private static final int PATTERN_MONARCH = 0;
    private static final int PATTERN_MORPHO = 1;
    private static final int PATTERN_SWALLOWTAIL = 2;
    private static final int PATTERN_PAINTED_LADY = 3;
    private static final int PATTERN_BUCKEYE = 4;

    // Real-time wing flap tuning: angle (radians) oscillates around FLAP_CENTER
    // by +/- FLAP_AMPLITUDE, driven directly off the wall clock.
    private static final double FLAP_CENTER = 0.55;
    private static final double FLAP_AMPLITUDE = 0.85;

    // --- Variant-selected appearance state (set once in initialize()) ---
    private int patternType;

    private String primaryColor; // main wing membrane color
    private String secondaryColor; // secondary shade for shimmer/stripes
    private String veinColor; // veins, borders, dark markings
    private String spotColor; // white/cream spots
    private String accentColor; // eyespot mid-ring / stripe accent
    private String eyespotInnerColor; // eyespot pupil color
    private String bodyColor;
    private String antennaColor;

    private boolean hasTails;
    private double flapFrequencyHz;

    // --- Variant-selected wing shape state ---
    // Each wing is a smooth ellipse-ish outline: 0 width at the body (u=0),
    // widening to "span" at "peak" (fraction of length from the body), then
    // tapering back to a point at the tip (u=length). No straight edges.
    private double forewingLength, forewingSpan, forewingPeak;
    private double hindwingLength, hindwingSpan, hindwingPeak;

    private double rotationAngle = 0.0;

    public ButterflyLoader() {
        super(BUTTERFLY_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        int variant = (int) (Math.random() * 5 + 1);
        switch (variant) {
            case 1:
                setupMonarch();
                break;
            case 2:
                setupBlueMorpho();
                break;
            case 3:
                setupTigerSwallowtail();
                break;
            case 4:
                setupPaintedLady();
                break;
            case 5:
                setupCommonBuckeye();
                break;
            default:
                setupMonarch();
        }
    }

    // -----------------------------------------------------------------
    // SPECIES VARIANTS
    // -----------------------------------------------------------------

    private void setupMonarch() {
        patternType = PATTERN_MONARCH;
        primaryColor = "\u001B[38;2;237;110;18m";
        secondaryColor = "\u001B[38;2;180;80;10m";
        veinColor = "\u001B[38;2;20;18;16m";
        spotColor = "\u001B[38;2;250;250;255m";
        accentColor = "\u001B[38;2;255;200;60m";
        eyespotInnerColor = veinColor;
        bodyColor = "\u001B[38;2;40;30;25m";
        antennaColor = "\u001B[38;2;20;18;16m";
        hasTails = false;
        flapFrequencyHz = 2.6;

        forewingLength = 1.15;
        forewingSpan = 0.62;
        forewingPeak = 0.30;
        hindwingLength = 0.85;
        hindwingSpan = 0.68;
        hindwingPeak = 0.48;
    }

    private void setupBlueMorpho() {
        patternType = PATTERN_MORPHO;
        primaryColor = "\u001B[38;2;35;110;235m";
        secondaryColor = "\u001B[38;2;90;170;255m";
        veinColor = "\u001B[38;2;10;10;15m";
        spotColor = "\u001B[38;2;230;240;255m";
        accentColor = "\u001B[38;2;180;210;255m";
        eyespotInnerColor = veinColor;
        bodyColor = "\u001B[38;2;25;25;35m";
        antennaColor = "\u001B[38;2;25;25;35m";
        hasTails = false;
        flapFrequencyHz = 1.9;

        forewingLength = 1.05;
        forewingSpan = 0.78;
        forewingPeak = 0.42;
        hindwingLength = 0.95;
        hindwingSpan = 0.80;
        hindwingPeak = 0.52;
    }

    private void setupTigerSwallowtail() {
        patternType = PATTERN_SWALLOWTAIL;
        primaryColor = "\u001B[38;2;245;205;40m";
        secondaryColor = "\u001B[38;2;230;180;20m";
        veinColor = "\u001B[38;2;15;15;15m";
        spotColor = "\u001B[38;2;250;250;250m";
        accentColor = "\u001B[38;2;70;140;230m";
        eyespotInnerColor = "\u001B[38;2;220;110;30m";
        bodyColor = "\u001B[38;2;25;22;20m";
        antennaColor = "\u001B[38;2;25;22;20m";
        hasTails = true;
        flapFrequencyHz = 2.1;

        forewingLength = 1.2;
        forewingSpan = 0.66;
        forewingPeak = 0.28;
        hindwingLength = 0.8;
        hindwingSpan = 0.62;
        hindwingPeak = 0.46;
    }

    private void setupPaintedLady() {
        patternType = PATTERN_PAINTED_LADY;
        primaryColor = "\u001B[38;2;210;110;45m";
        secondaryColor = "\u001B[38;2;150;70;30m";
        veinColor = "\u001B[38;2;30;22;18m";
        spotColor = "\u001B[38;2;245;245;240m";
        accentColor = "\u001B[38;2;60;90;150m";
        eyespotInnerColor = "\u001B[38;2;30;22;18m";
        bodyColor = "\u001B[38;2;45;35;28m";
        antennaColor = "\u001B[38;2;30;22;18m";
        hasTails = false;
        flapFrequencyHz = 2.8;

        forewingLength = 1.1;
        forewingSpan = 0.60;
        forewingPeak = 0.32;
        hindwingLength = 0.82;
        hindwingSpan = 0.64;
        hindwingPeak = 0.50;
    }

    private void setupCommonBuckeye() {
        patternType = PATTERN_BUCKEYE;
        primaryColor = "\u001B[38;2;150;120;80m";
        secondaryColor = "\u001B[38;2;120;95;60m";
        veinColor = "\u001B[38;2;35;28;20m";
        spotColor = "\u001B[38;2;235;225;200m";
        accentColor = "\u001B[38;2;200;150;40m";
        eyespotInnerColor = "\u001B[38;2;50;70;140m";
        bodyColor = "\u001B[38;2;60;48;35m";
        antennaColor = "\u001B[38;2;35;28;20m";
        hasTails = false;
        flapFrequencyHz = 2.3;

        forewingLength = 1.05;
        forewingSpan = 0.58;
        forewingPeak = 0.38;
        hindwingLength = 0.78;
        hindwingSpan = 0.60;
        hindwingPeak = 0.50;
    }

    // -----------------------------------------------------------------
    // FRAME RENDER
    // -----------------------------------------------------------------

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosR = Math.cos(rotationAngle);
        double sinR = Math.sin(rotationAngle);

        // Fixed cinematic pitch downward tilt (X-Axis) so top textures are readable
        double pitch = 0.45;
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);

        // Wing flap is driven directly off the wall clock (not the frame counter)
        // so the flap speed stays consistent regardless of render frame rate.
        double timeSeconds = System.currentTimeMillis() / 1000.0;
        double wingFlapAngle = FLAP_CENTER + FLAP_AMPLITUDE * Math.sin(2 * Math.PI * flapFrequencyHz * timeSeconds);

        double cameraDistance = 3.2;
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        renderBody(cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance, outputBuffer,
                zBuffer);
        renderAntennae(cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance, outputBuffer,
                zBuffer);
        renderWings(cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance, outputBuffer,
                zBuffer);
        if (hasTails) {
            renderTails(cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance, outputBuffer,
                    zBuffer);
        }

        rotationAngle += 0.012; // Slow deliberate tumble advance loop
    }

    // -----------------------------------------------------------------
    // COMPONENT 1: BODY -- three elongated spheroids (head/thorax/abdomen)
    // -----------------------------------------------------------------

    private void renderBody(double cosR, double sinR, double cosP, double sinP, double wingFlapAngle,
            double lightX, double lightY, double lightZ, double cameraDistance,
            String[] outputBuffer, double[] zBuffer) {
        // Head: small round sphere up front
        renderSpheroid(0.60, 0.072, 0.072, 0.085, cosR, sinR, cosP, sinP, wingFlapAngle,
                lightX, lightY, lightZ, cameraDistance, outputBuffer, zBuffer);
        // Thorax: thick muscular segment where the wings hinge on
        renderSpheroid(0.14, 0.105, 0.095, 0.24, cosR, sinR, cosP, sinP, wingFlapAngle,
                lightX, lightY, lightZ, cameraDistance, outputBuffer, zBuffer);
        // Abdomen: long tapering rear segment
        renderSpheroid(-0.34, 0.068, 0.062, 0.44, cosR, sinR, cosP, sinP, wingFlapAngle,
                lightX, lightY, lightZ, cameraDistance, outputBuffer, zBuffer);
    }

    private void renderSpheroid(double centerZ, double a, double b, double c,
            double cosR, double sinR, double cosP, double sinP, double wingFlapAngle,
            double lightX, double lightY, double lightZ, double cameraDistance,
            String[] outputBuffer, double[] zBuffer) {
        for (double theta = 0; theta < 2 * Math.PI; theta += 0.33) {
            for (double phi = -Math.PI / 2; phi <= Math.PI / 2; phi += 0.26) {
                double cosPhi = Math.cos(phi), sinPhi = Math.sin(phi);
                double cosTheta = Math.cos(theta), sinTheta = Math.sin(theta);

                double lx = a * cosPhi * cosTheta;
                double ly = b * cosPhi * sinTheta;
                double lz = centerZ + c * sinPhi;

                // Analytic ellipsoid surface normal: gradient of (x/a)^2+(y/b)^2+(z/c)^2
                double nx = cosPhi * cosTheta / a;
                double ny = cosPhi * sinTheta / b;
                double nz = sinPhi / c;
                double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);

                plotProjectedComponent(lx, ly, lz, nx / nLen, ny / nLen, nz / nLen, SURFACE_BODY, 0, 0.0,
                        cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance,
                        outputBuffer, zBuffer);
            }
        }
    }

    // -----------------------------------------------------------------
    // COMPONENT 2: ANTENNAE -- thin curved lines with a small club tip
    // -----------------------------------------------------------------

    private void renderAntennae(double cosR, double sinR, double cosP, double sinP, double wingFlapAngle,
            double lightX, double lightY, double lightZ, double cameraDistance,
            String[] outputBuffer, double[] zBuffer) {
        double baseZ = 0.66; // just above/ahead of the head sphere

        for (int side : SIDES) {
            for (double t = 0.0; t <= 1.0; t += 0.07) {
                // Sweeps outward and up, curling slightly forward at the tip like a real
                // antenna club
                double lx = side * (0.025 + 0.15 * t);
                double ly = -0.08 - 0.20 * t + 0.09 * t * t * t;
                double lz = baseZ + 0.05 * t;

                double nx = side * 0.6, ny = -0.7, nz = 0.15;
                double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);

                plotProjectedComponent(lx, ly, lz, nx / nLen, ny / nLen, nz / nLen, SURFACE_ANTENNA, side, t,
                        cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance,
                        outputBuffer, zBuffer);
            }
        }
    }

    // -----------------------------------------------------------------
    // COMPONENT 3: WINGS -- anatomically distinct fore/hindwing outlines
    // -----------------------------------------------------------------

    // Half-span (v extent) of a wing at a given distance u from the body. This is a
    // genuine ellipse arc, not a straight-line taper: it is 0 right at the body
    // (u=0), swells out to "span" at u="peak"*length (the widest point), then
    // narrows smoothly back to 0 at the tip (u=length). Because it's a true
    // sqrt(1 - t^2) curve on both sides of the peak, there's no straight edge and
    // no hard corner where the wing meets the body -- just a round taper in, and a
    // round taper back out to the tip. Shared between the render loop bounds and
    // the per-pixel margin/uNorm calculation so the "outer edge" always lines up
    // with the actual rendered silhouette.
    private double wingHalfSpanAt(int surfaceType, double u) {
        double length = (surfaceType == SURFACE_FOREWING) ? forewingLength : hindwingLength;
        double span = (surfaceType == SURFACE_FOREWING) ? forewingSpan : hindwingSpan;
        double peakFrac = (surfaceType == SURFACE_FOREWING) ? forewingPeak : hindwingPeak;

        double peakU = length * peakFrac;
        double sideRadius = (u <= peakU) ? peakU : (length - peakU);
        double t = (sideRadius > 0.0001) ? (u - peakU) / sideRadius : 0.0;

        double ellipseFactor = Math.sqrt(Math.max(0.0, 1.0 - t * t));
        return span * ellipseFactor;
    }

    private void renderWings(double cosR, double sinR, double cosP, double sinP, double wingFlapAngle,
            double lightX, double lightY, double lightZ, double cameraDistance,
            String[] outputBuffer, double[] zBuffer) {
        double maxLength = Math.max(forewingLength, hindwingLength);

        for (double u = 0.04; u <= maxLength; u += 0.022) {
            if (u <= forewingLength) {
                double vMax = wingHalfSpanAt(SURFACE_FOREWING, u);
                for (double v = 0.015; v <= vMax; v += 0.022) {
                    for (int side : SIDES) {
                        emitWingPoint(u, v, side, SURFACE_FOREWING, cosR, sinR, cosP, sinP, wingFlapAngle,
                                lightX, lightY, lightZ, cameraDistance, outputBuffer, zBuffer);
                    }
                }
            }
            if (u <= hindwingLength) {
                double vMax = wingHalfSpanAt(SURFACE_HINDWING, u);
                for (double v = 0.015; v <= vMax; v += 0.022) {
                    for (int side : SIDES) {
                        emitWingPoint(u, -v, side, SURFACE_HINDWING, cosR, sinR, cosP, sinP, wingFlapAngle,
                                lightX, lightY, lightZ, cameraDistance, outputBuffer, zBuffer);
                    }
                }
            }
        }
    }

    private void emitWingPoint(double u, double v, int side, int surfaceTag,
            double cosR, double sinR, double cosP, double sinP, double wingFlapAngle,
            double lightX, double lightY, double lightZ, double cameraDistance,
            String[] outputBuffer, double[] zBuffer) {
        double wx = u * side;
        double wy = 0.0;
        double wz = v;

        // Flat membrane, normal points straight up before hinge/world rotation
        plotProjectedComponent(wx, wy, wz, 0.0, -1.0, 0.0, surfaceTag, side, u,
                cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance,
                outputBuffer, zBuffer);
    }

    // Swallowtail-style tails: thin dark spikes trailing off the lower hindwing
    // edge
    private void renderTails(double cosR, double sinR, double cosP, double sinP, double wingFlapAngle,
            double lightX, double lightY, double lightZ, double cameraDistance,
            String[] outputBuffer, double[] zBuffer) {
        for (int side : SIDES) {
            double baseU = hindwingLength * 0.90;
            double baseV = -hindwingSpan * 0.15;
            for (double t = 0.0; t <= 1.0; t += 0.12) {
                double u = baseU + 0.24 * t;
                double v = baseV - 0.04 * t;
                double wx = u * side;
                double wy = 0.03 * t;
                double wz = v;

                plotProjectedComponent(wx, wy, wz, 0.0, -1.0, 0.0, SURFACE_TAIL, side, u,
                        cosR, sinR, cosP, sinP, wingFlapAngle, lightX, lightY, lightZ, cameraDistance,
                        outputBuffer, zBuffer);
            }
        }
    }

    // -----------------------------------------------------------------
    // PROJECTION + SHADING PIPELINE
    // -----------------------------------------------------------------

    private void plotProjectedComponent(double lx, double ly, double lz, double lnx, double lny, double lnz,
            int surfaceType, int side, double wingDist,
            double cosR, double sinR, double cosP, double sinP,
            double wingFlapAngle,
            double lightX, double lightY, double lightZ, double cameraDistance,
            String[] outputBuffer, double[] zBuffer) {

        double rx = lx, ry = ly, rz = lz;
        double nx = lnx, ny = lny, nz = lnz;

        boolean isWingSurface = surfaceType == SURFACE_FOREWING
                || surfaceType == SURFACE_HINDWING
                || surfaceType == SURFACE_TAIL;

        // --- STEP 1: HINGE FOLD FOR WINGS/TAILS ONLY ---
        if (isWingSurface) {
            double wingLength = (surfaceType == SURFACE_FOREWING) ? forewingLength : hindwingLength;

            // Real wings aren't perfectly rigid -- the tip swings a bit further than the
            // root
            double bendFactor = 0.6 + 0.4 * Math.min(1.0, wingDist / wingLength);
            double localFlap = wingFlapAngle * bendFactor;

            // Both wings must rotate the same visual direction (up/down) even though
            // their local x sits on opposite sides of the body hinge line, hence the
            // side-dependent sign flip.
            double effectiveAngle = -side * localFlap;
            double cosE = Math.cos(effectiveAngle);
            double sinE = Math.sin(effectiveAngle);

            rx = lx * cosE - ly * sinE;
            ry = lx * sinE + ly * cosE;

            nx = lnx * cosE - lny * sinE;
            ny = lnx * sinE + lny * cosE;
        }

        // --- STEP 2: DUAL-AXIS GLOBAL WORLD ROTATION PIPELINE ---
        double x1 = rx * cosR + rz * sinR;
        double y1 = ry;
        double z1 = -rx * sinR + rz * cosR;

        double nx1 = nx * cosR + nz * sinR;
        double ny1 = ny;
        double nz1 = -nx * sinR + nz * cosR;

        double worldX = x1;
        double worldY = y1 * cosP - z1 * sinP;
        double worldZ = y1 * sinP + z1 * cosP;

        double worldNx = nx1;
        double worldNy = ny1 * cosP - nz1 * sinP;
        double worldNz = ny1 * sinP + nz1 * cosP;

        // --- STEP 3: SCREEN FRUSTUM PROJECTOR MAPPING ---
        double ooz = 1.0 / (worldZ + cameraDistance);
        int xp = (int) (40 + 64 * ooz * worldX * 2.3);
        int yp = (int) (11 + 28 * ooz * worldY);

        if (xp < 0 || xp >= 80 || yp < 0 || yp >= 22) {
            return;
        }

        int bufferIndex = xp + 80 * yp;
        if (ooz <= zBuffer[bufferIndex] + 0.0001) {
            return;
        }
        zBuffer[bufferIndex] = ooz;

        // --- STEP 4: LAMBERTIAN SHADING + PROCEDURAL TEXTURING ---
        double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
        int shadeIndex = (int) ((luminance + 1.0) * 5.5);
        shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));

        String chosenColor;
        char asciiChar;

        switch (surfaceType) {
            case SURFACE_BODY:
                chosenColor = bodyColor;
                asciiChar = SHADE_RAMP[shadeIndex];
                break;

            case SURFACE_ANTENNA:
                chosenColor = antennaColor;
                asciiChar = (wingDist > 0.9) ? '\u25CF' : SHADE_RAMP[Math.min(shadeIndex, 4)];
                break;

            case SURFACE_TAIL:
                chosenColor = veinColor;
                asciiChar = '\u2588';
                break;

            default: {
                double wingLength = (surfaceType == SURFACE_HINDWING) ? hindwingLength : forewingLength;
                double uNorm = Math.min(1.0, wingDist / wingLength);
                double halfSpan = wingHalfSpanAt(surfaceType, wingDist);
                double vNorm = (halfSpan > 0.0001) ? Math.min(1.0, Math.abs(lz) / halfSpan) : 1.0;

                WingPixel wp = computeWingColor(surfaceType, uNorm, vNorm, side, shadeIndex);
                chosenColor = wp.color;
                asciiChar = wp.glyph;
            }
        }

        outputBuffer[bufferIndex] = chosenColor + asciiChar + RESET;
    }

    // -----------------------------------------------------------------
    // SPECIES-SPECIFIC WING PATTERNS
    // -----------------------------------------------------------------

    private static final class WingPixel {
        final String color;
        final char glyph;

        WingPixel(String color, char glyph) {
            this.color = color;
            this.glyph = glyph;
        }
    }

    private WingPixel computeWingColor(int surfaceType, double uNorm, double vNorm, int side, int shadeIndex) {
        boolean isForewing = (surfaceType == SURFACE_FOREWING);
        boolean isMargin = vNorm > 0.86 || uNorm > 0.93;

        switch (patternType) {
            case PATTERN_MONARCH:
                return monarchPixel(uNorm, vNorm, isMargin, shadeIndex);
            case PATTERN_MORPHO:
                return morphoPixel(uNorm, vNorm, isMargin, shadeIndex);
            case PATTERN_SWALLOWTAIL:
                return swallowtailPixel(isForewing, uNorm, vNorm, isMargin, shadeIndex);
            case PATTERN_PAINTED_LADY:
                return paintedLadyPixel(isForewing, uNorm, vNorm, isMargin, shadeIndex);
            case PATTERN_BUCKEYE:
                return buckeyePixel(isForewing, uNorm, vNorm, isMargin, shadeIndex);
            default:
                return new WingPixel(primaryColor, SHADE_RAMP[shadeIndex]);
        }
    }

    private WingPixel monarchPixel(double u, double v, boolean isMargin, int shadeIndex) {
        if (isMargin) {
            if (Math.sin(v * 46.0 + u * 10.0) > 0.35) {
                return new WingPixel(spotColor, '\u2591');
            }
            return new WingPixel(veinColor, '\u2588');
        }
        double vein = Math.sin(u * 17.0 + v * 6.0) * Math.cos(v * 20.0 - u * 3.0);
        if (Math.abs(vein) > 0.63 || u < 0.05) {
            return new WingPixel(veinColor, '\u2593');
        }
        return new WingPixel(primaryColor, SHADE_RAMP[shadeIndex]);
    }

    private WingPixel morphoPixel(double u, double v, boolean isMargin, int shadeIndex) {
        if (isMargin) {
            return new WingPixel(veinColor, '\u2588');
        }
        // Iridescent shimmer: alternates between two blue shades depending on
        // viewing-angle-ish surface shading, mimicking a light-catching sheen.
        double shimmer = Math.sin(u * 9.0 + v * 14.0) + 0.4 * Math.sin(shadeIndex * 0.6);
        String c = (shimmer > 0.15) ? primaryColor : secondaryColor;
        return new WingPixel(c, SHADE_RAMP[shadeIndex]);
    }

    private WingPixel swallowtailPixel(boolean isForewing, double u, double v, boolean isMargin, int shadeIndex) {
        if (isMargin) {
            return new WingPixel(veinColor, '\u2588');
        }
        if (!isForewing) {
            WingPixel spot = eyespot(u, v, 0.80, 0.55, 0.16, veinColor, accentColor, eyespotInnerColor);
            if (spot != null) {
                return spot;
            }
        }
        if (Math.sin(u * 18.0) > 0.25) {
            return new WingPixel(veinColor, '\u2593');
        }
        return new WingPixel(primaryColor, SHADE_RAMP[shadeIndex]);
    }

    private WingPixel paintedLadyPixel(boolean isForewing, double u, double v, boolean isMargin, int shadeIndex) {
        if (isMargin) {
            return new WingPixel(veinColor, '\u2588');
        }
        if (isForewing && u > 0.72 && v > 0.45) {
            if (Math.sin(u * 50.0) * Math.cos(v * 45.0) > 0.55) {
                return new WingPixel(spotColor, '\u2591');
            }
        }
        if (!isForewing) {
            for (int i = 0; i < 3; i++) {
                double cu = 0.35 + i * 0.22;
                WingPixel spot = eyespot(u, v, cu, 0.78, 0.075, veinColor, accentColor, spotColor);
                if (spot != null) {
                    return spot;
                }
            }
        }
        double blotch = Math.sin(u * 9.0 + v * 13.0) * Math.cos(v * 7.0 - u * 4.0);
        if (blotch > 0.5) {
            return new WingPixel(veinColor, '\u2593');
        }
        return new WingPixel(primaryColor, SHADE_RAMP[shadeIndex]);
    }

    private WingPixel buckeyePixel(boolean isForewing, double u, double v, boolean isMargin, int shadeIndex) {
        if (isForewing) {
            WingPixel spot = eyespot(u, v, 0.78, 0.5, 0.22, veinColor, accentColor, eyespotInnerColor);
            if (spot != null) {
                return spot;
            }
        } else {
            WingPixel spot1 = eyespot(u, v, 0.55, 0.55, 0.20, veinColor, accentColor, eyespotInnerColor);
            if (spot1 != null) {
                return spot1;
            }
            WingPixel spot2 = eyespot(u, v, 0.28, 0.3, 0.12, veinColor, accentColor, eyespotInnerColor);
            if (spot2 != null) {
                return spot2;
            }
        }
        if (isMargin) {
            return new WingPixel(spotColor, '\u2591');
        }
        if (Math.abs(Math.sin(u * 6.0)) > 0.9) {
            return new WingPixel(veinColor, '\u2593');
        }
        return new WingPixel(primaryColor, SHADE_RAMP[shadeIndex]);
    }

    // Concentric-ring eyespot: outer dark ring -> mid accent ring -> inner pupil.
    // Returns null when the point falls outside the spot entirely.
    private WingPixel eyespot(double u, double v, double centerU, double centerV, double radius,
            String outerColor, String midColor, String innerColor) {
        double du = u - centerU;
        double dv = v - centerV;
        double dist = Math.sqrt(du * du + dv * dv);
        if (dist > radius) {
            return null;
        }
        double frac = dist / radius;
        if (frac < 0.3) {
            return new WingPixel(innerColor, '\u25CF');
        }
        if (frac < 0.65) {
            return new WingPixel(midColor, '\u25C9');
        }
        return new WingPixel(outerColor, '\u25CB');
    }
}