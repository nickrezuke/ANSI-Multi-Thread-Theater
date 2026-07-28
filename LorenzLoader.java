public class LorenzLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Seeding deterministic chaos:"),
        new StatusStage(55, "Calculating Lorenz vector equations:"),
        new StatusStage(80, "Mapping strange attractor nodes:"),
        new StatusStage(100, "Chaos Instability Contained!")
    };

    // Current calculation head node
    private double lx = 0.1, ly = 0.0, lz = 0.0;
    
    // Constant Lorenz Parameters
    private static final double SIGMA = 10.0;
    private static final double RHO = 28.0;
    private static final double BETA = 8.0 / 3.0;
    private static final double DT = 0.0075;

    // --- 3D HISTORICAL VECTOR RING BUFFER ---
    // Instead of locking pixels to the screen, we cache raw 3D vectors
    private static final int MAX_POINTS = 600; // Total length of the trailing thread
    private final double[][] historyXyz = new double[MAX_POINTS][3];
    private int historyIndex = 0;
    private int activePointsCount = 0;

    private double angle = 0.0;

    public LorenzLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        activePointsCount = 0;
        historyIndex = 0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;

        // 1. Generate new 3D math points and push them into our raw geometry queue
        for (int step = 0; step < 6; step++) {
            double dx = SIGMA * (ly - lx) * DT;
            double dy = (lx * (RHO - lz) - ly) * DT;
            double dz = (lx * ly - BETA * lz) * DT;
            lx += dx; ly += dy; lz += dz;

            // Normalize and scale around the butterfly's local origin axis
            historyXyz[historyIndex][0] = lx * 0.07;
            historyXyz[historyIndex][1] = ly * 0.07;
            historyXyz[historyIndex][2] = (lz - 25.0) * 0.07;

            // Increment cyclic ring index
            historyIndex = (historyIndex + 1) % MAX_POINTS;
            if (activePointsCount < MAX_POINTS) activePointsCount++;
        }

        // 2. Precompute 3D Rotation Angles for the global frame tumble
        double rX = angle * 0.20;
        double rY = angle * 0.35;
        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);

        // 3. Process the ENTIRE 3D historical path from tail to head
        // This guarantees that the entire butterfly rotates together as a single solid body!
        for (int i = 0; i < activePointsCount; i++) {
            // Read chronologically from oldest tail segment to newest leading apex point
            int lookupIdx = (historyIndex - activePointsCount + i + MAX_POINTS) % MAX_POINTS;
            
            double cx = historyXyz[lookupIdx][0];
            double cy = historyXyz[lookupIdx][1];
            double cz = historyXyz[lookupIdx][2];

            // Apply 3D Rotation Matrix directly to the cached spatial coordinate
            double y1 = cy * cosX - cz * sinX;
            double z1 = cy * sinX + cz * cosX;
            double x2 = cx * cosY + z1 * sinY;
            double z2 = -cx * sinY + z1 * cosY;

            // Perspective Projection (z2 represents the fully rotated depth axis)
            double distanceToCamera = 3.5;
            double ooz = 1.0 / (z2 + distanceToCamera);
            
            int projX = (int) (40 + 40 * ooz * 1.8 * x2);
            int projY = (int) (11 - 18 * ooz * y1);

            if (projX >= 0 && projX < width && projY >= 0 && projY < height) {
                int o = projX + width * projY;

                // Depth test ensures overlapping lines closer to the screen render on top
                if (ooz > zBuffer[o]) {
                    zBuffer[o] = ooz;

                    // Calculate age percentage (0.0 = oldest tail point, 1.0 = newest head tip)
                    double ageFactor = (double) i / activePointsCount;

                    String colorCode;
                    char trailChar;

                    if (ageFactor > 0.94) {
                        colorCode = "\u001B[38;5;255m"; trailChar = '@'; // Leading pointer
                    } else if (ageFactor > 0.75) {
                        colorCode = "\u001B[38;5;81m";  trailChar = '*'; // Neon Cyan
                    } else if (ageFactor > 0.45) {
                        colorCode = "\u001B[38;5;201m"; trailChar = '+'; // Hot Pink
                    } else if (ageFactor > 0.15) {
                        colorCode = "\u001B[38;5;93m";  trailChar = '.'; // Violet
                    } else {
                        colorCode = "\u001B[38;5;54m";  trailChar = ','; // Whisp Fade
                    }

                    outputBuffer[o] = colorCode + trailChar + RESET;
                }
            }
        }
        
        // Slow, majestic drift speed modifier
        angle += 0.015; 
    }
}
