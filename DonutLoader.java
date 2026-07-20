import java.util.Arrays;

public class DonutLoader implements Runnable {
    private volatile boolean isRunning = true;
    private volatile int progress = 0;

    // A small class for custom messages and their upper-bound percentage thresholds
    private static class StatusStage {
        final int maxPercent;
        final String message;

        StatusStage(int maxPercent, String message) {
            this.maxPercent = maxPercent;
            this.message = message;
        }
    }

    // These define the loading messages for the changing text
    private static final StatusStage[] STAGES = {
            new StatusStage(20, "Mixing batter:"), // Covers 0%-20%
            new StatusStage(35, "Baking cake:"), // Covers 20%-35%
            new StatusStage(55, "Cooling down:"), // Covers 35%-55%
            new StatusStage(75, "Applying glaze:"), // Covers 55%-75%
            new StatusStage(95, "Adding sprinkles:"), // Covers 75%-95%
            new StatusStage(100, "Boxed & Ready!") // Covers 95%-100%
    };

    // The set of sprinkles on the top of the donut.
    // Defined as {theta, phi, colorIndex} where colorIndex maps to SPRINKLE_COLORS
    // and theta/phi are the donut surface coordinates.
    private static final double[][] SPRINKLES = {
            { 0.3, 0.6, 0 }, { 0.8, 0.8, 1 }, { 1.4, 0.5, 2 }, { 1.9, 0.7, 0 }, { 2.5, 0.6, 1 },
            { 3.0, 0.8, 2 }, { 3.6, 0.5, 0 }, { 4.1, 0.7, 1 }, { 4.7, 0.6, 2 }, { 5.3, 0.8, 0 },
            { 5.9, 0.5, 1 }, { 6.2, 0.7, 2 }, { 0.5, 1.1, 1 }, { 1.1, 1.3, 2 }, { 1.7, 1.0, 0 },
            { 2.2, 1.2, 1 }, { 2.8, 1.4, 2 }, { 3.3, 1.1, 0 }, { 3.9, 1.3, 1 }, { 4.4, 1.0, 2 },
            { 5.0, 1.2, 0 }, { 5.6, 1.4, 1 }, { 0.1, 1.6, 2 }, { 0.9, 1.5, 0 }, { 1.5, 1.7, 1 },
            { 2.3, 1.6, 2 }, { 3.5, 1.5, 0 }, { 4.2, 1.7, 1 }, { 4.9, 1.6, 2 }, { 5.7, 1.5, 0 }
    };

