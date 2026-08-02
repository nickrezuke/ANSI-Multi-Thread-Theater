// TODO: Fix this Navier-Stokes Simulator

public class FluidDynamicsLoader extends Loader {
    private static final StatusStage[] FLUID_STAGES = {
        new StatusStage(25, "Initializing vector field grids:"),
        new StatusStage(50, "Allocating velocity and density buffers:"),
        new StatusStage(75, "Configuring Jacobi pressure solvers:"),
        new StatusStage(100, "Fluid Core Stable!")
    };

    private static final char[] DENSITY_RAMP = { ' ', '░', '▒', '▓', '█' };
    
    // Grid dimensions matching output buffer constraints
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;
    private static final int SIZE = WIDTH * HEIGHT;

    // Fluid state arrays: current and previous states
    private double[] density = new double[SIZE];
    private double[] dPrev = new double[SIZE];
    private double[] u = new double[SIZE];     // X velocity
    private double[] uPrev = new double[SIZE];
    private double[] v = new double[SIZE];     // Y velocity
    private double[] vPrev = new double[SIZE];

    private double dt = 0.1;      // Time step
    private double diff = 0.0001;  // Diffusion rate
    private double visc = 0.0001;  // Viscosity
    private double frameTimer = 0.0;

    public FluidDynamicsLoader() {
        super(FLUID_STAGES);
    }

    @Override 
    protected void initialize() { 
        // Clear pools natively
        for (int i = 0; i < SIZE; i++) {
            density[i] = dPrev[i] = u[i] = uPrev[i] = v[i] = vPrev[i] = 0.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        frameTimer += dt;

        // 1. Inject continuously moving forces and smoke density into the scene
        injectSources();

        // 2. Velocity Solver: Diffuse, Project (incompressibility), Advect, Project again
        diffuse(1, uPrev, u, visc);
        diffuse(2, vPrev, v, visc);
        
        project(uPrev, vPrev, u, v);
        
        advect(1, u, uPrev, uPrev, vPrev);
        advect(2, v, vPrev, uPrev, vPrev);
        
        project(u, v, uPrev, vPrev);

        // 3. Density Solver: Diffuse and Advect smoke through the solved velocity field
        diffuse(0, dPrev, density, diff);
        advect(0, density, dPrev, u, v);

        // 4. Render the grid state to the ANSI terminal buffer
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int idx = x + y * WIDTH;
                double d = density[idx];

                // Calculate velocity magnitude for color grading
                double velMag = Math.sqrt(u[idx] * u[idx] + v[idx] * v[idx]);

                // Map density value to ASCII character ramp
                int shadeIndex = (int) (d * (DENSITY_RAMP.length - 1));
                shadeIndex = Math.max(0, Math.min(DENSITY_RAMP.length - 1, shadeIndex));
                char renderChar = DENSITY_RAMP[shadeIndex];

                // Dynamically shift colors based on fluid kinetic energy (Velocity = Blue -> Red)
                int r = (int) Math.min(255, velMag * 1500);
                int g = (int) Math.min(255, d * 180 + velMag * 400);
                int b = (int) Math.min(255, 200 + d * 55);

                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                outputBuffer[idx] = colorCode + renderChar + RESET;
            }
        }

