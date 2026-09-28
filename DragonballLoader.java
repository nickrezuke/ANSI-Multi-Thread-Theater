import java.util.Arrays;

public class DragonballLoader extends Loader {
    private static final StatusStage[] DRAGONBALL_STAGES = {
            new StatusStage(20, "Casting spherical amber resin core:"),
            new StatusStage(35, "Embedding tetra-star crimson cluster:"),
            new StatusStage(50, "Polishing outer surface for specular gleam:"),
            new StatusStage(75, "4-Star Dragon Ball Rotation Active!"),
            new StatusStage(98, "Checking Power Levels..."),
            new StatusStage(100, "It's Over 9000!!!")

    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    // Raw RGB color arrays for analog transmissive alpha blending
    private static final int[] RGB_BALL_BACK = { 180, 60, 0 };      // Dark shadowed ambient orange 
    private static final int[] RGB_GLASS_TINT = { 255, 130, 10 };   // Warm golden-orange resin surface
    private static final int[] RGB_STAR = { 240, 20, 25 };          // Vibrant crimson red
    private static final int[] RGB_STONE = { 90, 95, 100 };         // Martial arts arena tile base
    private static final int[] RGB_MOSS = { 50, 80, 40 };           // Overgrown moss patches

    private double A = 0.0;

    // Mirror screen buffers to track un-encoded raw character states for alpha coloring passes
    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public DragonballLoader() {
        super(DRAGONBALL_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // Star positions and logic are mathematically evaluated per frame
    }

    /**
     * Perfect mathematical bounds for a 5-pointed star using polar line intersection.
     */
    private boolean isInsideStar(double x, double y, double R) {
        double r = Math.sqrt(x * x + y * y);
        if (r > R) return false;
        if (r < 0.001) return true;

        // Orient star to point straight UP (Terminal Y is flipped, so use -y)
        double a = Math.atan2(-y, x) + Math.PI / 2.0;
        while (a < 0) a += 2.0 * Math.PI;

        double PI_5 = Math.PI / 5.0; // 36 degrees
        double localA = a % (2.0 * PI_5); // Fold into 5 identical wedges
        
        // Mirror the wedge so we only need to evaluate a single edge line
        if (localA > PI_5) {
            localA = (2.0 * PI_5) - localA;
        }

        // Standard golden ratio for inner pentagon of a 5-pointed star
        double r_inner = R * 0.381966;
        
        // Polar line equation connecting the outer tip to the inner corner
        double num = R * r_inner * Math.sin(PI_5);
        double den = R * Math.sin(localA) + r_inner * Math.sin(PI_5 - localA);

        return r <= (num / den);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        
        // Classic anime overhead dramatic lighting
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        double midDist = 1.75;
        //double distanceToCamera = midDist - 0.38 * Math.cos(A * 1.5); // standard wave
        double distanceToCamera = midDist - 0.5 * Math.sin(A) - (0.5 / Math.sqrt(3)) * Math.cos(2 * A) - (0.25 / Math.sqrt(3)); // waved wave

        double ballRadius = 0.65;
        double ballCY = -0.15; // Shifted up slightly to rest on the stone pedestal

        // Clear raw mirroring text buffers
        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // -------------------------------------------------------------
        // STEP 1: STONE TEMPLE PEDESTAL (Rendered First)
        // -------------------------------------------------------------
        for (double hy = 0.45; hy <= 0.95; hy += 0.015) {
            // Tiered ancient pillar design
            double r = 0.55;
            if (hy > 0.45 && hy < 0.50) r = 0.65;
            if (hy > 0.70 && hy < 0.75) r = 0.62;
            if (hy > 0.85) r = 0.65 + (hy - 0.85) * 0.8; // Flared base
            
            for (double phi = 0; phi < 2.0 * Math.PI; phi += 0.04) {
                double lx = r * Math.cos(phi);
                double lz = r * Math.sin(phi);
                
                // Add some organic moss patches to the stone
                int type = (Math.sin(phi * 15.0 + hy * 10.0) > 0.6 && hy > 0.5) ? 4 : 3;
                plotRawElement(lx, hy, lz, Math.cos(phi), 0, Math.sin(phi), type, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // STEP 2: INTERIOR RESIN BACK-FACE (Rendered Behind Stars)
        // -------------------------------------------------------------
        for (int t = 0; t <= 90; t++) {
            double theta = (t / 90.0) * Math.PI;
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
            for (int p = 0; p < 180; p++) {
                double phi = (p / 180.0) * 2.0 * Math.PI;
                
                // Unrotated surface normals
                double nx = sinTheta * Math.cos(phi);
                double ny = cosTheta;
                double nz = sinTheta * Math.sin(phi);
                
                // Rotate normal to camera space to cull the front-facing pixels
                double vNz = -nx * sinA + nz * cosA;
                if (vNz > 0.02) { 
                    // Facing away from camera: Draw deep interior amber shadow
                    double lx = ballRadius * nx;
                    double ly = ballCY + ballRadius * ny;
                    double lz = ballRadius * nz;
                    plotRawElement(lx, ly, lz, nx, ny, nz, 1, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 3: THE 4 RED STARS (Suspended Inside the Core)
        // -------------------------------------------------------------
        // Positioned in a classic square formation near the center of the ball
        double[][] starCenters = {
            {-0.19, -0.19, 0.0},
            { 0.19, -0.19, 0.0},
            {-0.19,  0.19, 0.0},
            { 0.19,  0.19, 0.0}
        };
        double starRadius = 0.13;
        
        for (double[] center : starCenters) {
            double cx = center[0];
            double cy = ballCY + center[1];
            double cz = center[2];
            
            // Render a flat 2D star extruded slightly along the Z-axis
            for (double lx = -starRadius; lx <= starRadius; lx += 0.007) {
                for (double ly = -starRadius; ly <= starRadius; ly += 0.007) {
                    if (isInsideStar(lx, ly, starRadius)) {
                        for (double lz = -0.015; lz <= 0.015; lz += 0.01) {
                            plotRawElement(cx + lx, cy + ly, cz + lz, 0, 0, (lz > 0 ? 1 : -1), 2, cosA, sinA, lightX, lightY, lightZ, distanceToCamera, zBuffer);
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 4: CHROMATIC TRANSMISSIVE FRONT GLASS PASS
        // -------------------------------------------------------------
        for (int t = 0; t <= 90; t++) {
            double theta = (t / 90.0) * Math.PI;
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
            
            for (int p = 0; p < 180; p++) {
                double phi = (p / 180.0) * 2.0 * Math.PI;
                
                double nx = sinTheta * Math.cos(phi);
                double ny = cosTheta;
                double nz = sinTheta * Math.sin(phi);
                
                // Only process the front-facing hemisphere
                double gNz = -nx * sinA + nz * cosA;
                if (gNz > 0.02) continue; 
                
                double localX = ballRadius * nx;
                double localY = ballCY + ballRadius * ny;
                double localZ = ballRadius * nz;

                // Rotate coordinates to camera space
                double rx = localX * cosA + localZ * sinA;
                double ry = localY;
                double rz = -localX * sinA + localZ * cosA;

                double ooz = 1.0 / (rz + distanceToCamera);
                int xp = (int) (40 + 36 * ooz * rx * 1.2);
                int yp = (int) (11 + 17 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int bufferIndex = xp + 80 * yp;

                    // Ensure front glass is closest
                    if (ooz > zBuffer[bufferIndex]) {
                        
                        double gNx = nx * cosA + nz * sinA;
                        double gNy = ny;
                        double luminance = gNx * lightX + gNy * lightY + gNz * lightZ;

                        int rCol, gCol, bCol;
                        char finalChar;

                        if (rawCharBuffer[bufferIndex] != ' ' && rawCharBuffer[bufferIndex] != 0) {
                            // --- RESIN TINT INJECTION: Wash the internal stars and back wall with golden amber ---
                            double alpha = 0.35; // Glass thickness density
                            rCol = (int) (rawColorBuffer[bufferIndex][0] * (1.0 - alpha) + RGB_GLASS_TINT[0] * alpha);
                            gCol = (int) (rawColorBuffer[bufferIndex][1] * (1.0 - alpha) + RGB_GLASS_TINT[1] * alpha);
                            bCol = (int) (rawColorBuffer[bufferIndex][2] * (1.0 - alpha) + RGB_GLASS_TINT[2] * alpha);
                            finalChar = rawCharBuffer[bufferIndex];

                            // Anime-style blown-out specular highlight for the glossy resin
                            if (luminance > 0.82) {
                                double sheen = Math.min(1.0, (luminance - 0.82) / 0.15);
                                rCol = (int) (rCol * (1.0 - sheen) + 255 * sheen);
                                gCol = (int) (gCol * (1.0 - sheen) + 250 * sheen);
                                bCol = (int) (bCol * (1.0 - sheen) + 200 * sheen);
                                finalChar = sheen > 0.6 ? '@' : '░'; // Starburst reflection core
                            }
                        } else {
                            // Glass rim profile (where the sphere hangs off the edge of the screen)
                            double rim = 1.0 - Math.abs(gNz); 
                            if (rim > 0.92) {
                                rCol = 255; gCol = 200; bCol = 100; finalChar = '░';
                            } else {
                                rCol = 20; gCol = 5; bCol = 0; finalChar = '.'; 
                            }
                        }

                        String esc = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, rCol)),
                                Math.max(0, Math.min(255, gCol)), Math.max(0, Math.min(255, bCol)));
                        outputBuffer[bufferIndex] = esc + finalChar + RESET;
                        zBuffer[bufferIndex] = ooz; // Lock depth
                    }
                }
            }
        }
        
        // -------------------------------------------------------------
        // STEP 5: FLUSH BASE & INTERNAL BUFFER TO TERMINAL
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
        
        A += 0.02; // Smooth rotation to admire the stars locked inside
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
                
                int[] rgb = RGB_BALL_BACK;
                boolean fixedFullShade = false;
                
                if (surfaceType == 1) {
                    rgb = RGB_BALL_BACK;
                    asciiChar = '.'; // Soft ambient texture for the deep resin background
                } else if (surfaceType == 2) {
                    rgb = RGB_STAR;
                    asciiChar = '█'; // Solid flat color for the suspended stars
                    fixedFullShade = true;
                } else if (surfaceType == 3) {
                    rgb = RGB_STONE;
                    asciiChar = '▓'; 
                } else if (surfaceType == 4) {
                    rgb = RGB_MOSS;
                    asciiChar = '▒';
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