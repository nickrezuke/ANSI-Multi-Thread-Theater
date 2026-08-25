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
    
    // Adjusted canonical center for perspective layout
    private static final int CENTER_X = 36;
    private static final int CENTER_Y = 11;

    // Canonical PS1 logo palette
    private static final int[] RGB_RED = { 222, 30, 42 };
    private static final int[] RGB_YELLOW = { 247, 200, 20 };
    private static final int[] RGB_TEAL = { 20, 150, 140 };
    private static final int[] RGB_BLUE = { 55, 100, 182 };

    private static class PixelNode {
        int x, y;
        int[] baseColor;
        double depth; // Used to prevent rendering artifacts or handle overlaps

        PixelNode(int x, int y, int[] baseColor, double depth) {
            this.x = x;
            this.y = y;
            this.baseColor = baseColor;
            this.depth = depth;
        }
    }

    private final List<PixelNode> pVectorGroup = new ArrayList<>();
    private final List<PixelNode> sVectorGroup = new ArrayList<>();

    public PlayStation1IntroLoader() {
        super(PS1_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        pVectorGroup.clear();
        sVectorGroup.clear();

        char[][] grid = new char[height][width];
        for (char[] row : grid) Arrays.fill(row, ' ');

        // -------------------------------------------------------------
        // 1. THE VERTICAL "P" (Stands perfectly upright)
        // -------------------------------------------------------------
        int pLeft = CENTER_X - 6;
        int pWidth = 4;
        
        // Vertical spine
        for (int y = CENTER_Y - 8; y <= CENTER_Y + 5; y++) {
            for (int x = pLeft; x < pLeft + pWidth; x++) {
                if (x >= 0 && x < width && y >= 0 && y < height) grid[y][x] = 'P';
            }
        }
        
        // P's Rounded Upper Bowl Loop
        int bowlRadiusOuterX = 9;
        int bowlRadiusOuterY = 5;
        int bowlRadiusInnerX = 5;
        int bowlRadiusInnerY = 2;
        int bowlCenterX = pLeft + pWidth;
        int bowlCenterY = CENTER_Y - 4;

        for (int y = CENTER_Y - 9; y <= CENTER_Y + 1; y++) {
            for (int x = pLeft; x <= pLeft + bowlRadiusOuterX + 2; x++) {
                double dx = x - bowlCenterX;
                double dy = y - bowlCenterY;
                
                // Normalized ellipse math
                double normOuter = (dx*dx) / (bowlRadiusOuterX*bowlRadiusOuterX) + (dy*dy) / (bowlRadiusOuterY*bowlRadiusOuterY);
                double normInner = (dx*dx) / (bowlRadiusInnerX*bowlRadiusInnerX) + (dy*dy) / (bowlRadiusInnerY*bowlRadiusInnerY);
                
                if (normOuter <= 1.0 && (dx < 0 || normInner >= 1.0)) {
                    if (x >= pLeft && x < width && y >= 0 && y < height) {
                        grid[y][x] = 'P';
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. THE FLOATING HORIZONTAL "S" (Sheared Perspective Plane)
        // -------------------------------------------------------------
        // We model the S flat on the ground using standard parametric formulas,
        // then shear/compress it horizontally so it mimics the 3D floor plane.
        for (int sy = -6; sy <= 8; sy++) {
            for (int sx = -22; sx <= 22; sx++) {
                
                // Transform target coordinate: map 2D layout grid space 
                // into perspective space lying behind/underneath the P
                int renderX = CENTER_X + sx + (int)(sy * 1.5); 
                int renderY = CENTER_Y + 4 + (int)(sy * 0.45);

                if (renderX < 0 || renderX >= width || renderY < 0 || renderY >= height) continue;

                // Trace the logic flow of the original 'S' ribbon shape
                boolean isS = false;
                int[] bandColor = RGB_YELLOW;

                // Top arc of the S
                if (sy <= 0) {
                    double r = Math.sqrt(sx*sx + (sy+3)*(sy+3)*4.0);
                    if (r >= 7.0 && r <= 13.0 && sx <= 2) {
                        isS = true;
                        bandColor = (sx < -4) ? RGB_YELLOW : RGB_TEAL;
                    }
                }
                // Bottom arc of the S
                if (sy >= -1) {
                    double r = Math.sqrt(sx*sx + (sy-4)*(sy-4)*4.0);
                    if (r >= 7.0 && r <= 13.0 && sx >= -2) {
                        isS = true;
                        bandColor = (sx > 5) ? RGB_BLUE : RGB_TEAL;
                    }
                }
                // Center transition stroke joining top & bottom arcs
                if (sy >= -2 && sy <= 3 && sx >= -6 && sx <= 6) {
                    if (sx + sy * 1.2 >= -3 && sx + sy * 1.2 <= 3) {
                        isS = true;
                        bandColor = RGB_TEAL;
                    }
                }

                // Inject nodes safely into group (S stays behind P vertically)
                if (isS && grid[renderY][renderX] != 'P') {
                    sVectorGroup.add(new PixelNode(renderX, renderY, bandColor, 0.5));
                }
            }
        }

        // Convert the structural "P" grid layout into nodes
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x] == 'P') {
                    pVectorGroup.add(new PixelNode(x, y, RGB_RED, 0.9));
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, 0.0);

        timeClock += 0.015;
        double progress = Math.min(1.0, timeClock * 0.75);
        double easeFactor = 1.0 - Math.pow(1.0 - progress, 3); // Cubic Ease Out

        // -------------------------------------------------------------
        // PASS 1: THE HORIZONTAL "S" SLIDES IN FROM THE LEFT
        // -------------------------------------------------------------
        int sOffsetX = (int) (-35.0 * (1.0 - easeFactor));
        for (PixelNode node : sVectorGroup) {
            int currentX = node.x + sOffsetX;
            int currentY = node.y;

            if (currentX < 0 || currentX >= width || currentY < 0 || currentY >= height) continue;

            int index = currentX + width * currentY;
            if (node.depth > zBuffer[index]) {
                zBuffer[index] = node.depth;
                int[] rgb = applyLightGlint(node.baseColor, currentX, currentY);
                outputBuffer[index] = ansi(rgb) + "█" + RESET;
            }
        }

        // -------------------------------------------------------------
        // PASS 2: THE UPRIGHT RED "P" DROPS DOWN FROM THE TOP
        // -------------------------------------------------------------
        int pOffsetY = (int) (-20.0 * (1.0 - easeFactor));
        for (PixelNode node : pVectorGroup) {
            int currentX = node.x;
            int currentY = node.y + pOffsetY;

            if (currentX < 0 || currentX >= width || currentY < 0 || currentY >= height) continue;

            int index = currentX + width * currentY;
            if (node.depth > zBuffer[index]) {
                zBuffer[index] = node.depth;
                int[] rgb = applyLightGlint(node.baseColor, currentX, currentY);
                outputBuffer[index] = ansi(rgb) + "█" + RESET;
            }
        }

        // Standard auto-loop reset sequence
        if (timeClock > 3.0) {
            initialize();
        }
    }

    // Authentic flat colors with subtle light tint sweep over the vector geometry
    private int[] applyLightGlint(int[] baseColor, int x, int y) {
        double factor = 1.0;
        if (timeClock > 1.2) {
            double scanline = ((timeClock - 1.2) * 65.0) % (width + height + 30) - 15;
            double distance = Math.abs((x + y) - scanline);
            if (distance < 5.0) {
                factor += (5.0 - distance) / 5.0 * 0.28; // Subtle bright shine factor
            }
        }
        return new int[]{
            Math.min(255, (int)(baseColor[0] * factor)),
            Math.min(255, (int)(baseColor[1] * factor)),
            Math.min(255, (int)(baseColor[2] * factor))
        };
    }

    private static String ansi(int[] rgb) {
        return String.format("\u001B[38;2;%d;%d;%dm", rgb[0], rgb[1], rgb[2]);
    }
}
