// TODO: Stop the player from shooting the bunkers lmao

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpaceInvadersLoader extends Loader {
    private static final StatusStage[] SPACE_STAGES = {
            new StatusStage(25, "Calibrating laser cannons:"),
            new StatusStage(50, "Deploying alien vanguard matrices:"),
            new StatusStage(75, "Syncing shield coordinate vectors:"),
            new StatusStage(100, "Space Defensive Grid Online!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    private static class Entity {
        double x, y;
        boolean active = true;

        Entity(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Invader extends Entity {
        int type;

        Invader(double x, double y, int type) {
            super(x, y);
            this.type = type;
        }
    }

    private final List<Invader> invaders = new ArrayList<>();
    private final List<Entity> playerLasers = new ArrayList<>();
    private final List<Entity> alienBombs = new ArrayList<>();

    // DISTINCT SYSTEM: 4 Live tracking destructible shield rows
    private final int[][] shields = new int[HEIGHT][WIDTH];

    private double playerX;
    private static final int PLAYER_Y = HEIGHT - 2;
    private int score = 0;
    private boolean isDead = false;
    private long lastTickTime = 0;
    private static final long REFRESH_RATE_MS = 30;
    private long deathTimestamp = 0;
    private final Random rand = new Random();

    private int invaderDirX = 1;
    private long lastInvaderMoveTime = 0;
    private double invaderSpeedMs = 600;

    private static final String COLOR_PLAYER = "\u001B[38;5;46m";
    private static final String COLOR_LASER = "\u001B[38;5;81m";
    private static final String COLOR_BOMB = "\u001B[38;5;214m";
    private static final String COLOR_TEXT = "\u001B[38;5;244m";
    private static final String COLOR_SHIELD = "\u001B[38;5;40m"; // Dense Shield Green

    private static final String[] INVADER_COLORS = {
            "\u001B[38;5;201m", "\u001B[38;5;99m", "\u001B[38;5;39m"
    };

    public SpaceInvadersLoader() {
        super(SPACE_STAGES);
    }

    @Override
    protected void initialize() {
        score = 0;
        resetMatch();
    }

    private void resetMatch() {
        isDead = false;
        playerX = WIDTH / 2.0;
        invaders.clear();
        playerLasers.clear();
        alienBombs.clear();
        invaderDirX = 1;
        invaderSpeedMs = 600;

        // Reset and deploy the 4 distinct structural defensive bunkers
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++)
                shields[y][x] = 0;
        }
        int shieldY = PLAYER_Y - 3;
        int[] bunkerPositions = { 12, 30, 48, 66 };
        for (int bX : bunkerPositions) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dx = 0; dx < 5; dx++) {
                    shields[shieldY + dy][bX + dx] = 3; // 3 hit-points per bunker pixel node
                }
            }
        }

        for (int r = 0; r < 5; r++) {
            int invaderType = (r == 0) ? 0 : (r < 3) ? 1 : 2;
            for (int c = 0; c < 11; c++) {
                invaders.add(new Invader(15 + c * 4, 3 + r * 1.5, invaderType));
            }
        }
        lastTickTime = System.currentTimeMillis();
        lastInvaderMoveTime = System.currentTimeMillis();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (isDead) {
            if (currentTime - deathTimestamp >= 1000) {
                resetMatch();
            } else {
                drawScene(outputBuffer);
                return;
            }
        }

        if (currentTime - lastTickTime >= REFRESH_RATE_MS) {
            lastTickTime = currentTime;
            updatePhysics(currentTime);
        }

        drawScene(outputBuffer);
    }

    private void updatePhysics(long currentTime) {
        if (currentTime - lastInvaderMoveTime >= invaderSpeedMs) {
            lastInvaderMoveTime = currentTime;
            boolean shiftDownRequired = false;

            for (Invader inv : invaders) {
                if (invaderDirX == 1 && inv.x >= WIDTH - 4) {
                    shiftDownRequired = true;
                    break;
                }
                if (invaderDirX == -1 && inv.x <= 2) {
                    shiftDownRequired = true;
                    break;
                }
            }

            if (shiftDownRequired) {
                invaderDirX = -invaderDirX;
                for (Invader inv : invaders) {
                    inv.y += 1.0;
                    if (inv.y >= PLAYER_Y) {
                        triggerDeath();
                        return;
                    }
                }
                invaderSpeedMs = Math.max(100, invaderSpeedMs - 40);
            } else {
                for (Invader inv : invaders) {
                    inv.x += invaderDirX * 2;
                }
            }
        }

        if (!invaders.isEmpty()) {
            Invader target = invaders.get(0);
            for (Invader inv : invaders) {
                if (inv.y > target.y
                        || (inv.y == target.y && Math.abs(inv.x - playerX) < Math.abs(target.x - playerX))) {
                    target = inv;
                }
            }

            if (playerX < target.x && playerX < WIDTH - 3)
                playerX += 1.0;
            if (playerX > target.x && playerX > 2)
                playerX -= 1.0;

            if (Math.abs(playerX - target.x) <= 2 && playerLasers.size() < 2 && rand.nextInt(10) > 7) {
                playerLasers.add(new Entity(playerX, PLAYER_Y - 1));
            }
        }

        // Advance and resolve player lasers hitting alien grids or bunkers
        Iterator<Entity> laserIt = playerLasers.iterator();
        while (laserIt.hasNext()) {
            Entity laser = laserIt.next();
            laser.y -= 1.2;
            int ly = (int) Math.round(laser.y);
            int lx = (int) Math.round(laser.x);

            if (ly < 0) {
                laserIt.remove();
                continue;
            }

            // Collision check: Laser hitting defensive bunkers from below
            if (ly >= 0 && ly < HEIGHT && lx >= 0 && lx < WIDTH && shields[ly][lx] > 0) {
                shields[ly][lx]--; // Damage shield block
                laserIt.remove();
                continue;
            }

            for (Invader inv : invaders) {
                if (Math.abs(laser.x - inv.x) <= 1.5 && Math.round(laser.y) == Math.round(inv.y)) {
                    inv.active = false;
                    laser.active = false;
                    score += (inv.type == 0) ? 30 : (inv.type == 1) ? 20 : 10;
                    break;
                }
            }
            if (!laser.active)
                laserIt.remove();
        }

        if (!invaders.isEmpty() && alienBombs.size() < 4 && rand.nextInt(100) > 94) {
            Invader shooter = invaders.get(rand.nextInt(invaders.size()));
            alienBombs.add(new Entity(shooter.x, shooter.y + 1));
        }

        // Advance and resolve alien bombs hitting shields or the player
        Iterator<Entity> bombIt = alienBombs.iterator();
        while (bombIt.hasNext()) {
            Entity bomb = bombIt.next();
            bomb.y += 0.8;
            int by = (int) Math.round(bomb.y);
            int bx = (int) Math.round(bomb.x);

            if (by >= HEIGHT) {
                bombIt.remove();
                continue;
            }

            // Collision check: Alien bombs blasting shields from above
            if (by >= 0 && by < HEIGHT && bx >= 0 && bx < WIDTH && shields[by][bx] > 0) {
                shields[by][bx] = 0; // Wipe out shield node instantly on blast impact
                bombIt.remove();
                continue;
            }

            if (Math.abs(bomb.x - playerX) <= 1.5 && by == PLAYER_Y) {
                triggerDeath();
                return;
            }
        }

        invaders.removeIf(inv -> !inv.active);
        if (invaders.isEmpty())
            resetMatch();
    }

    private void triggerDeath() {
        isDead = true;
        deathTimestamp = System.currentTimeMillis();
    }

    private void drawScene(String[] outputBuffer) {
        String scoreHud = "SCORE: " + score;
        for (int i = 0; i < scoreHud.length(); i++) {
            outputBuffer[2 + i] = COLOR_TEXT + scoreHud.charAt(i) + RESET;
        }

        // Render active structural shield particles
        char[] shieldShades = { ' ', '░', '▒', '█' };
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int hp = shields[y][x];
                if (hp > 0) {
                    outputBuffer[x + y * WIDTH] = COLOR_SHIELD + shieldShades[hp] + RESET;
                }
            }
        }

        for (Invader inv : invaders) {
            int iX = (int) Math.round(inv.x);
            int iY = (int) Math.round(inv.y);
            if (iX >= 0 && iX < WIDTH - 1 && iY >= 0 && iY < HEIGHT) {
                int cellIdx = iX + iY * WIDTH;
                String color = INVADER_COLORS[inv.type];
                String sprite = (inv.type == 0) ? "╩═╩" : (inv.type == 1) ? "╚═╝" : "▄█▄";
                for (int w = 0; w < 3; w++) {
                    if (cellIdx + w < outputBuffer.length) {
                        outputBuffer[cellIdx + w] = color + sprite.charAt(w) + RESET;
                    }
                }
            }
        }

        for (Entity laser : playerLasers) {
            int lX = (int) Math.round(laser.x);
            int lY = (int) Math.round(laser.y);
            if (lX >= 0 && lX < WIDTH && lY >= 0 && lY < HEIGHT) {
                outputBuffer[lX + lY * WIDTH] = COLOR_LASER + "║" + RESET;
            }
        }

        for (Entity bomb : alienBombs) {
            int bX = (int) Math.round(bomb.x);
            int bY = (int) Math.round(bomb.y);
            if (bX >= 0 && bX < WIDTH && bY >= 0 && bY < HEIGHT) {
                outputBuffer[bX + bY * WIDTH] = COLOR_BOMB + "☼" + RESET;
            }
        }

        int pX = (int) Math.round(playerX);
        int pIdx = pX + PLAYER_Y * WIDTH;
        if (pX >= 1 && pX < WIDTH - 1) {
            if (isDead) {
                outputBuffer[pIdx - 1] = "\u001B[31m<";
                outputBuffer[pIdx] = "X";
                outputBuffer[pIdx + 1] = ">\u001B[0m";
            } else {
                outputBuffer[pIdx - 1] = COLOR_PLAYER + "▄";
                outputBuffer[pIdx] = "█";
                outputBuffer[pIdx + 1] = "▄" + RESET;
            }
        }
    }
}