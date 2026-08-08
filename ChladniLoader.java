// TODO: Make sure these standing waves are correct for sand on a harmonic vibrating surface

public class ChladniLoader extends Loader {
    private static final StatusStage[] CHLADNI_STAGES = {
        new StatusStage(25, "Calibrating acoustic frequencies:"),
        new StatusStage(50, "Generating harmonic wave equations:"),
        new StatusStage(75, "Exciting metal plate resonance nodes:"),
        new StatusStage(100, "Chladni Resonance Field Stable!")
    };

    private double timeClock = 0.0;
    
    // Cyberpunk color palettes for the energetic standing wave field
    private static final String COLOR_PLATE  = "\u001B[38;2;30;40;65m";    // Cold Steel Plate Base
    private static final String COLOR_SAND   = "\u001B[38;2;0;255;180m";   // Glowing Cyan Sand Nodes
    private static final String COLOR_ENERGY = "\u001B[38;2;255;0;128m";  // Blazing Pink Antinodes

    public ChladniLoader() {
        // This uses 80x22 specifically
        super(CHLADNI_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        
        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Advance the master timeline clock
        timeClock += 0.012;

        int width = 80;
        int height = 22;

        // Step 2: DYNAMICALLY OSCILLATE MODAL WAVE INTEGERS (n, m)
        // We smoothly cycle the integers over time to force the sand to morph layouts
        double cycle = (timeClock % (2.0 * Math.PI));
        
        // Modal frequencies smoothly shift between pairs like (2,4), (3,5), and (5,3)
        double n = 3.5 + 1.5 * Math.sin(cycle * 0.7);
        double m = 4.0 + 2.0 * Math.cos(cycle * 0.5);

        // Symmetry blending factors morphing the pattern geometry shapes
        double a = Math.sin(cycle * 0.4);
        double b = Math.cos(cycle * 0.4);

        // Step 3: Evaluate the Chladni Wave Function across the 80x22 grid
        for (int y = 0; y < height; y++) {
            // Normalize grid coordinates to a clean [-1.0, 1.0] spatial matrix
            double normY = (y / (double) (height - 1)) * 2.0 - 1.0;

            for (int x = 0; x < width; x++) {
                double normX = (x / (double) (width - 1)) * 2.0 - 1.0;

                // Adjust for terminal character aspect ratio distortion (stretched wider)
                // We compress X space inside the trigonometry checks to force square parity
                double aspectCorrectedX = normX * 1.25;

                // 2D Square Plate Wave Equation Evaluation
                double wave = a * Math.cos(n * Math.PI * aspectCorrectedX) * Math.cos(m * Math.PI * normY)
                            + b * Math.cos(m * Math.PI * aspectCorrectedX) * Math.cos(n * Math.PI * normY);

                // Absolute displacement amplitude magnitude
                double amplitude = Math.abs(wave);

                int index = x + width * y;
                
                // Store displacement energy in zBuffer for external tracking safety checks
                zBuffer[index] = amplitude;

                // Step 4: MATERIAL LIGHTING AND TEXTURING DISPATCH
                if (amplitude < 0.15) {
                    // NODAL LINE ZONE: Kinetic energy is zero, sand particles cluster here!
                    // Cluer point proximity scales down character densities cleanly
                    String palette = "█▓▒░· ";
                    int shadeIdx = (int) ((amplitude / 0.15) * (palette.length() - 1));
                    shadeIdx = Math.max(0, Math.min(palette.length() - 1, shadeIdx));
                    char renderChar = palette.charAt(shadeIdx);

                    if (renderChar != ' ') {
                        outputBuffer[index] = COLOR_SAND + renderChar + RESET;
                    } else {
                        outputBuffer[index] = " ";
                    }
                } else if (amplitude > 1.2) {
                    // ANTINODE CORES: Blazing maximum structural displacement vibration zones
                    outputBuffer[index] = COLOR_ENERGY + "☼" + RESET;
                } else {
                    // PASSIVE PLATE SURFACE: Muted cold steel background grid texturing
                    char backgroundChar = (x % 4 == 0 && y % 2 == 0) ? '·' : ' ';
                    if (backgroundChar != ' ') {
                        outputBuffer[index] = COLOR_PLATE + backgroundChar + RESET;
                    } else {
                        outputBuffer[index] = " ";
                    }
                }
            }
        }
    }
}
