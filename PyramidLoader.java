public class PyramidLoader extends Loader {
    private static final StatusStage[] PYRAMID_STAGES = {
        new StatusStage(40, "Polishing chrome facets:"),
        new StatusStage(80, "Igniting spectrum sweep:"),
        new StatusStage(100, "Reflective Core Online!")
    };

    private static final char[] SHADE_CHARS = { ':', '=', '+', '*', 'X', '%', '&', '$', '#', '@' };
    
    private double pyramidAngle = 0.0;
    private double lightAngleX = 0.0;
    private double lightAngleY = 0.0;
    
    // Independent color cycle timeline variable (cycles from 0.0 to 1.0)
    private double colorHue = 0.0;

    // Pure reflective silver/chrome material base
    private static final int BASE_R = 70;
    private static final int BASE_G = 73;
    private static final int BASE_B = 78;

    // Fixed overhead camera downward pitch angle (~28 degrees)
    private static final double CAMERA_TILT = 0.50; 
    private final double cosTilt = Math.cos(CAMERA_TILT);
    private final double sinTilt = Math.sin(CAMERA_TILT);


    public PyramidLoader() {
        super(PYRAMID_STAGES);
    }

    @Override
    protected void initialize() {
        // No random picking anymore; everything runs on real-time loops!
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosPyramid = Math.cos(pyramidAngle);
        double sinPyramid = Math.sin(pyramidAngle);

        // Precompute dual-axis donut sweep math for the light location
        double cosLX = Math.cos(lightAngleX), sinLX = Math.sin(lightAngleX);
        double cosLY = Math.cos(lightAngleY), sinLY = Math.sin(lightAngleY);

        double localLX = 0.0;
        double localLY = 0.0;
        double localLZ = 1.8;

        double ly1 = localLY * cosLX - localLZ * sinLX;
        double lz1 = localLY * sinLX + localLZ * cosLX;
        double lx1 = localLX;

        double lightX = lx1 * cosLY + lz1 * sinLY;
        double lightY = ly1;
        double lightZ = -lx1 * sinLY + lz1 * cosLY;

        // Calculate the current RGB values of the moving light source using HSV math
        int[] currentLightRGB = hueToRGB(colorHue);
        int lightR = currentLightRGB[0];
        int lightG = currentLightRGB[1];
        int lightB = currentLightRGB[2];

        // FIXED: Re-aligned vertex coordinates to standard 3D camera space bounds
        // In this setup: -Z is closer to camera (Front), +Z is deeper (Back)
        double[][] vertices = {
            {  0.0, -1.0,  0.0 }, // Vertex 0: Apex (Top Point)
            { -1.0,  1.0, -1.0 }, // Vertex 1: Front-Left Corner
            {  1.0,  1.0, -1.0 }, // Vertex 2: Front-Right Corner
            {  1.0,  1.0,  1.0 }, // Vertex 3: Back-Right Corner
            { -1.0,  1.0,  1.0 }  // Vertex 4: Back-Left Corner
        };

        // FIXED: Winding configurations updated so all 5 face normals point outward 
        int[][] faces = {
            { 0, 1, 2 },    // Face 0: Front Triangle Side
            { 0, 2, 3 },    // Face 1: Right Triangle Side
            { 0, 3, 4 },    // Face 2: Back Triangle Side
            { 0, 4, 1 },    // Face 3: Left Triangle Side
            { 1, 4, 3, 2 }  // Face 4: Square Base (Bottom Plane)
        };

        for (int i = 0; i < faces.length; i++) {
            int[] faceVertices = faces[i];

            double[] v0 = vertices[faceVertices[0]];
            double[] v1 = vertices[faceVertices[1]];
            double[] v2 = vertices[faceVertices[2]];

            double edge1x = v1[0] - v0[0];
            double edge1y = v1[1] - v0[1];
            double edge1z = v1[2] - v0[2];

            double edge2x = v2[0] - v0[0];
            double edge2y = v2[1] - v0[1];
            double edge2z = v2[2] - v0[2];

            double nx = edge1y * edge2z - edge1z * edge2y;
            double ny = edge1z * edge2x - edge1x * edge2z;
            double nz = edge1x * edge2y - edge1y * edge2x;

            double nMag = Math.sqrt(nx*nx + ny*ny + nz*nz);
            nx /= nMag; ny /= nMag; nz /= nMag;

            double rNx = nx * cosPyramid + nz * sinPyramid;
            double rNy1 = ny;
            double rNz1 = -nx * sinPyramid + nz * cosPyramid;

            double rNy = rNy1 * cosTilt - rNz1 * sinTilt;
            double rNz = rNy1 * sinTilt + rNz1 * cosTilt;

            if (rNz > 0) {
                continue;
            }

            if (i < 4) { 
                for (double u = 0; u <= 1.0; u += 0.015) {
                    for (double v = 0; v <= 1.0 - u; v += 0.015) {
                        double w = 1.0 - u - v;

                        double x = u * v0[0] + v * v1[0] + w * v2[0];
                        double y = u * v0[1] + v * v1[1] + w * v2[1];
                        double z = u * v0[2] + v * v1[2] + w * v2[2];

                        calculateLightingAndPlot(x, y, z, cosPyramid, sinPyramid, rNx, rNy, rNz, lightX, lightY, lightZ, lightR, lightG, lightB, outputBuffer, zBuffer);
                    }
                }
            } else { 
                for (double u = 0; u <= 1.0; u += 0.025) {
                    for (double v = 0; v <= 1.0; v += 0.025) {
                        double x = -1.0 + 2.0 * u;
                        double y = 1.0;
                        double z = -1.0 + 2.0 * v;

                        calculateLightingAndPlot(x, y, z, cosPyramid, sinPyramid, rNx, rNy, rNz, lightX, lightY, lightZ, lightR, lightG, lightB, outputBuffer, zBuffer);
                    }
                }
            }
        }

        // Pacing updates
        pyramidAngle += 0.012; 
        lightAngleX += 0.014;  
        lightAngleY += 0.018;  
        
        colorHue += 0.0014;
        if (colorHue > 1.0) {
            colorHue -= 1.0;
        }
    }

    private void calculateLightingAndPlot(double x, double y, double z, double cosY, double sinY, 
                                           double rNx, double rNy, double rNz, 
                                           double lightX, double lightY, double lightZ, 
                                           int lightR, int lightG, int lightB,
                                           String[] outputBuffer, double[] zBuffer) {
        double rx = x * cosY + z * sinY;
        double ry1 = y;
        double rz1 = -x * sinY + z * cosY;

        double ry = ry1 * cosTilt - rz1 * sinTilt;
        double rz = ry1 * sinTilt + rz1 * cosTilt;

        double toLightX = lightX - rx;
        double toLightY = lightY - ry;
        double toLightZ = lightZ - rz;
        double distToLight = Math.sqrt(toLightX*toLightX + toLightY*toLightY + toLightZ*toLightZ);
        toLightX /= distToLight; toLightY /= distToLight; toLightZ /= distToLight;

        double diffuse = rNx * toLightX + rNy * toLightY + rNz * toLightZ;
        if (diffuse < 0) diffuse = 0;

        double refZ = 2 * diffuse * rNz - toLightZ;
        double specular = -refZ; 
        if (specular < 0) specular = 0;
        specular = Math.pow(specular, 8); 

        double ambientWeight = 0.20;
        double diffuseWeight = 0.40;
        double specularWeight = 0.65; 

        int r = (int) (BASE_R * ambientWeight + lightR * (diffuseWeight * diffuse + specularWeight * specular));
        int g = (int) (BASE_G * ambientWeight + lightG * (diffuseWeight * diffuse + specularWeight * specular));
        int b = (int) (BASE_B * ambientWeight + lightB * (diffuseWeight * diffuse + specularWeight * specular));

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        double totalIntensity = 0.3 * diffuse + 0.7 * specular;
        int shadeIndex = (int) (totalIntensity * 9.9);
        shadeIndex = Math.max(0, Math.min(9, shadeIndex));
        char renderChar = SHADE_CHARS[shadeIndex];

        double distanceToCamera = 2.9;
        double ooz = 1.0 / (rz + distanceToCamera);

        int xp = (int) (40 + 44 * ooz * rx);
        int yp = (int) (9 + 22 * ooz * ry); 

        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.0001) {
                zBuffer[index] = ooz;
                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
        }
    }

    private int[] hueToRGB(double hue) {
        double h = hue * 6.0; 
        int i = (int) Math.floor(h);
        double f = h - i;
        
        int pv = 0;
        int qv = (int) (255 * (1.0 - f));
        int tv = (int) (255 * f);

        switch (i % 6) {
            case 0: return new int[]{ 255, tv, pv };  
            case 1: return new int[]{ qv, 255, pv };  
            case 2: return new int[]{ pv, 255, tv };  
            case 3: return new int[]{ pv, qv, 255 };  
            case 4: return new int[]{ tv, pv, 255 };  
            case 5: return new int[]{ 255, pv, qv };  
            default: return new int[]{ 255, 255, 255 };
        }
    }
}
