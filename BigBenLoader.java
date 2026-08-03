// TODO: Finish this implementation of Big Ben

public class BigBenLoader extends Loader {
    private static final StatusStage[] BIG_BEN_STAGES = {
            new StatusStage(20, "Forging 4-sided stone buttress turrets:"),
            new StatusStage(45, "Carving triple-layered gothic window bays:"),
            new StatusStage(70, "Indenting 3D clock-housing window cavities:"),
            new StatusStage(95, "Slicing tapered copper belfry roof pyramid:"),
            new StatusStage(100, "Elizabeth Tower Ground POV Matrix Active!")
    };

    // Fine-grained structural typography symbols for sharp stone texturing
    private static final char CH_STONE = '\u2588'; // █ Solid Anston magnesian limestone piers
    private static final char CH_TRACERY = '\u2593'; // ▓ Gothic openwork window framing grilles
    private static final char CH_GLASS = '\u2592'; // ▒ Recessed window glass/empty belfry cavity
    private static final char CH_DIAL = '\u2591'; // ░ Translucent backlit opal glass dial track
    private static final char CH_IRONWORK = '\u2588'; // █ Dark cast-iron decorative clock frame / hands
    private static final char CH_COPPER = '\u2593'; // ▓ Aged oxidized copper roofing patina
    private static final char CH_SPIRE = '\u00B7'; // · Fine lightning pinnacle finial rods

    // Fixed Overcast Daytime Palette (No Day/Night timeline changes)
    private static final int[] MASONRY_BASE = { 235, 222, 198 }; // Warm Anston sandstone
    private static final int[] MASONRY_SHD = { 135, 125, 110 }; // Sooty Westminster atmospheric weathering
    private static final int[] CLOCK_OPAL = { 255, 242, 210 }; // Backlit opal glass dial face
    private static final int[] COPPER_ROOF = { 68, 122, 112 }; // Oxidized green patina plates
    private static final int[] DARK_IRON = { 42, 44, 50 }; // Charcoal iron frameworks

    // Overcast London Sky Background Gradient
    private static final int[] SKY_TOP = { 170, 182, 195 };
    private static final int[] SKY_BOTTOM = { 195, 205, 215 };

    private double rotationY = 0.0;
    private static final double CAMERA_DISTANCE = 6.3; // Zoomed out cleanly to frame the whole spire height

