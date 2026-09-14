//TODO: Add Stars or Moons to the cloth at the bottom under the crystal ball

import java.util.Random;
import java.util.Arrays;

public class CrystalBallLoader extends Loader {
    private static final StatusStage[] CRYSTAL_STAGES = {
            new StatusStage(20, "Forging obsidian pedestal:"),
            new StatusStage(50, "Blowing quartz crystal sphere:"),
            new StatusStage(80, "Igniting inner nebula core:"),
            new StatusStage(100, "3D Component Crystal Ball Active!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    // Raw RGB color arrays for analog transmissive alpha blending
    private static final int[] RGB_GLASS = { 180, 120, 255 }; // Amethyst magic tint
    private static final int[] RGB_CORE = { 255, 200, 255 }; // Blinding pink/white core
    private static final int[] RGB_NEBULA = { 140, 60, 220 }; // Deep swirling purple
    private static final int[] RGB_STAR = { 220, 240, 255 }; // Starlight cyan
    private static final int[] RGB_BASE = { 35, 30, 45 }; // Polished obsidian
    private static final int[] RGB_GOLD = { 230, 195, 90 }; // Arcane brass engravings

    private double A = 0.0;
    private final Random rand = new Random(2026);

    private static final int STAR_COUNT = 85;
    private final double[] sX = new double[STAR_COUNT];
    private final double[] sY = new double[STAR_COUNT];
    private final double[] sZ = new double[STAR_COUNT];
    private final double[] sSpeed = new double[STAR_COUNT];

    // Mirror screen buffers to track un-encoded raw character states for alpha coloring passes
    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public CrystalBallLoader() {
        super(CRYSTAL_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // Distribute stars randomly within the inner sphere volume
        for (int i = 0; i < STAR_COUNT; i++) {
            double radius = 0.1 + rand.nextDouble() * 0.65;
            double theta = rand.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rand.nextDouble() - 1.0);
            
            sX[i] = radius * Math.sin(phi) * Math.cos(theta);
            sY[i] = radius * Math.sin(phi) * Math.sin(theta);
            sZ[i] = radius * Math.cos(phi);
            sSpeed[i] = 1.0 + rand.nextDouble() * 2.5;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;
        double globeRadius = 0.85;

        double timeStep = System.currentTimeMillis() / 1500.0;
        double sinTimeStep = Math.sin(timeStep);
        double midDist = 1.9;
        double distanceToCamera = midDist + 1.0 * Math.signum(sinTimeStep) * Math.sqrt(Math.abs(sinTimeStep));

        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // -------------------------------------------------------------
        // STEP 1: INTERIOR CONTENT (Magical Swirling Galaxy)
        // -------------------------------------------------------------

        // INTERIOR A: The dense glowing core
        double corePulse = 0.12 + 0.03 * Math.sin(A * 8.0);
        for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.4) {
            for (double theta = 0; theta < Math.PI; theta += 0.4) {
                double cx = corePulse * Math.sin(theta) * Math.cos(phi);
                double cy = corePulse * Math.cos(theta);
                double cz = corePulse * Math.sin(theta) * Math.sin(phi);
                plotRawElement(cx, cy, cz, 0, 0, 0, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // INTERIOR B: Swirling Nebula Arms (Parametric Spirals)
        int armCount = 3;
        for (double r = 0.15; r < 0.75; r += 0.02) {
            for (int arm = 0; arm < armCount; arm++) {
                // Angle winds outward and rotates over time A
                double angle = r * 12.0 - A * 3.5 + (arm * 2.0 * Math.PI / armCount);
                
                double lx = r * Math.cos(angle);
                double lz = r * Math.sin(angle);
                
                // Add a vertical wave to the galactic plane so it's not totally flat
                double ly = 0.25 * r * Math.sin(angle * 2.0 + A * 2.0); 

                // Thicken the arms slightly
                plotRawElement(lx, ly, lz, 0, 0, 0, 3, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                plotRawElement(lx * 0.95, ly + 0.04, lz * 0.95, 0, 0, 0, 3, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // INTERIOR C: Free-floating starlight particles
        for (int i = 0; i < STAR_COUNT; i++) {
            // Rotate particles around the Y axis over time to match the galaxy swirl
            double currentAngle = A * sSpeed[i];
            double rx = sX[i] * Math.cos(currentAngle) - sZ[i] * Math.sin(currentAngle);
            double ry = sY[i] + 0.05 * Math.sin(A * 4.0 + i); // slight bob
            double rz = sX[i] * Math.sin(currentAngle) + sZ[i] * Math.cos(currentAngle);
            
            plotRawElement(rx, ry, rz, 0, 0, 0, 4, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
        }

        // -------------------------------------------------------------
        // STEP 2: EXTERIOR ORNATE PEDESTAL
        // -------------------------------------------------------------
        // Base structure flaring outwards
        for (double h = 0.60; h <= 1.05; h += 0.02) {
            // Taper and flare math: narrow at the glass joint, wide at the bottom
            double t = (h - 0.60) / 0.45; 
            double rBase = 0.65 - 0.3 * Math.sin(t * Math.PI) + 0.4 * t * t;

            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.04) {
                // Add grooved arcane textures to the obsidian
                double groove = 0.02 * Math.sin(phi * 8.0);
                double lx = (rBase + groove) * Math.cos(phi);
                double lz = (rBase + groove) * Math.sin(phi);

                int type = 5; // Obsidian
                if (h > 1.02 || (h > 0.72 && h < 0.76)) {
                    type = 6; // Gold rim/bands
                    lx = (rBase + 0.02) * Math.cos(phi); // extrude gold slightly
                    lz = (rBase + 0.02) * Math.sin(phi);
                }

                plotRawElement(lx, h, lz, Math.cos(phi), 0.2, Math.sin(phi), type, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 3: CHROMATIC TRANSMISSIVE CRYSTAL DOME
        // -------------------------------------------------------------
        for (int tIndex = 0; tIndex < 90; tIndex++) {
            double theta = (tIndex / 90.0) * Math.PI;
            if (theta > Math.PI * 0.75) continue; // Base cutout
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

            for (int pIndex = 0; pIndex < 180; pIndex++) {
                double phi = (pIndex / 180.0) * 2.0 * Math.PI;
                double localX = globeRadius * sinTheta * Math.cos(phi);
                double localY = globeRadius * cosTheta;
                double localZ = globeRadius * sinTheta * Math.sin(phi);

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
                            // --- MAGIC TINT INJECTION: heavy purple wash ---
                            double alpha = 0.40; 
                            r = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_GLASS[0] * alpha);
                            g = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_GLASS[1] * alpha);
                            b = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_GLASS[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex]; 

                            // Glass specular highlight
                            if (luminance > 0.85) {
                                double sheen = 0.40;
                                r = (int) (r * (1.0 - sheen) + 255 * sheen);
                                g = (int) (g * (1.0 - sheen) + 255 * sheen);
                                b = (int) (b * (1.0 - sheen) + 255 * sheen);
                            }
                        } else {
                            // Empty canvas glass layer
                            double rim = 1.0 - Math.abs(gNx);
                            if (rim <= 0.95 && luminance > 0.78) {
                                r = 210; g = 180; b = 255; finalChar = '░'; // soft highlight
                            } else {
                                r = 35; g = 20; b = 55; finalChar = '.'; // deep background crystal
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
                    String esc = String.format("\u001B[38;2;%d;%d;%dm", rawColorBuffer[i][0], rawColorBuffer[i][1], rawColorBuffer[i][2]);
                    outputBuffer[i] = esc + rawCharBuffer[i] + RESET;
                } else {
                    outputBuffer[i] = " ";
                }
            }
        }
        A += 0.015; // Rotate galaxy and camera rig
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

                if (surfaceType == 2) { // Core
                    rgb = RGB_CORE;
                    asciiChar = '█';
                    fixedFullShade = true;
                } else if (surfaceType == 3) { // Nebula Arm
                    rgb = RGB_NEBULA;
                    asciiChar = '▓';
                    fixedFullShade = true;
                } else if (surfaceType == 4) { // Stars
                    rgb = RGB_STAR;
                    asciiChar = '✦';
                    fixedFullShade = true;
                } else if (surfaceType == 5) { // Obsidian Base
                    rgb = RGB_BASE;
                    asciiChar = '█';
                } else if (surfaceType == 6) { // Gold Trim
                    rgb = RGB_GOLD;
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