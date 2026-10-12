public class HighwayLoader extends Loader {
    private static final StatusStage[] HIGHWAY_STAGES = {
        new StatusStage(25, "Merging onto the 405:"),
        new StatusStage(50, "Painting lane lines into the haze:"),
        new StatusStage(75, "Backing up traffic for miles:"),
        new StatusStage(100, "SigAlert - Nobody's Moving!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;

    private static final int HORIZON = 14;     // where the road meets the skyline
    private static final double SUN_X = 52.0, SUN_Y = 10.0;
    private static final int LANES = 4;
    private static final int SLOTS = 11;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int[] CAR_COLORS = {
        rgb(230, 230, 232), rgb(30, 30, 34), rgb(178, 32, 32), rgb(40, 70, 150), rgb(150, 150, 158), rgb(214, 182, 40)
    };

    private final int[] px = new int[PW * PH];

    public HighwayLoader() {
        // This uses 80x22 specifically
        super(HIGHWAY_STAGES, WIDTH, HEIGHT);
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

        paintSignGantry();
        paintPalmTrees();
        paintHelicopters();
        paintBirds();
        paintTraffic();

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
    //  ARCHITECTURE: hazy golden-hour sky, downtown silhouette, the freeway
    // =====================================================================

    private int scenePixel(int x, int y) {
        if (y < HORIZON) return skyPixel(x, y);
        double y0 = y - HORIZON;
        double hw = halfWidthAtY0(y0);
        return Math.abs(x - 40) <= hw ? roadSurfacePixel(x, y0, hw) : shoulderPixel(x, y, y0, hw);
    }

    private int skyPixel(int x, int y) {
        double t = clamp01(y / (double) HORIZON);
        int top = rgb(58, 78, 148), mid = rgb(196, 122, 112), bottom = rgb(255, 176, 112);
        int c = t < 0.5 ? mix(top, mid, t / 0.5) : mix(mid, bottom, (t - 0.5) / 0.5);

        double dx = x - SUN_X, dy = y - SUN_Y, d = Math.sqrt(dx * dx + dy * dy);
        c = mix(c, rgb(255, 230, 180), 0.55 * Math.exp(-d * d / 40.0));
        if (d < 3.0) c = mix(c, rgb(255, 250, 230), clamp01((3.0 - d) * 1.5));

        double haze = clamp01((y - (HORIZON - 8)) / 8.0);
        if (haze > 0) c = mix(c, rgb(184, 154, 124), haze * 0.55);

        int bld = skylineAt(x, y);
        if (bld >= 0) c = mix(bld, rgb(184, 154, 124), 0.35);
        return c;
    }

    private static int skylineAt(int x, int y) {
        int topY = HORIZON - (2 + hash(x / 3, 3) % 9);
        if (y < topY) return -1;
        if ((y - topY) % 3 == 1 && x % 2 == 0 && hash(x / 2, 55) % 5 == 0) return rgb(255, 208, 118); // lit window
        return rgb(38, 34, 46);
    }

    private static double halfWidthAtY0(double y0) {
        return 3 + 35 * Math.pow(clamp01(y0 / 29.0), 1.25);
    }

    /** Dashed lane lines that thin toward the horizon, a warm glare streak, and a little heat shimmer. */
    private int roadSurfacePixel(int x, double y0, double hw) {
        double u = (x - 40) / hw;
        double laneU = u * LANES / 2.0;
        int boundary = (int) Math.round(laneU);
        double frac = Math.abs(laneU - boundary);
        boolean isEdge = Math.abs(boundary) >= LANES / 2;
        int dashRow = (int) Math.floor(18.0 / Math.max(1.0, y0));

        int far = rgb(84, 78, 90), near = rgb(46, 44, 52);
        int base = mix(far, near, clamp01(y0 / 29.0));
        int h = hash(x, (int) (y0 * 3));
        int c = shade(base, 0.94 + 0.1 * ((h % 5) / 4.0));

        double glare = Math.exp(-Math.pow(u * 3.0, 2)) * 0.32 * clamp01(1.0 - y0 / 40.0);
        c = mix(c, rgb(255, 210, 150), glare);

        double shimmer = 0.05 * Math.sin(y0 * 2.0 + x * 0.5 + timeClock * 6.0) * clamp01(1.0 - y0 / 9.0);
        c = shade(c, 1.0 + shimmer);

        if (frac < 0.045) c = isEdge ? rgb(232, 228, 220) : (dashRow % 2 == 0 ? rgb(220, 216, 208) : c);
        return c;
    }

    private int shoulderPixel(int x, int y, double y0, double hw) {
        double beyond = Math.abs(x - 40) - hw;
        int dirt = mix(rgb(158, 134, 84), rgb(122, 102, 62), clamp01(y0 / 29.0));
        int c = shade(dirt, 0.88 + 0.16 * ((hash(x, y) % 5) / 4.0));
        if (beyond < 0.9) c = (hash(x, y) % 2 == 0) ? rgb(196, 196, 202) : rgb(150, 150, 158); // guardrail
        return c;
    }

    // =====================================================================
    //  ROADSIDE: an overhead sign gantry and a row of swaying palm trees
    // =====================================================================

    private void paintSignGantry() {
        double y0 = 9.0, y = HORIZON + y0;
        double hw = halfWidthAtY0(y0);
        int leftX = (int) Math.round(40 - hw - 2), rightX = (int) Math.round(40 + hw + 2);
        int beamY = (int) Math.round(y) - 9;
        int post = rgb(120, 120, 128);

        for (int yy = beamY; yy <= (int) Math.round(y); yy++) {
            plot(leftX, yy, post);
            plot(rightX, yy, post);
        }
        for (int xx = leftX; xx <= rightX; xx++) plot(xx, beamY, post);

        int signW = Math.min(26, rightX - leftX - 4);
        int signL = 40 - signW / 2, signTop = beamY + 1, signBot = beamY + 4;
        for (int yy = signTop; yy <= signBot; yy++) {
            for (int xx = signL; xx <= signL + signW; xx++) plot(xx, yy, rgb(18, 108, 58));
        }
        for (int xx = signL + 2; xx < signL + signW - 2; xx += 4) {
            plot(xx, signTop + 1, rgb(240, 240, 235));
            plot(xx + 1, signTop + 1, rgb(240, 240, 235));
        }
        for (int xx = signL + 3; xx < signL + signW - 3; xx += 3) plot(xx, signTop + 3, rgb(240, 240, 235));
    }

    private void paintPalmTrees() {
        for (int i = 0; i < 7; i++) {
            double y0 = 2 + i * 4.2;
            drawPalm(-1, y0, i);
            drawPalm(1, y0, i + 50);
        }
    }

    private void drawPalm(int side, double y0, int seed) {
        double y = HORIZON + y0;
        double hw = halfWidthAtY0(y0);
        double x = 40 + side * (hw + 2 + 3 * rand01(seed, 1));
        double scale = 0.3 + 0.9 * (y0 / 29.0);
        int trunkH = Math.max(2, (int) Math.round(7 * scale));
        int trunk = rgb(96, 64, 42);
        int frond = rgb(46, 118, 56);

        int topX = (int) Math.round(x), topY = (int) Math.round(y) - trunkH;
        for (int i = 0; i < trunkH; i++) {
            topX = (int) Math.round(x + Math.sin(i * 0.3) * scale * 0.6);
            plot(topX, (int) Math.round(y) - i, trunk);
        }
        double frondLen = Math.max(1.0, 4.0 * scale);
        for (int f = 0; f < 6; f++) {
            double ang = f * Math.PI / 3 + Math.sin(timeClock * 0.6 + seed) * 0.15;
            int fx = (int) Math.round(topX + Math.cos(ang) * frondLen);
            int fy = (int) Math.round(topY + Math.sin(ang) * frondLen * 0.5 - frondLen * 0.3);
            line(topX, topY, fx, fy, frond, 1);
        }
    }

    // =====================================================================
    //  SKY LIFE: a couple of news choppers circling overhead, and some birds
    // =====================================================================

    private void paintHelicopters() {
        heli(18, 5.0, 0);
        heli(58, 3.5, 1);
    }

    private void heli(double baseX, double baseY, int i) {
        double x = baseX + 4 * Math.sin(timeClock * 0.15 + i * 2.0);
        double y = baseY + 0.6 * Math.sin(timeClock * 0.4 + i);
        int cx = (int) Math.round(x), cy = (int) Math.round(y);
        int body = rgb(28, 28, 34);
        plot(cx - 2, cy, body);
        plot(cx - 1, cy, body);
        plot(cx, cy, body);
        plot(cx + 1, cy, body);
        plot(cx + 2, cy - 1, body); // tail boom kicking up

        double rot = timeClock * 30.0 + i;
        blend((int) Math.round(cx - 3 * Math.cos(rot)), cy - 1, rgb(205, 205, 210), 0.4);
        blend((int) Math.round(cx + 3 * Math.cos(rot)), cy - 1, rgb(205, 205, 210), 0.4);
        blend(cx, cy - 1, rgb(215, 215, 220), 0.55);
        if (Math.sin(timeClock * 6.0 + i * 3.0) > 0.7) plot(cx, cy + 1, rgb(255, 40, 40));
    }

    private void paintBirds() {
        int dark = rgb(42, 40, 52);
        for (int i = 0; i < 2; i++) {
            int bx = (int) (((timeClock * 3.0 + i * 37.0) % 100.0) - 10);
            int by = (int) Math.round(6 + i * 2.5 + Math.sin(timeClock * 0.7 + i));
            plot(bx, by, dark);
            plot(bx - 1, by - 1, dark);
            plot(bx + 1, by - 1, dark);
        }
    }

    // =====================================================================
    //  THE JAM: a car grid packed into every lane, a rig, and a lane-splitter
    // =====================================================================

    private void paintTraffic() {
        double creep = 0.5 + 0.5 * Math.sin(timeClock * 0.5); // the whole jam inches forward together, rarely
        for (int l = 0; l < LANES; l++) {
            for (int s = 0; s < SLOTS; s++) {
                if (rand01(l, s * 7 + 3) < 0.07) continue; // an occasional gap
                double y0 = 2 + s * 2.6 + (rand01(l, s * 13 + 1) - 0.5) * 1.0;
                drawCar(l, y0, hash(l * 31 + s, 91), creep);
            }
        }
        drawTruck(2, 21.5);
    }

    private void drawCar(int lane, double y0, int seed, double creep) {
        double y = HORIZON + y0;
        double hw = halfWidthAtY0(y0);
        double laneW = 2 * hw / LANES;
        double x = 40 - hw + (lane + 0.5) * laneW + (rand01(seed, 17) - 0.5) * laneW * 0.12;
        double scale = 0.35 + 0.95 * (y0 / 29.0);

        int w = Math.max(1, (int) Math.round(3.4 * scale));
        int h = Math.max(1, (int) Math.round(2.6 * scale));
        int cx = (int) Math.round(x), base = (int) Math.round(y);
        int body = CAR_COLORS[seed % CAR_COLORS.length];
        boolean brakeOn = Math.sin(timeClock * 1.1 + seed * 0.7) < (0.5 - 0.35 * creep);

        for (int dx = -w / 2; dx <= w / 2; dx++) {
            for (int dy = 0; dy < h; dy++) plot(cx + dx, base - dy, shade(body, dy == h - 1 ? 1.0 : 0.85));
        }
        if (h >= 3) {
            for (int dx = -w / 2 + 1; dx <= w / 2 - 1; dx++) plot(cx + dx, base - h + 1, rgb(30, 34, 40));
        }
        int bl = brakeOn ? rgb(255, 40, 40) : rgb(90, 20, 20);
        plot(cx - w / 2, base, bl);
        plot(cx + w / 2, base, bl);
        if (brakeOn && scale > 0.55) {
            blend(cx - w / 2 - 1, base, rgb(255, 60, 60), 0.3);
            blend(cx + w / 2 + 1, base, rgb(255, 60, 60), 0.3);
        }
    }

    private void drawTruck(int lane, double y0) {
        double y = HORIZON + y0;
        double hw = halfWidthAtY0(y0);
        double laneW = 2 * hw / LANES;
        double x = 40 - hw + (lane + 0.5) * laneW;
        double scale = 0.35 + 0.95 * (y0 / 29.0);

        int w = Math.max(2, (int) Math.round(5.0 * scale));
        int hCab = Math.max(2, (int) Math.round(3.0 * scale));
        int hBox = Math.max(3, (int) Math.round(5.0 * scale));
        int cx = (int) Math.round(x), base = (int) Math.round(y);
        int box = rgb(222, 222, 228), cab = rgb(58, 58, 64);

        for (int dx = -w / 2; dx <= w / 2; dx++) {
            for (int dy = 0; dy < hBox; dy++) plot(cx + dx, base - hCab - dy, shade(box, dy == hBox - 1 ? 1.05 : 0.92));
        }
        for (int dx = -w / 2; dx <= w / 2; dx++) {
            for (int dy = 0; dy < hCab; dy++) plot(cx + dx, base - dy, shade(cab, dy == hCab - 1 ? 1.1 : 0.9));
        }
        plot(cx - w / 2, base, rgb(255, 50, 50));
        plot(cx + w / 2, base, rgb(255, 50, 50));
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

    private static double rand01(int a, int b) { return hash(a, b) / (double) 0x7fffffff; }
}