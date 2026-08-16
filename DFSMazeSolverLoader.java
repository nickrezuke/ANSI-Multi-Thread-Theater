import java.util.Stack;
import java.util.Arrays;

public class DFSMazeSolverLoader extends Loader {
    private static final StatusStage[] MAZE_STAGES = {
        new StatusStage(15, "Generating labyrinth path walls:"),
        new StatusStage(40, "Sealing cul-de-sacs:"),
        new StatusStage(75, "Injecting pathfinding solver:"),
        new StatusStage(100, "Maze Solved Successfully!")
    };

    // --- ADJUST TRAIL LIFESPAN HERE ---
    private static final int TRAIL_LENGTH = 16; 

    private int rows;
    private int cols;
    private byte[][] grid;
    
    // Tracks trail life: 0 = unvisited/fully faded, TRAIL_LENGTH = fresh step
    private int[][] trailIntensity; 
    private boolean[][] visited; 
    
    private Stack<Cell> dfsStack;
    private boolean mazeSolved = false;

    private int solvedFramesCount = 0;
    private static final int RESET_DELAY_FRAMES = 120;

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    // Static colors using 24-bit Truecolor formats for pixel perfect consistency
    private static final String COLOR_WALL = "\u001B[48;2;45;45;45m  \u001B[0m"; 
    private static final String COLOR_PATH = "\u001B[48;2;15;15;15m  \u001B[0m"; 
    private static final String COLOR_CURRENT = "\u001B[48;2;255;0;0m[]\u001B[0m"; 
    private static final String COLOR_SOLUTION = "\u001B[48;2;50;255;50m  \u001B[0m"; 
    private static final String COLOR_GOAL = "\u001B[48;2;255;215;0m⚑⚑\u001B[0m";

    // Dynamic gradient palette container
    private String[] trailColors;

    private static class Cell {
        int r, c;
        Cell(int r, int c) { this.r = r; this.c = c; }
    }

    public DFSMazeSolverLoader() { super(MAZE_STAGES, 80, 22); }
    public DFSMazeSolverLoader(int w, int h) { super(MAZE_STAGES, w, h); }

    @Override
    protected void initialize() {
        this.cols = window_width / 2;
        this.rows = window_height;
        if (this.cols % 2 == 0) this.cols--;
        if (this.rows % 2 == 0) this.rows--;
        this.grid = new byte[rows][cols];

        // Generate the blended color palette dynamically
        generateGradientPalette();

        resetAndGenerateNewMaze();
    }

    /**
     * Linearly interpolates RGB components from Pure Blue to Pure Red.
     * Guarantees zero rainbow artifacting or secondary color leaks.
     */
    private void generateGradientPalette() {
        this.trailColors = new String[TRAIL_LENGTH + 1];
        
        // Define explicit RGB bounds for the mix
        int startR = 0,   startG = 0, startB = 255; // Intensity 0: Base Visited Blue
        int endR = 255,   endG = 0,   endB = 0;   // Intensity MAX: Matches Pointer Red
        
        for (int i = 0; i <= TRAIL_LENGTH; i++) {
            double ratio = (double) i / TRAIL_LENGTH;
            
            // Linear mathematical mix (lerp) between start and end vectors
            int r = (int) (startR + (endR - startR) * ratio);
            int g = (int) (startG + (endG - startG) * ratio);
            int b = (int) (startB + (endB - startB) * ratio);
            
            // Format to standard 24-bit Truecolor escape string
            trailColors[i] = "\u001B[48;2;" + r + ";" + g + ";" + b + "m  \u001B[0m";
        }
    }

    private void resetAndGenerateNewMaze() {
        this.mazeSolved = false;
        this.solvedFramesCount = 0;
        generateRandomMaze();
        this.visited = new boolean[rows][cols];
        this.trailIntensity = new int[rows][cols]; 
        this.dfsStack = new Stack<>();
        
        Cell startCell = new Cell(1, 1);
        this.dfsStack.push(startCell);
        this.visited[1][1] = true;
        this.trailIntensity[1][1] = TRAIL_LENGTH; 
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
        // --- STEP 1: EXECUTE ATOMIC SOLVER STEP OR TICK THE RESET ENGINE ---
        if (mazeSolved) {
            solvedFramesCount++;
            if (solvedFramesCount >= RESET_DELAY_FRAMES) {
                resetAndGenerateNewMaze();
            }
        } else if (!dfsStack.isEmpty()) {
            decayTrails();

            Cell current = dfsStack.peek();
            trailIntensity[current.r][current.c] = TRAIL_LENGTH;

            if (current.r == rows - 2 && current.c == cols - 2) {
                mazeSolved = true;
            } else {
                boolean shiftedForward = false;
                for (int i = 0; i < 4; i++) {
                    int nr = current.r + DR[i];
                    int nc = current.c + DC[i];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        if (grid[nr][nc] == 1 && !visited[nr][nc]) {
                            visited[nr][nc] = true;
                            dfsStack.push(new Cell(nr, nc));
                            trailIntensity[nr][nc] = TRAIL_LENGTH; 
                            shiftedForward = true;
                            break;
                        }
                    }
                }
                if (!shiftedForward) {
                    dfsStack.pop(); 
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
                } else if (!dfsStack.isEmpty() && dfsStack.peek().r == r && dfsStack.peek().c == c) {
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
        for (Cell cell : dfsStack) {
            if (cell.r == r && cell.c == c) return true;
        }
        return false;
    }
}
