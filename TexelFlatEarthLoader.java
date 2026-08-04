public class TexelFlatEarthLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(25, "Condensing Tectonic Plates:"),
            new StatusStage(55, "Filling Cubical Ocean Depths:"),
            new StatusStage(85, "Freezing Square Polar Icecaps:"),
            new StatusStage(100, "Planetary Orbit Synchronized!")
    };

    public TexelFlatEarthLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // 2. Polar Ice Cap Layers (Top and Bottom poles)
        double px = x - 7.5;
        double py = y - 7.5;
        double polarRadiusSq = px * px + py * py;

        char blockChar = '\u2588'; // █ (Solid pixel fill)

        if (face == 3 && polarRadiusSq < 22.0) { // Top Face: Arctic Pole
            int ice = 220 + noise * 10;
            // Boost blue slightly (ice, ice, 255) to guarantee a frozen white/cyan look,
            // preventing yellow drift
            return new VoxelTexel(ice, ice, 255, blockChar);
        }
        if (face == 2 && polarRadiusSq < 18.0) { // Bottom Face: Antarctic Pole
            int ice = 220 + noise * 10;
            return new VoxelTexel(ice, ice, 255, blockChar);
        }

        // 3. Normalize incoming 0-15 grid coordinates to a clean 0.0 - 1.0 range
        double tu = x / 15.0;
        double tv = y / 15.0;

        // Flip the face wrapping rotations safely
        double u = 0, v = 0;
        switch (face) {
            case 0:
                u = tu;
                v = 1.0 - tv;
                break; // Back
            case 1:
                u = 1.0 - tu;
                v = 1.0 - tv;
                break; // Front
            case 4:
                u = 1.0 - tv;
                v = 1.0 - tu;
                break; // Left
            case 5:
                u = 1.0 - tv;
                v = tu;
                break; // Right
            case 2:
            case 3:
                u = tu;
                v = tv;
                break; // Bottom & Top
        }

        // 4. Map to true un-rotated 3D cube coordinates (-1.0 to 1.0)
        double uc = 2.0 * u - 1.0;
        double vc = 2.0 * v - 1.0;
        double cx = 0, cy = 0, cz = 0;
        switch (face) {
            case 0:
                cx = uc;
                cy = vc;
                cz = -1.0;
                break;
            case 1:
                cx = uc;
                cy = vc;
                cz = 1.0;
                break;
            case 2:
                cx = uc;
                cy = -1.0;
                cz = vc;
                break;
            case 3:
                cx = uc;
                cy = 1.0;
                cz = vc;
                break;
            case 4:
                cx = -1.0;
                cy = uc;
                cz = vc;
                break;
            case 5:
                cx = 1.0;
                cy = uc;
                cz = vc;
                break;
        }

        // 5. Translate 3D space back into a stable 0-15 wave coordinate system
        double wx = (cx + 1.0) * 7.5;
        double wy = (cy + 1.0) * 7.5;
        double wz = (cz + 1.0) * 7.5;

        // 6. 3D Cyclic Wave Math for continuous, wrapping continents
        double wave1 = Math.sin(wx * 0.45) * Math.cos(wy * 0.45);
        double wave2 = Math.sin(wz * 0.45) * Math.cos(wx * 0.45);
        double wave3 = Math.sin(wy * 0.45) * Math.cos(wz * 0.45);

        double landForm = (wave1 + wave2 + wave3) * 0.65;

        // --- EARTH TEXTURE COMPOSITING PIPELINE ---

        // --- EARTH TEXTURE COMPOSITING PIPELINE ---

        if (landForm > 0.05) {
            // Layer A: Shoreline Sand (Clean light beige)
            if (landForm < 0.12) {
                int sandR = 225 + noise * 5;
                int sandG = 210 + noise * 3;
                int sandB = 160;
                return new VoxelTexel(sandR, sandG, sandB, blockChar);
            }

            // Layer B: Higher Altitude Peaks (Rich, deep forest green)
            if (landForm > 0.32) {
                int highLandG = 120 + noise * 10; // Darker emerald for topographic depth
                return new VoxelTexel(10, highLandG, 30, blockChar);
            }

            // Layer C: Standard Lowland Grass Fields (Crisp, vibrant green)
            int landG = 165 + noise * 15;
            return new VoxelTexel(20, landG, 40, blockChar);

        } else {
            // Layer D: Shallow Coastal Waters (Right next to beaches)
            if (landForm > -0.15) {
                int coastB = 210 + noise * 12;
                return new VoxelTexel(10, 140 + noise * 8, coastB, '\u2593'); // ▓ (Bright teal shelf)
            }

            // Layer E: Deep Abyssal Ocean Plunge
            int oceanB = 170 + noise * 10;
            return new VoxelTexel(10, 60 + noise * 5, oceanB, '\u2592'); // ▒ (Dark royal blue ocean)
        }
    }
}
