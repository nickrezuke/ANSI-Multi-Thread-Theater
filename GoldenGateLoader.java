// TODO: Improve the camera work / zooming to get a better view of the bridge through the san fran fog

public class GoldenGateLoader extends Loader {
    private static final StatusStage[] BRIDGE_STAGES = {
            new StatusStage(20, "Sinking the concrete fender and south pier..."),
            new StatusStage(45, "Erecting the 746-foot Art Deco suspension towers..."),
            new StatusStage(70, "Spinning 80,000 miles of steel wire for the main cables..."),
            new StatusStage(100, "Painting the spans International Orange. Welcome to San Francisco!")
    };

    private static final char CH_SOLID = '\u2588';

    // The iconic International Orange and San Francisco Marine Layer
    private static final int[] INTL_ORANGE = { 220, 56, 33 }; 
    private static final int[] ORANGE_SHD  = { 100, 20, 10 };
    private static final int[] CABLE_DARK  = { 80, 25, 15 };
    private static final int[] WATER_BASE  = { 25, 60, 85 };
    
    // Atmospheric Scattering (Fog)
    private static final int[] SKY_TOP = { 110, 140, 180 };
    private static final int[] FOG_COL = { 190, 200, 210 };

    private double rotationY = 0.0;

    public GoldenGateLoader() {
        super(BRIDGE_STAGES, 80, 22);
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

        // --- CAMERA ORBIT ---
        double sideProfileFactor = Math.cos(rotationY * 2.0); 
        double effectiveDistance = 14.5 - (sideProfileFactor * 3.5); 
        double focusY = 1.0;                     
        
        // Tilt slightly upwards to appreciate the towers
        double tiltX = -0.15; 
        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // --- RENDER DYNAMIC MARINE LAYER (SKY & OCEAN) ---
        for (int yp = 0; yp < 22; yp++) {
            int sr, sg, sb;
            if (yp > 16) {
                // Ocean Water
                sr = WATER_BASE[0]; sg = WATER_BASE[1]; sb = WATER_BASE[2];
            } else if (yp > 8) {
                // Heavy Fog Layer Horizon
                double fogMix = (yp - 8) / 8.0;
                sr = (int) (SKY_TOP[0] * (1.0 - fogMix) + FOG_COL[0] * fogMix);
                sg = (int) (SKY_TOP[1] * (1.0 - fogMix) + FOG_COL[1] * fogMix);
                sb = (int) (SKY_TOP[2] * (1.0 - fogMix) + FOG_COL[2] * fogMix);
            } else {
                // Clear Upper Sky
                sr = SKY_TOP[0]; sg = SKY_TOP[1]; sb = SKY_TOP[2];
            }
            
            String skyColor = String.format("\u001B[38;2;%d;%d;%dm", sr, sg, sb);
            for (int xp = 0; xp < 80; xp++) {
                outputBuffer[xp + 80 * yp] = skyColor + " " + RESET;
            }
        }

        // --- BUILD ARCHITECTURAL GEOMETRY ---
        
        // 1. Main Deck & Roadway (Stretching massively across the Z axis)
        drawBox(0, 0, 0, 0.5, 0.05, 5.5, INTL_ORANGE, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 2. The Towers (Z = -2.5 and Z = +2.5)
        double[] towersZ = {-2.5, 2.5};
        for (double tz : towersZ) {
            // Legs
            drawBox(-0.35, 1.25, tz, 0.08, 1.45, 0.12, INTL_ORANGE, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            drawBox( 0.35, 1.25, tz, 0.08, 1.45, 0.12, INTL_ORANGE, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            
            // Art Deco Stepped Portals (Cross-bracing)
            drawBox(0.0, 2.5, tz, 0.35, 0.10, 0.12, INTL_ORANGE, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            drawBox(0.0, 1.6, tz, 0.35, 0.08, 0.10, INTL_ORANGE, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            drawBox(0.0, 0.6, tz, 0.35, 0.12, 0.10, INTL_ORANGE, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            drawBox(0.0,-0.1, tz, 0.35, 0.15, 0.14, INTL_ORANGE, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer); // Base pier
        }

        // 3. Mathematical Catenary Suspension Cables
        for(double z = -5.25; z <= 5.25; z += 0.25) {
            // Skip rendering cable nodes exactly inside the towers
            if (Math.abs(z) > 2.3 && Math.abs(z) < 2.7) continue;

            double cableY;
            if (z >= -2.5 && z <= 2.5) {
                // Inner Main Span: Opens upward, dips to Y=0.2 at Z=0
                cableY = 0.368 * (z * z) + 0.2; 
            } else if (z < -2.5) {
                // Outer Marin Anchorage Span
                double dz = z + 5.5; 
                cableY = 0.277 * (dz * dz);
            } else {
                // Outer SF Anchorage Span
                double dz = z - 5.5;
                cableY = 0.277 * (dz * dz);
            }

            // Draw thick main suspension cables
            drawBox(-0.43, cableY, z, 0.03, 0.03, 0.12, CABLE_DARK, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            drawBox( 0.43, cableY, z, 0.03, 0.03, 0.12, CABLE_DARK, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

            // Draw vertical suspender lines dropping to the deck (only above the deck)
            if (cableY > 0.15 && z > -5.0 && z < 5.0) {
                double suspenderHeight = cableY - 0.05;
                double cy = 0.05 + (suspenderHeight / 2.0);
                drawBox(-0.43, cy, z, 0.005, suspenderHeight / 2.0, 0.02, CABLE_DARK, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
                drawBox( 0.43, cy, z, 0.005, suspenderHeight / 2.0, 0.02, CABLE_DARK, ORANGE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            }
        }
    }

    // --- PROCEDURAL ARCHITECTURAL BUILDERS ---
    
    // Builds a 3D box based on a center point and half-extents (width, height, depth)
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

        // 6 Faces (Quads)
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

    // --- BARYCENTRIC RASTERIZER W/ VOLUMETRIC FOG ---
    private void drawTriangle(Vec3 v1, Vec3 v2, Vec3 v3, int[] baseCol, int[] shdCol,  
                              double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        // Calculate surface normal for lighting
        Vec3 edge1 = v2.sub(v1);
        Vec3 edge2 = v3.sub(v1);
        Vec3 normal = edge1.cross(edge2).normalize();

        double step = 0.05; // Slightly coarser step for architectural performance
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
                
                // 1. Calculate Sun Lighting Angle (Absolute value avoids winding order dark spots)
                double nxSpun = normal.x * cosY + normal.z * sinY;
                double nySpun = normal.y * cosX - normal.z * sinX;
                double lightIntensity = Math.abs(nxSpun * 0.4 + nySpun * 0.85);
                
                double ambient = 0.25;
                double intensity = ambient + (1.0 - ambient) * lightIntensity;
                
                int rawR = (int) (shdCol[0] * (1.0 - intensity) + baseCol[0] * intensity);
                int rawG = (int) (shdCol[1] * (1.0 - intensity) + baseCol[1] * intensity);
                int rawB = (int) (shdCol[2] * (1.0 - intensity) + baseCol[2] * intensity);
                
                // 2. VOLUMETRIC Z-DEPTH FOG CALCULATION
                // The further away a pixel is from the camera, the more it blends into FOG_COL
                double dist = rz + effectiveDistance;
                double fogFactor = Math.min(1.0, Math.max(0.0, (dist - 10.0) / 10.0)); 
                
                int finalR = (int) (rawR * (1.0 - fogFactor) + FOG_COL[0] * fogFactor);
                int finalG = (int) (rawG * (1.0 - fogFactor) + FOG_COL[1] * fogFactor);
                int finalB = (int) (rawB * (1.0 - fogFactor) + FOG_COL[2] * fogFactor);
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", finalR, finalG, finalB) + CH_SOLID + RESET;
            }
        }
    }
}