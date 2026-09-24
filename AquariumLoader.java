import java.util.Arrays;

public class AquariumLoader extends Loader {
    private static final StatusStage[] AQUA_STAGES = {
        new StatusStage(25, "Filtering aquatic water columns:"),
        new StatusStage(50, "Laying down multicolored gravel substrate:"),
        new StatusStage(75, "Planting dynamic kelp & configuring air stones:"),
        new StatusStage(100, "Aquarium Environment Active!")
    };

    private static final int WIDTH   = 80;
    private static final int HEIGHT  = 22;
    private static final int FLOOR_Y = 19; // first row of gravel

    // Layer depths (higher = closer to the viewer)
    private static final double Z_GRAVEL = 0.90;
    private static final double Z_HOUSE  = 0.80;
    private static final double Z_KELP   = 0.75;
    private static final double Z_FISH_A = 0.72;
    private static final double Z_FISH_B = 0.70;
    private static final double Z_BUBBLE = 0.65;
    private static final double Z_WATER  = 0.01;

    private double timeClock = 0.0;

    // Water gradient stops (top / middle / bottom of the tank)
    private static final int[] RGB_WATER_TOP = { 60,  180, 240 };
    private static final int[] RGB_WATER_MID = { 20,  130, 200 };
    private static final int[] RGB_WATER_BTM = { 0,   70,  140 };
    private static final int[] RGB_BUBBLE    = { 220, 245, 255 };

    // Orange tropical fish
    private static final int[] RGB_FISH_CORE  = { 255, 110, 20 };
    private static final int[] RGB_FISH_BELLY = { 255, 215, 0  };

    // Fish house
    private static final int HOUSE_X    = 65;
    private static final int HOUSE_TOP  = 11;
    private static final int HOUSE_BASE = FLOOR_Y - 1;
    private static final int[] RGB_ROCK      = { 122, 116, 110 };
    private static final int[] RGB_ROOF      = { 182, 88,  62  };
    private static final int[] RGB_LINTEL    = { 205, 195, 175 };
    private static final int[] RGB_MOSS      = { 70,  150, 70  };
    private static final int[] RGB_DOOR_TOP  = { 8,   14,  26  };
    private static final int[] RGB_DOOR_BTM  = { 18,  32,  52  };
    private static final int[] RGB_WINDOW    = { 255, 205, 90  };
    private static final int[] RGB_EYES      = { 255, 236, 130 };

    // Gravel, kelp, air stones
    private static final int[][] PEBBLES = {
        { 172, 150, 120 }, { 140, 138, 135 }, { 196, 120, 84 }, { 96, 110, 128 },
        { 214, 196, 150 }, { 120, 150, 110 }, { 170, 90,  90 }, { 230, 225, 215 }
    };
    private static final int[] RGB_AIR_STONE = { 190, 205, 215 };
    private static final int[] RGB_KELP_DARK  = { 16,  100, 46 };
    private static final int[] RGB_KELP_LIGHT = { 110, 210, 90 };
    private static final int[][] KELP = { // { baseX, height, phase }
        { 9, 8, 0 }, { 12, 12, 1 }, { 15, 9, 2 }, { 32, 7, 3 }, { 35, 11, 4 }, { 38, 8, 5 }, { 47, 6, 6 }
    };
    private static final int[] AIR_STONES = { 25, 52, 76 };
    private static final int BUBBLES_PER_STREAM = 5;
    private static final double RISE_SPAN = 16.0;

    private final char[] bubbleGlyphs = new char[WIDTH * HEIGHT];
    private boolean eyesVisible = false;

    /** A fish silhouette: returns {r, g, b, alpha%} for a point in the fish's local space, or null. */
    private interface FishShape {
        int[] pixel(double u, double v);
    }

