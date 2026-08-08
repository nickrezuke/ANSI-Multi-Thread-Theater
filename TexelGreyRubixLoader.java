public class TexelGreyRubixLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(12, "Scrambling:"),
            new StatusStage(24, "Rotating:"),
            new StatusStage(36, "Scrambling:"),
            new StatusStage(48, "Rotating:"),
            new StatusStage(60, "Scrambling:"),
            new StatusStage(72, "Rotating:"),
            new StatusStage(84, "Scrambling:"),
            new StatusStage(96, "Rotating:"),
            new StatusStage(100, "Scramble Solved!")
    };

    public TexelGreyRubixLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        // 1. Symmetrical Grid Borders (Locked at perfect 5-cell intervals)
        boolean isGridBorder = (x == 0 || x == 5 || x == 10 || x == 15 ||
                y == 0 || y == 5 || y == 10 || y == 15);

        if (isGridBorder) {
            return new VoxelTexel(25, 25, 30, '\u2588'); // █ Sharp dark charcoal grid lines
        }

        // 2. Identify the 3x3 grid tile coordinates (0, 1, or 2)
        int tileX = (x < 5) ? 0 : (x < 10) ? 1 : 2;
        int tileY = (y < 5) ? 0 : (y < 10) ? 1 : 2;

        // 3. Normalized tile coordinates centered around index 1 (-1.0 to 1.0)
        double cx = tileX - 1.0;
        double cy = tileY - 1.0;

        // 4. Generate Highly Asymmetric, Irrational Spatial Phase Shifts
        // Using prime products and square roots ensures each tile on every face
        // receives a unique wave offset
        double uniqueTileID = (tileX * 7.13) + (tileY * 13.37) + (face * 19.99);
        double localPhaseOffset = Math.sin(uniqueTileID * 0.6180339887) * 4.44;

        // 5. Establish Time Vectors with Desynchronized Frequencies per Tile
        // Multiplying the clock speed by an irrational multiplier stops the tiles from
        // sharing a timeline
        double baseTime = System.currentTimeMillis() * 0.003;
        double localTime = baseTime + localPhaseOffset;
        double speedModulator = 0.85 + (Math.abs(Math.sin(uniqueTileID)) * 0.3); // Slight speed variance (0.85 to
                                                                                 // 1.15)
        double t = localTime * speedModulator;

        // 6. Layered Sines (Now running on fully desynchronized time streams)
        double wave1 = Math.sin(cx * 3.5 + t) * Math.cos(cy * 3.5 - t * 0.85);
        double wave2 = Math.sin(Math.sqrt(cx * cx + cy * cy) * 4.0 - t * 1.3);

        // Combine waves and normalize to a clean 0.0 -> 1.0 range
        double combined = (wave1 + wave2) / 2.0;
        double colorPercent = (combined + 1.0) / 2.0;

        // 7. Flip the spectrum for alternating tiles to preserve the Checkerboard
        // Pattern
        boolean isAlternateTile = (tileX + tileY) % 2 == 1;
        if (isAlternateTile) {
            colorPercent = 1.0 - colorPercent;
        }

        // 8. Interpolate Monochrome Grayscale Values
        int grayValue = (int) (35 + (colorPercent * 210)); // Ranges smoothly from dark slate to bright white

        // 9. Dynamic Character Morphing matching the color shift
        char morphChar;
        if (colorPercent > 0.8) {
            morphChar = '\u2588'; // █ (Solid Peaks)
        } else if (colorPercent > 0.5) {
            morphChar = '\u2593'; // ▓
        } else if (colorPercent > 0.25) {
            morphChar = '\u2592'; // ▒
        } else {
            morphChar = '\u2591'; // ░ (Fine Grain Valleys)
        }

        return new VoxelTexel(grayValue, grayValue, grayValue, morphChar);

    }

}
