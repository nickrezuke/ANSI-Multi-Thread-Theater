import java.util.Arrays;

public class CoralReefLoader extends Loader {
    private static final StatusStage[] REEF_STAGES = {
        new StatusStage(25, "Calibrating volumetric sunbeam scattering:"),
        new StatusStage(50, "Generating vibrant coral polyps & topography:"),
        new StatusStage(75, "Spawning synchronized schools of neon tetras:"),
        new StatusStage(100, "Coral Reef Biosphere Active!")
    };

    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW = WIDTH;       // sub-pixel canvas: one column per cell...
    private static final int PH = HEIGHT * 2;  // ...and two rows per cell (half-blocks)
    private static final double Z_SCENE = 0.5;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    // Tropical Reef Water Gradient (Teal / Cyan)
    private static final int[] RGB_WATER_TOP = { 0,   210, 255 }; // Bright tropical cyan
    private static final int[] RGB_WATER_MID = { 0,   140, 200 }; // Rich teal
    private static final int[] RGB_WATER_BTM = { 0,   60,  130 }; // Deep oceanic blue

    // Sunbeam Color
    private static final int RGB_SUNBEAM = 0xFFFADC; // Warm, pale sunlight

    // Vibrant Coral Colors (the original four warm tones, plus cooler accents)
    private static final int[][] CORAL_PALETTE = {
        { 220, 50,  130 }, // Hot Pink
        { 160, 40,  200 }, // Deep Purple
        { 255, 120, 30  }, // Vibrant Orange
        { 200, 70,  90  }, // Muted Rose
        { 30,  180, 170 }, // Teal
        { 150, 215, 70  }, // Lime
        { 130, 110, 230 }, // Lavender
        { 245, 195, 60  }  // Sun Yellow
    };
    private static final int[] CLUSTER_PICK = { 0, 1, 2, 3, 0, 2, 4, 5, 6, 7 }; // weights the mix toward warm colors

    // Baked (static) scene: reef wall, sand, corals. -1 = empty.
    private final int[] scene    = new int[PW * PH];
    private final int[] waterRow = new int[PH];
    private final int[] frame    = new int[PW * PH];
    private final int[] groundY  = new int[PW];
    private final int[] glowX    = new int[256];
    private final int[] glowY    = new int[256];
    private int glowCount = 0;
    private boolean built = false;
    private double segT = 0.0; // side output of segDist()

    /** A sprite silhouette: returns {r, g, b, alpha%} for a point in local space (u = toward head, v = down), or null. */
    private interface SpriteShape {
        int[] pixel(double u, double v);
    }

