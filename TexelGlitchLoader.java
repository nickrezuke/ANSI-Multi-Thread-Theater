public class TexelGlitchLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(10, generateGlitchString() + ":"),
            new StatusStage(20, generateGlitchString() + ":"),
            new StatusStage(30, generateGlitchString() + ":"),
            new StatusStage(40, generateGlitchString() + ":"),
            new StatusStage(50, generateGlitchString() + ":"),
            new StatusStage(60, generateGlitchString() + ":"),
            new StatusStage(70, generateGlitchString() + ":"),
            new StatusStage(80, generateGlitchString() + ":"),
            new StatusStage(90, generateGlitchString() + ":"),
            new StatusStage(98, generateGlitchString() + ":"),
            new StatusStage(100, generateGlitchString() + "!:")
    };

    public TexelGlitchLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // 2. Define Layout Boundaries
        boolean isOuterCasing = (x == 0 || x == 15 || y == 0 || y == 15);
        boolean isInnerBevel = (x == 1 || x == 14 || y == 1 || y == 14);
        boolean isCornerBracket = (x <= 3 || x >= 12) && (y <= 3 || y >= 12);
        boolean isCircuitTrace = (x == 4 || x == 11 || y == 4 || y == 11);

        // --- CYBER GLITCH COMPOSITING ENGINE ---

        // Layer A: The Glitchy Outer Framing & Corner Brackets
        if (isOuterCasing || isInnerBevel || isCornerBracket) {
            // High-speed edge corruption engine (5% chance per pixel to glitch out)
            if (Math.random() < 0.05) {
                boolean criticalError = Math.random() < 0.5;

                int r = criticalError ? 255 : 240;
                int g = criticalError ? 15 : 240;
                int b = criticalError ? 50 : 240; // Alternates between Warning Crimson and Bleaching White

                char errorChar = criticalError ? '\u25A0' : '\u2591'; // ■ (Error Block) or ░ (Data Stream)
                return new VoxelTexel(r, g, b, errorChar);
            }

            // Dark Gunmetal Base (When the frame is holding its integrity)
            int metal = 40 + noise * 4;
            char casingChar = isOuterCasing ? '#' : '%';
            return new VoxelTexel(metal, metal + 3, metal + 5, casingChar);
        }

        // Layer B: Recessed Shadow Trench
        if (x == 2 || x == 13 || y == 2 || y == 13) {
            // The deep gap can occasionally leak bright green code spikes
            if (Math.random() < 0.02) {
                return new VoxelTexel(10, 255, 80, '!'); // Blazing warning ticker
            }
            return new VoxelTexel(12, 10, 15, ';'); // Normal dark void gap
        }

        // Layer C: Internal High-Voltage Circuit Traces
        if (isCircuitTrace) {
            // Continuous electric green/cyan flickering logic
            int r = (int) (Math.random() * 30);
            int g = 190 + (int) (Math.random() * 65);
            int b = 210 + (int) (Math.random() * 45);
            return new VoxelTexel(r, g, b, '=');
        }

        // Layer D: The Pure High-Voltage TV Static Screen Core
        int staticR = (int) (Math.random() * 256);
        int staticG = (int) (Math.random() * 256);
        int staticB = (int) (Math.random() * 256);

        char staticChar;
        double densityCheck = Math.random();
        if (densityCheck > 0.66) {
            staticChar = '\u2588'; // █
        } else if (densityCheck > 0.33) {
            staticChar = '\u2592'; // ▒
        } else {
            staticChar = '\u2591'; // ░
        }

        return new VoxelTexel(staticR, staticG, staticB, staticChar);
    }

    // Helper generateGlitchString
    private static String generateGlitchString() {
        String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Generates a random length between 8 and 12 (inclusive)
        int length = (int) (Math.random() * 5) + 8;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            // Generates a random index between 0 and 61
            int randomIndex = (int) (Math.random() * 62);
            sb.append(alphaNumeric.charAt(randomIndex));
        }

        return sb.toString();
    }
}
