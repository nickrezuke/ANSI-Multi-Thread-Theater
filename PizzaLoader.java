// TODO: Perfect the pepperoni placement

import java.util.ArrayList;
import java.util.List;

public class PizzaLoader extends Loader {
    private static final StatusStage[] PIZZA_STAGES = {
        new StatusStage(20, "Tossing dough:"),
        new StatusStage(45, "Spreading marinara:"),
        new StatusStage(70, "Shredding mozzarella:"),
        new StatusStage(90, "Layering pepperonis:"),
        new StatusStage(100, "Baked Fresh & Hot!")
    };

    // Simple Coordinate tracking for randomly scattered toppings
    private static class PizzaTopping {
        double x, y;
        PizzaTopping(double x, double y) {
            this.x = x; this.y = y;
        }
    }

    private static final List<PizzaTopping> PEPPERONIS = new ArrayList<>();
    static {        
        PEPPERONIS.add(new PizzaTopping(1.2,  0.0));   
        PEPPERONIS.add(new PizzaTopping(2.0, -1.1));  
        PEPPERONIS.add(new PizzaTopping(2.3,  1.25));  
        PEPPERONIS.add(new PizzaTopping(2.6, -0.3));  
        PEPPERONIS.add(new PizzaTopping(3.2, -1.75)); 
        PEPPERONIS.add(new PizzaTopping(3.1,  1.7));  
        PEPPERONIS.add(new PizzaTopping(3.3,  0.2));   
    }

    private static final String LUMINANCE_CHARS = ":;=!*#$@▒▓█";
    
    // Geometry Constants
    private static final double MAX_RADIUS = 3.8; 
    private static final double SLICE_ANGLE = (2.0 * Math.PI) / 6.0; // 60 degrees total (6 slice pizza)
    private static final double HALF_ANGLE = SLICE_ANGLE / 2.0;
    private static final double EDGE_SLOPE = Math.tan(HALF_ANGLE);   // Line slope of straight slice cuts
    private static final double CHEESE_THICKNESS = 0.3; 
    private static final double CRUST_TORUS_R = 0.35;   

    // FIXED A: Every pepperoni is uniformly sized to look realistic
    private static final double PEPPERONI_RADIUS = 0.26; 

    // Rotation pivot alignment
    private static final double X_OFFSET = MAX_RADIUS * 0.65; 

    private String crustColor;
    private String cheeseColor;
    private String pepperoniColor;
    private String[][] cellCache;

    private double A = Math.PI / 3.0; 
    private double B = 0.0;

    public PizzaLoader() {
        super(PIZZA_STAGES, 80, 22);
    }

    public PizzaLoader(int w, int h) {
        super(PIZZA_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        crustColor     = "\u001B[38;5;172m"; // Baked Crust Orange-Brown
        cheeseColor    = "\u001B[38;5;220m"; // Gooey Mozzarella Yellow
        pepperoniColor = "\u001B[38;5;124m"; // Pepperoni Deep Red

        String[] fullPalette = { crustColor, cheeseColor, pepperoniColor };
        cellCache = new String[fullPalette.length][LUMINANCE_CHARS.length()];
        for (int c = 0; c < fullPalette.length; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = fullPalette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinB = Math.sin(B), cosB = Math.cos(B);

        // --- STEP 1: Main Cheese & Dough Flat Faces ---
        double maxX = MAX_RADIUS * Math.cos(HALF_ANGLE);
        for (double x = 0.0; x <= maxX; x += 0.06) {
            double maxY = x * EDGE_SLOPE; 
            for (double y = -maxY; y <= maxY; y += 0.04) {
                // Top Cheese Layer
                drawPoint(x, y, CHEESE_THICKNESS/2, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
                // Bottom Crust Layer
                drawPoint(x, y, -CHEESE_THICKNESS/2, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // --- STEP 2: Render Pepperoni Slices FIRST (Allows them to override depth boundaries cleanly) ---
        for (PizzaTopping pep : PEPPERONIS) {
            for (double pr = 0.0; pr <= PEPPERONI_RADIUS; pr += 0.04) {
                for (double pa = 0; pa < 2 * Math.PI; pa += 0.2) {
                    double px = pep.x + pr * Math.cos(pa);
                    double py = pep.y + pr * Math.sin(pa);
                    
                    // FIXED: Bumped Z slightly higher to guarantee it overrides the cheese base depth
                    double pz = (CHEESE_THICKNESS / 2.0) + 0.05; 

                    // Clipping Intersections
                    if ((px * px + py * py) > (MAX_RADIUS * MAX_RADIUS)) continue;
                    if (px < 0 || Math.abs(py) > (px * EDGE_SLOPE)) continue;

                    double nx = 0.1 * Math.cos(pa);
                    double ny = 0.1 * Math.sin(pa);
                    double nz = 0.95;

                    drawPoint(px, py, pz, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                }
            }
        }

        // --- STEP 3: Cheese Side Walls SECOND (Fills in behind the cut pepperonis) ---
        for (double x = 0.0; x <= maxX; x += 0.06) {
            double maxY = x * EDGE_SLOPE; 
            // Only draw along the absolute outer left and right cutting rays
            for (double y : new double[]{-maxY, maxY}) {
                for (double z = -CHEESE_THICKNESS/2; z <= CHEESE_THICKNESS/2; z += 0.05) {
                    double nx = -EDGE_SLOPE;
                    double ny = (y > 0) ? 1.0 : -1.0;
                    drawPoint(x, y, z, nx, ny, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 1);
                }
            }
        }

        // --- STEP 4: Torus-Arc Puffy Crust ---
        double crustCenterRadius = MAX_RADIUS; 
        for (double t = -HALF_ANGLE; t <= HALF_ANGLE; t += 0.008) {
            double cosT = Math.cos(t), sinT = Math.sin(t);
            for (double p = 0; p < 2 * Math.PI; p += 0.15) {
                double cosP = Math.cos(p), sinP = Math.sin(p);

                double x = (crustCenterRadius + CRUST_TORUS_R * cosP) * cosT;
                double y = (crustCenterRadius + CRUST_TORUS_R * cosP) * sinT;
                double z = CRUST_TORUS_R * sinP;

                double nx = cosP * cosT;
                double ny = cosP * sinT;
                double nz = sinP;

                drawPoint(x, y, z, nx, ny, nz, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        A += 0.02 * Math.sin(Math.E * B / Math.PI); // Even Euler thinks Pizza is Beautiful
        B += 0.03;
    }


    private void drawPoint(double x, double y, double z, double nx, double ny, double nz,
                           double sinA, double cosA, double sinB, double cosB,
                           String[] outputBuffer, double[] zBuffer, int colorIndex) {
        
        double shiftedX = x - X_OFFSET;

        double x1 = shiftedX * cosB - y * sinB;
        double y1 = shiftedX * sinB + y * cosB;
        
        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;

        double distance = 4.2; 
        double ooZ = 1.0 / (z2 + distance);

        int xp = (int) (window_width / 2.0 + 38 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 17 * ooZ * y2);

        int bufferIndex = xp + window_width * yp;

        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            double nx1 = nx * cosB - ny * sinB;
            double ny1 = nx * sinB + ny * cosB;
            double ny2 = ny1 * cosA - nz * sinA;
            double nz2 = ny1 * sinA + nz * cosA;
            double nx2 = nx1;

            double luminance = nx2 * 0.3 + ny2 * 0.3 + nz2 * 0.9;

            if (ooZ > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooZ;

                int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));

                outputBuffer[bufferIndex] = cellCache[colorIndex][charIndex];
            }
        }
    }
}
