public class TexelBorgCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(10, "Establishing Collective Link:"),
            new StatusStage(22, "Interlocking Biomechanical Matrix:"),
            new StatusStage(35, "Powering Sub-Space Transwarp Coil:"),
            new StatusStage(50, "Analyzing Target Biological Distinctiveness:"),
            new StatusStage(65, "Charging Distribution Node Circuitry:"),
            new StatusStage(80, "Modulating Shield Frequencies:"),
            new StatusStage(92, "Locking Tractor Beams:"),
            new StatusStage(98, "Lowering Deflector Shields:"),
            new StatusStage(100, "Assimilation Successful!")
    };

    public TexelBorgCubeLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        // 1. Map higher-grain 4-level voxel noise
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // 2. Face-Disambiguated Structural Grid System
        // Scrambles layout alignments dynamically based on the current face index
        int faceSeed = face * 37 + 13;
        int vertPipe = (faceSeed ^ 0x2A) % 8 + 4; // Distinct structural vertical conduit row
        int horizPipe = (faceSeed ^ 0xC5) % 8 + 4; // Distinct structural horizontal conduit row

        // 3. Layer Bitmask Mapping Rules
        boolean isVerticalConduit = (x == vertPipe);
        boolean isHorizConduit = (y == horizPipe);
        boolean isSecondaryWire = ((x * 5 + y * 11 + face) % 7 == 0);
        boolean isPanelTrench = (x % 5 == 0 || y % 5 == 0);

        // Geometric Green Power Matrices (X-shaped circuitry channels crossing the
        // face)
        boolean isPlasmaChannel = (x == y || x == 15 - y) && (x > 1 && x < 14);

        // --- TEXTURE RASTERIZATION PROCESSING ---

        // Layer A: Heavy Foreground Structural Piping
        // Vertical pipes use vertical bars, horizontal pipes use double equals to give
        // a 3D illusion
        if (isVerticalConduit) {
            int pipeColor = 125 + noise * 10;
            return new VoxelTexel(pipeColor, pipeColor, pipeColor + 5, '\u2551'); // (Double vertical wall pipe)
        }
        if (isHorizConduit) {
            int pipeColor = 115 + noise * 10;
            return new VoxelTexel(pipeColor, pipeColor, pipeColor + 5, '='); // = (Horizontal equals)
        }

        // Layer B: Continuous Glowing Plasma Matrices (Neon Green Channels)
        if (isPlasmaChannel) {
            // Pure unshaded radioactive neon green using safe 16-bit primitives
            return new VoxelTexel(10, 235, 45, '\u2592'); // (Medium shaded mesh)
        }

        // Layer C: Exposed Secondary Cable Bundles and Wiring
        if (isSecondaryWire) {
            int wireColor = 80 + noise * 5;
            return new VoxelTexel(wireColor - 5, wireColor, wireColor, '-'); // Fine mesh wires
        }

        // Layer D: Armor Plate Bevels and Deep Machinery Crevices
        if (isPanelTrench) {
            int trenchColor = 55 + noise * 5;
            return new VoxelTexel(trenchColor, trenchColor, trenchColor + 4, '%'); // Deep hull panel splits
        }

        // Layer E: Base Metal Under-plating
        int hullColor = 35 + noise * 4;
        return new VoxelTexel(hullColor, hullColor, hullColor + 2, '#'); // Dark graphite backing plate
    }

}
