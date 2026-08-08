public class EiffelTowerLoader extends Loader {
    private static final StatusStage[] EIFFEL_STAGES = {
        new StatusStage(25, "Forging 4-pillar puddle-iron foundation:"),
        new StatusStage(50, "Riveting transparent lattice openwork:"),
        new StatusStage(75, "Calibrating Parisian sky illumination:"),
        new StatusStage(100, "Eiffel Midnight Sparkle Operational!")
    };

    // Refined architectural typography symbols for ironwork density
    private static final char CH_BEAM    = '\u2588'; // █ Solid structural corner pillars
    private static final char CH_STRUT   = '\u2592'; // ▒ Openwork cross-bracing matrix
    private static final char CH_BEACON  = '\u2605'; // ★ Periodic sparkling strobe node

    // Deep structural coloring for French puddle iron (Tour Eiffel Brown)
    private static final int[] IRON_BASE      = { 68, 58, 51 };
    private static final int[] IRON_HIGHLIGHT = { 115, 102, 92 };

    // Twilight to Midnight Parisian sky backdrop palettes
    private static final int[] SKY_TOP    = { 8, 12, 24 };
    private static final int[] SKY_BOTTOM = { 28, 20, 35 };
    
    // Golden interior floodlight registers
    private static final int[] FLOODLIGHT = { 255, 160, 40 };

    private double timeClock = 0.0;
    private double rotationY = 0.0;
    private static final double CAMERA_DISTANCE = 4.5;

    public EiffelTowerLoader() {
        // This uses 80x22 specifically
        super(EIFFEL_STAGES, 80, 22);
    }

    @Override
    protected void initialize() { }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        rotationY += 0.015;
        timeClock += 0.020;

        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // 1. DYNAMIC LIGHTING TIMERS
        boolean isSparkleHour = (timeClock % 12.0) > 7.0; 

        // STEP 1: RENDER GRADIENT BACKGROUND SKY WITH ATMOSPHERIC HAZE
        for (int yp = 0; yp < 22; yp++) {
            double skyGrad = (double) yp / 21.0;
            int sr = (int) (SKY_TOP[0] * (1.0 - skyGrad) + SKY_BOTTOM[0] * skyGrad);
            int sg = (int) (SKY_TOP[1] * (1.0 - skyGrad) + SKY_BOTTOM[1] * skyGrad);
            int sb = (int) (SKY_TOP[2] * (1.0 - skyGrad) + SKY_BOTTOM[2] * skyGrad);
            
            double cityGlow = Math.max(0, (yp - 8) / 13.0) * 0.25;
            sr += (int) (FLOODLIGHT[0] * cityGlow);
            sg += (int) (FLOODLIGHT[1] * cityGlow);
            sb += (int) (FLOODLIGHT[2] * cityGlow);

            String skyColor = String.format("\u001B[38;2;%d;%d;%dm", Math.min(255, sr), Math.min(255, sg), Math.min(255, sb));
            for (int xp = 0; xp < 80; xp++) {
                outputBuffer[xp + 80 * yp] = skyColor + " " + RESET;
            }
        }

        // STEP 2: GENERATE SQUARE-BASED EIFFEL TOWER PILLARS AND PLATFORMS
        // Scan upward from base floor (y = 1.6) to summit spire (y = -2.0)
        for (double y = 1.6; y >= -2.0; y -= 0.02) {
            
            // Exponential profile curve modeling the square footprint tapering up
            double profileRadius = 0.05 + 0.55 * Math.pow((y + 2.0) / 3.6, 2.5);
            
            // Architectural milestone elevations
            boolean isBasePlaza   = y > 1.55;
            boolean isPlatform1   = y > 0.82 && y < 0.90;
            boolean isPlatform2   = y > 0.12 && y < 0.18;
            boolean isPlatform3   = y > -1.65 && y < -1.58;
            boolean isPlatformLevel = isPlatform1 || isPlatform2 || isPlatform3;

            // Strict 4-corner footprint layout matrix (-1 or 1 for X and Z axes)
            int[][] squareCorners = { {-1, -1}, {1, -1}, {1, 1}, {-1, 1} };

            // Render hollow, transparent cross-bracing frameworks between corners
            if (!isPlatformLevel && !isBasePlaza && y > -1.5) {
                for (int c = 0; c < 4; c++) {
                    int[] c1 = squareCorners[c];
                    int[] c2 = squareCorners[(c + 1) % 4];
                    
                    // Thinned out loop to leave dramatic see-through spaces inside the tower body
                    for (double step = 0.1; step < 0.9; step += 0.2) {
                        double bracePattern = Math.sin(y * 40.0 + step * Math.PI);
                        // High filter threshold means a tighter structure and less solid clutter
                        if (Math.abs(bracePattern) > 0.78) {
                            double xLocal = (c1[0] * (1.0 - step) + c2[0] * step) * profileRadius;
                            double zLocal = (c1[1] * (1.0 - step) + c2[1] * step) * profileRadius;
                            
                            // Passing true for 'isLattice' permits background colors to pass through via blending
                            renderStructuralPoint(xLocal, y, zLocal, CH_STRUT, 0.3, cosY, sinY, isSparkleHour, true, outputBuffer, zBuffer);
                        }
                    }
                }
            }

            // Render primary solid corner pillars or platforms
            for (int[] corner : squareCorners) {
                double xLocal = corner[0] * profileRadius;
                double zLocal = corner[1] * profileRadius;

                if (isPlatformLevel) {
                    // Solid platform rows
                    for (double fill = -1.0; fill <= 1.0; fill += 0.15) {
                        renderStructuralPoint(corner[0] * profileRadius, y, fill * profileRadius, CH_BEAM, 1.0, cosY, sinY, false, false, outputBuffer, zBuffer);
                        renderStructuralPoint(fill * profileRadius, y, corner[1] * profileRadius, CH_BEAM, 1.0, cosY, sinY, false, false, outputBuffer, zBuffer);
                    }
                } else if (isBasePlaza) {
                    continue; // Base arch void space
                } else {
                    // Solid Corner Pillar Columns (isLattice = false, locks out depth buffering completely)
                    renderStructuralPoint(xLocal, y, zLocal, CH_BEAM, 1.0, cosY, sinY, isSparkleHour, false, outputBuffer, zBuffer);

                    // Transparent architectural leg arches
                    if (y > 0.90 && y < 1.55) {
                        double archFactor = 1.0 - Math.pow((y - 0.90) / 0.65, 2.0);
                        if (archFactor > 0.1) {
                            renderStructuralPoint(xLocal * archFactor, y, zLocal, CH_STRUT, 0.6, cosY, sinY, false, true, outputBuffer, zBuffer);
                            renderStructuralPoint(xLocal, y, zLocal * archFactor, CH_STRUT, 0.6, cosY, sinY, false, true, outputBuffer, zBuffer);
                        }
                    }
                }
            }
        }
    }

    private void renderStructuralPoint(double xLocal, double y, double zLocal, char renderChar, double weight,
                                        double cosY, double sinY, boolean isSparkleHour, boolean isLattice, String[] outputBuffer, double[] zBuffer) {
        double rx = xLocal * cosY + zLocal * sinY;
        double ry = y;
        double rz = -xLocal * sinY + zLocal * cosY;

        double ooz = 1.0 / (rz + CAMERA_DISTANCE);
        int xp = (int) (40 + 52 * ooz * rx * 2.1);
        int yp = (int) (11 + 24 * ooz * ry);

        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;

            // ALPHA OVERLAY RULE: If it's a see-through inner lattice segment, let it draw even if a back-leg is behind it
            if (ooz > zBuffer[index] || (isLattice && ooz > zBuffer[index] - 0.08)) {
                
                // Track solid geometries firmly in the Z-Buffer mask
                if (!isLattice) {
                    zBuffer[index] = ooz;
                }

                double lightUpwardIntens = Math.max(0.0, 1.0 - (y + 1.2) / 2.8);
                
                int r = (int) (IRON_BASE[0] * (1.0 - weight) + IRON_HIGHLIGHT[0] * weight);
                int g = (int) (IRON_BASE[1] * (1.0 - weight) + IRON_HIGHLIGHT[1] * weight);
                int b = (int) (IRON_BASE[2] * (1.0 - weight) + IRON_HIGHLIGHT[2] * weight);

                r += (int) (FLOODLIGHT[0] * lightUpwardIntens * 0.85);
                g += (int) (FLOODLIGHT[1] * lightUpwardIntens * 0.85);
                b += (int) (FLOODLIGHT[2] * lightUpwardIntens * 0.50);

                if (isSparkleHour && renderChar != CH_BEAM) {
                    double randomSparkleHash = Math.sin(xp * 12.9898 + yp * 78.233 + timeClock) * 43758.5453;
                    if ((randomSparkleHash % 1.0) > 0.97) {
                        renderChar = CH_BEACON;
                        r = 255; g = 255; b = 255;
                    }
                }

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}
