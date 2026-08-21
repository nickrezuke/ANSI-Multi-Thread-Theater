import java.util.Arrays;

public class PaintCubeLoader extends InteractiveLoader {
    private static final StatusStage[] STAGES = {
        new StatusStage(100, "[Use ARROW KEYS to Roll the Paint Cube!]")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // --- CAMERA & PERSPECTIVE PARAMETERS ---
    private static final double CAMERA_PITCH_DEG = 32.0; 
    private static final double CAMERA_Y = -1.15; // Camera height above floor
    private static final double CAMERA_Z = -0.55; // Camera distance from grid center
    private static final double FLOOR_LEVEL = 0.65; // Floor plane Y coordinate

    // Grid & Paper Setup (11x11 Grid on Floor)
    private static final int GRID_SIZE = 11;
    private static final double TILE_SIZE = 0.38;

    // Persistent Paint Grid RGB Stores
    private final int[][] paperGridR = new int[GRID_SIZE][GRID_SIZE];
    private final int[][] paperGridG = new int[GRID_SIZE][GRID_SIZE];
    private final int[][] paperGridB = new int[GRID_SIZE][GRID_SIZE];

    // Cube Logic & Physics State
    private int cubeGridZ = 2;
    private int cubeGridX = 3;
    private int targetGridZ = 2;
    private int targetGridX = 3;

    private boolean isRolling = false;
    private double rollProgress = 0.0;
    private int rollDirX = 0;
    private int rollDirZ = 0;

    private double colorHueAngle = 0.0;

    public PaintCubeLoader() {
        // This one used 80x22
        super(STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void onInitialize() {
        this.cubeGridZ = 2;
        this.cubeGridX = 3;
        this.targetGridZ = 2;
        this.targetGridX = 3;
        this.isRolling = false;
        this.rollProgress = 0.0;
        this.rollDirX = 0;
        this.rollDirZ = 0;
        this.colorHueAngle = 0.0;

        for (int x = 0; x < GRID_SIZE; x++) {
            Arrays.fill(paperGridR[x], 0);
            Arrays.fill(paperGridG[x], 0);
            Arrays.fill(paperGridB[x], 0);
        }

        stampGroundPaint(cubeGridX, cubeGridZ);
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        if (isRolling) return;

        int dx = 0, dz = 0;

        // Corrected vertical orientation: UP moves toward top/horizon (+Z), DOWN moves toward bottom (-Z)
        if (keyCode == 'A' || keyCode == 'a' || keyCode == 'w' || keyCode == 38) {
            dz = 1;  // Roll Up (Toward Horizon)
        } else if (keyCode == 'B' || keyCode == 'b' || keyCode == 's' || keyCode == 40) {
            dz = -1; // Roll Down (Toward Camera)
        } else if (keyCode == 'C' || keyCode == 'c' || keyCode == 'd' || keyCode == 39) {
            dx = 1;  // Roll Right (+X)
        } else if (keyCode == 'D' || keyCode == 'd' || keyCode == 'a' || keyCode == 37) {
            dx = -1; // Roll Left (-X)
        }

        if (dx != 0 || dz != 0) {
            int newX = cubeGridX + dx;
            int newZ = cubeGridZ + dz;

            if (newX >= 0 && newX < GRID_SIZE && newZ >= 0 && newZ < GRID_SIZE) {
                targetGridX = newX;
                targetGridZ = newZ;
                rollDirX = dx;
                rollDirZ = dz;
                isRolling = true;
                rollProgress = 0.0;
                colorHueAngle += (Math.PI / 3.0);
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        if (isRolling) {
            rollProgress += 0.16;
            if (rollProgress >= 1.0) {
                rollProgress = 0.0;
                isRolling = false;
                cubeGridX = targetGridX;
                cubeGridZ = targetGridZ;
                rollDirX = 0;
                rollDirZ = 0;
                stampGroundPaint(cubeGridX, cubeGridZ);
            }
        }

        // Background starfield
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int idx = x + y * WIDTH;
                if ((x + y * 13) % 31 == 0 && y < 8) {
                    outputBuffer[idx] = "\u001B[38;2;65;70;85m.\u001B[0m";
                } else {
                    outputBuffer[idx] = " ";
                }
            }
        }

        renderPaperSheet(outputBuffer, zBuffer);
        renderRollingCube(outputBuffer, zBuffer);
    }

    private void stampGroundPaint(int gx, int gz) {
        int r = (int) (165 + 90 * Math.sin(colorHueAngle));
        int g = (int) (165 + 90 * Math.sin(colorHueAngle + 2.0 * Math.PI / 3.0));
        int b = (int) (165 + 90 * Math.sin(colorHueAngle + 4.0 * Math.PI / 3.0));

        paperGridR[gx][gz] = Math.min(255, Math.max(120, r));
        paperGridG[gx][gz] = Math.min(255, Math.max(120, g));
        paperGridB[gx][gz] = Math.min(255, Math.max(120, b));
    }

    private void renderPaperSheet(String[] buffer, double[] zBuffer) {
        double halfGrid = (GRID_SIZE * TILE_SIZE) / 2.0;

        for (int gx = 0; gx < GRID_SIZE; gx++) {
            for (int gz = 0; gz < GRID_SIZE; gz++) {
                double worldX = (gx * TILE_SIZE) - halfGrid + (TILE_SIZE / 2.0);
                double worldZ = (gz * TILE_SIZE) + 0.2;
                boolean isPainted = paperGridR[gx][gz] > 0 || paperGridG[gx][gz] > 0 || paperGridB[gx][gz] > 0;

                // Ultra-dense sampling step (0.02) prevents sub-pixel gaps and moiré lines
                double step = 0.02;
                for (double dx = -TILE_SIZE / 2.0; dx <= TILE_SIZE / 2.0 + 0.001; dx += step) {
                    for (double dz = -TILE_SIZE / 2.0; dz <= TILE_SIZE / 2.0 + 0.001; dz += step) {
                        double wx = worldX + dx;
                        double wz = worldZ + dz;
                        double wy = FLOOR_LEVEL;

                        int[] screenPos = new int[2];
                        double[] depth = new double[1];

                        if (projectToScreen(wx, wy, wz, screenPos, depth)) {
                            int sx = screenPos[0];
                            int sy = screenPos[1];
                            int idx = sx + sy * WIDTH;

                            if (depth[0] > zBuffer[idx]) {
                                zBuffer[idx] = depth[0];

                                if (isPainted) {
                                    int r = paperGridR[gx][gz];
                                    int g = paperGridG[gx][gz];
                                    int b = paperGridB[gx][gz];

                                    buffer[idx] = String.format("\u001B[38;2;%d;%d;%dm█\u001B[0m", r, g, b);
                                } else {
                                    int r, g, b;
                                    if ((gx + gz) % 2 == 0) {
                                        r = 55; g = 60; b = 70;
                                    } else {
                                        r = 170; g = 175; b = 185;
                                    }

                                    boolean isEdge = (Math.abs(dx) > (TILE_SIZE / 2.0 - 0.03)) || (Math.abs(dz) > (TILE_SIZE / 2.0 - 0.03));
                                    if (isEdge) {
                                        r = (int) (r * 0.6); g = (int) (g * 0.6); b = (int) (b * 0.6);
                                    }

                                    buffer[idx] = String.format("\u001B[38;2;%d;%d;%dm░\u001B[0m", r, g, b);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderRollingCube(String[] buffer, double[] zBuffer) {
        double halfGrid = (GRID_SIZE * TILE_SIZE) / 2.0;

        double startX = (cubeGridX * TILE_SIZE) - halfGrid + (TILE_SIZE / 2.0);
        double startZ = (cubeGridZ * TILE_SIZE) + 0.2;
        double cubeHalf = TILE_SIZE / 2.0;

        double pivotX = startX + (rollDirX * cubeHalf);
        double pivotZ = startZ + (rollDirZ * cubeHalf);
        double pivotY = FLOOR_LEVEL;

        double rollAngle = rollProgress * (Math.PI / 2.0);

        int baseR = (int) (165 + 90 * Math.sin(colorHueAngle));
        int baseG = (int) (165 + 90 * Math.sin(colorHueAngle + 2.0 * Math.PI / 3.0));
        int baseB = (int) (165 + 90 * Math.sin(colorHueAngle + 4.0 * Math.PI / 3.0));

        baseR = Math.min(255, Math.max(120, baseR));
        baseG = Math.min(255, Math.max(120, baseG));
        baseB = Math.min(255, Math.max(120, baseB));

        for (int face = 0; face < 6; face++) {
            for (double u = -cubeHalf; u <= cubeHalf; u += 0.03) {
                for (double v = -cubeHalf; v <= cubeHalf; v += 0.03) {
                    double lx = 0, ly = 0, lz = 0;
                    double faceLight = 0.85;

                    switch (face) {
                        case 0: lx = u; ly = -cubeHalf; lz = v; faceLight = 1.00; break; // Top
                        case 1: lx = u; ly = cubeHalf; lz = v; faceLight = 0.80; break;  // Bottom
                        case 2: lx = u; ly = v; lz = cubeHalf; faceLight = 0.93; break;  // Front
                        case 3: lx = u; ly = v; lz = -cubeHalf; faceLight = 0.82; break; // Back
                        case 4: lx = cubeHalf; ly = u; lz = v; faceLight = 0.88; break;  // Right
                        case 5: lx = -cubeHalf; ly = u; lz = v; faceLight = 0.85; break; // Left
                    }

                    double wx = startX + lx;
                    double wy = FLOOR_LEVEL - cubeHalf + ly;
                    double wz = startZ + lz;

                    if (isRolling) {
                        double rx = wx - pivotX;
                        double ry = wy - pivotY;
                        double rz = wz - pivotZ;

                        if (rollDirX == 1) {
                            double newX = rx * Math.cos(rollAngle) - ry * Math.sin(rollAngle);
                            double newY = rx * Math.sin(rollAngle) + ry * Math.cos(rollAngle);
                            wx = pivotX + newX; wy = pivotY + newY;
                        } else if (rollDirX == -1) {
                            double newX = rx * Math.cos(-rollAngle) - ry * Math.sin(-rollAngle);
                            double newY = rx * Math.sin(-rollAngle) + ry * Math.cos(-rollAngle);
                            wx = pivotX + newX; wy = pivotY + newY;
                        } else if (rollDirZ == 1) { // Up (+Z)
                            double newZ = rz * Math.cos(rollAngle) - ry * Math.sin(rollAngle);
                            double newY = rz * Math.sin(rollAngle) + ry * Math.cos(rollAngle);
                            wz = pivotZ + newZ; wy = pivotY + newY;
                        } else if (rollDirZ == -1) { // Down (-Z)
                            double newZ = rz * Math.cos(-rollAngle) - ry * Math.sin(-rollAngle);
                            double newY = rz * Math.sin(-rollAngle) + ry * Math.cos(-rollAngle);
                            wz = pivotZ + newZ; wy = pivotY + newY;
                        }
                    }

                    int[] screenPos = new int[2];
                    double[] depth = new double[1];

                    if (projectToScreen(wx, wy, wz, screenPos, depth)) {
                        int sx = screenPos[0];
                        int sy = screenPos[1];
                        int idx = sx + sy * WIDTH;

                        int r = (int) (baseR * faceLight);
                        int g = (int) (baseG * faceLight);
                        int b = (int) (baseB * faceLight);

                        if (depth[0] > (zBuffer[idx] + 0.0001)) {
                            zBuffer[idx] = depth[0];
                            buffer[idx] = String.format("\u001B[38;2;%d;%d;%dm█\u001B[0m", r, g, b);
                        }
                    }
                }
            }
        }
    }

    private boolean projectToScreen(double wx, double wy, double wz, int[] screenOut, double[] depthOut) {
        double dx = wx - 0.0;
        double dy = wy - CAMERA_Y;
        double dz = wz - CAMERA_Z;

        double rad = Math.toRadians(CAMERA_PITCH_DEG);
        double cosP = Math.cos(rad);
        double sinP = Math.sin(rad);

        double ry = dy * cosP - dz * sinP;
        double rz = dy * sinP + dz * cosP;

        if (rz <= 0.2) return false;

        double fieldOfView = 1.1;
        double charAspectCorrection = 2.1; 

        double screenX = (dx / rz) * fieldOfView;
        double screenY = (ry / rz) * fieldOfView * charAspectCorrection;

        int sx = (int) ((screenX + 1.0) * 0.5 * WIDTH);
        int sy = (int) ((screenY + 0.8) * 0.5 * HEIGHT);

        if (sx < 0 || sx >= WIDTH || sy < 0 || sy >= HEIGHT) return false;

        screenOut[0] = sx;
        screenOut[1] = sy;
        depthOut[0] = 1.0 / rz;
        return true;
    }
}