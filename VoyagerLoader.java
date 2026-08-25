import java.util.Arrays;

public class VoyagerLoader extends Loader {

    private static final StatusStage[] VOYAGER_STAGES = {
            new StatusStage(10, "Leaving the heliosphere..."),
            new StatusStage(30, "Crossing the termination shock boundary..."),
            new StatusStage(50, "Entering interstellar medium..."),
            new StatusStage(75, "Transmitting plasma wave instrument data..."),
            new StatusStage(100, "Signal faint. Continuing into the great dark.")
    };

    private double angleX = 0.6;
    private double angleY = -0.3;
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Deep Space Voyager Color Palette
    private static final String C_DISH   = "\u001B[38;2;230;235;240m"; // High-gain antenna white/grey
    private static final String C_BUS    = "\u001B[38;2;140;145;155m"; // Metallic central equipment bus
    private static final String C_STRUT  = "\u001B[38;2;110;115;125m"; // Support booms and trusses
    private static final String C_RTG    = "\u001B[38;2;50;55;60m";    // Dark casing of the RTG power plant
    private static final String C_MAG    = "\u001B[38;2;160;160;170m"; // Astromast magnetometer boom
    private static final String C_GOLD   = "\u001B[38;2;255;200;40m";  // The Golden Record!
    private static final String C_SENSOR = "\u001B[38;2;180;190;200m"; // Science platform instruments
    private static final String RESET    = "\u001B[0m";

