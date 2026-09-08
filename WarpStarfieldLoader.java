public class WarpStarfieldLoader extends Loader {
    private static final StatusStage[] WARP_STAGES = {
        new StatusStage(15, "Spooling frame shift drives:"),
        new StatusStage(40, "Calculating astrometric trajectories:"),
        new StatusStage(65, "Bypassing navigational deflectors:"),
        new StatusStage(85, "Approaching terminal velocity:"),
        new StatusStage(100, "Warp Jump Successful!")
    };

    private static final int WIDTH = 130;
    private static final int HEIGHT = 32;
    private static final int NUM_STARS = 180; // High density for the grid

    // 3D Space boundaries
    private static final double MAX_Z = 100.0;
    private static final double MIN_Z = 1.0;
    private static final double BASE_SPEED = 0.25;

    // Star data arrays
    private final double[] x = new double[NUM_STARS];
    private final double[] y = new double[NUM_STARS];
    private final double[] z = new double[NUM_STARS];
    private final double[] speeds = new double[NUM_STARS];

    // Depth-based color palette (Dark Gray -> Light Gray -> White -> Blazing Cyan/White)
    private static final String Z_FAR   = "\u001B[38;5;236m"; // Very dark gray
    private static final String Z_MID   = "\u001B[38;5;244m"; // Mid gray
    private static final String Z_CLOSE = "\u001B[38;5;252m"; // Bright white
    private static final String Z_FACE  = "\u001B[38;5;159m"; // Pale cyan flash right as it passes

    public WarpStarfieldLoader() {
        super(WARP_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        for (int i = 0; i < NUM_STARS; i++) {
            // Initial spawn randomizes Z so the screen is full immediately
            resetStar(i, true);
        }
        setTargetFps(240); // For fast moving stars this actually makes it look MUCH better
    }

    private void resetStar(int i, boolean randomizeZ) {
        // Spread X and Y across a wide virtual plane so they originate from all angles
        x[i] = (Math.random() - 0.5) * 200; 
        y[i] = (Math.random() - 0.5) * 200;
        
        // When resetting during animation, spawn them at the back wall (MAX_Z)
        z[i] = randomizeZ ? (Math.random() * MAX_Z + MIN_Z) : MAX_Z;
        
        // Slight speed variations so they don't look like they are flying in flat sheets
        speeds[i] = BASE_SPEED + (Math.random() * 1.5);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int cx = WIDTH / 2;
        int cy = HEIGHT / 2;
        
        // Field of View multipliers. Y is roughly half of X because console fonts 
        // are typically twice as tall as they are wide.
        double fovX = 45.0; 
        double fovY = 22.0;

        for (int i = 0; i < NUM_STARS; i++) {
            // 1. Move star closer to camera
            z[i] -= speeds[i];

            // If the star passes behind the camera, respawn it far away
            if (z[i] <= MIN_Z) {
                resetStar(i, false);
                continue;
            }

            // 2. 3D-to-2D Perspective Projection Math (The magic sauce)
            int sx = (int) ((x[i] / z[i]) * fovX + cx);
            int sy = (int) ((y[i] / z[i]) * fovY + cy);

            // 3. Render if within console bounds
            if (sx >= 0 && sx < WIDTH && sy >= 0 && sy < HEIGHT) {
                int o = sx + WIDTH * sy;
                
                // Invert Z for depth buffering (so closer stars overwrite farther ones)
                double depthZ = MAX_Z - z[i];

                if (depthZ > zBuffer[o]) {
                    zBuffer[o] = depthZ;

                    char glyph;
                    String color;

                    // 4. Scale size and brightness based on proximity to the camera
                    if (z[i] > 70) {
                        glyph = '.';
                        color = Z_FAR;
                    } else if (z[i] > 40) {
                        glyph = '-'; // Gives a slight motion blur effect
                        color = Z_MID;
                    } else if (z[i] > 15) {
                        glyph = '*';
                        color = Z_CLOSE;
                    } else {
                        glyph = '@'; // Massive right before hitting the camera
                        color = Z_FACE;
                    }

                    outputBuffer[o] = color + glyph + RESET;
                }
            } else {
                // Optimization: If a star flies off the left/right/top/bottom edges 
                // of the screen, don't wait for its Z to hit 0. Recycle it immediately 
                // to maintain high star density on screen.
                if (z[i] < MAX_Z - 10) {
                    resetStar(i, false);
                }
            }
        }
    }
}