        // Clear density fields for the next physics loop iteration
        System.arraycopy(new double[SIZE], 0, dPrev, 0, SIZE);
    }

    private void injectSources() {
        // Create an oscillating vortex source in the left-center of the map
        int srcX = 5;
        int srcY = HEIGHT / 2;
        
        double forceX = 4.5;
        double forceY = Math.sin(frameTimer * 1.5) * 3.5; // Up and down whipping motion

        // Inject vectors over a small patch
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = 0; dx <= 1; dx++) {
                int nx = srcX + dx;
                int ny = srcY + dy;
                if (nx > 0 && nx < WIDTH - 1 && ny > 0 && ny < HEIGHT - 1) {
                    int idx = nx + ny * WIDTH;
                    density[idx] = 1.0; // Max density drop
                    u[idx] = forceX;
                    v[idx] = forceY;
                }
            }
        }
    }

    private void diffuse(int b, double[] x, double[] x0, double diff) {
        double a = dt * diff * WIDTH * HEIGHT;
        linearSolve(b, x, x0, a, 1 + 4 * a);
    }

    private void linearSolve(int b, double[] x, double[] x0, double a, double c) {
        // Jacobi relaxation loop for continuous linear system solving
        for (int k = 0; k < 20; k++) {
            for (int j = 1; j < HEIGHT - 1; j++) {
                for (int i = 1; i < WIDTH - 1; i++) {
                    x[i + j * WIDTH] = (x0[i + j * WIDTH] + a * (
                                        x[(i - 1) + j * WIDTH] + 
                                        x[(i + 1) + j * WIDTH] + 
                                        x[i + (j - 1) * WIDTH] + 
                                        x[i + (j + 1) * WIDTH]
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
                // Trace back coordinates in time
                double x = i - dt0x * velU[i + j * WIDTH];
                double y = j - dt0y * velV[i + j * WIDTH];

                // Clamp to boundary bounds
                if (x < 0.5) x = 0.5; if (x > WIDTH - 1.5) x = WIDTH - 1.5;
                if (y < 0.5) y = 0.5; if (y > HEIGHT - 1.5) y = HEIGHT - 1.5;

                int i0 = (int) x; int i1 = i0 + 1;
                int j0 = (int) y; int j1 = j0 + 1;

                double s1 = x - i0; double s0 = 1 - s1;
                double t1 = y - j0; double t0 = 1 - t1;

                // Bilinear interpolation of back-traced property
                d[i + j * WIDTH] = s0 * (t0 * d0[i0 + j0 * WIDTH] + t1 * d0[i0 + j1 * WIDTH]) +
                                   s1 * (t0 * d0[i1 + j0 * WIDTH] + t1 * d0[i1 + j1 * WIDTH]);
            }
        }
        setBounds(b, d);
    }

    private void project(double[] velU, double[] velV, double[] p, double[] div) {
        // Enforce mass conservation (makes fluid swirl instead of compressing into nothing)
        for (int j = 1; j < HEIGHT - 1; j++) {
            for (int i = 1; i < WIDTH - 1; i++) {
                div[i + j * WIDTH] = -0.5 * (
                                        velU[(i + 1) + j * WIDTH] - velU[(i - 1) + j * WIDTH] +
                                        velV[i + (j + 1) * WIDTH] - velV[i + (j - 1) * WIDTH]
                                     ) / Math.max(WIDTH, HEIGHT);
                p[i + j * WIDTH] = 0;
            }
        }
        setBounds(0, div);
        setBounds(0, p);

        linearSolve(0, p, div, 1, 4);

        for (int j = 1; j < HEIGHT - 1; j++) {
            for (int i = 1; i < WIDTH - 1; i++) {
                velU[i + j * WIDTH] -= 0.5 * Math.max(WIDTH, HEIGHT) * (p[(i + 1) + j * WIDTH] - p[(i - 1) + j * WIDTH]);
                velV[i + j * WIDTH] -= 0.5 * Math.max(WIDTH, HEIGHT) * (p[i + (j + 1) * WIDTH] - p[i + (j - 1) * WIDTH]);
            }
        }
        setBounds(1, velU);
        setBounds(2, velV);
    }

    private void setBounds(int b, double[] x) {
        // Handle vector mirroring at edges to keep fluid contained in the terminal frame
        for (int i = 1; i < WIDTH - 1; i++) {
            x[i + 0 * WIDTH]          = b == 2 ? -x[i + 1 * WIDTH] : x[i + 1 * WIDTH];
            x[i + (HEIGHT - 1) * WIDTH] = b == 2 ? -x[i + (HEIGHT - 2) * WIDTH] : x[i + (HEIGHT - 2) * WIDTH];
        }
        for (int j = 1; j < HEIGHT - 1; j++) {
            x[0 + j * WIDTH]          = b == 1 ? -x[1 + j * WIDTH] : x[1 + j * WIDTH];
            x[(WIDTH - 1) + j * WIDTH]  = b == 1 ? -x[(WIDTH - 2) + j * WIDTH] : x[(WIDTH - 2) + j * WIDTH];
        }

        // Handle corners smoothly
        x[0 + 0 * WIDTH] = 0.5 * (x[1 + 0 * WIDTH] + x[0 + 1 * WIDTH]);
        x[0 + (HEIGHT - 1) * WIDTH] = 0.5 * (x[1 + (HEIGHT - 1) * WIDTH] + x[0 + (HEIGHT - 2) * WIDTH]);
        x[(WIDTH - 1) + 0 * WIDTH] = 0.5 * (x[(WIDTH - 2) + 0 * WIDTH] + x[(WIDTH - 1) + 1 * WIDTH]);
        x[(WIDTH - 1) + (HEIGHT - 1) * WIDTH] = 0.5 * (x[(WIDTH - 2) + (HEIGHT - 1) * WIDTH] + x[(WIDTH - 1) + (HEIGHT - 2) * WIDTH]);
    }
}
