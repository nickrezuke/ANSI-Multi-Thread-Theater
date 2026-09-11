// TODO: Fix the camera angle and the zooming (double zoom peak??)

public class AcropolisOfAthensLoader extends Loader {
    private static final StatusStage[] ACROPOLIS_STAGES = {
            new StatusStage(25, "Quarrying Pentelic marble foundations:"),
            new StatusStage(50, "Erecting Doric columns and cella walls:"),
            new StatusStage(75, "Sculpting the frieze and pediment metopes:"),
            new StatusStage(100, "Acropolis of Athens Operational!")
    };

    // Refined architectural typography symbols for classical Greek geometry
    private static final char CH_STONE = '\u2588';  // █ Solid marble blocks
    private static final char CH_COLUMN = '\u2593'; // ▓ Fluted shadows for Doric pillars
    private static final char CH_DETAIL = '\u2592'; // ▒ Sculpted tympanum and frieze reliefs
    private static final char CH_ROOF = '\u2591';   // ░ Terracotta roof tiles

    // Pentelic Marble fixed palette: Sunlit Golden Warmth
    private static final int[] MARBLE_BASE = { 245, 235, 215 }; // Bright sunlit marble
    private static final int[] MARBLE_SHD = { 170, 140, 110 };  // Warm Mediterranean shadow

    // Mediterranean Sky gradient endpoints (Clear deep blue fading to azure)
    private static final int[] SKY_TOP = { 15, 60, 150 };
    private static final int[] SKY_BOTTOM = { 100, 180, 240 };

    private double rotationY = 0.0;
    // Wider base distance to fit the 2:1 rectangular footprint of the Parthenon
    private static final double CAMERA_DISTANCE = 8.5; 

