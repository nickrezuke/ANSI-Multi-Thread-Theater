import java.util.Arrays;

public class LorenzLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Seeding deterministic chaos:"),
        new StatusStage(55, "Calculating Lorenz vector equations:"),
        new StatusStage(80, "Mapping strange attractor nodes:"),
        new StatusStage(100, "Chaos Instability Contained!")
    };

    // System coordinates starting states
    private double x = 0.1, y = 0.0, z = 0.0;
    
    // Constant Lorenz Parameters (The standard chaotic butterfly values)
    private static final double SIGMA = 10.0;
    private static final double RHO = 28.0;
    private static final double BETA = 8.0 / 3.0;
    private static final double DT = 0.009; // Frame step slice velocity

    // Deep persistent path buffer coordinates tracking
    private final int[][] screenTrails = new int[22][80];

    public LorenzLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        for (int[] row : screenTrails) Arrays.fill(row, 0);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;

        // Dim existing trail records slightly to age out historical tracks
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (screenTrails[r][c] > 0) {
                    screenTrails[r][c] = Math.max(0, screenTrails[r][c] - 2);
                }
            }
        }

        // Run several math loops per frame step to ensure smooth trail rendering lines
        for (int step = 0; step < 8; step++) {
            double dx = SIGMA * (y - x) * DT;
            double dy = (x * (RHO - z) - y) * DT;
            double dz = (x * y - BETA * z) * DT;

            x += dx;
            y += dy;
            z += dz;

            // Project 3D Lorenz space coordinates down to 2D console buffer plane bounds
            // Shift offsets map centered coordinates scale matches 80x22 box bounds
            int projX = (int) (40 + (x * 1.65));
            int projY = (int) (22 - (z * 0.45)); // Scale mapping using Z up axis orientation

            if (projX >= 0 && projX < width && projY >= 0 && projY < height) {
                screenTrails[projY][projX] = 100; // Bright strike flash injection tag
            }
        }

        // Blit trail calculations down into the main output visual engine layer buffers
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int intensity = screenTrails[r][c];
                if (intensity > 0) {
                    int o = c + width * r;
                    double pseudoDepth = intensity / 100.0;

                    if (pseudoDepth > zBuffer[o]) {
                        zBuffer[o] = pseudoDepth;

                        // Graduating coloration shift down from Electric Ice blue into Deep Cobalt
                        String colorCode;
                        char trailChar;

                        if (intensity > 80) {
                            colorCode = "\u001B[38;5;81m";  // White-blue apex
                            trailChar = '@';
                        } else if (intensity > 50) {
                            colorCode = "\u001B[38;5;33m";  // Electric blue
                            trailChar = '*';
                        } else if (intensity > 20) {
                            colorCode = "\u001B[38;5;27m";  // Deep ocean sapphire
                            trailChar = '+';
                        } else {
                            colorCode = "\u001B[38;5;18m";  // Midnight blue fade
                            trailChar = '.';
                        }

                        outputBuffer[o] = colorCode + trailChar + RESET;
                    }
                }
            }
        }
    }
}
