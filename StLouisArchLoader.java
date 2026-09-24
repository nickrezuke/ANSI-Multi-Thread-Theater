// TODO: Improve the step size of the surrounding ground / improve landscape

public class StLouisArchLoader extends Loader {
    private static final StatusStage[] ARCH_STAGES = {
            new StatusStage(20, "Pouring the foundations at Gateway Arch National Park..."),
            new StatusStage(45, "Calculating the 630-foot weighted inverted catenary curve..."),
            new StatusStage(70, "Cladding the tapering triangular cross-sections in stainless steel..."),
            new StatusStage(100, "Securing the keystone. The Gateway to the West is complete!")
    };

    private static final char CH_SOLID = '\u2588';

    // Stainless Steel Palette with high specular potential
    private static final int[] STEEL_BASE = { 210, 215, 225 }; 
    private static final int[] STEEL_SHD  = { 85, 95, 110 };
    
    // Gateway Arch National Park (Grounds & Mississippi River)
    private static final int[] PARK_GRASS = { 65, 105, 55 };
    private static final int[] PARK_SHD   = { 40, 70, 35 };
    private static final int[] PATH_CONC  = { 150, 150, 145 };
    private static final int[] RIVER_BASE = { 35, 55, 80 };

    // Midwest Spring Sky
    private static final int[] SKY_TOP    = { 45, 115, 200 };
    private static final int[] SKY_BOTTOM = { 180, 210, 235 };

    private double rotationY = 0.0;

