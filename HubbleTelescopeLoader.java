import java.util.Arrays;

public class HubbleTelescopeLoader extends Loader {

    private static final StatusStage[] HUBBLE_STAGES = {
            new StatusStage(15, "Powering up gyroscopes for attitude control..."),
            new StatusStage(35, "Deploying high-gain antennas..."),
            new StatusStage(55, "Unlocking aperture door..."),
            new StatusStage(75, "Slewing to target: Eagle Nebula (M16)..."),
            new StatusStage(100, "Locking guide stars. Acquiring deep field photons.")
    };

    private double angleX = 0.3;
    private double angleY = 0.0;
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Authentic Hubble Color Palette
    private static final String C_FOIL_SILVER = "\u001B[38;2;190;200;210m"; // Reflective silver thermal blanket
    private static final String C_AFT_SHROUD  = "\u001B[38;2;160;170;180m"; // Slightly darker metallic aft section
    private static final String C_SOLAR_BLUE  = "\u001B[38;2;30;80;190m";   // Deep blue solar array blankets
    private static final String C_GOLD_BOOM   = "\u001B[38;2;220;170;40m";  // Gold/bronze support struts and booms
    private static final String C_INTERIOR    = "\u001B[38;2;15;15;20m";    // Pitch black/dark purple interior baffle
    private static final String C_MIRROR      = "\u001B[38;2;100;220;255m"; // Glowing cyan primary mirror reflection
    private static final String RESET         = "\u001B[0m";

