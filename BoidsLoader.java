import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoidsLoader extends Loader {
    private static final StatusStage[] FLOCK_STAGES = {
        new StatusStage(25, "Seeding position vectors:"),
        new StatusStage(50, "Injecting local neighbor radii:"),
        new StatusStage(75, "Syncing multi-agent steering forces:"),
        new StatusStage(100, "Boids Flocking Model Operational!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;
    private static final int NUM_BOIDS = 28; // Optimized for performance and readability

    private static class Boid {
        double x, y;
        double vx, vy;

        Boid(double x, double y, double vx, double vy) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
        }

        void wrap() {
            if (x < 0) x += WIDTH;
            if (x >= WIDTH) x -= WIDTH;
            if (y < 0) y += HEIGHT;
            if (y >= HEIGHT) y -= HEIGHT;
        }
    }

    private final List<Boid> boids = new ArrayList<>();
    private long lastTickTime = 0;
    private static final long FRAME_DELAY_MS = 33; // Smooth ~30 FPS tracking
    private final Random rand = new Random();

    // Visual Palette Profile
    private static final String COLOR_BOID = "\u001B[38;5;81m"; // Electric Cyan Boids
    private static final String BACKGROUND_DOT = "\u001B[38;5;234m·\u001B[0m";

    public BoidsLoader() {
        super(FLOCK_STAGES);
    }

    @Override
    protected void initialize() {
        boids.clear();
        for (int i = 0; i < NUM_BOIDS; i++) {
            boids.add(new Boid(
                rand.nextDouble() * WIDTH,
                rand.nextDouble() * HEIGHT,
                (rand.nextDouble() * 2.0 - 1.0) * 0.5,
                (rand.nextDouble() * 2.0 - 1.0) * 0.25
            ));
        }
        lastTickTime = System.currentTimeMillis();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTickTime >= FRAME_DELAY_MS) {
            lastTickTime = currentTime;
            updateFlockPhysics();
        }

        drawScene(outputBuffer);
    }

    private void updateFlockPhysics() {
        // Distance parameters for localized neighbor evaluations
        double visualRange = 12.0;
        double protectedRange = 2.5;

        // Steering weight scales
        double separationWeight = 0.15;
        double alignmentWeight = 0.05;
        double cohesionWeight = 0.01;
        double speedLimit = 0.6;

        for (Boid boid : boids) {
            double closeDx = 0, closeDy = 0;
            double avgVx = 0, avgVy = 0;
            double avgX = 0, avgY = 0;
            int neighborsCount = 0;

            for (Boid other : boids) {
                if (boid == other) continue;

                // Handle toroidal distance tracking across wrapping boundaries
                double dx = other.x - boid.x;
                if (dx > WIDTH / 2.0) dx -= WIDTH;
                else if (dx < -WIDTH / 2.0) dx += WIDTH;

                double dy = other.y - boid.y;
                if (dy > HEIGHT / 2.0) dy -= HEIGHT;
                else if (dy < -HEIGHT / 2.0) dy += HEIGHT;

                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < visualRange) {
                    // Separation checklist
                    if (distance < protectedRange) {
                        closeDx += (boid.x - other.x);
                        closeDy += (boid.y - other.y);
                    }

                    // Alignment & Cohesion accumulation fields
                    avgVx += other.vx;
                    avgVy += other.vy;
                    avgX += boid.x + dx; // Use toroidal adjusted offset positions
                    avgY += boid.y + dy;
                    neighborsCount++;
                }
            }

            // Apply Reynolds' Three Rules of Steering
            if (neighborsCount > 0) {
                avgVx /= neighborsCount;
                avgVy /= neighborsCount;
                avgX /= neighborsCount;
                avgY /= neighborsCount;

                // Alignment: Turn towards the group vector speed
                boid.vx += (avgVx - boid.vx) * alignmentWeight;
                boid.vy += (avgVy - boid.vy) * alignmentWeight;

                // Cohesion: Turn towards the group center of mass
                boid.vx += (avgX - boid.x) * cohesionWeight;
                boid.vy += (avgY - boid.y) * cohesionWeight;
            }

            // Separation: Evade nearby neighbors aggressively to avoid stacking crashes
            boid.vx += closeDx * separationWeight;
            boid.vy += closeDy * separationWeight;

            // Enforce maximum structural speed clamping boundaries
            double speed = Math.sqrt(boid.vx * boid.vx + boid.vy * boid.vy);
            if (speed > speedLimit) {
                boid.vx = (boid.vx / speed) * speedLimit;
                boid.vy = (boid.vy / speed) * speedLimit;
            }

            // Translate positions and wrap boundaries seamlessly
            boid.x += boid.vx;
            boid.y += boid.vy;
            boid.wrap();
        }
    }

    private void drawScene(String[] outputBuffer) {
        // Seed blank backdrop fields safely
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            outputBuffer[i] = BACKGROUND_DOT;
        }

        // Rasterize active boid agents mapping headings directly to text directional arrows
        for (Boid b : boids) {
            int bx = (int) Math.round(b.x);
            int by = (int) Math.round(b.y);

            // Safety guard clamp bounds matching matrix properties
            if (bx >= 0 && bx < WIDTH && by >= 0 && by < HEIGHT) {
                double angle = Math.atan2(b.vy, b.vx) * 180.0 / Math.PI;
                if (angle < 0) angle += 360.0;

                char glyph = '►'; // Default right heading pointer
                if (angle >= 337.5 || angle < 22.5)    glyph = '►';
                else if (angle >= 22.5 && angle < 67.5)   glyph = '◢';
                else if (angle >= 67.5 && angle < 112.5)  glyph = '▼';
                else if (angle >= 112.5 && angle < 157.5) glyph = '◣';
                else if (angle >= 157.5 && angle < 202.5) glyph = '◄';
                else if (angle >= 202.5 && angle < 247.5) glyph = '◤';
                else if (angle >= 247.5 && angle < 292.5) glyph = '▲';
                else if (angle >= 292.5 && angle < 337.5) glyph = '◥';

                outputBuffer[bx + by * WIDTH] = COLOR_BOID + glyph + RESET;
            }
        }
    }
}
