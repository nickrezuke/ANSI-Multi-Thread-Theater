// TODO Improve this tornado

import java.util.Random;

public class TornadoLoader extends Loader {
    private static final StatusStage[] CYCLONE_STAGES = {
            new StatusStage(20, "Churning supercell pressure clouds:"),
            new StatusStage(45, "Dropping hyperbolic funnel vortex:"),
            new StatusStage(75, "Orbiting detached farmhouse debris:"),
            new StatusStage(100, "Kansas Cyclone Protocol Synchronized!")
    };

    private double timeClock = 0.0;
    private final int width = 120;
    private final int height = 36;
    private final Random rand = new Random(1939);

    // Moody, low-saturation vintage cinematic color palette registers
    private static final int[] RGB_SKY_TOP = { 20, 24, 28 }; // Menacing Inky Charcoal
    private static final int[] RGB_SKY_BTM = { 55, 60, 52 }; // Sickly Storm-Green Haze
    private static final int[] RGB_FUNNEL = { 40, 42, 45 }; // Dense Dust-Laden Grey
    private static final int[] RGB_FUNNEL_LIT = { 75, 80, 85 }; // Lightning-Illuminated Edge
    private static final int[] RGB_PLAINS = { 32, 42, 28 }; // Windswept Prairie Grass
    private static final int[] RGB_HOUSE = { 90, 75, 60 }; // Splintered Wood Farmhouse

