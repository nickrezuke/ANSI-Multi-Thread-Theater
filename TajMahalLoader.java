public class TajMahalLoader extends Loader {
    private static final StatusStage[] TAJ_STAGES = {
            new StatusStage(25, "Carving high-density marble foundation:"),
            new StatusStage(50, "Extruding pristine facades and corner minarets:"),
            new StatusStage(75, "Revolving sub-degree math onion dome:"),
            new StatusStage(100, "Agra Golden Dusk Matrix Operational!")
    };

    // Refined architectural typography symbols for geometric surfaces
    private static final char CH_MARBLE = '\u2588'; // █ Solid ivory/white marble walls
    private static final char CH_DETAIL = '\u2592'; // ▒ Inlay textures / shadowed arch recesses
    private static final char CH_SPIRE = '\u2591'; // ░ Fine metal finials and spires

    // Makrana Marble fixed palette: Golden Ivory dusk lighting
    private static final int[] MARBLE_BASE = { 255, 246, 230 }; // Rich ivory gold reflection
    private static final int[] MARBLE_SHD = { 165, 145, 130 }; // Warm sand shadow

    // Dusk sky gradient endpoints
    private static final int[] SKY_TOP = { 220, 120, 60 };
    private static final int[] SKY_BOTTOM = { 245, 190, 115 };

    private double rotationY = 0.0;
    private static final double CAMERA_DISTANCE = 4.2;

    public TajMahalLoader() {
        super(TAJ_STAGES);
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

        // SINGLE-DIP SYMMETRY EQUATION: Evaluates distance to 0 (and 2PI) specifically
        double distToFront = Math.min(currentAngleRad, Math.abs(2.0 * Math.PI - currentAngleRad));

        // 1. GAUSSIAN ROTATION SLOWDOWN ENGINE (Tracks 2PI full loop proximity)
        double rotationSlowWindow = Math.exp(-Math.pow(distToFront, 2.0) / (2.0 * Math.pow(0.40, 2.0)));
        double dynamicStepSpeed = 0.024 * (1.0 - rotationSlowWindow * 0.82) + 0.002;

        rotationY += dynamicStepSpeed;

        // 2. GAUSSIAN CAMERA VERTICAL TILT DYNAMICS (Dips down only at the front face)
        double tiltSlowWindow = Math.exp(-Math.pow(distToFront, 2.0) / (2.0 * Math.pow(0.35, 2.0)));
        double tiltX = 0.42 * (1.0 - tiltSlowWindow) + 0.02 * tiltSlowWindow;

        // 3. DYNAMIC DUSK ZOOM MODIFIER: Camera backs out by an extra 1.5 units during
        // the front dip
        double zoomOutUnits = 1.5;
        double effectiveDistance = CAMERA_DISTANCE + (tiltSlowWindow * zoomOutUnits);

        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // STEP 1: RENDER FULL CANVAS SKY GRADIENT
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

        // STEP 2: VERTICAL COMPONENT GEOMETRY PIPELINE
        for (double y = 1.3; y >= -1.9; y -= 0.020) {

            boolean isBaseTerrace = y > 1.1;
            boolean isMainTombBlock = y <= 1.1 && y > -0.1;
            boolean isCenterPishtaq = y <= -0.1 && y > -0.4;
            boolean isDomeDrum = y <= -0.4 && y > -0.6;
            boolean isMainDome = y <= -0.6 && y > -1.3;
            boolean isSpireSpoke = y <= -1.3;

            // --- A. MAIN PLINTH TERRACE ---
            if (isBaseTerrace) {
                for (double x = -1.2; x <= 1.2; x += 0.015) {
                    for (double z = -1.2; z <= 1.2; z += 0.015) {
                        if (Math.max(Math.abs(x), Math.abs(z)) < 1.1) {
                            renderProjectedPoint(x, y, z, CH_MARBLE, cosX, sinX, cosY, sinY, effectiveDistance,
                                    outputBuffer, zBuffer);
                        }
                    }
                }
            }
            // --- B. CENTRAL ARCHED TOMB BLOCK ---
            else if (isMainTombBlock) {
                for (double x = -0.85; x <= 0.85; x += 0.015) {
                    for (double z = -0.85; z <= 0.85; z += 0.015) {
                        double absX = Math.abs(x);
                        double absZ = Math.abs(z);
                        double wallThickness = Math.max(absX, absZ);

                        if (absX + absZ > 1.25)
                            continue;

                        if (wallThickness > 0.76) {
                            char texturing = CH_MARBLE;
                            boolean isFrontIwan = z < -0.74 && absX < 0.35 && y > 0.1;
                            boolean isBackIwan = z > 0.74 && absX < 0.35 && y > 0.1;
                            boolean isLeftIwan = x < -0.74 && absZ < 0.35 && y > 0.1;
                            boolean isRightIwan = x > 0.74 && absZ < 0.35 && y > 0.1;

                            if (isFrontIwan || isBackIwan || isLeftIwan || isRightIwan) {
                                if (wallThickness > 0.82)
                                    continue;
                                texturing = CH_DETAIL;
                            }
                            renderProjectedPoint(x, y, z, texturing, cosX, sinX, cosY, sinY, effectiveDistance,
                                    outputBuffer, zBuffer);
                        }
                    }
                }
            }
            // --- C. CENTER FACADE PISHTAQ WALLS & FLANKING CHHATRI KIOSKS ---
            else if (isCenterPishtaq) {
                for (double x = -0.80; x <= 0.80; x += 0.015) {
                    for (double z = -0.80; z <= 0.80; z += 0.015) {
                        double absX = Math.abs(x);
                        double absZ = Math.abs(z);

                        boolean isFrontWall = z < -0.68 && absX < 0.40;
                        boolean isBackWall = z > 0.68 && absX < 0.40;
                        boolean isLeftWall = x < -0.68 && absZ < 0.40;
                        boolean isRightWall = x > 0.68 && absZ < 0.40;

                        if (isFrontWall || isBackWall || isLeftWall || isRightWall) {
                            renderProjectedPoint(x, y, z, CH_MARBLE, cosX, sinX, cosY, sinY, effectiveDistance,
                                    outputBuffer, zBuffer);
                        }
                    }
                }

                double[][] chhatriPositions = { { -0.55, -0.55 }, { 0.55, -0.55 }, { 0.55, 0.55 }, { -0.55, 0.55 } };
                for (double[] pos : chhatriPositions) {
                    for (double angle = 0; angle < 2.0 * Math.PI; angle += 0.08) {
                        double cx = pos[0] + Math.cos(angle) * 0.09;
                        double cz = pos[1] + Math.sin(angle) * 0.09;
                        renderProjectedPoint(cx, y, cz, CH_DETAIL, cosX, sinX, cosY, sinY, effectiveDistance,
                                outputBuffer, zBuffer);
                    }
                }
            }
            // --- D. CORE DOME COLLAR DRUM ---
            else if (isDomeDrum) {
                for (double angle = 0; angle < 2.0 * Math.PI; angle += 0.02) {
                    double r = 0.44;
                    renderProjectedPoint(Math.cos(angle) * r, y, Math.sin(angle) * r, CH_MARBLE, cosX, sinX, cosY, sinY,
                            effectiveDistance, outputBuffer, zBuffer);
                }
            }
            // --- E. BULBOUS ONION DOME ---
            else if (isMainDome) {
                double normalizedDomeY = (y - (-0.6)) / (-1.3 - (-0.6));
                double domeProfileRadius = 0.44 * Math.sin(normalizedDomeY * Math.PI) * (1.12 - 0.28 * normalizedDomeY)
                        + 0.02;
                if (normalizedDomeY < 0.05)
                    domeProfileRadius = 0.44;

                for (double angle = 0; angle < 2.0 * Math.PI; angle += 0.015) {
                    double dx = Math.cos(angle) * domeProfileRadius;
                    double dz = Math.sin(angle) * domeProfileRadius;
                    renderProjectedPoint(dx, y, dz, CH_MARBLE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer,
                            zBuffer);
                }
            }
            // --- F. TOP FINIAL SPIRE ---
            else if (isSpireSpoke) {
                renderProjectedPoint(0.0, y, 0.0, CH_SPIRE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer,
                        zBuffer);
            }

            // --- G. FOUR MINARET CORNER PILLARS ---
            double[][] minaretPositions = { { -1.2, -1.2 }, { 1.2, -1.2 }, { 1.2, 1.2 }, { -1.2, 1.2 } };
            if (y > -0.6) {
                for (int m = 0; m < 4; m++) {
                    double mx = minaretPositions[m][0];
                    double mz = minaretPositions[m][1];
                    double minaretRadius = 0.07 * (1.0 - (y + 0.6) * 0.08);

                    char minaretChar = CH_MARBLE;
                    boolean isBalconyStage = Math.abs(y - 0.8) < 0.03 || Math.abs(y - 0.2) < 0.03
                            || Math.abs(y - (-0.4)) < 0.03;
                    if (isBalconyStage) {
                        minaretRadius *= 1.35;
                        minaretChar = CH_DETAIL;
                    }

                    for (double angle = 0; angle < 2.0 * Math.PI; angle += 0.05) {
                        double xPos = mx + Math.cos(angle) * minaretRadius;
                        double zPos = mz + Math.sin(angle) * minaretRadius;
                        renderProjectedPoint(xPos, y, zPos, minaretChar, cosX, sinX, cosY, sinY, effectiveDistance,
                                outputBuffer, zBuffer);
                    }
                }
            }
        }
    }

    private void renderProjectedPoint(double x, double y, double z, char renderChar, double cosX, double sinX,
            double cosY, double sinY, double effectiveDistance, String[] outputBuffer, double[] zBuffer) {
        double xSpun = x * cosY + z * sinY;
        double ySpun = y;
        double zSpun = -x * sinY + z * cosY;
        double rx = xSpun;
        double ry = ySpun * cosX - zSpun * sinX;
        double rz = ySpun * sinX + zSpun * cosX;
        double ooz = 1.0 / (rz + effectiveDistance);
        int xp = (int) (40 + 54 * ooz * rx * 1.95);
        int yp = (int) (11 + 25 * ooz * ry);
        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.00005) {
                zBuffer[index] = ooz;
                // Static sunset lighting computation
                double lightCompassDirection = Math.cos(xSpun + 0.5) * Math.cos(ySpun - 0.4);
                double intensity = 0.45 + 0.55 * Math.max(0.0, lightCompassDirection);
                int r = (int) (MARBLE_SHD[0] * (1.0 - intensity) + MARBLE_BASE[0] * intensity);
                int g = (int) (MARBLE_SHD[1] * (1.0 - intensity) + MARBLE_BASE[1] * intensity);
                int b = (int) (MARBLE_SHD[2] * (1.0 - intensity) + MARBLE_BASE[2] * intensity);
                if (renderChar == CH_DETAIL) {
                    r *= 0.76;
                    g *= 0.74;
                    b *= 0.80;
                }
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}