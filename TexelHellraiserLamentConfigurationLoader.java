public class TexelHellraiserLamentConfigurationLoader extends TexelCubeLoader {
    // TODO: Finish deciding on Loading Statements
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(20, "???:"),
            new StatusStage(50, "???:"),
            new StatusStage(80, "???:"),
            new StatusStage(100, "???!")
    };

    public TexelHellraiserLamentConfigurationLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        // 1. Surface weathering noise for the antique brass and grain wood
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 4);

        // 2. Exact Layout Math for the Iconic Puzzle Box Framing
        // Center point is 7.5
        double dx = x - 7.5;
        double dy = y - 7.5;
        double radius = Math.sqrt(dx * dx + dy * dy);

        // Boolean mask triggers for the structural brass etching layers
        boolean isOuterBrassFrame = (x == 0 || x == 15 || y == 0 || y == 15);
        boolean isInnerBrassSquare = (x == 3 || x == 12 || y == 3 || y == 12)
                && (x >= 3 && x <= 12 && y >= 3 && y <= 12);

        // Circular brass mechanisms (The clockwork dials)
        boolean isCentralDialRing = (radius >= 3.4 && radius <= 4.6);
        boolean isCorePin = (radius >= 0.0 && radius <= 1.2);

        // Star-burst clockwork tracks radiating from the center ring to the inner
        // square
        boolean isClockworkSpoke = (x == y || x == 15 - y || x == 7 || x == 8 || y == 7 || y == 8)
                && (radius >= 2.5 && radius <= 6.0);

        // Combine everything into a unified brass mechanism overlay
        boolean isBrassMechanism = isOuterBrassFrame || isInnerBrassSquare || isCentralDialRing || isCorePin
                || isClockworkSpoke;

        // 3. Drop Shadow Masking (Recessed gaps between wood panels and brass sheets)
        // Placed right along the borders to create deep 3D optical tracking
        boolean isTrenchShadow = (x == 1 || x == 14 || y == 1 || y == 14) ||
                (radius >= 4.7 && radius <= 5.5 && !isClockworkSpoke);

        // --- TEXTURE RASTERIZATION PROCESSING ---

        // Layer A: Ornate Gleaming Antique Brass Overlays
        if (isBrassMechanism && !isTrenchShadow) {
            // High-contrast, rich metallic gold/brass color space
            int r = 215 + noise * 10;
            int g = 165 + noise * 5;
            int b = 40;

            // Use dense symbols to depict highly complex, intricate clockwork etching
            char brassChar = (isCorePin) ? '\u2588' : ((x + y) % 2 == 0 ? '\u25CE' : '\u25C9'); // █, ◎, or ◉
            return new VoxelTexel(r, g, b, brassChar);
        }

        // Layer B: Deep Inset Trench Shadows
        if (isTrenchShadow) {
            int shadowR = 25 + noise * 2;
            int shadowG = 15;
            int shadowB = 10; // Dark charcoal void split
            return new VoxelTexel(shadowR, shadowG, shadowB, ';');
        }

        // Layer C: Deep Polished Mahogany / Rosewood Wood Panels
        // Fills out the remainder of the backing plates with a rich, dark dark-red wood
        // finish
        int woodR = 65 + noise * 6;
        int woodG = 25 + noise * 2;
        int woodB = 15;

        // Choose heavy dense shading blocks to represent solid organic lumber grains
        char woodChar = (noise % 2 == 0) ? '\u2593' : '\u2592'; // ▓ or ▒
        return new VoxelTexel(woodR, woodG, woodB, woodChar);

    }

}
