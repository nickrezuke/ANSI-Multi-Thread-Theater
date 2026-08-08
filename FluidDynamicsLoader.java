public class FluidDynamicsLoader extends Loader {
    private static final StatusStage[] FLUID_STAGES = {
        new StatusStage(25, "Initializing vector field grids:"),
        new StatusStage(50, "Allocating velocity and density buffers:"),
        new StatusStage(75, "Configuring Jacobi pressure solvers:"),
        new StatusStage(100, "Fluid Core Stable!")
    };

    private static final char[] DENSITY_RAMP = { ' ', '░', '▒', '▓', '█' }; 
    private static final int WIDTH = 80;
    private static final int HEIGHT = 32;
    private static final int SIZE = WIDTH * HEIGHT; 

    private final double[] density = new double[SIZE];
    private final double[] dPrev = new double[SIZE];
    private final double[] u = new double[SIZE]; 
    private final double[] uPrev = new double[SIZE];
    private final double[] v = new double[SIZE]; 
    private final double[] vPrev = new double[SIZE];
    private final double dt = 0.1; 
    private final double diff = 0.0001; 
    private final double visc = 0.0001; 
    private double frameTimer = 0.0;

    // Optimized: Reusable zero-array to clear buffers cleanly without thwacking the GC
    private final double[] zeroBuffer = new double[SIZE];

    public FluidDynamicsLoader() {
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

        // 4. Render Layout Processor & Exponential Dissipation
        for (int j = 0; j < HEIGHT; j++) {
            int rowOffset = j * WIDTH;
            for (int i = 0; i < WIDTH; i++) {
                int idx = i + rowOffset;
                double d = density[idx]; 
                
                // Exponential decay: Smoke slowly fades out globally over time
                density[idx] *= 0.985;

                double velMag = Math.sqrt(u[idx] * u[idx] + v[idx] * v[idx]);
                int shadeIndex = (int) (d * (DENSITY_RAMP.length - 1));
                shadeIndex = Math.max(0, Math.min(DENSITY_RAMP.length - 1, shadeIndex));
                char renderChar = DENSITY_RAMP[shadeIndex];

                int r = (int) Math.min(255, velMag * 1200);
                int g = (int) Math.min(255, d * 150 + velMag * 350);
                int b = (int) Math.min(255, 180 + d * 75);
                
                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                outputBuffer[idx] = colorCode + renderChar + RESET;
            }
        }
        System.arraycopy(zeroBuffer, 0, dPrev, 0, SIZE);
    }

    private void injectSources() {
        int srcX = WIDTH / 2;
        int srcY = HEIGHT - 4; 
        double forceY = -6.5; 
        double forceX = Math.sin(frameTimer * 1.4) * 3.5; 

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = srcX + dx;
                int ny = srcY + dy;
                if (nx > 0 && nx < WIDTH - 1 && ny > 0 && ny < HEIGHT - 1) {
                    int idx = nx + ny * WIDTH;
                    //density[idx] = Math.PI; 
                    density[idx] = 3.9 + Math.sin(System.currentTimeMillis() / 710.0); 
                    // Waving the spray actally helps with performance???
                    // Is it easier to spread out a dense cluser a bunch of times than it is to constantly calculate uniform spread???? WTF CHECK THIS LATER
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
        for (int k = 0; k < 20; k++) {
            for (int j = 1; j < HEIGHT - 1; j++) {
                int rowOffset = j * WIDTH;
                int rowAbove = (j - 1) * WIDTH;
                int rowBelow = (j + 1) * WIDTH;
                for (int i = 1; i < WIDTH - 1; i++) {
                    int idx = i + rowOffset;
                    x[idx] = (x0[idx] + a * (
                        x[(i - 1) + rowOffset] + 
                        x[(i + 1) + rowOffset] + 
                        x[i + rowAbove] + 
                        x[i + rowBelow]
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
            int rowOffset = j * WIDTH;
            for (int i = 1; i < WIDTH - 1; i++) {
                int idx = i + rowOffset;
                double x = i - dt0x * velU[idx];
                double y = j - dt0y * velV[idx];

                if (x < 0.5) x = 0.5;
                if (x > WIDTH - 1.5) x = WIDTH - 1.5;
                if (y < 0.5) y = 0.5;
                if (y > HEIGHT - 1.5) y = HEIGHT - 1.5;

                int i0 = (int) x;
                int i1 = i0 + 1;
                int j0 = (int) y;
                int j1 = j0 + 1;

                double s1 = x - i0;
                double s0 = 1.0 - s1;
                double t1 = y - j0; double t0 = 1.0 - t1;

                d[idx] = s0 * (t0 * d0[i0 + j0 * WIDTH] + t1 * d0[i0 + j1 * WIDTH]) +
                         s1 * (t0 * d0[i1 + j0 * WIDTH] + t1 * d0[i1 + j1 * WIDTH]);
            }
        }
        setBounds(b, d);
    }

    private void project(double[] velU, double[] velV, double[] p, double[] div) {
        double maxDim = Math.max(WIDTH, HEIGHT);
        for (int j = 1; j < HEIGHT - 1; j++) {
            int rowOffset = j * WIDTH;
            for (int i = 1; i < WIDTH - 1; i++) {
                int idx = i + rowOffset;
                div[idx] = -0.5 * (
                    velU[(i + 1) + rowOffset] - velU[(i - 1) + rowOffset] + 
                    velV[i + (j + 1) * WIDTH] - velV[i + (j - 1) * WIDTH]
                ) / maxDim;
                p[idx] = 0;
            }
        }
        setBounds(0, div); 
        setBounds(0, p);
        linearSolve(0, p, div, 1, 4);

        for (int j = 1; j < HEIGHT - 1; j++) {
            int rowOffset = j * WIDTH;
            for (int i = 1; i < WIDTH - 1; i++) {
                int idx = i + rowOffset;
                velU[idx] -= 0.5 * maxDim * (p[(i + 1) + rowOffset] - p[(i - 1) + rowOffset]);
                velV[idx] -= 0.5 * maxDim * (p[i + (j + 1) * WIDTH] - p[i + (j - 1) * WIDTH]);
            }
        }
        setBounds(1, velU);
        setBounds(2, velV);
    }

    private void setBounds(int b, double[] x) {
        int lastRowOffset = (HEIGHT - 1) * WIDTH;
        int secondToLastRowOffset = (HEIGHT - 2) * WIDTH;
        
        for (int i = 1; i < WIDTH - 1; i++) {
            x[i] = b == 2 ? -x[i + WIDTH] : x[i + WIDTH];
            x[i + lastRowOffset] = b == 2 ? -x[i + secondToLastRowOffset] : x[i + secondToLastRowOffset];
        }
        for (int j = 1; j < HEIGHT - 1; j++) {
            int rowOffset = j * WIDTH;
            x[rowOffset] = b == 1 ? -x[1 + rowOffset] : x[1 + rowOffset];
            x[(WIDTH - 1) + rowOffset] = b == 1 ? -x[(WIDTH - 2) + rowOffset] : x[(WIDTH - 2) + rowOffset];
        }
        x[0] = 0.5 * (x[1] + x[WIDTH]);
        x[lastRowOffset] = 0.5 * (x[1 + lastRowOffset] + x[secondToLastRowOffset]);
        x[WIDTH - 1] = 0.5 * (x[WIDTH - 2] + x[(WIDTH - 1) + WIDTH]);
        x[(WIDTH - 1) + lastRowOffset] = 0.5 * (x[(WIDTH - 2) + lastRowOffset] + x[(WIDTH - 1) + secondToLastRowOffset]);
    }
}