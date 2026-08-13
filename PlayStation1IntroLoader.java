// TODO: Make this better this is terrible right now

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayStation1IntroLoader extends Loader {
    private static final StatusStage[] PS1_STAGES = {
        new StatusStage(20, "Parsing pristine 2D pixel-art blueprints:"),
        new StatusStage(50, "Splicing horizontal tri-band ribbon arrays:"),
        new StatusStage(75, "Calibrating independent linear sliding tracks:"),
        new StatusStage(100, "PlayStation Boot Sequence Complete!")
    };

    private double timeClock = 0.0;
    private final int width = 80;
    private final int height = 22;

    // Canonical PS1 BIOS Palette Registers
    private static final int[] RGB_P_RED     = { 235, 30, 30 };   // Stark Primary Red
    private static final int[] RGB_S_YELLOW  = { 245, 190, 15 };  // Top Band Yellow
    private static final int[] RGB_S_GREEN   = { 25, 165, 70 };   // Mid Band Green
    private static final int[] RGB_S_BLUE    = { 20, 90, 200 };   // Bottom Band Blue

    // Simple primitive node carrying raw screen-space design layouts
    private static class PixelNode {
        int x, y; // Final target destination coordinates on screen
        int[] rgb;

        PixelNode(int x, int y, int[] rgb) {
            this.x = x;
            this.y = y;
            this.rgb = rgb;
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

        // Screen-space center anchors for the final assembled logo alignment
        int centerX = 38;
        int centerY = 10;

        // -------------------------------------------------------------
        // BLUEPRINT 1: THE UPRIGHT RED "P" (Meticulously Hand-Plotted)
        // -------------------------------------------------------------
        // 1. The Solid Left Spine Post
        for (int y = centerY - 7; y <= centerY + 6; y++) {
            for (int x = centerX - 5; x <= centerX - 2; x++) {
                pVectorGroup.add(new PixelNode(x, y, RGB_P_RED));
            }
        }
        // 2. The Upper Right Hook Loop Canopy (Stands explicitly atop the spine)
        for (int y = centerY - 7; y <= centerY; y++) {
            for (int x = centerX - 1; x <= centerX + 7; x++) {
                // Calculate distance to create a smooth, clean rounded loop opening inner window
                double dx = x - (centerX - 1);
                double dy = y - (centerY - 3.5);
                double radiusSq = (dx * dx * 0.45) + (dy * dy); // Compensate font ratio aspect tracking

                if (radiusSq >= 2.0 && radiusSq <= 14.5 && x >= centerX - 1) {
                    pVectorGroup.add(new PixelNode(x, y, RGB_P_RED));
                }
            }
        }

        // -------------------------------------------------------------
        // BLUEPRINT 2: THE FLAT HORIZONTAL GRADIENT "S" 
        // -------------------------------------------------------------
        // Laid flat directly under the base of the "P".
        // Spliced into three vertical character row stripes to paint the yellow/green/blue bands.
        for (int y = centerY + 4; y <= centerY + 9; y++) {
            int[] bandColor = RGB_S_GREEN;
            if (y <= centerY + 5)      bandColor = RGB_S_YELLOW; // Top stripe
            else if (y >= centerY + 8) bandColor = RGB_S_BLUE;   // Bottom stripe

            for (int x = centerX - 12; x <= centerX + 12; x++) {
                boolean insideS = false;
                
                // Construct the winding loops back and forth across horizontal fields
                if (y <= centerY + 5) {
                    // Top ribbon sweep hooks right
                    insideS = (x >= centerX - 4 && x <= centerX + 11);
                } else if (y <= centerY + 7) {
                    // Center diagonal crossbar connector lane slanting down-left
                    double pct = (double)(y - (centerY + 6)) / 1.0;
                    int targetX = (centerX + 6) - (int)(pct * 8.0);
                    insideS = (x >= targetX - 4 && x <= targetX + 4);
                } else {
                    // Bottom ribbon sweep hooks back left
                    insideS = (x >= centerX - 11 && x <= centerX + 2);
                }

                if (insideS) {
                    sVectorGroup.add(new PixelNode(x, y, bandColor));
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear frame to a pristine black canvas space
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, 0.0);

        // Advance stopwatch timeline counter
        timeClock += 0.014;
        
        // Soft cubic ease-out curve calculation to make components stop smoothly
        double progress = Math.min(1.0, timeClock * 0.90);
        double easeFactor = 1.0 - Math.pow(1.0 - progress, 3); // Cubic ease out curve

        // -------------------------------------------------------------
        // SLIDING TRANSLATION PASS 1: DRAW THE RED "P" (SLIDES DOWN)
        // -------------------------------------------------------------
        // Starts high above view boundaries (-24 pixels) and drops straight down to 0
        int pOffsetY = (int) (-24.0 * (1.0 - easeFactor));

        for (PixelNode node : pVectorGroup) {
            int currentX = node.x;
            int currentY = node.y + pOffsetY;

            if (currentX >= 0 && currentX < width && currentY >= 0 && currentY < height) {
                int index = currentX + width * currentY;
                
                // The "P" sits in front of the horizontal "S" in perspective drawing priority
                zBuffer[index] = 0.90; 
                
                String colorStr = String.format("\u001B[38;2;%d;%d;%dm", node.rgb[0], node.rgb[1], node.rgb[2]);
                outputBuffer[index] = colorStr + "█" + RESET;
            }
        }

        // -------------------------------------------------------------
        // SLIDING TRANSLATION PASS 2: DRAW THE TRIPLE BAND "S" (SLIDES DIAG)
        // -------------------------------------------------------------
        // Starts down-right off screen (+28 X, +15 Y) and glides upward-left into place
        int sOffsetX = (int) (28.0 * (1.0 - easeFactor));
        int sOffsetY = (int) (15.0 * (1.0 - easeFactor));

        for (PixelNode node : sVectorGroup) {
            int currentX = node.x + sOffsetX;
            int currentY = node.y + sOffsetY;

            if (currentX >= 0 && currentX < width && currentY >= 0 && currentY < height) {
                int index = currentX + width * currentY;

                // Ground plane layer yields draw priority if the upright "P" is already filling the slot
                if (zBuffer[index] < 0.85) {
                    zBuffer[index] = 0.50;
                    
                    // Subtle horizontal micro-shading to imply a glossy plastic reflection over the ribbon
                    int[] ribbonRGB = node.rgb;
                    if ((currentX + currentY) % 7 == 0) {
                        ribbonRGB = new int[]{ Math.min(255, ribbonRGB[0] + 25), Math.min(255, ribbonRGB[1] + 25), Math.min(255, ribbonRGB[2] + 25) };
                    }

                    String colorStr = String.format("\u001B[38;2;%d;%d;%dm", ribbonRGB[0], ribbonRGB[1], ribbonRGB[2]);
                    outputBuffer[index] = colorStr + "█" + RESET;
                }
            }
        }

        // Pause on completed screen then trigger a clean loop timeline cycle restart
        if (timeClock > 1.9) {
            initialize();
        }
    }
}
