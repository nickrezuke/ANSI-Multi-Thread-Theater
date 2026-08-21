// TODO: Fix the cmera angle to better view the Sushi

public class SushiLoader extends Loader {
    private static final StatusStage[] SUSHI_STAGES = {
            new StatusStage(15, "Assembling Sushi:"),
            new StatusStage(30, "Filling Ceramic Bowls:"),
            new StatusStage(45, "Frying Tempura:"),
            new StatusStage(60, "Folding Gyoza:"),
            new StatusStage(80, "Slicing Salmon:"),
            new StatusStage(95, "Obtaining Chopsticks:"),
            new StatusStage(100, "Itadakimasu!")
    };

    private final int width = 120;
    private final int height = 36;

    // Camera pulls in and pulls back within this range; see the zoom/speed
    // oscillator at the top of renderGeometry().
    private static final double CAMERA_DISTANCE_BASE = 3.5;
    private static final double CAMERA_ZOOM_AMPLITUDE = 0.9;
    private static final double BASE_TRANSIT_SPEED = 0.0025;

    // Direct 24-bit ANSI color palettes
    private static final int[] RGB_BELT = { 55, 58, 62 }; // Dark Slate Tread Plates
    private static final int[] RGB_BELT_LIT = { 85, 90, 95 }; // Highlighted Tread Edges
    private static final int[] RGB_BOWL_RIM = { 210, 35, 35 }; // Ceramic Crimson Red
    private static final int[] RGB_NOODLES = { 245, 215, 90 }; // Bright Ramen Yellow
    private static final int[] RGB_SUSHI_RICE = { 240, 240, 245 }; // Pristine Rice White
    private static final int[] RGB_RICE_GRAIN_SHADE = { 222, 222, 230 }; // Subtle grain fleck
    private static final int[] RGB_SUSHI_FISH = { 250, 110, 90 }; // Salmon Pink
    private static final int[] RGB_GARNISH = { 45, 165, 65 }; // Scallion Green
    private static final int[] RGB_EGG_YOLK = { 248, 176, 44 }; // Ajitama soft-boiled egg
    private static final int[] RGB_CHASHU = { 206, 124, 112 }; // Braised pork slice
    private static final int[] RGB_NORI = { 28, 42, 30 }; // Seaweed sheet
    private static final int[] RGB_GYOZA_DOUGH = { 244, 233, 208 }; // Steamed dumpling wrapper
    private static final int[] RGB_GYOZA_SEAR = { 115, 68, 34 }; // Pan-fried crispy base
    private static final int[] RGB_TEMPURA_CRUST = { 228, 168, 64 }; // Golden fried batter
    private static final int[] RGB_TEMPURA_SPECKLE = { 160, 105, 40 }; // Crispy batter fleck
    private static final int[] RGB_TEMPURA_SHRIMP = { 248, 132, 118 }; // Exposed shrimp tail
    private static final int[] RGB_SPECULAR_HIGHLIGHT = { 255, 255, 255 }; // Glossy fish sheen

    private double timeClock = 0.0;
    private double zoomPhase = 0.0;
    private double currentCameraDistance = CAMERA_DISTANCE_BASE;

