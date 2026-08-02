public class RockPaperScissorsAutomatonLoader extends Loader {
    private static final StatusStage[] STAGES = {
            new StatusStage(25, "Seeding primordial ecosystem:"),
            new StatusStage(55, "Calculating predatory rule vectors:"),
            new StatusStage(85, "Balancing cyclic Nash equilibrium:"),
            new StatusStage(100, "Cyclic Chaos Matrix Stable!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // --- ADJUSTABLE CONTROL CONTROLS ---
    private static final int SIMULATION_DELAY = 5; // Slowed down to observe spiral formation
    private int renderTick = 0;

    // Mutation rate prevents global color death loops/synchronized flashing.
    // Ex) 0.0008 = ~0.08% chance per cell per frame to spontaneously mutate.
    private static final double MUTATION_RATE = 0.0;

    // Grid states: 0 = Rock, 1 = Paper, 2 = Scissors
    private int[][] currentGrid = new int[HEIGHT][WIDTH];
    private int[][] nextGrid = new int[HEIGHT][WIDTH];
    private int activeThreshold = 3;

    public RockPaperScissorsAutomatonLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        renderTick = 0;
        int seedStrategy = (int) (Math.random() * 3); // 3 premium, slow-stabilizing seeds

        // Clear grids
        for (int y = 0; y < HEIGHT; y++) {
            for(int i = 0; i < currentGrid[y].length; i++) {
                currentGrid[y][i] = 0;
            }
            for(int i = 0; i < nextGrid[y].length; i++) {
                nextGrid[y][i] = 0;
            }
        }

        switch (seedStrategy) {
            case 0:
                // SEED A: The Quad-Vortex Interlocking Engine (Slow-Burn Volatile Chaos)
                // Spawns 4 offset regional centers with an unstable high-entropy friction line
                // down their intersections, forcing 4 massive spirals to wage an endless
                // tug-of-war.
                activeThreshold = 3;

                // Choose 4 distinct, off-center focal coordinates
                int pt1X = (int) (WIDTH * 0.28), pt1Y = (int) (HEIGHT * 0.25);
                int pt2X = (int) (WIDTH * 0.72), pt2Y = (int) (HEIGHT * 0.30);
                int pt3X = (int) (WIDTH * 0.32), pt3Y = (int) (HEIGHT * 0.75);
                int pt4X = (int) (WIDTH * 0.68), pt4Y = (int) (HEIGHT * 0.70);

                for (int y = 0; y < HEIGHT; y++) {
                    for (int x = 0; x < WIDTH; x++) {
                        // Compute distance metrics to all four centers
                        double d1 = Math.hypot(x - pt1X, y - pt1Y);
                        double d2 = Math.hypot(x - pt2X, y - pt2Y);
                        double d3 = Math.hypot(x - pt3X, y - pt3Y);
                        double d4 = Math.hypot(x - pt4X, y - pt4Y);

                        // Establish primary quadrant domains
                        if (d1 <= d2 && d1 <= d3 && d1 <= d4) {
                            currentGrid[y][x] = 0; // Rock Domain
                        } else if (d2 <= d1 && d2 <= d3 && d2 <= d4) {
                            currentGrid[y][x] = 1; // Paper Domain
                        } else if (d3 <= d1 && d3 <= d2 && d3 <= d4) {
                            currentGrid[y][x] = 2; // Scissors Domain
                        } else {
                            currentGrid[y][x] = (Math.random() > 0.5) ? 0 : 1; // Unstable Matrix Core
                        }

                        // CATALYTIC FRICTION LAYER: Inject high-entropy noise directly on border seams.
                        // This prevents clean boundaries from freezing and instantly ignites
                        // multi-headed spiral waves.
                        double diff12 = Math.abs(d1 - d2);
                        double diff24 = Math.abs(d2 - d4);
                        double diff34 = Math.abs(d3 - d4);

                        if (diff12 < 3.5 || diff24 < 3.5 || diff34 < 3.5) {
                            if (Math.random() > 0.35) {
                                currentGrid[y][x] = (int) (Math.random() * 3);
                            }
                        }
                    }
                }
                break;

            case 1:
                // SEED B: Volatile Shuffled Labyrinth (Slow Stabilization Chaos)
                // By seeding alternating vertical blocks with precise internal high-entropy
                // noise,
                // the boundaries don't just shift—they fracture into localized pinwheel
                // battlegrounds.
                activeThreshold = 3;
                int blockSize = 10;
                for (int y = 0; y < HEIGHT; y++) {
                    for (int x = 0; x < WIDTH; x++) {
                        int baseBlockState = ((x / blockSize) + (y / blockSize)) % 3;
                        // Add controlled volatility to boundaries
                        if (Math.random() > 0.40) {
                            currentGrid[y][x] = baseBlockState;
                        } else {
                            currentGrid[y][x] = (baseBlockState + 1) % 3;
                        }
                    }
                }
                break;

            case 2:
                // SEED C: Triple Helical Spiral Key
                // Generates interlocking geometric arms resembling a pinwheel key. This
                // topology
                // natively drives the cellular math into beautiful, sweeping macro-vortices
                // right out of the gate.
                activeThreshold = 3;
                int cy = HEIGHT / 2;
                int cx = WIDTH / 2;
                for (int y = 0; y < HEIGHT; y++) {
                    for (int x = 0; x < WIDTH; x++) {
                        double angle = Math.atan2(y - cy, x - cx) + Math.PI; // 0 to 2PI
                        if (angle < (2 * Math.PI / 3)) {
                            currentGrid[y][x] = 0;
                        } else if (angle < (4 * Math.PI / 3)) {
                            currentGrid[y][x] = 1;
                        } else {
                            currentGrid[y][x] = 2;
                        }
                    }
                }
                // Break up the central convergence spot to catalyze immediate spinning waves
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        currentGrid[(cy + dy + HEIGHT) % HEIGHT][(cx + dx + WIDTH) % WIDTH] = (int) (Math.random() * 3);
                    }
                }
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        renderTick++;

        if (renderTick % SIMULATION_DELAY == 0) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {

                    // --- THE MECHANIC IMPROVEMENT: MUTATION ENGINE ---
                    // Randomly triggers a state change to prevent synchronous, global flashing
                    // patterns.
                    if (Math.random() < MUTATION_RATE) {
                        nextGrid[y][x] = (currentGrid[y][x] + (Math.random() > 0.5 ? 1 : 2)) % 3;
                        continue;
                    }

                    int myState = currentGrid[y][x];
                    int predatorState = (myState + 1) % 3;
                    int predatorCount = 0;

                    // Toroidal Moore Neighborhood Scan
                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = (y + dy + HEIGHT) % HEIGHT;
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0)
                                continue;
                            int nx = (x + dx + WIDTH) % WIDTH;

                            if (currentGrid[ny][nx] == predatorState) {
                                predatorCount++;
                            }
                        }
                    }

                    if (predatorCount >= activeThreshold) {
                        nextGrid[y][x] = predatorState;
                    } else {
                        nextGrid[y][x] = myState;
                    }
                }
            }

            int[][] temp = currentGrid;
            currentGrid = nextGrid;
            nextGrid = temp;
        }

        // --- RENDER POPULATION MAP USING THE ACADEMIC PALETTE ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int offset = x + WIDTH * y;
                int state = currentGrid[y][x];
                String colorCode;
                char glyph = '█';

                switch (state) {
                    case 0:
                        colorCode = "\u001B[38;2;235;75;75m";
                        break; // Terracotta Rock
                    case 1:
                        colorCode = "\u001B[38;2;85;195;110m";
                        break; // Sage Paper
                    case 2:
                        colorCode = "\u001B[38;2;70;130;225m";
                        break; // Denim Scissors
                    default:
                        colorCode = "";
                        glyph = ' ';
                        break;
                }
                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }
}
