// TODO: are the tips hollowed out??

import java.util.ArrayList;
import java.util.List;

public class ToyBoatLoader extends Loader {
    // 1. Toy Boat specific loading stages
    private static final StatusStage[] BOAT_STAGES = {
        new StatusStage(20, "Carving the toy hull:"),
        new StatusStage(45, "Mounting the captain's cabin:"),
        new StatusStage(70, "Erecting the smokestack:"),
        new StatusStage(90, "Painting the portholes:"),
        new StatusStage(100, "Float tested & Ready!")
    };

    // Tracker for porthole positions on the sides of the blue hull
    private static class Porthole {
        double x, y, z;
        double radius;
        Porthole(double x, double y, double z, double radius) {
            this.x = x; this.y = y; this.z = z; this.radius = radius;
        }
    }

    private static final List<Porthole> PORTHOLES = new ArrayList<>();
    static {
        // Scattered on the left and right flanks of the hull
        PORTHOLES.add(new Porthole(-0.8,  0.85, 0.1, 0.16));
        PORTHOLES.add(new Porthole( 0.4,  0.85, 0.1, 0.16));
        PORTHOLES.add(new Porthole(-0.8, -0.85, 0.1, 0.16));
        PORTHOLES.add(new Porthole( 0.4, -0.85, 0.1, 0.16));
    }

    private static final String LUMINANCE_CHARS = ".,-~:;=!*#$@";
    
    // Boat Scale Geometry Constants
    private static final double HULL_LENGTH = 4.2;
    private static final double HULL_WIDTH  = 1.8;
    private static final double HULL_HEIGHT = 0.8;

    private String hullBlue;
    private String hullWhite;
    private String cabinColor;
    private String orangeTrim;
    private String portholeColor;
    private String[][] cellCache;

    private double A = 1.3; // Tilted angle to view the deck and cabin clearly
    private double B = 0.0;

    public ToyBoatLoader() {
        super(BOAT_STAGES, 80, 22);
    }

