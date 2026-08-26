public class ChicagoBeanLoader extends Loader {
    private static final StatusStage[] BEAN_STAGES = {
        new StatusStage(25, "Casting molten stainless steel:"),
        new StatusStage(50, "Welding 168 mirrored plates:"),
        new StatusStage(75, "Buffing seamless chrome finish:"),
        new StatusStage(100, "Cloud Gate Installed in Millennium Park!")
    };

    // 12-Step Micro-Granular Shading Scale (reused from the reflective sphere)
    private static final char[] SHADE_RAMP = {
        '\u00B7', '\u2058', '\u2022', '\u00A4', '\u205C', ':', '=',
        '\u2591', '\u2592', '\u2593', '\u2588', '\u2588'
    };

    private double rotation = 0.0;

    // Brushed stainless steel base tint
    private static final int BASE_R = 140;
    private static final int BASE_G = 145;
    private static final int BASE_B = 155;

    // Overhead studio light (the sphere's rim light)
    private static final double OVERHEAD_X = 0.577;
    private static final double OVERHEAD_Y = -0.707;
    private static final double OVERHEAD_Z = -0.408;
    private static final int OVERHEAD_R = 220, OVERHEAD_G = 230, OVERHEAD_B = 250;

    // Fake environment reflection: sky above, plaza/ground below
    private static final int SKY_R = 90, SKY_G = 140, SKY_B = 195;
    private static final int GROUND_R = 95, GROUND_G = 85, GROUND_B = 75;

    // Bean body proportions (Cloud Gate is ~66ft long x 33ft high x 42ft wide)
    private static final double RADIUS_X = 1.8;
    private static final double RADIUS_Y = 0.82;
    private static final double RADIUS_Z = 1.05;

    // Subtle horizontal waist cinch
    private static final double WAIST_DEPTH = 0.10;
    private static final double WAIST_SHARPNESS = 3.0;

    // The underside archway (omphalos) - a concave carve at the bottom-center
    private static final double ARCH_DEPTH = 0.55;
    private static final double ARCH_SHARPNESS = 8.0;

    private static final double CAMERA_DISTANCE = 3.6;
    private static final double PROJ_X_SCALE = 42;
    private static final double PROJ_Y_SCALE = 46;

    public ChicagoBeanLoader() {
        // This uses 80x22 specifically
        super(BEAN_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {}

    // Reusable scratch space so the hot render loop doesn't allocate a
    // fresh array for every one of the ~30k surface points every frame.
    private final double[] scratchP = new double[3];
    private final double[] scratchPt = new double[3];
    private final double[] scratchPp = new double[3];
    private final double[] scratchN = new double[3];

    // Parametric bean surface: an elongated ellipsoid with a gentle waist
    // pinch and a concave archway carved into the bottom-center underside.
    // Writes the result into `out` instead of allocating a new array.
    private void beanSurface(double theta, double phi, double[] out) {
        double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
        double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);

        double x0 = sinTheta * cosPhi;
        double y0 = -cosTheta; // engine convention is +Y = screen-down, so this keeps theta=pi at the visible bottom
        double z0 = sinTheta * sinPhi;

        double waist = 1.0 - WAIST_DEPTH * Math.exp(-WAIST_SHARPNESS * x0 * x0);

        double bottomness = Math.max(0.0, y0);
        double centerness = Math.exp(-ARCH_SHARPNESS * x0 * x0);
        double bottomness2 = bottomness * bottomness;
        double archStrength = bottomness2 * bottomness * centerness; // bottomness^3
        double archPull = 1.0 - ARCH_DEPTH * archStrength;

        double scaleYZ = waist * archPull;

        out[0] = RADIUS_X * x0;
        out[1] = RADIUS_Y * y0 * scaleYZ;
        out[2] = RADIUS_Z * z0 * scaleYZ;
    }

    // Surface normal via finite differences (needed since the arch carve
    // means this isn't a simple analytic ellipsoid anymore). Takes the
    // already-computed center point to avoid recomputing it a second time,
    // and writes into `out` to avoid allocating.
    private void beanNormal(double theta, double phi, double[] p, double[] out) {
        double eps = 0.001;
        beanSurface(theta + eps, phi, scratchPt);
        beanSurface(theta, phi + eps, scratchPp);

        double dtx = (scratchPt[0] - p[0]) / eps, dty = (scratchPt[1] - p[1]) / eps, dtz = (scratchPt[2] - p[2]) / eps;
        double dpx = (scratchPp[0] - p[0]) / eps, dpy = (scratchPp[1] - p[1]) / eps, dpz = (scratchPp[2] - p[2]) / eps;

        double nx = dty * dpz - dtz * dpy;
        double ny = dtz * dpx - dtx * dpz;
        double nz = dtx * dpy - dty * dpx;

        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) {
            nx = p[0]; ny = p[1]; nz = p[2];
            len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-9) len = 1;
        }
        nx /= len; ny /= len; nz /= len;

        double dot = nx * p[0] + ny * p[1] + nz * p[2];
        if (dot < 0) { nx = -nx; ny = -ny; nz = -nz; }

        out[0] = nx; out[1] = ny; out[2] = nz;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosR = Math.cos(rotation), sinR = Math.sin(rotation);

        for (double theta = 0.02; theta < Math.PI - 0.02; theta += 0.025) {
            for (double phi = 0; phi < 2 * Math.PI; phi += 0.025) {
                beanSurface(theta, phi, scratchP);
                beanNormal(theta, phi, scratchP, scratchN);

                double rx = scratchP[0] * cosR - scratchP[2] * sinR;
                double ry = scratchP[1];
                double rz = scratchP[0] * sinR + scratchP[2] * cosR;

                double nx = scratchN[0] * cosR - scratchN[2] * sinR;
                double ny = scratchN[1];
                double nz = scratchN[0] * sinR + scratchN[2] * cosR;

                double ooz = 1.0 / (rz + CAMERA_DISTANCE);
                int xp = (int) (40 + PROJ_X_SCALE * ooz * rx);
                int yp = (int) (11 + PROJ_Y_SCALE * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        double viewX = -rx, viewY = -ry, viewZ = -(rz + CAMERA_DISTANCE);
                        double distToCam = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
                        if (distToCam > 0) { viewX /= distToCam; viewY /= distToCam; viewZ /= distToCam; }

                        // Overhead studio rim light: diffuse + specular
                        double diffOverhead = nx * OVERHEAD_X + ny * OVERHEAD_Y + nz * OVERHEAD_Z;
                        double specOverhead = 0;
                        if (diffOverhead < 0) {
                            diffOverhead = 0;
                        } else {
                            double refX = 2.0 * diffOverhead * nx - OVERHEAD_X;
                            double refY = 2.0 * diffOverhead * ny - OVERHEAD_Y;
                            double refZ = 2.0 * diffOverhead * nz - OVERHEAD_Z;
                            double specDot = refX * viewX + refY * viewY + refZ * viewZ;
                            if (specDot > 0) {
                                double s2 = specDot * specDot;
                                double s4 = s2 * s2;
                                double s8 = s4 * s4;
                                specOverhead = s8 * s8; // specDot^16 via repeated squaring
                            }
                        }

                        // Fake environment reflection: blend sky (top) to ground (bottom).
                        // Engine convention is +Y = down, so an upward-facing normal (ny near -1)
                        // should pick up the sky tint, and a downward-facing one (ny near +1) the ground.
                        double envBlend = (1.0 - ny) * 0.5;
                        double envR = GROUND_R + (SKY_R - GROUND_R) * envBlend;
                        double envG = GROUND_G + (SKY_G - GROUND_G) * envBlend;
                        double envB = GROUND_B + (SKY_B - GROUND_B) * envBlend;

                        double ambient = 0.16;
                        double envWeight = 0.55;

                        double outR = BASE_R * ambient + envR * envWeight + OVERHEAD_R * (0.45 * diffOverhead + 0.65 * specOverhead);
                        double outG = BASE_G * ambient + envG * envWeight + OVERHEAD_G * (0.45 * diffOverhead + 0.65 * specOverhead);
                        double outB = BASE_B * ambient + envB * envWeight + OVERHEAD_B * (0.45 * diffOverhead + 0.65 * specOverhead);

                        int iR = (int) Math.max(0, Math.min(255, outR));
                        int iG = (int) Math.max(0, Math.min(255, outG));
                        int iB = (int) Math.max(0, Math.min(255, outB));

                        double luminance = (0.299 * iR + 0.587 * iG + 0.114 * iB) / 255.0;
                        int shadeIndex = (int) (luminance * (SHADE_RAMP.length - 1));
                        shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
                        char renderChar = SHADE_RAMP[shadeIndex];

                        String colorCode = "\u001B[38;2;" + iR + ";" + iG + ";" + iB + "m";
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }

        rotation += 0.006;
    }
}