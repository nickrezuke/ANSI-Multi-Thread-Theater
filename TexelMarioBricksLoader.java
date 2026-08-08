public class TexelMarioBricksLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(16, "Starting Level:"),
            new StatusStage(30, "Stomping Enemies:"),
            new StatusStage(42, "Kicking Shells:"),
            new StatusStage(65, "Collecting Coins:"),
            new StatusStage(85, "Utilizing Power-Ups:"),
            new StatusStage(96, "Sliding Down Flagpole:"),
            new StatusStage(100, "Level Complete!:")
    };

    public TexelMarioBricksLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // 2. Exact Mortar Grout Seam Locations
        boolean isSeam = (y == 0 || y == 4 || y == 8 || y == 12 || y == 15);

        if (y > 0 && y < 4)
            isSeam |= (x == 0 || x == 8 || x == 15);
        if (y > 4 && y < 8)
            isSeam |= (x == 0 || x == 4 || x == 12 || x == 15);
        if (y > 8 && y < 12)
            isSeam |= (x == 0 || x == 8 || x == 15);
        if (y > 12 && y < 15)
            isSeam |= (x == 0 || x == 4 || x == 12 || x == 15);

        // 3. High-Contrast 3D Highlights (Top and Left inner edges of every individual
        // brick)
        boolean isHighlight = (y == 1 || y == 5 || y == 9 || y == 13) ||
                (y > 0 && y < 4 && x == 1) ||
                (y > 4 && y < 8 && (x == 1 || x == 5 || x == 13)) ||
                (y > 8 && y < 12 && x == 1) ||
                (y > 12 && y < 15 && (x == 1 || x == 5 || x == 13));

        // 4. 3D Drop Shadows (Bottom and Right inner edges of every individual brick)
        // This adds incredible depth, making the bricks look physically extruded
        boolean isBrickShadow = (y == 3 || y == 7 || y == 11 || y == 14) ||
                (y > 0 && y < 4 && (x == 7 || x == 14)) ||
                (y > 4 && y < 8 && (x == 3 || x == 11 || x == 14)) ||
                (y > 8 && y < 12 && (x == 7 || x == 14)) ||
                (y > 12 && y < 15 && (x == 3 || x == 11 || x == 14));

        // --- MARIO BRICK RASTERIZATION ENGINE ---

        // Layer A: Mortar Grout Lines (Deep, solid charcoal channels)
        if (isSeam) {
            return new VoxelTexel(45, 30, 25, '\u2591');
        }

        // Layer B: Vivid Pastel Salmon Highlights (Sunlit edges catching light)
        // Overrides shadows to keep the corners looking sharp
        if (isHighlight) {
            return new VoxelTexel(190, 125, 85, '\u2588'); // █ (Solid bright mass)
        }

        // Layer C: Deep Maroon/Terracotta Shadows (Recessed edge blocking)
        if (isBrickShadow) {
            int darkR = 90 + noise * 5;
            int darkG = 35 + noise * 2;
            int darkB = 15; // Heavy deep dark brick-red
            return new VoxelTexel(darkR, darkG, darkB, '\u2592'); // ▒ (Densely patterned shadow mesh)
        }

        // Layer D: Main Brick Clay Body (Vibrant Mario Orange-Red)
        int bodyR = 145 + noise * 8;
        int bodyG = 65 + noise * 4;
        int bodyB = 25;
        return new VoxelTexel(bodyR, bodyG, bodyB, '\u2593'); // ▓ (Thick clay textured brick face)
    }

}
