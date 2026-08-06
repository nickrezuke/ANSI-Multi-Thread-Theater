import java.time.LocalTime;

public class BigBenLoader extends Loader {
    private static final StatusStage[] BIG_BEN_STAGES = {
            new StatusStage(20, "Forging stepped buttress turrets:"),
            new StatusStage(45, "Carving gothic lancet window tracery:"),
            new StatusStage(70, "Gilding dial rims & setting the hands:"),
            new StatusStage(95, "Raising the corner pinnacles & spire:"),
            new StatusStage(100, "Elizabeth Tower Ground POV Matrix Active!")
    };

    // --- Glyphs (each ONE unique codepoint -> one unique color branch) ---
    private static final char CH_STONE = '\u2588';   // █ Anston limestone masonry (default, diffuse-shaded)
    private static final char CH_GOLD = '\u2589';    // ▉ Gilded trim: cornices, dial ring, finials
    private static final char CH_TRACERY = '\u2593'; // ▓ Dark ironwork window tracery / mullions
    private static final char CH_GLASS = '\u2592';   // ▒ Recessed window / louvre glass (dark navy)
    private static final char CH_DIAL = '\u2591';    // ░ Backlit opal glass dial face
    private static final char CH_HAND = '\u258C';    // ▌ Black clock hands, ticks, and hub
    private static final char CH_ROOF = '\u2590';    // ▐ Prussian-blue cast-iron spire panels
    private static final char CH_SPIRE = '\u00B7';   // · Finial rod tip

    // --- Palette: Elizabeth Tower, post-2022 restoration colors ---
    private static final int[] MASONRY_BASE = { 214, 198, 166 };
    private static final int[] MASONRY_SHD = { 92, 84, 72 };
    private static final int[] GOLD_BASE = { 214, 172, 62 };
    private static final int[] GOLD_SHD = { 132, 100, 34 };
    private static final int[] DIAL_OPAL = { 247, 243, 232 };
    private static final int[] HAND_COLOR = { 22, 21, 24 };
    private static final int[] GLASS_COLOR = { 16, 21, 34 };
    private static final int[] TRACERY_COLOR = { 33, 31, 30 };
    private static final int[] SPIRE_BASE = { 52, 82, 122 };
    private static final int[] SPIRE_SHD = { 20, 34, 54 };

    private static final int[] SKY_TOP = { 128, 163, 201 };
    private static final int[] SKY_BOTTOM = { 213, 227, 236 };

    private double rotationY = 0.0;
    private static final double CAMERA_DISTANCE_FAR = 6.6; // default low-and-far ground POV
    private static final double TILT_FAR = -0.65; // steep upward look from far below
    private static final double Y_OFFSET_FAR = 0.42; // frames the whole tower

    // First dwell only: the low, near-ground look the rotation starts on.
    private static final double CAMERA_DISTANCE_NEAR_BASE = 3.1;
    private static final double TILT_NEAR_BASE = -0.16;
    private static final double Y_OFFSET_NEAR_BASE = 0.66;

    // Every dwell after the first full revolution: punch in on the clock face.
    private static final double CAMERA_DISTANCE_NEAR_CLOCK = 1.5;
    private static final double TILT_NEAR_CLOCK = -0.55;
    private static final double Y_OFFSET_NEAR_CLOCK = -0.53; // clock-stage height, not the base

    // Set once per frame in renderGeometry, read by renderProjectedVertex.
    private double cameraDistance = CAMERA_DISTANCE_FAR;
    private double cameraYOffset = Y_OFFSET_FAR;

    // --- Radii for each tier: the SHAPE of these steps is what makes the
    // silhouette read as "Big Ben" instead of "pyramid on a box." ---
    private static final double R_BASE = 0.46;
    private static final double R_SHAFT_A = 0.40; // lower shaft (widest)
    private static final double R_SHAFT_B = 0.35; // mid shaft (stepped in)
    private static final double R_SHAFT_C = 0.315; // clock plinth (stepped in again)
    private static final double R_BALCONY = 0.45; // projecting cornice below the dials
    private static final double R_CLOCK = 0.43; // clock stage (wide, dials set in)
    private static final double R_BELFRY = 0.345; // steps back in above the clock cornice
    private static final double R_SPIRE_BASE = 0.30;

