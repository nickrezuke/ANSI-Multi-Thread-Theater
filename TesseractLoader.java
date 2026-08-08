public class TesseractLoader extends Loader {
    private static final StatusStage[] TESSERACT_STAGES = {
        new StatusStage(25, "Constructing hyper-framework:"),
        new StatusStage(50, "Engaging X-W dimensional rotation:"),
        new StatusStage(75, "Locking vertex color identities:"),
        new StatusStage(100, "Hyper-Tesseract Online!")
    };

    private static final double[][] VERTICES = {
        {-1, -1, -1, -1}, { 1, -1, -1, -1}, {-1,  1, -1, -1}, { 1,  1, -1, -1},
        {-1, -1,  1, -1}, { 1, -1,  1, -1}, {-1,  1,  1, -1}, { 1,  1,  1, -1},
        {-1, -1, -1,  1}, { 1, -1, -1,  1}, {-1,  1, -1,  1}, { 1,  1, -1,  1},
        {-1, -1,  1,  1}, { 1, -1,  1,  1}, {-1,  1,  1,  1}, { 1,  1,  1,  1}
    };

    private static final int[][] VERTEX_COLORS = {
        {255, 80, 0},   {216, 60, 63},  {216, 60, 63},  {177, 40, 127}, 
        {216, 60, 63},  {177, 40, 127}, {177, 40, 127}, {138, 20, 191}, 
        {216, 60, 63},  {177, 40, 127}, {177, 40, 127}, {138, 20, 191}, 
        {177, 40, 127}, {138, 20, 191}, {138, 20, 191}, {100, 0, 255}   
    };

    private static final int[][] EDGES = {
        {0,1}, {2,3}, {4,5}, {6,7}, {8,9}, {10,11}, {12,13}, {14,15}, 
        {0,2}, {1,3}, {4,6}, {5,7}, {8,10}, {9,11}, {12,14}, {13,15}, 
        {0,4}, {1,5}, {2,6}, {3,7}, {8,12}, {9,13}, {10,14}, {11,15}, 
        {0,8}, {1,9}, {2,10}, {3,11}, {4,12}, {5,13}, {6,14}, {7,15}  
    };

    private double angleXW = 0; 
    private double angleXY = 0; 
    private double angleYZ = 0; 

    public TesseractLoader() {
        // This uses 80x22 specifically
        super(TESSERACT_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosXW = Math.cos(angleXW), sinXW = Math.sin(angleXW);
        double cosXY = Math.cos(angleXY), sinXY = Math.sin(angleXY);
        double cosYZ = Math.cos(angleYZ), sinYZ = Math.sin(angleYZ);

        double[][] projected = new double[16][3];

        for (int i = 0; i < 16; i++) {
            double x = VERTICES[i][0];
            double y = VERTICES[i][1];
            double z = VERTICES[i][2];
            double w = VERTICES[i][3];

            double x4D = x * cosXW - w * sinXW;
            double w4D = x * sinXW + w * cosXW;

            double distance4D = 2.2; 
            double factor4D = 1.0 / (distance4D - w4D * 0.55); 
            double x3D = x4D * factor4D;
            double y3D = y * factor4D;
            double z3D = z * factor4D;

            double x1 = x3D * cosXY - y3D * sinXY;
            double y1 = x3D * sinXY + y3D * cosXY;
            double y2 = y1 * cosYZ - z3D * sinYZ;
            double z2 = y1 * sinYZ + z3D * cosYZ;

            projected[i][0] = x1;
            projected[i][1] = y2;
            projected[i][2] = z2; 
        }

        // Expanded comprehensive shading ramp (Dark/Distant -> Bright/Close)
        String shadeRamp = ".,-~:;=!*#$@";

        for (int i = 0; i < EDGES.length; i++) {
            int vStart = EDGES[i][0];
            int vEnd = EDGES[i][1];

            double xStart = projected[vStart][0];
            double yStart = projected[vStart][1];
            double zStart = projected[vStart][2];

            double xEnd = projected[vEnd][0];
            double yEnd = projected[vEnd][1];
            double zEnd = projected[vEnd][2];

            // 1. Calculate directional vector of the edge line for lighting normal calculations
            double dx = xEnd - xStart;
            double dy = yEnd - yStart;
            double dz = zEnd - zStart;
            double edgeLength = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            // Standardize line normal vector directions
            if (edgeLength > 0) { dx /= edgeLength; dy /= edgeLength; dz /= edgeLength; }

            int rStart = VERTEX_COLORS[vStart][0];
            int gStart = VERTEX_COLORS[vStart][1];
            int bStart = VERTEX_COLORS[vStart][2];

            int rEnd = VERTEX_COLORS[vEnd][0];
            int gEnd = VERTEX_COLORS[vEnd][1];
            int bEnd = VERTEX_COLORS[vEnd][2];

            int steps = 32; // Boosted accuracy points along bars to prevent gapping
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                
                double currX = xStart + dx * (edgeLength * t);
                double currY = yStart + dy * (edgeLength * t);
                double currZ = zStart + dz * (edgeLength * t);

                double distance3D = 2.2;
                double ooz = 1.0 / (currZ + distance3D);

                int xp = (int) (40 + 75 * ooz * currX);
                int yp = (int) (11 - 32 * ooz * currY);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;
                    
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        // 2. High-Fidelity 3D Shading Engine:
                        // Imaginary light vector placed at Top-Left-Front (-1, 1, -1)
                        // Uses cross product simulation to illuminate edges orthogonal to light rays
                        double lightX = -0.577, lightY = 0.577, lightZ = -0.577;
                        
                        // Dot product tells us how parallel the line is to the light stream
                        double dotProduct = dx * lightX + dy * lightY + dz * lightZ;
                        
                        // Structural edges perpendicular to light cast bright sheen; parallel lines catch shadows
                        double luminance = Math.sqrt(1.0 - dotProduct * dotProduct);
                        
                        // Mix luminance with 3D camera depth proximity so closer items pop out stronger
                        double depthWeight = (ooz - 0.3) / 0.5; 
                        if (depthWeight < 0) depthWeight = 0;
                        if (depthWeight > 1) depthWeight = 1;
                        
                        double finalIntensity = (luminance * 0.6) + (depthWeight * 0.4);

                        // Map intensity cleanly to the 12-character index array bounds
                        int rampIndex = (int) (finalIntensity * (shadeRamp.length() - 1));
                        if (rampIndex < 0) rampIndex = 0;
                        if (rampIndex >= shadeRamp.length()) rampIndex = shadeRamp.length() - 1;

                        char edgeChar = shadeRamp.charAt(rampIndex);

                        // Line color calculation matching vertex coordinates
                        int r = (int) (rStart + (rEnd - rStart) * t);
                        int g = (int) (gStart + (gEnd - gStart) * t);
                        int b = (int) (bStart + (bEnd - bStart) * t);

                        String retroColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);

                        outputBuffer[index] = retroColor + edgeChar + RESET;
                    }
                }
            }
        }

        angleXW += 0.012; 
        angleXY += 0.015;
        angleYZ += 0.008;
    }
}
