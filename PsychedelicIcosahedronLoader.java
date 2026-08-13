import java.util.Arrays;

public class PsychedelicIcosahedronLoader extends Loader {
    private static final StatusStage[] PSYCHEDELIC_STAGES = {
            new StatusStage(25, "Calibrating 20-vertex phase array:"),
            new StatusStage(50, "Weaving non-linear torsion lattices:"),
            new StatusStage(75, "Projecting chroma-shift field vectors:"),
            new StatusStage(100, "Psychedelic Core Synchronized!")
    };

    private double rotationY = 0.0;
    private double rotationX = 0.0;
    private double colorPhase = 0.0;
    private static final double CAMERA_DISTANCE = 3.8;

    public PsychedelicIcosahedronLoader() {
        super(PSYCHEDELIC_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.rotationY = 0.0;
        this.rotationX = 0.0;
        this.colorPhase = 0.0;
    }

    private static class SortedFace {
        int faceIndex;
        double avgZ;

        SortedFace(int faceIndex, double avgZ) {
            this.faceIndex = faceIndex;
            this.avgZ = avgZ;
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
        return new int[] { Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)) };
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Advance independent speeds for multi-axis tumbling
        rotationY += 0.0032;
        rotationX += 0.0046;
        colorPhase += 0.035; // Speed parameter for liquid color wheel cycles

        double cosY = Math.cos(rotationY), sinY = Math.sin(rotationY);
        double cosX = Math.cos(rotationX), sinX = Math.sin(rotationX);

        // STEP 1: DEFINE AN ICOSAHEDRON (Golden Ratio Coordinate Geometry)
        double phi = (1.0 + Math.sqrt(5.0)) / 2.0; // ~1.618
        double scale = 0.75; // Fits 80x22 camera constraints perfectly
        double[][] vertices = {
                { -1.0 * scale, phi * scale, 0.0 }, { 1.0 * scale, phi * scale, 0.0 },
                { -1.0 * scale, -phi * scale, 0.0 }, { 1.0 * scale, -phi * scale, 0.0 },
                { 0.0, -1.0 * scale, phi * scale }, { 0.0, 1.0 * scale, phi * scale },
                { 0.0, -1.0 * scale, -phi * scale }, { 0.0, 1.0 * scale, -phi * scale },
                { phi * scale, 0.0, -1.0 * scale }, { phi * scale, 0.0, 1.0 * scale },
                { -phi * scale, 0.0, -1.0 * scale }, { -phi * scale, 0.0, 1.0 * scale }
        };

        int[][] faces = {
                { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 },
                { 1, 5, 9 }, { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 }, { 7, 1, 8 },
                { 3, 9, 4 }, { 3, 4, 2 }, { 3, 2, 6 }, { 3, 6, 8 }, { 3, 8, 9 },
                { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 }, { 9, 8, 1 }
        };

        // STEP 2: PAINTER'S ALGORITHM BACK-TO-FRONT DEPTH SORT
        SortedFace[] sortedFaces = new SortedFace[20];
        for (int i = 0; i < faces.length; i++) {
            double zSum = 0.0;
            for (int vIdx : faces[i]) {
                double xl = vertices[vIdx][0];
                double yl = vertices[vIdx][1];
                double zl = vertices[vIdx][2];
                // Rotate around Y-axis then X-axis
                double z_rotY = -xl * sinY + zl * cosY;
                double rz = yl * sinX + z_rotY * cosX;
                zSum += rz;
            }
            sortedFaces[i] = new SortedFace(i, zSum / 3.0);
        }

        Arrays.sort(sortedFaces, (a, b) -> Double.compare(b.avgZ, a.avgZ));

        // STEP 3: HIGH-DENSITY SCAN RASTERIZATION WITH NON-LINEAR TORSION FIELDS
        for (int f = 0; f < sortedFaces.length; f++) {
            int activeFaceIndex = sortedFaces[f].faceIndex;
            int[] faceVertices = faces[activeFaceIndex];
            double[] v0 = vertices[faceVertices[0]];
            double[] v1 = vertices[faceVertices[1]];
            double[] v2 = vertices[faceVertices[2]];

            // Scan inside barycentric step triangles
            for (double u = 0; u <= 1.0; u += 0.025) {
                for (double v = 0; v <= 1.0 - u; v += 0.025) {
                    double w = 1.0 - u - v;

                    // Centered UV canvas coordinates on the face (-1.0 to 1.0)
                    double uc = (u * 2.0) - 0.66;
                    double vc = (v * 2.0) - 0.66;

                    // Twist the coordinate grid mathematically using non-linear math
                    double radius = Math.sqrt(uc * uc + vc * vc);
                    double angle = Math.atan2(vc, uc) + (radius * 3.5) - (colorPhase * 0.8);

                    // Re-project warped coordinates to generate Moiré patterns
                    double twistedU = radius * Math.cos(angle);
                    double twistedV = radius * Math.sin(angle);

                    // Complex psychedelic lattice patterns via high-frequency nested waves
                    double gridPattern1 = Math.sin(twistedU * 14.0) * Math.cos(twistedV * 14.0);
                    double gridPattern2 = Math.sin((twistedU + twistedV) * 9.0);

                    // Calculate a continuous intensity map from the non-linear wave functions
                    double lineIntensity = Math.max(0.0, (Math.abs(gridPattern1) - 0.5) / 0.5); // Smooth line falloff
                    double meshIntensity = Math.max(0.0, (gridPattern2 - 0.0) / 1.0); // Smooth mesh falloff

                    // Combine field intensities into a single analog blend factor (0.0 to 1.0)
                    double totalWeight = Math.max(lineIntensity * 1.0, meshIntensity * 0.65);
                    if (totalWeight > 1.0)
                        totalWeight = 1.0;

                    // Dynamic continuous shine scaling instead of jagged step-shifts
                    double dynamicShine = 0.25 + (totalWeight * 0.75);

                    // Smooth ASCII character ramp tracking intensity curves
                    String smoothRamp = " ░▒▓█";
                    int rampIdx = (int) (totalWeight * (smoothRamp.length() - 1));
                    char renderChar = smoothRamp.charAt(rampIdx);

                    // Vector point interpolation
                    double x = u * v0[0] + v * v1[0] + w * v2[0];
                    double y = u * v0[1] + v * v1[1] + w * v2[1];
                    double z = u * v0[2] + v * v1[2] + w * v2[2];

                    // Transform Spatial Pipeline
                    double x1 = x * cosY + z * sinY;
                    double y1 = y;
                    double z1 = -x * sinY + z * cosY;

                    double rx = x1;
                    double ry = y1 * cosX - z1 * sinX;
                    double rz = y1 * sinX + z1 * cosX;

                    // Display screen projections but vary the distance to go inside the structure and get trippy visuals
                    double ooz = 1.0 / (rz + (CAMERA_DISTANCE * (0.5 * (1.1+Math.cos(System.currentTimeMillis() / 1000.0)))));
                    int xp = (int) (40 + 56 * ooz * rx * 1.35);
                    int yp = (int) (11 + 27 * ooz * ry);

                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                        int index = xp + 80 * yp;

                        if (ooz > zBuffer[index] - 0.002) {
                            zBuffer[index] = ooz;

                            // Rainbow hue scales directly across spatial distribution fields
                            double spatialHue = colorPhase * 0.15 + (radius * 0.4) + (rz * 0.15);
                            spatialHue = (spatialHue % 1.0 + 1.0) % 1.0; // Safe wrap

                            int[] rgb = hsvToRgb(spatialHue, 0.95, dynamicShine);

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", rgb[0], rgb[1], rgb[2]);
                            outputBuffer[index] = colorCode + renderChar + RESET;
                        }
                    }
                }
            }
        }
    }
}
