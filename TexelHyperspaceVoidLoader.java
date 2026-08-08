public class TexelHyperspaceVoidLoader extends TexelCubeLoader {
    private static final StatusStage[] VOID_CUBE_STAGES = new StatusStage[] {
            new StatusStage(15, "Breaching event horizon:"),
            new StatusStage(35, "Bending spacetime metrics:"),
            new StatusStage(55, "Gravitational lensing active:"),
            new StatusStage(75, "Decoding tachyon radiation:"),
            new StatusStage(95, "Singularity stabilization:"),
            new StatusStage(100, "Hyperspace Void Stabilized!")
    };

    public TexelHyperspaceVoidLoader() {
        super(VOID_CUBE_STAGES, 80, 22);
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        long time = System.currentTimeMillis();

        // 1. NON-LINEAR SPACE WARPING (The Singularity Engine)
        double cx = x - 7.5;
        double cy = y - 7.5;

        double radius = Math.sqrt(cx * cx + cy * cy);
        double angle = Math.atan2(cy, cx);

        double tAngle = time * 0.0042;

        // 2. CORNER PULSE MATH (Out of Phase Rings)
        // Find the absolute distance to the nearest of the 4 outer texture corners
        double cornerX = (x < 8) ? 0 : 15;
        double cornerY = (y < 8) ? 0 : 15;
        double cornerRadius = Math.sqrt(Math.pow(x - cornerX, 2) + Math.pow(y - cornerY, 2));

        // Generates rings originating from corners, shifted 180 degrees (Math.PI) out
        // of phase
        double cornerRing = Math.sin(cornerRadius * 1.5 + (tAngle + Math.PI));

        // 3. OSCILLATING EVENT HORIZON MASK
        double horizonRadius = 3.5 + 2.0 * Math.sin(tAngle * 0.7);
        boolean isSingularityCore = (radius < 1.8);
        boolean isEventHorizon = (radius >= horizonRadius - 0.4 && radius <= horizonRadius + 0.4);

        int r = 0;
        int g = 0;
        int b = 0;
        char renderChar;

        // 4. COGNITIVE DIMENSIONAL COMPOSITING
        if (isSingularityCore) {
            // LAYER A: THE VOID HEART
            r = 0;
            g = (int) (15 + 15 * Math.sin(tAngle * 5.0));
            b = 0;
            renderChar = ' ';

        } else if (isEventHorizon) {
            // LAYER B: THE ACCRETION DISK EDGE
            double flash = 0.5 + 0.5 * Math.sin(tAngle * 3.5 + radius);
            r = (int) (70 + 35 * flash);
            g = (int) (120 + 40 * flash);
            b = (int) (20 * flash);

            renderChar = ((int) (angle * 4 / Math.PI) % 2 == 0) ? '\u2592' : '\u2591';

        } else {
            // LAYER C: THE SPIRAL TIME-WARP WITH CORNER INTERFERENCE
            double spiralWarp = angle + (3.5 / (radius + 0.5)) - tAngle;

            double ringPattern1 = Math.sin(radius * 1.5 + tAngle);
            double ringPattern2 = -Math.sin(spiralWarp * 3.0 + (face * 0.5));

            // Blend the center vortex with the out-of-phase corner waves
            double compositeVortex = (ringPattern1 * ringPattern2 + cornerRing) / 2.0;

            // Map the combined patterns into color registers
            double rChannel = Math.sin(compositeVortex * Math.PI);
            double gChannel = Math.sin(compositeVortex * Math.PI + (Math.PI / 2.0));
            double bChannel = Math.cos(compositeVortex * Math.PI - tAngle);

            r = (int) (130 + 125 * rChannel);
            g = (int) (20 + 80 * Math.max(0.0, gChannel));
            b = (int) (180 + 75 * bChannel);

            // 5. TRAPPY MATHEMATICAL GLYPH MATRIX
            // Injecting corner ring coordinates into the glyph seed so characters shift
            // with the wave
            int matrixSeed = (int) (Math.abs(spiralWarp * 5.0 + radius * 3.0 + cornerRadius * 2.0)) % 8;

            switch (matrixSeed) {
                case 0:
                    renderChar = '∞';
                    break;
                case 1:
                    renderChar = '√';
                    break;
                case 2:
                    renderChar = '∫';
                    break;
                case 3:
                    renderChar = 'Δ';
                    break;
                case 4:
                    renderChar = '¤';
                    break;
                case 5:
                    renderChar = '≈';
                    break;
                case 6:
                    renderChar = '×';
                    break;
                default:
                    renderChar = '·';
                    break;
            }

            if (compositeVortex > 0.5) {
                r = Math.min(255, r + 45);
                b = Math.min(255, b + 45);
            }

            // Green Corners
            // 1. Map x and y to distance from the nearest edge (0.0 at edge, 7.5 at center)
            double dx = Math.min(x, 15.0 - x);
            double dy = Math.min(y, 15.0 - y);

            // 2. Compute the distance to the closest corner
            double cornerDist = Math.sqrt(dx * dx + dy * dy);

            // 3. Apply an exponential falloff (Adjust 2.0 to change sharpness)
            double cornerFactor = Math.exp(-2.0 * cornerDist);

            g += 120.0 * cornerFactor;
        }

        // Enforce absolute safety limits
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new VoxelTexel(r, g, b, renderChar);
    }
}
