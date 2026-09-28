// TODO: Random Variants for lava lamp colors
// TODO: Fix camerawork and zooming

import java.util.Arrays;
import java.util.Random;

public class LavaLampLoader extends Loader {
    private static final StatusStage[] LAVA_STAGES = {
            new StatusStage(20, "Spinning brushed aluminum base:"),
            new StatusStage(50, "Filling vessel with translucent suspension fluid:"),
            new StatusStage(80, "Heating fluorescent paraffin wax compounds:"),
            new StatusStage(100, "3D Component Lava Lamp Active!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    // Raw RGB color arrays for analog transmissive alpha blending
    private static final int[] RGB_METAL = { 135, 140, 145 };       // Brushed aluminum base/cap
    private static final int[] RGB_WAX = { 255, 50, 90 };           // Hot fluorescent pink/magenta wax
    private static final int[] RGB_LIQUID = { 45, 15, 160 };        // Deep violet translucent fluid
    private static final int[] RGB_BULB = { 255, 230, 180 };        // Incandescent heat bulb

    private double A = 0.0;
    private final Random rand = new Random(1968);

    private static final int BLOB_COUNT = 5;
    private final double[] bX = new double[BLOB_COUNT];
    private final double[] bY = new double[BLOB_COUNT];
    private final double[] bZ = new double[BLOB_COUNT];
    private final double[] bR2 = new double[BLOB_COUNT]; // Blob radius squared (weight)
    private final double[] bPhase = new double[BLOB_COUNT];
    private final double[] bSpeed = new double[BLOB_COUNT];

    // Mirror screen buffers to track un-encoded raw character states for alpha coloring passes
    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public LavaLampLoader() {
        super(LAVA_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        setTargetFps(60);
        // Setup random phases and weights for the floating wax blobs
        double[] baseRadii = { 0.12, 0.09, 0.15, 0.07, 0.11 };
        for (int i = 0; i < BLOB_COUNT; i++) {
            bR2[i] = baseRadii[i] * baseRadii[i] * 0.45; // Pre-squared weight for metaball equation
            bPhase[i] = rand.nextDouble() * 2.0 * Math.PI;
            bSpeed[i] = 0.6 + rand.nextDouble() * 0.8;
        }

        // here is where we could set the random variant and choose the lava lamp colors
    }

    private double getGlassRadius(double y) {
        // Tapered classic lava lamp glass cone limits
        // Top cap boundary at y = -0.5, Base boundary at y = 0.45
        if (y < -0.5) return 0.11;
        if (y > 0.45) return 0.28;
        // Smooth sine-based tapering for the iconic curvy shape
        double t = (y - (-0.5)) / 0.95; 
        return 0.11 + (0.28 - 0.11) * Math.pow(Math.sin(t * Math.PI / 2.0), 1.2);
    }

    private double getWaxDensity(double x, double y, double z) {
        double density = 0.0;
        
        // Fixed bottom pooling puddle (over the heat bulb)
        density += 0.015 / (x * x + Math.pow(y - 0.50, 2) + z * z + 0.005);
        
        // Fixed top cooling surface tension
        density += 0.004 / (x * x + Math.pow(y + 0.55, 2) + z * z + 0.005);
        
        // Dynamic moving blobs
        for (int i = 0; i < BLOB_COUNT; i++) {
            double dx = x - bX[i];
            double dy = y - bY[i];
            double dz = z - bZ[i];
            density += bR2[i] / (dx * dx + dy * dy + dz * dz + 0.001);
        }
        return density;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        
        // Warm dramatic lighting mimicking the bottom-up incandescent bulb
        double lightX = 0.4, lightY = 0.8, lightZ = -0.4; 

        double midDist = 1.0;
        double distanceToCamera = midDist - 0.1 * Math.cos(A * 0.5);

        // Clear raw mirroring text buffers
        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // Update floating wax blob positions
        for (int i = 0; i < BLOB_COUNT; i++) {
            // Oscillate Y between bottom and top of the glass
            double t = (Math.sin(A * bSpeed[i] + bPhase[i]) + 1.0) / 2.0; 
            bY[i] = 0.40 - 0.85 * t; 
            
            // Slight horizontal drift based on height and time
            bX[i] = 0.06 * Math.cos(A * 1.1 + bPhase[i] * 2.0) * Math.sin(t * Math.PI);
            bZ[i] = 0.06 * Math.sin(A * 1.3 + bPhase[i] * 1.5) * Math.sin(t * Math.PI);
        }

        // -------------------------------------------------------------
        // STEP 1: INTERIOR WAX METABALL PASS (Volumetric Ray-Marching approximation)
        // -------------------------------------------------------------
        double step = 0.022; // Voxel resolution
        for (double ly = -0.48; ly <= 0.45; ly += step) {
            double maxR = getGlassRadius(ly) - 0.015;
            if (maxR < 0.01) continue;
            
            for (double lx = -maxR; lx <= maxR; lx += step) {
                for (double lz = -maxR; lz <= maxR; lz += step) {
                    // Only evaluate within the cylindrical bound of the glass
                    if (lx * lx + lz * lz < maxR * maxR) {
                        double density = getWaxDensity(lx, ly, lz);
                        
                        // Isosurface boundary threshold
                        if (density > 1.0 && density < 1.4) {
                            // Calculate surface normal via spatial density gradient
                            double eps = 0.01;
                            double nx = getWaxDensity(lx - eps, ly, lz) - getWaxDensity(lx + eps, ly, lz);
                            double ny = getWaxDensity(lx, ly - eps, lz) - getWaxDensity(lx, ly + eps, lz);
                            double nz = getWaxDensity(lx, ly, lz - eps) - getWaxDensity(lx, ly, lz + eps);
                            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                            
                            if (len > 0.0001) {
                                plotRawElement(lx, ly, lz, nx / len, ny / len, nz / len, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 2: SOLID ALUMINUM BASE & CAP
        // -------------------------------------------------------------
        
        // Aluminum Cap
        for (double hy = -0.75; hy <= -0.50; hy += 0.015) {
            double r = 0.04 + (0.11 - 0.04) * ((hy - (-0.75)) / 0.25);
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.1) {
                double lx = r * Math.cos(phi), ly = hy, lz = r * Math.sin(phi);
                plotRawElement(lx, ly, lz, Math.cos(phi), 0.2, Math.sin(phi), 1, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }
        
        // Aluminum Base Pedestal
        for (double hy = 0.45; hy <= 0.95; hy += 0.015) {
            double r = 0.28 + (0.40 - 0.28) * ((hy - 0.45) / 0.50);
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.06) {
                double lx = r * Math.cos(phi), ly = hy, lz = r * Math.sin(phi);
                plotRawElement(lx, ly, lz, Math.cos(phi), 0.2, Math.sin(phi), 1, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 3: CHROMATIC TRANSMISSIVE LIQUID/GLASS PASS
        // -------------------------------------------------------------
        for (double hy = -0.50; hy <= 0.45; hy += 0.012) {
            double r = getGlassRadius(hy);
            
            // Numerical derivative for glass normal tapering
            double dr = (getGlassRadius(hy + 0.001) - getGlassRadius(hy - 0.001)) / 0.002;
            double len = Math.sqrt(dr * dr + 1.0);
            double nyU = 1.0 / len;
            double nrU = -dr / len;

            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.06) {
                double localX = r * Math.cos(phi);
                double localY = hy;
                double localZ = r * Math.sin(phi);

                double rx = localX * cosA + localZ * sinA;
                double ry = localY;
                double rz = -localX * sinA + localZ * cosA;

                double ooz = 1.0 / (rz + distanceToCamera);
                int xp = (int) (40 + 36 * ooz * rx * 1.2);
                int yp = (int) (11 + 17 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int bufferIndex = xp + 80 * yp;

                    if (ooz > zBuffer[bufferIndex]) {
                        double nxU = nrU * Math.cos(phi);
                        double nzU = nrU * Math.sin(phi);

                        double gNx = nxU * cosA + nzU * sinA;
                        double gNy = nyU;
                        double gNz = -nxU * sinA + nzU * cosA;
                        
                        double luminance = gNx * 0.577 + gNy * -0.707 + gNz * -0.408;

                        int rCol, gCol, bCol;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            // --- FLUID TINT INJECTION: Wash the internal wax with the suspension fluid ---
                            double alpha = 0.45; // Liquid density
                            rCol = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_LIQUID[0] * alpha);
                            gCol = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_LIQUID[1] * alpha);
                            bCol = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_LIQUID[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex];

                            // Glass specular highlight overlapping the wax
                            if (luminance > 0.85) {
                                double sheen = 0.40;
                                rCol = (int) (rCol * (1.0 - sheen) + 255 * sheen);
                                gCol = (int) (gCol * (1.0 - sheen) + 255 * sheen);
                                bCol = (int) (bCol * (1.0 - sheen) + 255 * sheen);
                                finalChar = '░';
                            }
                        } else {
                            // Empty liquid background profile 
                            double rim = 1.0 - Math.abs(gNz); 
                            if (rim > 0.94) {
                                rCol = 200; gCol = 190; bCol = 255; finalChar = '░'; // Glass edge highlight
                            } else if (luminance > 0.78) {
                                rCol = 160; gCol = 140; bCol = 255; finalChar = '░'; // Ambient room reflection
                            } else {
                                // Deep colored liquid filling the empty space
                                rCol = 25; gCol = 5; bCol = 65; finalChar = '.'; 
                            }
                        }

                        // Add bottom-up bulb illumination to the glass
                        double bottomGlow = Math.max(0.0, 1.0 - ((hy - 0.2) / 0.25));
                        if (bottomGlow > 0) {
                            rCol = Math.min(255, rCol + (int)(RGB_BULB[0] * 0.15 * bottomGlow));
                            gCol = Math.min(255, gCol + (int)(RGB_BULB[1] * 0.15 * bottomGlow));
                            bCol = Math.min(255, bCol + (int)(RGB_BULB[2] * 0.15 * bottomGlow));
                        }

                        String esc = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, rCol)),
                                Math.max(0, Math.min(255, gCol)), Math.max(0, Math.min(255, bCol)));
                        outputBuffer[bufferIndex] = esc + finalChar + RESET;
                        zBuffer[bufferIndex] = ooz; // Claim depth to prevent overdraw
                    }
                }
            }
        }
        
        // -------------------------------------------------------------
        // STEP 4: FLUSH BASE & INTERNAL BUFFER TO TERMINAL
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
        
        A += 0.035; // Gentle rotation and wax flow speed
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
                
                int[] rgb = RGB_METAL;
                boolean fixedFullShade = false;
                
                if (surfaceType == 1) {
                    rgb = RGB_METAL;
                    asciiChar = '█';
                } else if (surfaceType == 2) {
                    rgb = RGB_WAX;
                    asciiChar = '▓'; // Dense texture for the wax
                }

                // Add bottom-up bulb glow specifically for the wax surface based on height
                int rCol = rgb[0], gCol = rgb[1], bCol = rgb[2];
                if (surfaceType == 2) {
                    double bulbGlow = Math.max(0.0, 1.0 - ((localY - 0.2) / 0.35)); // Fades out as it rises
                    rCol = (int) Math.min(255, rCol + RGB_BULB[0] * 0.4 * bulbGlow);
                    gCol = (int) Math.min(255, gCol + RGB_BULB[1] * 0.4 * bulbGlow);
                    bCol = (int) Math.min(255, bCol + RGB_BULB[2] * 0.4 * bulbGlow);
                    
                    // Wax emits its own subsurface light, keeping minimum brightness high
                    luminance = Math.max(0.2, luminance + 0.3 * bulbGlow);
                }

                rawCharBuffer[bufferIndex] = asciiChar;
                double shade = 0.35 + 0.65 * Math.max(0.0, luminance);
                double appliedShade = fixedFullShade ? 1.0 : shade;
                
                rawColorBuffer[bufferIndex][0] = (int) (rCol * appliedShade);
                rawColorBuffer[bufferIndex][1] = (int) (gCol * appliedShade);
                rawColorBuffer[bufferIndex][2] = (int) (bCol * appliedShade);
            }
        }
    }
}