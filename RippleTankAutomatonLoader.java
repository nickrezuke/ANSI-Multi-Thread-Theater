public class RippleTankAutomatonLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Filling hydrodynamic pressure basin:"),
        new StatusStage(55, "Computing discrete height-field velocities:"),
        new StatusStage(85, "Projecting wave phase interference:"),
        new StatusStage(100, "Fluid Dynamics Tank Stabilized!")
    };

    private static final char[] SHADE_RAMP = { ' ', '.', '-', '░', '▒', '▓', '█' };
    
    // --- VERTICAL STRETCH FIX: DOUBLED CORE PHYSICS HEIGHT ---
    // Doubling SIM_H to 88 allows us to squeeze 4 physics rows into 1 terminal row.
    // This squashes vertical height by exactly 0.5 mathematically.
    private static final int SIM_W = 160;
    private static final int SIM_H = 88; 
    private static final int OUT_W = 80;
    private static final int OUT_H = 22;
    private static final int WALL_X = 30;

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
            for(int i = 0; i < currentHeight[y].length; i++) {
                currentHeight[y][i] = 0.0;
            }
            for(int i = 0; i < previousHeight[y].length; i++) {
                previousHeight[y][i] = 0.0;
            }
            for(int i = 0; i < waveIntensity[y].length; i++) {
                waveIntensity[y][i] = 0.0;
            }
            for(int i = 0; i < obstacles[y].length; i++) {
                obstacles[y][i] = false;
            }
        }

        // Build the central barrier wall at WALL_X scaled up to the 88-row physics grid.
        // Aperture slit gaps are proportionally scaled up to match the height profile.
        for (int y = 0; y < SIM_H; y++) {
            if (y >= 27 && y <= 29) continue; // Slit 1 Aperture Channel
            if (y >= 58 && y <= 60) continue; // Slit 2 Aperture Channel
            obstacles[y][WALL_X] = true;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.25; 

        // --- STEP 1: WAVE ENGINE OSCILLATOR ---
        double waveFrequency = 1.35;
        double waveAmplitude = 3.5; 
        double currentPulse = Math.sin(timeClock * waveFrequency) * waveAmplitude;
        
        // Driver source points updated to target the new mathematical center rows
        currentHeight[SIM_H / 2][2] = currentPulse;
        currentHeight[SIM_H / 2 + 1][2] = currentPulse;

        // --- STEP 2: WAVE FIELD PROPAGATION ---
        double propagationForce = 0.45;
        double dampingFactor = 0.996; 

        for (int y = 1; y < SIM_H - 1; y++) {
            for (int x = 1; x < SIM_W - 1; x++) {
                if (obstacles[y][x]) {
                    nextHeight[y][x] = 0.0; 
                    continue;
                }

                double neighborSum = currentHeight[y-1][x] + currentHeight[y+1][x] + currentHeight[y][x-1] + currentHeight[y][x+1];
                double acceleration = (neighborSum / 4.0) - currentHeight[y][x];
                
                nextHeight[y][x] = (2.0 * currentHeight[y][x] - previousHeight[y][x] + acceleration * propagationForce) * dampingFactor;
            }
        }

        // --- STEP 3: NON-REFLECTING ABSORBING BOUNDARY CONDITIONS (ABC) ---
        double c = Math.sqrt(propagationForce); 
        
        for (int x = 1; x < SIM_W - 1; x++) {
            nextHeight[0][x] = currentHeight[1][x] + ((c - 1.0) / (c + 1.0)) * (nextHeight[1][x] - currentHeight[0][x]);
            nextHeight[SIM_H - 1][x] = currentHeight[SIM_H - 2][x] + ((c - 1.0) / (c + 1.0)) * (nextHeight[SIM_H - 2][x] - currentHeight[SIM_H - 1][x]);
        }
        
        for (int y = 1; y < SIM_H - 1; y++) {
            nextHeight[y][0] = currentHeight[y][1] + ((c - 1.0) / (c + 1.0)) * (nextHeight[y][1] - currentHeight[y][0]);
            nextHeight[y][SIM_W - 1] = currentHeight[y][SIM_W - 2] + ((c - 1.0) / (c + 1.0)) * (nextHeight[y][SIM_W - 2] - currentHeight[y][SIM_W - 1]);
        }

        // Corner nodes dampening overrides
        nextHeight[0][0] *= dampingFactor;
        nextHeight[0][SIM_W - 1] *= dampingFactor;
        nextHeight[SIM_H - 1][0] *= dampingFactor;
        nextHeight[SIM_H - 1][SIM_W - 1] *= dampingFactor;

        // --- STEP 4: INTENSITY CALCULATION ---
        for (int y = 0; y < SIM_H; y++) {
            for (int x = 0; x < SIM_W; x++) {
                double amp = nextHeight[y][x];
                waveIntensity[y][x] = (waveIntensity[y][x] * 0.95) + (amp * amp * 0.05);
            }
        }

        // Buffer Address Reference Pointer Swapping
        double[][] temp = previousHeight;
        previousHeight = currentHeight;
        currentHeight = nextHeight;
        nextHeight = temp;

        // --- STEP 5: TRUE TRUE 4x2 COMPRESSION RENDERING WINDOW ---
        // Squeezing exactly 4 vertical simulation cells down into 1 terminal character row
        // forces the height to shrink by 0.5 relative to your horizontal sampling.
        for (int outY = 0; outY < OUT_H; outY++) {
            int simY1 = outY * 4;
            int simY2 = simY1 + 1;
            int simY3 = simY1 + 2;
            int simY4 = simY1 + 3;

            for (int outX = 0; outX < OUT_W; outX++) {
                int simX1 = outX * 2;
                int simX2 = simX1 + 1;
                int offset = outX + OUT_W * outY;

                // --- FIXED ADAPTIVE SUB-GRID SLIT DETECTOR ---
                // For a macro-pixel to block the wave view, the wall must occupy the core of the 
                // vertical sample window. If rows are open for the slit channel, we keep the pixel clear.
                boolean isObstacle = (obstacles[simY1][simX1] || obstacles[simY1][simX2]) &&
                                     (obstacles[simY2][simX1] || obstacles[simY2][simX2]) &&
                                     (obstacles[simY3][simX1] || obstacles[simY3][simX2]) &&
                                     (obstacles[simY4][simX1] || obstacles[simY4][simX2]);
                                     
                if (isObstacle) {
                    outputBuffer[offset] = "\u001B[38;5;244m█" + RESET;
                    continue;
                }

                // Average height calculated across the full 4x2 high-density downsample box
                double avgHeight = (currentHeight[simY1][simX1] + currentHeight[simY1][simX2] + 
                                    currentHeight[simY2][simX1] + currentHeight[simY2][simX2] +
                                    currentHeight[simY3][simX1] + currentHeight[simY3][simX2] +
                                    currentHeight[simY4][simX1] + currentHeight[simY4][simX2]) / 8.0;

                // --- LOGARITHMIC DISTANCE COMPENSATION ENGINE ---
                double deltaX = (outX * 2) - WALL_X;
                double distanceFactor = (deltaX > 0) ? Math.sqrt(deltaX * 0.55) : 1.0;
                if (distanceFactor < 1.0) distanceFactor = 1.0;

                double amplifiedHeight = avgHeight * distanceFactor * 1.8;

                // Map amplitude values cleanly into shading index slots
                int shadeIdx = (int) ((amplifiedHeight + 1.2) / 2.4 * (SHADE_RAMP.length - 1));
                if (shadeIdx < 0) shadeIdx = 0; 
                else if (shadeIdx > SHADE_RAMP.length - 1) shadeIdx = SHADE_RAMP.length - 1;
                
                char glyph = SHADE_RAMP[shadeIdx];
                String colorCode;

                if (Math.abs(amplifiedHeight) < 0.15) {
                    colorCode = "\u001B[38;5;234m"; 
                    glyph = '·';
                } else {
                    colorCode = (amplifiedHeight > 0.0) ? "\u001B[38;2;85;195;235m" : "\u001B[38;2;235;65;180m"; 
                }

                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }
}