    public AcropolisOfAthensLoader() {
        // This uses 80x22 specifically
        super(ACROPOLIS_STAGES, 80, 22);
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

        // 1. ICONIC ANGLE ENGINE: Focus on the classic 3/4 front view (~25 degrees)
        double targetAngle1 = Math.PI / 7.0; 
        double targetAngle2 = 2.0 * Math.PI - Math.PI / 7.0;

        // Calculate shortest circular distance to the two front 3/4 views
        double d1 = Math.min(Math.abs(currentAngleRad - targetAngle1), 2.0 * Math.PI - Math.abs(currentAngleRad - targetAngle1));
        double d2 = Math.min(Math.abs(currentAngleRad - targetAngle2), 2.0 * Math.PI - Math.abs(currentAngleRad - targetAngle2));
        double distToIconicAngle = Math.min(d1, d2);

        // GAUSSIAN ROTATION SLOWDOWN (Tracks iconic viewing angle proximity)
        double iconicWindow = Math.exp(-Math.pow(distToIconicAngle, 2.0) / (2.0 * Math.pow(0.35, 2.0)));
        double dynamicStepSpeed = 0.024 * (1.0 - iconicWindow * 0.85) + 0.002;

        rotationY += dynamicStepSpeed;

        // 2. GAUSSIAN CAMERA VERTICAL TILT DYNAMICS
        // Less tilt at the front for a more imposing, monumental ground-level feel
        double tiltX = 0.35 * (1.0 - iconicWindow) + 0.15 * iconicWindow;

        // 3. DYNAMIC ZOOM MODIFIER: Camera zooms deeply IN at the front face
        double zoomInUnits = 3.5;
        double effectiveDistance = CAMERA_DISTANCE - (iconicWindow * zoomInUnits);

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
        for (double y = 1.3; y >= -0.8; y -= 0.020) {

            boolean isCrepidoma = y > 1.0;
            boolean isColonnade = y <= 1.0 && y > 0.0;
            boolean isEntablature = y <= 0.0 && y > -0.2;
            boolean isPedimentRoof = y <= -0.2;

            // --- A. CREPIDOMA (The 3 Stepped Foundation) ---
            if (isCrepidoma) {
                double stepRadius;
                if (y > 1.2) stepRadius = 0.30;
                else if (y > 1.1) stepRadius = 0.15;
                else stepRadius = 0.0;

                double maxX = 1.05 + stepRadius;
                double maxZ = 2.05 + stepRadius;

                // Draw perimeter shell for optimization
                for (double x = -maxX; x <= maxX; x += 0.04) {
                    renderProjectedPoint(x, y, -maxZ, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                    renderProjectedPoint(x, y, maxZ, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                }
                for (double z = -maxZ; z <= maxZ; z += 0.04) {
                    renderProjectedPoint(-maxX, y, z, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                    renderProjectedPoint(maxX, y, z, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                }
                // Fill the uppermost floor beneath the columns
                if (y < 1.05) {
                    for (double x = -maxX; x <= maxX; x += 0.05) {
                        for (double z = -maxZ; z <= maxZ; z += 0.05) {
                            renderProjectedPoint(x, y, z, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                        }
                    }
                }
            }
            // --- B. COLONNADE & INNER CELLA WALLS ---
            else if (isColonnade) {
                // Inner Cella walls (hollow rectangle housing the statue)
                double cellaMaxX = 0.6;
                double cellaMaxZ = 1.4;
                for (double x = -cellaMaxX; x <= cellaMaxX; x += 0.04) {
                    renderProjectedPoint(x, y, -cellaMaxZ, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                    renderProjectedPoint(x, y, cellaMaxZ, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                }
                for (double z = -cellaMaxZ; z <= cellaMaxZ; z += 0.04) {
                    renderProjectedPoint(-cellaMaxX, y, z, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                    renderProjectedPoint(cellaMaxX, y, z, CH_STONE, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                }

                // Outer Doric Columns (8 on the front/back, 17 on the sides)
                for (int i = 0; i < 8; i++) {
                    double cx = -1.0 + i * (2.0 / 7.0);
                    drawColumn(cx, y, -2.0, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                    drawColumn(cx, y, 2.0, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                }
                for (int j = 1; j < 16; j++) { // Skip corners already rendered
                    double cz = -2.0 + j * (4.0 / 16.0);
                    drawColumn(-1.0, y, cz, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                    drawColumn(1.0, y, cz, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                }
            }
            // --- C. ENTABLATURE (Architrave and Frieze) ---
            else if (isEntablature) {
                double eMaxX = 1.05;
                double eMaxZ = 2.05;
                for (double x = -eMaxX; x <= eMaxX; x += 0.03) {
                    for (double z = -eMaxZ; z <= eMaxZ; z += 0.03) {
                        // Draw structural shell
                        if (Math.abs(x) >= eMaxX - 0.04 || Math.abs(z) >= eMaxZ - 0.04 || y > -0.03) {
                            // Frieze details (metopes and triglyphs) on the upper half
                            char detail = (y < -0.10 && ((int)((x + z) * 25) % 2 == 0)) ? CH_DETAIL : CH_STONE;
                            renderProjectedPoint(x, y, z, detail, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                        }
                    }
                }
            }
            // --- D. PEDIMENT & SLOPED ROOF ---
            else if (isPedimentRoof) {
                for (double x = -1.05; x <= 1.05; x += 0.03) {
                    // Calculate the peak logic - sloped triangle from center out
                    double currentRoofEdgeY = -0.2 - 0.5 * (1.0 - Math.abs(x) / 1.05);
                    
                    if (y >= currentRoofEdgeY) {
                        for (double z = -2.05; z <= 2.05; z += 0.03) {
                            boolean isFrontBackFace = Math.abs(z) > 1.95;
                            boolean isRoofSurface = Math.abs(y - currentRoofEdgeY) < 0.04;

                            if (isFrontBackFace) {
                                // Tympanum (inner triangle) sculptures
                                char fill = (Math.abs(x) < 0.8 && y > currentRoofEdgeY + 0.05) ? CH_DETAIL : CH_STONE;
                                renderProjectedPoint(x, y, z, fill, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                            } else if (isRoofSurface) {
                                renderProjectedPoint(x, y, z, CH_ROOF, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
                            }
                        }
                    }
                }
            }
        }
    }

    private void drawColumn(double cx, double y, double cz, double cosX, double sinX, double cosY, double sinY, double effectiveDistance, String[] outputBuffer, double[] zBuffer) {
        double r = 0.06; // Standard column radius
        // 6 points to create a fluted cylinder appearance
        for (double a = 0; a < 2 * Math.PI; a += Math.PI / 3) {
            double px = cx + r * Math.cos(a);
            double pz = cz + r * Math.sin(a);
            renderProjectedPoint(px, y, pz, CH_COLUMN, cosX, sinX, cosY, sinY, effectiveDistance, outputBuffer, zBuffer);
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
                
                // Static Mediterranean directional lighting computation
                double lightCompassDirection = Math.cos(xSpun + 0.5) * Math.cos(ySpun - 0.4);
                double intensity = 0.45 + 0.55 * Math.max(0.0, lightCompassDirection);
                
                int r = (int) (MARBLE_SHD[0] * (1.0 - intensity) + MARBLE_BASE[0] * intensity);
                int g = (int) (MARBLE_SHD[1] * (1.0 - intensity) + MARBLE_BASE[1] * intensity);
                int b = (int) (MARBLE_SHD[2] * (1.0 - intensity) + MARBLE_BASE[2] * intensity);
                
                // Texture specific color adjustments
                if (renderChar == CH_DETAIL) {
                    r = (int)(r * 0.85);
                    g = (int)(g * 0.85);
                    b = (int)(b * 0.85);
                } else if (renderChar == CH_ROOF) {
                    // Inject a rusty terracotta tint to the roof tiles
                    r = (int)(r * 0.85 + 40);
                    g = (int)(g * 0.70 + 10);
                    b = (int)(b * 0.60);
                }
                
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}