public class TavernLoader extends Loader {
    private static final StatusStage[] TAVERN_STAGES = {
        new StatusStage(25, "Tapping the kegs & lighting the lanterns:"),
        new StatusStage(50, "Stocking the back bar:"),
        new StatusStage(75, "Seating the regulars:"),
        new StatusStage(100, "Tavern Open for the Night!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;

    // Vertical bands, front to back: ceiling -> back-wall shelves -> the bartender's
    // strip behind the counter -> the counter top -> the counter's front face.
    private static final int CEIL_H         = 3;
    private static final int WALL_BOTTOM    = 17;
    private static final int BACKBAR_BOTTOM = 30;
    private static final int TOP_BOTTOM     = 34;
    private static final int BAR_L = 16, BAR_R = 64; // the bar itself sits between the two patron wings

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int WOOD_DARK  = rgb(58,  38,  26);
    private static final int WOOD_MID   = rgb(94,  62,  40);
    private static final int WOOD_LIGHT = rgb(132, 92,  58);
    private static final int WALL_BG    = rgb(70,  34,  30);
    private static final int BRASS      = rgb(180, 140, 70);
    private static final int GLOW_WARM  = rgb(255, 200, 120);
    private static final int SUDS_WHITE = rgb(240, 244, 248);
    private static final int WATER_BLUE = rgb(150, 200, 220);
    private static final int STONE      = rgb(96,  90,  86);
    private static final int SOOT       = rgb(34,  28,  26);
    private static final int MUG_GOLD   = rgb(210, 170, 60);

    private static final int[] BOTTLE_COLORS = {
        rgb(90, 150, 90), rgb(160, 110, 40), rgb(60, 90, 150), rgb(180, 60, 50), rgb(210, 190, 140)
    };
    private static final int[] SHIRTS = {
        rgb(150, 60, 60), rgb(60, 100, 150), rgb(150, 130, 40), rgb(70, 120, 90), rgb(120, 80, 140)
    };
    private static final int[] SKIN = { rgb(196, 148, 112), rgb(140, 96, 66), rgb(230, 190, 150) };
    private static final int[] SHELF_RY = { 3, 7, 11 };

    private static final int[] PATRON_X = { 6, 12, 68, 74 };
    private static final boolean[] PATRON_TOAST = { true, false, false, true };

    private final int[] px = new int[PW * PH];

    public TavernLoader() {
        // This uses 80x22 specifically
        super(TAVERN_STAGES, WIDTH, HEIGHT);
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

        paintBartender();
        paintPatrons();
        paintCardNook();
        paintDustAndGlow();

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
    //  ARCHITECTURE: ceiling, back-bar shelves, side walls, counter, corners
    // =====================================================================

    private int scenePixel(int x, int y) {
        if (y < CEIL_H) return ceilingPixel(x, y);
        boolean inBar = x >= BAR_L && x < BAR_R;
        if (y < WALL_BOTTOM) return inBar ? shelvesPixel(x, y) : sideWallPixel(x, y);
        if (y < BACKBAR_BOTTOM) return inBar ? backBarPixel(x, y) : stoolBackPixel(x, y);
        if (y < TOP_BOTTOM) return countertopPixel(x, y);
        if (x < 10) return fireplacePixel(x, y);
        if (x > 70) return cardNookBasePixel(x, y);
        return frontPanelPixel(x, y);
    }

    private int ceilingPixel(int x, int y) {
        if (y == 0) return shade(WOOD_DARK, (x % 13) < 2 ? 0.7 : 1.0); // beam joists
        double d = Math.abs((x % 13) - 6);
        double phase = rand01(x / 13, 900) * 6.2831;
        double glow = Math.exp(-d * d / 6.0) * (0.6 + 0.4 * Math.sin(timeClock * 3.0 + phase));
        return mix(shade(WOOD_MID, 0.8), GLOW_WARM, clamp01(glow * 0.8));
    }

    /** Back-bar shelving: three tiers, each holding a row of bottles with the odd gap. */
    private int shelvesPixel(int x, int y) {
        int ry = y - CEIL_H;
        int col = x / 2;
        for (int s = 0; s < SHELF_RY.length; s++) {
            int shelfRy = SHELF_RY[s];
            if (ry == shelfRy) return WOOD_MID;
            if (ry == shelfRy + 1) return mix(WALL_BG, GLOW_WARM, 0.4); // under-shelf light
            if (ry < shelfRy) {
                int seed = hash(col, s * 7 + 3);
                int h = 2 + seed % 4;
                if (seed % 5 != 0 && ry >= shelfRy - h) {
                    double k = 0.72 + 0.28 * ((shelfRy - ry) / (double) h);
                    return shade(BOTTLE_COLORS[seed % BOTTLE_COLORS.length], k);
                }
                return WALL_BG;
            }
        }
        return WALL_BG;
    }

    /** Plain wall out past the ends of the bar, with a warm sconce glow at patron height. */
    private int sideWallPixel(int x, int y) {
        int ry = y - CEIL_H;
        int c = (hash(x / 4, 55) % 6 == 0) ? shade(WALL_BG, 0.8) : WALL_BG;
        double d = Math.hypot(x - (x < BAR_L ? 6 : 74), ry - 6);
        double glow = Math.exp(-d * d / 10.0) * (0.6 + 0.4 * Math.sin(timeClock * 2.2 + x * 0.1));
        return mix(c, GLOW_WARM, clamp01(glow * 0.5));
    }

    /** Behind the counter: dark cabinetry, colored tap handles, and the recessed sink basin. */
    private int backBarPixel(int x, int y) {
        int ry = y - WALL_BOTTOM;
        int[] tapXs = { 29, 31, 49, 51 };
        for (int i = 0; i < tapXs.length; i++) {
            if (x == tapXs[i] && ry >= 3 && ry <= 10) return ry == 3 ? shade(BRASS, 1.15) : SHIRTS[i % SHIRTS.length];
        }
        if (x >= 36 && x <= 44 && ry >= 9 && ry <= 12) {
            boolean rim = ry == 9 || x == 36 || x == 44;
            return rim ? shade(BRASS, 0.9) : mix(WATER_BLUE, SUDS_WHITE, 0.25);
        } // the sink
        return (x % 5 == 0) ? shade(WOOD_DARK, 0.85) : WOOD_DARK;
    }

    private int stoolBackPixel(int x, int y) {
        return (x % 6 == 0) ? shade(WALL_BG, 0.95) : shade(WALL_BG, 0.85);
    }

    private int countertopPixel(int x, int y) {
        int ry = y - BACKBAR_BOTTOM;
        int c = ry == 0 ? shade(WOOD_LIGHT, 1.05) : mix(WOOD_MID, WOOD_DARK, ry / 3.0);
        return (hash(x, ry + 800) % 9 == 0) ? shade(c, 1.15) : c;
    }

    private int frontPanelPixel(int x, int y) {
        if (y == PH - 1) return BRASS; // foot rail
        int c = ((x / 3) % 2 == 0) ? WOOD_MID : WOOD_DARK;
        return shade(c, 1.0 - 0.03 * (y - TOP_BOTTOM));
    }

    private int fireplacePixel(int x, int y) {
        int ry = y - TOP_BOTTOM; // 0..9
        if (ry < 2) return STONE; // mantle lintel
        double fx = x - 5;
        double h = 9 - ry; // height above the firebox floor: 0 at the base, taller near the opening
        double n = noise2(fx * 0.6 + timeClock * 3.2, h * 0.5 - timeClock * 4.5, 5);
        double width = 3.0 - h * 0.34;
        double mask = Math.max(0.0, 1.0 - h / 7.0) * n;
        if (width > 0.2 && Math.abs(fx) < width && mask > 0.26) {
            return mix(rgb(255, 210, 70), rgb(255, 70, 20), clamp01(h / 6.0));
        }
        if (ry == 9) return shade(rgb(255, 120, 40), 0.55); // glowing embers at the base
        return shade(SOOT, 1.0 + 0.04 * ry);
    }

    private int cardNookBasePixel(int x, int y) {
        int ry = y - TOP_BOTTOM;
        return ry < 3 ? shade(WOOD_MID, 0.9) : shade(rgb(40, 30, 26), 0.9);
    }

    // =====================================================================
    //  THE BARTENDER: the requested centerpiece, handwashing a glass at the sink
    // =====================================================================

    private void paintBartender() {
        int cx = 40;
        plot(cx - 1, 19, SKIN[0]);
        plot(cx, 19, SKIN[0]);
        plot(cx + 1, 19, SKIN[0]);
        plot(cx, 18, rgb(60, 40, 30)); // hair
        for (int y = 21; y <= 24; y++) {
            for (int dx = -2; dx <= 2; dx++) plot(cx + dx, y, rgb(230, 225, 210)); // apron
        }
        plot(cx - 3, 21, SHIRTS[1]);
        plot(cx + 3, 21, SHIRTS[1]); // rolled sleeves at the shoulders

        double scrub = Math.sin(timeClock * 3.2);
        int gx = cx + (int) Math.round(scrub * 2);
        int gy = 27;
        line(cx - 2, 24, gx - 2, gy, SKIN[1], 1); // forearms down to the sink
        line(cx + 2, 24, gx + 2, gy, SKIN[1], 1);
        plot(gx - 2, gy, SKIN[1]);
        plot(gx + 2, gy, SKIN[1]);
        plot(gx, gy - 1, mix(rgb(255, 255, 255), WATER_BLUE, 0.30)); // the glass, mid-scrub
        plot(gx, gy, mix(rgb(255, 255, 255), WATER_BLUE, 0.50));

        for (int i = 0; i < 7; i++) { // soap suds drifting around the hands
            double a = timeClock * 2.0 + i * 1.3;
            if (Math.sin(a * 3 + i) > -0.1) {
                plot(gx + (int) Math.round(3.2 * Math.cos(a)), gy - 1 + (int) Math.round(1.4 * Math.sin(a)), SUDS_WHITE);
            }
        }
        double dropPhase = (timeClock * 1.1) % 3.0; // an occasional flicked droplet
        if (dropPhase < 1.0) plot(gx + 3, gy - 2 - (int) Math.round(dropPhase * 3), WATER_BLUE);
    }

    // =====================================================================
    //  THE REGULARS: a couple of toasts, a couple of quiet pints
    // =====================================================================

    private void paintPatrons() {
        for (int i = 0; i < PATRON_X.length; i++) drawPatron(PATRON_X[i], PATRON_TOAST[i], i);
    }

    private void drawPatron(int cx, boolean toast, int i) {
        int skin = SKIN[i % SKIN.length];
        int shirt = SHIRTS[(i + 2) % SHIRTS.length];
        int hy = 20 + (int) Math.round(0.6 * Math.sin(timeClock * 1.4 + i * 1.7));
        plot(cx, hy, skin);
        plot(cx, hy - 1, rgb(50, 35, 25));
        for (int y = hy + 1; y <= hy + 4; y++) {
            for (int dx = -1; dx <= 1; dx++) plot(cx + dx, y, shirt);
        }

        if (toast) {
            double raise = 4 + 1.5 * Math.sin(timeClock * 2.0 + i);
            int my = hy - (int) Math.round(raise);
            int mx = cx + 2;
            line(cx + 1, hy + 2, mx, my, skin, 1);
            plot(mx, my, MUG_GOLD);
            plot(mx, my - 1, SUDS_WHITE);
        } else {
            line(cx, hy + 3, cx, 30, skin, 1);
            plot(cx, 30, MUG_GOLD);
            plot(cx, 29, SUDS_WHITE);
        }
    }

    // =====================================================================
    //  THE CORNER TABLE: dice rattling past the end of the bar
    // =====================================================================

    private void paintCardNook() {
        seatedFigure(74, 0);
        seatedFigure(78, 1);
        double rollT = (timeClock / 2.0) - Math.floor(timeClock / 2.0);
        plot(76, 36, rollT < 0.15 ? rgb(255, 255, 255) : rgb(235, 230, 220));
    }

    private void seatedFigure(int cx, int i) {
        int hy = 34 + (int) Math.round(0.5 * Math.sin(timeClock * 1.2 + i * 2.1));
        plot(cx, hy, SKIN[i % SKIN.length]);
        plot(cx, hy + 1, SHIRTS[(i + 4) % SHIRTS.length]);
        plot(cx, hy + 2, SHIRTS[(i + 4) % SHIRTS.length]);
    }

    /** A few warm dust motes drifting through the lamplight, for atmosphere. */
    private void paintDustAndGlow() {
        for (int i = 0; i < 14; i++) {
            double speed = 0.4 + 0.5 * rand01(i, 950);
            int y = (int) ((timeClock * speed * 6 + i * 7.0) % PH);
            int x = (int) (14 + (PW - 28) * rand01(i, 951) + 2.0 * Math.sin(timeClock * 0.5 + i));
            if (x >= 0 && x < PW && y >= 0 && y < PH) {
                px[y * PW + x] = mix(px[y * PW + x], rgb(255, 235, 200), 0.15 + 0.15 * rand01(i, 952));
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