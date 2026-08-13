// TODO: Make this one look better...

public class WavePropagationLoader extends Loader {
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

    private double[][] currentHeight;
    private double[][] previousHeight;
    private double[][] nextHeight;
    private boolean[][] obstacles;
    private double[][] waveIntensity;
    private double timeClock = 0.0;

    public WavePropagationLoader() {
        // Default to 100x28
        super(STAGES, 100, 22);
        OUT_H = 22;
        OUT_W = 100;
    }

    public WavePropagationLoader(int width, int height) {
        super(STAGES, width, height);
        OUT_W = width;
        OUT_H = height;
    }

    @Override
    protected void initialize() {
        // Figure out where wall goes
        SIM_H = OUT_H * 4;
        SIM_W = OUT_W * 2;

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

        // --- NEW RULE: GENERATE THE RETICULATED PEG LATTICE MATRIX ---
        // Places a diamond-pattern crystal lattice block right in the center field of the tank
        int startLatticeX = (int) (SIM_W * 0.35); // Begins 35% across the field
        int endLatticeX   = (int) (SIM_W * 0.70); // Ends 70% across the field
        
        int gridSpacingX = 24; // Distance between pegs horizontally
        int gridSpacingY = 16; // Distance between pegs vertically
        double pegRadius = 3.2; // Cylindrical thickness radius of each individual pin

        for (int y = 2; y < SIM_H - 2; y++) {
            for (int x = startLatticeX; x < endLatticeX; x++) {
                
                // Offset alternating vertical rows to create an organic diamond/triangular lattice
                int rowShift = ((y / gridSpacingY) % 2 == 0) ? 0 : gridSpacingX / 2;
                
                // Calculate tracking deltas to the nearest layout peg origin center point
                int nearestPegX = ((x - rowShift) / gridSpacingX) * gridSpacingX + rowShift + (gridSpacingX / 2);
                int nearestPegY = (y / gridSpacingY) * gridSpacingY + (gridSpacingY / 2);

                double dx = x - nearestPegX;
                double dy = y - nearestPegY;
                
                // If cell coordinates reside within the cylinder radius, plant a solid anchor obstacle
                if ((dx * dx) + (dy * dy) <= (pegRadius * pegRadius)) {
                    obstacles[y][x] = true;
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.25; 

        // 1. EXTENDED FLAT DRIVER LINE SOURCE: Generates wide parallel plane wave fronts on the far left wall
        double waveFrequency = 1.30;
        double waveAmplitude = 4.0;
        double currentPulse = Math.sin(timeClock * waveFrequency) * waveAmplitude;
        
        // Drive an entire vertical line array to send a wall of water into the pegs
        for (int y = 4; y < SIM_H - 4; y++) {
            currentHeight[y][2] = currentPulse;
            currentHeight[y][3] = currentPulse;
        }

        // 2. DISCRETE 2D FINITE WAVE ENGINE CORE PROPAGATION
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

        // 3. NON-REFLECTING WAVE ABSORPTION perimeter limits
        double c = Math.sqrt(propagationForce);
        for (int x = 1; x < SIM_W - 1; x++) {
            nextHeight[0][x] = currentHeight[1][x] + ((c - 1.0) / (c + 1.0)) * (nextHeight[1][x] - currentHeight[0][x]);
            nextHeight[SIM_H - 1][x] = currentHeight[SIM_H - 2][x] + ((c - 1.0) / (c + 1.0)) * (nextHeight[SIM_H - 2][x] - currentHeight[SIM_H - 1][x]);
        }
        for (int y = 1; y < SIM_H - 1; y++) {
            nextHeight[y][0] = currentHeight[y][1] + ((c - 1.0) / (c + 1.0)) * (nextHeight[y][1] - currentHeight[y][0]);
            nextHeight[y][SIM_W - 1] = currentHeight[y][SIM_W - 2] + ((c - 1.0) / (c + 1.0)) * (nextHeight[y][SIM_W - 2] - currentHeight[y][SIM_W - 1]);
        }

        // Swap memory arrays references
        double[][] temp = previousHeight; previousHeight = currentHeight; currentHeight = nextHeight; nextHeight = temp;

        // 4. ANAMORPHIC COMPRESSION CONSOLE GRAPHICS PASS
        for (int outY = 0; outY < OUT_H; outY++) {
            int simY1 = outY * 4, simY2 = simY1 + 1, simY3 = simY1 + 2, simY4 = simY1 + 3;
            
            for (int outX = 0; outX < OUT_W; outX++) {
                int simX1 = outX * 2, simX2 = simX1 + 1;
                int offset = outX + OUT_W * outY;

                // Sub-pixel layout checking logic for character mapping blocks
                boolean isObstacle = (obstacles[simY1][simX1] || obstacles[simY1][simX2]) &&
                                      (obstacles[simY2][simX1] || obstacles[simY2][simX2]) &&
                                      (obstacles[simY3][simX1] || obstacles[simY3][simX2]) &&
                                      (obstacles[simY4][simX1] || obstacles[simY4][simX2]);

                if (isObstacle) {
                    // Clean slate grey pins representing the fixed rigid mesh layout
                    outputBuffer[offset] = "\u001B[38;5;242m#\u001B[0m"; 
                    continue;
                }

                double avgHeight = (currentHeight[simY1][simX1] + currentHeight[simY1][simX2] +
                                     currentHeight[simY2][simX1] + currentHeight[simY2][simX2] +
                                     currentHeight[simY3][simX1] + currentHeight[simY3][simX2] +
                                     currentHeight[simY4][simX1] + currentHeight[simY4][simX2]) / 8.0;

                // Apply mild amplifier scaling factor across values
                double amplifiedHeight = avgHeight * 2.2;
                int shadeIdx = (int) ((amplifiedHeight + 1.2) / 2.4 * (SHADE_RAMP.length - 1));
                shadeIdx = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIdx));
                char glyph = SHADE_RAMP[shadeIdx];

                String colorCode;
                if (Math.abs(amplifiedHeight) < 0.12) {
                    colorCode = "\u001B[38;5;234m"; glyph = '·';
                } else {
                    colorCode = (amplifiedHeight > 0.0) ? "\u001B[38;2;135;135;180m" : "\u001B[38;2;45;210;240m";
                }
                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }
}
