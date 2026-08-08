public class TexelAllsparkLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(12, "Unlocking Ancient Cybernetic Etchings:"),
            new StatusStage(25, "Harnessing Raw Energon Currents:"),
            new StatusStage(40, "Decoding Prime Language Matrix:"),
            new StatusStage(55, "Compressing Universal Scale Matrix:"),
            new StatusStage(70, "Converting Surrounding Mechanical Elements:"),
            new StatusStage(85, "Awakening Mechanical Consciousness:"),
            new StatusStage(96, "Stabilizing Core Energy Outburst:"),
            new StatusStage(100, "AllSpark Stabilized!")
    };

    public TexelAllsparkLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // 2. Continuous Cybertronian Etchings (Abstract geometric line networks)
        // Uses structural line paths instead of repetitive grid patterns
        boolean isVerticalGroove = (x == 3 || x == 7 || x == 12);
        boolean isHorizontalGroove = (y == 3 || y == 8 || y == 11);
        boolean isDiagonalChasm = (x == y && x > 2 && x < 13);

        // Combine paths into a continuous sunken rune network
        boolean isRuneChannel = isVerticalGroove || isHorizontalGroove || isDiagonalChasm;

        // 3. Intricate Raised Layer Borders (Framing individual panel carvings)
        boolean isPanelBorder = (x == 1 || x == 14 || y == 1 || y == 14);

        // --- ALLSPARK RASTERIZATION LAYERS ---

        // Layer A: Radiant Cosmic Energy (Deep inside the carved chasm lines)
        if (isRuneChannel) {
            // High-intensity glowing Cybertronian Cyan Blue
            // Uses the code-safe 16-bit translucent pattern char for energy volume
            return new VoxelTexel(15, 205, 255, '\u2592'); // ▒ (Raw energy pulsing inside channels)
        }

        // Layer B: Sunken Trench Shadows (The dark metal borders right next to the
        // glow)
        // Adds incredible 3D shadow depth inside flat terminal cells
        boolean isTrenchShadow = (x == 2 || x == 4 || x == 6 || x == 8 || x == 11 || x == 13 ||
                y == 2 || y == 4 || y == 7 || y == 9 || y == 10 || y == 12);

        if (isTrenchShadow) {
            int shadowMetal = 95 - noise * 10; // Dark weathered silver/pewter
            return new VoxelTexel(shadowMetal, shadowMetal + 5, shadowMetal + 15, ':');
        }

        // Layer C: Raised Framing Borders (Slightly burnished accent borders)
        if (isPanelBorder) {
            int trimMetal = 185 + noise * 10;
            return new VoxelTexel(trimMetal - 5, trimMetal, trimMetal + 5, '%');
        }

        // Layer D: Outer Raised Platinum Plates (Bright, brushed metal panels)
        int plateMetal = 215 + noise * 8; // Bright platinum/silver core

        // Choose dense characters to give the plates a solid, thick structural presence
        char plateChar = (noise % 2 == 0) ? '\u2588' : '\u2593'; // █ or ▓ (Solid metal mass)
        return new VoxelTexel(plateMetal - 10, plateMetal - 5, plateMetal, plateChar);
    }

}
