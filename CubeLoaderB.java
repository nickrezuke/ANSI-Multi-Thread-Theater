import java.util.Random;
import java.util.stream.Collectors;

public class CubeLoaderB extends Loader {
    private int blockVariant = -1;
    private double angle = 0.0;

    private static int getRandomVariant() {
        // Right now there are 15 variants
        return (int) (Math.random() * 15) + 1;
    }

    public CubeLoaderB() {
        this(getRandomVariant());
    }

    public CubeLoaderB(int variant) {
        StatusStage[] CUBE_STAGES;
        variant = 8;

        switch (variant) {
            case 1: // Minecraft Grass Block
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(10, "Generating Chunks:"),
                        new StatusStage(20, "Growing Forests:"),
                        new StatusStage(30, "Populating Villages:"),
                        new StatusStage(40, "Scattering Ore Deposits:"),
                        new StatusStage(50, "Heating up the Nether:"),
                        new StatusStage(60, "Building Strongholds:"),
                        new StatusStage(70, "Filling in Loot Chests:"),
                        new StatusStage(80, "Determining World Spawn:"),
                        new StatusStage(90, "Initializing Player:"),
                        new StatusStage(99, "Removing Herobrine:"),
                        new StatusStage(100, "World Generation Complete!")
                };
                break;
            case 2: // Companion Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(8, "Calibrating Scientific Intruments:"),
                        new StatusStage(28, "Designing Test Chamber:"),
                        new StatusStage(55, "Synthesizing Test Subjects:"),
                        new StatusStage(75, "Activating Observation Deck:"),
                        new StatusStage(96, "Calibrating Portal Devices:"),
                        new StatusStage(98, "Rejecting Morality Upgrade:"),
                        new StatusStage(100, "Test Chamber Ready!")
                };
                break;
            case 14:
            case 3: // Rubik's Cube Cases
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(12, "Scrambling:"),
                        new StatusStage(24, "Rotating:"),
                        new StatusStage(36, "Scrambling:"),
                        new StatusStage(48, "Rotating:"),
                        new StatusStage(60, "Scrambling:"),
                        new StatusStage(72, "Rotating:"),
                        new StatusStage(84, "Scrambling:"),
                        new StatusStage(96, "Rotating:"),
                        new StatusStage(100, "Scramble Solved!")
                };
                break;
            case 4: // Kevin the Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(15, "Manifesting Lightning Strike:"),
                        new StatusStage(30, "Burning Dark Runes:"),
                        new StatusStage(45, "Charging Kinetic Shield:"),
                        new StatusStage(60, "Corrupting Nearby Soil:"),
                        new StatusStage(75, "Summoning Cube Monsters:"),
                        new StatusStage(88, "Floating Towards Loot Lake:"),
                        new StatusStage(95, "Initiating The Butterfly Event:"),
                        new StatusStage(99, "Shattering Into The In-Between:"),
                        new StatusStage(100, "Zero Point Reached!")
                };
                break;
            case 5: // Star Trek Borg Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(10, "Establishing Collective Link:"),
                        new StatusStage(22, "Interlocking Biomechanical Matrix:"),
                        new StatusStage(35, "Powering Sub-Space Transwarp Coil:"),
                        new StatusStage(50, "Analyzing Target Biological Distinctiveness:"),
                        new StatusStage(65, "Charging Distribution Node Circuitry:"),
                        new StatusStage(80, "Modulating Shield Frequencies:"),
                        new StatusStage(92, "Locking Tractor Beams:"),
                        new StatusStage(98, "Lowering Deflector Shields:"),
                        new StatusStage(100, "Assimilation Successful!")
                };
                break;
            case 6: // Allspark Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(12, "Unlocking Ancient Cybernetic Etchings:"),
                        new StatusStage(25, "Harnessing Raw Energon Currents:"),
                        new StatusStage(40, "Decoding Prime Language Matrix:"),
                        new StatusStage(55, "Compressing Universal Scale Matrix:"),
                        new StatusStage(70, "Converting Surrounding Mechanical Elements:"),
                        new StatusStage(85, "Awakening Mechanical Consciousness:"),
                        new StatusStage(96, "Stabilizing Core Energy Outburst:"),
                        new StatusStage(100, "AllSpark Stabilized!")
                };
                break;
            case 12:
            case 7: // Mario Block Cases
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(16, "Starting Level:"),
                        new StatusStage(30, "Stomping Enemies:"),
                        new StatusStage(42, "Kicking Shells:"),
                        new StatusStage(65, "Collecting Coins:"),
                        new StatusStage(85, "Utilizing Power-Ups:"),
                        new StatusStage(96, "Sliding Down Flagpole:"),
                        new StatusStage(100, "Level Complete!:")
                };
                break;
            case 8: // Psychedelic Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(11, "Debating If You Got Scammed:"),
                        new StatusStage(30, "Chasing White Rabbits:"),
                        new StatusStage(55, "Wiggin' Out, Man:"),
                        new StatusStage(75, "Stroking the Furry Walls:"),
                        new StatusStage(96, "Thinking this lasts forever:"),
                        new StatusStage(100, "Enlightenment Achieved!")
                };
                break;
            case 9: // Expression Face Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(30, "Feeling the Vibe:"),
                        new StatusStage(65, "Injecting Emotional Vectors:"),
                        new StatusStage(90, "Smoothing Personality:"),
                        new StatusStage(100, "Emojis Activated!")
                };
                break;
            case 10: // Hellraiser Lament Configuration (Puzzle Box)
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(20, "???:"),
                        new StatusStage(50, "???:"),
                        new StatusStage(80, "???:"),
                        new StatusStage(100, "???!")
                };
                break;
            case 11: // Cube Earth
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(25, "Condensing Tectonic Plates:"),
                        new StatusStage(55, "Filling Cubical Ocean Depths:"),
                        new StatusStage(85, "Freezing Square Polar Icecaps:"),
                        new StatusStage(100, "Planetary Orbit Synchronized!")
                };
                break;
            case 13: // Glitch Static
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(10,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(20,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(30,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(40,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(50,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(60,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(70,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(80,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(90,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(98,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + ":"),
                        new StatusStage(100,
                                new Random().ints(new Random().nextInt(5) + 8, 0, 62).mapToObj(i -> String.valueOf(
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i)))
                                        .collect(Collectors.joining()) + "!:")
                };
                break;
            case 15: // Warp Circle Cube
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(25, "Calibrating Flux Capacitor:"),
                        new StatusStage(55, "Warping through SpaceTime:"),
                        new StatusStage(85, "Signaling Homeworld:"),
                        new StatusStage(100, "Dimensional Rift Cleared!")
                };
                break;
            default: // default (should not happen)
                CUBE_STAGES = new StatusStage[] {
                        new StatusStage(98, "Loading:"),
                        new StatusStage(100, "Loading Complete!:")
                };
                break;
        }
        super(CUBE_STAGES);
        blockVariant = variant;
    }

    @Override
    protected void initialize() {
        // We should have already determined which one
        // to use, but just in case we didn't yet:
        if (this.blockVariant == -1) {
            // Randomly select between
            this.blockVariant = getRandomVariant();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double rX = angle * 0.4;
        double rY = angle * 0.7;
        double rZ = angle * 0.2;

        double cosX = Math.cos(rX), sinX = Math.sin(rX);
        double cosY = Math.cos(rY), sinY = Math.sin(rY);
        double cosZ = Math.cos(rZ), sinZ = Math.sin(rZ);

        for (int face = 0; face < 6; face++) {
            double nx = 0, ny = 0, nz = 0;
            switch (face) {
                case 0:
                    nz = -1;
                    break; // Back
                case 1:
                    nz = 1;
                    break; // Front
                case 2:
                    ny = -1;
                    break; // Bottom
                case 3:
                    ny = 1;
                    break; // Top
                case 4:
                    nx = -1;
                    break; // Left
                case 5:
                    nx = 1;
                    break; // Right
            }

            double nz3 = -nx * sinY + (ny * sinX + nz * cosX) * cosY;
            if (nz3 > 0) {
                continue;
            }

            for (double u = 0; u <= 1.0; u += 0.02) {
                for (double v = 0; v <= 1.0; v += 0.02) {

                    double uc = 2.0 * u - 1.0;
                    double vc = 2.0 * v - 1.0;
                    double x = 0, y = 0, z = 0;

                    switch (face) {
                        case 0:
                            x = uc;
                            y = vc;
                            z = -1;
                            break;
                        case 1:
                            x = uc;
                            y = vc;
                            z = 1;
                            break;
                        case 2:
                            x = uc;
                            y = -1;
                            z = vc;
                            break;
                        case 3:
                            x = uc;
                            y = 1;
                            z = vc;
                            break;
                        case 4:
                            x = -1;
                            y = uc;
                            z = vc;
                            break;
                        case 5:
                            x = 1;
                            y = uc;
                            z = vc;
                            break;
                    }

                    // --- 3D ROTATION ---
                    double y1 = y * cosX - z * sinX;
                    double z1 = y * sinX + z * cosX;
                    double x1 = x;

                    double x2 = x1 * cosY + z1 * sinY;
                    double z2 = -x1 * sinY + z1 * cosY;
                    double y2 = y1;

                    double x3 = x2 * cosZ - y2 * sinZ;
                    double y3 = x2 * sinZ + y2 * cosZ;
                    double z3 = z2;

                    // --- PERSPECTIVE PROJECTION ---
                    double distanceToCamera = 3.1;
                    double ooz = 1.0 / (z3 + distanceToCamera);

                    int xp = (int) (40 + 40 * ooz * x3);
                    int yp = (int) (11 - 18 * ooz * y3);

                    if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                        int index = xp + 80 * yp;

                        if (ooz > zBuffer[index] + 0.0001) {
                            zBuffer[index] = ooz;

                            double texU = u;
                            double texV = v;

                            // Standardized face layout alignment tracking
                            switch (face) {
                                case 0:
                                    texU = u;
                                    texV = 1.0 - v;
                                    break;
                                case 1:
                                    texU = 1.0 - u;
                                    texV = 1.0 - v;
                                    break;
                                case 4:
                                    texU = 1.0 - v;
                                    texV = 1.0 - u;
                                    break;
                                case 5:
                                    texU = v;
                                    texV = 1.0 - u;
                                    break;
                                case 2:
                                case 3:
                                    texU = u;
                                    texV = v;
                                    break;
                            }

                            int texX = (int) (texU * 16);
                            int texY = (int) (texV * 16);

                            texX = Math.max(0, Math.min(15, texX));
                            texY = Math.max(0, Math.min(15, texY));

                            VoxelTexel texel = getCubeTexel(blockVariant, face, texX, texY);

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", texel.r, texel.g, texel.b);
                            outputBuffer[index] = colorCode + texel.character + RESET;
                        }
                    }
                }
            }
        }
        angle += 0.025;
    }

    private VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        // High-frequency deterministic hash pattern accent noise
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        if (variant == 1) { // ==================== MINECRAFT GRASS BLOCK ====================
            // 1. Map higher-grain 4-level voxel noise
            noise = (int) (Math.abs((x * 2693L + y * 7919L + face * 4567L) ^ 0x5DEECE66DL) % 4);

            // 2. Uniform pixel block characters to simulate raw voxels
            char blockChar = '\u2588'; // █ (Solid block fill)

            // --- FACE 3: TOP GRASS FACE ---
            if (face == 3) {
                switch (noise) {
                    case 0:
                        return new VoxelTexel(80, 145, 30, blockChar); // Muted Forest Green
                    case 1:
                        return new VoxelTexel(85, 155, 35, blockChar); // Standard Grass (Baseline)
                    case 2:
                        return new VoxelTexel(92, 165, 38, blockChar); // Soft Mid-Tone Green
                    default:
                        return new VoxelTexel(98, 175, 42, blockChar); // Mild Highlights (Toned down)
                }
            }

            // --- FACE 2: BOTTOM DIRT FACE ---
            if (face == 2) {
                switch (noise) {
                    case 0:
                        return new VoxelTexel(90, 62, 38, blockChar); // Soft Dark Dirt
                    case 1:
                        return new VoxelTexel(98, 68, 42, blockChar); // Standard Dirt (Baseline)
                    case 2:
                        return new VoxelTexel(106, 74, 46, blockChar); // Soft Mid-Tone Brown
                    default:
                        return new VoxelTexel(114, 80, 50, blockChar); // Light Gravel Specks
                }
            }

            // --- FACES 0, 1, 4, 5: SIDE GRASS/DIRT BLENDS ---
            int grassThreshold = 4 + (x % 3 == 0 ? 1 : 0) + ((x * 7) % 2);

            if (y < grassThreshold) {
                // Side Grass Lip (Matches the toned-down Top Grass palette)
                switch (noise) {
                    case 0:
                        return new VoxelTexel(80, 145, 30, blockChar);
                    case 1:
                        return new VoxelTexel(85, 155, 35, blockChar);
                    case 2:
                        return new VoxelTexel(92, 165, 38, blockChar);
                    default:
                        return new VoxelTexel(98, 175, 42, blockChar);
                }
            } else {
                // Exposed Side Dirt (Matches the toned-down Bottom Dirt palette)
                switch (noise) {
                    case 0:
                        return new VoxelTexel(90, 62, 38, blockChar);
                    case 1:
                        return new VoxelTexel(98, 68, 42, blockChar);
                    case 2:
                        return new VoxelTexel(106, 74, 46, blockChar);
                    default:
                        return new VoxelTexel(114, 80, 50, blockChar);
                }
            }
        } else if (variant == 2) { // ==================== PORTAL COMPANION CUBE ====================
            // 2. Exact Coordinate System for the Aperture Science Framing
            double dx = x - 7.5;
            double dy = y - 7.5;
            double radius = Math.sqrt(dx * dx + dy * dy);

            // Layer A: The Central Aperture Pink Heart
            boolean isHeart = false;
            if (x >= 5 && x <= 10 && y >= 5 && y <= 9) {
                if (y == 5)
                    isHeart = (x == 6 || x == 9);
                if (y == 6)
                    isHeart = (x >= 5 && x <= 10);
                if (y == 7)
                    isHeart = (x >= 5 && x <= 10);
                if (y == 8)
                    isHeart = (x >= 6 && x <= 9);
                if (y == 9)
                    isHeart = (x == 7 || x == 8);
            }

            // Layer B: The Central Circular Aperture Disc (White Backing)
            boolean isWhiteDisc = (radius >= 0.0 && radius <= 4.2);

            // Layer C: Heavy Corner Bevel Protective Brackets & Outer Rim Trim
            // Recreates the thick, separate armor sheets bolted onto each vertex
            boolean isCornerBracket = (x <= 3 || x >= 12) && (y <= 3 || y >= 12);
            boolean isOuterRim = (x <= 1 || x >= 14 || y <= 1 || y >= 14);
            boolean isLightArmor = isCornerBracket || isOuterRim;

            // Layer D: Drop Shadows under the light armor plates for 3D depth perception
            boolean isPlateShadow = (x == 4 && (y <= 3 || y >= 12)) || (x == 11 && (y <= 3 || y >= 12)) ||
                    (y == 4 && (x <= 3 || x >= 12)) || (y == 11 && (x <= 3 || x >= 12));

            // --- PORTAL COMPOSITING PIPELINE ---

            // 1. High-Density Pink Heart Centerpiece
            if (isHeart) {
                int r = 245;
                int g = 110 + noise * 5;
                int b = 155; // Vibrant Hot Pink / Magenta
                return new VoxelTexel(r, g, b, '\u2588'); // █ (Solid colored energy core)
            }

            // 2. Circular Aperture Backdrop Ring
            if (isWhiteDisc) {
                int w = 215 + noise * 10;
                return new VoxelTexel(w, w, w, '\u2593'); // ▓ (Thick light textured disk)
            }

            // 3. Extruded Corner Shields and Rim Castings (Light Alloy)
            if (isLightArmor && !isPlateShadow) {
                int armor = 165 - noise * 6;
                return new VoxelTexel(armor, armor, armor + 5, '\u2588'); // █ (Solid thick plating)
            }

            // 4. Drop Shadow Insets flanking the armor borders
            if (isPlateShadow) {
                int shadow = 35 + noise * 3;
                return new VoxelTexel(shadow, shadow, shadow + 3, ';'); // Deep recessed groove
            }

            // 5. Central Inset Core Plating (Dark Alloy Base Hull)
            int hull = 80 + noise * 6;
            return new VoxelTexel(hull, hull, hull + 4, '\u2592'); // ▒ (Medium cross-hatched alloy matrix)

        } else if (variant == 3) { // ==================== CLASSIC 3X3 RUBIK'S CUBE ====================
            // 1. Gridlines: 16x16 boundaries wrapping 3x3 tiles separated by 5-cell
            // intervals
            if (x == 0 || x == 5 || x == 10 || x == 15 || y == 0 || y == 5 || y == 10 || y == 15) {
                // High-end matte black plastic grid boundaries
                return new VoxelTexel(20, 20, 25, '#');
            }

            // 2. Identify the specific sub-sticker quadrant coordinate (0, 1, or 2)
            int row = (y < 5) ? 0 : (y < 10) ? 1 : 2;
            int col = (x < 5) ? 0 : (x < 10) ? 1 : 2;

            // 3. Scrambled Sticker State Machine
            // A deterministic shuffle array to make the cube look realistically mixed up
            int[] shuffleArray = {
                    2, 0, 5, 1, 4, 3, 0, 4, 1, 5, 2, 3, 4, 1, 0, 2, 3, 5,
                    5, 3, 2, 0, 1, 4, 1, 5, 3, 4, 0, 2, 3, 2, 4, 1, 5, 0,
                    0, 1, 4, 2, 5, 3, 5, 2, 1, 0, 3, 4, 2, 4, 3, 5, 1, 0
            };

            // Calculate a unique index for each individual sticker slot (0 to 53)
            int stickerIndex = Math.abs(face * 9 + row * 3 + col) % shuffleArray.length;
            int stickerColor = shuffleArray[stickerIndex];

            // Subtle internal sticker highlight to give a glossy/curved reflection look
            boolean isHighlight = (x == 1 || x == 6 || x == 11) && (y == 1 || y == 6 || y == 11);

            // 4. Color Assignment and Material Rendering
            // Uses uniform unicode block primitives to look like flat plastic tiles
            char blockChar = '\u2588'; // █ (Solid color mass)

            switch (stickerColor) {
                case 0: // Radiant Red
                    return new VoxelTexel(isHighlight ? 255 : 220, 35, 35, blockChar);
                case 1: // Deep Blue
                    return new VoxelTexel(30, 100, isHighlight ? 255 : 230, blockChar);
                case 2: // Bright Orange
                    return new VoxelTexel(255, isHighlight ? 155 : 120, 15, blockChar);
                case 3: // Neon Green
                    return new VoxelTexel(45, isHighlight ? 245 : 200, 55, blockChar);
                case 4: // Pure Ceramic White
                    int w = isHighlight ? 255 : 240;
                    return new VoxelTexel(w, w, w, blockChar);
                default: // Vivid Canary Yellow
                    return new VoxelTexel(245, isHighlight ? 255 : 225, 25, blockChar);
            }
        } else if (variant == 4) { // ==================== FORTNITE KEVIN THE CUBE ====================
            // floating point wave ranging from 0.0 (dim) to 1.0 (bright) every ~2.5 seconds
            double timeWave = (Math.sin(System.currentTimeMillis() * 0.0025) + 1.0) / 2.0;

            // Mapping out Kevin's ancient runes
            long[][] kevinRunes = {
                    { 0x0000, 0x07E0, 0x0810, 0x1008, 0x23C4, 0x2424, 0x2424, 0x23C4, 0x23C4, 0x2424, 0x2424, 0x23C4,
                            0x1008, 0x0810, 0x07E0, 0x0000 }, // Face 0
                    { 0x0000, 0x0180, 0x03C0, 0x07E0, 0x0DB0, 0x198F, 0x318C, 0x6186, 0x6186, 0x318C, 0x198F, 0x0DB0,
                            0x07E0, 0x03C0, 0x0180, 0x0000 }, // Face 1
                    { 0x0000, 0x3FFF, 0x2001, 0x27E1, 0x2421, 0x2421, 0x2421, 0x27E1, 0x2001, 0x2181, 0x2181, 0x2181,
                            0x2181, 0x2001, 0x3FFF, 0x0000 }, // Face 2
                    { 0x0000, 0x1F00, 0x0E00, 0x0400, 0x0E00, 0x1B00, 0x31F8, 0x600C, 0x600C, 0x31F8, 0x1B00, 0x0E00,
                            0x0400, 0x0E00, 0x1F00, 0x0000 }, // Face 3
                    { 0x0000, 0x0000, 0x1E78, 0x1248, 0x1248, 0x1E78, 0x0000, 0x0000, 0x0000, 0x0000, 0x1E78, 0x1248,
                            0x1248, 0x1E78, 0x0000, 0x0000 }, // Face 4
                    { 0x0000, 0x0180, 0x0180, 0x0180, 0x0180, 0x0FF0, 0x1818, 0x1008, 0x1008, 0x1818, 0x0FF0, 0x0180,
                            0x0180, 0x0180, 0x0180, 0x0000 } // Face 5
            };
            char[] runeGlyphs = {
                    '\u039E', // Xi
                    '\u03A8', // Psi
                    '\u205C', // Dotted Cross
                    '\u29C9', // Joined Squared
                    '\u29D3', // Bowtie
                    '\u23C3' // Triangle w/ Line
            };

            // 3. Layer A: Outer Beveled Edges (Glows and breathes dynamically with the
            // wave)
            if (x <= 1 || x >= 14 || y <= 1 || y >= 14) {
                int r = (int) (100 + (noise * 10) + (timeWave * 45)); // Scales purple energy between 100 and 155
                int g = 25;
                int b = (int) (160 + (noise * 10) + (timeWave * 60)); // Scales blue/purple energy between 160 and 230
                return new VoxelTexel(r, g, b, '%');
            }

            // 4. Layer B: Check Bitmask for Inner Ancient Runes
            long rowBits = kevinRunes[face % 6][y];
            boolean isRuneGlyph = ((rowBits >> (15 - x)) & 1) == 1;

            if (isRuneGlyph) {
                // Glowing Pink/Magenta Runes pulsating in intensity
                int r = (int) (180 + (timeWave * 75)); // Alternates between dark magenta (180) and hot pink (255)
                int g = (int) (30 + (timeWave * 35));
                int b = (int) (140 + (timeWave * 80));

                char dynamicGlyph = runeGlyphs[face % runeGlyphs.length];
                return new VoxelTexel(r, g, b, dynamicGlyph);
            }

            // 5. Layer C: Dark Volcanic Dark Plating (Static obsidian background to
            // contrast the glow)
            int pr = 35 + noise * 5;
            int pg = 10 + noise * 2;
            int pb = 65 + noise * 5;

            // Complete unicode block density toolkit
            char fullShadedBlock = '\u2588'; // (100% color fill)
            char darkShadedBlock = '\u2593'; // (75% color fill)
            char mediumShadedBlock = '\u2592'; // (50% color fill)
            char lightShadedBlock = '\u2591'; // (25% color fill)
                                              // ( ; will be used for 0%)

            // Smooth 5-step stepping mapping based on wave intensity
            char plateChar;
            if (timeWave > 0.85) {
                plateChar = fullShadedBlock; // Blazing solid purple plate core
            } else if (timeWave > 0.65) {
                plateChar = darkShadedBlock; // Heavy dense shading
            } else if (timeWave > 0.4) {
                plateChar = mediumShadedBlock; // Medium cross-hatch shading
            } else if (timeWave > 0.15) {
                plateChar = lightShadedBlock; // Soft glowing speckles
            } else {
                plateChar = ';'; // Dark, dormant obsidian void
            }

            return new VoxelTexel(pr, pg, pb, plateChar);
        } else if (variant == 5) { // ==================== STAR TREK BORG CUBE ====================
            // 2. Face-Disambiguated Structural Grid System
            // Scrambles layout alignments dynamically based on the current face index
            int faceSeed = face * 37 + 13;
            int vertPipe = (faceSeed ^ 0x2A) % 8 + 4; // Distinct structural vertical conduit row
            int horizPipe = (faceSeed ^ 0xC5) % 8 + 4; // Distinct structural horizontal conduit row

            // 3. Layer Bitmask Mapping Rules
            boolean isVerticalConduit = (x == vertPipe);
            boolean isHorizConduit = (y == horizPipe);
            boolean isSecondaryWire = ((x * 5 + y * 11 + face) % 7 == 0);
            boolean isPanelTrench = (x % 5 == 0 || y % 5 == 0);

            // Geometric Green Power Matrices (X-shaped circuitry channels crossing the
            // face)
            boolean isPlasmaChannel = (x == y || x == 15 - y) && (x > 1 && x < 14);

            // --- TEXTURE RASTERIZATION PROCESSING ---

            // Layer A: Heavy Foreground Structural Piping
            // Vertical pipes use vertical bars, horizontal pipes use double equals to give
            // a 3D illusion
            if (isVerticalConduit) {
                int pipeColor = 125 + noise * 10;
                return new VoxelTexel(pipeColor, pipeColor, pipeColor + 5, '\u2551'); // (Double vertical wall pipe)
            }
            if (isHorizConduit) {
                int pipeColor = 115 + noise * 10;
                return new VoxelTexel(pipeColor, pipeColor, pipeColor + 5, '='); // = (Horizontal equals)
            }

            // Layer B: Continuous Glowing Plasma Matrices (Neon Green Channels)
            if (isPlasmaChannel) {
                // Pure unshaded radioactive neon green using safe 16-bit primitives
                return new VoxelTexel(10, 235, 45, '\u2592'); // (Medium shaded mesh)
            }

            // Layer C: Exposed Secondary Cable Bundles and Wiring
            if (isSecondaryWire) {
                int wireColor = 80 + noise * 5;
                return new VoxelTexel(wireColor - 5, wireColor, wireColor, '-'); // Fine mesh wires
            }

            // Layer D: Armor Plate Bevels and Deep Machinery Crevices
            if (isPanelTrench) {
                int trenchColor = 55 + noise * 5;
                return new VoxelTexel(trenchColor, trenchColor, trenchColor + 4, '%'); // Deep hull panel splits
            }

            // Layer E: Base Metal Under-plating
            int hullColor = 35 + noise * 4;
            return new VoxelTexel(hullColor, hullColor, hullColor + 2, '#'); // Dark graphite backing plate
        } else if (variant == 6) { // ==================== TRANSFORMERS ALLSPARK ====================
            // 2. Continuous Cybertronian Etchings (Abstract geometric line networks)
            // Uses structural line paths instead of repetitive grid patterns
            boolean isVerticalGroove = (x == 3 || x == 7 || x == 12);
            boolean isHorizontalGroove = (y == 3 || y == 8 || y == 11);
            boolean isDiagonalChasm = (x == y && x > 2 && x < 13);

            // Combine paths into a continuous sunken rune network
            boolean isRuneChannel = isVerticalGroove || isHorizontalGroove || isDiagonalChasm;

            // 3. Intricate Raised Layer Borders (Framing individual panel carvings)
            boolean isPanelBorder = (x == 1 || x == 14 || y == 1 || y == 14);

            // --- ALLSPARK RASTERIZATION LAYERS ---

            // Layer A: Radiant Cosmic Energy (Deep inside the carved chasm lines)
            if (isRuneChannel) {
                // High-intensity glowing Cybertronian Cyan Blue
                // Uses the code-safe 16-bit translucent pattern char for energy volume
                return new VoxelTexel(15, 205, 255, '\u2592'); // ▒ (Raw energy pulsing inside channels)
            }

            // Layer B: Sunken Trench Shadows (The dark metal borders right next to the
            // glow)
            // Adds incredible 3D shadow depth inside flat terminal cells
            boolean isTrenchShadow = (x == 2 || x == 4 || x == 6 || x == 8 || x == 11 || x == 13 ||
                    y == 2 || y == 4 || y == 7 || y == 9 || y == 10 || y == 12);

            if (isTrenchShadow) {
                int shadowMetal = 95 - noise * 10; // Dark weathered silver/pewter
                return new VoxelTexel(shadowMetal, shadowMetal + 5, shadowMetal + 15, ':');
            }

            // Layer C: Raised Framing Borders (Slightly burnished accent borders)
            if (isPanelBorder) {
                int trimMetal = 185 + noise * 10;
                return new VoxelTexel(trimMetal - 5, trimMetal, trimMetal + 5, '%');
            }

            // Layer D: Outer Raised Platinum Plates (Bright, brushed metal panels)
            int plateMetal = 215 + noise * 8; // Bright platinum/silver core

            // Choose dense characters to give the plates a solid, thick structural presence
            char plateChar = (noise % 2 == 0) ? '\u2588' : '\u2593'; // █ or ▓ (Solid metal mass)
            return new VoxelTexel(plateMetal - 10, plateMetal - 5, plateMetal, plateChar);
        } else if (variant == 7) { // ==================== MARIO BRICKS BLOCK ====================
            // 2. Exact Mortar Grout Seam Locations
            boolean isSeam = (y == 0 || y == 4 || y == 8 || y == 12 || y == 15);

            if (y > 0 && y < 4)
                isSeam |= (x == 0 || x == 8 || x == 15);
            if (y > 4 && y < 8)
                isSeam |= (x == 0 || x == 4 || x == 12 || x == 15);
            if (y > 8 && y < 12)
                isSeam |= (x == 0 || x == 8 || x == 15);
            if (y > 12 && y < 15)
                isSeam |= (x == 0 || x == 4 || x == 12 || x == 15);

            // 3. High-Contrast 3D Highlights (Top and Left inner edges of every individual
            // brick)
            boolean isHighlight = (y == 1 || y == 5 || y == 9 || y == 13) ||
                    (y > 0 && y < 4 && x == 1) ||
                    (y > 4 && y < 8 && (x == 1 || x == 5 || x == 13)) ||
                    (y > 8 && y < 12 && x == 1) ||
                    (y > 12 && y < 15 && (x == 1 || x == 5 || x == 13));

            // 4. 3D Drop Shadows (Bottom and Right inner edges of every individual brick)
            // This adds incredible depth, making the bricks look physically extruded
            boolean isBrickShadow = (y == 3 || y == 7 || y == 11 || y == 14) ||
                    (y > 0 && y < 4 && (x == 7 || x == 14)) ||
                    (y > 4 && y < 8 && (x == 3 || x == 11 || x == 14)) ||
                    (y > 8 && y < 12 && (x == 7 || x == 14)) ||
                    (y > 12 && y < 15 && (x == 3 || x == 11 || x == 14));

            // --- MARIO BRICK RASTERIZATION ENGINE ---

            // Layer A: Mortar Grout Lines (Deep, solid charcoal channels)
            if (isSeam) {
                return new VoxelTexel(45, 30, 25, '\u2591');
            }

            // Layer B: Vivid Pastel Salmon Highlights (Sunlit edges catching light)
            // Overrides shadows to keep the corners looking sharp
            if (isHighlight) {
                return new VoxelTexel(190, 125, 85, '\u2588'); // █ (Solid bright mass)
            }

            // Layer C: Deep Maroon/Terracotta Shadows (Recessed edge blocking)
            if (isBrickShadow) {
                int darkR = 90 + noise * 5;
                int darkG = 35 + noise * 2;
                int darkB = 15; // Heavy deep dark brick-red
                return new VoxelTexel(darkR, darkG, darkB, '\u2592'); // ▒ (Densely patterned shadow mesh)
            }

            // Layer D: Main Brick Clay Body (Vibrant Mario Orange-Red)
            int bodyR = 145 + noise * 8;
            int bodyG = 65 + noise * 4;
            int bodyB = 25;
            return new VoxelTexel(bodyR, bodyG, bodyB, '\u2593'); // ▓ (Thick clay textured brick face)

        } else if (variant == 8) { // ==================== PSYCHEDELIC CUBE ====================
            // 1. Establish Time Vectors
            double t = System.currentTimeMillis() * 0.003; // Speed of the fluid morphing

            // 2. Map coordinates (-1.0 to 1.0) so math functions ripple outwards from
            // center
            double cx = (x - 7.5) / 7.5;
            double cy = (y - 7.5) / 7.5;

            // 3. Layered Plasma Mathematics (Interlocking Sine Wave Fields)
            // Wave 1: Concentric ripples expanding from the center
            double wave1 = Math.sin(Math.sqrt(cx * cx + cy * cy) * 5.0 - t);

            // Wave 2: Distorted horizontal/vertical interference tracking the Face Index
            double wave2 = Math.sin(cx * 4.0 + t + face) + Math.cos(cy * 3.0 - t * 0.5);

            // Wave 3: A twisting diagonal vortex calculation
            double wave3 = Math.sin((cx + cy) * 3.0 + t * 1.5);

            // Combine waves and normalize value to run safely between -1.0 and 1.0
            double combined = (wave1 + wave2 + wave3) / 3.0;

            // 4. Psychedelic Color Phase Wheel Mapping (Converts float value into shifting
            // RGB spectrums)
            // Adding offsets to the phase wheels ensures Red, Green, and Blue peaks split
            // beautifully
            int r = (int) ((Math.sin(combined * Math.PI + t) + 1.0) * 127.5);
            int g = (int) ((Math.sin(combined * Math.PI + t + (2.0 * Math.PI / 3.0)) + 1.0) * 127.5);
            int b = (int) ((Math.sin(combined * Math.PI + t + (4.0 * Math.PI / 3.0)) + 1.0) * 127.5);

            // 5. Dynamic Density Morphing
            // The underlying terminal characters continuously shift and breathe alongside
            // the colors
            char fluidChar;
            double density = Math.abs(combined);
            if (density > 0.75) {
                fluidChar = '\u2588'; // █ (Solid peak plasma)
            } else if (density > 0.50) {
                fluidChar = '\u2593'; // ▓
            } else if (density > 0.25) {
                fluidChar = '\u2592'; // ▒
            } else {
                fluidChar = '\u2591'; // ░ (Low energy valleys)
            }

            return new VoxelTexel(r, g, b, fluidChar);

        } else if (variant == 9) { // ==================== 6 FACES CUBE ====================
            // 1. Smooth Radial Sphere Shading (Distance from Center calculation)
            // Center point of the 16x16 face is 7.5
            double dx = x - 7.5;
            double dy = y - 7.5;
            // Normalized distance from center (0.0 at center, roughly 1.0 at corners)
            double distFromCenter = Math.min(1.0, Math.sqrt(dx * dx + dy * dy) / 10.0);

            // 2. Default Feature Triggers
            boolean isEye = false, isMouth = false, isEyebrow = false, isAccent = false;
            char featureChar = ' ';
            int accentR = 0, accentG = 0, accentB = 0;

            // 3. Coordinate Matrices for the Face Layouts
            switch (face % 6) {
                case 0: // 😊 Smiling Blush
                    isEye = (y == 4 && (x == 4 || x == 5 || x == 10 || x == 11))
                            || (y == 5 && (x == 3 || x == 6 || x == 9 || x == 12));
                    featureChar = isEye ? '\u25DC' : ' '; // ⌒
                    isMouth = (y == 9 && x >= 4 && x <= 11) || (y == 10 && x >= 5 && x <= 10)
                            || (y == 11 && x >= 6 && x <= 9);
                    featureChar = isMouth ? '\u2584' : featureChar; // ▄
                    isAccent = (y == 7 || y == 8) && (x == 2 || x == 3 || x == 12 || x == 13);
                    accentR = 255;
                    accentG = 110;
                    accentB = 140;
                    break;

                case 1: // 😜 Wink & Tongue Out
                    boolean leftWink = (y == 5 && x >= 3 && x <= 6);
                    boolean rightOpen = (y >= 4 && y <= 6 && x >= 10 && x <= 11);
                    isEye = leftWink || rightOpen;
                    featureChar = leftWink ? '-' : '\u2588';
                    boolean smileLine = (y == 8 && x >= 4 && x <= 11) || (y == 9 && (x == 3 || x == 12));
                    boolean hangingTongue = (y >= 9 && y <= 11 && x >= 7 && x <= 9);
                    if (smileLine) {
                        isMouth = true;
                        featureChar = '\u2592';
                    } else if (hangingTongue) {
                        isAccent = true;
                        accentR = 240;
                        accentG = 40;
                        accentB = 70;
                    }
                    break;

                case 2: // 😠 Angry
                    isEyebrow = (y == 3 && (x == 3 || x == 12)) || (y == 4 && (x == 4 || x == 11))
                            || (y == 5 && (x == 5 || x == 10));
                    featureChar = '\u2584';
                    isEye = (y == 6 && (x == 4 || x == 5 || x == 10 || x == 11));
                    featureChar = isEye ? '\u25A0' : featureChar;
                    isMouth = (y == 11 && x >= 5 && x <= 10) || (y == 10 && (x == 4 || x == 11));
                    featureChar = isMouth ? '=' : featureChar;
                    break;

                case 3: // 😮 Shocked
                    isEyebrow = (y == 3 && x >= 4 && x <= 6) || (y == 3 && x >= 9 && x <= 11);
                    featureChar = '\u203E';
                    isEye = (y >= 5 && y <= 6) && (x == 4 || x == 5 || x == 10 || x == 11);
                    featureChar = isEye ? '\u2588' : featureChar;
                    isMouth = (y >= 9 && y <= 12 && x >= 6 && x <= 9) && !(y == 9 && (x == 6 || x == 9))
                            && !(y == 12 && (x == 6 || x == 9));
                    featureChar = isMouth ? '\u2588' : featureChar;
                    break;

                case 4: // 😎 Sunglasses Cool
                    boolean isGlassesLens = (y >= 4 && y <= 6) && ((x >= 2 && x <= 6) || (x >= 9 && x <= 13));
                    boolean isGlassesBridge = (y == 4 && x >= 6 && x <= 9);
                    if (isGlassesLens || isGlassesBridge) {
                        isAccent = true;
                        accentR = 30;
                        accentG = 30;
                        accentB = 35;
                    }
                    isMouth = (y == 10 && x >= 8 && x <= 12) || (y == 9 && x == 12);
                    featureChar = isMouth ? '\u25AC' : featureChar;
                    break;

                case 5: // 😢 Crying
                    isEyebrow = (y == 4 && (x == 5 || x == 10)) || (y == 5 && (x == 4 || x == 11));
                    featureChar = '\u25CB';
                    isEye = (y == 6 && (x == 4 || x == 5 || x == 10 || x == 11));
                    featureChar = isEye ? '\u25A0' : featureChar;
                    isMouth = (y == 11 && x >= 6 && x <= 9);
                    featureChar = isMouth ? '\u25AC' : featureChar;
                    isAccent = (x == 4 && y >= 7 && y <= 12);
                    accentR = 30;
                    accentG = 165;
                    accentB = 255;
                    break;
            }

            // --- TEXTURE COMPOSITING ENGINE ---

            // Layer A: Facial Line Features (Deep ink brown)
            if (isEye || isMouth || isEyebrow) {
                return new VoxelTexel(45, 30, 20, featureChar);
            }

            // Layer B: Special Colors (Blush, Tongues, Sunglasses, Tears)
            if (isAccent) {
                char accentChar = (face % 6 == 4) ? '\u2588' : '\u2592';
                return new VoxelTexel(accentR, accentG, accentB, accentChar);
            }

            // Layer C: Smooth Spherical Gradient Interpolation
            // Instead of a hard boundary, the color and characters scale smoothly from the
            // center outwards
            double brightnessFactor = 1.0 - (distFromCenter * 0.45); // Fade up to 45% darker at corners

            int r = (int) (255 * brightnessFactor);
            int g = (int) ((205 + noise * 6) * brightnessFactor);
            int b = (int) (35 * brightnessFactor);

            // Dynamic density scaling based on center distance mapping
            char skinChar;
            if (distFromCenter < 0.4) {
                skinChar = '\u2588'; // █ (Solid core)
            } else if (distFromCenter < 0.7) {
                skinChar = '\u2593'; // ▓ (Densely speckled mid-tone)
            } else if (distFromCenter < 0.9) {
                skinChar = '\u2592'; // ▒ (Medium mesh wrap)
            } else {
                skinChar = '\u2591'; // ░ (Light feathering on the sharpest corner vertices)
            }

            return new VoxelTexel(r, g, b, skinChar);
        } else if (variant == 10) {
            // ===== HELLRAISER LAMENT CONFIGURATION PUZZLE BOX =====
            // 1. Surface weathering noise for the antique brass and grain wood
            noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 4);

            // 2. Exact Layout Math for the Iconic Puzzle Box Framing
            // Center point is 7.5
            double dx = x - 7.5;
            double dy = y - 7.5;
            double radius = Math.sqrt(dx * dx + dy * dy);

            // Boolean mask triggers for the structural brass etching layers
            boolean isOuterBrassFrame = (x == 0 || x == 15 || y == 0 || y == 15);
            boolean isInnerBrassSquare = (x == 3 || x == 12 || y == 3 || y == 12)
                    && (x >= 3 && x <= 12 && y >= 3 && y <= 12);

            // Circular brass mechanisms (The clockwork dials)
            boolean isCentralDialRing = (radius >= 3.4 && radius <= 4.6);
            boolean isCorePin = (radius >= 0.0 && radius <= 1.2);

            // Star-burst clockwork tracks radiating from the center ring to the inner
            // square
            boolean isClockworkSpoke = (x == y || x == 15 - y || x == 7 || x == 8 || y == 7 || y == 8)
                    && (radius >= 2.5 && radius <= 6.0);

            // Combine everything into a unified brass mechanism overlay
            boolean isBrassMechanism = isOuterBrassFrame || isInnerBrassSquare || isCentralDialRing || isCorePin
                    || isClockworkSpoke;

            // 3. Drop Shadow Masking (Recessed gaps between wood panels and brass sheets)
            // Placed right along the borders to create deep 3D optical tracking
            boolean isTrenchShadow = (x == 1 || x == 14 || y == 1 || y == 14) ||
                    (radius >= 4.7 && radius <= 5.5 && !isClockworkSpoke);

            // --- TEXTURE RASTERIZATION PROCESSING ---

            // Layer A: Ornate Gleaming Antique Brass Overlays
            if (isBrassMechanism && !isTrenchShadow) {
                // High-contrast, rich metallic gold/brass color space
                int r = 215 + noise * 10;
                int g = 165 + noise * 5;
                int b = 40;

                // Use dense symbols to depict highly complex, intricate clockwork etching
                char brassChar = (isCorePin) ? '\u2588' : ((x + y) % 2 == 0 ? '\u25CE' : '\u25C9'); // █, ◎, or ◉
                return new VoxelTexel(r, g, b, brassChar);
            }

            // Layer B: Deep Inset Trench Shadows
            if (isTrenchShadow) {
                int shadowR = 25 + noise * 2;
                int shadowG = 15;
                int shadowB = 10; // Dark charcoal void split
                return new VoxelTexel(shadowR, shadowG, shadowB, ';');
            }

            // Layer C: Deep Polished Mahogany / Rosewood Wood Panels
            // Fills out the remainder of the backing plates with a rich, dark dark-red wood
            // finish
            int woodR = 65 + noise * 6;
            int woodG = 25 + noise * 2;
            int woodB = 15;

            // Choose heavy dense shading blocks to represent solid organic lumber grains
            char woodChar = (noise % 2 == 0) ? '\u2593' : '\u2592'; // ▓ or ▒
            return new VoxelTexel(woodR, woodG, woodB, woodChar);

        } else if (variant == 11) { // ==================== FLAT EARTH ====================
            // 2. Polar Ice Cap Layers (Top and Bottom poles)
            double px = x - 7.5;
            double py = y - 7.5;
            double polarRadiusSq = px * px + py * py;

            char blockChar = '\u2588'; // █ (Solid pixel fill)

            if (face == 3 && polarRadiusSq < 22.0) { // Top Face: Arctic Pole
                int ice = 220 + noise * 10;
                // Boost blue slightly (ice, ice, 255) to guarantee a frozen white/cyan look,
                // preventing yellow drift
                return new VoxelTexel(ice, ice, 255, blockChar);
            }
            if (face == 2 && polarRadiusSq < 18.0) { // Bottom Face: Antarctic Pole
                int ice = 220 + noise * 10;
                return new VoxelTexel(ice, ice, 255, blockChar);
            }

            // 3. Normalize incoming 0-15 grid coordinates to a clean 0.0 - 1.0 range
            double tu = x / 15.0;
            double tv = y / 15.0;

            // Flip the face wrapping rotations safely
            double u = 0, v = 0;
            switch (face) {
                case 0:
                    u = tu;
                    v = 1.0 - tv;
                    break; // Back
                case 1:
                    u = 1.0 - tu;
                    v = 1.0 - tv;
                    break; // Front
                case 4:
                    u = 1.0 - tv;
                    v = 1.0 - tu;
                    break; // Left
                case 5:
                    u = 1.0 - tv;
                    v = tu;
                    break; // Right
                case 2:
                case 3:
                    u = tu;
                    v = tv;
                    break; // Bottom & Top
            }

            // 4. Map to true un-rotated 3D cube coordinates (-1.0 to 1.0)
            double uc = 2.0 * u - 1.0;
            double vc = 2.0 * v - 1.0;
            double cx = 0, cy = 0, cz = 0;
            switch (face) {
                case 0:
                    cx = uc;
                    cy = vc;
                    cz = -1.0;
                    break;
                case 1:
                    cx = uc;
                    cy = vc;
                    cz = 1.0;
                    break;
                case 2:
                    cx = uc;
                    cy = -1.0;
                    cz = vc;
                    break;
                case 3:
                    cx = uc;
                    cy = 1.0;
                    cz = vc;
                    break;
                case 4:
                    cx = -1.0;
                    cy = uc;
                    cz = vc;
                    break;
                case 5:
                    cx = 1.0;
                    cy = uc;
                    cz = vc;
                    break;
            }

            // 5. Translate 3D space back into a stable 0-15 wave coordinate system
            double wx = (cx + 1.0) * 7.5;
            double wy = (cy + 1.0) * 7.5;
            double wz = (cz + 1.0) * 7.5;

            // 6. 3D Cyclic Wave Math for continuous, wrapping continents
            double wave1 = Math.sin(wx * 0.45) * Math.cos(wy * 0.45);
            double wave2 = Math.sin(wz * 0.45) * Math.cos(wx * 0.45);
            double wave3 = Math.sin(wy * 0.45) * Math.cos(wz * 0.45);

            double landForm = (wave1 + wave2 + wave3) * 0.65;

            // --- EARTH TEXTURE COMPOSITING PIPELINE ---

            // --- EARTH TEXTURE COMPOSITING PIPELINE ---

            if (landForm > 0.05) {
                // Layer A: Shoreline Sand (Clean light beige)
                if (landForm < 0.12) {
                    int sandR = 225 + noise * 5;
                    int sandG = 210 + noise * 3;
                    int sandB = 160;
                    return new VoxelTexel(sandR, sandG, sandB, blockChar);
                }

                // Layer B: Higher Altitude Peaks (Rich, deep forest green)
                if (landForm > 0.32) {
                    int highLandG = 120 + noise * 10; // Darker emerald for topographic depth
                    return new VoxelTexel(10, highLandG, 30, blockChar);
                }

                // Layer C: Standard Lowland Grass Fields (Crisp, vibrant green)
                int landG = 165 + noise * 15;
                return new VoxelTexel(20, landG, 40, blockChar);

            } else {
                // Layer D: Shallow Coastal Waters (Right next to beaches)
                if (landForm > -0.15) {
                    int coastB = 210 + noise * 12;
                    return new VoxelTexel(10, 140 + noise * 8, coastB, '\u2593'); // ▓ (Bright teal shelf)
                }

                // Layer E: Deep Abyssal Ocean Plunge
                int oceanB = 170 + noise * 10;
                return new VoxelTexel(10, 60 + noise * 5, oceanB, '\u2592'); // ▒ (Dark royal blue ocean)
            }
        } else if (variant == 12) { // ==================== MARIO "?" COIN BLOCK ====================
            // Baseline Yellow-Gold Theme
            int baseR = 230;
            int baseG = 160;
            int baseB = 15; // Solid Yellow-Gold
            int highR = 255;
            int highG = 225;
            int highB = 130; // Cream Highlight
            int shadowR = 135;
            int shadowG = 80;
            int shadowB = 5; // Deep Amber Shadow

            // 2. Exact Layout Math for the Iconic Symbol & Corner Rivets
            boolean isQuestionMark = false;
            boolean isMouthDot = false;
            boolean isCornerScrew = false;

            // Draw the 4 Corner Rivet/Screw holes
            if ((x == 2 || x == 13) && (y == 2 || y == 13)) {
                isCornerScrew = true;
            }

            // Mirror X axis only on face 3 (Top face) to correct the backward "?"
            int evalX = (face == 3) ? (15 - x) : x;

            // High-Res Question Mark (?) Matrix Core
            if (evalX >= 4 && evalX <= 11 && y >= 3 && y <= 13) {
                // Question Mark Upper Hook Loop
                if (y == 3)
                    isQuestionMark = (evalX >= 5 && evalX <= 10);
                if (y == 4)
                    isQuestionMark = (evalX >= 4 && evalX <= 5) || (evalX >= 10 && evalX <= 11);
                if (y == 5)
                    isQuestionMark = (evalX >= 4 && evalX <= 5) || (evalX >= 10 && evalX <= 11);
                if (y == 6)
                    isQuestionMark = (evalX >= 9 && evalX <= 11);

                // Question Mark Inward Stem Slide
                if (y == 7)
                    isQuestionMark = (evalX >= 7 && evalX <= 9);
                if (y == 8)
                    isQuestionMark = (evalX >= 7 && evalX <= 8);
                if (y == 9)
                    isQuestionMark = (evalX >= 7 && evalX <= 8);

                // Question Mark Isolated Bottom Period Dot
                if (y == 11 || y == 12) {
                    isMouthDot = (evalX >= 7 && evalX <= 8);
                }
            }

            // 3. Extruded Outer Box Shadow Bevel Lines
            boolean isOuterFrameHigh = (x == 1 || y == 1) && (x < 15 && y < 15);
            boolean isOuterFrameDark = (x == 14 || y == 14) && (x > 0 && y > 0);

            // --- QUESTION BLOCK RASTERIZATION INTERPOLATION ---

            // Layer A: The Question Mark Glyphs (Clean, Pure Ceramic White)
            if (isQuestionMark || isMouthDot) {
                int w = 245 + noise * 4;
                return new VoxelTexel(w, w, w, '\u2588'); // █
            }

            // Layer B: Corner Screw Insets (Deep Drop Shadows)
            if (isCornerScrew) {
                return new VoxelTexel(30, 20, 10, '#');
            }

            // Layer C: 3D Sunlit Top/Left Box Highlights
            if (isOuterFrameHigh && !isOuterFrameDark) {
                return new VoxelTexel(highR, highG, highB, '\u2588'); // █
            }

            // Layer D: 3D Recessed Bottom/Right Box Shadows
            if (isOuterFrameDark) {
                return new VoxelTexel(shadowR - noise * 5, shadowG - noise * 4, shadowB, '\u2592'); // ▒
            }

            // Layer E: Main Casing Body Face Plates
            return new VoxelTexel(baseR + noise * 8, baseG + noise * 6, baseB, '\u2593'); // ▓

        } else if (variant == 13) { // ==================== GLITCH STATIC ====================
            // 2. Define Layout Boundaries
            boolean isOuterCasing = (x == 0 || x == 15 || y == 0 || y == 15);
            boolean isInnerBevel = (x == 1 || x == 14 || y == 1 || y == 14);
            boolean isCornerBracket = (x <= 3 || x >= 12) && (y <= 3 || y >= 12);
            boolean isCircuitTrace = (x == 4 || x == 11 || y == 4 || y == 11);

            // --- CYBER GLITCH COMPOSITING ENGINE ---

            // Layer A: The Glitchy Outer Framing & Corner Brackets
            if (isOuterCasing || isInnerBevel || isCornerBracket) {
                // High-speed edge corruption engine (5% chance per pixel to glitch out)
                if (Math.random() < 0.05) {
                    boolean criticalError = Math.random() < 0.5;

                    int r = criticalError ? 255 : 240;
                    int g = criticalError ? 15 : 240;
                    int b = criticalError ? 50 : 240; // Alternates between Warning Crimson and Bleaching White

                    char errorChar = criticalError ? '\u25A0' : '\u2591'; // ■ (Error Block) or ░ (Data Stream)
                    return new VoxelTexel(r, g, b, errorChar);
                }

                // Dark Gunmetal Base (When the frame is holding its integrity)
                int metal = 40 + noise * 4;
                char casingChar = isOuterCasing ? '#' : '%';
                return new VoxelTexel(metal, metal + 3, metal + 5, casingChar);
            }

            // Layer B: Recessed Shadow Trench
            if (x == 2 || x == 13 || y == 2 || y == 13) {
                // The deep gap can occasionally leak bright green code spikes
                if (Math.random() < 0.02) {
                    return new VoxelTexel(10, 255, 80, '!'); // Blazing warning ticker
                }
                return new VoxelTexel(12, 10, 15, ';'); // Normal dark void gap
            }

            // Layer C: Internal High-Voltage Circuit Traces
            if (isCircuitTrace) {
                // Continuous electric green/cyan flickering logic
                int r = (int) (Math.random() * 30);
                int g = 190 + (int) (Math.random() * 65);
                int b = 210 + (int) (Math.random() * 45);
                return new VoxelTexel(r, g, b, '=');
            }

            // Layer D: The Pure High-Voltage TV Static Screen Core
            int staticR = (int) (Math.random() * 256);
            int staticG = (int) (Math.random() * 256);
            int staticB = (int) (Math.random() * 256);

            char staticChar;
            double densityCheck = Math.random();
            if (densityCheck > 0.66) {
                staticChar = '\u2588'; // █
            } else if (densityCheck > 0.33) {
                staticChar = '\u2592'; // ▒
            } else {
                staticChar = '\u2591'; // ░
            }

            return new VoxelTexel(staticR, staticG, staticB, staticChar);
        } else if (variant == 14) { // ==================== GREYSCALE RUBIK'S CUBE ====================
            // 1. Symmetrical Grid Borders (Locked at perfect 5-cell intervals)
            boolean isGridBorder = (x == 0 || x == 5 || x == 10 || x == 15 ||
                    y == 0 || y == 5 || y == 10 || y == 15);

            if (isGridBorder) {
                return new VoxelTexel(25, 25, 30, '\u2588'); // █ Sharp dark charcoal grid lines
            }

            // 2. Identify the 3x3 grid tile coordinates (0, 1, or 2)
            int tileX = (x < 5) ? 0 : (x < 10) ? 1 : 2;
            int tileY = (y < 5) ? 0 : (y < 10) ? 1 : 2;

            // 3. Normalized tile coordinates centered around index 1 (-1.0 to 1.0)
            double cx = tileX - 1.0;
            double cy = tileY - 1.0;

            // 4. Generate Highly Asymmetric, Irrational Spatial Phase Shifts
            // Using prime products and square roots ensures each tile on every face
            // receives a unique wave offset
            double uniqueTileID = (tileX * 7.13) + (tileY * 13.37) + (face * 19.99);
            double localPhaseOffset = Math.sin(uniqueTileID * 0.6180339887) * 4.44;

            // 5. Establish Time Vectors with Desynchronized Frequencies per Tile
            // Multiplying the clock speed by an irrational multiplier stops the tiles from
            // sharing a timeline
            double baseTime = System.currentTimeMillis() * 0.003;
            double localTime = baseTime + localPhaseOffset;
            double speedModulator = 0.85 + (Math.abs(Math.sin(uniqueTileID)) * 0.3); // Slight speed variance (0.85 to
                                                                                     // 1.15)
            double t = localTime * speedModulator;

            // 6. Layered Sines (Now running on fully desynchronized time streams)
            double wave1 = Math.sin(cx * 3.5 + t) * Math.cos(cy * 3.5 - t * 0.85);
            double wave2 = Math.sin(Math.sqrt(cx * cx + cy * cy) * 4.0 - t * 1.3);

            // Combine waves and normalize to a clean 0.0 -> 1.0 range
            double combined = (wave1 + wave2) / 2.0;
            double colorPercent = (combined + 1.0) / 2.0;

            // 7. Flip the spectrum for alternating tiles to preserve the Checkerboard
            // Pattern
            boolean isAlternateTile = (tileX + tileY) % 2 == 1;
            if (isAlternateTile) {
                colorPercent = 1.0 - colorPercent;
            }

            // 8. Interpolate Monochrome Grayscale Values
            int grayValue = (int) (35 + (colorPercent * 210)); // Ranges smoothly from dark slate to bright white

            // 9. Dynamic Character Morphing matching the color shift
            char morphChar;
            if (colorPercent > 0.8) {
                morphChar = '\u2588'; // █ (Solid Peaks)
            } else if (colorPercent > 0.5) {
                morphChar = '\u2593'; // ▓
            } else if (colorPercent > 0.25) {
                morphChar = '\u2592'; // ▒
            } else {
                morphChar = '\u2591'; // ░ (Fine Grain Valleys)
            }

            return new VoxelTexel(grayValue, grayValue, grayValue, morphChar);
        } else if (variant == 15) { // ==================== ??? ====================
            // 1. Establish global clock for high-speed cosmic energy waves
            double t = System.currentTimeMillis() * 0.005;

            // 3. Crisp Neon Outer Framing (Only tracking the actual structural edge
            // boundaries of the faces)
            boolean isOuterFrameLine = (x == 0 || x == 15 || y == 0 || y == 15);

            // 4. Stylized Radial Distance Math for Energy Ripples
            double dx = x - 7.5;
            double dy = y - 7.5;
            double radius = Math.sqrt(dx * dx + dy * dy);

            // Sharp concentric ring math: creates defined energy waves pulsing outward from
            // the core
            double rippleWave = Math.sin(radius * 1.8 - t * 2.0);

            // Normalize to a clean 0.0 -> 1.0 spectrum
            double finalEnergyField = (rippleWave + 1.0) / 2.0;

            // --- COSMIC ARTISTIC RASTERIZATION PIPELINE ---
            char blockChar = '\u2588'; // █ (Solid pixel fill)

            // Layer A: Pure Blazing White Infinity Stone Core (Stays sharp and locked at
            // the center)
            if (radius < 2.5) {
                int w = 240 + noise * 5;
                return new VoxelTexel(w, w, 255, blockChar); // █ Blazing hyper-white core
            }

            // Layer B: Crisp Electric Cyan Outer Casing Frames
            if (isOuterFrameLine) {
                // High-saturation electric cyan lines that lock the 3D cube geometry edges
                return new VoxelTexel(0, 235 + noise * 10, 255, blockChar);
            }

            // Layer C: Stylized Concentric Shading Shockwaves
            if (finalEnergyField > 0.78) {
                // High Energy Crest: Intense Bright Cyan
                return new VoxelTexel(30, 210, 255, blockChar); // █
            } else if (finalEnergyField > 0.52) {
                // Medium Energy Crest: Sky Blue
                return new VoxelTexel(20, 140, 240, '\u2593'); // ▓ Densely speckled mesh
            } else if (finalEnergyField > 0.28) {
                // Lower Energy Valleys: Deep Sapphire Blue
                return new VoxelTexel(10, 70, 190, '\u2592'); // ▒ Medium cross-hatch mesh
            } else {
                // Inactive Void Pockets: Deep Electric Charcoal
                int voidB = 40 + noise * 5;
                return new VoxelTexel(15, 20, voidB, '\u2591'); // ░ Fine grain backing
            }
        } else if (variant == 16) { // ======= 3 PHASE CUBE ======
            // 1. Establish precise time tracks
    double time = System.currentTimeMillis() * 0.003;
    long timeCycle = (System.currentTimeMillis() / 2000) % 3; // Swaps algorithm states every 2 seconds

    // 3. Rigid Structural Casing (Locks the 3D voxel box silhouette)
    boolean isFrame = (x == 0 || x == 15 || y == 0 || y == 15 || 
                       (x == 1 && y == 1) || (x == 14 && y == 14) || 
                       (x == 1 && y == 14) || (x == 14 && y == 1));

    if (isFrame) {
        int metal = 45 + noise * 5;
        return new VoxelTexel(metal, metal + 2, metal + 4, '#'); // Clean, stable gunmetal frame
    }

    // 4. Central Computational Core (The Algorithmic Engine)
    // Map center coordinates from -7 to 7
    int cx = x - 8;
    int cy = y - 8;

    int r = 0, g = 0, b = 0;
    char matrixChar = '\u2588'; // █ Default solid fill

    // 5. Triple-State Algorithmic State Machine
    if (timeCycle == 0) {
        // --- MODE 0: BITWISE FRACTAL LEAKAGE ---
        // Uses exclusive OR math mixed with time to sprout organic computer tree patterns
        int fractalValue = ((x * 4) ^ (y * 4) ^ ((int)(time * 15))) & 0xFF;
        
        r = fractalValue;
        g = (fractalValue * 3) % 256;
        b = 255 - fractalValue; // Electric Purple and Blue shifting nodes
        
        matrixChar = fractalValue > 128 ? '\u2593' : '\u2592'; // ▓ or ▒

    } else if (timeCycle == 1) {
        // --- MODE 1: INTERFERENCE WAVE LOOPS (THE MOIRÉ EFFECT) ---
        // Using nested modulos and trigonometric distances creates clean geometric shockwaves
        double dist = Math.sqrt(cx * cx + cy * cy);
        int ringCheck = (int) (dist * 3.5 - time * 6.0) % 6;
        
        if (ringCheck == 0 || ringCheck == 1) {
            r = 10; g = 255; b = 150; // Neon Emerald Laser Ring Lines
            matrixChar = '\u2588'; // █
        } else {
            r = 20; g = 50; b = 40;   // Dark Background Matrix Valleys
            matrixChar = '\u2591'; // ░
        }

    } else {
        // --- MODE 2: HIGH-FREQUENCY TRIG CORRUPTION (CHIPSAT TELEMETRY) ---
        // Forcing Math.cos to evaluate astronomical scales creates structured digital noise
        // This generates a highly stylized "smart static" that scans vertically
        double chaoticTrig = Math.cos((x * 2345.67) + (y * 8765.43) + time * 12.0);
        
        if (chaoticTrig > 0.4) {
            // Intense flashing cyber pink/magenta
            r = 255; g = 20; b = 145;
            matrixChar = '\u2588'; // █
        } else if (chaoticTrig < -0.4) {
            // Dark electric sapphire
            r = 15; g = 40; b = 180;
            matrixChar = '\u2592'; // ▒
        } else {
            // Randomly distributed pixel static drops
            r = (int) (Math.random() * 60);
            g = (int) (Math.random() * 256); // Heavy tracking green component
            b = (int) (Math.random() * 60);
            matrixChar = '\u2591'; // ░
        }
    }
    return new VoxelTexel(r, g, b, matrixChar);
        } else { // Default (This shouldn't happen...)
            return new VoxelTexel(150, 150, 150, '?');
        }
    }

    private static class VoxelTexel {
        final int r, g, b;
        final char character;

        VoxelTexel(int r, int g, int b, char character) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.character = character;
        }
    }
}
