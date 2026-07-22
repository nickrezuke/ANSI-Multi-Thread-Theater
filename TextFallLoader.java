import java.util.Arrays;

public class TextFallLoader extends Loader {
    private static final StatusStage[] TEXTFALL_STAGES = {
        new StatusStage(15, "Initializing construct:"),
        new StatusStage(40, "Establishing secure proxy:"),
        new StatusStage(65, "Bypassing mainframe firewall:"),
        new StatusStage(85, "Injecting digital rain vectors:"),
        new StatusStage(100, "System Override Complete!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;
    private static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789$+-*/%=<>!&_#@";

    // Track stream states for all 80 terminal columns
    private final double[] heads = new double[WIDTH];
    private final double[] speeds = new double[WIDTH];
    private final int[] lengths = new int[WIDTH];

    // Extended ANSI colors for smooth gradient fading
    private static final String COLOR_HEAD = "\u001B[38;5;231m";  // Pure White
    private static final String COLOR_HIGH = "\u001B[38;5;46m";   // Bright Green
    private static final String COLOR_MID = "\u001B[38;5;28m";    // Medium Green
    private static final String COLOR_LOW = "\u001B[38;5;22m";    // Dark/Fading Green

    public TextFallLoader() {
        super(TEXTFALL_STAGES);
    }

    @Override
    protected void initialize() {
        // Randomly stagger the starting positions and speeds of each stream
        for (int i = 0; i < WIDTH; i++) {
            heads[i] = -Math.random() * HEIGHT; 
            speeds[i] = 0.05 + Math.random() * 0.05;
            lengths[i] = 6 + (int) (Math.random() * 12);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        for (int x = 0; x < WIDTH; x++) {
            // Drop down the stream head position
            heads[x] += speeds[x];

            // Reset column stream if the trail completely leaves the screen view
            if (heads[x] - lengths[x] > HEIGHT) {
                heads[x] = -Math.random() * 5;
                speeds[x] = 0.25 + Math.random() * 0.55;
                lengths[x] = 6 + (int) (Math.random() * 12);
            }

            int currentHeadY = (int) heads[x];

            // Render active parts of the rain stream within the console bounds
            for (int y = 0; y < HEIGHT; y++) {
                if (y <= currentHeadY && y > currentHeadY - lengths[x]) {
                    int o = x + WIDTH * y;
                    
                    // Simple constant depth value for 2D geometry overlay
                    double pseudoDepth = 1.0; 

                    if (o >= 0 && o < outputBuffer.length && pseudoDepth > zBuffer[o]) {
                        zBuffer[o] = pseudoDepth;

                        // Shimmer effect: rapidly cycle characters every single frame
                        char activeGlyph = GLYPHS.charAt((int) (Math.random() * GLYPHS.length()));
                        
                        // Select color based on distance from the stream head
                        String chosenColor;
                        int trailingDistance = currentHeadY - y;

                        if (trailingDistance == 0) {
                            chosenColor = COLOR_HEAD; // Leading white pulse
                        } else if (trailingDistance < 3) {
                            chosenColor = COLOR_HIGH; // Vibrant green close to the tip
                        } else if (trailingDistance < lengths[x] * 0.6) {
                            chosenColor = COLOR_MID;  // Fading center body
                        } else {
                            chosenColor = COLOR_LOW;  // Dim trailing edge
                        }

                        outputBuffer[o] = chosenColor + activeGlyph + RESET;
                    }
                }
            }
        }
    }
}