    public ToyBoatLoader(int w, int h) {
        super(BOAT_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // Vibrant Toy-Store Palette
        hullBlue      = "\u001B[38;5;81m";  // Sky Blue Upper Hull
        hullWhite     = "\u001B[38;5;255m"; // Pure White Lower Hull / Accents
        cabinColor    = "\u001B[38;5;230m"; // Soft Cream Cabin
        orangeTrim    = "\u001B[38;5;214m"; // Safety Orange Mast & Trim
        portholeColor = "\u001B[38;5;18m";  // Deep Oceanic Blue Windows

        String[] fullPalette = { hullBlue, hullWhite, cabinColor, orangeTrim, portholeColor };
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
        // 1. THE BOAT HULL (Rounded tub design)
        // ==========================================
        for (double x = -HULL_LENGTH/2; x <= HULL_LENGTH/2; x += 0.02) {
            // Taper the width at the bow (front) and stern (back) to make a boat silhouette
            double bowTaper = 1.0 - Math.pow(x / (HULL_LENGTH / 1.8), 2);
            bowTaper = Math.max(0.3, Math.min(1.0, bowTaper));
            double activeWidth = (HULL_WIDTH / 2) * bowTaper;

            for (double y = -activeWidth; y <= activeWidth; y += 0.05) {
                // Flat deck floor (Z = HULL_HEIGHT / 2)
                drawPoint(x, y, HULL_HEIGHT/2, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1); // 1 = White Deck

                // Curved bottom hull profile
                double normY = y / activeWidth;
                double zBottom = -HULL_HEIGHT / 2 - (0.3 * (1.0 - normY * normY));
                
                // Color splits: lower half of the hull is White/Yellow, upper rim is Blue
                int hullColorIndex = (zBottom < -0.2) ? 1 : 0; 
                drawPoint(x, y, zBottom, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, hullColorIndex);
            }

            // Outer Hull Side Walls
            for (double z = -HULL_HEIGHT/2; z <= HULL_HEIGHT/2 + 0.1; z += 0.05) {
                int sideColor = (z < 0.1) ? 1 : 0; // 1 = White bottom, 0 = Blue top rim
                
                // Left flank wall
                double leftY = -activeWidth;
                drawPoint(x, leftY, z, 0.0, -1.0, 0.1, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, sideColor);
                
                // Right flank wall
                double rightY = activeWidth;
                drawPoint(x, rightY, z, 0.0, 1.0, 0.1, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, sideColor);
            }
        }

        // ==========================================
        // 2. THE CAPTAIN'S CABIN (Z: 0.4 to 1.1)
        // ==========================================
        double cabinL = 1.6;
        double cabinW = 1.1;
        double cabinH = 0.7;
        double cabinStartX = -0.5; // Offset slightly backward on the deck

        for (double cx = cabinStartX - cabinL/2; cx <= cabinStartX + cabinL/2; cx += 0.06) {
            for (double cy = -cabinW/2; cy <= cabinW/2; cy += 0.06) {
                // Cabin Roof
                drawPoint(cx, cy, 0.4 + cabinH, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2); // 2 = Cream Cabin
            }
        }
        // Cabin Vertical Walls
        for (double cz = 0.4; cz <= 0.4 + cabinH; cz += 0.025) {
            for (double cx = cabinStartX - cabinL/2; cx <= cabinStartX + cabinL/2; cx += 0.02) {
                drawPoint(cx, -cabinW/2, cz, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                drawPoint(cx,  cabinW/2, cz, 0.0,  1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            }
            for (double cy = -cabinW/2; cy <= cabinW/2; cy += 0.02) {
                drawPoint(cabinStartX - cabinL/2, cy, cz, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                drawPoint(cabinStartX + cabinL/2, cy, cz,  1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            }
        }

        // ==========================================
        // 3. THE SMOKESTACK / MAST (Z: 1.1 to 1.9)
        // ==========================================
        double stackX = cabinStartX - 0.2; // Anchored directly on top of the cabin
        double stackR = 0.2;
        for (double sz = 1.1; sz <= 1.9; sz += 0.05) {
            int partColor = (sz > 1.7) ? 0 : 3; // Give it that classic dual-tone blue tip over orange stack!
            for (double t = 0; t < 2 * Math.PI; t += 0.2) {
                double sx = stackX + stackR * Math.cos(t);
                double sy = stackR * Math.sin(t);
                drawPoint(sx, sy, sz, Math.cos(t), Math.sin(t), 0.1, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, partColor);
            }
        }

        // ==========================================
        // 4. ROUND HULL PORTHOLES (Intercepts the side walls)
        // ==========================================
        for (Porthole port : PORTHOLES) {
            for (double pr = 0.0; pr <= port.radius; pr += 0.03) {
                for (double pa = 0; pa < 2 * Math.PI; pa += 0.2) {
                    double px = port.x + pr * Math.cos(pa);
                    double py = port.y + 0.02 * (port.y > 0 ? 1 : -1); // Float slightly outside the hull skin
                    double pz = port.z + pr * Math.sin(pa);

                    // Porthole dark interior window vs orange ring casing
                    int col = (pr > port.radius - 0.04) ? 3 : 4; // 3 = Orange Trim rim, 4 = Blue window glass

                    drawPoint(px, py, pz, 0.0, (port.y > 0 ? 1.0 : -1.0), 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, col);
                }
            }
        }

        // Rolling sea rotation step values
        A += 0.010 * Math.sin(B);
        B += 0.025;
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz,
                           double sinA, double cosA, double sinB, double cosB,
                           String[] outputBuffer, double[] zBuffer, int colorIndex) {
        
        double x1 = x * cosB - y * sinB;
        double y1 = x * sinB + y * cosB;
        
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;

        double distance = 3.0; 
        double ooZ = 1.0 / (z2 + distance);

        int xp = (int) (window_width / 2.0 + 35 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 15 * ooZ * y2);

        int bufferIndex = xp + window_width * yp;

        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            double nx1 = nx * cosB - ny * sinB;
            double ny1 = nx * sinB + ny * cosB;
            double ny2 = ny1 * cosA - nz * sinA;
            double nz2 = ny1 * sinA + nz * cosA;
            double nx2 = nx1;

            double luminance = nx2 * 0.3 + ny2 * 0.3 + nz2 * 0.9;

            if (ooZ > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooZ;

                int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));

                outputBuffer[bufferIndex] = cellCache[colorIndex][charIndex];
            }
        }
    }
}
