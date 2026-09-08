// Andy's Fire https://www.a1k0n.net/2007/08/24/obfuscated-c-fire.html
public class AndysFireLoader extends Loader {
    private static final StatusStage[] FIRE_STAGES = {
        new StatusStage(15, "Igniting core thermal processes:"),
        new StatusStage(40, "Overriding cooling systems:"),
        new StatusStage(65, "Deploying algorithmic arson:"),
        new StatusStage(85, "Reaching critical mass:"),
        new StatusStage(100, "Thermal Roast Complete!")
    };

    private static final int WIDTH = 130;
    private static final int HEIGHT = 22;
    private static final int MAX_HEAT = 36;

    // Andy Sloane's secret: He used a 1D array so that the right edge of row 'y' 
    // seamlessly bleeds into the left edge of row 'y+1', saving code and creating wind.
    // We make it slightly larger than WIDTH * HEIGHT to act as the hidden "generator" rows.
    private final int[] b = new int[WIDTH * (HEIGHT + 2)];

    // 36-step character density ramp matching Andy's original string " .,-~:;=!*#$@"
    // 3 copies of each char for more range lmao
    private static final char[] FIRE_CHARS = 
        "   ...,,,---~~~:::;;;===!!!***###@@@".toCharArray();

    // 36-step ANSI truecolor gradient (Black -> Dark Red -> Orange -> Yellow -> White)
    private static final String[] FIRE_COLORS = new String[MAX_HEAT];

    static {
        // High-saturation 256-color palette mapped identically to the character array
        int[] paletteMap = {
            232, 232, 232, // Black / Empty space
            52,  52,  52,  // Dark Red
            88,  88,  88,  // Red
            124, 124, 124, // Bright Red
            160, 160, 160, // Orange-Red
            196, 196, 196, // Dark Orange
            202, 202, 202, // Orange
            208, 208, 208, // Bright Orange
            214, 214, 214, // Gold
            220, 220, 220, // Yellow
            226, 226, 226, // Bright Yellow
            231, 231, 231  // Blazing White
        };
        for (int i = 0; i < MAX_HEAT; i++) {
            FIRE_COLORS[i] = "\u001B[38;5;" + paletteMap[i] + "m";
        }
    }

    public AndysFireLoader() {
        super(FIRE_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        // Clear the entire continuous array buffer
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // 1. Seed the "hidden" bottom generator rows with chaotic heat.
        // The empty spaces (0) act as cold air pockets that rise up and naturally 
        // cool the fire via the division by 4 later, separating the flames.
        int bottomStart = WIDTH * HEIGHT;
        for (int x = 0; x < WIDTH; x++) {
            double rand = Math.random();
            int heat;
            
            if (rand > 0.45) {
                heat = MAX_HEAT - 1; // Solid white-hot base
            } else if (rand > 0.15) {
                heat = (int) (Math.random() * (MAX_HEAT / 2.0)); // Mid-level embers
            } else {
                heat = 0; // Cold fuel pockets (crucial for flame decay)
            }
            
            b[bottomStart + x] = heat;
            b[bottomStart + WIDTH + x] = heat;
        }

        // 2. Andy Sloane's famous obfuscated in-place 4-way box blur.
        // By pulling heat from the current pixel, the one right next to it, and 
        // the two directly below it, we drag the heat diagonally upwards.
        // Because of the 1D array wrap-around, it naturally creates a wind bias.
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            // b[i] = (b[i] + b[i+1] + b[i+80] + b[i+81]) / 4
            int avg = (b[i] + b[i + 1] + b[i + WIDTH] + b[i + WIDTH + 1]) / 4;
            
            // Integer division naturally truncates, fading the fire over time.
            b[i] = avg; 
        }

        // 3. Render Canvas Mapping Loop
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int o = x + WIDTH * y;
                
                int heat = b[o];
                // Safety clamp
                if (heat < 0) heat = 0;
                if (heat >= MAX_HEAT) heat = MAX_HEAT - 1;

                // Standard flat Z-layer representation for the fire background
                if (1.0 > zBuffer[o]) {
                    zBuffer[o] = 1.0;
                    
                    // We map the numeric heat value (0-35) to our dual-gradient arrays
                    outputBuffer[o] = FIRE_COLORS[heat] + FIRE_CHARS[heat] + RESET; 
                }
            }
        }
    }
}