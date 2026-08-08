public class TexelCompanionCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(8, "Calibrating Scientific Intruments:"),
            new StatusStage(28, "Designing Test Chamber:"),
            new StatusStage(55, "Synthesizing Test Subjects:"),
            new StatusStage(75, "Activating Observation Deck:"),
            new StatusStage(96, "Calibrating Portal Devices:"),
            new StatusStage(98, "Rejecting Morality Upgrade:"),
            new StatusStage(100, "Test Chamber Ready!")
    };

    public TexelCompanionCubeLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // Exact Coordinate System for the Aperture Science Framing
        double dx = x - 7.5;
        double dy = y - 7.5;
        double radius = Math.sqrt(dx * dx + dy * dy);

        // Layer A: The Central Aperture Pink Heart
        boolean isHeart = false;
        if (x >= 5 && x <= 10 && y >= 5 && y <= 9) {
            if (y == 5)
                isHeart = (x == 6 || x == 9);
            if (y == 6)
                isHeart = (x >= 5 && x <= 10);
            if (y == 7)
                isHeart = (x >= 5 && x <= 10);
            if (y == 8)
                isHeart = (x >= 6 && x <= 9);
            if (y == 9)
                isHeart = (x == 7 || x == 8);
        }

        // Layer B: The Central Circular Aperture Disc (White Backing)
        boolean isWhiteDisc = (radius >= 0.0 && radius <= 4.2);

        // Layer C: Heavy Corner Bevel Protective Brackets & Outer Rim Trim
        // Recreates the thick, separate armor sheets bolted onto each vertex
        boolean isCornerBracket = (x <= 3 || x >= 12) && (y <= 3 || y >= 12);
        boolean isOuterRim = (x <= 1 || x >= 14 || y <= 1 || y >= 14);
        boolean isLightArmor = isCornerBracket || isOuterRim;

        // Layer D: Drop Shadows under the light armor plates for 3D depth perception
        boolean isPlateShadow = (x == 4 && (y <= 3 || y >= 12)) || (x == 11 && (y <= 3 || y >= 12)) ||
                (y == 4 && (x <= 3 || x >= 12)) || (y == 11 && (x <= 3 || x >= 12));

        // --- PORTAL COMPOSITING PIPELINE ---

        // 1. High-Density Pink Heart Centerpiece
        if (isHeart) {
            int r = 245;
            int g = 110 + noise * 5;
            int b = 155; // Vibrant Hot Pink / Magenta
            return new VoxelTexel(r, g, b, '\u2588'); // █ (Solid colored energy core)
        }

        // 2. Circular Aperture Backdrop Ring
        if (isWhiteDisc) {
            int w = 215 + noise * 10;
            return new VoxelTexel(w, w, w, '\u2593'); // ▓ (Thick light textured disk)
        }

        // 3. Extruded Corner Shields and Rim Castings (Light Alloy)
        if (isLightArmor && !isPlateShadow) {
            int armor = 165 - noise * 6;
            return new VoxelTexel(armor, armor, armor + 5, '\u2588'); // █ (Solid thick plating)
        }

        // 4. Drop Shadow Insets flanking the armor borders
        if (isPlateShadow) {
            int shadow = 35 + noise * 3;
            return new VoxelTexel(shadow, shadow, shadow + 3, ';'); // Deep recessed groove
        }

        // 5. Central Inset Core Plating (Dark Alloy Base Hull)
        int hull = 80 + noise * 6;
        return new VoxelTexel(hull, hull, hull + 4, '\u2592'); // ▒ (Medium cross-hatched alloy matrix)

    }

}
