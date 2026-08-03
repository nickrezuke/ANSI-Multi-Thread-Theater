public class LangtonsAntLoader extends Loader {
    private static final StatusStage[] ANT_STAGES = {
        new StatusStage(25, "Initializing infinite grid state arrays:"),
        new StatusStage(50, "Calibrating relative cellular matrices:"),
        new StatusStage(75, "Positioning automated worker vectors:"),
        new StatusStage(100, "Langton's Ant Simulator Operational!")
    };

    // 1. Grid Structural Boundaries
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;
    private static final int SIZE = WIDTH * HEIGHT;

    // 2. Automaton State Buffers
    // false = White cell, true = Black cell
    private final boolean[] grid = new boolean[SIZE];
    
    // Ant position and orientation tracking
    private int antX;
    private int antY;
    private int antDir = 0; // 0 = Up, 1 = Right, 2 = Down, 3 = Left

    private long lastTickTime = 0;
    private static final long STEP_DELAY_MS = 15; // Fast iteration speed to observe highway patterns quickly

    // 3. Interface Styling Palettes
    private static final String COLOR_WHITE_CELL = "\u001B[48;5;235m\u001B[38;5;240m"; // Dark backdrop grid cells
    private static final String COLOR_BLACK_CELL = "\u001B[48;5;250m\u001B[38;5;255m"; // Bright inverted flipped cells
    private static final String COLOR_ANT = "\u001B[38;5;196;1m";                     // Vibrant Red active ant worker

    public LangtonsAntLoader() {
        super(ANT_STAGES);
    }

    @Override
    protected void initialize() {
        resetSimulation();
    }

    private void resetSimulation() {
        // Clear board fields to initial pristine state
        for (int i = 0; i < SIZE; i++) {
            grid[i] = false;
        }
        
        // Spawn the ant exactly in the geometric center of the canvas
        antX = WIDTH / 2;
        antY = HEIGHT / 2;
        antDir = 0; // Pointing upwards initially
        
        lastTickTime = System.currentTimeMillis();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        // Step cellular state rules forward continuously on frame intervals
        if (currentTime - lastTickTime >= STEP_DELAY_MS) {
            lastTickTime = currentTime;
            executeAntRules();
        }

        drawScene(outputBuffer);
    }

    private void executeAntRules() {
        int idx = antX + antY * WIDTH;
        
        // Safety wrap validation constraint: if ant climbs off canvas edge limits, loop boundaries safely
        if (idx < 0 || idx >= SIZE) {
            resetSimulation();
            return;
        }

        // --- Core Universal Turing Machine Logic Rules ---
        if (!grid[idx]) {
            // Rule 1: White Cell -> Turn Right, Flip cell color to Black
            antDir = (antDir + 1) % 4;
            grid[idx] = true;
        } else {
            // Rule 2: Black Cell -> Turn Left, Flip cell color back to White
            antDir = (antDir + 3) % 4;
            grid[idx] = false;
        }

        // Advance worker position one square forward depending on its active angle vector
        switch (antDir) {
            case 0: antY--; break; // Up
            case 1: antX++; break; // Right
            case 2: antY++; break; // Down
            case 3: antX--; break; // Left
        }

        // Out-of-bounds protection listener trigger: reset clean loop if boundary is reached
        if (antX < 0 || antX >= WIDTH || antY < 0 || antY >= HEIGHT) {
            resetSimulation();
        }
    }

    private void drawScene(String[] outputBuffer) {
        // 1. Rasterize cell matrix configurations sequentially
        for (int i = 0; i < SIZE; i++) {
            if (grid[i]) {
                outputBuffer[i] = COLOR_BLACK_CELL + "█" + RESET; // Solid block for flipped matrices
            } else {
                outputBuffer[i] = COLOR_WHITE_CELL + "·" + RESET; // Subtle dot indicator for blank fields
            }
        }

        // 2. Overlay active Ant coordinate position cell index
        int antIdx = antX + antY * WIDTH;
        if (antIdx >= 0 && antIdx < SIZE) {
            // Assign clear direction pointers matching active orientations
            char glyph = '▲';
            switch (antDir) {
                case 0: glyph = '▲'; break;
                case 1: glyph = '►'; break;
                case 2: glyph = '▼'; break;
                case 3: glyph = '◄'; break;
            }

            // Determine parent background coloration to prevent graphic cell tearing
            String currentBG = grid[antIdx] ? COLOR_BLACK_CELL : COLOR_WHITE_CELL;
            outputBuffer[antIdx] = currentBG + COLOR_ANT + glyph + RESET;
        }
    }
}
