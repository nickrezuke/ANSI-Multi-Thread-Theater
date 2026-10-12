// TODO: Make the magnifying rim and handle a little thicker they seem thin among the noisy background

public class RefractiveMagnifierBusyCrowdLoader extends Loader {
    private static final StatusStage[] INTENSE_MAG_STAGES = {
        new StatusStage(25, "Generating dense urban populace:"),
        new StatusStage(50, "Weaving red and white striped garments:"),
        new StatusStage(75, "Molding extreme convex glass curvature:"),
        new StatusStage(100, "High-Power Optical Matrix Operational!")
    };

    private double timeClock = 0.0;
    private final int width = 80;
    private final int height = 22;

    public RefractiveMagnifierBusyCrowdLoader() {
        super(INTENSE_MAG_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.012;

        // Lissajous-style drift so the lens wanders broadly across the whole
        // scene rather than orbiting a small patch of it. 
        double glassCenterX = 1.60 * Math.sin(timeClock * 0.45);
        double glassCenterY = 0.38 * Math.sin(timeClock * 0.71 + 1.3);
        double glassCenterZ = -0.6;

        double lensRadius = 0.56;
        double rimThickness = 0.035;
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // Handle geometry: fixed grip angle relative to the screen.
        double handleAngle = Math.PI / 4.0; // down-and-right
        double handleDirX = Math.cos(handleAngle);
        double handleDirY = Math.sin(handleAngle);
        double handleStart = lensRadius + rimThickness;
        double handleLength = 0.62;
        double handleEnd = handleStart + handleLength;

        for (int y = 0; y < height; y++) {
            double screenY = ((double) y / height) * 2.0 - 1.0;

            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                double screenX = (((double) x / width) * 2.0 - 1.0) * 2.3;

                double paperU = (screenX + 2.3) / 4.6;
                double paperV = (screenY + 1.0) / 2.0;

                double dx = screenX - glassCenterX;
                double dy = screenY - glassCenterY;
                double distToCenter = Math.sqrt(dx * dx + dy * dy);

                // -------------------------------------------------------------
                // OBJECT A: BRASS LENS RIM
                // -------------------------------------------------------------
                if (distToCenter >= lensRadius && distToCenter < lensRadius + rimThickness) {
                    double depthZ = 1.0 / (-glassCenterZ);
                    if (depthZ > zBuffer[idx]) {
                        zBuffer[idx] = depthZ;
                        double nx = dx / distToCenter;
                        double ny = dy / distToCenter;
                        double spec = Math.pow(Math.max(0, nx * lightX + ny * lightY), 12);

                        int r = clamp((int) (175 + spec * 80));
                        int g = clamp((int) (145 + spec * 90));
                        int b = clamp((int) (85 + spec * 70));

                        char renderChar = (spec > 0.4) ? '█' : '▓';
                        outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // OBJECT B: HIGH-POWER CONVEX MAGNIFYING LENS
                // -------------------------------------------------------------
                if (distToCenter < lensRadius) {
                    double depthZ = 1.0 / (-glassCenterZ);
                    if (depthZ > zBuffer[idx]) {
                        zBuffer[idx] = depthZ;

                        double t = distToCenter / lensRadius; // 0 at center .. 1 at rim

                        // Radial magnification: strong zoom near the optical center.
                        double zoomStrength = 5.5;
                        double falloff = (1.0 - t) * (1.0 - t);
                        double magFactor = 1.0 / (1.0 + zoomStrength * falloff);

                        // A gentle swirl gives the content a "warped through curved glass" look.
                        double swirlAngle = (1.0 - t) * 0.35;
                        double cosA = Math.cos(swirlAngle), sinA = Math.sin(swirlAngle);
                        double rotX = dx * cosA - dy * sinA;
                        double rotY = dx * sinA + dy * cosA;

                        double sampleX = glassCenterX + rotX * magFactor;
                        double sampleY = glassCenterY + rotY * magFactor;
                        double lensU = (sampleX + 2.3) / 4.6;
                        double lensV = (sampleY + 1.0) / 2.0;

                        ScenePixel px = sampleScene(lensU, lensV);
                        int r = px.r, g = px.g, b = px.b;
                        char renderChar = px.glyph;

                        // Spherical convex-glass normal purely for 3D shading.
                        double curveness = 0.9;
                        double nz = Math.sqrt(Math.max(0.0, 1.0 - t * t * curveness * curveness));
                        double nx = -(dx / lensRadius) * curveness;
                        double ny = -(dy / lensRadius) * curveness;
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        nx /= nLen; ny /= nLen; nz /= nLen;

                        double spec = Math.max(0.0, nx * lightX + ny * lightY + nz * lightZ);
                        double glint = Math.pow(spec, 24);
                        double fresnel = (1.0 - nz) * (1.0 - nz);

                        // Cool glass tint over whatever the scene sampled.
                        r = (int) (r * 0.90 + 210 * 0.10);
                        g = (int) (g * 0.90 + 230 * 0.10);
                        b = (int) (b * 0.90 + 255 * 0.10);

                        r = clamp(r + (int) (glint * 190) + (int) (fresnel * 35));
                        g = clamp(g + (int) (glint * 190) + (int) (fresnel * 40));
                        b = clamp(b + (int) (glint * 190) + (int) (fresnel * 45));

                        // Soft white highlight blob near the upper-left of the dome.
                        double hlOffX = -0.32, hlOffY = -0.36;
                        double hdx = (dx / lensRadius) - hlOffX;
                        double hdy = ((dy / lensRadius) - hlOffY) * 1.3; 
                        double hlDist = Math.sqrt(hdx * hdx + hdy * hdy);
                        double hlStrength = Math.max(0.0, 1.0 - hlDist / 0.30);
                        hlStrength = hlStrength * hlStrength * 0.65; 

                        r = clamp((int) (r + hlStrength * (255 - r)));
                        g = clamp((int) (g + hlStrength * (255 - g)));
                        b = clamp((int) (b + hlStrength * (255 - b)));
                        if (hlStrength > 0.4) {
                            renderChar = '˚';
                        }

                        if (glint > 0.55) {
                            renderChar = '*'; // bright glass sparkle
                        }

                        outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // OBJECT C: HANDLE
                // -------------------------------------------------------------
                double along = dx * handleDirX + dy * handleDirY;
                if (along >= handleStart && along <= handleEnd) {
                    double perp = -dx * handleDirY + dy * handleDirX;
                    double s = (along - handleStart) / handleLength; 
                    double halfWidth = 0.05 * (1.0 - 0.35 * s); 

                    if (Math.abs(perp) < halfWidth) {
                        double depthZ = 1.0 / (-glassCenterZ);
                        if (depthZ > zBuffer[idx]) {
                            zBuffer[idx] = depthZ;

                            boolean wrap = Math.sin(s * 26.0) > 0.15;
                            double edgeShade = 1.0 - Math.abs(perp) / halfWidth; 

                            int r, g, b;
                            if (wrap) {
                                r = clamp((int) (120 + edgeShade * 60));
                                g = clamp((int) (75 + edgeShade * 45));
                                b = clamp((int) (40 + edgeShade * 25));
                            } else {
                                r = clamp((int) (90 + edgeShade * 40));
                                g = clamp((int) (55 + edgeShade * 30));
                                b = clamp((int) (28 + edgeShade * 18));
                            }

                            char renderChar = edgeShade > 0.6 ? '█' : (edgeShade > 0.3 ? '▓' : '▒');
                            outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                            continue;
                        }
                    }
                }

                // -------------------------------------------------------------
                // UNMAGNIFIED BACKDROP
                // -------------------------------------------------------------
                ScenePixel px = sampleScene(paperU, paperV);
                outputBuffer[idx] = ansi(px.r, px.g, px.b) + px.glyph + RESET;
            }
        }
    }

    // -----------------------------------------------------------------------
    // PROCEDURAL SCENE: A zoomed-in blocky 8-bit style crowd packed tightly. 
    // We break the screen down into a 16x4 grid. At 80x22 chars, this gives
    // characters a solid shoulder-to-shoulder silhouette.
    // -----------------------------------------------------------------------

    private ScenePixel sampleScene(double u, double v) {
        // Grid setup for our zoomed-in packed crowd (64 people total)
        double cellsX = 16.0; 
        double cellsY = 4.0;
        
        // Ensure we don't go out of bounds on the right/bottom edge
        int cx = Math.min((int) (u * cellsX), 15);
        int cy = Math.min((int) (v * cellsY), 3);
        
        // Local position inside the current person's cell (0.0 to 1.0)
        double lx = (u * cellsX) - cx;
        double ly = (v * cellsY) - cy;

        // Hardcode a few character locations in the grid
        boolean isWaldo = (cx == 11 && cy == 2);
        boolean isDecoy1 = (cx == 3 && cy == 1);
        boolean isDecoy2 = (cx == 14 && cy == 3);
        boolean isDecoy3 = (cx == 2 && cy == 3);
        boolean isDecoy4 = (cx == 7 && cy == 2);
        boolean isDecoy = isDecoy1 || isDecoy2 || isDecoy3 || isDecoy4;

        // Bounding boxes defining the shape of our 8-bit humans.
        // The arms stretch from 0.05 to 0.95, meaning characters will basically 
        // touch shoulder-to-shoulder, filling the screen with no gaps.
        boolean isPomPom   = ly >= 0.02 && ly < 0.15 && lx >= 0.4  && lx <= 0.6;
        boolean isHat      = ly >= 0.15 && ly < 0.25 && lx >= 0.3  && lx <= 0.7;
        boolean isFace     = ly >= 0.25 && ly < 0.45 && lx >= 0.3  && lx <= 0.7;
        boolean isTorso    = ly >= 0.45 && ly < 0.75 && lx >= 0.2  && lx <= 0.8;
        boolean isLeftArm  = ly >= 0.45 && ly < 0.70 && lx >= 0.05 && lx < 0.2;
        boolean isRightArm = ly >= 0.45 && ly < 0.70 && lx >  0.8  && lx <= 0.95;
        boolean isShirt    = isTorso || isLeftArm || isRightArm;
        boolean isLeftLeg  = ly >= 0.75 && ly < 0.98 && lx >= 0.25 && lx <= 0.45;
        boolean isRightLeg = ly >= 0.75 && ly < 0.98 && lx >= 0.55 && lx <= 0.75;
        boolean isPants    = isLeftLeg || isRightLeg;

        boolean isPerson = isPomPom || isHat || isFace || isShirt || isPants;

        if (isPerson) {
            // Procedural generation parameters for random background characters
            double pHash1 = hashNoise(cx * 3.1, cy * 7.9);
            double pHash2 = hashNoise(cx * 5.3, cy * 1.1);
            
            // --------------------
            // WALDO AND DECOYS
            // --------------------
            if (isWaldo || isDecoy) {
                if (isPomPom) return new ScenePixel(210, 40, 40, '▄'); // Red Top
                
                if (isHat) {
                    boolean red = ((int)(lx * 20)) % 2 == 0; // Vertical stripes
                    return red ? new ScenePixel(210, 40, 40, '▓') : new ScenePixel(245, 245, 245, '▒');
                }
                if (isFace) {
                    // Waldo's iconic glasses
                    if (isWaldo && ly > 0.30 && ly < 0.38 && lx > 0.25 && lx < 0.75) {
                        return new ScenePixel(20, 20, 20, '∞'); 
                    }
                    return new ScenePixel(255, 218, 185, '█'); // Skin
                }
                if (isShirt) {
                    boolean red = ((int)(ly * 20)) % 2 == 0; // Horizontal stripes
                    return red ? new ScenePixel(210, 40, 40, '▓') : new ScenePixel(245, 245, 245, '▒');
                }
                if (isPants) {
                    if (isDecoy) return new ScenePixel(40, 40, 40, '█'); // Decoys wear black pants
                    return new ScenePixel(0, 102, 204, '█'); // Waldo wears blue jeans
                }
            } 
            // --------------------
            // GENERAL CROWD
            // --------------------
            else {
                if (isPomPom || isHat) { // Used as generic hair
                    int[][] hair = {{30,30,30}, {100,50,20}, {210,180,90}, {180,50,50}, {50,100,150}};
                    int[] hc = hair[(int)(pHash1 * hair.length) % hair.length];
                    return new ScenePixel(hc[0], hc[1], hc[2], '▄');
                }
                if (isFace) {
                    int[][] skin = {{255,224,189}, {241,194,125}, {224,172,105}, {141,85,36}, {70,34,17}};
                    int[] sc = skin[(int)(pHash2 * skin.length) % skin.length];
                    // Occasional sunglasses!
                    if (pHash1 > 0.85 && ly > 0.30 && ly < 0.38 && lx > 0.25 && lx < 0.75) {
                        return new ScenePixel(20, 20, 20, '■');
                    }
                    return new ScenePixel(sc[0], sc[1], sc[2], '█');
                }
                if (isShirt) {
                    int r = (int)(40 + pHash1 * 200);
                    int g = (int)(40 + pHash2 * 200);
                    int b = (int)(40 + (Math.abs(pHash1 - pHash2)) * 200);
                    return new ScenePixel(r, g, b, '▓');
                }
                if (isPants) {
                    int[][] pants = {{40,40,40}, {30,70,120}, {120,110,90}, {80,90,100}};
                    int[] pc = pants[(int)(pHash1 * 10 * pants.length) % pants.length];
                    return new ScenePixel(pc[0], pc[1], pc[2], '█');
                }
            }
        }

        // -------------------------------------------------------------
        // BACKGROUND SCENERY (Visible in the tiny gaps between people)
        // -------------------------------------------------------------
        
        // Striped market awning draped behind the top row
        if (v < 0.25) { 
            boolean isRed = ((int)(u * 40)) % 2 == 0;
            double shade = 1.0 - (v / 0.25) * 0.4; 
            if (isRed) return new ScenePixel((int)(200 * shade), (int)(40 * shade), (int)(40 * shade), '▓');
            else return new ScenePixel((int)(230 * shade), (int)(230 * shade), (int)(220 * shade), '▒');
        } 
        // Dark gray cobblestone ground
        else { 
            double gNoise = hashNoise(Math.floor(u * 120), Math.floor(v * 60));
            int gCol = (int)(110 + gNoise * 30);
            return new ScenePixel(gCol, gCol, gCol, gNoise > 0.5 ? '░' : '.');
        }
    }

    private static double hashNoise(double u, double v) {
        double n = Math.sin(u * 127.1 + v * 311.7) * 43758.5453;
        return n - Math.floor(n);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static String ansi(int r, int g, int b) {
        return String.format("\u001B[38;2;%d;%d;%dm", clamp(r), clamp(g), clamp(b));
    }

    private static final class ScenePixel {
        final int r, g, b;
        final char glyph;

        ScenePixel(int r, int g, int b, char glyph) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.glyph = glyph;
        }
    }
}