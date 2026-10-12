// TODO Improve this dragon its a little not quite dragon enough

public class DragonGoldLoader extends Loader {
    private static final StatusStage[] DRAGON_STAGES = {
        new StatusStage(25, "Delving into the ancient lair:"),
        new StatusStage(50, "Counting the hoarded gold:"),
        new StatusStage(75, "Letting the old wyrm sleep:"),
        new StatusStage(100, "The Hoard Is Guarded!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;
    private static final double FLOOR_Y = 42.0;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int ROCK_DARK  = rgb(26,  22,  26);
    private static final int ROCK_MID   = rgb(44,  38,  42);
    private static final int ROCK_LIT   = rgb(78,  62,  56);
    private static final int GOLD_DEEP  = rgb(120, 86,  24);
    private static final int GOLD_MID   = rgb(188, 142, 46);
    private static final int GOLD_HI    = rgb(255, 214, 110);
    private static final int SCALE_DARK = rgb(34,  70,  46);
    private static final int SCALE_MID  = rgb(52,  104, 64);
    private static final int SCALE_HI   = rgb(96,  160, 92);
    private static final int BELLY      = rgb(168, 186, 96);
    private static final int WING_SKIN  = rgb(58,  42,  56);
    private static final int HORN       = rgb(222, 214, 196);

    private static final int[] GEM_COLORS = {
        rgb(210, 30, 40), rgb(40, 110, 220), rgb(150, 40, 200), rgb(40, 180, 100), rgb(230, 200, 40)
    };

    // The dragon's spine: a chain of control points (x, y, bodyRadius) from the tail's curled
    // tip to the head, subdivided into a dense polyline once at startup for smooth distance tests.
    private static final double[][] SPINE_CTRL = {
        { 24, 33, 2.4 }, { 20, 28, 3.0 }, { 29, 30, 4.8 }, { 38, 25, 6.2 },
        { 46, 19, 7.6 }, { 54, 22, 6.6 }, { 59, 27, 4.2 }, { 64, 30, 5.6 }
    };
    private static final int SPINE_SUBDIV = 10;
    private static final int HEAD_X = 64, HEAD_Y = 30;
    private static final int NOSTRIL_X = 68, NOSTRIL_Y = 32;

    private static final int[] TORCH_X = { 9, 71 };
    private static final int[] TORCH_Y = { 16, 16 };

    // Everything baked once: the gold pile's coin texture, embedded gems, and treasure poking out.
    // Packed ARGB, 0 = empty, so a frame can tell "painted black" apart from "nothing painted".
    private final int[] near = new int[PW * PH];
    private boolean ready = false;
    private int[] gemX, gemY, gemC;
    private double[] spineXs, spineYs, spineRs;

    private final int[] px = new int[PW * PH];

    public DragonGoldLoader() {
        // This uses 80x22 specifically
        super(DRAGON_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.035;
        if (!ready) { buildSpine(); buildStatic(); ready = true; }

        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                px[y * PW + x] = scenePixel(x, y);
            }
        }
        for (int i = 0; i < near.length; i++) {
            if ((near[i] >>> 24) != 0) px[i] = near[i] & 0xFFFFFF;
        }

        paintTorchGlow();
        paintSparkles();
        paintDragon();
        paintSmoke();

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
    //  LIVE BACKDROP: cave rock walls, the ceiling, and the gold pile's floor
    // =====================================================================

    private static double moundTop(int x) {
        double bump = 15.0 * Math.exp(-((x - 46.0) * (x - 46.0)) / (2 * 24.0 * 24.0));
        double ripple = 1.2 * Math.sin(x * 0.5) + 0.8 * Math.sin(x * 0.23 + 1.0);
        return 37.0 - bump + ripple * 0.3;
    }

    private int scenePixel(int x, int y) {
        double mt = moundTop(x);
        if (y < mt) return cavePixel(x, y);
        if (y < FLOOR_Y) return GOLD_DEEP; // always overwritten by the baked pile texture below
        return floorPixel(x, y);
    }

    private int cavePixel(int x, int y) {
        double t = clamp01(y / 30.0);
        int c = mix(ROCK_DARK, ROCK_MID, t);
        double n = noise2(x * 0.15, y * 0.15, 11);
        if (n > 0.8) c = ROCK_LIT;
        else if (n > 0.68) c = shade(c, 0.8);
        else if (n < 0.12) c = shade(c, 1.12);
        double sb = stalactiteBottom(x);
        if (y < sb) c = shade(mix(c, ROCK_DARK, 0.6), 0.85);
        return c;
    }

    private static double stalactiteBottom(int x) {
        int cellIdx = Math.floorDiv(x, 7);
        double cx = cellIdx * 7 + 3.5 + (rand01(cellIdx, 301) - 0.5) * 2.0;
        double halfw = 1.3 + rand01(cellIdx, 302) * 0.8;
        double len = 2.0 + rand01(cellIdx, 300) * 7.0;
        double d = Math.abs(x - cx);
        if (d > halfw) return 0;
        return len * (1.0 - d / halfw);
    }

    private int floorPixel(int x, int y) {
        int c = mix(rgb(40, 36, 34), rgb(20, 18, 18), (y - FLOOR_Y) / 2.0);
        if (hash(x, y) % 9 == 0) c = shade(c, 1.2);
        return c;
    }

    // =====================================================================
    //  BAKED: the gold coin pile, its embedded gems, and treasure poking out
    // =====================================================================

    private void buildStatic() {
        buildPile();
        buildTreasureItems();
    }

    private void buildPile() {
        int count = 0;
        for (int x = 0; x < PW; x++) if (hash(x, 777) % 11 == 0) count++;
        gemX = new int[count];
        gemY = new int[count];
        gemC = new int[count];
        int gi = 0;

        for (int x = 0; x < PW; x++) {
            int top = (int) Math.ceil(moundTop(x));
            for (int y = top; y < FLOOR_Y; y++) {
                double depth = (y - top) / Math.max(1.0, FLOOR_Y - top);
                double n = noise2(x * 0.5, y * 0.5, 21);
                int base = mix(GOLD_MID, GOLD_DEEP, clamp01(depth * 0.9));
                int hi = mix(GOLD_HI, GOLD_MID, clamp01(depth));
                int c = n > 0.55 ? hi : base;
                double ring = Math.abs(noise2(x * 0.9 + 3, y * 0.9 + 7, 22) - 0.5);
                if (ring < 0.05) c = shade(c, 0.72);
                putNear(x, y, c);
            }
            if (hash(x, 777) % 11 == 0) {
                int gy = top + 1 + hash(x, 778) % 3;
                if (gy < FLOOR_Y) {
                    int gc = GEM_COLORS[hash(x, 779) % GEM_COLORS.length];
                    putNear(x, gy, shade(gc, 1.2));
                    gemX[gi] = x;
                    gemY[gi] = gy;
                    gemC[gi] = gc;
                    gi++;
                }
            }
        }
    }

    /** A sword driven hilt-up into the pile, and a tipped goblet, clear of the dragon's bulk. */
    private void buildTreasureItems() {
        int sx = 7, sy = (int) Math.ceil(moundTop(sx)) - 1;
        for (int i = 0; i < 4; i++) putNear(sx, sy - i, rgb(190, 192, 200));
        putNear(sx - 2, sy + 1, rgb(210, 180, 70));
        putNear(sx + 2, sy + 1, rgb(210, 180, 70));
        putNear(sx, sy + 2, rgb(110, 74, 38));
        putNear(sx, sy + 3, rgb(90, 60, 30));

        int gx = 76, gy = (int) Math.ceil(moundTop(gx)) - 1;
        putNear(gx, gy, GOLD_HI);
        putNear(gx - 1, gy + 1, GOLD_MID);
        putNear(gx + 1, gy + 1, GOLD_MID);
        putNear(gx, gy + 1, GOLD_MID);
    }

    private void putNear(int x, int y, int c) {
        if (x >= 0 && x < PW && y >= 0 && y < PH) near[y * PW + x] = 0xFF000000 | c;
    }

    // =====================================================================
    //  THE DRAGON: a sleeping wyrm curled across the top of the hoard
    // =====================================================================

    private void buildSpine() {
        int segs = SPINE_CTRL.length - 1;
        int n = segs * SPINE_SUBDIV + 1;
        spineXs = new double[n];
        spineYs = new double[n];
        spineRs = new double[n];
        int idx = 0;
        for (int i = 0; i < segs; i++) {
            double[] a = SPINE_CTRL[i], b = SPINE_CTRL[i + 1];
            for (int s = 0; s < SPINE_SUBDIV; s++) {
                double t = s / (double) SPINE_SUBDIV;
                double st = t * t * (3 - 2 * t); // smoothstep ease between control points
                spineXs[idx] = a[0] + (b[0] - a[0]) * st;
                spineYs[idx] = a[1] + (b[1] - a[1]) * st;
                spineRs[idx] = a[2] + (b[2] - a[2]) * st;
                idx++;
            }
        }
        double[] last = SPINE_CTRL[segs];
        spineXs[idx] = last[0];
        spineYs[idx] = last[1];
        spineRs[idx] = last[2];
    }

    private void paintDragon() {
        double breath = Math.sin(timeClock * 1.1);
        for (int y = 9; y <= 37; y++) {
            for (int x = 14; x <= 76; x++) {
                double best = Double.MAX_VALUE;
                int bi = 0;
                for (int i = 0; i < spineXs.length; i++) {
                    double dx = x - spineXs[i], dy = y - spineYs[i];
                    double d2 = dx * dx + dy * dy;
                    if (d2 < best) { best = d2; bi = i; }
                }
                double dist = Math.sqrt(best);
                double tGlobal = bi / (double) (spineXs.length - 1);
                double breathBonus = (tGlobal > 0.3 && tGlobal < 0.75)
                    ? breath * 0.6 * Math.sin((tGlobal - 0.3) / 0.45 * Math.PI) : 0.0;
                double r = spineRs[bi] + breathBonus;

                if (dist <= r) {
                    plot(x, y, dragonBodyColor(x, y, bi, dist, r, tGlobal));
                } else if (dist <= r + 0.8) {
                    double a = 1.0 - (dist - r) / 0.8;
                    blend(x, y, dragonBodyColor(x, y, bi, dist, r, tGlobal), a);
                }
            }
        }
        paintWings();
        paintSpikes();
        paintHead();
    }

    private int dragonBodyColor(int x, int y, int bi, double dist, double r, double tGlobal) {
        double dy = y - spineYs[bi];
        double k = 1.0 - clamp01(dist / Math.max(0.5, r));
        int base = mix(SCALE_DARK, SCALE_MID, k);

        if (dy > r * 0.2) base = mix(base, BELLY, clamp01((dy - r * 0.2) / Math.max(0.5, r * 0.8)));

        double scalePattern = noise2(x * 0.9 + tGlobal * 2.0, y * 0.9, 33);
        if (scalePattern > 0.6) base = mix(base, SCALE_HI, 0.4);
        else if (scalePattern < 0.25) base = shade(base, 0.88);

        double goldGlow = clamp01(dy / (r + 2.0));
        base = mix(base, GOLD_MID, clamp01(goldGlow * 0.22));

        if (dy < -r * 0.3 && dist / r > 0.55) base = shade(base, 1.2);
        if (tGlobal > 0.86) base = mix(base, rgb(150, 176, 104), 0.35); // a sandier, lighter snout
        return base;
    }

    private static final double[][] WING_CTRL = {
        { 33, 25, 1.6 }, { 39, 18, 3.4 }, { 47, 14, 4.0 }, { 53, 18, 3.2 }, { 57, 23, 1.6 }
    };

    /** A folded wing draped like a cloak over the back's hump, with a few soft creases. */
    private void paintWings() {
        int segs = WING_CTRL.length - 1, subdiv = 10;
        int n = segs * subdiv + 1;
        double[] wx = new double[n], wy = new double[n], wr = new double[n];
        int idx = 0;
        for (int i = 0; i < segs; i++) {
            double[] a = WING_CTRL[i], b = WING_CTRL[i + 1];
            for (int s = 0; s < subdiv; s++) {
                double t = s / (double) subdiv, st = t * t * (3 - 2 * t);
                wx[idx] = a[0] + (b[0] - a[0]) * st;
                wy[idx] = a[1] + (b[1] - a[1]) * st;
                wr[idx] = a[2] + (b[2] - a[2]) * st;
                idx++;
            }
        }
        double[] last = WING_CTRL[segs];
        wx[idx] = last[0]; wy[idx] = last[1]; wr[idx] = last[2];

        int wingBase = mix(WING_SKIN, SCALE_DARK, 0.45);
        for (int y = 11; y <= 28; y++) {
            for (int x = 30; x <= 60; x++) {
                double best = Double.MAX_VALUE;
                int bi = 0;
                for (int i = 0; i < wx.length; i++) {
                    double dx = x - wx[i], dy = y - wy[i];
                    double d2 = dx * dx + dy * dy;
                    if (d2 < best) { best = d2; bi = i; }
                }
                double dist = Math.sqrt(best), r = wr[bi];
                double tLocal = bi / (double) (wx.length - 1);
                double crease = 0.5 + 0.5 * Math.sin(tLocal * Math.PI * 4.0);
                int c = wingBase;
                if (crease > 0.8) c = shade(c, 0.78);
                else if (crease < 0.15) c = shade(c, 1.15);

                if (dist <= r) {
                    double k = 1.0 - clamp01(dist / Math.max(0.5, r));
                    plot(x, y, mix(shade(c, 0.82), c, k));
                } else if (dist <= r + 0.8) {
                    blend(x, y, c, (1.0 - (dist - r) / 0.8) * 0.9);
                }
            }
        }
    }

    /** A row of back spikes from the hip to the base of the neck, skipping the tail and head. */
    private void paintSpikes() {
        for (int i = 0; i < spineXs.length; i += 6) {
            double t = i / (double) (spineXs.length - 1);
            if (t < 0.18 || t > 0.92) continue;
            double sxF = spineXs[i], syF = spineYs[i] - spineRs[i] * 0.78;
            int sx = (int) Math.round(sxF), sy = (int) Math.round(syF);
            int h = 2 + (int) Math.round(spineRs[i] * 0.32);
            for (int k = 0; k < h; k++) plot(sx, sy - k, shade(SCALE_DARK, 1.3 - 0.05 * k));
        }
    }

    private void paintHead() {
        plot(HEAD_X - 3, HEAD_Y - 7, HORN);
        plot(HEAD_X - 3, HEAD_Y - 8, HORN);
        plot(HEAD_X,     HEAD_Y - 7, HORN);
        plot(HEAD_X,     HEAD_Y - 8, HORN);

        // a closed eye: one dark horizontal slit with a soft brow-light crease just above it
        plot(HEAD_X + 1, HEAD_Y - 3, mix(SCALE_HI, GOLD_MID, 0.3));
        plot(HEAD_X + 2, HEAD_Y - 3, mix(SCALE_HI, GOLD_MID, 0.3));
        plot(HEAD_X + 1, HEAD_Y - 2, rgb(16, 20, 16));
        plot(HEAD_X + 2, HEAD_Y - 2, rgb(16, 20, 16));
        plot(HEAD_X + 3, HEAD_Y - 2, rgb(16, 20, 16));

        plot(NOSTRIL_X, NOSTRIL_Y, rgb(20, 24, 18));
        plot(NOSTRIL_X, NOSTRIL_Y - 1, shade(rgb(150, 176, 104), 0.7));
    }

    // =====================================================================
    //  THINGS THAT MOVE: torchlight, gem glints, and nostril smoke
    // =====================================================================

    private void paintTorchGlow() {
        for (int t = 0; t < TORCH_X.length; t++) {
            double flick = 0.7 + 0.3 * Math.sin(timeClock * 6.0 + t * 2.1) * Math.sin(timeClock * 2.3 + t);
            for (int y = TORCH_Y[t] - 6; y <= TORCH_Y[t] + 10; y++) {
                for (int x = TORCH_X[t] - 7; x <= TORCH_X[t] + 7; x++) {
                    double d = Math.hypot(x - TORCH_X[t], (y - TORCH_Y[t]) * 1.3);
                    double g = Math.exp(-d * d / 40.0) * flick;
                    if (g > 0.02) blend(x, y, rgb(255, 160, 60), clamp01(g * 0.5));
                }
            }
            plot(TORCH_X[t], TORCH_Y[t] + 2, rgb(90, 60, 40)); // wall bracket
            int flameC = mix(rgb(255, 200, 80), rgb(255, 90, 20), 0.5 + 0.5 * Math.sin(timeClock * 8.0 + t));
            plot(TORCH_X[t], TORCH_Y[t], flameC);
            plot(TORCH_X[t], TORCH_Y[t] - 1, shade(flameC, 0.8));
        }
    }

    private void paintSparkles() {
        for (int i = 0; i < gemX.length; i++) {
            double tw = 0.5 + 0.5 * Math.sin(timeClock * 3.0 + rand01(i, 500) * 6.2831);
            if (tw > 0.75) plot(gemX[i], gemY[i], mix(gemC[i], rgb(255, 255, 255), (tw - 0.75) * 3.0));
        }
        double pulse = 0.5 + 0.5 * Math.sin(timeClock * 1.5);
        for (int x = 0; x < PW; x++) {
            int top = (int) Math.ceil(moundTop(x));
            for (int y = top; y < top + 2 && y < FLOOR_Y; y++) blend(x, y, GOLD_HI, 0.12 * pulse);
        }
    }

    private void paintSmoke() {
        for (int i = 0; i < 4; i++) {
            double period = 6.0;
            double phase = (timeClock + i * period / 4.0) % period;
            double t = phase / period;
            if (t > 0.85) continue;
            double x = NOSTRIL_X + 2.0 * t + 1.0 * Math.sin(t * 6.0 + i);
            double y = NOSTRIL_Y - t * 10.0;
            double size = 1.2 + t * 3.0;
            double alpha = clamp01((1.0 - t) * 0.75);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.hypot(dx, dy) <= size * 0.5) {
                        blend((int) x + dx, (int) y + dy, rgb(220, 220, 215), alpha * 0.7);
                    }
                }
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
        if (top == bot) return sb.append('m').append('█').append(RESET).toString();
        return sb.append(";48;2;").append((bot >> 16) & 255).append(';').append((bot >> 8) & 255).append(';').append(bot & 255)
                 .append('m').append('▀').append(RESET).toString();
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