import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class InternetDinoLoader extends InteractiveLoader {
    private static final StatusStage[] DINO_STAGES = {
        new StatusStage(100, "[Jump to avoid obstacles!]")
    };

    private static final int WIDTH = 110;
    private static final int HEIGHT = 30;
    private static final int GROUND_Y = HEIGHT - 4;

    // ------------------------------------------------------------------
    // Physics / game state
    // ------------------------------------------------------------------
    private volatile int gameState = 0; // 0 = running, 1 = crashed
    private int inputCooldown = 0;
    private int restartTimer = 0;

    private volatile double dinoY = GROUND_Y;
    private volatile double dinoVelocity = 0.0;
    private static final double GRAVITY = 0.02;
    private static final double JUMP_IMPULSE = -0.65;
    private static final double SCROLL_SPEED = 0.45;

    private int score = 0;
    private int highScore = 0;

    // ------------------------------------------------------------------
    // Obstacles
    // ------------------------------------------------------------------
    private static class Cactus {
        double posX;
        int widthSize;   // number of trunk segments (1 = single, 2 = cluster)
        int height;       // trunk height in rows (excludes the cap pixel)
        boolean scoreClaimed = false;
        boolean armLeft = false;
        boolean armRight = false;
    }

    private final List<Cactus> cacti = new ArrayList<>();
    private int obstacleSpawnTimer = 0;

    // ------------------------------------------------------------------
    // Background: parallax clouds
    // ------------------------------------------------------------------
    private static class Cloud {
        double posX;
        int rowY;
        double speed;
        int variant;
    }

    private final List<Cloud> clouds = new ArrayList<>();

    // ------------------------------------------------------------------
    // Background: stars (night only)
    // ------------------------------------------------------------------
    private static class Star {
        int x, y;
        double appearAt; // nightTransition threshold at which this star fades in
        int phase;        // twinkle phase offset
    }

    private final List<Star> stars = new ArrayList<>();

    // ------------------------------------------------------------------
    // Day / night cycle -- driven by score, like the real Chrome Dino,
    // which flips into "night mode" after enough points. We use a shorter
    // interval (150 vs. the real game's ~700) since this runs as a loader
    // minigame rather than a marathon session; tune NIGHT_SCORE_INTERVAL
    // to taste.
    // ------------------------------------------------------------------
    private long frameCounter = 0;
    private double nightTransition = 0.0; // 0 = full day, 1 = full night (smoothly animated)
    private double groundScrollX = 0.0;

    private static final int NIGHT_SCORE_INTERVAL = 150;
    private static final int TRANSITION_FRAMES = 55; // frames for the crossfade to complete

    private static final int[] DAY_BG = {247, 247, 247};
    private static final int[] NIGHT_BG = {17, 17, 26};
    private static final int[] DAY_FG = {83, 83, 83};
    private static final int[] NIGHT_FG = {230, 230, 238};
    private static final int[] CLOUD_DAY = {200, 200, 205};
    private static final int[] CLOUD_NIGHT = {68, 68, 84};

    private static final String COLOR_DINO_HIT = "\u001B[38;2;219;68;68m";
    private static final String COLOR_MOON = "\u001B[38;2;228;222;180m";
    private static final String COLOR_STAR = "\u001B[38;2;214;214;164m";

    private final Random random = new Random();

    // ------------------------------------------------------------------
    // Sprites
    // ------------------------------------------------------------------
    private static final int DINO_W = 14;
    private static final int DINO_H = 9;
    private static final int DINO_SCREEN_X = 20;
    private static final String[] DINO_RUN_A = {
        "    ▄█████▄    ",
        "   ███▒██▒██   ",
        "   █████████   ",
        "   ███████     ",
        " ▄ █████████   ",
        " ▀█████████▀   ",
        "   ██▀  ██     ",
        "   █▀   █▄     ",
        "               "
    };
    
    private static final String[] DINO_RUN_B = {
        "    ▄█████▄    ",
        "   ███▒██▒██   ",
        "   █████████   ",
        "   ███████     ",
        " ▄ █████████   ",
        " ▀█████████▀   ",
        "   ██   ██▀    ",
        "   ▄█   ▀█     ",
        "               "
    };
    
    private static final String[] DINO_JUMP = {
        "    ▄█████▄    ",
        "   ███▒██▒██   ",
        "   █████████   ",
        "   ███████     ",
        " ▄ █████████   ",
        " ▀█████████▀   ",
        "   ██   ██     ",
        "   ▀▀   ▀▀     ",
        "               "
    };
    
    private static final String[] DINO_DEAD = {
        "    ▄█████▄    ",
        "   ███X██X██   ",
        "   █████████   ",
        "   ███████     ",
        " ▄ █████████   ",
        " ▀█████████▀   ",
        "   ▀▀   ▀▀     ",
        "               ",
        "               "
    };
    

    private static final String[] CLOUD_SPRITE_A = {" ░▓▓▓░ ", "▓▓▓▓▓▓▓"};
    private static final String[] CLOUD_SPRITE_B = {"  ░▓░  ", " ▓▓▓▓▓ "};
    private static final String[] MOON_SPRITE = {" ▗▄▖", "▐███", " ▝▀▘"};

    public InternetDinoLoader() {
        super(DINO_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void onInitialize() {
        resetGame();
        this.highScore = 0;
        initBackground();
    }

    private void initBackground() {
        clouds.clear();
        int[] startRows = {3, 6, 9};
        for (int i = 0; i < startRows.length; i++) {
            Cloud c = new Cloud();
            c.posX = random.nextInt(WIDTH);
            c.rowY = startRows[i];
            c.speed = 0.04 + random.nextDouble() * 0.05;
            c.variant = random.nextInt(2);
            clouds.add(c);
        }

        stars.clear();
        for (int i = 0; i < 22; i++) {
            Star s = new Star();
            s.x = random.nextInt(WIDTH);
            s.y = 2 + random.nextInt(Math.max(1, GROUND_Y - 8));
            s.appearAt = 0.25 + random.nextDouble() * 0.4;
            s.phase = random.nextInt(60);
            stars.add(s);
        }
    }

    private void resetGame() {
        this.dinoY = GROUND_Y;
        this.dinoVelocity = 0.0;
        this.score = 0;
        this.gameState = 0;
        this.inputCooldown = 0;
        this.restartTimer = 0;
        this.cacti.clear();
        this.obstacleSpawnTimer = 30;
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        if (inputCooldown > 0) return;
        if (gameState == 1) return;

        if (dinoY >= GROUND_Y) {
            dinoVelocity = JUMP_IMPULSE;
            inputCooldown = 4;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(zBuffer, 0.0);
        frameCounter++;
        updateDayNight();

        String colorBg = rgb(
            lerp(DAY_BG[0], NIGHT_BG[0], nightTransition),
            lerp(DAY_BG[1], NIGHT_BG[1], nightTransition),
            lerp(DAY_BG[2], NIGHT_BG[2], nightTransition)
        );
        String colorFg = rgb(
            lerp(DAY_FG[0], NIGHT_FG[0], nightTransition),
            lerp(DAY_FG[1], NIGHT_FG[1], nightTransition),
            lerp(DAY_FG[2], NIGHT_FG[2], nightTransition)
        );
        String colorCloud = rgb(
            lerp(CLOUD_DAY[0], CLOUD_NIGHT[0], nightTransition),
            lerp(CLOUD_DAY[1], CLOUD_NIGHT[1], nightTransition),
            lerp(CLOUD_DAY[2], CLOUD_NIGHT[2], nightTransition)
        );

        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = colorBg + " " + RESET;
        }

        if (inputCooldown > 0) inputCooldown--;

        if (nightTransition > 0.05) {
            drawMoon(outputBuffer);
            drawStars(outputBuffer);
        }

        updateClouds();
        drawClouds(outputBuffer, colorCloud);

        if (gameState == 0) {
            dinoVelocity += GRAVITY;
            dinoY += dinoVelocity;

            if (dinoY >= GROUND_Y) {
                dinoY = GROUND_Y;
                dinoVelocity = 0.0;
            }

            for (int i = cacti.size() - 1; i >= 0; i--) {
                Cactus c = cacti.get(i);
                c.posX -= SCROLL_SPEED;

                if (!c.scoreClaimed && c.posX < DINO_SCREEN_X) {
                    score++;
                    if (score > highScore) highScore = score;
                    c.scoreClaimed = true;
                }

                if (c.posX < -8) {
                    cacti.remove(i);
                }
            }

            obstacleSpawnTimer--;
            if (obstacleSpawnTimer <= 0) {
                Cactus newCactus = new Cactus();
                newCactus.posX = WIDTH;
                newCactus.widthSize = 1 + random.nextInt(2);
                newCactus.height = 3 + random.nextInt(3);
                if (newCactus.widthSize == 1 && newCactus.height >= 4) {
                    newCactus.armLeft = random.nextBoolean();
                    newCactus.armRight = random.nextBoolean();
                }
                cacti.add(newCactus);
                obstacleSpawnTimer = 75 + random.nextInt(50);
            }

            int dLeft = DINO_SCREEN_X + 3;
            int dRight = DINO_SCREEN_X + DINO_W - 4;
            int dBottomY = (int) Math.round(dinoY) + 1;

            for (Cactus c : cacti) {
                int cLeft = (int) Math.round(c.posX);
                int cRight = cLeft + (c.widthSize * 2);

                if (dRight >= cLeft && dLeft <= cRight) {
                    int cactusTopY = GROUND_Y - c.height;

                    if (dBottomY >= cactusTopY) {
                        triggerCrash();
                    }
                }
            }
        } else {
            restartTimer--;
            if (restartTimer <= 0) {
                resetGame();
            }
        }

        groundScrollX += SCROLL_SPEED;

        for (int x = 0; x < WIDTH; x++) {
            outputBuffer[x + WIDTH * GROUND_Y] = colorFg + "▄" + RESET;
            int gx = x + (int) groundScrollX;
            int m = ((gx % 17) + 17) % 17;
            int m2 = ((gx % 41) + 41) % 41;
            if (m < 1) {
                outputBuffer[x + WIDTH * (GROUND_Y + 1)] = colorFg + "." + RESET;
            } else if (m2 < 1) {
                outputBuffer[x + WIDTH * (GROUND_Y + 1)] = colorFg + "," + RESET;
            }
        }

        drawCacti(outputBuffer, colorFg);
        drawDino(outputBuffer, colorFg);
        drawScoreText(outputBuffer, colorFg);
    }

    // ------------------------------------------------------------------
    // Drawing helpers
    // ------------------------------------------------------------------

    private void drawDino(String[] outputBuffer, String colorFg) {
        String[] frame;
        boolean crashed = gameState == 1;
        boolean airborne = dinoY < GROUND_Y - 0.5;

        if (crashed) {
            frame = DINO_DEAD;
        } else if (airborne) {
            frame = DINO_JUMP;
        } else {
            frame = ((frameCounter / 6) % 2 == 0) ? DINO_RUN_A : DINO_RUN_B;
        }

        int feetY = (int) Math.round(dinoY) + 1;
        int topY = feetY - (DINO_H - 1);
        String color = crashed ? COLOR_DINO_HIT : colorFg;

        for (int r = 0; r < DINO_H; r++) {
            int drawY = topY + r;
            if (drawY < 0 || drawY >= HEIGHT) continue;
            String row = frame[r];
            for (int c = 0; c < DINO_W; c++) {
                char ch = row.charAt(c);
                if (ch == ' ') continue;
                int drawX = DINO_SCREEN_X + c;
                if (drawX < 0 || drawX >= WIDTH) continue;
                outputBuffer[drawX + WIDTH * drawY] = color + ch + RESET;
            }
        }
    }

    private void drawCacti(String[] outputBuffer, String colorFg) {
        for (Cactus c : cacti) {
            int startX = (int) Math.round(c.posX);

            for (int w = 0; w < c.widthSize; w++) {
                int baseX = startX + (w * 2);
                int segHeight = (w == 0) ? c.height : Math.max(2, c.height - 1);
                int topTrunkY = GROUND_Y - segHeight;
                int capY = topTrunkY - 1;

                setCell(outputBuffer, baseX, capY, colorFg, '▲');
                for (int h = 0; h < segHeight; h++) {
                    int drawY = (GROUND_Y - 1) - h;
                    setCell(outputBuffer, baseX, drawY, colorFg, '█');
                }

                if (w == 0 && c.height >= 4) {
                    int trunkTop = GROUND_Y - c.height;
                    if (c.armLeft) {
                        setCell(outputBuffer, baseX - 1, trunkTop + 1, colorFg, '▛');
                        setCell(outputBuffer, baseX - 1, trunkTop + 2, colorFg, '█');
                    }
                    if (c.armRight) {
                        setCell(outputBuffer, baseX + 1, trunkTop, colorFg, '▜');
                        setCell(outputBuffer, baseX + 1, trunkTop + 1, colorFg, '█');
                    }
                }
            }
        }
    }

    private void updateClouds() {
        for (Cloud cloud : clouds) {
            cloud.posX -= cloud.speed;
            String[] spr = (cloud.variant == 0) ? CLOUD_SPRITE_A : CLOUD_SPRITE_B;
            if (cloud.posX < -spr[0].length()) {
                cloud.posX = WIDTH + random.nextInt(20);
                cloud.rowY = 3 + random.nextInt(8);
                cloud.variant = random.nextInt(2);
                cloud.speed = 0.04 + random.nextDouble() * 0.05;
            }
        }
    }

    private void drawClouds(String[] outputBuffer, String colorCloud) {
        for (Cloud cloud : clouds) {
            String[] spr = (cloud.variant == 0) ? CLOUD_SPRITE_A : CLOUD_SPRITE_B;
            int originX = (int) Math.round(cloud.posX);
            for (int r = 0; r < spr.length; r++) {
                String row = spr[r];
                for (int c = 0; c < row.length(); c++) {
                    char ch = row.charAt(c);
                    if (ch == ' ') continue;
                    setCell(outputBuffer, originX + c, cloud.rowY + r, colorCloud, ch);
                }
            }
        }
    }

    private void drawMoon(String[] outputBuffer) {
        int moonX = WIDTH - 16;
        int moonY = 3;
        for (int r = 0; r < MOON_SPRITE.length; r++) {
            String row = MOON_SPRITE[r];
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch == ' ') continue;
                setCell(outputBuffer, moonX + c, moonY + r, COLOR_MOON, ch);
            }
        }
    }

    private void drawStars(String[] outputBuffer) {
        char[] twinkle = {'.', '+', '\u2726', '+'};
        for (Star s : stars) {
            if (nightTransition < s.appearAt) continue;
            int idx = (int) (((frameCounter + s.phase) / 15) % twinkle.length);
            setCell(outputBuffer, s.x, s.y, COLOR_STAR, twinkle[idx]);
        }
    }

    private void drawScoreText(String[] outputBuffer, String colorFg) {
        String scoreMessage = "HI " + String.format("%05d", highScore) + "  " + String.format("%05d", score);
        if (gameState == 1) {
            scoreMessage = " G A M E   O V E R ";
        }

        int targetX = (WIDTH - scoreMessage.length()) / 2;
        int targetY = 2;

        for (int i = 0; i < scoreMessage.length(); i++) {
            setCell(outputBuffer, targetX + i, targetY, colorFg, scoreMessage.charAt(i));
        }
    }

    private void updateDayNight() {
        boolean targetNight = ((score / NIGHT_SCORE_INTERVAL) % 2) == 1;
        double target = targetNight ? 1.0 : 0.0;
        double step = 1.0 / TRANSITION_FRAMES;
        if (nightTransition < target) {
            nightTransition = Math.min(target, nightTransition + step);
        } else if (nightTransition > target) {
            nightTransition = Math.max(target, nightTransition - step);
        }
    }

    private void triggerCrash() {
        this.gameState = 1;
        this.restartTimer = 50;
        this.dinoVelocity = 0.0;
    }

    // ------------------------------------------------------------------
    // Small utilities
    // ------------------------------------------------------------------

    private static void setCell(String[] outputBuffer, int x, int y, String color, char ch) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        outputBuffer[x + WIDTH * y] = color + ch + RESET;
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private static String rgb(int r, int g, int b) {
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
    }
}