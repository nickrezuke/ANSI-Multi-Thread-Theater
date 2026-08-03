import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeLoader extends Loader {
    private static final StatusStage[] SNAKE_STAGES = {
        new StatusStage(25, "Booting autonomous reptile logic:"),
        new StatusStage(50, "Calibrating greedy square pathing:"),
        new StatusStage(75, "Consuming nutritional grid items:"),
        new StatusStage(100, "Snake Matrix Operational!")
    };

    // 1. Grid Structural Boundaries (WIDTH 80 divided into 40 double-cell spaces)
    private static final int GAME_GRID_W = 40; 
    private static final int GAME_GRID_H = 22;
    private static final int TERMINAL_W = 80;

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Point)) return false;
            Point p = (Point) obj;
            return this.x == p.x && this.y == p.y;
        }
    }

    private final List<Point> snakeBody = new ArrayList<>();
    private Point apple = new Point(0, 0);
    private int dirX = 1; 
    private int dirY = 0;
    private boolean isDead = false;
    private final Random rand = new Random();

    private long lastTickTime = 0;
    private static final long TICK_DURATION_MS = 45; 
    private long deathTimestamp = 0;

    // Solid full block pairs to build uniform squares
    private static final String COLOR_HEAD = "\u001B[38;5;82m";   // Bright Green
    private static final String COLOR_BODY = "\u001B[38;5;34m";   // Forest Green
    private static final String COLOR_PLUM = "\u001B[38;5;196m";  // Neon Red
    private static final String COLOR_DEAD = "\u001B[31m";        // Dark Red

    public SnakeLoader() {
        super(SNAKE_STAGES);
    }

    @Override
    protected void initialize() {
        resetGame();
    }

    private void resetGame() {
        snakeBody.clear();
        isDead = false;
        
        int startX = GAME_GRID_W / 2;
        int startY = GAME_GRID_H / 2;
        for (int i = 0; i < 4; i++) {
            snakeBody.add(new Point(startX - i, startY));
        }
        
        dirX = 1;
        dirY = 0;
        spawnApple();
        lastTickTime = System.currentTimeMillis();
    }

    private void spawnApple() {
        // Precompute precise 12% margin boundaries and range distributions
        int minX = (int) (GAME_GRID_W * 0.12); // Left floor boundary (40 * 0.12 = 4)
        int maxX = (int) (GAME_GRID_W * (1.0 - 0.12)); // Right ceiling boundary (40 * 0.88 = 35)
        int rangeX = maxX - minX + 1; // Total selectable width slots (35 - 4 + 1 = 32 slots)

        int minY = (int) (GAME_GRID_H * 0.12); // Top floor boundary (22 * 0.12 = 2)
        int maxY = (int) (GAME_GRID_H * (1.0 - 0.12)); // Bottom ceiling boundary (22 * 0.88 = 19)
        int rangeY = maxY - minY + 1; // Total selectable height slots (19 - 2 + 1 = 18 slots)

        while (true) {
            // Generate coordinates shifted safely away from the grid edges
            int rx = minX + rand.nextInt(rangeX);
            int ry = minY + rand.nextInt(rangeY);
            Point potentialApple = new Point(rx, ry);

            if (!snakeBody.contains(potentialApple)) {
                apple = potentialApple;
                break;
            }
        }
    }


    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (isDead) {
            if (currentTime - deathTimestamp >= 1000) {
                resetGame();
            } else {
                drawScene(outputBuffer);
                return;
            }
        }

        // Relate the duration to the snake's length so it speeds up as the game progresses
        double effectiveDuration = TICK_DURATION_MS * Math.pow(0.96, snakeBody.size() - 4);

        // Solid floor limit protection clamp (5ms) protects loops from collapsing down to zero
        if (effectiveDuration < 5.0) {
            effectiveDuration = 5.0;
        }

        if (currentTime - lastTickTime >= effectiveDuration) {
            lastTickTime = currentTime;
            executeCPUMovement();
        }

        drawScene(outputBuffer);
    }

    private void executeCPUMovement() {
        Point head = snakeBody.get(0);
        int bestDX = 0, bestDY = 0;
        double minDistance = Double.MAX_VALUE;
        boolean safeMoveFound = false;

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] d : directions) {
            int nextX = head.x + d[0];
            int nextY = head.y + d[1];
            Point nextPoint = new Point(nextX, nextY);

            if (nextX < 0 || nextX >= GAME_GRID_W || nextY < 0 || nextY >= GAME_GRID_H) continue;
            
            boolean collidesWithSelf = false;
            for (int i = 0; i < snakeBody.size() - 1; i++) {
                if (snakeBody.get(i).equals(nextPoint)) {
                    collidesWithSelf = true;
                    break;
                }
            }
            if (collidesWithSelf) continue;

            // Greedy distance tracking to apple coordinates
            double dist = Math.pow(nextX - apple.x, 2) + Math.pow(nextY - apple.y, 2);
            if (dist < minDistance) {
                minDistance = dist;
                bestDX = d[0];
                bestDY = d[1];
                safeMoveFound = true;
            }
        }

        if (!safeMoveFound) {
            isDead = true;
            deathTimestamp = System.currentTimeMillis();
            return;
        }

        dirX = bestDX;
        dirY = bestDY;

        Point newHead = new Point(head.x + dirX, head.y + dirY);
        snakeBody.add(0, newHead);

        if (newHead.equals(apple)) {
            spawnApple();
        } else {
            snakeBody.remove(snakeBody.size() - 1);
        }
    }

    private void drawScene(String[] outputBuffer) {
        // 1. Draw Apple Square (Takes 2 side-by-side cells in the buffer)
        int appleTermIndex = (apple.x * 2) + (apple.y * TERMINAL_W);
        if (appleTermIndex >= 0 && appleTermIndex < outputBuffer.length - 1) {
            outputBuffer[appleTermIndex] = COLOR_PLUM + "█";
            outputBuffer[appleTermIndex + 1] = "█" + RESET;
        }

        // 2. Draw Snake segments (Each maps to 2 side-by-side terminal cells)
        for (int i = snakeBody.size() - 1; i >= 0; i--) {
            Point p = snakeBody.get(i);
            int idx = (p.x * 2) + (p.y * TERMINAL_W);
            
            if (idx >= 0 && idx < outputBuffer.length - 1) {
                if (i == 0) {
                    // Head: Renders an 'XX' if dead, or bright green full blocks if alive
                    if (isDead) {
                        outputBuffer[idx] = COLOR_DEAD + "X";
                        outputBuffer[idx + 1] = "X" + RESET;
                    } else {
                        outputBuffer[idx] = COLOR_HEAD + "█";
                        outputBuffer[idx + 1] = "█" + RESET;
                    }
                } else {
                    // Body segments are dense full block squares
                    outputBuffer[idx] = COLOR_BODY + "█";
                    outputBuffer[idx + 1] = "█" + RESET;
                }
            }
        }
    }
}
