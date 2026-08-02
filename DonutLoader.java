public class DonutLoader extends Loader {

    private static final StatusStage[] DONUT_STAGES = {
            new StatusStage(20, "Mixing batter:"),
            new StatusStage(35, "Baking cake:"),
            new StatusStage(55, "Cooling down:"),
            new StatusStage(75, "Applying glaze:"),
            new StatusStage(95, "Adding sprinkles:"),
            new StatusStage(100, "Boxed & Ready!")
    };

    private static final double[][] SPRINKLES = {
            { 0.3, 0.6, 0 }, { 0.8, 0.8, 1 }, { 1.4, 0.5, 2 }, { 1.9, 0.7, 0 }, { 2.5, 0.6, 1 },
            { 3.0, 0.8, 2 }, { 3.6, 0.5, 0 }, { 4.1, 0.7, 1 }, { 4.7, 0.6, 2 }, { 5.3, 0.8, 0 },
            { 5.9, 0.5, 1 }, { 6.2, 0.7, 2 }, { 0.5, 1.1, 1 }, { 1.1, 1.3, 2 }, { 1.7, 1.0, 0 },
            { 2.2, 1.2, 1 }, { 2.8, 1.4, 2 }, { 3.3, 1.1, 0 }, { 3.9, 1.3, 1 }, { 4.4, 1.0, 2 },
            { 5.0, 1.2, 0 }, { 5.6, 1.4, 1 }, { 0.1, 1.6, 2 }, { 0.9, 1.5, 0 }, { 1.5, 1.7, 1 },
            { 2.3, 1.6, 2 }, { 3.5, 1.5, 0 }, { 4.2, 1.7, 1 }, { 4.9, 1.6, 2 }, { 5.7, 1.5, 0 }
    };

    private final int[][] sprinkleMap = new int[180][524];
    private String glazedIcing;
    private String donutCake;
    private String[] sprinkleColors;

    private double A = 0;
    private double B = 0;

    public DonutLoader() {
        super(DONUT_STAGES);
    }

    @Override
    protected void initialize() {
        // Setup visual flavor styling
        switch ((int) (Math.random() * 6) + 1) {

            case 1: // --- 1. MAPLE GLAZED ON GOLDEN DOUGH ---
                glazedIcing = "\u001B[38;5;94m"; // Deep Maple Amber
                donutCake = "\u001B[33m"; // Golden-Fried Yellow
                sprinkleColors = new String[] {
                        "\u001B[38;5;214m", // Orange
                        "\u001B[38;5;226m", // Bright Yellow
                        "\u001B[38;5;255m" // Pure White
                };
                break;

            case 2: // --- 2. DOUBLE CHOCOLATE FUDGE ---
                glazedIcing = "\u001B[38;5;130m"; // Milk Chocolate Gloss
                donutCake = "\u001B[38;5;94m"; // Rich Dark Chocolate Dough
                sprinkleColors = new String[] {
                        "\u001B[38;5;117m", // Baby Blue
                        "\u001B[38;5;207m", // Pastel Lavender/Pink
                        "\u001B[38;5;255m" // Pure White
                };
                break;

            case 3: // --- 3. VANILLA GLAZE ON TRADITIONAL BROWN CAKE ---
                glazedIcing = "\u001B[38;5;255m"; // Stark Vanilla White
                donutCake = "\u001B[38;5;137m"; // Soft Baked Brown Dough
                sprinkleColors = new String[] {
                        "\u001B[38;5;196m", // Crimson Red
                        "\u001B[38;5;94m", // Chocolate Brown
                        "\u001B[38;5;208m" // Vivid Orange
                };
                break;

            case 4: // --- 4. MATCHA GREEN TEA ON CHOCOLATE ---
                glazedIcing = "\u001B[38;5;114m"; // Vibrant Matcha Green
                donutCake = "\u001B[38;5;130m"; // Rich Chocolate Dough
                sprinkleColors = new String[] {
                        "\u001B[38;5;255m", // White
                        "\u001B[38;5;206m", // Pink
                        "\u001B[38;5;220m" // Gold Yellow
                };
                break;

            case 5: // --- 5. BLUEBERRY BLAST ON BUTTERMILK CAKE ---
                glazedIcing = "\u001B[38;5;61m"; // Deep Royal Blueberry Purple-Blue
                donutCake = "\u001B[38;5;229m"; // Fluffy Yellow-White Buttermilk Cake
                sprinkleColors = new String[] {
                        "\u001B[38;5;81m", // Sky Blue
                        "\u001B[38;5;46m", // Neon Lime Green
                        "\u001B[38;5;255m" // Pure White
                };
                break;

            case 6:
            default: // --- 6. THE HOMER SIMPSON STRAWBERRY SPECIAL ---
                glazedIcing = "\u001B[38;5;205m"; // Blazing Strawberry Bubblegum Pink
                donutCake = "\u001B[33m"; // 16-color system (Standard Olive-Yellow)
                sprinkleColors = new String[] {
                        "\u001B[38;5;51m", // Electric Neon Cyan
                        "\u001B[38;5;46m", // Pure Radioactive Green
                        "\u001B[38;5;255m" // Bright Sugar White
                };
                break;

        }

        // Generate sprinkle map mapping logic
        for (int[] row : sprinkleMap) {
            for(int i = 0; i < row.length; i++) {
                row[i] = -1;
            }
        }
        int tMapIndex = 0;
        for (double theta = 0; theta < 6.28; theta += 0.035) {
            int pMapIndex = 0;
            for (double phi = 0; phi < 6.28; phi += 0.012) {
                for (int i = 0; i < SPRINKLES.length; i++) {
                    double dTheta = Math.abs(theta - SPRINKLES[i][0]);
                    double dPhi = Math.abs(phi - SPRINKLES[i][1]);
                    if (dTheta > 3.14)
                        dTheta = 6.28 - dTheta;

                    if (dTheta < 0.15 && dPhi < 0.15) {
                        sprinkleMap[tMapIndex][pMapIndex] = (int) SPRINKLES[i][2];
                        break;
                    }
                }
                pMapIndex++;
            }
            tMapIndex++;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        for (int tIndex = 0; tIndex < 180; tIndex++) {
            double theta = tIndex * 0.035;
            for (int pIndex = 0; pIndex < 524; pIndex++) {
                double phi = pIndex * 0.012;
                double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
                double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);
                double sinA = Math.sin(A), cosA = Math.cos(A);
                double sinB = Math.sin(B), cosB = Math.cos(B);

                double h = cosPhi + 2;
                double D = 1 / (sinTheta * h * sinA + sinPhi * cosA + 7.0);
                double t = sinTheta * h * cosA - sinPhi * sinA;

                int x = (int) (40 + 42 * D * (cosTheta * h * cosB - t * sinB));
                int y = (int) (12 + 21 * D * (cosTheta * h * sinB + t * cosB));
                int o = x + 80 * y;

                double N_double = 8 * ((sinPhi * sinA - sinTheta * cosPhi * cosA) * cosB - sinTheta * cosPhi * sinA
                        - sinPhi * cosA - cosTheta * cosPhi * sinB);

                if (22 > y && y > 0 && x > 0 && 80 > x && D > (zBuffer[o] + 0.0001)) {
                    zBuffer[o] = D;
                    int charIndex = (int) Math.round(N_double);
                    if (charIndex < 0)
                        charIndex = 0;

                    String lString = ".,-~:;=!*#$@";
                    char asciiChar = lString.charAt(charIndex >= lString.length() ? lString.length() - 1 : charIndex);

                    double dripThreshold = -0.15 + 0.15 * Math.sin(3 * theta) + 0.08 * Math.cos(7 * theta)
                            + 0.04 * Math.sin(11 * theta);
                    boolean isFrosting = sinPhi > dripThreshold;
                    String chosenColor = isFrosting ? glazedIcing : donutCake;

                    if (isFrosting) {
                        int sprinkleColorIndex = sprinkleMap[tIndex][pIndex];
                        if (sprinkleColorIndex != -1) {
                            chosenColor = sprinkleColors[sprinkleColorIndex];
                        }
                    }
                    outputBuffer[o] = chosenColor + asciiChar + RESET;
                }
            }
        }

        A += 0.04;
        B += 0.02;
    }
}
