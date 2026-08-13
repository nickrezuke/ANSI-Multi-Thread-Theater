import java.util.Arrays;

public class CorridorWaveLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Filling hydrodynamic pressure basin:"),
        new StatusStage(55, "Molding waveguide corridor walls:"),
        new StatusStage(85, "Propagating angular diffraction fields:"),
        new StatusStage(100, "Wave-Guide Corridor Simulation Stable!")
    };

    private static final char[] SHADE_RAMP = { ' ', '.', '-', '░', '▒', '▓', '█' };

    // Target terminal viewport frame output sizes
    private static final int OUT_W = 140; 
    private static final int OUT_H = 38;  

    // Anamorphic 4x2 sub-pixel physics grid resolution metrics
    private static final int SIM_W = OUT_W * 2;
    private static final int SIM_H = OUT_H * 4;

    private double[][] currentHeight = new double[SIM_H][SIM_W];
    private double[][] previousHeight = new double[SIM_H][SIM_W];
    private double[][] nextHeight = new double[SIM_H][SIM_W];
    private boolean[][] obstacles = new boolean[SIM_H][SIM_W];
    private double[][] waveIntensity = new double[SIM_H][SIM_W];
    private double timeClock = 0.0;

    public CorridorWaveLoader() {
        // Only handles 140x38 at the moment due to specific size
        super(STAGES, OUT_W, OUT_H);
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

        // --- MAP CUSTOM CORRIDOR CORNER WALLS BASED ON THE USER SKETCH ---
        for (int y = 0; y < SIM_H; y++) {
            for (int x = 0; x < SIM_W; x++) {
                
                // 1. Structural Outer Boundary Shell (Padded slightly to let ABC layers function)
                boolean isOuterWall = (x < 4) || (y < 4) || (y > SIM_H - 5);
                
                // Construct the outer 45-degree angle corner ramp on the right side
                if (x >= 160) {
                    if (x + y > 307) { // Diagonal cutting line bounding the corner turn
                        isOuterWall = true;
                    }
                }
                // Cap off the far right edge boundary
                if (x > 255) {
                    isOuterWall = true;
                }

                // 2. Central Inner Island Obstacle (Forms the channel corridors)
                // Parallel diagonal constraint (x + y <= 245) mirrors the outer 45-degree slant perfectly
                boolean isIsland = (x >= 45 && x <= 210) && (y >= 35 && y <= 115) && (x + y <= 245);

                if (isOuterWall || isIsland) {
                    obstacles[y][x] = true;
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.08; 

        // 3. CENTRAL RADIAL DOT SOURCE: Planted exactly inside the bottom-left junction box
        double waveFrequency = 1.35;
        double waveAmplitude = 5.0;
        double currentPulse = Math.sin(timeClock * waveFrequency) * waveAmplitude;
        
        int srcX = 22;
        int srcY = SIM_H - 22; // Centers the point source inside the bottom corner junction
        
        currentHeight[srcY][srcX]     = currentPulse;
        currentHeight[srcY+1][srcX]   = currentPulse;
        currentHeight[srcY][srcX+1]   = currentPulse;
        currentHeight[srcY+1][srcX+1] = currentPulse;

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

        double[][] temp = previousHeight; previousHeight = currentHeight; currentHeight = nextHeight; nextHeight = temp;

        for (int outY = 0; outY < OUT_H; outY++) {
            int simY1 = outY * 4, simY2 = simY1 + 1, simY3 = simY1 + 2, simY4 = simY1 + 3;
            
            for (int outX = 0; outX < OUT_W; outX++) {
                int simX1 = outX * 2, simX2 = simX1 + 1;
                int offset = outX + OUT_W * outY;

                boolean isObstacle = (obstacles[simY1][simX1] || obstacles[simY1][simX2]) &&
                                      (obstacles[simY2][simX1] || obstacles[simY2][simX2]) &&
                                      (obstacles[simY3][simX1] || obstacles[simY3][simX2]) &&
                                      (obstacles[simY4][simX1] || obstacles[simY4][simX2]);

                if (isObstacle) {
                    outputBuffer[offset] = "\u001B[38;2;0;103;17m█\u001B[0m";
                    continue;
                }

                double avgHeight = (currentHeight[simY1][simX1] + currentHeight[simY1][simX2] +
                                     currentHeight[simY2][simX1] + currentHeight[simY2][simX2] +
                                     currentHeight[simY3][simX1] + currentHeight[simY3][simX2] +
                                     currentHeight[simY4][simX1] + currentHeight[simY4][simX2]) / 8.0;

                double amplifiedHeight = avgHeight * 2.2;
                int shadeIdx = (int) ((amplifiedHeight + 1.2) / 2.4 * (SHADE_RAMP.length - 1));
                shadeIdx = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIdx));
                char glyph = SHADE_RAMP[shadeIdx];

                String colorCode;
                if (Math.abs(amplifiedHeight) < 0.12) {
                    colorCode = "\u001B[38;5;234m"; glyph = '·';
                } else {
                    colorCode = (amplifiedHeight > 0.0) ? "\u001B[38;2;235;65;180m" : "\u001B[38;2;45;210;240m";
                }
                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }
}
