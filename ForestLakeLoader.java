import java.util.Arrays;

public class ForestLakeLoader extends Loader {
    private static final StatusStage[] LAKE_STAGES = {
        new StatusStage(25, "Planting whispering pine forests:"),
        new StatusStage(50, "Filling the mountain lake with still water:"),
        new StatusStage(75, "Tracing shimmering reflections & sun glitter:"),
        new StatusStage(100, "Forest Lake Environment Active!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final int HZ     = 22;       // first water row: the far shoreline, and the mirror line
    private static final double SUN_X = 56.0;
    private static final double SUN_Y = 16.0;
    private static final double Z_SCENE = 0.50;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    // Sunset sky, top to horizon
    private static final int[] SKY_STOPS = {
        rgb(18, 24, 72), rgb(62, 46, 122), rgb(190, 88, 120), rgb(255, 146, 94), rgb(255, 196, 122)
    };
    private static final int WATER_FAR  = rgb(58, 74, 108);   // lake tint by the far shore
    private static final int WATER_NEAR = rgb(10, 30, 54);    // ...and down near the viewer
    private static final int WARM_SPARK = rgb(255, 205, 150);
    private static final int GLINT      = rgb(255, 244, 196);
    private static final int TRUNK      = rgb(30, 20, 18);
    private static final int LAND       = rgb(10, 24, 24);
    private static final int GRASS      = rgb(30, 64, 42);

    private static final double[] RING_PERIOD = { 6.5, 8.3, 10.1 };
    private static final double RING_LIFE = 4.0;

    // Foreground pines on two peninsulas: { x, height, half-width }
    private static final double[][] NEAR_PINES = {
        { 2, 31, 8.5 }, { 10, 27, 7.0 }, { 18, 21, 5.6 },
        { 77, 30, 8.5 }, { 69, 25, 6.5 }, { 62, 18, 5.0 }
    };

    private final int[] src  = new int[PW * PH];    // everything above the far shoreline: what the lake reflects
    private final int[] far  = new int[PW * PH];    // baked ridge + tree rows, 0xFFRRGGBB (0 = empty)
    private final int[] near = new int[PW * PH];    // foreground peninsulas, redrawn each frame (the pines sway)
    private final int[] px   = new int[PW * PH];    // the finished frame, 0xRRGGBB
    private final int[] nearShore = new int[PW];    // waterline of the foreground land per column (0 = none)
    private double rHi = 0.0;                        // side output of displacement(): ring crest highlight
    private boolean ready = false;

    public ForestLakeLoader() {
        super(LAKE_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04;
        if (!ready) { buildStatic(); ready = true; }

        // 1. Everything above the far shoreline (this is what the water will mirror)
        paintSky();
        paintFar();
        paintMist();
        paintSkyLife();
        System.arraycopy(src, 0, px, 0, HZ * PW);

        // 2. The lake: mirrored, rippled, glittering
        paintWater();

        // 3. Foreground peninsulas: their reflection first, then the land and pines themselves
        paintNear();
        reflectNear();
        for (int i = 0; i < near.length; i++) {
            if ((near[i] >>> 24) != 0) px[i] = near[i] & 0xFFFFFF;
        }

        // 4. Things floating on / growing out of the near water
        paintSwan();
        paintReeds();

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = x + WIDTH * y;
                if (Z_SCENE > zBuffer[index]) {
                    zBuffer[index] = Z_SCENE;
                    double vig = 1.0 - 0.14 * sq((x - 39.5) / 40.0);       // gentle vignette
                    outputBuffer[index] = cell(shade(px[(2 * y) * PW + x], vig), shade(px[(2 * y + 1) * PW + x], vig));
                }
            }
        }
    }

    // =====================================================================
    //  SKY: sunset gradient, glow with faint rays, stars, drifting clouds
    // =====================================================================

    private static int skyColor(int py) {
        double t = Math.min(1.0, py / 21.0) * (SKY_STOPS.length - 1);
        int i = Math.min(SKY_STOPS.length - 2, (int) t);
        double f = t - i;
        return mix(SKY_STOPS[i], SKY_STOPS[i + 1], f * f * (3 - 2 * f));
    }

    private void paintSky() {
        for (int py = 0; py < HZ; py++) {
            int base = skyColor(py);
            for (int x = 0; x < PW; x++) {
                double dx = x - SUN_X, dy = py - SUN_Y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double ray  = 0.75 + 0.25 * Math.sin(Math.atan2(dy, dx) * 11.0 + timeClock * 0.15);
                int c = mix(base, rgb(255, 178, 96), 0.70 * Math.exp(-dist * dist / (2 * 15.0 * 15.0)) * ray);
                double disc = clamp01(4.4 - dist + 0.5);
                if (disc > 0) c = mix(c, mix(rgb(255, 202, 120), rgb(255, 244, 206), clamp01(1.0 - dist / 4.4)), disc);
                src[py * PW + x] = c;
            }
        }

        for (int i = 0; i < 20; i++) {                       // stars come out in the indigo
            int sx = hash(i, 401) % PW, sy = hash(i, 402) % 9;
            double tw = 0.5 + 0.5 * Math.sin(timeClock * 2.0 + i * 1.7);
            plotSrc(sx, sy, 0xFFFFFF, 0.55 * tw * (1.0 - sy / 10.0));
        }

        for (int py = 2; py < 16; py++) {                    // clouds, lit from below by the sun
            double band = smooth(1.5, 4.5, py) * (1.0 - smooth(9.0, 15.0, py));
            for (int x = 0; x < PW; x++) {
                double n = 0.62 * noise2(x * 0.045 + timeClock * 0.30, py * 0.34, 21)
                         + 0.38 * noise2(x * 0.12 + timeClock * 0.45, py * 0.62, 22);
                double a = clamp01((n - 0.52) * 3.2) * band * 0.85;
                if (a < 0.01) continue;
                double sunSide = Math.exp(-sq((x - SUN_X) / 30.0));
                double under = smooth(3.0, 13.0, py);
                int cc = mix(rgb(84, 62, 128), rgb(255, 168, 130), clamp01(0.25 + 0.75 * sunSide * (0.4 + 0.6 * under)));
                src[py * PW + x] = mix(src[py * PW + x], cc, a);
            }
        }
    }

    private void paintFar() {
        for (int i = 0; i < HZ * PW; i++) {
            if ((far[i] >>> 24) != 0) src[i] = far[i] & 0xFFFFFF;
        }
    }

    /** Low mist along the far shore. Because it lives in the mirrored scene, it drifts over the water too. */
    private void paintMist() {
        for (int py = 14; py < HZ; py++) {
            for (int x = 0; x < PW; x++) {
                double m = Math.exp(-sq((py - 21.0) / 3.2)) * (0.5 + 0.5 * noise2(x * 0.06 - timeClock * 0.25, py * 0.4, 41));
                src[py * PW + x] = mix(src[py * PW + x], rgb(246, 190, 172), 0.48 * m);
            }
        }
    }

    /** Birds and fireflies. They sit in the mirrored scene, so the lake reflects them too. */
    private void paintSkyLife() {
        for (int i = 0; i < 3; i++) {
            double bx = ((timeClock * 4.5 + i * 29.0) % 110.0) - 14.0;
            double by = 6.0 + i * 2.5 + 1.5 * Math.sin(timeClock * 0.6 + i);
            int x0 = (int) Math.round(bx), y0 = (int) Math.round(by);
            int wing = Math.sin(timeClock * 9.0 + i * 2.0) > 0 ? -1 : 1;
            int bird = rgb(36, 24, 54);
            plotSrc(x0 - 2, y0 + wing, bird, 1.0);
            plotSrc(x0 - 1, y0, bird, 1.0);
            plotSrc(x0, y0, bird, 1.0);
            plotSrc(x0 + 1, y0, bird, 1.0);
            plotSrc(x0 + 2, y0 + wing, bird, 1.0);
        }
        for (int i = 0; i < 16; i++) {
            double fx = 12 + rand01(i, 201) * 56 + 4.0 * Math.sin(timeClock * 0.5 + i * 1.3);
            double fy = 11 + rand01(i, 202) * 9 + 2.0 * Math.sin(timeClock * 0.7 + i * 2.1);
            double b = Math.max(0.0, Math.sin(timeClock * 1.6 + i * 2.4));
            b *= b;
            if (b < 0.03) continue;
            int x = (int) fx, y = (int) fy;
            int fire = rgb(222, 255, 120);
            plotSrc(x, y, fire, 0.95 * b);
            plotSrc(x - 1, y, fire, 0.25 * b);
            plotSrc(x + 1, y, fire, 0.25 * b);
            plotSrc(x, y - 1, fire, 0.25 * b);
            plotSrc(x, y + 1, fire, 0.25 * b);
        }
    }

    // =====================================================================
    //  THE LAKE: mirror + ripples + streaks + glitter path + expanding rings
    // =====================================================================

    /** Horizontal shift applied to the reflection at this pixel (rolling ripples plus expanding rings). */
    private double displacement(int x, int py) {
        double d = py - HZ;
        double amp = 0.30 + 0.05 * d;                 // ripples grow toward the viewer
        double disp = amp * (0.6 * Math.sin(py * 0.85 + x * 0.23 + timeClock * 1.7)
                           + 0.4 * Math.sin(py * 1.9 - x * 0.11 - timeClock * 2.4))
                    + (0.5 + 0.05 * d) * Math.sin(py * 0.33 + timeClock * 0.7);   // slow swell

        rHi = 0.0;
        for (int k = 0; k < 3; k++) {
            double clock = timeClock + k * 3.1;
            double age = clock % RING_PERIOD[k];
            if (age > RING_LIFE) continue;
            int cyc = (int) (clock / RING_PERIOD[k]);
            double cx = 12 + rand01(cyc, 50 + k) * 56;
            double cy = 27 + rand01(cyc, 60 + k) * 11;
            double r = age * 4.2;
            double dx = x - cx;
            if (Math.abs(dx) > r + 5) continue;
            double dy = (py - cy) * 2.6;                            // rings are squashed by perspective
            double band = Math.sqrt(dx * dx + dy * dy) - r;
            double env = Math.exp(-band * band / 3.5) * (1.0 - age / RING_LIFE);
            double s = Math.sin(band * 1.7);
            disp += 1.5 * env * s;
            rHi += Math.max(0.0, s) * env;
        }
        return disp;
    }

    private void paintWater() {
        for (int py = HZ; py < PH; py++) {
            double d = py - HZ;
            double refl = 0.90 - 0.012 * d;                         // a little less mirror-like up close
            int tint = mix(WATER_FAR, WATER_NEAR, d / (PH - HZ - 1.0));
            for (int x = 0; x < PW; x++) {
                double disp = displacement(x, py);
                int sx = clampInt((int) Math.round(x + disp), 0, PW - 1);
                double sy = 2 * HZ - 1 - py + 0.5 * Math.sin(x * 0.45 + timeClock * 1.2);
                int col = mix(tint, src[clampInt((int) Math.round(sy), 0, HZ - 1) * PW + sx], refl);

                // Long ripple crests catch the bright sky, streaking across the dark tree reflections
                double n = noise2(x * 0.16 + timeClock * 0.5, py * 0.85 - timeClock * 0.25, 31);
                double hl = Math.max(0.0, n - 0.66) * 2.2 + rHi * 0.35;
                if (hl > 0) col = mix(col, WARM_SPARK, Math.min(0.55, hl * 0.5));

                // Sun glitter path, widening toward the viewer
                double w = 2.0 + d * 0.30;
                double gx = Math.exp(-sq((x - SUN_X) / w));
                if (gx > 0.03) {
                    col = mix(col, WARM_SPARK, 0.16 * gx * (1.0 - d / 24.0));
                    double flick = rand01(x + py * 131, (int) Math.floor(timeClock * 7.0 + rand01(x, py) * 9.0));
                    if (flick < 0.5 * gx * (1.0 - d / 24.0)) col = mix(col, GLINT, 0.85);
                }
                // Scattered twinkles everywhere else
                double tw = rand01(x * 3 + py * 17, (int) Math.floor(timeClock * 4.0 + rand01(py, x) * 11.0));
                if (tw < 0.010 + 0.0006 * d) col = mix(col, WARM_SPARK, 0.7);

                px[py * PW + x] = col;
            }
        }
    }

    // =====================================================================
    //  FOREGROUND: two peninsulas with big swaying pines, reflected in the lake
    // =====================================================================

    private static int nearShoreY(int x) {
        if (x < 40) {
            int xx = Math.min(x, 24);
            return (int) Math.round(30.0 - 0.12 * xx + 0.9 * Math.sin(xx * 0.45 + 0.6));
        }
        int xx = Math.max(x, 55);
        return (int) Math.round(30.0 - 0.12 * (79 - xx) + 0.9 * Math.sin(xx * 0.41 + 2.0));
    }

    private static int landThick(int x) {
        if (x < 40) return x <= 20 ? 4 : Math.max(0, 24 - x);
        return x >= 60 ? 4 : Math.max(0, x - 56);
    }

    private void paintNear() {
        Arrays.fill(near, 0);
        for (int x = 0; x < PW; x++) {
            int thk = landThick(x);
            if (thk == 0) continue;
            int y2 = nearShoreY(x);
            for (int y = y2 - thk; y < y2; y++) {
                put(near, x, y, y == y2 - thk ? GRASS : shade(LAND, 0.85 + 0.3 * rand01(x, y)));
            }
        }
        for (int i = 0; i < NEAR_PINES.length; i++) {
            int cx = (int) NEAR_PINES[i][0];
            int base = nearShoreY(cx) - landThick(cx);
            double sway = 0.9 * Math.sin(timeClock * 0.9 + i * 1.9);        // wind in the treetops
            pine(near, cx, base, (int) NEAR_PINES[i][1], NEAR_PINES[i][2], rgb(8, 26, 30), rgb(232, 142, 84), sway);
        }
    }

    private void reflectNear() {
        for (int x = 0; x < PW; x++) {
            int y2 = nearShore[x];
            if (y2 == 0) continue;
            for (int py = y2; py < PH; py++) {
                int ys = 2 * y2 - 1 - py;
                if (ys < 0) break;
                int xs = clampInt((int) Math.round(x + displacement(x, py) * 1.1), 0, PW - 1);
                int s = near[ys * PW + xs];
                if ((s >>> 24) == 0) continue;
                double a = Math.max(0.35, 0.92 - 0.03 * (py - y2));
                px[py * PW + x] = mix(px[py * PW + x], mix(s & 0xFFFFFF, WATER_NEAR, 0.18), a);
            }
        }
    }

    // =====================================================================
    //  SWAN, WAKE AND REEDS
    // =====================================================================

    private void paintSwan() {
        double cx = -16 + ((timeClock * 2.6 + 40.0) % (PW + 36));         // gliding slowly left -> right
        int icx = (int) Math.round(cx);
        int wl = 36 + (int) Math.round(0.6 * Math.sin(timeClock * 1.3));  // bobs on the water

        for (int k = 2; k <= 12; k++) {                                   // faint V-wake behind the swan
            double a = 0.32 * (1.0 - k / 13.0);
            int wx = icx - (int) Math.round(4 + k * 1.3);
            plot(wx, wl - (int) Math.round(k * 0.12), 0xFFFFFF, a);
            plot(wx, wl + 1 + (int) Math.round(k * 0.22), 0xFFFFFF, a);
        }

        for (int dy = 1; dy <= 12; dy++) {                                // rippling reflection under the swan
            int py = wl + dy;
            if (py >= PH) break;
            double fade = 0.62 * (1.0 - (dy - 1) / 13.0);
            for (int dx = -10; dx <= 10; dx++) {
                int x = icx + dx;
                if (x < 0 || x >= PW) continue;
                double wob = 0.9 * Math.sin(py * 0.9 + x * 0.2 + timeClock * 2.0);
                int c = swanPixel(dx + wob, -(dy - 1));
                if (c >= 0) plot(x, py, mix(c, WATER_NEAR, 0.35), fade);
            }
        }
        for (int dy = -13; dy <= 0; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                int c = swanPixel(dx, dy);
                if (c >= 0) plot(icx + dx, wl + dy, c);
            }
        }
    }

