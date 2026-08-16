import java.util.PriorityQueue;
import java.util.Stack;
import java.util.Arrays;

public class AStarMazeSolverLoader extends Loader {
    private static final StatusStage[] MAZE_STAGES = {
        new StatusStage(15, "Generating labyrinth path walls:"),
        new StatusStage(40, "Sealing cul-de-sacs:"),
        new StatusStage(75, "Injecting A* Heuristic solver:"),
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
    
    // A* State Metrics
    private PriorityQueue<Cell> openSet;
    private Cell[][] parent; 
    private int[][] gCost; // Exact cost tracking from origin
    private Cell currentProcessingCell = null; 
    
    private boolean mazeSolved = false;
    private int solvedFramesCount = 0;
    private static final int RESET_DELAY_FRAMES = 120;

    private static final int[] DR = { -1, 1, 0, 0 };
    private static final int[] DC = { 0, 0, -1, 1 };

    // Custom A* Palette Tokens (All 2 Characters Wide)
    private static final String COLOR_WALL = "\u001B[48;2;30;86;49m  \u001B[0m";       
    private static final String COLOR_PATH = "\u001B[48;2;68;68;18m  \u001B[0m";       
    private static final String COLOR_CURRENT = "\u001B[48;2;255;255;25m\u001B[30m[]\u001B[0m"; 
    private static final String COLOR_SOLUTION = "\u001B[48;2;215;235;4m  \u001B[0m";   
    private static final String COLOR_GOAL = "\u001B[48;2;255;95;31m⚑⚑\u001B[0m";       

    // Dynamic gradient palette container (Fiery Amber Blend)
    private String[] trailColors;

    private static class Cell implements Comparable<Cell> {
        int r, c;
        int fCost; // Priority Metric (gCost + hCost)

        Cell(int r, int c, int fCost) {
            this.r = r;
            this.c = c;
            this.fCost = fCost;
        }

        @Override
        public int compareTo(Cell other) {
            return Integer.compare(this.fCost, other.fCost);
        }
    }

    public AStarMazeSolverLoader() {
        super(MAZE_STAGES, 80, 22);
    }

    public AStarMazeSolverLoader(int w, int h) {
        super(MAZE_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        this.cols = window_width / 2;
        this.rows = window_height;
        if (this.cols % 2 == 0) this.cols--;
        if (this.rows % 2 == 0) this.rows--;
        this.grid = new byte[rows][cols];

        generateGradientPalette();
        resetAndGenerateNewMaze();
    }

    private void generateGradientPalette() {
        this.trailColors = new String[TRAIL_LENGTH + 1];
        
        int startR = 20, startG = 50, startB = 15; 
        int endR = 205; int endG = 230; int endB = 25; 

        for (int i = 0; i <= TRAIL_LENGTH; i++) {
            double ratio = (double) i / TRAIL_LENGTH;
            int r = (int) (startR + (endR - startR) * ratio);
            int g = (int) (startG + (endG - startG) * ratio);
            int b = (int) (startB + (endB - startB) * ratio);
            
            trailColors[i] = "\u001B[48;2;" + r + ";" + g + ";" + b + "m  \u001B[0m";
        }
    }

    private int getManhattanHeuristic(int r, int c) {
        // Distance calculation to bottom-right exit node
        return Math.abs(r - (rows - 2)) + Math.abs(c - (cols - 2));
    }

    private void resetAndGenerateNewMaze() {
        this.mazeSolved = false;
        this.solvedFramesCount = 0;
        generateRandomMaze();
        
        this.visited = new boolean[rows][cols];
        this.trailIntensity = new int[rows][cols];
        this.parent = new Cell[rows][cols];
        this.gCost = new int[rows][cols];
        
        for (int r = 0; r < rows; r++) {
            Arrays.fill(gCost[r], Integer.MAX_VALUE);
        }

        this.openSet = new PriorityQueue<>();
        
        int startR = 1, startC = 1;
        gCost[startR][startC] = 0;
        int initialFCost = getManhattanHeuristic(startR, startC);
        
        Cell startCell = new Cell(startR, startC, initialFCost);
        this.openSet.add(startCell);
        this.visited[startR][startC] = true;
        this.trailIntensity[startR][startC] = TRAIL_LENGTH;
        this.currentProcessingCell = startCell;
    }

    private void generateRandomMaze() {
        for (int r = 0; r < rows; r++) {
            Arrays.fill(grid[r], (byte) 0);
        }
        Stack<Cell> generationStack = new Stack<>();
        boolean[][] genVisited = new boolean[rows][cols];
        Cell start = new Cell(1, 1, 0);
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
                generationStack.push(new Cell(targetR, targetC, 0));
            } else {
                generationStack.pop();
            }
        }
        grid[rows - 2][cols - 2] = 1;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // --- STEP 1: EXECUTE ONE STEP OF HEURISTIC HEAVY EVALUATION ---
        if (mazeSolved) {
            solvedFramesCount++;
            if (solvedFramesCount >= RESET_DELAY_FRAMES) {
                resetAndGenerateNewMaze();
            }
        } else if (!openSet.isEmpty()) {
            decayTrails();
            
            // Extract node with lowest fCost from the prioritized system
            Cell current = openSet.poll();
            currentProcessingCell = current;

            if (current.r == rows - 2 && current.c == cols - 2) {
                mazeSolved = true;
            } else {
                // Investigate and branch into neighbors
                for (int i = 0; i < 4; i++) {
                    int nr = current.r + DR[i];
                    int nc = current.c + DC[i];

                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] == 1) {
                        int tentativeGCost = gCost[current.r][current.c] + 1;

                        if (tentativeGCost < gCost[nr][nc]) {
                            gCost[nr][nc] = tentativeGCost;
                            int fCost = tentativeGCost + getManhattanHeuristic(nr, nc);
                            
                            parent[nr][nc] = current;
                            visited[nr][nc] = true;
                            trailIntensity[nr][nc] = TRAIL_LENGTH;
                            
                            openSet.add(new Cell(nr, nc, fCost));
                        }
                    }
                }
            }
        }

        // --- STEP 2: CHARACTER STRING PACKING VIA MATRIX OFFSET MAP ---
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
                } else if (!mazeSolved && currentProcessingCell != null && currentProcessingCell.r == r && currentProcessingCell.c == c) {
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
        return (r == 1 && c == 1);
    }
}
