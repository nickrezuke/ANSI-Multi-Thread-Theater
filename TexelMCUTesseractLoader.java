public class TexelMCUTesseractLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(25, "Calibrating Scott's Van:"),
            new StatusStage(55, "Warping through SpaceTime:"),
            new StatusStage(85, "Returning the Stones:"),
            new StatusStage(100, "Time Heist Cleared!")
    };

    public TexelMCUTesseractLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // 1. Establish global clock for high-speed cosmic energy waves
        double t = System.currentTimeMillis() * 0.005;

        // 3. Crisp Neon Outer Framing (Only tracking the actual structural edge
        // boundaries of the faces)
        boolean isOuterFrameLine = (x == 0 || x == 15 || y == 0 || y == 15);

        // 4. Stylized Radial Distance Math for Energy Ripples
        double dx = x - 7.5;
        double dy = y - 7.5;
        double radius = Math.sqrt(dx * dx + dy * dy);

        // Sharp concentric ring math: creates defined energy waves pulsing outward from
        // the core
        double rippleWave = Math.sin(radius * 1.8 - t * 2.0);

        // Normalize to a clean 0.0 -> 1.0 spectrum
        double finalEnergyField = (rippleWave + 1.0) / 2.0;

        // --- COSMIC ARTISTIC RASTERIZATION PIPELINE ---
        char blockChar = '\u2588'; // █ (Solid pixel fill)

        // Layer A: Pure Blazing White Infinity Stone Core (Stays sharp and locked at
        // the center)
        if (radius < 2.5) {
            int w = 240 + noise * 5;
            return new VoxelTexel(w, w, 255, blockChar); // █ Blazing hyper-white core
        }

        // Layer B: Crisp Electric Cyan Outer Casing Frames
        if (isOuterFrameLine) {
            // High-saturation electric cyan lines that lock the 3D cube geometry edges
            return new VoxelTexel(0, 235 + noise * 10, 255, blockChar);
        }

        // Layer C: Stylized Concentric Shading Shockwaves
        if (finalEnergyField > 0.78) {
            // High Energy Crest: Intense Bright Cyan
            return new VoxelTexel(30, 210, 255, blockChar); // █
        } else if (finalEnergyField > 0.52) {
            // Medium Energy Crest: Sky Blue
            return new VoxelTexel(20, 140, 240, '\u2593'); // ▓ Densely speckled mesh
        } else if (finalEnergyField > 0.28) {
            // Lower Energy Valleys: Deep Sapphire Blue
            return new VoxelTexel(10, 70, 190, '\u2592'); // ▒ Medium cross-hatch mesh
        } else {
            // Inactive Void Pockets: Deep Electric Charcoal
            int voidB = 40 + noise * 5;
            return new VoxelTexel(15, 20, voidB, '\u2591'); // ░ Fine grain backing
        }
    }
}
