// TODO: Improve the Halo Ring details and maybe make it slowly drift to show 3Dness or something?

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
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double timeClock = 0;

    // Palette registers for true UNSC/Forerunner hardware displays
    private static final int[] C_METAL_RIM  = { 95, 100, 110 };   // Dark matte metal walls
    private static final int[] C_LANDMASS   = { 35, 90, 45 };     // Terrain green valleys
    private static final int[] C_ATMOSPHERE = { 100, 155, 215 };  // Deep space Rayleigh scatter haze
    private static final int[] C_CLOUDS     = { 235, 240, 250 };  // High-altitude cloud banks

    public HaloRingLoader() {
        super(HALO_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0;
        Random rand = new Random(343); // Guilty Spark seed
        for (int i = 0; i < MAX_STARS; i++) {
            int rx = rand.nextInt(80);
            int ry = rand.nextInt(22);
            starPositions[i] = ry * 80 + rx;
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Draw Deep Space Background Starfield
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            int idx = starPositions[i];
            double twinkle = Math.sin((currentTime * 0.003) + starPhases[i]);
            if (twinkle > 0.82 && idx >= 0 && idx < 1760) {
                zBuffer[idx] = 0.00001;
                outputBuffer[idx] = "\u001B[37m.\u001B[0m";
            }
        }

        // Advance smooth terrain and atmospheric cloud drift speed
        timeClock += 0.006;

        // Mega-structure geometry scale parameters
        double ringRadius = 6.0;
        double ringWidth = 1.2;

        // Camera sitting directly on the bottom inner surface, looking up and forward down the spine
        double camY = -ringRadius + 0.18; 
        double camZ = 0.0;

        // ---------------------------------------------------------------------
        // Step 2: Inverse Pixel-Perfect Raymarching Loop
        // ---------------------------------------------------------------------
        for (int y = 0; y < 22; y++) {
            // Screen normalization to centered camera view coordinates
            double screenY = (11.0 - y) / 22.0; 

            for (int x = 0; x < 80; x++) {
                int pixelIndex = x + 80 * y;
                // Correct for terminal cell width-to-height aspect ratio (~2.0)
                double screenX = (x - 40.0) / 40.0 * 1.8; 

                // Generate primary ray direction vector pointing down the viewport tunnel
                double rayX = screenX;
                double rayY = screenY;
                double rayZ = 1.0; 

                // Normalize ray direction vector
                double rLen = Math.sqrt(rayX * rayX + rayY * rayY + rayZ * rayZ);
                rayX /= rLen; rayY /= rLen; rayZ /= rLen;

                // Infinite Inside-Cylinder Intersection Math: (camY + t*rayY)^2 + (camZ + t*rayZ)^2 = R^2
                // Formulate quadratic coefficients: a*t^2 + b*t + c = 0
                double a = rayY * rayY + rayZ * rayZ;
                double b = 2.0 * (camY * rayY + camZ * rayZ);
                double c = (camY * camY + camZ * camZ) - (ringRadius * ringRadius);
                double discriminant = b * b - 4.0 * a * c;

                // If ray doesn't hit the structure cylinder wall, it escapes into background deep space
                if (discriminant < 0) continue;

                // Solve for the nearest forward intersection distance along the ray path
                double t = (-b + Math.sqrt(discriminant)) / (2.0 * a);
                if (t <= 0.1) t = (-b - Math.sqrt(discriminant)) / (2.0 * a);
                if (t <= 0.1) continue; 

                // Calculate the precise 3D intersection point in world coordinate space
                double hitX = t * rayX;
                double hitY = camY + t * rayY;
                double hitZ = camZ + t * rayZ;

                // Validate if point falls within the width slice bounds of the installation ring
                if (Math.abs(hitX) > (ringWidth / 2.0)) continue;

                // Map intersection coordinates into linear surface coordinates for high-density mapping
                double angleTheta = Math.atan2(hitZ, hitY) + Math.PI / 2.0; 
                if (angleTheta < 0) angleTheta += 2.0 * Math.PI;

                double ooz = 1.0 / t;
                if (ooz > zBuffer[pixelIndex]) {
                    zBuffer[pixelIndex] = ooz;

                    // Procedural Texturing: Isolate structural containing side rims
                    boolean isRim = (Math.abs(hitX) > (ringWidth / 2.0 - 0.08));

                    // High-frequency analytical noise masks for land and cloud patterns
                    double terrainNoise = Math.sin(hitX * 18.0) * Math.cos(angleTheta * 24.0 + timeClock)
                                        + 0.4 * Math.sin(hitX * 35.0 + angleTheta * 50.0);
                    double cloudNoise = Math.sin(hitX * 6.0 + angleTheta * 10.0 - timeClock * 1.5)
                                      * Math.cos(hitX * 3.0 - angleTheta * 4.0);

                    int rd, gr, bl;
                    char renderChar = '=';

                    if (isRim) {
                        // High-detail metallic structural framing bands
                        double ribPattern = Math.sin(angleTheta * 160.0);
                        double rimShade = (ribPattern > 0.4) ? 0.65 : 0.85;
                        rd = (int) (C_METAL_RIM[0] * rimShade);
                        gr = (int) (C_METAL_RIM[1] * rimShade);
                        bl = (int) (C_METAL_RIM[2] * rimShade);
                        renderChar = (ribPattern > 0.4) ? '#' : '%';
                    } else if (cloudNoise > 0.35 && t > 1.5) {
                        // Fluffy high-altitude weather structures circling the internal ring valley
                        double cloudDensity = (cloudNoise - 0.35) / 0.65;
                        rd = (int) (C_CLOUDS[0] * (0.8 + 0.2 * cloudDensity));
                        gr = (int) (C_CLOUDS[1] * (0.8 + 0.2 * cloudDensity));
                        bl = (int) (C_CLOUDS[2]);
                        renderChar = '@';
                    } else if (terrainNoise > 0.05) {
                        // Complex continents, islands, and high mountain ranges
                        rd = C_LANDMASS[0]; gr = C_LANDMASS[1]; bl = C_LANDMASS[2];
                        renderChar = terrainNoise > 0.4 ? '#' : '+';
                    } else {
                        // Deep water surfaces and installation internal sub-structures
                        rd = 25; gr = 50; bl = 100;
                        renderChar = '.';
                    }

                    // ---------------------------------------------------------------------
                    // Step 3: High-Fidelity Environmental Shading Pipelines
                    // ---------------------------------------------------------------------
                    // Shading Metric A: Distant Horizon Gas Scatter Fade
                    double distanceFactor = Math.min(1.0, t / 14.0);

                    // Shading Metric B: Structural Shadow Band (The Halo casting an obstruction shadow onto itself)
                    double shadowFactor = 1.0;
                    if (angleTheta > Math.PI * 0.35 && angleTheta < Math.PI * 0.90) {
                        shadowFactor = 0.30; // Deep shade dropped across the middle arch loop
                    }

                    // Shading Metric C: Localized diffuse luminance based on screen orientation height
                    double heightLuminance = 0.4 + 0.6 * Math.clamp((double)(22 - y) / 22.0, 0.0, 1.0);
                    double finalShade = shadowFactor * heightLuminance;

                    // Apply shading mutations to primary colors
                    rd = (int) (rd * finalShade);
                    gr = (int) (gr * finalShade);
                    bl = (int) (bl * finalShade);

                    // Smooth-blend pixels into light blue atmospheric scattering haze over long distance
                    rd = (int) ((1.0 - distanceFactor) * rd + distanceFactor * C_ATMOSPHERE[0]);
                    gr = (int) ((1.0 - distanceFactor) * gr + distanceFactor * C_ATMOSPHERE[1]);
                    bl = (int) ((1.0 - distanceFactor) * bl + distanceFactor * C_ATMOSPHERE[2]);

                    // Absolute RGB safety boundary checking
                    rd = Math.max(0, Math.min(255, rd));
                    gr = Math.max(0, Math.min(255, gr));
                    bl = Math.max(0, Math.min(255, bl));

                    String colorCode = String.format("\u001B[38;2;%d;%d;%dm", rd, gr, bl);
                    outputBuffer[pixelIndex] = colorCode + renderChar + RESET;
                }
            }
        }
    }
}
