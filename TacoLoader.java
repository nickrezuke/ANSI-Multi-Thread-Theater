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

    // --- SHELL FOLD GEOMETRY ---
    private static final double TORTILLA_RADIUS = 2.225;
    private static final double OPEN_ANGLE_DEG = 9.0;
    private static final double OPEN_ANGLE_RAD = Math.toRadians(OPEN_ANGLE_DEG);
    private static final double SIN_OPEN = Math.sin(OPEN_ANGLE_RAD);
    private static final double COS_OPEN = Math.cos(OPEN_ANGLE_RAD);

    // Recenters every point before rotation so the model spins in place around
    // its own vertical center instead of orbiting off-screen as it tumbles.
    private static final double Z_CENTER_OFFSET = (TORTILLA_RADIUS * COS_OPEN) / 2.0;

    /**
     * How far up the fold (arc-length from the crease) the shell reaches at a given
     * x.
     */
    private static double wallExtent(double x) {
        double v = TORTILLA_RADIUS * TORTILLA_RADIUS - x * x;
        return v > 0 ? Math.sqrt(v) : 0.0;
    }

    /**
     * Half-width of the topping pile at height s (arc-length up from the crease)
     * for a shell whose rim sits at sMax. Below the rim, toppings are still
     * pinned to the shell's own narrow taper; above it, they're free to billow
     * outward a little - but gently. The old 0.9 multiplier here let the pile
     * grow nearly as fast sideways as it did upward, which is what made it
     * balloon out past the shell entirely instead of just looking loaded.
     */
    private static double moundHalfWidth(double sMax, double s) {
        if (sMax <= 0.0)
            return 0.0;
        double rimHalfWidth = sMax * SIN_OPEN;
        if (s <= sMax) {
            return (s / sMax) * rimHalfWidth;
        }
        double extra = s - sMax;
        return rimHalfWidth + extra * 0.3;
    }

    // --- TOPPINGS --------------------------------------------------------------
    // x = position along the shell's length. sFrac = height as a fraction of
    // however tall the shell is at that x (values above 1.0 sit above the rim,
    // piled into the mound). widthFrac = position across the mound's width, -1..1.
    private static class TacoTopping {
        final double x;
        final double sFrac;
        final double widthFrac;

        TacoTopping(double x, double sFrac, double widthFrac) {
            this.x = x;
            this.sFrac = sFrac;
            this.widthFrac = widthFrac;
        }
    }

    private static final List<TacoTopping> TOMATOES = new ArrayList<>();
    private static final List<TacoTopping> ONIONS = new ArrayList<>();
    private static final List<TacoTopping> CHEESE_STRANDS = new ArrayList<>();
    private static final List<TacoTopping> CHEESE_DRIPS = new ArrayList<>();

    static {
        // Packed densely, but now sitting near/just above the rim instead of
        // towering over it - a loaded taco, not a lettuce explosion.
        for (double x = -1.45; x <= 1.45; x += 0.12) {
            if (wallExtent(x) < 0.4)
                continue;
            TOMATOES.add(new TacoTopping(x, 0.95, 0.55));
            TOMATOES.add(new TacoTopping(x + 0.05, 1.05, -0.35));
            TOMATOES.add(new TacoTopping(x - 0.06, 0.80, 0.10));
            ONIONS.add(new TacoTopping(x - 0.03, 0.90, -0.65));
            ONIONS.add(new TacoTopping(x + 0.07, 0.75, 0.80));
            CHEESE_STRANDS.add(new TacoTopping(x, 1.00, 0.20));
            CHEESE_STRANDS.add(new TacoTopping(x - 0.05, 0.85, -0.85));
            CHEESE_STRANDS.add(new TacoTopping(x + 0.08, 0.70, 0.55));
        }
        for (double x = -1.3; x <= 1.3; x += 0.16) {
            CHEESE_DRIPS.add(new TacoTopping(x, 1.0, 1.0));
            CHEESE_DRIPS.add(new TacoTopping(x + 0.08, 1.0, -1.0));
        }
    }

    private static final String LUMINANCE_CHARS = "+*#&MWB@$";

    // Palette indices into cellCache
    private static final int SHELL = 0;
    private static final int BEEF = 1;
    private static final int CHEESE = 2;
    private static final int TOMATO = 3;
    private static final int LETTUCE = 4;
    private static final int SHELL_TOASTED = 5;
    private static final int SHELL_RIM = 6;
    private static final int SHELL_INNER = 7;
    private static final int ONION = 8;

    private String[][] cellCache;

    // Same coupled tilt/spin + pulsing zoom that CosmicBrownieLoader uses: A and
    // B feed off each other's sine instead of ticking up at independent fixed
    // rates, so the whole taco tumbles and settles rather than spinning at one
    // constant speed. Since camera distance below is also driven by cos(A), it
    // naturally zooms in tight right as the tilt swings into a good angle for
    // looking down into the open shell, then eases back out.
    private double A = Math.PI / 3.0; // Start tilted enough to see inside immediately
    private double B = 0.0;

    public TacoLoader() {
        super(TACO_STAGES, 80, 22);
    }

    public TacoLoader(int w, int h) {
        super(TACO_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        String shellColor = "\u001B[38;5;214m"; // Golden-fried tortilla
        String beefColor = "\u001B[38;5;94m"; // Seasoned ground beef
        String cheeseColor = "\u001B[38;5;220m"; // Cheddar yellow
        String tomatoColor = "\u001B[38;5;196m"; // Tomato red
        String lettuceColor = "\u001B[38;5;46m"; // Lime green shredded lettuce
        String shellRimColor = "\u001B[38;5;94m"; // Crispy, slightly browned edge (darker/warmer than the body)
        String shellToastedColor = "\u001B[38;5;94m"; // Deep dark brown charred/toasted freckles
        String shellInnerColor = "\u001B[38;5;94m"; // Paler doughy underside
        String onionColor = "\u001B[38;5;225m"; // Diced white onion

        String[] fullPalette = {
                shellColor, beefColor, cheeseColor, tomatoColor, lettuceColor,
                shellToastedColor, shellRimColor, shellInnerColor, onionColor
        };
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

        // --- LAYER 1: HINGED SHELL, TWO FLAPS FOLDED OPEN AT OPEN_ANGLE_DEG ---
        double TARGET_ARC_LENGTH = 0.02; // Controls overall density (smaller = denser, fewer gaps)
        double DELTA_R = 0.015; // Uniform radial step size

        for (double r = 0.0; r <= TORTILLA_RADIUS; r += DELTA_R) {
            // Prevent division by zero at the exact center, and scale angular step by
            // radius
            double phiStep = (r < 0.01) ? 0.5 : (TARGET_ARC_LENGTH / r);

            for (double phi = 0; phi < 2 * Math.PI; phi += phiStep) {
                double diskX = r * Math.cos(phi);
                double diskY = r * Math.sin(phi);

                // FIX: Skip the two degenerate corner tips where the rim meets the crease (x = +/- 2.0, y = 0)
                if (r >= TORTILLA_RADIUS - 0.02 && Math.abs(diskY) < 0.03) {
                    continue;
                }

                double side = diskY >= 0 ? 1.0 : -1.0;
                double s = Math.abs(diskY);

                double sx = diskX;
                double sy = side * s * SIN_OPEN;
                double sz = s * COS_OPEN;

                // Smooth the normal near the crease (s < 0.05) to give the bottom a unified downward look
                double smoothSide = (s < 0.05) ? (diskY / 0.05) : side;
                double rawNy = smoothSide * COS_OPEN;
                double rawNz = -SIN_OPEN;
                double nLen = Math.sqrt(rawNy * rawNy + rawNz * rawNz);
                
                double nx = 0.0;
                double ny = rawNy / nLen;
                double nz = rawNz / nLen;

                boolean toasted = hashNoise(Math.floor(sx * 11.0) + side, Math.floor(s * 11.0)) > 0.80;
                boolean atRim = (TORTILLA_RADIUS - r) < 0.09;
                int outerColor = atRim ? SHELL_RIM : (toasted ? SHELL_TOASTED : SHELL);

                drawPoint(sx, sy, sz, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, outerColor);
                drawPoint(sx, sy - ny * 0.05, sz - nz * 0.05, -nx, -ny, -nz, sinA, cosA, sinB, cosB,
                        outputBuffer, zBuffer, SHELL_INNER);
            }
        }

        // --- LAYER 2: SEASONED GROUND BEEF ---
        for (double x = -TORTILLA_RADIUS + 0.35; x <= TORTILLA_RADIUS - 0.35; x += 0.06) {
            double sMax = wallExtent(x);
            if (sMax < 0.35)
                continue;
            double sTop = sMax * 0.8;
            for (double s = sMax * 0.1; s <= sTop; s += 0.05) {
                double halfWidth = moundHalfWidth(sMax, s) * 0.85;
                for (double y = -halfWidth; y <= halfWidth; y += 0.045) {
                    double crumbleX = 0.16 * Math.sin(x * 41.0 + s * 17.0);
                    double crumbleZ = 0.16 * Math.cos(s * 37.0 - x * 13.0);
                    drawPoint(x, y, s * COS_OPEN, crumbleX, 0.0, crumbleZ,
                            sinA, cosA, sinB, cosB, outputBuffer, zBuffer, BEEF);
                }
            }
        }
        // A little beef peeking past the tortilla's own tip - the classic
        // "stuffed a bit too full to fully close" look from the reference photo.
        for (double t = 0; t <= 1.0; t += 0.08) {
            double px = TORTILLA_RADIUS - 0.12 + 0.22 * t;
            double py = 0.12 * Math.sin(t * 11.0);
            double pz = 0.15 + 0.1 * Math.cos(t * 9.0);
            drawPoint(px, py, pz, 0.3, 0.2, 0.9, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, BEEF);
        }

        // --- LAYER 3: SHREDDED LETTUCE, JUST A MODEST POOF ABOVE THE RIM ---
        for (double x = -TORTILLA_RADIUS + 0.15; x <= TORTILLA_RADIUS - 0.15; x += 0.035) {
            double sMax = wallExtent(x);
            if (sMax < 0.5)
                continue;
            // Three overlapping ruffle "leaves" that top out only slightly above
            // the rim (max ~1.05x sMax) - a light garnish, not a fourth pile.
            for (int leaf = 0; leaf < 3; leaf++) {
                double leafSeed = leaf * 7.3;
                double sBase = sMax * (0.75 + 0.15 * leaf);
                double baseHalfWidth = moundHalfWidth(sMax, sBase);
                
                for (double y_idx = -baseHalfWidth; y_idx <= baseHalfWidth; y_idx += 0.025) {
                    double wrinkle = 0.08 * Math.sin(15.0 * x + 10.0 * y_idx + leafSeed)
                            * Math.cos(9.0 * y_idx - leafSeed);
                    double s = Math.max(0.0, sBase + wrinkle);
                    
                    // Scale Y so it never exceeds the shell's width at the new height (s)
                    double actualHalfWidth = moundHalfWidth(sMax, s);
                    double y = (baseHalfWidth > 0) ? (y_idx / baseHalfWidth) * actualHalfWidth : 0.0;

                    drawPoint(x, y, s * COS_OPEN, 0.0, 0.0, 1.0,
                            sinA, cosA, sinB, cosB, outputBuffer, zBuffer, LETTUCE);
                }
            }
        }

        // --- LAYER 4: DICED TOMATOES ---
        for (TacoTopping tom : TOMATOES) {
            double sMax = wallExtent(tom.x);
            if (sMax <= 0)
                continue;
            double s = tom.sFrac * sMax;
            double cy = tom.widthFrac * moundHalfWidth(sMax, s);
            double cz = s * COS_OPEN;
            for (double dx = -0.07; dx <= 0.07; dx += 0.045) {
                for (double dy = -0.07; dy <= 0.07; dy += 0.045) {
                    for (double dz = -0.07; dz <= 0.07; dz += 0.045) {
                        drawPoint(tom.x + dx, cy + dy, cz + dz, 0.0, 0.0, 1.0,
                                sinA, cosA, sinB, cosB, outputBuffer, zBuffer, TOMATO);
                    }
                }
            }
        }

        // --- LAYER 5: DICED WHITE ONION ---
        for (TacoTopping onion : ONIONS) {
            double sMax = wallExtent(onion.x);
            if (sMax <= 0)
                continue;
            double s = onion.sFrac * sMax;
            double cy = onion.widthFrac * moundHalfWidth(sMax, s);
            double cz = s * COS_OPEN;
            drawPoint(onion.x, cy, cz, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, ONION);
            drawPoint(onion.x + 0.05, cy + 0.03, cz + 0.02, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB,
                    outputBuffer, zBuffer, ONION);
        }

        // --- LAYER 6: CHEDDAR CHEESE STRANDS, WOVEN THROUGH THE PILE ---
        for (TacoTopping strand : CHEESE_STRANDS) {
            double sMax = wallExtent(strand.x);
            if (sMax <= 0)
                continue;
            double sBase = strand.sFrac * sMax;
            double yBase = strand.widthFrac * moundHalfWidth(sMax, sBase);
            double zBase = sBase * COS_OPEN;
            for (double len = -0.1; len <= 0.1; len += 0.035) {
                drawPoint(strand.x + len * 0.4, yBase + len * 0.7, zBase + len * 0.45, 0.0, 0.0, 1.0,
                        sinA, cosA, sinB, cosB, outputBuffer, zBuffer, CHEESE);
            }
        }

        // --- LAYER 7: MELTED CHEESE DRAPING DOWN OVER THE OUTER SHELL ---
        for (TacoTopping drip : CHEESE_DRIPS) {
            double sMax = wallExtent(drip.x);
            if (sMax <= 0)
                continue;
            double rimHalfWidth = moundHalfWidth(sMax, sMax);
            double side = drip.widthFrac;
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                double y = side * (rimHalfWidth + 0.28 * t);
                double z = (sMax * COS_OPEN) - 0.18 * t * t;
                drawPoint(drip.x, y, z, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, CHEESE);
            }
        }

        // Coupled tilt/spin, copied from CosmicBrownieLoader: A and B drive
        // each other's rate of change instead of ticking up independently.
        A += 0.02 * Math.sin(B);
        B += 0.03 * Math.sin(A);
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz,
            double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer, int colorIndex) {

        double x1 = x * cosB - y * sinB;
        double y1 = x * sinB + y * cosB;

        double zc = z - Z_CENTER_OFFSET; // recenter vertically so it spins in place

        double y2 = y1 * cosA - zc * sinA;
        double z2 = y1 * sinA + zc * cosA;
        double x2 = x1;

        double distance = 3.0 + Math.cos(A);
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

    // Cheap positional hash (no allocation, no state) used to give the shell a
    // stable freckle pattern instead of true per-frame randomness, which would
    // flicker distractingly at 60fps.
    private static double hashNoise(double a, double b) {
        double n = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453;
        return n - Math.floor(n);
    }
}