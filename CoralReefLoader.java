// TODO: Improve this one its just a little boring

public class CoralReefLoader extends Loader {
    private static final StatusStage[] REEF_STAGES = {
        new StatusStage(25, "Calibrating volumetric sunbeam scattering:"),
        new StatusStage(50, "Generating vibrant coral polyps & topography:"),
        new StatusStage(75, "Spawning synchronized schools of neon tetras:"),
        new StatusStage(100, "Coral Reef Biosphere Active!")
    };

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    // Tropical Reef Water Gradient (Teal / Cyan)
    private static final int[] RGB_WATER_TOP = { 0,   210, 255 }; // Bright tropical cyan
    private static final int[] RGB_WATER_MID = { 0,   140, 200 }; // Rich teal
    private static final int[] RGB_WATER_BTM = { 0,   60,  130 }; // Deep oceanic blue
    
    // Sunbeam Color
    private static final int[] RGB_SUNBEAM   = { 255, 250, 220 }; // Warm, pale sunlight

    // Vibrant Coral Colors
    private static final int[][] CORAL_PALETTE = {
        { 220, 50,  130 }, // Hot Pink
        { 160, 40,  200 }, // Deep Purple
        { 255, 120, 30  }, // Vibrant Orange
        { 200, 70,  90  }  // Muted Rose
    };

    public CoralReefLoader() {
        super(REEF_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04; // Slightly faster to make the fish school look active

        int width = 80;
        int height = 22;

        for (int y = 0; y < height; y++) {
            // Gradient for tropical water
            int[] currentWaterRGB = RGB_WATER_BTM;
            if (y < 7)       currentWaterRGB = RGB_WATER_TOP;
            else if (y < 14) currentWaterRGB = RGB_WATER_MID;

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // --- LAYER 1: UNEVEN CORAL REEF WALL (Z-Depth: 0.85) ---
                // Generates a sloping, organic terrain that forms a "wall" on the left
                double reefHeight = 16.0 - 5.0 * Math.sin(x * 0.08) - 2.5 * Math.cos(x * 0.15);
                if (y >= (int) reefHeight) {
                    if (0.85 > zBuffer[index]) {
                        zBuffer[index] = 0.85;
                        
                        // Pick a coral color based on coordinates to create organic "clusters"
                        int colorNoise = (Math.abs(x * 13 ^ y * 7)) % CORAL_PALETTE.length;
                        int[] coralRgb = CORAL_PALETTE[colorNoise];
                        
                        // Add bioluminescent glowing cyan polyps randomly scattered
                        boolean isGlowing = ((x * 17 + y * 23) % 45 == 0);
                        
                        if (isGlowing) {
                            outputBuffer[index] = "\u001B[38;2;0;255;255m\u2593" + RESET; // Glowing Cyan
                        } else {
                            // Shading for depth (lower parts of the reef are darker)
                            double depthShade = Math.max(0.4, 1.0 - ((y - reefHeight) * 0.1));
                            int cr = (int)(coralRgb[0] * depthShade);
                            int cg = (int)(coralRgb[1] * depthShade);
                            int cb = (int)(coralRgb[2] * depthShade);
                            
                            char texture = ((x + y) % 2 == 0) ? '\u2588' : '\u2593';
                            outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm%c", cr, cg, cb, texture) + RESET;
                        }
                    }
                    continue;
                }

                // --- LAYER 2: SWAYING SEA FANS / TUBE SPONGES (Z-Depth: 0.80) ---
                boolean isFan = false;
                int[] fanBases = {10, 25, 60}; // X-coordinates for the sea fans
                for (int bx : fanBases) {
                    double sway = 2.5 * Math.sin((22 - y) * 0.3 + timeClock + bx);
                    double localReefHeight = 16.0 - 5.0 * Math.sin(bx * 0.08) - 2.5 * Math.cos(bx * 0.15);
                    
                    // Grow them up to 5 units above the reef
                    if (y >= (int)localReefHeight - 5 && y < (int)localReefHeight) {
                        if (x == (int)(bx + sway)) {
                            isFan = true;
                            break;
                        }
                    }
                }

                if (isFan && 0.80 > zBuffer[index]) {
                    zBuffer[index] = 0.80;
                    outputBuffer[index] = "\u001B[38;2;255;200;50m\u2592" + RESET; // Golden Yellow
                    continue;
                }

                // --- LAYER 3: DYNAMIC SCHOOL OF FISH (Z-Depth: 0.75) ---
                // Instead of one big fish, a swarm of 12 tiny neon fish looping across
                boolean isSchoolFish = false;
                for (int i = 0; i < 12; i++) {
                    // Spread the fish out horizontally and stagger them in time
                    double pathX = width - ((timeClock * 16.0 + i * 3.5) % (width + 30));
                    // Create an organic swirling motion using combined sine/cosine waves
                    double pathY = 8.0 + 2.0 * Math.sin(timeClock * 1.5 + i * 0.4) 
                                       + 1.5 * Math.cos(timeClock * 0.9 + i);
                    
                    if (Math.abs(x - pathX) < 1.0 && Math.abs(y - pathY) < 1.0) {
                        isSchoolFish = true;
                        break;
                    }
                }

                if (isSchoolFish && 0.75 > zBuffer[index]) {
                    zBuffer[index] = 0.75;
                    // Flashy Neon Yellow/Green tiny fish character
                    outputBuffer[index] = "\u001B[38;2;200;255;0m\u25C4" + RESET; 
                    continue;
                }

                // --- LAYER 4: VOLUMETRIC GOD RAYS & WATER BACKDROP (Z-Depth: 0.01) ---
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;
                    
                    // Create overlapping wave patterns to form diagonal shifting beams
                    double rayPhase1 = Math.sin((x - y * 1.8) * 0.12 - timeClock * 0.7);
                    double rayPhase2 = Math.sin((x - y * 1.2) * 0.08 - timeClock * 0.4);
                    double combinedRay = rayPhase1 + rayPhase2; // Range: ~ -2.0 to 2.0
                    
                    // Isolate the peaks of the waves to form distinct beams of light
                    double rayIntensity = Math.max(0.0, combinedRay - 1.0); // Range: 0.0 to 1.0
                    
                    // Fade out the sunbeams as they go deeper into the water
                    double depthFade = Math.max(0.0, 1.0 - (y / 20.0));
                    rayIntensity *= (depthFade * 0.45); // Max opacity of 45%
                    
                    // Blend current water color with the bright sunbeam color
                    int br = (int)(currentWaterRGB[0] * (1.0 - rayIntensity) + RGB_SUNBEAM[0] * rayIntensity);
                    int bg = (int)(currentWaterRGB[1] * (1.0 - rayIntensity) + RGB_SUNBEAM[1] * rayIntensity);
                    int bb = (int)(currentWaterRGB[2] * (1.0 - rayIntensity) + RGB_SUNBEAM[2] * rayIntensity);
                    
                    String waterColor = String.format("\u001B[38;2;%d;%d;%dm", br, bg, bb);
                    
                    // Light textured water near the surface, smooth deeper down
                    char waterTexture = (y < 5) ? '\u2592' : (y < 12) ? '\u2591' : ' ';
                    
                    outputBuffer[index] = waterColor + waterTexture + RESET;
                }
            }
        }
    }
}