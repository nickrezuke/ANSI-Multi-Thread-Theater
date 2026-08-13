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

    private static final Random RANDOM = new Random();

    static {
        // Define all loaders here.... (w/ lists of multiple possible names)
        register("Chladni", ChladniLoader::new);
        register("Apple", AppleLoader::new);
        register("RefractiveMagnifier", RefractiveMagnifierLoader::new);
        register("SpiderWeb", SpiderWebLoader::new);
        register("Donut", DonutLoader::new);
        register("RainyCityStreet", RainyCityStreetLoader::new);
        register("Sushi", SushiLoader::new);
        //register("Alphabet", Alphabet::new); TODO Make an "Alphabet" Loader
        register("SnowGlobe", SnowGlobeLoader::new);
        register("Campfire", CampfireLoader::new);
        register("Waterfall", WaterfallLoader::new);
        register("IceCreamCone", IceCreamConeLoader::new);
        register("Tornado", TornadoLoader::new);
        register("QuantumDispersion", QuantumDispersionLoader::new);
        register("TropicalIsland", TropicalIslandLoader::new);
        register("QuantumOrbital", QuantumOrbitalLoader::new);
        register("QuantumSpectrograph",QuantumSpectrographLoader::new);
        register(List.of("MarioKart64", "MarioKart64ItemBox", "MarioKartItemBox"), MarioKart64ItemBoxLoader::new);
        register(List.of("Nintendo64", "Nintendo64Logo", "N64Logo"), Nintendo64LogoLoader::new);
        register(List.of("Xbox1Logo", "XboxLogo"), XboxLogoLoader::new);
        register(List.of("PlayStationIntro", "PlayStation1Intro", "PlayStationLogo", "PlayStation1Logo"), PlayStation1IntroLoader::new);
        register("Butterfly", ButterflyLoader::new);
        //register("Pluto", PlutoLoader::new); TODO Make a Pluto Loader
        register("SlotMachine", SlotMachineLoader::new);
        register(List.of("Pinball", "PinballMachine"), PinballLoader::new);
        register("ChromeCheckerboardSphere", ChromeCheckerboardSphereLoader::new);
        register("TexelCompanionCube", TexelCompanionCubeLoader::new);
        register("GaltonBoard", GaltonBoardLoader::new);
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
        //register("CosmicBrownie", CosmicBrownieLoader::new); TODO Make a Cosmic Brownie Loader
        register("TexelMarioBricks", TexelMarioBricksLoader::new);
        register("TexelMonochromeStatic",TexelMonochromeStaticLoader::new);
        register("MarioCoinBlock", TexelMarioCoinBlockLoader::new);
        //register("CornellBox", CornellBoxLoader::new); TODO Make a "Cornell Box" Loader
        register(List.of("Suzanne", "SuzanneTheMonkey"), SuzanneTheMonkeyLoader::new);
        register("CatHead", CatHeadLoader::new);
        register("IkedaRibbon", IkedaRibbonLoader::new);
        register("Synthwave", SynthWaveLoader::new);
        register("BarnsleyFernZoom", BarnsleyFernZoomLoader::new);
        //register("Mercury", MercuryLoader::new); TODO Make a Mercury Loader
        register("Vaporwave", VaporWaveLoader::new);
        register("LunarLander", LunarLanderLoader::new);
        //register("Cupcake", CupcakeLoader::new); TODO Make a Cupcake loader (with flavor variants??)
        register("Jupiter", JupiterLoader::new);
        register(List.of("GreyScottReactionDiffusion", "ReactionDiffusion", "DiffusionReaction"), ReactionDiffusionLoader::new);
        register("MotorcycleRacer", MotorcycleRacerLoader::new);
        register("TexelEldritchAetherCube", TexelEldritchAetherCubeLoader::new);
        register("Radar", RadarLoader::new);
        //register("HotDog", HotDogLoader::new); TODO Make a Hot Dog Loader
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
        register("SchwarzschildGeodesic", SchwarzschildGeodesicLoader::new);
        register("Earth", EarthLoader::new);
        register(List.of("Lorenz", "LorenzAttractor"), LorenzLoader::new);
        //register(List.of("RippleTank", "RippleWaves"), RippleTankAutomatonLoader::new); // TODO Make more Ripple Tanks with the same waves as DoubleSlit
        register(List.of("DoubleSlitExperiment", "DoubleSlit"), DoubleSlitLoader::new);
        register("WavePropagation", WavePropagationLoader::new);
        register(List.of("CorridorWaveGuide", "CorridorWave"), CorridorWaveLoader::new);
        register("SeifertSurface", SeifertLoader::new);
        register("TextFall", TextFallLoader::new);
        register("FisheyeGlobe", FisheyeGlobeLoader::new);
        register("Ring", RingLoader::new);
        register(List.of("SonicRing", "SonicRings"), SonicRingLoader::new);
        register("HaloRing", HaloRingLoader::new);
        register("KleinRing", KleinRingLoader::new);
        register("RockPaperScissors", RockPaperScissorsAutomatonLoader::new);
        register("FrutigerAero", FrutigerAeroLoader::new);
        register("Labyrinth3D", Labyrinth3DLoader::new);
        register("RainbowWhispSphere", RainbowWhispSphereLoader::new);
        register("TexelMuseumCube", TexelMuseumCubeLoader::new);
        register("PacManSphere", PacManSphereLoader::new);
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
        register("PerlinNoiseB", PerlinNoiseLoaderB::new);
        register("SydneyOperaHouse", SydneyOperaHouseLoader::new);
        register("EiffelTower", EiffelTowerLoader::new);
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
        //register("Mars", MarsLoader::new); TODO Make a Mars Loader
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
        register("Neptune", NeptuneLoader::new);
        register("Galaga", GalagaLoader::new);
        register("LangtonsAnt", LangtonsAntLoader::new);
        register("UtahTeapot", UtahTeapotLoader::new);
        register(List.of("Boids", "HerdingAndFlocking"), BoidsLoader::new);
        register(List.of("TriColorChromeSpheres", "TriColorSpheres"), TriColorChromeSpheresLoader::new);
        register("BriansBrain", BriansBrainLoader::new);
        register("GyroidA", GyroidLoaderA::new);
        register("GyroidB", GyroidLoaderB::new);
        register("Tron", TronLoader::new);
        register("Uranus", UranusLoader::new);
        register("FlappyBird", FlappyBirdLoader::new);
        //register("Venus", VenusLoader::new); TODO Make a Venus Loader
        register("Unused", UnusedInfiniteFractalLoader::new); 
        register("Unused2", UnusedInfiniteFractalLoader2::new);

        // Derive names list automatically from unique registered keys
        LOADER_NAMES = new ArrayList<>(REGISTRY.keySet());
    }

    // Single-key helper registration
    private static void register(String name, Supplier<Loader> constructor) {
        REGISTRY.put(name.toLowerCase(), constructor);
    }

    // Multi-key/Alias helper registration
    private static void register(List<String> names, Supplier<Loader> constructor) {
        for (String name : names) {
            REGISTRY.put(name.toLowerCase(), constructor);
        }
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
        int randomIndex = RANDOM.nextInt(LOADER_NAMES.size());
        String selectedKey = LOADER_NAMES.get(randomIndex);
        return REGISTRY.get(selectedKey).get();
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