    public BigBenLoader() {
        super(BIG_BEN_STAGES);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Calculate the raw rotation location normalized within a true [0, 2PI] window
        double currentAngleRad = rotationY % (2.0 * Math.PI);
        if (currentAngleRad < 0)
            currentAngleRad += (2.0 * Math.PI);

        // SINGLE-DIP SYMMETRY EQUATION: Evaluates distance to 0 (and 2PI) specifically,
        // i.e. how close we are to the clock face swinging around to front-and-center.
        double distToFront = Math.min(currentAngleRad, Math.abs(2.0 * Math.PI - currentAngleRad));

        // 1. GAUSSIAN ROTATION SLOWDOWN ENGINE (Tracks 2PI full loop proximity)
        double rotationSlowWindow = Math.exp(-Math.pow(distToFront, 2.0) / (1.5 * Math.pow(0.42, 2.0)));
        double dynamicStepSpeed = 0.007 * (1.0 - rotationSlowWindow * 0.85) + 0.0012;

        rotationY += dynamicStepSpeed;

        // 2. GAUSSIAN CLOCK-DWELL WINDOW (narrower than the rotation slowdown, so the
        // zoom itself feels like a deliberate punch-in rather than a gradual drift)
        double dwellWindow = Math.exp(-Math.pow(distToFront, 2.0) / (1.7 * Math.pow(0.30, 2.0)));

        // Which revolution is this dwell on? The very first dwell (before we've
        // completed a full lap) keeps the low, near-ground starting look; every
        // dwell after that punches in on the clock face instead.
        long lapIndex = Math.round(rotationY / (2.0 * Math.PI));
        boolean isClockDwell = lapIndex >= 1;

        double tiltNearTarget = isClockDwell ? TILT_NEAR_CLOCK : TILT_NEAR_BASE;
        double distNearTarget = isClockDwell ? CAMERA_DISTANCE_NEAR_CLOCK : CAMERA_DISTANCE_NEAR_BASE;
        double yOffsetNearTarget = isClockDwell ? Y_OFFSET_NEAR_CLOCK : Y_OFFSET_NEAR_BASE;

        // 3. Blend camera tilt, distance, and vertical framing from the default
        // "ground POV, far away, looking up at the whole tower" pose toward
        // whichever near-state applies to this dwell.
        double tiltX = TILT_FAR * (1.0 - dwellWindow) + tiltNearTarget * dwellWindow;
        cameraDistance = CAMERA_DISTANCE_FAR - (dwellWindow * (CAMERA_DISTANCE_FAR - distNearTarget));
        cameraYOffset = Y_OFFSET_FAR * (1.0 - dwellWindow) + yOffsetNearTarget * dwellWindow;

        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);

        LocalTime now = LocalTime.now();
        double hourFrac = (now.getHour() % 12) + now.getMinute() / 60.0;
        double hourAngle = (hourFrac / 12.0) * 2.0 * Math.PI;
        double minuteFrac = now.getMinute() + now.getSecond() / 60.0;
        double minuteAngle = (minuteFrac / 60.0) * 2.0 * Math.PI;

        // STEP 1: SKY BACKDROP
        for (int yp = 0; yp < 22; yp++) {
            double skyGrad = (double) yp / 21.0;
            int sr = (int) (SKY_TOP[0] * (1.0 - skyGrad) + SKY_BOTTOM[0] * skyGrad);
            int sg = (int) (SKY_TOP[1] * (1.0 - skyGrad) + SKY_BOTTOM[1] * skyGrad);
            int sb = (int) (SKY_TOP[2] * (1.0 - skyGrad) + SKY_BOTTOM[2] * skyGrad);
            String skyColor = String.format("\u001B[38;2;%d;%d;%dm", sr, sg, sb);
            for (int xp = 0; xp < 80; xp++) {
                outputBuffer[xp + 80 * yp] = skyColor + " " + RESET;
            }
        }

        // STEP 2: STRUCTURE, GROUND UP. Tier boundaries below are chosen so the
        // profile actually steps in/out several times instead of running straight
        // up into one triangular cap.
        final double BASE_TOP = 1.5;
        final double SHAFT_A_TOP = BASE_TOP, SHAFT_A_BOT = 0.74;
        final double LEDGE_A_BOT = 0.70;
        final double SHAFT_B_TOP = LEDGE_A_BOT, SHAFT_B_BOT = 0.06;
        final double LEDGE_B_BOT = 0.02;
        final double SHAFT_C_TOP = LEDGE_B_BOT, SHAFT_C_BOT = -0.18;
        final double BALCONY_BOT = -0.34;
        final double CLOCK_BOT = -0.98;
        final double BELFRY_LEDGE_BOT = -1.03;
        final double BELFRY_BOT = -1.48;
        final double GABLET_BOT = -1.66;
        final double SPIRE_TIP = -2.55;

