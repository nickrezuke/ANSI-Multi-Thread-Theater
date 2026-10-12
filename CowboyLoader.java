// TODO: Fix the horses, they look a little unrealistic

public class CowboyLoader extends Loader {
    private static final StatusStage[] COWBOY_STAGES = {
        new StatusStage(25, "Saddling up the horses:"),
        new StatusStage(50, "Kicking up trail dust:"),
        new StatusStage(75, "Riding into the sunset:"),
        new StatusStage(100, "Yeehaw!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph. The riders stay put
    // at fixed screen columns; everything behind them scrolls right-to-left to sell the gallop.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;
    private static final int GROUND_Y = 32;

    private static final double SUN_X = 73, SUN_Y = 13;
    private static final double MESA_SPEED = 6.0, CACTUS_SPEED = 14.0, GROUND_SPEED = 26.0;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int SKIN = rgb(196, 148, 112);
    private static final int HAT  = rgb(110, 80, 45);
    private static final int ROPE = rgb(190, 150, 80);

    private static final int[] HORSE_XS     = { 16, 40, 64 };
    private static final int[] HORSE_COLORS = { rgb(120, 72, 40), rgb(58, 48, 45), rgb(192, 162, 102) };
    private static final int[] SHIRT_COLORS = { rgb(150, 60, 55), rgb(70, 100, 140), rgb(150, 125, 50) };
    private static final int[] ACTIONS      = { 0, 1, 2 }; // 0=lasso, 1=pistol, 2=hat-wave

    private final int[] px = new int[PW * PH];

    public CowboyLoader() {
        // This uses 80x22 specifically
        super(COWBOY_STAGES, WIDTH, HEIGHT);
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

        for (int i = 0; i < HORSE_XS.length; i++) {
            paintHorseAndRider(HORSE_XS[i], i);
        }

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
    //  THE SCROLLING BACKDROP: sunset sky, distant mesas, cacti, the trail
    // =====================================================================

    private int scenePixel(int x, int y) {
        if (y >= GROUND_Y) return groundPixel(x, y);

        double t = clamp01(y / (double) GROUND_Y);
        int sky = skyColor(x, y, t);

        double mh = mesaHeight(x);
        if (y >= mh) {
            double depth = clamp01((y - mh) / Math.max(1.0, GROUND_Y - mh));
            int mesaCol = mix(rgb(150, 80, 55), rgb(90, 45, 35), depth);
            return mix(mesaCol, sky, 0.12);
        }

        int scenery = cactusOrRockPixel(x, y);
        return scenery != -1 ? scenery : sky;
    }

    private int skyColor(int x, int y, double t) {
        int top = rgb(60, 50, 90), bottom = rgb(255, 150, 90);
        int c = mix(top, bottom, Math.pow(t, 0.6));
        double d = Math.hypot(x - SUN_X, (y - SUN_Y) * 1.1);
        double glow = Math.exp(-d * d / 110.0);
        c = mix(c, rgb(255, 230, 150), clamp01(glow * 0.8));
        if (d <= 5.0) c = rgb(255, 238, 180);
        return c;
    }

    private double mesaHeight(int x) {
        double sx = x + timeClock * MESA_SPEED;
        int seg = (int) Math.floor(sx / 14.0);
        double local = sx / 14.0 - seg;
        double h1 = GROUND_Y - 20 + (hash(seg, 5) % 6) * 1.6;
        double h2 = GROUND_Y - 20 + (hash(seg + 1, 5) % 6) * 1.6;
        double edge = 0.1;
        if (local < edge) { double u = local / edge; return h2 + (h1 - h2) * u; }
        if (local > 1 - edge) { double u = (local - (1 - edge)) / edge; return h1 + (h2 - h1) * u; }
        return h1;
    }

    private int cactusOrRockPixel(int x, int y) {
        double sx = x + timeClock * CACTUS_SPEED;
        int slot = (int) Math.floor(sx / 10.0);
        double local = sx - slot * 10.0;
        int kind = hash(slot, 21) % 4;
        if (kind == 3) return -1;

        if (kind == 2) {
            double dx = local - 5.0, dy = (y - (GROUND_Y - 2)) * 1.6;
            if (dx * dx + dy * dy <= 5.5) return shade(rgb(80, 62, 50), 0.85 + 0.15 * Math.sin(dx));
            return -1;
        }

        int cactusGreen = rgb(55, 100, 58), cactusLit = rgb(72, 128, 72);
        double topY = GROUND_Y - 9 - (hash(slot, 22) % 5);
        boolean trunk = local >= 4.0 && local < 6.3 && y >= topY;
        boolean arm = (hash(slot, 23) % 2 == 0) && local >= 6.3 && local < 8.3 && y >= topY + 3 && y < topY + 7;
        if (trunk || arm) return ((int) local % 2 == 0) ? cactusLit : cactusGreen;
        return -1;
    }

    private int groundPixel(int x, int y) {
        double sx = x + timeClock * GROUND_SPEED;
        int c = mix(rgb(124, 90, 56), rgb(82, 58, 38), clamp01((y - GROUND_Y) / (double) (PH - GROUND_Y)));
        double n = noise2(sx * 0.25, y * 0.3, 40);
        if (n > 0.72) c = shade(c, 0.8);
        else if (n < 0.14) c = shade(c, 1.15);
        if (hash((int) Math.floor(sx), y) % 23 == 0) c = shade(c, 1.3);
        return c;
    }

    // =====================================================================
    //  THE RIDERS: stationary on screen, galloping in place, action per rider
    // =====================================================================

    private void paintHorseAndRider(int cx, int idx) {
        double gaitPhase = timeClock * 7.0 + idx * 2.1;
        double bob = Math.sin(gaitPhase * 2.0) * 0.8;
        double bodyCY = (GROUND_Y - 10) + bob;
        int horseColor = HORSE_COLORS[idx % HORSE_COLORS.length];
        int horseDark = shade(horseColor, 0.72);

        paintHoofDust(cx, idx);
        paintLegs(cx, bodyCY, gaitPhase, horseDark);
        paintHorseShape(cx, bodyCY, horseColor, horseDark);
        paintManeTail(cx, bodyCY, gaitPhase, horseColor);
        paintRider(cx, bodyCY, idx);
    }

    private void paintHoofDust(int cx, int seed) {
        for (int i = 0; i < 6; i++) {
            double period = 1.4;
            double phase = (timeClock * 2.0 + i * period / 6.0 + seed * 0.37) % period;
            double t = phase / period;
            double x = cx - 3 - t * 8.0 + Math.sin(t * 5.0 + i) * 0.8;
            double y = GROUND_Y + 1 - t * 2.5 + rand01(i, seed) * 1.5;
            double size = 1.0 + t * 2.2;
            double alpha = clamp01((1.0 - t) * 0.5);
            int ix = (int) x, iy = (int) y;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.hypot(dx, dy) <= size * 0.5) blend(ix + dx, iy + dy, rgb(205, 180, 145), alpha * 0.55);
                }
            }
        }
    }

