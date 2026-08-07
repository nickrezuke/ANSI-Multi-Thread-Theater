import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.LinkedList;
import java.util.Queue;

public class TronLoader extends Loader {

    private static final StatusStage[] TRON_STAGES = {
            new StatusStage(25, "Initializing Grid network..."),
            new StatusStage(50, "Powering up Light Cycles..."),
            new StatusStage(75, "Stabilizing trail matrices..."),
            new StatusStage(100, "TRON Grid Online!")
    };

    private static final int GAME_GRID_W = 40;
    private static final int GAME_GRID_H = 22;
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

    private static class LightCycle {
        int id;
        String name;
        List<Point> trail = new ArrayList<>();
        int dirX = 1;
        int dirY = 0;
        String headColor;
        String trailColor;
        boolean isAlive = true;
        int wins = 0;

        LightCycle(int id, String name, String headColor, String trailColor) {
            this.id = id;
            this.name = name;
            this.headColor = headColor;
            this.trailColor = trailColor;
        }
    }

    private final List<LightCycle> cycles = new ArrayList<>();
    private final Random rand = new Random();

    private long lastTickTime = 0;
    private static final long TICK_DURATION_MS = 65;

    private boolean roundOver = false;
    private long roundResetTime = 0;
    private static final long PAUSE_DURATION_MS = 3000;

    private static final String RED_HEAD = "\u001B[38;2;255;80;80m";
    private static final String RED_WALL = "\u001B[38;2;180;40;40m";
    private static final String CYAN_HEAD = "\u001B[38;2;80;255;255m";
    private static final String CYAN_WALL = "\u001B[38;2;40;160;180m";
    private static final String YEL_HEAD = "\u001B[38;2;255;255;80m";
    private static final String YEL_WALL = "\u001B[38;2;180;160;40m";
    private static final String PURP_HEAD = "\u001B[38;2;220;80;255m";
    private static final String PURP_WALL = "\u001B[38;2;140;40;180m";

    public TronLoader() {
        super(TRON_STAGES);
    }

    @Override
    protected void initialize() {
        cycles.clear();
        cycles.add(new LightCycle(0, "CLU", RED_HEAD, RED_WALL));
        cycles.add(new LightCycle(1, "TRN", CYAN_HEAD, CYAN_WALL));
        cycles.add(new LightCycle(2, "VIR", YEL_HEAD, YEL_WALL));
        cycles.add(new LightCycle(3, "ANO", PURP_HEAD, PURP_WALL));

        startNewRound();
        lastTickTime = System.currentTimeMillis();
    }

