public class TexelMario64CapBlockLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(15, "Entering Castle Grounds:"),
            new StatusStage(35, "Draining the Moat:"),
            new StatusStage(55, "Chasing Mips:"),
            new StatusStage(75, "Activating Cap Switches:"),
            new StatusStage(90, "Collecting Red Coins:"),
            new StatusStage(100, "Here We Go!:")
    };

    // Texture space resolution for this cube - matches the real in-game asset (32x32).
    private static final int TEXTURE_RESOLUTION = 32;

    // The real block texture always shows exactly 7 checker squares along each edge - 4 in the
    // block's own color and 3 white, interlaced, with a color square anchoring every corner.
    // That relies on the square *count* being odd (see BORDER_SEGMENT_BOUNDS below for why).
    //
    // 32 doesn't divide evenly by 7 (32 / 7 = 4.57), so the 7 squares can't all be exactly the
    // same width if they're going to sum to exactly 32. BORDER_SEGMENT_BOUNDS splits 32 into 7
    // segments as evenly as possible - a symmetric mix of 4px and 5px - rather than dumping the
    // leftover remainder onto one edge the way the very first version did (that's what caused
    // the original "boundary line instead of squares" bug). The 1px difference between a 4-wide
    // and a 5-wide square is invisible at this scale; what matters for the corners is the
    // *count* (7, odd), not the exact width of each one.
    private static final int BORDER_SQUARES_PER_EDGE = 7;
    private static final int[] BORDER_SEGMENT_BOUNDS =
            computeSegmentBounds(TEXTURE_RESOLUTION, BORDER_SQUARES_PER_EDGE);
    // How deep the checkerboard ribbon extends inward from each edge.
    private static final int BORDER_THICKNESS =
            (TEXTURE_RESOLUTION + BORDER_SQUARES_PER_EDGE - 1) / BORDER_SQUARES_PER_EDGE; // ceil(32/7) = 5
    // Width of the black frame line, just inside the checkerboard.
    private static final int FRAME_THICKNESS = Math.max(1, Math.round(TEXTURE_RESOLUTION / 32f));
    // Where the solid inner panel begins (distance from any edge).
    private static final int INNER_START = BORDER_THICKNESS + FRAME_THICKNESS;
    private static final int INNER_SIZE = TEXTURE_RESOLUTION - 2 * INNER_START;

    // The "!" is built as actual geometry (a rounded cap that tapers to a point, plus a
    // separate circular dot) rather than a fixed-width rectangle. A constant-width box is what
    // made it look "skinny" - the real glyph is more like a tall teardrop / map-pin silhouette:
    // round through the top, narrowing to a point at the bottom of the stem.
    private static final double STEM_Y_START = INNER_START + Math.round(INNER_SIZE * 0.14);
    private static final double STEM_Y_END = INNER_START + Math.round(INNER_SIZE * 0.62);
    private static final double DOT_CENTER_Y = INNER_START + Math.round(INNER_SIZE * 0.77);

    // Radius of the stem's rounded top - this is what got "fattened up" (was an implicit ~7%
    // of the inner panel as a flat rectangle; now an explicit, wider 20% used as an actual
    // circle radius for the cap).
    private static final double STEM_CAP_RADIUS = INNER_SIZE * 0.13;
    private static final double STEM_TAPER_HEIGHT = (STEM_Y_END - STEM_Y_START) - STEM_CAP_RADIUS;
    private static final double DOT_RADIUS = INNER_SIZE * 0.082;

    /** Splits {@code length} into {@code count} segments, as equal as possible, that sum to
     *  exactly {@code length} - with any leftover pixels distributed symmetrically from the
     *  outer segments inward - and returns their cumulative boundaries (size {@code count + 1};
     *  {@code bounds[0] == 0}, {@code bounds[count] == length}). */
    private static int[] computeSegmentBounds(int length, int count) {
        int[] sizes = new int[count];
        java.util.Arrays.fill(sizes, length / count);
        int left = 0, right = count - 1, remaining = length % count;
        while (remaining > 0) {
            sizes[left]++;
            remaining--;
            if (remaining > 0 && right != left) {
                sizes[right]++;
                remaining--;
            }
            left++;
            right--;
        }
        int[] bounds = new int[count + 1];
        for (int i = 0; i < count; i++) {
            bounds[i + 1] = bounds[i] + sizes[i];
        }
        return bounds;
    }

    /** The index (0-based) of whichever segment in {@code bounds} contains {@code coord}. */
    private static int segmentIndexOf(int coord, int[] bounds) {
        for (int i = 0; i < bounds.length - 1; i++) {
            if (coord < bounds[i + 1]) {
                return i;
            }
        }
        return bounds.length - 2;
    }

    private int baseR, baseG, baseB;

    public TexelMario64CapBlockLoader() {
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    @Override
    protected int getTextureResolution() {
        return TEXTURE_RESOLUTION;
    }

    @Override
    protected void initialize() {
        initializePalette();
    }

    private void initializePalette() {
        int blockType = (int) (Math.random() * 4);

        switch (blockType) {
            case 0: // Red Block (Wing Cap)
                baseR = 215;
                baseG = 30;
                baseB = 30;
                break;
            case 1: // Green Block (Metal Cap)
                baseR = 25;
                baseG = 150;
                baseB = 60;
                break;
            case 2: // Blue Block (Vanish Cap)
                baseR = 35;
                baseG = 70;
                baseB = 190;
                break;
            case 3: // Yellow Block (Standard Item Box)
            default:
                baseR = 235;
                baseG = 165;
                baseB = 10;
                break;
        }
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int size = TEXTURE_RESOLUTION;

        // --- 1. CHECKERBOARD BORDER (color / white) ---
        // The real block texture's border is a color-and-white checkerboard ribbon running
        // all the way around each face, not a grey bevel. `edgeDist` is the true distance to
        // the *nearest* edge (symmetric in both directions), which is what the old fold-based
        // version was trying to compute but got wrong (it only folded the last two rows/columns,
        // so every edge's checker pattern glitched right at the corner).
        int edgeDist = Math.min(Math.min(x, size - 1 - x), Math.min(y, size - 1 - y));

        if (edgeDist < BORDER_THICKNESS) {
            int cellX = segmentIndexOf(x, BORDER_SEGMENT_BOUNDS);
            int cellY = segmentIndexOf(y, BORDER_SEGMENT_BOUNDS);
            if ((cellX + cellY) % 2 == 0) {
                return new VoxelTexel(baseR, baseG, baseB, '\u2593'); // Color check (▓)
            } else {
                return new VoxelTexel(255, 255, 255, '\u2588'); // White check (█)
            }
        }

        // --- 2. BLACK FRAME LINE ---
        // A thin black outline separates the checkerboard border from the solid inner panel -
        // this was missing entirely before, and is most of what reads as "there's a white
        // border in there": a white checker square sitting flush against a crisp black edge.
        if (edgeDist < INNER_START) {
            return new VoxelTexel(0, 0, 0, '\u2591'); // (░)
        }

        // --- 3. EXCLAMATION POINT (!), centered in the inner panel ---
        // Rendered only on the four vertical side faces (0 = Back, 1 = Front, 4 = Left, 5 = Right).
        // The stem is a rounded cap (a quarter-circle arc, widest at STEM_CAP_RADIUS below its
        // top point) that then tapers linearly down to a point; the dot is a plain circle. Both
        // are computed directly from geometry rather than boxed into a fixed-width rectangle, so
        // the "pin" silhouette holds up at any TEXTURE_RESOLUTION.
        if (face == 0 || face == 1 || face == 4 || face == 5) {
            double xf = x + 0.5;
            double yf = y + 0.5;
            double centerX = size / 2.0;

            if (yf >= STEM_Y_START && yf < STEM_Y_END) {
                double yPrime = yf - STEM_Y_START;
                double halfWidth;
                if (yPrime < STEM_CAP_RADIUS) {
                    // Rounded top: a quarter-circle arc from a point at the very top out to full
                    // radius at yPrime == STEM_CAP_RADIUS.
                    double d = STEM_CAP_RADIUS - yPrime;
                    halfWidth = Math.sqrt(Math.max(0.0, STEM_CAP_RADIUS * STEM_CAP_RADIUS - d * d));
                } else {
                    // Linear taper from full radius down to a point at STEM_Y_END.
                    double t = (yPrime - STEM_CAP_RADIUS) / STEM_TAPER_HEIGHT;
                    halfWidth = STEM_CAP_RADIUS * (1.0 - t);
                }
                if (Math.abs(xf - centerX) <= halfWidth) {
                    return new VoxelTexel(255, 255, 255, '\u2588'); // Pure white symbol (█)
                }
            }

            double dx = xf - centerX;
            double dy = yf - DOT_CENTER_Y;
            if (dx * dx + dy * dy <= DOT_RADIUS * DOT_RADIUS) {
                return new VoxelTexel(255, 255, 255, '\u2588');
            }
        }

        // --- 4. SOLID INNER PANEL ---
        return new VoxelTexel(baseR, baseG, baseB, '\u2593'); // (▓)
    }
}