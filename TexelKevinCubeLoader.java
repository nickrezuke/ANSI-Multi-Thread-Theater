public class TexelKevinCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(15, "Manifesting Lightning Strike:"),
            new StatusStage(30, "Burning Dark Runes:"),
            new StatusStage(45, "Charging Kinetic Shield:"),
            new StatusStage(60, "Corrupting Nearby Soil:"),
            new StatusStage(75, "Summoning Cube Monsters:"),
            new StatusStage(88, "Floating Towards Loot Lake:"),
            new StatusStage(95, "Initiating The Butterfly Event:"),
            new StatusStage(99, "Shattering Into The In-Between:"),
            new StatusStage(100, "Zero Point Reached!")
    };

    public TexelKevinCubeLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // floating point wave ranging from 0.0 (dim) to 1.0 (bright) every ~2.5 seconds
        double timeWave = (Math.sin(System.currentTimeMillis() * 0.0025) + 1.0) / 2.0;

        // Mapping out Kevin's ancient runes
        long[][] kevinRunes = {
                { 0x0000, 0x07E0, 0x0810, 0x1008, 0x23C4, 0x2424, 0x2424, 0x23C4, 0x23C4, 0x2424, 0x2424, 0x23C4,
                        0x1008, 0x0810, 0x07E0, 0x0000 }, // Face 0
                { 0x0000, 0x0180, 0x03C0, 0x07E0, 0x0DB0, 0x198F, 0x318C, 0x6186, 0x6186, 0x318C, 0x198F, 0x0DB0,
                        0x07E0, 0x03C0, 0x0180, 0x0000 }, // Face 1
                { 0x0000, 0x3FFF, 0x2001, 0x27E1, 0x2421, 0x2421, 0x2421, 0x27E1, 0x2001, 0x2181, 0x2181, 0x2181,
                        0x2181, 0x2001, 0x3FFF, 0x0000 }, // Face 2
                { 0x0000, 0x1F00, 0x0E00, 0x0400, 0x0E00, 0x1B00, 0x31F8, 0x600C, 0x600C, 0x31F8, 0x1B00, 0x0E00,
                        0x0400, 0x0E00, 0x1F00, 0x0000 }, // Face 3
                { 0x0000, 0x0000, 0x1E78, 0x1248, 0x1248, 0x1E78, 0x0000, 0x0000, 0x0000, 0x0000, 0x1E78, 0x1248,
                        0x1248, 0x1E78, 0x0000, 0x0000 }, // Face 4
                { 0x0000, 0x0180, 0x0180, 0x0180, 0x0180, 0x0FF0, 0x1818, 0x1008, 0x1008, 0x1818, 0x0FF0, 0x0180,
                        0x0180, 0x0180, 0x0180, 0x0000 } // Face 5
        };
        char[] runeGlyphs = {
                '\u039E', // Xi
                '\u03A8', // Psi
                '\u205C', // Dotted Cross
                '\u29C9', // Joined Squared
                '\u29D3', // Bowtie
                '\u23C3' // Triangle w/ Line
        };

        // 3. Layer A: Outer Beveled Edges (Glows and breathes dynamically with the
        // wave)
        if (x <= 1 || x >= 14 || y <= 1 || y >= 14) {
            int r = (int) (100 + (noise * 10) + (timeWave * 45)); // Scales purple energy between 100 and 155
            int g = 25;
            int b = (int) (160 + (noise * 10) + (timeWave * 60)); // Scales blue/purple energy between 160 and 230
            return new VoxelTexel(r, g, b, '%');
        }

        // 4. Layer B: Check Bitmask for Inner Ancient Runes
        long rowBits = kevinRunes[face % 6][y];
        boolean isRuneGlyph = ((rowBits >> (15 - x)) & 1) == 1;

        if (isRuneGlyph) {
            // Glowing Pink/Magenta Runes pulsating in intensity
            int r = (int) (180 + (timeWave * 75)); // Alternates between dark magenta (180) and hot pink (255)
            int g = (int) (30 + (timeWave * 35));
            int b = (int) (140 + (timeWave * 80));

            char dynamicGlyph = runeGlyphs[face % runeGlyphs.length];
            return new VoxelTexel(r, g, b, dynamicGlyph);
        }

        // 5. Layer C: Dark Volcanic Dark Plating (Static obsidian background to
        // contrast the glow)
        int pr = 35 + noise * 5;
        int pg = 10 + noise * 2;
        int pb = 65 + noise * 5;

        // Complete unicode block density toolkit
        char fullShadedBlock = '\u2588'; // (100% color fill)
        char darkShadedBlock = '\u2593'; // (75% color fill)
        char mediumShadedBlock = '\u2592'; // (50% color fill)
        char lightShadedBlock = '\u2591'; // (25% color fill)
                                          // ( ; will be used for 0%)

        // Smooth 5-step stepping mapping based on wave intensity
        char plateChar;
        if (timeWave > 0.85) {
            plateChar = fullShadedBlock; // Blazing solid purple plate core
        } else if (timeWave > 0.65) {
            plateChar = darkShadedBlock; // Heavy dense shading
        } else if (timeWave > 0.4) {
            plateChar = mediumShadedBlock; // Medium cross-hatch shading
        } else if (timeWave > 0.15) {
            plateChar = lightShadedBlock; // Soft glowing speckles
        } else {
            plateChar = ';'; // Dark, dormant obsidian void
        }

        return new VoxelTexel(pr, pg, pb, plateChar);
    }

}
