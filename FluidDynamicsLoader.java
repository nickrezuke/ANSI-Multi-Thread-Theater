// TODO: Fix this Navier-Stokes Simulator (SUPER SLOW)
public class FluidDynamicsLoader extends Loader {
    private static final StatusStage[] FLUID_STAGES = {
        new StatusStage(25, "Initializing vector field grids:"),
        new StatusStage(50, "Allocating velocity and density buffers:"),
        new StatusStage(75, "Configuring Jacobi pressure solvers:"),
        new StatusStage(100, "Fluid Core Stable!")
    };

    private static final char[] DENSITY_RAMP = { ' ', '░', '▒', '▓', '█' };

    // Explicit dimensions matching your narrow layout requirements
    private static final int WIDTH = 100;
    private static final int HEIGHT = 38;
    private static final int SIZE = WIDTH * HEIGHT;

    // Fluid state arrays (Row-Major: index = x + y * WIDTH)
    private final double[] density = new double[SIZE];
    private final double[] dPrev = new double[SIZE];
    private final double[] u = new double[SIZE];     // X velocity vector
    private final double[] uPrev = new double[SIZE];
    private final double[] v = new double[SIZE];     // Y velocity vector
    private final double[] vPrev = new double[SIZE];

    private final double dt = 0.1;       // Stable physics step interval
    private final double diff = 0.0001;  // Molecular smoke diffusion rate
    private final double visc = 0.0001;  // Fluid viscosity rate
    private double frameTimer = 0.0;

    public FluidDynamicsLoader() {
        // Enforce structural vertical window scaling constraints to the superclass constructor
        super(FLUID_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        for (int i = 0; i < SIZE; i++) {
            density[i] = dPrev[i] = u[i] = uPrev[i] = v[i] = vPrev[i] = 0.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        frameTimer += dt;

        // 1. Source Injection: Continuously force hot up-welling velocity vectors
        injectSources();

        // 2. Velocity Solver: Resolve Navier-Stokes conservation of mass and momentum
        diffuse(1, uPrev, u, visc);
        diffuse(2, vPrev, v, visc);
        project(uPrev, vPrev, u, v);
        
        advect(1, u, uPrev, uPrev, vPrev);
        advect(2, v, vPrev, uPrev, vPrev);
        project(u, v, uPrev, vPrev);

        // 3. Scalar Density Solver: Advance dye/smoke propagation across our vector grid
        diffuse(0, dPrev, density, diff);
        advect(0, density, dPrev, u, v);

        // 4. Render Layout Processor: Flatten values into standard row-major output buffers
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int idx = x + y * WIDTH;
                double d = density[idx];

                // Scale kinetic energy tracking metrics via velocity magnitude scalars
                double velMag = Math.sqrt(u[idx] * u[idx] + v[idx] * v[idx]);

                int shadeIndex = (int) (d * (DENSITY_RAMP.length - 1));
                shadeIndex = Math.max(0, Math.min(DENSITY_RAMP.length - 1, shadeIndex));
                char renderChar = DENSITY_RAMP[shadeIndex];

                // Thermodynamic gradient layout: High kinetic energy flashes red, dense smoke shines cyan/white
                int r = (int) Math.min(255, velMag * 1200);
                int g = (int) Math.min(255, d * 150 + velMag * 350);
                int b = (int) Math.min(255, 180 + d * 75);
                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);

                outputBuffer[idx] = colorCode + renderChar + RESET;
            }
        }