    /** Swan facing right, waterline at v = 0, v negative is up. Returns -1 where empty. */
    private static int swanPixel(double du, double dv) {
        double u = du / 0.85, v = dv / 0.85;
        double bx = u / 5.2, by = (v + 1.3) / 2.5;
        boolean body = bx * bx + by * by <= 1.0 && v <= 0.5;
        boolean tail = u >= -7.6 && u < -3.5 && v <= -1.0 && v >= -2.4 - (-3.5 - u) * 0.35;
        double t = (-v - 2.5) / 7.5;                                       // 0 at the neck base .. 1 at the head
        double nc = 3.4 + 1.6 * t - 1.3 * Math.sin(t * Math.PI);           // S-curved neck centreline
        boolean neck = t >= 0.0 && t <= 1.0 && Math.abs(u - nc) <= 0.85;
        boolean head = sq(u - 5.2) + sq(v + 10.7) <= 1.5;
        if (sq(u - 5.5) + sq(v + 10.9) < 0.3) return rgb(30, 30, 40);      // eye
        if (v >= -11.1 && v <= -10.2 && u >= 6.1 && u <= 8.2) return u < 6.7 ? rgb(30, 30, 30) : rgb(238, 132, 38); // bill
        if (!(body || tail || neck || head)) return -1;
        int c = rgb(248, 248, 252);
        if (v > -0.9) c = rgb(196, 204, 224);                              // shadow at the waterline
        else if (body && Math.floorMod((int) Math.floor(u * 0.6 + 20), 2) == 0 && v > -2.6) c = rgb(232, 234, 246); // feathers
        return mix(c, rgb(255, 214, 180), v < -2.0 ? 0.22 : 0.08);         // warm sunset tint
    }

