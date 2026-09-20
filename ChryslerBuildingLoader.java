// TODO: Fix the zoom in angles and positions right now its copying Big Bens but it should be different

import java.lang.Math;

public class ChryslerBuildingLoader extends Loader {
    private static final StatusStage[] CHRYSLER_STAGES = {
            new StatusStage(20, "Laying the dark grey brick facade:"),
            new StatusStage(45, "Forging Nirosta steel eagles & hubcaps:"),
            new StatusStage(70, "Riveting the 7 terraced crown arches:"),
            new StatusStage(95, "Mounting the vertex needle:"),
            new StatusStage(100, "Chrysler Building Night Matrix Active!")
    };

    // --- Glyphs ---
    private static final char CH_BRICK = '\u2593';   // ▓ Cool dark grey brick (matte texture)
    private static final char CH_STEEL = '\u2588';   // █ Nirosta steel (highly specular solid)
    private static final char CH_GLASS = '\u2592';   // ▒ Dark glass columns
    private static final char CH_LIGHT = '\u2588';   // █ Warm triangular crown lighting (unshaded solid)
    private static final char CH_SPIRE = '\u00B7';   // · Needle tip

    // --- True Art Deco Night Palette ---
    private static final int[] BRICK_BASE = { 150, 155, 165 }; // Cool, stark grey
    private static final int[] BRICK_SHD = { 35, 40, 50 };     // Deep shadow
    private static final int[] STEEL_BASE = { 225, 240, 255 }; // Bright reflective silver/blue
    private static final int[] STEEL_SHD = { 25, 35, 55 };     
    private static final int[] LIGHT_BASE = { 255, 195, 75 };  // Iconic deep amber/warm yellow
    private static final int[] WINDOW_COLOR = { 12, 16, 22 };

    private static final int[] SKY_TOP = { 8, 12, 20 };
    private static final int[] SKY_BOTTOM = { 30, 40, 55 };

    private double rotationY = 0.0;
    
    // PUSHED IN: The camera is now much closer and aimed higher up the shaft
    private static final double CAMERA_DISTANCE_FAR = 4.0; 
    private static final double TILT_FAR = -0.28; 
    private static final double Y_OFFSET_FAR = -0.65; 

    private static final double CAMERA_DISTANCE_NEAR_BASE = 2.8;
    private static final double TILT_NEAR_BASE = -0.15;
    private static final double Y_OFFSET_NEAR_BASE = 0.20;

    // Punch in extremely tight on the glowing terraced crown
    private static final double CAMERA_DISTANCE_NEAR_CROWN = 1.7;
    private static final double TILT_NEAR_CROWN = -0.25;
    private static final double Y_OFFSET_NEAR_CROWN = -1.30; 

    private double cameraDistance = CAMERA_DISTANCE_FAR;
    private double cameraYOffset = Y_OFFSET_FAR;

    // --- Radii ---
    private static final double R_BASE = 0.48;
    private static final double R_SHAFT_1 = 0.42;
    private static final double R_SHAFT_2 = 0.35;
    private static final double R_CROWN_BASE = 0.28;

    public ChryslerBuildingLoader() {
        super(CHRYSLER_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double currentAngleRad = rotationY % (2.0 * Math.PI);
        if (currentAngleRad < 0) currentAngleRad += (2.0 * Math.PI);
        double distToFront = Math.min(currentAngleRad, Math.abs(2.0 * Math.PI - currentAngleRad));

        double rotationSlowWindow = Math.exp(-Math.pow(distToFront, 2.0) / (1.5 * Math.pow(0.42, 2.0)));
        double dynamicStepSpeed = 0.007 * (1.0 - rotationSlowWindow * 0.85) + 0.0012;
        rotationY += dynamicStepSpeed;

        double dwellWindow = Math.exp(-Math.pow(distToFront, 2.0) / (1.7 * Math.pow(0.30, 2.0)));
        long lapIndex = Math.round(rotationY / (2.0 * Math.PI));
        boolean isCrownDwell = lapIndex >= 1;

        double tiltNearTarget = isCrownDwell ? TILT_NEAR_CROWN : TILT_NEAR_BASE;
        double distNearTarget = isCrownDwell ? CAMERA_DISTANCE_NEAR_CROWN : CAMERA_DISTANCE_NEAR_BASE;
        double yOffsetNearTarget = isCrownDwell ? Y_OFFSET_NEAR_CROWN : Y_OFFSET_NEAR_BASE;

        double tiltX = TILT_FAR * (1.0 - dwellWindow) + tiltNearTarget * dwellWindow;
        cameraDistance = CAMERA_DISTANCE_FAR - (dwellWindow * (CAMERA_DISTANCE_FAR - distNearTarget));
        cameraYOffset = Y_OFFSET_FAR * (1.0 - dwellWindow) + yOffsetNearTarget * dwellWindow;

        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);
        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);