    public SushiLoader() {
        super(SUSHI_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        this.zoomPhase = 0.0;
        this.currentCameraDistance = CAMERA_DISTANCE_BASE;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {

        // --- ZOOM / SPEED OSCILLATOR ---
        // One sine wave drives both. zoomWave near +1: camera pushes in close
        // and the belt slows down for a lingering look at whatever's passing.
        // zoomWave near -1: camera pulls back wide and the belt speeds up to
        // survey the whole spread before the next close-up.
        zoomPhase += 0.008;
        double zoomWave = Math.sin(zoomPhase);

        currentCameraDistance = CAMERA_DISTANCE_BASE - CAMERA_ZOOM_AMPLITUDE * zoomWave;
        double speedMultiplier = 1.0 - 0.45 * zoomWave;
        timeClock += BASE_TRANSIT_SPEED * speedMultiplier;

        // Global camera viewing angle adjustments
        double rotX = 0.45; // Tilted downward slightly to see inside the bowls
        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double rotY = 0.15 * Math.sin(timeClock * 0.4) + 0.05 * zoomWave; // slow drift, plus a touch of extra sway on close-ups
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);

        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // -------------------------------------------------------------
        // LAYER 1: THE TREADMILL BELT PLATES
        // -------------------------------------------------------------
        // The belt is an elliptical ring sized to hug the exact loop the food
        // travels on (radiusX/radiusZ below), so no plate can ever appear
        // somewhere a dish can't reach — that was the source of the stray
        // grey patches out toward the edges of the frame.
        double loopRadiusX = 1.85;
        double loopRadiusZ = 1.15;
        for (double bz = -1.6; bz <= 1.6; bz += 0.04) {
            for (double bx = -2.8; bx <= 2.8; bx += 0.05) {

                double normX = bx / loopRadiusX;
                double normZ = bz / loopRadiusZ;
                double ellipticalR = Math.sqrt(normX * normX + normZ * normZ);
                boolean inBeltPlates = ellipticalR > 0.80 && ellipticalR < 1.04;

                if (inBeltPlates) {
                    double by = 0.25; // Belt platform elevation plane

                    // Continuous cleat pattern running all the way around the
                    // loop (replaces the old bx>0/bx<0 split, which caused a
                    // visible seam where the two halves met).
                    double travelAngle = Math.atan2(normZ, normX);
                    double beltScroll = travelAngle * 3.0 + timeClock * 0.8;
                    boolean isCleatLine = Math.abs(Math.sin(beltScroll * 6.0)) > 0.82;

                    int[] bRGB = isCleatLine ? RGB_BELT_LIT : RGB_BELT;
                    char bChar = isCleatLine ? '>' : '█';

                    plot3DElement(bx, by, bz, 0.0, -1.0, 0.0, bRGB, bChar, cosX, sinX, cosY, sinY, lightX, lightY,
                            lightZ, outputBuffer, zBuffer);
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 2: THE MOVING FOOD AGENTS (Capsule Loop Distribution)
        // -------------------------------------------------------------
        int totalPlates = 8; // Cycles through 4 kinds of food, two of each
        for (int i = 0; i < totalPlates; i++) {
            double progressPhase = (timeClock * 0.4 + (double) i / totalPlates) * 2.0 * Math.PI;

            double itemZ = loopRadiusZ * 0.94 * Math.sin(progressPhase);
            double itemX = loopRadiusX * 0.94 * Math.cos(progressPhase);

            switch (i % 4) {
                case 0:
                    renderRamenBowlMesh(itemX, itemZ, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer,
                            zBuffer);
                    break;
                case 1:
                    boolean noriWrap = (i % 8) == 1; // Just one of the two nigiri gets a nori belt
                    renderSushiPlatterMesh(itemX, itemZ, noriWrap, cosX, sinX, cosY, sinY, lightX, lightY, lightZ,
                            outputBuffer, zBuffer);
                    break;
                case 2:
                    renderGyozaMesh(itemX, itemZ, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer,
                            zBuffer);
                    break;
                case 3:
                    renderTempuraMesh(itemX, itemZ, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer,
                            zBuffer);
                    break;
            }
        }
    }

    private void renderRamenBowlMesh(double cx, double cz, double cosX, double sinX, double cosY, double sinY,
            double lx, double ly, double lz, String[] outBuf, double[] zBuf) {
        double bowlRadius = 0.26;

        // Extrude a hemisphere shell container
        for (double ry = 0.0; ry <= bowlRadius; ry += 0.02) {
            double currentLayerRadius = Math.sqrt(bowlRadius * bowlRadius - ry * ry);

            for (double angle = 0; angle < 2 * Math.PI; angle += 0.15) {
                double px = cx + currentLayerRadius * Math.cos(angle);
                double py = 0.25 - ry; // Extrude upwards from the belt surface level
                double pz = cz + currentLayerRadius * Math.sin(angle);

                double rNx = Math.cos(angle);
                double rNy = -ry / bowlRadius;
                double rNz = Math.sin(angle);

                int[] itemRGB = RGB_BOWL_RIM;
                char itemChar = '█';

                if (ry > bowlRadius - 0.04) {
                    // The open top of the bowl, divided into topping wedges so
                    // each dish reads as a real bowl of ramen instead of a
                    // flat mono-textured fill.
                    double wavyNoodle = Math.sin(angle * 9.0 + cx * 4.0);

                    if (angle > 0.9 && angle < 1.35) {
                        itemRGB = RGB_EGG_YOLK; // Soft-boiled ajitama half
                        itemChar = '●';
                    } else if (angle > 3.55 && angle < 3.95) {
                        itemRGB = RGB_CHASHU; // Braised pork slice
                        itemChar = '▓';
                    } else if (angle > 5.5 && angle < 5.85) {
                        itemRGB = RGB_NORI; // Nori sheet standing in the broth
                        itemChar = '█';
                    } else if (Math.cos(angle * 5.0) * Math.sin(angle * 3.0) > 0.1) {
                        itemRGB = RGB_GARNISH; // Chopped scallions accent
                        itemChar = '▒';
                    } else {
                        itemRGB = RGB_NOODLES; // Wavy noodle fill texture
                        itemChar = wavyNoodle > 0.2 ? '▓' : '▒';
                    }
                }

                plot3DElement(px, py, pz, rNx, rNy, rNz, itemRGB, itemChar, cosX, sinX, cosY, sinY, lx, ly, lz, outBuf,
                        zBuf);
            }
        }
    }

    private void renderSushiPlatterMesh(double cx, double cz, boolean noriWrap, double cosX, double sinX,
            double cosY, double sinY, double lx, double ly, double lz, String[] outBuf, double[] zBuf) {
        // Construct a rectangular stacked block structure representing Salmon Nigiri
        for (double sy = 0.0; sy <= 0.18; sy += 0.03) {
            boolean isFishLayer = sy > 0.09; // Fish meat sits stacked cleanly on top of the rice block
            boolean isTopSlice = sy > 0.15;

            for (double sx = -0.22; sx <= 0.22; sx += 0.03) {
                for (double sz = -0.14; sz <= 0.14; sz += 0.03) {

                    double px = cx + sx;
                    double py = 0.25 - sy; // Extrude upward from belt track floor bounds
                    double pz = cz + sz;

                    double nx = (sx > 0) ? 1.0 : -1.0;
                    double ny = (sy > 0) ? -1.0 : 1.0;
                    double nz = (sz > 0) ? 1.0 : -1.0;

                    int[] itemRGB;
                    char itemChar = '█';

                    if (isFishLayer) {
                        itemRGB = RGB_SUSHI_FISH;

                        if (Math.abs(sx + sz * 1.5) % 0.12 < 0.03) {
                            itemRGB = RGB_SUSHI_RICE; // Stylized white fat marbling
                            itemChar = '▒';
                        } else if (isTopSlice && Math.abs(sx) < 0.03 && Math.abs(sz) < 0.02) {
                            itemRGB = RGB_SPECULAR_HIGHLIGHT; // A single glossy fleck
                            itemChar = '✦';
                        }
                    } else {
                        // Rice block: a fine grain fleck pattern so it doesn't
                        // read as one flat block of color.
                        boolean grainFleck = Math.sin(sx * 61.0 + cx * 7.0) * Math.cos(sz * 53.0 + cz * 5.0) > 0.55;
                        itemRGB = grainFleck ? RGB_RICE_GRAIN_SHADE : RGB_SUSHI_RICE;

                        if (noriWrap && sy < 0.06 && Math.abs(sz) > 0.10) {
                            itemRGB = RGB_NORI; // A belt of nori wrapped around the base
                            itemChar = '█';
                        }
                    }

                    plot3DElement(px, py, pz, nx, ny, nz, itemRGB, itemChar, cosX, sinX, cosY, sinY, lx, ly, lz,
                            outBuf, zBuf);
                }
            }
        }
    }

    private void renderGyozaMesh(double cx, double cz, double cosX, double sinX, double cosY, double sinY,
            double lx, double ly, double lz, String[] outBuf, double[] zBuf) {
        double gyozaLength = 0.28;
        double gyozaWidth = 0.13;
        double gyozaHeight = 0.11;

        for (double sy = 0.0; sy <= gyozaHeight; sy += 0.018) {
            double heightFrac = sy / gyozaHeight;
            double currentLength = gyozaLength * (1.0 - 0.35 * heightFrac);
            double currentWidth = gyozaWidth * (1.0 - 0.35 * heightFrac);
            boolean isSearedBase = sy < 0.018; // Crispy pan-fried bottom crust

            for (double sx = -currentLength; sx <= currentLength; sx += 0.025) {
                double edgeFalloff = 1.0 - (sx / currentLength) * (sx / currentLength);
                if (edgeFalloff <= 0.0) continue;
                double zExtent = currentWidth * Math.sqrt(edgeFalloff);

                for (double sz = -zExtent; sz <= zExtent; sz += 0.025) {
                    double px = cx + sx;
                    double py = 0.25 - sy;
                    double pz = cz + sz;

                    double nx = sx / (currentLength + 0.001);
                    double ny = -1.0;
                    double nz = sz / (currentWidth + 0.001);

                    int[] itemRGB;
                    char itemChar;

                    if (isSearedBase) {
                        itemRGB = RGB_GYOZA_SEAR;
                        itemChar = '▓';
                    } else {
                        // Pleated top: alternating ridges running across the dumpling
                        boolean onPleat = Math.abs(Math.sin(sx * 26.0)) > 0.75;
                        itemRGB = RGB_GYOZA_DOUGH;
                        itemChar = onPleat ? '▒' : '█';
                    }

                    plot3DElement(px, py, pz, nx, ny, nz, itemRGB, itemChar, cosX, sinX, cosY, sinY, lx, ly, lz,
                            outBuf, zBuf);
                }
            }
        }
    }

    private void renderTempuraMesh(double cx, double cz, double cosX, double sinX, double cosY, double sinY,
            double lx, double ly, double lz, String[] outBuf, double[] zBuf) {
        double arcRadius = 0.20;
        double tubeRadius = 0.055;

        // Walk along a curled arc spine — a shrimp tempura's signature "C" shape —
        // and build a tube of texels around each spine point.
        for (double t = -1.1; t <= 1.1; t += 0.14) {
            double spineX = arcRadius * Math.sin(t);
            double spineZ = arcRadius * (1.0 - Math.cos(t)) * 0.75;
            boolean isTailTip = t > 0.75; // The curled tail peeking out from the batter

            double localRadius = isTailTip ? tubeRadius * 0.55 : tubeRadius;

            for (double dy = -localRadius; dy <= localRadius; dy += 0.02) {
                double ringR = Math.sqrt(Math.max(0.0, localRadius * localRadius - dy * dy));

                for (double ang = 0; ang < 2 * Math.PI; ang += 0.5) {
                    double px = cx + spineX + ringR * Math.cos(ang);
                    double py = 0.28 - (localRadius + dy); // Rests slightly proud of the belt
                    double pz = cz + spineZ + ringR * Math.sin(ang);

                    double nx = Math.cos(ang);
                    double ny = dy / (localRadius + 0.001);
                    double nz = Math.sin(ang);

                    int[] itemRGB;
                    char itemChar;

                    if (isTailTip) {
                        itemRGB = RGB_TEMPURA_SHRIMP;
                        itemChar = '▓';
                    } else {
                        boolean crispSpeckle = Math.sin(px * 71.0 + pz * 67.0) > 0.7;
                        itemRGB = crispSpeckle ? RGB_TEMPURA_SPECKLE : RGB_TEMPURA_CRUST;
                        itemChar = crispSpeckle ? '▒' : '█';
                    }

                    plot3DElement(px, py, pz, nx, ny, nz, itemRGB, itemChar, cosX, sinX, cosY, sinY, lx, ly, lz,
                            outBuf, zBuf);
                }
            }
        }
    }

    private void plot3DElement(double x, double y, double z, double rNx, double rNy, double rNz,
            int[] rgb, char glyph, double cosX, double sinX, double cosY, double sinY,
            double lightX, double lightY, double lightZ, String[] outputBuffer, double[] zBuffer) {

        // --- AXIS 1: WORLD SPIN INTERPOLATION (Y-Axis Swing) ---
        double x1 = x * cosY + z * sinY;
        double y1 = y;
        double z1 = -x * sinY + z * cosY;

        double nx1 = rNx * cosY + rNz * sinY;
        double ny1 = rNy;
        double nz1 = -rNx * sinY + rNz * cosY;

        // --- AXIS 2: CAMERA CINEMATIC PITCH (X-Axis Tilt Down) ---
        double worldX = x1;
        double worldY = y1 * cosX - z1 * sinX;
        double worldZ = y1 * sinX + z1 * cosX;

        double worldNx = nx1;
        double worldNy = ny1 * cosX - nz1 * sinX;
        double worldNz = ny1 * sinX + nz1 * cosX;

        // --- PROJECTION SYSTEM MAPPING ---
        // Uses currentCameraDistance, which breathes in and out each frame
        // per the zoom/speed oscillator in renderGeometry().
        double ooz = 1.0 / (worldZ + currentCameraDistance);
        int xp = (int) (60 + 94 * ooz * worldX * 2.3); // Sized aspect ratio scale factor for 120x36 grid
        int yp = (int) (18 + 42 * ooz * worldY);

        if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
            int idx = xp + width * yp;

            if (ooz > zBuffer[idx] + 0.0001) {
                zBuffer[idx] = ooz;

                // Lambertian reflectance color shading calculation
                double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
                double shadeFactor = 0.5 + 0.5 * luminance;

                int r = (int) (rgb[0] * shadeFactor);
                int g = (int) (rgb[1] * shadeFactor);
                int b = (int) (rgb[2] * shadeFactor);

                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, r)),
                        Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
                outputBuffer[idx] = colorCode + glyph + RESET;
            }
        }
    }
}