    public AquariumLoader() {
        // This uses 80x22 specifically
        super(AQUA_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.035; // Controls water swaying, bubbles, and fish movement

        // Orange fish: right -> left, gliding low in the tank
        double orangeX = WIDTH + 12 - ((timeClock * 12.0 + 40.0) % (WIDTH + 24));
        double orangeY = 13.0 + 2.5 * Math.sin(timeClock * 0.6);

        // Angelfish: left -> right, slower, cruising higher up
        double angelX = -12 + ((timeClock * 4.5 + 34.0) % (WIDTH + 24));
        double angelY = 8.0 + 1.6 * Math.sin(timeClock * 0.35 + 1.0);

        // Whoever lives in the fish house peeks out now and then, and blinks
        double cycle = timeClock % 12.0;
        boolean blinking = (cycle > 5.4 && cycle < 5.7) || (cycle > 8.0 && cycle < 8.3);
        eyesVisible = cycle > 3.0 && cycle < 10.0 && !blinking;

        buildBubbles();

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = x + WIDTH * y;
                int[] water = waterAt(x, y + 0.5); // water colour behind this cell

                String out;
                double depth;

                // Layers are tested front to back; the first one to claim the cell wins
                if      ((out = gravel(x, y, water))                                              != null) depth = Z_GRAVEL;
                else if ((out = house(x, y, water))                                               != null) depth = Z_HOUSE;
                else if ((out = kelp(x, y, water))                                                != null) depth = Z_KELP;
                else if ((out = fishCell(x, y, orangeX, orangeY, -1, this::orangeFishPixel))     != null) depth = Z_FISH_A;
                else if ((out = fishCell(x, y, angelX, angelY, 1, this::angelFishPixel))          != null) depth = Z_FISH_B;
                else if ((out = bubble(index, water))                                             != null) depth = Z_BUBBLE;
                else if (y == 0) { out = surface(x); depth = Z_WATER; }
                else {
                    // Smooth water: two gradient samples per cell (top half / bottom half)
                    out = cell('\u2580', waterAt(x, y + 0.25), waterAt(x, y + 0.75));
                    depth = Z_WATER;
                }

                if (depth > zBuffer[index]) {
                    zBuffer[index] = depth;
                    outputBuffer[index] = out;
                }
            }
        }
    }

    // =====================================================================
    //  WATER: one continuous gradient, plus soft drifting light shafts
    // =====================================================================

    private int[] waterAt(double x, double yy) {
        // Quadratic curve through TOP (t=0), MID (t=0.5), BTM (t=1): no visible seams
        double t = Math.max(0.0, Math.min(1.0, yy / FLOOR_Y));
        double a = (1 - t) * (1 - 2 * t);
        double b = 4 * t * (1 - t);
        double c = t * (2 * t - 1);

        // Sunlight shafts that fade with depth
        double fade   = Math.max(0.0, 1.0 - yy / 15.0);
        double shaftA = 0.5 + 0.5 * Math.sin(x * 0.30 + yy * 0.16 + timeClock * 0.35);
        double shaftB = 0.5 + 0.5 * Math.sin(x * 0.11 - yy * 0.05 - timeClock * 0.20 + 2.0);
        double lift   = 30.0 * fade * shaftA * shaftB;

        return new int[] {
            clamp(RGB_WATER_TOP[0] * a + RGB_WATER_MID[0] * b + RGB_WATER_BTM[0] * c + lift),
            clamp(RGB_WATER_TOP[1] * a + RGB_WATER_MID[1] * b + RGB_WATER_BTM[1] * c + lift),
            clamp(RGB_WATER_TOP[2] * a + RGB_WATER_MID[2] * b + RGB_WATER_BTM[2] * c + lift * 0.6)
        };
    }

    /** Shimmering waterline along the top row. */
    private String surface(int x) {
        double wave = 0.5 + 0.5 * Math.sin(x * 0.55 + timeClock * 2.2) * Math.sin(x * 0.17 - timeClock * 0.9);
        int[] bg = mix(waterAt(x, 0.5), new int[] { 255, 255, 255 }, 0.18);
        int[] fg = mix(new int[] { 150, 215, 250 }, new int[] { 240, 252, 255 }, wave);
        return cell('~', fg, bg);
    }

    // =====================================================================
    //  GRAVEL: multicoloured pebbles, rolling top edge, air stones
    // =====================================================================

    private String gravel(int x, int y, int[] water) {
        // Air stones sit on top of the gravel and feed the bubble streams
        if (y == FLOOR_Y - 1) {
            for (int ax : AIR_STONES) {
                if (x == ax || x == ax + 1) return cell('\u2584', RGB_AIR_STONE, water);
            }
        }

        if (y < FLOOR_Y) {
            // Gentle mounds along the top edge (kept clear of the house doorway)
            if (Math.abs(x - HOUSE_X) <= 4 || y < FLOOR_Y - 2) return null;
            double m = 0.5 + 0.5 * Math.sin(x * 0.19 + 0.7) * Math.sin(x * 0.07 + 2.0)
                     + 0.25 * Math.sin(x * 0.53 + 1.0);
            if (y == FLOOR_Y - 1) {
                int[] pebble = PEBBLES[hash(x >> 1, y) % PEBBLES.length];
                if (m > 0.62) return cell('\u2588', scale(pebble, 0.95), null);
                if (m > 0.30) return cell('\u2584', scale(pebble, 0.95), water);
            } else if (m > 1.0) { // rare tall mound
                int[] pebble = PEBBLES[hash(x >> 1, y) % PEBBLES.length];
                return cell('\u2584', scale(pebble, 0.9), water);
            }
            return null;
        }

        int[] pebble = PEBBLES[hash((x + (y & 1)) >> 1, y) % PEBBLES.length];
        double shade = 1.0 - 0.14 * (y - FLOOR_Y);            // darker toward the bottom
        int[] base = mix(scale(pebble, shade), RGB_WATER_BTM, 0.10);
        int r = hash(x, y * 7);
        char glyph = (r % 5 < 2) ? '\u2588' : (r % 5 < 4) ? '\u2593' : '\u2592';
        return cell(glyph, base, scale(base, 0.62));
    }

    // =====================================================================
    //  FISH HOUSE: mossy stone hut with a lit window, lintel and a resident
    // =====================================================================

    private boolean inDoor(int dx, int y) {
        return y >= HOUSE_BASE - 3 && Math.abs(dx) <= (y == HOUSE_BASE - 3 ? 1 : 2);
    }

    private String house(int x, int y, int[] water) {
        if (y < HOUSE_TOP || y > HOUSE_BASE) return null;
        int dx = x - HOUSE_X;
        int ax = Math.abs(dx);
        double hw  = 0.4 + (y - HOUSE_TOP) * 1.1;   // half-width of the roof line at this row
        double cov = hw - (ax - 0.5);               // how much of this cell is inside the outline

        // Chimney sitting on the right slope, puffing bubbles (see buildBubbles)
        if (dx == 2 && (y == HOUSE_TOP || y == HOUSE_TOP + 1)) {
            int[] brick = y == HOUSE_TOP ? new int[] { 176, 166, 154 } : new int[] { 150, 84, 70 };
            return cell('\u2588', mix(brick, water, 0.10), null);
        }
        if (cov < 0.25) return null;

        // Doorway
        if (inDoor(dx, y)) {
            int[] inside = mix(RGB_DOOR_TOP, RGB_DOOR_BTM, (y - (HOUSE_BASE - 3)) / 3.0);
            if (eyesVisible && y == HOUSE_BASE - 1 && ax == 1) {
                return cell('\u2580', RGB_EYES, inside);
            }
            return cell('\u2588', inside, null);
        }

        // Lit attic window
        if (y == HOUSE_TOP + 2 && dx == 0) {
            double flicker = 0.88 + 0.12 * Math.sin(timeClock * 6.0);
            return cell('\u2588', scale(RGB_WINDOW, flicker), null);
        }

        boolean nearDoor   = inDoor(dx - 1, y) || inDoor(dx + 1, y) || inDoor(dx, y + 1);
        boolean nearWindow = y == HOUSE_TOP + 2 && ax == 1;
        double side  = dx / Math.max(hw, 1.0);                       // -1 (left) .. +1 (right)

        // Roof: terracotta shingles, each cell split into a light lower half and a dark upper half
        if (y <= HOUSE_TOP + 3 && !nearWindow) {
            double rl = 1.10 - 0.22 * side;
            int[] shingle = scale(RGB_ROOF, rl * (((dx + y) & 1) == 0 ? 1.0 : 0.86));
            if (y >= HOUSE_TOP + 2 && hash(x, y + 50) % 6 == 0) shingle = mix(shingle, RGB_MOSS, 0.65); // mossy patches
            shingle = mix(shingle, water, 0.08);
            if (cov < 0.75) return cell(dx < 0 ? '\u2590' : '\u258C', shingle, water);
            return cell('\u2584', shingle, scale(shingle, 0.68));
        }

        // Stone walls: staggered stones with dark seams
        int segLen = 3 + (hash(y, 91) & 1);
        int shift  = 40 + (hash(y, 17) & 3);
        int stone  = Math.floorDiv(dx + shift, segLen);
        boolean seam = cov > 1.5 && Math.floorMod(dx + shift, segLen) == 0;

        double light = 1.02 - 0.20 * side + 0.02 * (HOUSE_BASE - y); // lit from the upper left
        double tint  = 0.88 + 0.24 * (hash(stone, y) % 100) / 100.0;
        int[] rock = scale(RGB_ROCK, light * tint);

        if (y >= HOUSE_BASE - 1 && hash(x, y + 9) % 6 == 0)  rock = mix(rock, RGB_MOSS, 0.35);
        if (nearDoor || nearWindow)                          { rock = mix(rock, RGB_LINTEL, 0.45); seam = false; }
        if (y == HOUSE_BASE)                                 rock = scale(rock, 0.85);
        rock = mix(rock, water, 0.12); // a little water haze so the house sits in the tank

        int[] bg = scale(rock, 0.72);
        char glyph;
        if (seam) { rock = scale(rock, 0.55); bg = scale(rock, 0.7); glyph = '\u2592'; }
        else { int r = hash(x, y); glyph = (r % 10 < 5) ? '\u2588' : (r % 10 < 8) ? '\u2593' : '\u2592'; }

        // Sloped edges get half-block cells for a smoother outline
        if (cov < 0.75) {
            return cell(dx < 0 ? '\u2590' : '\u258C', rock, water);
        }
        return cell(glyph, rock, bg);
    }

    // =====================================================================
    //  KELP: rooted at the gravel, sways more toward the tip, side fronds
    // =====================================================================

    private String kelp(int x, int y, int[] water) {
        int h = (FLOOR_Y - 1) - y; // 0 at the root row
        if (h < 0) return null;
        for (int[] k : KELP) {
            int height = k[1];
            if (h >= height) continue;
            double f = h / (double) height;
            double sway = 2.4 * Math.pow(f, 1.3) * Math.sin(h * 0.28 + timeClock * 1.3 + k[2] * 1.7);
            int cx = (int) Math.round(k[0] + sway);
            int[] green = mix(RGB_KELP_DARK, RGB_KELP_LIGHT, f);

            if (x == cx) return cell(h == height - 1 ? '\u2584' : '\u2593', green, water);
            if (h % 3 == 1 && x == cx + (((h / 3) % 2 == 0) ? 1 : -1)) {
                return cell('\u2592', mix(green, RGB_KELP_LIGHT, 0.35), water);
            }
        }
        return null;
    }

    // =====================================================================
    //  BUBBLES: several per stream, growing as they rise
    // =====================================================================

    private void buildBubbles() {
        Arrays.fill(bubbleGlyphs, '\0');
        for (int s = 0; s < AIR_STONES.length; s++) {
            for (int b = 0; b < BUBBLES_PER_STREAM; b++) {
                double speed = 5.0 + 0.9 * b + 0.6 * s;
                double rise  = (timeClock * speed + b * 3.7 + s * 6.1) % RISE_SPAN;
                double by    = (FLOOR_Y - 2) - rise;
                double bx    = AIR_STONES[s] + 0.5
                             + (0.6 + 1.6 * rise / RISE_SPAN) * Math.sin(by * 0.45 + timeClock * 1.6 + b * 2.1 + s);
                int cx = (int) Math.round(bx);
                int cy = (int) Math.round(by);
                if (cx < 0 || cx >= WIDTH || cy < 1 || cy >= FLOOR_Y - 1) continue;
                bubbleGlyphs[cx + WIDTH * cy] = rise < 4.0 ? '\u00B7' : rise < 11.0 ? 'o' : 'O';
            }
        }
        buildChimneyBubbles();
    }

    private void buildChimneyBubbles() {
        for (int b = 0; b < 3; b++) {
            double rise = (timeClock * 2.6 + b * 2.9) % 8.0;
            double by   = (HOUSE_TOP - 1) - rise;
            double bx   = HOUSE_X + 2 + (0.3 + rise * 0.25) * Math.sin(by * 0.7 + timeClock * 1.3 + b * 2.0);
            int cx = (int) Math.round(bx);
            int cy = (int) Math.round(by);
            if (cx < 0 || cx >= WIDTH || cy < 1) continue;
            bubbleGlyphs[cx + WIDTH * cy] = rise < 3.0 ? '\u00B7' : 'o';
        }
    }

    private String bubble(int index, int[] water) {
        char g = bubbleGlyphs[index];
        return g == '\0' ? null : cell(g, RGB_BUBBLE, water);
    }

    // =====================================================================
    //  FISH: sub-cell (half-block) sprites so the shapes can carry detail
    // =====================================================================

    /**
     * Samples a fish at the top and bottom half of a cell.
     * heading = -1 swims left, +1 swims right. Local space: u = toward the head, v = down,
     * measured in "pixels" (1 column wide, half a row tall) so they come out roughly square.
     */
    private String fishCell(int x, int y, double cx, double cy, int heading, FishShape shape) {
        if (Math.abs(x + 0.5 - cx) > 16 || Math.abs(y + 0.5 - cy) > 6) return null;
        double u = heading * (x + 0.5 - cx);
        int[] top = shape.pixel(u, (y + 0.25 - cy) * 2.0);
        int[] bot = shape.pixel(u, (y + 0.75 - cy) * 2.0);
        if (top == null && bot == null) return null;

        int[] wTop = waterAt(x, y + 0.25);
        int[] wBot = waterAt(x, y + 0.75);
        int[] fgc = top == null ? wTop : mix(wTop, top, top[3] / 100.0);
        int[] bgc = bot == null ? wBot : mix(wBot, bot, bot[3] / 100.0);
        return cell('\u2580', fgc, bgc);
    }

    /** Neon orange fish: banded, finned, forked tail that wags. */
    private int[] orangeFishPixel(double pu, double pv) {
        double u = pu / 0.8;
        double v = pv / 0.8;
        if (u > 3.8 || u < -10.0) return null;

        double wag = Math.sin(timeClock * 9.0);
        if (u < -5.5) v -= wag * 1.4 * Math.min(1.0, (-5.5 - u) / 4.5); // tail bends toward its tip

        // Silhouette half-height at this u: rounded body -> narrow tail stalk -> flared, forked tail
        double join = 3.0 * Math.sqrt(1.0 - (2.0 / 3.8) * (2.0 / 3.8));
        double hh;
        if (u >= -2.0) {
            double e = u / 3.8;
            hh = 3.0 * Math.sqrt(Math.max(0.0, 1.0 - e * e));
        } else if (u >= -6.0) {
            hh = join + (u + 2.0) * ((join - 1.2) / 4.0);
        } else {
            hh = 1.2 + (-6.0 - u) * 0.95;
            if (u < -8.4 && Math.abs(v) < (-8.4 - u) * 1.3) return null; // fork notch
        }

        double av = Math.abs(v);
        if (av > hh) {
            // Dorsal fin (top) and belly fin (bottom)
            double dorsal = 1.8 * Math.max(0.0, 1.0 - Math.abs(u + 1.5) / 3.2);
            double belly  = 1.5 * Math.max(0.0, 1.0 - Math.abs(u - 0.5) / 2.2);
            if (v < 0 && av <= hh + dorsal && u >= -6.0) return new int[] { 235, 80, 25, 88 };
            if (v > 0 && av <= hh + belly  && u >= -6.0) return new int[] { 255, 175, 45, 80 };
            return null;
        }

        if (u < -6.0) { // tail fin, more see-through toward the tip
            int alpha = (int) Math.max(80, 100 + (u + 6.0) * 5.0);
            return new int[] { 255, 140, 30, alpha };
        }

        // Body: orange back blending to a yellow belly
        double belly = Math.max(0.0, Math.min(1.0, (v + hh) / (2.0 * hh)));
        int[] rgb = mix(RGB_FISH_CORE, RGB_FISH_BELLY, belly);

        if (u >= 0.2 && u <= 1.5)       rgb = mix(rgb, new int[] { 252, 248, 240 }, 0.95); // white band
        else if (u >= -0.3 && u <= 2.0) rgb = mix(rgb, new int[] { 150, 50, 10 }, 0.60);   // band edging

        double ex = u - 2.3, ey = v + 0.9;
        if (ex * ex + ey * ey < 1.1) rgb = new int[] { 20, 25, 40 };                       // eye
        return new int[] { rgb[0], rgb[1], rgb[2], 100 };
    }

    /** Silver-and-cream angelfish: tall sweeping fins, dark bars, red eye. */
    private int[] angelFishPixel(double pu, double pv) {
        double u = pu / 0.95;
        double v = pv / 0.95;
        if (u > 3.9 || u < -8.4 || Math.abs(v) > 8.0) return null;

        double wag = Math.sin(timeClock * 6.0);
        if (u < -3.6) v -= wag * 1.0 * Math.min(1.0, (-3.6 - u) / 4.0);

        boolean body = (u * u) / (3.7 * 3.7) + (v * v) / (3.0 * 3.0) <= 1.0;
        boolean dorsal   = inTriangle(u, v, 1.6, -2.4, -3.0, -6.6, -4.4, -1.0);
        boolean ventral  = inTriangle(u, v, 1.4, 2.4, -2.8, 6.2, -4.2, 1.0);
        boolean filament = distToSegment(u, v, 1.9, 2.8, -0.4, 7.6) < 0.6;
        boolean tail = u < -3.0 && u > -8.2
                    && Math.abs(v) <= 1.2 + (-3.0 - u) * 0.55
                    && !(u < -6.8 && Math.abs(v) < (-6.8 - u) * 0.9);
        if (!(body || dorsal || ventral || filament || tail)) return null;

        int[] rgb;
        int alpha;
        if (body) {
            rgb = mix(new int[] { 226, 232, 244 }, new int[] { 255, 244, 214 }, Math.max(0.0, Math.min(1.0, (v + 3.0) / 6.0)));
            alpha = 100;
        } else if (tail) {
            rgb = new int[] { 205, 218, 240 };
            alpha = (int) Math.max(40, 66 + (u + 3.0) * 5.0);
        } else if (filament) {
            rgb = new int[] { 235, 240, 252 };
            alpha = 72;
        } else {
            rgb = new int[] { 196, 216, 240 };
            alpha = 56;
        }

        // Vertical bars run through the body and up/down into the fins
        if (!tail && !filament) {
            boolean bar = Math.abs(u - 1.3) <= 0.5 || Math.abs(u + 1.3) <= 0.5;
            if (bar) {
                rgb = mix(rgb, new int[] { 44, 50, 84 }, 0.85);
                alpha = Math.max(alpha, body ? 100 : 78);
            }
        }

        double ex = u - 2.5, ey = v + 0.6;
        if (body && ex * ex + ey * ey < 1.0) { rgb = new int[] { 235, 60, 45 }; alpha = 100; } // eye
        return new int[] { rgb[0], rgb[1], rgb[2], alpha };
    }

    private static boolean inTriangle(double px, double py, double ax, double ay,
                                      double bx, double by, double cx, double cy) {
        double d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        double d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        double d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
        boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    private static double distToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double t = Math.max(0.0, Math.min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    private static int clamp(double v) {
        return v < 0 ? 0 : v > 255 ? 255 : (int) v;
    }

    private static int[] mix(int[] a, int[] b, double t) {
        return new int[] {
            clamp(a[0] * (1.0 - t) + b[0] * t),
            clamp(a[1] * (1.0 - t) + b[1] * t),
            clamp(a[2] * (1.0 - t) + b[2] * t)
        };
    }

    private static int[] scale(int[] c, double k) {
        return new int[] { clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k) };
    }

    private static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }

    /** One terminal cell: glyph in the foreground colour, optional background colour. */
    private String cell(char glyph, int[] fg, int[] bg) {
        StringBuilder sb = new StringBuilder(48);
        sb.append("\u001B[38;2;").append(fg[0]).append(';').append(fg[1]).append(';').append(fg[2]);
        if (bg != null) sb.append(";48;2;").append(bg[0]).append(';').append(bg[1]).append(';').append(bg[2]);
        return sb.append('m').append(glyph).append(RESET).toString();
    }
}