// TODO: Improve the visual clarity of this

import java.util.Arrays;
import java.awt.Color; // Used solely for HSB->RGB conversion efficiency

public class HypersphereLoader extends Loader {

    private static final StatusStage[] HYPER_STAGES = {
        new StatusStage(25, "Calibrating 4D hyper-polar grid..."),
        new StatusStage(50, "Generating 3-sphere cellular lattice..."),
        new StatusStage(75, "Engaging stereographic hyperspace rotation..."),
        new StatusStage(100, "4D Hypersphere Online!")
    };

    // Enhanced Luminance ramp optimized for volumetric effect (sparse to dense)
    private static final char[] GLYPH_RAMP = {' ', '.', ',', ':', '-', '~', '+', '=', '*', '$', '#', '@'};

    private double angleXW = 0.0;
    private double angleXY = 0.0;
    private double angleYZ = 0.0;

    public HypersphereLoader() {
        // Utilizing 80x22 terminal dimension
        super(HYPER_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // No persistent runtime allocations needed across render cycles
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // 1. Clear frame and reset Z-buffer to maximum distance
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, Double.MAX_VALUE);

        double cosXW = Math.cos(angleXW), sinXW = Math.sin(angleXW);
        double cosXY = Math.cos(angleXY), sinXY = Math.sin(angleXY);
        double cosYZ = Math.cos(angleYZ), sinYZ = Math.sin(angleYZ);

        double radius4D = 1.0;
        double distance4D = 2.1; // Sightly pulled back perspective
        double zoom = 22.0;

        // Establish a brighter light source vector (normalized)
        double lx = -0.577, ly = 0.577, lz = -0.577;

        // Precision lattice spacing for clean, illustrative cells
        double stepPsi = Math.PI / 8.0;      // 4D Latitudinal shells
        double stepTheta = Math.PI / 12.0;   // 3D Polar angle
        double stepPhi = Math.PI / 24.0;     // Longitudinal rings

        for (double psi = stepPsi; psi < Math.PI; psi += stepPsi) {
            double sinPsi = Math.sin(psi);
            double cosPsi = Math.cos(psi);

            for (double theta = stepTheta; theta < Math.PI; theta += stepTheta) {
                double sinTheta = Math.sin(theta);
                double cosTheta = Math.cos(theta);

                for (double phi = 0; phi < 2.0 * Math.PI; phi += stepPhi) {
                    double sinPhi = Math.sin(phi);
                    double cosPhi = Math.cos(phi);

                    // 1. Raw 4D Hyper-Polar Coordinates
                    double x = radius4D * sinPsi * sinTheta * cosPhi;
                    double y = radius4D * sinPsi * sinTheta * sinPhi;
                    double z = radius4D * sinPsi * cosTheta;
                    double w = radius4D * cosPsi;

                    // 2. Perform 4D X-W Hyper-Rotation (The true "inside-out" tumbling)
                    double x4D = x * cosXW - w * sinXW;
                    double w4D = x * sinXW + w * cosXW;

                    // 3. 4D Stereographic Projection down to 3D Space Coordinates
                    double factor4D = 1.0 / (distance4D - w4D * 0.65);
                    double x3D = x4D * factor4D;
                    double y3D = y * factor4D;
                    double z3D = z * factor4D;

                    // 4. Standard 3D Spatial Rotations
                    double x1 = x3D * cosXY - y3D * sinXY;
                    double y1 = x3D * sinXY + y3D * cosXY;
                    double y2 = y1 * cosYZ - z3D * sinYZ;
                    double z2 = y1 * sinYZ + z3D * cosYZ;

                    // 5. 2D Orthographic Screen Projection (with Aspect Ratio Correction)
                    int screenX = (int) (40 + zoom * 2.2 * x1);
                    int screenY = (int) (11 + zoom * y2);

                    // Skip cells projected outside the viewport
                    if (screenX < 0 || screenX >= 80 || screenY < 0 || screenY >= 22) {
                        continue;
                    }

                    // Consolidated Depth calculation (Z + W-bias for volumetric sorting)
                    double depth = z2 + w4D * 0.4;
                    int index = screenX + 80 * screenY;

                    // 6. Execute depth test (traditional min-sorting)
                    if (depth < zBuffer[index]) {
                        zBuffer[index] = depth;

                        // 7. Surface Normal & Improved Lighting Engine
                        // Normalize the projected 3D coordinates to estimate the surface normal vector
                        double len = Math.hypot(x1, Math.hypot(y2, z2));
                        double nx = len > 0 ? x1 / len : 0;
                        double ny = len > 0 ? y2 / len : 0;
                        double nz = len > 0 ? z2 / len : 0;

                        // Calculate light exposure via standard dot product (Lambertian)
                        double exposure = nx * lx + ny * ly + nz * lz;

                        // **FIX: DARKNESS** - Map visibility to W-depth and Light exposure combined
                        double normalizeW = (w4D + 1.0) / 2.0; // [-1, 1] -> [0, 1]

                        // Visibility scales with light exposure, but also drastically with W-depth
                        double visibility = Math.max(0.1, (normalizeW * 0.7 + exposure * 0.3));

                        // 8. **FIX: SHADING** - Map volumetric GLYPH Ramp via Visibility
                        int rampIdx = (int) (visibility * (GLYPH_RAMP.length - 1));
                        rampIdx = Math.max(0, Math.min(GLYPH_RAMP.length - 1, rampIdx));
                        char glyph = GLYPH_RAMP[rampIdx];

                        // 9. **FIX: 4D COLORS** - Chroma-Depth Mapping (W -> Rainbow Hue)
                        // This uses standard HSB color space to cycle hue based on 4D rotation
                        float hue = (float) normalizeW; // Map 4D depth directly to rainbow spectrum
                        float saturation = 0.9f;         // Vivid, illustrative colors
                        float brightness = (float) Math.max(0.4, exposure * 0.5 + normalizeW * 0.5); // Ensure baseline light

                        // Boost near-field cells (high visibility) to full radiance
                        if (rampIdx > GLYPH_RAMP.length - 4) {
                            brightness = Math.min(1.0f, brightness + 0.15f);
                        }

                        Color cellColor = Color.getHSBColor(hue, saturation, brightness);
                        int r = cellColor.getRed();
                        int g = cellColor.getGreen();
                        int b = cellColor.getBlue();

                        // Render the pixel utilizing true ANSI true-color (38;2;R;G;B)
                        outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm%c\u001B[0m", r, g, b, glyph);
                    }
                }
            }
        }

        // Increment rotation angles (4D and 3D) for the next cycle
        angleXW += 0.018;
        angleXY += 0.011;
        angleYZ += 0.015;
    }
}