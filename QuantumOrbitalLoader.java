public class QuantumOrbitalLoader extends Loader {

    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Solving Radial Laguerre Polynomials:"),
        new StatusStage(55, "Calculating Legendre Spherical Harmonics:"),
        new StatusStage(80, "Projecting 3D Electron Density Cloud:"),
        new StatusStage(100, "Atomic Orbital Superposition Stable!")
    };

    private static final int TOTAL_W = 80;
    private static final int HEIGHT = 22;

    private double time = 0.0;
    private double cameraAngle = 0.0;
    private static final double TIME_STEP = 0.04;
    private static final double ROTATION_SPEED = 0.02;

    private long lastStateTransitionTime = 0;
    private static final long STATE_DURATION_MS = 1400; // Extra time to appreciate the geometry

    private int q_n = 1; 
    private int q_l = 0; 
    private int q_m = 0; 

    // High resolution grid to cleanly parse the nodal splits
    private static final int GRID_RES = 30;
    private static final int HALF_RES = GRID_RES / 2;

    public QuantumOrbitalLoader() {
        super(STAGES, TOTAL_W, HEIGHT);
    }

    @Override
    protected void initialize() {
        time = 0.0;
        cameraAngle = 0.0;
        q_n = 1;
        q_l = 0;
        q_m = 0;
        lastStateTransitionTime = System.currentTimeMillis();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int maxIndex = TOTAL_W * HEIGHT;
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastStateTransitionTime >= STATE_DURATION_MS) {
            lastStateTransitionTime = currentTime;
            q_l++;
            if (q_l >= q_n) {
                q_n++;
                q_l = 0;
            }
            if (q_n > 4) {
                q_n = 1; q_l = 0; q_m = 0;
            } else {
                q_m = q_l; // Keep magnetic state balanced to highlight clean lobe alignment
            }
        }

        // Wipe frame clean
        for (int i = 0; i < maxIndex; i++) {
            outputBuffer[i] = " ";
            zBuffer[i] = -99999.0; 
        }

        time += TIME_STEP;
        cameraAngle += ROTATION_SPEED;

        double cosYaw = Math.cos(cameraAngle);
        double sinYaw = Math.sin(cameraAngle);
        double cosPitch = Math.cos(0.45); 
        double sinPitch = Math.sin(0.45);

        // COMPUTE BOUNDS FIX: Define accurate boundary radii for each specific principal shell
        double viewRadius = 5.0;
        if (q_n == 1) viewRadius = 4.5;
        else if (q_n == 2) viewRadius = 14.0;
        else if (q_n == 3) viewRadius = 28.0;
        else if (q_n == 4) viewRadius = 48.0;

        double spatialStep = viewRadius / HALF_RES;

        // Dynamic Pre-Pass to establish strict state peak boundaries
        double maxObservedDensity = 0.0001; 
        for (int x = -HALF_RES; x <= HALF_RES; x++) { 
            for (int y = -HALF_RES; y <= HALF_RES; y++) {
                for (int z = -HALF_RES; z <= HALF_RES; z++) {
                    double rx = x * spatialStep;
                    double ry = y * spatialStep;
                    double rz = z * spatialStep;
                    double r = Math.sqrt(rx * rx + ry * ry + rz * rz);
                    if (r < 0.1 || r > viewRadius) continue;

                    double theta = Math.acos(rz / r);
                    double phi = Math.atan2(ry, rx);

                    double radial = calculateRadialPart(r, q_n, q_l);
                    double angular = calculateAngularPart(theta, phi, q_l, q_m);
                    double density = (radial * angular) * (radial * angular);

                    if (density > maxObservedDensity) {
                        maxObservedDensity = density;
                    }
                }
            }
        }

        // 2. MAIN GEOMETRIC PROJECTION PASS
        for (int x = -HALF_RES; x <= HALF_RES; x++) {
            for (int y = -HALF_RES; y <= HALF_RES; y++) {
                for (int z = -HALF_RES; z <= HALF_RES; z++) {

                    double rx = x * spatialStep;
                    double ry = y * spatialStep;
                    double rz = z * spatialStep;

                    double r = Math.sqrt(rx * rx + ry * ry + rz * rz);
                    if (r < 0.1 || r > viewRadius) continue; 

                    double theta = Math.acos(rz / r);          
                    double phi = Math.atan2(ry, rx);           

                    double radialWave = calculateRadialPart(r, q_n, q_l);
                    double angularWave = calculateAngularPart(theta, phi, q_l, q_m);

                    // Compute complex wavefunction components with authentic sign properties
                    double psiRe = radialWave * angularWave * Math.cos(time);
                    double psiIm = radialWave * angularWave * Math.sin(time);

                    double probDensity = (psiRe * psiRe) + (psiIm * psiIm);
                    double relativeDensity = probDensity / maxObservedDensity;

                    // STRICT FILTER: Raised cutoff fence to aggressively eliminate fuzzy static fields
                    if (relativeDensity < 0.22) continue;

                    // CAMERA ZOOM FIX: Squeezed constants down heavily to force a massive zoom out perspective
                    double cameraZoomX = 3.8;
                    double cameraZoomY = 1.9;

                    double rotX = rx * cosYaw - rz * sinYaw;
                    double rotZ = rx * sinYaw + rz * cosYaw;
                    double rotY = ry * cosPitch - rotZ * sinPitch;
                    double depth = ry * sinPitch + rotZ * cosPitch;

                    // Render points cleanly framed center-screen without edge collisions
                    int screenX = (TOTAL_W / 2) + (int) ((rotX / viewRadius) * TOTAL_W * 0.42 * cameraZoomX);
                    int screenY = (HEIGHT / 2) + (int) ((rotY / viewRadius) * HEIGHT * 0.45 * cameraZoomY);

                    if (screenX >= 0 && screenX < TOTAL_W && screenY >= 1 && screenY < HEIGHT) {
                        int idx = screenY * TOTAL_W + screenX;

                        if (depth > zBuffer[idx]) {
                            zBuffer[idx] = depth;

                            // Map phase angles to exact 256 color gradients
                            double currentPhase = Math.atan2(psiIm, psiRe);
                            int colorCode = 16 + (int) (((currentPhase + Math.PI) / (2.0 * Math.PI)) * 215.0);

                            char cloudChar = (relativeDensity > 0.75) ? '█' : (relativeDensity > 0.45) ? '▓' : '▒';
                            outputBuffer[idx] = "\u001B[38;5;" + colorCode + "m" + cloudChar + RESET;
                        }
                    }
                }
            }
        }

        // Draw HUD tracking bar
        char subshellLabel = (q_l == 0) ? 's' : (q_l == 1) ? 'p' : (q_l == 2) ? 'd' : 'f';
        String telemetryString = String.format(" HYDROGEN ORBITAL SPECTROMETER  │  ACTIVE EIGENSTATE: %d%c (m=%d) ", q_n, subshellLabel, q_m);
        for (int i = 0; i < telemetryString.length(); i++) {
            if (i < TOTAL_W) {
                outputBuffer[i] = "\u001B[38;5;246m" + telemetryString.charAt(i) + RESET;
            }
        }
    }

    private double calculateRadialPart(double r, int n, int l) {
        if (n == 1) return Math.exp(-r);
        if (n == 2) return (l == 0) ? (2.0 - r) * Math.exp(-r / 2.0) : r * Math.exp(-r / 2.0);
        if (n == 3) {
            if (l == 0) return (27.0 - 18.0 * r + 2.0 * r * r) * Math.exp(-r / 3.0);
            if (l == 1) return r * (6.0 - r) * Math.exp(-r / 3.0);
            return r * r * Math.exp(-r / 3.0);
        }
        if (l == 0) return (96.0 - 72.0 * r + 12.0 * r * r - r * r * r) * Math.exp(-r / 4.0);
        if (l == 1) return r * (32.0 - 8.0 * r + r * r) * Math.exp(-r / 4.0);
        if (l == 2) return r * r * (12.0 - r) * Math.exp(-r / 4.0);
        return r * r * r * Math.exp(-r / 4.0);
    }

    // RIGOROUS GEOMETRY FIX: Added true sign phase splits to Legendre Polynomial functions
    private double calculateAngularPart(double theta, double phi, int l, int m) {
        if (l == 0) return 1.0;
        
        // p-orbitals: Form distinct dual opposing dumbbells separated by clear node voids
        if (l == 1) {
            if (m == 0) return Math.cos(theta); // p_z Lobe orientation
            return Math.sin(theta) * Math.cos(phi); // p_x Lobe orientation
        }
        
        // d-orbitals: Form crisp multi-quadrant cloverleaves matching real chemical notation
        if (l == 2) {
            if (m == 0) return 3.0 * Math.cos(theta) * Math.cos(theta) - 1.0; // d_z² Doughnut shape
            if (m == 1) return Math.sin(theta) * Math.cos(theta) * Math.cos(phi); 
            return Math.sin(theta) * Math.sin(theta) * Math.cos(2.0 * phi); // d_x²-y² Lobe alignment
        }
        
        // f-orbitals: Form authentic 8-lobed octant configurations
        if (m == 0) return 5.0 * Math.pow(Math.cos(theta), 3) - 3.0 * Math.cos(theta);
        if (m == 1) return Math.sin(theta) * (5.0 * Math.cos(theta) * Math.cos(theta) - 1.0) * Math.cos(phi);
        if (m == 2) return Math.sin(theta) * Math.sin(theta) * Math.cos(theta) * Math.cos(2.0 * phi);
        return Math.sin(theta) * Math.sin(theta) * Math.sin(theta) * Math.cos(3.0 * phi);
    }
}