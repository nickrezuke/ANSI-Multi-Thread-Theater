// TODO: Finish this implementation of the Sydney Opera House
//
// GEOMETRY NOTE: every shell below is rendered as a SECTOR OF ONE SHARED SPHERE
// (see SPHERE_R). That mirrors Utzon's real "spherical solution" for the roof:
// despite looking like different sizes, every shell in the real building is cut
// from the surface of a single common sphere. Varying just two bounds per shell
// -- phiMax (how much of the sphere you expose, i.e. how tall/open the shell is)
// and halfTheta (how wide the wedge is) -- reproduces the tapering, curved-rib
// silhouette with one reusable formula instead of hand-fitting a curve per shell.

public class SydneyOperaHouseLoader extends Loader {
    private static final StatusStage[] OPERA_HOUSE_STAGES = {
            new StatusStage(20, "Pouring the Tarana granite podium terraces:"),
            new StatusStage(45, "Casting precast rib segments from one master sphere:"),
            new StatusStage(70, "Fixing glossy white & cream chevron tiles:"),
            new StatusStage(90, "Glazing the curtain walls beneath the shells:"),
            new StatusStage(100, "Sydney Harbour Matrix Operational!")
    };

    // --- Glyphs (each ONE unique codepoint -> one unique color branch) ---
    private static final char CH_TILE_A = '\u2588'; // █ Glossy white shell tile
    private static final char CH_TILE_B = '\u2589'; // ▉ Matte cream shell tile (chevron alternation)
    private static final char CH_RIB_TRIM = '\u2593'; // ▓ Shadowed precast rib edge
    private static final char CH_GLASS = '\u2592'; // ▒ Glazed curtain wall beneath the shells
    private static final char CH_PODIUM = '\u258C'; // ▌ Tarana pink granite podium

    // --- Palette ---
    private static final int[] TILE_A_BASE = { 250, 248, 242 };
    private static final int[] TILE_A_SHD = { 188, 186, 180 };
    private static final int[] TILE_B_BASE = { 232, 223, 202 };
    private static final int[] TILE_B_SHD = { 172, 160, 132 };
    private static final int[] RIB_TRIM_COLOR = { 150, 148, 142 };
    private static final int[] GLASS_COLOR = { 32, 66, 76 };
    private static final int[] PODIUM_BASE = { 205, 152, 142 };
    private static final int[] PODIUM_SHD = { 116, 82, 74 };

    private static final int[] SKY_TOP = { 92, 152, 208 };
    private static final int[] SKY_BOTTOM = { 202, 226, 236 };
    private static final int[] WATER_TOP = { 60, 118, 140 };
    private static final int[] WATER_BOTTOM = { 22, 55, 72 };

    // --- Shared master sphere radius: every shell is a sector of THIS sphere. ---
    private static final double SPHERE_R = 1.05;

    private static final double PODIUM_TOP_Y = 0.42;

    private double rotationY = 0.0;
    private static final double CAMERA_DISTANCE = 8.4;
    private static final double TILT_X = -0.20; // gentle upward look, harbour-edge eye level
    private static final double WORLD_Y_OFFSET = 0.20;
    // These four were verified in a Python port of this exact projection math,
    // sweeping the full 0-2pi rotation and checking the resulting screen bounds
    // (see the "onscreen/offscreen" check) rather than picked by eye -- the
    // previous guesses put every point below row 30 on a 22-row screen, which is
    // why nothing rendered.
    private static final double X_SCALE = 189.0;
    private static final double Y_SCALE = 130.0;
    private static final double Y_SCREEN_OFFSET = -17.8;

    public SydneyOperaHouseLoader() {
        super(OPERA_HOUSE_STAGES);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        rotationY += 0.012;

        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);
        double cosX = Math.cos(TILT_X);
        double sinX = Math.sin(TILT_X);

        // STEP 1: SKY
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

