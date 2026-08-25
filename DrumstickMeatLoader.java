public class DrumstickMeatLoader extends Loader {

    private static final StatusStage[] DRUMSTICK_STAGES = {
            new StatusStage(20, "Plucking the feathers..."),
            new StatusStage(40, "Marinating in a secret blend of 11 herbs & spices..."),
            new StatusStage(60, "Roasting over an open cartoon campfire..."),
            new StatusStage(85, "Getting that crispy golden brown skin..."),
            new StatusStage(100, "Hot and ready! Time to chow down.")
    };

    private static final String LUMINANCE_CHARS = "~=!*#$@W";

    private String meatColor;
    private String boneColor;
    private String[][] cellCache;

    // Viewing angles
    private double A = 0.2; // Pitch
    private double B = 0.0; // Yaw

    public DrumstickMeatLoader() {
        super(DRUMSTICK_STAGES, 80, 24);
    }

    public DrumstickMeatLoader(int w, int h) {
        super(DRUMSTICK_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // True-color RGB values for maximum appetite appeal
        meatColor = "\u001B[38;2;210;105;30m"; // Golden/Roasted Brown
        boneColor = "\u001B[38;2;245;240;225m"; // Off-White / Ivory Bone

        String[] fullPalette = { meatColor, boneColor };
        cellCache = new String[fullPalette.length][LUMINANCE_CHARS.length()];
        for (int c = 0; c < fullPalette.length; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = fullPalette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinB = Math.sin(B), cosB = Math.cos(B);

        // ==========================================
        // 1. THE ROASTED MEAT (Teardrop / Bulbous Shape)
        // ==========================================
        // x goes from left (-1.8) to right (0.8), where the bone starts exposing
        double meatStartX = -1.8;
        double meatEndX = 1.3;
        double meatLength = meatEndX - meatStartX;

        for (double x = meatStartX; x <= meatEndX; x += 0.04) {
            // Normalize x to a 0.0 -> 1.0 range
            double normX = (x - meatStartX) / meatLength; 
            
            // Equation for a teardrop drumstick shape: 
            // Fat on the left (normX near 0.2 - 0.4), pinched on the right (normX near 1.0)
            double radius = 1.4 * Math.sin(normX * Math.PI) * (1.1 - normX * 0.7);

            if (radius < 0.02) continue; // Skip the absolute microscopic tips

            // Calculate surface normals to ensure perfectly rounded lighting
            double nx = Math.cos(normX * Math.PI); 
            double radialWeight = Math.sin(normX * Math.PI);

            for (double a = 0; a < 2 * Math.PI; a += 0.08) {
                double y = radius * Math.cos(a);
                double z = radius * Math.sin(a);

                double ny = Math.cos(a) * radialWeight;
                double nz = Math.sin(a) * radialWeight;

                // Normalize the normal vector
                double len = Math.sqrt(nx*nx + ny*ny + nz*nz);
                
                drawPoint(x, y, z, nx/len, ny/len, nz/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // ==========================================
        // 2. THE BONE STEM
        // ==========================================
        // Shoots out from the tapered end of the meat
        double stemRadius = 0.18;
        for (double x = 0.6; x <= 2.2; x += 0.05) {
            for (double a = 0; a < 2 * Math.PI; a += 0.25) {
                double y = stemRadius * Math.cos(a);
                double z = stemRadius * Math.sin(a);
                drawPoint(x, y, z, 0.0, Math.cos(a), Math.sin(a), sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }

        // ==========================================
        // 3. THE BONE KNOBS (Cartilage joints at the tip)
        // ==========================================
        double knobRadius = 0.3;
        // Two knobs positioned slightly offset from the central stem
        double[][] knobs = {
            {2.3,  0.3, 0.0}, 
            {2.3, -0.3, 0.0}
        };

        for (double[] knob : knobs) {
            double kx = knob[0];
            double ky = knob[1];
            double kz = knob[2];

            // Render a sphere for each knob
            for (double theta = 0; theta <= Math.PI; theta += 0.15) {
                for (double phi = 0; phi < 2 * Math.PI; phi += 0.15) {
                    double pnx = Math.sin(theta) * Math.cos(phi);
                    double pny = Math.sin(theta) * Math.sin(phi);
                    double pnz = Math.cos(theta);

                    double px = kx + knobRadius * pnx;
                    double py = ky + knobRadius * pny;
                    double pz = kz + knobRadius * pnz;

                    drawPoint(px, py, pz, pnx, pny, pnz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
                }
            }
        }

        A += 0.02;   
        B += 0.035;  
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz, double sinA, double cosA,
                           double sinB, double cosB, String[] outputBuffer, double[] zBuffer, int colorIndex) {
        
        // 3D Rotation Math
        double x1 = x * cosB - y * sinB;
        double y1 = x * sinB + y * cosB;
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;
        
        double distance = 4.0;
        double ooZ = 1.0 / (z2 + distance);
        
        // Perspective projection
        int xp = (int) (window_width / 2.0 + 38 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 17 * ooZ * y2);
        int bufferIndex = xp + window_width * yp;

        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            
            // Rotate the normals for accurate lighting on the moving object
            double nx1 = nx * cosB - ny * sinB;
            double ny1 = nx * sinB + ny * cosB;
            double ny2 = ny1 * cosA - nz * sinA;
            double nz2 = ny1 * sinA + nz * cosA;
            double nx2 = nx1;
            
            // Calculate luminance (directional light pointing mostly down and slightly right)
            double luminance = nx2 * 0.4 + ny2 * -0.2 + nz2 * 0.8;

            if (ooZ > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooZ;
                
                // Map the calculated luminance to the ASCII character array
                int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));
                
                outputBuffer[bufferIndex] = cellCache[colorIndex][charIndex];
            }
        }
    }
}