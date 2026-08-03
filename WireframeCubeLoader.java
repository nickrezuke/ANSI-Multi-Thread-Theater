public class WireframeCubeLoader extends Loader {
    private static final StatusStage[] NESTED_STAGES = {
        new StatusStage(25, "Calibrating infinite scale arrays:"),
        new StatusStage(50, "Generating nested geometric shells:"),
        new StatusStage(75, "Syncing asynchronous axis arrays:"),
        new StatusStage(100, "Cyber Grid Matrix Stabilized!")
    };

    private static final char CH_EDGE = '\u2022'; // • Wireframe Edge
    private static final char CH_VERTEX = '\u2588'; // █ Node Vertex

    private static final int[][] BASE_COLORS = {
        {0, 255, 150},   // Mint Green
        {255, 0, 180},   // Hot Pink
        {0, 180, 255},   // Electric Cyan
        {240, 230, 20}   // Cyber Yellow
    };

    private static final int TOTAL_CUBES = 12;
    private final double[] rotX = new double[TOTAL_CUBES];
    private final double[] rotY = new double[TOTAL_CUBES];

    private static final double[] SPEED_X = { 
         0.012, -0.022,  0.035, -0.015,
         0.012, -0.022,  0.035, -0.015,
         0.012, -0.022,  0.035, -0.015
    };
    private static final double[] SPEED_Y = { 
         0.019,  0.015, -0.028,  0.021,
         0.019,  0.015, -0.028,  0.021,
         0.019,  0.015, -0.028,  0.021
    };

    private static final double[][] BASE_VERTICES = {
        {-1.0, -1.0, -1.0}, { 1.0, -1.0, -1.0}, { 1.0,  1.0, -1.0}, {-1.0,  1.0, -1.0},
        {-1.0, -1.0,  1.0}, { 1.0, -1.0,  1.0}, { 1.0,  1.0,  1.0}, {-1.0,  1.0,  1.0}
    };

    private static final int[][] EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private double zoomAccumulator = 0.0;
    private static final double ZOOM_SPEED = 0.007; 
    private static final double NEST_RATIO = 0.5;  

    public WireframeCubeLoader() {
        super(NESTED_STAGES);
    }

    @Override
    protected void initialize() {
        zoomAccumulator = 0.0;
        for (int i = 0; i < TOTAL_CUBES; i++) {
            rotX[i] = (i % 4) * 0.4;
            rotY[i] = (i % 4) * 0.25;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        zoomAccumulator += ZOOM_SPEED;

        if (zoomAccumulator >= 4.0) {
            zoomAccumulator -= 4.0;
            for (int i = 0; i < TOTAL_CUBES; i++) {
                int sourceIdx = (i + 4) % TOTAL_CUBES;
                rotX[i] = rotX[sourceIdx];
                rotY[i] = rotY[sourceIdx];
            }
        }

        for (int layer = 0; layer < TOTAL_CUBES; layer++) {
            double currentExponent = layer - zoomAccumulator;
            double scale = 12.0 * Math.pow(NEST_RATIO, currentExponent);

            if (scale > 60.0 || scale < 0.1) continue;

            double cosX = Math.cos(rotX[layer]), sinX = Math.sin(rotX[layer]);
            double cosY = Math.cos(rotY[layer]), sinY = Math.sin(rotY[layer]);

            int[] rgb = BASE_COLORS[layer % 4];
            // FIX: Explicitly pass individual array indices into the String format parameters
            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", rgb[0], rgb[1], rgb[2]);

            // Precompute 2D Screen Projections and Depth values for all 8 vertices of this cube
            int[] projX = new int[8];
            int[] projY = new int[8];
            double[] depths = new double[8];

            for (int v = 0; v < 8; v++) {
                double x = BASE_VERTICES[v][0] * scale;
                double y = BASE_VERTICES[v][1] * scale;
                double z = BASE_VERTICES[v][2] * scale;

                double y1 = y * cosX - z * sinX;
                double z1 = y * sinX + z * cosX;
                double rx = x * cosY + z1 * sinY;
                double ry = y1;
                double rz = -x * sinY + z1 * cosY;

                projX[v] = (int) (40 + 2.3 * rx);
                projY[v] = (int) (11 + ry);
                depths[v] = rz;
            }

            // Draw all 12 edges using an unbroken 2D digital differential analyzer rasterizer
            for (int[] edge : EDGES) {
                int idxStart = edge[0];
                int idxEnd = edge[1];

                int x0 = projX[idxStart];
                int y0 = projY[idxStart];
                int x1 = projX[idxEnd];
                int y1 = projY[idxEnd];

                double zStart = depths[idxStart];
                double zEnd = depths[idxEnd];

                int dx = Math.abs(x1 - x0);
                int dy = Math.abs(y1 - y0);
                int steps = Math.max(dx, dy);

                if (steps == 0) {
                    drawPoint(outputBuffer, zBuffer, x0, y0, zStart, true, colorCode);
                    continue;
                }

                double xInc = (double) (x1 - x0) / steps;
                double yInc = (double) (y1 - y0) / steps;
                double zInc = (zEnd - zStart) / steps;

                double cx = x0;
                double cy = y0;
                double cz = zStart;

                for (int s = 0; s <= steps; s++) {
                    int renderX = (int) Math.round(cx);
                    int renderY = (int) Math.round(cy);
                    
                    boolean isVertex = (s < 2 || s > (steps - 2));

                    drawPoint(outputBuffer, zBuffer, renderX, renderY, cz, isVertex, colorCode);

                    cx += xInc;
                    cy += yInc;
                    cz += zInc;
                }
            }

            rotX[layer] += SPEED_X[layer];
            rotY[layer] += SPEED_Y[layer];
        }
    }

    private void drawPoint(String[] outputBuffer, double[] zBuffer, int x, int y, double depth, boolean isVertex, String colorCode) {
        if (x >= 0 && x < 80 && y >= 0 && y < 22) {
            int index = x + 80 * y;
            double testingDepth = isVertex ? (depth + 100.0) : depth;

            if (testingDepth > zBuffer[index]) {
                zBuffer[index] = depth;
                char renderChar = isVertex ? CH_VERTEX : CH_EDGE;
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
        }
    }
}
