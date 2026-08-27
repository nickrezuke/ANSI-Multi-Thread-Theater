import java.util.stream.IntStream;
import java.util.Arrays;

public class KaleidoscopicFractalLoader extends Loader {
    private static final StatusStage[] FRACTAL_STAGES = {
            new StatusStage(25, "Seeding recursive coordinate cells:"),
            new StatusStage(50, "Carving voxel sub-grid arrays:"),
            new StatusStage(75, "Tracing infinite ray intersections:"),
            new StatusStage(100, "Fractal Spatial Loop Stable!")
    };

    private static final char[] SHADE_RAMP = { ' ', '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };
    private static final double[] SCALE_POWERS = { 3.0, 9.0, 27.0 };

    private static final double L1X = 0.577, L1Y = -0.577, L1Z = -0.577;
    private static final double L2X = -0.577, L2Y = 0.577, L2Z = 0.577;

    private double zoomTimer = 0.0;
    private double rotationAngle = 0.0;

    private final double[] rawBrightnessValues = new double[4032];
    private final int[] hitR = new int[4032];
    private final int[] hitG = new int[4032];
    private final int[] hitB = new int[4032];
    private final boolean[] pixelHits = new boolean[4032];

    public KaleidoscopicFractalLoader() {
        super(FRACTAL_STAGES, 126, 32);
    }

    @Override
    protected void initialize() {
        zoomTimer = 0.0;
        rotationAngle = 0.0;
    }

    private double evaluateSDF(double px, double py, double pz, double cosT, double sinT) {
        double spacing = 2.0;
        px = (px % spacing + spacing) % spacing - spacing * 0.5;
        py = (py % spacing + spacing) % spacing - spacing * 0.5;
        pz = (pz % spacing + spacing) % spacing - spacing * 0.5;

        double d = Math.max(Math.abs(px) - 0.75, Math.max(Math.abs(py) - 0.75, Math.abs(pz) - 0.75));

        for (int i = 0; i < 3; i++) {
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

            double tx = px * cosT - py * sinT;
            py = px * sinT + py * cosT;
            px = tx;

            px = px * 3.0 - 1.4;
            py = py * 3.0 - 1.4;
            pz = pz * 3.0;

            if (pz > 1.0)
                pz -= 2.0;

            double subBoxDist = Math.max(Math.abs(px) - 0.6, Math.max(Math.abs(py) - 0.6, Math.abs(pz) - 0.6));
            d = Math.max(d, subBoxDist / SCALE_POWERS[i]);
        }
        return d;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        zoomTimer += 0.012;
        rotationAngle += 0.008;

        double cyclePosition = zoomTimer % 2.0;
        double progress = cyclePosition <= 1.0 ? cyclePosition : 2.0 - cyclePosition;
        double currentScale = Math.pow(3.0, progress);

        double cosA = Math.cos(rotationAngle), sinA = Math.sin(rotationAngle);
        double cosB = Math.cos(rotationAngle * 0.3), sinB = Math.sin(rotationAngle * 0.3);

        double twistAngle = Math.sin(zoomTimer * 0.5) * 0.001;
        double cosT = Math.cos(twistAngle);
        double sinT = Math.sin(twistAngle);
        double cameraZ = -2.0 / currentScale;

        Arrays.fill(pixelHits, false);
        Arrays.fill(rawBrightnessValues, 0.0);
        Arrays.fill(zBuffer, 0.0);

        IntStream.range(0, 32).parallel().forEach(screenY -> {
            for (int screenX = 0; screenX < 126; screenX++) {
                int index = screenX + 126 * screenY;

                double uvY = (screenY - 16.0) / 16.0;
                double uvX = (screenX - 83.0) / 83.0 * 2.4;

                double rxDir = uvX, ryDir = uvY, rzDir = 1.6;
                double rLen = Math.sqrt(rxDir * rxDir + ryDir * ryDir + rzDir * rzDir);
                rxDir /= rLen;
                ryDir /= rLen;
                rzDir /= rLen;

                double rx = (rxDir * cosA - rzDir * sinA) * cosB + ryDir * sinB;
                double ry = -(rxDir * cosA - rzDir * sinA) * sinB + ryDir * cosB;
                double rz = rxDir * sinA + rzDir * cosA;

                double distanceMarched = 0.01;
                int stepCount = 0;
                int maxMarchSteps = 150;
                boolean hitFound = false;

                for (int step = 0; step < maxMarchSteps; step++) {
                    stepCount++;
                    double curX = rx * distanceMarched;
                    double curY = ry * distanceMarched;
                    double curZ = cameraZ + rz * distanceMarched;

                    double safeDistance = evaluateSDF(curX * currentScale, curY * currentScale, curZ * currentScale,
                            cosT, sinT) / currentScale;

                    if (safeDistance < 0.0003) {
                        hitFound = true;
                        break;
                    }
                    distanceMarched += Math.max(safeDistance, 0.002 / currentScale);
                    if (distanceMarched > 4.0)
                        break;
                }

                if (hitFound) {
                    double hitX = rx * distanceMarched;
                    double hitY = ry * distanceMarched;
                    double hitZ = cameraZ + rz * distanceMarched;

                    double eps = 0.0015 / currentScale;
                    double baseSDF = evaluateSDF(hitX * currentScale, hitY * currentScale, hitZ * currentScale, cosT,
                            sinT);

                    double nX = evaluateSDF((hitX + eps) * currentScale, hitY * currentScale, hitZ * currentScale, cosT,
                            sinT) - baseSDF;
                    double nY = evaluateSDF(hitX * currentScale, (hitY + eps) * currentScale, hitZ * currentScale, cosT,
                            sinT) - baseSDF;
                    double nZ = evaluateSDF(hitX * currentScale, hitY * currentScale, (hitZ + eps) * currentScale, cosT,
                            sinT) - baseSDF;

                    double nMag = Math.sqrt(nX * nX + nY * nY + nZ * nZ);
                    if (nMag > 0.0) {
                        nX /= nMag;
                        nY /= nMag;
                        nZ /= nMag;
                    }

                    double diffuse1 = Math.max(0.0, nX * L1X + nY * L1Y + nZ * L1Z);
                    double diffuse2 = Math.max(0.0, nX * L2X + nY * L2Y + nZ * L2Z);
                    double totalDiffuse = diffuse1 + diffuse2 * 0.40;
                    double aoFactor = 1.0 - ((double) stepCount / maxMarchSteps * 0.40);
                    double luminance = (0.40 + 0.60 * totalDiffuse) * aoFactor;

                    double colorHue = (zoomTimer * 0.12 + distanceMarched * 0.25) % 1.0;

                    int iHue = (int) (colorHue * 6);
                    double f = colorHue * 6 - iHue;
                    double p = 0.95 * (1 - 0.95);
                    double q = 0.95 * (1 - f * 0.95);
                    double t = 0.95 * (1 - (1 - f) * 0.95);

                    double r = 0, g = 0, b = 0;
                    switch (iHue % 6) {
                        case 0 -> {
                            r = 0.95;
                            g = t;
                            b = p;
                        }
                        case 1 -> {
                            r = q;
                            g = 0.95;
                            b = p;
                        }
                        case 2 -> {
                            r = p;
                            g = 0.95;
                            b = t;
                        }
                        case 3 -> {
                            r = p;
                            g = q;
                            b = 0.95;
                        }
                        case 4 -> {
                            r = t;
                            g = p;
                            b = 0.95;
                        }
                        case 5 -> {
                            r = 0.95;
                            g = p;
                            b = q;
                        }
                    }

                    double inverseDepth = 1.0 / distanceMarched;
                    if (inverseDepth > zBuffer[index]) {
                        zBuffer[index] = inverseDepth;
                        pixelHits[index] = true;
                        rawBrightnessValues[index] = luminance;
                        hitR[index] = (int) (r * 255);
                        hitG[index] = (int) (g * 255);
                        hitB[index] = (int) (b * 255);
                    }
                }
            }
        });

        double maxCalculatedBrightness = 0.01;
        for (int k = 0; k < 4032; k++) {
            if (pixelHits[k] && rawBrightnessValues[k] > maxCalculatedBrightness) {
                maxCalculatedBrightness = rawBrightnessValues[k];
            }
        }

        maxCalculatedBrightness = Math.max(0.5, Math.min(maxCalculatedBrightness, 2.0));

        StringBuilder sb = new StringBuilder(32);
        for (int k = 0; k < 4032; k++) {
            if (!pixelHits[k])
                continue;

            double normalizedLuminance = Math.pow(rawBrightnessValues[k] / maxCalculatedBrightness, 0.65);

            int r = Math.min(255, (int) (hitR[k] * normalizedLuminance * 1.1));
            int g = Math.min(255, (int) (hitG[k] * normalizedLuminance * 1.1));
            int b = Math.min(255, (int) (hitB[k] * normalizedLuminance * 1.1));

            int shadeIndex = Math.clamp((int) (normalizedLuminance * (SHADE_RAMP.length - 1)), 0,
                    SHADE_RAMP.length - 1);

            sb.setLength(0);
            sb.append("\u001B[38;2;").append(r).append(';').append(g).append(';').append(b).append('m')
                    .append(SHADE_RAMP[shadeIndex]).append("\u001B[0m"); // Replaced RESET with literal if missing
            outputBuffer[k] = sb.toString();
        }
    }
}