    // The horse's spine, rump to poll, as (dx, dy, bodyRadius) control points relative to
    // (cx, bodyCY) — subdivided into a dense polyline so the whole body, neck, and head taper
    // smoothly into one silhouette instead of a stack of separate blobs.
    private static final double[][] HORSE_FRAME = {
        { -8.0,  1.0, 2.6 },  // rump
        { -5.0, -1.0, 3.3 },  // hindquarter
        { -1.0, -2.0, 3.5 },  // back (the saddle sits here)
        {  3.0, -1.2, 3.1 },  // withers / shoulder
        {  6.0,  0.3, 2.2 },  // chest
        {  8.0, -1.8, 1.5 },  // neck base
        {  9.8, -4.2, 1.2 },  // neck, rising
        { 11.2, -6.5, 1.0 },  // neck, near the poll
        { 12.0, -7.8, 1.1 }   // head
    };
    private static final int HORSE_SUBDIV = 8;
    private static final double SHOULDER_DX = 5.0, SHOULDER_DY = 2.1;
    private static final double HIP_DX = -6.0, HIP_DY = 1.7;

    // Two-bone leg lengths: a thicker thigh/shoulder bone down to a knee or hock, then a thinner
    // cannon bone to the hoof. Front legs knee forward (the carpus); hind legs hock backward —
    // picking the anatomically correct one of the two IK solutions every frame, not a fixed angle,
    // is what turns a straight stick into a leg that folds up under the horse as it swings forward.
    private static final double FRONT_L1 = 4.0, FRONT_L2 = 5.0;
    private static final double HIND_L1 = 4.2, HIND_L2 = 4.9;
    private static final int HOOF = rgb(28, 22, 18);

    private void paintLegs(int cx, double bodyCY, double gaitPhase, int legColor) {
        double hipY = bodyCY + HIP_DY, shoulderY = bodyCY + SHOULDER_DY;
        drawLeg(cx + SHOULDER_DX, shoulderY, gaitPhase, FRONT_L1, FRONT_L2, true, legColor);
        drawLeg(cx + HIP_DX, hipY, gaitPhase + Math.PI, HIND_L1, HIND_L2, false, legColor);
        drawLeg(cx + SHOULDER_DX - 2.0, shoulderY, gaitPhase - 0.9, FRONT_L1, FRONT_L2, true, shade(legColor, 0.88));
        drawLeg(cx + HIP_DX + 2.0, hipY, gaitPhase + Math.PI - 0.9, HIND_L1, HIND_L2, false, shade(legColor, 0.88));
    }

