// TODO: Make the sub cubes have better black borders between them

import java.util.Arrays;

public class RubiksCubeLoader extends Loader {

    private static final StatusStage[] RUBIKS_STAGES = {
            new StatusStage(10, "Molding 27 constituent cubies..."),
            new StatusStage(30, "Applying primary colored vinyl stickers..."),
            new StatusStage(50, "Lubricating core rotation axes..."),
            new StatusStage(75, "Executing scramble algorithms..."),
            new StatusStage(100, "Solving multidimensional matrix!")
    };

    private double globalPitch = 0.0;
    private double globalYaw = 0.0;
    private double globalRoll = 0.0;

    // Turn Animation State Machine
    private int turnAxis = 0;   // 0:X, 1:Y, 2:Z
    private int turnSlice = 0;  // -1, 0, 1
    private int turnDir = 1;    // 1, -1
    private double turnProgress = 0.0;
    private int waitFrames = 15;

    private Cubie[] cubies;

    private final int width;
    private final int height;

    // Classic Rubik's Palette
    private static final String C_WHITE  = "\u001B[38;2;240;240;240m";
    private static final String C_YELLOW = "\u001B[38;2;255;210;0m";
    private static final String C_RED    = "\u001B[38;2;220;30;30m";
    private static final String C_ORANGE = "\u001B[38;2;255;120;0m";
    private static final String C_GREEN  = "\u001B[38;2;20;200;40m";
    private static final String C_BLUE   = "\u001B[38;2;30;100;255m";
    private static final String C_BLACK  = "\u001B[38;2;30;30;30m"; // Plastic core/edges

    private static final String RESET = "\u001B[0m";

