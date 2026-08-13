// TODO: Is the "?" 3D or is it 2D?  

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
        // STEP 1: RENDER CHUNKY LOW-POLY 3D "?" HOLOGRAM CENTER
        // -----------------------------------------------------
        for (double qy = -0.32; qy <= 0.38; qy += 0.025) {
            double qx = 0.0;
            double qz = 0.0;
            boolean validPoint = false;

            if (qy >= 0.10) {
                // Top blocky hook arch segment - Shrunk radius from 0.25 to 0.18
                double pct = (qy - 0.10) / 0.28;
                double archAngle = pct * Math.PI * 1.35;
                qx = 0.18 * Math.cos(archAngle);
                qz = 0.18 * Math.sin(archAngle);
                validPoint = true;
            } else if (qy >= -0.10 && qy < 0.10) {
                // Low-poly linear center spine drop column - Shrunk center thickness
                double pct = (qy - (-0.10)) / 0.20;
                qx = -0.08 * (1.0 - pct);
                qz = 0.0;
                validPoint = true;
            } else if (qy >= -0.35 && qy <= -0.24) {
                // Bottom square dot punctuation indicator
                qx = 0.0; qz = 0.0;
                validPoint = true;
            }

            if (validPoint) {
                // Extrude thickness rings - Shrunk max depth thickness from 0.08 to 0.05
                for (double depth = -0.05; depth <= 0.05; depth += 0.025) {
                    plotRawCorePoint(qx, qy + bobOffsetY, depth + qz, cosCY, sinCY, zBuffer);
                }
            }
        }

        // ---------------------------------------------------
        // STEP 2: RENDER REGULAR TUMBLING OCTAHEDRON GEOMETRY
        // ---------------------------------------------------
        double scale = 1.45;
        double[][] vertices = {
                { 0.0, -scale, 0.0 },
                { 0.0, scale, 0.0 },
                { -scale, 0.0, -scale },
                { scale, 0.0, -scale },
                { scale, 0.0, scale },
                { -scale, 0.0, scale }
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
                    double finalY = y1 * cosSX - z1 * sinSX + bobOffsetY;
                    double finalZ = y1 * sinSX + z1 * cosSX;

                    double ooz = 1.0 / (finalZ + CAMERA_DISTANCE);
                    int xp = (int) (40 + 44 * ooz * finalX * 0.9);
                    int yp = (int) (11 - 20 * ooz * finalY);

                    if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
                        int index = xp + width * yp;

                        if (ooz > zBuffer[index] + 0.0001) {
                            zBuffer[index] = ooz;

                            double wave = Math.sin(x * 2.5 + shellAngleY * 1.5) * Math.cos(y * 2.5 - shellAngleY * 1.2)
                                    + Math.sin((x + y) * 1.5 + shellAngleY);

                            int rBox = 40, gBox = 220, bBox = 255;

                            if (wave > 0.45) {
                                rBox = 245;
                                gBox = 35;
                                bBox = 140;
                            } else if (wave > 0.10) {
                                rBox = 250;
                                gBox = 120;
                                bBox = 15;
                            } else if (wave < -0.45) {
                                rBox = 35;
                                gBox = 230;
                                bBox = 55;
                            } else if (wave < -0.15) {
                                rBox = 120;
                                gBox = 40;
                                bBox = 225;
                            }

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
                                    Math.max(0, Math.min(255, finalB)));
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
                    String esc = String.format("\u001B[38;2;%d;%d;%dm", rawColorBuffer[i][0], rawColorBuffer[i][1],
                            rawColorBuffer[i][2]);
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
        int xp = (int) (40 + 44 * ooz * finalX * 2);
        int yp = (int) (11 - 20 * ooz * finalY * 2);

        if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
            int bufferIndex = xp + width * yp;

            if (ooz > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooz;
                double luminance = (finalX * 0.577 - finalY * 0.707 - finalZ * 0.408);
                int[] rgb = (luminance > -0.2) ? RGB_CORE_LIT : RGB_CORE_SHD;
                rawCharBuffer[bufferIndex] = '█';
                rawColorBuffer[bufferIndex][0] = rgb[0];
                rawColorBuffer[bufferIndex][1] = rgb[1];
                rawColorBuffer[bufferIndex][2] = rgb[2];
            }
        }
    }
}