        // STEP 2: HARBOUR WATER (bottom rows, below the horizon line)
        int horizonRow = 17;
        for (int yp = horizonRow; yp < 22; yp++) {
            double waterGrad = (double) (yp - horizonRow) / (21 - horizonRow);
            int wr = (int) (WATER_TOP[0] * (1.0 - waterGrad) + WATER_BOTTOM[0] * waterGrad);
            int wg = (int) (WATER_TOP[1] * (1.0 - waterGrad) + WATER_BOTTOM[1] * waterGrad);
            int wb = (int) (WATER_TOP[2] * (1.0 - waterGrad) + WATER_BOTTOM[2] * waterGrad);
            String waterColor = String.format("\u001B[38;2;%d;%d;%dm", wr, wg, wb);
            for (int xp = 0; xp < 80; xp++) {
                outputBuffer[xp + 80 * yp] = waterColor + " " + RESET;
            }
        }

        // STEP 3: PODIUM -- two stepped granite terraces (shrunk footprint so the
        // building's silhouette dominates instead of a big flat platform)
        renderPodiumRing(PODIUM_TOP_Y + 0.16, 1.15, 0.68, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        renderPodiumRing(PODIUM_TOP_Y, 0.95, 0.55, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);

        // STEP 4: GLAZED CURTAIN WALL running beneath the shells
        renderPodiumRing(PODIUM_TOP_Y - 0.14, 0.80, 0.42, CH_GLASS, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);

        // STEP 5: SHELLS -- every one is a sector of the SAME sphere (SPHERE_R).
        // halfTheta is wide relative to phiMax now, so each shell reads as a full,
        // rounded sail (more sphere exposed) rather than a thin spike.
        // Concert Hall group (larger, taller shells, fanned toward the left):
        renderShell(-0.85, -0.08, 0.66, 0.42, 0.10, 6, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        renderShell(-0.55, 0.02, 0.85, 0.48, 0.13, 7, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        renderShell(-0.24, 0.12, 1.05, 0.55, 0.16, 8, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);

        // Joan Sutherland Theatre group (smaller, fewer shells, 
        // fanned toward the right):
        renderShell(0.45, -0.05, 0.60, 0.38, 0.09, 6, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        renderShell(0.72, 0.05, 0.76, 0.44, 0.12, 7, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);

        // Bennelong Restaurant group -- smaller still, and note the NEGATIVE lean:
        // these two shells are genuinely rotated to open the opposite way from the
        // other two groups in the real building, which is a big part of why the
        // roofline reads as asymmetric in photos instead of one uniform fan.
        renderShell(1.05, 0.30, 0.45, 0.30, -0.10, 5, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        renderShell(1.28, 0.42, 0.55, 0.34, -0.13, 6, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
    }

    
    // A rectangular terrace/glass ring using pink-granite 
    // podium coloring by default.
    
    private void renderPodiumRing(double y, double halfX, double halfZ, double cosX, double sinX, double cosY,
            double sinY, String[] outputBuffer, double[] zBuffer) {
        renderPodiumRing(y, halfX, halfZ, CH_PODIUM, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
    }

    private void renderPodiumRing(double y, double halfX, double halfZ, char ch, double cosX, double sinX,
            double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        for (double t = -halfX; t <= halfX; t += 0.02) {
            renderProjectedVertex(t, y, -halfZ, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(t, y, halfZ, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
        for (double t = -halfZ; t <= halfZ; t += 0.02) {
            renderProjectedVertex(-halfX, y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            renderProjectedVertex(halfX, y, t, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
        }
    }

    /**
     * Renders one shell as a sector of the shared master sphere: phi in
     * [0, phiMax] sweeps from the apex (pole) down to the base rim, and theta in
     * [-halfTheta, halfTheta] sweeps across the wedge's width. The shell is then
     * leaned back by 'lean' radians and translated so its base rim sits on the
     * podium at (baseX, baseZ).
     */
    private void renderShell(double baseX, double baseZ, double phiMax, double halfTheta, double lean,
            int tileBands, double cosX, double sinX, double cosY, double sinY, String[] outputBuffer,
            double[] zBuffer) {
        double cosLean = Math.cos(lean);
        double sinLean = Math.sin(lean);

        // Solve for the sphere center that places this shell's rim exactly on the
        // podium at (baseX, baseZ) -- see the derivation in the class comment.
        double centerX = baseX - SPHERE_R * Math.sin(phiMax);
        double centerY = PODIUM_TOP_Y + SPHERE_R * Math.cos(phiMax) * cosLean;
        double centerZ = baseZ + SPHERE_R * Math.cos(phiMax) * sinLean;

        double phiStep = 0.02;
        double thetaStep = 0.02;

        for (double phi = 0.04; phi <= phiMax; phi += phiStep) {
            for (double theta = -halfTheta; theta <= halfTheta; theta += thetaStep) {
                double lx = SPHERE_R * Math.sin(phi) * Math.cos(theta);
                double lyRaw = -SPHERE_R * Math.cos(phi);
                double lz = SPHERE_R * Math.sin(phi) * Math.sin(theta);

                double ly = lyRaw * cosLean - lz * sinLean;
                double lzLean = lyRaw * sinLean + lz * cosLean;

                double wx = centerX + lx;
                double wy = centerY + ly;
                double wz = centerZ + lzLean;

                char ch;
                boolean isRibEdge = (halfTheta - Math.abs(theta)) < thetaStep * 1.4;
                if (isRibEdge) {
                    ch = CH_RIB_TRIM;
                } else {
                    int band = (int) Math.floor(((theta + halfTheta) / (2.0 * halfTheta)) * tileBands);
                    ch = (band % 2 == 0) ? CH_TILE_A : CH_TILE_B;
                }
                renderProjectedVertex(wx, wy, wz, ch, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            }
        }
    }

    private void renderProjectedVertex(double x, double y, double z, char renderChar, double cosX, double sinX,
            double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        double xSpun = x * cosY + z * sinY;
        double ySpun = y;
        double zSpun = -x * sinY + z * cosY;

        double worldX = xSpun;
        double worldY = ySpun - WORLD_Y_OFFSET;
        double worldZ = zSpun + CAMERA_DISTANCE;

        double rx = worldX;
        double ry = worldY * cosX - worldZ * sinX;
        double rz = worldY * sinX + worldZ * cosX;

        double ooz = 1.0 / rz;
        int xp = (int) (40 + X_SCALE * ooz * rx);
        int yp = (int) (Y_SCREEN_OFFSET + Y_SCALE * ooz * ry);

        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.00003) {
                zBuffer[index] = ooz;

                double shadowCompass = Math.cos(xSpun + 0.45) * Math.cos(ySpun - 0.25);
                double diffuseWeight = 0.45 + 0.55 * Math.max(0.0, shadowCompass);

                int r, g, b;
                if (renderChar == CH_TILE_A) {
                    r = (int) (TILE_A_SHD[0] * (1.0 - diffuseWeight) + TILE_A_BASE[0] * diffuseWeight);
                    g = (int) (TILE_A_SHD[1] * (1.0 - diffuseWeight) + TILE_A_BASE[1] * diffuseWeight);
                    b = (int) (TILE_A_SHD[2] * (1.0 - diffuseWeight) + TILE_A_BASE[2] * diffuseWeight);
                } else if (renderChar == CH_TILE_B) {
                    r = (int) (TILE_B_SHD[0] * (1.0 - diffuseWeight) + TILE_B_BASE[0] * diffuseWeight);
                    g = (int) (TILE_B_SHD[1] * (1.0 - diffuseWeight) + TILE_B_BASE[1] * diffuseWeight);
                    b = (int) (TILE_B_SHD[2] * (1.0 - diffuseWeight) + TILE_B_BASE[2] * diffuseWeight);
                } else if (renderChar == CH_RIB_TRIM) {
                    r = (int) (RIB_TRIM_COLOR[0] * (0.65 + 0.35 * diffuseWeight));
                    g = (int) (RIB_TRIM_COLOR[1] * (0.65 + 0.35 * diffuseWeight));
                    b = (int) (RIB_TRIM_COLOR[2] * (0.65 + 0.35 * diffuseWeight));
                } else if (renderChar == CH_GLASS) {
                    r = GLASS_COLOR[0];
                    g = GLASS_COLOR[1];
                    b = GLASS_COLOR[2];
                } else {
                    // CH_PODIUM default
                    r = (int) (PODIUM_SHD[0] * (1.0 - diffuseWeight) + PODIUM_BASE[0] * diffuseWeight);
                    g = (int) (PODIUM_SHD[1] * (1.0 - diffuseWeight) + PODIUM_BASE[1] * diffuseWeight);
                    b = (int) (PODIUM_SHD[2] * (1.0 - diffuseWeight) + PODIUM_BASE[2] * diffuseWeight);
                }

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}