public class DoubleSlitLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Filling hydrodynamic pressure basin:"),
        new StatusStage(55, "Computing discrete height-field velocities:"),
        new StatusStage(85, "Projecting wave phase interference:"),
        new StatusStage(100, "Fluid Dynamics Tank Stabilized!")
    };

    private static final char[] SHADE_RAMP = { ' ', '.', '-', '░', '▒', '▓', '█' };
    
    private static int OUT_W; 
    private static int OUT_H;  

    private static int SIM_W;
    private static int SIM_H;

    private static int WALL_X;

    private double[][] currentHeight;
    private double[][] previousHeight;
    private double[][] nextHeight;
    private boolean[][] obstacles;
    private double[][] waveIntensity;
    private double timeClock = 0.0;

    public DoubleSlitLoader() {
        // Default to 100x28
        super(STAGES, 100, 22);
        OUT_H = 22;
        OUT_W = 100;
    }

    public DoubleSlitLoader(int width, int height) {
        super(STAGES, width, height);
        OUT_W = width;
        OUT_H = height;
    }

    @Override
    protected void initialize() {
        // Figure out where wall goes
        SIM_H = OUT_H * 4;
        SIM_W = OUT_W * 2;
        WALL_X = (int) (SIM_W * 0.2 + 10);

        // Define arrays
        currentHeight = new double[SIM_H][SIM_W];
        previousHeight = new double[SIM_H][SIM_W];
        nextHeight = new double[SIM_H][SIM_W];
        obstacles = new boolean[SIM_H][SIM_W];
        waveIntensity = new double[SIM_H][SIM_W];

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

        for (int y = 0; y < SIM_H; y++) {
            if (y >= 27 && y <= 29) continue;
            if (y >= 58 && y <= 60) continue;
            obstacles[y][WALL_X] = true;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.25; 

        double waveFrequency = 1.35;
        double waveAmplitude = 3.5; 
        double currentPulse = Math.sin(timeClock * waveFrequency) * waveAmplitude;
        
        currentHeight[SIM_H / 2][2] = currentPulse;
        currentHeight[SIM_H / 2 + 1][2] = currentPulse;

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

        double c = Math.sqrt(propagationForce); 
        
        for (int x = 1; x < SIM_W - 1; x++) {
            nextHeight[0][x] = currentHeight[1][x] + ((c - 1.0) / (c + 1.0)) * (nextHeight[1][x] - currentHeight[0][x]);
            nextHeight[SIM_H - 1][x] = currentHeight[SIM_H - 2][x] + ((c - 1.0) / (c + 1.0)) * (nextHeight[SIM_H - 2][x] - currentHeight[SIM_H - 1][x]);
        }
        
        for (int y = 1; y < SIM_H - 1; y++) {
            nextHeight[y][0] = currentHeight[y][1] + ((c - 1.0) / (c + 1.0)) * (nextHeight[y][1] - currentHeight[y][0]);
            nextHeight[y][SIM_W - 1] = currentHeight[y][SIM_W - 2] + ((c - 1.0) / (c + 1.0)) * (nextHeight[y][SIM_W - 2] - currentHeight[y][SIM_W - 1]);
        }

        nextHeight[0][0] *= dampingFactor;
        nextHeight[0][SIM_W - 1] *= dampingFactor;
        nextHeight[SIM_H - 1][0] *= dampingFactor;
        nextHeight[SIM_H - 1][SIM_W - 1] *= dampingFactor;

        for (int y = 0; y < SIM_H; y++) {
            for (int x = 0; x < SIM_W; x++) {
                double amp = nextHeight[y][x];
                waveIntensity[y][x] = (waveIntensity[y][x] * 0.95) + (amp * amp * 0.05);
            }
        }

        double[][] temp = previousHeight;
        previousHeight = currentHeight;
        currentHeight = nextHeight;
        nextHeight = temp;

        for (int outY = 0; outY < OUT_H; outY++) {
            int simY1 = outY * 4;
            int simY2 = simY1 + 1;
            int simY3 = simY1 + 2;
            int simY4 = simY1 + 3;

            for (int outX = 0; outX < OUT_W; outX++) {
                int simX1 = outX * 2;
                int simX2 = simX1 + 1;
                int offset = outX + OUT_W * outY;

                boolean isObstacle = (obstacles[simY1][simX1] || obstacles[simY1][simX2]) &&
                                     (obstacles[simY2][simX1] || obstacles[simY2][simX2]) &&
                                     (obstacles[simY3][simX1] || obstacles[simY3][simX2]) &&
                                     (obstacles[simY4][simX1] || obstacles[simY4][simX2]);
                                     
                if (isObstacle) {
                    outputBuffer[offset] = "\u001B[38;5;244m█" + RESET;
                    continue;
                }

                double avgHeight = (currentHeight[simY1][simX1] + currentHeight[simY1][simX2] + 
                                    currentHeight[simY2][simX1] + currentHeight[simY2][simX2] +
                                    currentHeight[simY3][simX1] + currentHeight[simY3][simX2] +
                                    currentHeight[simY4][simX1] + currentHeight[simY4][simX2]) / 8.0;

                double deltaX = (outX * 2) - WALL_X;
                double distanceFactor = (deltaX > 0) ? Math.sqrt(deltaX * 0.55) : 1.0;
                if (distanceFactor < 1.0) distanceFactor = 1.0;

                double amplifiedHeight = avgHeight * distanceFactor * 1.8;

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
