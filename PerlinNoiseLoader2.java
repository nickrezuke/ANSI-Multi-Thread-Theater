// TODO: Fix This Perlin Noise Loader It currently looks terrible.

public class PerlinNoiseLoader2 extends Loader {
    private static final StatusStage[] STAGES = {
            new StatusStage(25, "Generating cohesive gradient vectors:"),
            new StatusStage(55, "Interpolating multi-octave noise fields:"),
            new StatusStage(85, "Smoothing coordinate texture coordinates:"),
            new StatusStage(100, "Perlin Noise Matrix Stable!")
    };

    private static final char[] SHADE_RAMP = " .,:irsXA253hMHGS#9B&@".toCharArray();
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // Standard Perlin Noise Permutation Table (Doubled for overflow wrapping)
    private static final int[] P = new int[512];
    static {
        int[] sourcePermutation = {
                151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99,
                37, 240, 21, 10, 23,
                190, 6, 148, 247, 120, 234, 75, 0, 26, 56, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237,
                149, 56, 87, 174,
                20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229,
                122, 60, 211, 133,
                230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76,
                132, 187, 208, 89,
                18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250,
                124, 123, 5, 202,
                38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170,
                213, 119, 248, 152,
                2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113,
                224, 232, 178, 185,
                112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235,
                249, 14, 239, 107,
                49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236,
                205, 93, 222, 114,
                67, 29, 24, 72, 243, 141, 128, 195, 78, 212, 85, 11, 22, 151, 160, 137, 91, 90, 15, 131, 13, 201, 95,
                96, 53, 194, 233, 7, 225
        };
        for (int i = 0; i < 256; i++) {
            P[i] = sourcePermutation[i];
            P[256 + i] = sourcePermutation[i];
        }
    }

    private double cameraX = 0.0;

    public PerlinNoiseLoader2() {
        // This uses 80x22 specifically
        super(STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        cameraX = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step forward aling the camera path
        cameraX += 0.012;

        // Configuration scale factors for structural cloud sizing (Higher values mean
        // tighter, smaller pockets)
        double noiseScale = 0.018 + 0.002 * Math.sin(cameraX * 0.15);

        for (int y = 0; y < HEIGHT; y++) {
            // Apply a 2.1:1 horizontal character compression scalar to combat terminal
            // rectangular fonts
            double mappedY = y * noiseScale * 0.45;

            for (int x = 0; x < WIDTH; x++) {
                int index = x + WIDTH * y;
                double mappedX = x * noiseScale;

                double value = calculateImprovedNoise(
                        mappedX + cameraX,
                        mappedY,
                        0.0) + 0.35 * calculateImprovedNoise((mappedX + cameraX) * 2, mappedY * 2, 100.0);

                value /= 1.35;

                double normalizedValue = value * 0.5 + 0.5;
                // normalizedValue = Math.pow(normalizedValue, 1.25); // Do one of these
                normalizedValue = Math.sqrt(normalizedValue); // Do one of these

                


                double nx = (x - WIDTH / 2.0) / (WIDTH / 2.0);
                double ny = (y - HEIGHT / 2.0) / (HEIGHT / 2.0);
                
                double edge = 1.0 - Math.sqrt(nx * nx + ny * ny);
                
                edge = Math.max(0.0, edge);
                edge = Math.pow(edge, 0.35);    // tweak to taste
                
                normalizedValue *= edge;
                normalizedValue *= edge;

                double dx = calculateImprovedNoise(
                    mappedX + cameraX + 0.03,
                    mappedY,
                    0.0);
                
                double dy = calculateImprovedNoise(
                    mappedX + cameraX,
                    mappedY + 0.03,
                    0.0);
                
                double light =
                    (dx - value) * 0.8 +
                    (dy - value) * 0.5;
                
                normalizedValue += light * 0.25;
                normalizedValue = Math.max(0, Math.min(1, normalizedValue));

                double t = normalizedValue;

// Deep blue
int r = 10;
int g = 20;
int b = 70;

if (t > 0.35) {
    double u = (t - 0.35) / 0.35;

    r = (int)(10 + u * 40);
    g = (int)(20 + u * 180);
    b = (int)(70 + u * 170);
}

if (t > 0.75) {
    double u = (t - 0.75) / 0.25;

    r = (int)(50 + u * 205);
    g = (int)(160 + u * 55);
    b = (int)(140 + u * 15);
}

                

                // Select density character token based on intensity levels
                int shadeIndex = Math.min(SHADE_RAMP.length - 1,
                        Math.max(0, (int) (normalizedValue * (SHADE_RAMP.length - 1))));
                if (shadeIndex > SHADE_RAMP.length - 1) {
                    shadeIndex = SHADE_RAMP.length - 1;
                }
                char renderChar = SHADE_RAMP[shadeIndex];
                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                outputBuffer[index] = colorCode + renderChar + RESET;
            }
        }
    }

    /**
     * Ken Perlin's Improved Noise Formulation implementation for 3D coordinates.
     */
    private double calculateImprovedNoise(double x, double y, double z) {
        // Find unit cube coordinates containing the point vector
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        // Find relative coordinates inside the local cube block
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        // Compute fade curves to drive smooth Hermite s-curves ($3t^2 - 2t^3$)
        double u = x * x * x * (x * (x * 6 - 15) + 10);
        double v = y * y * y * (y * (y * 6 - 15) + 10);
        double w = z * z * z * (z * (z * 6 - 15) + 10);

        // Hash coordinates of the 8 cube corners
        int A = P[X] + Y;
        int AA = P[A] + Z;
        int AB = P[A + 1] + Z;
        int B = P[X + 1] + Y;
        int BA = P[B] + Z;
        int BB = P[B + 1] + Z;

        // Trilinear blend interpolation between corners using gradients
        return lerp(w, lerp(v, lerp(u, grad(P[AA], x, y, z),
                grad(P[BA], x - 1, y, z)),
                lerp(u, grad(P[AB], x, y - 1, z),
                        grad(P[BB], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(P[AA + 1], x, y, z - 1),
                        grad(P[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(P[AB + 1], x, y - 1, z - 1),
                                grad(P[BB + 1], x - 1, y - 1, z - 1))));
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y, double z) {
        // Convert low 4 bits of hash code into 12 gradient directional vectors
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
