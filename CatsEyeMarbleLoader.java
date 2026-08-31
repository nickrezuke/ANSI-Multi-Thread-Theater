//TODO: Is the "reflection" on the correct z layer it looks behind the cats eye interior?
//TODO: Add marble Variants?? like classic yellow / colored inrerior or like classic marbles

import java.util.Random;
import java.util.Arrays;

public class CatsEyeMarbleLoader extends Loader {
    private static final StatusStage[] MARBLE_STAGES = {
            new StatusStage(20, "Gathering molten glass:"),
            new StatusStage(50, "Twisting the color vane:"),
            new StatusStage(80, "Cooling the cat's eye swirl:"),
            new StatusStage(100, "Cat's Eye Marble Ready!")
    };

    // Raw RGB color arrays for analog transmissive alpha blending
    private static final int[] RGB_GLASS = { 210, 232, 248 }; // clear glass tint (cool, faintly blue)
    private static final int[] RGB_RIM = { 235, 245, 255 }; // bright rim highlight where the dome catches light
    private static final int[] RGB_SHADE = { 25, 35, 50 }; // deep glass shadow on the backlit side
    private static final int[] RGB_VANE_CENTER = { 45, 195, 120 }; // emerald "iris" at the swirl's core
    private static final int[] RGB_VANE_EDGE = { 235, 190, 65 }; // gold where the swirl fades toward the glass
    private static final int[] RGB_PUPIL = { 12, 12, 16 }; // near-black slit down the vane's spine
    private static final int[] RGB_SPARKLE = { 255, 255, 255 }; // glassy inclusions catching the light
    private static final int[] RGB_BUBBLE = { 190, 225, 235 }; // tiny trapped air bubbles
    private static final int[] RGB_SHADOW = { 8, 8, 10 }; // soft ground shadow beneath the marble

    // Interior envelope the vane grows within - deliberately smaller than the
    // glass shell radius so a visible rind of clear glass always separates the
    // swirl from the surface, the way it does in a real cat's eye marble.
    private static final double VANE_RADIUS = 0.90;
    private static final double TWIST_TURNS = 2.25; // full twists the ribbon makes from pole to pole
    private static final double TWIST_RATE = TWIST_TURNS * Math.PI * 2.0 / (VANE_RADIUS * 2.0);

    private double A = 0.0;
    private final Random rand = new Random(2026);

    private static final int SPARKLE_COUNT = 42;
    private final double[] skX = new double[SPARKLE_COUNT];
    private final double[] skY = new double[SPARKLE_COUNT];
    private final double[] skZ = new double[SPARKLE_COUNT];
    private final double[] skPhase = new double[SPARKLE_COUNT];

    private static final int BUBBLE_COUNT = 9;
    private final double[] bbX = new double[BUBBLE_COUNT];
    private final double[] bbY = new double[BUBBLE_COUNT];
    private final double[] bbZ = new double[BUBBLE_COUNT];

