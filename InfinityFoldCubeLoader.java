import java.util.Arrays;

public class InfinityFoldCubeLoader extends Loader {

    private static final StatusStage[] INFINITY_STAGES = {
            new StatusStage(10, "Calibrating 8-block hinge matrices..."),
            new StatusStage(30, "Aligning fractal kinematic linkages..."),
            new StatusStage(50, "Mapping sub-cube surface textures..."),
            new StatusStage(75, "Engaging infinite unfold cycle..."),
            new StatusStage(100, "Tumbling Infinity Cube mechanism!")
    };

    private double globalPitch = 0.0;
    private double globalYaw = 0.0;
    private double globalRoll = 0.0;

    private int currentPhase = 0;
    private double phaseProgress = 0.0;
    private Block[] permBlocks;

    private final int width;
    private final int height;

    private final double speed = 0.0235;

    // Palette: 8 distinct vibrant neon colors so you can track how the blocks permute
    private static final String[] C_CUBES = {
            "\u001B[38;2;255;50;80m",   // Neon Red
            "\u001B[38;2;50;255;100m",  // Neon Green
            "\u001B[38;2;50;150;255m",  // Neon Blue
            "\u001B[38;2;255;220;50m",  // Neon Yellow
            "\u001B[38;2;255;80;255m",  // Neon Magenta
            "\u001B[38;2;50;255;255m",  // Neon Cyan
            "\u001B[38;2;255;150;50m",  // Neon Orange
            "\u001B[38;2;180;50;255m"   // Neon Purple
    };

    private static final String RESET = "\u001B[0m";

    public InfinityFoldCubeLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public InfinityFoldCubeLoader() {
        super(INFINITY_STAGES, 126, 32);
        this.width = 126;
        this.height = 32;
    }

    @Override
    protected void initialize() {
        this.globalPitch = 0.2;
        this.globalYaw = 0.4;
        this.globalRoll = 0.1;
        this.currentPhase = 0;
        this.phaseProgress = 0.0;

        // Initialize the 8 permanent blocks of the 2x2x2 cube
        permBlocks = new Block[8];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -0.5 : 0.5;
            double y = (i & 2) == 0 ? -0.5 : 0.5;
            double z = (i & 4) == 0 ? -0.5 : 0.5;
            permBlocks[i] = new Block(i, x, y, z);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        // Global Camera Tumbling
        globalPitch += 0.015;
        globalYaw += 0.022;
        globalRoll += 0.010;

        // Advance the mechanical fold cycle
        phaseProgress += speed;

        // Compute geometric centroid of the CURRENT permanent state
        double cx = 0, cy = 0, cz = 0;
        for (Block b : permBlocks) {
            cx += b.x; cy += b.y; cz += b.z;
        }
        cx /= 8.0; cy /= 8.0; cz /= 8.0;

        // Handle phase transitions (locking in completed 180-degree folds)
        if (phaseProgress >= 1.0) {
            for (Block b : permBlocks) applyFold(b, currentPhase, 1.0, cx, cy, cz);
            phaseProgress -= 1.0;
            currentPhase = (currentPhase + 1) % 4;

            // Recompute centroid after the fold locks
            cx = cy = cz = 0;
            for (Block b : permBlocks) {
                cx += b.x; cy += b.y; cz += b.z;
            }
            cx /= 8.0; cy /= 8.0; cz /= 8.0;
        }

        // Generate temporary blocks for rendering the partial animation
        Block[] tempBlocks = new Block[8];
        for (int i = 0; i < 8; i++) {
            tempBlocks[i] = permBlocks[i].cloneBlock();
            applyFold(tempBlocks[i], currentPhase, smoothStep(phaseProgress), cx, cy, cz);
        }

        // Camera Tracking: Find centroid of the animating blocks
        double renderCx = 0, renderCy = 0, renderCz = 0;
        for (Block b : tempBlocks) {
            renderCx += b.x; renderCy += b.y; renderCz += b.z;
        }
        renderCx /= 8.0; renderCy /= 8.0; renderCz /= 8.0;

        // Lighting
        double lightX = 0.5, lightY = -0.7, lightZ = 0.6;
        double lLen = Math.hypot(lightX, Math.hypot(lightY, lightZ));
        lightX /= lLen; lightY /= lLen; lightZ /= lLen;

        // Render the connected kinematic chain
        for (Block b : tempBlocks) {
            renderSubCube(b, renderCx, renderCy, renderCz, C_CUBES[b.id],
                    lightX, lightY, lightZ, outputBuffer, zBuffer);
        }
    }

    private void applyFold(Block b, int phase, double t, double cX, double cY, double cZ) {
        double angle = t * Math.PI;
        // The Infinity Cube 4-Step Folding Algorithm
        switch (phase) {
            case 0:
                // Split front blocks and fold up/down
                if (b.z > cZ) {
                    if (b.y > cY) b.pivotX(cY + 1.0, cZ, -angle);
                    else b.pivotX(cY - 1.0, cZ, angle);
                }
                break;
            case 1:
                // Fold outer halves back inward
                if (b.y > cY) {
                    b.pivotX(cX, cZ + 0.5, angle);
                }
                break;
            case 2:
                // Split front blocks and fold left/right
                if (b.z > cZ) {
                    if (b.x > cX) b.pivotY(cX + 1.0, cZ, angle);
                    else b.pivotY(cX - 1.0, cZ, -angle);
                }
                break;
            case 3:
                // Fold outer halves back inward
                if (b.x > cX) {
                    b.pivotY(cX, cZ + 0.5, -angle);
                }
                break;
        }
    }

