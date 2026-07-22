public class BlackHoleLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(30, "Generating singularity core:"),
        new StatusStage(60, "Collapsing localized space-time:"),
        new StatusStage(85, "Stabilizing event horizon:"),
        new StatusStage(100, "Gravitational Lock Achieved!")
    };

    private double time = 0;

    public BlackHoleLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;
        int cX = 40, cY = 11; // Singularity origin center

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int o = x + width * y;

                // Adjust horizontal scaling factor for text terminal geometry alignment
                double dx = (x - cX) / 2.0;
                double dy = y - cY;
                double r = Math.sqrt(dx * dx + dy * dy);

                if (r < 2.5) {
                    // Inside the Absolute Singularity Void
                    if (1.0 > zBuffer[o]) {
                        zBuffer[o] = 1.0;
                        outputBuffer[o] = " "; 
                    }
                    continue;
                }

                // Swirl distortion physics mechanics modeling
                double originalAngle = Math.atan2(dy, dx);
                // Inward twisting velocity formula
                double twistedAngle = originalAngle + (4.5 / (r + 0.1)) - time;

                // Create a noise-like cyclic pattern on the accretion disk
                double sample = Math.sin(6 * twistedAngle) * Math.cos(2 * r - time * 2);

                if (sample > 0.2) {
                    double depth = 1.0 / r;
                    if (depth > zBuffer[o]) {
                        zBuffer[o] = depth;

                        // Dynamic color indexing mapping moving outward from heat epicenter
                        String ansiColor;
                        char particleChar;

                        if (r < 5.0) {
                            ansiColor = "\u001B[38;5;231m";  // White hot interior border
                            particleChar = '@';
                        } else if (r < 9.0) {
                            ansiColor = "\u001B[38;5;208m";  // Incandescent orange disk
                            particleChar = '#';
                        } else if (r < 15.0) {
                            ansiColor = "\u001B[38;5;124m";  // Darker trailing red gas
                            particleChar = '*';
                        } else {
                            ansiColor = "\u001B[38;5;54m";   // Distant violet gravity waves
                            particleChar = '.';
                        }

                        outputBuffer[o] = ansiColor + particleChar + RESET;
                    }
                }
            }
        }
        time += 0.04;
    }
}
