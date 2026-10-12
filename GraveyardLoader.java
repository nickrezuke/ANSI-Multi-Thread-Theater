public class GraveyardLoader extends Loader {
    private static final StatusStage[] GRAVEYARD_STAGES = {
        new StatusStage(25, "Raising the full moon:"),
        new StatusStage(50, "Planting the headstones:"),
        new StatusStage(75, "Rolling in the graveyard fog:"),
        new StatusStage(100, "The Veil Is Thin Tonight!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;

    private static final double MOON_X = 58.0, MOON_Y = 9.0, MOON_R = 5.5;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int STONE       = rgb(132, 130, 132);
    private static final int STONE_DARK  = rgb(62, 58, 64);
    private static final int MOSS        = rgb(60, 86, 56);
    private static final int WOOD_BLACK  = rgb(18, 16, 20);
    private static final int BARK        = rgb(26, 22, 24);

    // Everything baked once: gnarled trees, headstones, the distant crypt, and the iron fence.
    // Packed ARGB, 0 = empty, so a frame can tell "painted black" apart from "nothing painted".
    private final int[] near = new int[PW * PH];
    private boolean ready = false;

    private final int[] px = new int[PW * PH];

    public GraveyardLoader() {
        // This uses 80x22 specifically
        super(GRAVEYARD_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04;
        if (!ready) { buildStatic(); ready = true; }

        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                px[y * PW + x] = scenePixel(x, y);
            }
        }
        for (int i = 0; i < near.length; i++) {
            if ((near[i] >>> 24) != 0) px[i] = near[i] & 0xFFFFFF;
        }

        paintFog();
        paintWisps();
        paintBats();
        paintWitch();
        paintCatEyes();
        paintHouseWindow();

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
    //  LIVE BACKDROP: night sky, drifting clouds, the moon, and the hills
    // =====================================================================

    private static double hillFar(int x)  { return 24.0 + 2.5 * Math.sin(x * 0.08 + 1.0) + 1.3 * Math.sin(x * 0.17); }
    private static double hillNear(int x) { return 29.0 + 3.0 * Math.sin(x * 0.06 + 2.0) + 1.5 * Math.sin(x * 0.14 + 0.5); }

    private int scenePixel(int x, int y) {
        double hf = hillFar(x);
        if (y < hf) return skyPixel(x, y, hf);
        double hn = hillNear(x);
        if (y < hn) return farHillPixel((y - hf) / Math.max(1.0, hn - hf));
        return groundPixel(x, y);
    }

    private int skyPixel(int x, int y, double hf) {
        double t = clamp01(y / 26.0);
        int c = mix(rgb(8, 8, 22), rgb(44, 30, 60), t);

        double dx = x - MOON_X, dy = y - MOON_Y, d = Math.sqrt(dx * dx + dy * dy);
        c = mix(c, rgb(210, 208, 180), 0.55 * Math.exp(-d * d / (2 * 13.0 * 13.0)));

        if (d <= MOON_R) {
            double crater = noise2(x * 0.5, y * 0.5, 3);
            c = shade(mix(rgb(232, 228, 208), rgb(196, 192, 170), crater), 1.0 - 0.1 * (d / MOON_R));
        } else if (d < MOON_R + 1.0) {
            c = mix(c, rgb(255, 252, 235), clamp01((MOON_R + 1.0 - d) * 0.8));
        } else if (d > 9.0 && hash(x, y) % 97 == 0) {
            double tw = 0.5 + 0.5 * Math.sin(timeClock * 3.0 + rand01(x, y) * 6.2831);
            c = mix(c, rgb(255, 255, 255), tw * 0.8);
        }

        double cloudCover = cloudAt(x, y);
        if (cloudCover > 0) {
            int rim = mix(rgb(70, 64, 86), rgb(190, 186, 200), clamp01(0.55 * Math.exp(-d * d / 200.0)));
            c = mix(c, rim, cloudCover * 0.85);
        }
        return c;
    }

    private double cloudAt(int x, int y) {
        double[][] clouds = { { 14, 1.0, 1.1 }, { 9, 0.75, 1.7 }, { 19, 0.85, 0.7 } };
        double cover = 0.0;
        for (int i = 0; i < clouds.length; i++) {
            double cy = clouds[i][0], s = clouds[i][1], speed = clouds[i][2];
            double cx = ((timeClock * speed + i * 37.0) % 110.0) - 15.0;
            double u = (x - cx) / s, v = (y - cy) / s;
            double[][] lobes = { { 0, 0, 2.6 }, { -3.2, 0.6, 2.0 }, { 3.0, 0.7, 2.1 }, { 0.3, -0.9, 1.9 } };
            for (double[] lb : lobes) {
                double dd = Math.hypot(u - lb[0], (v - lb[1]) * 1.3);
                if (dd < lb[2]) cover = Math.max(cover, clamp01((lb[2] - dd) * 1.3));
            }
        }
        return cover;
    }

    private static int farHillPixel(double depth) {
        return shade(mix(rgb(28, 30, 40), rgb(16, 18, 26), clamp01(depth)), 1.0);
    }

    private int groundPixel(int x, int y) {
        int c = mix(rgb(40, 44, 40), rgb(22, 24, 24), clamp01((y - 29) / 14.0));
        int h = hash(x, y);
        if (h % 11 == 0) c = shade(c, 1.18);
        else if (h % 11 == 1) c = mix(c, rgb(60, 50, 38), 0.5); // bare dirt patch
        else if (h % 23 == 0) c = shade(c, 0.8);
        return c;
    }

    // =====================================================================
    //  BAKED: gnarled trees, headstones, the distant crypt, and the fence
    // =====================================================================

    private void buildStatic() {
        int[] standX = { 5, 13, 21, 33, 48, 55, 62, 70, 76 };
        for (int i = 0; i < standX.length; i++) headstone(standX[i], i);

        tree(10, (int) Math.round(hillNear(10)), 101);
        tree(66, (int) Math.round(hillNear(66)), 202);

        crypt(40, (int) Math.round(hillFar(40)));
        fence();

        cat(54, (int) Math.round(hillNear(54)));
    }

    private void putNear(int x, int y, int c) {
        if (x >= 0 && x < PW && y >= 0 && y < PH) near[y * PW + x] = 0xFF000000 | c;
    }

    private void lineNear(double x0, double y0, double x1, double y1, int c, int thick) {
        int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))) + 1;
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            int x = (int) Math.round(x0 + (x1 - x0) * t), y = (int) Math.round(y0 + (y1 - y0) * t);
            putNear(x, y, c);
            if (thick > 1) putNear(x + 1, y, c);
        }
    }

    /** One of four headstone shapes, moonlit on the side facing the moon and mossy on the other. */
    private void headstone(int x, int i) {
        int baseY = (int) Math.round(hillNear(x));
        int type = hash(x, 5) % 4;
        int h = 4 + hash(x, 6) % 4;
        int w = (type == 2) ? 2 : 3;
        int lightDir = MOON_X >= x ? 1 : -1;

        for (int dy = 0; dy < h; dy++) {
            int y = baseY - dy;
            boolean roundedTop = (type == 0) && dy == h - 1;
            for (int dx = -w / 2; dx <= w / 2; dx++) {
                if (type == 1 && dy < h - 2 && dx != 0) continue;       // a plain cross
                if (type == 3 && dy == h - 1 && Math.abs(dx) > 0) continue; // a pointed obelisk cap
                if (roundedTop && Math.abs(dx) == w / 2) continue;
                int lit = dx * lightDir > 0 ? shade(STONE, 1.08) : STONE_DARK;
                if (hash(x + dx, y) % 7 == 0) lit = mix(lit, MOSS, 0.5);
                putNear(x + dx, y, lit);
            }
        }
        if (type == 1) { // cross arm
            for (int dx = -1; dx <= 1; dx++) putNear(x + dx, baseY - h + 2, dx * lightDir >= 0 ? shade(STONE, 1.08) : STONE_DARK);
        }
        if (i % 4 == 3) { // one grave sits askew, broken and sunken
            putNear(x - 1, baseY, STONE_DARK);
            putNear(x + 2, baseY, STONE_DARK);
        }
    }

    /** A bare, fractally gnarled tree: thick near the trunk, splitting into wild, jittered branches. */
    private void tree(int x, int baseY, int seed) {
        gnarl(x, baseY, -0.08, 9.5, 6, seed);
    }

    private void gnarl(double x, double y, double ang, double len, int depth, int seed) {
        double x2 = x + Math.sin(ang) * len, y2 = y - Math.cos(ang) * len;
        double dx = MOON_X - x, dy = MOON_Y - y;
        double rim = clamp01(0.5 - Math.abs(Math.atan2(dy, dx) - (ang + Math.PI / 2)) * 0.3);
        int col = mix(BARK, rgb(120, 118, 128), rim * 0.4 * (depth < 3 ? 1.0 : 0.3));
        lineNear(x, y, x2, y2, col, depth >= 4 ? 2 : 1);
        if (depth == 0) return;
        double j = (rand01(seed, depth) - 0.5) * 1.1; // gnarlier jitter than a healthy tree
        gnarl(x2, y2, ang - 0.62 + j, len * 0.72, depth - 1, seed * 31 + 1);
        gnarl(x2, y2, ang + 0.58 + j, len * 0.70, depth - 1, seed * 31 + 2);
        if (depth >= 3 && rand01(seed, depth + 9) < 0.55) gnarl(x2, y2, ang + j * 1.4, len * 0.6, depth - 1, seed * 31 + 3);
    }

    private static final int CRYPT_X = 40;
    private static final int CRYPT_WINDOW_Y_OFFSET = -3;

    private void crypt(int cx, int baseY) {
        for (int dy = 0; dy < 6; dy++) {
            for (int dx = -5; dx <= 5; dx++) putNear(cx + dx, baseY - dy, dy == 0 ? STONE_DARK : WOOD_BLACK);
        }
        for (int dx = -6; dx <= 6; dx++) putNear(cx + dx, baseY - 6, STONE_DARK); // roof ledge
        for (int dx = -3; dx <= 3; dx++) putNear(cx + dx, baseY - 7, shade(STONE_DARK, 0.8));
        putNear(cx, baseY - 8, STONE_DARK); // peak
        for (int dy = 1; dy <= 3; dy++) putNear(cx, baseY - dy, WOOD_BLACK); // door shadow
    }

    private void fence() {
        int y = PH - 5;
        int iron = rgb(20, 20, 24);
        for (int x = 0; x < PW; x++) {
            if (x >= 36 && x <= 43) continue; // the gate gap
            if (x % 4 == 0) {
                putNear(x, y, iron);
                putNear(x, y - 1, iron);
                putNear(x, y - 2, iron);
                putNear(x, y - 3, shade(iron, 1.3)); // pointed finial
            } else if (x % 4 == 2) {
                putNear(x, y - 1, shade(iron, 0.85));
            }
        }
    }

    private static final int CAT_X = 54;
    private int catBaseY;

    private void cat(int x, int baseY) {
        catBaseY = baseY;
        int fur = rgb(14, 13, 16);
        putNear(x - 1, baseY - 1, fur);
        putNear(x, baseY - 1, fur);
        putNear(x + 1, baseY - 1, fur);
        putNear(x - 1, baseY - 2, fur);
        putNear(x, baseY - 2, fur);
        putNear(x - 1, baseY - 3, fur); // ear
        putNear(x + 1, baseY - 3, fur); // ear
        for (int i = 0; i < 2; i++) putNear(x + 2 + i, baseY - 1 + (i % 2), fur); // curled tail
    }

    // =====================================================================
    //  THINGS THAT MOVE: ground fog, wisps, bats, the witch, and the cat's eyes
    // =====================================================================

    private void paintFog() {
        for (int y = 32; y < PH; y++) {
            double band = clamp01(1.0 - Math.abs(y - 37) / 9.0);
            for (int x = 0; x < PW; x++) {
                double n = noise2(x * 0.12 + timeClock * 1.1, y * 0.25, 9);
                double a = band * clamp01(n - 0.35) * 0.55;
                if (a > 0.02) px[y * PW + x] = mix(px[y * PW + x], rgb(150, 160, 164), a);
            }
        }
    }

    private void paintWisps() {
        for (int i = 0; i < 6; i++) {
            double speed = 0.15 + 0.1 * rand01(i, 440);
            double x = (((rand01(i, 441) * PW) + timeClock * speed * 6.0) % PW);
            double y = 34 + rand01(i, 442) * 8 + 2.0 * Math.sin(timeClock * 0.6 + i);
            double a = 0.35 + 0.35 * Math.sin(timeClock * 1.4 + i * 1.7);
            blend((int) x, (int) y, rgb(140, 220, 160), Math.max(0.0, a));
        }
    }

    private void paintBats() {
        for (int i = 0; i < 5; i++) {
            double speed = 7.0 + i * 0.6;
            double bx = ((timeClock * speed + i * 23.0) % (PW + 20.0)) - 10.0;
            double by = 5 + (i % 3) * 3 + 3.0 * Math.sin(timeClock * 0.8 + i * 1.3);
            boolean up = Math.sin(timeClock * 10.0 + i * 2.0) > 0;
            int c = rgb(10, 9, 13);
            plot((int) bx, (int) by, c);
            plot((int) bx - 1, (int) by + (up ? -1 : 0), c);
            plot((int) bx + 1, (int) by + (up ? -1 : 0), c);
            plot((int) bx - 2, (int) by + (up ? -2 : 1), c);
            plot((int) bx + 2, (int) by + (up ? -2 : 1), c);
        }
    }

    /** She crosses the whole sky on a slow loop, cape snapping, broom bristles trailing behind. */
    private void paintWitch() {
        double period = 26.0;
        double t = (timeClock % period) / period;
        double x = -8 + t * (PW + 16);
        double y = 8 + 3.0 * Math.sin(t * Math.PI * 2.0 * 1.5) - 2.0;
        int cx = (int) Math.round(x), cy = (int) Math.round(y);
        int dark = rgb(14, 12, 18);

        double flap = Math.sin(timeClock * 2.0);
        for (int i = 0; i <= 4; i++) { // cape, snapping behind her
            int fx = cx - 2 - i;
            int fy = cy + 1 + (int) Math.round(Math.sin(timeClock * 3.0 + i) * 0.6 + flap * 0.4);
            plot(fx, fy, dark);
        }
        plot(cx, cy, dark);            // body
        plot(cx, cy - 1, dark);        // shoulders
        plot(cx - 1, cy - 2, dark);    // hat brim
        plot(cx, cy - 2, dark);
        plot(cx + 1, cy - 2, dark);
        plot(cx, cy - 3, dark);        // hat point
        for (int i = -3; i <= 3; i++) { // the broom, bristles trailing
            int brx = cx + 2 + i, bry = cy + 1 + (int) Math.round(Math.abs(i) * 0.25);
            plot(brx, bry, i <= 0 ? rgb(120, 86, 50) : rgb(96, 70, 30));
        }
    }

    private void paintCatEyes() {
        boolean blink = Math.sin(timeClock * 1.3) > 0.92;
        if (blink) return;
        double glow = 0.6 + 0.4 * Math.sin(timeClock * 4.0);
        int eye = mix(rgb(120, 210, 70), rgb(200, 255, 140), glow);
        plot(CAT_X - 1, catBaseY - 2, eye);
        plot(CAT_X, catBaseY - 2, eye);
    }

    private void paintHouseWindow() {
        double flicker = 0.5 + 0.5 * Math.sin(timeClock * 5.0) * Math.sin(timeClock * 1.7 + 1.0);
        int warm = mix(rgb(120, 70, 20), rgb(255, 180, 70), clamp01(flicker));
        int baseY = (int) Math.round(hillFar(CRYPT_X));
        plot(CRYPT_X, baseY + CRYPT_WINDOW_Y_OFFSET, warm);
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

    private void blend(int x, int y, int c, double a) {
        if (x >= 0 && x < PW && y >= 0 && y < PH) px[y * PW + x] = mix(px[y * PW + x], c, a);
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

    private static double rand01(int a, int b) { return hash(a, b) / (double) 0x7fffffff; }

    private static double noise2(double x, double y, int seed) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = rand01(ix + iy * 57, seed),       b = rand01(ix + 1 + iy * 57, seed);
        double c = rand01(ix + (iy + 1) * 57, seed), d = rand01(ix + 1 + (iy + 1) * 57, seed);
        return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
    }
}