        // STEP 1: NIGHT SKY
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

        // STEP 2: TOWER PROFILE
        final double BASE_TOP = 1.0;
        final double SHAFT1_TOP = 0.2;
        final double SHAFT2_TOP = -0.4;
        final double CROWN_BOT = -0.9;
        final double CROWN_TOP = -1.9;
        final double SPIRE_TIP = -2.9;

        for (double y = 1.9; y >= -3.0; y -= 0.012) {
            if (y > BASE_TOP) {
                renderArtDecoShaftSlice(y, R_BASE, R_BASE * 0.15, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (y > SHAFT1_TOP) {
                renderArtDecoShaftSlice(y, R_SHAFT_1, R_SHAFT_1 * 0.25, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (y > SHAFT2_TOP) {
                renderArtDecoShaftSlice(y, R_SHAFT_2, R_SHAFT_2 * 0.35, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (y > CROWN_BOT) {
                renderArtDecoShaftSlice(y, R_CROWN_BASE, R_CROWN_BASE * 0.1, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                // The massive steel eagle gargoyles at the base of the crown
                if (y <= SHAFT2_TOP && y > SHAFT2_TOP - 0.06) {
                    renderEagles(y, R_CROWN_BASE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                }
            } else if (y > CROWN_TOP) {
                renderTerracedCrownSlice(y, CROWN_BOT, CROWN_TOP, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else if (y > SPIRE_TIP) {
                renderNeedleSpireSlice(y, CROWN_TOP, SPIRE_TIP, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            } else {
                renderProjectedVertex(0.0, y, 0.0, CH_SPIRE, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            }
        }
    }

    // ---------- Tier helpers ----------

    /** 
     * Carves out the corners to create the deeply chamfered/cruciform 
     * footprint unique to Art Deco skyscrapers, completely breaking the "box" look.
     */
    private void renderArtDecoShaftSlice(double y, double r, double inset, double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        // Trace the perimeter of one quadrant, then mirror it to all 4
        drawQuadrantSegment(0, r, r - inset, r, y, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        drawQuadrantSegment(r - inset, r, r - inset, r - inset, y, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        drawQuadrantSegment(r - inset, r - inset, r, r - inset, y, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        drawQuadrantSegment(r, r - inset, r, 0, y, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
    }

    private void drawQuadrantSegment(double x1, double z1, double x2, double z2, double y, double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double dist = Math.hypot(x2 - x1, z2 - z1);
        int steps = (int)(dist / 0.005) + 1;
        for (int i = 0; i <= steps; i++) {
            double p = (double) i / steps;
            double px = x1 + (x2 - x1) * p;
            double pz = z1 + (z2 - z1) * p;
            
            char ch = CH_BRICK;
            // Vertical window stripes & steel banding
            if (Math.abs(px) < 0.06 || Math.abs(pz) < 0.06) ch = CH_GLASS;
            if ((Math.abs(y * 100) % 15) < 2.0) ch = CH_STEEL;

            // Project to all 4 symmetrical quadrants
            renderProjectedVertex(px, y, pz, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-px, y, pz, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-px, y, -pz, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(px, y, -pz, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    private void renderEagles(double y, double radius, double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double gLen = 0.16; // Project outwards significantly
        for (double d = 0; d < gLen; d += 0.006) {
            double drop = 0.02 * (d / gLen); // Angles down slightly like a hood ornament
            renderProjectedVertex(radius + d, y + drop, radius + d, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-radius - d, y + drop, radius + d, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(radius + d, y + drop, -radius - d, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-radius - d, y + drop, -radius - d, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    /** 
     * Implements a physical step-function to create 7 distinct terraces, 
     * masking out exact triangles intersected by vertical steel ribs. 
     */
    private void renderTerracedCrownSlice(double y, double bottomY, double topY, double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double progress = (bottomY - y) / (bottomY - topY); 
        int numTerraces = 7;
        
        // Quantize progress to discrete steps
        int terrace = (int)(progress * numTerraces);
        double localP = (progress * numTerraces) - terrace; // 0.0 to 1.0 within this specific terrace
        
        double tRatio = (double)terrace / numTerraces;
        double nextRatio = (double)(terrace + 1) / numTerraces;
        
        // Base radius of the current terrace step
        double rTerrace = R_CROWN_BASE * (1.0 - Math.pow(tRatio, 1.4));
        double rNext = R_CROWN_BASE * (1.0 - Math.pow(nextRatio, 1.4));
        
        // The terrace is a vertical wall for the first 80%, then a sloped roof for the last 20% stepping in.
        double currentR = (localP < 0.8) ? rTerrace : rTerrace - (rTerrace - rNext) * ((localP - 0.8) / 0.2);
        if (currentR < 0.002) return;

        for (double t = -currentR; t <= currentR; t += 0.004) {
            boolean isLight = false;
            
            // Only draw windows on the vertical face of the terrace, not the roof
            if (localP < 0.8) {
                // Creates the tapering triangle shape bounds
                double triangleWidth = currentR * 0.72 * (1.0 - (localP / 0.8));
                
                // If inside the triangle, AND not masked by a steel mullion
                if (Math.abs(t) < triangleWidth && (Math.abs(t) % 0.05 > 0.015)) {
                    isLight = true;
                }
            }
            
            char ch = isLight ? CH_LIGHT : CH_STEEL;
            
            renderProjectedVertex(t, y, -currentR, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(t, y, currentR, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-currentR, y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(currentR, y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    private void renderNeedleSpireSlice(double y, double bottomY, double topY, double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double progress = (bottomY - y) / (bottomY - topY); 
        double radius = 0.035 * (1.0 - Math.pow(progress, 0.6));
        if (radius < 0.002) return;

        for (double t = -radius; t <= radius; t += 0.004) {
            renderProjectedVertex(t, y, -radius, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(t, y, radius, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(-radius, y, t, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(radius, y, t, CH_STEEL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    private void renderProjectedVertex(double x, double y, double z, char renderChar, double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
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
                if (renderChar == CH_LIGHT) {
                    // Overrides shading, blooms in full color
                    r = LIGHT_BASE[0];
                    g = LIGHT_BASE[1];
                    b = LIGHT_BASE[2];
                } else if (renderChar == CH_STEEL) {
                    // Massive specular exponent (4.5) to make it glint like polished Nirosta
                    double specular = Math.pow(diffuseWeight, 4.5);
                    r = (int) (STEEL_SHD[0] * (1.0 - specular) + STEEL_BASE[0] * specular);
                    g = (int) (STEEL_SHD[1] * (1.0 - specular) + STEEL_BASE[1] * specular);
                    b = (int) (STEEL_SHD[2] * (1.0 - specular) + STEEL_BASE[2] * specular);
                } else if (renderChar == CH_GLASS) {
                    r = WINDOW_COLOR[0];
                    g = WINDOW_COLOR[1];
                    b = WINDOW_COLOR[2];
                } else if (renderChar == CH_SPIRE) {
                    r = 230; g = 240; b = 255;
                } else {
                    // Matte brick shading
                    r = (int) (BRICK_SHD[0] * (1.0 - diffuseWeight) + BRICK_BASE[0] * diffuseWeight);
                    g = (int) (BRICK_SHD[1] * (1.0 - diffuseWeight) + BRICK_BASE[1] * diffuseWeight);
                    b = (int) (BRICK_SHD[2] * (1.0 - diffuseWeight) + BRICK_BASE[2] * diffuseWeight);
                }

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}