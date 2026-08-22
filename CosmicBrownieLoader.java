import java.util.ArrayList;
import java.util.List;

public class CosmicBrownieLoader extends Loader {
    // 1. Cosmic Brownie specific loading stages
    private static final StatusStage[] BROWNIE_STAGES = {
        new StatusStage(20, "Mixing cocoa batter:"),
        new StatusStage(40, "Baking fudge base:"),
        new StatusStage(65, "Spreading chocolate ganache:"),
        new StatusStage(90, "Dropping candy cosmic dots:"),
        new StatusStage(100, "Wrapped & Ready!")
    };

    // 2. Cosmic Candy Dot Definition (X, Y on top surface, Color Index 0-5)
    private static class CosmicDot {
        double x, y, r; // Position and radius
        int colorIdx;
        CosmicDot(double x, double y, double r, int colorIdx) {
            this.x = x; this.y = y; this.r = r; this.colorIdx = colorIdx;
        }
    }

    private static final List<CosmicDot> CANDY_DOTS = new ArrayList<>();
    static {
        // Pre-scattered cosmic candy dots spread evenly across the top face of the brownie
        // Dimensions of brownie top face are roughly X: [-2.0 to 2.0], Y: [-1.0 to 1.0]
        CANDY_DOTS.add(new CosmicDot(-1.5, -0.6, 0.18, 0)); // Pink
        CANDY_DOTS.add(new CosmicDot(-1.1,  0.5, 0.18, 1)); // Orange
        CANDY_DOTS.add(new CosmicDot(-0.6, -0.3, 0.18, 2)); // Yellow
        CANDY_DOTS.add(new CosmicDot(-0.2,  0.6, 0.18, 3)); // Green
        CANDY_DOTS.add(new CosmicDot( 0.3, -0.5, 0.18, 4)); // Blue
        CANDY_DOTS.add(new CosmicDot( 0.8,  0.4, 0.18, 5)); // Purple
        CANDY_DOTS.add(new CosmicDot( 1.4, -0.4, 0.18, 1)); // Orange
        CANDY_DOTS.add(new CosmicDot( 1.6,  0.5, 0.18, 3)); // Green
        CANDY_DOTS.add(new CosmicDot(-1.7,  0.1, 0.18, 2)); // Yellow
        CANDY_DOTS.add(new CosmicDot(-0.8,  0.7, 0.18, 4)); // Blue
        CANDY_DOTS.add(new CosmicDot( 0.0, -0.8, 0.18, 0)); // Pink
        CANDY_DOTS.add(new CosmicDot( 0.5,  0.8, 0.18, 2)); // Yellow
        CANDY_DOTS.add(new CosmicDot( 1.1, -0.7, 0.18, 5)); // Purple
        CANDY_DOTS.add(new CosmicDot(-0.3, -0.1, 0.18, 1)); // Orange
        CANDY_DOTS.add(new CosmicDot( 1.2,  0.1, 0.18, 0)); // Pink
    }

    private static final String LUMINANCE_CHARS = ":;=!*#$@▒▓█";
    
    // Geometry bounds for the chocolate rectangle
    private static final double LENGTH = 4.0;  // X axis stretch
    private static final double WIDTH  = 2.2;  // Y axis stretch
    private static final double HEIGHT = 0.6;  // Z axis thickness (Brownie depth)

    private String brownieFudgeColor;
    private String[] cosmicColors;
    private String[][] cellCache;

    private double A = Math.PI / 3.0; // Initial tilt to see the top candy sprinkles right away
    private double B = 0.0;

    public CosmicBrownieLoader() {
        super(BROWNIE_STAGES, 80, 22);
    }

