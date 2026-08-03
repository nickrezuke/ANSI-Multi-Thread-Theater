import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AsteroidsLoader extends Loader {
    private static final StatusStage[] ACTION_STAGES = {
            new StatusStage(25, "Vector physics pipeline booting:"),
            new StatusStage(50, "Generating rocky asteroid fields:"),
            new StatusStage(75, "Syncing autonomous steering arrays:"),
            new StatusStage(100, "Asteroids Vector Arena Operational!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // 1. Structural Component Models
    private static class VectorObject {
        double x, y;
        double vx, vy;
        boolean active = true;

        VectorObject(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        void updatePosition() {
            x += vx;
            y += vy;
            // Toroidal Space wrapping mechanics along both axes
            if (x < 0)
                x += WIDTH;
            if (x >= WIDTH)
                x -= WIDTH;
            if (y < 0)
                y += HEIGHT;
            if (y >= HEIGHT)
                y -= HEIGHT;
        }
    }

    private static class Asteroid extends VectorObject {
        int size; // 3 = Large, 2 = Medium, 1 = Small

        Asteroid(double x, double y, double vx, double vy, int size) {
            super(x, y, vx, vy);
            this.size = size;
        }
    }

    // 2. Physics Simulation State Pools
    private double shipX, shipY;
    private double shipVx, shipVy;
    private double shipAngle; // Radians direction

    private final List<Asteroid> asteroids = new ArrayList<>();
    private final List<VectorObject> lasers = new ArrayList<>();

    private boolean isDead = false;
    private long lastTickTime = 0;
    private static final long FRAME_DELAY_MS = 33; // Running steady at ~30 FPS
    private long deathTimestamp = 0;
    private final Random rand = new Random();

    // 3. Stylized Color Hues
    private static final String COLOR_SHIP = "\u001B[38;5;46m"; // Neon Green Ship
    private static final String COLOR_LASER = "\u001B[38;5;196m"; // Laser Red
    private static final String COLOR_ROCK = "\u001B[38;5;246m"; // Gray Asteroid

    public AsteroidsLoader() {
        super(ACTION_STAGES);
    }

    @Override
    protected void initialize() {
        resetMatch();
    }

    private void resetMatch() {
        isDead = false;
        shipX = WIDTH / 2.0;
        shipY = HEIGHT / 2.0;
        shipVx = 0;
        shipVy = 0;
        shipAngle = 0;

        asteroids.clear();
        lasers.clear();

        // Populate baseline large drifting rocks safely away from the center spawn
        // point
        for (int i = 0; i < 5; i++) {
            double ax = rand.nextBoolean() ? rand.nextDouble() * 20 : WIDTH - rand.nextDouble() * 20;
            double ay = rand.nextBoolean() ? rand.nextDouble() * 6 : HEIGHT - rand.nextDouble() * 6;
            double avx = (rand.nextDouble() * 2.0 - 1.0) * 0.25;
            double avy = (rand.nextDouble() * 2.0 - 1.0) * 0.12;
            asteroids.add(new Asteroid(ax, ay, avx, avy, 3));
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

        if (currentTime - lastTickTime >= FRAME_DELAY_MS) {
            lastTickTime = currentTime;
            updateSimulationPhysics();
        }

        drawScene(outputBuffer);
    }

    private void updateSimulationPhysics() {
        // 1. Object Positional updates
        shipX += shipVx;
        shipY += shipVy;
        if (shipX < 0)
            shipX += WIDTH;
        if (shipX >= WIDTH)
            shipX -= WIDTH;
        if (shipY < 0)
            shipY += HEIGHT;
        if (shipY >= HEIGHT)
            shipY -= HEIGHT;

        // Apply constant friction dampening drag loss to space vacuum physics
        shipVx *= 0.98;
        shipVy *= 0.98;

        for (Asteroid ast : asteroids)
            ast.updatePosition();

        Iterator<VectorObject> laserIt = lasers.iterator();
        while (laserIt.hasNext()) {
            VectorObject laser = laserIt.next();
            laser.updatePosition();

            // Lasers decay quickly over time to force close-quarters encounters
            laser.vx *= 0.97;
            laser.vy *= 0.97;
            if (Math.abs(laser.vx) + Math.abs(laser.vy) < 0.2) {
                laserIt.remove();
            }
        }

        // 2. Automated Steering and Tactical Targeting AI Decision Tree
        if (!asteroids.isEmpty()) {
            // Target the absolute closest rock threat vector profile
            Asteroid target = asteroids.get(0);
            double minDist = Double.MAX_VALUE;
            for (Asteroid ast : asteroids) {
                double d = Math.pow(ast.x - shipX, 2) + Math.pow(ast.y - shipY, 2);
                if (d < minDist) {
                    minDist = d;
                    target = ast;
                }
            }

            // Calculate exact target alignment intercept angles
            double angleToTarget = Math.atan2(target.y - shipY, target.x - shipX);

            // Adjust angular delta alignment updates incrementally
            double angleDiff = angleToTarget - shipAngle;
            while (angleDiff < -Math.PI)
                angleDiff += Math.PI * 2;
            while (angleDiff > Math.PI)
                angleDiff -= Math.PI * 2;

            if (angleDiff > 0.1)
                shipAngle += 0.15;
            else if (angleDiff < -0.1)
                shipAngle -= 0.15;

            // Adaptive Thrust/Weapon Control loop
            if (minDist > 250.0 && rand.nextInt(10) > 7) {
                // Apply acceleration vector thrust modifications facing target direction
                shipVx += Math.cos(shipAngle) * 0.06;
                shipVy += Math.sin(shipAngle) * 0.03;
            }

            // Weapon Fire trigger: discharge beams when locked perfectly down alignment
            // axes
            if (Math.abs(angleDiff) <= 0.25 && lasers.size() < 4 && rand.nextInt(10) > 6) {
                lasers.add(new VectorObject(shipX, shipY, Math.cos(shipAngle) * 1.1, Math.sin(shipAngle) * 0.55));
            }
        }

        // 3. Collision Intersection Sweeper Loops
        // Laser vs Asteroid
        List<Asteroid> splitQueue = new ArrayList<>();
        for (VectorObject laser : lasers) {
            for (Asteroid ast : asteroids) {
                if (!ast.active || !laser.active)
                    continue;

                double radiusCheck = ast.size == 3 ? 3.0 : ast.size == 2 ? 1.8 : 1.0;
                if (Math.abs(laser.x - ast.x) < radiusCheck && Math.abs(laser.y - ast.y) < radiusCheck / 2.0) {
                    ast.active = false;
                    laser.active = false;

                    // Fracture mechanics: Split larger boulders down into fragments
                    if (ast.size > 1) {
                        for (int k = 0; k < 2; k++) {
                            double nvx = ast.vx + (rand.nextDouble() * 2.0 - 1.0) * 0.15;
                            double nvy = ast.vy + (rand.nextDouble() * 2.0 - 1.0) * 0.08;
                            splitQueue.add(new Asteroid(ast.x, ast.y, nvx, nvy, ast.size - 1));
                        }
                    }
                }
            }
        }
        asteroids.removeIf(ast -> !ast.active);
        lasers.removeIf(l -> !l.active);
        asteroids.addAll(splitQueue);

        // Ship vs Asteroid Collision Failure Listener
        for (Asteroid ast : asteroids) {
            double hitRadius = ast.size == 3 ? 2.5 : ast.size == 2 ? 1.5 : 0.8;
            if (Math.abs(shipX - ast.x) < hitRadius && Math.abs(shipY - ast.y) < hitRadius / 2.0) {
                isDead = true;
                deathTimestamp = System.currentTimeMillis();
                break;
            }
        }

        // Field cleared loop: Respawn fresh wave variations seamlessly
        if (asteroids.isEmpty()) {
            resetMatch();
        }
    }

    private void drawScene(String[] outputBuffer) {
        // 1. Rasterize Drifting Asteroid Field Blobs
        for (Asteroid ast : asteroids) {
            int ax = (int) Math.round(ast.x);
            int ay = (int) Math.round(ast.y);

            // Shape dimensions scale dynamically matching piece size metadata properties
            int radius = ast.size == 3 ? 2 : ast.size == 2 ? 1 : 0;

            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius * 2; dx <= radius * 2; dx++) {
                    int rx = ax + dx;
                    int ry = ay + dy;

                    // Simple radial roundness mask filter evaluation
                    if (dx * dx + dy * dy * 4 <= radius * radius * 4) {
                        if (rx >= 0 && rx < WIDTH && ry >= 0 && ry < HEIGHT) {
                            outputBuffer[rx + ry * WIDTH] = COLOR_ROCK + "░" + RESET;
                        }
                    }
                }
            }
        }

        // 2. Rasterize Laser Beams
        for (VectorObject laser : lasers) {
            int lx = (int) Math.round(laser.x);
            int ly = (int) Math.round(laser.y);
            if (lx >= 0 && lx < WIDTH && ly >= 0 && ly < HEIGHT) {
                outputBuffer[lx + ly * WIDTH] = COLOR_LASER + "·" + RESET;
            }
        }

        // 3. Rasterize Vector Ship Pointer Arrow
        int sx = (int) Math.round(shipX);
        int sy = (int) Math.round(shipY);
        int shipIdx = sx + sy * WIDTH;

        if (sx >= 0 && sx < WIDTH && sy >= 0 && sy < HEIGHT) {
            if (isDead) {
                // Flash debris boom layout on crash frames
                if (shipIdx - 1 >= 0)
                    outputBuffer[shipIdx - 1] = "\u001B[31m*";
                outputBuffer[shipIdx] = "X";
                if (shipIdx + 1 < outputBuffer.length)
                    outputBuffer[shipIdx + 1] = "*\u001B[0m";
            } else {
                // Determine pointer direction symbols based on rotation angles
                double deg = (shipAngle * 180.0 / Math.PI) + 180.0;
                while (deg < 0)
                    deg += 360;
                deg = deg % 360;
                char glyph = '▲';
                // Default facing up
                if (deg >= 337.5 || deg < 22.5)
                    glyph = '◄';
                else if (deg >= 22.5 && deg < 67.5)
                    glyph = '◤';
                else if (deg >= 67.5 && deg < 112.5)
                    glyph = '▲';
                else if (deg >= 112.5 && deg < 157.5)
                    glyph = '◥';
                else if (deg >= 157.5 && deg < 202.5)
                    glyph = '►';
                else if (deg >= 202.5 && deg < 247.5)
                    glyph = '◢';
                else if (deg >= 247.5 && deg < 292.5)
                    glyph = '▼';
                else if (deg >= 292.5 && deg < 337.5)
                    glyph = '◣';
                outputBuffer[shipIdx] = COLOR_SHIP + glyph + RESET;
            }
        }
    }
}