    /** Swing the hoof fore/aft and lift it during the forward reach, same as a real gait cycle. */
    private void drawLeg(double pivotX, double pivotY, double phase, double L1, double L2, boolean kneeForward, int color) {
        double swing = Math.sin(phase) * 2.3;
        double lift = Math.max(0.0, Math.sin(phase)) * 1.8;
        double targetX = pivotX + swing, targetY = GROUND_Y - lift;
        drawTwoBoneLeg(pivotX, pivotY, targetX, targetY, L1, L2, kneeForward, color);
        if (lift < 0.25) plot((int) Math.round(targetX), (int) Math.round(targetY), HOOF);
    }

    /** Standard 2-bone IK: solves for the joint position given the two segment lengths and picks
     *  whichever of the two mirror-image solutions bends toward the correct side of the horse. */
    private void drawTwoBoneLeg(double pivotX, double pivotY, double targetX, double targetY,
                                 double L1, double L2, boolean kneeForward, int color) {
        double dx = targetX - pivotX, dy = targetY - pivotY;
        double dRaw = Math.hypot(dx, dy);
        double minD = Math.abs(L1 - L2) + 0.05, maxD = L1 + L2 - 0.05;
        double d = Math.max(minD, Math.min(maxD, dRaw));
        double baseAng = Math.atan2(dy, dx);
        double cosA = (L1 * L1 + d * d - L2 * L2) / (2 * L1 * d);
        cosA = Math.max(-1.0, Math.min(1.0, cosA));
        double a = Math.acos(cosA);

        double k1x = pivotX + Math.cos(baseAng + a) * L1, k1y = pivotY + Math.sin(baseAng + a) * L1;
        double k2x = pivotX + Math.cos(baseAng - a) * L1, k2y = pivotY + Math.sin(baseAng - a) * L1;
        boolean use1 = kneeForward ? (k1x >= k2x) : (k1x <= k2x);
        double kneeX = use1 ? k1x : k2x, kneeY = use1 ? k1y : k2y;

        plotLine(pivotX, pivotY, kneeX, kneeY, color);
        plotLine(pivotX + 0.6, pivotY, kneeX + 0.6, kneeY, color); // a touch of thickness on the upper leg
        plotLine(kneeX, kneeY, targetX, targetY, shade(color, 0.9)); // thinner cannon bone below the joint
        plot((int) Math.round(kneeX), (int) Math.round(kneeY), shade(color, 1.25)); // joint
    }

    private void paintHorseShape(int cx, double bodyCY, int bodyColor, int bellyShade) {
        int segs = HORSE_FRAME.length - 1;
        int n = segs * HORSE_SUBDIV + 1;
        double[] sx = new double[n], sy = new double[n], sr = new double[n];
        int idx = 0;
        for (int i = 0; i < segs; i++) {
            double[] a = HORSE_FRAME[i], b = HORSE_FRAME[i + 1];
            for (int s = 0; s < HORSE_SUBDIV; s++) {
                double t = s / (double) HORSE_SUBDIV, st = t * t * (3 - 2 * t);
                sx[idx] = a[0] + (b[0] - a[0]) * st;
                sy[idx] = a[1] + (b[1] - a[1]) * st;
                sr[idx] = a[2] + (b[2] - a[2]) * st;
                idx++;
            }
        }
        double[] last = HORSE_FRAME[segs];
        sx[idx] = last[0]; sy[idx] = last[1]; sr[idx] = last[2];

        for (int y = -11; y <= 6; y++) {
            for (int x = -11; x <= 14; x++) {
                double best = Double.MAX_VALUE;
                int bi = 0;
                for (int i = 0; i < n; i++) {
                    double dx = x - sx[i], dy = y - sy[i];
                    double d2 = dx * dx + dy * dy;
                    if (d2 < best) { best = d2; bi = i; }
                }
                double dist = Math.sqrt(best), r = sr[bi];
                if (dist <= r) {
                    boolean belly = (y - sy[bi]) > r * 0.25;
                    plot(cx + x, (int) Math.round(bodyCY + y), belly ? bellyShade : bodyColor);
                }
            }
        }

        double[] head = HORSE_FRAME[segs];
        plotLine(cx + head[0], bodyCY + head[1] - 0.3, cx + head[0] + 2.4, bodyCY + head[1] + 0.9, shade(bodyColor, 0.8));
        plot((int) Math.round(cx + head[0] - 1.2), (int) Math.round(bodyCY + head[1] - 1.7), bodyColor); // ear
    }