    public RubiksCubeLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public RubiksCubeLoader() {
        super(RUBIKS_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.globalPitch = 0.4;
        this.globalYaw = 0.5;
        this.globalRoll = 0.2;

        cubies = new Cubie[27];
        int idx = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    cubies[idx++] = new Cubie(x, y, z);
                }
            }
        }
        pickNextMove();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        // Continuous global space tumbling
        globalPitch += 0.010;
        globalYaw   += 0.015;
        globalRoll  += 0.008;

        // Turn animation logic
        if (waitFrames > 0) {
            waitFrames--;
        } else {
            turnProgress += 0.065; // Turn speed
            if (turnProgress >= 1.0) {
                finalizeMove();
                pickNextMove();
            }
        }

        double currentAngle = smoothStep(Math.min(1.0, turnProgress)) * (Math.PI / 2.0) * turnDir;
        double[][] activeTMat = getRotMat(turnAxis, currentAngle);

        // Key light pointing down and inward toward the front of the cube
        double lightX = 0.4, lightY = -0.6, lightZ = -0.7;
        double lLen = Math.hypot(lightX, Math.hypot(lightY, lightZ));
        lightX /= lLen; lightY /= lLen; lightZ /= lLen;

        double spacing = 1.03;

        for (Cubie c : cubies) {
            boolean inSlice = (getCoord(c, turnAxis) == turnSlice) && waitFrames <= 0;

            double[] pos = {c.x, c.y, c.z};
            double[][] cMat = c.mat;

            if (inSlice) {
                pos = multiplyMatVec(activeTMat, pos);
                cMat = multiplyMat(activeTMat, cMat);
            }

            double cx = pos[0] * spacing;
            double cy = pos[1] * spacing;
            double cz = pos[2] * spacing;

            renderSubCube(cx, cy, cz, cMat, c.colors, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }
    }

    private void finalizeMove() {
        double angle = (Math.PI / 2.0) * turnDir;
        double[][] tMat = getRotMat(turnAxis, angle);

        for (Cubie c : cubies) {
            if (getCoord(c, turnAxis) == turnSlice) {
                double[] p = {c.x, c.y, c.z};
                double[] pNew = multiplyMatVec(tMat, p);
                
                c.x = (int) Math.round(pNew[0]);
                c.y = (int) Math.round(pNew[1]);
                c.z = (int) Math.round(pNew[2]);
                
                c.mat = multiplyMat(tMat, c.mat);
            }
        }
    }

    private void pickNextMove() {
        int[] slices = {-1, 0, 1};
        turnAxis = (int) (Math.random() * 3);
        turnSlice = slices[(int) (Math.random() * 3)];
        turnDir = Math.random() > 0.5 ? 1 : -1;
        turnProgress = 0.0;
        waitFrames = 12;
    }

    private int getCoord(Cubie c, int axis) {
        if (axis == 0) return c.x;
        if (axis == 1) return c.y;
        return c.z;
    }

    private double smoothStep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private void renderSubCube(double cx, double cy, double cz, double[][] cMat, String[] colors,
                               double lx, double ly, double lz, String[] out, double[] zb) {
        
        double hs = 0.48; 
        // Increased sampling density (decreased step from 0.16 to 0.05) to ensure solid quads
        double step = 0.05; 

        renderFace(cx, cy, cz, hs,  0,  0,  1, step, cMat, colors[0], lx, ly, lz, out, zb); // +Z
        renderFace(cx, cy, cz, hs,  0,  0, -1, step, cMat, colors[1], lx, ly, lz, out, zb); // -Z
        renderFace(cx, cy, cz, hs,  0,  1,  0, step, cMat, colors[2], lx, ly, lz, out, zb); // +Y
        renderFace(cx, cy, cz, hs,  0, -1,  0, step, cMat, colors[3], lx, ly, lz, out, zb); // -Y
        renderFace(cx, cy, cz, hs,  1,  0,  0, step, cMat, colors[4], lx, ly, lz, out, zb); // +X
        renderFace(cx, cy, cz, hs, -1,  0,  0, step, cMat, colors[5], lx, ly, lz, out, zb); // -X
    }

    private void renderFace(double cx, double cy, double cz, double hs,
                            double nx, double ny, double nz, double step,
                            double[][] cMat, String colorCode,
                            double lx, double ly, double lz, String[] out, double[] zb) {

        for (double u = -hs; u <= hs; u += step) {
            for (double v = -hs; v <= hs; v += step) {
                double px = 0, py = 0, pz = 0;
                if (Math.abs(nz) > 0.5) { px = u; py = v; pz = nz * hs; }
                else if (Math.abs(ny) > 0.5) { px = u; py = ny * hs; pz = v; }
                else { px = nx * hs; py = u; pz = v; }

                double rx = cMat[0][0] * px + cMat[0][1] * py + cMat[0][2] * pz;
                double ry = cMat[1][0] * px + cMat[1][1] * py + cMat[1][2] * pz;
                double rz = cMat[2][0] * px + cMat[2][1] * py + cMat[2][2] * pz;

                double rnx = cMat[0][0] * nx + cMat[0][1] * ny + cMat[0][2] * nz;
                double rny = cMat[1][0] * nx + cMat[1][1] * ny + cMat[1][2] * nz;
                double rnz = cMat[2][0] * nx + cMat[2][1] * ny + cMat[2][2] * nz;

                double wx = rx + cx;
                double wy = ry + cy;
                double wz = rz + cz;

                projectPoint(wx, wy, wz, rnx, rny, rnz, colorCode, lx, ly, lz, out, zb);
            }
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz,
                              String colorCode, double lx, double ly, double lz,
                              String[] out, double[] zb) {

        // Global Camera Pitch, Yaw, Roll
        double cY = Math.cos(globalYaw), sY = Math.sin(globalYaw);
        double x1 = px * cY + pz * sY, z1 = -px * sY + pz * cY;
        
        double cP = Math.cos(globalPitch), sP = Math.sin(globalPitch);
        double y2 = py * cP - z1 * sP, z2 = py * sP + z1 * cP;

        double cR = Math.cos(globalRoll), sR = Math.sin(globalRoll);
        double rotX = x1 * cR - y2 * sR, rotY = x1 * sR + y2 * cR, rotZ = z2;

        // Global Normal Vector Rotation
        double nx1 = nx * cY + nz * sY, nz1 = -nx * sY + nz * cY;
        double ny2 = ny * cP - nz1 * sP, nz2 = ny * sP + nz1 * cP;
        double rotNX = nx1 * cR - ny2 * sR, rotNY = nx1 * sR + ny2 * cR, rotNZ = nz2;

        // BACK-FACE CULLING: If normal points away from camera (+Z), cull it
        if (rotNZ >= 0.0) return;

        double cameraDepth = rotZ + 7.5;
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        int sx = (int) (width / 2.0 + 60.0 * D * rotX);
        int sy = (int) (height / 2.0 - 30.0 * D * rotY);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                // Accurate Diffuse Lighting
                double dot = (rotNX * lx + rotNY * ly + rotNZ * lz);
                double illuminance = Math.min(1.0, Math.max(0.18, dot + 0.15));

                if (colorCode.equals(C_BLACK)) illuminance *= 0.35;

                //char[] ramp = {'░', '▒', '▓', '█'};
                char[] ramp = {'▒'}; // honestly just one doesnt look bad
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));

                out[idx] = colorCode + ramp[rampIdx] + RESET;
            }
        }
    }

    private double[][] getRotMat(int axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        if (axis == 0) {
            return new double[][]{
                {1, 0, 0},
                {0, c, -s},
                {0, s, c}
            };
        } else if (axis == 1) {
            return new double[][]{
                {c, 0, s},
                {0, 1, 0},
                {-s, 0, c}
            };
        } else {
            return new double[][]{
                {c, -s, 0},
                {s, c, 0},
                {0, 0, 1}
            };
        }
    }

    private double[][] multiplyMat(double[][] a, double[][] b) {
        double[][] res = new double[3][3];
        for(int i=0; i<3; i++) {
            for(int j=0; j<3; j++) {
                res[i][j] = a[i][0]*b[0][j] + a[i][1]*b[1][j] + a[i][2]*b[2][j];
            }
        }
        return res;
    }

    private double[] multiplyMatVec(double[][] m, double[] v) {
        double[] res = new double[3];
        for(int i=0; i<3; i++) {
            res[i] = m[i][0]*v[0] + m[i][1]*v[1] + m[i][2]*v[2];
        }
        return res;
    }

    private class Cubie {
        int x, y, z;
        double[][] mat = {{1,0,0}, {0,1,0}, {0,0,1}};
        String[] colors = new String[6];

        Cubie(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            colors[0] = (z == 1) ? C_GREEN : C_BLACK;  // +Z
            colors[1] = (z == -1) ? C_BLUE : C_BLACK;  // -Z
            colors[2] = (y == 1) ? C_WHITE : C_BLACK;  // +Y
            colors[3] = (y == -1) ? C_YELLOW : C_BLACK;// -Y
            colors[4] = (x == 1) ? C_RED : C_BLACK;    // +X
            colors[5] = (x == -1) ? C_ORANGE : C_BLACK;// -X
        }
    }
}