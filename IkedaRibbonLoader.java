// TODO: Fix This lmao totally broken....

import java.util.Random;

public class IkedaRibbonLoader extends Loader { 
    private static final StatusStage[] IKEDA_STAGES = { 
        new StatusStage(25, "Tuning ring cavity mirrors:"), 
        new StatusStage(50, "Simulating non-linear refraction:"), 
        new StatusStage(75, "Tracing chaotic laser ribbons:"), 
        new StatusStage(100, "Ikeda Laser Core Operational!") 
    }; 

    private final int width; 
    private final int height; 
    private final int totalSize; 
    private final double[] ribbonDensity; 
    private final Random rand = new Random();
    private double timeClock = 0.0; 

    public IkedaRibbonLoader() { 
        super(IKEDA_STAGES, 80, 22); 
        this.width = 80; 
        this.height = 22; 
        this.totalSize = 1760; 
        this.ribbonDensity = new double[totalSize]; 
    } 

    @Override 
    protected void initialize() { 
        this.timeClock = 0.0; 
        for (int i = 0; i < totalSize; i++) { 
            ribbonDensity[i] = 0.0; 
        } 
    } 

    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        timeClock += 0.015; 
        
        // Clear density tracks fresh for this frame iteration
        for (int i = 0; i < totalSize; i++) { 
            ribbonDensity[i] = 0.0; 
        } 

        double B = 0.9; 
        double C = 6.0; 
        
        // CRITICAL FIX: Locked R to a stable value to keep the map centered on-screen
        double R = 1.0; 

        // MULTI-THREAD TRACING FLUIDITY: Spin 80 independent trajectories per frame
        for (int strand = 0; strand < 80; strand++) {
            // Seed each strand with a varied starting position across the attractor plane
            double px = -0.5 + (rand.nextDouble() * 2.0);
            double py = -1.5 + (rand.nextDouble() * 2.0);

            // Step each strand to organically reveal the whole filament ribbon
            for (int step = 0; step < 400; step++) { 
                double currentRadiusSq = px * px + py * py; 
                double t = R - (C / (1.0 + currentRadiusSq)); 
                double cosT = Math.cos(t); 
                double sinT = Math.sin(t); 
                
                double nextX = 1.0 + B * (px * cosT - py * sinT); 
                double nextY = B * (px * sinT + py * cosT); 
                px = nextX; 
                py = nextY; 

                // --- CALIBRATED CAMERA BOUNDS ---
                // Maps standard Ikeda geometry strictly to 80x22 pixels
                int screenX = (int) (32.0 + (px * 16.5)); 
                int screenY = (int) (11.0 - (py * 4.8)); 

                if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) { 
                    int idx = screenX + width * screenY; 
                    ribbonDensity[idx] += 0.015; // Balanced line thickness density
                } 
            } 
        }

        // --- RASTERIZATION PASS ---
        String palette = " .:-=+*#%@█"; 
        
        for (int y = 0; y < height; y++) { 
            int rowOffset = y * width; 
            for (int x = 0; x < width; x++) { 
                int idx = x + rowOffset; 
                double d = ribbonDensity[idx]; 

                if (d > 0.001) { 
                    if (d > 1.0) d = 1.0;
                    
                    if (d > zBuffer[idx]) {
                        zBuffer[idx] = d; 

                        int shadeIdx = (int) (d * (palette.length() - 1)); 
                        char renderChar = palette.charAt(shadeIdx); 

                        // Rainbow Palette Formula
                        double colorOffset = timeClock * 2.0 + (x * 0.04) + (y * 0.1); 
                        int r = (int) (128 + 127 * Math.sin(colorOffset)); 
                        int g = (int) (128 + 127 * Math.sin(colorOffset + 2.0 * Math.PI / 3.0)); 
                        int b = (int) (128 + 127 * Math.sin(colorOffset + 4.0 * Math.PI / 3.0)); 

                        String laserColor = String.format("\u001B[38;2;%d;%d;%dm", 
                            Math.max(0, Math.min(255, r)), 
                            Math.max(0, Math.min(255, g)), 
                            Math.max(0, Math.min(255, b))
                        ); 
                        outputBuffer[idx] = laserColor + renderChar + RESET; 
                    }
                } else { 
                    if (x % 6 == 0 && y % 3 == 0) { 
                        outputBuffer[idx] = "\u001B[38;2;50;55;65m.\u001B[0m"; 
                    } else { 
                        outputBuffer[idx] = " "; 
                    } 
                } 
            } 
        } 
    } 
}