    public void stopLoading() {
        this.isRunning = false;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void run() {
        double A = 0, B = 0;
        double[] zBuffer = new double[1760];
        String[] outputBuffer = new String[1760];

        // The donut color and the frosting color depends on the donut
        String GLAZED_ICING;
        String DONUT_CAKE;
        String[] SPRINKLE_COLORS;

        // Pick a random donut from these predefined half-dozen flavors!!
        switch ((int) (Math.random() * 6) + 1) {
            case 1: // Chocolate Frosted with Yellow Cake Batter
                GLAZED_ICING = "\u001B[38;5;94m";   // Milk Chocolate Frosting
                DONUT_CAKE = "\u001B[33m";          // Yellow Cake
                SPRINKLE_COLORS = new String[] {
                        "\u001B[38;5;214m",         // Bright Orange
                        "\u001B[38;5;226m",         // Vivid Yellow
                        "\u001B[38;5;255m"          // Pure White
                };
                break;
            case 2: // Triple-Chocolate Glazed
                GLAZED_ICING = "\u001B[38;5;130m";  // Fudge Glaze
                DONUT_CAKE = "\u001B[38;5;52m";     // Dark Chocolate Cake
                SPRINKLE_COLORS = new String[] {
                        "\u001B[38;5;117m",         // Light Blue
                        "\u001B[38;5;207m",         // Lavender Pink
                        "\u001B[38;5;255m"          // White Cream
                };
                break;
            case 3: // Old Fashioned Powdered Sugar
                GLAZED_ICING = "\u001B[38;5;255m";  // White Powdered Sugar
                DONUT_CAKE = "\u001B[38;5;137m";    // Toasted Golden Brown Cake
                SPRINKLE_COLORS = new String[] {
                        "\u001B[38;5;196m",         // Deep Raspberry Red
                        "\u001B[38;5;94m",          // Chocolate Cookie Crumbs
                        "\u001B[38;5;208m"          // Cinnamon Orange
                };
                break;
            case 4: // Matcha Green Tea with Chocolate Cake
                GLAZED_ICING = "\u001B[38;5;114m";  // Matcha Green
                DONUT_CAKE = "\u001B[38;5;52m";     // Dark Chocolate Cake
                SPRINKLE_COLORS = new String[] {
                        "\u001B[38;5;255m",         // White
                        "\u001B[38;5;206m",         // Sakura Pink
                        "\u001B[38;5;220m"          // Gold
                };
                break;
            case 5: // Blueberry Glazed / Vanilla Cake
                GLAZED_ICING = "\u001B[38;5;61m";   // Blueberry Blue-Purple
                DONUT_CAKE = "\u001B[38;5;229m";    // Vanilla Cream Cake
                SPRINKLE_COLORS = new String[] {
                        "\u001B[38;5;81m",          // Electric Sky Blue
                        "\u001B[38;5;46m",          // Bright Lime Green
                        "\u001B[38;5;255m"          // White Sugar
                };
                break;
            case 6:
            default: // Strawberry Classic (The Homer Simpson)
                GLAZED_ICING = "\u001B[38;5;205m";  // Hot Pink
                DONUT_CAKE = "\u001B[33m";          // Yellow Cake
                SPRINKLE_COLORS = new String[] {
                        "\u001B[36m",               // Cyan
                        "\u001B[32m",               // Green
                        "\u001B[37m"                // White
                };
                break;
        }

        // To reset the color back to default
        String RESET = "\u001B[0m";

        // Colors for the progress bar
        String GREEN = "\u001B[32m";
        String WHITE = "\u001B[37m";

        // --- GENERATE SPRINKLE MAP ---
        // We only need to do this once, not every time
        int[][] sprinkleMap = new int[180][524];
        for (int[] row : sprinkleMap)
            Arrays.fill(row, -1);

        // For each theta/phi coordinate, check if it is close to a sprinkle's location
        int tMapIdx = 0;
        for (double theta = 0; theta < 6.28; theta += 0.035) {
            int pMapIdx = 0;
            for (double phi = 0; phi < 6.28; phi += 0.012) {
                for (int i = 0; i < SPRINKLES.length; i++) {
                    double dTheta = Math.abs(theta - SPRINKLES[i][0]);
                    double dPhi = Math.abs(phi - SPRINKLES[i][1]);
                    if (dTheta > 3.14) {
                        dTheta = 6.28 - dTheta;
                    }

                    // If the current theta/phi is close enough to a sprinkle's location, assign that sprinkle's color index to the map
                    if (dTheta < 0.15 && dPhi < 0.15) {
                        sprinkleMap[tMapIdx][pMapIdx] = (int) SPRINKLES[i][2];
                        break;
                    }
                }
                pMapIdx++;
            }
            tMapIdx++;
        }

        // Clear the console before starting the donut animation
        System.out.print("\u001b[2J");

        // Loop until we are done
        while (isRunning) {
            //Start timing
            long startTime = System.nanoTime();

            // Reset the output and z-buffer for this frame
            Arrays.fill(outputBuffer, " ");
            Arrays.fill(zBuffer, 0);

            // --- DONUT RENDERING ALGORITHM ---

            // Loop through theta and phi angles to calculate the 3D coordinates of the donut surface
            for (int tIdx = 0; tIdx < 180; tIdx++) {
                double theta = tIdx * 0.035;
                for (int pIdx = 0; pIdx < 524; pIdx++) {
                    double phi = pIdx * 0.012;

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

                    double N_double = 8 * ((sinPhi * sinA - sinTheta * cosPhi * cosA) * cosB
                            - sinTheta * cosPhi * sinA
                            - sinPhi * cosA
                            - cosTheta * cosPhi * sinB);

                    // Only update the output buffer if the calculated pixel is within bounds and closer than previous pixels
                    if (22 > y && y > 0 && x > 0 && 80 > x && D > zBuffer[o]) {
                        zBuffer[o] = D;
                        int charIndex = (int) Math.round(N_double);
                        if (charIndex < 0)
                            charIndex = 0;

                        String lString = ".,-~:;=!*#$@";
                        char asciiChar = lString
                                .charAt(charIndex >= lString.length() ? lString.length() - 1 : charIndex);

                        double dripThreshold = -0.15 + 0.15 * Math.sin(3 * theta) + 0.08 * Math.cos(7 * theta)
                                + 0.04 * Math.sin(11 * theta);
                        boolean isFrosting = sinPhi > dripThreshold;
                        String chosenColor = isFrosting ? GLAZED_ICING : DONUT_CAKE;

                        // If the pixel is part of the frosting, check if it is close to a sprinkle's location and assign that sprinkle's color if so
                        if (isFrosting) {
                            int sprinkleColorIndex = sprinkleMap[tIdx][pIdx];
                            if (sprinkleColorIndex != -1) {
                                chosenColor = SPRINKLE_COLORS[sprinkleColorIndex];
                            }
                        }
                        outputBuffer[o] = chosenColor + asciiChar + RESET;
                    }
                }
            }

            // --- PRINT THE DONUT FRAME ---
            System.out.print("\u001b[H");
            for (int k = 0; k < 1760; k++) {
                System.out.print(k % 80 > 0 ? outputBuffer[k] : "\n");
            }

            // --- RESOLVE CURRENT STATUS MESSAGE ---
            int currentProgress = this.progress;
            String activeMessage = "Loading...";
            for (StatusStage stage : STAGES) {
                if (currentProgress <= stage.maxPercent) {
                    activeMessage = stage.message;
                    break;
                }
            }

            // --- BUILD DYNAMIC PROGRESS BAR ---
            int totalBars = 30;
            int filledBars = (int) ((currentProgress / 100.0) * totalBars);
            StringBuilder bar = new StringBuilder();
            for (int b = 0; b < totalBars; b++) {
                bar.append(b < filledBars ? '\u2665' : '\u2661'); // Empty and Full Hearts
            }

            // --- FIXED-WIDTH FORMATTING PRINT ---
            // %18s reserves exactly 18 horizontal character slots left-aligned.
            // If the message is shorter, the remainder is filled with empty spaces.
            String formattedStatus = String.format("       %18s", activeMessage);
            System.out.print("\n\n" + WHITE + formattedStatus + "[" + GREEN + bar.toString() + WHITE + "] "
                    + currentProgress + "%" + RESET);

            // --- UPDATE ROTATION ANGLES FOR NEXT FRAME ---
            A += 0.04;
            B += 0.02;

            // --- PLACE LIMIT ON RENDERING RATE ---
            try {
                // Determine how long that all took (in nanos)
                int elapsedTime = (int) (System.nanoTime() - startTime);

                // We should be expecting this many nanos per frame:
                int goalRate = 10000000; // 10 milliseconds
                
                // Sleep the extra time if we finished with time to spare
                int sleepTime = goalRate - elapsedTime;
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.print("\n\u001b[2J\u001b[H");
        System.out.flush();
    }
}
