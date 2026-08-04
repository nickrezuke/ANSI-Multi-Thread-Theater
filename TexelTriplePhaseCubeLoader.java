public class TexelTriplePhaseCubeLoader extends TexelCubeLoader {
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

    public TexelTriplePhaseCubeLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);
        // 1. Establish precise time tracks
            double time = System.currentTimeMillis() * 0.003;
            long timeCycle = (System.currentTimeMillis() / 2000) % 3; // Swaps algorithm states every 2 seconds

            // 3. Rigid Structural Casing (Locks the 3D voxel box silhouette)
            boolean isFrame = (x == 0 || x == 15 || y == 0 || y == 15 ||
                    (x == 1 && y == 1) || (x == 14 && y == 14) ||
                    (x == 1 && y == 14) || (x == 14 && y == 1));

            if (isFrame) {
                int metal = 45 + noise * 5;
                return new VoxelTexel(metal, metal + 2, metal + 4, '#'); // Clean, stable gunmetal frame
            }

            // 4. Central Computational Core (The Algorithmic Engine)
            // Map center coordinates from -7 to 7
            int cx = x - 8;
            int cy = y - 8;

            int r = 0, g = 0, b = 0;
            char matrixChar = '\u2588'; // █ Default solid fill

            // 5. Triple-State Algorithmic State Machine
            if (timeCycle == 0) {
                // --- MODE 0: BITWISE FRACTAL LEAKAGE ---
                // Uses exclusive OR math mixed with time to sprout organic computer tree
                // patterns
                int fractalValue = ((x * 4) ^ (y * 4) ^ ((int) (time * 15))) & 0xFF;

                r = fractalValue;
                g = (fractalValue * 3) % 256;
                b = 255 - fractalValue; // Electric Purple and Blue shifting nodes

                matrixChar = fractalValue > 128 ? '\u2593' : '\u2592'; // ▓ or ▒

            } else if (timeCycle == 1) {
                // --- MODE 1: INTERFERENCE WAVE LOOPS (THE MOIRÉ EFFECT) ---
                // Using nested modulos and trigonometric distances creates clean geometric
                // shockwaves
                double dist = Math.sqrt(cx * cx + cy * cy);
                int ringCheck = (int) (dist * 3.5 - time * 6.0) % 6;

                if (ringCheck == 0 || ringCheck == 1) {
                    r = 10;
                    g = 255;
                    b = 150; // Neon Emerald Laser Ring Lines
                    matrixChar = '\u2588'; // █
                } else {
                    r = 20;
                    g = 50;
                    b = 40; // Dark Background Matrix Valleys
                    matrixChar = '\u2591'; // ░
                }

            } else {
                // --- MODE 2: HIGH-FREQUENCY TRIG CORRUPTION (CHIPSAT TELEMETRY) ---
                // Forcing Math.cos to evaluate astronomical scales creates structured digital
                // noise
                // This generates a highly stylized "smart static" that scans vertically
                double chaoticTrig = Math.cos((x * 2345.67) + (y * 8765.43) + time * 12.0);

                if (chaoticTrig > 0.4) {
                    // Intense flashing cyber pink/magenta
                    r = 255;
                    g = 20;
                    b = 145;
                    matrixChar = '\u2588'; // █
                } else if (chaoticTrig < -0.4) {
                    // Dark electric sapphire
                    r = 15;
                    g = 40;
                    b = 180;
                    matrixChar = '\u2592'; // ▒
                } else {
                    // Randomly distributed pixel static drops
                    r = (int) (Math.random() * 60);
                    g = (int) (Math.random() * 256); // Heavy tracking green component
                    b = (int) (Math.random() * 60);
                    matrixChar = '\u2591'; // ░
                }
            }
            return new VoxelTexel(r, g, b, matrixChar);
    }

}
