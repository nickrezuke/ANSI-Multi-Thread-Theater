import java.util.ArrayList;
import java.util.List;

public class ToyBoatLoader extends Loader {
    private static final StatusStage[] BOAT_STAGES = {
            new StatusStage(20, "Carving the toy hull:"),
            new StatusStage(45, "Mounting the captain's cabin:"),
            new StatusStage(70, "Erecting the smokestack:"),
            new StatusStage(90, "Painting the portholes:"),
            new StatusStage(100, "Float tested & Ready!")
    };

    // 1. Keep the portholes defined only by their longitudinal placement (X) and
    // height (Z)
    private static class Porthole {
        double x, z;
        double radius;

        Porthole(double x, double z, double radius) {
            this.x = x;
            this.z = z;
            this.radius = radius;
        }
    }

    private static final List<Porthole> PORTHOLES = new ArrayList<>();
    static {
        // Only track X and Z positions now. One set forward (0.4), one set backward
        // (-0.8).
        PORTHOLES.add(new Porthole(-0.8, 0.1, 0.16));
        PORTHOLES.add(new Porthole(0.4, 0.1, 0.16));
    }

    private static final String LUMINANCE_CHARS = ".,-~:;=!*#$@";

    private static final double HULL_LENGTH = 4.2;
    private static final double HULL_WIDTH = 1.8;
    private static final double HULL_HEIGHT = 0.8;

    private String hullBlue;
    private String hullWhite;
    private String cabinColor;
    private String orangeTrim;
    private String portholeColor;
    private String[][] cellCache;
    private double A = 1.3;
    private double B = 0.0;

    public ToyBoatLoader() {
        super(BOAT_STAGES, 80, 22);
    }

