import java.util.Random;
import java.util.Arrays;

public class SnowGlobeLoader extends Loader {
    private static final StatusStage[] GLOBE_STAGES = {
            new StatusStage(20, "Blowing 3D glass outer sphere:"),
            new StatusStage(50, "Constructing interior snow cabin:"),
            new StatusStage(80, "Stoking active internal winter storm:"),
            new StatusStage(100, "3D Component Snow Globe Active!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    // Raw RGB color arrays for analog transmissive alpha blending
    private static final int[] RGB_BASE = { 120, 80, 55 }; // Mahogany Wood
    private static final int[] RGB_GLASS = { 160, 235, 255 }; // Translucent Cyan Tint
    private static final int[] RGB_SNOW = { 255, 255, 255 }; // Pure White Snow
    private static final int[] RGB_CABIN = { 145, 105, 80 }; // Timber Cabin
    private static final int[] RGB_HAT = { 40, 40, 45 }; // Coal Black

    // New detail materials
    private static final int[] RGB_COAL = { 30, 30, 35 }; // eyes & buttons
    private static final int[] RGB_CARROT = { 225, 120, 35 }; // nose
    private static final int[] RGB_TWIG = { 92, 64, 40 }; // arms
    private static final int[] RGB_SCARF = { 178, 40, 40 }; // scarf
    private static final int[] RGB_DOOR = { 70, 45, 30 }; // cabin door
    private static final int[] RGB_WINDOW = { 255, 205, 110 }; // warm window glow (emissive)
    private static final int[] RGB_CHIMNEY = { 120, 60, 50 }; // brick chimney
    private static final int[] RGB_SMOKE = { 205, 205, 210 }; // rising smoke wisps
    private static final int[] RGB_PINE = { 35, 90, 55 }; // pine foliage
    private static final int[] RGB_TRUNK = { 96, 65, 42 }; // tree trunk
    private static final int[] RGB_GOLD = { 205, 170, 70 }; // pedestal brass trim

    private double A = 0.0;
    private final Random rand = new Random(2026);

    private static final int PARTICLE_COUNT = 32; // Reduced snow count to ease visual clutter
    private final double[] pX = new double[PARTICLE_COUNT];
    private final double[] pY = new double[PARTICLE_COUNT];
    private final double[] pZ = new double[PARTICLE_COUNT];

    private static final int SMOKE_COUNT = 14;
    private final double[] sX = new double[SMOKE_COUNT];
    private final double[] sY = new double[SMOKE_COUNT];
    private final double[] sZ = new double[SMOKE_COUNT];

    // Mirror screen buffers to track un-encoded raw character states for alpha
    // coloring passes
    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public SnowGlobeLoader() {
        super(GLOBE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetSnowflake(i);
            pY[i] = -0.8 + rand.nextDouble() * 1.35;
        }
        for (int i = 0; i < SMOKE_COUNT; i++) {
            resetSmoke(i);
            sY[i] -= rand.nextDouble() * 0.5; // stagger heights so the plume doesn't pulse in unison
        }
    }

    private void resetSnowflake(int i) {
        double angle = rand.nextDouble() * 2.0 * Math.PI;
        double r = rand.nextDouble() * 0.70;
        pX[i] = r * Math.cos(angle);
        pY[i] = -0.8;
        pZ[i] = r * Math.sin(angle);
    }

    private void resetSmoke(int i) {
        sX[i] = (rand.nextDouble() - 0.5) * 0.02;
        sY[i] = -0.07;
        sZ[i] = (rand.nextDouble() - 0.5) * 0.02;
    }

    private boolean withinGlobe(double x, double y, double z, double limit) {
        return (x * x + y * y + z * z) < limit * limit;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;
        double globeRadius = 0.95;

        double timeStep = System.currentTimeMillis() / 1500.0;
        double sinTimeStep = Math.sin(timeStep);
        double midDist = 1.9;
        double swingFactor = 1.0;
        double distanceToCamera = midDist + swingFactor * Math.signum(sinTimeStep) * Math.sqrt(Math.abs(sinTimeStep));

        // I use this weirdo instead of a simple sine function to give more of an oomph to the zoom
        
        
        // Cabin anchor point, reused by walls/roof/door/window/chimney below
        double shiftX = -0.22, shiftZ = -0.10;
        double chimneyX = shiftX + 0.15, chimneyZ = shiftZ - 0.15;

        // Clear raw mirroring text buffers
        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // Advance snow field updates
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            pY[i] += 0.014; // Slowed down snow drop rate for readability
            pX[i] += 0.004 * Math.cos(A * 2.0 + i);
            double rSq = pX[i] * pX[i] + pY[i] * pY[i] + pZ[i] * pZ[i];
            if (pY[i] >= 0.55 || rSq >= globeRadius * globeRadius * 0.92) {
                resetSnowflake(i);
            }
        }

        // Advance chimney smoke updates (rises = more negative y, drifts, then resets)
        for (int i = 0; i < SMOKE_COUNT; i++) {
            sY[i] -= 0.010;
            sX[i] += 0.006 * Math.sin(A * 3.0 + i);
            sZ[i] += 0.004 * Math.cos(A * 2.0 + i);
            if (sY[i] < -0.55) {
                resetSmoke(i);
            }
            double lx = chimneyX + sX[i], ly = sY[i], lz = chimneyZ + sZ[i];
            if (withinGlobe(lx, ly, lz, globeRadius)) {
                plotRawElement(lx, ly, lz, 0, -1, 0, 12, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                        zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 1: INTERIOR CONTENT MATERIAL PASS (Rendered Behind Glass)
        // -------------------------------------------------------------

        // INTERIOR A: The Ground Snow Layer
        for (double h = 0.50; h <= 0.55; h += 0.01) {
            for (double r = 0.0; r < 0.85; r += 0.04) {
                for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.06) {
                    double lx = r * Math.cos(phi), ly = h, lz = r * Math.sin(phi);
                    if ((lx * lx + ly * ly + lz * lz) < globeRadius * globeRadius) {
                        plotRawElement(lx, ly, lz, 0, -1, 0, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                                zBuffer);
                    }
                }
            }
        }

        // INTERIOR B: Timber Cabin Walls
        for (double h = 0.15; h <= 0.50; h += 0.02) {
            for (double w = -0.20; w <= 0.20; w += 0.003) {
                for (double d = -0.20; d <= 0.20; d += 0.003) {
                    if (Math.abs(w) > 0.17 || Math.abs(d) > 0.17 || h < 0.17) {
                        double lx = shiftX + w, ly = h, lz = shiftZ + d;
                        if ((lx * lx + ly * ly + lz * lz) < globeRadius * globeRadius) {
                            plotRawElement(lx, ly, lz, (w > 0 ? 1 : -1), 0, (d > 0 ? 1 : -1), 3, cosA, sinA, lightX,
                                    lightY, lightZ, distanceToCamera, zBuffer);
                        }
                    }
                }
            }
        }

        // INTERIOR C: Cabin Snow Roof
        for (double rh = -0.05; rh <= 0.15; rh += 0.015) {
            double roofWidth = 0.24 * (1.0 - (rh + 0.05) / 0.20);
            for (double rw = -roofWidth; rw <= roofWidth; rw += 0.02) {
                for (double rd = -0.22; rd <= 0.22; rd += 0.02) {
                    double lx = shiftX + rw, ly = 0.15 + rh, lz = shiftZ + rd;
                    if ((lx * lx + ly * ly + lz * lz) < globeRadius * globeRadius) {
                        plotRawElement(lx, ly, lz, 0, -1, 0, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                                zBuffer);
                    }
                }
            }
        }

        // INTERIOR C2: Cabin Door & Glowing Window (new detail for the zoomed-in view)
        for (double h = 0.32; h <= 0.47; h += 0.001) {
            for (double w = -0.05; w <= 0.05; w += 0.001) {
                double lx = shiftX + w, ly = h, lz = shiftZ + 0.20;
                if (withinGlobe(lx, ly, lz, globeRadius)) {
                    plotRawElement(lx, ly, lz, 0, 0, 1, 9, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                            zBuffer);
                }
            }
        }
        for (double h = 0.20; h <= 0.30; h += 0.0004) {
            for (double d = -0.07; d <= 0.07; d += 0.0004) {
                double lx = shiftX - 0.20, ly = h, lz = shiftZ + d;
                if (withinGlobe(lx, ly, lz, globeRadius)) {
                    plotRawElement(lx, ly, lz, -1, 0, 0, 10, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                            zBuffer);
                }
            }
        }

        // INTERIOR C3: Chimney (bricks - the rising smoke is animated up above)
        for (double h = -0.06; h <= 0.11; h += 0.015) {
            for (double w = -0.02; w <= 0.02; w += 0.02) {
                for (double d = -0.02; d <= 0.02; d += 0.02) {
                    double lx = chimneyX + w, ly = h, lz = chimneyZ + d;
                    if (withinGlobe(lx, ly, lz, globeRadius)) {
                        plotRawElement(lx, ly, lz, (w > 0 ? 1 : -1), 0, (d > 0 ? 1 : -1), 11, cosA, sinA, lightX,
                                lightY, lightZ, distanceToCamera, zBuffer);
                    }
                }
            }
        }

        // INTERIOR D: Spheroid Snowman Components
        double smX = 0.32, smZ = 0.15;
        renderSpheroidRaw(smX, 0.40, smZ, 0.15, 0.15, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                zBuffer, globeRadius);
        renderSpheroidRaw(smX, 0.22, smZ, 0.11, 0.11, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                zBuffer, globeRadius);
        renderSpheroidRaw(smX, 0.08, smZ, 0.08, 0.08, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                zBuffer, globeRadius);

        // Snowman face, buttons, twig arms & scarf (new detail for the zoomed-in view)
        renderSnowmanFace(smX, 0.08, smZ, smX, 0.22, smZ, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                zBuffer, globeRadius);

        // Snowman Top-Hat
        for (double th = -0.06; th <= 0.00; th += 0.015) {
            double hatRadius = (th < -0.04) ? 0.09 : 0.055;
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.25) {
                double lx = smX + hatRadius * Math.cos(phi), ly = 0.01 + th, lz = smZ + hatRadius * Math.sin(phi);
                if ((lx * lx + ly * ly + lz * lz) < globeRadius * globeRadius) {
                    plotRawElement(lx, ly, lz, 0, -1, 0, 4, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                            zBuffer);
                }
            }
        }

        // INTERIOR D2: Small snow-capped pine tree (new detail for the zoomed-in view)
        renderPineTree(0.02, 0.34, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer, globeRadius);

        // INTERIOR E: Active Snow Flurry Nodes
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            plotRawElement(pX[i], pY[i], pZ[i], 0, -1, 0, 5, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                    zBuffer);
        }

        // -------------------------------------------------------------
        // STEP 2: SOLID PEDESTAL WOOD BASE LAYERS (now with carved fluting + brass trim)
        // -------------------------------------------------------------
        for (double h = 0.55; h <= 0.95; h += 0.02) {
            double baseRadiusAtH = 0.85 + (h - 0.55) * 0.5;
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.03) {
                double currentRadius = baseRadiusAtH + 0.018 * Math.sin(phi * 14.0); // carved vertical flutes
                double localX = currentRadius * Math.cos(phi), localY = h, localZ = currentRadius * Math.sin(phi);
                plotRawElement(localX, localY, localZ, Math.cos(phi), 0.2, Math.sin(phi), 1, cosA, sinA, lightX,
                        lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // Brass trim ring where the pedestal meets the glass dome
        for (double h = 0.555; h <= 0.585; h += 0.01) {
            double trimRadius = 0.85 + (h - 0.55) * 0.5 + 0.01;
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.025) {
                double localX = trimRadius * Math.cos(phi), localY = h, localZ = trimRadius * Math.sin(phi);
                plotRawElement(localX, localY, localZ, Math.cos(phi), 0.2, Math.sin(phi), 16, cosA, sinA, lightX,
                        lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // Decorative brass studs around the pedestal's midline
        for (double phi = 0; phi < 2.0 * Math.PI; phi += (Math.PI / 10.0)) {
            double studRadius = 0.85 + (0.72 - 0.55) * 0.5 + 0.03;
            double localX = studRadius * Math.cos(phi), localY = 0.72, localZ = studRadius * Math.sin(phi);
            plotRawElement(localX, localY, localZ, Math.cos(phi), 0.2, Math.sin(phi), 16, cosA, sinA, lightX, lightY,
                    lightZ, distanceToCamera, zBuffer);
        }

        // Brass trim ring at the pedestal's bottom rim
        for (double h = 0.93; h <= 0.96; h += 0.01) {
            double trimRadius = 0.85 + (h - 0.55) * 0.5 + 0.015;
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.025) {
                double localX = trimRadius * Math.cos(phi), localY = h, localZ = trimRadius * Math.sin(phi);
                plotRawElement(localX, localY, localZ, Math.cos(phi), 0.2, Math.sin(phi), 16, cosA, sinA, lightX,
                        lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 3: CHROMATIC TRANSMISSIVE GLASS DOME PASS (Color Filter Overwrite)
        // -------------------------------------------------------------
        for (int tIndex = 0; tIndex < 90; tIndex++) {
            double theta = (tIndex / 90.0) * Math.PI;
            if (theta > Math.PI * 0.70)
                continue; // Boundary cutout line where base attaches
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

            for (int pIndex = 0; pIndex < 180; pIndex++) {
                double phi = (pIndex / 180.0) * 2.0 * Math.PI;
                double localX = globeRadius * sinTheta * Math.cos(phi);
                double localY = globeRadius * cosTheta;
                double localZ = globeRadius * sinTheta * Math.sin(phi);

                // Rotate glass nodes to face camera coordinates
                double rx = localX * cosA + localZ * sinA;
                double ry = localY;
                double rz = -localX * sinA + localZ * cosA;

                // Uses the SAME per-frame distanceToCamera as everything else now -
                // this was previously recomputed here independently, which was half
                // of the "exploding grey lines" bug (see note at top of file).
                double ooz = 1.0 / (rz + distanceToCamera);

                int xp = (int) (40 + 36 * ooz * rx * 1.2);
                int yp = (int) (11 + 17 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int bufferIndex = xp + 80 * yp;

                    // Only affect screen positions where glass is closer than previously drawn
                    // background nodes
                    if (ooz > zBuffer[bufferIndex]) {
                        double gNx = sinTheta * Math.cos(phi) * cosA + sinTheta * Math.sin(phi) * sinA;
                        double gNy = cosTheta;
                        double luminance = gNx * lightX + gNy * lightY;

                        int r, g, b;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            // --- GLASS TINT INJECTION: gentle cyan wash over the interior scene ---
                            double alpha = 0.30; // Density transparency index (was 0.35)
                            r = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_GLASS[0] * alpha);
                            g = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_GLASS[1] * alpha);
                            b = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_GLASS[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex]; // Keep object silhouette intact!

                            // Soft specular sheen on catch-light spots - blended, not a hard
                            // white overwrite, so it no longer blots out the interior detail.
                            if (luminance > 0.80) {
                                double sheen = 0.22;
                                r = (int) (r * (1.0 - sheen) + 255 * sheen);
                                g = (int) (g * (1.0 - sheen) + 255 * sheen);
                                b = (int) (b * (1.0 - sheen) + 255 * sheen);
                            }
                        } else {
                            // Empty canvas glass layer background profiles (softened, narrower thresholds)
                            double rim = 1.0 - Math.abs(gNx);
                            if (rim > 0.94) {
                                r = 220; g = 232; b = 245; finalChar = '░'; // narrow rim glint
                            } else if (luminance > 0.72) {
                                r = 195; g = 212; b = 228; finalChar = '░'; // gentle sky-light highlight
                            } else {
                                r = 30; g = 40; b = 55; finalChar = '.'; // deep back shadow environment nodes
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
        // STEP 4: FLUSH BASE PEDESTAL LINES TO TERMINAL OUTPUT
        // -------------------------------------------------------------
        for (int i = 0; i < 80 * 22; i++) {
            if (outputBuffer[i] == null || outputBuffer[i].isEmpty() || outputBuffer[i].equals(" ")) {
                if (rawCharBuffer[i] != ' ' && rawCharBuffer[i] != 0) {
                    // Draw base pedestal pieces that extend below the glass boundaries
                    String esc = String.format("\u001B[38;2;%d;%d;%dm", rawColorBuffer[i][0], rawColorBuffer[i][1],
                            rawColorBuffer[i][2]);
                    outputBuffer[i] = esc + rawCharBuffer[i] + RESET;
                } else {
                    outputBuffer[i] = " ";
                }
            }
        }
        A += 0.015;
    }

    private void plotRawElement(double localX, double localY, double localZ, double rNx, double rNy, double rNz,
            int surfaceType, double cosA, double sinA, double lightX, double lightY, double lightZ,
            double distanceToCamera, double[] zBuffer) {
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
                int shadeIndex = (int) ((luminance + 1.0) * 5.5);
                shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
                char asciiChar = SHADE_RAMP[shadeIndex];
                int[] rgb = RGB_GLASS;
                boolean fixedFullShade = false;
                if (surfaceType == 1) {
                    rgb = RGB_BASE;
                } else if (surfaceType == 2) {
                    rgb = RGB_SNOW;
                    asciiChar = '█';
                    fixedFullShade = true;
                } else if (surfaceType == 3) {
                    rgb = RGB_CABIN;
                } else if (surfaceType == 4) {
                    rgb = RGB_HAT;
                    asciiChar = '█';
                } else if (surfaceType == 5) {
                    rgb = RGB_SNOW;
                    asciiChar = '·';
                } else if (surfaceType == 6) {
                    rgb = RGB_COAL;
                    asciiChar = '•';
                } else if (surfaceType == 7) {
                    rgb = RGB_CARROT;
                    asciiChar = '▶';
                } else if (surfaceType == 8) {
                    rgb = RGB_TWIG;
                } else if (surfaceType == 9) {
                    rgb = RGB_DOOR;
                    asciiChar = '█';
                } else if (surfaceType == 10) {
                    rgb = RGB_WINDOW;
                    asciiChar = '▓';
                    fixedFullShade = true; // glows regardless of external light
                } else if (surfaceType == 11) {
                    rgb = RGB_CHIMNEY;
                    asciiChar = '█';
                } else if (surfaceType == 12) {
                    rgb = RGB_SMOKE;
                    asciiChar = '°';
                } else if (surfaceType == 13) {
                    rgb = RGB_PINE;
                } else if (surfaceType == 14) {
                    rgb = RGB_TRUNK;
                    asciiChar = '█';
                } else if (surfaceType == 15) {
                    rgb = RGB_SCARF;
                    asciiChar = '█';
                } else if (surfaceType == 16) {
                    rgb = RGB_GOLD;
                }
                // Cache calculated data blocks to be blended inside Step 3
                rawCharBuffer[bufferIndex] = asciiChar;
                double shade = 0.45 + 0.55 * Math.max(0.0, luminance);
                double appliedShade = fixedFullShade ? 1.0 : shade;
                rawColorBuffer[bufferIndex][0] = (int) (rgb[0] * appliedShade);
                rawColorBuffer[bufferIndex][1] = (int) (rgb[1] * appliedShade);
                rawColorBuffer[bufferIndex][2] = (int) (rgb[2] * appliedShade);
            }
        }
    }

    private void renderSpheroidRaw(double cx, double cy, double cz, double rx, double ry, int type, double cosA,
            double sinA, double lx, double ly, double lz, double distanceToCamera, double[] zBuf,
            double globeLimit) {
        for (int t = 0; t < 20; t++) {
            double theta = (t / 20.0) * Math.PI;
            for (int p = 0; p < 40; p++) {
                double phi = (p / 40.0) * 2.0 * Math.PI;
                double x = cx + rx * Math.sin(theta) * Math.cos(phi);
                double y = cy + ry * Math.cos(theta);
                double z = cz + rx * Math.sin(theta) * Math.sin(phi);
                if ((x * x + y * y + z * z) < globeLimit * globeLimit) {
                    plotRawElement(x, y, z, Math.sin(theta) * Math.cos(phi), Math.cos(theta),
                            Math.sin(theta) * Math.sin(phi), type, cosA, sinA, lx, ly, lz, distanceToCamera, zBuf);
                }
            }
        }
    }

    // Snowman face: coal eyes, carrot nose, coal buttons, twig arms, and a red scarf.
    private void renderSnowmanFace(double headX, double headY, double headZ, double torsoX, double torsoY,
            double torsoZ, double cosA, double sinA, double lightX, double lightY, double lightZ,
            double distanceToCamera, double[] zBuffer, double globeRadius) {
        // Coal eyes
        double[][] eyes = {
                { headX + 0.032, headY - 0.015, headZ + 0.068 },
                { headX - 0.032, headY - 0.015, headZ + 0.068 }
        };
        for (double[] eye : eyes) {
            if (withinGlobe(eye[0], eye[1], eye[2], globeRadius)) {
                plotRawElement(eye[0], eye[1], eye[2], 0, -0.2, 0.9, 6, cosA, sinA, lightX, lightY, lightZ,
                        distanceToCamera, zBuffer);
            }
        }

        // Carrot nose, poking just past the head's surface
        double noseX = headX, noseY = headY + 0.005, noseZ = headZ + 0.095;
        if (withinGlobe(noseX, noseY, noseZ, globeRadius)) {
            plotRawElement(noseX, noseY, noseZ, 0, 0, 1, 7, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                    zBuffer);
        }

        // Coal buttons down the torso
        for (double dy = -0.05; dy <= 0.05; dy += 0.05) {
            double bx = torsoX, by = torsoY + dy, bz = torsoZ + 0.108;
            if (withinGlobe(bx, by, bz, globeRadius)) {
                plotRawElement(bx, by, bz, 0, 0, 1, 6, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                        zBuffer);
            }
        }

        // Twig arms, angled slightly upward like branches
        for (int side = -1; side <= 1; side += 2) {
            for (double t = 0.02; t <= 0.16; t += 0.02) {
                double lx = torsoX + side * t;
                double ly = torsoY - t * 0.55;
                double lz = torsoZ + 0.02;
                if (withinGlobe(lx, ly, lz, globeRadius)) {
                    plotRawElement(lx, ly, lz, 0, -1, 0, 8, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                            zBuffer);
                }
            }
        }

        // Red scarf ring at the neck
        double neckY = headY + 0.075;
        for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.3) {
            double lx = headX + 0.085 * Math.cos(phi);
            double lz = headZ + 0.085 * Math.sin(phi);
            if (withinGlobe(lx, neckY, lz, globeRadius)) {
                plotRawElement(lx, neckY, lz, Math.cos(phi), 0, Math.sin(phi), 15, cosA, sinA, lightX, lightY,
                        lightZ, distanceToCamera, zBuffer);
            }
        }
    }

    // Small snow-capped pine tree: stacked skirts + trunk + snow tip.
    private void renderPineTree(double treeX, double treeZ, double cosA, double sinA, double lightX, double lightY,
            double lightZ, double distanceToCamera, double[] zBuffer, double globeRadius) {
        double[] layerBaseY = { 0.49, 0.41, 0.33, 0.26 };
        double[] layerRadius = { 0.15, 0.115, 0.08, 0.05 };
        for (int layer = 0; layer < layerBaseY.length; layer++) {
            double baseY = layerBaseY[layer];
            double baseR = layerRadius[layer];
            for (double dy = 0; dy <= 0.07; dy += 0.012) {
                double frac = dy / 0.07;
                double r = baseR * (1.0 - frac * 0.85);
                for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.3) {
                    double lx = treeX + r * Math.cos(phi);
                    double ly = baseY - dy;
                    double lz = treeZ + r * Math.sin(phi);
                    if (withinGlobe(lx, ly, lz, globeRadius)) {
                        plotRawElement(lx, ly, lz, Math.cos(phi), -0.4, Math.sin(phi), 13, cosA, sinA, lightX,
                                lightY, lightZ, distanceToCamera, zBuffer);
                    }
                }
            }
        }
        // Trunk peeking out beneath the lowest skirt
        for (double h = 0.49; h <= 0.53; h += 0.015) {
            if (withinGlobe(treeX, h, treeZ, globeRadius)) {
                plotRawElement(treeX, h, treeZ, 0, -1, 0, 14, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                        zBuffer);
            }
        }
        // Snow-capped tip
        double tipY = layerBaseY[layerBaseY.length - 1] - 0.07;
        if (withinGlobe(treeX, tipY, treeZ, globeRadius)) {
            plotRawElement(treeX, tipY, treeZ, 0, -1, 0, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera,
                    zBuffer);
        }
    }
}