    private void paintReeds() {
        for (int x = 0; x < PW; x++) {                                     // dark bank along the bottom edge
            int top = 41 + (hash(x >> 1, 9) % 2);
            for (int y = top; y < PH; y++) px[y * PW + x] = shade(rgb(6, 16, 18), 0.8 + 0.4 * rand01(x, y));
        }
        for (int i = 0; i < 26; i++) {
            double x0 = i * 3.2 + 1.5 * rand01(i, 301);
            double edge = Math.abs(x0 - 40.0) / 40.0;                       // keep the middle low so the glitter shows
            int h = (int) (3 + 9 * rand01(i, 302) * (0.30 + 0.70 * edge));
            int lastX = (int) Math.round(x0), lastY = 42;
            for (int s = 0; s < h; s++) {
                double f = s / (double) h;
                lastX = (int) Math.round(x0 + 1.3 * f * f * Math.sin(timeClock * 1.1 + i * 1.7));
                lastY = 42 - s;
                plot(lastX, lastY, rgb(7, 20, 20), 1.0);
                if (s % 2 == 0) plot(lastX + 1, lastY, rgb(232, 142, 84), 0.30);  // sunset rim light
            }
            if (rand01(i, 303) < 0.4) {                                     // cattail head
                plot(lastX, lastY - 1, rgb(58, 34, 22), 1.0);
                plot(lastX, lastY - 2, rgb(58, 34, 22), 1.0);
            }
        }
    }

