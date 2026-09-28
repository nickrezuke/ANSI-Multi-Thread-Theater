public class FarmLoader extends Loader {
    private static final StatusStage[] FARM_STAGES = {
        new StatusStage(25, "Plowing the north forty:"),
        new StatusStage(50, "Raising the big red barn:"),
        new StatusStage(75, "Herding the livestock into the paddock:"),
        new StatusStage(100, "Farm Open - Sunny Day Ahead!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;

    private static final double SUN_X = 66.0, SUN_Y = 7.0;
    private static final int BARN_CX = 15, BARN_BASE = 28;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int BARN_RED       = rgb(178, 40,  36);
    private static final int TRIM_WHITE     = rgb(242, 236, 220);
    private static final int ROOF_DARK      = rgb(66,  60,  68);
    private static final int FENCE          = rgb(150, 100, 58);
    private static final int HAY            = rgb(224, 188, 78);
    private static final int TRACTOR_GREEN  = rgb(46,  146, 58);
    private static final int TRACTOR_DARK   = rgb(30,  100, 42);
    private static final int TRACTOR_YELLOW = rgb(246, 206, 50);
    private static final int TIRE           = rgb(32,  32,  36);
    private static final int SKIN           = rgb(226, 170, 130);
    private static final int[] FLOWERS = { rgb(255, 226, 60), rgb(255, 255, 255), rgb(255, 140, 190), rgb(255, 150, 50) };

    // { y, scale, speed, x offset } for each drifting cloud, and its puffy lobes { x, y, radius }
    private static final double[][] CLOUDS = { { 4.5, 1.0, 1.6, 10 }, { 9.5, 0.8, 1.1, 55 }, { 3.0, 0.7, 2.0, 90 }, { 11.5, 1.1, 0.8, 30 } };
    private static final double[][] CLOUD_LOBES = { { 0, 0, 3.0 }, { -3.6, 0.9, 2.3 }, { 3.8, 0.8, 2.5 }, { 0.4, -1.4, 2.4 }, { -1.6, -1.0, 2.0 } };

    private final int[] px = new int[PW * PH];

    public FarmLoader() {
        // This uses 80x22 specifically
        super(FARM_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.045;

        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                px[y * PW + x] = scenePixel(x, y);
            }
        }

        paintWindmill();
        paintWeathervane();
        paintFenceBack();
        paintTrough();
        paintCow(7, 34, 1, 0);
        paintCow(31, 34, -1, 1);
        paintSheep(4, 39, 1, 0);
        paintPig(14, 39, 1, 0);
        paintSheep(31, 39, -1, 1);
        paintFenceFront();
        paintChickens();
        paintTractor();
        paintButterfly();
        paintBirds();
        paintGrassTufts();

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = x + WIDTH * y;
                if (Z_SCENE > zBuffer[index]) {
                    zBuffer[index] = Z_SCENE;
                    outputBuffer[index] = cell(px[(2 * y) * PW + x], px[(2 * y + 1) * PW + x]);
                }
            }
        }
    }

    // =====================================================================
    //  LANDSCAPE: sky and clouds, rolling hills, meadow, crop field, barn
    // =====================================================================

    private static double hillFar(int x)  { return 16.0 + 2.4 * Math.sin(x * 0.07 + 1.0) + 1.2 * Math.sin(x * 0.19); }
    private static double hillNear(int x) { return 19.4 + 2.0 * Math.sin(x * 0.05 + 3.0) + 1.0 * Math.sin(x * 0.13); }
    private static double fieldLeft(int y) { return 42.0 - (y - 21) * 0.3; }

    private static boolean inPaddock(int x, int y) {
        return y >= 32 && y <= 42 && x >= 5 - (y - 32) * 0.5 && x <= 31 + (y - 32) * 0.35;
    }

    private static boolean inYard(int x, int y) {
        double u = (x - 15) / 9.0, v = (y - 29.5) / 2.0;
        return u * u + v * v < 1.0;
    }

    private int scenePixel(int x, int y) {
        int b = barnPixel(x, y);
        if (b >= 0) return b;

        int h = hash(x / 2, 44) % 6;
        double treeTop = hillFar(x) - (h == 0 ? 2 : h == 1 ? 1 : 0); // a ragged tree line along the far hills
        if (y < treeTop) return skyPixel(x, y);
        if (y < hillFar(x)) return rgb(66, 124, 86);
        if (y < hillNear(x)) return farHillPixel(y - hillFar(x));
        return groundPixel(x, y);
    }

    private int skyPixel(int x, int y) {
        double t = clamp01(y / 19.0);
        int c = mix(rgb(58, 144, 238), rgb(178, 224, 252), Math.pow(t, 1.3));
        double dx = x - SUN_X, dy = y - SUN_Y;
        double d = Math.sqrt(dx * dx + dy * dy);
        c = mix(c, rgb(255, 250, 205), 0.5 * Math.exp(-d * d / 90.0));
        if (d < 3.6) c = mix(c, rgb(255, 253, 228), clamp01((3.6 - d) * 1.6));

        for (double[] cl : CLOUDS) {
            double cx = ((cl[3] + timeClock * cl[2]) % 110.0) - 15.0;
            double u = (x - cx) / cl[1], v = (y - cl[0]) / cl[1];
            double cover = 0.0;
            for (double[] lb : CLOUD_LOBES) {
                double dd = Math.hypot(u - lb[0], (v - lb[1]) * 1.25);
                if (dd < lb[2]) cover = Math.max(cover, clamp01((lb[2] - dd) * 1.4));
            }
            if (cover > 0.0) {
                int cc = mix(rgb(255, 255, 255), rgb(198, 216, 238), clamp01(v * 0.35 + 0.15));
                c = mix(c, cc, cover * 0.95);
            }
        }
        return c;
    }

    private int farHillPixel(double depth) {
        int c = mix(rgb(128, 182, 152), rgb(104, 166, 116), clamp01(depth / 5.0));
        return depth < 1.0 ? shade(c, 1.1) : c;
    }

    private int groundPixel(int x, int y) {
        if (y >= 21 && x >= fieldLeft(y) - 1.2) {
            if (x < fieldLeft(y)) return (hash(x, y) % 4 == 0) ? rgb(70, 140, 60) : rgb(48, 108, 48); // hedgerow
            return fieldPixel(x, y);
        }
        double d = clamp01((y - 19.0) / 25.0);
        int c = mix(rgb(134, 202, 86), rgb(56, 144, 44), d);
        c = shade(c, 1.0 + 0.05 * Math.sin(x * 0.33 + timeClock * 1.7 + y * 0.4)); // wind rippling the grass
        int h = hash(x, y);
        if (h % 9 == 0) c = shade(c, 1.12); else if (h % 9 == 1) c = shade(c, 0.9);

        if (y >= 27 && inYard(x, y)) {
            c = mix(c, rgb(184, 148, 100), 0.85);
            if (h % 7 == 0) c = shade(c, 0.86);
        } else if (inPaddock(x, y)) {
            c = mix(c, rgb(196, 176, 98), 0.28);
            if (hash(x / 3, y / 2 + 77) % 5 == 0) c = mix(c, rgb(170, 138, 86), 0.5); // trampled dirt
        } else if (y >= 24 && hash(x / 2, y / 2 + 9) % 61 == 0) {
            double tw = 0.85 + 0.15 * Math.sin(timeClock * 2.0 + h % 7);
            c = shade(FLOWERS[h % FLOWERS.length], tw);
        }
        return c;
    }

    /** Planted crop rows in perspective: thin and dense near the horizon, wide and clear up close. */
    private int fieldPixel(int x, int y) {
        double y0 = y - 18.5;
        double phase = 3.0 * Math.sqrt(y0);
        double frac = phase - Math.floor(phase);
        double amp = clamp01((y0 - 3.0) / 5.0);
        int crop = (hash(x, (int) Math.floor(phase)) % 3 == 0) ? rgb(136, 188, 66) : rgb(96, 160, 52);
        int soil = rgb(122, 86, 54);
        int avg = mix(crop, soil, 0.45);
        int c = mix(avg, frac < 0.58 ? crop : soil, amp);
        c = shade(c, 1.0 + 0.04 * Math.sin(x * 0.25 + timeClock * 1.4 + phase * 2.0));
        return mix(c, rgb(170, 205, 150), clamp01(1.0 - (y - 21) / 10.0) * 0.30); // haze toward the horizon
    }

    // ---- the barn: a gambrel-roofed gable end with X-braced doors and a hayloft ----

    private int barnPixel(int x, int y) {
        int s = siloPixel(x, y);
        if (s >= 0) return s;

        int rx = x - BARN_CX;
        int dx = Math.abs(rx);
        double roofTop = dx <= 5.5 ? 9 + dx * 0.73 : 13 + (dx - 5.5) * 1.17;
        if (dx > 11 || y < roofTop - 0.2 || y > BARN_BASE) return -1;
        if (y >= 21 && dx > 10) return -1;

        if (y < roofTop + 1.0) return ROOF_DARK;      // shingle edge along the roofline
        if (y < roofTop + 2.0) return TRIM_WHITE;      // white trim under it
        if (y == BARN_BASE) return rgb(150, 146, 140); // stone foundation

        // hayloft: white frame, dark opening, hay spilling out
        if (Math.abs(rx) <= 3 && y >= 13 && y <= 18) {
            boolean inner = Math.abs(rx) <= 2 && y >= 14 && y <= 17;
            if (!inner) return TRIM_WHITE;
            return (y == 17 || (y == 16 && rx == 0)) ? HAY : rgb(40, 22, 18);
        }
        // windows
        if (y >= 21 && y <= 24 && Math.abs(rx) >= 7 && Math.abs(rx) <= 10) {
            boolean glass = y >= 22 && y <= 23 && Math.abs(rx) >= 8 && Math.abs(rx) <= 9;
            if (!glass) return TRIM_WHITE;
            return (y == 22 && Math.abs(rx) == 8) ? rgb(150, 190, 220) : rgb(78, 120, 160);
        }
        // big doors: left one closed with a white X, right one slid open onto a hay bale
        if (Math.abs(rx) <= 5 && y >= 20) {
            if (Math.abs(rx) == 5 || y == 20) return TRIM_WHITE;
            if (rx == 0) return rgb(60, 20, 18);
            if (rx < 0) {
                double u = rx + 4, v = y - 21;
                boolean brace = Math.abs(v - u * 2.33) < 1.0 || Math.abs(v - (3 - u) * 2.33) < 1.0;
                return brace ? TRIM_WHITE : shade(BARN_RED, 1.08);
            }
            if (y >= 26) return shade(HAY, y == 26 ? 1.0 : 0.85);
            return rgb(34, 20, 18);
        }
        if (Math.abs(rx) == 10 && y >= 21) return TRIM_WHITE; // corner boards

        double k = 0.92 + 0.10 * (rx + 11) / 22.0;
        if (x % 3 == 0) k *= 0.88; // plank seams
        if (hash(x, y) % 17 == 0) k *= 0.94;
        return shade(BARN_RED, k);
    }

    private int siloPixel(int x, int y) {
        if (x < 27 || x > 31) return -1;
        if (Math.hypot(x - 29, (y - 12.5) * 1.2) <= 2.8 && y < 12) return rgb(150, 60, 50); // cap
        if (y < 12 || y > BARN_BASE) return -1;
        double k = 0.68 + 0.36 * (x - 27) / 4.0; // lit from the sun on the right
        if (y % 4 == 0) k *= 0.85;               // hoop bands
        return shade(rgb(176, 188, 204), k);
    }

    // =====================================================================
    //  BACKGROUND MOVERS: windmill and weathervane
    // =====================================================================

    private void paintWindmill() {
        int cx = 50, top = 9, base = 20;
        int tower = rgb(118, 108, 98), braceCol = rgb(176, 166, 152);
        for (int y = top; y <= base; y++) {
            double hw = 0.8 + (y - top) * 0.2;
            int xl = (int) Math.round(cx - hw), xr = (int) Math.round(cx + hw);
            plot(xl, y, tower);
            plot(xr, y, tower);
            if ((y - top) % 3 == 1) for (int x = xl + 1; x < xr; x++) plot(x, y, braceCol);
        }
        double a0 = timeClock * 0.9;
        for (int b = 0; b < 4; b++) {
            double a = a0 + b * Math.PI / 2;
            line(cx + Math.cos(a) * 1.5, top - 1 + Math.sin(a) * 1.5, cx + Math.cos(a) * 5.5, top - 1 + Math.sin(a) * 5.5,
                 (b % 2 == 0) ? rgb(248, 248, 244) : rgb(220, 224, 226), 1);
        }
        plot(cx, top - 1, rgb(200, 60, 50));
        line(cx + 1, top - 1, cx + 4, top - 1, rgb(220, 90, 70), 1); // tail vane
    }

    private void paintWeathervane() {
        int d = Math.sin(timeClock * 0.5) > 0 ? 1 : -1;
        int metal = rgb(60, 60, 66), gold = rgb(240, 200, 60);
        for (int y = 6; y <= 8; y++) plot(BARN_CX, y, metal);
        plot(BARN_CX - 1, 6, metal);
        plot(BARN_CX + 1, 6, metal);
        for (int i = -1; i <= 2; i++) plot(BARN_CX + d * i, 5, gold);
        plot(BARN_CX + d * 2, 4, gold);
    }

    // =====================================================================
    //  THE PADDOCK: white ranch fence, trough, and the animals inside
    // =====================================================================

    private void post(int x, int baseY, int h) {
        for (int i = 0; i < h; i++) plot(x, baseY - i, i == h - 1 ? rgb(196, 146, 92) : shade(FENCE, 0.85));
    }

    private void paintFenceBack() {
        line(5, 31, 31, 31, FENCE, 1);
        for (int x = 5; x <= 26; x += 7) post(x, 32, 3);
        post(31, 32, 3);
        line(5, 31, 1, 41, FENCE, 1);
        line(31, 31, 34, 41, FENCE, 1);
        post(3, 36, 3);
        post(32, 36, 3);
    }

    private void paintFenceFront() {
        line(1, 41, 34, 41, FENCE, 1);
        line(1, 43, 34, 43, FENCE, 1);
        for (int x = 1; x <= 34; x += 8) post(x, 43, 4);
        post(34, 43, 4);
    }

    private void paintTrough() {
        int wood = rgb(120, 80, 50);
        for (int x = 18; x <= 20; x++) {
            plot(x, 34, wood);
            plot(x, 35, shade(wood, 0.8));
            double tw = 0.5 + 0.5 * Math.sin(timeClock * 3.0 + x);
            plot(x, 33, mix(rgb(110, 180, 230), rgb(230, 246, 255), tw * 0.5));
        }
    }

    private void shadow(int ox, int oy, int w, int dir) {
        for (int i = -1; i <= w; i++) {
            int x = ox + dir * i, y = oy + 1;
            if (x >= 0 && x < PW && y >= 0 && y < PH) px[y * PW + x] = mix(px[y * PW + x], rgb(28, 84, 34), 0.35);
        }
    }

    private void p(int ox, int oy, int dir, int dx, int dy, int c) {
        plot(ox + dir * dx, oy + dy, c);
    }

    /** Side-on cow, grazing most of the time and chewing with its head down. dir is +1 (faces right) or -1. */
    private void paintCow(int ox, int oy, int dir, int i) {
        boolean graze = Math.sin(timeClock * 0.35 + i * 2.3) > -0.35;
        boolean chew = Math.sin(timeClock * 7.0 + i) > 0.2;
        int white = rgb(248, 246, 240), gray = rgb(206, 204, 200), hoof = rgb(64, 54, 54);
        int patch = (i % 2 == 0) ? rgb(36, 32, 34) : rgb(150, 86, 46);
        shadow(ox, oy, 8, dir);

        for (int dy = -1; dy <= 0; dy++) {
            p(ox, oy, dir, 1, dy, dy == 0 ? hoof : gray);
            p(ox, oy, dir, 6, dy, dy == 0 ? hoof : gray);
            p(ox, oy, dir, 0, dy, dy == 0 ? hoof : white);
            p(ox, oy, dir, 5, dy, dy == 0 ? hoof : white);
        }
        for (int dx = 0; dx <= 6; dx++) {
            for (int dy = -4; dy <= -2; dy++) p(ox, oy, dir, dx, dy, dy == -2 ? gray : white);
        }
        int[][] spots = { { 1, -4 }, { 2, -4 }, { 2, -3 }, { 4, -4 }, { 4, -3 }, { 5, -3 }, { 5, -2 } };
        for (int[] s : spots) p(ox, oy, dir, s[0], s[1], patch);
        p(ox, oy, dir, 3, -1, rgb(238, 150, 160)); // udder

        int pink = rgb(240, 160, 170);
        if (graze) {
            int hy = chew ? -1 : -2;
            p(ox, oy, dir, 7, -3, white);
            p(ox, oy, dir, 8, -3, white);
            for (int dx = 8; dx <= 10; dx++) {
                p(ox, oy, dir, dx, hy, white);
                p(ox, oy, dir, dx, hy + 1, white);
            }
            p(ox, oy, dir, 9, hy, rgb(20, 20, 24));
            p(ox, oy, dir, 10, hy + 1, pink);
        } else {
            p(ox, oy, dir, 7, -4, white);
            p(ox, oy, dir, 7, -3, white);
            for (int dx = 8; dx <= 10; dx++) {
                p(ox, oy, dir, dx, -5, white);
                p(ox, oy, dir, dx, -4, white);
            }
            p(ox, oy, dir, 9, -5, rgb(20, 20, 24));
            p(ox, oy, dir, 10, -4, pink);
            p(ox, oy, dir, 8, -6, rgb(240, 226, 190)); // horn
        }
        int sw = Math.sin(timeClock * 2.2 + i) > 0 ? 1 : 0; // tail swish
        p(ox, oy, dir, -1, -4, white);
        p(ox, oy, dir, -1 - sw, -3, white);
        p(ox, oy, dir, -1 - sw, -2, hoof);
    }

    private void paintSheep(int ox, int oy, int dir, int i) {
        boolean graze = Math.sin(timeClock * 0.4 + i * 1.9) > -0.2;
        int leg = rgb(60, 56, 58), face = rgb(64, 56, 58);
        shadow(ox, oy, 6, dir);
        p(ox, oy, dir, 1, -1, leg);
        p(ox, oy, dir, 1, 0, leg);
        p(ox, oy, dir, 4, -1, leg);
        p(ox, oy, dir, 4, 0, leg);
        for (int dx = 0; dx <= 5; dx++) {
            for (int dy = -4; dy <= -2; dy++) {
                if (dy == -4 && (dx == 0 || dx == 5)) continue; // rounded fleece
                int r = hash(dx, dy + i * 5) % 3;
                int c = r == 0 ? rgb(255, 255, 255) : r == 1 ? rgb(236, 234, 226) : rgb(246, 244, 238);
                p(ox, oy, dir, dx, dy, dy == -2 ? shade(c, 0.9) : c);
            }
        }
        p(ox, oy, dir, -1, -3, rgb(246, 244, 238)); // stubby tail
        int hy = graze ? -1 : -3;
        p(ox, oy, dir, 6, hy, face);
        p(ox, oy, dir, 7, hy, face);
        p(ox, oy, dir, 6, hy + 1, face);
        p(ox, oy, dir, 7, hy + 1, face);
        if (graze) p(ox, oy, dir, 6, -2, rgb(246, 244, 238));
    }

    private void paintPig(int ox, int oy, int dir, int i) {
        int off = Math.sin(timeClock * 3.2 + i) > 0.3 ? 1 : 0; // rooting in the dirt
        int pink = rgb(246, 172, 178), dpink = rgb(220, 136, 148), snout = rgb(236, 128, 142);
        shadow(ox, oy, 7, dir);
        p(ox, oy, dir, 1, 0, dpink);
        p(ox, oy, dir, 4, 0, dpink);
        for (int dx = 0; dx <= 5; dx++) {
            for (int dy = -3; dy <= -1; dy++) p(ox, oy, dir, dx, dy, dy == -1 ? dpink : pink);
        }
        int hy = -3 + off;
        for (int dy = 0; dy < 3; dy++) {
            p(ox, oy, dir, 6, hy + dy, pink);
            p(ox, oy, dir, 7, hy + dy, pink);
        }
        p(ox, oy, dir, 7, hy, rgb(20, 20, 24));       // eye
        p(ox, oy, dir, 6, hy - 1, dpink);             // floppy ear
        p(ox, oy, dir, 8, hy + 1, snout);
        p(ox, oy, dir, 8, hy + 2, rgb(150, 70, 84));  // snout tip
        p(ox, oy, dir, -1, -3, dpink);
        p(ox, oy, dir, -2, Math.sin(timeClock * 6.0) > 0 ? -4 : -3, dpink); // curly tail
    }

    private void paintChickens() {
        for (int i = 0; i < 3; i++) {
            double ph = i * 2.1;
            int cx = (int) Math.round(12 + i * 5 + 5.0 * Math.sin(timeClock * 0.22 + ph));
            int dir = Math.cos(timeClock * 0.22 + ph) >= 0 ? 1 : -1;
            paintChicken(cx, 29 + (i % 2), dir, i);
        }
    }

    private void paintChicken(int cx, int cy, int dir, int i) {
        boolean peck = Math.sin(timeClock * 6.0 + i * 2.0) > 0.55;
        int body = (i % 2 == 0) ? rgb(252, 250, 244) : rgb(184, 112, 58);
        int leg = rgb(230, 160, 40);
        for (int dx = 0; dx <= 2; dx++) {
            p(cx, cy, dir, dx, -3, body);
            p(cx, cy, dir, dx, -2, shade(body, 0.9));
        }
        p(cx, cy, dir, -1, -4, shade(body, 0.8));
        p(cx, cy, dir, -1, -3, shade(body, 0.8));
        p(cx, cy, dir, 0, -1, leg);
        p(cx, cy, dir, 2, -1, leg);
        p(cx, cy, dir, 0, 0, leg);
        p(cx, cy, dir, 2, 0, leg);
        int hy = peck ? -2 : -4;
        p(cx, cy, dir, 3, hy, body);
        p(cx, cy, dir, 3, hy - 1, rgb(220, 40, 40)); // comb
        p(cx, cy, dir, 4, hy + (peck ? 1 : 0), rgb(250, 200, 40)); // beak
    }

    // =====================================================================
    //  THE TRACTOR: works a row back and forth, wheels turning, harrow and dust behind
    // =====================================================================

    private void paintTractor() {
        double ph = (timeClock * 0.037) % 1.0;
        double tri = ph < 0.5 ? ph * 2 : (1 - ph) * 2;
        double e = tri * tri * (3 - 2 * tri); // eases to a stop at each end of the row
        double xPos = 53 + 13 * e;
        int dir = ph < 0.5 ? 1 : -1;
        int ox = (int) Math.round(xPos), oy = 34;

        shadow(ox - dir * 4, oy, 19, dir);

        // disc harrow trailing on a hitch
        int frame = rgb(96, 96, 104), tine = rgb(60, 52, 46);
        for (int dx = -13; dx <= -3; dx++) p(ox, oy, dir, dx, -2, frame);
        for (int dx = -13; dx <= -7; dx += 2) {
            p(ox, oy, dir, dx, -1, tine);
            p(ox, oy, dir, dx, 0, tine);
        }

        drawWheel(ox, oy, dir, 0, -3.5, 3.6, xPos / 3.6);
        drawWheel(ox, oy, dir, 11, -2.0, 2.4, xPos / 2.4);

        for (int dx = 1; dx <= 3; dx++) {          // chassis between axles
            p(ox, oy, dir, dx, -5, TRACTOR_DARK);
            p(ox, oy, dir, dx, -4, TRACTOR_DARK);
        }
        for (int dx = 4; dx <= 12; dx++) {         // hood
            p(ox, oy, dir, dx, -7, shade(TRACTOR_GREEN, 1.18));
            p(ox, oy, dir, dx, -6, TRACTOR_GREEN);
            p(ox, oy, dir, dx, -5, TRACTOR_GREEN);
            p(ox, oy, dir, dx, -4, TRACTOR_DARK);
        }
        for (int dx = 5; dx <= 11; dx++) p(ox, oy, dir, dx, -5, TRACTOR_YELLOW); // hood stripe
        p(ox, oy, dir, 12, -6, rgb(30, 40, 30));
        p(ox, oy, dir, 12, -5, rgb(30, 40, 30));   // grille
        p(ox, oy, dir, 12, -7, rgb(255, 250, 200)); // headlight

        for (int y = -3; y <= 0; y++) { // (nothing: keeps the wheel area clear of chassis pixels)
            if (y > 0) p(ox, oy, dir, 0, y, TIRE);
        }
        for (int dx = -4; dx <= 2; dx++) p(ox, oy, dir, dx, -8, TRACTOR_GREEN); // rear fender

        // cab: glass, pillars, roof, and the farmer at the wheel
        for (int dx = -3; dx <= 2; dx++) {
            for (int dy = -12; dy <= -9; dy++) p(ox, oy, dir, dx, dy, rgb(160, 200, 224));
        }
        for (int dy = -12; dy <= -9; dy++) {
            p(ox, oy, dir, -3, dy, TRACTOR_DARK);
            p(ox, oy, dir, 2, dy, TRACTOR_DARK);
        }
        for (int dx = -4; dx <= 3; dx++) p(ox, oy, dir, dx, -13, TRACTOR_DARK);
        int overalls = rgb(60, 90, 180), straw = rgb(238, 214, 120);
        p(ox, oy, dir, -1, -9, overalls);
        p(ox, oy, dir, 0, -9, overalls);
        p(ox, oy, dir, 0, -10, SKIN);
        for (int dx = -1; dx <= 1; dx++) p(ox, oy, dir, dx, -11, straw);
        p(ox, oy, dir, 0, -12, straw);
        p(ox, oy, dir, 1, -9, SKIN); // arm on the wheel

        for (int dy = -11; dy <= -8; dy++) p(ox, oy, dir, 9, dy, rgb(56, 56, 62)); // exhaust stack
        p(ox, oy, dir, 10, -11, rgb(56, 56, 62));

        // exhaust puffs rising off the stack
        for (int k = 0; k < 4; k++) {
            double age = (timeClock * 1.3 + k * 0.5) % 2.0;
            int sx = (int) Math.round(ox + dir * (9 - age * 2.0));
            int sy = (int) Math.round(oy - 12 - age * 4.0);
            if (sx >= 0 && sx < PW && sy >= 0 && sy < PH) {
                px[sy * PW + sx] = mix(px[sy * PW + sx], rgb(214, 214, 220), 0.55 * (1.0 - age / 2.0));
            }
        }
        // dust kicked up behind the harrow
        for (int k = 0; k < 6; k++) {
            double age = (timeClock * 1.6 + k * 0.33) % 2.0;
            int dx = (int) Math.round(ox + dir * (-13 - age * 2.6));
            int dy = (int) Math.round(oy - 1 - age * 2.5 + Math.sin(k * 2.3) * 0.8);
            if (dx >= 0 && dx < PW && dy >= 0 && dy < PH) {
                px[dy * PW + dx] = mix(px[dy * PW + dx], rgb(206, 178, 130), 0.5 * (1.0 - age / 2.0));
            }
        }
    }

    /** A wheel that visibly rolls: dark tread lugs, yellow rim, and two spokes, all turning with the ground speed. */
    private void drawWheel(int ox, int oy, int dir, double cxl, double cyl, double r, double rot) {
        for (int dy = (int) Math.floor(cyl - r); dy <= (int) Math.ceil(cyl + r); dy++) {
            for (int dx = (int) Math.floor(cxl - r); dx <= (int) Math.ceil(cxl + r); dx++) {
                double ox2 = dir * (dx - cxl), oy2 = dy - cyl;
                double d = Math.hypot(ox2, oy2);
                if (d > r) continue;
                double ang = Math.atan2(oy2, ox2);
                int c;
                if (d > r * 0.62) {
                    c = Math.cos(7 * (ang - rot)) > 0.2 ? rgb(78, 78, 84) : TIRE;
                } else if (d > r * 0.28) {
                    c = Math.cos(2 * (ang - rot)) > 0.93 ? rgb(150, 120, 20) : TRACTOR_YELLOW;
                } else {
                    c = rgb(150, 150, 156);
                }
                p(ox, oy, dir, dx, dy, c);
            }
        }
    }

    // =====================================================================
    //  LITTLE LIFE: butterfly, birds, and swaying foreground grass
    // =====================================================================

    private void paintButterfly() {
        int bx = (int) Math.round(20 + 9 * Math.sin(timeClock * 0.55));
        int by = (int) Math.round(25 + 2.5 * Math.sin(timeClock * 0.9 + 1.0));
        boolean open = Math.sin(timeClock * 14.0) > 0;
        plot(bx, by, rgb(60, 40, 30));
        if (open) {
            plot(bx - 1, by, rgb(255, 150, 30));
            plot(bx + 1, by, rgb(255, 150, 30));
        } else {
            plot(bx, by - 1, rgb(255, 210, 60));
        }
    }

    private void paintBirds() {
        int dark = rgb(52, 52, 64);
        for (int i = 0; i < 3; i++) {
            int bx = (int) (((timeClock * 2.4 + i * 41.0) % 110.0) - 15.0);
            int by = (int) Math.round(4 + i * 3.2 + 1.2 * Math.sin(timeClock * 0.8 + i));
            boolean up = Math.sin(timeClock * 9.0 + i * 2.0) > 0;
            plot(bx, by, dark);
            plot(bx - 1, by + (up ? -1 : 0), dark);
            plot(bx + 1, by + (up ? -1 : 0), dark);
            plot(bx - 2, by + (up ? -2 : 1), dark);
            plot(bx + 2, by + (up ? -2 : 1), dark);
        }
    }

    private void paintGrassTufts() {
        for (int x = 33; x < PW; x += 2) {
            int h = 1 + hash(x, 31) % 3;
            int sway = (int) Math.round(Math.sin(timeClock * 1.5 + x * 0.7) * 0.9);
            for (int i = 0; i < h; i++) {
                plot(x + (i > 0 ? sway : 0), PH - 1 - i, i == h - 1 ? rgb(110, 190, 70) : rgb(40, 120, 40));
            }
        }
    }

    // =====================================================================
    //  PIXEL + MATH HELPERS
    // =====================================================================

    /** One terminal cell = two stacked pixels: foreground is the top pixel, background the bottom one. */
    private String cell(int top, int bot) {
        StringBuilder sb = new StringBuilder(48);
        sb.append("\u001B[38;2;").append((top >> 16) & 255).append(';').append((top >> 8) & 255).append(';').append(top & 255);
        if (top == bot) return sb.append('m').append('\u2588').append(RESET).toString();
        return sb.append(";48;2;").append((bot >> 16) & 255).append(';').append((bot >> 8) & 255).append(';').append(bot & 255)
                 .append('m').append('\u2580').append(RESET).toString();
    }

    private void plot(int x, int y, int c) {
        if (x >= 0 && x < PW && y >= 0 && y < PH) px[y * PW + x] = c;
    }

    private void line(double x0, double y0, double x1, double y1, int c, int thick) {
        int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))) + 1;
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            int x = (int) Math.round(x0 + (x1 - x0) * t), y = (int) Math.round(y0 + (y1 - y0) * t);
            plot(x, y, c);
            if (thick > 1) plot(x + 1, y, c);
        }
    }

    private static double clamp01(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    private static int clamp(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int) v; }

    private static int rgb(int r, int g, int b) { return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }

    private static int mix(int a, int b, double t) {
        t = clamp01(t);
        return rgb((int) (((a >> 16) & 255) * (1.0 - t) + ((b >> 16) & 255) * t),
                   (int) (((a >> 8) & 255) * (1.0 - t) + ((b >> 8) & 255) * t),
                   (int) ((a & 255) * (1.0 - t) + (b & 255) * t));
    }

    private static int shade(int c, double k) {
        return rgb((int) (((c >> 16) & 255) * k), (int) (((c >> 8) & 255) * k), (int) ((c & 255) * k));
    }

    private static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }
}