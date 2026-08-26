import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class LoaderFactory {

    // Central Single-Source-of-Truth Registry
    private static final Map<String, Supplier<Loader>> REGISTRY = new HashMap<>();
    private static final List<String> LOADER_NAMES;

    // One entry per DISTINCT loader (regardless of how many names it registered),
    // While the REGISTRY is used for lookup, this UNIQUE_LOADERS is pretty much
    // used exclusively so the random fallback samples loaders uniformly (instead
    // of over-weighting loaders that were registered with many different names).
    private static final List<Supplier<Loader>> UNIQUE_LOADERS = new ArrayList<>();

    private static final Random RANDOM = new Random();

    static {
        // Define all loaders here.... (w/ lists of multiple possible names)
        register("Chladni", ChladniLoader::new);
        register("Apple", AppleLoader::new);
        register("RefractiveMagnifierLake", RefractiveMagnifierLakeLoader::new);
        register("SpiderWeb", SpiderWebLoader::new);
        register("InternetDino", InternetDinoLoader::new);
        register("Donut", DonutLoader::new);
        register("RainyCityStreet", RainyCityStreetLoader::new);
        register("Sushi", SushiLoader::new);
        register("RhythmDance", RhythmDanceLoader::new);
        //register("Alphabet", Alphabet::new); TODO Make an "Alphabet" Loader
        register("SnowGlobe", SnowGlobeLoader::new);
        register("Campfire", CampfireLoader::new);
        register("Waterfall", WaterfallLoader::new);
        register("IceCreamCone", IceCreamConeLoader::new);
        register("Satellite", SatelliteLoader::new);
        register("Tornado", TornadoLoader::new);
        register("QuantumDispersion", QuantumDispersionLoader::new);
        register("TropicalIsland", TropicalIslandLoader::new);
        register("QuantumOrbital", QuantumOrbitalLoader::new);
        register("QuantumSpectrograph",QuantumSpectrographLoader::new);
        register(List.of("GifPlayer", "GifFilePlayer"), GifFilePlayerLoader::new);
        register(List.of("MarioKart64", "MarioKart64ItemBox", "MarioKartItemBox"), MarioKart64ItemBoxLoader::new);
        register(List.of("Nintendo64", "Nintendo64Logo", "N64Logo"), Nintendo64LogoLoader::new);
        register(List.of("Xbox1Logo", "XboxLogo"), XboxLogoLoader::new);
        register(List.of("PlayStationIntro", "PlayStation1Intro", "PlayStationLogo", "PlayStation1Logo", "PS1Intro", "PS1"), PlayStation1IntroLoader::new);
        register("Butterfly", ButterflyLoader::new);
        register("Pluto", PlutoLoader::new);
        register("Oasis", OasisLoader::new);
        register("SlotMachine", SlotMachineLoader::new);
        register(List.of("Pinball", "PinballMachine"), PinballLoader::new);
        register("ChromeCheckerboardSphere", ChromeCheckerboardSphereLoader::new);
        register("TexelCompanionCube", TexelCompanionCubeLoader::new);
        register("GaltonBoard", GaltonBoardLoader::new);
        register("PaintCube", PaintCubeLoader::new);
        register("TexelMinecraftGrassBlock", TexelMinecraftGrassBlockLoader::new);
        register("TexelBorgCube", TexelBorgCubeLoader::new);
        register("TexelAllspark", TexelAllsparkLoader::new);
        register("TexelHyperspaceVoid", TexelHyperspaceVoidLoader::new);
        register("TexelEmojiCube", TexelEmojiCubeLoader::new);
        register(List.of("ConwaysGameOfLife", "GameOfLife"), ConwaysGameOfLifeLoader::new);
        register("TexelPlasmaCube", TexelPlasmaCubeLoader::new);
        register("TexelPsychCube", TexelPsychCubeLoader::new);
        register("TexelHellraiserLamentConfiguration", TexelHellraiserLamentConfigurationLoader::new);
        register("TexelFlatEarth", TexelFlatEarthLoader::new);
        register("TexelMario64CapBlock",TexelMario64CapBlockLoader::new);
        register("TexelGlitch", TexelGlitchLoader::new);
        register(List.of("Aizawa", "AizawaAttractor"), AizawaLoader::new);
        register("TexelRubixCube", TexelRubixCubeLoader::new);
        register("TexelGreyRubix", TexelGreyRubixLoader::new);
        register("TexelMCUTesseract", TexelMCUTesseractLoader::new);
        register("TexelTriplePhaseCube", TexelTriplePhaseCubeLoader::new);
        register("TexelMarioBricks", TexelMarioBricksLoader::new);
        register("TexelMonochromeStatic",TexelMonochromeStaticLoader::new);
        register(List.of("Curiosity", "CuriosityRover", "MarsRover"), CuriosityRoverLoader::new);
        register("MarioCoinBlock", TexelMarioCoinBlockLoader::new);
        //register("CornellBox", CornellBoxLoader::new); TODO Make a "Cornell Box" Loader
        register(List.of("Suzanne", "SuzanneTheMonkey"), SuzanneTheMonkeyLoader::new);
        register("CatHead", CatHeadLoader::new);
        register("IkedaRibbon", IkedaRibbonLoader::new);
        register("Synthwave", SynthWaveLoader::new);
        register("BarnsleyFernZoom", BarnsleyFernZoomLoader::new);
        register("Mercury", MercuryLoader::new);
        register("Vaporwave", VaporWaveLoader::new);
        register("LunarLander", LunarLanderLoader::new);
        //register("Cupcake", CupcakeLoader::new); TODO Make a Cupcake loader (with flavor variants??)
        register("Taco", TacoLoader::new);
        register("Jupiter", JupiterLoader::new);
        register(List.of("GreyScottReactionDiffusion", "ReactionDiffusion", "DiffusionReaction"), ReactionDiffusionLoader::new);
        register("MotorcycleRacer", MotorcycleRacerLoader::new);
        register("TexelEldritchAetherCube", TexelEldritchAetherCubeLoader::new);
        register("Radar", RadarLoader::new);
        register(List.of("NASAShuttle", "SpaceShuttle"), SpaceShuttleLoader::new);
        register("PsychedelicIcosahedron", PsychedelicIcosahedronLoader::new);
        register("HillTree", HillTreeLoader::new);
        register("StanfordBunny", StanfordBunnyLoader::new);
        register("MandelbrotZoom", MandelbrotZoomLoader::new);
        register("DNA", DNALoader::new);
        //register(List.of("TheSun", "Sol", "Sun"), SolarLoader::new); TODO Make a Loader for just the Sun
        //register(List.of("SolarSystem", "TheSolarSystem"), SolarSystemLoader::new); TODO Make a Loader for the whole Solar System
        register("Pendulum", PendulumLoader::new);
        register("Saturn", SaturnLoader::new);
        //register("AsteroidBelt", AsteroidBeltLoader::new); TODO Make a Loader for the Asteroid Belt
        register("SchwarzschildGeodesic", SchwarzschildGeodesicLoader::new);
        register("Earth", EarthLoader::new);
        register(List.of("Lorenz", "LorenzAttractor"), LorenzLoader::new);
        register(List.of("Burger", "Cheeseburger"), CheeseburgerLoader::new);
        //register(List.of("RippleTank", "RippleWaves"), RippleTankAutomatonLoader::new); // TODO Make more Ripple Tanks with the same waves as DoubleSlit
        register(List.of("DoubleSlitExperiment", "DoubleSlit"), DoubleSlitLoader::new);
        register("WavePropagation", WavePropagationLoader::new);
        register(List.of("CorridorWaveGuide", "CorridorWave"), CorridorWaveLoader::new);
        register("SeifertSurface", SeifertLoader::new);
        register("TextFall", TextFallLoader::new);
        register(List.of("GraphPlot", "Desmos"), GraphLoader::new);
        register("WireframeFisheyeGlobe", WireframeFisheyeGlobeLoader::new);
        register("Bad Apple", () -> new ImageFolderLoader("Bad Apple"));
        register(List.of("HotDog", "Glizzy"), HotDogLoader::new);
        register("RainbowRing", RainbowRingLoader::new);
        register("CosmicBrownie", CosmicBrownieLoader::new);
        register(List.of("SonicRing", "SonicRings"), SonicRingLoader::new);
        register("HaloRing", HaloRingLoader::new);
        register("KleinRing", KleinRingLoader::new);
        register("RockPaperScissors", RockPaperScissorsAutomatonLoader::new);
        register("FrutigerAero", FrutigerAeroLoader::new);
        register("Labyrinth3D", Labyrinth3DLoader::new);
        register("RainbowWhispSphere", RainbowWhispSphereLoader::new);
        register(List.of("Pizza", "PizzaSlice", "SliceOfPizza"), PizzaLoader::new);
        register(List.of("ImageFolder", "ImagePlayer"), ImageFolderLoader::new);
        register("TexelMuseumCube", TexelMuseumCubeLoader::new);
        register("PacManSphere", PacManSphereLoader::new);
        register(List.of("InternationalSpaceStation", "ISS"), InternationalSpaceStationLoader::new);
        register("PsychedelicOctahedron", PsychedelicOctahedronLoader::new);
        register("RainbowWhispPyramid", RainbowWhispPyramidLoader::new);
        register(List.of("Tesseract", "4D Cube"), TesseractLoader::new);
        register(List.of("EyeOfProvidence", "Illuminati"), EyeOfProvidenceLoader::new);
        register("WireframeSphere", WireframeSphereLoader::new);
        register("Louvre", LouvreLoader::new);
        register(List.of("FluidDynamics", "NavierStokes"), FluidDynamicsLoader::new);
        register(List.of("DJ", "DJTurntable"), DJTurntableLoader::new);
        register("Moon", MoonLoader::new);
        register("PerlinNoiseA", PerlinNoiseLoaderA::new);
        register(List.of("ApolloLunarModule", "LunarModule"), ApolloLunarModuleLoader::new);
        register("PerlinNoiseB", PerlinNoiseLoaderB::new);
        register("SydneyOperaHouse", SydneyOperaHouseLoader::new);
        register("Cookie", CookieLoader::new);
        register("EiffelTower", EiffelTowerLoader::new);
        register(List.of("HubbleTelescope", "Hubble"), HubbleTelescopeLoader::new);
        register(List.of("Voyager", "VoyagerSpacecraft", "VoyagerProbe"), VoyagerLoader::new);
        register("TajMahal", TajMahalLoader::new);
        register("Slitherio", SlitherioLoader::new);
        register(List.of("ChicagoBean", "CloudGate"), ChicagoBeanLoader::new);
        register("BigBen", BigBenLoader::new);
        register("TexelKevinCube", TexelKevinCubeLoader::new);
        register("NyanCat", NyanCatLoader::new);
        register("BouncingSpinner", BouncingSpinnerLoader::new);
        register("TexelHyperchromaticKaleidoscope", TexelHyperchromaticKaleidoscopeLoader::new);
        register("DancingBanana", DancingBananaLoader::new);
        register("ThreeBody", ThreeBodyLoader::new);
        register("Mars", MarsLoader::new);
        register("WireframeCube", WireframeCubeLoader::new);
        register(List.of("HyperSphere", "4D Sphere"), HypersphereLoader::new);
        register("HopfFibration", HopfFibrationLoader::new);
        register("Snake", SnakeLoader::new);
        register("Pong", PongLoader::new);
        register(List.of("BrickBreakout", "Breakout"), BrickBreakoutLoader::new);
        register("SpaceInvaders", SpaceInvadersLoader::new);
        register("Chess", ChessLoader::new);
        register("Checkers", CheckersLoader::new);
        register("Tetris", TetrisLoader::new);
        register("Asteroids", AsteroidsLoader::new);
        register("Centepede", CentipedeLoader::new);
        register("Fish", FishLoader::new);
        register("Neptune", NeptuneLoader::new);
        register(List.of("Drumstick", "TurkeyLeg"), DrumstickMeatLoader::new);
        register("BaukusManifold", BaukusManifoldLoader::new);
        register("Galaga", GalagaLoader::new);
        register("LangtonsAnt", LangtonsAntLoader::new);
        register("UtahTeapot", UtahTeapotLoader::new);
        register(List.of("Boids", "HerdingAndFlocking"), BoidsLoader::new);
        register(List.of("TriColorChromeSpheres", "TriColorSpheres", "TriColorCheckerboard"), TriColorChromeSpheresLoader::new);
        register("BriansBrain", BriansBrainLoader::new);
        register("GyroidA", GyroidLoaderA::new);
        register("GyroidB", GyroidLoaderB::new);
        register(List.of("DFSMazeSolver", "DepthFirstSearch"), DFSMazeSolverLoader::new);
        register(List.of("BFSMazeSolver", "BreadthFirstSearch"), BFSMazeSolverLoader::new);
        register(List.of("AStarMazeSolver", "A*MazeSolver", "AStar", "A*"), AStarMazeSolverLoader::new);
        register(List.of("DeadEndFillingMazeSolver", "CulDeSacCullingMazeSolver"), DeadEndFillingMazeSolverLoader::new);
        register("Tron", TronLoader::new);
        register("ToyBoat", ToyBoatLoader::new);
        register("ToyTrain", ToyTrainLoader::new);
        register("ToyCar", ToyCarLoader::new);
        register("Uranus", UranusLoader::new);
        register("FlappyBird", FlappyBirdLoader::new);
        register("Venus", VenusLoader::new);
        register("GravityFabric", GravityFabricLoader::new); 
        register("TexelMagicalGlyph", TexelMagicalGlyphCubeLoader::new); 
        register("MengerSponge", MengerSpongeLoader::new); 
        register("KaleidoscopicFractal", KaleidoscopicFractalLoader::new);

        // Derive names list automatically from unique registered keys
        LOADER_NAMES = new ArrayList<>(REGISTRY.keySet());
    }

    // Single-key helper registration
    private static void register(String name, Supplier<Loader> constructor) {
        REGISTRY.put(name.toLowerCase(), constructor);
        UNIQUE_LOADERS.add(constructor);
    }

    // Multi-key/Alias helper registration
    private static void register(List<String> names, Supplier<Loader> constructor) {
        for (String name : names) {
            REGISTRY.put(name.toLowerCase(), constructor);
        }
        // ONCE per call, not once per name, so a loader with multiple names
        // isn't more likely to be picked from the fallback at random.
        UNIQUE_LOADERS.add(constructor);
    }

    public static Loader createLoaderInstance() {
        return fallbackRandomInstance();
    }

    public static Loader createLoaderInstance(String requestedName) {
        if (requestedName == null || requestedName.isEmpty()) {
            return fallbackRandomInstance();
        }

        String targetKey = requestedName.toLowerCase();

        // 1. Direct case-insensitive O(1) Dictionary Lookup
        Supplier<Loader> match = REGISTRY.get(targetKey);
        if (match != null) {
            return match.get();
        }

        // 2. Spell-checking Matcher
        String closestMatch = findClosestMatch(targetKey, LOADER_NAMES);
        if (closestMatch != null) {
            return REGISTRY.get(closestMatch).get();
        }

        // 3. Absolute Fallback State
        return fallbackRandomInstance();
    }

    private static Loader fallbackRandomInstance() {
        int randomIndex = RANDOM.nextInt(UNIQUE_LOADERS.size());
        return UNIQUE_LOADERS.get(randomIndex).get();
    }

    private static String findClosestMatch(String inputLower, List<String> possibilities) {
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String option : possibilities) {
            int m = inputLower.length();
            int n = option.length();
            
            // Early exit optimization: If lengths differ by more than current minDistance 
            // AND more than the structural threshold, we can skip calculating the DP matrix entirely.
            int lengthDiff = Math.abs(m - n);
            int structuralThreshold = Math.max(4, n / 2);
            if (lengthDiff >= minDistance || lengthDiff > structuralThreshold) {
                continue;
            }

            int[][] dp = new int[m + 1][n + 1];

            for (int i = 0; i <= m; i++) dp[i][0] = i;
            for (int j = 0; j <= n; j++) dp[0][j] = j;

            for (int i = 1; i <= m; i++) {
                for (int j = 1; j <= n; j++) {
                    if (inputLower.charAt(i - 1) == option.charAt(j - 1)) {
                        dp[i][j] = dp[i - 1][j - 1];
                    } else {
                        dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                    }
                }
            }

            int finalDistance = dp[m][n];
            if (finalDistance < minDistance && finalDistance <= structuralThreshold) {
                minDistance = finalDistance;
                bestMatch = option;
            }
        }
        return bestMatch;
    }
}