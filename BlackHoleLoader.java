public class BlackHoleLoader extends Loader {
    private static final StatusStage[] STAGES = {
            new StatusStage(30, "Generating singularity core:"),
            new StatusStage(60, "Collapsing localized space-time:"),
            new StatusStage(85, "Stabilizing event horizon:"),
            new StatusStage(100, "Gravitational Lock Achieved!")
    };

    // Physics constants (Fine-tuned for dramatic relativistic distortion warping)
    private static final double STEP_SIZE = 0.14;
    private static final double HORIZON_RADIUS = 2.0;
    private static final double DISK_INNER = 3.2;
    private static final double DISK_OUTER = 13.0;
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // Simulation clocks
    private double timeClock = 0.0;
    private double cameraAngle = 0.0;

    // Camera coordinate vectors
    private final double[] camPos = new double[3];
    private final double[] camDir = new double[3];
    private final double[] camUp = new double[3];
    private final double[] camRight = new double[3];

    public BlackHoleLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        timeClock = 0.0;
        cameraAngle = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        for(int i = 0; i < zBuffer.length; i++) {
            zBuffer[i] = 0.0;
        }

        // 1. DYNAMIC CAMERA ORBIT: Gentle cinematic rotation around the gravity well
        cameraAngle += 0.020;
        double rCam = 13.5; // Pulled closer to maximize lens distortions across the viewport
        double pitch = 0.32 + 0.08 * Math.sin(timeClock * 0.4); // Subtle camera bobbing

        camPos[0] = rCam * Math.cos(cameraAngle) * Math.cos(pitch);
        camPos[1] = rCam * Math.sin(pitch);
        camPos[2] = rCam * Math.sin(cameraAngle) * Math.cos(pitch);

        // Look-at Target Transformation Matrix
        double[] target = { 0.0, 0.0, 0.0 };
        double[] forward = { target[0] - camPos[0], target[1] - camPos[1], target[2] - camPos[2] };
        normalize(forward, camDir);
        double[] worldUp = { 0.0, 1.0, 0.0 };
        crossProduct(camDir, worldUp, camRight);
        normalize(camRight, camRight);
        crossProduct(camRight, camDir, camUp);
        normalize(camUp, camUp);

        // 2. BACKWARD PHOTON RAYTRACE ENGINE LOOP
        for (int y = 0; y < HEIGHT; y++) {
            // Perspective aspect scale mapping
            double screenY = ((y - (HEIGHT / 2.0)) / (HEIGHT / 2.0)) * 0.58;
            for (int x = 0; x < WIDTH; x++) {
                double screenX = ((x - (WIDTH / 2.0)) / (WIDTH / 2.0)) * 1.05;
                int offset = x + WIDTH * y;

                double[] pPos = { camPos[0], camPos[1], camPos[2] };
                double[] pVel = {
                        camDir[0] + screenX * camRight[0] + screenY * camUp[0],
                        camDir[1] + screenX * camRight[1] + screenY * camUp[1],
                        camDir[2] + screenX * camRight[2] + screenY * camUp[2]
                };
                normalize(pVel, pVel);
                tracePhoton(pPos, pVel, outputBuffer, zBuffer, offset);
            }
        }
        timeClock += 0.04;
    }

    private void tracePhoton(double[] pos, double[] vel, String[] outBuf, double[] zBuf, int offset) {
        int maxSteps = 220;
        for (int step = 0; step < maxSteps; step++) {
            double r2 = pos[0] * pos[0] + pos[1] * pos[1] + pos[2] * pos[2];
            double r = Math.sqrt(r2);

            // Swallowed by the Singularity (Pure black shadow core - Gargantua)
            if (r <= HORIZON_RADIUS) {
                zBuf[offset] = 9999.0;
                outBuf[offset] = " ";
                return;
            }

            // Escaped safely to outer space boundaries
            if (r > 45.0) {
                break;
            }

            double oldY = pos[1];
            double[] oldPos = { pos[0], pos[1], pos[2] };

            // Runge-Kutta 4th Order Relativity Integrator step
            rk4Step(pos, vel);

            // Accretion Disk Intercept Intersection (Plane Y = 0)
            if ((oldY > 0 && pos[1] <= 0) || (oldY < 0 && pos[1] >= 0)) {
                double alpha = (0.0 - oldY) / (pos[1] - oldY);
                double interX = oldPos[0] + alpha * (pos[0] - oldPos[0]);
                double interZ = oldPos[2] + alpha * (pos[2] - oldPos[2]);
                double interR = Math.sqrt(interX * interX + interZ * interZ);

                if (interR >= DISK_INNER && interR <= DISK_OUTER) {
                    zBuf[offset] = r;
                    double diskAngle = Math.atan2(interZ, interX);
                    double swirl = diskAngle + (5.5 / (interR + 0.1)) - timeClock;

                    // Procedural high-contrast plasma gas lanes
                    double densityWave = Math.sin(5.0 * swirl) * Math.cos(0.8 * interR);

                    if (densityWave > -0.3) {
                        // Relativistic Doppler Computations
                        double vDiskX = -interZ / interR;
                        double vDiskZ = interX / interR;
                        double speedScalar = 0.48 / Math.sqrt(interR); // Orbit velocity fallback
                        vDiskX *= speedScalar;
                        vDiskZ *= speedScalar;

                        double dotProduct = vel[0] * vDiskX + vel[2] * vDiskZ;
                        double gamma = 1.0 / Math.sqrt(1.0 - speedScalar * speedScalar);
                        double dopplerShift = 1.0 / (gamma * (1.0 - dotProduct));

                        double baselineIntensity = (1.2 / (interR * 0.20)) * (densityWave + 0.8);
                        double finalIntensity = baselineIntensity * Math.pow(dopplerShift, 3.0);

                        // --- UPGRADED RETRO-TECH PARTICLE & GAS DENSITY OVERHAUL ---
                        String ansiColor;
                        char blockChar;

                        // 9-Step Micro-Granular Shading Scale (Smoothly tapers from 100% mass down to lone pixels)
                        if (finalIntensity > 1.6)        blockChar = '\u2588'; // █ (Blazing Peak Core)
                        else if (finalIntensity > 1.2)   blockChar = '\u2593'; // ▓ (Dense Plasma Layer)
                        else if (finalIntensity > 0.9)   blockChar = '\u2592'; // ▒ (Fluid Mid-tone Shading)
                        else if (finalIntensity > 0.65)  blockChar = '\u2591'; // ░ (Light Vapor Gas)
                        else if (finalIntensity > 0.48)  blockChar = '\u205C'; // ⁜ (Dotted Cross - adds airy technical grain)
                        else if (finalIntensity > 0.35)  blockChar = '\u00A4'; // ¤ (Currency Node - behaves like a heavy particle)
                        else if (finalIntensity > 0.22)  blockChar = '\u2058'; // ⁘ (Four-Dot Scatter Punctuation)
                        else if (finalIntensity > 0.12)  blockChar = '\u2022'; // • (Defined Outer Bullet Filament)
                        else if (finalIntensity > 0.05)  blockChar = '\u00B7'; // · (Faint Gravitational Dust Tail)
                        else                             blockChar = ' ';      // Space Vacuum Void

                        // Cinematic Blinding White-Blue -> Deep Hot Crimson color spectrum shifts
                        if (dopplerShift > 1.25)       ansiColor = "\u001B[38;5;159m"; // Pure white-hot approaching tip
                        else if (dopplerShift > 1.08)  ansiColor = "\u001B[38;5;220m"; // Electric Gold Core
                        else if (dopplerShift > 0.95)  ansiColor = "\u001B[38;5;208m"; // Bright Outrun Orange
                        else if (dopplerShift > 0.72)  ansiColor = "\u001B[38;5;160m"; // Receding Red-Shift
                        else                           ansiColor = "\u001B[38;5;53m";  // Extreme Gravitational Plum Red

                        // Blit the color code and custom particle texture down to the screen buffer
                        if (blockChar != ' ') {
                            outBuf[offset] = ansiColor + blockChar + RESET;
                            return;
                        }

                    }
                }
            }
        }

        // 3. GRAVITATIONAL LENSING BACKGROUND COSMIC GRID
        if (zBuf[offset] == 0.0) {
            double escapePhi = Math.atan2(vel[2], vel[0]);
            double escapeTheta = Math.acos(vel[1]);

            // Sharpened distant cosmic coordinates tracking grid parameters
            double gridX = Math.sin(escapePhi * 12.0);
            double gridY = Math.sin(escapeTheta * 12.0);

            // Cleaned up the background lines! Replaced clumsy "#" blocks with crisp matrix
            // tracks
            boolean isGridLine = (Math.abs(gridX) > 0.96) || (Math.abs(gridY) > 0.96);

            if (isGridLine) {
                // Sleek, dim ultraviolet grid lines warped by general relativity
                outBuf[offset] = "\u001B[38;5;61m\u253C" + RESET; // ┼ (Fine tracking hair crosshairs)
            } else {
                // Render sharp glittering background star particles floating behind the void
                double starsNoise = Math.sin(escapePhi * 50.0) * Math.cos(escapeTheta * 50.0);
                if (starsNoise > 0.94) {
                    outBuf[offset] = "\u001B[38;5;248m."; // Bright drifting distant suns
                } else {
                    outBuf[offset] = " "; // Deep vacuum void space
                }
            }
        }
    }

    private void rk4Step(double[] pos, double[] vel) {
        double[] k1Pos = new double[3], k1Vel = new double[3];
        double[] k2Pos = new double[3], k2Vel = new double[3];
        double[] k3Pos = new double[3], k3Vel = new double[3];
        double[] k4Pos = new double[3], k4Vel = new double[3];

        getAcceleration(pos, vel, k1Vel);
        k1Pos[0] = vel[0]; k1Pos[1] = vel[1]; k1Pos[2] = vel[2];

        double[] p2 = { pos[0] + k1Pos[0]*STEP_SIZE*0.5, pos[1] + k1Pos[1]*STEP_SIZE*0.5, pos[2] + k1Pos[2]*STEP_SIZE*0.5 };
        double[] v2 = { vel[0] + k1Vel[0]*STEP_SIZE*0.5, vel[1] + k1Vel[1]*STEP_SIZE*0.5, vel[2] + k1Vel[2]*STEP_SIZE*0.5 };
        getAcceleration(p2, v2, k2Vel);
        k2Pos[0] = v2[0]; k2Pos[1] = v2[1]; k2Pos[2] = v2[2];

        double[] p3 = { pos[0] + k2Pos[0]*STEP_SIZE*0.5, pos[1] + k2Pos[1]*STEP_SIZE*0.5, pos[2] + k2Pos[2]*STEP_SIZE*0.5 };
        double[] v3 = { vel[0] + k2Vel[0]*STEP_SIZE*0.5, vel[1] + k2Vel[1]*STEP_SIZE*0.5, vel[2] + k2Vel[2]*STEP_SIZE*0.5 };
        getAcceleration(p3, v3, k3Vel);
        k3Pos[0] = v3[0]; k3Pos[1] = v3[1]; k3Pos[2] = v3[2];

        double[] p4 = { pos[0] + k4Pos[0]*STEP_SIZE, pos[1] + k4Pos[1]*STEP_SIZE, pos[2] + k4Pos[2]*STEP_SIZE };
        double[] v4 = { vel[0] + k3Vel[0]*STEP_SIZE, vel[1] + k3Vel[1]*STEP_SIZE, vel[2] + k3Vel[2]*STEP_SIZE };
        getAcceleration(p4, v4, k4Vel);
        k4Pos[0] = v4[0]; k4Pos[1] = v4[1]; k4Pos[2] = v4[2];
        
        for (int i = 0; i < 3; i++) {
            pos[i] += (STEP_SIZE / 6.0) * (k1Pos[i] + 2.0*k2Pos[i] + 2.0*k3Pos[i] + k4Pos[i]);
            vel[i] += (STEP_SIZE / 6.0) * (k1Vel[i] + 2.0*k2Vel[i] + 2.0*k3Vel[i] + k4Vel[i]);
        }
    }

    private void getAcceleration(double[] pos, double[] vel, double[] accOut) {
        double r2 = pos[0] * pos[0] + pos[1] * pos[1] + pos[2] * pos[2];
        double r = Math.sqrt(r2);
        double r5 = r2 * r2 * r;
        double hX = pos[1] * vel[2] - pos[2] * vel[1];
        double hY = pos[2] * vel[0] - pos[0] * vel[2];
        double hZ = pos[0] * vel[1] - pos[1] * vel[0];
        double h2 = hX * hX + hY * hY + hZ * hZ;

        // Schwarzschild metric Einstein Bending factor (-1.5 * Rs * h^2 / r^5)
        double bendingFactor = -1.5 * HORIZON_RADIUS * h2 / r5;
        accOut[0] = bendingFactor * pos[0];
        accOut[1] = bendingFactor * pos[1];
        accOut[2] = bendingFactor * pos[2];
    }

    private void normalize(double[] src, double[] dest) {
        double len = Math.sqrt(src[0] * src[0] + src[1] * src[1] + src[2] * src[2]);
        dest[0] = src[0] / (len == 0 ? 1 : len);
        dest[1] = src[1] / (len == 0 ? 1 : len);
        dest[2] = src[2] / (len == 0 ? 1 : len);
    }

    private void crossProduct(double[] v1, double[] v2, double[] dest) {
        dest[0] = v1[1] * v2[2] - v1[2] * v2[1];
        dest[1] = v1[2] * v2[0] - v1[0] * v2[2];
        dest[2] = v1[0] * v2[1] - v1[1] * v2[0];
    }
}