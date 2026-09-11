import java.util.Random;
import java.util.Arrays;

public class SwirlMarbleLoader extends Loader {
    private static final StatusStage[] MARBLE_STAGES = {
            new StatusStage(20, "Melting high-purity clear glass:"),
            new StatusStage(50, "Extruding multi-color strand ribbons:"),
            new StatusStage(80, "Forming smooth interior S-curves:"),
            new StatusStage(100, "Swirl Strand Marble Ready!")
    };

    // Glass shell tint colors
    private int[] rgbGlass;
    private int[] rgbRim;
    private int[] rgbShade;
    private int[] rgbBubble = { 210, 235, 245 };
    private int[] rgbSparkle = { 255, 255, 255 };
    private int[] rgbShadow = { 8, 8, 10 };

    // Multi-color strand band palette for the current marble variant
    private int[][] strandColors; // [0] = primary strand band, [1] = secondary strand band, [optional 2] = accent band

    private double waveFrequency;
    private double waveAmplitude;
    private double A = 0.0;
    private final Random rand = new Random();

    private static final int BUBBLE_COUNT = 2;
    private final double[] bbX = new double[BUBBLE_COUNT];
    private final double[] bbY = new double[BUBBLE_COUNT];
    private final double[] bbZ = new double[BUBBLE_COUNT];

    private static final int SPARKLE_COUNT = 3;
    private final double[] skX = new double[SPARKLE_COUNT];
    private final double[] skY = new double[SPARKLE_COUNT];
    private final double[] skZ = new double[SPARKLE_COUNT];
    private final double[] skPhase = new double[SPARKLE_COUNT];

    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public SwirlMarbleLoader() {
        super(MARBLE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        int variant = rand.nextInt(6);
        switch (variant) {
            case 0: // Red & Blue Ribbon
                rgbGlass = new int[]{ 220, 235, 248 };
                rgbRim   = new int[]{ 240, 248, 255 };
                rgbShade = new int[]{ 25, 35, 55 };
                strandColors = new int[][]{
                    { 225, 35, 35 },  // Bold Red
                    { 35, 95, 225 }   // Deep Royal Blue
                };
                waveAmplitude = 0.38;
                waveFrequency = 1.1;
                break;

            case 1: // Blue & Yellow Ribbon
                rgbGlass = new int[]{ 215, 238, 245 };
                rgbRim   = new int[]{ 245, 250, 255 };
                rgbShade = new int[]{ 20, 35, 50 };
                strandColors = new int[][]{
                    { 30, 110, 220 }, // Cobalt Blue
                    { 245, 195, 30 }  // Vibrant Yellow
                };
                waveAmplitude = 0.42;
                waveFrequency = 0.95;
                break;

            case 2: // Green & Red Strand
                rgbGlass = new int[]{ 210, 235, 240 };
                rgbRim   = new int[]{ 235, 245, 255 };
                rgbShade = new int[]{ 20, 40, 35 };
                strandColors = new int[][]{
                    { 35, 175, 75 },  // Emerald Green
                    { 220, 45, 45 }   // Crimson Red
                };
                waveAmplitude = 0.36;
                waveFrequency = 1.25;
                break;

            case 3: // Pink & Purple Ribbon
                rgbGlass = new int[]{ 235, 225, 245 };
                rgbRim   = new int[]{ 250, 240, 255 };
                rgbShade = new int[]{ 40, 25, 50 };
                strandColors = new int[][]{
                    { 240, 110, 170 }, // Hot Pink
                    { 130, 55, 180 }   // Velvet Purple
                };
                waveAmplitude = 0.40;
                waveFrequency = 1.0;
                break;

            case 4: // Green & Yellow Ribbon
                rgbGlass = new int[]{ 220, 240, 230 };
                rgbRim   = new int[]{ 240, 255, 245 };
                rgbShade = new int[]{ 25, 45, 30 };
                strandColors = new int[][]{
                    { 25, 155, 65 },  // Grass Green
                    { 245, 210, 35 }  // Canary Yellow
                };
                waveAmplitude = 0.35;
                waveFrequency = 1.15;
                break;

            case 5: // Navy Blue & Burnt Orange
                rgbGlass = new int[]{ 210, 225, 245 };
                rgbRim   = new int[]{ 235, 245, 255 };
                rgbShade = new int[]{ 15, 25, 45 };
                strandColors = new int[][]{
                    { 20, 30, 70 },   // Midnight Navy
                    { 240, 120, 20 }  // Burnt Orange
                };
                waveAmplitude = 0.44;
                waveFrequency = 0.90;
                break;
        }

        for (int i = 0; i < BUBBLE_COUNT; i++) {
            resetBubble(i);
        }
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            resetSparkle(i);
            skPhase[i] = rand.nextDouble() * 2.0 * Math.PI;
        }
    }

