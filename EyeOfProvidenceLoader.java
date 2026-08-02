public class EyeOfProvidenceLoader extends Loader {
    private static final StatusStage[] PYRAMID_STAGES = {
            new StatusStage(20, "Etching Masonic borders:"),
            new StatusStage(40, "Obtaining collosal eyedrop vial:"),
            new StatusStage(60, "Centering the All-Seeing Pupil:"),
            new StatusStage(80, "Radiating light ray filaments:"),
            new StatusStage(98, "Asking the Masons three times:"),
            new StatusStage(100, "[-MESSAGE REDACTED-]!")
    };

    private static final char BLK_PUPIL = '\u2588'; // █ Solid black/white core elements
    private static final char BLK_IRIS = '\u2593'; // ▓ Textured eye color ring
    private static final char BLK_SKIN = '\u2592'; // ▒ Mid-tone masonry brickwork
    private static final char BLK_RAY = '\u00B7'; // · Ethereal background light rays

    private static final int[] CLR_EYE_WHITE = { 245, 245, 240 };
    private static final int[] CLR_PUPIL = { 20, 20, 25 };
    private static final int[] CLR_IRIS = { 0, 190, 165 };
    private static final int[] CLR_BRICK = { 210, 165, 95 };
    private static final int[] CLR_RAY = { 255, 230, 140 };

    private double pyramidAngle = 0.0;
    private static final double CAMERA_TILT = 0.42; // Constant downward view tilt

    public EyeOfProvidenceLoader() {
        super(PYRAMID_STAGES);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosY = Math.cos(pyramidAngle);
        double sinY = Math.sin(pyramidAngle);
        double cosTilt = Math.cos(CAMERA_TILT);
        double sinTilt = Math.sin(CAMERA_TILT);

        // Define a natural overhead light vector coming from the top-right-front
        double lightDirX = 0.408;
        double lightDirY = -0.816; // Strong downward angle
        double lightDirZ = -0.408;

        double[][] vertices = {
                { 0.0, -1.2, 0.0 }, { -1.1, 0.6, -1.1 }, { 1.1, 0.6, -1.1 }, { 1.1, 0.6, 1.1 }, { -1.1, 0.6, 1.1 }
        };

        int[][] faces = {
                { 0, 1, 2 }, { 0, 2, 3 }, { 0, 3, 4 }, { 0, 4, 1 }
        };

        for (int i = 0; i < faces.length; i++) {
            int[] faceVertices = faces[i];
            double[] v0 = vertices[faceVertices[0]];
            double[] v1 = vertices[faceVertices[1]];
            double[] v2 = vertices[faceVertices[2]];

            double edge1x = v1[0] - v0[0];
            double edge1y = v1[1] - v0[1];
            double edge1z = v1[2] - v0[2];
            double edge2x = v2[0] - v0[0];
            double edge2y = v2[1] - v0[1];
            double edge2z = v2[2] - v0[2];

            double nx = edge1y * edge2z - edge1z * edge2y;
            double ny = edge1z * edge2x - edge1x * edge2z;
            double nz = edge1x * edge2y - edge1y * edge2x;
            double nMag = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nMag > 0) {
                nx /= nMag;
                ny /= nMag;
                nz /= nMag;
            }

            double rNx = nx * cosY + nz * sinY;
            double rNy1 = ny;
            double rNz1 = -nx * sinY + nz * cosY;
            double rNy = rNy1 * cosTilt - rNz1 * sinTilt;
            double rNz = rNy1 * sinTilt + rNz1 * cosTilt;

            if (rNz > 0)
                continue;

            // Standard Dot Product calculation for realistic 3D shadowing
            double dotProduct = rNx * lightDirX + rNy * lightDirY + rNz * lightDirZ;
            double diffuse = Math.max(0.0, dotProduct);
            double shadowFactor = 0.35 + 0.65 * diffuse; // Keeps a 35% ambient floor so it's never pitch black

            for (double u = 0; u <= 1.0; u += 0.008) {
                for (double v = 0; v <= 1.0 - u; v += 0.008) {
                    double w = 1.0 - u - v;
                    double x = u * v0[0] + v * v1[0] + w * v2[0];
                    double y = u * v0[1] + v * v1[1] + w * v2[1];
                    double z = u * v0[2] + v * v1[2] + w * v2[2];

                    double texU = v / (1.0 - u + 0.0001);
                    double texV = 1.0 - u;

                    boolean drawEyeOnThisFace = (i == 1);
                    rasterizePixel(x, y, z, texU, texV, shadowFactor, drawEyeOnThisFace, cosY, sinY, cosTilt, sinTilt,
                            outputBuffer, zBuffer);
                }
            }
        }
        pyramidAngle += 0.020;

        // Give the Pyramid the surreal effect that every third face is the Eye, despite
        // the pyramid having 4 triangular faces.
        // This makes the eye seemingly change faces unbeknownst to the viewer
        if (pyramidAngle > 3.0 * Math.PI / 2.0) {
            pyramidAngle -= 3.0 * Math.PI / 2.0;
        }
    }

    private void rasterizePixel(double x, double y, double z, double u, double v, double shadow, boolean drawEye,
            double cosY, double sinY, double cosTilt, double sinTilt, String[] outputBuffer, double[] zBuffer) {
        double rx = x * cosY + z * sinY;
        double ry1 = y;
        double rz1 = -x * sinY + z * cosY;
        double ry = ry1 * cosTilt - rz1 * sinTilt;
        double rz = ry1 * sinTilt + rz1 * cosTilt;

        double distanceToCamera = 3.0;
        double ooz = 1.0 / (rz + distanceToCamera);
        int xp = (int) (40 + 48 * ooz * rx);
        int yp = (int) (11 + 24 * ooz * ry);

        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.0001) {
                zBuffer[index] = ooz;

                int targetR = CLR_BRICK[0], targetG = CLR_BRICK[1], targetB = CLR_BRICK[2];
                char renderChar = BLK_SKIN;

                if (drawEye) {
                    double eyeCenterX = u - 0.5;
                    double eyeCenterY = v - 0.58;

                    double outerSqr = (eyeCenterX * eyeCenterX) / 0.08 + (eyeCenterY * eyeCenterY) / 0.014;
                    double innerSqr = (eyeCenterX * eyeCenterX) / 0.065 + (eyeCenterY * eyeCenterY) / 0.009;
                    double radialDist = Math.sqrt(eyeCenterX * eyeCenterX + (eyeCenterY * eyeCenterY) * 2.2);

                    if (outerSqr <= 1.0 && innerSqr >= 1.0) {
                        targetR = CLR_PUPIL[0];
                        targetG = CLR_PUPIL[1];
                        targetB = CLR_PUPIL[2];
                        renderChar = BLK_PUPIL;
                    } else if (innerSqr < 1.0) {
                        if (radialDist <= 0.07) {
                            targetR = CLR_PUPIL[0];
                            targetG = CLR_PUPIL[1];
                            targetB = CLR_PUPIL[2];
                            renderChar = BLK_PUPIL;
                        } else if (radialDist <= 0.16) {
                            targetR = CLR_IRIS[0];
                            targetG = CLR_IRIS[1];
                            targetB = CLR_IRIS[2];
                            renderChar = BLK_IRIS;
                        } else {
                            targetR = CLR_EYE_WHITE[0];
                            targetG = CLR_EYE_WHITE[1];
                            targetB = CLR_EYE_WHITE[2];
                            renderChar = BLK_PUPIL;
                        }
                    } else {
                        double angle = Math.atan2(eyeCenterY, eyeCenterX);
                        if (v > 0.15 && (int) (Math.floor(angle * 6.5)) % 2 == 0) {
                            targetR = CLR_RAY[0];
                            targetG = CLR_RAY[1];
                            targetB = CLR_RAY[2];
                            renderChar = BLK_RAY;
                        }
                    }
                }

                // Apply shading values seamlessly to moving geometry colors
                targetR = (int) (targetR * shadow);
                targetG = (int) (targetG * shadow);
                targetB = (int) (targetB * shadow);

                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", targetR, targetG, targetB);
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
        }
    }
}
