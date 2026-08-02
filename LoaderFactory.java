public class LoaderFactory {
    // All the loaders currently being considered
    private static final String[] LOADER_NAMES = {
            "ConwaysGameOfLife", "GameOfLife", "Donut", "CubeA", "CubeB", "CatHead", "Retrowave", "Synthwave",
            "GreyScottReactionDiffusion", "ReactionDiffusion", "Radar", "MandelbrotZoom", "DNA", "BlackHole", "Lorenz",
            "LorenzAttractor", "RippleTank", "DoubleSlit", "TextFall", "Ring", "RockPaperScissors",
            "RainbowWhispSphere", "PacMan", "OctahedralMatrix", "RainbowWhispPyramid", "Tesseract", "4D Cube",
            "EyeOfProvidence", "Illuminati", "WireframeSphere", "Louvre", "FluidDynamics", "NavierStokes",
            "PerlinNoise", "Unused", "Unused2"
    };

    public static Loader createLoaderInstance(String requestedName) {
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

        // 3. Fallback: If truly no match was found,
        // resolve to a random variant name
        if (finalName == null) {
            int randomIndex = (int) (Math.random() * LOADER_NAMES.length);
            finalName = LOADER_NAMES[randomIndex];
        }

        // 4. Centralized Construction Switch (Strings in switch are
        // exact/case-sensitive)
        switch (finalName) {
            case "ConwaysGameOfLife":
            case "GameOfLife":
                return new ConwaysGameOfLifeLoader();
            case "Donut":
                return new DonutLoader();
            case "CubeA":
                return new CubeLoaderA();
            case "CubeB":
                return new CubeLoaderB();
            case "CatHead":
                return new CatHeadLoader();
            case "Retrowave":
            case "Synthwave":
                return new RetroWaveLoader();
            case "GreyScottReactionDiffusion":
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
            case "LorenzAttractor":
                return new LorenzLoader();
            case "RippleTank":
            case "DoubleSlit":
                return new RippleTankAutomatonLoader();
            case "TextFall":
                return new TextFallLoader();
            case "Ring":
                return new RingLoader();
            case "RockPaperScissors":
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
            case "4D Cube":
                return new TesseractLoader();
            case "EyeOfProvidence":
            case "Illuminati":
                return new EyeOfProvidenceLoader();
            case "WireframeSphere":
                return new WireframeSphereLoader();
            case "Louvre":
                return new LouvreLoader();
            case "FluidDynamics":
            case "NavierStokes":
                return new FluidDynamicsLoader();
            case "PerlinNoise":
                return new PerlinNoiseLoader();
            case "Unused":
                return new UnusedInfiniteFractalLoader();
            case "Unused2":
                return new UnusedInfiniteFractalLoader2();
            default:
                return new UnusedInfiniteFractalLoader(); // We should never get here
        }
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

            // Quality Threshold Gate: A valid typo match should
            // realistically have an edit distance that doesn't exceed
            // half the length of the targeted target name.
            // This prevents unrelated keys from matching aggressively
            // Like "xpmqsudhqjdigkudr" with "ConwaysGameOfLife".
            int structuralThreshold = Math.max(4, option.length() / 2);

            if (finalDistance < minDistance && finalDistance <= structuralThreshold) {
                minDistance = finalDistance;
                bestMatch = option;
            }
        }

        return bestMatch;
    }
}