    public BigBenLoader() {
        super(BIG_BEN_STAGES);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Continuous, smooth horizontal spin around the vertical axis
        rotationY += 0.012;

        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // TRUE LOOKING-UP CAMERA MATRIX: Negative pitch angle tilts the lens backward
        // and points it skyward
        double groundPOVUpwardTiltX = -0.85;
        double cosX = Math.cos(groundPOVUpwardTiltX);
        double sinX = Math.sin(groundPOVUpwardTiltX);

        // STEP 1: RENDER FULL CANVAS SKY BACKDROP
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

        // STEP 2: HIGH-RESOLUTION STRUCTURAL VECTOR TRACER
        // Vertical coordinates map building elevations directly from ground base up to
        // belfry pinnacle
        for (double y = 1.8; y >= -2.2; y -= 0.012) {

            boolean isBasePlinth = y <= 1.8 && y > 1.4;
            boolean isMainShaft = y <= 1.4 && y > -0.1;
            boolean isBalconyDeck = y <= -0.1 && y > -0.25;
            boolean isClockHousing = y <= -0.25 && y > -0.95;
            boolean isBelfryLouvers = y <= -0.95 && y > -1.35;
            boolean isSpireRoof = y <= -1.35 && y > -2.1;
            boolean isFinialSpoke = y <= -2.1;

            double shaftR = 0.44;

            // --- A. STEPS BASE PLINTH FOUNDATION ---
            if (isBasePlinth) {
                double scale = shaftR * (1.0 + (y - 1.4) * 0.45);
                for (double t = -scale; t <= scale; t += 0.01) {
                    renderProjectedVertex(-scale, y, t, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    renderProjectedVertex(scale, y, t, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    renderProjectedVertex(t, y, -scale, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    renderProjectedVertex(t, y, scale, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                }
            }
            // --- B. DETAILED RECESSED WINDOW GOTHIC SHAFT ---
            else if (isMainShaft) {
                for (double t = -shaftR; t <= shaftR; t += 0.004) {
                    // Extrude four distinct structural corner turret column bars past the flat
                    // walls
                    boolean isCornerTurret = Math.abs(t) > shaftR - 0.045;
                    char wallTexture = CH_STONE;

                    // Compute position for triple vertical window slots cut into each face
                    boolean isWindowSlot = !isCornerTurret
                            && (Math.abs(t) < 0.02 || Math.abs(t - 0.14) < 0.018 || Math.abs(t + 0.14) < 0.018);

                    // Trace Front Wall Face
                    char frontText = isWindowSlot ? CH_GLASS : wallTexture;
                    renderProjectedVertex(t, y, -shaftR, frontText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isWindowSlot)
                        renderProjectedVertex(t, y, -(shaftR - 0.015), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);

                    // Trace Back Wall Face
                    char backText = isWindowSlot ? CH_GLASS : wallTexture;
                    renderProjectedVertex(t, y, shaftR, backText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isWindowSlot)
                        renderProjectedVertex(t, y, (shaftR - 0.015), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);

                    // Trace Left Wall Face
                    char leftText = isWindowSlot ? CH_GLASS : wallTexture;
                    renderProjectedVertex(-shaftR, y, t, leftText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isWindowSlot)
                        renderProjectedVertex(-(shaftR - 0.015), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);

                    // Trace Right Wall Face
                    char rightText = isWindowSlot ? CH_GLASS : wallTexture;
                    renderProjectedVertex(shaftR, y, t, rightText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isWindowSlot)
                        renderProjectedVertex((shaftR - 0.015), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);
                }
            }
            // --- C. PROMINENT OUTJUTTING BALCONY ---
            else if (isBalconyDeck) {
                double rBalc = shaftR * 1.09;
                for (double t = -rBalc; t <= rBalc; t += 0.008) {
                    renderProjectedVertex(-rBalc, y, t, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    renderProjectedVertex(rBalc, y, t, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    renderProjectedVertex(t, y, -rBalc, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    renderProjectedVertex(t, y, rBalc, CH_STONE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                }
            }
            // --- D. TRUE RECESSED 3D CLOCK HOUSINGS (IWANS) ---
            else if (isClockHousing) {
                double hR = shaftR * 1.04;
                double cY = -0.60; // Center offset of the dial circle vertically
                double outerR2 = 0.065;
                double innerR2 = 0.052;

                for (double t = -hR; t <= hR; t += 0.004) {
                    boolean isCornerTurret = Math.abs(t) > hR - 0.045;

                    double dist = t * t + Math.pow(y - cY, 2.0);
                    boolean insideDial = dist < outerR2 && !isCornerTurret;
                    char dialText = CH_STONE;
                    if (insideDial) {
                        dialText = (dist < innerR2) ? CH_DIAL : CH_IRONWORK;
                        // Add central indicator hand detail line
                        if (dist < innerR2 && (dist < 0.003 || Math.abs(t) < 0.012 && y > cY - 0.04)) {
                            dialText = CH_IRONWORK;
                        }
                    }

                    // Render front/back/left/right wall sweeps with explicit physical inset depth
                    // gaps for the clock panes
                    double zOffset = insideDial ? -(hR - 0.02) : -hR;
                    renderProjectedVertex(t, y, zOffset, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);

                    zOffset = insideDial ? (hR - 0.02) : hR;
                    renderProjectedVertex(t, y, zOffset, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);

                    double xOffset = insideDial ? -(hR - 0.02) : -hR;
                    renderProjectedVertex(xOffset, y, t, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);

                    xOffset = insideDial ? (hR - 0.02) : hR;
                    renderProjectedVertex(xOffset, y, t, dialText, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                }
            }
            // --- E. UPPER BELFRY TIER (Open bell-louvers) ---
            else if (isBelfryLouvers) {
                for (double t = -shaftR; t <= shaftR; t += 0.004) {
                    boolean isCorner = Math.abs(t) > shaftR - 0.045;
                    boolean isVentOpen = !isCorner && (Math.abs(t) > 0.05 && Math.abs(t) < 0.28);
                    char text = isVentOpen ? CH_GLASS : CH_STONE;
                    renderProjectedVertex(t, y, -shaftR, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                        renderProjectedVertex(t, y, -(shaftR - 0.01), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);
                    renderProjectedVertex(t, y, shaftR, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                        renderProjectedVertex(t, y, (shaftR - 0.01), CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);
                    renderProjectedVertex(-shaftR, y, t, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                        renderProjectedVertex(-(shaftR - 0.01), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);
                    renderProjectedVertex(shaftR, y, t, text, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                    if (isVentOpen && Math.sin(y * 150.0) > 0.0)
                        renderProjectedVertex((shaftR - 0.01), y, t, CH_TRACERY, cosX, sinX, cosY, sinY, outputBuffer,
                                zBuffer);
                }
            }
            // --- F. TAPERED PYRAMID ROOF WITH DETAILED CORNER SEAMS ---
            else if (isSpireRoof) {
                double roofProgress = (y - (-1.35)) / (-2.1 - (-1.35));
                double currentRoofRadius = (shaftR * 1.04) * (1.0 - roofProgress * 0.96);
                for (double t = -currentRoofRadius; t <= currentRoofRadius; t += 0.005) {
                    char faceText = CH_COPPER;
                    // Draw stone joints tracing up the 4 pyramidal corner ridges
                    if (Math.abs(Math.abs(t) - currentRoofRadius) < 0.018 || Math.abs(t) < 0.01) {
                        faceText = CH_STONE;
                    }
                    // Extrude projecting gothic gable points near the roof base
                    if (roofProgress < 0.30 && Math.abs(t) < 0.12) {
                        faceText = CH_STONE;
                    }
                    renderProjectedVertex(t, y, -currentRoofRadius, faceText, cosX, sinX, cosY, sinY, outputBuffer,
                            zBuffer);
                    renderProjectedVertex(t, y, currentRoofRadius, faceText, cosX, sinX, cosY, sinY, outputBuffer,
                            zBuffer);
                    renderProjectedVertex(-currentRoofRadius, y, t, faceText, cosX, sinX, cosY, sinY, outputBuffer,
                            zBuffer);
                    renderProjectedVertex(currentRoofRadius, y, t, faceText, cosX, sinX, cosY, sinY, outputBuffer,
                            zBuffer);
                }
            }
            // --- G. TOP AERIAL POLE ---
            else if (isFinialSpoke) {
                renderProjectedVertex(0.0, y, 0.0, CH_SPIRE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            }
        }
    }

    private void renderProjectedVertex(double x, double y, double z, char renderChar, double cosX, double sinX,
            double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        // Transform Step 1: Smooth horizontal rotation around tower's central vertical
        // axis
        double xSpun = x * cosY + z * sinY;
        double ySpun = y;
        double zSpun = -x * sinY + z * cosY;
        // ACCURATE NATIVE CAMERA TRANSLATION MATRIX:
        // We drop the tower base down on the Y axis and move the object forward out in
        // front of the lens.
        double worldX = xSpun;
        double worldY = ySpun - 0.40;
        // Lowers the tower base below the virtual eye sightline floor
        double worldZ = zSpun + CAMERA_DISTANCE;
        // Transform Step 2: Pitch around the lens origin via negative tilt factors
        double rx = worldX;
        // Correct 3D rotation matrix calculation: rises higher into the sky retreats
        // further back on Z-axis,
        // causing parallel lines to taper inward to a clean vanishing point.
        double ry = worldY * cosX - worldZ * sinX;
        double rz = worldY * sinX + worldZ * cosX;
        // Focal perspective projection execution
        double ooz = 1.0 / rz;
        int xp = (int) (40 + 58 * ooz * rx * 2.35);
        // Aspect ratio scaling
        int yp = (int) (-12 + 26 * ooz * ry);
        // Baseline center lowered to display the tall spire cleanly
        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.00003) {
                zBuffer[index] = ooz;
                // Precision light-rig tracing from upper-right studio coordinates
                double shadowCompass = Math.cos(xSpun + 0.45) * Math.cos(ySpun - 0.25);
                double diffuseWeight = 0.45 + 0.55 * Math.max(0.0, shadowCompass);
                int r = (int) (MASONRY_SHD[0] * (1.0 - diffuseWeight) + MASONRY_BASE[0] * diffuseWeight);
                int g = (int) (MASONRY_SHD[1] * (1.0 - diffuseWeight) + MASONRY_BASE[1] * diffuseWeight);
                int b = (int) (MASONRY_SHD[2] * (1.0 - diffuseWeight) + MASONRY_BASE[2] * diffuseWeight);
                if (renderChar == CH_TRACERY) {
                    r *= 0.64;
                    g *= 0.60;
                    b *= 0.56;
                } else if (renderChar == CH_GLASS) {
                    r *= 0.24;
                    g *= 0.22;
                    b *= 0.28;
                } else if (renderChar == CH_DIAL) {
                    r = CLOCK_OPAL[0];
                    g = CLOCK_OPAL[1];
                    b = CLOCK_OPAL[2];
                } else if (renderChar == CH_IRONWORK) {
                    r = DARK_IRON[0];
                    g = DARK_IRON[1];
                    b = DARK_IRON[2];
                } else if (renderChar == CH_COPPER) {
                    r = (int) (COPPER_ROOF[0] * diffuseWeight);
                    g = (int) (COPPER_ROOF[1] * diffuseWeight);
                    b = (int) (COPPER_ROOF[2] * diffuseWeight);
                } else if (renderChar == CH_SPIRE) {
                    r = 245;
                    g = 250;
                    b = 255;
                }
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}