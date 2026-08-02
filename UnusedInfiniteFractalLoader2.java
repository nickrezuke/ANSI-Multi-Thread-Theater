// TODO Think of something to do with this fractal one.  Its not rendering properly

public class UnusedInfiniteFractalLoader2 extends Loader {
    private static final StatusStage[] FRACTAL_STAGES = {
            new StatusStage(25, "Seeding recursive coordinate cells:"),
            new StatusStage(50, "Carving voxel sub-grid arrays:"),
            new StatusStage(75, "Tracing infinite ray intersections:"),
            new StatusStage(100, "Fractal Spatial Loop Stable!")
    };

    // 12-step density ramp map for terminal surface texturing
    private static final char[] SHADE_RAMP = { ' ', '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };

    private double zoomTimer = 0.0;
    private double rotationAngle = 0.0;

    public UnusedInfiniteFractalLoader2() {
        super(FRACTAL_STAGES);
    }

    @Override
    protected void initialize() {
        // Core states reset securely upon entry
        zoomTimer = 0.0;
        rotationAngle = 0.0;
    }

    // --- ENHANCED GEOMETRY BLUEPRINT: EXPANDED COHERENT DISTANCE FUNCTION ---
    // Evaluates the exact minimum safe distance from a 3D point to our infinite
    // structural geometry.
    // Instead of raw true/false checking, this calculates a true mathematical
    // Signed Distance Field.
    private double evaluateSDF(double px, double py, double pz, double currentScale) {
        // 1. Force periodic spatial repetition across all 3 axes
        // This loops space into an infinite grid array of structural fractal bricks.
        double spacing = 2.0;
        px = (px % spacing + spacing) % spacing - spacing * 0.5;
        py = (py % spacing + spacing) % spacing - spacing * 0.5;
        pz = (pz % spacing + spacing) % spacing - spacing * 0.5;

        // Scale space inward to drive the continuous nested recursive zoom
        px *= currentScale;
        py *= currentScale;
        pz *= currentScale;

        // Baseline primitive shape: A rounded cross-frame box structure
        double rX = Math.abs(px) - 0.75;
        double rY = Math.abs(py) - 0.75;
        double rZ = Math.abs(pz) - 0.75;

        double maxOfXYZ = Math.max(rX, Math.max(rY, rZ));
        double baselineBoxDist = Math.min(maxOfXYZ, 0.0)
                + Math.hypot(Math.max(rX, 0.0), Math.hypot(Math.max(rY, 0.0), Math.max(rZ, 0.0)));

        // 2. Perform fractal folder transformations to punch smooth hollow channels
        double fractalScale = 3.0;
        double d = baselineBoxDist;

        for (int i = 0; i < 3; i++) {
            // Fold coordinate domains symmetrically over core axes
            px = Math.abs(px);
            py = Math.abs(py);
            pz = Math.abs(pz);

            if (px < py) {
                double t = px;
                px = py;
                py = t;
            }
            if (px < pz) {
                double t = px;
                px = pz;
                pz = t;
            }
            if (py < pz) {
                double t = py;
                py = pz;
                pz = t;
            }

            // Magnify and offset space down into sub-quadrant sectors
            px = px * fractalScale - 1.4;
            py = py * fractalScale - 1.4;
            pz = pz * fractalScale;

            if (pz > 0.5 * (fractalScale - 1.0))
                pz -= (fractalScale - 1.0);

            // Distance to standard sub-gasket frames
            double cX = Math.abs(px) - 0.5;
            double cY = Math.abs(py) - 0.5;
            double cZ = Math.abs(pz) - 0.5;
            double subBoxDist = Math.max(cX, Math.max(cY, cZ));

            // Merge distance scales smoothly back to primary scene data
            d = Math.max(d, subBoxDist / Math.pow(fractalScale, i + 1));
        }

        return d / currentScale; // De-scale distance to retain raw world coordinates
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        zoomTimer += 0.015;
        rotationAngle += 0.006;

        double progress = zoomTimer % 1.0;
        double currentScale = Math.pow(3.0, progress);

        double cosA = Math.cos(rotationAngle), sinA = Math.sin(rotationAngle);
        double cosB = Math.cos(rotationAngle * 0.4), sinB = Math.sin(rotationAngle * 0.4);

        // Core light direction vectors
        double l1X = 0.577,  l1Y = -0.577, l1Z = -0.577; // Key
        double l2X = -0.577, l2Y = 0.577,  l2Z = 0.577;  // Fill

        // Local array caches to accumulate data for the auto-gain normalization step
        double[] rawBrightnessValues = new double[1760];
        int[][] hitRGBs = new int[1760][3];
        boolean[] pixelHits = new boolean[1760];
        for(int i = 0; i < pixelHits.length; i++) {
            pixelHits[i] = false;
        }

        double maxCalculatedBrightness = 0.01; // Avoid divide-by-zero on black frames

        // --- PASS 1: SPHERE TRACING AND RAW ILLUMINATION ASSEMBLY ---
        for (int screenY = 0; screenY < 22; screenY++) {
            double uvY = (screenY - 11.0) / 11.0;
            
            for (int screenX = 0; screenX < 80; screenX++) {
                int index = screenX + 80 * screenY;
                double uvX = (screenX - 40.0) / 40.0 * 2.1;

                double rxDir = uvX;
                double ryDir = uvY;
                double rzDir = 1.6;

                double rLen = Math.sqrt(rxDir*rxDir + ryDir*ryDir + rzDir*rzDir);
                rxDir /= rLen; ryDir /= rLen; rzDir /= rLen;

                double rx = (rxDir * cosA - rzDir * sinA) * cosB + ryDir * sinB;
                double ry = -(rxDir * cosA - rzDir * sinA) * sinB + ryDir * cosB;
                double rz = rxDir * sinA + rzDir * cosA;

                double distanceMarched = 0.05;
                boolean hitFound = false;
                int maxMarchSteps = 60;
                int stepCount = 0;
                double cameraX = 0.0, cameraY = 0.0, cameraZ = -0.5;

                for (int step = 0; step < maxMarchSteps; step++) {
                    stepCount++;
                    double curX = cameraX + rx * distanceMarched;
                    double curY = cameraY + ry * distanceMarched;
                    double curZ = cameraZ + rz * distanceMarched;

                    double safeDistance = evaluateSDF(curX, curY, curZ, currentScale);
                    if (safeDistance < 0.0008) {
                        hitFound = true;
                        break;
                    }
                    distanceMarched += safeDistance;
                    if (distanceMarched > 3.5) break;
                }

                if (hitFound) {
                    double inverseDepth = 1.0 / distanceMarched;

                    if (inverseDepth > zBuffer[index]) {
                        zBuffer[index] = inverseDepth;
                        pixelHits[index] = true;

                        double hitX = cameraX + rx * distanceMarched;
                        double hitY = cameraY + ry * distanceMarched;
                        double hitZ = cameraZ + rz * distanceMarched;

                        double eps = 0.001;
                        double nX = evaluateSDF(hitX + eps, hitY, hitZ, currentScale) - evaluateSDF(hitX - eps, hitY, hitZ, currentScale);
                        double nY = evaluateSDF(hitX, hitY + eps, hitZ, currentScale) - evaluateSDF(hitX, hitY - eps, hitZ, currentScale);
                        double nZ = evaluateSDF(hitX, hitY, hitZ + eps, currentScale) - evaluateSDF(hitX, hitY, hitZ - eps, currentScale);

                        double nMag = Math.sqrt(nX*nX + nY*nY + nZ*nZ);
                        if (nMag > 0.0) { nX /= nMag; nY /= nMag; nZ /= nMag; }

                        double diffuse1 = Math.max(0.0, nX * l1X + nY * l1Y + nZ * l1Z);
                        double diffuse2 = Math.max(0.0, nX * l2X + nY * l2Y + nZ * l2Z);
                        double totalDiffuse = (diffuse1 * 1.0) + (diffuse2 * 0.40);

                        // Soften the ambient occlusion dampening so it doesn't crush exposure ranges
                        double aoFactor = 1.0 - ((double) stepCount / maxMarchSteps * 0.50);
                        
                        // Combined luminance calculation
                        double rawLuminance = (0.35 + 0.65 * totalDiffuse) * aoFactor;
                        rawBrightnessValues[index] = rawLuminance;

                        if (rawLuminance > maxCalculatedBrightness) {
                            maxCalculatedBrightness = rawLuminance;
                        }

                        // Store raw baseline colors
                        double colorHue = (zoomTimer * 0.08 + (distanceMarched * 0.15)) % 1.0;
                        hitRGBs[index] = hsvToRgb(colorHue, 0.75, 0.85);
                    }
                }
            }
        }

        // --- PASS 2: AUTO-GAIN SCALING AND CHARACTER BLITTING ---
        // Dynamically pushes the frame context to use the highest elements on the shade ramp.
        for (int k = 0; k < 1760; k++) {
            if (!pixelHits[k]) continue;

            // Stretch the brightness curve so the highest point maps to 100% intensity
            double normalizedLuminance = rawBrightnessValues[k] / maxCalculatedBrightness;
            
            // Gamma curve correction (0.75) boosts midtones out of dark regions
            normalizedLuminance = Math.pow(normalizedLuminance, 0.75);

            // Compute structural text colors under amplified conditions
            int r = (int) (hitRGBs[k][0] * normalizedLuminance * 1.3);
            int g = (int) (hitRGBs[k][1] * normalizedLuminance * 1.3);
            int b = (int) (hitRGBs[k][2] * normalizedLuminance * 1.3);
            
            if (r > 255) r = 255; if (g > 255) g = 255; if (b > 255) b = 255;

            // Map values directly to the absolute highest brackets of the text array
            int shadeIndex = (int) (normalizedLuminance * (SHADE_RAMP.length - 1));
            if (shadeIndex < 0) shadeIndex = 0;
            else if (shadeIndex > SHADE_RAMP.length - 1) shadeIndex = SHADE_RAMP.length - 1;

            char renderChar = SHADE_RAMP[shadeIndex];
            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);

            outputBuffer[k] = colorCode + renderChar + RESET;
        }
    }

    private int[] hsvToRgb(double h, double s, double v) {
        int r = 0, g = 0, b = 0;
        int i = (int) (h * 6);
        double f = h * 6 - i;
        double p = v * (1 - s);
        double q = v * (1 - f * s);
        double t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0:
                r = (int) (v*255);
                g = (int) (t*255);
                b = (int) (p*255);
                break;
            case 1:
                r = (int) (q*255);
                g = (int) (v*255);
                b = (int) (p*255);
                break;
            case 2:
                r = (int) (p*255);
                g = (int) (v*255);
                b = (int) (t*255);
                break;
            case 3:
                r = (int) (p*255);
                g = (int) (q*255);
                b = (int) (v*255);
                break;
            case 4:
                r = (int) (t*255);
                g = (int) (p*255);
                b = (int) (v*255);
                break;
            case 5:
                r = (int) (v*255);
                g = (int) (p*255);
                b = (int) (q*255);
                break;
        }
        return new int[] { r, g, b };
    }    
}
