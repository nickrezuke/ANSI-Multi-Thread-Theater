// TODO: Make this look more like a rocket ship / the shuttle

public class RocketLoader extends Loader {

    private static final StatusStage[] SHUTTLE_STAGES = {
            new StatusStage(20, "Clearing the launch pad gantry..."),
            new StatusStage(45, "Igniting Solid Rocket Boosters..."),
            new StatusStage(75, "Max-Q structural pressure peak..."),
            new StatusStage(90, "Main engine throttle up..."),
            new StatusStage(100, "Orbital Insertion Successful!")
    };

    // Automated multi-axis continuous rotation angles
    private double angleX = 0.0;
    private double angleY = 0.0;

    private final int width;
    private final int height;

    // HIGH-FIDELITY STRUCTURAL 3D MESH KEYFRAMES: THE NASA SPACE SHUTTLE ORBITER
    // Maps [X, Y, Z] structural nodes capturing the real delta-wing and fin
    // profiles.
    // X = Left/Right span, Y = Front/Back length, Z = Up/Down vertical fuselage
    // axis.
    private static final double[][] SHUTTLE_NODES = {
            // --- THE COCKPIT NOSE CONE & FORWARD FUSELAGE (Indices 0 - 6) ---
            { 0.0, 1.8, -0.1 }, { 0.12, 1.6, -0.15 }, { -0.12, 1.6, -0.15 },
            { 0.24, 1.3, -0.18 }, { -0.24, 1.3, -0.18 }, { 0.30, 0.9, -0.20 }, { -0.30, 0.9, -0.20 },
            // --- MAIN CARGO BAY BODY HULL CORE (Indices 7 - 14) ---
            { 0.32, 0.4, -0.22 }, { -0.32, 0.4, -0.22 }, { 0.35, -0.2, -0.25 }, { -0.35, -0.2, -0.25 },
            { 0.35, -0.8, -0.25 }, { -0.35, -0.8, -0.25 }, { 0.32, -1.3, -0.22 }, { -0.32, -1.3, -0.22 },
            // --- SWEEPING DELTA WING FORWARD GLIDE RIM (Indices 15 - 20) ---
            { 0.45, 0.2, -0.24 }, { -0.45, 0.2, -0.24 }, { 0.85, -0.4, -0.26 }, { -0.85, -0.4, -0.26 },
            { 1.35, -1.1, -0.28 }, { -1.35, -1.1, -0.28 },
            // --- EXTENDED DELTA WING REAR TRAILING EDGES (Indices 21 - 24) ---
            { 1.55, -1.4, -0.29 }, { -1.55, -1.4, -0.29 }, { 0.40, -1.4, -0.24 }, { -0.40, -1.4, -0.24 },
            // --- REAR ORBITAL MANEUVERING SYSTEM PODS (OMS) & ENGINES (Indices 25 - 30)
            // ---
            { 0.22, -1.45, -0.05 }, { -0.22, -1.45, -0.05 }, { 0.18, -1.60, 0.10 }, { -0.18, -1.60, 0.10 },
            { 0.0, -1.65, -0.12 }, { 0.0, -1.65, 0.0 },
            // --- SWEEPING VERTICAL RUDDER TAIL STABILIZER FIN (Indices 31 - 34) ---
            { 0.0, -0.9, 0.15 }, { 0.0, -1.2, 0.55 }, { 0.0, -1.5, 0.95 }, { 0.0, -1.52, 0.18 }
    };

