import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CentipedeLoader extends Loader {
    private static final StatusStage[] GAME_STAGES = {
        new StatusStage(25, "Spawning mushroom colony patches:"),
        new StatusStage(50, "Assembling multi-segment arthropods:"),
        new StatusStage(75, "Syncing bug-blaster targeting routines:"),
        new StatusStage(100, "Centipede Arcade Core Operational!")
    };

    // FIX: Dimensions flipped to standard vertical arcade aspect ratios
    private static final int WIDTH = 45;
    private static final int HEIGHT = 30;
    private static final int SIZE = WIDTH * HEIGHT;

    private static class Segment {
        int x, y;
        int dirX; // 1 = Right, -1 = Left
        boolean isHead;
        boolean active = true;

        Segment(int x, int y, int dirX, boolean isHead) {
            this.x = x; this.y = y;
            this.dirX = dirX; this.isHead = isHead;
        }
    }

    private static class Laser {
        int x, y;
        boolean active = true;
        Laser(int x, int y) { this.x = x; this.y = y; }
    }

    private final boolean[][] mushrooms = new boolean[HEIGHT][WIDTH];
    private final List<Segment> centipede = new ArrayList<>();
    private final List<Laser> lasers = new ArrayList<>();

    private int playerX;
    private int playerY;
    private static final int PLAYER_ZONE_TOP = HEIGHT - 6; // Proportional vertical zone top boundary

    private boolean isDead = false;
    private long lastTickTime = 0;
    private long lastMoveTime = 0;
    
    private static final long REFRESH_RATE_MS = 10;     
    private static final long CENTIPEDE_MOVE_MS = 50;   // Adjusted step delay for narrow tracking lanes
    private long deathTimestamp = 0;
    private final Random rand = new Random();

    private static final String COLOR_PLAYER = "\u001B[38;5;46m";   
    private static final String COLOR_SHROOM = "\u001B[38;5;214m";  
    private static final String COLOR_HEAD = "\u001B[38;5;196m";    
    private static final String COLOR_BODY = "\u001B[38;5;162m";    
    private static final String COLOR_LASER = "\u001B[38;5;81m";    

    public CentipedeLoader() {
        super(GAME_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        resetMatch();
    }

    private void resetMatch() {
        isDead = false;
        playerX = WIDTH / 2;
        playerY = HEIGHT - 2;
        
        lasers.clear();
        centipede.clear();

        // Regenerate mushroom field matching the new vertical grid proportions
        for (int y = 3; y < HEIGHT - 3; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                mushrooms[y][x] = (rand.nextInt(100) > 94); // Slightly increased density to fill columns
            }
        }

        // Spawn a compact 8-segment chain appropriate for 24-column widths
        for (int i = 0; i < 8; i++) {
            centipede.add(new Segment(10 - i, 1, 1, i == 0));
        }

        lastTickTime = System.currentTimeMillis();
        lastMoveTime = System.currentTimeMillis();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (isDead) {
            if (currentTime - deathTimestamp >= 1200) {
                resetMatch();
            } else {
                drawScene(outputBuffer);
                return;
            }
        }

        if (currentTime - lastTickTime >= REFRESH_RATE_MS) {
            lastTickTime = currentTime;
            updateSimulationPhysics(currentTime);
        }

        drawScene(outputBuffer);
    }

    private void updateSimulationPhysics(long currentTime) {
        if (currentTime - lastMoveTime >= CENTIPEDE_MOVE_MS) {
            lastMoveTime = currentTime;

            for (int i = 0; i < centipede.size(); i++) {
                Segment seg = centipede.get(i);
                int nextX = seg.x + seg.dirX;

                boolean hitWall = (nextX < 0 || nextX >= WIDTH);
                boolean hitMushroom = (!hitWall && mushrooms[seg.y][nextX]);

                if (hitWall || hitMushroom) {
                    seg.dirX = -seg.dirX; 
                    seg.y++;             

                    if (seg.y >= HEIGHT) {
                        seg.y = PLAYER_ZONE_TOP;
                    }
                } else {
                    seg.x = nextX; 
                }

                if (seg.x == playerX && seg.y == playerY) {
                    triggerDeath();
                    return;
                }
            }
        }

        Iterator<Laser> laserIt = lasers.iterator();
        while (laserIt.hasNext()) {
            Laser laser = laserIt.next();
            laser.y -= 1; 
            if (laser.y < 0) {
                laserIt.remove();
                continue;
            }

            if (mushrooms[laser.y][laser.x]) {
                mushrooms[laser.y][laser.x] = false; 
                laserIt.remove();
                continue;
            }

            for (int i = 0; i < centipede.size(); i++) {
                Segment seg = centipede.get(i);
                if (seg.x == laser.x && seg.y == laser.y) {
                    seg.active = false;
                    laser.active = false;
                    mushrooms[seg.y][seg.x] = true;

                    if (i + 1 < centipede.size()) {
                        centipede.get(i + 1).isHead = true;
                    }
                    break;
                }
            }
            if (!laser.active) {
                laserIt.remove();
            }
        }

        centipede.removeIf(seg -> !seg.active);

        if (!centipede.isEmpty()) {
            Segment target = centipede.get(0);
            for (Segment seg : centipede) {
                if (seg.y > target.y) {
                    target = seg;
                }
            }

            if (playerX < target.x && !mushrooms[playerY][playerX + 1] && playerX < WIDTH - 1) playerX++;
            else if (playerX > target.x && !mushrooms[playerY][playerX - 1] && playerX > 0) playerX--;

            if (Math.abs(target.x - playerX) < 3 && target.y >= PLAYER_ZONE_TOP) {
                if (playerY > PLAYER_ZONE_TOP && !mushrooms[playerY - 1][playerX]) playerY--;
            } else if (playerY < HEIGHT - 2 && !mushrooms[playerY + 1][playerX]) {
                playerY++;
            }

            if (Math.abs(playerX - target.x) <= 1 && lasers.size() < 2 && rand.nextInt(10) > 5) {
                lasers.add(new Laser(playerX, playerY - 1));
            }
        } else {
            resetMatch();
        }
    }

    private void triggerDeath() {
        isDead = true;
        deathTimestamp = System.currentTimeMillis();
    }

    private void drawScene(String[] outputBuffer) {
        for (int y = 0; y < HEIGHT; y++) {
            int rowOffset = y * WIDTH;
            for (int x = 0; x < WIDTH; x++) {
                if (mushrooms[y][x]) {
                    outputBuffer[x + rowOffset] = COLOR_SHROOM + "♣" + RESET; 
                }
            }
        }

        for (Laser laser : lasers) {
            int idx = laser.x + laser.y * WIDTH;
            if (idx >= 0 && idx < SIZE) {
                outputBuffer[idx] = COLOR_LASER + "│" + RESET;
            }
        }

        for (Segment seg : centipede) {
            int idx = seg.x + seg.y * WIDTH;
            if (idx >= 0 && idx < SIZE) {
                if (seg.isHead) {
                    outputBuffer[idx] = COLOR_HEAD + "Ö" + RESET; 
                } else {
                    outputBuffer[idx] = COLOR_BODY + "º" + RESET; 
                }
            }
        }

        int playerIdx = playerX + playerY * WIDTH;
        if (playerX >= 0 && playerX < WIDTH && playerY >= 0 && playerY < HEIGHT) {
            if (isDead) {
                outputBuffer[playerIdx] = "\u001B[31m*X*" + RESET; 
            } else {
                outputBuffer[playerIdx] = COLOR_PLAYER + "╨" + RESET; 
            }
        }
    }
}
