public class TexelPlasmaCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
        new StatusStage(20, "Syncing matrix lattices:"),
        new StatusStage(40, "Quantum dot alignment:"),
        new StatusStage(60, "Calibrating plasma waves:"),
        new StatusStage(80, "Splicing bio-circuits:"),
        new StatusStage(98, "Defragmenting neural core:"),
        new StatusStage(100, "Cybernetic Lattice Online!")
    };

    public TexelPlasmaCubeLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        long time = System.currentTimeMillis();

        // 1. AFFINE MATRIX BOUNDARY SEPARATORS
        // Dimensions range from 0 to 15 on the 16x16 voxel texture patch faces
        boolean isOuterRim = (x == 0 || x == 15 || y == 0 || y == 15);
        boolean isSubPanelFrame = (x == 3 || x == 12 || y == 3 || y == 12);
        
        // Permanent structural frame mask
        boolean isHardwiredCircuit = isOuterRim || isSubPanelFrame;

        // 2. MULTI-FREQUENCY PLASMA WAVE GENERATOR
        // Converts current timeline ticks into continuous radians
        double tAngle = time * 0.0035;

        // Combine space coordinates and time coordinates to form complex shifting patterns
        // We use geometric dot weights that warp differently depending on which 3D cube face is facing us
        double wave1 = Math.sin((x * 0.35) + tAngle) * Math.cos((y * 0.35) - tAngle * 0.5);
        double wave2 = Math.sin((y * 0.25) + (face * 1.25) + tAngle * 0.8) * Math.cos((x * 0.25) + tAngle);
        double wave3 = Math.sin(Math.sqrt(Math.pow(x - 7.5, 2) + Math.pow(y - 7.5, 2)) * 0.4 - tAngle * 1.4);

        // Blended wave magnitude normalized between -1.0 and +1.0
        double plasmaField = (wave1 + wave2 + wave3) / 3.0;

        // Color registers
        int brailleR = 0;
        int brailleG = 0;
        int brailleB = 0;
        char renderChar;

        // 3. CYBERNETIC TEXTURE COMPOSITING
        if (isHardwiredCircuit) {
            // LAYER A: THE EMBEDDED CYBERNETIC CHASSIS (Fixed Structural Frame)
            // Stays anchored to the surface to preserve the cube's geometric identity.
            // Pulses with an electric, high-contrast neon aqua-cyan wave.
            double framePulse = 0.5 + 0.5 * Math.sin(tAngle * 2.0 + (x + y) * 0.15);
            
            brailleR = (int) (20  * framePulse);
            brailleG = (int) (180 + 75 * framePulse);
            brailleB = (int) (210 + 45 * framePulse);

            // Hardwired frames use dense, heavy geometric characters to hold structure
            // If it rolls on a crosshair spine node, draw an intersection vertex symbol
            if (x == y || x == 15 - y) {
                renderChar = '#'; // Metallic hardware intersection joints
            } else {
                renderChar = (isOuterRim) ? '█' : '='; // Solid circuit trace lines
            }

        } else {
            // LAYER B: THE LIVING BIO-PLASMA STATIC FIELD (Flickering Core)
            // Evaluates the continuous plasma wave equation to map a fluid color gradient,
            // then adds a fast-noise generator to create an erratic TV static fuzz.
            
            // Map the plasma field variable into 3 independent phase-shifted channels
            double rChannel = Math.sin(plasmaField * Math.PI + tAngle);
            double gChannel = Math.sin(plasmaField * Math.PI + tAngle + (2.0 * Math.PI / 3.0));
            double bChannel = Math.sin(plasmaField * Math.PI + tAngle + (4.0 * Math.PI / 3.0));

            // Shift from deep neon magenta/purple to high-saturation violet and electric emeralds
            brailleR = (int) (140 + 115 * rChannel);
            brailleG = (int) (40  + 215 * Math.max(0.0, gChannel)); // Keeps green selective
            brailleB = (int) (160 + 95  * bChannel);

            // CRUCIAL UPGRADE: DYNAMIC SUB-PIXEL FRAGMENTATION
            // Generates an unpredictable hex offset mask from the 256-cell Braille table block (0x2800)
            // every single frame. This produces an intense, high-frequency, fuzzing TV static look.
            int brailleBitmask = (int) (Math.random() * 256);

            // NASA Detail: We tie the minimum number of activated Braille dots to our local plasma intensity!
            // When plasma waves peak, the noise gets thicker and more dense. When it dips, it thins down.
            if (plasmaField > 0.35) {
                brailleBitmask |= (1 | 8 | 64 | 128); // Force minimum dense bottom/top bounding rows
            } else if (plasmaField < -0.35) {
                brailleBitmask &= (2 | 16 | 4 | 32);  // Filter down to sparse interior filaments
            }

            renderChar = (char) (0x2800 + brailleBitmask);

        }

        // Enforce safety limits to guard the 24-bit TrueColor buffer arrays
        brailleR = Math.max(0, Math.min(255, brailleR));
        brailleG = Math.max(0, Math.min(255, brailleG)); 
        brailleB = Math.max(0, Math.min(255, brailleB));

        return new VoxelTexel(brailleR, brailleG, brailleB, renderChar);
    }
}
