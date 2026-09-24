// TODO: Improve this very basic geometry.  Are the arms the right way?

public class StatueOfLibertyLoader extends Loader {
    private static final StatusStage[] LIBERTY_STAGES = {
            new StatusStage(25, "Laying the star-shaped Fort Wood pedestal:"),
            new StatusStage(50, "Assembling the internal iron pylon framework:"),
            new StatusStage(75, "Attaching flat-shaded copper repoussé skin:"),
            new StatusStage(100, "Lighting the low-poly golden torch!")
    };

    private static final char CH_SOLID = '\u2588';

    // Color Palettes
    private static final int[] COPPER_BASE = { 100, 190, 140 }; 
    private static final int[] COPPER_SHD = { 20, 60, 45 };
    private static final int[] STONE_BASE = { 170, 160, 140 };
    private static final int[] STONE_SHD = { 60, 55, 45 };
    private static final int[] FLAME_GLOW = { 255, 200, 50 };   

    // New York Harbor Twilight Sky
    private static final int[] SKY_TOP = { 10, 20, 50 };
    private static final int[] SKY_BOTTOM = { 80, 110, 130 };

    private double rotationY = 0.0;

    public StatueOfLibertyLoader() {
        super(LIBERTY_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
    }

    // --- 3D VECTOR MATH ENGINE ---
    static class Vec3 {
        double x, y, z;
        Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
        Vec3 cross(Vec3 o) { return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        Vec3 normalize() { 
            double mag = Math.sqrt(x * x + y * y + z * z); 
            if(mag == 0) return new Vec3(0, 1, 0); 
            return new Vec3(x / mag, y / mag, z / mag); 
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        rotationY += 0.02;

        // Cinematic zoom & pan adapted for the much taller, slender model
        double zoomCycle = Math.pow(Math.sin((rotationY + Math.PI) * 0.5), 2.0); 
        // Camera starts much further back (19.0) to fit the whole height
        double effectiveDistance = 6.0 - (zoomCycle * 4.0); 
        // Camera pans much higher up the Y-axis to reach the new head position
        double focusY = zoomCycle * 1.2 + 0.8;                     

        double tiltX = -0.15; 
        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // --- RENDER HARBOR SKY ---
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

        // --- BUILD THE PROPORTIONALLY ACCURATE LOW POLY MESH ---
        
        // 1. Pedestal (Taller and narrower. Height matches the real 1:1 statue/pedestal ratio)
        drawPrism(new Vec3(0, -4.5, 0), new Vec3(0, -1.5, 0), 1.0, 0.6, 8, STONE_BASE, STONE_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 2. Body / Robes (Drastically slimmed down radii and stretched Y coordinates)
        drawPrism(new Vec3(0, -1.5, 0), new Vec3(0, 0.5, 0), 0.55, 0.4, 7, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(new Vec3(0, 0.5, 0), new Vec3(0, 2.0, 0), 0.4, 0.28, 7, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 3. Head & Neck (Miniaturized to realistic human proportions)
        drawPrism(new Vec3(0, 2.0, 0), new Vec3(0, 2.15, 0), 0.1, 0.1, 5, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(new Vec3(0, 2.15, 0), new Vec3(0, 2.5, 0), 0.15, 0.16, 6, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 4. Right Arm (Raised higher, stretching up to y=2.8)
        Vec3 rShoulder = new Vec3(0.3, 1.8, 0);
        Vec3 rElbow = new Vec3(0.5, 2.2, 0.1);
        Vec3 rWrist = new Vec3(0.5, 2.8, 0.0);
        drawPrism(rShoulder, rElbow, 0.12, 0.09, 4, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(rElbow, rWrist, 0.09, 0.07, 4, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 5. Torch Handle & Emissive Low-Poly Flame (Shifted up to sit in the new higher hand)
        Vec3 torchBase = new Vec3(0.5, 2.8, 0.0);
        Vec3 torchMid = new Vec3(0.5, 2.95, 0.0);
        Vec3 flameMid = new Vec3(0.5, 3.15, 0.0);
        Vec3 flameTop = new Vec3(0.5, 3.4, 0.0);
        
        drawPrism(torchBase, torchMid, 0.05, 0.12, 5, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(torchMid, flameMid, 0.10, 0.20, 4, FLAME_GLOW, FLAME_GLOW, true, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(flameMid, flameTop, 0.20, 0.0, 4, FLAME_GLOW, FLAME_GLOW, true, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 6. Left Arm (Holding Tablet down at waist level)
        Vec3 lShoulder = new Vec3(-0.3, 1.8, 0);
        Vec3 lWrist = new Vec3(-0.4, 1.2, 0.2);
        drawPrism(lShoulder, lWrist, 0.12, 0.10, 4, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 7. Tabula Ansata (The Tablet Quad - repositioned closer to the slimmer body)
        Vec3 t1 = new Vec3(-0.6, 0.8, 0.2);
        Vec3 t2 = new Vec3(-0.25, 0.8, 0.35);
        Vec3 t3 = new Vec3(-0.25, 1.6, 0.35);
        Vec3 t4 = new Vec3(-0.6, 1.6, 0.2);
        drawQuad(t1, t2, t3, t4, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 8. The 7-Point Crown (Scaled down and shifted up)
        for(int i = 0; i < 7; i++) {
            double angle = Math.PI * 1.15 + i * (Math.PI * 0.7 / 6.0); 
            Vec3 root1 = new Vec3(Math.cos(angle - 0.1) * 0.17, 2.4, Math.sin(angle - 0.1) * 0.17);
            Vec3 root2 = new Vec3(Math.cos(angle + 0.1) * 0.17, 2.4, Math.sin(angle + 0.1) * 0.17);
            Vec3 tip = new Vec3(Math.cos(angle) * 0.35, 2.55, Math.sin(angle) * 0.35);
            drawTriangle(root1, root2, tip, COPPER_BASE, COPPER_SHD, false, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        }
    }

    // --- PROCEDURAL GEOMETRY BUILDERS ---
    
    private void drawPrism(Vec3 start, Vec3 end, double rStart, double rEnd, int sides, 
                           int[] baseCol, int[] shdCol, boolean isEmissive, 
                           double cosX, double sinX, double cosY, double sinY, 
                           double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        Vec3 dir = end.sub(start).normalize();
        Vec3 up = (Math.abs(dir.y) > 0.9) ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        up = right.cross(dir).normalize();
        
        Vec3[] basePts = new Vec3[sides];
        Vec3[] topPts = new Vec3[sides];
        
        for (int i = 0; i < sides; i++) {
            double angle = i * 2 * Math.PI / sides;
            double cx = Math.cos(angle);
            double cy = Math.sin(angle);
            
            basePts[i] = new Vec3(start.x + right.x * cx * rStart + up.x * cy * rStart,
                                  start.y + right.y * cx * rStart + up.y * cy * rStart,
                                  start.z + right.z * cx * rStart + up.z * cy * rStart);
            
            topPts[i] = new Vec3(end.x + right.x * cx * rEnd + up.x * cy * rEnd,
                                 end.y + right.y * cx * rEnd + up.y * cy * rEnd,
                                 end.z + right.z * cx * rEnd + up.z * cy * rEnd);
        }
        
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            drawQuad(basePts[i], basePts[next], topPts[next], topPts[i], baseCol, shdCol, isEmissive, 
                     cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        }
    }

    // Fixed Vec4 typo here in the signature
    private void drawQuad(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, int[] baseCol, int[] shdCol, boolean isEmissive, 
                          double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        drawTriangle(v1, v2, v3, baseCol, shdCol, isEmissive, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(v1, v3, v4, baseCol, shdCol, isEmissive, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
    }

    // --- BARYCENTRIC RASTERIZER (The core of the Low Poly look) ---
    private void drawTriangle(Vec3 v1, Vec3 v2, Vec3 v3, int[] baseCol, int[] shdCol, boolean isEmissive, 
                              double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        Vec3 edge1 = v2.sub(v1);
        Vec3 edge2 = v3.sub(v1);
        Vec3 normal = edge1.cross(edge2).normalize();

        double step = 0.025; 
        for (double u = 0; u <= 1.0; u += step) {
            for (double v = 0; v <= 1.0 - u; v += step) {
                double w = 1.0 - u - v;
                double px = u * v1.x + v * v2.x + w * v3.x;
                double py = u * v1.y + v * v2.y + w * v3.y;
                double pz = u * v1.z + v * v2.z + w * v3.z;
                
                renderProjectedPoint(px, py, pz, normal, baseCol, shdCol, isEmissive, 
                                     cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            }
        }
    }

    private void renderProjectedPoint(double x, double y, double z, Vec3 normal, int[] baseCol, int[] shdCol, boolean isEmissive, 
                                      double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        double xSpun = x * cosY + z * sinY;
        double zSpun = -x * sinY + z * cosY;
        double ySpun = y - focusY; 
        
        double rx = xSpun;
        double ry = ySpun * cosX - zSpun * sinX;
        double rz = ySpun * sinX + zSpun * cosX;
        
        double ooz = 1.0 / (rz + effectiveDistance);
        int xp = (int) (40 + 54 * ooz * rx * 1.95);
        int yp = (int) (11 - 25 * ooz * ry);
        
        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.00005) {
                zBuffer[index] = ooz;
                
                int r, g, b;
                if (isEmissive) {
                    double flicker = 0.85 + 0.15 * Math.sin(rotationY * 20.0 + y * 10.0);
                    r = Math.min(255, (int)(baseCol[0] * flicker));
                    g = Math.min(255, (int)(baseCol[1] * flicker));
                    b = Math.min(255, (int)(baseCol[2] * flicker));
                } else {
                    double nxSpun = normal.x * cosY + normal.z * sinY;
                    double lightIntensity = Math.max(0.0, nxSpun * 0.75 + normal.y * 0.25);
                    double ambient = 0.25;
                    double intensity = ambient + (1.0 - ambient) * lightIntensity;
                    
                    r = (int) (shdCol[0] * (1.0 - intensity) + baseCol[0] * intensity);
                    g = (int) (shdCol[1] * (1.0 - intensity) + baseCol[1] * intensity);
                    b = (int) (shdCol[2] * (1.0 - intensity) + baseCol[2] * intensity);
                }
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + CH_SOLID + RESET;
            }
        }
    }
}