    private void resetBubble(int i) {
        double t = (rand.nextDouble() * 2.0 - 1.0) * 0.75;
        double maxR = 0.80 * Math.sqrt(Math.max(0.0001, 0.90 * 0.90 - t * t));
        double r = (0.2 + rand.nextDouble() * 0.7) * maxR;
        double angle = rand.nextDouble() * 2.0 * Math.PI;
        bbX[i] = r * Math.cos(angle);
        bbY[i] = t;
        bbZ[i] = r * Math.sin(angle);
    }

    private void resetSparkle(int i) {
        double t = (rand.nextDouble() * 2.0 - 1.0) * 0.80;
        double maxR = 0.75 * Math.sqrt(Math.max(0.0001, 0.85 * 0.85 - t * t));
        double r = rand.nextDouble() * maxR;
        double angle = rand.nextDouble() * 2.0 * Math.PI;
        skX[i] = r * Math.cos(angle);
        skY[i] = t;
        skZ[i] = r * Math.sin(angle);
    }

    private boolean withinGlobe(double x, double y, double z, double limit) {
        return (x * x + y * y + z * z) < limit * limit;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;
        double glassRadius = 0.95;

        // Slow zoom cycle
        double zoomPeriodMillis = 11000.0;
        double zoomT = (System.currentTimeMillis() % (long) zoomPeriodMillis) / zoomPeriodMillis;
        double zoomPhase = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * zoomT);
        double distanceToCamera = 2.4 - 0.7 * zoomPhase;

        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // 1. Soft Ground Shadow
        renderShadow(outputBuffer, distanceToCamera);

        // 2. Interior S-Curve Strand Ribbon
        renderStrandRibbon(cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer, glassRadius);

