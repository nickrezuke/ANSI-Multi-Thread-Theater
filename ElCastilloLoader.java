// TODO: Improve the camerawork and refine the building geometry (looks like flickering TV Static)

public class ElCastilloLoader extends Loader {
    private static final StatusStage[] CHICHEN_STAGES = {
            new StatusStage(25, "Laying the massive limestone foundation in the Yucatan jungle..."),
            new StatusStage(50, "Stacking the nine terraced platforms of El Castillo..."),
            new StatusStage(75, "Carving the four central stairways and feathered serpent balustrades..."),
            new StatusStage(100, "Ascending to the summit temple of Kukulcan. El Castillo stands eternal!")
    };

    private static final char CH_SOLID = '\u2588';

    // Mayan Sunbaked Limestone Palette
    private static final int[] LIMESTONE_BASE = { 215, 190, 145 }; 
    private static final int[] LIMESTONE_SHD  = { 115, 85, 55 };
    private static final int[] STAIR_BASE     = { 185, 160, 120 };
    private static final int[] TEMPLE_DARK    = { 40, 30, 20 };     // Pitch interior for the top temple door

    // Yucatan Jungle Sunset Sky (Deep tropical turquoise fading to golden amber horizon)
    private static final int[] SKY_TOP    = { 25, 75, 95 };
    private static final int[] SKY_BOTTOM = { 235, 140, 50 };

    private double rotationY = Math.PI / 4.0; // Start at a 3/4 angle to show both stairways and terraces

    public ElCastilloLoader() {
        super(CHICHEN_STAGES, 80, 22);
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
        rotationY += 0.018;

        // --- CAMERA ORBIT ---
        double effectiveDistance = 5.0; 
        double focusY = 0.65;                     
        
        // Pitch down slightly to clearly view the top temple and terraced steps
        double tiltX = 0.28; 
        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // --- RENDER YUCATAN SUNSET SKY ---
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

        // --- BUILD EL CASTILLO GEOMETRY ---
        
        // 1. The 9 Terraced Platforms
        int numTerraces = 9;
        double baseSize = 3.8;
        double topSize  = 1.4;
        double totalHeight = 1.8;
        double levelHeight = totalHeight / numTerraces;

        for (int i = 0; i < numTerraces; i++) {
            double progress = (double) i / (numTerraces - 1);
            double currentHalfSize = (baseSize * (1.0 - progress) + topSize * progress) / 2.0;
            double cy = -1.0 + (i * levelHeight) + (levelHeight / 2.0);

            drawBox(0.0, cy, 0.0, currentHalfSize, levelHeight / 2.0, currentHalfSize, 
                    LIMESTONE_BASE, LIMESTONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        }

        // 2. Top Temple Shrine & Roof Cap
        double templeBaseY = -1.0 + totalHeight;
        double templeHeight = 0.45;
        drawBox(0.0, templeBaseY + (templeHeight / 2.0), 0.0, 0.6, templeHeight / 2.0, 0.6, 
                LIMESTONE_BASE, LIMESTONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // Temple Roof Cap
        drawBox(0.0, templeBaseY + templeHeight + 0.08, 0.0, 0.68, 0.08, 0.68, 
                LIMESTONE_BASE, LIMESTONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // Temple Doorways (Pitch black recesses on all 4 sides of top temple)
        double doorW = 0.18, doorH = 0.22;
        drawBox( 0.0, templeBaseY + doorH,  0.58, doorW, doorH, 0.05, TEMPLE_DARK, TEMPLE_DARK, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawBox( 0.0, templeBaseY + doorH, -0.58, doorW, doorH, 0.05, TEMPLE_DARK, TEMPLE_DARK, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawBox( 0.58, templeBaseY + doorH,  0.0, 0.05, doorH, doorW, TEMPLE_DARK, TEMPLE_DARK, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
        drawBox(-0.58, templeBaseY + doorH,  0.0, 0.05, doorH, doorW, TEMPLE_DARK, TEMPLE_DARK, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);

        // 3. Four Central Staircases (North, South, East, West)
        double stairWidth = 0.35;
        double stairSteps = 18; // Subdivided sloped steps

        for (int step = 0; step < stairSteps; step++) {
            double t = (double) step / stairSteps;
            double sy = -1.0 + (t * totalHeight);
            double sz = (baseSize / 2.0) * (1.0 - t) + (topSize / 2.0) * t + 0.08;

            // North Stairway (+Z)
            drawBox(0.0, sy, sz, stairWidth, levelHeight * 0.6, 0.08, STAIR_BASE, LIMESTONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            // South Stairway (-Z)
            drawBox(0.0, sy, -sz, stairWidth, levelHeight * 0.6, 0.08, STAIR_BASE, LIMESTONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            // East Stairway (+X)
            drawBox(sz, sy, 0.0, 0.08, levelHeight * 0.6, stairWidth, STAIR_BASE, LIMESTONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
            // West Stairway (-X)
            drawBox(-sz, sy, 0.0, 0.08, levelHeight * 0.6, stairWidth, STAIR_BASE, LIMESTONE_SHD, cosX, sinX, cosY, sinY, effectiveDistance, focusY, outputBuffer, zBuffer);
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

    // --- BARYCENTRIC RASTERIZER ---
    private void drawTriangle(Vec3 v1, Vec3 v2, Vec3 v3, int[] baseCol, int[] shdCol,  
                              double cosX, double sinX, double cosY, double sinY, double effectiveDistance, double focusY, String[] outputBuffer, double[] zBuffer) {
        
        Vec3 edge1 = v2.sub(v1);
        Vec3 edge2 = v3.sub(v1);
        Vec3 normal = edge1.cross(edge2).normalize();

        double step = 0.045; 
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
                
                // High Mesoamerican overhead sunlight
                double nxSpun = normal.x * cosY + normal.z * sinY;
                double nySpun = normal.y * cosX - normal.z * sinX;
                double lightIntensity = Math.abs(nxSpun * 0.35 + nySpun * 0.85);
                
                double ambient = 0.22;
                double intensity = ambient + (1.0 - ambient) * lightIntensity;
                
                int r = (int) (shdCol[0] * (1.0 - intensity) + baseCol[0] * intensity);
                int g = (int) (shdCol[1] * (1.0 - intensity) + baseCol[1] * intensity);
                int b = (int) (shdCol[2] * (1.0 - intensity) + baseCol[2] * intensity);
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + CH_SOLID + RESET;
            }
        }
    }
}