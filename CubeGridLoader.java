// TODO: Fix the flightpath sometimes goes inside of cubes.  The step density could use a bump when close to the camera too

import java.util.Arrays;

public class CubeGridLoader extends Loader {

    private static final StatusStage[] GRID_STAGES = {
            new StatusStage(10, "Initializing 3D projection matrix..."),
            new StatusStage(30, "Generating infinite lattice grid..."),
            new StatusStage(50, "Engaging camera motion vectors..."),
            new StatusStage(75, "Raysorting volumetric depth buffers..."),
            new StatusStage(100, "Tumbling into the grid matrix!")
    };

    private double camX = 0.0;
    private double camY = 0.0;
    private double camZ = 0.0;

    private double pitch = 0.0;
    private double yaw = 0.0;
    private double roll = 0.0;

    private int frameTick = 0;

    private final int width;
    private final int height;

    // Palette: Cyberpunk Matrix Neon Colors
    private static final String C_CYAN   = "\u001B[38;2;80;220;250m"; 
    private static final String C_PURPLE = "\u001B[38;2;180;90;255m"; 
    private static final String C_GREEN  = "\u001B[38;2;50;240;140m";  
    private static final String RESET    = "\u001B[0m";

    public CubeGridLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public CubeGridLoader() {
        super(GRID_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.camX = 0.0;
        this.camY = 0.0;
        this.camZ = 0.0;
        this.pitch = 0.1;
        this.yaw = 0.2;
        this.roll = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;

        // Camera flight controls: scrolling continuously forward and drifting
        camZ += 0.08;
        camX += 0.03 * Math.sin(frameTick * 0.02);
        camY += 0.02 * Math.cos(frameTick * 0.025);

        // Gradually tumbling camera rotation (Pitch, Yaw, Roll)
        pitch = 0.35 * Math.sin(frameTick * 0.015);
        yaw   += 0.012;
        roll  = 0.25 * Math.cos(frameTick * 0.018);

        // Compute 3D Camera Rotation Matrix
        double cP = Math.cos(pitch), sP = Math.sin(pitch);
        double cY = Math.cos(yaw),   sY = Math.sin(yaw);
        double cR = Math.cos(roll),  sR = Math.sin(roll);

        // Dynamic directional light vector
        double lightX = 0.5, lightY = -0.7, lightZ = 0.5;
        double lLen = Math.hypot(lightX, Math.hypot(lightY, lightZ));
        lightX /= lLen; lightY /= lLen; lightZ /= lLen;

        // Infinite Grid Spacing parameters
        double spacing = 2.8; 
        double cubeSize = 1.0;
        int gridRadius = 4; // Renders 9x9x9 grid neighborhood wrapping around camera

        int baseIx = (int) Math.floor(camX / spacing);
        int baseIy = (int) Math.floor(camY / spacing);
        int baseIz = (int) Math.floor(camZ / spacing);

        // Render wrapping 3D cube lattice
        for (int dx = -gridRadius; dx <= gridRadius; dx++) {
            for (int dy = -gridRadius; dy <= gridRadius; dy++) {
                for (int dz = -gridRadius; dz <= gridRadius; dz++) {

                    int ix = baseIx + dx;
                    int iy = baseIy + dy;
                    int iz = baseIz + dz;

                    // World position of the current cube in the infinite grid
                    double worldX = ix * spacing;
                    double worldY = iy * spacing;
                    double worldZ = iz * spacing;

                    // Position relative to camera position
                    double relX = worldX - camX;
                    double relY = worldY - camY;
                    double relZ = worldZ - camZ;

                    // Color assignment based on spatial index pattern
                    String cubeColor = C_CYAN;
                    if ((ix + iy + iz) % 3 == 0) cubeColor = C_PURPLE;
                    else if ((ix + iy + iz) % 2 == 0) cubeColor = C_GREEN;

                    renderCube(relX, relY, relZ, cubeSize, cubeColor,
                            cP, sP, cY, sY, cR, sR,
                            lightX, lightY, lightZ, outputBuffer, zBuffer);
                }
            }
        }
    }

    private void renderCube(double cx, double cy, double cz, double size, String colorCode,
                            double cP, double sP, double cY, double sY, double cR, double sR,
                            double lx, double ly, double lz, String[] out, double[] zb) {

        double hs = size / 2.0;
        double step = 0.05; // Sampling density along cube face quads

        // Render 6 faces of the cube
        // Front (+Z) & Back (-Z)
        renderFace(cx, cy, cz, hs,  0,  0,  1, step, colorCode, cP, sP, cY, sY, cR, sR, lx, ly, lz, out, zb);
        renderFace(cx, cy, cz, hs,  0,  0, -1, step, colorCode, cP, sP, cY, sY, cR, sR, lx, ly, lz, out, zb);

        // Top (+Y) & Bottom (-Y)
        renderFace(cx, cy, cz, hs,  0,  1,  0, step, colorCode, cP, sP, cY, sY, cR, sR, lx, ly, lz, out, zb);
        renderFace(cx, cy, cz, hs,  0, -1,  0, step, colorCode, cP, sP, cY, sY, cR, sR, lx, ly, lz, out, zb);

        // Right (+X) & Left (-X)
        renderFace(cx, cy, cz, hs,  1,  0,  0, step, colorCode, cP, sP, cY, sY, cR, sR, lx, ly, lz, out, zb);
        renderFace(cx, cy, cz, hs, -1,  0,  0, step, colorCode, cP, sP, cY, sY, cR, sR, lx, ly, lz, out, zb);
    }

    private void renderFace(double cx, double cy, double cz, double hs,
                            double nx, double ny, double nz, double step, String colorCode,
                            double cP, double sP, double cY, double sY, double cR, double sR,
                            double lx, double ly, double lz, String[] out, double[] zb) {

        for (double u = -hs; u <= hs; u += step) {
            for (double v = -hs; v <= hs; v += step) {
                double px = cx, py = cy, pz = cz;

                if (Math.abs(nz) > 0.5) { // Facing Z
                    px += u; py += v; pz += nz * hs;
                } else if (Math.abs(ny) > 0.5) { // Facing Y
                    px += u; py += ny * hs; pz += v;
                } else { // Facing X
                    px += nx * hs; py += u; pz += v;
                }

                projectPoint(px, py, pz, nx, ny, nz, colorCode,
                        cP, sP, cY, sY, cR, sR, lx, ly, lz, out, zb);
            }
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                              double cP, double sP, double cY, double sY, double cR, double sR,
                              double lx, double ly, double lz, String[] out, double[] zb) {

        // 1. Camera Rotation Matrix (Yaw -> Pitch -> Roll)
        // Yaw
        double x1 = px * cY + pz * sY;
        double y1 = py;
        double z1 = -px * sY + pz * cY;

        // Pitch
        double x2 = x1;
        double y2 = y1 * cP - z1 * sP;
        double z2 = y1 * sP + z1 * cP;

        // Roll
        double rotX = x2 * cR - y2 * sR;
        double rotY = x2 * sR + y2 * cR;
        double rotZ = z2;

        // Rotate Normal Vectors for light shading
        double nx1 = nx * cY + nz * sY;
        double ny1 = ny;
        double nz1 = -nx * sY + nz * cY;

        double nx2 = nx1;
        double ny2 = ny1 * cP - nz1 * sP;
        double nz2 = ny1 * sP + nz1 * cP;

        double rotNX = nx2 * cR - ny2 * sR;
        double rotNY = nx2 * sR + ny2 * cR;
        double rotNZ = nz2;

        // Clip points behind camera perspective plane
        if (rotZ <= 0.2) return;

        double D = 1.0 / rotZ;

        // Screen mapping to terminal character grid
        int sx = (int) (width / 2.0 + 40.0 * D * rotX);
        int sy = (int) (height / 2.0 - 20.0 * D * rotY);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                
                // Add fog/depth fading effect as cubes recede into the distance
                double fog = Math.max(0.1, 1.0 - (rotZ / 10.0));
                double illuminance = Math.min(1.0, Math.max(0.15, dot + 0.2)) * fog;

                char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];

                out[idx] = colorCode + glyph + RESET;
            }
        }
    }
}