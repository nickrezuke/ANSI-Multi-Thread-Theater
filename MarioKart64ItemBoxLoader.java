import java.util.Arrays;

public class MarioKart64ItemBoxLoader extends Loader {
    private static final StatusStage[] MK64_STAGES = {
        new StatusStage(25, "Assembling regular octahedron vertex array:"),
        new StatusStage(50, "Simulating internal viscous fluid plasma:"),
        new StatusStage(75, "Suspending blocky low-poly question mark core:"),
        new StatusStage(100, "Super Mario 64 Item Box Operational!")
    };

    private double shellAngleY = 0.0;
    private double shellAngleX = 0.0;
    private double coreAngleY = 0.0;
    private double bobClock = 0.0;
    private final int width = 80;
    private final int height = 22;
    private static final double CAMERA_DISTANCE = 3.6;

    private static final int[] RGB_CORE_LIT = { 255, 255, 230 };
    private static final int[] RGB_CORE_SHD = { 220, 180, 50 };

    private final char[] rawCharBuffer = new char[80 * 22];
    private final int[][] rawColorBuffer = new int[80 * 22][3];

    public MarioKart64ItemBoxLoader() {
        super(MK64_STAGES, 80, 22);
    }

    public MarioKart64ItemBoxLoader(int w, int h) {
        super(MK64_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        this.shellAngleY = 0.0;
        this.shellAngleX = 0.0;
        this.coreAngleY = 0.0;
        this.bobClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        shellAngleY += 0.005;
        shellAngleX += 0.0035;
        coreAngleY -= 0.022;
        bobClock += 0.032;

        double cosSY = Math.cos(shellAngleY), sinSY = Math.sin(shellAngleY);
        double cosSX = Math.cos(shellAngleX), sinSX = Math.sin(shellAngleX);
        double cosCY = Math.cos(coreAngleY), sinCY = Math.sin(coreAngleY);

        double bobOffsetY = 0.12 * Math.sin(bobClock);

        Arrays.fill(rawCharBuffer, ' ');
        for (int i = 0; i < rawColorBuffer.length; i++) {
            rawColorBuffer[i][0] = 0;
            rawColorBuffer[i][1] = 0;
            rawColorBuffer[i][2] = 0;
        }

        // -----------------------------------------------------
        // STEP 1: RENDER FLAT 2D "?" HOLOGRAM CENTER (REFINED)
        // -----------------------------------------------------
        // traced exclusively on the Z=0 plane to completely eliminate corkscrewing
        
        // 1. Refined Top Arch (Sweeps tightly into a balanced hook shape)
        // Shifting angle bounds from -0.1*PI to 1.25*PI creates a true question mark curve
        for (double angle = -Math.PI * 0.1; angle <= Math.PI * 1.25; angle += 0.04) {
            double radius = 0.16; // Tighter radius ensures it doesn't look too wide
            double centerX = radius * Math.cos(angle) - 0.02; // Aligned with stem
            double centerY = radius * Math.sin(angle) + 0.18; // Lifted top section
            
            // Render 2D pixel cluster thickness
            for (double dx = -0.035; dx <= 0.035; dx += 0.015) {
                for (double dy = -0.035; dy <= 0.035; dy += 0.015) {
                    plotRawCorePoint(centerX + dx, centerY + dy + bobOffsetY, 0.0, cosCY, sinCY, zBuffer);
                }
            }
        }

        // 2. Straight Middle Vertical Stem (Slightly tapered down into the dot gap)
        for (double qy = -0.12; qy <= 0.05; qy += 0.02) {
            double qx = -0.10; // Placed perfectly under the hook terminal falloff
            for (double dx = -0.035; dx <= 0.035; dx += 0.015) {
                plotRawCorePoint(qx + dx, qy + bobOffsetY, 0.0, cosCY, sinCY, zBuffer);
            }
        }

        // 3. Bottom Disconnected Square Dot (Centered perfectly under the stem)
        for (double qy = -0.36; qy <= -0.26; qy += 0.02) {
            double qx = -0.10; // Perfectly vertically tracking with the center line above
            for (double dx = -0.035; dx <= 0.035; dx += 0.015) {
                plotRawCorePoint(qx + dx, qy + bobOffsetY, 0.0, cosCY, sinCY, zBuffer);
            }
        }


        // ---------------------------------------------------
        // STEP 2: RENDER REGULAR TUMBLING OCTAHEDRON GEOMETRY
        // ---------------------------------------------------
        double scale = 1.45;
        double[][] vertices = { 
            { 0.0, -scale, 0.0 }, { 0.0, scale, 0.0 }, 
            { -scale, 0.0, -scale }, { scale, 0.0, -scale }, 
            { scale, 0.0, scale }, { -scale, 0.0, scale } 
        };
        int[][] faces = { 
            { 1, 2, 3 }, { 1, 3, 4 }, { 1, 4, 5 }, { 1, 5, 2 }, 
            { 0, 3, 2 }, { 0, 4, 3 }, { 0, 5, 4 }, { 0, 2, 5 } 
        };

        for (int f = 0; f < 8; f++) {
            double[] v0 = vertices[faces[f][0]];
            double[] v1 = vertices[faces[f][1]];
            double[] v2 = vertices[faces[f][2]];

            for (double u = 0.0; u <= 1.0; u += 0.035) {
                for (double v = 0.0; v <= 1.0 - u; v += 0.035) {
                    double w = 1.0 - u - v;
                    double x = u * v0[0] + v * v1[0] + w * v2[0];
                    double y = u * v0[1] + v * v1[1] + w * v2[1];
                    double z = u * v0[2] + v * v1[2] + w * v2[2];

                    double x1 = x * cosSY + z * sinSY;
                    double y1 = y;
                    double z1 = -x * sinSY + z * cosSY;

                    double finalX = x1;
                    double finalY = y1 * cosSX + bobOffsetY - z1 * sinSX + bobOffsetY;
                    double finalZ = y1 * sinSX + z1 * cosSX;

                    double ooz = 1.0 / (finalZ + CAMERA_DISTANCE);

                    int xp = (int) (40 + 44 * ooz * finalX * 0.9);
                    int yp = (int) (11 - 20 * ooz * finalY);

                    if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
                        int index = xp + width * yp;
                        if (ooz > zBuffer[index] + 0.0001) {
                            zBuffer[index] = ooz;

                            double wave = Math.sin(x * 2.5 + shellAngleY * 1.5) * Math.cos(y * 2.5 - shellAngleY * 1.2) + Math.sin((x + y) * 1.5 + shellAngleY);
                            int rBox = 40, gBox = 220, bBox = 255;

                            if (wave > 0.45) { rBox = 245; gBox = 35; bBox = 140; } 
                            else if (wave > 0.10) { rBox = 250; gBox = 120; bBox = 15; } 
                            else if (wave < -0.45) { rBox = 35; gBox = 230; bBox = 55; } 
                            else if (wave < -0.15) { rBox = 120; gBox = 40; bBox = 225; }

                            int finalR = rBox, finalG = gBox, finalB = bBox;
                            char finalChar = '░';

                            if (rawCharBuffer[index] != ' ' && rawCharBuffer[index] != 0) {
                                double alpha = 0.42;
                                finalR = (int) (rawColorBuffer[index][0] * (1.0 - alpha) + rBox * alpha);
                                finalG = (int) (rawColorBuffer[index][1] * (1.0 - alpha) + gBox * alpha);
                                finalB = (int) (rawColorBuffer[index][2] * (1.0 - alpha) + bBox * alpha);
                                finalChar = rawCharBuffer[index];
                            }

                            if ((xp + yp) % 2 == 0 && rawCharBuffer[index] == ' ') {
                                continue;
                            }

                            String esc = String.format("\u001B[38;2;%d;%d;%dm", 
                                Math.max(0, Math.min(255, finalR)), 
                                Math.max(0, Math.min(255, finalG)), 
                                Math.max(0, Math.min(255, finalB))
                            );
                            outputBuffer[index] = esc + finalChar + RESET;
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // STEP 3: FLUSH UN-OCCLUDED INTERNAL HOLOGRAPHIC PARTS
        // -------------------------------------------------------------
        for (int i = 0; i < width * height; i++) {
            if (outputBuffer[i] == null || outputBuffer[i].isEmpty() || outputBuffer[i].equals(" ")) {
                if (rawCharBuffer[i] != ' ' && rawCharBuffer[i] != 0) {
                    String esc = String.format("\u001B[38;2;%d;%d;%dm", rawColorBuffer[i][0], rawColorBuffer[i][1], rawColorBuffer[i][2]);
                    outputBuffer[i] = esc + rawCharBuffer[i] + RESET;
                } else {
                    outputBuffer[i] = " ";
                }
            }
        }
    }

    private void plotRawCorePoint(double x, double y, double z, double cosC, double sinC, double[] zBuffer) {
        double finalX = x * cosC + z * sinC;
        double finalY = y;
        double finalZ = -x * sinC + z * cosC;

        double ooz = 1.0 / (finalZ + CAMERA_DISTANCE);

        int xp = (int) (40 + 44 * ooz * finalX * 1.8);
        int yp = (int) (11 - 20 * ooz * finalY * 2);

        if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
            int bufferIndex = xp + width * yp;
            if (ooz > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooz;

                double luminance = (finalX * 0.577 - finalY * 0.707 - finalZ * 0.408);
                int[] rgb = (luminance > -0.2) ? RGB_CORE_LIT : RGB_CORE_SHD;

                rawCharBuffer[bufferIndex] = '▒';
                rawColorBuffer[bufferIndex][0] = rgb[0];
                rawColorBuffer[bufferIndex][1] = rgb[1];
                rawColorBuffer[bufferIndex][2] = rgb[2];
            }
        }
    }
}
