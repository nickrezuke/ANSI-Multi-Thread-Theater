public class RainbowWhispSphereLoader extends Loader {
    private static final StatusStage[] REFLECTIVE_STAGES = {
        new StatusStage(30, "Calibrating curved surface normals:"),
        new StatusStage(65, "Projecting dual-light matrix:"),
        new StatusStage(90, "Focusing rainbow specular lens:"),
        new StatusStage(100, "Reflective Core Operational!")
    };

    // 12-Step Micro-Granular Shading Scale (From index 0: Space Vacuum to index 11: Solid Peak Core)
    private static final char[] SHADE_RAMP = {
        '\u00B7', // 0:  · (Faint Gravitational Dust Tail)
        '\u2058', // 1:  ⁘ (Four-Dot Scatter Punctuation)
        '\u2022', // 2:  • (Defined Outer Bullet Filament)
        '\u00A4', // 3:  ¤ (Particle Node)
        '\u205C', // 4:  ⁜ (Dotted Cross - Airy Technical Grain)
        ':',      // 5:  : (Standard Density Anchor)
        '=',      // 6:  = (Mid-Weight Structure)
        '\u2591', // 7:  ░ (Light Vapor Block)
        '\u2592', // 8:  ▒ (Fluid Mid-Tone Block)
        '\u2593', // 9:  ▓ (Dense Plasma Layer Block)
        '\u2588', // 10: █ (Blazing Peak Core Block)
        '\u2588'  // 11: █ (Overdrive Core Guard)
    };

    private double lightAngle = 0.0;
    private double sphereRotation = 0.0;
    private double colorHue = 0.0;

    private static final int BASE_R = 140;
    private static final int BASE_G = 145;
    private static final int BASE_B = 155;

    public RainbowWhispSphereLoader() {
        super(REFLECTIVE_STAGES);
    }

    @Override
    protected void initialize() {}

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // 1. Permanent Studio Light (Upper-Right-Front rim contour)
        double overheadX = 0.577;
        double overheadY = -0.707; 
        double overheadZ = -0.408;   
        int overheadR = 220, overheadG = 230, overheadB = 250;

        // 2. Dynamic Orbiting Rainbow Light Core
        double lightRadius = 1.6;
        double lightX = lightRadius * Math.cos(lightAngle);
        double lightY = 0.3 * Math.sin(lightAngle * 1.5); 
        double lightZ = lightRadius * Math.sin(lightAngle);

        double cameraDistance = 3.6;
        double lightOoz = 1.0 / (lightZ + cameraDistance);
        int lightXp = (int) (40 + 66 * lightOoz * lightX);
        int lightYp = (int) (11 + 33 * lightOoz * lightY);

        int[] rainbowRGB = hueToRGB(colorHue);
        int lightR = rainbowRGB[0];
        int lightG = rainbowRGB[1];
        int lightB = rainbowRGB[2];

        // 3. Render the Smooth 3D Sphere Geometry
        double sphereRadius = 1.0;
        for (double theta = 0; theta < Math.PI; theta += 0.02) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.02) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double x = sphereRadius * sinTheta * cosPhi;
                double y = sphereRadius * sinTheta * sinPhi;
                double z = sphereRadius * cosTheta;

                double cosR = Math.cos(sphereRotation), sinR = Math.sin(sphereRotation);
                double rx = x * cosR - z * sinR;
                double ry = y;
                double rz = x * sinR + z * cosR;

                double ooz = 1.0 / (rz + cameraDistance);
                int xp = (int) (40 + 75 * ooz * rx);
                int yp = (int) (11 + 38 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        double nx = rx / sphereRadius;
                        double ny = ry / sphereRadius;
                        double nz = rz / sphereRadius;

                        double viewX = -rx;
                        double viewY = -ry;
                        double viewZ = -(rz + cameraDistance);
                        double distToCam = Math.sqrt(viewX*viewX + viewY*viewY + viewZ*viewZ);
                        if (distToCam > 0) { viewX /= distToCam; viewY /= distToCam; viewZ /= distToCam; }

                        // -------------------------------------------------------------
                        // LIGHT A: PERMANENT OVERHEAD RIM LIGHT
                        // -------------------------------------------------------------
                        double diffOverhead = nx * overheadX + ny * overheadY + nz * overheadZ;
                        double specOverhead = 0;
                        
                        if (diffOverhead < 0) {
                            diffOverhead = 0;
                        } else {
                            double refOverheadX = 2.026 * diffOverhead * nx - overheadX;
                            double refOverheadY = 2.026 * diffOverhead * ny - overheadY;
                            double refOverheadZ = 2.026 * diffOverhead * nz - overheadZ;

                            double specOverheadDot = refOverheadX * viewX + refOverheadY * viewY + refOverheadZ * viewZ;
                            specOverhead = (specOverheadDot > 0) ? Math.pow(specOverheadDot, 16) : 0;
                        }

                        // -------------------------------------------------------------
                        // LIGHT B: MOVING RAINBOW LIGHT WITH HORIZON OCCLUSION
                        // -------------------------------------------------------------
                        double toLightX = lightX - rx;
                        double toLightY = lightY - ry;
                        double toLightZ = lightZ - rz;
                        double distToLight = Math.sqrt(toLightX*toLightX + toLightY*toLightY + toLightZ*toLightZ);
                        if (distToLight > 0) { toLightX /= distToLight; toLightY /= distToLight; toLightZ /= distToLight; }

                        double diffRainbow = nx * toLightX + ny * toLightY + nz * toLightZ;
                        double specRainbow = 0;

                        if (diffRainbow < 0) {
                            diffRainbow = 0;
                        } else {
                            double refRainbowX = 2.04 * diffRainbow * nx - toLightX;
                            double refRainbowY = 2.04 * diffRainbow * ny - toLightY;
                            double refRainbowZ = 2.04 * diffRainbow * nz - toLightZ;

                            double specRainbowDot = refRainbowX * viewX + refRainbowY * viewY + refRainbowZ * viewZ;
                            specRainbow = (specRainbowDot > 0) ? Math.pow(specRainbowDot, 32) : 0;
                        }

                        // Ambient environment baseline illumination
                        double ambient = 0.34;

                        // -------------------------------------------------------------
                        // COMPOSING THE CHANNELS
                        // -------------------------------------------------------------
                        int outR = (int) (BASE_R * ambient);
                        int outG = (int) (BASE_G * ambient);
                        int outB = (int) (BASE_B * ambient);

                        outR += (int) (overheadR * (0.5 * diffOverhead + 0.6 * specOverhead));
                        outG += (int) (overheadG * (0.5 * diffOverhead + 0.6 * specOverhead));
                        outB += (int) (overheadB * (0.5 * diffOverhead + 0.6 * specOverhead));

                        outR += (int) (lightR * (0.3 * diffRainbow + 1.0 * specRainbow));
                        outG += (int) (lightG * (0.3 * diffRainbow + 1.0 * specRainbow));
                        outB += (int) (lightB * (0.3 * diffRainbow + 1.0 * specRainbow));

                        outR = Math.max(0, Math.min(255, outR));
                        outG = Math.max(0, Math.min(255, outG));
                        outB = Math.max(0, Math.min(255, outB));

                        // Character Shading Selector Math mapped to 12 elements
                        double totalIntensity = (0.3 * diffOverhead) + (0.4 * specOverhead) + (0.1 * diffRainbow) + (0.7 * specRainbow);
                        int shadeIndex = (int) (totalIntensity * (SHADE_RAMP.length - 1));
                        shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
                        char renderChar = SHADE_RAMP[shadeIndex];

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", outR, outG, outB);
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }

        // 4. Render the floating light orb itself into buffer space
        if (lightXp >= 0 && lightXp < 80 && lightYp >= 0 && lightYp < 22) {
            int lightIndex = lightXp + 80 * lightYp;
            if (lightOoz > zBuffer[lightIndex]) {
                zBuffer[lightIndex] = lightOoz;
                String coreColor = String.format("\u001B[38;2;%d;%d;%dm", lightR, lightG, lightB);
                outputBuffer[lightIndex] = coreColor + "\u2588" + RESET; // Render light as a solid block core
            }
        }

        lightAngle += 0.035;
        sphereRotation += 0.004;
        colorHue += 0.0035; 
        if (colorHue > 1.0) colorHue -= 1.0;
    }

    private int[] hueToRGB(double hue) {
        double h = hue * 6.0;
        int i = (int) Math.floor(h);
        double f = h - i;
        int pv = 40; 
        int qv = (int) (255 * (1.0 - f));
        int tv = (int) (255 * f);
        switch (i % 6) {
            case 0: return new int[]{ 255, tv, pv };
            case 1: return new int[]{ qv, 255, pv };
            case 2: return new int[]{ pv, 255, tv };
            case 3: return new int[]{ pv, qv, 255 };
            case 4: return new int[]{ tv, pv, 255 };
            case 5: return new int[]{ 255, pv, qv };
            default: return new int[]{ 255, 255, 255 };
        }}}