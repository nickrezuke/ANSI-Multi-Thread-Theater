// TODO: FIX THIS LMAO

public class LeaningTowerOfPisaLoader extends Loader {
    private static final StatusStage[] PISA_STAGES = {
            new StatusStage(20, "Laying the unstable foundation in the soft Pisan soil..."),
            new StatusStage(45, "Raising the lower marble tiers as the tilt begins..."),
            new StatusStage(70, "Curving the upper colonnades to compensate for the lean..."),
            new StatusStage(100, "Crowning the belfry. The Square of Miracles is complete!")
    };

    private static final char CH_SOLID = '\u2588';

    // Italian White Marble Palette
    private static final int[] MARBLE_BASE  = { 245, 240, 230 }; 
    private static final int[] MARBLE_SHD   = { 170, 160, 150 };
    private static final int[] CORE_SHADOW  = { 60, 55, 50 };     // Deep shadow behind the pillars
    
    // Piazza dei Miracoli
    private static final int[] PARK_GRASS   = { 90, 140, 60 };
    private static final int[] PARK_SHD     = { 50, 80, 35 };

    // Tuscan Sky
    private static final int[] SKY_TOP      = { 30, 110, 200 };
    private static final int[] SKY_BOTTOM   = { 140, 190, 245 };

    private double rotationY = 0.0;

