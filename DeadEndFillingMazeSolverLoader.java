import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.Arrays;

public class DeadEndFillingMazeSolverLoader extends Loader {
    private static final StatusStage[] MAZE_STAGES = {
            new StatusStage(15, "Generating labyrinth path walls:"),
            new StatusStage(40, "Sealing cul-de-sacs:"),
            new StatusStage(75, "Injecting Dead-End structural solver:"),
            new StatusStage(100, "Maze Solved Successfully!")
    };

    // --- ADJUST TRAIL LIFESPAN FOR THE WALL CRUMBLING EFFECT ---
    private static final int TRAIL_LENGTH = 40;

    private int rows;
    private int cols;
    private byte[][] grid; // 0 = Wall, 1 = Path

    // Tracks crumbling wall animation: 0 = stable wall, TRAIL_LENGTH = fresh
    // collapse
    private int[][] wallCrumbleIntensity;
    private boolean[][] wasEaten;

    // Two-tier Queue System for layered wave animations
    private Queue<Cell> activePhaseQueue;
    private Queue<Cell> nextPhaseQueue;
    private Cell currentEatingCell = null;

    private boolean mazeSolved = false;
    private int solvedFramesCount = 0;
    private static final int RESET_DELAY_FRAMES = 120;

    private static final int[] DR = { -1, 1, 0, 0 };
    private static final int[] DC = { 0, 0, -1, 1 };

    // Unique Color Tokens (All 2 Characters Wide)
    private static final String COLOR_WALL = "\u001B[48;2;37;37;37m  \u001B[0m";
    private static final String COLOR_PATH = "\u001B[48;2;10;10;10m  \u001B[0m";
    private static final String COLOR_CURRENT = "\u001B[48;2;255;255;0m\u001B[30m[]\u001B[0m";
    private static final String COLOR_SOLUTION = "\u001B[48;2;195;225;5m  \u001B[0m";
    private static final String COLOR_GOAL = "\u001B[48;2;195;225;155m⚑⚑\u001B[0m";

    // Magma crumble gradient palette
    private String[] crumbleColors;

    private static class Cell {
        int r, c;

        Cell(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    public DeadEndFillingMazeSolverLoader() {
        super(MAZE_STAGES, 80, 22);
    }

    public DeadEndFillingMazeSolverLoader(int w, int h) {
        super(MAZE_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        this.cols = window_width / 2;
        this.rows = window_height;
        if (this.cols % 2 == 0)
            this.cols--;
        if (this.rows % 2 == 0)
            this.rows--;
        this.grid = new byte[rows][cols];

        generateGradientPalette();
        resetAndGenerateNewMaze();
    }

    /**
     * Linearly interpolates RGB components across a hot magma/rust spectrum
     * to visualize walls actively collapsing.
     */
    private void generateGradientPalette() {
        this.crumbleColors = new String[TRAIL_LENGTH + 1];

        // Faded Rust/Slate matching our base wall color
        int startR = 45, startG = 40, startB = 40;
        // Fresh molten lava orange-red
        int endR = 230;
        int endG = 50;
        int endB = 20;

        for (int i = 0; i <= TRAIL_LENGTH; i++) {
            double ratio = (double) i / TRAIL_LENGTH;
            int r = (int) (startR + (endR - startR) * ratio);
            int g = (int) (startG + (endG - startG) * ratio);
            int b = (int) (startB + (endB - startB) * ratio);

            crumbleColors[i] = "\u001B[48;2;" + r + ";" + g + ";" + b + "m  \u001B[0m";
        }
    }

    private void resetAndGenerateNewMaze() {
        this.mazeSolved = false;
        this.solvedFramesCount = 0;
        this.currentEatingCell = null;

        generateRandomMaze();

        this.wallCrumbleIntensity = new int[rows][cols];
        this.wasEaten = new boolean[rows][cols];
        this.activePhaseQueue = new LinkedList<>();
        this.nextPhaseQueue = new LinkedList<>();

        // Phase 1: Populate active queue with ALL initial dead ends in the maze
        populateInitialDeadEnds();
    }

    private void populateInitialDeadEnds() {
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                // Do not eat the entrance or exit nodes
                if ((r == 1 && c == 1) || (r == rows - 2 && c == cols - 2)) {
                    continue;
                }
                if (grid[r][c] == 1 && countOpenNeighbors(r, c) <= 1) {
                    activePhaseQueue.add(new Cell(r, c));
                }
            }
        }
    }

    private int countOpenNeighbors(int r, int c) {
        int openCount = 0;
        for (int i = 0; i < 4; i++) {
            int nr = r + DR[i];
            int nc = c + DC[i];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                if (grid[nr][nc] == 1) {
                    openCount++;
                }
            }
        }
        return openCount;
    }

