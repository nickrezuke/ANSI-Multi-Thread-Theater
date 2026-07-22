import java.util.Arrays;

public class RadarLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(20, "Activating sonar grid:"),
        new StatusStage(50, "Pinging network clusters:"),
        new StatusStage(80, "Analyzing perimeter data:"),
        new StatusStage(100, "All Sectors Clean!")
    };

    private double sweepAngle = 0;
    // Internal map tracking persistence trails across ticks
    private final double[][] trailDensity = new double[22][80];

    // Static threat targets configured as {Y, X, LabelType}
    private static final double[][] TARGETS = {
        {6, 25, 1}, {15, 60, 2}, {8, 52, 3}
    };

    public RadarLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        for (double[] row : trailDensity) Arrays.fill(row, 0.0);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;
        int cX = 40, cY = 11; // Core center of the scope radar

        // Age/fade out past sweep trails across frames
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                trailDensity[y][x] *= 0.85; 
            }
        }

        // Project the dynamic rotational sweep beam vector line outward
        for (double r = 0; r < 35; r += 0.5) {
            // Apply scale multiplier to account for rectangular console pixel aspect ratio
            int sX = (int) (cX + r * Math.cos(sweepAngle) * 1.8);
            int sY = (int) (cY + r * Math.sin(sweepAngle));

            if (sX >= 0 && sX < width && sY >= 0 && sY < height) {
                // Calculate signal strength drop across distance length
                trailDensity[sY][sX] = 1.0 - (r / 35.0);
            }
        }

        // Frame rendering processing
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int o = x + width * y;
                double distance = Math.sqrt(Math.pow((x - cX) / 1.8, 2) + Math.pow(y - cY, 2));

                // Restrict execution grid rendering strictly inside circular boundary plate perimeter
                if (distance > 10.5 && distance < 11.0) {
                    if (0.5 > zBuffer[o]) {
                        zBuffer[o] = 0.5;
                        outputBuffer[o] = "\u001B[38;5;28m#\u001B[0m"; // Frame outline
                    }
                } else if (distance < 10.5) {
                    double intensity = trailDensity[y][x];

                    // Check target coordinate intersection hit flags
                    for (double[] target : TARGETS) {
                        if ((int) target[0] == y && (int) target[1] == x) {
                            // Target angle position calculation check
                            double tAngle = Math.atan2(y - cY, (x - cX) / 1.8);
                            if (tAngle < 0) tAngle += 2 * Math.PI;

                            double diff = Math.abs(sweepAngle - tAngle);
                            if (diff < 0.2) {
                                intensity = 2.0; // Flash danger signal
                            }
                        }
                    }

                    if (intensity > 0.01 && intensity > zBuffer[o]) {
                        zBuffer[o] = intensity;

                        if (intensity > 1.5) {
                            outputBuffer[o] = "\u001B[38;5;196mX" + RESET; // Threat Target alert
                        } else {
                            // Assign map density intensity scales down through green index lines
                            int greenANSI = 22; 
                            if (intensity > 0.7)      greenANSI = 46; // Bright neon pulse
                            else if (intensity > 0.4) greenANSI = 28; // Medium trail
                            else if (intensity > 0.1) greenANSI = 22; // Low dark decay
                            
                            char radChar = (intensity > 0.7) ? '*' : '.';
                            outputBuffer[o] = "\u001B[38;5;" + greenANSI + "m" + radChar + RESET;
                        }
                    }
                }
            }
        }
        sweepAngle = (sweepAngle + 0.07) % (2 * Math.PI); // Rotate beam clockwise
    }
}
