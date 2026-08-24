import java.util.Arrays;
import java.awt.Color;

public class HypersphereLoader extends Loader {
    private static final StatusStage[] HYPER_STAGES = {
        new StatusStage(25, "Calibrating 4D hyper-polar grid..."),
        new StatusStage(50, "Generating 3-sphere cellular lattice..."),
        new StatusStage(75, "Engaging stereographic hyperspace rotation..."),
        new StatusStage(100, "4D Hypersphere Online!")
    };

    // Cleaned up shade ramp to minimize dark space gaps
    private static final char[] GLYPH_RAMP = {
        ' ', '.', '-', ':', ';', '=', '!', '*', '#', '$', '@', '░'
    };

    private double angleXW = 0.0;
    private double angleXY = 0.0;
    private double angleYZ = 0.0;

    public HypersphereLoader() {
        super(HYPER_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // No persistent runtime allocations needed
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, Double.MAX_VALUE);

        double cosXW = Math.cos(angleXW), sinXW = Math.sin(angleXW);
        double cosXY = Math.cos(angleXY), sinXY = Math.sin(angleXY);
        double cosYZ = Math.cos(angleYZ), sinYZ = Math.sin(angleYZ);

        double radius4D = 1.0;
        double distance4D = 2.3; 
        double zoom = 24.0; 

        // Balanced lighting vectors
        double lx = 0.577, ly = -0.577, lz = -0.577;

        double stepPsi = Math.PI / 12.0;   
        double stepTheta = Math.PI / 16.0; 
        double stepPhi = Math.PI / 32.0;   

        for (double psi = stepPsi; psi < Math.PI; psi += stepPsi) {
            double sinPsi = Math.sin(psi);
            double cosPsi = Math.cos(psi);

            for (double theta = stepTheta; theta < Math.PI; theta += stepTheta) {
                double sinTheta = Math.sin(theta);
                double cosTheta = Math.cos(theta);

                for (double phi = 0; phi < 2.0 * Math.PI; phi += stepPhi) {
                    double sinPhi = Math.sin(phi);
                    double cosPhi = Math.cos(phi);

                    double x = radius4D * sinPsi * sinTheta * cosPhi;
                    double y = radius4D * sinPsi * sinTheta * sinPhi;
                    double z = radius4D * sinPsi * cosTheta;
                    double w = radius4D * cosPsi;

                    double x4D = x * cosXW - w * sinXW;
                    double w4D = x * sinXW + w * cosXW;

                    double factor4D = 1.0 / (distance4D - w4D * 0.75);
                    double x3D = x4D * factor4D;
                    double y3D = y * factor4D;
                    double z3D = z * factor4D;

                    double x1 = x3D * cosXY - y3D * sinXY;
                    double y1 = x3D * sinXY + y3D * cosXY;
                    double y2 = y1 * cosYZ - z3D * sinYZ;
                    double z2 = y1 * sinYZ + z3D * cosYZ;

                    int screenX = (int) (40 + zoom * 2.2 * x1);
                    int screenY = (int) (11 + zoom * y2);

                    if (screenX < 0 || screenX >= 80 || screenY < 0 || screenY >= 22) {
                        continue;
                    }

                    double depth = z2 + w4D * 0.5;
                    int index = screenX + 80 * screenY;

                    if (depth < zBuffer[index]) {
                        zBuffer[index] = depth;

                        double len = Math.sqrt(x3D * x3D + y3D * y3D + z3D * z3D);
                        double nx = len > 0 ? x3D / len : 0;
                        double ny = len > 0 ? y3D / len : 0;
                        double nz = len > 0 ? z3D / len : 0;

                        double exposure = nx * lx + ny * ly + nz * lz;
                        double normalizeW = (w4D + 1.0) / 2.0;

                        // FIX: Soften darkness by shifting from standard Lambertian to Half-Lambert
                        // This transforms exposure from [-1.0 to 1.0] down to a highly visible [0.4 to 1.0] range
                        double lightIntensity = (exposure * 0.5 + 0.5) * 0.7 + 0.3;
                        
                        int rampIdx = (int) (lightIntensity * (GLYPH_RAMP.length - 1));
                        rampIdx = Math.max(0, Math.min(GLYPH_RAMP.length - 1, rampIdx));
                        char glyph = GLYPH_RAMP[rampIdx];

                        float hue = (float) ((normalizeW * 0.85 + (angleXW * 0.1)) % 1.0); 
                        float saturation = 0.85f; 
                        
                        // FIX: Lifted brightness floors drastically to keep hidden layers clear and readable
                        float brightness = (float) Math.max(0.60, lightIntensity * 0.6 + 0.4);

                        Color cellColor = Color.getHSBColor(hue, saturation, brightness);
                        int r = cellColor.getRed();
                        int g = cellColor.getGreen();
                        int b = cellColor.getBlue();

                        outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm%c\u001B[0m", r, g, b, glyph);
                    }
                }
            }
        }
        angleXW += 0.015;
        angleXY += 0.010;
        angleYZ += 0.012;
    }
}