    private void generateRandomMaze() {
        for (int r = 0; r < rows; r++) {
            Arrays.fill(grid[r], (byte) 0);
        }
        Stack<Cell> generationStack = new Stack<>();
        boolean[][] genVisited = new boolean[rows][cols];
        Cell start = new Cell(1, 1);
        generationStack.push(start);
        genVisited[1][1] = true;
        grid[1][1] = 1;
        java.util.Random rand = new java.util.Random();

        while (!generationStack.isEmpty()) {
            Cell current = generationStack.peek();
            java.util.List<Integer> validNeighbors = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int nr = current.r + DR[i] * 2;
                int nc = current.c + DC[i] * 2;
                if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && !genVisited[nr][nc]) {
                    validNeighbors.add(i);
                }
            }
            if (!validNeighbors.isEmpty()) {
                int dirIdx = validNeighbors.get(rand.nextInt(validNeighbors.size()));
                int wallR = current.r + DR[dirIdx];
                int wallC = current.c + DC[dirIdx];
                int targetR = current.r + DR[dirIdx] * 2;
                int targetC = current.c + DC[dirIdx] * 2;
                grid[wallR][wallC] = 1;
                grid[targetR][targetC] = 1;
                genVisited[targetR][targetC] = true;
                generationStack.push(new Cell(targetR, targetC));
            } else {
                generationStack.pop();
            }
        }
        grid[rows - 2][cols - 2] = 1;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // --- STEP 1: SOLVER LOGIC (EAT ONE TILE PER FRAME) ---
        decayCrumbleTrails();

        if (mazeSolved) {
            solvedFramesCount++;
            if (solvedFramesCount >= RESET_DELAY_FRAMES) {
                resetAndGenerateNewMaze();
            }
        } else {
            // Check if current layer batch is spent, if so shift secondary wave in
            if (activePhaseQueue.isEmpty() && !nextPhaseQueue.isEmpty()) {
                while (!nextPhaseQueue.isEmpty()) {
                    activePhaseQueue.add(nextPhaseQueue.poll());
                }
            }

            if (!activePhaseQueue.isEmpty()) {
                Cell target = activePhaseQueue.poll();

                // Confirm it hasn't already been eaten and is still technically a dead end
                if (grid[target.r][target.c] == 1 && countOpenNeighbors(target.r, target.c) <= 1) {

                    // Turn corridor into a solid wall
                    grid[target.r][target.c] = 0;
                    wasEaten[target.r][target.c] = true;
                    wallCrumbleIntensity[target.r][target.c] = TRAIL_LENGTH;
                    currentEatingCell = target;

                    // Instantly check neighbors to see if this collapse created a NEW dead end
                    for (int i = 0; i < 4; i++) {
                        int nr = target.r + DR[i];
                        int nc = target.c + DC[i];

                        if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1) {
                            // Don't add structural entrance or exit locations
                            if ((nr == 1 && nc == 1) || (nr == rows - 2 && nc == cols - 2)) {
                                continue;
                            }
                            if (grid[nr][nc] == 1 && countOpenNeighbors(nr, nc) <= 1) {
                                nextPhaseQueue.add(new Cell(nr, nc));
                            }
                        }
                    }
                }
            } else {
                // Queues are dead empty, meaning only structural solution trails survive
                currentEatingCell = null;
                mazeSolved = true;
            }
        }

        // --- STEP 2: CHARACTER MAP RENDER PACKING ---
        Arrays.fill(outputBuffer, "");
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String renderToken;

                if (r == rows - 2 && c == cols - 2) {
                    renderToken = COLOR_GOAL;
                } else if (!mazeSolved && currentEatingCell != null && currentEatingCell.r == r
                        && currentEatingCell.c == c) {
                    renderToken = COLOR_CURRENT;
                } else if (grid[r][c] == 0) {
                    // Check if it's a structural wall or a newly eaten crumbling tile
                    if (wasEaten[r][c] && wallCrumbleIntensity[r][c] > 0) {
                        int intensity = wallCrumbleIntensity[r][c];
                        renderToken = crumbleColors[intensity];
                    } else {
                        renderToken = COLOR_WALL;
                    }
                } else {
                    // If the maze is solved, remaining path tiles flash with the solution color!
                    if (mazeSolved) {
                        renderToken = COLOR_SOLUTION;
                    } else {
                        renderToken = COLOR_PATH;
                    }
                }
                int bufferOffset = (c * 2) + (window_width * r);
                if (bufferOffset >= 0 && bufferOffset + 1 < outputBuffer.length) {
                    outputBuffer[bufferOffset] = renderToken;
                    outputBuffer[bufferOffset + 1] = "";
                }
            }
        }
    }

    private void decayCrumbleTrails() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (wallCrumbleIntensity[r][c] > 0) {
                    wallCrumbleIntensity[r][c]--;
                }
            }
        }
    }
}