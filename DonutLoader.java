import java.util.Arrays;

public class DonutLoader extends Loader {

    private static final StatusStage[] DONUT_STAGES = {
            new StatusStage(20, "Mixing batter:"),
            new StatusStage(35, "Baking cake:"),
            new StatusStage(55, "Cooling down:"),
            new StatusStage(75, "Applying glaze:"),
            new StatusStage(95, "Adding sprinkles:"),
            new StatusStage(100, "Boxed & Ready!")
    };

    private static final double[][] SPRINKLES = {
            { 0.3, 0.6, 0 }, { 0.8, 0.8, 1 }, { 1.4, 0.5, 2 }, { 1.9, 0.7, 0 }, { 2.5, 0.6, 1 },
            { 3.0, 0.8, 2 }, { 3.6, 0.5, 0 }, { 4.1, 0.7, 1 }, { 4.7, 0.6, 2 }, { 5.3, 0.8, 0 },
            { 5.9, 0.5, 1 }, { 6.2, 0.7, 2 }, { 0.5, 1.1, 1 }, { 1.1, 1.3, 2 }, { 1.7, 1.0, 0 },
            { 2.2, 1.2, 1 }, { 2.8, 1.4, 2 }, { 3.3, 1.1, 0 }, { 3.9, 1.3, 1 }, { 4.4, 1.0, 2 },
            { 5.0, 1.2, 0 }, { 5.6, 1.4, 1 }, { 0.1, 1.6, 2 }, { 0.9, 1.5, 0 }, { 1.5, 1.7, 1 },
            { 2.3, 1.6, 2 }, { 3.5, 1.5, 0 }, { 4.2, 1.7, 1 }, { 4.9, 1.6, 2 }, { 5.7, 1.5, 0 }
    };

    private final int[][] sprinkleMap = new int[180][524];
    private String glazedIcing;
    private String donutCake;
    private String[] sprinkleColors;
    
    private double A = 0;
    private double B = 0;

    public DonutLoader() {
        super(DONUT_STAGES);
    }

    @Override
    protected void initialize() {
        // Setup visual flavor styling
        switch ((int) (Math.random() * 6) + 1) {
            case 1:
                glazedIcing = "\u001B[38;5;94m"; 
                donutCake = "\u001B[33m"; 
                sprinkleColors = new String[] { "\u001B[38;5;214m", "\u001B[38;5;226m", "\u001B[38;5;255m" };
                break;
            case 2:
                glazedIcing = "\u001B[38;5;130m"; 
                donutCake = "\u001B[38;5;52m"; 
                sprinkleColors = new String[] { "\u001B[38;5;117m", "\u001B[38;5;207m", "\u001B[38;5;255m" };
                break;
            case 3:
                glazedIcing = "\u001B[38;5;255m"; 
                donutCake = "\u001B[38;5;137m"; 
                sprinkleColors = new String[] { "\u001B[38;5;196m", "\u001B[38;5;94m", "\u001B[38;5;208m" };
                break;
            case 4:
                glazedIcing = "\u001B[38;5;114m"; 
                donutCake = "\u001B[38;5;52m"; 
                sprinkleColors = new String[] { "\u001B[38;5;255m", "\u001B[38;5;206m", "\u001B[38;5;220m" };
                break;
            case 5:
                glazedIcing = "\u001B[38;5;61m"; 
                donutCake = "\u001B[38;5;229m"; 
                sprinkleColors = new String[] { "\u001B[38;5;81m", "\u001B[38;5;46m", "\u001B[38;5;255m" };
                break;
            case 6:
            default:
                glazedIcing = "\u001B[38;5;205m"; 
                donutCake = "\u001B[33m"; 
                sprinkleColors = new String[] { "\u001B[36m", "\u001B[32m", "\u001B[37m" };
                break;
        }

        // Generate sprinkle map mapping logic
        for (int[] row : sprinkleMap) Arrays.fill(row, -1);
        int tMapIndex = 0;
        for (double theta = 0; theta < 6.28; theta += 0.035) {
            int pMapIndex = 0;
            for (double phi = 0; phi < 6.28; phi += 0.012) {
                for (int i = 0; i < SPRINKLES.length; i++) {
                    double dTheta = Math.abs(theta - SPRINKLES[i][0]);
                    double dPhi = Math.abs(phi - SPRINKLES[i][1]);
                    if (dTheta > 3.14) dTheta = 6.28 - dTheta;

                    if (dTheta < 0.15 && dPhi < 0.15) {
                        sprinkleMap[tMapIndex][pMapIndex] = (int) SPRINKLES[i][2];
                        break;
                    }
                }
                pMapIndex++;
            }
            tMapIndex++;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        for (int tIndex = 0; tIndex < 180; tIndex++) {
            double theta = tIndex * 0.035;
            for (int pIndex = 0; pIndex < 524; pIndex++) {
                double phi = pIndex * 0.012;
                double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
                double sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);
                double sinA = Math.sin(A), cosA = Math.cos(A);
                double sinB = Math.sin(B), cosB = Math.cos(B);

                double h = cosPhi + 2;
                double D = 1 / (sinTheta * h * sinA + sinPhi * cosA + 7.0);
                double t = sinTheta * h * cosA - sinPhi * sinA;

                int x = (int) (40 + 42 * D * (cosTheta * h * cosB - t * sinB));
                int y = (int) (12 + 21 * D * (cosTheta * h * sinB + t * cosB));
                int o = x + 80 * y;

                double N_double = 8 * ((sinPhi * sinA - sinTheta * cosPhi * cosA) * cosB - sinTheta * cosPhi * sinA - sinPhi * cosA - cosTheta * cosPhi * sinB);

                if (22 > y && y > 0 && x > 0 && 80 > x && D > zBuffer[o]) {
                    zBuffer[o] = D;
                    int charIndex = (int) Math.round(N_double);
                    if (charIndex < 0) charIndex = 0;

                    String lString = ".,-~:;=!*#$@";
                    char asciiChar = lString.charAt(charIndex >= lString.length() ? lString.length() - 1 : charIndex);

                    double dripThreshold = -0.15 + 0.15 * Math.sin(3 * theta) + 0.08 * Math.cos(7 * theta) + 0.04 * Math.sin(11 * theta);
                    boolean isFrosting = sinPhi > dripThreshold;
                    String chosenColor = isFrosting ? glazedIcing : donutCake;

                    if (isFrosting) {
                        int sprinkleColorIndex = sprinkleMap[tIndex][pIndex];
                        if (sprinkleColorIndex != -1) {
                            chosenColor = sprinkleColors[sprinkleColorIndex];
                        }
                    }
                    outputBuffer[o] = chosenColor + asciiChar + RESET;
                }
            }
        }

        A += 0.04;
        B += 0.02;
    }
}