    private double smoothStep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    private void renderSubCube(Block b, double rcx, double rcy, double rcz,
                               String colorCode, double lx, double ly, double lz,
                               String[] out, double[] zb) {
        
        double hs = 0.48; 
        double step = 0.02;

        renderFace(b, rcx, rcy, rcz, hs,  0,  0,  1, step, colorCode, lx, ly, lz, out, zb);
        renderFace(b, rcx, rcy, rcz, hs,  0,  0, -1, step, colorCode, lx, ly, lz, out, zb);
        renderFace(b, rcx, rcy, rcz, hs,  0,  1,  0, step, colorCode, lx, ly, lz, out, zb);
        renderFace(b, rcx, rcy, rcz, hs,  0, -1,  0, step, colorCode, lx, ly, lz, out, zb);
        renderFace(b, rcx, rcy, rcz, hs,  1,  0,  0, step, colorCode, lx, ly, lz, out, zb);
        renderFace(b, rcx, rcy, rcz, hs, -1,  0,  0, step, colorCode, lx, ly, lz, out, zb);
    }

    private void renderFace(Block b, double rcx, double rcy, double rcz, double hs,
                            double nx, double ny, double nz, double step, String colorCode,
                            double lx, double ly, double lz, String[] out, double[] zb) {

        for (double u = -hs; u <= hs; u += step) {
            for (double v = -hs; v <= hs; v += step) {
                double px = 0, py = 0, pz = 0;
                if (Math.abs(nz) > 0.5) { px = u; py = v; pz = nz * hs; }
                else if (Math.abs(ny) > 0.5) { px = u; py = ny * hs; pz = v; }
                else { px = nx * hs; py = u; pz = v; }

                // 1. Apply Local Intrinsic Rotation (Kinematic tumbling)
                double rx = b.mat[0][0] * px + b.mat[0][1] * py + b.mat[0][2] * pz;
                double ry = b.mat[1][0] * px + b.mat[1][1] * py + b.mat[1][2] * pz;
                double rz = b.mat[2][0] * px + b.mat[2][1] * py + b.mat[2][2] * pz;

                double rnx = b.mat[0][0] * nx + b.mat[0][1] * ny + b.mat[0][2] * nz;
                double rny = b.mat[1][0] * nx + b.mat[1][1] * ny + b.mat[1][2] * nz;
                double rnz = b.mat[2][0] * nx + b.mat[2][1] * ny + b.mat[2][2] * nz;

                // 2. Translate to Block's spatial offset (relative to tracked centroid)
                double wx = rx + (b.x - rcx);
                double wy = ry + (b.y - rcy);
                double wz = rz + (b.z - rcz);

                // 3. Project globally
                projectPoint(wx, wy, wz, rnx, rny, rnz, colorCode, lx, ly, lz, out, zb);
            }
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz,
                              String colorCode, double lx, double ly, double lz,
                              String[] out, double[] zb) {

        // Yaw, Pitch, Roll (Global Camera)
        double cY = Math.cos(globalYaw), sY = Math.sin(globalYaw);
        double x1 = px * cY + pz * sY, z1 = -px * sY + pz * cY;
        
        double cP = Math.cos(globalPitch), sP = Math.sin(globalPitch);
        double y2 = py * cP - z1 * sP, z2 = py * sP + z1 * cP;

        double cR = Math.cos(globalRoll), sR = Math.sin(globalRoll);
        double rotX = x1 * cR - y2 * sR, rotY = x1 * sR + y2 * cR, rotZ = z2;

        // Normal rotation
        double nx1 = nx * cY + nz * sY, nz1 = -nx * sY + nz * cY;
        double ny2 = ny * cP - nz1 * sP, nz2 = ny * sP + nz1 * cP;
        double rotNX = nx1 * cR - ny2 * sR, rotNY = nx1 * sR + ny2 * cR, rotNZ = nz2;

        double cameraDepth = rotZ + 3.8;
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        int sx = (int) (width / 2.0 + 55.0 * D * rotX);
        int sy = (int) (height / 2.0 - 25.0 * D * rotY);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;
            if (D > zb[idx]) {
                zb[idx] = D;

                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                double illuminance = Math.min(1.0, Math.max(0.1, dot + 0.3));
                illuminance *= Math.max(0.2, 1.0 - (rotZ / 4.0)); // Depth Fog

                char[] ramp = {'.', '~', ':', '-', '=', '+', '*', '&', '#', '@'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));

                out[idx] = colorCode + ramp[rampIdx] + RESET;
            }
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

    // Encapsulated Sub-Block Kinematics
    private class Block {
        int id;
        double x, y, z;
        double[][] mat = {{1,0,0},{0,1,0},{0,0,1}};

        Block(int id, double x, double y, double z) {
            this.id = id; this.x = x; this.y = y; this.z = z;
        }

        void pivotX(double py, double pz, double angle) {
            double dy = y - py;
            double dz = z - pz;
            double c = Math.cos(angle), s = Math.sin(angle);
            y = py + dy * c - dz * s;
            z = pz + dy * s + dz * c;

            double[][] r = {{1,0,0},{0,c,-s},{0,s,c}};
            mat = multiplyMat(r, mat);
        }

        void pivotY(double px, double pz, double angle) {
            double dx = x - px;
            double dz = z - pz;
            double c = Math.cos(angle), s = Math.sin(angle);
            x = px + dx * c + dz * s;
            z = pz - dx * s + dz * c;

            double[][] r = {{c,0,s},{0,1,0},{-s,0,c}};
            mat = multiplyMat(r, mat);
        }

        Block cloneBlock() {
            Block clone = new Block(id, x, y, z);
            for(int i = 0; i < 3; i++) System.arraycopy(mat[i], 0, clone.mat[i], 0, 3);
            return clone;
        }
    }
}