// This is now the REAL Utah Teapot: the exact 306 control points and 32
// bicubic Bezier patches Martin Newell measured off a real Melitta teapot
// in 1975 and Jim Blinn later rescaled -- the same dataset baked into
// glutSolidTeapot() and every OpenGL textbook demo since. No more
// hand-eyeballed lathe profile: the body, lid, handle, spout and bottom
// are each genuine Bezier surfaces, tessellated with real De Casteljau
// evaluation and analytic (not guessed) surface normals.

public class UtahTeapotLoader extends Loader {
    private static final StatusStage[] TEAPOT_STAGES = {
        new StatusStage(10, "Loading 32 bicubic Bezier patches (Newell/Blinn dataset)..."),
        new StatusStage(30, "Evaluating De Casteljau control point lattice..."),
        new StatusStage(55, "Deriving analytic tangents and surface normals..."),
        new StatusStage(80, "Baking high-density point mesh (spout, handle, lid, body)..."),
        new StatusStage(100, "Orthographic Rasterization Complete!")
    };

    // Automated rotation tracking angles
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
    // cost of baking a bigger point cloud.
    private static final int PATCH_RES = 12;

    // Baked once at class-load: rotating/projecting is all that happens
    // per frame, not surface evaluation.
    private static final double[][] MESH_POSITIONS;
    private static final double[][] MESH_NORMALS;

    static {
        double[][][] baked = bakeMesh();
        MESH_POSITIONS = baked[0];
        MESH_NORMALS = baked[1];
    }

    public UtahTeapotLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public UtahTeapotLoader() {
        // This one uses 80x22 specifically
        super(TEAPOT_STAGES, 80, 22);
        this.width = this.window_width;
        this.height = this.window_height;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.0;
        this.angleY = 0.0;

        // Ensure standard newline rendering is clean
        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Increment automated spin angles over time
        angleX += 0.022;
        angleY += 0.038;

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Clean white directional 3D light vector coming from the top-front-right
        double lightX = 0.577;
        double lightY = -0.577;
        double lightZ = 0.577;

        for (int i = 0; i < MESH_POSITIONS.length; i++) {
            double[] p = MESH_POSITIONS[i];
            double[] n = MESH_NORMALS[i];
            projectPointToBuffer(p[0], p[1], p[2], n[0], n[1], n[2],
                    cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }
    }

    private void projectPointToBuffer(double lx, double ly, double lz, double nx, double ny, double nz,
                                      double cosX, double sinX, double cosY, double sinY,
                                      double lightX, double lightY, double lightZ, String[] out, double[] zb) {
        // 1. Apply multi-axis 3D rotations sequentially
        double r1x = lx * cosY - ly * sinY;
        double r1y = lx * sinY + ly * cosY;
        double r1z = lz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // 2. Rotate the (already analytically-correct) surface normal the same way
        double n1x = nx * cosY - ny * sinY;
        double n1y = nx * sinY + ny * cosY;
        double n1z = nz;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        double nLen = Math.sqrt(rotNX * rotNX + rotNY * rotNY + rotNZ * rotNZ);
        if (nLen > 0) {
            rotNX /= nLen;
            rotNY /= nLen;
            rotNZ /= nLen;
        }

        // 3. Perspective Projection Calculations (Y represents deep distance)
        double cameraDepth = rotY + 5.0;
        double D = 1.0 / cameraDepth;

        // Map coordinates into the canvas, scaled relative to its actual
        // dimensions (rather than a fixed 80x22 assumption) so the model
        // stays correctly proportioned at any resolution.
        int sx = (int) (width / 2.0 + (width * 0.6) * D * rotX);
        int sy = (int) (height / 2.0 - (height * 1.0) * D * rotZ);
        int o = sx + width * sy;

        // 4. BLINN-PHONG-STYLE MONOCHROME SHADER (ambient + diffuse + a
        // touch of specular, echoing the glossy look most Utah Teapot
        // renders are famous for)
        double dotNL = rotNX * lightX + rotNY * lightY + rotNZ * lightZ;
        double diffuse = Math.max(0.0, dotNL);

        // Approximate view direction: camera sits back along -Y looking
        // toward +Y, matching the cameraDepth = rotY + 5 setup above.
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

        if (sy < height && sy >= 0 && sx >= 0 && sx < width && D > (zb[o] + 0.0001)) {
            zb[o] = D;

            // Finer-grained grayscale ramp (the classic "ASCII density"
            // ordering) gives noticeably smoother shading gradients than
            // a coarse dozen-character palette.
            String palette = " .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$";
            int charIndex = (int) (luminance * (palette.length() - 1));
            charIndex = Math.max(0, Math.min(palette.length() - 1, charIndex));
            char asciiChar = palette.charAt(charIndex);

            if (asciiChar != ' ') {
                out[o] = WHITE + asciiChar + RESET;
            } else {
                out[o] = " ";
            }
        }
    }

    // -----------------------------------------------------------------
    // Bezier surface math: real De Casteljau/Bernstein evaluation of the
    // 32 patches into a dense point+normal cloud, computed once.
    // -----------------------------------------------------------------

    private static double[][][] bakeMesh() {
        // Bounding box of the raw control lattice, so we can center the
        // teapot on the origin and scale it to a canvas-friendly size
        // regardless of the dataset's original units.
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
                cp[k] = CONTROL_POINTS[patch[k] - 1]; // stored 1-based, convert to 0-based
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

    // Evaluates one bicubic Bezier patch at (u, v), returning both the
    // surface position and the analytic normal (cross product of the
    // partial-derivative tangents dP/du and dP/dv).
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
            // A handful of true poles exist (the knob tip, the flat
            // bottom center) where the patch collapses to a point and
            // the tangent basis degenerates. Default to a vertical
            // normal there so the point still shades sensibly instead
            // of producing NaNs.
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
}