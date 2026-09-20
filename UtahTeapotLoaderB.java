// "B" variant of the Utah Teapot loader: instead of plotting each baked
// surface point as an isolated character, this fills in real triangular
// faces and Gouraud-shades them from the same per-vertex analytic normals
// UtahTeapotLoaderA already evaluated -- so the teapot reads as a
// continuous glossy surface instead of a scatter of dots.
//
// SuzanneTheMonkeyLoaderB / StanfordDragonLoaderB / StanfordBunnyLoaderB
// all have to *reconstruct* face connectivity from a raw, unstructured
// point cloud: for every point, find its nearest neighbors, sort them by
// angle around that point's own tangent plane, fan them into triangles,
// then dedupe the same physical triangle away from however many
// different apex points rediscovered it. None of that applies here.
// bakeMesh() below tessellates each of the 32 patches on its own regular
// (PATCH_RES+1) x (PATCH_RES+1) grid, in a fixed, already-known row-major
// order -- so the quad between tessellated grid cell (i, j) and its
// neighbors (i+1, j), (i, j+1), (i+1, j+1) is always exactly four
// particular array indices, known in closed form, with zero search and
// zero ambiguity. buildFaces() below just walks that grid directly and
// splits each quad into two triangles -- cheaper and exact, instead of
// approximate and O(n^2).
//
// (Triangle winding is not oriented to match the vertex normals the way
// the other loaders' buildFaces() do, because it doesn't need to be:
// rasterizeTriangle()'s barycentric weights are invariant to swapping two
// corners -- doing so flips the sign of `area` and every w0/w1/w2 term
// that divides by it in lockstep, leaving the inside/outside test and the
// interpolated depth/luminance untouched -- and the backface cull below
// reads each vertex's own already-analytic normal directly rather than
// any winding-derived face normal.)
//
// A few patches (the lid's knob and rim) repeat a single control point
// across an entire row of their 4x4 grid, which collapses one whole edge
// of that patch's tessellated grid down to a single physical 3D point.
// The quads touching that collapsed edge end up with two of their three
// corners landing on the exact same point -- physically correct (that's
// what a real cone tip looks like) and harmless: rasterizeTriangle()'s
// existing near-zero-area guard already skips them with no special
// casing needed here.

