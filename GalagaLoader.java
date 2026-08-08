import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GalagaLoader extends Loader {
    private static final StatusStage[] GALAGA_STAGES = {
            new StatusStage(25, "Booting dual-fighter profiles:"),
            new StatusStage(50, "Assembling flagship command grids:"),
            new StatusStage(75, "Syncing flight attack paths:"),
            new StatusStage(100, "Galaga Star-Force Operational!")
    };

    // Aligned to space-conscious vertical cabinet specifications
    private static final int WIDTH = 24;
    private static final int HEIGHT = 28;
    private static final int SIZE = WIDTH * HEIGHT;


    private static class Alien {
        double x, y;
        double homeX, homeY;
        int type; // 0 = Boss Flagship, 1 = Red Guard, 2 = Blue Drone
        boolean isDiving = false;
        double diveTimer = 0;
        boolean active = true;

        Alien(double hX, double hY, int t) {
            this.homeX = hX;
            this.homeY = hY;
            this.x = hX;
            this.y = hY;
            this.type = t;
        }
    }

    private static class Bullet {
        double x, y;
        boolean isAlien;
        boolean active = true;

        Bullet(double x, double y, boolean isAlien) {
            this.x = x;
            this.y = y;
            this.isAlien = isAlien;
        }
    }

    private final List<Alien> aliens = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();

    private double playerX;
    private final int playerY = HEIGHT - 2;
    private boolean isDead = false;

    // Tractor Beam State Mechanics
    private boolean beamActive = false;
    private int beamX = -1;
    private int beamTopY = -1;
    private int beamTimer = 0;

    private long lastTickTime = 0;
    private static final long REFRESH_RATE_MS = 25; // ~40 FPS fast vertical loop update
    private long deathTimestamp = 0;
    private final Random rand = new Random();
    private double globalSway = 0;

    // Neon Styling Parameters
    private static final String COLOR_SHIP = "\u001B[38;5;255m"; // White/Red Fighter
    private static final String COLOR_LASER = "\u001B[38;5;51m"; // Blue Star Laser
    private static final String COLOR_BOMB = "\u001B[38;5;226m"; // Yellow Alien Shells
    private static final String COLOR_BEAM = "\u001B[38;5;201m"; // Magenta Tractor Beam Field

    private static final String[] ALIEN_COLORS = {
            "\u001B[38;5;46m", // Boss: Bright Green
            "\u001B[38;5;196m", // Guard: Red
            "\u001B[38;5;33m" // Drone: Blue
    };

    public GalagaLoader() {
        super(GALAGA_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        resetMatch();
    }

    private void resetMatch() {
        isDead = false;
        beamActive = false;
        playerX = WIDTH / 2.0;
        aliens.clear();
        bullets.clear();
        globalSway = 0;

        // Build the classic Galaga armada matrix grid near the upper display deck
        for (int row = 0; row < 4; row++) {
            int type = (row == 0) ? 0 : (row == 1) ? 1 : 2;
            for (int col = 0; col < 6; col++) {
                aliens.add(new Alien(4 + col * 3, 3 + row * 2, type));
            }
        }
        lastTickTime = System.currentTimeMillis();
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
            updateSimulationPhysics();
        }

        drawScene(outputBuffer);
    }

    private void updateSimulationPhysics() {
        globalSway += 0.05;
        double swayOffset = Math.sin(globalSway) * 1.2;

        // 1. Manage Swarm Matrices & Dive Launch Cycles
        for (Alien alien : aliens) {
            if (!alien.isDiving) {
                // Sway gently in passive flagship grid formation
                alien.x = alien.homeX + swayOffset;
                alien.y = alien.homeY;

                // Random trigger launches an unaligned diving run on our player
                if (rand.nextInt(675) == 81 && !beamActive) {
                    alien.isDiving = true;
                    alien.diveTimer = 0;
                }
            } else {
                // PATH PHYSICS: Execute a dynamic, curving sinusoidal dive down the screen
                alien.diveTimer += 0.06;
                alien.y += 0.4;
                alien.x = alien.homeX + Math.sin(alien.diveTimer * 4.0) * 4.5;

                // FIX: Boss halts 6 cells above player zone to deploy tractor fields safely
                if (alien.type == 0 && Math.round(alien.y) == playerY - 6 && !beamActive && rand.nextInt(10) > 7) {
                    beamActive = true;
                    beamX = (int) Math.round(alien.x);
                    beamTopY = (int) Math.round(alien.y) + 1;
                    beamTimer = 35; // Run tractor field for 35 simulation frames
                }

                // Loop element fallback back to upper assembly bounds if it sails past the
                // bottom
                if (alien.y >= HEIGHT) {
                    alien.y = 0;
                    alien.isDiving = false;
                }
            }

            // Verify lethal collision overlaps on contact intersections
            if (Math.abs(alien.x - playerX) < 1.2 && Math.round(alien.y) == playerY) {
                triggerDeath();
                return;
            }
        }

        // 2. Tractor Beam field handler
        if (beamActive) {
            beamTimer--;
            if (beamTimer <= 0) {
                beamActive = false;
            } else {
                // If player touches the tracking column width coordinates inside field bounds,
                // trigger abduction failure
                if (Math.round(playerX) == beamX) {
                    triggerDeath(); // Abducted ship triggers reset
                    return;
                }
            }
        }

        // 3. Autonomous Fighter AI Controller
        if (!aliens.isEmpty()) {
            // Priority target locking: Isolate active diving ships first
            Alien target = aliens.get(0);
            for (Alien al : aliens) {
                if (al.isDiving && al.y > target.y)
                    target = al;
            }
            if (!target.isDiving) {
                // If no divers exist, snap onto standard column assets
                for (Alien al : aliens) {
                    if (Math.abs(al.x - playerX) < Math.abs(target.x - playerX))
                        target = al;
                }
            }

            // Track horizontally towards target column index lanes safely
            if (playerX < target.x && playerX < WIDTH - 2)
                playerX += 0.6;
            if (playerX > target.x && playerX > 1)
                playerX -= 0.6;

            // Dodge diving kamikazes if they approach directly overhead
            if (target.isDiving && Math.round(target.y) > playerY - 4 && Math.abs(target.x - playerX) < 2) {
                if (playerX > WIDTH / 2.0)
                    playerX = Math.max(1, playerX - 1.5);
                else
                    playerX = Math.min(WIDTH - 2, playerX + 1.5);
            }

            // Return fire when locked underneath alien units
            if (Math.abs(playerX - target.x) <= 1.5 && countPlayerBullets() < 2 && rand.nextInt(10) > 6) {
                bullets.add(new Bullet(playerX, playerY - 1, false));
            }
        }

        // 4. Counter-Offensive: Diving combatants drop plasma shells downwards
        for (Alien alien : aliens) {
            if (alien.isDiving && rand.nextInt(100) > 97) {
                bullets.add(new Bullet(alien.x, alien.y + 1, true));
            }
        }

        // 5. Update Projectile Pools
        Iterator<Bullet> bIt = bullets.iterator();
        while (bIt.hasNext()) {
            Bullet b = bIt.next();
            if (b.isAlien) {
                b.y += 0.8; // Drop alien bomb downward
                if (b.y >= HEIGHT) {
                    bIt.remove();
                    continue;
                }

                if (Math.abs(b.x - playerX) < 1.2 && Math.round(b.y) == playerY) {
                    triggerDeath();
                    return;
                }
            } else {
                b.y -= 1.4; // Fire player weapon upward
                if (b.y < 0) {
                    bIt.remove();
                    continue;
                }

                // Check bullet collisions hitting alien grids
                for (Alien al : aliens) {
                    if (Math.abs(b.x - al.x) < 1.5 && Math.round(b.y) == Math.round(al.y)) {
                        al.active = false;
                        b.active = false;
                        break;
                    }
                }
                if (!b.active)
                    bIt.remove();
            }
        }

        aliens.removeIf(al -> !al.active);
        if (aliens.isEmpty())
            resetMatch();
    }

    private int countPlayerBullets() {
        int c = 0;
        for (Bullet b : bullets)
            if (!b.isAlien)
                c++;
        return c;
    }

    private void triggerDeath() {
        isDead = true;
        deathTimestamp = System.currentTimeMillis();
    }

    private void drawScene(String[] outputBuffer) {
        // Draw the vertical pulsing Tractor Beam overlay if engaged
        if (beamActive) {
            for (int y = beamTopY; y < playerY; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int bx = beamX + dx;
                    if (bx >= 0 && bx < WIDTH && y >= 0 && y < HEIGHT) {
                        outputBuffer[bx + y * WIDTH] = COLOR_BEAM + (dx == 0 ? "▒" : "░") + RESET;
                    }
                }
            }
        }
        // Draw dynamic scrolling background starfields to simulate space flight
        // acceleration
        int starIdx = (int) ((System.currentTimeMillis() / 150) % HEIGHT);
        for (int starY = 0; starY < HEIGHT; starY += 6) {
            int computedY = (starY + starIdx) % HEIGHT;
            int offset = computedY * WIDTH + ((starY * 7) % WIDTH);
            if (offset >= 0 && offset < SIZE && outputBuffer[offset].equals(" ")) {
                outputBuffer[offset] = "\u001B[38;5;237m·\u001B[0m";
            }
        }
        // Draw alien ship hulls
        for (Alien alien : aliens) {
            int ax = (int) Math.round(alien.x);
            int ay = (int) Math.round(alien.y);
            if (ax >= 0 && ax < WIDTH && ay >= 0 && ay < HEIGHT) {
                char glyph = (alien.type == 0) ? 'W' : (alien.type == 1) ? 'M' : 'V';
                outputBuffer[ax + ay * WIDTH] = ALIEN_COLORS[alien.type] + glyph + RESET;
            }
        }
        // Draw bullet indicators
        for (Bullet b : bullets) {
            int bx = (int) Math.round(b.x);
            int by = (int) Math.round(b.y);
            if (bx >= 0 && bx < WIDTH && by >= 0 && by < HEIGHT) {
                outputBuffer[bx + by * WIDTH] = b.isAlien ? COLOR_BOMB + "v" + RESET : COLOR_LASER + "╎" + RESET;
            }
        }
        // Draw player ship
        int px = (int) Math.round(playerX);
        if (px >= 1 && px < WIDTH - 1) {
            int pIdx = px + playerY * WIDTH;
            if (isDead) {
                outputBuffer[pIdx - 1] = "\u001B[31m💥";
                outputBuffer[pIdx] = " ";
                outputBuffer[pIdx + 1] = " " + RESET;
            } else {
                outputBuffer[pIdx - 1] = COLOR_SHIP + "┪";
                outputBuffer[pIdx] = "▲";
                outputBuffer[pIdx + 1] = "┪" + RESET;
            }
        }
    }
}