import java.util.Arrays;

public class RippleTankAutomatonLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Filling hydrodynamic pressure basin:"),
        new StatusStage(55, "Computing discrete height-field velocities:"),
        new StatusStage(85, "Projecting wave phase interference:"),
        new StatusStage(100, "Fluid Dynamics Tank Stabilized!")
    };

    private static final char[] SHADE_RAMP = { ' ', '.', '-', '░', '▒', '▓', '█' };
    
    private static final int SIM_W = 160;
    private static final int SIM_H = 44;
    private static final int OUT_W = 80;
    private static final int OUT_H = 22;
    private static final int WALL_X = 40;

    private double[][] currentHeight = new double[SIM_H][SIM_W];
    private double[][] previousHeight = new double[SIM_H][SIM_W];
    private double[][] nextHeight = new double[SIM_H][SIM_W];
    private final boolean[][] obstacles = new boolean[SIM_H][SIM_W];
    private double[][] waveIntensity = new double[SIM_H][SIM_W];
    private double timeClock = 0.0;

    public RippleTankAutomatonLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        timeClock = 0.0;
        for (int y = 0; y < SIM_H; y++) {
            Arrays.fill(currentHeight[y], 0.0);
            Arrays.fill(previousHeight[y], 0.0);
            Arrays.fill(waveIntensity[y], 0.0);
            Arrays.fill(obstacles[y], false);
        }

        // Build the central barrier wall at WALL_X with two precise aperture gaps
        for (int y = 0; y < SIM_H; y++) {
            if (y >= 14 && y <= 16) continue; // Slit 1 Aperture Channel
            if (y >= 27 && y <= 29) continue; // Slit 2 Aperture Channel
            obstacles[y][WALL_X] = true;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.25; // Continuous time stepping

        // --- STEP 1: WAVE ENGINE OSCILLATOR ---
        double waveFrequency = 1.35;
        double waveAmplitude = 3.0; 
        double currentPulse = Math.sin(timeClock * waveFrequency) * waveAmplitude;
        
        // Dynamic continuous wave source positioned on the left side
        currentHeight[SIM_H / 2][4] = currentPulse;
        currentHeight[SIM_H / 2 + 1][4] = currentPulse;

        // --- STEP 2: WAVE FIELD PROPAGATION ---
        double propagationForce = 0.45;
        double dampingFactor = 0.996; 

        for (int y = 1; y < SIM_H - 1; y++) {
            for (int x = 1; x < SIM_W - 1; x++) {
                if (obstacles[y][x]) {
                    nextHeight[y][x] = 0.0;
                    continue;
                }

                // 4-Way Discrete Wave Difference Equation
                double neighborSum = currentHeight[y-1][x] + currentHeight[y+1][x] + currentHeight[y][x-1] + currentHeight[y][x+1];
                double acceleration = (neighborSum / 4.0) - currentHeight[y][x];
                
                nextHeight[y][x] = (2.0 * currentHeight[y][x] - previousHeight[y][x] + acceleration * propagationForce) * dampingFactor;
            }
        }

        // --- STEP 3: EDGE ABSORPTION FIELDS ---
        for (int x = 0; x < SIM_W; x++) {
            nextHeight[0][x] *= dampingFactor;
            nextHeight[SIM_H - 1][x] *= dampingFactor;
        }
        for (int y = 0; y < SIM_H; y++) {
            nextHeight[y][0] *= dampingFactor; 
            nextHeight[y][SIM_W - 1] *= dampingFactor; 
        }

        // --- STEP 4: INTENSITY CALCULATION ---
        for (int y = 0; y < SIM_H; y++) {
            for (int x = 0; x < SIM_W; x++) {
                double amp = nextHeight[y][x];
                // Accumulate mean square energy over time
                waveIntensity[y][x] = (waveIntensity[y][x] * 0.95) + (amp * amp * 0.05);
            }
        }

        // Buffer Address Reference Pointer Swapping
        double[][] temp = previousHeight;
        previousHeight = currentHeight;
        currentHeight = nextHeight;
        nextHeight = temp;

        // --- STEP 5: UNIFIED OVERLAY RENDERING WINDOW ---
        for (int outY = 0; outY < OUT_H; outY++) {
            int simY1 = outY * 2;
            int simY2 = simY1 + 1;
            
            for (int outX = 0; outX < OUT_W; outX++) {
                int simX1 = outX * 2;
                int simX2 = simX1 + 1;
                int offset = outX + OUT_W * outY;

                // Detect obstacles inside the 2x2 downsample grid block
                boolean isObstacle = obstacles[simY1][simX1] || obstacles[simY1][simX2] || obstacles[simY2][simX1] || obstacles[simY2][simX2];
                if (isObstacle) {
                    outputBuffer[offset] = "\u001B[38;5;244m█" + RESET;
                    continue;
                }

                // Downsample height fields
                double avgHeight = (currentHeight[simY1][simX1] + currentHeight[simY1][simX2] + currentHeight[simY2][simX1] + currentHeight[simY2][simX2]) / 4.0;
                double avgIntensity = (waveIntensity[simY1][simX1] + waveIntensity[simY1][simX2] + waveIntensity[simY2][simX1] + waveIntensity[simY2][simX2]) / 4.0;

                // --- GAIN EQUALIZER: DISTANCE COMPENSATED LOGARITHMIC GAIN ---
                // Calculates distance from the slit plane center to normalize the energy levels.
                double deltaX = (outX * 2) - WALL_X;
                double distanceFactor = (deltaX > 0) ? Math.sqrt(deltaX * 0.45) : 1.0;
                if (distanceFactor < 1.0) distanceFactor = 1.0;

                // Boost the local raw displacement height dynamically to offset radial energy dilution
                double amplifiedHeight = avgHeight * distanceFactor * 1.5;

                // Render dynamic cyan & magenta waves across the entire container canvas
                int shadeIdx = (int) ((amplifiedHeight + 1.2) / 2.4 * (SHADE_RAMP.length - 1));
                if (shadeIdx < 0) shadeIdx = 0; 
                else if (shadeIdx > SHADE_RAMP.length - 1) shadeIdx = SHADE_RAMP.length - 1;
                
                char glyph = SHADE_RAMP[shadeIdx];
                String colorCode;

                if (Math.abs(amplifiedHeight) < 0.15) {
                    colorCode = "\u001B[38;5;234m"; // Dark background dust spot for zero lines
                    glyph = '·';
                } else {
                    colorCode = (amplifiedHeight > 0.0) ? "\u001B[38;2;85;195;235m" : "\u001B[38;2;235;65;180m"; // Cyan Crests vs Pink Troughs
                }

                // --- CHROMATIC FIELD INTERFERENCE COMBINATOR ---
                // On the right side of the wall, look for structural high-intensity fringe boundaries
                if (outX > WALL_X / 2) {
                    // Logarithmic tracking maps intensity bands without blowing out near the openings
                    double logIntensity = Math.log1p(avgIntensity * distanceFactor * 6.0);
                    
                    if (logIntensity > 0.45) {
                        // Blend the underlying wave glyph with a high-contrast lime green background layer.
                        // This lets you look straight at active ripple patterns passing directly through the green bands.
                        colorCode = "\u001B[48;2;40;110;50m" + colorCode; 
                    }
                }

                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }
}
