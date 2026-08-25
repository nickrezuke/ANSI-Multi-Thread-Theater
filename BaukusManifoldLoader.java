import java.util.Arrays;

public class BaukusManifoldLoader extends Loader {
    private static final StatusStage[] MANIFOLD_STAGES = {
        new StatusStage(15, "Parsing Baukus matrix vectors..."),
        new StatusStage(40, "Calculating local manifold gradient:"),
        new StatusStage(65, "Compressing dimensional instability lines:"),
        new StatusStage(85, "Eigen-space threshold reached..."),
        new StatusStage(100, "Manifold Unstable! Containment Failing!")
    };

    private double rotX = 0;        
    private double rotY = 0;        
    private double timeStep = 0;    
    private static final String RESET = "\u001B[0m";

    public BaukusManifoldLoader() {
        super(MANIFOLD_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // Dynamic calculations executed in real-time
    }

    @Override
protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
    Arrays.fill(outputBuffer, " ");
    Arrays.fill(zBuffer, -Double.MAX_VALUE);

    double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
    double cosY = Math.cos(rotY), sinY = Math.sin(rotY);

    // Iterating through spherical coordinates to form a closed sphere
    for (int thetaIndex = 0; thetaIndex < 250; thetaIndex++) {
        // Theta goes from 0 to PI (latitude)
        double theta = (thetaIndex / 250.0) * Math.PI; 
        
        for (int phiIndex = 0; phiIndex < 375; phiIndex++) {
            // Phi goes from 0 to 2PI (longitude)
            double phi = (phiIndex / 375.0) * 2.0 * Math.PI; 

            // --- THE BAUKUS MANIFOLD INSTABILITY EQUATION ---
            // Base sphere radius
            double baseRadius = 1.2;
            
            // Interlaced sine waves create the moving, localized "bumps" on the surface
            double baseWave = Math.sin(theta * 5.0 + timeStep) * Math.cos(phi * 4.0 - timeStep * 1.5);
            double subHarmonic = 0.25 * Math.sin(phi * 8.0 + timeStep * 3.0) * Math.sin(theta * 3.0);
            
            // Total radial distortion (the dynamic bumps)
            double distortedRadius = baseRadius + (baseWave + subHarmonic) * 0.4;

            // Convert Spherical Coordinates to 3D Cartesian space
            double x3d = distortedRadius * Math.sin(theta) * Math.cos(phi);
            double y3d = distortedRadius * Math.sin(theta) * Math.sin(phi);
            double z3d = distortedRadius * Math.cos(theta);

            // --- 3D ROTATION TRANSFORM (Pitch and Yaw) ---
            // Rotate around Y-axis
            double r1x = x3d * cosY - z3d * sinY;
            double r1y = y3d;
            double r1z = x3d * sinY + z3d * cosY;

            // Rotate around X-axis
            double rot3dX = r1x;
            double rot3dY = r1y * cosX - r1z * sinX;
            double rot3dZ = r1y * sinX + r1z * cosX;

            // --- PERSPECTIVE PROJECTION ---
            double cameraDepth = rot3dZ + 3.0;
            if (cameraDepth <= 0.2) continue;

            double invDepth = 1.0 / cameraDepth;

            // Project down to terminal screen matrix (adjusting for non-square console characters)
            int screenX = (int) (40 + 55 * invDepth * rot3dX); 
            int screenY = (int) (11 - 26 * invDepth * rot3dY);
            int bufferIndex = screenX + 80 * screenY;

            if (screenY >= 0 && screenY < 22 && screenX >= 0 && screenX < 80) {
                if (invDepth > zBuffer[bufferIndex]) {
                    zBuffer[bufferIndex] = invDepth;

                    // Pseudo-shading calculated dynamically by surface depth/normals
                    double intensity = (rot3dZ + 1.5) / 3.0;
                    int charIndex = (int) (intensity * 10);
                    charIndex = Math.max(0, Math.min(10, charIndex));
                    
                    String dataGridRamp = ".-:=*#%@▒▓█";
                    char dataGlyph = dataGridRamp.charAt(charIndex);

                    // Reactor fluid green/cyan shifting warning patterns
                    double fluxPhase = timeStep * 3.0 + (theta * 4.0) + phi;
                    int r = (int) (100 + 50 * Math.sin(fluxPhase));
                    int g = (int) (200 + 55 * Math.sin(fluxPhase + Math.PI / 4.0));
                    int b = (int) (160 + 95 * Math.sin(fluxPhase + Math.PI / 2.0));

                    String instabilityColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                    outputBuffer[bufferIndex] = instabilityColor + dataGlyph + RESET;
                }
            }
        }
    }

    // Mutation variables over time
    rotX += 0.010;
    rotY += 0.015;
    timeStep += 0.05;
}

}
