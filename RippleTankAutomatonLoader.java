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

        // Build the left-shifted barrier wall at WALL_X
        for (int y = 0; y < SIM_H; y++) {
            if (y >= 13 && y <= 15) continue;  // Slit 1
            if (y >= 28 && y <= 30) continue;  // Slit 2
            obstacles[y][WALL_X] = true;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // TODO: Figure out why wave doesn't pass through both slits
        timeClock += 0.16;

        // 1. STANDARD WAVE DRIVER
        double waveFrequency = 1.65; 
        double waveAmplitude = 1.8; 
        double currentPulse = Math.sin(timeClock * waveFrequency) * waveAmplitude;
        
        // FIX 1: Shifted the source to column 2 so it sits cleanly inside the simulation arena
        currentHeight[SIM_H / 2][2] = currentPulse;
        currentHeight[SIM_H / 2 + 1][2] = currentPulse;

        // 2. STABLE WAVE FIELD INTEGRATION
        double propagationForce = 1.0; 
        double dampingFactor = 0.990; 

        for (int y = 1; y < SIM_H - 1; y++) {
            for (int x = 1; x < SIM_W - 1; x++) {
                
                if (obstacles[y][x]) {
                    nextHeight[y][x] = 0.0;
                    continue;
                }

                double neighborSum = currentHeight[y-1][x] + 
                                    currentHeight[y+1][x] + 
                                    currentHeight[y][x-1] + 
                                    currentHeight[y][x+1];

                double acceleration = (neighborSum / 4.0) - currentHeight[y][x];
                nextHeight[y][x] = (2.0 * currentHeight[y][x] - previousHeight[y][x] + acceleration * propagationForce) * dampingFactor;
            }
        }

        // --- SYNCHRONIZED SLIT ENERGY COUPLING ---
        for (int y = 13; y <= 15; y++) {
            nextHeight[y][WALL_X + 1] = nextHeight[y][WALL_X - 1] * 2.0;
        }
        for (int y = 28; y <= 30; y++) {
            nextHeight[y][WALL_X + 1] = nextHeight[y][WALL_X - 1] * 2.0;
        }

        // FIX 2: Corrected the edge absorption fields to loop safely from column 1
        // This stops column 0 from being forcefully muted during active iterations
        for (int x = 0; x < SIM_W; x++) {
            nextHeight[0][x] = 0.0;
            nextHeight[SIM_H - 1][x] = 0.0;
        }
        for (int y = 0; y < SIM_H; y++) {
            nextHeight[y][0] = nextHeight[y][1] * dampingFactor; // Let left border absorb safely
            nextHeight[y][SIM_W - 1] = 0.0;
        }

        // --- ENERGY INTENSITY ACCUMULATION ---
        for (int y = 0; y < SIM_H; y++) {
            for (int x = WALL_X + 1; x < SIM_W; x++) {
                double amp = nextHeight[y][x];
                waveIntensity[y][x] = (waveIntensity[y][x] * 0.95) + (amp * amp * 0.05);
            }
        }

        // Swap memory buffers
        double[][] temp = previousHeight;
        previousHeight = currentHeight;
        currentHeight = nextHeight;
        nextHeight = temp;

        // --- 3. 2x2 BOX DOWNSAMPLING & RENDERING MATRIX ---
        for (int outY = 0; outY < OUT_H; outY++) {
            int simY1 = outY * 2;
            int simY2 = simY1 + 1;

            for (int outX = 0; outX < OUT_W; outX++) {
                int simX1 = outX * 2;
                int simX2 = simX1 + 1;

                int offset = outX + OUT_W * outY;

                boolean isObstacle = obstacles[simY1][simX1] || obstacles[simY1][simX2] || 
                                     obstacles[simY2][simX1] || obstacles[simY2][simX2];

                if (isObstacle) {
                    outputBuffer[offset] = "\u001B[38;5;244m█" + RESET; 
                    continue;
                }

                // Left Side: Live, moving wave ripples
                if (outX <= WALL_X / 2) {
                    double avgHeight = (currentHeight[simY1][simX1] + currentHeight[simY1][simX2] +
                                         currentHeight[simY2][simX1] + currentHeight[simY2][simX2]) / 4.0;

                    int shadeIdx = (int) ((avgHeight + 1.2) / 2.4 * (SHADE_RAMP.length - 1));
                    shadeIdx = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIdx));
                    char glyph = SHADE_RAMP[shadeIdx];

                    String colorCode = (avgHeight > 0.0) ? "\u001B[38;2;85;195;235m" : "\u001B[38;2;235;65;180m";
                    if (Math.abs(avgHeight) < 0.08) { colorCode = "\u001B[38;5;235m"; glyph = '·'; }

                    outputBuffer[offset] = colorCode + glyph + RESET;
                } 
                // Right Side: Stable, non-flickering interference detector screen
                else {
                    double avgIntensity = (waveIntensity[simY1][simX1] + waveIntensity[simY1][simX2] +
                                           waveIntensity[simY2][simX1] + waveIntensity[simY2][simX2]) / 4.0;

                    int shadeIdx = (int) (avgIntensity * 12.0);
                    shadeIdx = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIdx));
                    char glyph = SHADE_RAMP[shadeIdx];

                    String colorCode = "\u001B[38;2;85;215;105m"; // Academic Emerald Green
                    if (shadeIdx == 0) { colorCode = "\u001B[38;5;234m"; glyph = '·'; }

                    outputBuffer[offset] = colorCode + glyph + RESET;
                }
            }
        }
    }
}