    public StLouisArchLoader() {
        super(ARCH_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
    }

    // --- 3D VECTOR MATH ENGINE ---
    static class Vec3 {
        double x, y, z;
        Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
        Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
        Vec3 scale(double s) { return new Vec3(x * s, y * s, z * s); }
        Vec3 cross(Vec3 o) { return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        Vec3 normalize() { 
            double mag = Math.sqrt(x * x + y * y + z * z); 
            if(mag == 0) return new Vec3(0, 1, 0); 
            return new Vec3(x / mag, y / mag, z / mag); 
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        rotationY += 0.016;

        // --- CAMERA ORBIT ---
        double sideProfileFactor = Math.cos(rotationY * 2.0); 
        
        // Back the camera up slightly to accommodate the 45-degree diagonal perspective
        double effectiveDistance = 12.0 - (sideProfileFactor * 2.0); 
        
        // Lower the focal point so the ground doesn't clip out of the bottom of the terminal
        double focusY = 1.5;                     
        
        // 45-degree downward tilt (~0.785 radians)
        double tiltX = -0.685 + 0.2 * sideProfileFactor; 
        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // --- RENDER MIDWEST SKY ---
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

        // --- 1. BUILD NATIONAL PARK GROUNDS & MISSISSIPPI RIVER ---
        
        // Main grassy park grounds extending around the legs
        drawBox(0, 0, -1.0, 6.0, 0.05, 3.5, PARK_GRASS, PARK_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        
        // Walkways mapping the layout of the park (connecting the legs)
        drawBox(0, 0.06, 0, 2.5, 0.02, 0.15, PATH_CONC, PATH_CONC, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawBox(0, 0.06, -1.5, 0.15, 0.02, 1.5, PATH_CONC, PATH_CONC, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        
        // The Mississippi River flowing directly behind the Arch (+Z axis)
        drawBox(0, -0.2, 4.5, 8.0, 0.15, 2.0, RIVER_BASE, PARK_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);


        // --- 2. BUILD THE INVERTED CATENARY ARCH ---
        
        int numSegments = 32;
        Vec3[] prevRing = null;
        
        for (int i = 0; i <= numSegments; i++) {
            // Span X from -2.5 to 2.5
            double x = -2.5 + 5.0 * ((double) i / numSegments);
            
            // Mathematical formula for the Arch: y = 5.0 - 0.974 * (cosh(x) - 1.0)
            double y = 5.0 - 0.974 * (Math.cosh(x) - 1.0);
            Vec3 center = new Vec3(x, y, 0);
            
            // Calculate Tangent and Normal vectors along the curve
            double dy_dx = -0.974 * Math.sinh(x);
            Vec3 tangent = new Vec3(1.0, dy_dx, 0.0).normalize();
            
            // Normal points outwards (UP and AWAY from the center of the arch)
            Vec3 normal = new Vec3(-tangent.y, tangent.x, 0.0).normalize();
            if (normal.y < 0) normal = normal.scale(-1.0); 
            
            Vec3 binormal = new Vec3(0, 0, 1);
            
            // Tapering width: Narrows smoothly from 0.45 at the base to 0.14 at the apex
            double width = 0.45 - 0.31 * (y / 5.0);
            double r = width / 2.0;

            // Generate the triangular cross-section (Equilateral, pointing outwards)
            Vec3[] ring = new Vec3[3];
            ring[0] = center.add(normal.scale(r));                               // Outer ridge (extrados)
            ring[1] = center.sub(normal.scale(r / 2.0)).add(binormal.scale(r));  // Inner face left (intrados)
            ring[2] = center.sub(normal.scale(r / 2.0)).sub(binormal.scale(r));  // Inner face right (intrados)

            // Stitch the current triangular ring to the previous one
            if (prevRing != null) {
                // Outer face 1
                drawQuad(prevRing[0], prevRing[1], ring[1], ring[0], STEEL_BASE, STEEL_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
                // Inner flat face facing the ground
                drawQuad(prevRing[1], prevRing[2], ring[2], ring[1], STEEL_BASE, STEEL_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
                // Outer face 2
                drawQuad(prevRing[2], prevRing[0], ring[0], ring[2], STEEL_BASE, STEEL_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            }
            prevRing = ring;
        }
    }

    // --- PROCEDURAL ARCHITECTURAL BUILDERS ---
    
    private void drawBox(double cx, double cy, double cz, double w, double h, double d, 
                         int[] baseCol, int[] shdCol,  
                         double cosX, double sinX, double cosY, double sinY, 
                         double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        Vec3[] v = new Vec3[8];
        v[0] = new Vec3(cx-w, cy-h, cz-d);
        v[1] = new Vec3(cx+w, cy-h, cz-d);
        v[2] = new Vec3(cx+w, cy+h, cz-d);
        v[3] = new Vec3(cx-w, cy+h, cz-d);
        v[4] = new Vec3(cx-w, cy-h, cz+d);
        v[5] = new Vec3(cx+w, cy-h, cz+d);
        v[6] = new Vec3(cx+w, cy+h, cz+d);
        v[7] = new Vec3(cx-w, cy+h, cz+d);

        drawQuad(v[0], v[1], v[2], v[3], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); // Front
        drawQuad(v[5], v[4], v[7], v[6], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); // Back
        drawQuad(v[4], v[0], v[3], v[7], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); // Left
        drawQuad(v[1], v[5], v[6], v[2], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); // Right
        drawQuad(v[3], v[2], v[6], v[7], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); // Top
        drawQuad(v[4], v[5], v[1], v[0], baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); // Bottom
    }

    private void drawQuad(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, int[] baseCol, int[] shdCol,  
                          double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        drawTriangle(v1, v2, v3, baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawTriangle(v1, v3, v4, baseCol, shdCol, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
    }

    // --- BARYCENTRIC RASTERIZER W/ SPECULAR HIGHLIGHTS ---
    private void drawTriangle(Vec3 v1, Vec3 v2, Vec3 v3, int[] baseCol, int[] shdCol,  
                              double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        Vec3 edge1 = v2.sub(v1);
        Vec3 edge2 = v3.sub(v1);
        Vec3 normal = edge1.cross(edge2).normalize();

        double step = 0.04; 
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
                
                // Diffuse Lighting
                double nxSpun = normal.x * cosY + normal.z * sinY;
                double nySpun = normal.y * cosX - normal.z * sinX;
                double lightIntensity = Math.abs(nxSpun * 0.5 + nySpun * 0.7);
                
                // Specular Highlight
                double specular = 0.0;
                if (baseCol == STEEL_BASE) {
                    specular = Math.pow(lightIntensity, 6.0) * 0.6;
                }
                
                double ambient = 0.25;
                double totalIntensity = Math.min(1.0, ambient + (1.0 - ambient) * lightIntensity + specular);
                
                int r = (int) (shdCol[0] * (1.0 - totalIntensity) + baseCol[0] * totalIntensity);
                int g = (int) (shdCol[1] * (1.0 - totalIntensity) + baseCol[1] * totalIntensity);
                int b = (int) (shdCol[2] * (1.0 - totalIntensity) + baseCol[2] * totalIntensity);
                
                // Prevent color blowout
                r = Math.min(255, r); g = Math.min(255, g); b = Math.min(255, b);
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + CH_SOLID + RESET;
            }
        }
    }
}