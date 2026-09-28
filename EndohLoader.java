// TODO: Fix This

// Homage to Yusuke Endoh's "endoh1.c" (Most Complex ASCII fluid, IOCCC 2012):
// a smoothed-particle-hydrodynamics (SPH) fluid solver whose only display
// surface is the terminal grid itself. The real thing shipped a handful of
// named "configuration files" (column.txt, pour-out.txt, fountain.txt,
// tanada.txt/terraces, and its own logo.txt) that seeded the simulation in
// different starting shapes before letting the physics take over - this
// loader borrows that idea directly: initialize() rolls one of five such
// starting layouts every time the loader is created.
public class EndohLoader extends Loader {

    private static final StatusStage[] ENDOH_STAGES = {
            new StatusStage(15, "Seeding particle field:"),
            new StatusStage(40, "Computing SPH density kernels:"),
            new StatusStage(65, "Resolving pressure gradients:"),
            new StatusStage(85, "Damping viscous turbulence:"),
            new StatusStage(100, "Fluid Simulation Stable!")
    };

    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;
    private static final int MAX_PARTICLES = 450;

    // --- SPH tuning constants (tuned by eye for a pleasant terminal-scale
    // fluid, not physically dimensioned units) ---
    private static final double SMOOTHING_RADIUS = 1.75;
    private static final double REST_DENSITY = 3.6;
    private static final double STIFFNESS = 11.0;
    private static final double VISCOSITY = 0.85;
    private static final double GRAVITY = 11.0;
    private static final double DT = 0.05;
    private static final double WALL_DAMPING = 0.5;
    private static final double MAX_SPEED = 18.0;

    // A gentle, endless drip keeps the pool alive instead of settling into
    // a static puddle once a task runs longer than the initial splash.
    private static final long DROPLET_INTERVAL_MS = 900;

    // Flat particle arrays (no per-frame allocation/GC churn at 60fps)
    private final double[] px = new double[MAX_PARTICLES];
    private final double[] py = new double[MAX_PARTICLES];
    private final double[] vx = new double[MAX_PARTICLES];
    private final double[] vy = new double[MAX_PARTICLES];
    private final double[] fx = new double[MAX_PARTICLES];
    private final double[] fy = new double[MAX_PARTICLES];
    private final double[] rho = new double[MAX_PARTICLES];
    private final double[] pres = new double[MAX_PARTICLES];
    private int particleCount;

    // Per-cell aggregation scratch buffers, rebuilt every frame
    private final double[] cellDensity = new double[WIDTH * HEIGHT];
    private final double[] cellSpeed = new double[WIDTH * HEIGHT];
    private final int[] cellCount = new int[WIDTH * HEIGHT];

    private boolean vibrationFrame = false;
    private long lastDropletTime = System.currentTimeMillis();

    // Water palette, deep body -> airborne spray
    private static final String C_DEEP    = "\u001B[38;5;18m";
    private static final String C_BODY    = "\u001B[38;5;25m";
    private static final String C_SURFACE = "\u001B[38;5;39m";
    private static final String C_FOAM    = "\u001B[38;5;159m";
    private static final String C_SPRAY   = "\u001B[38;5;255m";

    public EndohLoader() {
        // Endoh's original wants an 80x25-ish terminal; this framework's
        // other set-geometry loaders standardize on 80x22.
        super(ENDOH_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        lastDropletTime = System.currentTimeMillis();
        int variant = (int) (Math.random() * 5);
        switch (variant) {
            case 0: initColumn(); break;
            case 1: initPourOut(); break;
            case 2: initFountain(); break;
            case 3: initTerraces(); break;
            default: initLogo(); break;
        }
    }

    // 0. "column" - a tall, tightly packed dam-break block that collapses
    // outward and spreads into a puddle across the floor.
    private void initColumn() {
        particleCount = 0;
        for (double y = 4; y < 14 && particleCount < MAX_PARTICLES; y += 0.65) {
            for (double x = 32; x < 48 && particleCount < MAX_PARTICLES; x += 0.65) {
                spawn(x + jitter(), y + jitter(), 0, 0);
            }
        }
    }

    // 1. "pour-out" - a narrow stream already falling from a spout near
    // the top, landing into an empty basin below.
    private void initPourOut() {
        particleCount = 0;
        for (double y = 1; y < 8 && particleCount < MAX_PARTICLES; y += 0.4) {
            for (double x = 36; x < 44 && particleCount < MAX_PARTICLES; x += 0.5) {
                spawn(x + jitter(), y + jitter(), 0.4, 3.0);
            }
        }
    }

    // 2. "fountain" - a burst launched upward from a nozzle at the
    // bottom-center that arcs back down under gravity.
    private void initFountain() {
        particleCount = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            double angle = (Math.PI / 2.2) + (Math.random() - 0.5) * (Math.PI / 3.5);
            double speed = 9.0 + Math.random() * 5.0;
            spawn(40 + jitter() * 2, 19 + jitter(),
                    Math.cos(angle) * speed * 0.4, -Math.sin(angle) * speed);
        }
    }

