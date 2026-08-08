import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class MandelbrotZoomLoader extends Loader {
    private static final StatusStage[] ZOOM_STAGES = {
        new StatusStage(25, "Mapping complex plane boundaries:"),
        new StatusStage(50, "Executing deep escape-time iterations:"),
        new StatusStage(75, "Syncing smooth anti-aliased color ramps:"),
        new StatusStage(100, "Mandelbrot Core Stable!")
    };

    private static final char[] DENSITY_RAMP = { '█', '▓', '▒', '░', ' ' };
    private double zoomTimer = -4.5; 

    private static final MathContext MC = new MathContext(40, RoundingMode.HALF_UP);

    private BigDecimal targetX;
    private BigDecimal targetY;

    private static final String[][] COORDINATE_POOL = {
        {"-0.743643887037158704752191506114774", "0.131825904205311970493132056385139"},
        {"-0.745146781250172421319200429402500", "0.112715481249953282240974026359000"},
        {"-0.162485814541571439284242636250000", "1.030570249221111629853825835937500"},
        {"-1.484609958742211603512879503375000", "0.000000000002410313887250216437500"}
    };

    public MandelbrotZoomLoader() {
        // This uses 80x22 specifically
        super(ZOOM_STAGES, 80, 22);
    }

    @Override 
    protected void initialize() { 
        // Select a random index row from the pool
        int randomIndex = (int) (Math.random() * COORDINATE_POOL.length);
        String[] selectedCoords = COORDINATE_POOL[randomIndex];

        // Instantiate the high-precision targets natively for this run instance
        this.targetX = new BigDecimal(selectedCoords[0], MC);
        this.targetY = new BigDecimal(selectedCoords[1], MC);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        zoomTimer += 0.008; 

        // Loop if we get too deep, precision issues start to arise despite our big decimal efforts
        double currentClock = ((zoomTimer + 4.5) % 15.0) - 4.5;

        // Use the safe, resetting clock variable for all geometric scale and color configurations
        double exponent = (currentClock * 0.4) % 6.0 + 1.0;
        double currentZoom = Math.pow(10.0, exponent);

        BigDecimal bdZoom = new BigDecimal(Double.toString(currentZoom), MC);
        BigDecimal bdWidth = new BigDecimal("3.0", MC).divide(bdZoom, MC);
        
        double aspectRatioFactor = (22.0 / 80.0) * 2.1;
        BigDecimal bdHeight = bdWidth.multiply(new BigDecimal(Double.toString(aspectRatioFactor), MC), MC);

        BigDecimal bdHalfWidth = bdWidth.divide(new BigDecimal("2.0", MC), MC);
        BigDecimal bdHalfHeight = bdHeight.divide(new BigDecimal("2.0", MC), MC);

        BigDecimal stepX = bdHalfWidth.divide(new BigDecimal("40.0", MC), MC);
        BigDecimal stepY = bdHalfHeight.divide(new BigDecimal("11.0", MC), MC);

        double startX = targetX.doubleValue();
        double startY = targetY.doubleValue();
        double dX = stepX.doubleValue();
        double dY = stepY.doubleValue();

        for (int screenY = 0; screenY < 22; screenY++) {
            double baseCY = startY + (screenY - 11.0) * dY;

            for (int screenX = 0; screenX < 80; screenX++) {
                double baseCX = startX + (screenX - 40.0) * dX;

                double totalIter = 0.0;
                double mixedR = 0, mixedG = 0, mixedB = 0;

                for (double sy = 0.25; sy <= 0.75; sy += 0.50) {
                    double cY = baseCY + (sy * dY);

                    for (double sx = 0.25; sx <= 0.75; sx += 0.50) {
                        double cX = baseCX + (sx * dX);

                        double zx = 0.0;
                        double zy = 0.0;
                        int maxIter = 140; 
                        int iter = 0;

                        while (zx * zx + zy * zy <= 4.0 && iter < maxIter) {
                            double xtemp = zx * zx - zy * zy + cX;
                            zy = 2.0 * zx * zy + cY;
                            zx = xtemp;
                            iter++;
                        }

                        double nuIter = iter;
                        if (iter < maxIter) {
                            double log_zn = Math.log(zx * zx + zy * zy) / 2.0;
                            double nu = Math.log(log_zn / Math.log(2.0)) / Math.log(2.0);
                            nuIter = iter + 1.0 - nu;
                        }
                        totalIter += nuIter;

                        if (iter < maxIter) {
                            // Updated to use the resetting currentClock for stable color loops
                            double colorHue = (nuIter * 0.03 + currentClock * 0.1) % 1.0;
                            if (colorHue < 0) colorHue += 1.0;
                            int[] sampleColor = hsvToRgb(colorHue, 0.85, 0.95);
                            mixedR += sampleColor[0];
                            mixedG += sampleColor[1];
                            mixedB += sampleColor[2];
                        } else {
                            mixedR += 10; mixedG += 10; mixedB += 18;
                        }
                    }
                }

                int r = (int) (mixedR / 4.0);
                int g = (int) (mixedG / 4.0);
                int b = (int) (mixedB / 4.0);
                double avgEscape = totalIter / 4.0;

                int index = screenX + 80 * screenY;
                
                int shadeIndex = (int) ((avgEscape % 10.0) / 10.0 * (DENSITY_RAMP.length - 1));
                shadeIndex = Math.max(0, Math.min(DENSITY_RAMP.length - 1, shadeIndex));
                char renderChar = DENSITY_RAMP[shadeIndex];

                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
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
            case 0: r = (int)(v*255); g = (int)(t*255); b = (int)(p*255); break;
            case 1: r = (int)(q*255); g = (int)(v*255); b = (int)(p*255); break;
            case 2: r = (int)(p*255); g = (int)(v*255); b = (int)(t*255); break;
            case 3: r = (int)(p*255); g = (int)(q*255); b = (int)(v*255); break;
            case 4: r = (int)(t*255); g = (int)(p*255); b = (int)(v*255); break;
            case 5: r = (int)(v*255); g = (int)(p*255); b = (int)(q*255); break;
        }
        return new int[]{r, g, b};
    }
}
