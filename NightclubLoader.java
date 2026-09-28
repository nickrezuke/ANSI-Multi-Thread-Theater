public class NightclubLoader extends Loader {
    private static final StatusStage[] CLUB_STAGES = {
        new StatusStage(25, "Rigging the truss & laser array:"),
        new StatusStage(50, "Wiring the LED dance floor:"),
        new StatusStage(75, "Loading the DJ set & hyping the crowd:"),
        new StatusStage(100, "Nightclub Live - Drop Incoming!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;

    // Bands, back to front: ceiling truss -> the stage -> a perspective dance floor
    // (a widening trapezoid) with darkness beyond its edges.
    private static final int CEIL_H     = 4;
    private static final int STAGE_BOT  = 16;
    private static final int STAGE_L = 20, STAGE_R = 60;
    private static final double FLOOR_ORIGIN_Y = 15.0; // y0 = y - this, used for the floor's perspective
    private static final double BEAT = 1.4;             // beats per (scaled) second

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int TRUSS_COLOR = rgb(28, 28, 34);
    private static final int STAGE_DECK  = rgb(20, 18, 24);
    private static final int VOID_COLOR  = rgb(7, 6, 11);
    private static final int SKIN0       = rgb(196, 148, 112);

    private static final int[] PARTY_PALETTE = {
        rgb(255, 40, 140), rgb(60, 220, 255), rgb(255, 210, 40),
        rgb(120, 255, 90), rgb(180, 80, 255), rgb(255, 120, 40)
    };
    private static final int[] FIXTURE_XS = { 12, 28, 52, 68 };
    private static final int DANCER_COUNT = 18;

    private final int[] px = new int[PW * PH];

    public NightclubLoader() {
        // This uses 80x22 specifically
        super(CLUB_STAGES, WIDTH, HEIGHT);
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

        paintWashes();
        paintLasers();
        paintDJ();
        paintDancers();
        paintConfettiAndCO2();
        paintStrobe();

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
    //  ARCHITECTURE: ceiling truss, the stage, and a perspective dance floor
    // =====================================================================

    private int scenePixel(int x, int y) {
        if (y < CEIL_H) return ceilingPixel(x, y);
        if (y < STAGE_BOT) return (x >= STAGE_L && x < STAGE_R) ? stagePixel(x, y) : shade(VOID_COLOR, 1.1);

        double y0 = y - FLOOR_ORIGIN_Y;
        double hw = halfWidthAtY(y0);
        return Math.abs(x - 40) <= hw ? floorTilePixel(x, y0, hw) : VOID_COLOR;
    }

    private int ceilingPixel(int x, int y) {
        int c = shade(TRUSS_COLOR, (x % 6 < 1) ? 0.65 : 1.0);
        for (int fx : FIXTURE_XS) {
            if (Math.abs(x - fx) <= 1 && y >= 1) c = mix(c, rgb(240, 240, 245), 0.55);
        }
        return c;
    }

    private int stagePixel(int x, int y) {
        int sy = y - CEIL_H;
        if (x < STAGE_L + 6 || x >= STAGE_R - 6) return speakerPixel(x, sy, x < STAGE_L + 6);
        if (sy <= 5) return backdropPixel(x, sy);
        if (sy == 6) {
            if ((x - STAGE_L) % 3 == 0) return rainbow(rand01(x, 777) + timeClock * 0.3);
            return TRUSS_COLOR;
        }
        if (x >= 33 && x < 47 && sy >= 8) return boothPixel(x, sy);
        return STAGE_DECK;
    }

    private int speakerPixel(int x, int sy, boolean left) {
        int lx = left ? x - STAGE_L : x - (STAGE_R - 6);
        double d1 = Math.hypot(lx - 3, sy - 4), d2 = Math.hypot(lx - 3, sy - 8);
        double pulse = 0.6 + 0.4 * Math.max(0.0, Math.sin(timeClock * BEAT * 2 * Math.PI));
        double d = Math.min(d1, d2);
        if (d < 2.2) return shade(rgb(120, 110, 150), 0.6 + 0.7 * pulse * (1.0 - d / 2.2));
        if (lx == 0 || lx == 5 || sy == 0 || sy == 11) return rgb(74, 70, 90); // cabinet trim
        return rgb(34, 32, 42);
    }

    /** LED wall showing bouncing equalizer bars that climb from the bottom of the screen. */
    private int backdropPixel(int x, int sy) {
        int bar = (x - STAGE_L) / 2;
        if ((x - STAGE_L) % 2 == 1) return rgb(10, 8, 18); // gap between bars
        double level = 0.3 + 0.7 * (0.5 + 0.5 * Math.sin(timeClock * 3.1 + bar * 1.7) * Math.sin(timeClock * 1.3 + bar * 0.6));
        double fromBottom = (6 - sy) / 6.0;
        if (fromBottom <= level) return shade(rainbow(bar * 0.045 + timeClock * 0.12), 0.75 + 0.25 * fromBottom);
        return rgb(20, 14, 34);
    }

    private int boothPixel(int x, int sy) {
        int lx = x - 33; // 0..13
        if (sy == 8) return rgb(150, 150, 190); // lit top edge of the booth
        for (int deck = 0; deck < 2; deck++) {
            double dcx = deck == 0 ? 3.5 : 9.5;
            double d = Math.hypot(lx - dcx, sy - 10);
            if (d < 1.9) {
                double ang = Math.atan2(sy - 10, lx - dcx);
                boolean marker = Math.cos(ang - timeClock * (deck == 0 ? 4.0 : -4.6)) > 0.85;
                return marker ? rgb(0, 230, 255) : rgb(36, 34, 48);
            }
        }
        if (lx >= 6 && lx <= 7) { // mixer strip with blinking LEDs
            double b = 0.5 + 0.5 * Math.sin(timeClock * 7.0 + sy * 1.9);
            return mix(rgb(22, 20, 32), rgb(255, 60, 200), clamp01(b));
        }
        return rgb(26, 24, 38);
    }

    /** The dance floor: a widening trapezoid of glowing tiles, dense and small near the stage, big up close. */
    private static double halfWidthAtY(double y0) {
        return 6 + 34 * Math.pow(clamp01(y0 / 28.0), 1.4);
    }

    private int floorTilePixel(int x, double y0, double hw) {
        double u = (x - 40) / hw;
        int col = (int) Math.floor(u * 10.0);
        int row = (int) Math.floor(60.0 / Math.max(1.0, y0));
        int seed = hash(col, row);
        int base = PARTY_PALETTE[seed % PARTY_PALETTE.length];
        double beat = 0.5 + 0.5 * Math.sin(timeClock * BEAT * 2 * Math.PI + seed * 0.35);
        double edgeFade = 1.0 - 0.45 * Math.abs(u);
        int c = shade(base, (0.45 + 0.55 * beat) * edgeFade);
        double gridFrac = Math.abs(u * 10.0 - Math.round(u * 10.0));
        if (gridFrac < 0.06) c = shade(c, 1.35);
        return c;
    }

    // =====================================================================
    //  MOVING LIGHTS: color washes and hard laser lines sweeping the floor
    // =====================================================================

    private void paintWashes() {
        wash(20, 2, 44 + 30 * Math.sin(timeClock * 0.33), 30 + 8 * Math.sin(timeClock * 0.21), rgb(255, 70, 200));
        wash(60, 2, 36 - 28 * Math.sin(timeClock * 0.29 + 1.2), 28 + 7 * Math.sin(timeClock * 0.25 + 2.0), rgb(70, 210, 255));
    }

    private void wash(double ax, double ay, double tx, double ty, int color) {
        for (int y = (int) ay; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                double t = segT(x, y, ax, ay, tx, ty);
                if (t < 0 || t > 1) continue;
                double d = distOnSeg(x, y, ax, ay, tx, ty, t);
                double sigma = 1.0 + 3.0 * t;
                double amt = Math.exp(-d * d / (2 * sigma * sigma)) * 0.16;
                if (amt > 0.01) px[y * PW + x] = mix(px[y * PW + x], color, amt);
            }
        }
    }

    private void paintLasers() {
        laser(FIXTURE_XS[0], 1, rgb(255, 40, 60), 0.75, 1.3, 0.0);
        laser(FIXTURE_XS[1], 1, rgb(60, 255, 140), -0.9, 1.7, 1.0);
        laser(FIXTURE_XS[2], 1, rgb(60, 180, 255), 0.85, 1.1, 2.1);
        laser(FIXTURE_XS[3], 1, rgb(255, 220, 40), -0.75, 1.9, 0.6);
    }

    private void laser(double ax, double ay, int color, double sweepAmp, double sweepSpeed, double phase) {
        double angle = Math.PI / 2 + sweepAmp * Math.sin(timeClock * sweepSpeed + phase);
        double tx = ax + Math.cos(angle) * 70;
        double ty = ay + Math.sin(angle) * 70;
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                double t = segT(x, y, ax, ay, tx, ty);
                if (t < 0 || t > 1) continue;
                double d = distOnSeg(x, y, ax, ay, tx, ty, t);
                double amt = Math.exp(-d * d / 0.9) * 0.55;
                if (amt > 0.02) px[y * PW + x] = mix(px[y * PW + x], color, amt);
            }
        }
    }

    // =====================================================================
    //  THE DJ: booth is baked into the stage; the figure bobs and pumps a fist
    // =====================================================================

    private void paintDJ() {
        int cx = 40;
        double beat = Math.sin(timeClock * BEAT * 2 * Math.PI);
        int hy = 8 + (int) Math.round(beat * 0.7);
        int dark = rgb(10, 8, 14);
        int rim = rainbow(timeClock * 0.2);

        for (int dx = -1; dx <= 1; dx++) plot(cx + dx, hy - 1, dark);  // headphone band
        plot(cx - 2, hy, rgb(40, 38, 52));
        plot(cx + 2, hy, rgb(40, 38, 52));                             // ear cups
        plot(cx - 1, hy, dark);
        plot(cx, hy, shade(SKIN0, 0.6));                               // face
        plot(cx + 1, hy, dark);
        for (int y = hy + 1; y <= hy + 3; y++) {
            for (int dx = -2; dx <= 2; dx++) plot(cx + dx, y, dark);   // torso
            plot(cx + 2, y, rim);                                      // rim light
        }
        double raise = 2.5 + 2.5 * Math.max(0.0, beat);                // right fist pumps on the beat
        int ax = cx + 4, ay = hy + 1 - (int) Math.round(raise);
        line(cx + 2, hy + 1, ax, ay, dark, 1);
        plot(ax, ay, shade(SKIN0, 0.85));
        line(cx - 2, hy + 2, cx - 3, 12, dark, 1);                     // left hand riding the deck
    }

    // =====================================================================
    //  THE CROWD: dancers scattered across the floor, jumping to the beat
    // =====================================================================

    private void paintDancers() {
        for (int i = 0; i < DANCER_COUNT; i++) {
            double u = -0.82 + 1.64 * rand01(i, 601);
            double y0 = 3 + 24 * rand01(i, 602);
            drawDancer(u, y0, i);
        }
    }

    private void drawDancer(double u, double y0, int i) {
        double y = FLOOR_ORIGIN_Y + y0;
        double hw = halfWidthAtY(y0);
        double x = 40 + u * hw;
        double scale = 0.55 + 0.85 * (y0 / 28.0);
        double phase = rand01(i, 603) * 6.2831;
        double jump = Math.abs(Math.sin(timeClock * BEAT * Math.PI + phase));
        int bounce = (int) Math.round(jump * 2.2 * scale);
        int hx = (int) Math.round(x);
        int hy = (int) Math.round(y - 2 * scale) - bounce;
        int body = rgb(14, 11, 18);
        int rim = PARTY_PALETTE[hash(i, 5) % PARTY_PALETTE.length];

        plot(hx, hy, rim);
        int legLen = Math.max(1, (int) Math.round(2 * scale));
        for (int dy = 1; dy <= legLen; dy++) plot(hx, hy + dy, body);
        if (jump > 0.55) {
            plot(hx - 1, hy, body);
            plot(hx + 1, hy, body);
            plot(hx - 1, hy - 1, body);
            plot(hx + 1, hy - 1, body);
        }
    }

    // =====================================================================
    //  ATMOSPHERE: CO2 jets, drifting confetti, and the odd full-room strobe
    // =====================================================================

    private void paintConfettiAndCO2() {
        for (int i = 0; i < 22; i++) {
            double speed = 5 + 5 * rand01(i, 700);
            int y = (int) ((timeClock * speed + i * 6.0) % (PH + 6)) - 3;
            int x = (int) (6 + (PW - 12) * rand01(i, 701) + 3.0 * Math.sin(timeClock * 0.7 + i));
            if (x >= 0 && x < PW && y >= 0 && y < PH) px[y * PW + x] = PARTY_PALETTE[i % PARTY_PALETTE.length];
        }
        for (int cannon = 0; cannon < 2; cannon++) {
            double baseX = cannon == 0 ? STAGE_L + 3 : STAGE_R - 4;
            for (int p = 0; p < 6; p++) {
                double t = (timeClock * 11.0 + p * 3.0 + cannon * 20.0) % 40.0;
                if (t > 16.0) continue;
                double py = STAGE_BOT - t * 1.15;
                double pxCoord = baseX + 2.2 * Math.sin(p * 1.3 + timeClock * 1.5);
                int xi = (int) Math.round(pxCoord), yi = (int) Math.round(py);
                if (xi >= 0 && xi < PW && yi >= 0 && yi < PH) {
                    px[yi * PW + xi] = mix(px[yi * PW + xi], rgb(235, 245, 255), 0.7 * (1.0 - t / 16.0));
                }
            }
        }
    }

    private void paintStrobe() {
        double cyc = (timeClock + 1.0) % 3.2;
        if (cyc >= 0.08) return;
        double a = 0.5 * (1.0 - cyc / 0.08);
        for (int i = 0; i < px.length; i++) px[i] = mix(px[i], rgb(255, 255, 255), a);
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

    private static double segT(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        return len2 < 1e-6 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
    }

    private static double distOnSeg(double px, double py, double ax, double ay, double bx, double by, double t) {
        double x = ax + t * (bx - ax), y = ay + t * (by - ay);
        return Math.hypot(px - x, py - y);
    }

    private static int rainbow(double hue) {
        double h = hue - Math.floor(hue);
        double r = 0.5 + 0.5 * Math.sin(2 * Math.PI * h);
        double g = 0.5 + 0.5 * Math.sin(2 * Math.PI * (h + 1.0 / 3));
        double b = 0.5 + 0.5 * Math.sin(2 * Math.PI * (h + 2.0 / 3));
        return rgb((int) (r * 255), (int) (g * 255), (int) (b * 255));
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