public class UtahTeapotLoaderB extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(10, "Loading 32 bicubic Bezier patches (Newell/Blinn dataset)..."),
        new StatusStage(30, "Evaluating De Casteljau control point lattice..."),
        new StatusStage(50, "Deriving analytic tangents and surface normals..."),
        new StatusStage(70, "Meshing patch grids into quad-split triangular faces..."),
        new StatusStage(90, "Z-Buffer occlusion test successful..."),
        new StatusStage(100, "Gouraud-Shaded Rasterization Complete!")
    };

    // Automated rotation tracking angles. angleX is set once in
    // initialize() to a fixed tilt and never touched again inside
    // renderGeometry() -- only angleY advances per frame -- so the teapot
    // spins steadily around a single upright axis, matching
    // UtahTeapotLoaderA and every other loader in this set.
    private double angleX = 0.0;
    private double angleY = 0.0;

    private final int width;
    private final int height;

    // -----------------------------------------------------------------
    // THE REAL DATA. 306 control points feeding 32 sixteen-point bicubic
    // Bezier patches (rim, body, lid, bottom, handle, spout). Index
    // numbers below are the original 1-based FORTRAN-era numbering; the
    // mesh baker below subtracts 1 when it looks them up.
    // -----------------------------------------------------------------
    private static final double[][] CONTROL_POINTS = {
        {1.4, 0.0, 2.4},        {1.4, -0.784, 2.4},        {0.784, -1.4, 2.4},        {0.0, -1.4, 2.4},
        {1.3375, 0.0, 2.53125},        {1.3375, -0.749, 2.53125},        {0.749, -1.3375, 2.53125},        {0.0, -1.3375, 2.53125},
        {1.4375, 0.0, 2.53125},        {1.4375, -0.805, 2.53125},        {0.805, -1.4375, 2.53125},        {0.0, -1.4375, 2.53125},
        {1.5, 0.0, 2.4},        {1.5, -0.84, 2.4},        {0.84, -1.5, 2.4},        {0.0, -1.5, 2.4},
        {-0.784, -1.4, 2.4},        {-1.4, -0.784, 2.4},        {-1.4, 0.0, 2.4},        {-0.749, -1.3375, 2.53125},
        {-1.3375, -0.749, 2.53125},        {-1.3375, 0.0, 2.53125},        {-0.805, -1.4375, 2.53125},        {-1.4375, -0.805, 2.53125},
        {-1.4375, 0.0, 2.53125},        {-0.84, -1.5, 2.4},        {-1.5, -0.84, 2.4},        {-1.5, 0.0, 2.4},
        {-1.4, 0.784, 2.4},        {-0.784, 1.4, 2.4},        {0.0, 1.4, 2.4},        {-1.3375, 0.749, 2.53125},
        {-0.749, 1.3375, 2.53125},        {0.0, 1.3375, 2.53125},        {-1.4375, 0.805, 2.53125},        {-0.805, 1.4375, 2.53125},
        {0.0, 1.4375, 2.53125},        {-1.5, 0.84, 2.4},        {-0.84, 1.5, 2.4},        {0.0, 1.5, 2.4},
        {0.784, 1.4, 2.4},        {1.4, 0.784, 2.4},        {0.749, 1.3375, 2.53125},        {1.3375, 0.749, 2.53125},
        {0.805, 1.4375, 2.53125},        {1.4375, 0.805, 2.53125},        {0.84, 1.5, 2.4},        {1.5, 0.84, 2.4},
        {1.75, 0.0, 1.875},        {1.75, -0.98, 1.875},        {0.98, -1.75, 1.875},        {0.0, -1.75, 1.875},
        {2.0, 0.0, 1.35},        {2.0, -1.12, 1.35},        {1.12, -2.0, 1.35},        {0.0, -2.0, 1.35},
        {2.0, 0.0, 0.9},        {2.0, -1.12, 0.9},        {1.12, -2.0, 0.9},        {0.0, -2.0, 0.9},
        {-0.98, -1.75, 1.875},        {-1.75, -0.98, 1.875},        {-1.75, 0.0, 1.875},        {-1.12, -2.0, 1.35},
        {-2.0, -1.12, 1.35},        {-2.0, 0.0, 1.35},        {-1.12, -2.0, 0.9},        {-2.0, -1.12, 0.9},
        {-2.0, 0.0, 0.9},        {-1.75, 0.98, 1.875},        {-0.98, 1.75, 1.875},        {0.0, 1.75, 1.875},
        {-2.0, 1.12, 1.35},        {-1.12, 2.0, 1.35},        {0.0, 2.0, 1.35},        {-2.0, 1.12, 0.9},
        {-1.12, 2.0, 0.9},        {0.0, 2.0, 0.9},        {0.98, 1.75, 1.875},        {1.75, 0.98, 1.875},
        {1.12, 2.0, 1.35},        {2.0, 1.12, 1.35},        {1.12, 2.0, 0.9},        {2.0, 1.12, 0.9},
        {2.0, 0.0, 0.45},        {2.0, -1.12, 0.45},        {1.12, -2.0, 0.45},        {0.0, -2.0, 0.45},
        {1.5, 0.0, 0.225},        {1.5, -0.84, 0.225},        {0.84, -1.5, 0.225},        {0.0, -1.5, 0.225},
        {1.5, 0.0, 0.15},        {1.5, -0.84, 0.15},        {0.84, -1.5, 0.15},        {0.0, -1.5, 0.15},
        {-1.12, -2.0, 0.45},        {-2.0, -1.12, 0.45},        {-2.0, 0.0, 0.45},        {-0.84, -1.5, 0.225},
        {-1.5, -0.84, 0.225},        {-1.5, 0.0, 0.225},        {-0.84, -1.5, 0.15},        {-1.5, -0.84, 0.15},
        {-1.5, 0.0, 0.15},        {-2.0, 1.12, 0.45},        {-1.12, 2.0, 0.45},        {0.0, 2.0, 0.45},
        {-1.5, 0.84, 0.225},        {-0.84, 1.5, 0.225},        {0.0, 1.5, 0.225},        {-1.5, 0.84, 0.15},
        {-0.84, 1.5, 0.15},        {0.0, 1.5, 0.15},        {1.12, 2.0, 0.45},        {2.0, 1.12, 0.45},
        {0.84, 1.5, 0.225},        {1.5, 0.84, 0.225},        {0.84, 1.5, 0.15},        {1.5, 0.84, 0.15},
        {-1.6, 0.0, 2.025},        {-1.6, -0.3, 2.025},        {-1.5, -0.3, 2.25},        {-1.5, 0.0, 2.25},
        {-2.3, 0.0, 2.025},        {-2.3, -0.3, 2.025},        {-2.5, -0.3, 2.25},        {-2.5, 0.0, 2.25},
        {-2.7, 0.0, 2.025},        {-2.7, -0.3, 2.025},        {-3.0, -0.3, 2.25},        {-3.0, 0.0, 2.25},
        {-2.7, 0.0, 1.8},        {-2.7, -0.3, 1.8},        {-3.0, -0.3, 1.8},        {-3.0, 0.0, 1.8},
        {-1.5, 0.3, 2.25},        {-1.6, 0.3, 2.025},        {-2.5, 0.3, 2.25},        {-2.3, 0.3, 2.025},
        {-3.0, 0.3, 2.25},        {-2.7, 0.3, 2.025},        {-3.0, 0.3, 1.8},        {-2.7, 0.3, 1.8},
        {-2.7, 0.0, 1.575},        {-2.7, -0.3, 1.575},        {-3.0, -0.3, 1.35},        {-3.0, 0.0, 1.35},
        {-2.5, 0.0, 1.125},        {-2.5, -0.3, 1.125},        {-2.65, -0.3, 0.9375},        {-2.65, 0.0, 0.9375},
        {-2.0, -0.3, 0.9},        {-1.9, -0.3, 0.6},        {-1.9, 0.0, 0.6},        {-3.0, 0.3, 1.35},
        {-2.7, 0.3, 1.575},        {-2.65, 0.3, 0.9375},        {-2.5, 0.3, 1.125},        {-1.9, 0.3, 0.6},
        {-2.0, 0.3, 0.9},        {1.7, 0.0, 1.425},        {1.7, -0.66, 1.425},        {1.7, -0.66, 0.6},
        {1.7, 0.0, 0.6},        {2.6, 0.0, 1.425},        {2.6, -0.66, 1.425},        {3.1, -0.66, 0.825},
        {3.1, 0.0, 0.825},        {2.3, 0.0, 2.1},        {2.3, -0.25, 2.1},        {2.4, -0.25, 2.025},
        {2.4, 0.0, 2.025},        {2.7, 0.0, 2.4},        {2.7, -0.25, 2.4},        {3.3, -0.25, 2.4},
        {3.3, 0.0, 2.4},        {1.7, 0.66, 0.6},        {1.7, 0.66, 1.425},        {3.1, 0.66, 0.825},
        {2.6, 0.66, 1.425},        {2.4, 0.25, 2.025},        {2.3, 0.25, 2.1},        {3.3, 0.25, 2.4},
        {2.7, 0.25, 2.4},        {2.8, 0.0, 2.475},        {2.8, -0.25, 2.475},        {3.525, -0.25, 2.49375},
        {3.525, 0.0, 2.49375},        {2.9, 0.0, 2.475},        {2.9, -0.15, 2.475},        {3.45, -0.15, 2.5125},
        {3.45, 0.0, 2.5125},        {2.8, 0.0, 2.4},        {2.8, -0.15, 2.4},        {3.2, -0.15, 2.4},
        {3.2, 0.0, 2.4},        {3.525, 0.25, 2.49375},        {2.8, 0.25, 2.475},        {3.45, 0.15, 2.5125},
        {2.9, 0.15, 2.475},        {3.2, 0.15, 2.4},        {2.8, 0.15, 2.4},        {0.0, 0.0, 3.15},
        {0.0, -0.002, 3.15},        {0.002, 0.0, 3.15},        {0.8, 0.0, 3.15},        {0.8, -0.45, 3.15},
        {0.45, -0.8, 3.15},        {0.0, -0.8, 3.15},        {0.0, 0.0, 2.85},        {0.2, 0.0, 2.7},
        {0.2, -0.112, 2.7},        {0.112, -0.2, 2.7},        {0.0, -0.2, 2.7},        {-0.002, 0.0, 3.15},
        {-0.45, -0.8, 3.15},        {-0.8, -0.45, 3.15},        {-0.8, 0.0, 3.15},        {-0.112, -0.2, 2.7},
        {-0.2, -0.112, 2.7},        {-0.2, 0.0, 2.7},        {0.0, 0.002, 3.15},        {-0.8, 0.45, 3.15},
        {-0.45, 0.8, 3.15},        {0.0, 0.8, 3.15},        {-0.2, 0.112, 2.7},        {-0.112, 0.2, 2.7},
        {0.0, 0.2, 2.7},        {0.45, 0.8, 3.15},        {0.8, 0.45, 3.15},        {0.112, 0.2, 2.7},
        {0.2, 0.112, 2.7},        {0.4, 0.0, 2.55},        {0.4, -0.224, 2.55},        {0.224, -0.4, 2.55},
        {0.0, -0.4, 2.55},        {1.3, 0.0, 2.55},        {1.3, -0.728, 2.55},        {0.728, -1.3, 2.55},
        {0.0, -1.3, 2.55},        {1.3, 0.0, 2.4},        {1.3, -0.728, 2.4},        {0.728, -1.3, 2.4},
        {0.0, -1.3, 2.4},        {-0.224, -0.4, 2.55},        {-0.4, -0.224, 2.55},        {-0.4, 0.0, 2.55},
        {-0.728, -1.3, 2.55},        {-1.3, -0.728, 2.55},        {-1.3, 0.0, 2.55},        {-0.728, -1.3, 2.4},
        {-1.3, -0.728, 2.4},        {-1.3, 0.0, 2.4},        {-0.4, 0.224, 2.55},        {-0.224, 0.4, 2.55},
        {0.0, 0.4, 2.55},        {-1.3, 0.728, 2.55},        {-0.728, 1.3, 2.55},        {0.0, 1.3, 2.55},
        {-1.3, 0.728, 2.4},        {-0.728, 1.3, 2.4},        {0.0, 1.3, 2.4},        {0.224, 0.4, 2.55},
        {0.4, 0.224, 2.55},        {0.728, 1.3, 2.55},        {1.3, 0.728, 2.55},        {0.728, 1.3, 2.4},
        {1.3, 0.728, 2.4},        {0.0, 0.0, 0.0},        {1.5, 0.0, 0.15},        {1.5, 0.84, 0.15},
        {0.84, 1.5, 0.15},        {0.0, 1.5, 0.15},        {1.5, 0.0, 0.075},        {1.5, 0.84, 0.075},
        {0.84, 1.5, 0.075},        {0.0, 1.5, 0.075},        {1.425, 0.0, 0.0},        {1.425, 0.798, 0.0},
        {0.798, 1.425, 0.0},        {0.0, 1.425, 0.0},        {-0.84, 1.5, 0.15},        {-1.5, 0.84, 0.15},
        {-1.5, 0.0, 0.15},        {-0.84, 1.5, 0.075},        {-1.5, 0.84, 0.075},        {-1.5, 0.0, 0.075},
        {-0.798, 1.425, 0.0},        {-1.425, 0.798, 0.0},        {-1.425, 0.0, 0.0},        {-1.5, -0.84, 0.15},
        {-0.84, -1.5, 0.15},        {0.0, -1.5, 0.15},        {-1.5, -0.84, 0.075},        {-0.84, -1.5, 0.075},
        {0.0, -1.5, 0.075},        {-1.425, -0.798, 0.0},        {-0.798, -1.425, 0.0},        {0.0, -1.425, 0.0},
        {0.84, -1.5, 0.15},        {1.5, -0.84, 0.15},        {0.84, -1.5, 0.075},        {1.5, -0.84, 0.075},
        {0.798, -1.425, 0.0},        {1.425, -0.798, 0.0},
    };

    private static final int[][] PATCHES = {
        {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16},
        {4, 17, 18, 19, 8, 20, 21, 22, 12, 23, 24, 25, 16, 26, 27, 28},
        {19, 29, 30, 31, 22, 32, 33, 34, 25, 35, 36, 37, 28, 38, 39, 40},
        {31, 41, 42, 1, 34, 43, 44, 5, 37, 45, 46, 9, 40, 47, 48, 13},
        {13, 14, 15, 16, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60},
        {16, 26, 27, 28, 52, 61, 62, 63, 56, 64, 65, 66, 60, 67, 68, 69},
        {28, 38, 39, 40, 63, 70, 71, 72, 66, 73, 74, 75, 69, 76, 77, 78},
        {40, 47, 48, 13, 72, 79, 80, 49, 75, 81, 82, 53, 78, 83, 84, 57},
        {57, 58, 59, 60, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96},
        {60, 67, 68, 69, 88, 97, 98, 99, 92, 100, 101, 102, 96, 103, 104, 105},
        {69, 76, 77, 78, 99, 106, 107, 108, 102, 109, 110, 111, 105, 112, 113, 114},
        {78, 83, 84, 57, 108, 115, 116, 85, 111, 117, 118, 89, 114, 119, 120, 93},
        {121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136},
        {124, 137, 138, 121, 128, 139, 140, 125, 132, 141, 142, 129, 136, 143, 144, 133},
        {133, 134, 135, 136, 145, 146, 147, 148, 149, 150, 151, 152, 69, 153, 154, 155},
        {136, 143, 144, 133, 148, 156, 157, 145, 152, 158, 159, 149, 155, 160, 161, 69},
        {162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177},
        {165, 178, 179, 162, 169, 180, 181, 166, 173, 182, 183, 170, 177, 184, 185, 174},
        {174, 175, 176, 177, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197},
        {177, 184, 185, 174, 189, 198, 199, 186, 193, 200, 201, 190, 197, 202, 203, 194},
        {204, 204, 204, 204, 207, 208, 209, 210, 211, 211, 211, 211, 212, 213, 214, 215},
        {204, 204, 204, 204, 210, 217, 218, 219, 211, 211, 211, 211, 215, 220, 221, 222},
        {204, 204, 204, 204, 219, 224, 225, 226, 211, 211, 211, 211, 222, 227, 228, 229},
        {204, 204, 204, 204, 226, 230, 231, 207, 211, 211, 211, 211, 229, 232, 233, 212},
        {212, 213, 214, 215, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245},
        {215, 220, 221, 222, 237, 246, 247, 248, 241, 249, 250, 251, 245, 252, 253, 254},
        {222, 227, 228, 229, 248, 255, 256, 257, 251, 258, 259, 260, 254, 261, 262, 263},
        {229, 232, 233, 212, 257, 264, 265, 234, 260, 266, 267, 238, 263, 268, 269, 242},
        {270, 270, 270, 270, 279, 280, 281, 282, 275, 276, 277, 278, 271, 272, 273, 274},
        {270, 270, 270, 270, 282, 289, 290, 291, 278, 286, 287, 288, 274, 283, 284, 285},
        {270, 270, 270, 270, 291, 298, 299, 300, 288, 295, 296, 297, 285, 292, 293, 294},
        {270, 270, 270, 270, 300, 305, 306, 279, 297, 303, 304, 275, 294, 301, 302, 271},
    };

    // How many samples per patch edge (inclusive) when the surface is
    // tessellated. Higher = smoother/denser silhouette, at the one-time
    // cost of baking a bigger point cloud and mesh.
    private static final int PATCH_RES = 12;

    // Baked once at class-load: centered/scaled positions, a real
    // analytic normal for every tessellated point, and the triangular
    // faces meshed directly from each patch's known grid connectivity
    // (see buildFaces() near the bottom of this file).
    private static final double[][] MESH_POSITIONS;
    private static final double[][] MESH_NORMALS;
    private static final int[][] FACES;

    static {
        double[][][] baked = bakeMesh();
        MESH_POSITIONS = baked[0];
        MESH_NORMALS = baked[1];
        FACES = buildFaces();
    }

    // Reusable per-vertex scratch buffers (screen position, depth, and
    // shaded luminance), computed ONCE per frame in renderGeometry()'s
    // first pass and then read many times over by the triangle
    // rasterizer in its second pass -- allocated once here, per
    // instance, to keep this at 0% GC pressure per frame just like the
    // rest of the pipeline.
    private final double[] screenX;
    private final double[] screenY;
    private final double[] depthBuf;
    private final double[] vertexLum;
    private final double[] rotNormalY;

    public UtahTeapotLoaderB(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
        int vertexCount = MESH_POSITIONS.length;
        this.screenX = new double[vertexCount];
        this.screenY = new double[vertexCount];
        this.depthBuf = new double[vertexCount];
        this.vertexLum = new double[vertexCount];
        this.rotNormalY = new double[vertexCount];
    }

    public UtahTeapotLoaderB() {
        // This one uses 80x22 specifically
        super(STAGES, 80, 22);
        this.width = this.window_width;
        this.height = this.window_height;
        int vertexCount = MESH_POSITIONS.length;
        this.screenX = new double[vertexCount];
        this.screenY = new double[vertexCount];
        this.depthBuf = new double[vertexCount];
        this.vertexLum = new double[vertexCount];
        this.rotNormalY = new double[vertexCount];
    }

    @Override
    protected void initialize() {
        this.angleX = 0.3;
        this.angleY = 0.0;

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Only the azimuth advances per frame -- angleX stays pinned at
        // the tilt initialize() set it to -- so this spins steadily
        // around one upright axis instead of tumbling end over end.
        angleY -= 0.028;

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        double lightX = 0.577;
        double lightY = -0.577;
        double lightZ = 0.577;

        // Pass 1: transform every baked point exactly once per frame --
        // rotated screen position, depth, and Blinn-Phong luminance --
        // and cache the results in the scratch buffers above. FACES only
        // ever references points by index, so nothing here is
        // recomputed per-triangle-corner in pass 2 below.
        for (int i = 0; i < MESH_POSITIONS.length; i++) {
            double[] p = MESH_POSITIONS[i];
            double[] nrm = MESH_NORMALS[i];

            double r1x = p[0] * cosY - p[1] * sinY;
            double r1y = p[0] * sinY + p[1] * cosY;
            double r1z = p[2];
            double rotX = r1x;
            double rotY = r1y * cosX - r1z * sinX;
            double rotZ = r1y * sinX + r1z * cosX;

            double n1x = nrm[0] * cosY - nrm[1] * sinY;
            double n1y = nrm[0] * sinY + nrm[1] * cosY;
            double n1z = nrm[2];
            double rotNX = n1x;
            double rotNY = n1y * cosX - n1z * sinX;
            double rotNZ = n1y * sinX + n1z * cosX;
            double nLen = Math.sqrt(rotNX * rotNX + rotNY * rotNY + rotNZ * rotNZ);
            if (nLen > 0) {
                rotNX /= nLen;
                rotNY /= nLen;
                rotNZ /= nLen;
            }

            double cameraDepth = rotY + 5.0;
            double D = 1.0 / cameraDepth;

            this.screenX[i] = width / 2.0 + (width * 0.6) * D * rotX;
            this.screenY[i] = height / 2.0 - (height * 1.0) * D * rotZ;
            this.depthBuf[i] = D;
            this.rotNormalY[i] = rotNY;

            // Same Blinn-Phong ambient + diffuse + specular shader as
            // UtahTeapotLoaderA, just cached per-vertex here instead of
            // applied to one pixel at a time.
            double dotNL = rotNX * lightX + rotNY * lightY + rotNZ * lightZ;
            double diffuse = Math.max(0.0, dotNL);

            double halfX = lightX;
            double halfY = lightY - 1.0;
            double halfZ = lightZ;
            double halfLen = Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
            halfX /= halfLen;
            halfY /= halfLen;
            halfZ /= halfLen;
            double dotNH = Math.max(0.0, rotNX * halfX + rotNY * halfY + rotNZ * halfZ);
            double specular = Math.pow(dotNH, 24.0);

            double luminance = 0.15 + 0.65 * diffuse + 0.35 * specular;
            if (luminance > 1.0) luminance = 1.0;
            this.vertexLum[i] = luminance;
        }

        // Pass 2: fill in every meshed triangular face, z-buffered and
        // Gouraud-shaded from the three cached vertex luminances above
        // instead of only ever touching three isolated pixels.
        for (int[] face : FACES) {
            rasterizeTriangle(face[0], face[1], face[2], outputBuffer, zBuffer);
        }
    }

    // Fills one triangular face into out/zb using the per-vertex values
    // pass 1 (above) already cached for its three corner indices.
    // Barycentric weights across the triangle interpolate depth (for
    // the z-buffer test) and luminance (for the character chosen from
    // the palette) the same way a GPU would Gouraud-shade a polygon --
    // just landing on ASCII cells instead of real pixels.
    private void rasterizeTriangle(int ia, int ib, int ic, String[] out, double[] zb) {
        // Backface cull: the camera looks toward +Y (see cameraDepth
        // in pass 1), so a face whose averaged, rotated normal is still
        // pointing toward +Y is facing away from the viewer this frame
        // -- skip it rather than let it fight the z-buffer. This reads
        // the vertices' own analytic normals directly, so it doesn't
        // depend on the winding order buildFaces() happened to emit.
        double avgNY = (rotNormalY[ia] + rotNormalY[ib] + rotNormalY[ic]) / 3.0;
        if (avgNY > 0.0) {
            return;
        }

        double x0 = screenX[ia], y0 = screenY[ia], d0 = depthBuf[ia], l0 = vertexLum[ia];
        double x1 = screenX[ib], y1 = screenY[ib], d1 = depthBuf[ib], l1 = vertexLum[ib];
        double x2 = screenX[ic], y2 = screenY[ic], d2 = depthBuf[ic], l2 = vertexLum[ic];

        int minX = (int) Math.floor(Math.min(x0, Math.min(x1, x2)));
        int maxX = (int) Math.ceil(Math.max(x0, Math.max(x1, x2)));
        int minY = (int) Math.floor(Math.min(y0, Math.min(y1, y2)));
        int maxY = (int) Math.ceil(Math.max(y0, Math.max(y1, y2)));
        if (minX < 0) minX = 0;
        if (minY < 0) minY = 0;
        if (maxX >= width) maxX = width - 1;
        if (maxY >= height) maxY = height - 1;
        if (minX > maxX || minY > maxY) {
            return;
        }

        double area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        if (Math.abs(area) < 1e-9) {
            // Zero-area triangle -- this is exactly what a quad touching
            // a collapsed pole edge (the lid knob/rim) looks like once
            // two of its corners land on the same physical point. Skip
            // it; there's nothing to draw.
            return;
        }
        double invArea = 1.0 / area;

        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                double w0 = ((x1 - px) * (y2 - py) - (x2 - px) * (y1 - py)) * invArea;
                double w1 = ((x2 - px) * (y0 - py) - (x0 - px) * (y2 - py)) * invArea;
                double w2 = 1.0 - w0 - w1;
                if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) {
                    continue;
                }

                double D = w0 * d0 + w1 * d1 + w2 * d2;
                int o = px + width * py;
                if (D > zb[o] + 0.0001) {
                    zb[o] = D;

                    double luminance = w0 * l0 + w1 * l1 + w2 * l2;

                    String palette = " .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$";
                    int charIndex = (int) (luminance * (palette.length() - 1));
                    charIndex = Math.max(0, Math.min(palette.length() - 1, charIndex));
                    char asciiChar = palette.charAt(charIndex);

                    out[o] = (asciiChar != ' ') ? (WHITE + asciiChar + RESET) : " ";
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // Bezier surface math: real De Casteljau/Bernstein evaluation of the
    // 32 patches into a dense point+normal cloud, computed once. Identical
    // to UtahTeapotLoaderA -- both loaders share the same baked geometry,
    // just a different renderer on top of it.
    // -----------------------------------------------------------------

    private static double[][][] bakeMesh() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] p : CONTROL_POINTS) {
            if (p[0] < minX) minX = p[0];
            if (p[0] > maxX) maxX = p[0];
            if (p[1] < minY) minY = p[1];
            if (p[1] > maxY) maxY = p[1];
            if (p[2] < minZ) minZ = p[2];
            if (p[2] > maxZ) maxZ = p[2];
        }
        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double cz = (minZ + maxZ) / 2.0;
        double extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        double targetSize = 6.5;
        double scale = targetSize / extent;

        int perPatch = (PATCH_RES + 1) * (PATCH_RES + 1);
        int total = PATCHES.length * perPatch;
        double[][] positions = new double[total][3];
        double[][] normals = new double[total][3];

        double[][] cp = new double[16][];
        double[] pos = new double[3];
        double[] nrm = new double[3];
        int idx = 0;
        for (int[] patch : PATCHES) {
            for (int k = 0; k < 16; k++) {
                cp[k] = CONTROL_POINTS[patch[k] - 1];
            }
            for (int i = 0; i <= PATCH_RES; i++) {
                double u = i / (double) PATCH_RES;
                for (int j = 0; j <= PATCH_RES; j++) {
                    double v = j / (double) PATCH_RES;
                    evalPatch(cp, u, v, pos, nrm);
                    positions[idx][0] = (pos[0] - cx) * scale;
                    positions[idx][1] = (pos[1] - cy) * scale;
                    positions[idx][2] = (pos[2] - cz) * scale;
                    normals[idx][0] = nrm[0];
                    normals[idx][1] = nrm[1];
                    normals[idx][2] = nrm[2];
                    idx++;
                }
            }
        }

        return new double[][][] { positions, normals };
    }

    private static void evalPatch(double[][] cp, double u, double v, double[] outPos, double[] outNormal) {
        double[][] uCurve = new double[4][];
        double[][] uDeriv = new double[4][];
        for (int i = 0; i < 4; i++) {
            double[] p0 = cp[i * 4];
            double[] p1 = cp[i * 4 + 1];
            double[] p2 = cp[i * 4 + 2];
            double[] p3 = cp[i * 4 + 3];
            uCurve[i] = bezier(p0, p1, p2, p3, u);
            uDeriv[i] = bezierDeriv(p0, p1, p2, p3, u);
        }

        double[] pos = bezier(uCurve[0], uCurve[1], uCurve[2], uCurve[3], v);
        double[] dPdv = bezierDeriv(uCurve[0], uCurve[1], uCurve[2], uCurve[3], v);
        double[] dPdu = bezier(uDeriv[0], uDeriv[1], uDeriv[2], uDeriv[3], v);

        double nx = dPdu[1] * dPdv[2] - dPdu[2] * dPdv[1];
        double ny = dPdu[2] * dPdv[0] - dPdu[0] * dPdv[2];
        double nz = dPdu[0] * dPdv[1] - dPdu[1] * dPdv[0];
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) {
            nx = 0;
            ny = 0;
            nz = (pos[2] >= 0) ? 1 : -1;
            len = 1.0;
        }

        outPos[0] = pos[0];
        outPos[1] = pos[1];
        outPos[2] = pos[2];
        outNormal[0] = nx / len;
        outNormal[1] = ny / len;
        outNormal[2] = nz / len;
    }

    private static double[] bezier(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
        double mt = 1 - t;
        double b0 = mt * mt * mt;
        double b1 = 3 * mt * mt * t;
        double b2 = 3 * mt * t * t;
        double b3 = t * t * t;
        return new double[] {
                b0 * p0[0] + b1 * p1[0] + b2 * p2[0] + b3 * p3[0],
                b0 * p0[1] + b1 * p1[1] + b2 * p2[1] + b3 * p3[1],
                b0 * p0[2] + b1 * p1[2] + b2 * p2[2] + b3 * p3[2]
        };
    }

    private static double[] bezierDeriv(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
        double mt = 1 - t;
        double d0 = -3 * mt * mt;
        double d1 = 3 * mt * mt - 6 * mt * t;
        double d2 = 6 * mt * t - 3 * t * t;
        double d3 = 3 * t * t;
        return new double[] {
                d0 * p0[0] + d1 * p1[0] + d2 * p2[0] + d3 * p3[0],
                d0 * p0[1] + d1 * p1[1] + d2 * p2[1] + d3 * p3[1],
                d0 * p0[2] + d1 * p1[2] + d2 * p2[2] + d3 * p3[2]
        };
    }

    // -----------------------------------------------------------------
    // Surface reconstruction: turns the baked point grid into an actual
    // triangle mesh. Each of the 32 patches was tessellated into its own
    // regular (PATCH_RES+1) x (PATCH_RES+1) grid, stored row-major
    // (outer loop = i, inner loop = j) starting at patch index p's base
    // offset p * perPatch -- so grid cell (i, j) and its three neighbors
    // (i, j+1), (i+1, j), (i+1, j+1) are always four exact, known array
    // indices. No search, no angular sort, no dedup pass: every quad is
    // visited exactly once, split into two triangles.
    // -----------------------------------------------------------------

    private static int[][] buildFaces() {
        int perPatch = (PATCH_RES + 1) * (PATCH_RES + 1);
        java.util.List<int[]> faces = new java.util.ArrayList<>();

        for (int p = 0; p < PATCHES.length; p++) {
            int base = p * perPatch;
            for (int i = 0; i < PATCH_RES; i++) {
                for (int j = 0; j < PATCH_RES; j++) {
                    int i00 = base + i * (PATCH_RES + 1) + j;
                    int i01 = base + i * (PATCH_RES + 1) + (j + 1);
                    int i10 = base + (i + 1) * (PATCH_RES + 1) + j;
                    int i11 = base + (i + 1) * (PATCH_RES + 1) + (j + 1);

                    faces.add(new int[] { i00, i10, i11 });
                    faces.add(new int[] { i00, i11, i01 });
                }
            }
        }

        return faces.toArray(new int[0][]);
    }
}