public class TexelEmojiCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(30, "Feeling the Vibe:"),
            new StatusStage(65, "Injecting Emotional Vectors:"),
            new StatusStage(90, "Smoothing Personality:"),
            new StatusStage(100, "Emojis Activated!")
    };

    public TexelEmojiCubeLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    protected VoxelTexel getCubeTexel(int variant, int face, int x, int y) {
        int noise = (int) (Math.abs((x * 34211L + y * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3);

        // 1. Smooth Radial Sphere Shading (Distance from Center calculation)
        // Center point of the 16x16 face is 7.5
        double dx = x - 7.5;
        double dy = y - 7.5;
        // Normalized distance from center (0.0 at center, roughly 1.0 at corners)
        double distFromCenter = Math.min(1.0, Math.sqrt(dx * dx + dy * dy) / 10.0);

        // 2. Default Feature Triggers
        boolean isEye = false, isMouth = false, isEyebrow = false, isAccent = false;
        char featureChar = ' ';
        int accentR = 0, accentG = 0, accentB = 0;

        // 3. Coordinate Matrices for the Face Layouts
        switch (face % 6) {
            case 0: // 😊 Smiling Blush
                isEye = (y == 4 && (x == 4 || x == 5 || x == 10 || x == 11))
                        || (y == 5 && (x == 3 || x == 6 || x == 9 || x == 12));
                featureChar = isEye ? '\u25DC' : ' '; // ⌒
                isMouth = (y == 9 && x >= 4 && x <= 11) || (y == 10 && x >= 5 && x <= 10)
                        || (y == 11 && x >= 6 && x <= 9);
                featureChar = isMouth ? '\u2584' : featureChar; // ▄
                isAccent = (y == 7 || y == 8) && (x == 2 || x == 3 || x == 12 || x == 13);
                accentR = 255;
                accentG = 110;
                accentB = 140;
                break;

            case 1: // 😜 Wink & Tongue Out
                boolean leftWink = (y == 5 && x >= 3 && x <= 6);
                boolean rightOpen = (y >= 4 && y <= 6 && x >= 10 && x <= 11);
                isEye = leftWink || rightOpen;
                featureChar = leftWink ? '-' : '\u2588';
                boolean smileLine = (y == 8 && x >= 4 && x <= 11) || (y == 9 && (x == 3 || x == 12));
                boolean hangingTongue = (y >= 9 && y <= 11 && x >= 7 && x <= 9);
                if (smileLine) {
                    isMouth = true;
                    featureChar = '\u2592';
                } else if (hangingTongue) {
                    isAccent = true;
                    accentR = 240;
                    accentG = 40;
                    accentB = 70;
                }
                break;

            case 2: // 😠 Angry
                isEyebrow = (y == 3 && (x == 3 || x == 12)) || (y == 4 && (x == 4 || x == 11))
                        || (y == 5 && (x == 5 || x == 10));
                featureChar = '\u2584';
                isEye = (y == 6 && (x == 4 || x == 5 || x == 10 || x == 11));
                featureChar = isEye ? '\u25A0' : featureChar;
                isMouth = (y == 11 && x >= 5 && x <= 10) || (y == 10 && (x == 4 || x == 11));
                featureChar = isMouth ? '=' : featureChar;
                break;

            case 3: // 😮 Shocked
                isEyebrow = (y == 3 && x >= 4 && x <= 6) || (y == 3 && x >= 9 && x <= 11);
                featureChar = '\u203E';
                isEye = (y >= 5 && y <= 6) && (x == 4 || x == 5 || x == 10 || x == 11);
                featureChar = isEye ? '\u2588' : featureChar;
                isMouth = (y >= 9 && y <= 12 && x >= 6 && x <= 9) && !(y == 9 && (x == 6 || x == 9))
                        && !(y == 12 && (x == 6 || x == 9));
                featureChar = isMouth ? '\u2588' : featureChar;
                break;

            case 4: // 😎 Sunglasses Cool
                boolean isGlassesLens = (y >= 4 && y <= 6) && ((x >= 2 && x <= 6) || (x >= 9 && x <= 13));
                boolean isGlassesBridge = (y == 4 && x >= 6 && x <= 9);
                if (isGlassesLens || isGlassesBridge) {
                    isAccent = true;
                    accentR = 30;
                    accentG = 30;
                    accentB = 35;
                }
                isMouth = (y == 10 && x >= 8 && x <= 12) || (y == 9 && x == 12);
                featureChar = isMouth ? '\u25AC' : featureChar;
                break;

            case 5: // 😢 Crying
                isEyebrow = (y == 4 && (x == 5 || x == 10)) || (y == 5 && (x == 4 || x == 11));
                featureChar = '\u25CB';
                isEye = (y == 6 && (x == 4 || x == 5 || x == 10 || x == 11));
                featureChar = isEye ? '\u25A0' : featureChar;
                isMouth = (y == 11 && x >= 6 && x <= 9);
                featureChar = isMouth ? '\u25AC' : featureChar;
                isAccent = (x == 4 && y >= 7 && y <= 12);
                accentR = 30;
                accentG = 165;
                accentB = 255;
                break;
        }

        // --- TEXTURE COMPOSITING ENGINE ---

        // Layer A: Facial Line Features (Deep ink brown)
        if (isEye || isMouth || isEyebrow) {
            return new VoxelTexel(45, 30, 20, featureChar);
        }

        // Layer B: Special Colors (Blush, Tongues, Sunglasses, Tears)
        if (isAccent) {
            char accentChar = (face % 6 == 4) ? '\u2588' : '\u2592';
            return new VoxelTexel(accentR, accentG, accentB, accentChar);
        }

        // Layer C: Smooth Spherical Gradient Interpolation
        // Instead of a hard boundary, the color and characters scale smoothly from the
        // center outwards
        double brightnessFactor = 1.0 - (distFromCenter * 0.45); // Fade up to 45% darker at corners

        int r = (int) (255 * brightnessFactor);
        int g = (int) ((205 + noise * 6) * brightnessFactor);
        int b = (int) (35 * brightnessFactor);

        // Dynamic density scaling based on center distance mapping
        char skinChar;
        if (distFromCenter < 0.4) {
            skinChar = '\u2588'; // █ (Solid core)
        } else if (distFromCenter < 0.7) {
            skinChar = '\u2593'; // ▓ (Densely speckled mid-tone)
        } else if (distFromCenter < 0.9) {
            skinChar = '\u2592'; // ▒ (Medium mesh wrap)
        } else {
            skinChar = '\u2591'; // ░ (Light feathering on the sharpest corner vertices)
        }

        return new VoxelTexel(r, g, b, skinChar);
    }
}
