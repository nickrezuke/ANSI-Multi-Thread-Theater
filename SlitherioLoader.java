import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SlitherioLoader extends Loader {
    private static final StatusStage[] IO_STAGES = {
            new StatusStage(25, "Spawning shared server..."),
            new StatusStage(50, "Connecting autonomous agents..."),
            new StatusStage(75, "Injecting endless food matrix..."),
            new StatusStage(100, "Snake.io Server Online!")
    };

    // Grid coordinates leave Row 0 open for the HUD scoreboard
    private static final int GAME_GRID_W = 40;
    private static final int GAME_GRID_H = 21; // Height reduced by 1 for scoreboard row
    private static final int TERMINAL_W = 80;

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Point))
                return false;
            Point p = (Point) obj;
            return this.x == p.x && this.y == p.y;
        }
    }

    private static class SlitherBot {
        int id;
        String name;
        List<Point> body = new ArrayList<>();
        int dirX = 1;
        int dirY = 0;
        String headColor;
        String bodyColor;

        SlitherBot(int id, String name, String headColor, String bodyColor) {
            this.id = id;
            this.name = name;
            this.headColor = headColor;
            this.bodyColor = bodyColor;
        }
    }

    private final List<SlitherBot> bots = new ArrayList<>();
    private final List<Point> foodPellets = new ArrayList<>();
    private final Random rand = new Random();

    private long lastTickTime = 0;
    private static final long TICK_DURATION_MS = 65;

    // TrueColor Color Profiles
    private static final String RED_HEAD = "\u001B[38;2;255;50;50m";
    private static final String RED_BODY = "\u001B[38;2;180;30;30m";
    private static final String GREEN_HEAD = "\u001B[38;2;50;255;50m";
    private static final String GREEN_BODY = "\u001B[38;2;30;180;30m";
    private static final String BLUE_HEAD = "\u001B[38;2;50;150;255m";
    private static final String BLUE_BODY = "\u001B[38;2;30;90;180m";
    private static final String PURPLE_HEAD = "\u001B[38;2;200;50;255m";
    private static final String PURPLE_BODY = "\u001B[38;2;130;30;180m";
    private static final String FOOD_COLOR = "\u001B[38;2;255;220;50m";

    public SlitherioLoader() {
        // This uses 80x22 specifically
        super(IO_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        bots.clear();
        foodPellets.clear();

        // Instantiate our 4 competing server bots
        bots.add(new SlitherBot(0, "RED", RED_HEAD, RED_BODY));
        bots.add(new SlitherBot(1, "GRN", GREEN_HEAD, GREEN_BODY));
        bots.add(new SlitherBot(2, "BLU", BLUE_HEAD, BLUE_BODY));
        bots.add(new SlitherBot(3, "PRP", PURPLE_HEAD, PURPLE_BODY));

        for (SlitherBot bot : bots) {
            respawnBot(bot);
        }

        // Initialize arena with 30 scattered food dots
        for (int i = 0; i < 30; i++) {
            spawnPellet();
        }

        lastTickTime = System.currentTimeMillis();
    }

    private void respawnBot(SlitherBot bot) {
        bot.body.clear();

        // Spawn baby snakes safely shifted away from the direct edges
        int startX = 4 + rand.nextInt(GAME_GRID_W - 8);
        int startY = 3 + rand.nextInt(GAME_GRID_H - 6);

        for (int i = 0; i < 3; i++) {
            bot.body.add(new Point(startX, startY));
        }
        bot.dirX = (rand.nextBoolean() ? 1 : -1);
        bot.dirY = 0;
    }

    private void spawnPellet() {
        int fx = rand.nextInt(GAME_GRID_W);
        // +1 to ensure food never spawns on Row 0 (reserved for the HUD)
        int fy = 1 + rand.nextInt(GAME_GRID_H - 1);
        foodPellets.add(new Point(fx, fy));
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTickTime >= TICK_DURATION_MS) {
            lastTickTime = currentTime;
            processServerArenaPhysics();
        }

        drawEndlessArena(outputBuffer);
    }

    private void processServerArenaPhysics() {
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        // 1. UPDATE PATHING VECTORS FOR ALL SERVER BOTS
        for (SlitherBot bot : bots) {
            Point head = bot.body.get(0);
            int bestDX = bot.dirX;
            int bestDY = bot.dirY;
            double minDistanceToFood = Double.MAX_VALUE;
            boolean calculatedSafePath = false;

            // Greedy Search: Find the absolute closest nutrient pellet
            Point targetFood = null;
            for (Point food : foodPellets) {
                double d = Math.pow(head.x - food.x, 2) + Math.pow(head.y - food.y, 2);
                if (d < minDistanceToFood) {
                    minDistanceToFood = d;
                    targetFood = food;
                }
            }

            double bestMoveScore = Double.MAX_VALUE;
            for (int[] d : directions) {
                int nextX = head.x + d[0];
                int nextY = head.y + d[1];
                Point nextPoint = new Point(nextX, nextY);

                // Wall bounds checks (Leaving Row 0 empty for scoreboard)
                if (nextX < 0 || nextX >= GAME_GRID_W || nextY < 1 || nextY >= GAME_GRID_H)
                    continue;

                // Predictive obstacle mapping against all active snakes
                boolean predictiveCollision = false;
                for (SlitherBot obstacleBot : bots) {
                    for (Point p : obstacleBot.body) {
                        if (p.equals(nextPoint)) {
                            predictiveCollision = true;
                            break;
                        }
                    }
                    if (predictiveCollision)
                        break;
                }
                if (predictiveCollision)
                    continue;

                double foodDist = 0;
                if (targetFood != null) {
                    foodDist = Math.pow(nextX - targetFood.x, 2) + Math.pow(nextY - targetFood.y, 2);
                } else {
                    foodDist = rand.nextDouble();
                }

                // Straight path momentum bias factor
                if (d[0] == bot.dirX && d[1] == bot.dirY) {
                    foodDist *= 0.85;
                }

                if (foodDist < bestMoveScore) {
                    bestMoveScore = foodDist;
                    bestDX = d[0];
                    bestDY = d[1];
                    calculatedSafePath = true;
                }
            }

            if (calculatedSafePath) {
                bot.dirX = bestDX;
                bot.dirY = bestDY;
            }

            Point newHead = new Point(head.x + bot.dirX, head.y + bot.dirY);
            bot.body.add(0, newHead);

            if (foodPellets.contains(newHead)) {
                foodPellets.remove(newHead);
                spawnPellet(); // Replenish server food baseline density
            } else {
                bot.body.remove(bot.body.size() - 1);
            }
        }

        // 2. CRASH DETECTION LOOP
        for (SlitherBot bot : bots) {
            Point head = bot.body.get(0);

            // Wall strike instant respawn rule
            if (head.x < 0 || head.x >= GAME_GRID_W || head.y < 1 || head.y >= GAME_GRID_H) {
                killAndScatter(bot);
                continue;
            }

            // Head-to-Body crash verification sequence
            boolean hitObstacle = false;
            for (SlitherBot opponent : bots) {
                for (int i = 0; i < opponent.body.size(); i++) {
                    if (bot.id == opponent.id && i == 0)
                        continue;

                    if (head.equals(opponent.body.get(i))) {
                        hitObstacle = true;
                        break;
                    }
                }
                if (hitObstacle)
                    break;
            }

            if (hitObstacle) {
                killAndScatter(bot);
            }
        }
    }

    private void killAndScatter(SlitherBot bot) {
        // Explode snake body elements into permanent scattered food trails!
        for (int i = 1; i < bot.body.size(); i++) {
            Point p = bot.body.get(i);
            if (p.x >= 0 && p.x < GAME_GRID_W && p.y >= 1 && p.y < GAME_GRID_H) {
                foodPellets.add(new Point(p.x, p.y));
            }
        }
        // Continuous IO Rule: Instantly respawn as a brand new baby snake!
        respawnBot(bot);
    }

    private void drawEndlessArena(String[] outputBuffer) {
        // 1. GENERATE THE TOP ENDLESS LEADERBOARD SCORE HUD (Row 0)
        StringBuilder scoreboard = new StringBuilder(" ");
        for (SlitherBot bot : bots) {
            scoreboard.append(bot.headColor).append(bot.name).append(":")
                    .append(WHITE).append(String.format("%-3d  ", bot.body.size()));
        }
        scoreboard.append(RESET);

        // Write HUD string into the top row character buffer slice
        String hudString = scoreboard.toString();
        // Simple strip mapping wrapper to clear escape lengths
        outputBuffer[0] = hudString;
        for (int i = 1; i < TERMINAL_W; i++) {
            outputBuffer[i] = ""; // Blind out trailing cells on Row 0 so formatting lines align
        }

        // 2. DRAW FOOD PELLETS
        for (Point food : foodPellets) {
            int idx = (food.x * 2) + (food.y * TERMINAL_W);
            if (idx >= TERMINAL_W && idx < outputBuffer.length - 1) {
                outputBuffer[idx] = FOOD_COLOR + "·";
                outputBuffer[idx + 1] = " " + RESET;
            }
        }

        // 3. OVERLAY ACTIVE MULTIPLAYER SNAKES
        for (SlitherBot bot : bots) {
            if (bot.body.isEmpty())
                continue;

            for (int i = bot.body.size() - 1; i >= 0; i--) {
                Point p = bot.body.get(i);
                int idx = (p.x * 2) + (p.y * TERMINAL_W);
                if (idx >= TERMINAL_W && idx < outputBuffer.length - 1) {
                    if (i == 0) {
                        outputBuffer[idx] = bot.headColor + "█";
                        outputBuffer[idx + 1] = "█" + RESET;
                    } else {
                        outputBuffer[idx] = bot.bodyColor + "█";
                        outputBuffer[idx + 1] = "█" + RESET;
                    }
                }
            }
        }
    }
}