    public RocketLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public RocketLoader() {
        super(SHUTTLE_STAGES);
        this.width = this.window_width;
        this.height = this.window_height;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.4; // Default slight tilt angle to show off the delta-wings
        this.angleY = 0.0;

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Smooth automated continuous multi-axis tumbling rotation
        angleX += 0.020;
        angleY += 0.035;

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Directional overhead 3D lighting vector coming from the top-front-right
        double lightX = 0.577;
        double lightY = -0.577;
        double lightZ = 0.577;
        double cameraDistance = 4.2;

        // Loop 1: Render the Main Fuselage Hull and Cockpit (Indices 0 to 14)
        // Sweeps the keyframe node profiles in a full cross-sectional circle to create
        // the 3D body
        for (int layer = 0; layer <= 14; layer += 1) {
            int nodeL = layer;
            int nodeR = Math.min(SHUTTLE_NODES.length - 1, layer + 1);

            double baseWidth = Math.abs(SHUTTLE_NODES[nodeL][0] - SHUTTLE_NODES[nodeR][0]) * 0.5;
            double baseLength = (SHUTTLE_NODES[nodeL][1] + SHUTTLE_NODES[nodeR][1]) * 0.5;
            double baseHeight = (SHUTTLE_NODES[nodeL][2] + SHUTTLE_NODES[nodeR][2]) * 0.5;

            for (int step = 0; step < 36; step++) {
                double rad = step * (2.0 * Math.PI / 36.0);
                double cosRad = Math.cos(rad);
                double sinRad = Math.sin(rad);

                // Extrude a slightly flattened cylinder structure to match the real cargo bay
                // cross section
                double localX = baseWidth * cosRad;
                double localY = baseLength;
                double localZ = baseHeight + (baseWidth * 0.75 * sinRad); // Oval fuselage factor

                // Structural outward normals pointing away from the fuselage center axis
                double normalX = cosRad;
                double normalY = 0.2;
                double normalZ = sinRad;

                projectShuttleVertex(localX, localY, localZ, normalX, normalY, normalZ,
                        cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // Loop 2: Render the Authentic Sweeping Delta Wings (Indices 15 to 24)
        // Linearly interpolates skin points across the wing keyframe skeleton paths
        for (int i = 15; i <= 19; i += 2) {
            double[] rootL = SHUTTLE_NODES[i];
            double[] rootR = SHUTTLE_NODES[i + 1];
            double[] tipL = SHUTTLE_NODES[i + 2];
            double[] tipR = SHUTTLE_NODES[Math.min(24, i + 3)];

            for (int seg = 0; seg <= 25; seg++) {
                double alpha = seg / 25.0; // Interpolation down the wing span

                for (int thick = 0; thick < 4; thick++) {
                    double wingThick = (thick - 2) * 0.025; // Gives the wings thin physical volume

                    // Left wing sweep
                    double lxL = rootL[0] * (1.0 - alpha) + tipL[0] * alpha;
                    double lyL = rootL[1] * (1.0 - alpha) + tipL[1] * alpha;
                    double lzL = rootL[2] * (1.0 - alpha) + tipL[2] * alpha + wingThick;
                    projectShuttleVertex(lxL, lyL, lzL, 0, 0, 1.0, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                            outputBuffer, zBuffer);

                    // Right wing sweep
                    double lxR = rootR[0] * (1.0 - alpha) + tipR[0] * alpha;
                    double lyR = rootR[1] * (1.0 - alpha) + tipR[1] * alpha;
                    double lzR = rootR[2] * (1.0 - alpha) + tipR[2] * alpha + wingThick;
                    projectShuttleVertex(lxR, lyR, lzR, 0, 0, 1.0, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                            outputBuffer, zBuffer);
                }
            }
        }

        // Loop 3: Render the Tall Vertical Rudder Fin Tail Stabilizer (Indices 31 to
        // 34)
        // Draws the towering triangular rudder spine rising out of the rear hull
        for (int seg = 0; seg < 20; seg++) {
            double alpha = seg / 20.0;
            // Linearly interpolate coordinates from the base root up to the high peak node
            double localX = 0.0;
            double localY = SHUTTLE_NODES[31][1] * (1.0 - alpha) + SHUTTLE_NODES[33][1] * alpha;
            double localZ = SHUTTLE_NODES[31][2] * (1.0 - alpha) + SHUTTLE_NODES[33][2] * alpha;

            // Extrude width outward slightly to give the tail fin physical cross-sectional
            // thickness
            for (int w = -3; w <= 3; w++) {
                double thickOffset = w * 0.015 * (1.0 - alpha); // Fin tapers to a sharp edge at the top peak

                // Fin surface normals point directly out to the left and right sides
                double normalX = (w >= 0) ? 1.0 : -1.0;
                double normalY = -0.15; // Swept back slope
                double normalZ = 0.0;

                projectShuttleVertex(localX + thickOffset, localY, localZ, normalX, normalY, normalZ,
                        cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // Loop 4: Render the Rear OMS Thrust Pod Exhaust Cones (Indices 25 to 30)
        for (int pod = 25; pod <= 26; pod++) {
            double[] node = SHUTTLE_NODES[pod];
            for (int step = 0; step < 16; step++) {
                double rad = step * (2.0 * Math.PI / 16.0);
                double localX = node[0] + 0.15 * Math.cos(rad);
                double localY = node[1];
                double localZ = node[2] + 0.15 * Math.sin(rad);
                projectShuttleVertex(localX, localY, localZ, 0, -1.0, 0, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                        outputBuffer, zBuffer);
            }
        }
    }

    private void projectShuttleVertex(double lx, double ly, double lz, double nx, double ny, double nz,
            double cosX, double sinX, double cosY, double sinY,
            double lightX, double lightY, double lightZ, String[] out, double[] zb) {
        // 1. Rotate the Vertex Position Vectors (Yaw around vertical Z, then Pitch
        // around horizontal X)
        double r1x = lx * cosY - ly * sinY;
        double r1y = lx * sinY + ly * cosY;
        double r1z = lz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // 2. Rotate the Surface Normal Vectors identically so shading matches the
        // viewpoint perspective
        double n1x = nx * cosY - ny * sinY;
        double n1y = nx * sinY + ny * cosY;
        double n1z = nz;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        double nLen = Math.sqrt(rotNX * rotNX + rotNY * rotNY + rotNZ * rotNZ);
        if (nLen > 0) {
            rotNX /= nLen;
            rotNY /= nLen;
            rotNZ /= nLen;
        }

        // 3. Perspective Projection & Scale calculations (Y represents deep distance
        // away from eye)
        double cameraDepth = rotY + 4.2;
        double D = 1.0 / cameraDepth;

        // Map coordinates cleanly into your 80x22 canvas layout dimensions
        int sx = (int) (width / 2.0 + 54 * D * rotX);
        int sy = (int) (height / 2.0 - 24 * D * rotZ);
        int o = sx + width * sy;

        // 4. VECTOR DIFFUSE LUMINANCE SHADER
        double dotProduct = rotNX * lightX + rotNY * lightY + rotNZ * lightZ;

        // Ambient background illumination constant ensures unlit hulls don't disappear
        // completely
        double luminance = 0.16 + 0.84 * Math.max(0.0, dotProduct);

        if (sy < height && sy >= 0 && sx >= 0 && sx < width && D > (zb[o] + 0.0001)) {
            zb[o] = D;
            // Map intensity smoothly into a crisp 12-stage grayscale character ramp
            String palette = " .,-~:;=!*#█";
            int charIndex = (int) (luminance * (palette.length() - 1));
            charIndex = Math.max(0, Math.min(palette.length() - 1, charIndex));
            char asciiChar = palette.charAt(charIndex);
            if (asciiChar != ' ') {
                out[o] = WHITE + asciiChar + RESET;
            } else {
                out[o] = " ";
            }
        }
    }
}