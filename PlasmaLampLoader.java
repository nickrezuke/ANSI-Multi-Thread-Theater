import java.util.Arrays;

public class PlasmaLampLoader extends Loader {
    private static final StatusStage[] PLASMA_STAGES = {
            new StatusStage(20, "Forging glass globe & central electrode:"),
            new StatusStage(50, "Charging high-voltage core transformer:"),
            new StatusStage(80, "Igniting reactive plasma filaments:"),
            new StatusStage(100, "3D Component Plasma Lamp Active!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    // Raw RGB color arrays for 24-bit true-color rendering
    private static final int[] RGB_BASE = { 35, 38, 48 };         // Dark metallic base
    private static final int[] RGB_BASE_TRIM = { 160, 130, 60 };   // Brass accent trim ring
    private static final int[] RGB_SWITCH = { 220, 50, 40 };      // Red power toggle switch
    private static final int[] RGB_STEM = { 20, 22, 28 };         // Insulated center stem
    private static final int[] RGB_CORE = { 230, 60, 220 };       // High-voltage central bulb
    private static final int[] RGB_ARC_INNER = { 240, 245, 255 }; // Electric arc core filament
    private static final int[] RGB_ARC_OUTER = { 180, 50, 255 };  // Neon purple/magenta tendril shroud
    private static final int[] RGB_HOTSPOT = { 80, 220, 255 };     // Cyan plasma impact point on glass
    private static final int[] RGB_GLASS = { 140, 180, 245 };     // Glass globe translucency

    private double A = 0.0;

    // Number of active dynamic plasma filaments
    private static final int ARC_COUNT = 12;

    // Mirror screen buffers to track raw character states and RGB colors for alpha passes
    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public PlasmaLampLoader() {
        super(PLASMA_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        setTargetFps(60); // the fast moving plasma arcs look better this way
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        double globeCenterY = -0.10;
        double globeRadius = 0.85;

        double midDist = 1.5;
        double swingFactor = 0.5;
        double distanceToCamera = midDist + swingFactor * Math.cos(A * 0.25);

        // Clear raw mirroring text buffers
        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // -------------------------------------------------------------
        // STEP 1: INTERIOR CONTENT MATERIAL PASS (Rendered Behind Glass)
        // -------------------------------------------------------------

        // INTERIOR A: Central Insulated Stem
        double stemBaseY = 0.42;
        double stemTopY = 0.08;
        for (double sy = stemTopY; sy <= stemBaseY; sy += 0.015) {
            double stemR = 0.06;
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.2) {
                double lx = stemR * Math.cos(phi);
                double ly = sy;
                double lz = stemR * Math.sin(phi);
                plotRawElement(lx, ly, lz, Math.cos(phi), 0, Math.sin(phi), 4, cosA, sinA, lightX, lightY, lightZ,
                        distanceToCamera, zBuffer);
            }
        }

        // INTERIOR B: Central High-Voltage Core Bulb (Emissive Sphere)
        double coreY = globeCenterY + 0.05;
        double coreRadius = 0.16;
        for (double t = 0; t <= Math.PI; t += 0.12) {
            double sinT = Math.sin(t), cosT = Math.cos(t);
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.2) {
                double lx = coreRadius * sinT * Math.cos(phi);
                double ly = coreY + coreRadius * cosT;
                double lz = coreRadius * sinT * Math.sin(phi);
                plotRawElement(lx, ly, lz, sinT * Math.cos(phi), cosT, sinT * Math.sin(phi), 5, cosA, sinA, lightX,
                        lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // INTERIOR C: Dynamic Plasma Filaments / Electric Arcs
        for (int arc = 0; arc < ARC_COUNT; arc++) {
            // Target angle on glass sphere surface moving dynamically over time
            double targetTheta = 0.3 * Math.PI + 0.5 * Math.PI * Math.sin(A * 1.2 + arc * 1.5);
            double targetPhi = arc * (2.0 * Math.PI / ARC_COUNT) + A * 0.8 + 0.3 * Math.sin(A * 2.5 + arc);

            // Arc end-point on the inner surface of the glass sphere
            double targetX = globeRadius * 0.96 * Math.sin(targetTheta) * Math.cos(targetPhi);
            double targetY = globeCenterY + globeRadius * 0.96 * Math.cos(targetTheta);
            double targetZ = globeRadius * 0.96 * Math.sin(targetTheta) * Math.sin(targetPhi);

            // Arc start-point on the core bulb
            double startX = coreRadius * Math.sin(targetTheta) * Math.cos(targetPhi);
            double startY = coreY + coreRadius * Math.cos(targetTheta);
            double startZ = coreRadius * Math.sin(targetTheta) * Math.sin(targetPhi);

            // Interpolate points along tendril path with organic writhing wiggles
            int steps = 45;
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                double envelope = Math.sin(t * Math.PI); // Envelope peaks in middle of arc

                // Sine/cosine harmonic displacement for electrical writhing effect
                double dx = 0.09 * envelope * Math.sin(8.0 * t + A * 6.0 + arc * 2.1);
                double dy = 0.09 * envelope * Math.cos(7.0 * t - A * 5.0 + arc * 1.7);
                double dz = 0.09 * envelope * Math.sin(9.0 * t + A * 7.0 + arc * 0.9);

                double px = startX + t * (targetX - startX) + dx;
                double py = startY + t * (targetY - startY) + dy;
                double pz = startZ + t * (targetZ - startZ) + dz;

                // Electric arc core filament
                plotRawElement(px, py, pz, 0, -1, 0, 6, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);

                // Surrounding arc glow shroud
                if (s % 2 == 0) {
                    plotRawElement(px + 0.015, py + 0.015, pz, 0, -1, 0, 7, cosA, sinA, lightX, lightY, lightZ,
                            distanceToCamera, zBuffer);
                }
            }

            // INTERIOR D: Plasma Hotspot / Touch Point on Glass Sphere
            for (double hpRadius = 0.0; hpRadius <= 0.07; hpRadius += 0.02) {
                for (double hpPhi = 0; hpPhi < 2.0 * Math.PI; hpPhi += 0.5) {
                    double hx = targetX + hpRadius * Math.cos(hpPhi);
                    double hy = targetY + hpRadius * Math.sin(hpPhi);
                    double hz = targetZ;
                    plotRawElement(hx, hy, hz, 0, -1, 0, 8, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 2: SOLID PEDESTAL BASE & ACCENTS
        // -------------------------------------------------------------
        double baseTopY = 0.42;
        double baseBottomY = 0.92;

        for (double hy = baseTopY; hy <= baseBottomY; hy += 0.02) {
            double progress = (hy - baseTopY) / (baseBottomY - baseTopY);
            double baseR = 0.42 + 0.32 * Math.pow(progress, 0.7); // Tapered conical silhouette

            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.04) {
                double lx = baseR * Math.cos(phi);
                double ly = hy;
                double lz = baseR * Math.sin(phi);

                int type = 1; // Default dark metallic base
                // Brass decorative accent rings near top and bottom
                if ((hy >= 0.43 && hy <= 0.46) || (hy >= 0.87 && hy <= 0.90)) {
                    type = 2;
                }

                plotRawElement(lx, ly, lz, Math.cos(phi), 0.3, Math.sin(phi), type, cosA, sinA, lightX, lightY, lightZ,
                        distanceToCamera, zBuffer);
            }
        }

        // Front Panel Power Toggle Switch
        for (double swY = 0.65; swY <= 0.73; swY += 0.015) {
            for (double swX = -0.04; swX <= 0.04; swX += 0.015) {
                double swZ = 0.58; // Protrudes on the front face
                plotRawElement(swX, swY, swZ, 0, 0, 1, 3, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 3: CHROMATIC TRANSMISSIVE GLASS GLOBE PASS
        // -------------------------------------------------------------
        for (int tIndex = 0; tIndex < 90; tIndex++) {
            double theta = (tIndex / 90.0) * Math.PI;
            if (theta > Math.PI * 0.72) continue; // Cutoff where glass seats into base pedestal

            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

            for (int pIndex = 0; pIndex < 180; pIndex++) {
                double phi = (pIndex / 180.0) * 2.0 * Math.PI;
                double localX = globeRadius * sinTheta * Math.cos(phi);
                double localY = globeCenterY + globeRadius * cosTheta;
                double localZ = globeRadius * sinTheta * Math.sin(phi);

                // Rotate glass node to face camera space
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

                        int r, g, b;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            // --- GLASS TRANSMISSION PASS: Blend glass tint with inner electric glow ---
                            double alpha = 0.22; // Subtrahend transparency factor
                            r = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_GLASS[0] * alpha);
                            g = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_GLASS[1] * alpha);
                            b = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_GLASS[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex];

                            // Dynamic specular catchlight highlights on outer curvature
                            if (luminance > 0.78) {
                                double sheen = 0.30;
                                r = (int) (r * (1.0 - sheen) + 255 * sheen);
                                g = (int) (g * (1.0 - sheen) + 255 * sheen);
                                b = (int) (b * (1.0 - sheen) + 255 * sheen);
                            }
                        } else {
                            // Empty canvas glass rim & atmosphere profile
                            double rim = 1.0 - Math.abs(gNz);
                            if (rim > 0.93) {
                                r = 210; g = 190; b = 255; finalChar = '░'; // Neon rim glint
                            } else if (luminance > 0.72) {
                                r = 180; g = 170; b = 240; finalChar = '░'; // Surface specular highlight
                            } else {
                                r = 25; g = 20; b = 40; finalChar = '.'; // Deep background dark node
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
                    String esc = String.format("\u001B[38;2;%d;%d;%dm", rawColorBuffer[i][0], rawColorBuffer[i][1],
                            rawColorBuffer[i][2]);
                    outputBuffer[i] = esc + rawCharBuffer[i] + RESET;
                } else {
                    outputBuffer[i] = " ";
                }
            }
        }

        A += 0.025; // Speed factor for high-frequency plasma animation
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

                int[] rgb = RGB_BASE;
                boolean fixedFullShade = false;

                if (surfaceType == 1) {
                    rgb = RGB_BASE;
                    asciiChar = '█';
                } else if (surfaceType == 2) {
                    rgb = RGB_BASE_TRIM;
                    asciiChar = '▓';
                } else if (surfaceType == 3) {
                    rgb = RGB_SWITCH;
                    asciiChar = '■';
                    fixedFullShade = true;
                } else if (surfaceType == 4) {
                    rgb = RGB_STEM;
                    asciiChar = '█';
                } else if (surfaceType == 5) {
                    rgb = RGB_CORE;
                    asciiChar = '█';
                    fixedFullShade = true; // Emissive core bulb glows continuously
                } else if (surfaceType == 6) {
                    rgb = RGB_ARC_INNER;
                    asciiChar = '*';
                    fixedFullShade = true; // High energy inner arc filament
                } else if (surfaceType == 7) {
                    rgb = RGB_ARC_OUTER;
                    asciiChar = '~';
                    fixedFullShade = true;
                } else if (surfaceType == 8) {
                    rgb = RGB_HOTSPOT;
                    asciiChar = 'o';
                    fixedFullShade = true; // Bright plasma contact point
                }

                rawCharBuffer[bufferIndex] = asciiChar;
                double shade = 0.45 + 0.55 * Math.max(0.0, luminance);
                double appliedShade = fixedFullShade ? 1.0 : shade;

                rawColorBuffer[bufferIndex][0] = (int) (rgb[0] * appliedShade);
                rawColorBuffer[bufferIndex][1] = (int) (rgb[1] * appliedShade);
                rawColorBuffer[bufferIndex][2] = (int) (rgb[2] * appliedShade);
            }
        }
    }
}