    public CosmicBrownieLoader(int w, int h) {
        super(BROWNIE_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // Locked-in Delicious Fudge Palette
        brownieFudgeColor = "\u001B[38;5;94m"; // Rich Chocolate Fudge Brown

        // Cosmic Candy Sprinkles Palette (Classic bright 256-color ANSI variants)
        cosmicColors = new String[] {
            "\u001B[38;5;205m", // 0: Cosmic Pink
            "\u001B[38;5;208m", // 1: Cosmic Orange
            "\u001B[38;5;226m", // 2: Cosmic Yellow
            "\u001B[38;5;46m",  // 3: Cosmic Green
            "\u001B[38;5;51m",  // 4: Cosmic Blue
            "\u001B[38;5;129m"  // 5: Cosmic Purple
        };

        // Allocate cache: [0] = Fudge Base, [1..6] = Cosmic Sprinkles
        int totalColors = 1 + cosmicColors.length;
        String[] fullPalette = new String[totalColors];
        fullPalette[0] = brownieFudgeColor;
        System.arraycopy(cosmicColors, 0, fullPalette, 1, cosmicColors.length);

        cellCache = new String[totalColors][LUMINANCE_CHARS.length()];
        for (int c = 0; c < totalColors; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = fullPalette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinB = Math.sin(B), cosB = Math.cos(B);

        // Step sizes across the 3D surface grid of the brownie
        double step = 0.01;

        // Render the 6 faces of our rectangular brownie solid
        for (double x = -LENGTH/2; x <= LENGTH/2; x += step) {
            for (double y = -WIDTH/2; y <= WIDTH/2; y += step) {
                // Face 1 & 2: Top and Bottom Caps (Z constant)
                drawPoint(x, y,  HEIGHT/2, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer);
                drawPoint(x, y, -HEIGHT/2, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer);
            }
        }

        for (double x = -LENGTH/2; x <= LENGTH/2; x += step) {
            for (double z = -HEIGHT/2; z <= HEIGHT/2; z += step) {
                // Face 3 & 4: Front and Back Edges (Y constant)
                drawPoint(x,  WIDTH/2, z, 0.0, 1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer);
                drawPoint(x, -WIDTH/2, z, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer);
            }
        }

        for (double y = -WIDTH/2; y <= WIDTH/2; y += step) {
            for (double z = -HEIGHT/2; z <= HEIGHT/2; z += step) {
                // Face 5 & 6: Left and Right Profiles (X constant)
                drawPoint( LENGTH/2, y, z, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer);
                drawPoint(-LENGTH/2, y, z, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer);
            }
        }

        // Render the candy dots on top (semi-spheres sticking out of the brownie top)
        // Sweeping sphere angular parameters (theta, phi)
        for (CosmicDot dot : CANDY_DOTS) {
            for (double t = 0; t < Math.PI; t += 0.2) {         // Elevation (only top hemisphere)
                for (double p = 0; p < 2 * Math.PI; p += 0.2) { // Azimuthal angle
                    // Sphere parametric coordinates localized around dot center
                    double sx = dot.x + dot.r * Math.sin(t) * Math.cos(p);
                    double sy = dot.y + dot.r * Math.sin(t) * Math.sin(p);
                    double sz = (HEIGHT/2) + dot.r * Math.cos(t); // Shifted halfway out of top surface

                    // Surface normal vectors for the hemisphere
                    double nx = Math.sin(t) * Math.cos(p);
                    double ny = Math.sin(t) * Math.sin(p);
                    double nz = Math.cos(t);

                    // Pass color index offset (1 + dot.colorIdx) into the drawer
                    drawPoint(sx, sy, sz, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1 + dot.colorIdx);
                }
            }
        }

        // Spin rates to showcase the spinning brownie
        A += 0.02 * Math.sin(B);
        B += 0.03 * Math.sin(A);
    }

    // Default brownie base drawing utility
    private void drawPoint(double x, double y, double z, double nx, double ny, double nz,
                           double sinA, double cosA, double sinB, double cosB,
                           String[] outputBuffer, double[] zBuffer) {
        drawPoint(x, y, z, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
    }

    // Extended drawer that applies 3D rotation, calculates lighting luminance, and writes to buffer
    private void drawPoint(double x, double y, double z, double nx, double ny, double nz,
                           double sinA, double cosA, double sinB, double cosB,
                           String[] outputBuffer, double[] zBuffer, int colorIndex) {
        
        // 1. 3D Rotation Matrix Calculations (X, Y, Z coordinate transformations)
        double x1 = x * cosB - y * sinB;
        double y1 = x * sinB + y * cosB;
        
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;

        // 2. Dynamic depth calculation and projection scalars
        double distance = 1.6 + (Math.cos(A) + 1.0 * 2.0);  // Zooms in on the top
        double ooZ = 1.0 / (z2 + distance); 

        // 3. Scale projection to fit standard text terminal dimensions (w:80, h:22)
        int xp = (int) (window_width / 2.0 + 35 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 17 * ooZ * y2);

        int bufferIndex = xp + window_width * yp;

        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            // 4. Transform shading normal vector directions
            double nx1 = nx * cosB - ny * sinB;
            double ny1 = nx * sinB + ny * cosB;
            double ny2 = ny1 * cosA - nz * sinA;
            double nz2 = ny1 * sinA + nz * cosA;
            double nx2 = nx1;

            // 5. Compute directional illumination luminance (Simulating lighting source from top-front-right)
            double luminance = nx2 * 0.4 + ny2 * 0.4 + nz2 * 0.8;

            if (ooZ > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooZ;

                // Scale raw luminance scalar safely into available string array index 
                int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));

                outputBuffer[bufferIndex] = cellCache[colorIndex][charIndex];
            }
        }
    }
}