    // Mirror screen buffers to track un-encoded raw character states for alpha
    // coloring passes, same trick the snow globe uses to let the glass shell
    // pass tint whatever interior content is behind it.
    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public CatsEyeMarbleLoader() {
        super(MARBLE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            resetSparkle(i);
            skPhase[i] = rand.nextDouble() * 2.0 * Math.PI;
        }
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            resetBubble(i);
        }
    }

    // Scatters a sparkle somewhere inside the twisted vane's own envelope (not
    // just anywhere in the sphere), so they read as inclusions caught in the
    // color rather than dust floating in the clear glass.
    private void resetSparkle(int i) {
        double t = (rand.nextDouble() * 2.0 - 1.0) * VANE_RADIUS;
        double crossRadiusMax = 0.80 * Math.sqrt(Math.max(0.0001, VANE_RADIUS * VANE_RADIUS - t * t));
        double w = (rand.nextDouble() * 2.0 - 1.0) * crossRadiusMax * 0.92;
        double angle = twistAngleAt(t);
        skX[i] = w * Math.cos(angle);
        skY[i] = t;
        skZ[i] = w * Math.sin(angle);
    }

    private void resetBubble(int i) {
        double t = (rand.nextDouble() * 2.0 - 1.0) * 0.80;
        double maxR = 0.85 * Math.sqrt(Math.max(0.0001, 0.90 * 0.90 - t * t));
        double r = (0.25 + rand.nextDouble() * 0.65) * maxR;
        double angle = rand.nextDouble() * 2.0 * Math.PI;
        bbX[i] = r * Math.cos(angle);
        bbY[i] = t;
        bbZ[i] = r * Math.sin(angle);
    }

    private double twistAngleAt(double t) {
        return TWIST_RATE * t;
    }

    private boolean withinGlobe(double x, double y, double z, double limit) {
        return (x * x + y * y + z * z) < limit * limit;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;
        double glassRadius = 0.95;

        // --- Slow breathing zoom: eases from a small, whole marble seen from a
        // distance, in to a close-up that fills the frame with the swirled vane,
        // and back out again. A full breath takes about 11 seconds, so a viewer
        // watching for even a few seconds catches both the "whole object" read
        // and the "inner detail" read rather than being stuck on just one.
        double zoomPeriodMillis = 11000.0;
        double zoomT = (System.currentTimeMillis() % (long) zoomPeriodMillis) / zoomPeriodMillis;
        double zoomPhase = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * zoomT); // eases 0 -> 1 -> 0
        double farDistance = 3.0; // whole marble, small, floating in frame
        double nearDistance = 1.5; // close-up, swirl fills most of the screen
        double distanceToCamera = farDistance - (farDistance - nearDistance) * zoomPhase;

        // Clear raw mirroring text buffers
        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // -------------------------------------------------------------
        // STEP 1: SOFT GROUND SHADOW (written straight to the frame first, so
        // nothing downstream has to know it exists)
        // -------------------------------------------------------------
        renderShadow(outputBuffer, distanceToCamera);

        // -------------------------------------------------------------
        // STEP 2: INTERIOR CONTENT MATERIAL PASS (Rendered Behind Glass)
        // -------------------------------------------------------------
        renderVane(cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer, glassRadius);

        for (int i = 0; i < BUBBLE_COUNT; i++) {
            if (withinGlobe(bbX[i], bbY[i], bbZ[i], glassRadius)) {
                plotRawElement(bbX[i], bbY[i], bbZ[i], 0, -1, 0, RGB_BUBBLE, '○', false,
                        cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        double nowSeconds = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            double twinkle = 0.5 + 0.5 * Math.sin(nowSeconds * 3.0 + skPhase[i]);
            if (twinkle < 0.35) {
                continue; // sparkle's "off" phase - keeps the glints from feeling static
            }
            char glyph = twinkle > 0.75 ? '*' : '.';
            if (withinGlobe(skX[i], skY[i], skZ[i], glassRadius)) {
                plotRawElement(skX[i], skY[i], skZ[i], 0, -1, 0, RGB_SPARKLE, glyph, true,
                        cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 3: CHROMATIC TRANSMISSIVE GLASS SHELL PASS (Color Filter Overwrite)
        // Full sphere this time (no cutout) since a marble has no base attached
        // to it the way the snow globe's dome does.
        // -------------------------------------------------------------
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
                        double luminance = gNx * lightX + gNy * lightY;

                        int r, g, b;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            // --- GLASS TINT INJECTION: gentle clear wash over the swirl showing through ---
                            double alpha = 0.22;
                            r = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_GLASS[0] * alpha);
                            g = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_GLASS[1] * alpha);
                            b = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_GLASS[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex]; // Keep the swirl's silhouette intact

                            if (luminance > 0.80) {
                                double sheen = 0.20;
                                r = (int) (r * (1.0 - sheen) + 255 * sheen);
                                g = (int) (g * (1.0 - sheen) + 255 * sheen);
                                b = (int) (b * (1.0 - sheen) + 255 * sheen);
                            }
                        } else {
                            // Empty canvas glass layer background profile (rim glint / sky sheen / shadow)
                            double rim = 1.0 - Math.abs(gNx);
                            if (rim > 0.94) {
                                r = RGB_RIM[0]; g = RGB_RIM[1]; b = RGB_RIM[2]; finalChar = '░';
                            } else if (luminance > 0.72) {
                                r = 200; g = 220; b = 235; finalChar = '░';
                            } else {
                                r = RGB_SHADE[0]; g = RGB_SHADE[1]; b = RGB_SHADE[2]; finalChar = '.';
                            }
                        }

                        String esc = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, r)),
                                Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
                        outputBuffer[bufferIndex] = esc + finalChar + RESET;
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 4: FLUSH ANY UNCOVERED SWIRL PIXELS - a sampling safety net for
        // when the glass shell's ray density can't quite keep up with how large
        // the marble gets on screen at the closest point of the zoom.
        // -------------------------------------------------------------
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

        A += 0.012;
    }

    // A soft, static (non-rotating) shadow ellipse just under the marble, like
    // it's resting on a table. Painted directly into outputBuffer before
    // anything else, and never touched again since later passes only ever
    // write pixels that fall within the marble's own silhouette.
    private void renderShadow(String[] outputBuffer, double distanceToCamera) {
        double shadowY = 1.05;
        double shadowRadiusX = 0.95;
        double shadowRadiusZ = 0.55; // squashed toward the viewer, like an ellipse cast on a table

        for (double r = 0.0; r <= 1.0; r += 0.035) {
            double fade = 1.0 - r; // darkest directly under the marble, fading outward
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
                        String esc = String.format("\u001B[38;2;%d;%d;%dm", RGB_SHADOW[0], RGB_SHADOW[1],
                                RGB_SHADOW[2]);
                        outputBuffer[bufferIndex] = esc + glyph + RESET;
                    }
                }
            }
        }
    }

    // The twisted color ribbon at the marble's core. At each height t, the
    // vane is a straight cross-section line through the local origin; as t
    // climbs, that line's angle sweeps around the vertical axis at TWIST_RATE,
    // so the union of all the lines forms a continuous twisted ribbon surface -
    // the classic cat's eye vane. A dark slit runs down its spine (the
    // "pupil"), fading out to gold at the ribbon's edges (the "iris").
    private void renderVane(double cosA, double sinA, double lightX, double lightY, double lightZ,
            double distanceToCamera, double[] zBuffer, double glassRadius) {
        for (double t = -VANE_RADIUS; t <= VANE_RADIUS; t += 0.018) {
            double crossRadiusMax = 0.80 * Math.sqrt(Math.max(0.0001, VANE_RADIUS * VANE_RADIUS - t * t));
            double angle = twistAngleAt(t);
            double cosAngle = Math.cos(angle), sinAngle = Math.sin(angle);

            // Ribbon face normal: perpendicular to both the twist direction and the
            // vertical axis, so lighting reads it as a flat twisted strip.
            double nx = -sinAngle, ny = 0.10, nz = cosAngle;

            for (double w = -crossRadiusMax; w <= crossRadiusMax; w += 0.02) {
                double localX = w * cosAngle;
                double localY = t;
                double localZ = w * sinAngle;

                if (!withinGlobe(localX, localY, localZ, glassRadius)) {
                    continue;
                }

                double u = crossRadiusMax > 0.0001 ? Math.abs(w) / crossRadiusMax : 0.0;
                int[] rgb;
                char glyph;
                if (u < 0.09) {
                    rgb = RGB_PUPIL;
                    glyph = '█';
                } else {
                    rgb = lerpColor(RGB_VANE_CENTER, RGB_VANE_EDGE, (u - 0.09) / 0.91);
                    glyph = u > 0.85 ? '▒' : '▓';
                }

                plotRawElement(localX, localY, localZ, nx, ny, nz, rgb, glyph, false,
                        cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }
    }

    private int[] lerpColor(int[] a, int[] b, double f) {
        f = Math.max(0.0, Math.min(1.0, f));
        return new int[] {
                (int) (a[0] + (b[0] - a[0]) * f),
                (int) (a[1] + (b[1] - a[1]) * f),
                (int) (a[2] + (b[2] - a[2]) * f)
        };
    }

    // Generalized version of the snow globe's plotRawElement: takes an explicit
    // RGB + glyph instead of a fixed surfaceType switch, so callers (the vane's
    // gradient in particular) can hand it a freshly-blended color per vertex.
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