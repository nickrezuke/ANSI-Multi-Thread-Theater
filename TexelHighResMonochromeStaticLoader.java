public class TexelHighResMonochromeStaticLoader extends TexelCubeLoader {
    private static final StatusStage[] STATIC_STAGES = new StatusStage[] {
        new StatusStage(15, "Tuning cathode ray tube:"),
        new StatusStage(40, "Amplifying white noise:"),
        new StatusStage(65, "De-aligning frame sync:"),
        new StatusStage(85, "Overclocking signal gain:"),
        new StatusStage(99, "Snowstorm matrix complete:"),
        new StatusStage(100, "High-Res Static Online!")
    };

    public TexelHighResMonochromeStaticLoader() {
        super(STATIC_STAGES, 80, 22);
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        // --- 1. HARDWIRED STRUCTURAL GEOMETRY OVERRIDES ---
        // Identify the exact 4 outer corners of the 16x16 voxel texture patch
        boolean isCorner = (x == 0 && y == 0) || (x == 15 && y == 0) || 
                           (x == 0 && y == 15) || (x == 15 && y == 15);
                           
        // Identify the exact 2x2 physical center core of the 16x16 grid
        boolean isCenter = (x == 7 || x == 8) && (y == 7 || y == 8);

        if (isCorner || isCenter) {
            // Force pure, solid, blinding white hot-spots
            return new VoxelTexel(255, 255, 255, '█');
        }

        // --- 2. HYPER-FAST HARDWARE NOISE GENERATION ---
        // Pull high-resolution nanosecond timers and bitmask them with space coordinates
        // to make sure every individual console cell acts completely independently.
        long seed = System.nanoTime() ^ ((long) x << 24) ^ ((long) y << 8);
        java.util.Random rand = new java.util.Random(seed);
        
        double randomValue = rand.nextDouble();

        // --- 3. DYNAMIC 8-DOT SUB-PIXEL FRAGMENTATION ---
        // Unicode Braille starts at 0x2800 and contains exactly 256 configurations (up to 0x28FF).
        // By rolling a random integer between 0 and 255, we randomize all 8 sub-pixel dots instantly.
        int brailleBitmask = rand.nextInt(256);
        char renderChar = (char) (0x2800 + brailleBitmask);

        // --- 4. MONOCHROME BINARY STROBE ---
        // Hard snap the pixel color to absolute white or absolute black for raw, un-smoothed fuzz.
        int brightness = (randomValue > 0.5) ? 255 : 0;

        // Visual enhancement: If the pixel is evaluated as black, we clear out the character 
        // to a blank space on a 50% chance. This adds deep contrast and stops the static 
        // from turning into a muddy, uniform gray block.
        if (brightness == 0 && randomValue < 0.25) {
            renderChar = ' '; 
        }

        return new VoxelTexel(brightness, brightness, brightness, renderChar);
    }
}
