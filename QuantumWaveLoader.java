// TODO: This looks so bad lmao make this more scientifically accurate

public class QuantumWaveLoader extends Loader {
    private static final StatusStage[] QUANTUM_STAGES = {
        new StatusStage(25, "Starting off in both states:"),
        new StatusStage(50, "Estimating Uncertainty:"),
        new StatusStage(75, "Quantum Tunel Possible:"),
        new StatusStage(100, "Quantum Entanglement Stable!")
};
    private final int width;
    private final int height;
    private final int size;

    // We need 2D grids to track the wave state across time steps
    private double[] currentWave;
    private double[] pastWave;
    private double[] nextWave;
    private boolean[] boundaries; // Stores where "walls" reflect waves

    private double timeStep = 0.0;
    
    // Character ramp to translate wave heights into visual intensity profiles
    private static final String DENSITY = " .:-=+*#%@";

    public QuantumWaveLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
        this.size = width * height;
    }

    public QuantumWaveLoader() {
        super(QUANTUM_STAGES);
        this.width = window_width;
        this.height = window_height;
        this.size = width * height;
    }

    @Override
    protected void initialize() {
        this.currentWave = new double[size];
        this.pastWave = new double[size];
        this.nextWave = new double[size];
        this.boundaries = new boolean[size];

        // Let's draw an elliptical/circular "Corral" boundary wall into our matrix
        int centerX = width / 2;
        int centerY = height / 2;
        // Adjust radii to account for the terminal's non-square font aspect ratio
        double radiusX = width * 0.4;
        double radiusY = height * 0.42;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                
                // Ellipse equation: (dx/rx)^2 + (dy/ry)^2 = 1
                double dx = (x - centerX) / radiusX;
                double dy = (y - centerY) / radiusY;
                
                // If a cell sits right on or outside our corral perimeter, make it a wall
                if ((dx * dx + dy * dy) >= 1.0) {
                    boundaries[idx] = true;
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeStep += 0.2;

        // 1. Inject an oscillating energy driver (the electron source) in the center
        int centerX = width / 2;
        int centerY = height / 2;
        int centerIdx = centerY * width + centerX;
        
        // Simulates a continuous harmonic wave source pumping energy into the system
        currentWave[centerIdx] = Math.sin(timeStep) * 4.0;

        // 2. Compute the discrete wave equation over the internal canvas grid
        double waveSpeedSq = 0.15; // c^2 parameter controlling how fast ripples flow
        double damping = 0.993;    // Prevents kinetic energy from exploding out of bounds

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;

                // Skip calculations if the cell is part of the hard boundary wall
                if (boundaries[idx]) continue;

                // Grab heights of the 4 cardinal neighbors (Laplacian)
                double current = currentWave[idx];
                double left    = currentWave[idx - 1];
                double right   = currentWave[idx + 1];
                double up      = currentWave[idx - width];
                double down    = currentWave[idx + width];

                // FDTD calculation sequence
                double laplacian = (left + right + up + down) - (4.0 * current);
                double nextVal = (2.0 * current) - pastWave[idx] + (waveSpeedSq * laplacian);

                nextWave[idx] = nextVal * damping;
            }
        }

        // 3. Resolve boundary conditions (Hard reflection: walls push wave energy back)
        for (int i = 0; i < size; i++) {
            if (boundaries[i]) {
                nextWave[i] = 0.0; // Force energy to zero at wall locations
            }
        }

        // 4. Cycle state buffers forward for the upcoming loop frame
        System.arraycopy(currentWave, 0, pastWave, 0, size);
        System.arraycopy(nextWave, 0, currentWave, 0, size);

        // 5. Project the results down into the framework's output canvas buffers
        for (int i = 0; i < size; i++) {
            if (boundaries[i]) {
                // Render the physical layout perimeter wall explicitly
                outputBuffer[i] = WHITE + "█" + RESET;
                zBuffer[i] = 999.0; // High value to ensure it clips properly
            } else {
                // Map the absolute value of the displacement amplitude to the character ramp
                double amplitude = Math.abs(currentWave[i]);
                
                // Store the height value in the zBuffer so depth checks still compute logically
                zBuffer[i] = amplitude;

                // Quantize amplitude to fit the length of the string array
                int densityIdx = (int) (amplitude * 3.5);
                densityIdx = Math.max(0, Math.min(DENSITY.length() - 1, densityIdx));
                char waveChar = DENSITY.charAt(densityIdx);

                // Use ANSI Green text for positive waves, default color for valleys to create depth contrast
                if (currentWave[i] > 0.02 && waveChar != ' ') {
                    outputBuffer[i] = GREEN + waveChar + RESET;
                } else {
                    outputBuffer[i] = String.valueOf(waveChar);
                }
            }
        }
    }
}
