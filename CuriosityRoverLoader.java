import java.util.Arrays;

public class CuriosityRoverLoader extends Loader {

    private static final StatusStage[] MSL_STAGES = {
            new StatusStage(15, "Cruise stage separation complete. Entering Martian atmosphere..."),
            new StatusStage(35, "Guided entry active. Deploying supersonic parachute..."),
            new StatusStage(60, "Radar lock on Gale Crater. Powered descent initiated..."),
            new StatusStage(80, "Sky Crane active. Lowering rover on umbilical..."),
            new StatusStage(100, "Touchdown confirmed! Tango Delta. Mast deploying...")
    };

    private double angleX = -0.35; // Pitch (looking slightly down at the rover)
    private double angleY = 0.5;   // Yaw (rotating around the rover)
    private int frameTick = 0;

    private final int width;
    private final int height;

    // Mars Curiosity Authentic Color Palette
    private static final String C_WHITE_DECK   = "\u001B[38;2;235;240;245m"; // Top deck and Mast
    private static final String C_SILVER_BODY  = "\u001B[38;2;170;180;190m"; // Chassis and components
    private static final String C_DARK_METAL   = "\u001B[38;2;70;75;80m";    // Rocker-bogie suspension & Arm
    private static final String C_BLACK_WHEEL  = "\u001B[38;2;35;40;45m";    // Aluminum wheels (darkened by shadow/dust)
    private static final String C_GOLD_FOIL    = "\u001B[38;2;215;165;40m";  // Kapton wiring / instrument shielding
    private static final String C_RTG_FINS     = "\u001B[38;2;100;105;110m"; // Radioisotope Thermoelectric Generator
    private static final String C_LENS         = "\u001B[38;2;50;180;220m";  // ChemCam / Mastcam optical lenses
    private static final String RESET          = "\u001B[0m";

    public CuriosityRoverLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public CuriosityRoverLoader() {
        super(MSL_STAGES, 80, 24);
        this.width = 80;
        this.height = 24;
    }

    @Override
    protected void initialize() {
        this.angleX = -0.4; // Tilt down
        this.angleY = 0.8;  // Angled view
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, -Double.MAX_VALUE);

        frameTick++;
        angleY += 0.015; // Roving panoramic pan
        
        // Slight suspension bounce as it "drives"
        double bounce = 0.017 * Math.sin(frameTick * 0.04); 

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Sunlight on Mars (from above and slightly right/front)
        double lightX = 0.5, lightY = 0.7, lightZ = -0.5;

