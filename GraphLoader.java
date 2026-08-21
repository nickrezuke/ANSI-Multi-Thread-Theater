public class GraphLoader extends Loader {
    private static final StatusStage[] STAGES = {
            new Loader.StatusStage(30, "Parsing function..."),
            new Loader.StatusStage(70, "Evaluating bounds..."),
            new Loader.StatusStage(100, "Graph Rendered OK! ")
    };

    /**
     * Functional interface allowing arbitrary mathematical expressions...
     * Lets say you want to graph an equation: Left Side = Right Side...
     * This interface equation evaluates an equation rewritten as:
     * F(x, y, t) = LeftSide - RightSide = 0,
     * Where x and y are the graph axis values, and t is time
     * For example, y = x^3... 1) rewrite it as x^3 - y = 0... 2) do:
     * currentEquation = (x, y, t) -> {return x * x * x - y;}
     * I also included the variable t (t is defined in terms of system time)
     * t can be included to make moving graphs. It increments 1.0/sec, try adding
     * t to a function, such as y = .25x / sin(t) :-> F = 0.25 * x / Math.sin(t) - y
     */
    @FunctionalInterface
    public interface ImplicitEquation {
        double evaluate(double x, double y, double t);
    }

    // Current active equation
    private ImplicitEquation currentEquation;

    // Coordinate grid scale definitions
    private double xMin = -4.0;
    private double xMax = 4.0;
    private double yMin = -4.0;
    private double yMax = 4.0;

    // Direct fallback for perfect hits or edge cases
    private double epsilon = 0.05;

    public GraphLoader() {
        super(STAGES, 140, 40);
    }

    public GraphLoader(int width, int height) {
        super(STAGES, width, height);
    }

    public GraphLoader(ImplicitEquation equation) {
        this();
        this.setEquation(equation);
    }

    public GraphLoader(ImplicitEquation equation, int width, int height) {
        this(width, height);
        this.setEquation(equation);
    }

    public void setEquation(ImplicitEquation equation) {
        this.currentEquation = equation;
    }

    @Override
    protected void initialize() {
        // If a custom equation was already passed to the constructor, DONT overwrite!
        if (this.currentEquation != null) {
            return;
        }

        // But is there wasn't, lets select a random one...
        int variant = (int) (Math.random() * 6);
        // TODO: Add more Math Functions / variants??
        switch (variant) {
            case 0:
                // Represents sin(x + sin(t)) * cos(y + cos(t)) = 0.2
                // --- THE KALEIDOSCOPE GRID ---
                // Interlocking sin/cos waves that create a shifting lattice pattern.
                // Visually: Looks like a grid of geometric cells.
                currentEquation = (x, y, t) -> {
                    return Math.sin(x + Math.sin(t)) * Math.cos(y + Math.cos(t)) - 0.2;
                };
                break;

            case 1:
                // Represents y = 0.4x / sin(2.2t)
                // --- THE PULSING HYPERBOLIC LINE ---
                // Visually: A straight line through the origin that rocks back and forth,
                // accelerating and stretching out toward the vertical plane.
                currentEquation = (x, y, t) -> {
                    double speedModifier = Math.sin(t * 2.2);
                    return 0.4 * x - (y * speedModifier);
                };
                break;

            case 2:
                // Represents (R - sqrt(rotX^2 + rotY^2))^2 + rotZ^2 = r^2
                // --- THE 3D SPINNING TORUS RING CROSS SECTION ---
                // Visually: A blob of the cross section of a 3D donut
                // that rolls through three-dimensional space over time.
                currentEquation = (x, y, t) -> {
                    double R = 2.0; // Major radius (distance from center to middle of the tube)
                    double r = 0.7; // Minor radius (thickness of the tube)

                    // Dynamic rotation angles over time for multi-axis tumbling
                    double pitch = t * 1.3;
                    double yaw = t * 0.7;

                    double cosP = Math.cos(pitch), sinP = Math.sin(pitch);
                    double cosY = Math.cos(yaw), sinY = Math.sin(yaw);

                    // 1. Inferred 3D mapping: Synthesize a rolling Z depth estimation field
                    // out of the 2D canvas coordinates to allow 3D matrix rotation.
                    double assumedZ = Math.sin(x + y + t);

                    // 2. Apply full 3D Rotation Matrix transformations
                    // Yaw (Y-Axis rotation)
                    double rotX1 = x * cosY - assumedZ * sinY;
                    double rotZ1 = x * sinY + assumedZ * cosY;
                    double rotY1 = y;

                    // Pitch (X-Axis rotation)
                    double rotX = rotX1;
                    double rotY = rotY1 * cosP - rotZ1 * sinP;
                    double rotZ = rotY1 * sinP + rotZ1 * cosP;

                    // 3. Evaluate the structural implicit 3D Torus equation
                    double torusWalk = R - Math.sqrt(rotX * rotX + rotY * rotY);
                    return (torusWalk * torusWalk + rotZ * rotZ) - (r * r);
                };
                break;

            case 3:
                // Represents (x^2 + y^2)^2 - 2a^2(x^2 - y^2) + b*sin(4*theta + t) = 0
                // --- THE MULTI-BLADE SPIROGRAPH PROPELLER ---
                // Visually: A complex propeller that splits into interlocking outer loops
                // and secondary waves, pulsing from a 4-blade star to a geometric blossom.
                currentEquation = (x, y, t) -> {
                    x /= 2.2; // Unit scaling to zoom in on this one, its cool looking...
                    y /= 2.2;

                    double r2 = x * x + y * y;
                    double r4 = r2 * r2;

                    // Spin rotation matrix
                    double cosT = Math.cos(t * 0.8);
                    double sinT = Math.sin(t * 0.8);
                    double rotX = x * cosT - y * sinT;
                    double rotY = x * sinT + y * cosT;

                    // Base Lemniscate profile
                    double baseLoop = r4 - 5.0 * (rotX * rotX - rotY * rotY);

                    // Outer loop harmonics: use polar angle to inject secondary multi-blade folds
                    double angle = Math.atan2(y, x);
                    double outerRipples = Math.sin(4.0 * angle + t * 2.0) * Math.cos(t) * 3.5;

                    return baseLoop + outerRipples;
                };
                break;

            case 4:
                // Represents cos(3*(x - 0.5t)) * cos(3*(y - 0.3t)) * morph + sin(5*(x - 0.5t))
                // * sin(5*(y - 0.3t)) * (1-morph) = 0.1
                // --- THE DRIFTING CHLADNI WAVE RESONANCE ---
                // Visually: Clahdni standing waves that seamlessly slide diagonally across the
                // screen
                // like flowing liquid sand, shifting forms between a checkerboard and a starry
                // mesh.
                currentEquation = (x, y, t) -> {
                    // Apply a slow, constant diagonal drift to the coordinate space
                    double driftX = x - (t * 0.5);
                    double driftY = y - (t * 0.3);

                    double waveA = Math.cos(3.0 * driftX) * Math.cos(3.0 * driftY);
                    double waveB = Math.sin(5.0 * driftX) * Math.sin(5.0 * driftY);

                    double morph = Math.sin(t * 2.0) * 0.5 + 0.5; // Smooth 0.0 to 1.0 cycle
                    return (waveA * morph) + (waveB * (1.0 - morph)) - 0.1;
                };
                break;

            case 5:
            default:
                // Represents tan(x^2 + y^2 - t) * cos(x + y) = cos(x^2 + y^2 - t)
                // --- THE SPIRALING RIPPLE ---
                // Visually: Concentric circles expanding outward from the origin, intersecting
                // a global static diagonal lattice wave pattern.
                currentEquation = (x, y, t) -> {
                    double r2 = x * x + y * y - t;
                    return (Math.tan(r2) * Math.cos(x + y)) - Math.cos(r2);
                };
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int centerX = window_width / 2;
        int centerY = window_height / 2;
        double t = System.currentTimeMillis() / 1000.0;

        // Precompute coordinate values for grid corners to save CPU cycles
        double[] xCoords = new double[window_width + 1];
        for (int sx = 0; sx <= window_width; sx++) {
            xCoords[sx] = xMin + ((double) sx / window_width) * (xMax - xMin);
        }

        double[] yCoords = new double[window_height + 1];
        for (int sy = 0; sy <= window_height; sy++) {
            yCoords[sy] = yMax - ((double) sy / window_height) * (yMax - yMin);
        }

        // 1. First Pass: Compute raw function evaluation matrix at all grid
        // intersections
        double[][] evalGrid = new double[window_height + 1][window_width + 1];
        for (int sy = 0; sy <= window_height; sy++) {
            for (int sx = 0; sx <= window_width; sx++) {
                evalGrid[sy][sx] = currentEquation.evaluate(xCoords[sx], yCoords[sy], t);
            }
        }

        // 2. Second Pass: Vector boundary rasterization per screen character cell
        for (int screenY = 0; screenY < window_height; screenY++) {
            for (int screenX = 0; screenX < window_width; screenX++) {
                int bufferIndex = screenY * window_width + screenX;

                // Extract evaluations for the 4 corners of this specific character cell
                double f00 = evalGrid[screenY][screenX]; // Top-Left
                double f10 = evalGrid[screenY][screenX + 1]; // Top-Right
                double f01 = evalGrid[screenY + 1][screenX]; // Bottom-Left
                double f11 = evalGrid[screenY + 1][screenX + 1]; // Bottom-Right

                if (Double.isNaN(f00) || Double.isInfinite(f00))
                    continue;

                // Compute how violently the function is swinging inside this single cell
                double maxDelta = Math.max(
                        Math.max(Math.abs(f00 - f10), Math.abs(f00 - f01)),
                        Math.max(Math.abs(f11 - f10), Math.abs(f11 - f01)));

                // --- NEW ANTI-GLITCH FILTER ---
                // If maxDelta is too high (> 50), it's a vertical asymptote (tan explosion).
                // If maxDelta is moderately high (> 4.5), the wave is oscillating faster than
                // our terminal grid resolution can cleanly resolve. We skip it to avoid Moiré
                // noise blobs.
                boolean frequencyTooHigh = maxDelta > 4.5;
                boolean isAsymptote = maxDelta > 50.0;

                // Check standard mathematical zero intersections across boundary lines
                boolean crossesZero = false;
                if (!frequencyTooHigh && !isAsymptote) {
                    crossesZero = (Math.signum(f00) != Math.signum(f10)) ||
                            (Math.signum(f01) != Math.signum(f11)) ||
                            (Math.signum(f00) != Math.signum(f01)) ||
                            (Math.signum(f10) != Math.signum(f11));

                }

                // Fallback check for exact localized point plotting
                boolean closeToZero = !frequencyTooHigh && Math.abs(f00) < epsilon;

                if (crossesZero || closeToZero) {
                    outputBuffer[bufferIndex] = GREEN + "#" + RESET;
                    zBuffer[bufferIndex] = 1.0;
                } else {
                    // Draw clean orientation axis overlays if no line is plotted
                    if (screenY == centerY && screenX == centerX) {
                        outputBuffer[bufferIndex] = "+";
                    } else if (screenY == centerY) {
                        outputBuffer[bufferIndex] = "-";
                    } else if (screenX == centerX) {
                        outputBuffer[bufferIndex] = "|";
                    }
                }
            }
        }

    }

    public void setViewBounds(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }
}
