public class WireframeSphereLoader extends Loader { 
    private static final StatusStage[] STAGES = { 
        new StatusStage(30, "Calibrating uniform angular mesh:"), 
        new StatusStage(60, "Balancing polar coordinate density:"), 
        new StatusStage(90, "Projecting seamless wireframe:"), 
        new StatusStage(100, "Uniform Grid Operational!") 
    };

    private static final char CH_INTERSECTION = '\u2588'; // █ Solid Block
    private static final char CH_LINE         = '\u00B7'; // · Faint Vector Dot
    
    private double sphereRotationX = 0.0;
    private double sphereRotationY = 0.0;
    
    private static final int COLOR_R = 0;
    private static final int COLOR_G = 255;
    private static final int COLOR_B = 150;

    public WireframeSphereLoader() { 
        // This uses 80x22 specifically
        super(STAGES, 80, 22); 
    }

    @Override 
    protected void initialize() { 
    }

    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        double cameraDistance = 2.2; 
        double sphereRadius = 1.0; 

        double cosX = Math.cos(sphereRotationX);
        double sinX = Math.sin(sphereRotationX);
        double cosY = Math.cos(sphereRotationY);
        double sinY = Math.sin(sphereRotationY);

        // Keep loop steps fine so lines look completely unbroken
        for (double theta = 0; theta < Math.PI; theta += 0.015) { 
            double sinTheta = Math.sin(theta); 
            double cosTheta = Math.cos(theta); 

            // 1. Latitude Grid Check (Every 18 degrees)
            double latStep = Math.PI / 10.0; 
            boolean isLatitude = Math.abs((theta % latStep)) < 0.02 || Math.abs((theta % latStep) - latStep) < 0.02;

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.015) { 
                double sinPhi = Math.sin(phi); 
                double cosPhi = Math.cos(phi); 

                // 2. Longitude Grid Check (Every 18 degrees)
                double lonStep = Math.PI / 10.0;
                boolean isLongitude = Math.abs((phi % lonStep)) < 0.02 || Math.abs((phi % lonStep) - lonStep) < 0.02;

                // POLAR NORMALIZATION TRICK:
                // As we get closer to the poles (sinTheta approaches 0), longitude lines bunch up.
                // We skip rendering redundant longitude lines near poles to maintain visual balance.
                if (isLongitude && sinTheta < 0.4) {
                    // Only draw every 2nd or 3rd longitude line near the top/bottom poles
                    double tightLonStep = Math.PI / 2.0; 
                    isLongitude = Math.abs((phi % tightLonStep)) < 0.02 || Math.abs((phi % tightLonStep) - tightLonStep) < 0.02;
                }

                // If it's not a grid line, skip it
                if (!isLatitude && !isLongitude) {
                    continue;
                }

                // Raw 3D Geometry
                double x = sphereRadius * sinTheta * cosPhi; 
                double y = sphereRadius * sinTheta * sinPhi; 
                double z = sphereRadius * cosTheta; 

                // Dual-Axis 3D Rotation
                double x1 = x;
                double y1 = y * cosX - z * sinX;
                double z1 = y * sinX + z * cosX;

                double rx = x1 * cosY + z1 * sinY;
                double ry = y1;
                double rz = -x1 * sinY + z1 * cosY;

                // Perspective projection math
                double ooz = 1.0 / (rz + cameraDistance); 
                int xp = (int) (40 + 110 * ooz * rx); 
                int yp = (int) (11 + 52 * ooz * ry);  

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) { 
                    int index = xp + 80 * yp; 

                    char renderChar = (isLatitude && isLongitude) ? CH_INTERSECTION : CH_LINE;

                    String colorCode = String.format("\u001B[38;2;%d;%d;%dm", COLOR_R, COLOR_G, COLOR_B); 
                    outputBuffer[index] = colorCode + renderChar + RESET; 
                } 
            } 
        } 

        sphereRotationX += 0.011; 
        sphereRotationY += 0.019; 
    } 
}
