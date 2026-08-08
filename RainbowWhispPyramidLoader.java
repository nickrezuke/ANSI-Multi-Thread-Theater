public class RainbowWhispPyramidLoader extends Loader {
    private static final StatusStage[] PYRAMID_STAGES = {
            new StatusStage(40, "Polishing chrome facets:"),
            new StatusStage(80, "Igniting spectrum sweep:"),
            new StatusStage(100, "Reflective Core Online!")
    };

    private static final char[] SHADE_RAMP = {
            '\u00B7', // 0: · (Faint Ambient Detail)
            '\u2022', // 1: • (Defined Outer Fragment)
            '\u2058', // 2: ⁘ (Four-Dot Scatter)
            '\u00A4', // 3: ¤ (Particle Node)
            '\u205C', // 4: ⁜ (Dotted Cross)
            ':', // 5: : (Standard Density Anchor)
            '=', // 6: = (Mid-Weight Structure)
            '\u2591', // 7: ░ (Light Vapor Block)
            '\u2592', // 8: ▒ (Fluid Mid-Tone Block)
            '\u2593', // 9: ▓ (Dense Plasma Layer Block)
            '\u2588', // 10: █ (Blazing Peak Core Block)
            '\u2588' // 11: █ (Overdrive Core Guard)
    };

    private double pyramidAngle = 0.0;
    private double lightAngleX = 0.0;
    private double lightAngleY = 0.0;
    private double colorHue = 0.0;

    private static final int BASE_R = 100;
    private static final int BASE_G = 105;
    private static final int BASE_B = 115;

    private static final double CAMERA_TILT = 0.20;
    private final double cosTilt = Math.cos(CAMERA_TILT);
    private final double sinTilt = Math.sin(CAMERA_TILT);

    public RainbowWhispPyramidLoader() {
        // This uses 80x22 specifically
        super(PYRAMID_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosPyramid = Math.cos(pyramidAngle);
        double sinPyramid = Math.sin(pyramidAngle);

        // 1. Map orbiting point light paths smoothly into world space coordinates
        double orbitRadius = 1.5;
        double rawLightY = -0.25 + 0.35 * Math.sin(lightAngleY);
        double rawLightX = orbitRadius * Math.sin(lightAngleX);
        double rawLightZ = orbitRadius * Math.cos(lightAngleX);

        // CRITICAL BUG FIX: Transform the light position into the EXACT same
        // World-Camera space
        // as the geometry vertices so their spatial coordinates align during lighting
        // steps.
        double lightX = rawLightX * cosPyramid + rawLightZ * sinPyramid;
        double lY1 = rawLightY;
        double lZ1 = -rawLightX * sinPyramid + rawLightZ * cosPyramid;
        double lightY = lY1 * cosTilt - lZ1 * sinTilt;
        double lightZ = lY1 * sinTilt + lZ1 * cosTilt;

        int[] currentLightRGB = hueToRGB(colorHue);
        int lightR = currentLightRGB[0];
        int lightG = currentLightRGB[1];
        int lightB = currentLightRGB[2];

        // Base 3D Coordinates
        double[][] localVertices = {
                { 0.0, -0.9, 0.0 }, // 0: Apex
                { -1.0, 0.7, -1.0 }, // 1: Front-Left
                { 1.0, 0.7, -1.0 }, // 2: Front-Right
                { 1.0, 0.7, 1.0 }, // 3: Back-Right
                { -1.0, 0.7, 1.0 } // 4: Back-Left
        };

        // Rotate and project all vertices into world camera space BEFORE rendering
        // lines
        double[][] worldVertices = new double[5][3];
        for (int v = 0; v < 5; v++) {
            double x = localVertices[v][0];
            double y = localVertices[v][1];
            double z = localVertices[v][2];

            // Horizontal rotation loop
            double rx = x * cosPyramid + z * sinPyramid;
            double ry1 = y;
            double rz1 = -x * sinPyramid + z * cosPyramid;

            // Camera downward pitch tilt loop
            double ry = ry1 * cosTilt - rz1 * sinTilt;
            double rz = ry1 * sinTilt + rz1 * cosTilt;

            worldVertices[v][0] = rx;
            worldVertices[v][1] = ry;
            worldVertices[v][2] = rz;
        }

        int[][] faces = {
                { 0, 1, 2 }, // Face 0: Front Triangle Side
                { 0, 2, 3 }, // Face 1: Right Triangle Side
                { 0, 3, 4 }, // Face 2: Back Triangle Side
                { 0, 4, 1 }, // Face 3: Left Triangle Side
                { 1, 4, 3, 2 } // Face 4: Square Base
        };

        // STEP 1: Process Face Shell Surfaces
        for (int i = 0; i < faces.length; i++) {
            int[] faceVertices = faces[i];
            double[] v0 = worldVertices[faceVertices[0]];
            double[] v1 = worldVertices[faceVertices[1]];
            double[] v2 = worldVertices[faceVertices[2]];

            // Calculate precise normal directly from world-space geometry vectors
            double edge1x = v1[0] - v0[0];
            double edge1y = v1[1] - v0[1];
            double edge1z = v1[2] - v0[2];
            double edge2x = v2[0] - v0[0];
            double edge2y = v2[1] - v0[1];
            double edge2z = v2[2] - v0[2];

            double rNx = edge1y * edge2z - edge1z * edge2y;
            double rNy = edge1z * edge2x - edge1x * edge2z;
            double rNz = edge1x * edge2y - edge1y * edge2x;
            double nMag = Math.sqrt(rNx * rNx + rNy * rNy + rNz * rNz);
            if (nMag > 0) {
                rNx /= nMag;
                rNy /= nMag;
                rNz /= nMag;
            }

            // Clean backface culling step
            if (rNz > 0)
                continue;

            // Draw surfaces using smooth parametric area stepping loops
            if (i < 4) {
                // Calculate the true geometric center of the triangle face for realistic
                // specular physics
                double faceX = (v0[0] + v1[0] + v2[0]) / 3.0;
                double faceY = (v0[1] + v1[1] + v2[1]) / 3.0;
                double faceZ = (v0[2] + v1[2] + v2[2]) / 3.0;

                for (double u = 0; u <= 1.0; u += 0.012) {
                    for (double v = 0; v <= 1.0 - u; v += 0.012) {
                        double w = 1.0 - u - v;
                        double rx = u * v0[0] + v * v1[0] + w * v2[0];
                        double ry = u * v0[1] + v * v1[1] + w * v2[1];
                        double rz = u * v0[2] + v * v1[2] + w * v2[2];
                        calculateLightingAndPlot(rx, ry, rz, rNx, rNy, rNz, lightX, lightY, lightZ, lightR, lightG,
                                lightB, faceX, faceY, faceZ, outputBuffer, zBuffer);
                    }
                }
            } else {
                // Draw square base surface smoothly
                double[] v3 = worldVertices[faceVertices[3]];
                // Calculate the true geometric center of the quad base
                double faceX = (v0[0] + v1[0] + v2[0] + v3[0]) / 4.0;
                double faceY = (v0[1] + v1[1] + v2[1] + v3[1]) / 4.0;
                double faceZ = (v0[2] + v1[2] + v2[2] + v3[2]) / 4.0;

                for (double u = 0; u <= 1.0; u += 0.02) {
                    for (double v = 0; v <= 1.0; v += 0.02) {
                        // Bilinear interpolation across the base quadrilateral plane
                        double rx = (1 - u) * (1 - v) * v0[0] + u * (1 - v) * v1[0] + u * v * v2[0]
                                + (1 - u) * v * v3[0];
                        double ry = (1 - u) * (1 - v) * v0[1] + u * (1 - v) * v1[1] + u * v * v2[1]
                                + (1 - u) * v * v3[1];
                        double rz = (1 - u) * (1 - v) * v0[2] + u * (1 - v) * v1[2] + u * v * v2[2]
                                + (1 - u) * v * v3[2];
                        calculateLightingAndPlot(rx, ry, rz, rNx, rNy, rNz, lightX, lightY, lightZ, lightR, lightG,
                                lightB, faceX, faceY, faceZ, outputBuffer, zBuffer);
                    }
                }
            }
        }

        // STEP 2: Project and Render the Moving Rainbow Light Node securely in space
        double distanceToCamera = 2.9;
        double oozLight = 1.0 / (lightZ + distanceToCamera);
        int xpLight = (int) (40 + 44 * oozLight * lightX);
        int ypLight = (int) (9 + 22 * oozLight * lightY);

        if (xpLight >= 0 && xpLight < 80 && ypLight >= 0 && ypLight < 22) {
            int lightIndex = xpLight + 80 * ypLight;
            if (oozLight > zBuffer[lightIndex] - 0.02) {
                zBuffer[lightIndex] = oozLight;
                String lightColorCode = String.format("\u001B[38;2;%d;%d;%dm", lightR, lightG, lightB);
                outputBuffer[lightIndex] = lightColorCode + "█" + RESET;
            }
        }

        // Updates
        pyramidAngle -= 0.0015;
        lightAngleX += 0.02;
        lightAngleY += 0.05;
        colorHue += 0.0019;
        if (colorHue > 1.0)
            colorHue -= 1.0;
    }

    private void calculateLightingAndPlot(double rx, double ry, double rz, double rNx, double rNy, double rNz,
            double lightX, double lightY, double lightZ, int lightR, int lightG, int lightB, double faceX, double faceY,
            double faceZ, String[] outputBuffer, double[] zBuffer) {
        double distanceToCamera = 2.9;

        // Unified eye vector
        double viewX = -faceX;
        double viewY = -faceY;
        double viewZ = -(faceZ + distanceToCamera);
        double distToCam = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        if (distToCam > 0) {
            viewX /= distToCam;
            viewY /= distToCam;
            viewZ /= distToCam;
        }

        // SOURCE A: OVERHEAD FIXED RIM LIGHTING
        double overheadX = 0.577, overheadY = -0.707, overheadZ = -0.408;
        int overheadR = 130, overheadG = 140, overheadB = 160;
        double diffOverhead = rNx * overheadX + rNy * overheadY + rNz * overheadZ;
        double specOverhead = 0;
        if (diffOverhead > 0) {
            double refOverheadX = 2.0 * diffOverhead * rNx - overheadX;
            double refOverheadY = 2.0 * diffOverhead * rNy - overheadY;
            double refOverheadZ = 2.0 * diffOverhead * rNz - overheadZ;
            double specDot = refOverheadX * viewX + refOverheadY * viewY + refOverheadZ * viewZ;
            specOverhead = (specDot > 0) ? Math.pow(specDot, 16) : 0;
        } else {
            diffOverhead = 0;
        }
        // SOURCE B: ORBITING POINT LIGHTING (Now perfectly tracked!)
        double toLightX = lightX - rx;
        double toLightY = lightY - ry;
        double toLightZ = lightZ - rz;
        double distToLight = Math.sqrt(toLightX * toLightX + toLightY * toLightY + toLightZ * toLightZ);
        if (distToLight > 0) {
            toLightX /= distToLight;
            toLightY /= distToLight;
            toLightZ /= distToLight;
        }
        double diffuse = rNx * toLightX + rNy * toLightY + rNz * toLightZ;
        double specular = 0;
        if (diffuse > 0) {
            double refX = 2.0 * diffuse * rNx - toLightX;
            double refY = 2.0 * diffuse * rNy - toLightY;
            double refZ = 2.0 * diffuse * rNz - toLightZ;
            double specDot = refX * viewX + refY * viewY + refZ * viewZ;
            specular = (specDot > 0) ? Math.pow(specDot, 16) : 0;
        } else {
            diffuse = 0;
        }
        // AMBIENT LAYER BLENDING MIX
        double ambientWeight = 0.22;
        int r = (int) (BASE_R * ambientWeight);
        int g = (int) (BASE_G * ambientWeight);
        int b = (int) (BASE_B * ambientWeight);
        r += (int) (overheadR * (0.30 * diffOverhead + 0.40 * specOverhead));
        g += (int) (overheadG * (0.30 * diffOverhead + 0.40 * specOverhead));
        b += (int) (overheadB * (0.30 * diffOverhead + 0.40 * specOverhead));
        r += (int) (lightR * (0.40 * diffuse + 0.60 * specular));
        g += (int) (lightG * (0.40 * diffuse + 0.60 * specular));
        b += (int) (lightB * (0.40 * diffuse + 0.60 * specular));
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        double totalIntensity = (0.15 * diffOverhead) + (0.35 * specOverhead) + (0.20 * diffuse) + (0.30 * specular);
        int shadeIndex = (int) (totalIntensity * (SHADE_RAMP.length - 1));
        shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
        char renderChar = SHADE_RAMP[shadeIndex];
        double ooz = 1.0 / (rz + distanceToCamera);
        int xp = (int) (40 + 44 * ooz * rx);
        int yp = (int) (9 + 22 * ooz * ry);
        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.0001) {
                zBuffer[index] = ooz;
                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
        }
    }

    private int[] hueToRGB(double hue) {
        double h = hue * 6.0;
        int i = (int) Math.floor(h);
        double f = h - i;
        int pv = 0;
        int qv = (int) (255 * (1.0 - f));
        int tv = (int) (255 * f);
        switch (i % 6) {
            case 0:
                return new int[] { 255, tv, pv };
            case 1:
                return new int[] { qv, 255, pv };
            case 2:
                return new int[] { pv, 255, tv };
            case 3:
                return new int[] { pv, qv, 255 };
            case 4:
                return new int[] { tv, pv, 255 };
            case 5:
                return new int[] { 255, pv, qv };
            default:
                return new int[] { 255, 255, 255 };
        }
    }
}