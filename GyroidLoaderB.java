public class GyroidLoaderB extends Loader {
    private static final StatusStage[] GYROID_STAGES = {
        new StatusStage(30, "Condensing liquid vapor fields:"),
        new StatusStage(60, "Solving periodic differential matrices:"),
        new StatusStage(90, "Tracing isosurface depth bounds:"),
        new StatusStage(100, "Gyroid Core Floating Perfectly!")
    };

    private double timeClock = 0.0;
    private final int width = 100;
    private final int height = 26;

    public GyroidLoaderB() {
        super(GYROID_STAGES, 100, 26);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    private double evaluateGyroid(double x, double y, double z, double time) {
        double scale = 2.5 + 0.5 * Math.sin(time * 0.3);
        double sx = x * scale + time * 0.4;
        double sy = y * scale;
        double sz = z * scale + time * 0.2;
        return Math.sin(sx) * Math.cos(sy) + Math.sin(sy) * Math.cos(sz) + Math.sin(sz) * Math.cos(sx);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.03; // Slightly slowed down time increment for ultra-fluid movement
        double cosRot = Math.cos(timeClock * 0.2);
        double sinRot = Math.sin(timeClock * 0.2);
        String shadingRamp = " .:-=+*#%@█";

        for (int y = 0; y < height; y++) {
            double screenY = ((double) y / height) * 2.0 - 1.0;
            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                double screenX = (((double) x / width) * 2.0 - 1.0) * 2.3;

                // Camera Setup
                double rayX = 0.0, rayY = 0.0, rayZ = -3.5; // Pushed camera back a bit to see more
                double dirX = screenX, dirY = screenY, dirZ = 1.8;

                double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= length;
                dirY /= length;
                dirZ /= length;

                boolean hitSurface = false;
                double depthTravelled = 0.0;
                
                // EXTENDED HORIZON: Increased max distance and maximum step count for depth
                double maxDistance = 7.5; 
                int maxSteps = 75;

                for (int step = 0; step < maxSteps; step++) {
                    double px = rayX + dirX * depthTravelled;
                    double py = rayY + dirY * depthTravelled;
                    double pz = rayZ + dirZ * depthTravelled;

                    double rx = px * cosRot - pz * sinRot;
                    double ry = py;
                    double rz = px * sinRot + pz * cosRot;

                    double d = evaluateGyroid(rx, ry, rz, timeClock);
                    double absD = Math.abs(d);

                    // FLUID MOTION FIX: Thinner target thickness threshold combined with adaptive steps
                    if (absD < 0.08) {
                        hitSurface = true;

                        // Surface Normals Calculation
                        double eps = 0.005; // Finer epsilon for smoother lighting transitions
                        double nx = evaluateGyroid(rx + eps, ry, rz, timeClock) - evaluateGyroid(rx - eps, ry, rz, timeClock);
                        double ny = evaluateGyroid(rx, ry + eps, rz, timeClock) - evaluateGyroid(rx, ry - eps, rz, timeClock);
                        double nz = evaluateGyroid(rx, ry, rz + eps, timeClock) - evaluateGyroid(rx, ry, rz - eps, timeClock);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz + 0.0001);
                        nx /= nLen; ny /= nLen; nz /= nLen;

                        double rimLight = 1.0 - Math.abs(dirX * nx + dirY * ny + dirZ * nz);
                        if (rimLight < 0.0) rimLight = 0.0;

                        double distanceFade = 1.0 - (depthTravelled / maxDistance);
                        distanceFade = Math.max(0.0, Math.min(1.0, distanceFade));
                        rimLight *= distanceFade; 

                        // Use Z-Buffer logic
                        double depthZ = 1.0 / (depthTravelled + 0.01);
                        if (depthZ > zBuffer[idx]) {
                            zBuffer[idx] = depthZ;

                            int shadeIdx = (int) (rimLight * (shadingRamp.length() - 1));
                            char renderChar = shadingRamp.charAt(Math.max(0, Math.min(shadingRamp.length() - 1, shadeIdx)));

                            // Neon Dream Palette (with distance-based brightness throttling)
                            int r = (int) ((200 + 55 * Math.sin(px * 2.0 + timeClock)) * distanceFade);
                            int g = (int) ((100 + 100 * Math.cos(py * 2.0 - timeClock)) * distanceFade);
                            int b = (int) ((220 + 35 * Math.sin(pz * 2.0)) * distanceFade);

                            String color = String.format("\u001B[38;2;%d;%d;%dm", 
                                Math.max(0, Math.min(255, r)), 
                                Math.max(0, Math.min(255, g)), 
                                Math.max(0, Math.min(255, b))
                            );
                            outputBuffer[idx] = color + renderChar + RESET;
                        }
                        break;
                    }

                    // ADAPTIVE RAY DIRECTION TRAVEL:
                    // If we are far from a surface boundary, take a slightly larger safe leap.
                    // If we get close, decrease step size so we don't jitter right through it.
                    depthTravelled += (absD > 0.5) ? 0.08 : 0.04;

                    if (depthTravelled > maxDistance) break;
                }

                if (!hitSurface) {
                    if (x % 8 == 0 && y % 4 == 0) {
                        outputBuffer[idx] = "\u001B[38;2;40;45;60m+\u001B[0m";
                    } else {
                        outputBuffer[idx] = " ";
                    }
                    zBuffer[idx] = 0.0;
                }
            }
        }
    }
}
