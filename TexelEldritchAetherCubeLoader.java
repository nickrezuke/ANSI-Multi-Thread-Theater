public class TexelEldritchAetherCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] MYSTIC_STAGES = new StatusStage[] {
            new StatusStage(15, "Engraving gold chassis:"),
            new StatusStage(35, "Channelling aetheric flow:"),
            new StatusStage(60, "Aligning runic lattices:"),
            new StatusStage(80, "Slowing chaotic flux:"),
            new StatusStage(95, "Coalescing calm singularity:"),
            new StatusStage(100, "Eldritch Aether Core Bound!")
    };

    public TexelEldritchAetherCubeLoader() {
        super(MYSTIC_STAGES, 80, 22);
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        long time = System.currentTimeMillis();

        // 1. BOUNDARY DEFINITION: THE THIN GOLD BAR CHASSIS
        // Isolates the outermost 1-voxel layer as the structural framing
        boolean isGoldFrame = (x == 0 || x == 15 || y == 0 || y == 15);

        // Slow, elegant temporal clock for smooth transitioning
        double tAngle = time * 0.0015;

        // Color registers
        int r = 0;
        int g = 0;
        int b = 0;
        char renderChar;

        if (isGoldFrame) {
            // --- LAYER A: SHIMMERING METALLIC GOLD BARS ---
            // A slow-rolling, highly polished sheen reflecting along the frame lines
            double sheen = 0.5 + 0.5 * Math.sin(tAngle * 1.5 + (x + y) * 0.4);

            // Premium metallic gold hex balance: High red, warm copper-green, deep amber
            // blue
            r = (int) (210 + 45 * sheen);
            g = (int) (165 + 35 * sheen);
            b = (int) (40 + 25 * sheen);

            // Thin framing characters to mimic elegant rods
            if ((x == 0 || x == 15) && (y == 0 || y == 15)) {
                renderChar = '▓'; // Corner socket joints
            } else {
                renderChar = '░'; // Slender borders
            }
        } else {
            // --- LAYER B: THE COHESIVE "CALM GLITCH" FIELD ---
            // To create blobby lava-lamp shapes, x and y must be tightly correlated.
            // We scale coordinates down significantly (multiplying by low numbers like
            // 0.18)
            // so neighboring pixels share almost the same mathematical input space.
            double scaledX = x * 0.18;
            double scaledY = y * 0.18;

            // Generate macro-scale fluid noise blobs via coupled phase equations
            double blob1 = Math.sin(scaledX + Math.cos(scaledY + tAngle) + tAngle * 0.4);
            double blob2 = Math.cos(scaledY + Math.sin(scaledX - tAngle) + tAngle * 0.6);

            // Mix with radial distance to anchor the energy towards the core center
            double cx = x - 7.5;
            double cy = y - 7.5;
            double centerRadius = Math.sqrt(cx * cx + cy * cy);
            double corePulse = Math.sin(centerRadius * 0.3 - tAngle * 0.8);

            // Synthesize the final cohesive field (Strictly bounded smooth distribution)
            double calmFlux = (blob1 + blob2 + corePulse) / 3.0;

            // Map the fluid fields to rich, deep, harmonious mystical tones.
            // By applying unique offset phases, colors transition softly rather than
            // flashing.
            double rPhase = Math.sin(calmFlux * Math.PI + (tAngle * 0.3));
            double gPhase = Math.sin(calmFlux * Math.PI + (Math.PI / 3.0));
            double bPhase = Math.cos(calmFlux * Math.PI - (tAngle * 0.2));

            // Velvet amethyst violets, deep sapphire blues, and rich ethereal emeralds
            r = (int) (90 + 95 * rPhase);
            g = (int) (40 + 125 * Math.max(-0.2, gPhase)); // Smooth green blend
            b = (int) (150 + 105 * bPhase);

            // --- LAYER C: ELDRITCH CANADIAN SYLLABICS RUNIC MATRIX ---
            // Range requested: U+1400 (5120 decimal) to U+167F (5759 decimal). Total span
            // of 640 cells.
            int baseUnicode = 0x1400;
            int totalSpan = 0x167F - 0x1400;

            // Derive a calm, deterministic glyph pointer from the fluid flow.
            // Multiplying by a small scale ensures glyph structures cluster together into
            // sentences
            // that slowly morph across the grid rather than sparking like static fuzz.
            double noiseSeed = (calmFlux + 1.0) / 2.0; // Normalize between 0.0 and 1.0
            int glyphOffset = (int) (noiseSeed * totalSpan + (centerRadius * 2)) % totalSpan;

            renderChar = (char) (baseUnicode + Math.abs(glyphOffset));

            // Gentle highlighting: Warm up the illumination slightly in the densest
            // hot-zones
            if (calmFlux > 0.4) {
                r = Math.min(255, r + 30);
                g = Math.min(255, g + 25);
                b = Math.min(255, b + 20);
            }
            // TimeScale warping effect
            r *= 0.5 * (Math.sin(time / 350.0 + 0.1 * (7.5 - x) * (7.5 - x) + 0.1 * (7.5 - y) * (7.5 - y)) + 1);
            g *= 0.5 * (Math.sin(time / 350.0 + 0.1 * (7.5 - x) * (7.5 - x) + 0.1 * (7.5 - y) * (7.5 - y)) + 1);
            b *= 0.5 * (Math.sin(time / 350.0 + 0.1 * (7.5 - x) * (7.5 - x) + 0.1 * (7.5 - y) * (7.5 - y)) + 1);

        }

        // 24-bit TrueColor overflow protection
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new VoxelTexel(r, g, b, renderChar);
    }
}
