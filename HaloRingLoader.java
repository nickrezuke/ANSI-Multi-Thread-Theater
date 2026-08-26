import java.util.Random;

public class HaloRingLoader extends Loader {
    private static final StatusStage[] HALO_STAGES = {
            new StatusStage(20, "Establishing slipspace exit trajectory:"),
            new StatusStage(45, "Mapping inner-surface topographical plates:"),
            new StatusStage(70, "Igniting atmospheric containment fields:"),
            new StatusStage(90, "Synchronizing installation superstructure:"),
            new StatusStage(100, "Halo Installation Grid Active!")
    };

    private static final int MAX_STARS = 45;
    private final int[] starX = new int[MAX_STARS];
    private final int[] starY = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double timeClock = 0;

    // Palette registers for true UNSC/Forerunner hardware displays
    private static final int[] C_METAL_RIM = { 85, 90, 100 }; // Dark matte metal walls
    private static final int[] C_LANDMASS = { 35, 90, 45 }; // Terrain green valleys
    private static final int[] C_ATMOSPHERE = { 100, 155, 215 }; // Deep space Rayleigh scatter haze
    private static final int[] C_CLOUDS = { 235, 240, 250 }; // High-altitude cloud banks

    public HaloRingLoader() {
        super(HALO_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0;
        Random rand = new Random(343); // Guilty Spark seed
        for (int i = 0; i < MAX_STARS; i++) {
            starX[i] = rand.nextInt(80);
            starY[i] = rand.nextInt(22);
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Advance smooth terrain and atmospheric cloud drift speed
        timeClock += 0.015;

        // Dynamic Camera Drift (Yaw and Pitch)
        double camYaw = Math.sin(timeClock * 0.4) * 0.28; // Gently sweeping left/right
        double camPitch = 0.35 + Math.sin(timeClock * 0.3) * 0.05; // Looking upward into the ring

        // Step 1: Draw Deep Space Background Starfield (with parallax matching camera
        // yaw)
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            // Shift stars in opposite direction of camera pan for parallax depth
            int shiftedX = (int) (starX[i] - camYaw * 35.0) % 80;
            if (shiftedX < 0)
                shiftedX += 80;
            int idx = shiftedX + starY[i] * 80;

            double twinkle = Math.sin((currentTime * 0.003) + starPhases[i]);
            if (twinkle > 0.82 && idx >= 0 && idx < 1760) {
                zBuffer[idx] = 0.00001;
                outputBuffer[idx] = "\u001B[37m.\u001B[0m"; // White twinkle
            }
        }

        // Mega-structure geometry scale parameters
        double ringRadius = 6.0;
        double ringWidth = 1.25;

        // Camera sitting directly on the bottom inner surface, looking up and forward
        double camY = -ringRadius + 0.18;
        double camZ = 0.0;

        double cy = Math.cos(camYaw), sy = Math.sin(camYaw);
        double cp = Math.cos(camPitch), sp = Math.sin(camPitch);

        // ---------------------------------------------------------------------
        // Step 2: Inverse Pixel-Perfect Raymarching Loop
        // ---------------------------------------------------------------------
        for (int y = 0; y < 22; y++) {
            // Screen normalization to centered camera view coordinates
            double screenY = (11.0 - y) / 22.0;

            for (int x = 0; x < 80; x++) {
                int pixelIndex = x + 80 * y;
                double screenX = (x - 40.0) / 40.0 * 1.8;

                // Generate primary ray direction vector
                double rayX = screenX;
                double rayY = screenY;
                double rayZ = 1.0;

                // Apply Camera Pitch (tilt)
                double tempY = rayY * cp - rayZ * sp;
                double tempZ = rayY * sp + rayZ * cp;
                rayY = tempY;
                rayZ = tempZ;

                // Apply Camera Yaw (pan)
                double tempX = rayX * cy - rayZ * sy;
                tempZ = rayX * sy + rayZ * cy;
                rayX = tempX;
                rayZ = tempZ;

                // Normalize ray direction vector
                double rLen = Math.sqrt(rayX * rayX + rayY * rayY + rayZ * rayZ);
                rayX /= rLen;
                rayY /= rLen;
                rayZ /= rLen;

                // Infinite Inside-Cylinder Intersection Math
                double a = rayY * rayY + rayZ * rayZ;
                double b = 2.0 * (camY * rayY + camZ * rayZ);
                double c = (camY * camY + camZ * camZ) - (ringRadius * ringRadius);
                double discriminant = b * b - 4.0 * a * c;

                if (discriminant < 0)
                    continue; // Ray missed into space

                // Solve for the nearest forward intersection distance
                double t = (-b + Math.sqrt(discriminant)) / (2.0 * a);
                if (t <= 0.1)
                    t = (-b - Math.sqrt(discriminant)) / (2.0 * a);
                if (t <= 0.1)
                    continue;

                // 3D intersection point in world space
                double hitX = t * rayX;
                double hitY = camY + t * rayY;
                double hitZ = camZ + t * rayZ;

                // Validate bounds of the installation ring's width
                if (Math.abs(hitX) > (ringWidth / 2.0))
                    continue;

                // Map intersection coordinates into linear surface coordinates
                double angleTheta = Math.atan2(hitZ, hitY) + Math.PI / 2.0;
                if (angleTheta < 0)
                    angleTheta += 2.0 * Math.PI;

                double ooz = 1.0 / t;
                if (ooz > zBuffer[pixelIndex]) {
                    zBuffer[pixelIndex] = ooz;

                    // Procedural Texturing Masks
                    boolean isRim = (Math.abs(hitX) > (ringWidth / 2.0 - 0.10));

                    // High-frequency analytical noise
                    double terrainNoise = Math.sin(hitX * 18.0) * Math.cos(angleTheta * 24.0 + timeClock * 0.5)
                            + 0.5 * Math.sin(hitX * 45.0 + angleTheta * 40.0);
                    double cloudNoise = Math.sin(hitX * 6.0 + angleTheta * 10.0 - timeClock * 1.5)
                            * Math.cos(hitX * 3.0 - angleTheta * 4.0);

                    int rd, gr, bl;
                    char renderChar = '=';

                    if (isRim) {
                        // Metallic structural ribs
                        double ribPattern = Math.sin(angleTheta * 180.0);
                        double rimShade = (ribPattern > 0.4) ? 0.75 : 0.45;
                        rd = (int) (C_METAL_RIM[0] * rimShade);
                        gr = (int) (C_METAL_RIM[1] * rimShade);
                        bl = (int) (C_METAL_RIM[2] * rimShade);
                        renderChar = (ribPattern > 0.4) ? '#' : '=';
                    } else if (cloudNoise > 0.35 && t > 1.5) {
                        // Fluffy high-altitude weather structures
                        double cloudDensity = (cloudNoise - 0.35) / 0.65;
                        rd = (int) (C_CLOUDS[0] * (0.8 + 0.2 * cloudDensity));
                        gr = (int) (C_CLOUDS[1] * (0.8 + 0.2 * cloudDensity));
                        bl = (int) (C_CLOUDS[2]);
                        renderChar = '@';
                    } else if (terrainNoise > 0.1) {
                        // Landmasses (Mountains vs Plains)
                        rd = C_LANDMASS[0];
                        gr = C_LANDMASS[1];
                        bl = C_LANDMASS[2];
                        renderChar = terrainNoise > 0.45 ? 'A' : '+';
                    } else {
                        // Deep water oceans
                        rd = 20;
                        gr = 60;
                        bl = 120;
                        renderChar = '~';
                    }

                    // ---------------------------------------------------------------------
                    // Step 3: High-Fidelity Environmental Shading Pipelines
                    // ---------------------------------------------------------------------

                    // Shading Metric A: Exponential Atmospheric Fog
                    double distanceFactor = 1.0 - Math.exp(-t * 0.08);

                    // Shading Metric B: Angled Planetary Eclipse Shadow
                    // Creates a diagonal band of darkness that slowly washes over the ring
                    double shadowPhase = Math.sin(angleTheta * 1.1 + hitX * 2.0 - timeClock * 0.7);
                    double shadowFactor = 1.0;
                    if (shadowPhase > 0.8)
                        shadowFactor = 0.15; // Umbra (Deep shadow)
                    else if (shadowPhase > 0.6)
                        shadowFactor = 0.45; // Penumbra (Soft edge)

                    // Shading Metric C: Localized diffuse luminance based on orientation
                    double clampedHeight = Math.max(0.0, Math.min(1.0, (double) (22 - y) / 22.0));
                    double heightLuminance = 0.4 + 0.6 * clampedHeight;

                    double finalShade = shadowFactor * heightLuminance;
                    rd = (int) (rd * finalShade);
                    gr = (int) (gr * finalShade);
                    bl = (int) (bl * finalShade);

                    // Blend pixels into atmospheric haze over long distance
                    rd = (int) ((1.0 - distanceFactor) * rd + distanceFactor * C_ATMOSPHERE[0]);
                    gr = (int) ((1.0 - distanceFactor) * gr + distanceFactor * C_ATMOSPHERE[1]);
                    bl = (int) ((1.0 - distanceFactor) * bl + distanceFactor * C_ATMOSPHERE[2]);

                    // RGB safety boundary checking
                    rd = Math.max(0, Math.min(255, rd));
                    gr = Math.max(0, Math.min(255, gr));
                    bl = Math.max(0, Math.min(255, bl));

                    String colorCode = String.format("\u001B[38;2;%d;%d;%dm", rd, gr, bl);

                    // Ensure deep space black doesn't get drawn over the background if we reached
                    // max render distance
                    if (distanceFactor < 0.98) {
                        outputBuffer[pixelIndex] = colorCode + renderChar + RESET;
                    }
                }
            }
        }
    }
}