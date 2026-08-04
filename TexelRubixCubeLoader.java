public class TexelRubixCubeLoader extends TexelCubeLoader {
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

    public TexelRubixCubeLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        // 1. Gridlines: 16x16 boundaries wrapping 3x3 tiles separated by 5-cell
            // intervals
            if (x == 0 || x == 5 || x == 10 || x == 15 || y == 0 || y == 5 || y == 10 || y == 15) {
                // High-end matte black plastic grid boundaries
                return new VoxelTexel(20, 20, 25, '#');
            }

            // 2. Identify the specific sub-sticker quadrant coordinate (0, 1, or 2)
            int row = (y < 5) ? 0 : (y < 10) ? 1 : 2;
            int col = (x < 5) ? 0 : (x < 10) ? 1 : 2;

            // 3. Scrambled Sticker State Machine
            // A deterministic shuffle array to make the cube look realistically mixed up
            int[] shuffleArray = {
                    2, 0, 5, 1, 4, 3, 0, 4, 1, 5, 2, 3, 4, 1, 0, 2, 3, 5,
                    5, 3, 2, 0, 1, 4, 1, 5, 3, 4, 0, 2, 3, 2, 4, 1, 5, 0,
                    0, 1, 4, 2, 5, 3, 5, 2, 1, 0, 3, 4, 2, 4, 3, 5, 1, 0
            };

            // Calculate a unique index for each individual sticker slot (0 to 53)
            int stickerIndex = Math.abs(face * 9 + row * 3 + col) % shuffleArray.length;
            int stickerColor = shuffleArray[stickerIndex];

            // Subtle internal sticker highlight to give a glossy/curved reflection look
            boolean isHighlight = (x == 1 || x == 6 || x == 11) && (y == 1 || y == 6 || y == 11);

            // 4. Color Assignment and Material Rendering
            // Uses uniform unicode block primitives to look like flat plastic tiles
            char blockChar = '\u2588'; // █ (Solid color mass)

            switch (stickerColor) {
                case 0: // Radiant Red
                    return new VoxelTexel(isHighlight ? 255 : 220, 35, 35, blockChar);
                case 1: // Deep Blue
                    return new VoxelTexel(30, 100, isHighlight ? 255 : 230, blockChar);
                case 2: // Bright Orange
                    return new VoxelTexel(255, isHighlight ? 155 : 120, 15, blockChar);
                case 3: // Neon Green
                    return new VoxelTexel(45, isHighlight ? 245 : 200, 55, blockChar);
                case 4: // Pure Ceramic White
                    int w = isHighlight ? 255 : 240;
                    return new VoxelTexel(w, w, w, blockChar);
                default: // Vivid Canary Yellow
                    return new VoxelTexel(245, isHighlight ? 255 : 225, 25, blockChar);
            }
    }

}
