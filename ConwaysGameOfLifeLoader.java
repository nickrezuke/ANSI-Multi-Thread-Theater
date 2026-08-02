public class ConwaysGameOfLifeLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Seeding cell colony arrays:"),
        new StatusStage(55, "Calibrating neighbor scanning grids:"),
        new StatusStage(85, "Synchronizing generational tick rates:"),
        new StatusStage(100, "Conway Bio-Matrix Core Stable!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // --- SPEED CONTROL ---
    // Change this value to make the simulation faster or slower.
    // 1 = Max speed, 10 = Updates once every 10 engine frames.
    private static final int SIMULATION_DELAY = 10; 
    private int renderTick = 0;

    // Dual-buffered world maps
    private int[][] currentGrid = new int[HEIGHT][WIDTH];
    private int[][] nextGrid = new int[HEIGHT][WIDTH];
    
    // Tracks cell age to drive pure brightness decay (0 = Dead)
    private int[][] cellAge = new int[HEIGHT][WIDTH];

    public ConwaysGameOfLifeLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        for (int y = 0; y < HEIGHT; y++) {
            for(int i = 0; i < currentGrid[y].length; i++) {
                currentGrid[y][i] = 0;
            }
            for(int i = 0; i < cellAge[y].length; i++) {
                cellAge[y][i] = 0;
            }
        }

        // --- NEW ARCHETYPAL & PROCEDURAL SEEDS ---
        int variant = (int) (Math.random() * 4);
        switch (variant) {
            case 0: 
                // SEED A [Procedural]: Kaleidoscopic Mirror Symmetry
                // Generates random noise in one quadrant and mirrors it to create organic patterns
                for (int y = 0; y < HEIGHT / 2; y++) {
                    for (int x = 0; x < WIDTH / 2; x++) {
                        if (Math.random() > 0.65) {
                            // Mirror across all 4 quadrants
                            currentGrid[y][x] = 1;
                            currentGrid[y][WIDTH - 1 - x] = 1;
                            currentGrid[HEIGHT - 1 - y][x] = 1;
                            currentGrid[HEIGHT - 1 - y][WIDTH - 1 - x] = 1;
                            
                            cellAge[y][x] = 1;
                            cellAge[y][WIDTH - 1 - x] = 1;
                            cellAge[HEIGHT - 1 - y][x] = 1;
                            cellAge[HEIGHT - 1 - y][WIDTH - 1 - x] = 1;
                        }
                    }
                }
                break;

            case 1:
                // SEED B [Procedural]: Urban Colony Clusters
                // Spawns highly localized dense cellular hubs instead of uniform cosmic noise
                int totalHubs = 4 + (int)(Math.random() * 3);
                for (int h = 0; h < totalHubs; h++) {
                    int centerX = 10 + (int)(Math.random() * (WIDTH - 20));
                    int centerY = 4 + (int)(Math.random() * (HEIGHT - 8));
                    for (int step = 0; step < 25; step++) {
                        int rx = centerX + (int)(Math.random() * 9 - 4);
                        int ry = centerY + (int)(Math.random() * 5 - 2);
                        if (ry >= 0 && ry < HEIGHT && rx >= 0 && rx < WIDTH) {
                            currentGrid[ry][rx] = 1;
                            cellAge[ry][rx] = 1;
                        }
                    }
                }
                break;

            case 2: 
                // SEED C [Legendary Methuselah]: The Acorn
                // A tiny 7-cell structure that explodes into complex chaos stabilizing over 5,200 generations
                int ay = HEIGHT / 2;
                int ax = WIDTH / 2 - 2;
                if (ay + 2 < HEIGHT && ax + 6 < WIDTH) {
                    currentGrid[ay][ax+1] = 1;
                    currentGrid[ay+1][ax+3] = 1;
                    currentGrid[ay+2][ax] = 1;
                    currentGrid[ay+2][ax+1] = 1;
                    currentGrid[ay+2][ax+4] = 1;
                    currentGrid[ay+2][ax+5] = 1;
                    currentGrid[ay+2][ax+6] = 1;
                    
                    for(int y=ay; y<=ay+2; y++) {
                        for(int x=ax; x<=ax+6; x++) {
                            if(currentGrid[y][x] == 1) cellAge[y][x] = 1;
                        }
                    }
                }
                break;

            case 3:
                // SEED D [Factory Engine]: Gosper Glider Gun
                // A stable mechanism that continuously manufactures and shoots flying gliders
                int gy = 1;
                int gx = 5;
                int[][] gunCoords = {
                    {5,1}, {5,2}, {6,1}, {6,2}, {5,11}, {6,11}, {7,11}, {4,12}, {8,12}, {3,13}, {9,13}, 
                    {3,14}, {9,14}, {6,15}, {4,16}, {8,16}, {5,17}, {6,17}, {7,17}, {6,18}, {3,21}, {4,21}, 
                    {5,21}, {3,22}, {4,22}, {5,22}, {2,23}, {6,23}, {1,25}, {2,25}, {6,25}, {7,25}, {3,35}, 
                    {4,35}, {3,36}, {4,36}
                };
                for (int[] coord : gunCoords) {
                    int targetY = gy + coord[0];
                    int targetX = gx + coord[1];
                    if (targetY < HEIGHT && targetX < WIDTH) {
                        currentGrid[targetY][targetX] = 1;
                        cellAge[targetY][targetX] = 1;
                    }
                }
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        renderTick++;

        // Only advance Conway's state machine when reaching the threshold factor
        if (renderTick % SIMULATION_DELAY == 0) {
            // Evaluate Conway's rules cell-by-cell
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int liveNeighbors = 0;

                    // Scan 8 toroidal wrapping neighbors
                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = (y + dy + HEIGHT) % HEIGHT;
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = (x + dx + WIDTH) % WIDTH;
                            if (currentGrid[ny][nx] == 1) {
                                liveNeighbors++;
                            }
                        }
                    }

                    int myState = currentGrid[y][x];
                    if (myState == 1) {
                        if (liveNeighbors < 2 || liveNeighbors > 3) {
                            nextGrid[y][x] = 0; // Under/Overpopulation death
                        } else {
                            nextGrid[y][x] = 1; // Survival
                        }
                    } else {
                        if (liveNeighbors == 3) {
                            nextGrid[y][x] = 1; // Birth
                        } else {
                            nextGrid[y][x] = 0;
                        }
                    }

                    // Strict age increment or instant wipeout (no ghost trail delay)
                    if (nextGrid[y][x] == 1) {
                        cellAge[y][x]++;
                    } else {
                        cellAge[y][x] = 0;
                    }
                }
            }

            // Buffer Swap
            int[][] temp = currentGrid;
            currentGrid = nextGrid;
            nextGrid = temp;
        }

        // --- RENDER MONOCHROME DECAY TO CANVAS ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int offset = x + WIDTH * y;
                int age = cellAge[y][x];
                String colorCode;
                char glyph;

                if (age > 0) {
                    glyph = '█'; // Filled uniform block
                    
                    // Specific brightness percentages matching Conway Turns
                    int brightness;
                    if (age == 1)       brightness = 255; // 100% Brightness
                    else if (age == 2)  brightness = 229; // 90%
                    else if (age == 3)  brightness = 209; // 82%
                    else if (age == 4)  brightness = 193; // 76%
                    else if (age == 5)  brightness = 181; // 71%
                    else if (age == 6)  brightness = 170; // 67%
                    else                brightness = 163; // 64% Static Lock
                    
                    colorCode = String.format("\u001B[38;2;%d;%d;%dm", brightness, brightness, brightness);
                } else {
                    glyph = ' '; // Absolute empty space vacuum void
                    colorCode = "";
                }

                outputBuffer[offset] = colorCode + glyph + RESET;
            }
        }
    }
}