    // 3. "tanada" (terraced rice fields) - pools resting on stepped
    // shelves that spill from level to level.
    private void initTerraces() {
        particleCount = 0;
        int[] shelfTop = { 4, 9, 14 };
        int[] shelfLeft = { 8, 28, 48 };
        int[] shelfRight = { 26, 46, 72 };
        for (int s = 0; s < shelfTop.length; s++) {
            for (double y = shelfTop[s]; y < shelfTop[s] + 3 && particleCount < MAX_PARTICLES; y += 0.5) {
                for (double x = shelfLeft[s]; x < shelfRight[s] && particleCount < MAX_PARTICLES; x += 0.5) {
                    spawn(x + jitter(), y + jitter(), 0, 0);
                }
            }
        }
    }

    // 4. "logo" - particles seeded along a simple blocky glyph that loses
    // its form immediately and melts into a puddle, the way Endoh's own
    // logo.txt configuration did.
    private void initLogo() {
        particleCount = 0;
        String[] glyph = {
                "###  ###   #  #",
                "#  # #     #  #",
                "###  ###   ####",
                "#    #     #  #",
                "#    ###   #  #"
        };
        double startX = 28, startY = 4, cellW = 1.1, cellH = 1.4;
        for (int r = 0; r < glyph.length; r++) {
            String row = glyph[r];
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) != '#') continue;
                double bx = startX + c * cellW;
                double by = startY + r * cellH;
                // Pack a small jittered cluster into each filled glyph cell
                // so the letters read as a solid mass instead of a sparse
                // wire outline once rendered.
                for (int k = 0; k < 6 && particleCount < MAX_PARTICLES; k++) {
                    spawn(bx + jitter(), by + jitter(), 0, 0);
                }
            }
        }
    }

    private static double jitter() {
        return (Math.random() - 0.5) * 0.3;
    }

    private void spawn(double x, double y, double vx0, double vy0) {
        if (particleCount >= MAX_PARTICLES) return;
        px[particleCount] = x;
        py[particleCount] = y;
        vx[particleCount] = vx0;
        vy[particleCount] = vy0;
        particleCount++;
    }

    // Recycles a slot (growing the active set first, then picking a random
    // existing particle once we're at capacity) into a fresh raindrop
    // falling from the top. Keeps the surface perpetually rippling instead
    // of ever fully settling, no matter how long the real task runs.
    private void injectDroplet() {
        int idx = (particleCount < MAX_PARTICLES) ? particleCount++ : (int) (Math.random() * MAX_PARTICLES);
        px[idx] = 4 + Math.random() * (WIDTH - 8);
        py[idx] = 0;
        vx[idx] = (Math.random() - 0.5) * 1.5;
        vy[idx] = 2.0 + Math.random() * 2.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        vibrationFrame = !vibrationFrame;

        long now = System.currentTimeMillis();
        if (now - lastDropletTime > DROPLET_INTERVAL_MS) {
            lastDropletTime = now;
            injectDroplet();
        }

        computeDensityAndPressure();
        computeForces();
        integrate();

        java.util.Arrays.fill(cellDensity, 0);
        java.util.Arrays.fill(cellSpeed, 0);
        java.util.Arrays.fill(cellCount, 0);

        for (int i = 0; i < particleCount; i++) {
            int cx = (int) Math.round(px[i]);
            int cy = (int) Math.round(py[i]);
            if (cx < 0 || cx >= WIDTH || cy < 0 || cy >= HEIGHT) continue;

            int o = cx + WIDTH * cy;
            cellDensity[o] += rho[i];
            cellSpeed[o] += Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i]);
            cellCount[o]++;
        }

        for (int o = 0; o < cellCount.length; o++) {
            int n = cellCount[o];
            if (n == 0) continue;

            double avgDensity = cellDensity[o] / n;
            double avgSpeed = cellSpeed[o] / n;

            String color;
            char glyph;

            if (n == 1 && avgDensity < REST_DENSITY * 0.5) {
                // Lone airborne droplet / spray
                color = C_SPRAY;
                glyph = avgSpeed > 4.0 ? '\'' : '.';
            } else if (avgDensity < REST_DENSITY * 0.75) {
                // Thin, agitated surface - vibrates between the two slashes
                // the way the real endoh1 renders a turbulent skin.
                color = C_FOAM;
                glyph = avgSpeed > 3.0 ? (vibrationFrame ? '/' : '\\') : '-';
            } else if (avgDensity < REST_DENSITY * 1.15) {
                color = C_SURFACE;
                glyph = avgSpeed > 3.0 ? '=' : '~';
            } else if (avgDensity < REST_DENSITY * 1.6) {
                color = C_BODY;
                glyph = '#';
            } else {
                color = C_DEEP;
                glyph = '@';
            }

            outputBuffer[o] = color + glyph + RESET;
        }
    }

    // SPH density estimate: rho_i = sum_j m * W(r_ij, h), using a cheap
    // cubic falloff kernel instead of the classical poly6/spiky pair - the
    // exact kernel shape doesn't matter for a decorative sim, only that it
    // is smooth, falls to zero at h, and is cheap per pair.
    private void computeDensityAndPressure() {
        double h = SMOOTHING_RADIUS;
        for (int i = 0; i < particleCount; i++) {
            double sum = 0;
            for (int j = 0; j < particleCount; j++) {
                double dx = px[i] - px[j];
                double dy = py[i] - py[j];
                double r2 = dx * dx + dy * dy;
                if (r2 < h * h) {
                    double q = 1.0 - Math.sqrt(r2) / h;
                    sum += q * q * q;
                }
            }
            rho[i] = Math.max(sum, 0.0001);
            pres[i] = STIFFNESS * Math.max(0.0, rho[i] - REST_DENSITY);
        }
    }

    // Pairwise pressure (push neighbors apart when over-compressed) and
    // viscosity (pull velocities toward nearby particles, damping jitter).
    // Newton's third law lets each pair be visited once.
    private void computeForces() {
        double h = SMOOTHING_RADIUS;
        java.util.Arrays.fill(fx, 0, particleCount, 0.0);
        java.util.Arrays.fill(fy, 0, particleCount, 0.0);

        for (int i = 0; i < particleCount; i++) {
            for (int j = i + 1; j < particleCount; j++) {
                double dx = px[i] - px[j];
                double dy = py[i] - py[j];
                double r2 = dx * dx + dy * dy;
                if (r2 >= h * h || r2 < 1e-9) continue;

                double r = Math.sqrt(r2);
                double q = 1.0 - r / h;
                double nx = dx / r, ny = dy / r;

                double pTerm = (pres[i] + pres[j]) / (2.0 * rho[j]);
                double pForce = pTerm * q * q;
                fx[i] += nx * pForce;
                fy[i] += ny * pForce;
                fx[j] -= nx * pForce;
                fy[j] -= ny * pForce;

                double dvx = vx[j] - vx[i];
                double dvy = vy[j] - vy[i];
                double visc = VISCOSITY * q / rho[j];
                fx[i] += dvx * visc;
                fy[i] += dvy * visc;
                fx[j] -= dvx * visc;
                fy[j] -= dvy * visc;
            }
        }
    }

    private void integrate() {
        for (int i = 0; i < particleCount; i++) {
            // Pressure/viscosity are forces (divide by density for
            // acceleration); gravity is already an acceleration, so it
            // skips that division - otherwise dense particles would fall
            // slower than light ones, which looks wrong.
            vx[i] += (fx[i] / rho[i]) * DT;
            vy[i] += (fy[i] / rho[i] + GRAVITY) * DT;

            double speed = Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i]);
            if (speed > MAX_SPEED) {
                double scale = MAX_SPEED / speed;
                vx[i] *= scale;
                vy[i] *= scale;
            }

            px[i] += vx[i] * DT;
            py[i] += vy[i] * DT;

            // Floor and side walls reflect with damping. The ceiling is
            // left open so fountain/pour-out jets can rise above the
            // visible grid before gravity pulls them back down.
            if (py[i] > HEIGHT - 1) {
                py[i] = HEIGHT - 1;
                vy[i] *= -WALL_DAMPING;
            }
            if (px[i] < 0) {
                px[i] = 0;
                vx[i] *= -WALL_DAMPING;
            } else if (px[i] > WIDTH - 1) {
                px[i] = WIDTH - 1;
                vx[i] *= -WALL_DAMPING;
            }
        }
    }
}