    public HubbleTelescopeLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public HubbleTelescopeLoader() {
        super(HUBBLE_STAGES, 80, 22);
        this.width = 80;
        this.height = 22;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.3;
        this.angleY = -0.5;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;
        angleY += 0.015; // Slow orbital rotation
        angleX = 0.25 + 0.1 * Math.sin(frameTick * 0.02); // Microgravity drift

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Sunlight direction (Top-Right-Front)
        double lightX = 0.577, lightY = -0.577, lightZ = 0.577;

        // 1. AFT SHROUD (Equipment Section - Wider base)
        renderCylinder(0.0, -0.6, 0.0, 0.45, 0.8, C_AFT_SHROUD, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. FORWARD BAFFLE (Main Telescope Tube)
        renderCylinder(0.0, 0.5, 0.0, 0.35, 1.4, C_FOIL_SILVER, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. TELESCOPE INTERIOR & PRIMARY MIRROR
        // Render a black circle inside the tube to make it look hollow
        renderDisc(0.0, 1.15, 0.0, 0.34, 0.0, 1.0, 0.0, C_INTERIOR, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Render the primary mirror deep at the bottom of the tube
        renderDisc(0.0, -0.1, 0.0, 0.33, 0.0, 1.0, 0.0, C_MIRROR, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 4. APERTURE DOOR (Tilted open at the top)
        double tilt = Math.PI / 3.0; // 60 degrees open
        renderApertureDoor(0.0, 1.2, -0.35, 0.35, tilt, C_FOIL_SILVER, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 5. SOLAR ARRAYS (Massive twin blankets extending on X-axis)
        renderSolarArrays(cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 6. HIGH-GAIN ANTENNAS (Extending outward from the aft shroud)
        renderAntenna(0.45, -0.6, 0.0, 0.8, 0.2, C_GOLD_BOOM, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderAntenna(-0.45, -0.6, 0.0, -0.8, 0.2, C_GOLD_BOOM, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
    }

    private void renderCylinder(double cx, double cy, double cz, double radius, double height, String color,
                                double cosX, double sinX, double cosY, double sinY,
                                double lx, double ly, double lz, String[] out, double[] zb) {
        for (double y = cy - height / 2.0; y <= cy + height / 2.0; y += 0.04) {
            for (int step = 0; step < 48; step++) {
                double rad = step * (2.0 * Math.PI / 48.0);
                double nx = Math.cos(rad);
                double nz = Math.sin(rad);
                double px = cx + radius * nx;
                double pz = cz + radius * nz;

                projectPoint(px, y, pz, nx, 0.0, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderDisc(double cx, double cy, double cz, double maxRadius, 
                            double nx, double ny, double nz, String color,
                            double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        for (double r = 0; r <= maxRadius; r += 0.03) {
            int steps = (int)(24 * (r / maxRadius)) + 1;
            for (int step = 0; step < steps; step++) {
                double rad = step * (2.0 * Math.PI / steps);
                // Disc lies on XZ plane relative to its center, before applying its normal (assuming ny=1 here)
                double px = cx + r * Math.cos(rad);
                double pz = cz + r * Math.sin(rad);

                projectPoint(px, cy, pz, nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderApertureDoor(double hingeX, double hingeY, double hingeZ, double radius, double tilt, String color,
                                    double cosX, double sinX, double cosY, double sinY,
                                    double lx, double ly, double lz, String[] out, double[] zb) {
        double cosTilt = Math.cos(tilt);
        double sinTilt = Math.sin(tilt);
        
        // The door normal vector
        double nx = 0.0;
        double ny = sinTilt;
        double nz = -cosTilt;

        for (double r = 0; r <= radius; r += 0.04) {
            int steps = (int)(24 * (r / radius)) + 1;
            for (int step = 0; step < steps; step++) {
                double rad = step * (2.0 * Math.PI / steps);
                
                // Local disc coordinates
                double dx = r * Math.cos(rad);
                double dy = r * Math.sin(rad); // distance from hinge along the door's surface

                // Translate and apply tilt rotation around X-axis at the hinge
                double px = hingeX + dx;
                double py = hingeY + dy * cosTilt;
                double pz = hingeZ + dy * sinTilt;

                // Render both front (+normal) and back (-normal) of the door
                projectPoint(px, py, pz, nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                projectPoint(px, py, pz, -nx, -ny, -nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderSolarArrays(double cosX, double sinX, double cosY, double sinY,
                                   double lx, double ly, double lz, String[] out, double[] zb) {
        // Hubble's classic twin arrays unrolled along the X-axis
        double[] wings = {1.0, -1.0}; // Right and Left multipliers

        for (double w : wings) {
            // Central gold deployment boom
            for (double x = 0.5; x <= 2.6; x += 0.1) {
                projectPoint(x * w, -0.1, 0.0, 0, 1, 0, C_GOLD_BOOM, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }

            // Blue Solar Blankets (above and below the central boom)
            for (double x = 0.6; x <= 2.5; x += 0.03) {
                for (double y = -0.4; y <= 0.2; y += 0.03) {
                    if (y > -0.15 && y < -0.05) continue; // Gap for the central boom
                    
                    String color = C_SOLAR_BLUE;
                    // Grid pattern for cells
                    if (Math.abs(x * 10) % 5 < 0.5) color = C_FOIL_SILVER; 

                    // Front facing (+Z) and back facing (-Z)
                    projectPoint(x * w, y, 0.01, 0.0, 0.0, 1.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                    projectPoint(x * w, y, -0.01, 0.0, 0.0, -1.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
                }
            }
        }
    }

    private void renderAntenna(double startX, double startY, double startZ, double dx, double dz, String color,
                               double cosX, double sinX, double cosY, double sinY,
                               double lx, double ly, double lz, String[] out, double[] zb) {
        // Simple line drawing for the antenna strut
        int steps = 15;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = startX + dx * t;
            double py = startY; 
            double pz = startZ + dz * t;
            
            // Artificial normal pointing up for lighting calculation
            projectPoint(px, py, pz, 0.0, 1.0, 0.0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
        
        // Antenna dish at the tip
        double tipX = startX + dx;
        double tipZ = startZ + dz;
        renderDisc(tipX, startY, tipZ, 0.15, 0.0, 1.0, 0.0, C_FOIL_SILVER, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
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
        double cameraDepth = rotY + 2.9; 
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
                
                // Keep the glowing interior mirror fully bright, shadow the interior walls
                double illuminance;
                if (colorCode.equals(C_MIRROR)) {
                    illuminance = 0.9; 
                } else if (colorCode.equals(C_INTERIOR)) {
                    illuminance = 0.05; // Very dark interior
                } else {
                    illuminance = Math.max(0.15, dot);
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