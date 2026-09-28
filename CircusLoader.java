public class CircusLoader extends Loader {
    private static final StatusStage[] CIRCUS_STAGES = {
        new StatusStage(25, "Raising the big top canvas:"),
        new StatusStage(50, "Rigging trapeze lines & spotlights:"),
        new StatusStage(75, "Seating the crowd & polishing the ring:"),
        new StatusStage(100, "Center Ring Performance Live!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;

    // The center ring, viewed at an angle from the stands: a filled ellipse.
    private static final double CX = 40.0, CY = 27.0, RX = 32.0, RY = 15.0;
    private static final double Z_SCENE = 0.50;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int STRIPE_RED   = rgb(178, 34,  48);
    private static final int STRIPE_CREAM = rgb(230, 212, 168);
    private static final int POLE_COLOR   = rgb(150, 120, 80);
    private static final int CURB_RED     = rgb(196, 40,  46);
    private static final int CURB_WHITE   = rgb(226, 220, 206);
    private static final int SAWDUST_A    = rgb(178, 140, 90);
    private static final int SAWDUST_B    = rgb(150, 112, 68);
    private static final int RING_HAZE    = rgb(60,  34,  36);
    private static final int ROPE_COLOR   = rgb(18,  14,  12);
    private static final int BAR_COLOR    = rgb(196, 172, 96);
    private static final int WHITE_GLOVE  = rgb(250, 248, 244);

    private static final int[] SHIRTS = {
        rgb(190, 60,  60), rgb(60,  110, 190), rgb(210, 170, 40), rgb(70,  150, 90),
        rgb(160, 80,  170), rgb(220, 130, 60), rgb(90,  90,  190), rgb(200, 200, 210)
    };
    private static final int[] SKIN = { rgb(196, 148, 112), rgb(140, 96, 66), rgb(230, 190, 150) };

    private final int[] px = new int[PW * PH];

    public CircusLoader() {
        // This uses 80x22 specifically
        super(CIRCUS_STAGES, WIDTH, HEIGHT);
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

        paintSpotlights();
        paintTrapeze();
        paintRingmaster();
        paintConfetti();
        paintFlashes();

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
    //  BASE SCENE: canvas ceiling on top, the ring dead ahead, crowd peeking
    //  in around the edges (thin behind the ring, thicker at the far sides).
    // =====================================================================

    private static double ceilBottom(int x) {
        // A scalloped valance where the tent canvas meets the stands.
        return 9.0 + 2.2 * Math.sin(x * 0.5) + 1.0 * Math.sin(x * 0.13 + 1.0);
    }

    private int scenePixel(int x, int y) {
        double cb = ceilBottom(x);
        if (y < cb) return ceilingPixel(x, y, cb);

        double u = (x - CX) / RX;
        double v = (y - CY) / RY;
        double rr = Math.sqrt(u * u + v * v);
        if (rr <= 1.0) return ringFloorPixel(u, v, rr);

        if (Math.abs(u) <= 1.0) {
            double s = Math.sqrt(1.0 - u * u);
            double yTop = CY - RY * s, yBot = CY + RY * s;
            return y < yTop ? crowdPixel(x, y, cb, yTop) : crowdPixel(x, y, yBot, PH);
        }
        return crowdPixel(x, y, cb, PH);
    }

    /** Radial big-top canvas stripes, side support poles with pennants, and the king pole. */
    private int ceilingPixel(int x, int y, double cb) {
        if (x == 9 || x == 10 || x == 69 || x == 70) {
            return y < 1 ? rgb(220, 190, 90) : shade(POLE_COLOR, 1.0 - 0.3 * (y / Math.max(1.0, cb)));
        }
        double angle = Math.atan2(y + 9.0, x - CX); // apex sits just above the visible area
        int stripe = (int) Math.floor(((angle + Math.PI) / (2 * Math.PI)) * 24);
        int base = (stripe & 1) == 0 ? STRIPE_RED : STRIPE_CREAM;
        int c = shade(base, 0.72 + 0.34 * clamp01(y / Math.max(1.0, cb)));
        if (x == (int) CX && y > 0) c = shade(POLE_COLOR, 0.85); // king pole, piercing the canvas apex
        return c;
    }

    /** Tiered seating: heads and shirts, hazier and dimmer the farther back the row sits, occasional cheers. */
    private int crowdPixel(int x, int y, double bandTop, double bandBottom) {
        double span = Math.max(1.0, bandBottom - bandTop);
        double t = clamp01((y - bandTop) / span);
        int seatH = 3;
        int col = x / 2;
        double rel = y - bandTop;
        int row = (int) (rel / seatH);
        int localY = (int) (rel - row * seatH);

        int seed = hash(col, row * 31 + (int) Math.round(bandTop * 5));
        int shirt = SHIRTS[seed % SHIRTS.length];
        int skin = SKIN[(seed / 7) % SKIN.length];
        int c = localY == 0 ? skin : shirt;

        c = mix(c, RING_HAZE, (1.0 - t) * 0.55);   // far rows fade into haze
        c = shade(c, 0.55 + 0.55 * t);              // and darken

        double phase = rand01(col, row + 500) * 6.2831;
        if (Math.sin(timeClock * 1.7 + phase) > 0.965) c = mix(c, rgb(255, 255, 255), 0.4); // a cheer ripples through
        return c;
    }

    /** Sawdust floor with radial raking, a painted curb with pennant posts, and twinkling glitter. */
    private int ringFloorPixel(double u, double v, double rr) {
        double angle = Math.atan2(v, u);
        if (rr > 0.86) {
            int seg = (int) Math.floor(((angle + Math.PI) / (2 * Math.PI)) * 20);
            int c = (seg & 1) == 0 ? CURB_RED : CURB_WHITE;
            if (rr > 0.95 && seg % 4 == 0) c = mix(c, rgb(255, 215, 60), 0.6); // flag post
            return shade(c, 0.88 + 0.20 * (1.0 - rr));
        }

        double rayF = 0.5 + 0.5 * Math.sin(angle * 32.0);
        int c = mix(SAWDUST_B, SAWDUST_A, rayF);
        c = shade(c, 1.0 - 0.25 * rr);
        if (rr > 0.46 && rr < 0.50) c = mix(c, CURB_WHITE, 0.55); // the performers' pedestal ring
        if (rr < 0.12) c = mix(c, rgb(210, 180, 90), 0.4);        // center medallion

        int px2 = (int) Math.round((u + 1) * 1000), py2 = (int) Math.round((v + 1) * 1000);
        if (hash(px2, py2) % 37 == 0) {
            double tw = 0.5 + 0.5 * Math.sin(timeClock * 3.0 + rand01(px2, py2) * 6.2831);
            c = mix(c, rgb(255, 244, 190), 0.5 * Math.max(0.0, tw - 0.4) / 0.6);
        }
        return c;
    }

    // =====================================================================
    //  MOVING PARTS: crossing spotlights, swinging trapeze duo, the ringmaster
    // =====================================================================

    private void paintSpotlights() {
        double t1x = CX - 10 + 16 * Math.sin(timeClock * 0.31);
        double t1y = CY + 2 + 5 * Math.sin(timeClock * 0.23 + 1.0);
        double t2x = CX + 8 - 14 * Math.sin(timeClock * 0.27 + 2.0);
        double t2y = CY + 1 + 4 * Math.sin(timeClock * 0.19 + 0.5);
        spot(22, 4, t1x, t1y, rgb(255, 244, 200));
        spot(58, 4, t2x, t2y, rgb(210, 230, 255));
    }

    private void spot(double ax, double ay, double tx, double ty, int color) {
        for (int y = (int) ay; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                double t = segT(x, y, ax, ay, tx, ty);
                if (t < 0 || t > 1) continue;
                double d = distOnSeg(x, y, ax, ay, tx, ty, t);
                double sigma = 0.5 + 2.0 * t;
                double amt = Math.exp(-d * d / (2 * sigma * sigma)) * 0.15 * (1.0 - t * 0.3);
                if (amt > 0.01) px[y * PW + x] = mix(px[y * PW + x], color, amt);
            }
        }
        int tX = (int) Math.round(tx), tY = (int) Math.round(ty);
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                int x = tX + dx, y = tY + dy;
                if (x < 0 || x >= PW || y < 0 || y >= PH) continue;
                double d = Math.sqrt(dx * dx * 0.6 + dy * dy);
                double amt = Math.exp(-d * d / 8.0) * 0.35;
                if (amt > 0.02) px[y * PW + x] = mix(px[y * PW + x], color, amt);
            }
        }
    }

    private void paintTrapeze() {
        trapeze(28, 3, 20, 0.85, timeClock * 0.80, rgb(40, 140, 220), rgb(255, 215, 60));
        trapeze(52, 3, 18, 0.85, timeClock * 0.95 + Math.PI, rgb(220, 50, 140), rgb(255, 215, 60));
    }

    /** A rope, a bar, and a rider: legs kicked into a "flag" pose at the peak of each swing. */
    private void trapeze(double ax, double ay, double len, double amp, double phase, int suit, int accent) {
        double ang = amp * Math.sin(phase);
        double bx = ax + len * Math.sin(ang);
        double by = ay + len * Math.cos(ang);
        line(ax, ay, bx, by, ROPE_COLOR, 1);

        int hx = (int) Math.round(bx), hy = (int) Math.round(by);
        for (int i = -2; i <= 2; i++) plot((int) Math.round(bx + i * Math.cos(ang)), (int) Math.round(by - i * Math.sin(ang)), BAR_COLOR);

        boolean flag = Math.abs(ang) > amp * 0.75; // near the top of the swing
        plot(hx, hy + 1, accent);   // grip
        plot(hx, hy + 2, SKIN[0]);  // head
        plot(hx, hy + 3, suit);
        plot(hx, hy + 4, suit);
        if (flag) {
            plot(hx - 2, hy + 4, suit);
            plot(hx + 2, hy + 4, suit);
        } else {
            plot(hx, hy + 5, suit);
            plot(hx, hy + 6, suit);
        }
    }

    /** Center-ring ringmaster: top hat, red coat, one glove raised with a whip, a little idle bounce. */
    private void paintRingmaster() {
        int hx = (int) Math.round(CX);
        int hy = (int) Math.round(CY + RY * 0.55 + 0.4 * Math.sin(timeClock * 2.0));

        plot(hx - 1, hy + 3, rgb(20, 20, 20));
        plot(hx + 1, hy + 3, rgb(20, 20, 20)); // boots
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -dy - 1; dx <= dy + 1; dx++) plot(hx + dx, hy + 1 + dy, rgb(150, 20, 30));
        }
        plot(hx - 1, hy, rgb(160, 30, 40));
        plot(hx, hy, rgb(230, 190, 90));
        plot(hx + 1, hy, rgb(160, 30, 40));
        plot(hx - 2, hy, WHITE_GLOVE); // resting hand

        double armAngle = 0.5 + 0.3 * Math.sin(timeClock * 1.3);
        int wx = hx + 2 + (int) Math.round(2 * Math.cos(armAngle));
        int wy = hy - 1 - (int) Math.round(2 * Math.sin(armAngle));
        line(hx + 1, hy, wx, wy, WHITE_GLOVE, 1);
        plot(wx, wy, WHITE_GLOVE);
        line(wx, wy, wx + (int) Math.round(3 * Math.cos(armAngle - 0.4)), wy - (int) Math.round(3 * Math.sin(armAngle - 0.4)), rgb(40, 30, 20), 1);

        plot(hx, hy - 1, SKIN[0]);
        plot(hx, hy - 2, rgb(15, 15, 15));
        plot(hx, hy - 3, rgb(15, 15, 15)); // top hat
        plot(hx - 1, hy - 2, rgb(200, 30, 40));
        plot(hx + 1, hy - 2, rgb(200, 30, 40)); // hat band
    }

    private void paintConfetti() {
        for (int i = 0; i < 18; i++) {
            double speed = 6 + 4 * rand01(i, 700);
            double fy = ((timeClock * speed + i * 9.0) % (PH + 10)) - 5;
            double fx = 10 + (PW - 20) * rand01(i, 701) + 3.0 * Math.sin(timeClock * 0.6 + i);
            plot((int) fx, (int) fy, SHIRTS[i % SHIRTS.length]);
        }
    }

    private void paintFlashes() {
        for (int i = 0; i < 4; i++) {
            double cyc = timeClock / 1.3;
            int slot = (int) Math.floor(cyc) + i * 17;
            double localT = cyc - Math.floor(cyc);
            int fx = 6 + hash(slot, 1) % 68;
            int fy = 10 + hash(slot, 2) % 30;
            double bright = Math.max(0.0, 1.0 - Math.abs(localT - 0.1) * 8.0);
            if (bright > 0.05) plot(fx, fy, mix(px[fy * PW + fx], rgb(255, 255, 255), Math.min(1.0, bright)));
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

    private static double segT(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        return len2 < 1e-6 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
    }

    private static double distOnSeg(double px, double py, double ax, double ay, double bx, double by, double t) {
        double x = ax + t * (bx - ax), y = ay + t * (by - ay);
        return Math.hypot(px - x, py - y);
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