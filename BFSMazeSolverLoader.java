import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.Arrays;

public class BFSMazeSolverLoader extends Loader {
    private static final StatusStage[] MAZE_STAGES = {
            new StatusStage(15, "Generating labyrinth path walls:"),
            new StatusStage(40, "Sealing cul-de-sacs:"),
            new StatusStage(75, "Injecting BFS pathfinding solver:"),
            new StatusStage(100, "Maze Solved Successfully!")
    };

    // --- ADJUST TRAIL LIFESPAN HERE ---
    private static final int TRAIL_LENGTH = 40;

    private int rows;
    private int cols;
    private byte[][] grid;

    // Tracks trail life: 0 = unvisited/fully faded, TRAIL_LENGTH = fresh step
    private int[][] trailIntensity;
    private boolean[][] visited;

    // BFS State
    private Queue<Cell> bfsQueue;
    private Cell[][] parent; // Required to re-render the final shortest path
    private Cell currentProcessingCell = null; // Currently active branching node

    private boolean mazeSolved = false;
    private int solvedFramesCount = 0;
    private static final int RESET_DELAY_FRAMES = 120;

    private static final int[] DR = { -1, 1, 0, 0 };
    private static final int[] DC = { 0, 0, -1, 1 };

    // Static colors using 24-bit Truecolor formats for pixel perfect consistency
    private static final String COLOR_WALL = "\u001B[48;2;180;40;40m  \u001B[0m";
    private static final String COLOR_PATH = "\u001B[48;2;15;15;15m  \u001B[0m";

    private static final String COLOR_CURRENT = "\u001B[48;2;40;220;40m\u001B[30m[]\u001B[0m";

    private static final String COLOR_SOLUTION = "\u001B[48;2;0;225;255m  \u001B[0m";
    private static final String COLOR_GOAL = "\u001B[48;2;100;200;255m⚑⚑\u001B[0m";

    // Dynamic gradient palette container
    private String[] trailColors;

    private static class Cell {
        int r, c;

        Cell(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    public BFSMazeSolverLoader() {
        super(MAZE_STAGES, 80, 22);
    }

    public BFSMazeSolverLoader(int w, int h) {
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

        // Generate the blended color palette dynamically
        generateGradientPalette();
        resetAndGenerateNewMaze();
    }

    /**
     * Linearly interpolates RGB components across a Purple spectrum.
     */
    private void generateGradientPalette() {
        this.trailColors = new String[TRAIL_LENGTH + 1];

        // Deep, dark purple for older, fading tracks
        int startR = 45, startG = 15, startB = 85;
        // Bright, vibrant purple/magenta for fresh steps
        int endR = 180;
        int endG = 30;
        int endB = 220;

        for (int i = 0; i <= TRAIL_LENGTH; i++) {
            double ratio = (double) i / TRAIL_LENGTH;
            int r = (int) (startR + (endR - startR) * ratio);
            int g = (int) (startG + (endG - startG) * ratio);
            int b = (int) (startB + (endB - startB) * ratio);

            // Formatted with two spaces to maintain aspect ratio
            trailColors[i] = "\u001B[48;2;" + r + ";" + g + ";" + b + "m  \u001B[0m";
        }
    }

    private void resetAndGenerateNewMaze() {
        this.mazeSolved = false;
        this.solvedFramesCount = 0;
        generateRandomMaze();
        this.visited = new boolean[rows][cols];
        this.trailIntensity = new int[rows][cols];
        this.parent = new Cell[rows][cols];
        this.bfsQueue = new LinkedList<>();
        Cell startCell = new Cell(1, 1);
        this.bfsQueue.add(startCell);
        this.visited[1][1] = true;
        this.trailIntensity[1][1] = TRAIL_LENGTH;
        this.currentProcessingCell = startCell;
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
        // --- STEP 1: EXECUTE ONE BFS STEP PER FRAME ---
        if (mazeSolved) {
            solvedFramesCount++;
            if (solvedFramesCount >= RESET_DELAY_FRAMES) {
                resetAndGenerateNewMaze();
            }
        } else if (!bfsQueue.isEmpty()) {
            decayTrails();
            Cell current = bfsQueue.peek();
            currentProcessingCell = current;
            if (current.r == rows - 2 && current.c == cols - 2) {
                mazeSolved = true;
            } else {
                boolean actionTakenThisFrame = false;
                // Find exactly ONE valid neighbor to branch into this frame
                for (int i = 0; i < 4; i++) {
                    int nr = current.r + DR[i];
                    int nc = current.c + DC[i];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        if (grid[nr][nc] == 1 && !visited[nr][nc]) {
                            visited[nr][nc] = true;
                            trailIntensity[nr][nc] = TRAIL_LENGTH;
                            parent[nr][nc] = current;
                            bfsQueue.add(new Cell(nr, nc));
                            // Point the flashing pointer to the newly explored branch tile
                            currentProcessingCell = new Cell(nr, nc);
                            actionTakenThisFrame = true;
                            break;
                        }
                    }
                }
                // If the front node has no valid neighbors left, pop it out next frame
                if (!actionTakenThisFrame) {
                    bfsQueue.poll();
                }
            }
        }

        // --- STEP 2: TRANSLATE MAZE MATRIX TO STRING CHARACTER BUFFER ---
        Arrays.fill(outputBuffer, "");
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String renderToken;
                if (grid[r][c] == 0) {
                    renderToken = COLOR_WALL;
                } else if (r == rows - 2 && c == cols - 2) {
                    renderToken = COLOR_GOAL;
                } else if (mazeSolved && isPartOfSolutionPath(r, c)) {
                    renderToken = COLOR_SOLUTION;
                } else if (!mazeSolved && currentProcessingCell != null && currentProcessingCell.r == r
                        && currentProcessingCell.c == c) {
                    renderToken = COLOR_CURRENT;
                } else if (visited[r][c]) {
                    int intensity = trailIntensity[r][c];
                    renderToken = trailColors[intensity];
                } else {
                    renderToken = COLOR_PATH;
                }

                int bufferOffset = (c * 2) + (window_width * r);
                if (bufferOffset >= 0 && bufferOffset + 1 < outputBuffer.length) {
                    outputBuffer[bufferOffset] = renderToken;
                    outputBuffer[bufferOffset + 1] = "";
                }
            }
        }
    }

    private void decayTrails() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (trailIntensity[r][c] > 0) {
                    trailIntensity[r][c]--;
                }
            }
        }
    }

    private boolean isPartOfSolutionPath(int r, int c) {
        Cell curr = parent[rows - 2][cols - 2];
        while (curr != null) {
            if (curr.r == r && curr.c == c) {
                return true;
            }
            curr = parent[curr.r][curr.c];
        }
        return (r == 1 && c == 1); // Ensure start cell is included
    }
}
