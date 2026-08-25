import java.util.Arrays;

public class InternationalSpaceStationLoader extends Loader {

    private static final StatusStage[] ISS_STAGES = {
            new StatusStage(15, "Phasing orbital trajectory for approach..."),
            new StatusStage(35, "Aligning with Integrated Truss Structure..."),
            new StatusStage(55, "Establishing Ku-band telemetry link..."),
            new StatusStage(75, "Clearance granted for Destiny module docking..."),
            new StatusStage(100, "Capture confirmed. Welcome to the ISS.")
    };

    private double angleX = 0.3;
    private double angleY = 0.0;
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Authentic ISS Color Palette
    private static final String C_TRUSS      = "\u001B[38;2;160;165;170m"; // Metallic grey truss backbone
    private static final String C_MODULE     = "\u001B[38;2;240;245;250m"; // White pressurized modules
    private static final String C_SOLAR_GOLD = "\u001B[38;2;210;140;40m";  // Gold/Bronze Kapton solar wings
    private static final String C_SOLAR_GRID = "\u001B[38;2;60;50;40m";    // Dark structural grid on panels
    private static final String C_RADIATOR   = "\u001B[38;2;255;255;255m"; // Pure white thermal radiators
    private static final String C_CUPOLA     = "\u001B[38;2;100;180;255m"; // Blue cupola observation windows
    private static final String C_RED_NAV    = "\u001B[38;2;255;50;50m";   // Port navigation light
    private static final String C_GREEN_NAV  = "\u001B[38;2;50;255;100m";  // Starboard navigation light
    private static final String RESET        = "\u001B[0m";

    public InternationalSpaceStationLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public InternationalSpaceStationLoader() {
        super(ISS_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.2;
        this.angleY = -0.5;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear frame output buffer and reset z-buffer
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;
        angleY += 0.015; // Slow orbital sweep around the station
        angleX = 0.25 + 0.1 * Math.sin(frameTick * 0.02); // Gentle microgravity wobble

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Sunlight direction (Top-Right-Front)
        double lightX = 0.577, lightY = -0.577, lightZ = 0.577;

        // 1. MAIN INTEGRATED TRUSS (X-Axis Backbone)
        for (double x = -3.2; x <= 3.2; x += 0.08) {
            for (int step = 0; step < 6; step++) { // Hexagonal truss structure
                double rad = step * (Math.PI / 3.0);
                double ny = Math.cos(rad);
                double nz = Math.sin(rad);
                projectPoint(x, ny * 0.12, nz * 0.12, 0, ny, nz, C_TRUSS, 
                        cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // 2. PRESSURIZED MODULES (Y-Axis Core: Destiny, Unity, Zvezda)
        for (double y = -1.5; y <= 1.5; y += 0.08) {
            for (int step = 0; step < 16; step++) {
                double rad = step * (Math.PI / 8.0);
                double nx = Math.cos(rad);
                double nz = Math.sin(rad);
                // Taper the ends to simulate docking nodes
                double r = (Math.abs(y) > 1.3) ? 0.22 : 0.32;
                projectPoint(nx * r, y, nz * r, nx, 0, nz, C_MODULE, 
                        cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // 3. CROSS MODULES (X-Axis: Kibo, Columbus labs attached to nodes)
        for (double x = -0.7; x <= 0.7; x += 0.08) {
            if (Math.abs(x) < 0.2) continue; // Skip intersecting the central hub
            for (int step = 0; step < 16; step++) {
                double rad = step * (Math.PI / 8.0);
                double ny = Math.cos(rad);
                double nz = Math.sin(rad);
                projectPoint(x, 0.4 + ny * 0.25, nz * 0.25, 0, ny, nz, C_MODULE, 
                        cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // 4. THE CUPOLA (Observation deck facing Earth / -Z Axis)
        for (double z = -0.32; z >= -0.45; z -= 0.05) {
            double r = (z <= -0.4) ? 0.12 : 0.18; // Angled bevel
            for (int step = 0; step < 12; step++) {
                double rad = step * (Math.PI / 6.0);
                double nx = Math.cos(rad);
                double ny = Math.sin(rad);
                // Alternate glass panels and hull struts
                String color = (step % 2 == 0 && z < -0.32) ? C_CUPOLA : C_MODULE;
                projectPoint(nx * r, ny * r, z, nx, ny, -1.0, color, 
                        cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // 5. THERMAL RADIATORS (Trailing downwards/backwards from truss)
        for (double x : new double[]{-0.8, 0.8}) { // Left and right cooling arrays
            for (double y = 0.2; y <= 1.4; y += 0.05) {
                for (double dx = -0.15; dx <= 0.15; dx += 0.05) {
                    // Face up (+Z normal)
                    projectPoint(x + dx, y, 0.25, 0, 0, 1.0, C_RADIATOR, 
                            cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
                    // Face down (-Z normal)
                    projectPoint(x + dx, y, 0.24, 0, 0, -1.0, C_RADIATOR, 
                            cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
                }
            }
        }

        // 6. MAIN SOLAR ARRAYS (Four Massive Wings on the Truss Ends)
        double[] arrayXs = {-2.8, -2.1, 2.1, 2.8};
        for (double ax : arrayXs) {
            // Upper and lower arrays per wing segment
            renderSolarWing(ax, 0.3, 2.2, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            renderSolarWing(ax, -2.2, -0.3, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }

        // 7. EXTREMITY NAVIGATION LIGHTS
        if (frameTick % 20 < 10) {
            // Port (Red)
            projectPoint(-3.4, 0, 0, -1, 0, 0, C_RED_NAV, 
                    cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            // Starboard (Green)
            projectPoint(3.4, 0, 0, 1, 0, 0, C_GREEN_NAV, 
                    cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }
    }

    private void renderSolarWing(double xCenter, double zStart, double zEnd,
                                 double cosX, double sinX, double cosY, double sinY,
                                 double lx, double ly, double lz, String[] out, double[] zb) {
        double zMin = Math.min(zStart, zEnd);
        double zMax = Math.max(zStart, zEnd);

        for (double z = zMin; z <= zMax; z += 0.06) {
            for (double y = -0.6; y <= 0.6; y += 0.05) {
                // Procedural grid pattern generation for individual solar cells
                boolean isGrid = (Math.abs(z * 10) % 4 < 0.6) || (Math.abs(y * 10) % 4 < 0.6);
                String color = isGrid ? C_SOLAR_GRID : C_SOLAR_GOLD;

                // Arrays are broadside to the Y axis (normal faces +/- X)
                projectPoint(xCenter, y, z, 1.0, 0.0, 0.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                projectPoint(xCenter - 0.01, y, z, -1.0, 0.0, 0.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
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
        double cameraDepth = rotY + 6.0; // Pushed back slightly to fit the wide truss
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
                
                // Keep navigational lights fully bright regardless of rotation
                double illuminance = (colorCode.equals(C_RED_NAV) || colorCode.equals(C_GREEN_NAV)) 
                        ? 1.0 : Math.max(0.15, dot);

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