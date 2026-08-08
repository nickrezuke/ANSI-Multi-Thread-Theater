public class KleinRingLoader extends InteractiveLoader {
    private static final StatusStage[] RING_STAGES = {
        new StatusStage(100, "[Control w/ Arrow Keys!]")
    };

    // Continuous 3D accumulation matrix (1D flattened array tracking indices 0-8)
    private final double[] matrix = {
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0
    };

    // Screen-space velocity tracking variables
    private volatile double velCamX = 0.0; // Viewport Up/Down tilt velocity
    private volatile double velCamY = 0.0; // Viewport Left/Right spin velocity

    private static final double ACCELERATION = 0.04;
    private static final double FRICTION = 0.92;

    public KleinRingLoader() {
        // This uses 80x22 specifically
        super(RING_STAGES, 80, 22);
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        switch (keyCode) {
            case 'A': // Arrow UP
                velCamX -= ACCELERATION;
                break;
            case 'B': // Arrow DOWN
                velCamX += ACCELERATION;
                break;
            case 'C': // Arrow RIGHT
                velCamY += ACCELERATION;
                break;
            case 'D': // Arrow LEFT
                velCamY -= ACCELERATION;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // 1. Capture momentum shifts and apply decay friction values
        double deltaX = velCamX; 
        double deltaY = velCamY;
        
        velCamX *= FRICTION;
        velCamY *= FRICTION;

        // 2. Compute exact screen-space incremental rotation components
        double cosX = Math.cos(deltaX), sinX = Math.sin(deltaX);
        double cosY = Math.cos(deltaY), sinY = Math.sin(deltaY);

        // Explicit fully expanded matrix multiplication components
        double s00 = cosY;     double s01 = -sinY;    double s02 = 0.0;
        double s10 = cosX*sinY; double s11 = cosX*cosY; double s12 = -sinX;
        double s20 = sinX*sinY; double s21 = sinX*cosY; double s22 = cosX;

        // 3. Multiply from the LEFT side (matrix = step * matrix) for camera-space stacking
        double r00 = s00 * matrix[0] + s01 * matrix[3] + s02 * matrix[6];
        double r01 = s00 * matrix[1] + s01 * matrix[4] + s02 * matrix[7];
        double r02 = s00 * matrix[2] + s01 * matrix[5] + s02 * matrix[8];

        double r10 = s10 * matrix[0] + s11 * matrix[3] + s12 * matrix[6];
        double r11 = s10 * matrix[1] + s11 * matrix[4] + s12 * matrix[7];
        double r12 = s10 * matrix[2] + s11 * matrix[5] + s12 * matrix[8];

        double r20 = s20 * matrix[0] + s21 * matrix[3] + s22 * matrix[6];
        double r21 = s20 * matrix[1] + s21 * matrix[4] + s22 * matrix[7];
        double r22 = s20 * matrix[2] + s21 * matrix[5] + s22 * matrix[8];

        matrix[0] = r00; matrix[1] = r01; matrix[2] = r02;
        matrix[3] = r10; matrix[4] = r11; matrix[5] = r12;
        matrix[6] = r20; matrix[7] = r21; matrix[8] = r22;

        // 4. Render the 3D Klein Bottle Geometry parametric equation matrix grid
        // Evaluates two orthogonal geometric loop spaces (u = handle sweep, v = pipe profile)
        for (int uIndex = 0; uIndex < 120; uIndex++) {
            double u = uIndex * (Math.PI / 60.0); // Ranges from 0 to PI
            for (int vIndex = 0; vIndex < 120; vIndex++) {
                double v = vIndex * (Math.PI / 60.0); // Ranges from 0 to 2*PI

                // Standard parametric coordinates for a Figure-8 Klein Bottle formulation
                double localX = (2.0 + Math.cos(u / 2.0) * Math.sin(v) - Math.sin(u / 2.0) * Math.sin(2.0 * v)) * Math.cos(u);
                double localZ = (2.0 + Math.cos(u / 2.0) * Math.sin(v) - Math.sin(u / 2.0) * Math.sin(2.0 * v)) * Math.sin(u);
                double localY = Math.sin(u / 2.0) * Math.sin(v) + Math.cos(u / 2.0) * Math.sin(2.0 * v);

                // Scale down slightly to fit the terminal bounds
                localX *= 0.85;
                localY *= 0.85;
                localZ *= 0.85;

                // 5. Matrix vector multiplication step
                double rotX = matrix[0]*localX + matrix[1]*localY + matrix[2]*localZ;
                double rotY = matrix[3]*localX + matrix[4]*localY + matrix[5]*localZ;
                double rotZ = matrix[6]*localX + matrix[7]*localY + matrix[8]*localZ;

                // 6. PERFECT BALANCED PROJECTION HOOK
                // Instead of offseting raw screen characters, we offset depth inside 3D camera coordinates (rotY + 5.5).
                // This eliminates the wobble completely because perspective scales away uniformly from the center.
                double cameraDepth = rotY + 5.5; 
                double D = 1.0 / cameraDepth;

                // Project points using an adjusted lens magnification factor
                int x = (int) (40 + 52 * D * rotX);
                // Flip Z axis scaling so that "Up" on the keyboard tilts the bottle forward seamlessly
                int y = (int) (11 - 24 * D * rotZ); 
                int o = x + 80 * y;

                // Pseudo-shading vector calculation based on surface coordinate density
                double N_double = 10.0 * (Math.abs(rotX) * 0.4 + Math.abs(rotZ) * 0.6 + (D * 2.0));

                if (22 > y && y >= 0 && x >= 0 && 80 > x && D > (zBuffer[o] + 0.0001)) {
                    zBuffer[o] = D;
                    int charIndex = (int) Math.round(N_double);
                    if (charIndex < 0) charIndex = 0;
                    String lString = ".,-~:;=!*#$@";
                    char asciiChar = lString.charAt(charIndex >= lString.length() ? lString.length() - 1 : charIndex);

                    // 24-Bit TrueColor RGB Rainbow Palette Map
                    // Using pure 3D local coordinates to anchor colors to the shape itself,
                    // making colors twist across its geometric surface as it spins!
                    int r = (int) (128 + 127 * Math.sin(localX * 1.2));
                    int g = (int) (128 + 127 * Math.sin(localY * 1.2 + 2.0 * Math.PI / 3.0));
                    int b = (int) (128 + 127 * Math.sin(localZ * 1.2 + 4.0 * Math.PI / 3.0));

                    String rainbowColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                    outputBuffer[o] = rainbowColor + asciiChar + RESET;
                }
            }
        }
    }
}