        // Wipe baseline density history state to accept fresh delta frame updates smoothly
        System.arraycopy(new double[SIZE], 0, dPrev, 0, SIZE);
    }

    private void injectSources() {
        // Classic Plume Architecture: Position a source at the bottom center of the vertical column
        int srcX = WIDTH / 2;
        int srcY = HEIGHT - 4; // Floating slightly above the lower edge floor boundary
        
        // Upward pushing velocity vector with an active horizontal oscillation frequency
        double forceY = -5.0; // Negative pushes up along the standard row-major grid
        double forceX = Math.sin(frameTimer * 1.2) * 3.0; // Dynamic harmonic sway

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = srcX + dx;
                int ny = srcY + dy;
                
                if (nx > 0 && nx < WIDTH - 1 && ny > 0 && ny < HEIGHT - 1) {
                    int idx = nx + ny * WIDTH;
                    density[idx] = 1.0; // Continuous max density emission
                    u[idx] = forceX;
                    v[idx] = forceY;
                }
            }
        }
    }

    private void diffuse(int b, double[] x, double[] x0, double diffRate) {
        double a = dt * diffRate * WIDTH * HEIGHT;
        linearSolve(b, x, x0, a, 1 + 4 * a);
    }

    private void linearSolve(int b, double[] x, double[] x0, double a, double c) {
        // Run 20 Jacobi relaxation iterations to converge incompressible pressure fields
        for (int k = 0; k < 20; k++) {
            for (int j = 1; j < HEIGHT - 1; j++) {
                for (int i = 1; i < WIDTH - 1; i++) {
                    int idx = i + j * WIDTH;
                    x[idx] = (x0[idx] + a * (
                        x[(i - 1) + j * WIDTH] + x[(i + 1) + j * WIDTH] +
                        x[i + (j - 1) * WIDTH] + x[i + (j + 1) * WIDTH]
                    )) / c;
                }
            }
            setBounds(b, x);
        }
    }

    private void advect(int b, double[] d, double[] d0, double[] velU, double[] velV) {
        double dt0x = dt * WIDTH;
        double dt0y = dt * HEIGHT;

        for (int j = 1; j < HEIGHT - 1; j++) {
            for (int i = 1; i < WIDTH - 1; i++) {
                int idx = i + j * WIDTH;
                
                // Track current particles backwards down historical velocity paths
                double x = i - dt0x * velU[idx];
                double y = j - dt0y * velV[idx];

                // Safety guard rails: Clamp limits inside wall boundaries
                if (x < 0.5) x = 0.5;
                if (x > WIDTH - 1.5) x = WIDTH - 1.5;
                if (y < 0.5) y = 0.5;
                if (y > HEIGHT - 1.5) y = HEIGHT - 1.5;

                int i0 = (int) x; int i1 = i0 + 1;
                int j0 = (int) y; int j1 = j0 + 1;

                double s1 = x - i0; double s0 = 1.0 - s1;
                double t1 = y - j0; double t0 = 1.0 - t1;

                // Bilinear mapping function interpolating surrounding cell metrics
                d[idx] = s0 * (t0 * d0[i0 + j0 * WIDTH] + t1 * d0[i0 + j1 * WIDTH]) +
                         s1 * (t0 * d0[i1 + j0 * WIDTH] + t1 * d0[i1 + j1 * WIDTH]);
            }
        }
        setBounds(b, d);
    }

    private void project(double[] velU, double[] velV, double[] p, double[] div) {
        // Enforce mass conservation principles (Incompressibility solver)
        for (int j = 1; j < HEIGHT - 1; j++) {
            for (int i = 1; i < WIDTH - 1; i++) {
                int idx = i + j * WIDTH;
                div[idx] = -0.5 * (
                    velU[(i + 1) + j * WIDTH] - velU[(i - 1) + j * WIDTH] +
                    velV[i + (j + 1) * WIDTH] - velV[i + (j - 1) * WIDTH]
                ) / Math.max(WIDTH, HEIGHT);
                p[idx] = 0;
            }
        }
        setBounds(0, div);
        setBounds(0, p);
        linearSolve(0, p, div, 1, 4);

        // Subtract pressure gradients back out to form clean swirl vectors
        for (int j = 1; j < HEIGHT - 1; j++) {
            for (int i = 1; i < WIDTH - 1; i++) {
                int idx = i + j * WIDTH;
                velU[idx] -= 0.5 * Math.max(WIDTH, HEIGHT) * (p[(i + 1) + j * WIDTH] - p[(i - 1) + j * WIDTH]);
                velV[idx] -= 0.5 * Math.max(WIDTH, HEIGHT) * (p[i + (j + 1) * WIDTH] - p[i + (j - 1) * WIDTH]);
            }
        }
        setBounds(1, velU);
        setBounds(2, velV);
    }

    private void setBounds(int b, double[] x) {
        // Vector reflections keeping velocities contained cleanly inside boundaries
        for (int i = 1; i < WIDTH - 1; i++) {
            x[i + 0 * WIDTH] = b == 2 ? -x[i + 1 * WIDTH] : x[i + 1 * WIDTH];
            x[i + (HEIGHT - 1) * WIDTH] = b == 2 ? -x[i + (HEIGHT - 2) * WIDTH] : x[i + (HEIGHT - 2) * WIDTH];
        }
        for (int j = 1; j < HEIGHT - 1; j++) {
            x[0 + j * WIDTH] = b == 1 ? -x[1 + j * WIDTH] : x[1 + j * WIDTH];
            x[(WIDTH - 1) + j * WIDTH] = b == 1 ? -x[(WIDTH - 2) + j * WIDTH] : x[(WIDTH - 2) + j * WIDTH];
        }
        
        // Handle corners smoothly
        x[0 + 0 * WIDTH] = 0.5 * (x[1 + 0 * WIDTH] + x[0 + 1 * WIDTH]);
        x[0 + (HEIGHT - 1) * WIDTH] = 0.5 * (x[1 + (HEIGHT - 1) * WIDTH] + x[0 + (HEIGHT - 2) * WIDTH]);
        x[(WIDTH - 1) + 0 * WIDTH] = 0.5 * (x[(WIDTH - 2) + 0 * WIDTH] + x[(WIDTH - 1) + 1 * WIDTH]);
        x[(WIDTH - 1) + (HEIGHT - 1) * WIDTH] = 0.5 * (x[(WIDTH - 2) + (HEIGHT - 1) * WIDTH] + x[(WIDTH - 1) + (HEIGHT - 2) * WIDTH]);
    }
}
