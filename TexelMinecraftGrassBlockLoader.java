public class TexelMinecraftGrassBlockLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(10, "Generating Chunks:"),
            new StatusStage(20, "Growing Forests:"),
            new StatusStage(30, "Populating Villages:"),
            new StatusStage(40, "Scattering Ore Deposits:"),
            new StatusStage(50, "Heating up the Nether:"),
            new StatusStage(60, "Building Strongholds:"),
            new StatusStage(70, "Filling in Loot Chests:"),
            new StatusStage(80, "Determining World Spawn:"),
            new StatusStage(90, "Initializing Player:"),
            new StatusStage(99, "Removing Herobrine:"),
            new StatusStage(100, "World Generation Complete!")
    };

    public TexelMinecraftGrassBlockLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        // 1. Map higher-grain 4-level voxel noise
        int noise = (int) (Math.abs((x * 2693L + y * 7919L + face * 4567L) ^ 0x5DEECE66DL) % 4);

        // 2. Uniform pixel block characters to simulate raw voxels
        char blockChar = '\u2588'; // █ (Solid block fill)

        // --- FACE 3: TOP GRASS FACE ---
        if (face == 3) {
            switch (noise) {
                case 0:
                    return new VoxelTexel(80, 145, 30, blockChar); // Muted Forest Green
                case 1:
                    return new VoxelTexel(85, 155, 35, blockChar); // Standard Grass (Baseline)
                case 2:
                    return new VoxelTexel(92, 165, 38, blockChar); // Soft Mid-Tone Green
                default:
                    return new VoxelTexel(98, 175, 42, blockChar); // Mild Highlights (Toned down)
            }
        }

        // --- FACE 2: BOTTOM DIRT FACE ---
        if (face == 2) {
            switch (noise) {
                case 0:
                    return new VoxelTexel(90, 62, 38, blockChar); // Soft Dark Dirt
                case 1:
                    return new VoxelTexel(98, 68, 42, blockChar); // Standard Dirt (Baseline)
                case 2:
                    return new VoxelTexel(106, 74, 46, blockChar); // Soft Mid-Tone Brown
                default:
                    return new VoxelTexel(114, 80, 50, blockChar); // Light Gravel Specks
            }
        }

        // --- FACES 0, 1, 4, 5: SIDE GRASS/DIRT BLENDS ---
        int grassThreshold = 4 + (x % 3 == 0 ? 1 : 0) + ((x * 7) % 2);

        if (y < grassThreshold) {
            // Side Grass Lip (Matches the toned-down Top Grass palette)
            switch (noise) {
                case 0:
                    return new VoxelTexel(80, 145, 30, blockChar);
                case 1:
                    return new VoxelTexel(85, 155, 35, blockChar);
                case 2:
                    return new VoxelTexel(92, 165, 38, blockChar);
                default:
                    return new VoxelTexel(98, 175, 42, blockChar);
            }
        } else {
            // Exposed Side Dirt (Matches the toned-down Bottom Dirt palette)
            switch (noise) {
                case 0:
                    return new VoxelTexel(90, 62, 38, blockChar);
                case 1:
                    return new VoxelTexel(98, 68, 42, blockChar);
                case 2:
                    return new VoxelTexel(106, 74, 46, blockChar);
                default:
                    return new VoxelTexel(114, 80, 50, blockChar);
            }
        }
    }

}
