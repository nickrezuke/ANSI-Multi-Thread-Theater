public class PendulumLoader extends InteractiveLoader {
    private static final StatusStage[] RING_STAGES = {
        new StatusStage(100, "3D Cartesian Physics Pendulum Active! [Arrow Keys, Q to quit]")
    };

    // Physics Tuning Parameters
    private static final double GRAVITY = 9.81;
    private static final double LENGTH = 3.5;    
    private static final double DT = 0.016;       
    private static final double DAMPING = 0.995;  

    // Cartesian State Vectors
    private double px, py, pz; 
    private double vx, vy, vz; 

    // Interactive Kick Impulse Scale
    private static final double KICK_FORCE = 3.5;

    // Shared Volatile Color Shift Parameter to bridge the background input thread and geometry thread
    private volatile double interactiveColorShift = 0.0;

    // History trail buffer
    private static final int TRAIL_SIZE = 45;
    private final double[][] trailX = new double[3][TRAIL_SIZE];

    public PendulumLoader() {
        super(RING_STAGES);
    }

    @Override
    protected void onInitialize() {
        this.px = 1.0;
        this.py = 0.0;
        this.pz = -Math.sqrt(LENGTH * LENGTH - px * px - py * py);
        this.vx = 0.0;
        this.vy = 0.0;
        this.vz = 0.0;
        this.interactiveColorShift = 0.0;
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        boolean validKey = false;
        switch (keyCode) {
            case 'A': vy += KICK_FORCE; validKey = true; break;
            case 'B': vy -= KICK_FORCE; validKey = true; break;
            case 'C': vx += KICK_FORCE; validKey = true; break;
            case 'D': vx -= KICK_FORCE; validKey = true; break;
        }

        // If a valid arrow key was struck, instantly skip the color wheel forward by 2/3rds of a full cycle
        if (validKey) {
            interactiveColorShift += (2.0 * Math.PI * 2.0 / 3.0);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // 1. RUN PENDULUM LAGRANGIAN PHYSICS INTEGRATION
        double ax = 0.0;
        double ay = 0.0;
        double az = -GRAVITY;

        vx += ax * DT;
        vy += ay * DT;
        vz += az * DT;

        vx *= DAMPING;
        vy *= DAMPING;
        vz *= DAMPING;

        px += vx * DT;
        py += vy * DT;
        pz += vz * DT;

        // 2. RUN RIGID CABLE CONSTRAINT PROJECTION
        double currentLength = Math.sqrt(px * px + py * py + pz * pz);
        if (currentLength < 0.001) currentLength = 0.001; 
        
        double nx = px / currentLength;
        double ny = py / currentLength;
        double nz = pz / currentLength;

        px = nx * LENGTH;
        py = ny * LENGTH;
        pz = nz * LENGTH;

        double velocityAlongCable = vx * nx + vy * ny + vz * nz;

        vx -= velocityAlongCable * nx;
        vy -= velocityAlongCable * ny;
        vz -= velocityAlongCable * nz;

        // 3. SHIFT PATH TRAIL BUFFER
        for (int i = TRAIL_SIZE - 1; i > 0; i--) {
            trailX[0][i] = trailX[0][i - 1];
            trailX[1][i] = trailX[1][i - 1];
            trailX[2][i] = trailX[2][i - 1];
        }
        trailX[0][0] = px;
        trailX[1][0] = py;
        trailX[2][0] = pz;

        // 4. CALCULATE DYNAMIC SYSTEM-TIME COLOR WHEEL VARIABLES
        // 12000 milliseconds = 12 seconds per full cycle loop rotation
        long timeMod = System.currentTimeMillis() % 12000;
        double baseTimeAngle = (timeMod / 12000.0) * 2.0 * Math.PI;
        
        // Combine baseline time rotation angle with our interactive key increments
        double finalHueAngle = baseTimeAngle + interactiveColorShift;

        // 5. RENDER FADING PATH TRAIL
        for (int i = 1; i < TRAIL_SIZE; i++) {
            if (trailX[0][i] == 0 && trailX[1][i] == 0) continue;
            
            double tx = trailX[0][i];
            double ty = trailX[1][i] + 6.0; 
            double tz = trailX[2][i] + 1.5; 
            
            double D = 1.0 / ty;
            int sx = (int) (40 + 55 * D * tx);
            int sy = (int) (11 - 25 * D * tz);
            int idx = sx + 80 * sy;

            if (22 > sy && sy >= 0 && sx >= 0 && 80 > sx && D > zBuffer[idx]) {
                zBuffer[idx] = D;
                int opacity = (int) (240 * (1.0 - ((double) i / TRAIL_SIZE)));
                outputBuffer[idx] = String.format("\u001B[38;2;0;%d;0m.\u001B[0m", opacity);
            }
        }

        // 6. DRAW THE SUSPENSION CABLE
        int cableSegments = 130;
        for (int i = 0; i <= cableSegments; i++) {
            double ratio = (double) i / cableSegments;
            double cx = px * ratio;
            double cy = py * ratio + 6.0;
            double cz = (pz * ratio) + 1.5;

            double D = 1.0 / cy;
            int sx = (int) (40 + 55 * D * cx);
            int sy = (int) (11 - 25 * D * cz);
            int idx = sx + 80 * sy;

            if (22 > sy && sy >= 0 && sx >= 0 && 80 > sx && D > zBuffer[idx]) {
                zBuffer[idx] = D;
                outputBuffer[idx] = WHITE + "·" + RESET;
            }
        }

        // 7. DRAW THE 3D SPHERICAL PENDULUM MASS BOB WITH RE-MAPPED COLOR
        double radius = 1.3;
        for (int bThetaIdx = 0; bThetaIdx < 30; bThetaIdx++) {
            double bTheta = bThetaIdx * (Math.PI / 30.0);
            for (int bPhiIdx = 0; bPhiIdx < 60; bPhiIdx++) {
                double bPhi = bPhiIdx * (2.0 * Math.PI / 60.0);

                double sX = px + radius * Math.sin(bTheta) * Math.cos(bPhi);
                double sZ = pz + radius * Math.sin(bTheta) * Math.sin(bPhi);
                double sY = py + radius * Math.cos(bTheta);

                double cameraDepth = sY + 6.0;
                double D = 1.0 / cameraDepth;

                int sx = (int) (40 + 55 * D * sX);
                int sy = (int) (11 - 25 * D * (sZ + 1.5));
                int idx = sx + 80 * sy;

                double normalShading = 11.0 * (Math.abs(Math.sin(bTheta) * Math.cos(bPhi)) * 0.4 + (D * 1.5));

                if (22 > sy && sy >= 0 && sx >= 0 && 80 > sx && D > (zBuffer[idx] + 0.0001)) {
                    zBuffer[idx] = D;
                    int charIndex = (int) Math.round(normalShading);
                    if (charIndex < 0) charIndex = 0;
                    String lString = ".,-~:;=!*#$@";
                    char asciiChar = lString.charAt(charIndex >= lString.length() ? lString.length() - 1 : charIndex);

                    // Map Sine waves out from our final calculated hue angle to derive standard 120-degree phase-shifted RGB vectors
                    int r = (int) (128 + 127 * Math.sin(finalHueAngle));
                    int g = (int) (128 + 127 * Math.sin(finalHueAngle + 2.0 * Math.PI / 3.0));
                    int b = (int) (128 + 127 * Math.sin(finalHueAngle + 4.0 * Math.PI / 3.0));

                    r = Math.min(255, Math.max(0, r));
                    g = Math.min(255, Math.max(0, g));
                    b = Math.min(255, Math.max(0, b));

                    String neonColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                    outputBuffer[idx] = neonColor + asciiChar + RESET;
                }
            }
        }
    }
}
