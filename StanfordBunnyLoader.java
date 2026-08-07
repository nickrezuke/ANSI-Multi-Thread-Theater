// TODO: Make this higher fidelity and only rotate along one axis

public class StanfordBunnyLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(15, "Synthesizing parametric surface coordinates..."),
        new StatusStage(40, "Calculating surface normals and lighting vectors..."),
        new StatusStage(65, "Projecting 3D geometry onto TTY text arrays..."),
        new StatusStage(85, "Z-Buffer occlusion test successful..."),
        new StatusStage(100, "Orthographic Rasterization Complete!")
    };
    
    // Automated spin control registers
    private double angleX = 0.0;
    private double angleY = 0.0;

    private final int width;
    private final int height;

    // Hand-optimized anatomical spatial node dataset of the Stanford Bunny mesh topology
    // Maps out [X, Y, Z] structural rings (Base crouch, chest, head, and independent ears)
    private static final double[][] BUNNY_NODES = {
        // --- THE PLUMP REAR HAUNCH & PAW BASE (Indices 0 - 7) ---
        {0.0, -0.6, -0.8}, {0.4, -0.5, -0.8}, {0.6, -0.2, -0.6}, {0.7, 0.2, -0.5},
        {0.6, 0.5, -0.6},  {0.3, 0.7, -0.8},  {0.0, 0.8, -0.8}, {-0.4, 0.2, -0.7},
        // --- THE HUNCHED BACK & MAIN CHEST SHELL (Indices 8 - 15) ---
        {0.0, -0.4, -0.2}, {0.4, -0.3, -0.1}, {0.55, 0.1, 0.1},  {0.5, 0.4, 0.0},
        {0.3, 0.6, -0.2},  {0.0, 0.7, -0.3},  {-0.3, 0.4, -0.3}, {-0.3, -0.1, -0.2},
        // --- THE NECK & ROUND HEAD CRANIUM (Indices 16 - 21) ---
        {0.0, -0.1, 0.4},  {0.25, 0.0, 0.5},  {0.35, 0.2, 0.6},  {0.2, 0.4, 0.5},
        {0.0, 0.5, 0.4},   {-0.2, 0.1, 0.4},
        // --- LEFT EAR RIMS (Indices 22 - 25) ---
        {-0.15, 0.15, 0.7}, {-0.20, 0.12, 1.0}, {-0.18, 0.10, 1.3}, {-0.10, 0.12, 1.1},
        // --- RIGHT EAR RIMS (Indices 26 - 29) ---
        {0.15, 0.15, 0.7},  {0.20, 0.12, 1.0},  {0.18, 0.10, 1.3},  {0.10, 0.12, 1.1}
    };

    public StanfordBunnyLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public StanfordBunnyLoader() {
        super(STAGES);
        this.height = this.window_height;
        this.width = this.window_width;
    }

    @Override
    protected void initialize() {
        this.angleX = 0.3; // Default slight downward look angle
        this.angleY = 0.0;
        
        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Increment automated spin pacing variables over time per frame step
        angleX += 0.015;
        angleY += 0.032;

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        // Directional overhead 3D lighting ray vector coming from the top-front-right
        double lightX = 0.577; double lightY = -0.577; double lightZ = 0.577;

        // Loop 1: Render the main structural organic body (Latitudinal segment blending)
        // Tracks 3 sequential core profile rings (Base, Chest, and Head)
        for (int layer = 0; layer < 3; layer++) {
            int startIdx = (layer == 0) ? 0 : (layer == 1) ? 8 : 16;
            int nodeCount = (layer == 0 || layer == 1) ? 8 : 6;

            for (int step = 0; step < 80; step++) {
                double phi = step * (2.0 * Math.PI / 80.0);
                double cosPhi = Math.cos(phi);
                double sinPhi = Math.sin(phi);

                for (int i = 0; i < nodeCount; i++) {
                    double[] node = BUNNY_NODES[startIdx + i];
                    
                    // We blend the raw keyframes smoothly into an asymmetric organic teardrop volume
                    double radiusScale = Math.sqrt(node[0]*node[0] + node[1]*node[1]);
                    
                    double localX = radiusScale * cosPhi + (node[0] * 0.3);
                    double localY = radiusScale * sinPhi + (node[1] * 0.7); // Elongate the tail haunch
                    double localZ = node[2];

                    // Surface normals mapping curves aggressively outward from the organic hull
                    double normalX = cosPhi;
                    double normalY = sinPhi;
                    double normalZ = (node[2] > 0.3) ? 0.6 : -0.4;

                    // Unified structural downscale to fit cleanly inside your layout borders
                    projectBunnyVertex(localX * 0.95, localY * 0.95, localZ * 0.95, normalX, normalY, normalZ,
                                       cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
                }
            }
        }

        // Loop 2: Render the long asymmetric vertical Stanford Ears!
        // We sweep both left and right ear arrays into distinct independent tube profiles
        for (int ear = 0; ear < 2; ear++) {
            int startIdx = (ear == 0) ? 22 : 26;

            for (int i = 0; i < 4; i++) {
                double[] node = BUNNY_NODES[startIdx + i];

                // Pipe sweep loop to give the flat ear nodes realistic physical volume thickness
                for (int ring = 0; ring < 16; ring++) {
                    double arc = ring * (2.0 * Math.PI / 16.0);
                    double thickness = 0.12;

                    double localX = node[0] + thickness * Math.cos(arc) * 0.7;
                    double localY = node[1] + thickness * Math.sin(arc);
                    double localZ = node[2];

                    // Ear normal vectors point sharply out relative to the head pitch
                    double normalX = (ear == 0) ? -0.7 : 0.7;
                    double normalY = Math.sin(arc);
                    double normalZ = Math.cos(arc) + 0.2;

                    projectBunnyVertex(localX * 0.95, localY * 0.95, localZ * 0.95, normalX, normalY, normalZ,
                                       cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer, zBuffer);
                }
            }
        }
    }

    private void projectBunnyVertex(double lx, double ly, double lz, double nx, double ny, double nz,
                                    double cosX, double sinX, double cosY, double sinY,
                                    double lightX, double lightY, double lightZ, String[] out, double[] zb) {
        // 1. Rotate the Vertex Position Vectors (Yaw around Z, then Pitch around X)
        double r1x = lx * cosY - ly * sinY;
        double r1y = lx * sinY + ly * cosY;
        double r1z = lz;

        double rotX = r1x;
        double rotY = r1y * cosX - r1z * sinX;
        double rotZ = r1y * sinX + r1z * cosX;

        // 2. Rotate the Surface Normal Vectors identically so shadows stay pinned to the viewer
        double n1x = nx * cosY - ny * sinY;
        double n1y = nx * sinY + ny * cosY;
        double n1z = nz;

        double rotNX = n1x;
        double rotNY = n1y * cosX - n1z * sinX;
        double rotNZ = n1y * sinX + n1z * cosX;

        double nLen = Math.sqrt(rotNX*rotNX + rotNY*rotNY + rotNZ*rotNZ);
        if (nLen > 0) { rotNX /= nLen; rotNY /= nLen; rotNZ /= nLen; }

        // 3. Perspective Projection & Scale calculations (Y represents deep distance away from eye)
        double cameraDepth = rotY + 5.0; 
        double D = 1.0 / cameraDepth;

        // Project down to your custom 80x22 grid boundaries
        int sx = (int) (width / 2.0 + 46 * D * rotX);
        int sy = (int) (height / 2.0 - 22 * D * (rotZ + 0.1)); // Center height offset
        int o = sx + width * sy;

        // 4. MONOCHROME LUMINANCE DOT-PRODUCT SHADER
        double dotProduct = rotNX * lightX + rotNY * lightY + rotNZ * lightZ;
        double luminance = 0.20 + 0.80 * Math.max(0.0, dotProduct);

        if (sy < height && sy >= 0 && sx >= 0 && sx < width && D > (zb[o] + 0.0001)) {
            zb[o] = D;

            // Map intensity value smoothly into a 12-stage grayscale character ramp
            String palette = " .,-~:;=!*#$@";
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
