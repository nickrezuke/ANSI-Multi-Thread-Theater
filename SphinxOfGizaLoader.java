// TODO: Improve the details and the camera position / zooming

public class SphinxOfGizaLoader extends Loader {
    private static final StatusStage[] SPHINX_STAGES = {
            new StatusStage(25, "Carving the monolithic limestone bedrock:"),
            new StatusStage(50, "Shaping the massive lion body and extended paws:"),
            new StatusStage(75, "Sculpting the Nemes headdress and Pharaoh's visage:"),
            new StatusStage(100, "The Great Sphinx of Giza stands eternal!")
    };

    private static final char CH_SOLID = '\u2588';

    // Desert Limestone Palette
    private static final int[] STONE_BASE = { 220, 190, 150 };  // Sunbaked limestone
    private static final int[] STONE_SHD = { 130, 95, 65 };     // Terracotta shadow
    private static final int[] STONE_CAVITY = { 40, 25, 15 };   // Pitch black for the broken nose cavity

    // Egyptian Twilight Sky (Deep purple fading down to a dusty, vibrant orange horizon)
    private static final int[] SKY_TOP = { 20, 15, 60 };
    private static final int[] SKY_BOTTOM = { 240, 120, 40 };

    // Start at an establishing 3/4 angle
    private double rotationY = Math.PI / 4.0;

    public SphinxOfGizaLoader() {
        super(SPHINX_STAGES, 80, 22);
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
        rotationY += 0.015;

        // --- DYNAMIC ELLIPTICAL CAMERA ---
        // The Sphinx is extremely long along the Z-axis. 
        // We zoom OUT to distance 22.0 when viewing the side, and zoom IN to 13.5 when viewing the front face.
        double sideProfileFactor = Math.cos(rotationY * 2.0); 
        double effectiveDistance = 17.75 - (sideProfileFactor * 4.25); 

        // Pan down slightly to ground the massive paws
        double focusY = -0.5;                     

        // Slight downward tilt to view the back and paws clearly
        double tiltX = 0.15; 
        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // --- RENDER DESERT SKY ---
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

        // --- BUILD THE SPHINX MESH ---
        
        // 1. The Main Lion Body (Stretches deeply back into the negative Z axis)
        drawPrism(new Vec3(0, -1.0, 1.0), new Vec3(0, -1.0, -4.5), 1.6, 1.25, 7, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 2. The Hind Quarters & Folded Legs
        drawPrism(new Vec3(-1.0, -1.5, -3.0), new Vec3(-0.8, -2.2, -4.5), 0.6, 0.4, 5, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(new Vec3(1.0, -1.5, -3.0), new Vec3(0.8, -2.2, -4.5), 0.6, 0.4, 5, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 3. The Massive Extended Front Paws (Reaching out to positive Z)
        drawPrism(new Vec3(-0.85, -1.5, 1.0), new Vec3(-0.85, -2.2, 4.5), 0.6, 0.45, 5, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(new Vec3(0.85, -1.5, 1.0), new Vec3(0.85, -2.2, 4.5), 0.6, 0.45, 5, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 4. The Neck connecting the body to the head
        drawPrism(new Vec3(0, -0.5, 1.2), new Vec3(0, 0.8, 1.2), 0.8, 0.7, 6, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 5. The Nemes Headdress (Main bulbous back shape and cascading side lapets)
        drawPrism(new Vec3(0, 0.8, 1.0), new Vec3(0, 2.2, 1.0), 1.35, 1.05, 8, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(new Vec3(-1.2, 1.4, 1.0), new Vec3(-1.2, -0.5, 1.7), 0.4, 0.1, 4, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawPrism(new Vec3(1.2, 1.4, 1.0), new Vec3(1.2, -0.5, 1.7), 0.4, 0.1, 4, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 6. THE PHARAOH'S VISAGE (Bespoke Hand-Mapped Facial Mesh)
        Vec3 A = new Vec3(0, 2.0, 1.55);      // Forehead Center
        Vec3 B = new Vec3(-0.5, 1.8, 1.4);    // Temple L
        Vec3 C = new Vec3(0.5, 1.8, 1.4);     // Temple R
        Vec3 D = new Vec3(0, 1.5, 1.7);       // Nose Bridge (top of the nose)
        Vec3 E = new Vec3(-0.6, 1.0, 1.4);    // Cheek L
        Vec3 F = new Vec3(0.6, 1.0, 1.4);     // Cheek R
        Vec3 G = new Vec3(0, 1.1, 1.35);      // THE MISSING NOSE CRATER (Pushed deeply inwards on the Z axis)
        Vec3 H = new Vec3(0, 0.7, 1.65);      // Mouth / Pouting Lips
        Vec3 I = new Vec3(-0.5, 0.5, 1.4);    // Jaw L
        Vec3 J = new Vec3(0.5, 0.5, 1.4);     // Jaw R
        Vec3 K = new Vec3(0, 0.3, 1.6);       // Smooth Chin (missing beard)

        // Upper Face
        drawTriangle(A, B, D, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(A, D, C, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(B, E, D, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(C, D, F, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // The Missing Nose (Explicitly rendered with STONE_CAVITY to create a pitch black void in the center of the face)
        drawTriangle(D, E, G, STONE_CAVITY, STONE_CAVITY, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(D, G, F, STONE_CAVITY, STONE_CAVITY, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(E, H, G, STONE_CAVITY, STONE_CAVITY, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(F, G, H, STONE_CAVITY, STONE_CAVITY, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // Lower Face & Chin
        drawTriangle(E, I, H, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(F, H, J, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(I, K, H, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(J, H, K, STONE_BASE, STONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
    }

    // --- PROCEDURAL GEOMETRY BUILDERS ---
    
    private void drawPrism(Vec3 start, Vec3 end, double rStart, double rEnd, int sides, 
                           int[] baseCol, int[] shdCol,  
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
            drawQuad(basePts[i], basePts[next], topPts[next], topPts[i], baseCol, shdCol,  
                     cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        }
    }

    private void drawQuad(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, int[] baseCol, int[] shdCol,  
                          double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        drawTriangle(v1, v2, v3, baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(v1, v3, v4, baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
    }

    // --- BARYCENTRIC RASTERIZER ---
    private void drawTriangle(Vec3 v1, Vec3 v2, Vec3 v3, int[] baseCol, int[] shdCol,  
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
                
                renderProjectedPoint(px, py, pz, normal, baseCol, shdCol, 
                                     cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            }
        }
    }

    private void renderProjectedPoint(double x, double y, double z, Vec3 normal, int[] baseCol, int[] shdCol,  
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
                
                // Hot high-noon desert lighting (strong vertical bias)
                double nxSpun = normal.x * cosY + normal.z * sinY;
                double lightIntensity = Math.max(0.0, nxSpun * 0.4 + normal.y * 0.85);
                double ambient = 0.20;
                double intensity = ambient + (1.0 - ambient) * lightIntensity;
                
                int r = (int) (shdCol[0] * (1.0 - intensity) + baseCol[0] * intensity);
                int g = (int) (shdCol[1] * (1.0 - intensity) + baseCol[1] * intensity);
                int b = (int) (shdCol[2] * (1.0 - intensity) + baseCol[2] * intensity);
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + CH_SOLID + RESET;
            }
        }
    }
}