    public CoralReefLoader() {
        super(REEF_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04; // Slightly faster to make the fish school look active
        if (!built) {
            buildScene();
            built = true;
        }

        paintWater();      // smooth gradient + volumetric god rays
        paintScene();      // baked reef / sand / corals, lit by drifting caustics
        paintGlowPolyps();
        paintMotes();      // drifting plankton
        paintSwayers();    // sea fans, sea whips, anemone
        paintTurtle();     // slow, left -> right
        paintClownfish();  // hovering in the anemone
        paintSchool();     // neon tetras, right -> left

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = x + WIDTH * y;
                int top = frame[x + PW * (2 * y)];
                int bot = frame[x + PW * (2 * y + 1)];
                String out = (y == 0) ? surface(x, top, bot) : cell('\u2580', top, bot);
                if (Z_SCENE > zBuffer[index]) {
                    zBuffer[index] = Z_SCENE;
                    outputBuffer[index] = out;
                }
            }
        }
    }

    // =====================================================================
    //  WATER: one continuous gradient, with the original god-ray maths
    // =====================================================================

    private int waterGradient(int py) {
        // Quadratic curve through TOP (t=0), MID (t=0.5), BTM (t=1): no visible seams
        double t = py / (double) (PH - 1);
        double a = (1 - t) * (1 - 2 * t);
        double b = 4 * t * (1 - t);
        double c = t * (2 * t - 1);
        return pack(
            clamp(RGB_WATER_TOP[0] * a + RGB_WATER_MID[0] * b + RGB_WATER_BTM[0] * c),
            clamp(RGB_WATER_TOP[1] * a + RGB_WATER_MID[1] * b + RGB_WATER_BTM[1] * c),
            clamp(RGB_WATER_TOP[2] * a + RGB_WATER_MID[2] * b + RGB_WATER_BTM[2] * c));
    }

    private void paintWater() {
        for (int py = 0; py < PH; py++) {
            double y = py * 0.5;
            double depthFade = Math.max(0.0, 1.0 - (y / 20.0)); // sunbeams fade as they go deeper
            int base = waterRow[py];
            for (int px = 0; px < PW; px++) {
                // Overlapping wave patterns form diagonal shifting beams; only the peaks show
                double rayPhase1 = Math.sin((px - y * 1.8) * 0.12 - timeClock * 0.7);
                double rayPhase2 = Math.sin((px - y * 1.2) * 0.08 - timeClock * 0.4);
                double ray = Math.max(0.0, rayPhase1 + rayPhase2 - 1.0) * depthFade * 0.45;
                frame[px + PW * py] = ray > 0.0 ? mixRGB(base, RGB_SUNBEAM, ray) : base;
            }
        }
    }

    /** Shimmering waterline along the top row. */
    private String surface(int x, int top, int bot) {
        double wave = 0.5 + 0.5 * Math.sin(x * 0.55 + timeClock * 2.2) * Math.sin(x * 0.17 - timeClock * 0.9);
        int bg = mixRGB(mixRGB(top, bot, 0.5), 0xFFFFFF, 0.20);
        int fg = mixRGB(pack(150, 225, 255), pack(240, 252, 255), wave);
        return cell('~', fg, bg);
    }

    private void paintScene() {
        for (int py = 0; py < PH; py++) {
            for (int px = 0; px < PW; px++) {
                int s = scene[px + PW * py];
                if (s < 0) continue;
                // Caustics: a shifting net of light across the reef and sand
                double c = Math.sin(px * 0.55 + timeClock * 1.3)
                         + Math.sin(py * 0.70 - timeClock * 1.1 + px * 0.15)
                         + Math.sin((px + py) * 0.38 + timeClock * 0.8);
                double k = Math.max(0.0, c - 1.3) * 0.22;
                frame[px + PW * py] = k > 0.0 ? scaleRGB(s, 1.0 + k) : s;
            }
        }
    }

    private void paintGlowPolyps() {
        for (int i = 0; i < glowCount; i++) {
            double pulse = 0.5 + 0.5 * Math.sin(timeClock * 2.6 + i * 1.9);
            plot(glowX[i], glowY[i], pack(0, 255, 255), 0.35 + 0.55 * pulse); // bioluminescent cyan
        }
    }

    private void paintMotes() {
        for (int i = 0; i < 26; i++) {
            double x = ((hash(i, 1) % PW) + timeClock * (0.8 + (i % 3) * 0.5) * 3.0) % PW;
            double y = ((hash(i, 2) % PH) + timeClock * (0.6 + (i % 4) * 0.25) * 2.0) % (PH - 2);
            int px = (int) x;
            int py = (int) y;
            if (scene[px + PW * py] < 0) plot(px, py, 0xFFFFFF, 0.12 + 0.06 * (i % 3));
        }
    }

    // =====================================================================
    //  REEF: sloping coral wall (original heightfield), sandy floor, colonies
    // =====================================================================

    private double reefTopPx(int x) {
        // The original terrain curve (in rows), converted to sub-pixels, with a little lumpiness
        double rows = 16.0 - 5.0 * Math.sin(x * 0.08) - 2.5 * Math.cos(x * 0.15);
        return 2.0 * rows + 1.3 * Math.sin(x * 0.9 + 1.0) + 0.9 * Math.sin(x * 2.3 + 0.4);
    }

    private double sandTopPx(int x) {
        return 38.5 + 0.9 * Math.sin(x * 0.17 + 0.5) + 0.6 * Math.sin(x * 0.41 + 2.0);
    }

    private void buildScene() {
        Arrays.fill(scene, -1);
        for (int py = 0; py < PH; py++) waterRow[py] = waterGradient(py);

        for (int x = 0; x < PW; x++) {
            double rt = reefTopPx(x);
            double st = sandTopPx(x);
            double top = Math.min(rt, st);
            groundY[x] = (int) Math.floor(top);
            for (int py = (int) Math.ceil(top); py < PH; py++) {
                if (py >= st) {
                    setScene(x, py, sandPixel(x, py, py - st), 0.35);
                } else {
                    double depth = py - rt;
                    setScene(x, py, reefPixel(x, py, depth));
                    if (depth > 2.0 && glowCount < glowX.length && hash(x * 7, py * 13 + 5) % 40 == 0) {
                        glowX[glowCount] = x;
                        glowY[glowCount] = py;
                        glowCount++;
                    }
                }
            }
        }
        decorate();
    }

    private int sandPixel(int x, int py, double depth) {
        double ripple = 0.90 + 0.10 * Math.sin(x * 0.45 + py * 1.7 + Math.sin(x * 0.13) * 2.0);
        int[] sand = { 246, 226, 170 };
        int h = hash(x, py * 5);
        if (h % 17 == 0)      sand = new int[] { 200, 176, 136 };  // darker pebble
        else if (h % 53 == 0) sand = new int[] { 250, 240, 228 };  // shell fragment
        double shade = ripple * (depth < 1.0 ? 1.10 : 1.0 - 0.03 * depth);
        return scaleRGB(pack(sand), shade);
    }

    private int reefPixel(int x, int py, double depth) {
        final int cellW = 6;
        final int cellH = 4;
        int gx = Math.floorDiv(x, cellW);
        int gy = Math.floorDiv(py, cellH);
        double best = 1e9, second = 1e9;
        int bestId = 0;
        // Voronoi colonies: each nearest-site region is one coral head
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                int cx = gx + i;
                int cy = gy + j;
                double sx = (cx + 0.15 + 0.7 * (hash(cx, cy * 3 + 1) % 100) / 100.0) * cellW;
                double sy = (cy + 0.15 + 0.7 * (hash(cx * 5 + 2, cy) % 100) / 100.0) * cellH;
                double d = Math.hypot(x + 0.5 - sx, py + 0.5 - sy);
                if (d < best) { second = best; best = d; bestId = hash(cx, cy); }
                else if (d < second) second = d;
            }
        }
        int[] base = CORAL_PALETTE[CLUSTER_PICK[bestId % CLUSTER_PICK.length]];
        double tone = 0.88 + 0.24 * ((bestId >> 8) % 100) / 100.0;

        // Shading for depth (lower parts of the reef are darker), bright lip along the top
        double shade = Math.max(0.55, Math.min(1.12, 1.12 - 0.035 * depth)) * tone;
        if (depth < 1.5) shade *= 1.15;

        switch ((bestId >> 4) % 3) {
            case 0:  shade *= 0.82 + 0.28 * (0.5 + 0.5 * Math.sin((x + py * 0.8) * 1.4 + bestId)); break; // ridged, brain-like
            case 1:  if (hash(x, py * 7 + bestId) % 4 == 0) shade *= 1.22; break;                        // polyp dots
            default: shade *= 0.92 + 0.16 * (hash(x * 3, py) % 100) / 100.0;                              // fine speckle
        }
        if (second - best < 0.8) shade *= 0.74; // crevices between colonies
        int c = scaleRGB(pack(base), shade);
        if (shade < 1.0) c = mixRGB(c, pack(45, 35, 110), (1.0 - shade) * 0.5); // shadows lean violet, not brown
        return c;
    }

    private void decorate() {
        // Brain corals (domed, ridged)
        dome(4,  groundY[4] + 1,  5.0, 4.2, new int[] { 215, 190, 80  }, 3);
        dome(77, groundY[77] + 1, 4.2, 3.6, new int[] { 90,  200, 150 }, 7);

        // Table coral: a cool teal plate on a stalk
        tableCoral(22, groundY[22] - 9, 8.5, 1.9, groundY[22] + 1, new int[] { 40, 190, 170 });

        // Branching corals
        branch(40, groundY[40] + 1, -Math.PI / 2 + 0.10, 5.0, 1.3, 3, pack(255, 130, 40), pack(255, 220, 130), 0.0, 11);
        branch(73, groundY[73] + 1, -Math.PI / 2 - 0.10, 4.6, 1.2, 3, pack(235, 85, 150), pack(255, 210, 232), 0.0, 23);

        // Tube sponges
        tubeSponge(44, groundY[44] + 1, 3, 8,  new int[] { 160, 80,  200 });
        tubeSponge(47, groundY[47] + 1, 3, 12, new int[] { 190, 110, 220 });
        tubeSponge(50, groundY[50] + 1, 3, 9,  new int[] { 235, 200, 70  });

        // Starfish on the sand
        starfish(64.5, groundY[64] + 1.2, 3.0, pack(255, 140, 50));
    }

    private void dome(double cx, double baseY, double r, double h, int[] color, int seed) {
        int c = pack(color);
        for (int py = (int) Math.floor(baseY - h); py <= (int) Math.ceil(baseY); py++) {
            for (int px = (int) Math.floor(cx - r); px <= (int) Math.ceil(cx + r); px++) {
                double nx = (px + 0.5 - cx) / r;
                double ny = (py + 0.5 - baseY) / h;
                double q = nx * nx + ny * ny;
                if (q > 1.0 || ny > 0.15) continue;
                double nz = Math.sqrt(Math.max(0.0, 1.0 - q));
                double lit = -0.5 * nx - 0.55 * ny + 0.66 * nz;            // light from the upper left
                double shade = 0.50 + 0.70 * Math.max(0.0, lit);
                double ridge = 0.5 + 0.5 * Math.sin((px - cx) * 1.5 + 2.0 * Math.sin((py - baseY) * 0.9 + seed) + seed);
                shade *= 0.78 + 0.32 * ridge;                               // maze-like grooves
                setScene(px, py, scaleRGB(c, shade));
            }
        }
    }

    private void tableCoral(double cx, double plateY, double a, double b, double groundBase, int[] color) {
        int c = pack(color);
        int sx = (int) Math.round(cx);
        for (int py = (int) plateY; py <= (int) groundBase; py++) {
            setScene(sx - 1, py, scaleRGB(c, 0.62));
            setScene(sx,     py, scaleRGB(c, 0.50));
        }
        for (int py = (int) Math.floor(plateY - b); py <= (int) Math.ceil(plateY + b); py++) {
            for (int px = (int) Math.floor(cx - a); px <= (int) Math.ceil(cx + a); px++) {
                double nx = (px + 0.5 - cx) / a;
                double ny = (py + 0.5 - plateY) / b;
                double q = nx * nx + ny * ny;
                if (q > 1.0) continue;
                double shade = ny < -0.2 ? 1.18 : ny > 0.35 ? 0.62 : 0.95; // lit top, shadowed underside
                shade *= 0.90 + 0.20 * (0.5 + 0.5 * Math.sin(px * 1.3));
                if (q > 0.7) shade *= 1.12;                                  // pale growing edge
                setScene(px, py, scaleRGB(c, shade));
            }
        }
    }

    private void branch(double x, double y, double ang, double len, double r, int depth,
                        int base, int tip, double lvl, int seed) {
        double x2 = x + Math.cos(ang) * len;
        double y2 = y + Math.sin(ang) * len;
        stroke(x, y, x2, y2, r, mixRGB(base, tip, lvl), mixRGB(base, tip, Math.min(1.0, lvl + 0.3)));
        if (depth == 0) return;
        double jitter = ((hash(seed, depth) % 100) / 100.0 - 0.5) * 0.5;
        double thin = Math.max(0.55, r * 0.72);
        branch(x2, y2, ang - 0.55 + jitter, len * 0.74, thin, depth - 1, base, tip, lvl + 0.3, seed * 3 + 1);
        branch(x2, y2, ang + 0.55 + jitter, len * 0.70, thin, depth - 1, base, tip, lvl + 0.3, seed * 3 + 2);
        if (depth == 3) branch(x2, y2, ang + jitter * 0.6, len * 0.62, thin, depth - 2, base, tip, lvl + 0.3, seed * 3 + 3);
    }

    private void stroke(double x0, double y0, double x1, double y1, double r, int c0, int c1) {
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(x1 - x0, y1 - y0) * 2));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            disc(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r, mixRGB(c0, c1, t));
        }
    }

    private void disc(double cx, double cy, double r, int rgb) {
        for (int py = (int) Math.floor(cy - r); py <= (int) Math.ceil(cy + r); py++) {
            for (int px = (int) Math.floor(cx - r); px <= (int) Math.ceil(cx + r); px++) {
                double dx = px + 0.5 - cx;
                double dy = py + 0.5 - cy;
                if (dx * dx + dy * dy <= r * r) setScene(px, py, rgb);
            }
        }
    }

    private void tubeSponge(int x, int baseY, int w, int h, int[] color) {
        int c = pack(color);
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                double k = (1.18 - 0.45 * dx / Math.max(1, w - 1)) * (1.0 - 0.25 * dy / h); // lit left, darker toward the base
                setScene(x + dx, baseY - dy, scaleRGB(c, k));
            }
        }
        for (int dx = 1; dx < w - 1; dx++) setScene(x + dx, baseY - h, pack(30, 10, 45)); // dark opening
        setScene(x, baseY - h, scaleRGB(c, 1.3));
        setScene(x + w - 1, baseY - h, scaleRGB(c, 1.0));
    }

    private void starfish(double cx, double cy, double r, int color) {
        for (int py = (int) Math.floor(cy - r); py <= (int) Math.ceil(cy + r); py++) {
            for (int px = (int) Math.floor(cx - r); px <= (int) Math.ceil(cx + r); px++) {
                double dx = px + 0.5 - cx;
                double dy = py + 0.5 - cy;
                double rad = Math.hypot(dx, dy);
                double edge = r * Math.pow(Math.max(0.0, 0.5 + 0.5 * Math.cos(5.0 * (Math.atan2(dy, dx) + Math.PI / 2))), 0.6);
                if (rad <= Math.max(edge, r * 0.34)) setScene(px, py, rad < 0.9 ? scaleRGB(color, 1.25) : color);
            }
        }
    }

    private void setScene(int x, int y, int rgb) {
        setScene(x, y, rgb, 1.0);
    }

    private void setScene(int x, int y, int rgb, double hazeScale) {
        if (x < 0 || x >= PW || y < 0 || y >= PH) return;
        // A touch of water haze so everything sits in the same ocean
        scene[x + PW * y] = mixRGB(rgb, waterRow[y], (0.06 + 0.16 * y / PH) * hazeScale);
    }

    // =====================================================================
    //  SWAYING LIFE: sea fans (lacy lattice), sea whips, anemone
    // =====================================================================

    private void paintSwayers() {
        whip(35, 12, 0.3, pack(200, 160, 235));
        whip(42, 10, 1.9, pack(230, 170, 210));
        whip(70, 13, 3.1, pack(200, 160, 235));

        fan(10, 12, 12, pack(255, 200, 50),  0.0);   // golden
        fan(30, 11, 11, pack(190, 60,  190), 2.0);   // purple
        fan(67, 12, 11, pack(255, 105, 70),  4.1);   // red-orange

        anemone(58);
    }

    private void fan(int bx, int fanH, int fanW, int color, double phase) {
        int rootY = groundY[bx] + 1;
        for (int h = 0; h < fanH; h++) {
            double f = h / (double) fanH;
            // Widens steadily from the stalk, then a rounded crown
            double hw = (fanW / 2.0) * (f < 0.6 ? f / 0.6
                                                : Math.sqrt(Math.max(0.0, 1.0 - Math.pow((f - 0.6) / 0.42, 2))));
            double cx = bx + 2.4 * Math.pow(f, 1.3) * Math.sin(timeClock * 1.3 + phase + h * 0.14); // rooted at the base
            int reach = (int) Math.ceil(hw);
            int rgb = mixRGB(scaleRGB(color, 0.62), scaleRGB(color, 1.12), f);
            for (int dx = -reach; dx <= reach; dx++) {
                if (Math.abs(dx) > hw) continue;
                double ratio = hw < 0.6 ? 0.0 : dx / hw;
                boolean rib = hw < 1.6 || Math.abs(((ratio + 1.0) * 3.0) % 1.0 - 0.5) < 0.22; // radial ribs
                boolean bar = h % 3 == 1;                                                     // cross-bars
                plot((int) Math.round(cx + dx), rootY - h, rib || bar ? rgb : scaleRGB(rgb, 0.85), rib || bar ? 1.0 : 0.34);
            }
        }
    }

    private void whip(int x, int len, double phase, int color) {
        int rootY = groundY[x] + 1;
        for (int h = 0; h < len; h++) {
            double f = h / (double) len;
            double sway = 2.2 * Math.pow(f, 1.4) * Math.sin(timeClock * 1.5 + phase + h * 0.22);
            plot((int) Math.round(x + sway), rootY - h, mixRGB(color, 0xFFFFFF, f * 0.55), 1.0);
        }
    }

    private void anemone(int ax) {
        int rootY = groundY[ax] + 1;
        // Foot
        for (int dx = -2; dx <= 2; dx++) {
            plot(ax + dx, rootY,     pack(150, 60, 120), 1.0);
            plot(ax + dx, rootY - 1, pack(175, 75, 135), 1.0);
        }
        // Tentacles fan outward and wave
        for (int k = -4; k <= 4; k++) {
            int len = (int) Math.round(7.0 - 0.6 * Math.abs(k));
            for (int h = 0; h < len; h++) {
                double f = h / (double) len;
                double x = ax + k * (1.25 + 0.10 * h) + 0.9 * f * f * Math.sin(timeClock * 2.3 + k * 0.9);
                plot((int) Math.round(x), rootY - 2 - h, mixRGB(pack(215, 55, 145), pack(255, 215, 238), f), 1.0);
            }
        }
    }

    // =====================================================================
    //  CREATURES
    // =====================================================================

    /** Draws a sprite centred on (cx, cy) in sub-pixel space. heading: +1 faces right, -1 faces left. */
    private void drawSprite(double cx, double cy, int heading, double reachU, double reachV, SpriteShape shape) {
        int x0 = (int) Math.floor(cx - reachU), x1 = (int) Math.ceil(cx + reachU);
        int y0 = (int) Math.floor(cy - reachV), y1 = (int) Math.ceil(cy + reachV);
        for (int py = y0; py <= y1; py++) {
            for (int px = x0; px <= x1; px++) {
                if (px < 0 || px >= PW || py < 0 || py >= PH) continue;
                int[] c = shape.pixel(heading * (px + 0.5 - cx), py + 0.5 - cy);
                if (c != null) plot(px, py, pack(c[0], c[1], c[2]), c[3] / 100.0);
            }
        }
    }

    private void paintTurtle() {
        // Slow glide left -> right, opposite to the school
        double cx = -14 + ((timeClock * 3.6 + 45.0) % (PW + 34));
        double cy = 9.0 + 2.2 * Math.sin(timeClock * 0.32 + 0.7);
        drawSprite(cx, cy, 1, 13.0, 10.0, this::turtlePixel);
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private int[] turtlePixel(double pu, double pv) {
        double u = pu / 1.15;
        double v = pv / 1.15;
        if (u < -9.0 || u > 11.0 || v < -4.5 || v > 8.5) return null;
        double flap = Math.sin(timeClock * 2.4);

        // Head, neck and eye
        double ex = u - 8.9, ey = v - 0.0;
        if (ex * ex + ey * ey < 0.36) return new int[] { 20, 24, 30, 100 };
        double hx = (u - 8.2) / 2.0, hy = (v - 0.5) / 1.35;
        if (hx * hx + hy * hy <= 1.0) return new int[] { 184, 186, 118, 100 };
        if (u > 5.6 && u < 7.4 && Math.abs(v - 0.7) < 0.9) return new int[] { 168, 172, 104, 100 };

        // Near front flipper: long, paddling forward and back below the shell
        double phi = 1.8 + 0.8 * flap;
        double ax = 1.8, ay = 1.0;
        double d = segDist(u, v, ax, ay, ax + 6.8 * Math.cos(phi), ay + 6.8 * Math.sin(phi));
        if (d <= 1.5 - 1.0 * segT) return new int[] { 178, 188, 112, 100 };

        // Rear flipper and tail
        double phi2 = 2.5 + 0.4 * Math.sin(timeClock * 2.4 + 1.0);
        double rx = -4.6, ry = 1.0;
        d = segDist(u, v, rx, ry, rx + 3.6 * Math.cos(phi2), ry + 3.6 * Math.sin(phi2));
        if (d <= 1.0 - 0.5 * segT) return new int[] { 150, 160, 92, 100 };
        d = segDist(u, v, -6.2, 0.8, -8.0, 1.4);
        if (d <= 0.7 - 0.3 * segT) return new int[] { 150, 160, 92, 100 };

        // Shell: domed, with a diamond grid of scutes
        double sx = u / 6.5, sy = (v + 0.3) / 3.7;
        double q = sx * sx + sy * sy;
        if (q <= 1.0 && v <= 1.0) {
            double lit = 1.15 - 0.35 * ((v + 4.0) / 5.0);
            boolean seam = frac((u + v * 0.7) / 3.1) < 0.16 || frac((u - v * 0.7) / 3.1) < 0.16;
            int[] c = seam ? new int[] { 52, 62, 32 } : new int[] { 108, 128, 62 };
            if (q > 0.78) c = new int[] { 66, 80, 42 };                    // darker rim
            return new int[] { clamp(c[0] * lit), clamp(c[1] * lit), clamp(c[2] * lit), 100 };
        }

        // Pale underside
        double bu = u / 5.4, bv = (v - 1.0) / 0.9;
        if (v > 1.0 && bu * bu + bv * bv <= 1.0) return new int[] { 214, 204, 150, 100 };
        return null;
    }

    private void paintClownfish() {
        double cx = 58 + 4.0 * Math.sin(timeClock * 0.8);
        double cy = groundY[58] - 7.0 + 1.6 * Math.sin(timeClock * 1.3 + 0.6);
        int heading = Math.cos(timeClock * 0.8) >= 0 ? 1 : -1;
        drawSprite(cx, cy, heading, 7.5, 4.5, this::clownPixel);
    }

    private int[] clownPixel(double u, double v) {
        if (u > 4.2 || u < -6.6) return null;
        double wag = Math.sin(timeClock * 11.0);
        if (u < -3.4) v -= wag * 0.7 * Math.min(1.0, (-3.4 - u) / 2.5);

        double hh;
        if (u >= -3.4) {
            double e = u / 4.2;
            hh = 2.1 * Math.sqrt(Math.max(0.0, 1.0 - e * e));
        } else {
            hh = 1.15 + (-3.4 - u) * 0.55; // flared tail
        }
        if (Math.abs(v) > hh) {
            if (v < 0 && -v <= hh + 1.0 && u > -2.0 && u < 1.6) return new int[] { 235, 80, 20, 90 }; // dorsal fin
            return null;
        }
        double ex = u - 2.8, ey = v + 0.6;
        if (ex * ex + ey * ey < 0.5) return new int[] { 20, 20, 30, 100 };                                 // eye
        if ((u >= 0.9 && u <= 1.9) || (u >= -2.2 && u <= -1.3)) return new int[] { 252, 248, 240, 100 };   // white bands
        int[] c = mix3(new int[] { 255, 118, 28 }, new int[] { 255, 175, 60 }, Math.max(0.0, Math.min(1.0, (v + hh) / (2 * hh))));
        return new int[] { c[0], c[1], c[2], u < -3.4 ? 88 : 100 };
    }

    // Tetra sprite, head on the left. B = neon stripe, S = silver, R = red, E = eye, t = translucent tail
    private static final String[] TETRA = { ".BBBBt", "SESRR.", ".SSRRt" };

    private void paintSchool() {
        // A loose, cohesive cloud drifting right -> left, weaving through itself
        double centerX = WIDTH + 18 - ((timeClock * 9.0 + 34.0) % (WIDTH + 40));
        double centerRow = 7.0 + 1.6 * Math.sin(timeClock * 0.5);
        int heading = -1;
        for (int i = 0; i < 12; i++) {
            double ox = ((hash(i, 3) % 1000) / 1000.0 - 0.5) * 30.0;
            double oy = ((hash(i, 4) % 1000) / 1000.0 - 0.5) * 10.0;
            double fx = centerX + ox + 3.5 * Math.sin(timeClock * 0.9 + i * 1.7);
            double fy = 2.0 * (centerRow + 0.9 * Math.sin(timeClock * 1.5 + i * 0.4)) + oy + 1.3 * Math.sin(timeClock * 1.1 + i * 2.3);
            int hx = (int) Math.round(fx);
            int hy = (int) Math.round(fy);
            double glint = 0.5 + 0.5 * Math.sin(timeClock * 10.0 + i * 2.0);
            int stripe = mixRGB(pack(30, 110, 255), pack(120, 235, 255), glint); // electric blue stripe with a flicker
            for (int r = 0; r < TETRA.length; r++) {
                for (int c = 0; c < TETRA[r].length(); c++) {
                    char ch = TETRA[r].charAt(c);
                    int px = hx - heading * c; // head first, tail trailing behind
                    switch (ch) {
                        case 'B': plot(px, hy + r, stripe, 1.0); break;
                        case 'S': plot(px, hy + r, pack(235, 240, 250), 1.0); break;
                        case 'R': plot(px, hy + r, pack(245, 45, 60), 1.0); break;
                        case 'E': plot(px, hy + r, pack(10, 20, 50), 1.0); break;
                        case 't': plot(px, hy + r, r == 0 ? pack(140, 190, 255) : pack(255, 130, 140), 0.55); break;
                        default: break;
                    }
                }
            }
        }
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    private double segDist(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        segT = Math.max(0.0, Math.min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
        return Math.hypot(px - (ax + segT * dx), py - (ay + segT * dy));
    }

    private void plot(int x, int y, int rgb, double alpha) {
        if (x < 0 || x >= PW || y < 0 || y >= PH) return;
        int i = x + PW * y;
        frame[i] = alpha >= 0.999 ? rgb : mixRGB(frame[i], rgb, alpha);
    }

    private static int clamp(double v) {
        return v < 0 ? 0 : v > 255 ? 255 : (int) v;
    }

    private static int pack(int r, int g, int b) {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int pack(int[] c) {
        return pack(c[0], c[1], c[2]);
    }

    private static int mixRGB(int a, int b, double t) {
        return pack(
            clamp(((a >> 16) & 255) * (1.0 - t) + ((b >> 16) & 255) * t),
            clamp(((a >> 8) & 255) * (1.0 - t) + ((b >> 8) & 255) * t),
            clamp((a & 255) * (1.0 - t) + (b & 255) * t));
    }

    private static int[] mix3(int[] a, int[] b, double t) {
        return new int[] { clamp(a[0] * (1 - t) + b[0] * t), clamp(a[1] * (1 - t) + b[1] * t), clamp(a[2] * (1 - t) + b[2] * t) };
    }

    private static int scaleRGB(int c, double k) {
        return pack(clamp(((c >> 16) & 255) * k), clamp(((c >> 8) & 255) * k), clamp((c & 255) * k));
    }

    private static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }

    /** One terminal cell: glyph in the foreground colour on a background colour. */
    private String cell(char glyph, int fg, int bg) {
        return new StringBuilder(48)
            .append("\u001B[38;2;").append((fg >> 16) & 255).append(';').append((fg >> 8) & 255).append(';').append(fg & 255)
            .append(";48;2;").append((bg >> 16) & 255).append(';').append((bg >> 8) & 255).append(';').append(bg & 255)
            .append('m').append(glyph).append(RESET).toString();
    }
}