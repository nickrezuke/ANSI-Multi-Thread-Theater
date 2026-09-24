import java.util.Arrays;

public class RainbowPrismLoader extends Loader {

    private static final StatusStage[] PRISM_STAGES = {
            new StatusStage(10, "Constructing wireframe bounding geometry..."),
            new StatusStage(30, "Mapping spectrum gradients across vertices..."),
            new StatusStage(50, "Suspending RGB core triangle..."),
            new StatusStage(75, "Engaging independent rotation gyros..."),
            new StatusStage(100, "Refracting prism matrix!")
    };

    private double cubePitch = 0.0;
    private double cubeYaw = 0.0;
    private double cubeRoll = 0.0;

    private double triPitch = 0.0;
    private double triYaw = 0.0;
    private double triRoll = 0.0;

    private int frameTick = 0;

    private final int width;
    private final int height;

    private static final String RESET = "\u001B[0m";

    public RainbowPrismLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public RainbowPrismLoader() {
        super(PRISM_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.cubePitch = 0.2;
        this.cubeYaw = 0.3;
        this.cubeRoll = 0.0;

        this.triPitch = -0.4;
        this.triYaw = 0.0;
        this.triRoll = 0.2;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;

        // 1. Independent Motion Kinematics
        cubePitch += 0.018;
        cubeYaw   += 0.025;
        cubeRoll  += 0.012;

        triPitch -= 0.035;
        triYaw   -= 0.048;
        triRoll  += 0.022;

        // Directional Light Vector (kept for compatibility, though shapes are fully lit now)
        double lightX = 0.4, lightY = -0.8, lightZ = 0.5;
        double lLen = Math.hypot(lightX, Math.hypot(lightY, lightZ));
        lightX /= lLen; lightY /= lLen; lightZ /= lLen;

        // 2. Render Outer Rainbow Wireframe Cube
        renderWireframeCube(2.45, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. Render Inner RGB Triangle Core
        renderRGBCoreTriangle(1.3, lightX, lightY, lightZ, outputBuffer, zBuffer);
    }

    private void renderWireframeCube(double size, double lx, double ly, double lz, 
                                     String[] out, double[] zb) {
        double h = size / 2.0;

        double[][] v = {
                {-h, -h, -h}, { h, -h, -h}, { h,  h, -h}, {-h,  h, -h},
                {-h, -h,  h}, { h, -h,  h}, { h,  h,  h}, {-h,  h,  h}
        };

        int[][] edges = {
                {0,1}, {1,2}, {2,3}, {3,0}, 
                {4,5}, {5,6}, {6,7}, {7,4}, 
                {0,4}, {1,5}, {2,6}, {3,7}  
        };

        double[][] rotV = new double[8][3];
        for (int i = 0; i < 8; i++) {
            rotV[i] = rotate3D(v[i][0], v[i][1], v[i][2], cubePitch, cubeYaw, cubeRoll);
        }

        for (int e = 0; e < edges.length; e++) {
            double[] p1 = rotV[edges[e][0]];
            double[] p2 = rotV[edges[e][1]];

            double dist = Math.hypot(p2[0] - p1[0], Math.hypot(p2[1] - p1[1], p2[2] - p1[2]));
            int steps = (int) (dist * 28);

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double px = p1[0] + (p2[0] - p1[0]) * t;
                double py = p1[1] + (p2[1] - p1[1]) * t;
                double pz = p1[2] + (p2[2] - p1[2]) * t;

                double hue = (e * 30.0 + t * 90.0 + frameTick * 3.0) % 360.0;

                projectPoint(px, py, pz, 0, 1, 0, hue, -1, -1, -1, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderRGBCoreTriangle(double radius, double lx, double ly, double lz, 
                                        String[] out, double[] zb) {
        double[] v0 = {0.0, radius, 0.0};                                
        double[] v1 = {-radius * 0.866, -radius * 0.5, 0.0};            
        double[] v2 = { radius * 0.866, -radius * 0.5, 0.0};            

        double[] r0 = rotate3D(v0[0], v0[1], v0[2], triPitch, triYaw, triRoll);
        double[] r1 = rotate3D(v1[0], v1[1], v1[2], triPitch, triYaw, triRoll);
        double[] r2 = rotate3D(v2[0], v2[1], v2[2], triPitch, triYaw, triRoll);

        double ax = r1[0] - r0[0], ay = r1[1] - r0[1], az = r1[2] - r0[2];
        double bx = r2[0] - r0[0], by = r2[1] - r0[1], bz = r2[2] - r0[2];

        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;

        double nLen = Math.hypot(nx, Math.hypot(ny, nz));
        if (nLen > 0) {
            nx /= nLen; ny /= nLen; nz /= nLen;
        }

        double step = 0.04;
        for (double u = 0; u <= 1.0; u += step) {
            for (double v = 0; u + v <= 1.0; v += step) {
                double w = 1.0 - u - v;

                double px = w * r0[0] + u * r1[0] + v * r2[0];
                double py = w * r0[1] + u * r1[1] + v * r2[1];
                double pz = w * r0[2] + u * r1[2] + v * r2[2];

                int red   = (int) (w * 255);
                int green = (int) (u * 255);
                int blue  = (int) (v * 255);

                // Render both front and back faces with solid brightness
                projectPoint(px, py, pz, nx, ny, nz, -1, red, green, blue, lx, ly, lz, out, zb);
                projectPoint(px, py, pz, -nx, -ny, -nz, -1, red, green, blue, lx, ly, lz, out, zb);
            }
        }
    }

    private double[] rotate3D(double x, double y, double z, double pitch, double yaw, double roll) {
        double cY = Math.cos(yaw), sY = Math.sin(yaw);
        double x1 = x * cY + z * sY;
        double y1 = y;
        double z1 = -x * sY + z * cY;

        double cP = Math.cos(pitch), sP = Math.sin(pitch);
        double x2 = x1;
        double y2 = y1 * cP - z1 * sP;
        double z2 = y1 * sP + z1 * cP;

        double cR = Math.cos(roll), sR = Math.sin(roll);
        double rx = x2 * cR - y2 * sR;
        double ry = x2 * sR + y2 * cR;
        double rz = z2;

        return new double[]{rx, ry, rz};
    }

    private String getRainbowANSI(double hue) {
        hue = ((hue % 360.0) + 360.0) % 360.0;
        double c = 1.0;
        double x = c * (1.0 - Math.abs((hue / 60.0) % 2.0 - 1.0));
        double r = 0, g = 0, b = 0;

        if (hue < 60)       { r = c; g = x; b = 0; }
        else if (hue < 120) { r = x; g = c; b = 0; }
        else if (hue < 180) { r = 0; g = c; b = x; }
        else if (hue < 240) { r = 0; g = x; b = c; }
        else if (hue < 300) { r = x; g = 0; b = c; }
        else                { r = c; g = 0; b = x; }

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);

        return String.format("\u001B[38;2;%d;%d;%dm", ir, ig, ib);
    }

    private String getRGBANSI(int r, int g, int b) {
        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz,
                              double hue, int r, int g, int b,
                              double lx, double ly, double lz, String[] out, double[] zb) {

        double cameraDepth = pz + 3.8;
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        int sx = (int) (width / 2.0 + 44.0 * D * px);
        int sy = (int) (height / 2.0 - 22.0 * D * py);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                String ansiColor;
                if (hue >= 0) {
                    ansiColor = getRainbowANSI(hue);
                } else {
                    ansiColor = getRGBANSI(r, g, b);
                }

                // Both shapes now use solid bright block characters ('█')
                char glyph = '█';

                out[idx] = ansiColor + glyph + RESET;
            }
        }
    }
}