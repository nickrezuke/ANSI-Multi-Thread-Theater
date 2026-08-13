// TODO: Make that center color different?  Improve this whole thing

public class FisheyeGlobeLoader extends Loader {
    private static final StatusStage[] GLOBE_STAGES = {
        new StatusStage(20, "Over-inflating barrel lens parameters:"),
        new StatusStage(50, "Projecting spherical grid topology:"),
        new StatusStage(80, "Applying neon chroma wire attributes:"),
        new StatusStage(100, "Super-Fisheye Globe Active!")
    };

    private double yaw = 0;   // Y-axis rotation
    private double pitch = 0; // X-axis rotation

    // High-visibility neon wire colors
    private static final int[] C_FRONT_WIRE = { 0,   255, 200 }; // Bright Neon Cyan (Foreground)
    private static final int[] C_BACK_WIRE  = { 140,  30, 255 }; // Deep Electric Purple (Background)

    public FisheyeGlobeLoader() {
        super(GLOBE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.yaw = 0.0;
        this.pitch = 0.4; // Tilted slightly to show off both poles simultaneously
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Continuous rotation updates
        yaw += 0.025;
        pitch += 0.015;

        double sinY = Math.sin(yaw),   cosY = Math.cos(yaw);
        double sinP = Math.sin(pitch), cosP = Math.cos(pitch);

        double sphereRadius = 1.3;

        // Step 1: Sample across the sphere's surface geometry (Latitude/Longitude coordinates)
        // Using dense iterations to guarantee a fluid, unbroken ASCII line drawing
        for (int latStep = 0; latStep <= 60; latStep++) {
            double theta = latStep * (Math.PI / 60.0); // 0 to Pi (North Pole to South Pole)
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int lonStep = 0; lonStep < 160; lonStep++) {
                double phi = lonStep * (2.0 * Math.PI / 160.0); // 0 to 2*Pi
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                // Base 3D Euclidean Sphere Coordinates
                double lx = sphereRadius * sinTheta * cosPhi;
                double ly = sphereRadius * sinTheta * sinPhi;
                double lz = sphereRadius * cosTheta;

                // Step 2: Apply 3D rotation transforms
                // Yaw transform (Y-axis)
                double rx1 = lx * cosY - lz * sinY;
                double ry1 = ly;
                double rz1 = lx * sinY + lz * cosY;

                // Pitch transform (X-axis)
                double rx = rx1;
                double ry = ry1 * cosP - rz1 * sinP;
                double rz = ry1 * sinP + rz1 * cosP;

                // Translate sphere right in front of the lens path
                double finalZ = rz + 2.4;
                if (finalZ <= 0.1) continue;

                // Depth value
                double D = 1.0 / finalZ;

                // MAXED OUT FISHEYE: Exponent parameter set to 3.25 for dramatic barrel distortion
                double extremeFisheye = Math.pow(D, 3.25);

                // Map the 3D transformed points into the 80x22 console canvas boundaries
                int x = (int) (40 + 330 * extremeFisheye * rx);
                int y = (int) (11 + 160 * extremeFisheye * ry);
                int o = x + 80 * y;

                if (y >= 0 && y < 22 && x >= 0 && x < 80 && D > (zBuffer[o] + 0.0001)) {
                    
                    // Step 3: Wireframe Grid Masking
                    // Isolate rings at specific coordinate lines (like an armillary globe cage)
                    double latDegrees = Math.toDegrees(theta);
                    double lonDegrees = Math.toDegrees(phi);

                    // Drop lines every 30 degrees of latitude and longitude
                    boolean isLatitudeLine = Math.abs(latDegrees % 30.0) < 1.8;
                    boolean isLongitudeLine = Math.abs(lonDegrees % 30.0) < 1.8;

                    if (isLatitudeLine || isLongitudeLine) {
                        zBuffer[o] = D;

                        // Separate colors for the front and back wires to avoid visual flattening
                        boolean isForeground = (rz < 0); 
                        int[] baseColor = isForeground ? C_FRONT_WIRE : C_BACK_WIRE;
                        char renderChar = isForeground ? '@' : '.';

                        // Soft light falloff into the edges to create a glassy finish
                        double edgeDimming = Math.min(1.0, Math.max(0.2, D * 1.8));
                        int r = (int) (baseColor[0] * edgeDimming);
                        int g = (int) (baseColor[1] * edgeDimming);
                        int b = (int) (baseColor[2] * edgeDimming);

                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        b = Math.max(0, Math.min(255, b));

                        String ansiColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                        outputBuffer[o] = ansiColor + renderChar + RESET;
                    }
                }
            }
        }
    }
}
