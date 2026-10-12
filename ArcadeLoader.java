public class ArcadeLoader extends Loader {
    private static final StatusStage[] ARCADE_STAGES = {
        new StatusStage(25, "Feeding quarters into the slot:"),
        new StatusStage(50, "Booting the attract-mode demos:"),
        new StatusStage(75, "Warming up the neon tubes:"),
        new StatusStage(100, "Insert Coin!")
    };

    // The scene is painted on a "pixel" grid twice as tall as the terminal: every cell holds two
    // stacked pixels (top half / bottom half), drawn with a half-block glyph.
    private static final int WIDTH  = 80;
    private static final int HEIGHT = 22;
    private static final int PW     = WIDTH;
    private static final int PH     = HEIGHT * 2;
    private static final double Z_SCENE = 0.50;

    private static final int CEIL_H     = 4;
    private static final int HORIZON_Y  = 24;
    private static final int VANISH_X   = 40;

    private double timeClock = 0.0;
    private static final String RESET = "\u001B[0m";

    private static final int[] NEON_PALETTE = {
        rgb(255, 45, 145), rgb(50, 225, 255), rgb(175, 70, 255),
        rgb(255, 225, 50), rgb(60, 255, 150), rgb(255, 140, 40)
    };

    private static final int[] TUBE_X  = { 14, 30, 50, 66 };
    private static final int[] CAB_XS  = { 8, 23, 57, 72 };

    // A tiny 5x7 block font, just enough letters to spell the marquee.
    private static final String[][] FONT = {
        { "01110", "10001", "10001", "11111", "10001", "10001", "10001" }, // A
        { "11110", "10001", "10001", "11110", "10100", "10010", "10001" }, // R
        { "01111", "10000", "10000", "10000", "10000", "10000", "01111" }, // C
        { "11110", "10001", "10001", "10001", "10001", "10001", "11110" }, // D
        { "11111", "10000", "10000", "11110", "10000", "10000", "11111" }  // E
    };
    private static final String SIGN_TEXT = "ARCADE";

    private final int[] px = new int[PW * PH];

    public ArcadeLoader() {
        // This uses 80x22 specifically
        super(ARCADE_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04;

        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                px[y * PW + x] = scenePixel(x, y);
            }
        }

        paintCeilingTubes();
        paintSignGlow();
        paintSign();
        paintCabinets();
        paintFloatingSparks();
        paintPinball();
        paintClawMachine();

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
    //  BACKDROP: ceiling, back wall, and the receding synthwave floor grid
    // =====================================================================

    private int scenePixel(int x, int y) {
        if (y < CEIL_H) return rgb(8, 6, 14);
        if (y < HORIZON_Y) return wallPixel(x, y);
        return floorPixel(x, y);
    }

    private int wallPixel(int x, int y) {
        double t = clamp01((y - CEIL_H) / (double) (HORIZON_Y - CEIL_H));
        int c = mix(rgb(26, 10, 40), rgb(14, 6, 24), t);
        if (hash(x / 4, 50) % 7 == 0) c = mix(c, rgb(120, 40, 160), 0.15);
        return c;
    }

    private int floorPixel(int x, int y) {
        double d = clamp01((y - HORIZON_Y) / (double) (PH - HORIZON_Y));
        double dd = Math.max(d, 0.05);
        double worldX = (x - VANISH_X) / dd;
        double colF = worldX / 6.0;
        double rowF = (1.0 / dd) * 1.3;
        double colFrac = Math.abs(colF - Math.round(colF));
        double rowFrac = Math.abs(rowF - Math.round(rowF));
        double lineW = 0.06 + 0.10 * (1.0 - d);
        boolean onLine = colFrac < lineW || rowFrac < lineW;

        int floorBase = mix(rgb(10, 6, 22), rgb(18, 8, 26), d);
        if (onLine) {
            double wave = 0.5 + 0.5 * Math.sin(timeClock * 2.0 - d * 6.0);
            int lineColor = mix(rgb(255, 40, 170), rgb(60, 220, 255), 0.5 + 0.5 * Math.sin(timeClock * 0.6));
            return mix(floorBase, lineColor, 0.55 + 0.35 * wave);
        }
        return floorBase;
    }

    private void paintCeilingTubes() {
        for (int i = 0; i < TUBE_X.length; i++) {
            int color = NEON_PALETTE[i % 2 == 0 ? 0 : 1];
            double flicker = 0.85 + 0.15 * Math.sin(timeClock * 5.0 + i * 2.0);
            int c = shade(color, flicker);
            for (int dx = -3; dx <= 3; dx++) plot(TUBE_X[i] + dx, 1, c);
            for (int y = 2; y <= 6; y++) {
                double a = Math.exp(-(y - 1) * (y - 1) / 8.0) * 0.3;
                for (int dx = -4; dx <= 4; dx++) blend(TUBE_X[i] + dx, y, color, a);
            }
        }
    }

    // =====================================================================
    //  THE MARQUEE: a chasing-neon "ARCADE" sign with a soft glow halo
    // =====================================================================

    private int neonCycle(int offset) {
        double phase = timeClock * 1.2 + offset * 0.8;
        double t = 0.5 + 0.5 * Math.sin(phase);
        int a = NEON_PALETTE[offset % NEON_PALETTE.length];
        int b = NEON_PALETTE[(offset + 1) % NEON_PALETTE.length];
        return mix(a, b, t);
    }

    private void paintSignGlow() {
        int cx = VANISH_X, cy = 8;
        double pulse = 0.6 + 0.4 * Math.sin(timeClock * 2.0);
        int glowColor = neonCycle(2);
        for (int y = 2; y <= 16; y++) {
            for (int x = cx - 22; x <= cx + 22; x++) {
                double d = Math.hypot((x - cx) / 1.6, y - cy);
                double g = Math.exp(-d * d / 90.0) * pulse;
                if (g > 0.02) blend(x, y, glowColor, clamp01(g * 0.35));
            }
        }
    }

    private void paintSign() {
        int totalW = SIGN_TEXT.length() * 6 - 1;
        int startX = VANISH_X - totalW / 2;
        int startY = 5;
        for (int li = 0; li < SIGN_TEXT.length(); li++) {
            String[] glyph = FONT[fontIndex(SIGN_TEXT.charAt(li))];
            int color = neonCycle(li);
            for (int row = 0; row < glyph.length; row++) {
                String bits = glyph[row];
                for (int col = 0; col < bits.length(); col++) {
                    if (bits.charAt(col) != '1') continue;
                    int px_ = startX + li * 6 + col, py_ = startY + row;
                    blend(px_ - 1, py_, color, 0.3);
                    blend(px_ + 1, py_, color, 0.3);
                    blend(px_, py_ - 1, color, 0.3);
                    blend(px_, py_ + 1, color, 0.3);
                    plot(px_, py_, color);
                }
            }
        }
    }

    private static int fontIndex(char c) {
        switch (c) {
            case 'A': return 0;
            case 'R': return 1;
            case 'C': return 2;
            case 'D': return 3;
            case 'E': return 4;
            default:  return 0;
        }
    }

    // =====================================================================
    //  THE CABINET ROW: four machines against the back wall, screens looping
    // =====================================================================

    private void paintCabinets() {
        for (int i = 0; i < CAB_XS.length; i++) cabinet(CAB_XS[i], HORIZON_Y, i);
    }

    private void cabinet(int cx, int baseY, int idx) {
        int topY = baseY - 12;
        int halfW = 4;
        int body = rgb(26, 22, 32), bodyLit = rgb(36, 30, 42);

        for (int y = topY; y < baseY; y++) {
            for (int dx = -halfW; dx <= halfW; dx++) plot(cx + dx, y, ((dx + y) % 5 == 0) ? bodyLit : body);
        }

        int marqueeColor = NEON_PALETTE[idx % NEON_PALETTE.length];
        double mFlicker = 0.9 + 0.1 * Math.sin(timeClock * 3.0 + idx);
        for (int y = topY; y <= topY + 1; y++) {
            for (int dx = -halfW; dx <= halfW; dx++) plot(cx + dx, y, shade(marqueeColor, mFlicker));
        }
        for (int dx = -halfW - 1; dx <= halfW + 1; dx++) blend(cx + dx, topY + 2, marqueeColor, 0.3);

        int scrY0 = topY + 2, scrY1 = topY + 7;
        int colorA = NEON_PALETTE[idx % NEON_PALETTE.length];
        int colorB = NEON_PALETTE[(idx + 2) % NEON_PALETTE.length];
        for (int y = scrY0; y <= scrY1; y++) {
            for (int dx = -halfW + 1; dx <= halfW - 1; dx++) {
                int bx = cx + dx;
                double p = noise2(bx * 0.6 + idx * 5, y * 0.6 - timeClock * 2.5, 60 + idx);
                plot(bx, y, shade(mix(colorA, colorB, p), 0.85));
            }
        }

        int panelY = scrY1 + 1;
        for (int dx = -halfW + 1; dx <= halfW - 1; dx++) plot(cx + dx, panelY, rgb(18, 16, 22));
        plot(cx - 2, panelY, rgb(220, 220, 220));
        plot(cx - 2, panelY - 1, rgb(230, 50, 50));
        plot(cx + 1, panelY, mix(rgb(255, 255, 255), marqueeColor, 0.6));
        plot(cx + 2, panelY, mix(rgb(255, 255, 255), NEON_PALETTE[(idx + 3) % NEON_PALETTE.length], 0.6));

        int glowColor = NEON_PALETTE[(idx + 1) % NEON_PALETTE.length];
        double midY = (scrY0 + scrY1) / 2.0;
        for (int y = scrY0 - 2; y <= scrY1 + 2; y++) {
            for (int dx = -halfW - 3; dx <= halfW + 3; dx++) {
                double d = Math.hypot(dx, y - midY);
                if (d > halfW) blend(cx + dx, y, glowColor, Math.max(0.0, 0.18 - d * 0.012));
            }
        }
    }

    /** A few drifting neon sparks for atmosphere, blinking as they cross the upper room. */
    private void paintFloatingSparks() {
        for (int i = 0; i < 4; i++) {
            double speed = 4.0 + i * 0.7;
            double sx = ((timeClock * speed + i * 23.0) % (PW + 20.0)) - 10.0;
            double sy = 16 + (i % 2) * 4 + 2.0 * Math.sin(timeClock * 0.8 + i);
            int c = NEON_PALETTE[i % NEON_PALETTE.length];
            int ix = (int) sx, iy = (int) sy;
            plot(ix, iy, c);
            blend(ix - 1, iy, c, 0.5);
            blend(ix + 1, iy, c, 0.5);
            blend(ix, iy - 1, c, 0.5);
            blend(ix, iy + 1, c, 0.5);
        }
    }

    // =====================================================================
    //  FOREGROUND: a flashing pinball machine and a claw crane, up close
    // =====================================================================

    private void paintPinball() {
        int cx = 13;
        int body = rgb(20, 18, 26);

        for (int y = 20; y <= 30; y++) {
            for (int dx = -6; dx <= 6; dx++) plot(cx + dx, y, ((dx + y) % 6 == 0) ? shade(body, 1.25) : body);
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 5; c++) {
                int lx = cx - 4 + c * 2, ly = 22 + r * 3;
                double phase = rand01(r * 5 + c, 900) * 6.2831;
                double on = 0.5 + 0.5 * Math.sin(timeClock * 4.0 + phase);
                int col = NEON_PALETTE[(r * 5 + c) % NEON_PALETTE.length];
                plot(lx, ly, on > 0.4 ? col : shade(col, 0.25));
            }
        }

        for (int y = 31; y <= 40; y++) {
            int w = Math.max(1, 6 - (y - 31) / 3);
            for (int dx = -w; dx <= w; dx++) plot(cx + dx, y, rgb(14, 40, 22));
        }
        plot(cx - 2, 34, shade(NEON_PALETTE[0], 0.7 + 0.3 * Math.sin(timeClock * 6.0)));
        plot(cx + 2, 34, shade(NEON_PALETTE[1], 0.7 + 0.3 * Math.sin(timeClock * 6.0 + 1.0)));
        plot(cx, 37, shade(NEON_PALETTE[2], 0.7 + 0.3 * Math.sin(timeClock * 6.0 + 2.0)));

        plot(cx - 5, 41, body); plot(cx - 5, 42, body);
        plot(cx + 5, 41, body); plot(cx + 5, 42, body);
    }

    private void paintClawMachine() {
        int cx = 67;
        int frame = rgb(34, 28, 20);

        for (int y = 22; y <= 40; y++) {
            for (int dx = -7; dx <= 7; dx++) {
                boolean border = dx == -7 || dx == 7 || y == 22;
                plot(cx + dx, y, border ? shade(frame, 1.3) : mix(rgb(10, 14, 22), rgb(18, 10, 26), (y - 22) / 18.0));
            }
        }
        for (int i = 0; i < 10; i++) {
            int px_ = cx - 6 + (int) (rand01(i, 700) * 13);
            int py_ = 36 + (int) (rand01(i, 701) * 4);
            plot(px_, py_, NEON_PALETTE[i % NEON_PALETTE.length]);
        }

        double cyc = (timeClock * 0.3) % 4.0;
        double t = cyc < 2.0 ? cyc / 2.0 : (4.0 - cyc) / 2.0;
        int clawY = 24 + (int) Math.round(t * 10);
        for (int y = 23; y < clawY; y++) plot(cx, y, rgb(160, 160, 160));
        plot(cx - 1, clawY, rgb(220, 220, 220));
        plot(cx + 1, clawY, rgb(220, 220, 220));
        plot(cx, clawY, rgb(235, 235, 235));

        for (int dx = -7; dx <= 7; dx++) plot(cx + dx, 41, frame);
        plot(cx - 3, 41, rgb(220, 60, 60));
        plot(cx + 3, 41, mix(rgb(255, 255, 255), NEON_PALETTE[4], 0.6));
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