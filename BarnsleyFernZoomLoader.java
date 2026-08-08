import java.util.Random;

public class BarnsleyFernZoomLoader extends Loader {
    private static final StatusStage[] FERN_STAGES = {
        new StatusStage(25, "Calibrating camera tracking flight paths:"),
        new StatusStage(50, "Streaming high-density fractal passes:"),
        new StatusStage(75, "Initiating low-altitude frond macro scan:"),
        new StatusStage(100, "Barnsley Fractal Core Stable!")
    };

    private final int width;
    private final int height;
    private final int totalSize;

    private final double[] leafDensity;
    private final Random rand = new Random();

    // Timeline clock variable driving our drone flight path path
    private double flightTimer = 0.0;

    // Moving camera targets that drift smoothly across dense regions
    private double camCenterX = 0.0;
    private double camCenterY = 5.0;
    private double currentScale = 1.0;

    // TrueColor Green color profiles mapping structure depths
    private static final String COLOR_DEEP   = "\u001B[38;2;34;90;20m";   
    private static final String COLOR_LEAF   = "\u001B[38;2;50;185;50m";  
    private static final String COLOR_BRIGHT = "\u001B[38;2;140;255;40m"; 

    public BarnsleyFernZoomLoader() {
        super(FERN_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
        this.totalSize = 1760;
        this.leafDensity = new double[totalSize];
    }

    @Override
    protected void initialize() {
        this.flightTimer = 2.0;
        this.camCenterX = 0.0;
        this.camCenterY = 5.0;
        this.currentScale = 1.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Advance the cinematic drone camera timeline loop
        // We use a slow master pace variable to cycle the system
        flightTimer += 0.006;
        
        // Convert the raw timer into a unified radian angle loop tracking 0 to 2*PI
        double loopAngle = flightTimer % (2.0 * Math.PI);

        // --- MATH RE-MAPPING FOR AN ABSOLUTELY SEAMLESS LOOP ---
        // 1. Dynamic Scale: Cosine wave naturally starts at maximum zoom (10.0), 
        // smoothly pulls back out to 1.0, and glides back to 10.0 with zero cuts.
        double cosWave = Math.cos(loopAngle); // Ranges -1 to +1
        currentScale = 5.5 + 4.5 * cosWave;  // Oscillates fluidly between 1.0x and 10.0x

        // 2. Dynamic Camera Center Positions:
        // We use synchronized harmonic frequencies to create a seamless Lissajous flight path.
        // Multiplying by (1.0 - cosWave) ensure that as the camera pulls out to full view (scale=1),
        // the panning offsets naturally damp down to exactly 0.0, centering the full fern perfectly.
        double cameraDampenFactor = (10.0 - currentScale) / 9.0; // 0 when unzoomed, 1 when close

        // The drone hovers and glides left/right, and up/down through the lush mid-fronds
        camCenterX = 0.35 * Math.sin(loopAngle * 2.0) * cameraDampenFactor;
        camCenterY = 5.0 + 1.25 * (1.0 + Math.cos(loopAngle)) * cameraDampenFactor;

        // Step 2: Clear fresh density history array frame updates
        for (int i = 0; i < totalSize; i++) {
            leafDensity[i] = 0.0;
        }

        // Maintain a rock-solid baseline of 18,000 points per frame step for dense coverage
        int activeIterations = 18000;

        double px = 0.0;
        double py = 0.0;

        // Run the Affine Transformation Iterations
        for (int iter = 0; iter < activeIterations; iter++) {
            double nextX, nextY;
            double r = rand.nextDouble();

            if (r < 0.01) {
                nextX = 0.0;
                nextY = 0.16 * py;
            } else if (r < 0.86) {
                nextX = 0.85 * px + 0.04 * py;
                nextY = -0.04 * px + 0.85 * py + 1.6;
            } else if (r < 0.93) {
                nextX = 0.20 * px - 0.26 * py;
                nextY = 0.23 * px + 0.22 * py + 1.6;
            } else {
                nextX = -0.15 * px + 0.28 * py;
                nextY = 0.26 * px + 0.24 * py + 0.44;
            }

            px = nextX;
            py = nextY;

            // Camera viewport positioning translation around our fluidly sliding drone path
            double dx = px - camCenterX;
            double dy = py - camCenterY;

            double baseWidthScale = 14.0;
            double baseHeightScale = 2.10;

            int screenX = (int) (40.0 + (dx * baseWidthScale * currentScale));
            int screenY = (int) (11.0 - (dy * baseHeightScale * currentScale));

            if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) {
                int idx = screenX + width * screenY;
                leafDensity[idx] = Math.min(1.0, leafDensity[idx] + 0.12);
            }
        }

        // Step 3: AUTO-GAIN LEVEL EXPOSURE COMPENSATION
        double brightnessGain = (currentScale * 0.28) + 0.4;

        // Step 4: RASTERIZE DENSITY ARRAYS & RAMP COLOR FIELDS
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int idx = x + rowOffset;
                
                double d = leafDensity[idx] * brightnessGain;

                if (d > 0.01) {
                    zBuffer[idx] = d;

                    char leafChar;
                    String leafColor;

                    if (d > 0.65) {
                        leafChar = '█'; 
                        leafColor = COLOR_BRIGHT;
                    } else if (d > 0.25) {
                        leafChar = '▓'; 
                        leafColor = COLOR_LEAF;
                    } else {
                        leafChar = '░'; 
                        leafColor = COLOR_DEEP;
                    }

                    outputBuffer[idx] = leafColor + leafChar + RESET;
                } else {
                    outputBuffer[idx] = " "; 
                }
            }
        }
    }
}