    private void startNewRound() {
        roundOver = false;
        for (LightCycle cycle : cycles) {
            cycle.isAlive = true;
            cycle.trail.clear();

            int startX = 4 + rand.nextInt(GAME_GRID_W - 8);
            int startY = 3 + rand.nextInt(GAME_GRID_H - 6);

            cycle.trail.add(new Point(startX, startY));

            int dirChoice = rand.nextInt(4);
            if (dirChoice == 0) {
                cycle.dirX = 1;
                cycle.dirY = 0;
            } else if (dirChoice == 1) {
                cycle.dirX = -1;
                cycle.dirY = 0;
            } else if (dirChoice == 2) {
                cycle.dirX = 0;
                cycle.dirY = 1;
            } else {
                cycle.dirX = 0;
                cycle.dirY = -1;
            }

            cycle.trail.add(0, new Point(startX + cycle.dirX, startY + cycle.dirY));
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (roundOver) {
            if (currentTime - roundResetTime >= PAUSE_DURATION_MS) {
                startNewRound();
                lastTickTime = currentTime;
            }
            drawGridArena(outputBuffer);
            return;
        }

        if (currentTime - lastTickTime >= TICK_DURATION_MS) {
            lastTickTime = currentTime;
            processGridPhysics();
        }
        drawGridArena(outputBuffer);
    }

    private void processGridPhysics() {
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        // 1. DEEP FLOOD FILL SURVIVAL AI
        for (LightCycle cycle : cycles) {
            if (!cycle.isAlive)
                continue;

            Point head = cycle.trail.get(0);

            int nextX = head.x + cycle.dirX;
            int nextY = head.y + cycle.dirY;

            // Check if staying straight is an imminent crash, or choose to evaluate a turn
            // 15% of the time
            boolean straightIsDeadly = isCollisionLocation(nextX, nextY);
            boolean desireToTurn = (rand.nextInt(100) < 15);

            if (straightIsDeadly || desireToTurn) {
                int maxAccessibleTerritory = -1;
                List<int[]> optimalMoves = new ArrayList<>();

                // Evaluate all 4 possible movement directions
                for (int[] d : directions) {
                    // Prevent immediate 180-degree self-collisions
                    if (d[0] == -cycle.dirX && d[1] == -cycle.dirY)
                        continue;

                    int testX = head.x + d[0];
                    int testY = head.y + d[1];

                    if (!isCollisionLocation(testX, testY)) {
                        // Perform a full Flood Fill (BFS) to map the total reachable grid space
                        int areaSize = calculateAccessibleArea(testX, testY);

                        if (areaSize > maxAccessibleTerritory) {
                            maxAccessibleTerritory = areaSize;
                            optimalMoves.clear();
                            optimalMoves.add(d);
                        } else if (areaSize == maxAccessibleTerritory) {
                            optimalMoves.add(d);
                        }
                    }
                }

                // Turn toward the absolute largest pocket of open survival space
                if (!optimalMoves.isEmpty()) {
                    // Bias towards continuing straight if it's tied for the maximum area choice
                    boolean straightIsOptimal = false;
                    for (int[] move : optimalMoves) {
                        if (move[0] == cycle.dirX && move[1] == cycle.dirY) {
                            straightIsOptimal = true;
                            break;
                        }
                    }

                    if (!straightIsOptimal || straightIsDeadly) {
                        int[] chosenMove = optimalMoves.get(rand.nextInt(optimalMoves.size()));
                        cycle.dirX = chosenMove[0];
                        cycle.dirY = chosenMove[1];
                    }
                }
            }

            Point newHead = new Point(head.x + cycle.dirX, head.y + cycle.dirY);
            cycle.trail.add(0, newHead);
        }

        // 2. CRASH EVALUATION
        for (LightCycle cycle : cycles) {
            if (!cycle.isAlive)
                continue;

            Point head = cycle.trail.get(0);
            boolean crashed = false;

            if (head.x < 0 || head.x >= GAME_GRID_W || head.y < 1 || head.y >= GAME_GRID_H) {
                crashed = true;
            } else {
                for (LightCycle obs : cycles) {
                    if (!obs.isAlive)
                        continue;
                    for (int i = 0; i < obs.trail.size(); i++) {
                        if (cycle.id == obs.id && i == 0)
                            continue;
                        if (head.equals(obs.trail.get(i))) {
                            crashed = true;
                            break;
                        }
                    }
                    if (crashed)
                        break;
                }
            }

            if (crashed) {
                cycle.isAlive = false;
                cycle.trail.clear();
            }
        }

        // 3. WINNER CHECKING
        int aliveCount = 0;
        LightCycle potentialWinner = null;
        for (LightCycle cycle : cycles) {
            if (cycle.isAlive) {
                aliveCount++;
                potentialWinner = cycle;
            }
        }

        if (aliveCount <= 1) {
            if (potentialWinner != null && aliveCount == 1) {
                potentialWinner.wins++;
            }
            roundOver = true;
            roundResetTime = System.currentTimeMillis();
        }
    }

    // BFS Territory Flood Fill Analysis Function
    private int calculateAccessibleArea(int startX, int startY) {
        boolean[][] visited = new boolean[GAME_GRID_W][GAME_GRID_H];
        int count = 0;

        // Pre-populate structural obstacles into our temporary search matrix
        for (LightCycle c : cycles) {
            if (!c.isAlive)
                continue;
            for (Point p : c.trail) {
                if (p.x >= 0 && p.x < GAME_GRID_W && p.y >= 1 && p.y < GAME_GRID_H) {
                    visited[p.x][p.y] = true;
                }
            }
        }

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        int[][] moves = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        while (!queue.isEmpty()) {
            Point curr = queue.poll();
            count++;

            for (int[] m : moves) {
                int nx = curr.x + m[0];
                int ny = curr.y + m[1];

                if (nx >= 0 && nx < GAME_GRID_W && ny >= 1 && ny < GAME_GRID_H) {
                    if (!visited[nx][ny]) {
                        visited[nx][ny] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }
        return count;
    }

    private boolean isCollisionLocation(int x, int y) {
        if (x < 0 || x >= GAME_GRID_W || y < 1 || y >= GAME_GRID_H)
            return true;
        Point checkPoint = new Point(x, y);
        for (LightCycle c : cycles) {
            if (!c.isAlive)
                continue;
            for (Point p : c.trail) {
                if (p.equals(checkPoint))
                    return true;
            }
        }
        return false;
    }

    private void drawGridArena(String[] outputBuffer) {
        for (int i = 0; i < TERMINAL_W; i++) {
            outputBuffer[i] = "";
        }
        StringBuilder scoreboard = new StringBuilder(" ");
        for (LightCycle cycle : cycles) {
            scoreboard.append(cycle.headColor).append(cycle.name).append(":").append(WHITE)
                    .append(String.format("%-3d ", cycle.wins));
        }
        if (roundOver) {
            scoreboard.append("  [ROUND OVER - REBOOTING GRID]");
        } else {
            scoreboard.append("                               ");
        }
        scoreboard.append(RESET);
        outputBuffer[0] = scoreboard.toString();
        for (LightCycle cycle : cycles) {
            if (!cycle.isAlive || cycle.trail.isEmpty())
                continue;
            for (int i = cycle.trail.size() - 1; i >= 0; i--) {
                Point p = cycle.trail.get(i);
                int idx = (p.x * 2) + (p.y * TERMINAL_W);
                if (idx >= TERMINAL_W && idx < outputBuffer.length - 1) {
                    if (i == 0) {
                        outputBuffer[idx] = cycle.headColor + "█";
                        outputBuffer[idx + 1] = "█" + RESET;
                    } else {
                        outputBuffer[idx] = cycle.trailColor + "▒";
                        outputBuffer[idx + 1] = "▒" + RESET;
                    }
                }
            }
        }
    }
}