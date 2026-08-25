import java.util.Arrays;

public class SatelliteLoader extends Loader {

    private static final StatusStage[] SATELLITE_STAGES = {
            new StatusStage(15, "Establishing secure telemetry uplink..."),
            new StatusStage(35, "Deploying primary solar arrays..."),
            new StatusStage(55, "Aligning parabolic dish with ground station..."),
            new StatusStage(80, "Synchronizing orbital trajectory..."),
            new StatusStage(100, "Uplink stable. Data transmission active.")
    };

    private double angleX = 0.4;
    private double angleY = 0.0;
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Satellite Aesthetic Color Palette
    private static final String C_GOLD_FOIL   = "\u001B[38;2;230;185;30m";  // Kapton insulation foil (Main Body)
    private static final String C_SOLAR_BLUE  = "\u001B[38;2;25;70;180m";   // Deep blue solar cells
    private static final String C_SILVER_TECH = "\u001B[38;2;180;190;200m"; // Aluminum struts and dish antenna
    private static final String C_PANEL_GRID  = "\u001B[38;2;120;150;210m"; // Solar panel grid/veins
    private static final String C_RED_LIGHT   = "\u001B[38;2;255;50;50m";   // Status indicator lights
    private static final String RESET         = "\u001B[0m";

    public SatelliteLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public SatelliteLoader() {
        super(SATELLITE_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.4;
        this.angleY = -0.4;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear frame output buffer and reset z-buffer
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;
        angleY += 0.013; // Slow, continuous orbital spin
        angleX = 0.35 + 0.15 * Math.sin(frameTick * 0.015); // Gentle floating wobble

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Normalized Lighting Direction (Sunlight from Top-Right-Front)
        double lightX = 0.577, lightY = -0.577, lightZ = 0.577;

        // 1. RENDER MAIN BUS (Central Gold Foil Cylinder)
        renderCylinder(0.0, 0.0, 0.0, 0.35, 1.4, C_GOLD_FOIL,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        
        // Lower instrument module (Silver)
        renderCylinder(0.0, -0.8, 0.0, 0.25, 0.3, C_SILVER_TECH,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. RENDER SOLAR PANELS (Left and Right extending wings)
        renderSolarPanels(cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. RENDER PARABOLIC DISH ANTENNA (Top mounted)
        renderDish(0.0, 0.7, 0.0, 0.65, 0.3, C_SILVER_TECH, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        
        // Central transmitting spire extending from the dish
        renderCylinder(0.0, 1.05, 0.0, 0.03, 0.7, C_SILVER_TECH,
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // Blinking Red Transmitter Light at the tip of the spire
        if (frameTick % 20 < 10) {
            projectPoint(0.0, 1.45, 0.0, 0.0, 1.0, 0.0, C_RED_LIGHT, 
                    cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }
    }

    private void renderCylinder(double cx, double cy, double cz, double radius, double height, String color,
                                double cosX, double sinX, double cosY, double sinY,
                                double lx, double ly, double lz, String[] out, double[] zb) {
        for (double y = cy - height / 2.0; y <= cy + height / 2.0; y += 0.08) {
            renderCircleSlice(cx, y, cz, radius, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void renderDish(double cx, double cy, double cz, double maxRadius, double depth, String color,
                            double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        // Renders a parabolic curve for the antenna dish
        for (double r = 0; r <= maxRadius; r += 0.05) {
            double yOffset = depth * (r * r) / (maxRadius * maxRadius);
            double y = cy + yOffset;
            
            int steps = (int)(30 * (r / maxRadius)) + 5;
            for (int step = 0; step < steps; step++) {
                double rad = step * (2.0 * Math.PI / steps);
                double nx = Math.cos(rad);
                double nz = Math.sin(rad);

                double px = cx + r * nx;
                double pz = cz + r * nz;

                // Approximate normal vector pointing upwards and slightly inwards
                double nnx = -nx * (yOffset / depth);
                double nnz = -nz * (yOffset / depth);
                double ny = 1.0;

                projectPoint(px, y, pz, nnx, ny, nnz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderSolarPanels(double cosX, double sinX, double cosY, double sinY,
                                   double lx, double ly, double lz, String[] out, double[] zb) {
        // Two massive solar arrays stretching outwards
        for (double y = -0.6; y <= 0.6; y += 0.05) {
            for (double x = 0.4; x <= 2.2; x += 0.05) {
                // Procedurally generate the grid lines of the photovoltaic cells
                int gridX = (int)(x * 20) % 6;
                int gridY = (int)(Math.abs(y) * 20) % 6;
                boolean isGrid = (gridX == 0 || gridY == 0);
                
                String color = isGrid ? C_PANEL_GRID : C_SOLAR_BLUE;

                // Render Front Side of panels (+Z normal)
                projectPoint(x, y, 0.0, 0.0, 0.0, 1.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                projectPoint(-x, y, 0.0, 0.0, 0.0, 1.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);

                // Render Back Side of panels (-Z normal) to ensure they are visible when rotated
                projectPoint(x, y, -0.01, 0.0, 0.0, -1.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                projectPoint(-x, y, -0.01, 0.0, 0.0, -1.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderCircleSlice(double cx, double cy, double cz, double radius, String color,
                                   double cosX, double sinX, double cosY, double sinY,
                                   double lx, double ly, double lz, String[] out, double[] zb) {
        for (int step = 0; step < 24; step++) {
            double rad = step * (2.0 * Math.PI / 24.0);
            double nx = Math.cos(rad);
            double nz = Math.sin(rad);
            double px = cx + radius * nx;
            double pz = cz + radius * nz;

            projectPoint(px, cy, pz, nx, 0.0, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                              double cosX, double sinX, double cosY, double sinY,
                              double lx, double ly, double lz, String[] out, double[] zb) {
        // 1. Apply World Rotations (Yaw around Y-axis, then Pitch around X-axis)
        double r1x = px * cosY - py * sinY;
        double r1y = px * sinY + py * cosY;
        double r1z = pz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // Rotate Normal Vectors for accurate dynamic lighting
        double n1x = nx * cosY - ny * sinY;
        double n1y = nx * sinY + ny * cosY;
        double n1z = nz;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        // Normalization
        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen;
        }

        // 2. Perspective Projection & Depth Calculations
        double cameraDepth = rotY + 4.0; // Distance of the camera
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;

        // Map 3D coordinates to the 2D terminal grid
        int sx = (int) (width / 2.0 + 46.0 * D * rotX);
        int sy = (int) (height / 2.0 - 22.0 * D * rotZ);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            // Z-Buffer Max-Sorting (Closest geometry wins out to block hidden points)
            if (D > zb[idx]) {
                zb[idx] = D;

                // Lighting Exposure Calculation
                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                
                // Allow the red indicator light to ignore shading
                double illuminance = colorCode.equals(C_RED_LIGHT) ? 1.0 : Math.max(0.15, dot); 

                // ASCII Luminance Ramp Selection
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