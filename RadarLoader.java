import java.util.Arrays;

public class RadarLoader extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(20, "Activating sonar grid:"),
        new StatusStage(50, "Pinging network clusters:"),
        new StatusStage(80, "Analyzing perimeter data:"),
        new StatusStage(100, "All Sectors Clean!")
    };

    private double sweepAngle = 0;
    
    // Internal trail memory maps
    private final double[][] trailDensity = new double[22][80];
    private final double[][] targetLuminance = new double[22][80]; // Stores persistent danger blip trails

    // Static threat targets configured as {Y, X, TrackID}
    // Repositioned to sit safely inside the expanded sweep window
    private static final double[][] TARGETS = {
        {4, 25, 1},
        {18, 56, 2},
        {8, 62, 3}
    };

    public RadarLoader() {
        super(STAGES);
    }

    @Override
    protected void initialize() {
        for (double[] row : trailDensity) Arrays.fill(row, 0.0);
        for (double[] row : targetLuminance) Arrays.fill(row, 0.0);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int width = 80;
        int height = 22;
        int cX = 40, cY = 11; // Core center of the scope radar
        
        // 1. Fade out historical trails and danger target highlights across updates
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                trailDensity[y][x] *= 0.88;     // Sweeping emerald trail persistence
                targetLuminance[y][x] *= 0.92;  // Danger blips burn and decay slower for maximum tracking visualization
            }
        }

        // 2. Normalize Current Sweep Angle to a true 0 -> 2*PI circle bounds
        double currentSweep = sweepAngle;
        if (currentSweep < 0) currentSweep += 2 * Math.PI;

        // 3. Project the Dynamic Radial Sweep Line Vector Outward
        // Max radius expanded to 19 cells to fill up the screen vertical height perfectly
        for (double r = 0; r < 19.5; r += 0.25) {
            // Apply 1.9 aspect ratio multiplier to ensure the terminal output maps as a true circle
            int sX = (int) (cX + r * Math.cos(currentSweep) * 1.9);
            int sY = (int) (cY + r * Math.sin(currentSweep));
            
            if (sX >= 0 && sX < width && sY >= 0 && sY < height) {
                // Outer edge pixels maintain high energy value, fading smoothly inward
                double sweepIntensity = 1.0; 
                if (sweepIntensity > trailDensity[sY][sX]) {
                    trailDensity[sY][sX] = sweepIntensity;
                }
            }
        }

        // 4. Evaluate Threat Intersections directly against the trailing sweep vectors
        for (double[] target : TARGETS) {
            int tY = (int) target[0];
            int tX = (int) target[1];
            
            // Calculate target spatial orientation relative to center origin
            double tAngle = Math.atan2(tY - cY, (tX - cX) / 1.9);
            if (tAngle < 0) tAngle += 2 * Math.PI;

            // Check angular distance delta between beam position and danger node
            double angleDiff = Math.abs(currentSweep - tAngle);
            if (angleDiff > Math.PI) angleDiff = (2 * Math.PI) - angleDiff;

            // Sharp intercept click! When the line hits the target, it instantly flashes a blazing 2.0 energy signal
            if (angleDiff < 0.12) {
                targetLuminance[tY][tX] = 2.0; 
            }
        }

        // 5. Scope Screen Rasterization Compositing Pipeline
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int o = x + width * y;
                
                // Track radial distance matching the 1.9 aspect filter scale
                double distance = Math.sqrt(Math.pow((x - cX) / 1.9, 2) + Math.pow(y - cY, 2));

                // Layer A: Outermost Scope Boundary Protective Rim (Radius = 19)
                if (distance >= 19.0 && distance < 19.8) {
                    if (0.9 > zBuffer[o]) {
                        zBuffer[o] = 0.9;
                        outputBuffer[o] = "\u001B[38;5;34m\u2588" + RESET; // Solid dark green border casing
                    }
                    continue;
                }

                // Restrict interior drawing operations strictly inside the scope glass (Radius < 19)
                if (distance < 19.0) {
                    double sweepIntensity = trailDensity[y][x];
                    double dangerIntensity = targetLuminance[y][x];

                    // --- STRUCTURAL RADAR GRID OVERLAYS ---
                    boolean isCrosshair = (x == cX || y == cY);
                    boolean isRangeRing = Math.abs(distance - 6.0) < 0.35 || Math.abs(distance - 13.0) < 0.35;
                    boolean isGridNode  = isCrosshair || isRangeRing;

                    // Layer B: Direct Threat Contact Intercept (Blazing alert blips)
                    if (dangerIntensity > 0.05) {
                        if (dangerIntensity > zBuffer[o]) {
                            zBuffer[o] = dangerIntensity;
                            
                            String colorCode;
                            char targetChar;
                            
                            if (dangerIntensity > 1.4) {
                                colorCode = "\u001B[38;5;196m"; targetChar = '\u25A0'; // Blazing active warning ■
                            } else if (dangerIntensity > 0.6) {
                                colorCode = "\u001B[38;5;208m"; targetChar = 'X';        // Cooling track alert code
                            } else {
                                colorCode = "\u001B[38;5;166m"; targetChar = '\u2022';  // Fading phosphorus heat dot •
                            }
                            outputBuffer[o] = colorCode + targetChar + RESET;
                        }
                        continue;
                    }

                    // Layer C: Active Sweeping Laser Beam Core Pointer
                    if (sweepIntensity > 0.96) {
                        if (0.8 > zBuffer[o]) {
                            zBuffer[o] = 0.8;
                            outputBuffer[o] = "\u001B[38;5;81m\u2588" + RESET; // Ice Blue / Pure White leading beam
                        }
                        continue;
                    }

                    // Layer D: Moving Phosphorus Trail and Grid Overlay Processing
                    double compositeIntensity = Math.max(sweepIntensity, isGridNode ? 0.15 : 0.0);

                    if (compositeIntensity > 0.01 && compositeIntensity > zBuffer[o]) {
                        zBuffer[o] = compositeIntensity;

                        String colorCode;
                        char renderChar;

                        if (sweepIntensity > 0.70) {
                            colorCode = "\u001B[38;5;46m"; renderChar = '\u2593'; // ▓ Blazing green core cloud
                        } else if (sweepIntensity > 0.40) {
                            colorCode = "\u001B[38;5;28m"; renderChar = '\u2592'; // ▒ Medium trail mesh
                        } else if (sweepIntensity > 0.12) {
                            colorCode = "\u001B[38;5;22m"; renderChar = '\u2591'; // ░ Faint decaying green
                        } else {
                            // Sub-surface passive background grid structure tracking
                            colorCode = "\u001B[38;5;234m"; 
                            renderChar = isCrosshair ? '\u253C' : (isRangeRing ? '\u25AC' : ' '); // Background lines (┼ or ▬)
                        }

                        outputBuffer[o] = colorCode + renderChar + RESET;
                    }
                }
            }
        }
        
        // Control rotation increment speed
        sweepAngle = (sweepAngle + 0.045) % (2 * Math.PI);
    }
}
