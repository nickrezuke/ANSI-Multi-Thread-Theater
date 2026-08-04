public class LoaderFactory {
    // All the loaders currently being considered
    private static final String[] LOADER_NAMES = {
            "ConwaysGameOfLife", "GameOfLife", "Donut", "Mario64Cube", "TexelCompanionCube", "TexelMinecraftGrassBlock",
            "TexelBorgCube", "CatHead", "Vaporwave", "Synthwave", "GreyScottReactionDiffusion", "ReactionDiffusion",
            "Radar", "MandelbrotZoom", "DNA", "BlackHole", "Lorenz", "KleinRing",
            "LorenzAttractor", "RippleTank", "DoubleSlit", "TextFall", "Ring", "RockPaperScissors", "FrutigerAero",
            "TexelMCUTesseract", "TexelHellraiserLamentConfiguration", "TexelGreyRubix", "TexelTriplePhase",
            "TexelMarioBricks", "RainbowWhispSphere", "PacManSphere", "OctahedralMatrix", "RainbowWhispPyramid",
            "Tesseract", "4D Cube", "SeifertSurface", "Pendulum",
            "EyeOfProvidence", "Illuminati", "WireframeSphere", "Louvre", "FluidDynamics", "NavierStokes", "DJ",
            "TexelRubixCube", "DJTurntable", "PerlinNoise", "SydneyOperaHouse", "EiffelTower", "TajMahal",
            "ChicagoBean", "CloudGate",
            "BigBen", "NyanCat", "BouncingSpinner", "DancingBanana", "ThreeBody", "WireframeCube", "HyperSphere",
            "TexelKevinCube", "4D Sphere", "HopfFibration", "Snake", "Pong", "BrickBreakout", "Breakout",
            "SpaceInvaders", "Chess",
            "TexelGlitch", "Checkers", "Tetris", "Asteroids", "Centepede", "Galaga", "LangtonsAnt", "Boids",
            "HerdingAndFlocking",
            "TexelAllspark", "TexelEmojiCube", "TexelPsychCube", "TexelFlatEarth", "QuantumWave",
            "BriansBrain", "Gyroid", "MarioCoinBlock", "Unused", "Unused2"
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
            case "QuantumWave":
                return new QuantumWaveLoader();
            case "Mario64Cube":
                return new Mario64CubeLoader();
            case "TexelCompanionCube":
                return new TexelCompanionCubeLoader();
            case "TexelMinecraftGrassBlock":
                return new TexelMinecraftGrassBlockLoader();
            case "TexelBorgCube":
                return new TexelBorgCubeLoader();
            case "TexelAllspark":
                return new TexelAllsparkLoader();
            case "TexelEmojiCube":
                return new TexelEmojiCubeLoader();
            case "TexelPsychCube":
                return new TexelPsychCubeLoader();
            case "TexelHellraiserLamentConfiguration":
                return new TexelHellraiserLamentConfigurationLoader();
            case "TexelFlatEarth":
                return new TexelFlatEarthLoader();
            case "TexelGlitch":
                return new TexelGlitchLoader();
            case "TexelRubixCube":
                return new TexelRubixCubeLoader();
            case "TexelGreyRubix":
                return new TexelGreyRubixLoader();
            case "TexelMCUTesseract":
                return new TexelMCUTesseractLoader();
            case "TriplePhaseCube":
                return new TexelTriplePhaseCubeLoader();
            case "MarioBricks":
                return new TexelMarioBricksLoader();
            case "MarioCoinBlock":
                return new TexelMarioCoinBlockLoader();
            case "CatHead":
                return new CatHeadLoader();
            case "Synthwave":
                return new SynthWaveLoader();
            case "Vaporwave":
                return new VaporWaveLoader();
            case "GreyScottReactionDiffusion":
            case "ReactionDiffusion":
                return new ReactionDiffusionLoader();
            case "Radar":
                return new RadarLoader();
            case "MandelbrotZoom":
                return new MandelbrotZoomLoader();
            case "DNA":
                return new DNALoader();
            case "Pendulum":
                return new PendulumLoader();
            case "BlackHole":
                return new BlackHoleLoader();
            case "Lorenz":
            case "LorenzAttractor":
                return new LorenzLoader();
            case "RippleTank":
            case "DoubleSlit":
                return new RippleTankAutomatonLoader();
            case "SeifertSurface":
                return new SeifertLoader();
            case "TextFall":
                return new TextFallLoader();
            case "Ring":
                return new RingLoader();
            case "KleinRing":
                return new KleinRingLoader();
            case "RockPaperScissors":
                return new RockPaperScissorsAutomatonLoader();
            case "FrutigerAero":
                return new FrutigerAeroLoader();
            case "RainbowWhispSphere":
                return new RainbowWhispSphereLoader();
            case "PacManSphere":
                return new PacManSphereLoader();
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
            case "DJ":
            case "DJTurntable":
                return new DJTurntableLoader();
            case "PerlinNoise":
                return new PerlinNoiseLoader();
            case "SydneyOperaHouse":
                return new SydneyOperaHouseLoader();
            case "EiffelTower":
                return new EiffelTowerLoader();
            case "TajMahal":
                return new TajMahalLoader();
            case "ChicagoBean":
            case "CloudGate":
                return new ChicagoBeanLoader();
            case "BigBen":
                return new BigBenLoader();
            case "TexelKevinCube":
                return new TexelKevinCubeLoader();
            case "NyanCat":
                return new NyanCatLoader();
            case "BouncingSpinner":
                return new BouncingSpinnerLoader();
            case "DancingBanana":
                return new DancingBananaLoader();
            case "ThreeBody":
                return new ThreeBodyLoader();
            case "WireframeCube":
                return new WireframeCubeLoader();
            case "HyperSphere":
            case "4D Sphere":
                return new HypersphereLoader();
            case "HopfFibration":
                return new HopfFibrationLoader();
            case "Snake":
                return new SnakeLoader();
            case "Pong":
                return new PongLoader();
            case "BrickBreakout":
            case "Breakout":
                return new BrickBreakoutLoader();
            case "SpaceInvaders":
                return new SpaceInvadersLoader();
            case "Chess":
                return new ChessLoader();
            case "Checkers":
                return new CheckersLoader();
            case "Tetris":
                return new TetrisLoader();
            case "Asteroids":
                return new AsteroidsLoader();
            case "Centepede":
                return new CentipedeLoader();
            case "Galaga":
                return new GalagaLoader();
            case "LangtonsAnt":
                return new LangtonsAntLoader();
            case "Boids":
            case "HerdingAndFlocking":
                return new BoidsLoader();
            case "BriansBrain":
                return new BriansBrainLoader();
            case "Gyroid":
                return new GyroidLoader();
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
