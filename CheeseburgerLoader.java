import java.util.ArrayList;
import java.util.List;

public class CheeseburgerLoader extends Loader {
    // 1. Cheeseburger specific loading stages
    private static final StatusStage[] BURGER_STAGES = {
            new StatusStage(15, "Grilling the prime beef patty:"),
            new StatusStage(35, "Melting sharp cheddar cheese:"),
            new StatusStage(55, "Slicing tomatoes and onions:"),
            new StatusStage(75, "Ripping crisp green lettuce:"),
            new StatusStage(90, "Toasting the sesame bun:"),
            new StatusStage(100, "Stacked high & Ready!")
    };

    // Tracker for static sesame seed coordinates (Spherical angles on the top bun
    // dome)
    private static class SesameSeed {
        double theta, phi;

        SesameSeed(double theta, double phi) {
            this.theta = theta;
            this.phi = phi;
        }
    }

    private static final List<SesameSeed> SESAME_SEEDS = new ArrayList<>();
    static {
        // Uniformly scatter seeds across the dome surface of the top bun
        SESAME_SEEDS.add(new SesameSeed(0.2, 0.5));
        SESAME_SEEDS.add(new SesameSeed(0.4, 2.1));
        SESAME_SEEDS.add(new SesameSeed(0.3, 4.3));
        SESAME_SEEDS.add(new SesameSeed(0.6, 1.2));
        SESAME_SEEDS.add(new SesameSeed(0.7, 3.1));
        SESAME_SEEDS.add(new SesameSeed(0.5, 5.5));
        SESAME_SEEDS.add(new SesameSeed(0.9, 0.2));
        SESAME_SEEDS.add(new SesameSeed(0.8, 2.5));
        SESAME_SEEDS.add(new SesameSeed(1.1, 4.0));
        SESAME_SEEDS.add(new SesameSeed(1.0, 1.8));
        SESAME_SEEDS.add(new SesameSeed(1.2, 5.0));
        SESAME_SEEDS.add(new SesameSeed(0.5, 0.9));
        SESAME_SEEDS.add(new SesameSeed(0.8, 4.8));
        SESAME_SEEDS.add(new SesameSeed(1.3, 2.9));
    }

    private static final String LUMINANCE_CHARS = ":;=!*#$@▒▓█";

    // Burger Scale Geometry Constants
    private static final double BURGER_RADIUS = 2.0;
    private static final double CHEESE_SIZE = 3.9; // Larger than diameter so corners stick out dramatically

    private String bunColor;
    private String pattyColor;
    private String cheeseColor;
    private String tomatoColor;
    private String onionColor;
    private String lettuceColor;
    private String seedColor;
    private String[][] cellCache;

    private double A = 1.15; // Initial tilt
    private double B = 0.0;

    public CheeseburgerLoader() {
        super(BURGER_STAGES, 80, 22);
    }

