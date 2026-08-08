public class TexelMarioCoinBlockLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(16, "Starting Level:"),
            new StatusStage(30, "Stomping Enemies:"),
            new StatusStage(42, "Kicking Shells:"),
            new StatusStage(65, "Collecting Coins:"),
            new StatusStage(85, "Utilizing Power-Ups:"),
            new StatusStage(96, "Sliding Down Flagpole:"),
            new StatusStage(100, "Level Complete!:")
    };

    public TexelMarioCoinBlockLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);
        // Baseline Yellow-Gold Theme
        int baseR = 230;
        int baseG = 160;
        int baseB = 15; // Solid Yellow-Gold
        int highR = 255;
        int highG = 225;
        int highB = 130; // Cream Highlight
        int shadowR = 135;
        int shadowG = 80;
        int shadowB = 5; // Deep Amber Shadow

        // 2. Exact Layout Math for the Iconic Symbol & Corner Rivets
        boolean isQuestionMark = false;
        boolean isMouthDot = false;
        boolean isCornerScrew = false;

        // Draw the 4 Corner Rivet/Screw holes
        if ((x == 2 || x == 13) && (y == 2 || y == 13)) {
            isCornerScrew = true;
        }

        // Mirror X axis only on face 3 (Top face) to correct the backward "?"
        int evalX = (face == 3) ? (15 - x) : x;

        // High-Res Question Mark (?) Matrix Core
        if (evalX >= 4 && evalX <= 11 && y >= 3 && y <= 13) {
            // Question Mark Upper Hook Loop
            if (y == 3)
                isQuestionMark = (evalX >= 5 && evalX <= 10);
            if (y == 4)
                isQuestionMark = (evalX >= 4 && evalX <= 5) || (evalX >= 10 && evalX <= 11);
            if (y == 5)
                isQuestionMark = (evalX >= 4 && evalX <= 5) || (evalX >= 10 && evalX <= 11);
            if (y == 6)
                isQuestionMark = (evalX >= 9 && evalX <= 11);

            // Question Mark Inward Stem Slide
            if (y == 7)
                isQuestionMark = (evalX >= 7 && evalX <= 9);
            if (y == 8)
                isQuestionMark = (evalX >= 7 && evalX <= 8);
            if (y == 9)
                isQuestionMark = (evalX >= 7 && evalX <= 8);

            // Question Mark Isolated Bottom Period Dot
            if (y == 11 || y == 12) {
                isMouthDot = (evalX >= 7 && evalX <= 8);
            }
        }

        // 3. Extruded Outer Box Shadow Bevel Lines
        boolean isOuterFrameHigh = (x == 1 || y == 1) && (x < 15 && y < 15);
        boolean isOuterFrameDark = (x == 14 || y == 14) && (x > 0 && y > 0);

        // --- QUESTION BLOCK RASTERIZATION INTERPOLATION ---

        // Layer A: The Question Mark Glyphs (Clean, Pure Ceramic White)
        if (isQuestionMark || isMouthDot) {
            int w = 245 + noise * 4;
            return new VoxelTexel(w, w, w, '\u2588'); // █
        }

        // Layer B: Corner Screw Insets (Deep Drop Shadows)
        if (isCornerScrew) {
            return new VoxelTexel(30, 20, 10, '#');
        }

        // Layer C: 3D Sunlit Top/Left Box Highlights
        if (isOuterFrameHigh && !isOuterFrameDark) {
            return new VoxelTexel(highR, highG, highB, '\u2588'); // █
        }

        // Layer D: 3D Recessed Bottom/Right Box Shadows
        if (isOuterFrameDark) {
            return new VoxelTexel(shadowR - noise * 5, shadowG - noise * 4, shadowB, '\u2592'); // ▒
        }

        // Layer E: Main Casing Body Face Plates
        return new VoxelTexel(baseR + noise * 8, baseG + noise * 6, baseB, '\u2593'); // ▓
    }

}
