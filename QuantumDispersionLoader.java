// TODO: Improve this its looking real terrible

public class QuantumDispersionLoader extends Loader {
    private static final StatusStage[] DISPERSION_STAGES = {
        new StatusStage(25, "Collapsing initial position vector:"),
        new StatusStage(50, "Solving time-dependent Schrödinger bounds:"),
        new StatusStage(75, "Calculating wave packet dispersion curves:"),
        new StatusStage(100, "Quantum Cloud Simulation Engaged!")
    };

    private double timeClock = 0.0;
    private final int width = 120;
    private final int height = 36;

    public QuantumDispersionLoader() {
        super(DISPERSION_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Advance time. When the cloud exits the chamber, it loops back seamlessly
        timeClock += 0.025;
        double loopDuration = 6.5;
        double t = timeClock % loopDuration;

        // --- QUANTUM PARAMETERS ---
        double initialWidth = 1.8;      // σ0: Initial tightness of the particle localization
        double dispersionRate = 1.3;    // Controls how fast the wave packet spreads out
        double forwardVelocity = 14.5;   // Drifts along the X-axis
        double waveFrequency = 4.5;      // Phase frequency of internal quantum oscillations

        double startX = 15.0;            // Starting position inside the chamber
        double currentMeanX = startX + forwardVelocity * t;

        // Dynamic width calculation matching the exact Schrödinger solution: σ(t)
        double currentWidth = Math.sqrt(initialWidth * initialWidth + Math.pow(dispersionRate * t, 2));

        // Textural rendering ramp ranging from deep vacuum space to maximum particle density core
        String shadingRamp = " .:-=+*#%@█";

        for (int y = 0; y < height; y++) {
            // Normalize vertical coordinate space relative to center axis row
            double centerY = height / 2.0;
            double dy = (y - centerY) * 1.5; // Aspect correction stretch factor

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                double dx = x - currentMeanX;

                // 1. GAUSSIAN ENVELOPE DENSITY CALCULATION
                // Computes the 2D distribution curve of the probability packet spatial structure
                double exponent = -((dx * dx) + (dy * dy)) / (2.0 * currentWidth * currentWidth);
                double baseDensity = Math.exp(exponent);

                // Peak amplitude drops as the area broadens to preserve total probability integration (100%)
                double amplitudeCompensation = initialWidth / currentWidth;
                double probabilityDensity = baseDensity * amplitudeCompensation;

                // 2. INTERNAL QUANTUM PHASE OSCILLATIONS
                // Simulates the underlying complex real/imaginary matter wave phases cycling inside the cloud
                double internalPhase = Math.cos(x * waveFrequency - t * 12.0);
                // Blend phase rings smoothly over the density profile
                double activeVisualIntensity = probabilityDensity * (0.65 + 0.35 * internalPhase);

                // Absolute safety clamps
                activeVisualIntensity = Math.max(0.0, Math.min(1.0, activeVisualIntensity));

                if (activeVisualIntensity > 0.005) {
                    zBuffer[index] = activeVisualIntensity;

                    int shadeIdx = (int) (activeVisualIntensity * (shadingRamp.length() - 1));
                    char renderChar = shadingRamp.charAt(shadeIdx);

                    // Laboratory Neon Phosphor Cyan/Teal Spectrum
                    // Brighter peak nodes experience a chromatic shift toward hot electric white
                    int r = (int) (10 + activeVisualIntensity * 230);
                    int g = (int) (120 + activeVisualIntensity * 135);
                    int b = (int) (180 + activeVisualIntensity * 75);

                    String cloudColor = String.format("\u001B[38;2;%d;%d;%dm", 
                        Math.max(0, Math.min(255, r)), 
                        Math.max(0, Math.min(255, g)), 
                        Math.max(0, Math.min(255, b))
                    );

                    outputBuffer[index] = cloudColor + renderChar + RESET;
                } else {
                    // Empty Particle Chamber Chamber Vacuum Trailing Grid Lines
                    if (x % 15 == 0 && y % 6 == 0) {
                        outputBuffer[index] = "\u001B[38;2;45;50;60m+\u001B[0m"; // Measurement coordinate nodes
                    } else if (y == (int)centerY && x % 3 == 0) {
                        outputBuffer[index] = "\u001B[38;2;35;40;45m-\u001B[0m"; // Flight axis trace line
                    } else {
                        outputBuffer[index] = " ";
                    }
                    zBuffer[index] = 0.0;
                }
            }
        }
    }
}
