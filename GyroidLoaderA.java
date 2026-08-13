// TODO: do something with this

public class GyroidLoaderA extends Loader {
    private static final StatusStage[] STAGES = { 
        new StatusStage(30, "Condensing liquid vapor fields:"),
        new StatusStage(60, "Solving periodic differential matrices:"),
        new StatusStage(90, "Tracing isosurface depth bounds:"),
        new StatusStage(100, "Gyroid Core Floating Perfectly!")
    }; 

    private int width;
    private int height;
    private double timeStep = 0.0;
    
    // Smooth character density ramp for depth shading
    private static final String DENSITY = ".,-~:;=!*#$@";

    public GyroidLoaderA(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
    }

    public GyroidLoaderA() {
        // This uses 80x22 specifically
        super(STAGES, 80, 22);
        this.width = this.window_width;
        this.height = this.window_height;
    }

    @Override
    protected void initialize() {
        this.timeStep = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Advance time to animate the 3D slicing morph effect
        timeStep += 0.04;

        // Scale factors to balance out the terminal's non-square character aspect ratio
        double scaleX = 0.25;
        double scaleY = 0.50;

        // Iterate across every cell in the 80x22 canvas
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int bufferIndex = y * width + x;

                // Center coordinates around the screen origin
                double posX = (x - width / 2.0) * scaleX;
                double posY = (y - height / 2.0) * scaleY;

                // Raycast down a localized depth axis (z-depth) to check for surface intersections
                for (double posZ = -5.0; posZ <= 5.0; posZ += 0.2) {
                    
                    // Modulate coordinates with time to create smooth rotation/movement
                    double rotX = posX * Math.cos(timeStep * 0.5) - posZ * Math.sin(timeStep * 0.5);
                    double rotZ = posX * Math.sin(timeStep * 0.5) + posZ * Math.cos(timeStep * 0.5);
                    double rotY = posY + Math.sin(timeStep + rotX) * 0.3; // Add subtle warpage

                    // Evaluated Gyroid Isosurface Approximation Function
                    double eval = Math.sin(rotX) * Math.cos(rotY) 
                                + Math.sin(rotY) * Math.cos(rotZ) 
                                + Math.sin(rotZ) * Math.cos(rotX);

                    // If the point is close to the zero threshold boundary, a surface slice is found
                    if (Math.abs(eval) < 0.15) {
                        // Normalize depth to a standard positive range for the Z-Buffer comparison
                        double depth = posZ + 5.0; 

                        // Update buffer if this point is closer than any previously evaluated point
                        if (depth > zBuffer[bufferIndex]) {
                            zBuffer[bufferIndex] = depth;

                            // Calculate shading density based on ray distance depth
                            int shadeIdx = (int) ((depth / 10.0) * (DENSITY.length() - 1));
                            shadeIdx = Math.max(0, Math.min(DENSITY.length() - 1, shadeIdx));
                            
                            // Write ANSI green colored text element into the final rendering buffer
                            outputBuffer[bufferIndex] = GREEN + DENSITY.charAt(shadeIdx) + RESET;
                        }
                    }
                }
            }
        }
    }
}
