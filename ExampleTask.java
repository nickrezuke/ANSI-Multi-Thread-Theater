// This class acts as my heavy processing example application
public class ExampleTask {
    // --- ALL LOADERS CURRENTLY BEING CONSIDERED ---
    private static final String[] LOADER_NAMES = {
            "ConwaysGameOfLife", "Donut", "CubeA", "CubeB", "CatHead", "RetroWave",
            "ReactionDiffusion", "Radar", "MandelbrotZoom", "DNA", "BlackHole", "Lorenz",
            "RippleTankAutomaton", "TextFall", "Ring", "RockPaperScissorsAutomaton",
            "RainbowWhispSphere", "PacMan", "OctahedralMatrix", "RainbowWhispPyramid",
            "Tesseract", "EyeOfProvidence", "WireframeSphere", "Louvre"
    };

    // Processes input name case-insensitively. If none, random
    private static Loader createLoaderInstance(String requestedName) {
        String finalName = null;

        // 1. Scan for a valid case-insensitive match against the registry
        if (requestedName != null) {
            for (int i = 0; i < LOADER_NAMES.length; i++) {
                if (LOADER_NAMES[i].equalsIgnoreCase(requestedName)) {
                    finalName = LOADER_NAMES[i];
                    break;
                }
            }
        }

        // 2. Attempt Best Match...
        if (finalName == null && requestedName != null) {
            // We entered something but no match...
            finalName = findClosestMatch(requestedName, LOADER_NAMES);
        }

        // 3. Fallback: If truly no match was found, resolve to an organic random
        // variant name
        if (finalName == null) {
            int randomIndex = (int) (Math.random() * LOADER_NAMES.length);
            finalName = LOADER_NAMES[randomIndex];
        }

        // 4. Centralized Construction Switch (Strings in switch are
        // exact/case-sensitive)
        switch (finalName) {
            case "ConwaysGameOfLife":
                return new ConwaysGameOfLifeLoader();
            case "Donut":
                return new DonutLoader();
            case "CubeA":
                return new CubeLoaderA();
            case "CubeB":
                return new CubeLoaderB();
            case "CatHead":
                return new CatHeadLoader();
            case "RetroWave":
                return new RetroWaveLoader();
            case "ReactionDiffusion":
                return new ReactionDiffusionLoader();
            case "Radar":
                return new RadarLoader();
            case "MandelbrotZoom":
                return new MandelbrotZoomLoader();
            case "DNA":
                return new DNALoader();
            case "BlackHole":
                return new BlackHoleLoader();
            case "Lorenz":
                return new LorenzLoader();
            case "RippleTankAutomaton":
                return new RippleTankAutomatonLoader();
            case "TextFall":
                return new TextFallLoader();
            case "Ring":
                return new RingLoader();
            case "RockPaperScissorsAutomaton":
                return new RockPaperScissorsAutomatonLoader();
            case "RainbowWhispSphere":
                return new RainbowWhispSphereLoader();
            case "PacMan":
                return new PacManLoader();
            case "OctahedralMatrix":
                return new OctahedralMatrixLoader();
            case "RainbowWhispPyramid":
                return new RainbowWhispPyramidLoader();
            case "Tesseract":
                return new TesseractLoader();
            case "EyeOfProvidence":
                return new EyeOfProvidenceLoader();
            case "WireframeSphere":
                return new WireframeSphereLoader();
            case "Louvre":
                return new LouvrePyramidLoader();
            default:
                return new ConwaysGameOfLifeLoader(); // We should never get here
        }
    }

    public static void main(String[] args) {

        // --- THE CRITICAL SHUTDOWN FAILSAFE ---
        // Registers a thread that fires exclusively when Control+C is hit.
        // It clears down the screen after a force shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print("\u001b[2J\u001b[H\u001b[?25h");
            System.out.flush();
        }));

        // 1. Evaluate the desired loader and create it
        String userPreference = (args.length > 0) ? args[0] : null;
        Loader loader = createLoaderInstance(userPreference);

        // 3. Spin up the desired loader on a new thread, and start it
        Thread loadingThread = new Thread(loader);
        loadingThread.start();

        // 4. Perform heavy task that needs a loader to watch while waiting
        try {

            // In theory, you would be running your code block right in
            // this try block right here:_____ and if you're able to
            // meaningfully calculate/ evaluate your progress with
            // some int p ranged [0,100], you can pass that through to update the loading
            // bar progress

            // I simulate this by just looping and running sleep for a bit before
            // updating the progress variable in increments from 0% to 100% over a few
            // seconds
            for (int p = 0; p <= 100; p += 1) {
                loader.setProgress(p); // Push progress values to the loader
                Thread.sleep(200); // Simulating 200ms of work time
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. When Task is complete, shut down the loader cleanly
        loader.stopLoading();
        try {
            loadingThread.join(); // Wait for final frame cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 6. Fresh clear so our completion message prints cleanly
        // Clear Screen + Cursor Home + Show Cursor
        System.out.print("\u001b[2J\u001b[H\u001b[?25h");
    }

    // Just a helper method to help match strings...
    private static String findClosestMatch(String input, String[] possibilities) {
        if (input == null || input.isEmpty() || possibilities == null || possibilities.length == 0) {
            return null;
        }

        String inputLower = input.toLowerCase();
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String option : possibilities) {
            String optionLower = option.toLowerCase();

            // Levenshtein Matrix Construction
            int m = inputLower.length();
            int n = optionLower.length();
            int[][] dp = new int[m + 1][n + 1];

            // Initialize baseline empty-string mapping costs
            for (int i = 0; i <= m; i++)
                dp[i][0] = i;
            for (int j = 0; j <= n; j++)
                dp[0][j] = j;

            // Fill edit distance matrix
            for (int i = 1; i <= m; i++) {
                for (int j = 1; j <= n; j++) {
                    if (inputLower.charAt(i - 1) == optionLower.charAt(j - 1)) {
                        dp[i][j] = dp[i - 1][j - 1]; // Character match costs 0 operations
                    } else {
                        // Find minimum between: Deletion, Insertion, or Substitution operations
                        int deleteCost = dp[i - 1][j];
                        int insertCost = dp[i][j - 1];
                        int substituteCost = dp[i - 1][j - 1];

                        int minOp = deleteCost;
                        if (insertCost < minOp)
                            minOp = insertCost;
                        if (substituteCost < minOp)
                            minOp = substituteCost;

                        dp[i][j] = 1 + minOp;
                    }
                }
            }

            int finalDistance = dp[m][n];

            // Quality Threshold Gate: A valid typo match should realistically have an edit
            // distance
            // that doesn't exceed half the length of the targeted target name.
            // This prevents unrelated keys (like "banana") matching aggressively with
            // "Donut".
            int structuralThreshold = Math.max(3, option.length() / 2);

            if (finalDistance < minDistance && finalDistance <= structuralThreshold) {
                minDistance = finalDistance;
                bestMatch = option;
            }
        }

        return bestMatch;
    }
}