    // =====================================================================
    //  STATIC FAR SHORE: ridge, three rows of trees, shoreline
    // =====================================================================

    private static double ridgeTop(int x) {
        return 15.0 + 3.0 * Math.sin(x * 0.09 + 0.8) + 2.0 * Math.sin(x * 0.23 + 1.5) + 1.1 * Math.sin(x * 0.51)
             + 4.5 * Math.exp(-sq((x - SUN_X) / 11.0));          // a saddle in the ridge where the sun sets
    }

    private void buildStatic() {
        for (int x = 0; x < PW; x++) nearShore[x] = (x <= 27 || x >= 52) ? nearShoreY(x) : 0;

        // Distant mountain ridge, hazy purple with a sunset rim
        for (int x = 0; x < PW; x++) {
            double top = ridgeTop(x);
            for (int y = (int) Math.ceil(top); y < HZ; y++) {
                int c = mix(rgb(150, 108, 152), rgb(86, 68, 126), (y - top) / 8.0);
                if (y - top < 1.2) c = mix(c, rgb(255, 190, 140), 0.35);
                put(far, x, y, c);
            }
        }

        // Row 1: tiny hazy pines
        for (int i = 0; ; i++) {
            double cx = i * 3.1 + 1.0 + 1.6 * rand01(i, 101);
            if (cx > PW + 3) break;
            pine(far, cx, 19, 6 + (int) (3 * rand01(i, 102)), 1.9 + 0.8 * rand01(i, 103), rgb(90, 76, 134), rgb(214, 140, 132), 0.0);
        }
        // Row 2: mid-size blue-green pines
        for (int i = 0; ; i++) {
            double cx = i * 4.3 + 2.0 + 2.0 * rand01(i, 105);
            if (cx > PW + 4) break;
            pine(far, cx, 20, 9 + (int) (5 * rand01(i, 106)), 2.8 + 0.9 * rand01(i, 107), rgb(42, 62, 92), rgb(200, 118, 110), 0.0);
        }
        // Dark shoreline strip: gives the reflection a strong base
        for (int x = 0; x < PW; x++) {
            for (int y = HZ - 1 - (hash(x, 7) % 3 == 0 ? 1 : 0); y < HZ; y++) put(far, x, y, rgb(12, 26, 32));
        }
        // Row 3: big dark pines, with the odd autumn tree for colour
        double cx = 1.5;
        for (int i = 0; cx < PW + 4; i++) {
            if (rand01(i, 110) < 0.24) {
                boolean gold = rand01(i, 112) < 0.55;
                roundTree(far, cx, HZ - 1, 10 + (int) (3 * rand01(i, 113)), 3.2 + rand01(i, 114),
                          gold ? rgb(188, 110, 40) : rgb(158, 60, 40), gold ? rgb(255, 204, 96) : rgb(238, 130, 70));
            } else {
                pine(far, cx, HZ - 1, 11 + (int) (6 * rand01(i, 115)), 3.6 + 1.4 * rand01(i, 116), rgb(18, 42, 50), rgb(230, 140, 84), 0.0);
            }
            cx += 4.2 + 3.0 * rand01(i, 111);
        }
    }