    public TornadoLoader() {
        super(CYCLONE_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.045; // Master clock driving storm vortex rotation and debris paths

        int groundY = 28; // The horizon tracking split line separating earth from sky

        // Dynamic center coordinate tracking for the wandering tornado column
        double tornadoX = 60.0 + 12.0 * Math.sin(timeClock * 0.8) * Math.cos(timeClock * 0.3);

        for (int y = 0; y < height; y++) {
            // Render the universal background sky gradient
            double verticalPct = (double) y / height;
            int[] skyRGB = new int[] {
                    (int) (RGB_SKY_TOP[0] * (1.0 - verticalPct) + RGB_SKY_BTM[0] * verticalPct),
                    (int) (RGB_SKY_TOP[1] * (1.0 - verticalPct) + RGB_SKY_BTM[1] * verticalPct),
                    (int) (RGB_SKY_TOP[2] * (1.0 - verticalPct) + RGB_SKY_BTM[2] * verticalPct)
            };

            // Lightning Flash Modifier: Periodically brightens the background canvas color
            boolean isLightningFlash = Math.sin(timeClock * 4.5) > 0.94 && (rand.nextDouble() > 0.3);
            if (isLightningFlash && y < groundY) {
                skyRGB = new int[] { skyRGB[0] + 70, skyRGB[1] + 75, skyRGB[2] + 90 };
            }

            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // -------------------------------------------------------------
                // LAYER 1: ORBITING DEBRIS FIELDS (Z-Depth Tracker)
                // -------------------------------------------------------------
                boolean hitDebris = false;

                // Track 3 distinct elements rotating rapidly in 3D around the funnel
                for (int dId = 0; dId < 3; dId++) {
                    double orbitSpeed = 2.4 + dId * 0.6;
                    double orbitPhase = timeClock * orbitSpeed + (dId * 2.094); // Space items 120 deg apart

                    // 3D Cylinder Projection Math around the moving tornado center column
                    double itemRadius = 14.0 + 8.0 * Math.sin(timeClock * 0.2 + dId);
                    double itemZ = itemRadius * Math.sin(orbitPhase);

                    // Only render the item if it passes in front of the tornado center (Z-Buffer
                    // logic)
                    if (itemZ < -2.0) {
                        double itemX = tornadoX + itemRadius * Math.cos(orbitPhase);
                        double itemY = 6.0 + dId * 7.5 + 3.0 * Math.cos(orbitPhase * 2.0); // Levitated heights

                        int deltaX = x - (int) itemX;
                        int deltaY = y - (int) itemY;

                        if (dId == 0) {
                            // DEBRIS 0: THE FLYING KANSAS FARMHOUSE
                            boolean inRoof = (deltaY == -2 && Math.abs(deltaX) <= 1) || (deltaY == -3 && deltaX == 0);
                            boolean inBase = (deltaY >= -1 && deltaY <= 1) && Math.abs(deltaX) <= 2;

                            if ((inRoof || inBase) && 0.95 > zBuffer[index]) {
                                zBuffer[index] = 0.95;
                                String hColor = String.format("\u001B[38;2;%d;%d;%dm", RGB_HOUSE[0], RGB_HOUSE[1],
                                        RGB_HOUSE[2]);
                                char houseGlyph = inRoof ? '^' : '█';
                                outputBuffer[index] = hColor + houseGlyph + RESET;
                                hitDebris = true;
                                break;
                            }
                        } else {
                            // DEBRIS 1 & 2: Spinning uprooted tree branches / fragments
                            if (Math.abs(deltaX) <= 1 && Math.abs(deltaY) <= 1) {
                                if (0.92 > zBuffer[index]) {
                                    zBuffer[index] = 0.92;
                                    char branchChar = (Math.sin(orbitPhase * 4.0) > 0.0) ? '/' : '\\';
                                    outputBuffer[index] = "\u001B[38;2;45;38;32m" + branchChar + RESET;
                                    hitDebris = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (hitDebris)
                    continue;

                // -------------------------------------------------------------
                // LAYER 2: THE HYPERBOLIC CYCLONE FUNNEL (Z-Depth: 0.80)
                // -------------------------------------------------------------
                if (y < groundY) {
                    double dx = x - tornadoX;

                    // Hyperbolic Equation: Funnel expands outward exponentially as it approaches
                    // sky clouds
                    double inverseHeight = (groundY - y);
                    double funnelRadius = 2.0 + Math.pow(inverseHeight * 0.48, 1.45);

                    // Multi-threaded sin/cos noise fields spin inside the boundary shape
                    double vortexSpin = Math.sin(x * 0.7 - timeClock * 22.0) * Math.cos(y * 0.4 + timeClock * 8.0);
                    double adjustedDx = dx + 2.5 * Math.sin(y * 0.5 + timeClock * 4.0); // Sinusoidal rope sway

                    if (Math.abs(adjustedDx) < funnelRadius && 0.80 > zBuffer[index]) {
                        zBuffer[index] = 0.80;

                        // Lightning strikes highlight the left edge of the storm column
                        boolean leftEdgeGlow = (isLightningFlash && adjustedDx < -funnelRadius * 0.3);
                        int[] fRGB = leftEdgeGlow ? RGB_FUNNEL_LIT : RGB_FUNNEL;

                        // Map internal density textures
                        char funnelChar = '▓';
                        if (Math.abs(adjustedDx) > funnelRadius * 0.78)
                            funnelChar = '▒';
                        else if (vortexSpin > 0.4)
                            funnelChar = '█';

                        String fColor = String.format("\u001B[38;2;%d;%d;%dm", fRGB[0], fRGB[1], fRGB[2]);
                        outputBuffer[index] = fColor + funnelChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // LAYER 3: TURBULENT SURFACE DUST BASIN (Z-Depth: 0.75)
                // -------------------------------------------------------------
                // Churning cloud matrix resting where funnel touches down on plains
                double dustDx = x - tornadoX;
                double dustDy = groundY - y;
                double dustRadius = Math.sqrt(dustDx * dustDx * 0.45 + dustDy * dustDy * 3.5);

                if (dustRadius < 11.5 && y > 18 && 0.75 > zBuffer[index]) {
                    zBuffer[index] = 0.75;
                    double dustNoise = Math.sin(x * 1.5 + y * 2.2 + timeClock * 18.0);

                    int[] dRGB = { (int) (RGB_FUNNEL[0] * 0.8), (int) (RGB_FUNNEL[1] * 0.8),
                            (int) (RGB_FUNNEL[2] * 0.8) };
                    char dustChar = (dustNoise > 0.5) ? '▒' : (dustNoise > -0.2) ? '░' : '·';

                    String dColor = String.format("\u001B[38;2;%d;%d;%dm", dRGB[0], dRGB[1], dRGB[2]);
                    outputBuffer[index] = dColor + dustChar + RESET;
                    continue;
                }

                // -------------------------------------------------------------
                // LAYER 4: THE ROLLING PRAIRIE PLAINS (Z-Depth: 0.70)
                // -------------------------------------------------------------
                if (y >= groundY && 0.70 > zBuffer[index]) {
                    zBuffer[index] = 0.70;

                    // Surface grass sheets bend violently away from the vortex core
                    double distToCore = x - tornadoX;
                    double horizontalWindBend = Math.sin(x * 0.65 + timeClock * 5.0) * Math.cos(y * 1.1);

                    char groundChar = '▒';
                    if (horizontalWindBend > 0.4)
                        groundChar = (distToCore > 0) ? '/' : '\\';
                    else if (horizontalWindBend < -0.4)
                        groundChar = '▓';

                    // Churn up mud tracks closer to the eye path touchdown
                    int[] gRGB = (Math.abs(distToCore) < 14.0) ? new int[] { 55, 48, 38 } : RGB_PLAINS;

                    String gColor = String.format("\u001B[38;2;%d;%d;%dm", gRGB[0], gRGB[1], gRGB[2]);
                    outputBuffer[index] = gColor + groundChar + RESET;
                    continue;
                }
                // -------------------------------------------------------------
                // LAYER 5: STORM CLOUD BACKDROP CANVAS (Z-Depth: 0.01)
                // -------------------------------------------------------------
                if (0.01 > zBuffer[index]) {
                    zBuffer[index] = 0.01;
                    // Procedural high-altitude scud clouds tracking across upper sky boundaries
                    double cloudNoise = Math.sin(x * 0.08 + timeClock * 0.5) * Math.cos(y * 0.15);
                    if (cloudNoise > 0.25 && y < 15) {
                        outputBuffer[index] = "\u001B[38;2;32;35;40m▓\u001B[0m";
                        // Dark overlapping shelf cloud
                    } else {
                        String sColor = String.format("\u001B[38;2;%d;%d;%dm", skyRGB[0], skyRGB[1], skyRGB[2]);
                        outputBuffer[index] = sColor + "█" + RESET;
                    }
                }
            }
        }
    }
}