    public LeaningTowerOfPisaLoader() {
        super(PISA_STAGES, 80, 22);
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
        rotationY += 0.019;

        // --- CAMERA ORBIT ---
        double effectiveDistance = 9.8; 
        double focusY = 2.8; // Focus halfway up the tower                     
        
        double tiltX = -0.495; 
        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // --- RENDER TUSCAN SKY ---
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

        // --- 1. BUILD PIAZZA DEI MIRACOLI (GROUNDS) ---
        drawBox(0, 0, 0, 4.0, 0.05, 4.0, PARK_GRASS, PARK_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);



        // --- 2. PROCEDURAL GENERATION OF THE TOWER ---
        
        double y = 0.0;
        // The famous tilt (~5 degrees represented mathematically)
        double leanAngle = -0.12; 
        
        // BASE TIER (Solid, wider base)
        drawTiltedCylinder(1.1, y, y + 0.8, 20, MARBLE_BASE, MARBLE_SHD, leanAngle, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        y += 0.8;

        // THE 6 LOGGIA TIERS (The Colonnades)
        for (int tier = 0; tier < 6; tier++) {
            // Cornice (Horizontal protruding ring separating floors)
            drawTiltedCylinder(1.12, y, y + 0.1, 20, MARBLE_BASE, MARBLE_SHD, leanAngle, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            
            // Inner structural core (shadowed)
            drawTiltedCylinder(0.85, y + 0.1, y + 0.7, 16, CORE_SHADOW, CORE_SHADOW, leanAngle, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            
            // Generate the exterior marble pillars
            int numPillars = 18;
            for (int p = 0; p < numPillars; p++) {
                double angle = p * (2 * Math.PI / numPillars);
                double px = 1.0 * Math.cos(angle);
                double pz = 1.0 * Math.sin(angle);
                double pw = 0.1; // Pillar width
                
                // Construct a 2D quad facing outwards
                Vec3 p1 = new Vec3(px - pw*Math.sin(angle), y + 0.1, pz + pw*Math.cos(angle));
                Vec3 p2 = new Vec3(px + pw*Math.sin(angle), y + 0.1, pz - pw*Math.cos(angle));
                Vec3 p3 = new Vec3(p2.x, y + 0.7, p2.z);
                Vec3 p4 = new Vec3(p1.x, y + 0.7, p1.z);
                
                p1 = tilt(p1, leanAngle); p2 = tilt(p2, leanAngle);
                p3 = tilt(p3, leanAngle); p4 = tilt(p4, leanAngle);
                
                drawQuad(p1, p2, p3, p4, MARBLE_BASE, MARBLE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            }
            y += 0.7;
        }

        // BELFRY BASE (Top ring)
        drawTiltedCylinder(1.05, y, y + 0.15, 20, MARBLE_BASE, MARBLE_SHD, leanAngle, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        y += 0.15;

        // BELFRY CORE (Narrower top section where the bells live)
        drawTiltedCylinder(0.7, y, y + 0.7, 16, CORE_SHADOW, CORE_SHADOW, leanAngle, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        
        // Belfry Pillars
        for (int p = 0; p < 12; p++) {
            double angle = p * (2 * Math.PI / 12);
            double px = 0.85 * Math.cos(angle);
            double pz = 0.85 * Math.sin(angle);
            double pw = 0.08;
            
            Vec3 p1 = new Vec3(px - pw*Math.sin(angle), y, pz + pw*Math.cos(angle));
            Vec3 p2 = new Vec3(px + pw*Math.sin(angle), y, pz - pw*Math.cos(angle));
            Vec3 p3 = new Vec3(p2.x, y + 0.7, p2.z);
            Vec3 p4 = new Vec3(p1.x, y + 0.7, p1.z);
            
            p1 = tilt(p1, leanAngle); p2 = tilt(p2, leanAngle);
            p3 = tilt(p3, leanAngle); p4 = tilt(p4, leanAngle);
            
            drawQuad(p1, p2, p3, p4, MARBLE_BASE, MARBLE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        }
        y += 0.7;
        
        // BELFRY ROOF (Final cap)
        drawTiltedCylinder(0.9, y, y + 0.1, 16, MARBLE_BASE, MARBLE_SHD, leanAngle, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
    }

    // --- PROCEDURAL ARCHITECTURAL BUILDERS ---
    
    // Applies a Z-axis rotation matrix to simulate the structural lean
    private Vec3 tilt(Vec3 p, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // The tower pivots from its foundation (0,0,0)
        return new Vec3(p.x * cos - p.y * sin, p.x * sin + p.y * cos, p.z);
    }

    private void drawTiltedCylinder(double r, double yB, double yT, int segs, int[] baseCol, int[] shdCol, double leanAngle,
                                    double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        double step = 2 * Math.PI / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;

            Vec3 v1 = new Vec3(r * Math.cos(a1), yB, r * Math.sin(a1));
            Vec3 v2 = new Vec3(r * Math.cos(a2), yB, r * Math.sin(a2));
            Vec3 v3 = new Vec3(r * Math.cos(a2), yT, r * Math.sin(a2));
            Vec3 v4 = new Vec3(r * Math.cos(a1), yT, r * Math.sin(a1));

            v1 = tilt(v1, leanAngle);
            v2 = tilt(v2, leanAngle);
            v3 = tilt(v3, leanAngle);
            v4 = tilt(v4, leanAngle);

            drawQuad(v1, v2, v3, v4, baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        }
    }

    private void drawBox(double cx, double cy, double cz, double w, double h, double d, 
                         int[] baseCol, int[] shdCol,  
                         double cosX, double sinX, double cosY, double sinY, 
                         double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        Vec3[] v = new Vec3[8];
        v[0] = new Vec3(cx-w, cy-h, cz-d); v[1] = new Vec3(cx+w, cy-h, cz-d);
        v[2] = new Vec3(cx+w, cy+h, cz-d); v[3] = new Vec3(cx-w, cy+h, cz-d);
        v[4] = new Vec3(cx-w, cy-h, cz+d); v[5] = new Vec3(cx+w, cy-h, cz+d);
        v[6] = new Vec3(cx+w, cy+h, cz+d); v[7] = new Vec3(cx-w, cy+h, cz+d);

        drawQuad(v[0], v[1], v[2], v[3], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); 
        drawQuad(v[5], v[4], v[7], v[6], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); 
        drawQuad(v[4], v[0], v[3], v[7], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); 
        drawQuad(v[1], v[5], v[6], v[2], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); 
        drawQuad(v[3], v[2], v[6], v[7], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); 
    }

    private void drawQuad(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, int[] baseCol, int[] shdCol,  
                          double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        drawTriangle(v1, v2, v3, baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(v1, v3, v4, baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
    }

    private void drawTriangle(Vec3 v1, Vec3 v2, Vec3 v3, int[] baseCol, int[] shdCol,  
                              double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        // Normal is inherently calculated post-tilt, ensuring lighting matches the lean
        Vec3 edge1 = v2.sub(v1);
        Vec3 edge2 = v3.sub(v1);
        Vec3 normal = edge1.cross(edge2).normalize();

        double step = 0.05; // Slightly lower resolution step to save rendering time on heavy geometry
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
                
                double nxSpun = normal.x * cosY + normal.z * sinY;
                double nySpun = normal.y * cosX - normal.z * sinX;
                double lightIntensity = Math.abs(nxSpun * 0.5 + nySpun * 0.7);
                
                // Soft specular highlight for polished marble (much less extreme than the steel arch)
                double specular = 0.0;
                if (baseCol == MARBLE_BASE) {
                    specular = Math.pow(lightIntensity, 3.0) * 0.2;
                }
                
                double ambient = 0.3;
                double totalIntensity = Math.min(1.0, ambient + (1.0 - ambient) * lightIntensity + specular);
                
                int r = (int) (shdCol[0] * (1.0 - totalIntensity) + baseCol[0] * totalIntensity);
                int g = (int) (shdCol[1] * (1.0 - totalIntensity) + baseCol[1] * totalIntensity);
                int b = (int) (shdCol[2] * (1.0 - totalIntensity) + baseCol[2] * totalIntensity);
                
                r = Math.min(255, r); g = Math.min(255, g); b = Math.min(255, b);
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + CH_SOLID + RESET;
            }
        }
    }
}