    // ---- tree drawing (into any ARGB layer) ----

    /** Tiered pine: steps outward at each branch tier, sun-facing edge catches warm light. sway moves the top. */
    private void pine(int[] buf, double cx, int baseY, int h, double halfW, int col, int rim, double sway) {
        int trunk = Math.max(1, h / 7);
        int fh = h - trunk;
        int topY = baseY - h + 1;
        int tiers = Math.max(2, fh / 3);
        double litSide = SUN_X >= cx ? 1.0 : -1.0;
        for (int i = 0; i < fh; i++) {
            int y = topY + i;
            double f = (i + 0.5) / fh;
            double tf = f * tiers;
            int tier = (int) tf;
            double w = Math.max(0.6, halfW * (tier + 0.35 + 0.65 * (tf - tier)) / tiers);
            double c0 = cx + sway * Math.pow(1.0 - f, 1.5);
            for (int x = (int) Math.round(c0 - w); x <= (int) Math.round(c0 + w); x++) {
                double side = (x - c0) / w;
                int c = mix(col, rim, 0.75 * clamp01((side * litSide - 0.1) * 1.4));
                int hs = hash(x, y) % 9;
                if (hs == 0) c = shade(c, 1.22); else if (hs == 1) c = shade(c, 0.8);
                put(buf, x, y, c);
            }
        }
        for (int i = 0; i < trunk; i++) {
            put(buf, (int) Math.round(cx), baseY - i, TRUNK);
            if (halfW > 4) put(buf, (int) Math.round(cx) + 1, baseY - i, TRUNK);
        }
    }