    private void paintManeTail(int cx, double bodyCY, double phase, int color) {
        int dark = shade(color, 0.65);
        for (int i = 0; i < 6; i++) {
            double t = i / 5.0;
            double bx = -1.5 + t * 12.5, by = -2.3 - t * 4.8;
            double sway = Math.sin(phase * 0.5 + i) * 0.9;
            plot((int) Math.round(cx + bx), (int) Math.round(bodyCY + by + sway), dark);
        }
        for (int i = 0; i < 6; i++) {
            double sway = Math.sin(phase * 0.6 + i * 0.7) * 1.6;
            plot(cx - 9 - i, (int) Math.round(bodyCY + 0.6 + i * 0.3 + sway), dark);
        }
    }

    private void paintRider(int cx, double bodyCY, int idx) {
        double seatX = cx + 1, seatY = bodyCY - 5.3;
        int shirt = SHIRT_COLORS[idx % SHIRT_COLORS.length];
        int headY = (int) Math.round(seatY - 5);

        plot((int) seatX, (int) Math.round(seatY), shirt);
        plot((int) seatX, (int) Math.round(seatY - 1), shirt);
        plot((int) seatX, (int) Math.round(seatY - 2), shirt);
        plot((int) seatX + 1, (int) Math.round(seatY - 2), shirt);
        plot((int) seatX - 1, (int) Math.round(seatY + 1), shirt);
        plot((int) seatX - 1, (int) Math.round(seatY + 2), rgb(90, 60, 40));

        plot((int) seatX, headY, SKIN);
        plot((int) seatX - 1, headY - 1, HAT);
        plot((int) seatX, headY - 1, HAT);
        plot((int) seatX + 1, headY - 1, HAT);
        plot((int) seatX, headY - 2, HAT);

        plot((int) seatX + 2, (int) Math.round(seatY - 1), SKIN); // rein hand, forward and down

        switch (ACTIONS[idx % ACTIONS.length]) {
            case 0: paintLasso(seatX, seatY, idx); break;
            case 1: paintPistol(seatX, seatY, idx); break;
            default: paintHatWave(seatX, seatY, idx); break;
        }
    }

    private void paintLasso(double seatX, double seatY, int idx) {
        double handX = seatX - 1, handY = seatY - 6;
        plotLine(seatX, seatY - 3, handX, handY, SKIN);
        plot((int) handX, (int) handY, SKIN);

        double ang = timeClock * 7.0 + idx;
        double loopCX = handX, loopCY = handY - 3, radius = 3.2;
        for (double a = 0; a < Math.PI * 2; a += 0.35) {
            int lx = (int) Math.round(loopCX + Math.cos(a + ang) * radius);
            int ly = (int) Math.round(loopCY + Math.sin(a + ang) * radius * 0.55);
            plot(lx, ly, ROPE);
        }
        double tx = loopCX + Math.cos(ang) * radius, ty = loopCY + Math.sin(ang) * radius * 0.55;
        plotLine(handX, handY, tx, ty, shade(ROPE, 0.85));
    }

    private void paintPistol(double seatX, double seatY, int idx) {
        double handX = seatX - 1, handY = seatY - 7;
        plotLine(seatX, seatY - 3, handX, handY, SKIN);
        plot((int) handX, (int) handY, rgb(60, 56, 52));

        double cyc = (timeClock * 1.3 + idx * 1.7) % 3.0;
        if (cyc < 0.35) {
            double k = 1.0 - cyc / 0.35;
            plot((int) handX, (int) handY - 1, mix(rgb(255, 160, 40), rgb(255, 240, 180), k));
            blend((int) handX - 1, (int) handY - 1, rgb(255, 220, 120), k * 0.6);
            blend((int) handX + 1, (int) handY - 1, rgb(255, 220, 120), k * 0.6);
            blend((int) handX, (int) handY - 2, rgb(255, 230, 150), k * 0.5);
        } else if (cyc < 0.9) {
            double t = (cyc - 0.35) / 0.55;
            blend((int) handX, (int) handY - 2 - (int) (t * 2), rgb(210, 210, 210), clamp01((1 - t) * 0.4));
        }
    }

    private void paintHatWave(double seatX, double seatY, int idx) {
        double raise = 2.5 + Math.sin(timeClock * 2.0 + idx) * 1.5;
        plotLine(seatX, seatY - 3, seatX - 2, seatY - 3 - raise, SKIN);
        plot((int) (seatX - 2), (int) Math.round(seatY - 3 - raise), rgb(225, 215, 165));
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

    private void plotLine(double x0, double y0, double x1, double y1, int c) {
        int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))) + 1;
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            plot((int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t), c);
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