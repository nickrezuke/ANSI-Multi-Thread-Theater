public class BriansBrainLoader extends Loader {
    private static final StatusStage[] BRAIN_STAGES = {
            new StatusStage(25, "Allocating synaptic state arrays:"),
            new StatusStage(50, "Calibrating cellular automata rules:"),
            new StatusStage(75, "Seeding chaotic initial neural nodes:"),
            new StatusStage(100, "Brian's Brain Simulator Operational!")
    };

    // 1. Structural Boundaries
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;
    private static final int SIZE = WIDTH * HEIGHT;

    // 2. Cell States
    private static final byte READY = 0;
    private static final byte FIRING = 1;
    private static final byte REFRACTORY = 2;

    // Dual buffers to process synchronous step generations safely
    private byte[] grid = new byte[SIZE];
    private byte[] nextGrid = new byte[SIZE];

    private long lastTickTime = 0;
    private static final long STEP_DELAY_MS = 40; // Balanced delay to comfortably watch glider wave cycles

    // 3. Graphic Color Profiles
    private static final String COLOR_READY = "\u001B[38;5;234m·" + RESET; // Dim background dots
    private static final String COLOR_FIRING = "\u001B[38;5;81;1m█" + RESET; // High-intensity Electric Cyan
    private static final String COLOR_REFRACTORY = "\u001B[38;5;162m░" + RESET; // Muted Magenta/Red decay shade

    public BriansBrainLoader() {
        super(BRAIN_STAGES);
    }

    @Override
    protected void initialize() {
        resetSimulation();
    }

    private void resetSimulation() {
        java.util.Random rand = new java.util.Random();

        // Seed the canvas with a dense, chaotic mix of initial 
        // firing nodes (~30% density)
        for (int i = 0; i < SIZE; i++) {
            int chance = rand.nextInt(100);
            if (chance < 20) {
                grid[i] = FIRING;
            } else if (chance < 30) {
                grid[i] = REFRACTORY;
            } else {
                grid[i] = READY;
            }
            nextGrid[i] = READY;
        }
        lastTickTime = System.currentTimeMillis();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        // Advance simulation generation steps at regulated clock frames
        if (currentTime - lastTickTime >= STEP_DELAY_MS) {
            lastTickTime = currentTime;
            computeNextGeneration();
        }

        drawScene(outputBuffer);
    }

    private void computeNextGeneration() {
        boolean fieldIsDead = true;

        for (int y = 0; y < HEIGHT; y++) {
            int row = y * WIDTH;
            int rowPrev = ((y - 1 + HEIGHT) % HEIGHT) * WIDTH; // Toroidal coordinate wrapping offsets
            int rowNext = ((y + 1) % HEIGHT) * WIDTH;

            for (int x = 0; x < WIDTH; x++) {
                int idx = x + row;
                byte state = grid[idx];

                if (state == FIRING) {
                    // Rule 1: A Firing cell always turns into a Refractory cell on the next step
                    nextGrid[idx] = REFRACTORY;
                    fieldIsDead = false;
                } else if (state == REFRACTORY) {
                    // Rule 2: A Refractory cell always recovers back to a Ready state
                    nextGrid[idx] = READY;
                } else {
                    // Rule 3: A Ready cell only fires if it has EXACTLY 2 Firing neighbors
                    int xPrev = (x - 1 + WIDTH) % WIDTH;
                    int xNext = (x + 1) % WIDTH;

                    int firingNeighbors = 0;
                    if (grid[xPrev + rowPrev] == FIRING)
                        firingNeighbors++;
                    if (grid[x + rowPrev] == FIRING)
                        firingNeighbors++;
                    if (grid[xNext + rowPrev] == FIRING)
                        firingNeighbors++;
                    if (grid[xPrev + row] == FIRING)
                        firingNeighbors++;
                    if (grid[xNext + row] == FIRING)
                        firingNeighbors++;
                    if (grid[xPrev + rowNext] == FIRING)
                        firingNeighbors++;
                    if (grid[x + rowNext] == FIRING)
                        firingNeighbors++;
                    if (grid[xNext + rowNext] == FIRING)
                        firingNeighbors++;

                    if (firingNeighbors == 2) {
                        nextGrid[idx] = FIRING;
                        fieldIsDead = false;
                    } else {
                        nextGrid[idx] = READY;
                    }
                }
            }
        }

        // Swap state buffer pointers
        byte[] temp = grid;
        grid = nextGrid;
        nextGrid = temp;

        // Auto-reseed if the chaotic field reaches total stasis or completely decays
        if (fieldIsDead) {
            resetSimulation();
        }
    }

    private void drawScene(String[] outputBuffer) {
        for (int i = 0; i < SIZE; i++) {
            byte state = grid[i];
            if (state == FIRING) {
                outputBuffer[i] = COLOR_FIRING;
            } else if (state == REFRACTORY) {
                outputBuffer[i] = COLOR_REFRACTORY;
            } else {
                outputBuffer[i] = COLOR_READY;
            }
        }
    }
}
