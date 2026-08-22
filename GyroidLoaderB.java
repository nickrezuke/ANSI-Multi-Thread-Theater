// TODO: This looks jittery, a little slow, and sometimes it skips?  Make this one better

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
        // This one asks for 100 x 26
        super(GYROID_STAGES, 100, 26);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    // Evaluates the continuous implicit gyroid surface density field
    private double evaluateGyroid(double x, double y, double z, double time) {
        // Feed time into the spatial coordinates to cause a liquid morphing shift
        double scale = 2.5 + 0.5 * Math.sin(time * 0.3);
        double sx = x * scale + time * 0.4;
        double sy = y * scale;
        double sz = z * scale + time * 0.2;
        
        return Math.sin(sx) * Math.cos(sy) + Math.sin(sy) * Math.cos(sz) + Math.sin(sz) * Math.cos(sx);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04;
        
        double cosRot = Math.cos(timeClock * 0.2);
        double sinRot = Math.sin(timeClock * 0.2);
        
        String shadingRamp = " .:-=+*#%@█";
        
        // Raymarch directly across every screen character cell
        for (int y = 0; y < height; y++) {
            // Map character space to normalized screen coordinates (-1.0 to 1.0)
            double screenY = ((double) y / height) * 2.0 - 1.0;
            
            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                // Correct for standard console font aspect ratio distortion (roughly 2.3)
                double screenX = (((double) x / width) * 2.0 - 1.0) * 2.3;
                
                // Camera Ray Setup
                double rayX = 0.0, rayY = 0.0, rayZ = -3.0; // Origin
                double dirX = screenX, dirY = screenY, dirZ = 1.8; // Direction vector
                
                // Normalize direction vector
                double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= length; dirY /= length; dirZ /= length;
                
                boolean hitSurface = false;
                double depthTravelled = 0.0;
                double maxDistance = 5.0;
                
                // Raymarch step loop
                for (int step = 0; step < 48; step++) {
                    double px = rayX + dirX * depthTravelled;
                    double py = rayY + dirY * depthTravelled;
                    double pz = rayZ + dirZ * depthTravelled;
                    
                    // Rotate the spatial ray calculation around the Y axis for global movement
                    double rx = px * cosRot - pz * sinRot;
                    double ry = py;
                    double rz = px * sinRot + pz * cosRot;
                    
                    double d = evaluateGyroid(rx, ry, rz, timeClock);
                    
                    // Isosurface target thickness check
                    if (Math.abs(d) < 0.12) {
                        hitSurface = true;
                        
                        // Use numerical differentiation to approximate surface normal vector
                        double eps = 0.01;
                        double nx = evaluateGyroid(rx + eps, ry, rz, timeClock) - evaluateGyroid(rx - eps, ry, rz, timeClock);
                        double ny = evaluateGyroid(rx, ry + eps, rz, timeClock) - evaluateGyroid(rx, ry - eps, rz, timeClock);
                        double nz = evaluateGyroid(rx, ry, rz + eps, timeClock) - evaluateGyroid(rx, ry, rz - eps, timeClock);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz + 0.0001);
                        nx /= nLen; ny /= nLen; nz /= nLen;
                        
                        // Fake specular rim lighting calculation
                        double rimLight = 1.0 - Math.abs(dirX * nx + dirY * ny + dirZ * nz);
                        if (rimLight < 0.0) rimLight = 0.0;
                        
                        double depthZ = 1.0 / (depthTravelled + 0.01);
                        if (depthZ > zBuffer[idx]) {
                            zBuffer[idx] = depthZ;
                            
                            // Map character weights to normal vectors for textural shading
                            int shadeIdx = (int) (rimLight * (shadingRamp.length() - 1));
                            char renderChar = shadingRamp.charAt(Math.max(0, Math.min(shadingRamp.length() - 1, shadeIdx)));
                            
                            // Vaporwave Neon Dream Palette (Pink/Cyan balance)
                            int r = (int) (200 + 55 * Math.sin(px * 2.0 + timeClock));
                            int g = (int) (100 + 100 * Math.cos(py * 2.0 - timeClock));
                            int b = (int) (220 + 35 * Math.sin(pz * 2.0));
                            
                            String color = String.format("\u001B[38;2;%d;%d;%dm", 
                                Math.max(0, Math.min(255, r)), 
                                Math.max(0, Math.min(255, g)), 
                                Math.max(0, Math.min(255, b))
                            );
                            
                            outputBuffer[idx] = color + renderChar + RESET;
                        }
                        break;
                    }
                    depthTravelled += 0.06; // Advance ray forward
                    if (depthTravelled > maxDistance) break;
                }
                
                // Render dark grid background if the ray misses completely
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
