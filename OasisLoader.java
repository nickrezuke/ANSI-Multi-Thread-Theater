public class OasisLoader extends Loader {
    private static final StatusStage[] OASIS_STAGES = {
            new StatusStage(25, "Calculating atmospheric light scattering:"),
            new StatusStage(50, "Carving sharp dune ridge shadows:"),
            new StatusStage(75, "Excavating crater & pooling mirror water:"),
            new StatusStage(100, "Tranquil Oasis Horizon Synchronized!")
    };

    private double timeClock = 0.0;
    private final int width = 120;
    private final int height = 36;

    // Sun-Drenched Daytime Desert Color Palette
    private static final int[] RGB_SKY_GLOW = { 255, 248, 220 }; // Blinding midday sun white-gold
    private static final int[] RGB_SKY_DEEP = { 90, 150, 195 }; // Brilliant desert blue sky

    private static final int[] RGB_SAND_LIT = { 245, 175, 75 }; // Bright golden sand
    private static final int[] RGB_SAND_MID = { 205, 130, 50 }; // Warm midtone sand
    private static final int[] RGB_SAND_SHADOW = { 135, 70, 35 }; // Rich terra-cotta ridge shadow

    private static final int[] RGB_CRATER_LIT = { 170, 105, 50 }; // Lit dirt bank
    private static final int[] RGB_CRATER_SHADOW = { 90, 45, 25 }; // Crater rim shadow

    // Brighter, Beautiful Daytime Oasis Water
    private static final int[] RGB_WATER = { 50, 120, 140 }; // Vibrant turquoise pool
    private static final int[] RGB_WATER_GLINT = { 160, 220, 210 }; // Shimmering sky reflection

    // Lush Sunlit Oasis Flora
    private static final int[] RGB_LEAF_LIT = { 100, 170, 65 }; // Bright sun-kissed palm green
    private static final int[] RGB_LEAF_SHADOW = { 50, 95, 35 }; // Deep green frond shadow
    private static final int[] RGB_PALM_TRUNK = { 110, 75, 45 }; // Warm wooden trunk
    private static final int[] RGB_PALM_CAST = { 110, 55, 30 }; // Soft shadows cast on sand

    public OasisLoader() {
        super(OASIS_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.04; // Advances animation frames smoothly

        double poolCx = 60.0;
        double poolCy = 27.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + width * y;
                double noise = spatialHash(x, y);

                // -------------------------------------------------------------
                // LAYER 1: ATMOSPHERIC SKY SCATTERING (Z-Depth: 0.10)
                // -------------------------------------------------------------
                double lightDx = x - (-10.0);
                double lightDy = (y - (-5.0)) * 2.2;
                double distToLight = Math.sqrt(lightDx * lightDx + lightDy * lightDy);

                double glowWeight = Math.max(0.0, 1.0 - (distToLight / 140.0));
                if (distToLight < 8.0)
                    glowWeight = 1.2;

                int[] skyRGB = blendColors(RGB_SKY_DEEP, RGB_SKY_GLOW, Math.pow(Math.min(1.0, glowWeight), 2.0));

                if (0.10 > zBuffer[index]) {
                    zBuffer[index] = 0.10;

                    char skyChar = (noise > glowWeight * 2.0) ? '▓' : '█';
                    outputBuffer[index] = colorStr(skyRGB) + skyChar + RESET;

                }

                // -------------------------------------------------------------
                // LAYER 2: SUNLIT ORGANIC SAND DUNES (Z-Depth: 0.30 - 0.50)
                // -------------------------------------------------------------
                double backDune = 12.0 + Math.sin(x * 0.04) * 2.5 + Math.cos(x * 0.1) * 1.0;
                double midDune = 18.0 + Math.sin((x - 20) * 0.06) * 4.0;
                double frontDune = 24.0 + Math.cos((x + 40) * 0.03) * 6.0;

                double activeDune = height;
                if (y >= backDune)
                    activeDune = backDune;
                if (y >= midDune && midDune > backDune)
                    activeDune = midDune;
                if (y >= frontDune && frontDune > midDune)
                    activeDune = frontDune;

                if (y >= activeDune && 0.40 > zBuffer[index]) {
                    zBuffer[index] = 0.40;

                    double slope = Math.sin(x * 0.05 + y * 0.1);
                    int[] sandRGB;
                    char sandChar;

                    if (slope > 0.2) {
                        sandRGB = blendColors(RGB_SAND_SHADOW, RGB_SAND_MID, 0.4 + noise * 0.2);
                        sandChar = (noise > 0.6) ? '▓' : '▒';
                    } else {
                        double ripple = Math.sin(y * 3.0 + x * 0.1) + noise * 0.5;
                        if (ripple > 0.8) {
                            sandRGB = blendColors(RGB_SAND_MID, RGB_SAND_LIT, 0.5);
                            sandChar = '▒';
                        } else {
                            sandRGB = RGB_SAND_LIT;
                            sandChar = (noise > 0.8) ? '▓' : '█';
                        }
                    }

                    outputBuffer[index] = colorStr(sandRGB) + sandChar + RESET;
                }

                // -------------------------------------------------------------
                // LAYER 3: ERODED CRATER & SHIMMERING WATER POOL (Z-Depth: 0.60)
                // -------------------------------------------------------------
                double dx = x - poolCx;
                double dy = y - poolCy;

                double angle = Math.atan2(dy, dx);
                double edgeWarp = Math.sin(angle * 7.0) * 0.15 + (noise - 0.5) * 0.3;

                double distOuter = (dx * dx) / (40.0 * 40.0) + (dy * dy) / (10.0 * 10.0) + edgeWarp;
                double distInner = (dx * dx) / (28.0 * 28.0) + (dy * dy) / (5.5 * 5.5) + (edgeWarp * 1.5);

                if (distOuter < 1.0 && 0.60 > zBuffer[index]) {
                    zBuffer[index] = 0.60;

                    if (distInner < 1.0) {
                        // SHIMMERING WATER SURFACE (Animated with timeClock)
                        int[] waterRGB = RGB_WATER;
                        char waterChar = '█';

                        // 1. Multi-layered organic wave interference
                        double wave1 = Math.sin(x * 0.4 + timeClock * 2.5 + y * 0.5);
                        double wave2 = Math.cos(x * 0.15 - timeClock * 1.2 + y * 0.3);
                        double wave3 = Math.sin((x - y) * 0.25 + timeClock * 1.8);

                        // Combine frequencies and add spatial noise for chaos
                        double waveHeight = (wave1 * 0.4) + (wave2 * 0.4) + (wave3 * 0.2);
                        waveHeight += (noise - 0.5) * 0.3;

                        // 2. Map ASCII chars to wave topography (peaks and troughs)
                        if (waveHeight > 0.3) {
                            waterChar = '~';
                        } else if (waveHeight < -0.3) {
                            waterChar = '=';
                        } else if (noise > 0.8) {
                            waterChar = '-';
                        }

                        // 3. Dynamic Specular Sunlight Glint
                        // Creates a sweeping reflection band that only illuminates wave peaks
                        double glintSweep = Math.sin(timeClock * 0.8) * 5.0;
                        double sunBand = Math.exp(-Math.pow((dx + 8.0 + glintSweep) / 7.0, 2.0));

                        if (waveHeight > 0.0 && sunBand > 0.1) {
                            double glintIntensity = Math.min(1.0, waveHeight * sunBand * 2.5);
                            waterRGB = blendColors(RGB_WATER, RGB_WATER_GLINT, glintIntensity);

                            // High intensity specular glints
                            if (glintIntensity > 0.5)
                                waterChar = '▒';
                            if (glintIntensity > 0.8)
                                waterChar = '▓';
                        } else {
                            // Subsurface depth coloring (troughs become slightly darker turquoise)
                            int[] deepWater = { 35, 95, 115 };
                            double depthRatio = Math.max(0.0, Math.min(1.0, (waveHeight + 1.0) / 2.0));
                            waterRGB = blendColors(deepWater, RGB_WATER, depthRatio);
                        }

                        // 4. Distorted Palm Tree Reflections
                        if (x > 75 && x < 105 && (noise > 0.3) && (x % 12 < 4)) {
                            // Reflection breaks up on the highest wave peaks
                            if (waveHeight < 0.5) {
                                waterRGB = blendColors(waterRGB, RGB_LEAF_SHADOW, 0.5);
                            }
                        }

                        outputBuffer[index] = colorStr(waterRGB) + waterChar + RESET;
                    } else {
                        // SHORELINE BANK
                        int[] rimRGB = (dx < 0) ? RGB_CRATER_SHADOW : RGB_CRATER_LIT;
                        char rimChar = (noise > 0.5) ? '▓' : '▒';

                        if (distOuter > 0.7) {
                            int[] adjacentSand = (dx < 0) ? RGB_SAND_SHADOW : RGB_SAND_LIT;
                            rimRGB = blendColors(rimRGB, adjacentSand, (distOuter - 0.7) / 0.3);
                        }

                        outputBuffer[index] = colorStr(rimRGB) + rimChar + RESET;
                    }
                }

                // -------------------------------------------------------------
                // LAYER 4: CAST SHADOWS (Z-Depth: 0.70)
                // -------------------------------------------------------------
                if (y > 18 && x > 75 && 0.70 > zBuffer[index]) {
                    double shadowWarp = Math.sin(y * 0.5) * 1.5 + noise * 1.0;
                    double shadow1 = Math.abs(y - (21 + (x - 85 + shadowWarp) * 0.28));
                    double shadow2 = Math.abs(y - (18 + (x - 98 + shadowWarp) * 0.25));

                    if ((shadow1 < 1.0 || shadow2 < 1.0) && distOuter > 1.1) {
                        zBuffer[index] = 0.70;
                        outputBuffer[index] = colorStr(RGB_PALM_CAST) + ((noise > 0.4) ? "▓" : "▒") + RESET;
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 5: SWAYING PALM TREES & FLORA (Z-Depth: 0.80)
        // -------------------------------------------------------------
        // Calculate organic wind sway offset based on timeClock
        double windSway = Math.sin(timeClock * 1.8) * 1.5;

        renderPalmTree(outputBuffer, zBuffer, 85, 22, (int) (80 + windSway), 6);
        renderPalmTree(outputBuffer, zBuffer, 98, 19, (int) (95 + windSway * 1.2), 8);

        renderReeds(outputBuffer, zBuffer, 40, 24);
        renderReeds(outputBuffer, zBuffer, 45, 27);
        renderReeds(outputBuffer, zBuffer, 88, 25);
    }

    private void renderPalmTree(String[] outputBuffer, double[] zBuffer, int baseX, int baseY, int topX, int topY) {
        // Draw Curved Trunk
        for (int y = topY; y <= baseY; y++) {
            double t = (double) (y - topY) / (baseY - topY);
            int curX = (int) (topX + t * (baseX - topX) + Math.sin(t * Math.PI) * 2.0);

            int index = curX + width * y;
            if (index >= 0 && index < width * height && 0.80 > zBuffer[index]) {
                zBuffer[index] = 0.80;
                int[] trunkColor = blendColors(RGB_PALM_TRUNK, RGB_SAND_LIT, 0.15);
                outputBuffer[index] = colorStr(trunkColor) + "█" + RESET;
            }
        }

        // Lush Sunlit Fronds
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 1.5 && dist < 7.5) {
                    double arc = -Math.abs(dx) * 0.6 + 2.0;
                    if (Math.abs(dy - arc) < 1.5) {
                        if (spatialHash(topX + dx, topY + dy) > 0.3) {
                            int fx = topX + dx;
                            int fy = topY + dy;
                            int index = fx + width * fy;

                            if (index >= 0 && index < width * height && 0.80 > zBuffer[index]) {
                                zBuffer[index] = 0.80;
                                int[] leafColor = (dx < 0 && dy < 0) ? RGB_LEAF_LIT : RGB_LEAF_SHADOW;
                                char leaf = (Math.abs(dx) > Math.abs(dy)) ? '≈' : '║';
                                if (dx < 0 && dy > 0)
                                    leaf = '/';
                                if (dx > 0 && dy > 0)
                                    leaf = '\\';

                                outputBuffer[index] = colorStr(leafColor) + leaf + RESET;
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderReeds(String[] outputBuffer, double[] zBuffer, int cx, int cy) {
        int[][] grassOffsets = { { 0, 0 }, { -1, -1 }, { 1, -1 }, { 0, -2 } };
        for (int[] offset : grassOffsets) {
            int index = (cx + offset[0]) + width * (cy + offset[1]);
            if (index >= 0 && index < width * height && 0.80 > zBuffer[index]) {
                zBuffer[index] = 0.80;
                int[] grassColor = (offset[0] < 0) ? RGB_LEAF_LIT : RGB_LEAF_SHADOW;
                outputBuffer[index] = colorStr(grassColor) + "│" + RESET;
            }
        }
    }

    private double spatialHash(int x, int y) {
        long n = (long) x * 374761393L + (long) y * 668265263L;
        n = (n ^ (n >> 13)) * 1274126177L;
        return ((n ^ (n >> 16)) & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private int[] blendColors(int[] c1, int[] c2, double ratio) {
        double r = Math.max(0.0, Math.min(1.0, ratio));
        return new int[] {
                (int) (c1[0] * (1.0 - r) + c2[0] * r),
                (int) (c1[1] * (1.0 - r) + c2[1] * r),
                (int) (c1[2] * (1.0 - r) + c2[2] * r)
        };
    }

    private String colorStr(int[] rgb) {
        return String.format("\u001B[38;2;%d;%d;%dm", rgb[0], rgb[1], rgb[2]);
    }
}