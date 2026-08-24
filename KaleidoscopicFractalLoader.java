// TODO Fix this Kaleidoscopic Fractal Loader It "jumps" and is a little hard to see the details

public class KaleidoscopicFractalLoader extends Loader {
    private static final StatusStage[] FRACTAL_STAGES = {
            new StatusStage(25, "Seeding recursive coordinate cells:"),
            new StatusStage(50, "Carving voxel sub-grid arrays:"),
            new StatusStage(75, "Tracing infinite ray intersections:"),
            new StatusStage(100, "Fractal Spatial Loop Stable!")
    };

    private static final char[] SHADE_RAMP = { ' ', '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };
    private double zoomTimer = 0.0;
    private double rotationAngle = 0.0;

    public KaleidoscopicFractalLoader() {
        super(FRACTAL_STAGES, 126, 32);
    }

    @Override
    protected void initialize() {
        zoomTimer = 0.0;
        rotationAngle = 0.0;
    }

    // --- KALEIDOSCOPIC SIGNED DISTANCE FIELD ---
    private double evaluateSDF(double px, double py, double pz, double twistAngle) {
        double spacing = 2.0;
        px = (px % spacing + spacing) % spacing - spacing * 0.5;
        py = (py % spacing + spacing) % spacing - spacing * 0.5;
        pz = (pz % spacing + spacing) % spacing - spacing * 0.5;

        double fractalScale = 3.0;
        double d = Math.max(Math.abs(px) - 0.75, Math.max(Math.abs(py) - 0.75, Math.abs(pz) - 0.75));

        // Active dynamic rotation matrices for internal folds
        double cosT = Math.cos(twistAngle), sinT = Math.sin(twistAngle);

        for (int i = 0; i < 4; i++) { // Boosted iterations for richer kaleidoscope detailing
            // 1. Kaleidoscope planar mirroring folds
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

            // 2. Psychedelic spatial twist (rotates space as it moves deeper)
            double tx = px * cosT - py * sinT;
            py = px * sinT + py * cosT;
            px = tx;

            // 3. Sub-grid scaling transformation
            px = px * fractalScale - 1.4;
            py = py * fractalScale - 1.4;
            pz = pz * fractalScale;

            if (pz > 0.5 * (fractalScale - 1.0))
                pz -= (fractalScale - 1.0);

            double subBoxDist = Math.max(Math.abs(px) - 0.6, Math.max(Math.abs(py) - 0.6, Math.abs(pz) - 0.6));
            d = Math.max(d, subBoxDist / Math.pow(fractalScale, i + 1));
        }
        return d;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        zoomTimer += 0.012; // Slowed down slightly for smooth visual immersion
        rotationAngle += 0.008;

        // CRITICAL FIX: Match the modulo calculation to the logarithmic scale of the
        // geometric folds
        double progress = zoomTimer % 1.0;
        double currentScale = Math.pow(3.0, progress);

        double cosA = Math.cos(rotationAngle), sinA = Math.sin(rotationAngle);
        double cosB = Math.cos(rotationAngle * 0.3), sinB = Math.sin(rotationAngle * 0.3);

        // Light tracking coordinates
        double l1X = 0.577, l1Y = -0.577, l1Z = -0.577;
        double l2X = -0.577, l2Y = 0.577, l2Z = 0.577;

        double[] rawBrightnessValues = new double[4032];
        int[][] hitRGBs = new int[4032][3];
        boolean[] pixelHits = new boolean[4032];
        double maxCalculatedBrightness = 0.01;

        // Trippy Twist variance based on time
        double twistAngle = Math.sin(zoomTimer * 0.5) * 0.4;

        // --- SPHERE TRACING ENGINE ---
        for (int screenY = 0; screenY < 22; screenY++) {
            double uvY = (screenY - 11.0) / 11.0;
            for (int screenX = 0; screenX < 80; screenX++) {
                int index = screenX + 126 * screenY;
                double uvX = (screenX - 40.0) / 40.0 * 2.1;

                // Ray Direction setup
                double rxDir = uvX, ryDir = uvY, rzDir = 1.6;
                double rLen = Math.sqrt(rxDir * rxDir + ryDir * ryDir + rzDir * rzDir);
                rxDir /= rLen;
                ryDir /= rLen;
                rzDir /= rLen;

                double rx = (rxDir * cosA - rzDir * sinA) * cosB + ryDir * sinB;
                double ry = -(rxDir * cosA - rzDir * sinA) * sinB + ryDir * cosB;
                double rz = rxDir * sinA + rzDir * cosA;

                // CRITICAL FIX: Scale the camera's Z starting position along with space
                // This eliminates the sudden "jump-frame" flash completely!
                double cameraX = 0.0, cameraY = 0.0;
                double cameraZ = -2.0 / currentScale;

                double distanceMarched = 0.01;
                boolean hitFound = false;
                int stepCount = 0;
                int maxMarchSteps = 70; // More depth accuracy

                for (int step = 0; step < maxMarchSteps; step++) {
                    stepCount++;
                    double curX = cameraX + rx * distanceMarched;
                    double curY = cameraY + ry * distanceMarched;
                    double curZ = cameraZ + rz * distanceMarched;

                    // Evaluate world mapping
                    double safeDistance = evaluateSDF(curX * currentScale, curY * currentScale, curZ * currentScale,
                            twistAngle) / currentScale;

                    if (safeDistance < 0.0003) {
                        hitFound = true;
                        break;
                    }
                    distanceMarched += safeDistance;
                    if (distanceMarched > 4.0)
                        break;
                }

                if (hitFound) {
                    double inverseDepth = 1.0 / distanceMarched;
                    if (inverseDepth > zBuffer[index]) {
                        zBuffer[index] = inverseDepth;
                        pixelHits[index] = true;

                        double hitX = cameraX + rx * distanceMarched;
                        double hitY = cameraY + ry * distanceMarched;
                        double hitZ = cameraZ + rz * distanceMarched;

                        // High fidelity structural normals
                        double eps = 0.0005;
                        double nX = evaluateSDF((hitX + eps) * currentScale, hitY * currentScale, hitZ * currentScale,
                                twistAngle)
                                - evaluateSDF((hitX - eps) * currentScale, hitY * currentScale, hitZ * currentScale,
                                        twistAngle);
                        double nY = evaluateSDF(hitX * currentScale, (hitY + eps) * currentScale, hitZ * currentScale,
                                twistAngle)
                                - evaluateSDF(hitX * currentScale, (hitY - eps) * currentScale, hitZ * currentScale,
                                        twistAngle);
                        double nZ = evaluateSDF(hitX * currentScale, hitY * currentScale, (hitZ + eps) * currentScale,
                                twistAngle)
                                - evaluateSDF(hitX * currentScale, hitY * currentScale, (hitZ - eps) * currentScale,
                                        twistAngle);

                        double nMag = Math.sqrt(nX * nX + nY * nY + nZ * nZ);
                        if (nMag > 0.0) {
                            nX /= nMag;
                            nY /= nMag;
                            nZ /= nMag;
                        }

                        double diffuse1 = Math.max(0.0, nX * l1X + nY * l1Y + nZ * l1Z);
                        double diffuse2 = Math.max(0.0, nX * l2X + nY * l2Y + nZ * l2Z);
                        double totalDiffuse = (diffuse1 * 1.0) + (diffuse2 * 0.40);

                        // Ambient Occlusion rendering tweak
                        double aoFactor = 1.0 - ((double) stepCount / maxMarchSteps * 0.40);
                        double rawLuminance = (0.40 + 0.60 * totalDiffuse) * aoFactor;
                        rawBrightnessValues[index] = rawLuminance;

                        if (rawLuminance > maxCalculatedBrightness) {
                            maxCalculatedBrightness = rawLuminance;
                        }

                        // TRIPPY COLOR SHIFT: Blend position, depth, and time together for
                        // hyper-vibrant rainbows
                        double colorHue = (zoomTimer * 0.12 + (distanceMarched * 0.25) + (double) stepCount * 0.01)
                                % 1.0;
                        // Boosted saturation (0.95) and brightness value (0.95) to fight the dark
                        // frames
                        hitRGBs[index] = hsvToRgb(colorHue, 0.95, 0.95);
                    }
                }
            }
        }

        // --- RENDER PASS: AMPLIFIED AUTO-GAIN BLITTING ---
        for (int k = 0; k < 4032; k++) {
            if (!pixelHits[k])
                continue;

            double normalizedLuminance = rawBrightnessValues[k] / maxCalculatedBrightness;
            normalizedLuminance = Math.pow(normalizedLuminance, 0.65); // Aggressive gamma curve to lift shadows

            // Apply color gain amplification multiplier
            int r = (int) (hitRGBs[k][0] * normalizedLuminance * 1.5);
            int g = (int) (hitRGBs[k][1] * normalizedLuminance * 1.5);
            int b = (int) (hitRGBs[k][2] * normalizedLuminance * 1.5);

            if (r > 255)
                r = 255;
            if (g > 255)
                g = 255;
            if (b > 255)
                b = 255;

            int shadeIndex = (int) (normalizedLuminance * (SHADE_RAMP.length - 1));
            shadeIndex = Math.clamp(shadeIndex, 0, SHADE_RAMP.length - 1);

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
                r = (int) (v * 255);
                g = (int) (t * 255);
                b = (int) (p * 255);
                break;
            case 1:
                r = (int) (q * 255);
                g = (int) (v * 255);
                b = (int) (p * 255);
                break;
            case 2:
                r = (int) (p * 255);
                g = (int) (v * 255);
                b = (int) (t * 255);
                break;
            case 3:
                r = (int) (p * 255);
                g = (int) (q * 255);
                b = (int) (v * 255);
                break;
            case 4:
                r = (int) (t * 255);
                g = (int) (p * 255);
                b = (int) (v * 255);
                break;
            case 5:
                r = (int) (v * 255);
                g = (int) (p * 255);
                b = (int) (q * 255);
                break;
        }
        return new int[] { r, g, b };
    }
}