    /** Round-crowned autumn tree built from three overlapping lobes. */
    private void roundTree(int[] buf, double cx, int baseY, int h, double r, int dark, int light) {
        int trunk = (int) (h * 0.45);
        for (int i = 0; i < trunk; i++) put(buf, (int) Math.round(cx), baseY - i, TRUNK);
        double litSide = SUN_X >= cx ? 1.0 : -1.0;
        double ccx = cx, ccy = baseY - trunk - r * 0.4;
        double[][] lobes = { { -0.45, 0.0, 0.72 }, { 0.45, 0.1, 0.72 }, { 0.0, -0.5, 0.72 } };
        for (int y = (int) (ccy - r * 1.4); y <= (int) (ccy + r); y++) {
            for (int x = (int) (ccx - r * 1.4); x <= (int) (ccx + r * 1.4); x++) {
                boolean inside = false;
                for (double[] lb : lobes) {
                    if (sq(x - (ccx + lb[0] * r)) + sq(y - (ccy + lb[1] * r)) <= sq(lb[2] * r)) { inside = true; break; }
                }
                if (!inside) continue;
                double k = clamp01(0.5 + 0.5 * ((x - ccx) * litSide / r) - 0.35 * ((y - ccy) / r));
                int c = mix(dark, light, k);
                int hs = hash(x, y * 3) % 6;
                if (hs == 0) c = shade(c, 1.2); else if (hs == 1) c = shade(c, 0.82);
                put(buf, x, y, c);
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

    private void plot(int x, int y, int c, double alpha) {
        if (x >= 0 && x < PW && y >= 0 && y < PH) px[y * PW + x] = alpha >= 0.999 ? c : mix(px[y * PW + x], c, alpha);
    }

    private void plot(int x, int y, int c) {
        plot(x, y, c, 1.0);
    }

    private void plotSrc(int x, int y, int c, double alpha) {
        if (x >= 0 && x < PW && y >= 0 && y < HZ) src[y * PW + x] = mix(src[y * PW + x], c, alpha);
    }

    private static void put(int[] buf, int x, int y, int c) {
        if (x >= 0 && x < PW && y >= 0 && y < PH) buf[y * PW + x] = 0xFF000000 | c;
    }

    private static double sq(double v) { return v * v; }

    private static double clamp01(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    private static int clampInt(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }

    private static double smooth(double a, double b, double x) {
        double t = clamp01((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

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