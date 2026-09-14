//TODO: Fix the zooming and rotation speed to get better viewing angles
//TODO: Increase the sampling granularity of the cork and etc I can see the "sparse grid" from step size being too big

import java.util.Arrays;

public class ShipInBottleLoader extends Loader {
    private static final StatusStage[] BOTTLE_STAGES = {
            new StatusStage(20, "Blowing cylindrical glass bottle:"),
            new StatusStage(50, "Carving miniature wooden hull:"),
            new StatusStage(80, "Rigging tiny sails & filling sea:"),
            new StatusStage(100, "3D Component Ship in a Bottle Active!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    // Raw RGB color arrays for analog transmissive alpha blending
    private static final int[] RGB_STAND = { 65, 35, 20 }; // Mahogany wood cradle
    private static final int[] RGB_BOTTLE = { 170, 245, 230 }; // Translucent sea-glass cyan
    private static final int[] RGB_CORK = { 180, 130, 80 }; // Porous cork
    private static final int[] RGB_WATER = { 20, 95, 175 }; // Deep ocean blue
    private static final int[] RGB_WAVES = { 180, 220, 245 }; // Frothy wave caps
    private static final int[] RGB_HULL = { 90, 50, 25 }; // Dark timber hull
    private static final int[] RGB_DECK = { 150, 110, 70 }; // Weathered deck planks
    private static final int[] RGB_MAST = { 120, 80, 50 }; // Pine masts
    private static final int[] RGB_SAIL = { 240, 235, 220 }; // Canvas sails
    private static final int[] RGB_FLAG = { 220, 45, 45 }; // Crimson pennant
    private static final int[] RGB_GOLD = { 210, 175, 75 }; // Brass trim accents
    private static final int[] RGB_GULL = { 255, 255, 255 }; // White seagulls

    private double A = 0.0;

    // Mirror screen buffers to track un-encoded raw character states for alpha coloring passes
    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public ShipInBottleLoader() {
        super(BOTTLE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // No persistent particle arrays needed; dynamic sine-wave generation used in render loop
    }

    private double getBottleRadius(double x) {
        if (x < -0.8 || x > 0.75) return 0.0; // Outside glass bounds
        if (x < -0.5) {
            // Hemispherical base of the bottle
            double dx = x + 0.5;
            double rSq = 0.45 * 0.45 - dx * dx;
            return rSq > 0 ? Math.sqrt(rSq) : 0.0;
        }
        if (x <= 0.3) {
            return 0.45; // Main cylindrical body
        }
        if (x <= 0.6) {
            // Smooth cosine taper for the bottle neck
            double t = (x - 0.3) / 0.3;
            return 0.15 + (0.45 - 0.15) * (0.5 + 0.5 * Math.cos(t * Math.PI));
        }
        return 0.15; // Straight neck
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        double timeStep = System.currentTimeMillis() / 1500.0;
        double sinTimeStep = Math.sin(timeStep);
        double midDist = 1.9;
        double swingFactor = 1.0;
        double distanceToCamera = midDist + swingFactor * Math.signum(sinTimeStep) * Math.sqrt(Math.abs(sinTimeStep));

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

        // INTERIOR A: The Ocean Water inside the bottle
        double waterY = 0.18;
        for (double wx = -0.75; wx <= 0.65; wx += 0.015) {
            double rSq = Math.pow(getBottleRadius(wx), 2) - waterY * waterY;
            if (rSq > 0) {
                double maxZ = Math.sqrt(rSq);
                for (double wz = -maxZ; wz <= maxZ; wz += 0.015) {
                    // Check if water point is hidden inside the ship's hull footprint
                    double L = wx - (-0.1);
                    double hullMaxZ = 0.14 * (1.0 - (L * L) / 0.1225);
                    double hullZAtWater = hullMaxZ * (1.0 - ((waterY + 0.05) / 0.3));
                    if (hullZAtWater > 0 && Math.abs(wz) < hullZAtWater) continue;

                    // Apply slight rolling wave height variation
                    double wy = waterY + 0.01 * Math.sin(wx * 20.0 + A * 4.0) * Math.cos(wz * 20.0 + A * 3.0);
                    int type = (Math.sin(wx * 25.0 + A * 5.0) > 0.85) ? 11 : 3; // Occasional whitecap
                    plotRawElement(wx, wy, wz, 0, -1, 0, type, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                }
            }
        }

        // INTERIOR B: Carved Ship Hull
        for (double hx = -0.45; hx <= 0.25; hx += 0.01) {
            double L = hx - (-0.1); // Ship centered at X = -0.1
            double maxZ = 0.14 * (1.0 - (L * L) / 0.1225); // Parabolic hull tapering
            if (maxZ <= 0) continue;

            for (double hy = -0.05; hy <= 0.22; hy += 0.01) { // Deck at -0.05, Keel at 0.22
                double zAtY = maxZ * (1.0 - ((hy + 0.05) / 0.3));
                for (double hz = -zAtY; hz <= zAtY; hz += 0.01) {
                    // Only draw exterior shell to prevent terminal rendering overdraw
                    boolean isSurface = Math.abs(hz) > zAtY - 0.015 || hy < -0.04 || hy > 0.21 || hx > 0.24 || hx < -0.44;
                    if (isSurface) {
                        int type = (hy < -0.03) ? 5 : 4; // Deck vs Hull
                        plotRawElement(hx, hy, hz, hx > -0.1 ? 1 : -1, 0, hz > 0 ? 1 : -1, type, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                    }
                }
            }
        }

        // INTERIOR C: Masts, Sails & Flags
        double[] mastsX = { -0.25, 0.05 }; // Fore mast and Main mast
        double[] mastsTop = { -0.35, -0.42 };
        for (int m = 0; m < 2; m++) {
            // Mast pole
            for (double my = mastsTop[m]; my <= -0.05; my += 0.01) {
                for (double mz = -0.01; mz <= 0.01; mz += 0.01) {
                    plotRawElement(mastsX[m], my, mz, 1, 0, mz, 6, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                }
            }
            // Billowing Sail (curved forward via sine wave)
            for (double sy = mastsTop[m] + 0.04; sy <= -0.05; sy += 0.01) {
                double progress = (sy - (mastsTop[m] + 0.04)) / (-0.05 - (mastsTop[m] + 0.04));
                double bowX = 0.12 * Math.sin(progress * Math.PI); // Wind pushes sail forward (+X)
                double sWidth = 0.03 + 0.16 * progress; // Sail widens towards deck
                for (double sz = -sWidth; sz <= sWidth; sz += 0.015) {
                    double sx = mastsX[m] + bowX;
                    plotRawElement(sx, sy, sz, 1, -0.2, sz, 7, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                }
            }
            // Fluttering Flag
            for (double fx = mastsX[m]; fx >= mastsX[m] - 0.10; fx -= 0.01) {
                double fz = 0.015 * Math.sin(fx * 25.0 + A * 6.0); // Wind flutter
                double flagHeight = (fx - (mastsX[m] - 0.10)) * 0.4;
                for (double fy = mastsTop[m]; fy <= mastsTop[m] + flagHeight; fy += 0.01) {
                    plotRawElement(fx, fy, fz, -1, 0, 1, 8, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                }
            }
        }

        // INTERIOR D: Seagulls circling the masts
        for (int i = 0; i < 5; i++) {
            double angle = A * (1.8 + i * 0.3) + i * 2.0;
            double radius = 0.12 + 0.04 * Math.sin(i + A);
            double bX = -0.1 + radius * Math.cos(angle);
            double bZ = radius * Math.sin(angle);
            double bY = -0.18 + 0.06 * Math.cos(A * 3.0 + i);
            plotRawElement(bX, bY, bZ, 0, -1, 0, 10, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
        }

        // INTERIOR E: Cork sealing the bottle neck
        for (double cx = 0.70; cx <= 0.85; cx += 0.015) {
            for (double cphi = 0; cphi < 2.0 * Math.PI; cphi += 0.1) {
                double cr = 0.135;
                double cy = cr * Math.cos(cphi);
                double cz = cr * Math.sin(cphi);
                plotRawElement(cx, cy, cz, 1, Math.cos(cphi), Math.sin(cphi), 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
            if (cx >= 0.835) { // Outer flat end of the cork
                for (double cr = 0.0; cr <= 0.135; cr += 0.015) {
                    for (double cphi = 0; cphi < 2.0 * Math.PI; cphi += 0.2) {
                        plotRawElement(cx, cr * Math.cos(cphi), cr * Math.sin(cphi), 1, 0, 0, 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 2: EXTERIOR WOODEN PEDESTAL STAND
        // -------------------------------------------------------------
        for (double h = 0.35; h <= 0.65; h += 0.015) {
            for (double sx = -0.65; sx <= 0.50; sx += 0.015) {
                for (double sz = -0.25; sz <= 0.25; sz += 0.015) {
                    // Create two cradles and a connecting base plate
                    boolean inCradle1 = (sx >= -0.55 && sx <= -0.35);
                    boolean inCradle2 = (sx >= 0.15 && sx <= 0.35);
                    boolean isBasePlate = (h >= 0.58);
                    
                    if (inCradle1 || inCradle2 || isBasePlate) {
                        double rBottle = getBottleRadius(sx);
                        // Prevent the stand from rendering inside the glass bottle
                        if (h * h + sz * sz > rBottle * rBottle + 0.005) {
                            boolean isEdge = h > 0.63 || h < 0.38 || Math.abs(sz) > 0.23 || sx < -0.63 || sx > 0.48;
                            if (isEdge) {
                                int type = (h > 0.62 && (Math.abs(sx - (-0.45)) < 0.11 || Math.abs(sx - 0.25) < 0.11)) ? 12 : 1;
                                plotRawElement(sx, h, sz, sx > 0 ? 1 : -1, h > 0.6 ? 1 : -1, sz > 0 ? 1 : -1, type, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 3: CHROMATIC TRANSMISSIVE BOTTLE GLASS (Color Filter Overwrite)
        // -------------------------------------------------------------
        for (int xIndex = 0; xIndex < 160; xIndex++) {
            double bx = -0.8 + xIndex * 1.55 / 160.0;
            double r = getBottleRadius(bx);
            if (r < 0.01) continue;
            
            // Numerical derivative for surface normals
            double dr = (getBottleRadius(bx + 0.001) - getBottleRadius(bx - 0.001)) / 0.002;

            for (int p = 0; p < 180; p++) {
                double phi = (p / 180.0) * 2.0 * Math.PI;
                double localX = bx;
                double localY = r * Math.cos(phi);
                double localZ = r * Math.sin(phi);

                // Rotate bottle points to camera coordinates
                double rx = localX * cosA + localZ * sinA;
                double ry = localY;
                double rz = -localX * sinA + localZ * cosA;

                double ooz = 1.0 / (rz + distanceToCamera);
                int xp = (int) (40 + 36 * ooz * rx * 1.2);
                int yp = (int) (11 + 17 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int bufferIndex = xp + 80 * yp;

                    // Only affect screen positions where glass is closer than previously drawn background nodes
                    if (ooz > zBuffer[bufferIndex]) {
                        // Normalize local unrotated surface gradient
                        double len = Math.sqrt(dr * dr + 1.0);
                        double nxU = -dr / len;
                        double nyU = Math.cos(phi) / len;
                        double nzU = Math.sin(phi) / len;

                        // Rotate normal to camera space for dynamic lighting
                        double gNx = nxU * cosA + nzU * sinA;
                        double gNy = nyU;
                        double luminance = gNx * lightX + gNy * lightY;

                        int rCol, gCol, bCol;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            // --- GLASS TINT INJECTION: gentle sea-green wash over the interior scene ---
                            double alpha = 0.28; 
                            rCol = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_BOTTLE[0] * alpha);
                            gCol = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_BOTTLE[1] * alpha);
                            bCol = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_BOTTLE[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex]; 

                            // Soft specular sheen on catch-light spots
                            if (luminance > 0.82) {
                                double sheen = 0.35;
                                rCol = (int) (rCol * (1.0 - sheen) + 255 * sheen);
                                gCol = (int) (gCol * (1.0 - sheen) + 255 * sheen);
                                bCol = (int) (bCol * (1.0 - sheen) + 255 * sheen);
                            }
                        } else {
                            // Empty canvas glass layer background profiles
                            double rim = 1.0 - Math.abs(gNx);
                            if (rim > 0.94) {
                                rCol = 220; gCol = 245; bCol = 235; finalChar = '░'; // narrow rim glint
                            } else if (luminance > 0.75) {
                                rCol = 185; gCol = 235; bCol = 225; finalChar = '░'; // gentle specular highlight
                            } else {
                                rCol = 25; gCol = 45; bCol = 40; finalChar = '.'; // deep background shadow nodes
                            }
                        }

                        String esc = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, rCol)),
                                Math.max(0, Math.min(255, gCol)), Math.max(0, Math.min(255, bCol)));
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
        A += 0.012; // Slow continuous rotation to admire the ship profile
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
                
                int[] rgb = RGB_STAND;
                boolean fixedFullShade = false;
                
                if (surfaceType == 1) {
                    rgb = RGB_STAND;
                    asciiChar = '█';
                } else if (surfaceType == 2) {
                    rgb = RGB_CORK;
                    asciiChar = '▓';
                } else if (surfaceType == 3) {
                    rgb = RGB_WATER;
                    asciiChar = '~';
                } else if (surfaceType == 4) {
                    rgb = RGB_HULL;
                    asciiChar = '█';
                } else if (surfaceType == 5) {
                    rgb = RGB_DECK;
                    asciiChar = '=';
                } else if (surfaceType == 6) {
                    rgb = RGB_MAST;
                    asciiChar = '█';
                } else if (surfaceType == 7) {
                    rgb = RGB_SAIL;
                } else if (surfaceType == 8) {
                    rgb = RGB_FLAG;
                    asciiChar = '▶';
                } else if (surfaceType == 10) {
                    rgb = RGB_GULL;
                    asciiChar = 'v';
                    fixedFullShade = true;
                } else if (surfaceType == 11) {
                    rgb = RGB_WAVES;
                    asciiChar = '~';
                    fixedFullShade = true;
                } else if (surfaceType == 12) {
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
}