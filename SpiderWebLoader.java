// TODO: Improve this Spider Web simulator its prettu bad rn

import java.util.Random;

public class SpiderWebLoader extends Loader {
    private static final StatusStage[] SIM_STAGES = {
        new StatusStage(25, "Deploying anchor state matrices:"),
        new StatusStage(50, "Extruding radial thread agents:"),
        new StatusStage(75, "Simulating sticky spiral loops:"),
        new StatusStage(100, "Automata Grid System Balanced!")
    };

    private final int width = 80;
    private final int height = 22;
    private final int totalSize = 1760;

    // Simulation grid buffers containing state values
    // 0 = Empty space, 1 = Frame thread, 2 = Radial thread, 3 = Sticky spiral thread
    private final int[] gridState = new int[totalSize];
    private final Random rand = new Random();

    // Spider Agent Parameters
    private int spiderX;
    private int spiderY;
    private int currentPhase; // 0 = Frame, 1 = Radials, 2 = Spiral
    private int internalTicks;
    private double currentRadius;
    private double currentAngle;
    private int radialIndex;

    public SpiderWebLoader() {
        super(SIM_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // Reset the environment matrix
        for (int i = 0; i < totalSize; i++) {
            gridState[i] = 0;
        }
        
        // Spawn spider at the central hub point
        this.spiderX = width / 2;
        this.spiderY = height / 2;
        this.currentPhase = 0;
        this.internalTicks = 0;
        this.radialIndex = 0;
        this.currentRadius = 1.0;
        this.currentAngle = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        internalTicks++;

        // --- STEP 1: ADVANCE SPIDER BEHAVIORAL AGENT STATE MACHINE ---
        // Run several iteration cycles per frame to give the spider quick movement
        for (int cycle = 0; cycle < 6; cycle++) {
            if (currentPhase == 0) {
                // PHASE 0: Weave the 4 Corner Outer Anchor Borders
                if (internalTicks < 45) {
                    // Trace a box outwards toward boundaries
                    int tx = width / 2 + (int)(15.0 * Math.sin(internalTicks * 0.4) * 2.3);
                    int ty = height / 2 + (int)(8.0 * Math.cos(internalTicks * 0.4));
                    
                    tx = Math.max(2, Math.min(width - 3, tx));
                    ty = Math.max(1, Math.min(height - 2, ty));
                    
                    gridState[tx + ty * width] = 1; // Mark frame line state
                    this.spiderX = tx;
                    this.spiderY = ty;
                } else {
                    currentPhase = 1; // Transition to spokes
                }
            } 
            else if (currentPhase == 1) {
                // PHASE 1: Weave Radial Spokes from Hub to Outer Frame Boundary
                int totalSpokes = 16;
                if (radialIndex < totalSpokes) {
                    double angle = (radialIndex * 2.0 * Math.PI) / totalSpokes;
                    
                    // Travel outward along target ray direction angle
                    double t = (double) (internalTicks % 20) / 20.0;
                    int tx = width / 2 + (int) (Math.cos(angle) * 34.0 * 2.3 * t);
                    int ty = height / 2 + (int) (Math.sin(angle) * 10.0 * t);
                    
                    tx = Math.max(0, Math.min(width - 1, tx));
                    ty = Math.max(0, Math.min(height - 1, ty));
                    
                    gridState[tx + ty * width] = 2; // Mark radial line state
                    this.spiderX = tx;
                    this.spiderY = ty;

                    if (internalTicks % 20 == 19) {
                        radialIndex++; // Lock spoke thread, skip to next direction ray
                    }
                } else {
                    currentPhase = 2; // Transition to core capture spirals
                    this.currentRadius = 32.0; // Start spinning from outside in
                }
            } 
            else if (currentPhase == 2) {
                // PHASE 2: Step along Logarithmic Path to Weave Sticky Capture Rings
                if (currentRadius > 1.5) {
                    // Deconstruct step trajectory vectors using circular calculations
                    currentAngle += 0.08;
                    currentRadius -= 0.015; // Spiral loops tighten inward
                    
                    int tx = width / 2 + (int) (Math.cos(currentAngle) * currentRadius * 2.3);
                    int ty = height / 2 + (int) (Math.sin(currentAngle) * currentRadius * 0.32);
                    
                    if (tx >= 0 && tx < width && ty >= 0 && ty < height) {
                        gridState[tx + ty * width] = 3; // Mark sticky silk cell state
                        this.spiderX = tx;
                        this.spiderY = ty;
                    }
                } else {
                    // Loop execution bounds: Web complete, trigger rebuild respawn reset
                    if (rand.nextDouble() < 0.01) {
                        initialize();
                    }
                }
            }
        }

        // --- STEP 2: RASTERIZE CELLULAR BUFFER STATES TO OUTPUT ---
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                int state = gridState[idx];

                // Render matching color profiles depending on active cell conditions
                if (x == spiderX && y == spiderY) {
                    // High-contrast blinking neon color code representing active spider location
                    outputBuffer[idx] = "\u001B[38;2;255;50;50m█\u001B[0m"; 
                    zBuffer[idx] = 1.0;
                } else if (state == 1) {
                    // Frame Anchor Strut (Heavy Wood Brown/Grey)
                    outputBuffer[idx] = "\u001B[38;2;100;105;110m#\u001B[0m";
                    zBuffer[idx] = 0.5;
                } else if (state == 2) {
                    // Radial Spokes Thread (Pale Silver Dry Silk Filament)
                    outputBuffer[idx] = "\u001B[38;2;160;165;170m·\u001B[0m";
                    zBuffer[idx] = 0.6;
                } else if (state == 3) {
                    // Active Glistening Sticky Capture Spiral Loop Segment
                    double shift = Math.sin((x * 0.2) + (y * 0.4) + (internalTicks * 0.1));
                    if (shift > 0.6) {
                        outputBuffer[idx] = "\u001B[38;2;100;220;255m¤\u001B[0m"; // Dewdrop node highlight
                    } else {
                        outputBuffer[idx] = "\u001B[38;2;130;185;200m-\u001B[0m"; // Connected weave bridge
                    }
                    zBuffer[idx] = 0.7;
                } else {
                    // Empty negative space air environment dots
                    if ((x + y * 7) % 31 == 0 && (x * y) % 5 == 2) {
                        outputBuffer[idx] = "\u001B[38;2;50;55;60m.\u001B[0m";
                    } else {
                        outputBuffer[idx] = " ";
                    }
                    zBuffer[idx] = 0.0;
                }
            }
        }
    }
}