        // 1. MAIN CHASSIS (WEB/Deck)
        renderBox(-0.35, 0.0 + bounce, -0.5, 0.35, 0.2 + bounce, 0.5, C_WHITE_DECK, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        
        // Underbelly
        renderBox(-0.3, -0.1 + bounce, -0.45, 0.3, 0.0 + bounce, 0.45, C_SILVER_BODY, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 2. RTG (Radioisotope Thermoelectric Generator at the rear)
        renderBox(-0.15, 0.05 + bounce, -0.75, 0.15, 0.25 + bounce, -0.5, C_RTG_FINS, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // RTG Heat fins (gold-ish hue in some lighting, dark grey in reality - using gold for accent)
        renderBox(-0.18, 0.1 + bounce, -0.7, 0.18, 0.2 + bounce, -0.55, C_GOLD_FOIL, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 3. ROCKER-BOGIE SUSPENSION & WHEELS (6 wheels total)
        renderSuspensionSide(-0.45, C_DARK_METAL, C_BLACK_WHEEL, bounce, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        renderSuspensionSide(0.45, C_DARK_METAL, C_BLACK_WHEEL, bounce, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 4. REMOTE SENSING MAST (Head and Neck)
        // Neck
        renderLine(-0.25, 0.2 + bounce, 0.35, -0.25, 0.7 + bounce, 0.35, C_WHITE_DECK, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Head (Mastcam / ChemCam assembly)
        renderBox(-0.35, 0.7 + bounce, 0.25, -0.15, 0.85 + bounce, 0.45, C_WHITE_DECK, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Camera Lenses facing forward
        renderBox(-0.3, 0.75 + bounce, 0.45, -0.2, 0.8 + bounce, 0.48, C_LENS, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);

        // 5. ROBOTIC ARM (Extended out front)
        // Shoulder joint
        renderLine(0.0, 0.1 + bounce, 0.5, 0.15, 0.1 + bounce, 0.7, C_DARK_METAL, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Elbow to Turret
        renderLine(0.15, 0.1 + bounce, 0.7, 0.0, -0.1 + bounce, 0.9, C_DARK_METAL, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
        // Turret (Drill/Scoop instrument cluster)
        renderBox(-0.08, -0.15 + bounce, 0.85, 0.08, 0.0 + bounce, 1.0, C_SILVER_BODY, 
                cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
    }

    private void renderSuspensionSide(double xOffset, String strutColor, String wheelColor, double bounce,
                                      double cosX, double sinX, double cosY, double sinY,
                                      double lx, double ly, double lz, String[] out, double[] zb) {
        
        double wheelRadius = 0.16;
        double wheelWidth = 0.14;
        
        double wheelFrontZ = 0.45;
        double wheelMidZ = 0.0;
        double wheelRearZ = -0.45;
        double wheelY = -0.3; // Wheels touch ground at roughly -0.46

        // 1. Render the three wheels (Cylinders aligned along the X axis)
        double wX = (xOffset > 0) ? xOffset + 0.05 : xOffset - 0.05;
        renderWheel(wX, wheelY, wheelFrontZ, wheelRadius, wheelWidth, wheelColor, 
                cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        renderWheel(wX, wheelY, wheelMidZ, wheelRadius, wheelWidth, wheelColor, 
                cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        renderWheel(wX, wheelY, wheelRearZ, wheelRadius, wheelWidth, wheelColor, 
                cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);

        // 2. Render Rocker-Bogie Struts
        double pivotX = xOffset > 0 ? xOffset - 0.05 : xOffset + 0.05;
        double mainPivotY = 0.0 + bounce;
        double mainPivotZ = 0.0;
        
        double frontBogiePivotY = -0.15 + bounce;
        double frontBogiePivotZ = 0.25;

        // Main rocker: Pivot to Rear Wheel
        renderLine(pivotX, mainPivotY, mainPivotZ, pivotX, wheelY, wheelRearZ, strutColor, 
                cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        // Main rocker: Pivot to Front Bogie joint
        renderLine(pivotX, mainPivotY, mainPivotZ, pivotX, frontBogiePivotY, frontBogiePivotZ, strutColor, 
                cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        
        // Front Bogie: Joint to Mid Wheel
        renderLine(pivotX, frontBogiePivotY, frontBogiePivotZ, pivotX, wheelY, wheelMidZ, strutColor, 
                cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        // Front Bogie: Joint to Front Wheel
        renderLine(pivotX, frontBogiePivotY, frontBogiePivotZ, pivotX, wheelY, wheelFrontZ, strutColor, 
                cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
    }

    private void renderWheel(double cx, double cy, double cz, double radius, double width, String color,
                             double cosX, double sinX, double cosY, double sinY,
                             double lx, double ly, double lz, String[] out, double[] zb) {
        double halfW = width / 2.0;
        
        // Wheel tread tube
        for (double x = cx - halfW; x <= cx + halfW; x += 0.04) {
            for (int step = 0; step < 24; step++) {
                double rad = step * (Math.PI * 2.0 / 24.0);
                double ny = Math.cos(rad);
                double nz = Math.sin(rad);
                projectPoint(x, cy + radius * ny, cz + radius * nz, 0, ny, nz, color, 
                        cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
        
        // Wheel outer hubcap
        double capX = (cx > 0) ? cx + halfW : cx - halfW;
        double nx = (cx > 0) ? 1 : -1;
        for (double r = 0; r <= radius; r += 0.04) {
            int steps = (int)(16 * (r / radius)) + 1;
            for (int step = 0; step < steps; step++) {
                double rad = step * (Math.PI * 2.0 / steps);
                double py = Math.cos(rad);
                double pz = Math.sin(rad);
                projectPoint(capX, cy + r * py, cz + r * pz, nx, 0, 0, C_SILVER_BODY, 
                        cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
            }
        }
    }

    private void renderBox(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax, String color,
                           double cosX, double sinX, double cosY, double sinY,
                           double lx, double ly, double lz, String[] out, double[] zb) {
        double step = 0.025;
        // Top and Bottom faces (Y normal)
        for (double x = xMin; x <= xMax; x += step) {
            for (double z = zMin; z <= zMax; z += step) {
                projectPoint(x, yMin, z, 0, -1, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Bottom
                projectPoint(x, yMax, z, 0, 1, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);  // Top
            }
        }
        // Left and Right faces (X normal)
        for (double y = yMin; y <= yMax; y += step) {
            for (double z = zMin; z <= zMax; z += step) {
                projectPoint(xMin, y, z, -1, 0, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Left
                projectPoint(xMax, y, z, 1, 0, 0, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);  // Right
            }
        }
        // Front and Back faces (Z normal)
        for (double x = xMin; x <= xMax; x += step) {
            for (double y = yMin; y <= yMax; y += step) {
                projectPoint(x, y, zMin, 0, 0, -1, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb); // Back
                projectPoint(x, y, zMax, 0, 0, 1, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);  // Front
            }
        }
    }

    private void renderLine(double x1, double y1, double z1, double x2, double y2, double z2, 
                            String color, double cosX, double sinX, double cosY, double sinY,
                            double lx, double ly, double lz, String[] out, double[] zb) {
        double dist = Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2) + Math.pow(z2-z1, 2));
        int steps = (int) (dist * 30); 
        // Using an upward normal for lines to ensure they catch ambient light
        double nx = 0.0, ny = 1.0, nz = 0.0; 
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            projectPoint(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t, 
                    nx, ny, nz, color, cosX, sinX, cosY, sinY, lx, ly, lz, out, zb);
        }
    }

    private void projectPoint(double px, double py, double pz, double nx, double ny, double nz, String colorCode,
                              double cosX, double sinX, double cosY, double sinY,
                              double lx, double ly, double lz, String[] out, double[] zb) {
        
        // STANDARD 3D PIPELINE (X=Right, Y=Up, Z=Depth into screen)
        
        // 1. Yaw Rotation (around Y axis)
        double r1x = px * cosY + pz * sinY;
        double r1y = py;
        double r1z = -px * sinY + pz * cosY;

        // 2. Pitch Rotation (around X axis)
        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // Apply identical rotation to normals for correct dynamic lighting
        double n1x = nx * cosY + nz * sinY;
        double n1y = ny;
        double n1z = -nx * sinY + nz * cosY;
        
        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        // Normalize light vector normal
        double nLen = Math.hypot(rotNX, Math.hypot(rotNY, rotNZ));
        if (nLen > 0) {
            rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen;
        }

        // 3. Perspective Projection
        double cameraDepth = rotZ + 2.4; 
        if (cameraDepth <= 0.1) return;

        double D = 1.0 / cameraDepth;
        
        // Screen mapping (Y is inverted because console Y goes down)
        int sx = (int) (width / 2.0 + 65.0 * D * rotX);
        int sy = (int) (height / 2.0 - 32.0 * D * rotY + 1.9);

        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
            int idx = sx + width * sy;

            if (D > zb[idx]) {
                zb[idx] = D;

                // Lighting calculation (dot product of rotated normal and light direction)
                double dot = rotNX * lx + rotNY * ly + rotNZ * lz;
                double illuminance = Math.max(0.1, dot); 

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