    public CheeseburgerLoader(int w, int h) {
        super(BURGER_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // High-Fidelity Multi-Ingredient Palette Matrix
        bunColor = "\u001B[38;5;215m"; // Toasted Golden-Brown Bun
        pattyColor = "\u001B[38;5;94m"; // Flame-Broiled Beef Brown
        cheeseColor = "\u001B[38;5;220m"; // Melted Cheddar Yellow
        tomatoColor = "\u001B[38;5;196m"; // Juicy Tomato Red
        onionColor = "\u001B[38;5;255m"; // Crisp Onion White
        lettuceColor = "\u001B[38;5;46m"; // Leafy Green Lettuce
        seedColor = "\u001B[38;5;229m"; // Small Sesame White-Cream

        String[] fullPalette = { bunColor, pattyColor, cheeseColor, tomatoColor, onionColor, lettuceColor, seedColor };
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
        // 1. THE BOTTOM BUN (Z: -1.3 to -0.9)
        // ==========================================
        for (double r = 0; r <= BURGER_RADIUS; r += 0.08) {
            for (double t = 0; t < 2 * Math.PI; t += 0.05) {
                double x = r * Math.cos(t);
                double y = r * Math.sin(t);
                // Bottom flat face
                drawPoint(x, y, -1.3, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
                // Top flat face
                drawPoint(x, y, -0.9, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }
        // Bottom bun outer rim
        for (double z = -1.3; z <= -0.9; z += 0.02) {
            for (double t = 0; t < 2 * Math.PI; t += 0.02) {
                double x = BURGER_RADIUS * Math.cos(t);
                double y = BURGER_RADIUS * Math.sin(t);
                drawPoint(x, y, z, Math.cos(t), Math.sin(t), 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // ==========================================
        // 2. THE MEAT PATTY (Z: -0.8 to -0.3)
        // ==========================================
        double pattyR = BURGER_RADIUS * 1.05; // Patty leaks slightly wider than the bun
        for (double r = 0; r <= pattyR; r += 0.08) {
            for (double t = 0; t < 2 * Math.PI; t += 0.1) {
                double x = r * Math.cos(t);
                double y = r * Math.sin(t);
                drawPoint(x, y, -0.8, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
                drawPoint(x, y, -0.3, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }
        // Meat patty textured edge walls
        for (double z = -0.8; z <= -0.3; z += 0.02) {
            for (double t = 0; t < 2 * Math.PI; t += 0.02) {
                double noise = 1.0 + 0.03 * Math.sin(16 * t); // Gives the grilled edges structural meat texture
                double x = pattyR * noise * Math.cos(t);
                double y = pattyR * noise * Math.sin(t);
                drawPoint(x, y, z, Math.cos(t), Math.sin(t), 0.1, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
            }
        }

        // ==========================================
        // 3. SQUARE CHEESE LAYER (MELTED DROOP MOD)
        // ==========================================
        double cAngle = Math.PI / 4.0;
        double cosC = Math.cos(cAngle), sinC = Math.sin(cAngle);
        double basePattyRadius = BURGER_RADIUS * 1.05; // The threshold where cheese starts hanging over

        for (double cx = -CHEESE_SIZE / 2; cx <= CHEESE_SIZE / 2; cx += 0.06) {
            for (double cy = -CHEESE_SIZE / 2; cy <= CHEESE_SIZE / 2; cy += 0.06) {
                // Apply 2D cheese rotation transformation
                double x = cx * cosC - cy * sinC;
                double y = cx * sinC + cy * cosC;

                double r = Math.sqrt(x * x + y * y);

                // Default flat cheese height
                double z = -0.22;
                double nx = 0.0;
                double ny = 0.0;
                double nz = 1.0;

                // If the point is outside the patty radius, droop it downward!
                if (r > basePattyRadius) {
                    double overhang = r - basePattyRadius;

                    // Progressively push the Z coordinate down the further it hangs over
                    z -= 0.6 * Math.pow(overhang, 1.5);

                    // Alter normals to follow the curve of the droop 
                    // (pointing outward and slightly up)
                    double normScale = 0.4 * overhang;
                    nx = (x / r) * normScale;
                    ny = (y / r) * normScale;
                    nz = 1.0 - normScale; // Flattening vector component
                }

                // Draw the beautifully draped cheese surface
                drawPoint(x, y, z, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2); // 2 = Cheese
            }
        }

        // ==========================================
        // 4. CRISP MODULATED LETTUCE (Z: -0.15 to 0.05)
        // ==========================================
        for (double r = 0; r <= BURGER_RADIUS * 1.1; r += 0.08) {
            for (double t = 0; t < 2 * Math.PI; t += 0.06) {
                double x = r * Math.cos(t);
                double y = r * Math.sin(t);
                // Modulating Z height using high frequency sin curves creates a realistic
                // wrinkled leaf silhouette
                double leafWrinkle = 0.08 * Math.sin(8 * t) * (r / BURGER_RADIUS);
                double z = -0.05 + leafWrinkle;

                // Leaf surface normals tracking ruffled lighting fields
                double nz = 0.9;
                double nx = -0.1 * Math.cos(8 * t);

                drawPoint(x, y, z, nx, 0.0, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 5); // 5 = Lettuce
            }
        }

        // ==========================================
        // 5. THE SLICED ONION RINGS (Z: 0.15)
        // ==========================================
        // Modeled as two nested hollow rings sitting side-by-side
        for (double t = 0; t < 2 * Math.PI; t += 0.05) {
            double cosT = Math.cos(t), sinT = Math.sin(t);
            // Ring thickness sweeps
            for (double w = 0.0; w <= 0.15; w += 0.03) {
                double r1 = 0.8 + w;
                double r2 = 0.7 + w;

                // Ring 1 Center Offset Left
                double x1 = -0.4 + r1 * cosT;
                double y1 = 0.2 + r1 * sinT;
                drawPoint(x1, y1, 0.15, cosT, sinT, 0.5, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 4); // 4 = Onion

                // Ring 2 Center Offset Right
                double x2 = 0.5 + r2 * cosT;
                double y2 = -0.3 + r2 * sinT;
                drawPoint(x2, y2, 0.15, cosT, sinT, 0.5, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 4);
            }
        }

        // ==========================================
        // 6. JUICY TOMATO DISCS (Z: 0.25 to 0.45)
        // ==========================================
        // Render two individual thick tomato discs offset across the cross section
        double tomatoRadius = 0.85;
        double[][] tomatoCenters = { { -0.6, -0.3 }, { 0.6, 0.3 } };
        for (double[] center : tomatoCenters) {
            for (double r = 0; r <= tomatoRadius; r += 0.06) {
                for (double t = 0; t < 2 * Math.PI; t += 0.08) {
                    double x = center[0] + r * Math.cos(t);
                    double y = center[1] + r * Math.sin(t);
                    drawPoint(x, y, 0.25, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3);
                    drawPoint(x, y, 0.45, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3); // 3 =
                                                                                                            // Tomato
                }
            }
            // Tomato outer walls
            for (double z = 0.25; z <= 0.45; z += 0.04) {
                for (double t = 0; t < 2 * Math.PI; t += 0.06) {
                    double x = center[0] + tomatoRadius * Math.cos(t);
                    double y = center[1] + tomatoRadius * Math.sin(t);
                    drawPoint(x, y, z, Math.cos(t), Math.sin(t), 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 3);
                }
            }
        }
        // ==========================================
        // 7. THE TOP SESAME BUN DOME (Z: 0.5 to 1.5)
        // ==========================================
        // Modeled as a true 3D hemisphere dome shell
        double bunHeightScale = 1.0;
        // Squashes or stretches the top dome depth height
        for (double theta = 0.0; theta <= Math.PI / 2.0; theta += 0.04) {
            // Elevation angle (top half)
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.025) {
                // Azimuth angle sweep
                double sinT = Math.sin(theta), cosT = Math.cos(theta);
                double sinP = Math.sin(phi), cosP = Math.cos(phi);
                double x = BURGER_RADIUS * sinT * cosP;
                double y = BURGER_RADIUS * sinT * sinP;
                double z = 0.5 + bunHeightScale * cosT;
                // Lifted up on top of the sandwich stack
                // Hemispherical lighting directional normal vectors
                double nx = sinT * cosP;
                double ny = sinT * sinP;
                double nz = cosT;
                drawPoint(x, y, z, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
                // 0 = Bun
            }
        }
        // Base closure plate underneath the top bun cavity curve
        for (double r = 0; r <= BURGER_RADIUS; r += 0.08) {
            for (double t = 0; t < 2 * Math.PI; t += 0.1) {
                drawPoint(r * Math.cos(t), r * Math.sin(t), 0.5, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer,
                        zBuffer, 0);
            }
        }
        // ==========================================
        // 8. CRUNCHY SESAME SEEDS (Overlay on Top Bun Dome Surface)
        // ==========================================
        for (SesameSeed seed : SESAME_SEEDS) {
            double sinT = Math.sin(seed.theta), cosT = Math.cos(seed.theta);
            double sinP = Math.sin(seed.phi), cosP = Math.cos(seed.phi);
            // Compute local surface anchoring coordinates right on the bun's skin
            double sx = BURGER_RADIUS * sinT * cosP;
            double sy = BURGER_RADIUS * sinT * sinP;
            double sz = 0.5 + bunHeightScale * cosT + 0.03;
            // Lifted minimally to resolve depth fighting
            // Small seed structural volumetric thickness loops
            for (double r = 0; r <= 0.05; r += 0.02) {
                for (double sa = 0; sa < 2 * Math.PI; sa += 1.0) {
                    double seedX = sx + r * Math.cos(sa);
                    double seedY = sy + r * Math.sin(sa);
                    drawPoint(seedX, seedY, sz, sinT * cosP, sinT * sinP, cosT, sinA, cosA, sinB, cosB, outputBuffer,
                            zBuffer, 6);
                    // 6 = Sesame Seed
                }
            }
        }
        // Pitch/yaw rotation animation step rates
        A += 0.015 * Math.sin(B) * 1.2;
        B += 0.025;
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz, double sinA, double cosA,
            double sinB, double cosB, String[] outputBuffer, double[] zBuffer, int colorIndex) {
        // Center of mass rotation alignment (Already symmetrical around center axis)
        double shiftedX = x - 0;
        // Execute core 3D matrix coordinate transformations
        double x1 = shiftedX * cosB - y * sinB;
        double y1 = shiftedX * sinB + y * cosB;
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;
        double distance = 4.2;
        double ooZ = 1.0 / (z2 + distance);
        // Project calculations out onto terminal aspect ratio grid limits (80x22 layout
        // layout)
        int xp = (int) (window_width / 2.0 + 35 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 15 * ooZ * y2);
        int bufferIndex = xp + window_width * yp;
        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            // Apply pitch/yaw onto surface light shading normal maps
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