    public VoyagerLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public VoyagerLoader() {
        super(VOYAGER_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.5;
        this.angleY = -0.4;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;
        // Voyager is 3-axis stabilized, not spinning, so we give it a very slow, majestic pan
        angleY += 0.015; 
        angleX = 0.5 + 0.05 * Math.sin(frameTick * 0.01); 

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Sunlight is extremely distant and faint. Distant star directional lighting.
        double lightX = 0.8, lightY = -0.2, lightZ = 0.5;

        // 1. HIGH-GAIN ANTENNA (The Massive Forward Dish)
        // Pointing generally "up" along the Y axis
        renderDish(0.0, 0.2, 0.0, 1.1, 0.35, C_DISH, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        
        // Dish feed horn (central spike in the dish)
        renderLine(0.0, 0.2, 0.0, 0.0, 0.8, 0.0, C_STRUT, 0.0, 1.0, 0.0, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderDisc(0.0, 0.8, 0.0, 0.08, 0.0, 1.0, 0.0, C_BUS, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. CENTRAL BUS (Decagonal equipment module below the dish)
        renderBus(0.0, -0.4, 0.0, 0.45, 0.6, C_BUS, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. THE GOLDEN RECORD
        // Affixed to the outside of the bus (facing +Z direction)
        renderDisc(0.0, -0.3, 0.46, 0.15, 0.0, 0.0, 1.0, C_GOLD, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 4. RTG BOOM (Power Plant trailing off to the back-left)
        double rtgX = -1.2, rtgY = -0.9, rtgZ = -0.6;
        renderLine(0.0, -0.5, 0.0, rtgX, rtgY, rtgZ, C_STRUT, -1.0, -1.0, -1.0,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Render 3 stacked RTG cylinders at the end of the boom
        for (int i = 0; i < 3; i++) {
            double offset = i * 0.25;
            renderCylinder(rtgX - offset * 0.4, rtgY - offset * 0.4, rtgZ - offset * 0.4, 0.15, 0.3, C_RTG,
                    cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }

        // 5. SCIENCE PLATFORM BOOM (Extending forward-right)
        double sciX = 1.3, sciY = -0.1, sciZ = 0.5;
        renderLine(0.0, -0.4, 0.0, sciX, sciY, sciZ, C_STRUT, 1.0, 1.0, 1.0,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Cameras and cosmic ray sensors clustered at the end
        renderCylinder(sciX, sciY, sciZ, 0.12, 0.4, C_SENSOR,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderDisc(sciX + 0.1, sciY + 0.2, sciZ + 0.1, 0.15, 1.0, 1.0, 0.0, C_SENSOR,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 6. MAGNETOMETER BOOM (The extremely long, thin astromast trailing down and right)
        double magX = 2.5, magY = -1.5, magZ = -1.5;
        renderLine(0.0, -0.4, 0.0, magX, magY, magZ, C_MAG, 1.0, -1.0, -1.0,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
    }

    private void renderDish(double cx, double cy, double cz, double maxRadius, double depth, String color,
                            double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        for (double r = 0; r <= maxRadius; r += 0.05) {
            double yOffset = depth * (r * r) / (maxRadius * maxRadius);
            double y = cy + yOffset;
            
            int steps = (int)(40 * (r / maxRadius)) + 8;
            for (int step = 0; step < steps; step++) {
                double rad = step * (2.0 * Math.PI / steps);
                double nx = Math.cos(rad);
                double nz = Math.sin(rad);

                double px = cx + r * nx;
                double pz = cz + r * nz;

                // Parabolic normal vector
                double nnx = -nx * (yOffset / depth);
                double nnz = -nz * (yOffset / depth);
                double ny = 1.0;

                // Inside of the dish (+normal)
                projectPoint(px, y, pz, nnx, ny, nnz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                // Outside of the dish (-normal)
                projectPoint(px, y - 0.02, pz, -nnx, -ny, -nnz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderBus(double cx, double cy, double cz, double radius, double height, String color,
                           double cosX, double sinX, double cosY, double sinY,
                           double lx, double ly, double lz, String[] out, double[] zb) {
        // Voyager's bus is a decagon (10-sided). We approximate it with a dense 10-step cylinder.
        for (double y = cy - height / 2.0; y <= cy + height / 2.0; y += 0.06) {
            for (int step = 0; step < 10; step++) {
                double rad1 = step * (2.0 * Math.PI / 10.0);
                double rad2 = (step + 1) * (2.0 * Math.PI / 10.0);
                
                // Interpolate along the flat face of the decagon
                for (double t = 0; t <= 1.0; t += 0.15) {
                    double angle = rad1 * (1 - t) + rad2 * t;
                    double nx = Math.cos(angle);
                    double nz = Math.sin(angle);
                    
                    double px = cx + radius * nx;
                    double pz = cz + radius * nz;

                    projectPoint(px, y, pz, Math.cos(rad1), 0.0, Math.sin(rad1), color, 
                            cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                }
            }
        }
    }

    private void renderCylinder(double cx, double cy, double cz, double radius, double height, String color,
                                double cosX, double sinX, double cosY, double sinY,
                                double lx, double ly, double lz, String[] out, double[] zb) {
        for (double y = cy - height / 2.0; y <= cy + height / 2.0; y += 0.06) {
            for (int step = 0; step < 16; step++) {
                double rad = step * (2.0 * Math.PI / 16.0);
                double nx = Math.cos(rad);
                double nz = Math.sin(rad);
                projectPoint(cx + radius * nx, y, cz + radius * nz, nx, 0.0, nz, color, 
                        cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderDisc(double cx, double cy, double cz, double maxRadius, 
                            double nx, double ny, double nz, String color,
                            double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        // Generates a flat disc aligned with the specified normal
        
        // Find orthogonal vectors to the normal to construct the plane
        double uX = 1, uY = 0, uZ = 0;
        if (Math.abs(nx) > 0.9) {
            uX = 0; uY = 1;
        }
        // Cross product for U vector
        double crossX = ny * uZ - nz * uY;
        double crossY = nz * uX - nx * uZ;
        double crossZ = nx * uY - ny * uX;
        
        // Cross product for V vector
        double vX = ny * crossZ - nz * crossY;
        double vY = nz * crossX - nx * crossZ;
        double vZ = nx * crossY - ny * crossX;

        // Normalize
        double lenU = Math.hypot(crossX, Math.hypot(crossY, crossZ));
        crossX /= lenU; crossY /= lenU; crossZ /= lenU;

        double lenV = Math.hypot(vX, Math.hypot(vY, vZ));
        vX /= lenV; vY /= lenV; vZ /= lenV;

        for (double r = 0; r <= maxRadius; r += 0.03) {
            int steps = (int)(24 * (r / maxRadius)) + 1;
            for (int step = 0; step < steps; step++) {
                double rad = step * (2.0 * Math.PI / steps);
                
                double px = cx + r * Math.cos(rad) * crossX + r * Math.sin(rad) * vX;
                double py = cy + r * Math.cos(rad) * crossY + r * Math.sin(rad) * vY;
                double pz = cz + r * Math.cos(rad) * crossZ + r * Math.sin(rad) * vZ;

                projectPoint(px, py, pz, nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderLine(double x1, double y1, double z1, double x2, double y2, double z2, 
                            String color, double nx, double ny, double nz,
                            double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        
        double dist = Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2) + Math.pow(z2-z1, 2));
        int steps = (int) (dist * 20); // Density of the line

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double pz = z1 + (z2 - z1) * t;
            
            projectPoint(px, py, pz, nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                              double cosX, double sinX, double cosY, double sinY,
                              double lx, double ly, double lz, String[] out, double[] zb) {
        // 1. World Rotations (Yaw around Y, Pitch around X)
        double r1x = px * cosY - py * sinY;
        double r1y = px * sinY + py * cosY;
        double r1z = pz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // Rotate Normal Vectors for shading
        double n1x = nx * cosY - ny * sinY;
        double n1y = nx * sinY + ny * cosY;
        double n1z = nz;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen;
        }

        // 2. Camera Projection & Depth
        double cameraDepth = rotY + 3.8; 
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        // Map to 2D Terminal Space (Adjusted for ASCII character aspect ratio)
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotZ);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            // Z-Buffer Max-Sorting
            if (D > zb[idx]) {
                zb[idx] = D;

                // Lighting Exposure 
                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                
                double illuminance;
                if (colorCode.equals(C_GOLD)) {
                    // The Golden Record always shines brightly against the darkness of space
                    illuminance = 0.95; 
                } else {
                    // Baseline deep space ambient light is very low
                    illuminance = Math.max(0.12, dot);
                }

                // ASCII Luminance Ramp
                char[] ramp = {' ', '.', ':', '-', '=', '+', '*', '#', '%', '@', '█'};
                int rampIdx = (int) (illuminance * (ramp.length - 1));
                rampIdx = Math.max(0, Math.min(ramp.length - 1, rampIdx));
                char glyph = ramp[rampIdx];

                if (glyph != ' ') {
                    out[idx] = colorCode + glyph + RESET;
                }
            }
        }
    }
}