// TODO: Make this better this is terrible right now

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayStation1IntroLoader extends Loader {
    private static final StatusStage[] PS1_STAGES = {
        new StatusStage(20, "Parsing red P vector geometry:"),
        new StatusStage(50, "Splicing tri-color ribbon bands:"),
        new StatusStage(80, "Compositing PlayStation wordmark:"),
        new StatusStage(100, "PlayStation Boot Sequence Complete!")
    };

    private double timeClock = 0.0;
    private final int width = 80;
    private final int height = 22;
    private static final int CENTER_X = 38;
    private static final int CENTER_Y = 10;

    // Canonical PS1 logo palette: a solid red "P" plus a yellow / teal /
    // blue ribbon that folds into the "S" - not a grey/chrome render.
    private static final int[] RGB_RED    = { 222, 30, 42 };
    private static final int[] RGB_YELLOW = { 247, 200, 20 };
    private static final int[] RGB_TEAL   = { 20, 150, 140 };
    private static final int[] RGB_BLUE   = { 55, 100, 182 };

    // A node carries its rest position, which color band it belongs to,
    // and a bevel shade (0..1) computed once so lighting stays fixed to
    // the shape as it slides, instead of to the screen.
    private static class PixelNode {
        int x, y;
        int[] baseColor;
        double shade;

        PixelNode(int x, int y, int[] baseColor, double shade) {
            this.x = x;
            this.y = y;
            this.baseColor = baseColor;
            this.shade = shade;
        }
    }

    private final List<PixelNode> pVectorGroup = new ArrayList<>();
    private final List<PixelNode> sVectorGroup = new ArrayList<>();

    public PlayStation1IntroLoader() {
        super(PS1_STAGES, 80, 22);
    }

    private static double norm360(double a) {
        while (a < 0) a += 360;
        while (a >= 360) a -= 360;
        return a;
    }

    private static boolean inAngleGap(double theta, double gapStart, double gapEnd) {
        theta = norm360(theta);
        gapStart = norm360(gapStart);
        gapEnd = norm360(gapEnd);
        if (gapStart <= gapEnd) {
            return theta >= gapStart && theta <= gapEnd;
        }
        return theta >= gapStart || theta <= gapEnd;
    }

    // Simulated top-left light source, subtle - just enough to give each
    // flat color band a touch of gloss without washing out its hue.
    private static double bevelShade(int x, int y) {
        double diag = (-(x - CENTER_X) - (y - CENTER_Y));
        double shade = 0.5 + diag / 55.0;
        shade += 0.04 * Math.sin(x * 0.7 + y * 0.4);
        return Math.max(0.0, Math.min(1.0, shade));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static int[] scaleColor(int[] c, double factor) {
        return new int[]{
            clamp255((int) (c[0] * factor)),
            clamp255((int) (c[1] * factor)),
            clamp255((int) (c[2] * factor))
        };
    }

    private static int[] lerpColor(int[] a, int[] b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return new int[]{
            (int) (a[0] + (b[0] - a[0]) * t),
            (int) (a[1] + (b[1] - a[1]) * t),
            (int) (a[2] + (b[2] - a[2]) * t)
        };
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        pVectorGroup.clear();
        sVectorGroup.clear();

        // Build on a scratch grid so a quick neighbor-cleanup pass can
        // strip stray single-cell fragments left by the hook math.
        char[][] grid = new char[height][width];
        for (char[] row : grid) Arrays.fill(row, ' ');

        // -------------------------------------------------------------
        // THE "P" — solid spine + rounded bowl with an open counter
        // -------------------------------------------------------------
        for (int y = CENTER_Y - 9; y < CENTER_Y + 9; y++) {
            for (int x = CENTER_X - 16; x < CENTER_X - 12; x++) {
                if (x >= 0 && x < width && y >= 0 && y < height) grid[y][x] = 'P';
            }
        }
        int loopCx = CENTER_X - 12;
        int loopCy = CENTER_Y - 4;
        for (int y = CENTER_Y - 9; y < CENTER_Y + 1; y++) {
            for (int x = CENTER_X - 13; x < CENTER_X; x++) {
                double dx = x - loopCx;
                double dy = y - loopCy;
                double val = dx * dx * 0.42 + dy * dy;
                if (val >= 2.0 && val <= 15.5 && x >= CENTER_X - 13) {
                    if (x >= 0 && x < width && y >= 0 && y < height) grid[y][x] = 'P';
                }
            }
        }

        // -------------------------------------------------------------
        // THE RIBBON — flattened yellow hook / teal diagonal / blue hook
        // that folds into an "S", laid across the P's lower half
        // -------------------------------------------------------------
        double ribbonCy = CENTER_Y + 4;
        double xScale = 3.4;
        double yScale = 1.6;
        double outerR = 3.4, innerR = 1.7;
        double leftCx = CENTER_X - 11;
        double rightCx = CENTER_X + 13;

        for (int y = CENTER_Y; y < CENTER_Y + 9; y++) {
            for (int x = CENTER_X - 24; x < CENTER_X + 24; x++) {
                char band = 0;

                double dxL = (x - leftCx) / xScale;
                double dyL = (y - ribbonCy) * yScale / 2.0;
                double rL = Math.sqrt(dxL * dxL + dyL * dyL);
                double thL = Math.toDegrees(Math.atan2(dyL, dxL));
                if (rL >= innerR && rL <= outerR && !inAngleGap(thL, -70, 70)) {
                    band = 'Y';
                }

                double dxR = (x - rightCx) / xScale;
                double dyR = (y - ribbonCy) * yScale / 2.0;
                double rR = Math.sqrt(dxR * dxR + dyR * dyR);
                double thR = Math.toDegrees(Math.atan2(dyR, dxR));
                if (rR >= innerR && rR <= outerR && !inAngleGap(thR, 110, 250)) {
                    band = 'B';
                }

                if (y >= ribbonCy - 1 && y <= ribbonCy + 1) {
                    double pct = (y - (ribbonCy - 1)) / 2.0;
                    int targetX = (CENTER_X + 1) - (int) (pct * 6);
                    if (x >= targetX - 10 && x <= targetX + 10) {
                        band = 'T';
                    }
                }

                if (band != 0 && x >= 0 && x < width && y >= 0 && y < height) {
                    // Ribbon only shows where the P hasn't already claimed the cell.
                    if (grid[y][x] != 'P') grid[y][x] = band;
                }
            }
        }

        // Erode fragments that aren't part of a connected stroke.
        for (int pass = 0; pass < 2; pass++) {
            char[][] next = new char[height][width];
            for (int y = 0; y < height; y++) next[y] = grid[y].clone();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    char ch = grid[y][x];
                    if (ch == ' ') continue;
                    int neighborCount = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[ny][nx] == ch) {
                                neighborCount++;
                            }
                        }
                    }
                    if (neighborCount < 2) next[y][x] = ' ';
                }
            }
            grid = next;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char ch = grid[y][x];
                double shade = bevelShade(x, y);
                if (ch == 'P') {
                    pVectorGroup.add(new PixelNode(x, y, RGB_RED, shade));
                } else if (ch == 'Y') {
                    sVectorGroup.add(new PixelNode(x, y, RGB_YELLOW, shade));
                } else if (ch == 'T') {
                    sVectorGroup.add(new PixelNode(x, y, RGB_TEAL, shade));
                } else if (ch == 'B') {
                    sVectorGroup.add(new PixelNode(x, y, RGB_BLUE, shade));
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, 0.0);

        timeClock += 0.014;

        double progress = Math.min(1.0, timeClock * 0.85);
        double easeFactor = 1.0 - Math.pow(1.0 - progress, 3);

        // -------------------------------------------------------------
        // PASS 1: THE RED "P" DROPS STRAIGHT DOWN INTO PLACE
        // -------------------------------------------------------------
        int pOffsetY = (int) (-22.0 * (1.0 - easeFactor));

        for (PixelNode node : pVectorGroup) {
            int currentX = node.x;
            int currentY = node.y + pOffsetY;
            if (currentX < 0 || currentX >= width || currentY < 0 || currentY >= height) continue;

            int index = currentX + width * currentY;
            zBuffer[index] = 0.90;
            int[] rgb = shadeToColor(node, currentX, currentY, progress);
            outputBuffer[index] = ansi(rgb) + "█" + RESET;
        }

        // -------------------------------------------------------------
        // PASS 2: THE RIBBON GLIDES IN FROM THE LOWER-RIGHT
        // -------------------------------------------------------------
        int sOffsetX = (int) (26.0 * (1.0 - easeFactor));
        int sOffsetY = (int) (10.0 * (1.0 - easeFactor));

        for (PixelNode node : sVectorGroup) {
            int currentX = node.x + sOffsetX;
            int currentY = node.y + sOffsetY;
            if (currentX < 0 || currentX >= width || currentY < 0 || currentY >= height) continue;

            int index = currentX + width * currentY;
            if (zBuffer[index] < 0.85) {
                zBuffer[index] = 0.50;
                int[] rgb = shadeToColor(node, currentX, currentY, progress);
                outputBuffer[index] = ansi(rgb) + "█" + RESET;
            }
        }

        

        if (timeClock > 2.6) {
            initialize();
        }
    }

    // Blends a node's true hue between a darkened and lightened variant
    // of itself (never toward grey) and adds a soft moving glint once
    // assembly has mostly settled.
    private int[] shadeToColor(PixelNode node, int currentX, int currentY, double progress) {
        double shade = node.shade;

        if (progress > 0.6) {
            double diagPos = currentX + currentY;
            double sweepCenter = (timeClock * 55.0) % (width + height + 20) - 10;
            double dist = Math.abs(diagPos - sweepCenter);
            if (dist < 4.0) {
                shade = Math.min(1.0, shade + (4.0 - dist) / 4.0 * 0.35);
            }
        }

        int[] darker = scaleColor(node.baseColor, 0.55);
        int[] lighter = scaleColor(node.baseColor, 1.35);
        return lerpColor(darker, lighter, shade);
    }

    private static String ansi(int[] rgb) {
        return String.format("\u001B[38;2;%d;%d;%dm", rgb[0], rgb[1], rgb[2]);
    }
}