public class TexelHyperchromaticKaleidoscopeLoader extends TexelCubeLoader {
    private static final StatusStage[] KALEIDO_STAGES = new StatusStage[] {
        new StatusStage(15, "Splitting light matrices:"),
        new StatusStage(40, "Deploying geometric mirrors:"),
        new StatusStage(65, "Syncing RGB phase strobes:"),
        new StatusStage(85, "Overclocking optical prisms:"),
        new StatusStage(99, "Fractal feedback loop stable:"),
        new StatusStage(100, "Hyperchromatic Matrix Online!")
    };

    public TexelHyperchromaticKaleidoscopeLoader() {
        // Keeps the optimized 80x22 console resolution
        super(KALEIDO_STAGES, 80, 22);
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        long time = System.currentTimeMillis();
        
        // 1. KALEIDOSCOPIC MIRROR COORDINATE MAPPING
        // Center coordinates onto a floating point system from -7.5 to 7.5
        double cx = x - 7.5;
        double cy = y - 7.5;

        // Polar conversions for rotation/mirror calculation
        double radius = Math.sqrt(cx * cx + cy * cy);
        double angle = Math.atan2(cy, cx);

        // Map timeline into an aggressive, fast-paced rotational velocity
        double tAngle = time * 0.0065; 

        // Split the 360-degree space into an 8-way kaleidoscope reflection symmetry grid
        double wedgeSize = Math.PI / 4.0; // 45-degree slices
        double localWedgeAngle = Math.abs(((angle + Math.PI) % wedgeSize) - (wedgeSize / 2.0));

        // Reconstruct mirrored Cartesian coordinates based on the kaleidoscope slice
        double kx = radius * Math.cos(localWedgeAngle);
        double ky = radius * Math.sin(localWedgeAngle);

        // 2. FRACTAL SYMMETRY GRID GEOMETRY
        // Generate high-density geometric interference bands using the mirrored mapping
        double patternA = Math.sin(kx * 1.8 + Math.cos(tAngle * 1.5));
        double patternB = Math.cos(ky * 1.8 + Math.sin(tAngle * 1.2));
        // Deep recursive mathematical echo for a layered matrix layout
        double patternC = Math.sin((kx + ky) * 0.95 - tAngle * 2.5);

        // Combined kaleidoscopic signal frequency field
        double mirrorField = (patternA * patternB) + patternC;

        // 3. AGGRESSIVE HYPER-SPEED PHASE SHIFTING (The Psychedelic Trippy Strobe)
        // Every single primary color color-channel runs at a wildly unique frequency speed multiplier.
        // This shifts the cube through complete color spectrum cycles in fractions of a second.
        double rStrobe = Math.sin(mirrorField * Math.PI + tAngle * 3.0);
        double gStrobe = Math.sin(mirrorField * Math.PI + tAngle * 1.5 + (Math.PI / 3.0));
        double bStrobe = Math.cos(mirrorField * Math.PI + tAngle * 4.5 + (2.0 * Math.PI / 3.0));

        // Shift between pure electric neon lime greens, deep acid magenta, and cyan spikes
        int r = (int) (127 + 128 * rStrobe);
        int g = (int) (127 + 128 * gStrobe);
        int b = (int) (127 + 128 * bStrobe);

        // 4. TRAPPY HIGH-CONTRAST CENTRAL CROSS PRISM
        // Generates an expanding laser-sharp central diamond that cross-cuts through the mirror axes
        double diamondDistance = Math.abs(cx) + Math.abs(cy);
        double diamondPulseRadius = 6.0 + 3.0 * Math.cos(tAngle * 2.0);
        boolean isPrismLine = Math.abs(diamondDistance - diamondPulseRadius) < 0.6;

        char renderChar;

        if (isPrismLine) {
            // Hot white geometric inversion flash along the exploding diamond wave rings
            r = 255;
            g = 255;
            b = 255;
            renderChar = '█'; // Solid block elements block light reflections completely
        } else {
            // 5. TRAPPY KALEIDOSCOPIC ICONOGRAPHY FONT SELECTOR
            // Select structured characters based on the symmetrical mirrored coordinates 
            // This maintains perfect geometry across the reflection mirrors
            int glyphSeed = (int) (Math.abs(mirrorField * 6.0 + radius)) % 6;
            
            switch (glyphSeed) {
                case 0: renderChar = '╬'; break; // Quad-intersection lattice node
                case 1: renderChar = '◈'; break; // Balanced diamond crystal core
                case 2: renderChar = '❖'; break; // Ornate central mandala point
                case 3: renderChar = '═'; break; // Horizontal laser array trace
                case 4: renderChar = '║'; break; // Vertical laser array trace
                default: renderChar = '░'; break; // Low density chromatic light noise matrix
            }
        }

        // Absolute safety boundary checks to lock colors into 24-bit TrueColor guidelines
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new VoxelTexel(r, g, b, renderChar);
    }
}