        // Bubbles inside glass
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            if (withinGlobe(bbX[i], bbY[i], bbZ[i], glassRadius)) {
                plotRawElement(bbX[i], bbY[i], bbZ[i], 0, -1, 0, rgbBubble, '○', false,
                        cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // Glints / Sparkles inside glass
        double nowSeconds = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            double twinkle = 0.5 + 0.5 * Math.sin(nowSeconds * 3.0 + skPhase[i]);
            if (twinkle < 0.40) continue;
            char glyph = twinkle > 0.80 ? '*' : '.';
            if (withinGlobe(skX[i], skY[i], skZ[i], glassRadius)) {
                plotRawElement(skX[i], skY[i], skZ[i], 0, -1, 0, rgbSparkle, glyph, true,
                        cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // 3. Chromatic Glass Shell Pass with Front Surface Reflection Overrides
        int thetaSteps = 110;
        int phiSteps = 200;
        for (int tIndex = 0; tIndex <= thetaSteps; tIndex++) {
            double theta = (tIndex / (double) thetaSteps) * Math.PI;
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

            for (int pIndex = 0; pIndex < phiSteps; pIndex++) {
                double phi = (pIndex / (double) phiSteps) * 2.0 * Math.PI;
                double localX = glassRadius * sinTheta * Math.cos(phi);
                double localY = glassRadius * cosTheta;
                double localZ = glassRadius * sinTheta * Math.sin(phi);

                double rx = localX * cosA + localZ * sinA;
                double ry = localY;
                double rz = -localX * sinA + localZ * cosA;

                double ooz = 1.0 / (rz + distanceToCamera);

                int xp = (int) (40 + 36 * ooz * rx * 1.2);
                int yp = (int) (11 + 17 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int bufferIndex = xp + 80 * yp;

                    if (ooz > zBuffer[bufferIndex]) {
                        double gNx = sinTheta * Math.cos(phi) * cosA + sinTheta * Math.sin(phi) * sinA;
                        double gNy = cosTheta;
                        double gNz = -sinTheta * Math.cos(phi) * sinA + sinTheta * Math.sin(phi) * cosA;

                        double luminance = gNx * lightX + gNy * lightY + gNz * lightZ;
                        double rim = 1.0 - Math.abs(gNz);
                        boolean isFrontFace = (rz < 0);

                        double specular = 0.0;
                        if (luminance > 0) {
                            double rzSpec = 2 * luminance * gNz - lightZ;
                            double viewDotR = -rzSpec;
                            if (viewDotR > 0) {
                                specular = Math.pow(viewDotR, 10.0);
                            }
                        }

                        int r, g, b;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            if (isFrontFace && specular > 0.60) {
                                r = (int) Math.min(255, rgbRim[0] * 0.5 + 255 * 0.5);
                                g = (int) Math.min(255, rgbRim[1] * 0.5 + 255 * 0.5);
                                b = (int) Math.min(255, rgbRim[2] * 0.5 + 255 * 0.5);
                                finalChar = specular > 0.82 ? '*' : '░';
                            } else if (isFrontFace && rim > 0.93) {
                                r = rgbRim[0]; g = rgbRim[1]; b = rgbRim[2];
                                finalChar = '░';
                            } else {
                                double alpha = 0.25;
                                r = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + rgbGlass[0] * alpha);
                                g = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + rgbGlass[1] * alpha);
                                b = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + rgbGlass[2] * alpha);
                                finalChar = rawCharBuffer[bufferIndex];

                                if (luminance > 0.70) {
                                    double sheen = 0.35 * ((luminance - 0.70) / 0.30);
                                    r = (int) Math.min(255, r * (1.0 - sheen) + 255 * sheen);
                                    g = (int) Math.min(255, g * (1.0 - sheen) + 255 * sheen);
                                    b = (int) Math.min(255, b * (1.0 - sheen) + 255 * sheen);
                                }
                            }
                        } else {
                            if (specular > 0.60) {
                                r = 255; g = 255; b = 255;
                                finalChar = specular > 0.82 ? '*' : '░';
                            } else if (rim > 0.92) {
                                r = rgbRim[0]; g = rgbRim[1]; b = rgbRim[2];
                                finalChar = '░';
                            } else if (luminance > 0.65) {
                                r = (int) (rgbGlass[0] * 0.7 + 255 * 0.3);
                                g = (int) (rgbGlass[1] * 0.7 + 255 * 0.3);
                                b = (int) (rgbGlass[2] * 0.7 + 255 * 0.3);
                                finalChar = '░';
                            } else {
                                r = rgbShade[0]; g = rgbShade[1]; b = rgbShade[2];
                                finalChar = '.';
                            }
                        }

                        String esc = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, r)),
                                Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
                        outputBuffer[bufferIndex] = esc + finalChar + RESET;
                    }
                }
            }
        }

        // 4. Safety net cleanup
        for (int i = 0; i < 80 * 22; i++) {
            if (outputBuffer[i] == null || outputBuffer[i].isEmpty() || outputBuffer[i].equals(" ")) {
                if (rawCharBuffer[i] != ' ' && rawCharBuffer[i] != 0) {
                    String esc = String.format("\u001B[38;2;%d;%d;%dm", rawColorBuffer[i][0], rawColorBuffer[i][1],
                            rawColorBuffer[i][2]);
                    outputBuffer[i] = esc + rawCharBuffer[i] + RESET;
                } else if (outputBuffer[i] == null) {
                    outputBuffer[i] = " ";
                }
            }
        }

        A += 0.014;
    }

    // Renders a smooth, wavy multi-colored ribbon strand inside the marble
    private void renderStrandRibbon(double cosA, double sinA, double lightX, double lightY, double lightZ,
            double distanceToCamera, double[] zBuffer, double glassRadius) {
        
        double ribbonHeightLimit = 0.82;
        
        for (double t = -ribbonHeightLimit; t <= ribbonHeightLimit; t += 0.016) {
            // S-curve wave trajectory along height t
            double wavePhase = t * Math.PI * waveFrequency;
            double offsetX = waveAmplitude * Math.sin(wavePhase);
            double offsetZ = (waveAmplitude * 0.6) * Math.cos(wavePhase * 0.8);

            // Ribbon width contracts near top & bottom poles
            double heightScale = Math.sqrt(Math.max(0.0001, 1.0 - (t * t) / (ribbonHeightLimit * ribbonHeightLimit)));
            double ribbonWidth = 0.55 * heightScale;

            // Surface normal for lighting the ribbon face
            double dXdt = waveAmplitude * Math.PI * waveFrequency * Math.cos(wavePhase);
            double nx = -dXdt;
            double ny = 1.0;
            double nz = 0.3;
            double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= nLen; ny /= nLen; nz /= nLen;

            for (double w = -ribbonWidth / 2.0; w <= ribbonWidth / 2.0; w += 0.018) {
                double localX = offsetX + w;
                double localY = t;
                double localZ = offsetZ;

                if (!withinGlobe(localX, localY, localZ, glassRadius * 0.88)) {
                    continue;
                }

                // Determine parallel color strand band based on position across ribbon width
                double normW = (w + (ribbonWidth / 2.0)) / ribbonWidth; // 0.0 to 1.0
                int colorIdx = normW < 0.50 ? 0 : 1;
                int[] rgb = strandColors[colorIdx];

                // Smooth ASCII glyphs across the ribbon face
                char glyph = (normW > 0.44 && normW < 0.56) ? '▒' : '█';

                // Thin ribbon front & back surfaces
                for (double d = -0.02; d <= 0.02; d += 0.02) {
                    plotRawElement(localX, localY, localZ + d, nx, ny, nz, rgb, glyph, false,
                            cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                }
            }
        }
    }

    private void renderShadow(String[] outputBuffer, double distanceToCamera) {
        double shadowY = 1.05;
        double shadowRadiusX = 0.95;
        double shadowRadiusZ = 0.55;

        for (double r = 0.0; r <= 1.0; r += 0.035) {
            double fade = 1.0 - r;
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.12) {
                double rx = r * shadowRadiusX * Math.cos(phi);
                double rz = r * shadowRadiusZ * Math.sin(phi);
                double ooz = 1.0 / (rz + distanceToCamera);
                int xp = (int) (40 + 36 * ooz * rx * 1.2);
                int yp = (int) (11 + 17 * ooz * shadowY);
                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int bufferIndex = xp + 80 * yp;
                    if (outputBuffer[bufferIndex].equals(" ")) {
                        char glyph = fade > 0.55 ? '▓' : (fade > 0.22 ? '▒' : '░');
                        String esc = String.format("\u001B[38;2;%d;%d;%dm", rgbShadow[0], rgbShadow[1], rgbShadow[2]);
                        outputBuffer[bufferIndex] = esc + glyph + RESET;
                    }
                }
            }
        }
    }

    private void plotRawElement(double localX, double localY, double localZ, double rNx, double rNy, double rNz,
            int[] rgb, char asciiChar, boolean fixedFullShade, double cosA, double sinA, double lightX,
            double lightY, double lightZ, double distanceToCamera, double[] zBuffer) {
        double rx = localX * cosA + localZ * sinA, ry = localY, rz = -localX * sinA + localZ * cosA;
        double nx = rNx * cosA + rNz * sinA, ny = rNy, nz = -rNx * sinA + rNz * cosA;
        double ooz = 1.0 / (rz + distanceToCamera);
        int xp = (int) (40 + 36 * ooz * rx * 1.2);
        int yp = (int) (11 + 17 * ooz * ry);
        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int bufferIndex = xp + 80 * yp;
            if (ooz > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooz;
                double luminance = nx * lightX + ny * lightY + nz * lightZ;
                double shade = fixedFullShade ? 1.0 : 0.45 + 0.55 * Math.max(0.0, luminance);
                rawCharBuffer[bufferIndex] = asciiChar;
                rawColorBuffer[bufferIndex][0] = (int) Math.min(255, rgb[0] * shade);
                rawColorBuffer[bufferIndex][1] = (int) Math.min(255, rgb[1] * shade);
                rawColorBuffer[bufferIndex][2] = (int) Math.min(255, rgb[2] * shade);
            }
        }
    }
}