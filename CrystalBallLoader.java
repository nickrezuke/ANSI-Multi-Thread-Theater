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
        
        // Increased ball size to make it the dominant focal point
        double globeRadius = 0.94;

        double timeStep = System.currentTimeMillis() / 1500.0;
        double sinTimeStep = Math.sin(timeStep);
        double midDist = 1.7;
        double distanceToCamera = midDist + Math.signum(sinTimeStep) * Math.sqrt(Math.abs(sinTimeStep));

        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // -------------------------------------------------------------
        // STEP 1: INTERIOR CONTENT (Magical Swirling Galaxy)
        // -------------------------------------------------------------
        double corePulse = 0.12 + 0.03 * Math.sin(A * 8.0);
        for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.2) {
            for (double theta = 0; theta < Math.PI; theta += 0.2) {
                double cx = corePulse * Math.sin(theta) * Math.cos(phi);
                double cy = corePulse * Math.cos(theta);
                double cz = corePulse * Math.sin(theta) * Math.sin(phi);
                plotRawElement(cx, cy, cz, 0, 0, 0, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        int armCount = 3;
        for (double r = 0.15; r < 0.75; r += 0.02) {
            for (int arm = 0; arm < armCount; arm++) {
                double angle = r * 12.0 - A * 3.5 + (arm * 2.0 * Math.PI / armCount);
                double lx = r * Math.cos(angle);
                double lz = r * Math.sin(angle);
                double ly = 0.25 * r * Math.sin(angle * 2.0 + A * 2.0); 

                plotRawElement(lx, ly, lz, 0, 0, 0, 3, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                plotRawElement(lx * 0.95, ly + 0.04, lz * 0.95, 0, 0, 0, 3, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        for (int i = 0; i < STAR_COUNT; i++) {
            double currentAngle = A * sSpeed[i];
            double rx = sX[i] * Math.cos(currentAngle) - sZ[i] * Math.sin(currentAngle);
            double ry = sY[i] + 0.05 * Math.sin(A * 4.0 + i);
            double rz = sX[i] * Math.sin(currentAngle) + sZ[i] * Math.cos(currentAngle);
            
            plotRawElement(rx, ry, rz, 0, 0, 0, 4, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
        }

        // -------------------------------------------------------------
        // STEP 2: EXTERIOR PEDESTAL (CLASSIC PROPORTIONS)
        // -------------------------------------------------------------
        // Re-proportioned so the base fits nicely under the massive crystal ball
        for (double h = 0.65; h <= 1.30; h += 0.015) {
            double t = (h - 0.65) / 0.65; 
            
            // Narrowed base: Top radius is 0.58, flares out to max 0.90 at the bottom
            // This ensures the base is always strictly narrower than the 0.94 ball
            double rBase = 0.58 - 0.12 * Math.sin(t * Math.PI) + 0.32 * t * t;

            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.015) {
                boolean isCelestialMotif = false;
                
                // Centered perfectly in the middle of the new base height
                double dy = h - 0.975; 

                double dPhi0 = Math.min(phi, 2.0 * Math.PI - phi);
                double dPhi90 = Math.abs(phi - Math.PI / 2.0);
                double dPhi180 = Math.abs(phi - Math.PI);
                double dPhi270 = Math.abs(phi - 3.0 * Math.PI / 2.0);

                // BOLD CRESCENT MOONS at 0 and 180 degrees
                if (dPhi0 < 0.6 || dPhi180 < 0.6) {
                    double signedDPhi = (dPhi0 < 0.6) ? (phi > Math.PI ? phi - 2.0 * Math.PI : phi) : (phi - Math.PI);
                    double dx = signedDPhi * rBase;
                    
                    double rSq = dx * dx + dy * dy;
                    double cutSq = (dx - 0.06) * (dx - 0.06) + dy * dy;
                    
                    if (rSq < 0.055 && cutSq > 0.03) {
                        isCelestialMotif = true;
                    }
                }
                // BOLD 4-POINT STARS at 90 and 270 degrees
                else if (dPhi90 < 0.6 || dPhi270 < 0.6) {
                    double signedDPhi = (dPhi90 < 0.6) ? (phi - Math.PI / 2.0) : (phi - 3.0 * Math.PI / 2.0);
                    double dx = signedDPhi * rBase;
                    
                    double absX = Math.abs(dx);
                    double absY = Math.abs(dy);
                    
                    if ((absX < 0.045 && absY < 0.22) || // Tall vertical beam
                        (absX < 0.22 && absY < 0.045) || // Wide horizontal beam
                        (absX + absY < 0.14)) {          // Solid core
                        isCelestialMotif = true;
                    }
                }

                double groove = 0.02 * Math.sin(phi * 8.0);
                double lx = (rBase + groove) * Math.cos(phi);
                double lz = (rBase + groove) * Math.sin(phi);

                int type = 5; // Obsidian base
                
                if (h > 1.25 || (h > 0.70 && h < 0.73)) {
                    type = 6; // Delicate Gold trim bands
                    lx = (rBase + 0.02) * Math.cos(phi); 
                    lz = (rBase + 0.02) * Math.sin(phi);
                }
                
                if (isCelestialMotif) {
                    type = 7; // Solid Gold Celestial Block
                    lx = (rBase + 0.05) * Math.cos(phi); // Extrude heavily outward
                    lz = (rBase + 0.05) * Math.sin(phi);
                }

                plotRawElement(lx, h, lz, Math.cos(phi), 0.2, Math.sin(phi), type, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 3: CHROMATIC TRANSMISSIVE CRYSTAL DOME
        // -------------------------------------------------------------
        for (int tIndex = 0; tIndex < 90; tIndex++) {
            double theta = (tIndex / 90.0) * Math.PI;
            if (theta > Math.PI * 0.85) continue; // Leaves a tiny cutout just for the stand pipe
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
                
                // Adjusted Camera pan to perfectly center the newly proportioned object
                int yp = (int) (10 + 15 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int bufferIndex = xp + 80 * yp;

                    if (ooz > zBuffer[bufferIndex]) {
                        double gNx = sinTheta * Math.cos(phi) * cosA + sinTheta * Math.sin(phi) * sinA;
                        double gNy = cosTheta;
                        double luminance = gNx * lightX + gNy * lightY;

                        int r, g, b;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            double alpha = 0.40; 
                            r = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_GLASS[0] * alpha);
                            g = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_GLASS[1] * alpha);
                            b = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_GLASS[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex]; 

                            if (luminance > 0.85) {
                                double sheen = 0.40;
                                r = (int) (r * (1.0 - sheen) + 255 * sheen);
                                g = (int) (g * (1.0 - sheen) + 255 * sheen);
                                b = (int) (b * (1.0 - sheen) + 255 * sheen);
                            }
                        } else {
                            double rim = 1.0 - Math.abs(gNx);
                            if (rim <= 0.95 && luminance > 0.78) {
                                r = 210; g = 180; b = 255; finalChar = '░';
                            } else {
                                r = 35; g = 20; b = 55; finalChar = '.';
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
        A += 0.015;
    }

    private void plotRawElement(double localX, double localY, double localZ, double rNx, double rNy, double rNz,
            int surfaceType, double cosA, double sinA, double lightX, double lightY, double lightZ,
            double distanceToCamera, double[] zBuffer) {
        
        double rx = localX * cosA + localZ * sinA, ry = localY, rz = -localX * sinA + localZ * cosA;
        double nx = rNx * cosA + rNz * sinA, ny = rNy, nz = -rNx * sinA + rNz * cosA;
        
        // Backface Culling: hides the rear of the pedestal from drawing through the ball
        if (surfaceType >= 5 && nz > 0.05) {
            return; 
        }

        double ooz = 1.0 / (rz + distanceToCamera);
        int xp = (int) (40 + 36 * ooz * rx * 1.2);
        
        // Centered Camera pan
        int yp = (int) (10 + 15 * ooz * ry);

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

                if (surfaceType == 2) { 
                    rgb = RGB_CORE; asciiChar = '█'; fixedFullShade = true;
                } else if (surfaceType == 3) { 
                    rgb = RGB_NEBULA; asciiChar = '▓'; fixedFullShade = true;
                } else if (surfaceType == 4) { 
                    rgb = RGB_STAR; asciiChar = '✦'; fixedFullShade = true;
                } else if (surfaceType == 5) { 
                    rgb = RGB_BASE; asciiChar = '█';
                } else if (surfaceType == 6) { 
                    rgb = RGB_GOLD;
                } else if (surfaceType == 7) { 
                    rgb = RGB_GOLD; asciiChar = '█'; fixedFullShade = true;
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