        for (double y = 1.9; y >= -2.8; y -= 0.012) {

            boolean isBasePlinth = y <= 1.9 && y > BASE_TOP;
            boolean isShaftA = y <= SHAFT_A_TOP && y > SHAFT_A_BOT;
            boolean isLedgeA = y <= SHAFT_A_BOT && y > LEDGE_A_BOT;
            boolean isShaftB = y <= SHAFT_B_TOP && y > SHAFT_B_BOT;
            boolean isLedgeB = y <= SHAFT_B_BOT && y > LEDGE_B_BOT;
            boolean isShaftC = y <= SHAFT_C_TOP && y > SHAFT_C_BOT;
            boolean isBalcony = y <= SHAFT_C_BOT && y > BALCONY_BOT;
            boolean isClockHousing = y <= BALCONY_BOT && y > CLOCK_BOT;
            boolean isBelfryLedge = y <= CLOCK_BOT && y > BELFRY_LEDGE_BOT;
            boolean isBelfryLouvers = y <= BELFRY_LEDGE_BOT && y > BELFRY_BOT;
            boolean isGablets = y <= BELFRY_BOT && y > GABLET_BOT;
            boolean isSpire = y <= GABLET_BOT && y > SPIRE_TIP;
            boolean isFinial = y <= SPIRE_TIP;

            if (isBasePlinth) {
                double scale = R_BASE * (1.0 + (y - BASE_TOP) * 0.4);
                renderPlainBox(y, scale, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isShaftA) {
                renderGothicWindowRow(y, R_SHAFT_A, SHAFT_A_TOP, 0.38, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isLedgeA) {
                renderCornice(y, R_SHAFT_A * 1.1, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isShaftB) {
                renderGothicWindowRow(y, R_SHAFT_B, SHAFT_B_TOP, 0.32, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isLedgeB) {
                renderCornice(y, R_SHAFT_B * 1.1, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isShaftC) {
                renderPlainBox(y, R_SHAFT_C, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isBalcony) {
                // Wide projecting cornice: the tower's silhouette steps OUT here before
                // the clock stage, which is the single biggest cue that reads as "Big Ben."
                boolean edgeRow = y > BALCONY_BOT + 0.02 && y < SHAFT_C_BOT - 0.02;
                renderPlainBox(y, R_BALCONY, edgeRow ? CH_STONE : CH_GOLD, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
            } else if (isClockHousing) {
                renderClockDials(y, R_CLOCK, BALCONY_BOT, hourAngle, minuteAngle, cosX, sinX, cosY, sinY,
                        outputBuffer, zBuffer);
            } else if (isBelfryLedge) {
                renderCornice(y, R_BELFRY * 1.12, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                if (y <= CLOCK_BOT && y > CLOCK_BOT - 0.02) {
                    renderCornerPinnacles(R_BELFRY, CLOCK_BOT, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                }
            } else if (isBelfryLouvers) {
                renderBelfryRow(y, R_BELFRY, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isGablets) {
                double progress = (y - BELFRY_BOT) / (GABLET_BOT - BELFRY_BOT);
                double radius = R_BELFRY * (1.0 - progress) + R_SPIRE_BASE * progress;
                renderPlainBox(y, radius, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                renderGablets(y, radius, progress, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isSpire) {
                renderSpireRow(y, GABLET_BOT, SPIRE_TIP, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (isFinial) {
                renderFinialRow(y, SPIRE_TIP, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            }
        }
    }

    // ---------- Tier helpers ----------

    /** Solid four-wall box outline with clasping corner-turret bars. */
    private void renderPlainBox(double y, double radius, char ch, double cosX, double sinX, double cosY,
            double sinY, String[] outputBuffer, double[] zBuffer) {
        for (double t = -radius; t <= radius; t += 0.008) {
            renderProjectedVertex(-radius, y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(radius, y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(t, y, -radius, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(t, y, radius, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    /** A thin gilded cornice band, solid across its whole (small) height range. */
    private void renderCornice(double y, double radius, double cosX, double sinX, double cosY, double sinY,
            String[] outputBuffer, double[] zBuffer) {
        renderPlainBox(y, radius, CH_GOLD, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
    }

    /** A shaft tier with tiered lancet windows that taper to a gothic point. */
    private void renderGothicWindowRow(double y, double radius, double tierTop, double bandHeight, double cosX,
            double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double posInTower = tierTop - y;
        double bandFrac = (posInTower % bandHeight) / bandHeight;
        double windowStart = 0.22, windowEnd = 0.90;
        boolean inWindowBand = bandFrac >= windowStart && bandFrac <= windowEnd;
        double windowFrac = inWindowBand ? (bandFrac - windowStart) / (windowEnd - windowStart) : -1.0;
        double archScale = 1.0;
        if (inWindowBand && windowFrac < 0.22) {
            archScale = Math.max(0.08, windowFrac / 0.22);
        }
        double slotOffset = radius * 0.34;

        for (double t = -radius; t <= radius; t += 0.004) {
            boolean isCornerTurret = Math.abs(t) > radius - 0.045;
            boolean isWindowSlot = inWindowBand && !isCornerTurret
                    && (Math.abs(t) < 0.02 * archScale
                            || Math.abs(t - slotOffset) < 0.016 * archScale
                            || Math.abs(t + slotOffset) < 0.016 * archScale);

            char wallChar = isWindowSlot ? CH_GLASS : CH_STONE;
            renderProjectedVertex(t, y, -radius, wallChar, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isWindowSlot)
                renderProjectedVertex(t, y, -(radius - 0.015), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
            renderProjectedVertex(t, y, radius, wallChar, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isWindowSlot)
                renderProjectedVertex(t, y, (radius - 0.015), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
            renderProjectedVertex(-radius, y, t, wallChar, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isWindowSlot)
                renderProjectedVertex(-(radius - 0.015), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
            renderProjectedVertex(radius, y, t, wallChar, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isWindowSlot)
                renderProjectedVertex((radius - 0.015), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
        }
    }

    /** Gilded-ring, opal-faced dials with real hands driven by system time. */
    private void renderClockDials(double y, double hR, double stageTop, double hourAngle, double minuteAngle,
            double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double cY = stageTop - 0.32;
        double outerR2 = 0.075;
        double innerR2 = 0.06;
        double innerRadius = Math.sqrt(innerR2);

        for (double t = -hR; t <= hR; t += 0.004) {
            boolean isCornerTurret = Math.abs(t) > hR - 0.05;
            double yRel = y - cY;
            double dist = t * t + yRel * yRel;
            boolean insideOuter = dist < outerR2 && !isCornerTurret;
            boolean insideFace = dist < innerR2 && !isCornerTurret;

            char dialText = CH_STONE;
            if (insideOuter && !insideFace) {
                dialText = CH_GOLD;
            } else if (insideFace) {
                dialText = CH_DIAL;
                double r = Math.sqrt(dist);
                double angle = Math.atan2(t, -yRel);
                if (angle < 0)
                    angle += 2.0 * Math.PI;
                boolean isTick = false;
                for (int k = 0; k < 12 && !isTick; k++) {
                    double markAngle = k * (Math.PI / 6.0);
                    double diff = Math.abs(angle - markAngle);
                    if (diff > Math.PI)
                        diff = 2.0 * Math.PI - diff;
                    if (diff < 0.045 && r > innerRadius * 0.72)
                        isTick = true;
                }
                if (isTick)
                    dialText = CH_HAND;
                if (isOnHand(t, yRel, hourAngle, innerRadius * 0.55, 0.006))
                    dialText = CH_HAND;
                if (isOnHand(t, yRel, minuteAngle, innerRadius * 0.85, 0.005))
                    dialText = CH_HAND;
                if (r < 0.006)
                    dialText = CH_HAND;
            }

            double zOffset = insideFace ? -(hR - 0.025) : -hR;
            renderProjectedVertex(t, y, zOffset, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            zOffset = insideFace ? (hR - 0.025) : hR;
            renderProjectedVertex(t, y, zOffset, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            double xOffset = insideFace ? -(hR - 0.025) : -hR;
            renderProjectedVertex(xOffset, y, t, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            xOffset = insideFace ? (hR - 0.025) : hR;
            renderProjectedVertex(xOffset, y, t, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    /** Open belfry stage with louvre vents. */
    private void renderBelfryRow(double y, double radius, double cosX, double sinX, double cosY, double sinY,
            String[] outputBuffer, double[] zBuffer) {
        for (double t = -radius; t <= radius; t += 0.004) {
            boolean isCorner = Math.abs(t) > radius - 0.045;
            boolean isVentOpen = !isCorner && (Math.abs(t) > radius * 0.12 && Math.abs(t) < radius * 0.65);
            char text = isVentOpen ? CH_GLASS : CH_STONE;
            renderProjectedVertex(t, y, -radius, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                renderProjectedVertex(t, y, -(radius - 0.01), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
            renderProjectedVertex(t, y, radius, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                renderProjectedVertex(t, y, (radius - 0.01), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
            renderProjectedVertex(-radius, y, t, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                renderProjectedVertex(-(radius - 0.01), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
            renderProjectedVertex(radius, y, t, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                renderProjectedVertex((radius - 0.01), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                        zBuffer);
        }
    }

    /** Small triangular gablets on each face at the base of the spire. */
    private void renderGablets(double y, double radius, double progress, double cosX, double sinX, double cosY,
            double sinY, String[] outputBuffer, double[] zBuffer) {
        double halfWidth = 0.09 * (1.0 - progress);
        double bump = 0.05 * (1.0 - progress);
        if (halfWidth < 0.004)
            return;
        for (double t = -halfWidth; t <= halfWidth; t += 0.006) {
            char ch = Math.abs(Math.abs(t) - halfWidth) < 0.01 ? CH_GOLD : CH_STONE;
            renderProjectedVertex(t, y, -(radius + bump), ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(t, y, (radius + bump), ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-(radius + bump), y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex((radius + bump), y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    /** Slender two-stage taper: steep lower spire, needle-like upper spire. */
    private void renderSpireRow(double y, double top, double tip, double cosX, double sinX, double cosY,
            double sinY, String[] outputBuffer, double[] zBuffer) {
        double progress = (y - top) / (tip - top);
        double radius;
        if (progress < 0.55) {
            double p = progress / 0.55;
            radius = R_SPIRE_BASE * (1.0 - p * 0.6);
        } else {
            double p = (progress - 0.55) / 0.45;
            radius = R_SPIRE_BASE * 0.4 * (1.0 - p * 0.96);
        }
        for (double t = -radius; t <= radius; t += 0.005) {
            char faceText = CH_ROOF;
            if (Math.abs(Math.abs(t) - radius) < 0.016 || Math.abs(t) < 0.008) {
                faceText = CH_GOLD;
            }
            renderProjectedVertex(t, y, -radius, faceText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(t, y, radius, faceText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-radius, y, t, faceText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(radius, y, t, faceText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    /** Ball-and-cross finial capping the spire. */
    private void renderFinialRow(double y, double tip, double cosX, double sinX, double cosY, double sinY,
            String[] outputBuffer, double[] zBuffer) {
        boolean isBallZone = y <= tip - 0.02 && y > tip - 0.12;
        boolean isCrossZone = y <= tip + 0.10 && y > tip + 0.03;
        if (isBallZone) {
            double ballR = 0.035;
            for (double a = 0.0; a < Math.PI * 2.0; a += 0.6) {
                renderProjectedVertex(ballR * Math.cos(a), y, ballR * Math.sin(a), CH_GOLD, cosX, sinX, cosY, sinY,
                        outputBuffer, zBuffer);
            }
        } else if (isCrossZone) {
            renderProjectedVertex(0.05, y, 0.0, CH_GOLD, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-0.05, y, 0.0, CH_GOLD, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(0.0, y, 0.05, CH_GOLD, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(0.0, y, -0.05, CH_GOLD, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(0.0, y, 0.0, CH_SPIRE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        } else {
            renderProjectedVertex(0.0, y, 0.0, CH_SPIRE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    /** True if the 2D point (t, yRel) lies along a clock hand from the center. */
    private boolean isOnHand(double t, double yRel, double angle, double length, double thickness) {
        double ux = Math.sin(angle);
        double uy = -Math.cos(angle);
        double proj = t * ux + yRel * uy;
        if (proj < 0.0 || proj > length)
            return false;
        double perp = Math.abs(t * uy - yRel * ux);
        return perp < thickness;
    }

    /**
     * Four slender pinnacles flanking the spire, rising from the top of the
     * clock stage up alongside the belfry and partway up the spire. This is the
     * single biggest thing that breaks a plain "pyramid on a box" outline into
     * the recognizable multi-spike Big Ben crown.
     */
    private void renderCornerPinnacles(double belfryRadius, double baseY, double cosX, double sinX, double cosY,
            double sinY, String[] outputBuffer, double[] zBuffer) {
        double topY = -2.15;
        double cornerOffset = belfryRadius * 0.95;
        int[] signs = { -1, 1 };
        for (int sx : signs) {
            for (int sz : signs) {
                double cx = sx * cornerOffset;
                double cz = sz * cornerOffset;
                for (double y = baseY; y >= topY; y -= 0.018) {
                    double progress = (baseY - y) / (baseY - topY);
                    double r = 0.05 * (1.0 - progress * 0.9);
                    char ch = progress > 0.92 ? CH_GOLD : CH_STONE;
                    for (double a = 0.0; a < Math.PI * 2.0; a += 0.7) {
                        renderProjectedVertex(cx + r * Math.cos(a), y, cz + r * Math.sin(a), ch, cosX, sinX, cosY,
                                sinY, outputBuffer, zBuffer);
                    }
                }
                renderProjectedVertex(cx, topY - 0.02, cz, CH_GOLD, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            }
        }
    }

    private void renderProjectedVertex(double x, double y, double z, char renderChar, double cosX, double sinX,
            double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double xSpun = x * cosY + z * sinY;
        double ySpun = y;
        double zSpun = -x * sinY + z * cosY;

        double worldX = xSpun;
        double worldY = ySpun - cameraYOffset;
        double worldZ = zSpun + cameraDistance;

        double rx = worldX;
        double ry = worldY * cosX - worldZ * sinX;
        double rz = worldY * sinX + worldZ * cosX;

        double ooz = 1.0 / rz;
        int xp = (int) (40 + 58 * ooz * rx * 2.35);
        int yp = (int) (-12 + 48 * ooz * ry);

        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.00003) {
                zBuffer[index] = ooz;

                double shadowCompass = Math.cos(xSpun + 0.45) * Math.cos(ySpun - 0.25);
                double diffuseWeight = 0.45 + 0.55 * Math.max(0.0, shadowCompass);

                int r, g, b;
                if (renderChar == CH_GOLD) {
                    r = (int) (GOLD_SHD[0] * (1.0 - diffuseWeight) + GOLD_BASE[0] * diffuseWeight);
                    g = (int) (GOLD_SHD[1] * (1.0 - diffuseWeight) + GOLD_BASE[1] * diffuseWeight);
                    b = (int) (GOLD_SHD[2] * (1.0 - diffuseWeight) + GOLD_BASE[2] * diffuseWeight);
                } else if (renderChar == CH_TRACERY) {
                    r = (int) (TRACERY_COLOR[0] * (0.6 + 0.4 * diffuseWeight));
                    g = (int) (TRACERY_COLOR[1] * (0.6 + 0.4 * diffuseWeight));
                    b = (int) (TRACERY_COLOR[2] * (0.6 + 0.4 * diffuseWeight));
                } else if (renderChar == CH_GLASS) {
                    r = GLASS_COLOR[0];
                    g = GLASS_COLOR[1];
                    b = GLASS_COLOR[2];
                } else if (renderChar == CH_DIAL) {
                    r = DIAL_OPAL[0];
                    g = DIAL_OPAL[1];
                    b = DIAL_OPAL[2];
                } else if (renderChar == CH_HAND) {
                    r = HAND_COLOR[0];
                    g = HAND_COLOR[1];
                    b = HAND_COLOR[2];
                } else if (renderChar == CH_ROOF) {
                    r = (int) (SPIRE_SHD[0] * (1.0 - diffuseWeight) + SPIRE_BASE[0] * diffuseWeight);
                    g = (int) (SPIRE_SHD[1] * (1.0 - diffuseWeight) + SPIRE_BASE[1] * diffuseWeight);
                    b = (int) (SPIRE_SHD[2] * (1.0 - diffuseWeight) + SPIRE_BASE[2] * diffuseWeight);
                } else if (renderChar == CH_SPIRE) {
                    r = 250;
                    g = 245;
                    b = 225;
                } else {
                    r = (int) (MASONRY_SHD[0] * (1.0 - diffuseWeight) + MASONRY_BASE[0] * diffuseWeight);
                    g = (int) (MASONRY_SHD[1] * (1.0 - diffuseWeight) + MASONRY_BASE[1] * diffuseWeight);
                    b = (int) (MASONRY_SHD[2] * (1.0 - diffuseWeight) + MASONRY_BASE[2] * diffuseWeight);
                }

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}