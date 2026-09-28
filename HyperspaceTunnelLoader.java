// TODO: Fix the camera position and step sizing

import java.util.Arrays;

public class HyperspaceTunnelLoader extends Loader {

    private static final StatusStage[] TUNNEL_STAGES = {
            new StatusStage(10, "Spinning up FTL warp drives..."),
            new StatusStage(25, "Mapping spacetime curvature manifolds..."),
            new StatusStage(50, "Calibrating cylindrical projection matrix..."),
            new StatusStage(80, "Folding dimensional barriers..."),
            new StatusStage(100, "Entering hyperspace transit!")
    };

    private int frameTick = 0;

    private final int width;
    private final int height;

    // Palette: Vaporwave / Hyperspace Neon Colors
    private static final String C_CYAN    = "\u001B[38;2;0;255;255m";
    private static final String C_MAGENTA = "\u001B[38;2;255;0;255m";
    private static final String C_BLUE    = "\u001B[38;2;0;100;255m";
    private static final String C_WHITE   = "\u001B[38;2;255;255;255m";
    private static final String RESET     = "\u001B[0m";

    public HyperspaceTunnelLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public HyperspaceTunnelLoader() {
        super(TUNNEL_STAGES, 120, 30);
        this.width = 120;
        this.height = 30;
    }

    @Override
    protected void initialize() {
        setTargetFps(120); 
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;

        // Base tunnel radius
        double radius = 5.0;
        
        // Flight speed (advances the phase of the waves and textures)
        double speed = frameTick * 0.4;
        
        // Continuous camera roll to simulate spiraling out of control
        double roll = frameTick * 0.025;
        double cR = Math.cos(roll), sR = Math.sin(roll);

        // We don't move the camera forward in world space (to avoid floating point drift).
        // Instead, we pull the tunnel geometry towards the camera.
        // Z loops from very close (1.0) to far away (60.0).
        
        double z = 1.0;
        while (z < 60.0) {
            
            // Dynamic Level of Detail: 
            // Render denser rings closer to the camera to prevent "holes", 
            // and sparser rings further away to optimize performance.
            double zStep = Math.max(0.05, z * 0.015);
            
            // Bending the tunnel: The center of the tunnel drifts using sine waves.
            // We use (z - speed) so the curves appear to rush TOWARDS the camera.
            double curveZ = z - speed * 0.5;
            double pathX = Math.sin(curveZ * 0.08) * 4.0 + Math.sin(curveZ * 0.03) * 2.0;
            double pathY = Math.cos(curveZ * 0.07) * 4.0 + Math.sin(curveZ * 0.04) * 2.0;

            // Generate a ring of points at the current Z depth
            double aStep = 0.05; // Angular step for the circle
            for (double a = 0; a < Math.PI * 2; a += aStep) {
                
                // Base cylindrical coordinates translated to the curved path
                double pX = pathX + radius * Math.cos(a);
                double pY = pathY + radius * Math.sin(a);
                
                // Apply Camera Roll
                double rotX = pX * cR - pY * sR;
                double rotY = pX * sR + pY * cR;
                double rotZ = z;

                // Projection / Perspective calculation
                double D = 1.0 / rotZ;
                
                // Map to 2D screen coordinates
                // Multiplying by wider FOV scalars because the tunnel is large
                int sx = (int) (width / 2.0 + 100.0 * D * rotX);
                int sy = (int) (height / 2.0 - 50.0 * D * rotY);

                if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                    int idx = sx + width * sy;

                    // Standard Z-Buffer depth check
                    if (D > zBuffer[idx]) {
                        zBuffer[idx] = D;

                        // 1. Fog / Distance fading
                        double fog = Math.max(0.0, 1.0 - (rotZ / 50.0));
                        
                        // 2. Warp Pulse: Bright waves of light traveling down the tunnel
                        double pulse = (Math.sin(z * 0.5 - speed) + 1.0) / 2.0; 
                        
                        // 3. Spiral texture mapping (creates the twisting wireframe illusion)
                        double spiral = Math.sin(a * 4.0 + z * 0.3 - speed * 0.8);

                        // Combine lighting
                        double illuminance = (0.2 + pulse * 0.6) * fog;

                        // Determine Color based on the spiral texture
                        String colorCode;
                        if (spiral > 0.6) {
                            colorCode = C_WHITE; // Bright highlights on the ridges
                            illuminance *= 1.5; 
                        } else if (spiral > 0.0) {
                            colorCode = C_CYAN;
                        } else if (spiral > -0.6) {
                            colorCode = C_BLUE;
                        } else {
                            colorCode = C_MAGENTA;
                        }

                        // Map illuminance to ASCII shading ramp
                        char[] ramp = {'.', ',', '-', '~', ':', '=', '+', '*', '#', '%', '@', '█'};
                        int rampIdx = (int) (illuminance * (ramp.length - 1));
                        rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                        char glyph = ramp[rampIdx];

                        // Occasional "star streaks" (random bright characters)
                        if (Math.random() > 0.99 && fog > 0.3) {
                            glyph = '-';
                            colorCode = C_WHITE;
                        }

                        outputBuffer[idx] = colorCode + glyph + RESET;
                    }
                }
            }
            z += zStep;
        }
    }
}