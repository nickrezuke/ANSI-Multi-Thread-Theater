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
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 4);

        // Smooth continuous wave ranging from 0.0 to 1.0
        double timeWave = (Math.sin(System.currentTimeMillis() * 0.0025) + 1.0) / 2.0;

        // Kevin's ancient geometric rune structures per face
        long[][] kevinRunes = {
                { 0x0000, 0x07E0, 0x1818, 0x2004, 0x4182, 0x4242, 0x4242, 0x4182, 0x4182, 0x4242, 0x4242, 0x4182,
                        0x2004, 0x1818, 0x07E0, 0x0000 }, // Face 0
                { 0x0000, 0x1FF8, 0x1008, 0x17E8, 0x1428, 0x1428, 0x17E8, 0x1008, 0x1188, 0x1188, 0x1188, 0x1188,
                        0x1008, 0x1FF8, 0x0100, 0x0000 }, // Face 1
                { 0x0000, 0x3FFF, 0x2001, 0x27E1, 0x2421, 0x2421, 0x2421, 0x27E1, 0x2001, 0x2181, 0x2181, 0x2181,
                        0x2181, 0x2001, 0x3FFF, 0x0000 }, // Face 2
                { 0x0000, 0x1F00, 0x0E00, 0x0400, 0x0E00, 0x1B00, 0x31F8, 0x600C, 0x600C, 0x31F8, 0x1B00, 0x0E00,
                        0x0400, 0x0E00, 0x1F00, 0x0000 }, // Face 3
                { 0x0000, 0x0000, 0x1E78, 0x1248, 0x1248, 0x1E78, 0x0000, 0x3C3C, 0x2424, 0x2424, 0x3C3C, 0x1248,
                        0x1248, 0x1E78, 0x0000, 0x0000 }, // Face 4
                { 0x0000, 0x03C0, 0x0420, 0x0810, 0x1008, 0x2184, 0x4242, 0x4242, 0x4242, 0x4242, 0x2184, 0x1008,
                        0x0810, 0x0420, 0x03C0, 0x0000 }  // Face 5
        };

        char[] complexGlyphs = {
            '\u25C8', '\u25CE', '\u25C7', '\u25A3', '\u25A0', '\u259F', '\u259A'
        };

        // Layer A: Outer Beveled Edges (Steady energy border)
        if (x <= 1 || x >= 14 || y <= 1 || y >= 14) {
            int r = 130;
            int g = 20;
            int b = 190; 
            return new VoxelTexel(r, g, b, '%');
        }

        // Layer B: Progressive Percentage Dissolve for Inner Runes
        long rowBits = kevinRunes[face % kevinRunes.length][y];
        boolean isBaseRunePixel = ((rowBits >> (15 - x)) & 1) == 1;

        if (isBaseRunePixel) {
            // Instead of turning all rune pixels on/off together, create a localized 
            // threshold percentage (0 to 100) based on position coordinates.
            int pixelThreshold = (x * 11 + y * 17) % 100;
            double currentPercentage = timeWave * 100.0;

            // Only render the rune if the wave's percentage has reached this pixel yet
            if (pixelThreshold <= currentPercentage) {
                // Color is locked ("always on") to a rich, consistent magenta
                int r = 215;
                int g = 30;
                int b = 175;

                int glyphIndex = (x * 7 + y * 13 + face * 3) % complexGlyphs.length;
                return new VoxelTexel(r, g, b, complexGlyphs[glyphIndex]);
            }
        }

        // Layer C: Volcanic Dark Obsidian Plating (Smooth background breath)
        int pr = (int) (30 + (noise * 5) + (timeWave * 20));
        int pg = 8 + (noise * 1);
        int pb = (int) (50 + (noise * 6) + (timeWave * 25));

        return new VoxelTexel(pr, pg, pb, '\u2592');
    }
}