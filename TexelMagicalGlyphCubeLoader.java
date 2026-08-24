public class TexelMagicalGlyphCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(15, "Awakening Ancient Runes:"),
            new StatusStage(30, "Channeling Arcane Energy:"),
            new StatusStage(45, "Igniting Mana Core:"),
            new StatusStage(60, "Inscribing Sigils:"),
            new StatusStage(75, "Resonating Ley Lines:"),
            new StatusStage(88, "Unleashing Elemental Barrier:"),
            new StatusStage(95, "Achieving Full Resonance:"),
            new StatusStage(99, "Stabilizing Matrix:"),
            new StatusStage(100, "Magical Core Activated!")
    };

    public TexelMagicalGlyphCubeLoader() {
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    @Override
    protected int getTextureResolution() {
        return 64;
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int res = getTextureResolution();
        
        // Normalize coordinates from 0.0 to 1.0
        double u = (double) x / res;
        double v = (double) y / res;
        
        // Centered coordinates (-0.5 to 0.5)
        double cx = u - 0.5;
        double cy = v - 0.5;
        double distFromCenter = Math.sqrt(cx * cx + cy * cy);

        // Time-based energy pulse (breathes every ~2.5 seconds)
        double timeWave = (Math.sin(System.currentTimeMillis() * 0.0025) + 1.0) / 2.0;

        // Pseudo-random noise for stone texture and magical sparks
        long seed = (x * 73856093L) ^ (y * 19349663L) ^ (face * 83492791L);
        double noise = (Math.abs(seed ^ 0x5DEECE66DL) % 100) / 100.0;

        // 1. Outer Edge Glow (Warm amber/gold mystical borders)
        boolean isEdge = (u < 0.08 || u > 0.92 || v < 0.08 || v > 0.92);
        if (isEdge) {
            int r = (int) (210 + timeWave * 45);
            int g = (int) (110 + timeWave * 50);
            int b = (int) (20 + noise * 30);
            return new VoxelTexel(r, g, b, '#');
        }

        // 2. Procedural Face Runes (Distinct geometric glowing energy lines per face)
        boolean isRune = false;
        switch (face % 6) {
            case 0: // Central diamond/cross rune
                isRune = Math.abs(cx) + Math.abs(cy) < 0.22 && Math.abs(cx - cy) > 0.05;
                break;
            case 1: // Concentric square rune
                isRune = (Math.abs(cx) < 0.25 && Math.abs(cy) < 0.25) && (Math.abs(cx) > 0.18 || Math.abs(cy) > 0.18);
                break;
            case 2: // Vertical energy slit with branching glyphs
                isRune = Math.abs(cx) < 0.06 || (Math.abs(cy) < 0.06 && Math.abs(cx) < 0.3);
                break;
            case 3: // Intersecting diagonal rune lines
                isRune = Math.abs(Math.abs(cx) - Math.abs(cy)) < 0.04 && distFromCenter < 0.3;
                break;
            case 4: // Dual glowing core rings
                isRune = distFromCenter > 0.15 && distFromCenter < 0.22;
                break;
            case 5: // The main focal rune
                isRune = distFromCenter < 0.12 || (Math.abs(cx) < 0.04 && Math.abs(cy) < 0.3);
                break;
        }

        if (isRune) {
            // Hot fiery orange/yellow glowing magic
            int r = 255;
            int g = (int) (140 + timeWave * 75);
            int b = (int) (20 + timeWave * 30);
            char runeChar = (noise > 0.5) ? '\u2588' : '\u2593';
            return new VoxelTexel(r, g, b, runeChar);
        }

        // 3. Magical Spark / Energy Arcs across the surface
        boolean isSpark = (Math.sin(u * 15.0 + v * 15.0 + System.currentTimeMillis() * 0.01) > 0.85) && (noise > 0.7);
        if (isSpark) {
            return new VoxelTexel(255, 230, 150, '*');
        }

        // 4. Dark Bronze / Charcoal Stone Plating Background
        int baseShade = (int) (25 + noise * 15);
        int pr = baseShade + 10;
        int pg = (int) (baseShade * 0.7);
        int pb = (int) (baseShade * 0.3);

        char plateChar;
        if (timeWave > 0.7) {
            plateChar = '\u2593'; 
        } else if (timeWave > 0.4) {
            plateChar = '\u2592'; 
        } else if (timeWave > 0.15) {
            plateChar = '\u2591'; 
        } else {
            plateChar = '.'; 
        }

        return new VoxelTexel(pr, pg, pb, plateChar);
    }
}