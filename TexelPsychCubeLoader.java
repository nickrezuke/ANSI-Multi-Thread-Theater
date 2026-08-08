public class TexelPsychCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(11, "Debating If You Got Scammed:"),
            new StatusStage(30, "Chasing White Rabbits:"),
            new StatusStage(55, "Wiggin' Out, Man:"),
            new StatusStage(75, "Stroking the Furry Walls:"),
            new StatusStage(96, "Thinking this lasts forever:"),
            new StatusStage(100, "Enlightenment Achieved!")
    };

    public TexelPsychCubeLoader() {
        // This uses 80x22 specifically
        super(TEXEL_CUBE_STAGES, 80, 22);
    }

    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        // 1. Establish Time Vectors
        double t = System.currentTimeMillis() * 0.003; // Speed of the fluid morphing

        // 2. Map coordinates (-1.0 to 1.0) so math functions ripple outwards from
        // center
        double cx = (x - 7.5) / 7.5;
        double cy = (y - 7.5) / 7.5;

        // 3. Layered Plasma Mathematics (Interlocking Sine Wave Fields)
        // Wave 1: Concentric ripples expanding from the center
        double wave1 = Math.sin(Math.sqrt(cx * cx + cy * cy) * 5.0 - t);

        // Wave 2: Distorted horizontal/vertical interference tracking the Face Index
        double wave2 = Math.sin(cx * 4.0 + t + face) + Math.cos(cy * 3.0 - t * 0.5);

        // Wave 3: A twisting diagonal vortex calculation
        double wave3 = Math.sin((cx + cy) * 3.0 + t * 1.5);

        // Combine waves and normalize value to run safely between -1.0 and 1.0
        double combined = (wave1 + wave2 + wave3) / 3.0;

        // 4. Psychedelic Color Phase Wheel Mapping (Converts float value into shifting
        // RGB spectrums)
        // Adding offsets to the phase wheels ensures Red, Green, and Blue peaks split
        // beautifully
        int r = (int) ((Math.sin(combined * Math.PI + t) + 1.0) * 127.5);
        int g = (int) ((Math.sin(combined * Math.PI + t + (2.0 * Math.PI / 3.0)) + 1.0) * 127.5);
        int b = (int) ((Math.sin(combined * Math.PI + t + (4.0 * Math.PI / 3.0)) + 1.0) * 127.5);

        // 5. Dynamic Density Morphing
        // The underlying terminal characters continuously shift and breathe alongside
        // the colors
        char fluidChar;
        double density = Math.abs(combined);
        if (density > 0.75) {
            fluidChar = '\u2588'; // █ (Solid peak plasma)
        } else if (density > 0.50) {
            fluidChar = '\u2593'; // ▓
        } else if (density > 0.25) {
            fluidChar = '\u2592'; // ▒
        } else {
            fluidChar = '\u2591'; // ░ (Low energy valleys)
        }

        return new VoxelTexel(r, g, b, fluidChar);
    }

}
