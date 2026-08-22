// TODO Make this better and fix the rotation

import java.util.ArrayList;
import java.util.List;

public class TacoLoader extends Loader {
    private static final StatusStage[] TACO_STAGES = {
        new StatusStage(20, "Flipping flat corn tortillas:"),
        new StatusStage(45, "Crisping the folded shell:"),
        new StatusStage(70, "Stuffing seasoned beef:"),
        new StatusStage(90, "Layering cheddar and salsa:"),
        new StatusStage(100, "Crunchy & Ready!")
    };

    private static class TacoTopping {
        double x, y, z;
        TacoTopping(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    private static final List<TacoTopping> TOMATOES = new ArrayList<>();
    private static final List<TacoTopping> CHEESE_STRANDS = new ArrayList<>();

    static {
        // Scatter toppings cluster-style toward the wider middle opening (X: -1.2 to 1.2)
        // They narrow down as they reach the pinched corners
        for (double x = -1.2; x <= 1.2; x += 0.35) {
            double maxWidth = 0.6 * (1.0 - (x*x)/4.0); // Widest at center
            if (maxWidth > 0.1) {
                TOMATOES.add(new TacoTopping(x,  maxWidth * 0.4, 0.9));
                TOMATOES.add(new TacoTopping(x, -maxWidth * 0.3, 1.0));
                
                CHEESE_STRANDS.add(new TacoTopping(x,  maxWidth * 0.2, 1.1));
                CHEESE_STRANDS.add(new TacoTopping(x, -maxWidth * 0.5, 0.8));
            }
        }
    }

    private static final String LUMINANCE_CHARS = ".,-~:;=!*#$@";
    
    // Tortilla Disc Radius
    private static final double TORTILLA_RADIUS = 2.0;
    private static final double SHELL_GAP = 0.45; // Separation width between left and right walls

    private String shellColor;
    private String beefColor;
    private String cheeseColor;
    private String tomatoColor;
    private String lettuceColor;
    private String[][] cellCache;

    private double A = 0.4; // Tilted to see inside the top opening
    private double B = 0.0;

    public TacoLoader() {
        super(TACO_STAGES, 80, 22);
    }

    public TacoLoader(int w, int h) {
        super(TACO_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        shellColor   = "\u001B[38;5;214m"; // Crispy Yellow-Orange Tortilla
        beefColor    = "\u001B[38;5;94m";  // Seasoned Ground Beef
        cheeseColor  = "\u001B[38;5;220m"; // Cheddar Yellow
        tomatoColor  = "\u001B[38;5;196m"; // Tomato Red
        lettuceColor = "\u001B[38;5;46m";  // Lime Green Lettuce

        String[] fullPalette = { shellColor, beefColor, cheeseColor, tomatoColor, lettuceColor };
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

        // --- LAYER 1: TRUE FOLDED CIRCULAR DISC SHELL ---
        // Sweep a full flat circle grid mapping radius (r) and circle angle (phi)
        for (double r = 0.0; r <= TORTILLA_RADIUS; r += 0.06) {
            for (double phi = 0; phi < 2 * Math.PI; phi += 0.05) {
                // Flat circular disc coordinates before folding
                double diskX = r * Math.cos(phi);
                double diskY = r * Math.sin(phi);

                // Fold logic: Keep X running straight along the bottom crease.
                // Fold the vertical component up into the Z axis.
                double sx = diskX;
                double sy = Math.signum(diskY) * SHELL_GAP / 2.0; // Separate walls slightly for fillings
                double sz = Math.abs(diskY);                     // Folded upward!

                // Smooth out the bottom fold slightly so it isn't an infinitely sharp razor edge
                if (sz < 0.25) {
                    double transition = sz / 0.25;
                    sy = (diskY / 0.25) * (SHELL_GAP / 2.0); 
                }

                // Mathematical normals of a folded vertical shell
                double nx = 0.0;
                double ny = (diskY >= 0) ? 1.0 : -1.0;
                double nz = 0.1; 
                if (sz < 0.25) { // Normal adjustments along the curved base crease
                    ny = Math.sin(phi);
                    nz = -Math.cos(phi);
                }

                // Render outer skin and inner thickness skin
                drawPoint(sx, sy, sz, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
                drawPoint(sx, sy - 0.03 * Math.signum(sy), sz, -nx, -ny, -nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // --- LAYER 2: PINCHED GROUND BEEF FILLING ---
        // Fits perfectly inside the tapering cavity
        for (double x = -TORTILLA_RADIUS + 0.3; x <= TORTILLA_RADIUS - 0.3; x += 0.07) {
            // Find the active shell height boundary at this coordinate
            double maxZBoundary = Math.sqrt(Math.max(0.01, TORTILLA_RADIUS * TORTILLA_RADIUS - x * x));
            double fillHeight = Math.min(0.5, maxZBoundary * 0.5);

            for (double z = 0.1; z <= fillHeight; z += 0.05) {
                // Width limits pinch closer together near the tips
                double currentWidth = SHELL_GAP / 2.0;
                for (double y = -currentWidth + 0.05; y <= currentWidth - 0.05; y += 0.05) {
                    double noiseX = 0.2 * Math.sin(x * 35.0);
                    double noiseZ = 0.2 * Math.cos(z * 35.0);
                    drawPoint(x, y, z, noiseX, 0.0, noiseZ, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1); // 1 = Beef
                }
            }
        }

        // --- LAYER 3: RUFFLED SHREDDED LETTUCE ---
        // Sits on top of the beef and cascades out of the center opening
        for (double x = -TORTILLA_RADIUS + 0.2; x <= TORTILLA_RADIUS - 0.2; x += 0.05) {
            double maxZBoundary = Math.sqrt(Math.max(0.01, TORTILLA_RADIUS * TORTILLA_RADIUS - x * x));
            double lettuceBaseZ = Math.min(0.5, maxZBoundary * 0.5);
            
            // Generate lettuce only where the shell is tall enough
            if (maxZBoundary > 0.4) {
                double currentWidth = SHELL_GAP / 1.5;
                for (double y = -currentWidth; y <= currentWidth; y += 0.12) {
                    double wrinkle = 0.1 * Math.sin(16.0 * x) * Math.cos(10.0 * y);
                    double z = lettuceBaseZ + 0.15 + wrinkle;

                    // Ensure lettuce follows the circular drop off near corners
                    if (z < maxZBoundary + 0.1) {
                        drawPoint(x, y, z, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 4); // 4 = Lettuce
                    }
                }
            }
        }

        // --- LAYER 4: DICED TOMATOES (PINCHED ALIGNMENT) ---
        for (TacoTopping tom : TOMATOES) {
            for (double dx = -0.06; dx <= 0.06; dx += 0.04) {
                for (double dy = -0.06; dy <= 0.06; dy += 0.04) {
                    for (double dz = -0.06; dz <= 0.06; dz += 0.04) {
                        drawPoint(tom.x + dx, tom.y + dy, tom.z + dz, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3); // 3 = Tomato
                    }
                }
            }
        }

        // --- LAYER 5: CHEDDAR CHEESE STRANDS ---
        for (TacoTopping strand : CHEESE_STRANDS) {
            for (double len = -0.1; len <= 0.1; len += 0.04) {
                drawPoint(strand.x + len * 0.4, strand.y + len * 0.7, strand.z + len * 0.4, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2); // 2 = Cheese
            }
        }

        A += 0.015;
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

        double distance = 4.8; 
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