    public ToyBoatLoader(int w, int h) {
        super(BOAT_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        hullBlue = "\u001B[38;5;81m";
        hullWhite = "\u001B[38;5;255m";
        cabinColor = "\u001B[38;5;230m";
        orangeTrim = "\u001B[38;5;214m";
        portholeColor = "\u001B[38;5;18m";

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
        // 1. THE BOAT HULL (True Pointed Bow & Closed Flat Stern)
        // ==========================================
        // Force the starting slice to match the exact mathematical shape of the stern
        // (-HULL_LENGTH / 2)
        double initialNormX = -1.0;
        double initialTaper = 0.65 + 0.35 * (1.0 - Math.pow(initialNormX, 2)); // Starts at exactly 65% width
        double lastActiveWidth = (HULL_WIDTH / 2) * initialTaper;

        for (double x = -HULL_LENGTH / 2; x <= HULL_LENGTH / 2; x += 0.02) {

            // Normalized position from -1.0 (stern) to 1.0 (bow)
            double normX = x / (HULL_LENGTH / 2.0);

            // Single continuous parabolic curve across the entire ship length
            // Dropping 'normX' straight into the bow shift forces a perfect point at 1.0
            double bowTaper = 1.0 - Math.pow((normX + 0.3) / 1.3, 2);
            bowTaper = Math.max(0.0, Math.min(1.0, bowTaper));

            double activeWidth = (HULL_WIDTH / 2) * bowTaper;

            // A: Handle the Back Wall (Stern End-Cap) on the very first cycle
            if (x <= -HULL_LENGTH / 2 + 0.001) {
                for (double z = -HULL_HEIGHT / 2; z <= HULL_HEIGHT / 2 + 0.1; z += 0.05) {
                    int sideColor = (z < 0.1) ? 1 : 0;
                    // Draw a solid flat wall completely spanning the stern width
                    for (double wy = -activeWidth; wy <= activeWidth; wy += 0.01) {
                        drawPoint(x, wy, z, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, sideColor);
                    }
                }
            }

            // B: Render Hull Horizontal Elements (Deck & Floor Profile)
            if (activeWidth > 0.001) {
                for (double y = -activeWidth; y <= activeWidth; y += 0.05) {
                    // Deck Floor
                    drawPoint(x, y, HULL_HEIGHT / 2, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);

                    // Curved Hull Bottom
                    double normY = y / activeWidth;
                    double zBottom = -HULL_HEIGHT / 2 - (0.3 * (1.0 - normY * normY));
                    int hullColorIndex = (zBottom < -0.2) ? 1 : 0;
                    drawPoint(x, y, zBottom, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer,
                            hullColorIndex);
                }
            }

            // C: Render Hull Longitudinal Sides & Taper Fillers
            for (double z = -HULL_HEIGHT / 2; z <= HULL_HEIGHT / 2 + 0.1; z += 0.05) {
                int sideColor = (z < 0.1) ? 1 : 0;

                double yStart = Math.min(activeWidth, lastActiveWidth);
                double yEnd = Math.max(activeWidth, lastActiveWidth);

                // Continuous angled lighting vector based on ship curvature
                double nxValue = -normX * 0.4;

                for (double wy = yStart; wy <= yEnd; wy += 0.01) {
                    // Left flank wall and cap transitions
                    drawPoint(x, -wy, z, nxValue, -0.7, 0.1, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, sideColor);
                    // Right flank wall and cap transitions
                    drawPoint(x, wy, z, nxValue, 0.7, 0.1, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, sideColor);
                }
            }
            lastActiveWidth = activeWidth;
        }

        // ==========================================
        // 2. THE CAPTAIN'S CABIN
        // ==========================================
        double cabinL = 1.6;
        double cabinW = 1.1;
        double cabinH = 0.7;
        double cabinStartX = -0.5;
        for (double cx = cabinStartX - cabinL / 2; cx <= cabinStartX + cabinL / 2; cx += 0.06) {
            for (double cy = -cabinW / 2; cy <= cabinW / 2; cy += 0.06) {
                drawPoint(cx, cy, 0.4 + cabinH, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            }
        }
        for (double cz = 0.4; cz <= 0.4 + cabinH; cz += 0.025) {
            for (double cx = cabinStartX - cabinL / 2; cx <= cabinStartX + cabinL / 2; cx += 0.02) {
                drawPoint(cx, -cabinW / 2, cz, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                drawPoint(cx, cabinW / 2, cz, 0.0, 1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            }
            for (double cy = -cabinW / 2; cy <= cabinW / 2; cy += 0.02) {
                drawPoint(cabinStartX - cabinL / 2, cy, cz, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer,
                        zBuffer, 2);
                drawPoint(cabinStartX + cabinL / 2, cy, cz, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer,
                        zBuffer, 2);
            }
        }

        // ==========================================
        // 3. THE SMOKESTACK
        // ==========================================
        double stackX = cabinStartX - 0.2;
        double stackR = 0.2;
        for (double sz = 1.1; sz <= 1.9; sz += 0.05) {
            int partColor = (sz > 1.7) ? 0 : 3;
            for (double t = 0; t < 2 * Math.PI; t += 0.2) {
                double sx = stackX + stackR * Math.cos(t);
                double sy = stackR * Math.sin(t);
                drawPoint(sx, sy, sz, Math.cos(t), Math.sin(t), 0.1, sinA, cosA, sinB, cosB, outputBuffer, zBuffer,
                        partColor);
            }
        }

        // ==========================================
        // 4. ROUND HULL PORTHOLES (Dynamically Snapped to New Skin)
        // ==========================================
        for (Porthole port : PORTHOLES) {
            // Recompute the exact hull taper at this specific window's X coordinate
            double portNormX = port.x / (HULL_LENGTH / 2.0);
            double portTaper = 1.0 - Math.pow((portNormX + 0.3) / 1.3, 2);
            portTaper = Math.max(0.0, Math.min(1.0, portTaper));

            // Snap the window baseline Y exactly to the new tapered outer side-wall
            double snappedY = (HULL_WIDTH / 2) * portTaper;

            for (double pr = 0.0; pr <= port.radius; pr += 0.03) {
                for (double pa = 0; pa < 2 * Math.PI; pa += 0.2) {
                    double px = port.x + pr * Math.cos(pa);
                    double pz = port.z + pr * Math.sin(pa);

                    // Double circle generation: project one onto the left hull, one onto the right
                    // hull
                    for (int side = -1; side <= 1; side += 2) {
                        // Float slightly outside the skin (+0.015) so it layers clean over the wall
                        // texturing
                        double py = (snappedY * side) + (0.015 * side);

                        // 3 = Orange Trim rim casing, 4 = Deep Blue window glass
                        int col = (pr > port.radius - 0.04) ? 3 : 4;

                        drawPoint(px, py, pz, 0.0, side, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, col);
                    }
                }
            }
        }

        A += 0.010 * Math.sin(B);
        B += 0.025;
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz, double sinA, double cosA,
            double sinB, double cosB, String[] outputBuffer, double[] zBuffer, int colorIndex) {
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
