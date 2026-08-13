import java.util.Arrays;

public class XboxLogoLoader extends Loader {
    private static final StatusStage[] XBOX_STAGES = {
            new StatusStage(25, "Forging spherical cyber-shield matrix:"),
            new StatusStage(50, "Carving recessed internal 'X' chasms:"),
            new StatusStage(75, "Igniting radioactive green phosphor glow:"),
            new StatusStage(100, "Xbox Core Protocol Fully Engaged!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };
    private double rotationAngle = 0.0;
    private double timeClock = 0.0;
    private final int width = 80;
    private final int height = 22;
    private static final double CAMERA_DISTANCE = 3.2;

    // Authentic early 2000s high-contrast cyber color palette registers
    private static final int[] RGB_SHIELD_BASE = { 45, 48, 52 }; // Brushed Steel Grey
    private static final int[] RGB_SHIELD_LIT = { 100, 110, 115 }; // Metallic Specular Highlights
    private static final int[] RGB_PLASMA_CORE = { 40, 255, 30 }; // Blazing Radioactive Green
    private static final int[] RGB_PLASMA_EDGE = { 0, 100, 15 }; // Deep Emerald Undertone

    public XboxLogoLoader() {
        super(XBOX_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.rotationAngle = 0.0;
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear terminal frame to a deep vacuum space void
        Arrays.fill(outputBuffer, " ");

        // Advance the independent linear time phase clock uniformly
        timeClock += 0.020;

        // Base continuous background spin speed
        double baseVelocity = 0.006;

        // Smoothly brake the velocity down to a slow crawl as it hits the iconic
        // front-facing frame
        // Math.cos(timeClock) swings between -1.0 and 1.0, cushioning the delta
        // increment step smoothly
        double smoothVelocityModifier = 0.024 * (1.0 + Math.cos(timeClock));

        // Increment the actual rotation angle by the unified, un-looped continuous step
        rotationAngle += baseVelocity + smoothVelocityModifier;

        double cosR = Math.cos(rotationAngle), sinR = Math.sin(rotationAngle);

        // Fixed cinematic pitch downward tilt (~18 degrees) to peer slightly into the X
        // grooves
        double pitch = 1.12 + 5 * smoothVelocityModifier;
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch);

        // Directional overhead spotlight vector matching the classic logo glare
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;
        double sphereRadius = 0.95;

        // High-density 3D spheroid scan loops
        for (int tIndex = 0; tIndex < 90; tIndex++) {
            double theta = (tIndex / 90.0) * Math.PI;
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

            for (int pIndex = 0; pIndex < 180; pIndex++) {
                double phi = (pIndex / 180.0) * 2.0 * Math.PI;
                double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);

                // Local unrotated coordinates of a perfect sphere
                double lx = sphereRadius * sinTheta * cosPhi;
                double ly = sphereRadius * cosTheta;
                double lz = sphereRadius * sinTheta * sinPhi;

                // --- THE STRUCTURAL "X" CHASM MASK RULE ---
                double xCut = Math.abs(lx);
                double yConeSpace = Math.abs(ly) * 0.42 + 0.18; // Pinch throttling factor

                // FIXED: Extended the polar Y-axis clamp from 0.88 out to 0.98 to prevent the
                // chasm from black-holing at the apex
                boolean insideXChasm = Math.abs(xCut - Math.abs(lz)) < yConeSpace && Math.abs(ly) < 0.98;

                // Calculate surface normal vectors for shading
                double rNx = sinTheta * cosPhi;
                double rNy = cosTheta;
                double rNz = sinTheta * sinPhi;

                // --- 3D MATRIX ROTATION PIPELINE ---
                // Pass A: Spin coordinates around the vertical Y-axis
                double x1 = lx * cosR + lz * sinR;
                double y1 = ly;
                double z1 = -lx * sinR + lz * cosR;

                double nx1 = rNx * cosR + rNz * sinR;
                double ny1 = rNy;
                double nz1 = -rNx * sinR + rNz * cosR;

                // Pass B: Apply fixed camera pitch tilt lookdown
                double finalX = x1;
                double finalY = y1 * cosP + z1 * sinP;
                double finalZ = -y1 * sinP + z1 * cosP;

                double worldNx = nx1;
                double worldNy = ny1 * cosP + nz1 * sinP;
                double worldNz = -ny1 * sinP + nz1 * cosP;

                // Project 3D vectors directly to terminal 2D character row cells
                double ooz = 1.0 / (finalZ + CAMERA_DISTANCE);
                int xp = (int) (40 + 44 * ooz * finalX * 2.2); // Aspect stretch factor
                int yp = (int) (11 - 19 * ooz * finalY);

                if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
                    int idx = xp + width * yp;

                    // Standard Z-Buffer depth optimization validation pass
                    if (ooz > zBuffer[idx] + 0.0001) {
                        zBuffer[idx] = ooz;

                        // Lambertian reflectance illumination shader
                        double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
                        double shade = 0.50 + 0.50 * Math.max(0.0, luminance);

                        int[] baseRGB = RGB_SHIELD_BASE;
                        char renderChar = '█';

                        if (insideXChasm) {
                            // --- LAYER 2: CHROMA RADIONUCLIDE PLASMA RECESSED LAYER ---
                            double edgeFalloff = Math.abs(xCut - Math.abs(lz)) / yConeSpace;

                            // Blend glowing center cores into rich emerald boundaries
                            int rGlow = (int) (RGB_PLASMA_CORE[0] * (1.0 - edgeFalloff)
                                    + RGB_PLASMA_EDGE[0] * edgeFalloff);
                            int gGlow = (int) (RGB_PLASMA_CORE[1] * (1.0 - edgeFalloff)
                                    + RGB_PLASMA_EDGE[1] * edgeFalloff);
                            int bGlow = (int) (RGB_PLASMA_CORE[2] * (1.0 - edgeFalloff)
                                    + RGB_PLASMA_EDGE[2] * edgeFalloff);

                            baseRGB = new int[] { rGlow, gGlow, bGlow };

                            // Use medium mesh fills inside the chasm to emulate a glowing gas density
                            renderChar = (edgeFalloff < 0.4) ? '█' : '▓';
                        } else {
                            // --- LAYER 1: OUTER METALLIC SHIELD PLATES ---
                            if (luminance > 0.45) {
                                baseRGB = RGB_SHIELD_LIT; // Sunlight catching steel glare
                                renderChar = '█';
                            } else {
                                int shadeIndex = (int) ((luminance + 1.0) * 5.5);
                                shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
                                renderChar = SHADE_RAMP[shadeIndex];

                                // FIXED: If the character resolves to a space or a faint dot due to the steep
                                // camera angle shadow,
                                // force a baseline solid dither '░' so it draws the metal plates instead of
                                // cutting holes.
                                if (renderChar == ' ' || renderChar == '.') {
                                    renderChar = '░';
                                }
                            }
                        }

                        int r = (int) (baseRGB[0] * (insideXChasm ? 1.0 : shade));
                        int g = (int) (baseRGB[1] * (insideXChasm ? 1.0 : shade));
                        int b = (int) (baseRGB[2] * (insideXChasm ? 1.0 : shade));

                        String esc = String.format("\u001B[38;2;%d;%d;%dm",
                                Math.max(0, Math.min(255, r)),
                                Math.max(0, Math.min(255, g)),
                                Math.max(0, Math.min(255, b)));
                        outputBuffer[idx] = esc + renderChar + RESET;
                    }
                }
            }
        }
    }
}
