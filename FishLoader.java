public class FishLoader extends Loader {

    private static final StatusStage[] FISH_STAGES = {
            new StatusStage(15, "Hatching from the egg..."),
            new StatusStage(35, "Growing silver belly scales..."),
            new StatusStage(60, "Developing deep blue dorsal camo..."),
            new StatusStage(80, "Flexing caudal fin muscles..."),
            new StatusStage(100, "Swimming into the deep blue sea!")
    };

    private static final String LUMINANCE_CHARS = ".,-~:;=!*#$@";

    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String[][] cellCache;

    // Viewing angles and animation time
    private double A = -0.25; // Pitch (slight downward angle)
    private double B = 1.4; // Yaw (angled to see the side and front)
    private int frameTick = 0;

    public FishLoader() {
        super(FISH_STAGES, 80, 24);
    }

    public FishLoader(int w, int h) {
        super(FISH_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        // Randomly pick one of 6 predetermined color variants
        switch ((int) (Math.random() * 6)) {
            case 0: // Classic Bluefin Tuna (Deep Blue / Silver / Yellow)
                primaryColor   = "\u001B[38;2;20;60;140m";   // Deep Ocean Blue
                secondaryColor = "\u001B[38;2;210;220;230m"; // Shimmering Silver
                accentColor    = "\u001B[38;2;255;210;50m";  // Yellow Finlets
                break;
            case 1: // Golden Trout / Koi Variant (Orange / White / Red)
                primaryColor   = "\u001B[38;2;230;120;30m";  // Vibrant Orange
                secondaryColor = "\u001B[38;2;245;240;220m"; // Cream White Belly
                accentColor    = "\u001B[38;2;220;50;50m";   // Reddish Orange Accents
                break;
            case 2: // Neon Tropical Fish (Teal / Magenta / Bright Cyan)
                primaryColor   = "\u001B[38;2;0;150;160m";   // Deep Teal
                secondaryColor = "\u001B[38;2;220;80;180m";  // Magenta Stripe
                accentColor    = "\u001B[38;2;0;240;220m";   // Bright Cyan Fins
                break;
            case 3: // Sunset Pufferfish (Coral Pink / Yellow / Purple)
                primaryColor   = "\u001B[38;2;235;100;120m"; // Coral Pink
                secondaryColor = "\u001B[38;2;255;220;100m"; // Sunny Yellow Belly
                accentColor    = "\u001B[38;2;140;80;180m";  // Deep Purple Accents
                break;
            case 4: // Electric Neon Tetra (Neon Blue / Dark Stripe / Bright Red)
                primaryColor   = "\u001B[38;2;30;144;255m";  // Electric Blue
                secondaryColor = "\u001B[38;2;40;40;40m";    // Dark Metallic Stripe
                accentColor    = "\u001B[38;2;255;50;50m";   // Neon Red Tail
                break;
            case 5: // Emerald Abyss (Deep Green / Lime / Gold)
                primaryColor   = "\u001B[38;2;15;110;70m";   // Emerald Green
                secondaryColor = "\u001B[38;2;120;220;140m"; // Lime Shimmer
                accentColor    = "\u001B[38;2;255;215;0m";   // Gold Highlights
                break;
        }

        String[] fullPalette = { primaryColor, secondaryColor, accentColor };
        cellCache = new String[fullPalette.length][LUMINANCE_CHARS.length()];
        for (int c = 0; c < fullPalette.length; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = fullPalette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        frameTick++;
        
        // Gentle global sway
        double currentA = A + 0.05 * Math.sin(frameTick * 0.02);
        double currentB = B + 0.1 * Math.sin(frameTick * 0.015);

        double sinA = Math.sin(currentA), cosA = Math.cos(currentA);
        double sinB = Math.sin(currentB), cosB = Math.cos(currentB);

        // ==========================================
        // 1. THE STREAMLINED BODY
        // ==========================================
        // X ranges from -2.2 (Tail) to 2.2 (Nose)
        double bodyStartX = -2.2;
        double bodyEndX = 2.2;
        double bodyLength = bodyEndX - bodyStartX;

        for (double x = bodyStartX; x <= bodyEndX; x += 0.05) {
            double normX = (x - bodyStartX) / bodyLength; // 0.0 (tail) to 1.0 (nose)
            
            // Traveling sine wave for swimming motion. 
            // Amplitude is higher at the tail (normX near 0) and minimal at the nose.
            double swimAmplitude = 0.5 * Math.pow(1.0 - normX, 1.5);
            double swimZ = swimAmplitude * Math.sin(x * 3.0 + frameTick * 0.25);

            // Ellipsoid profile using sine wave
            double height = 0.85 * Math.sin(normX * Math.PI) * (1.1 - 0.2 * normX);
            double width  = 0.35 * Math.sin(normX * Math.PI);

            for (double t = 0; t < 2 * Math.PI; t += 0.15) {
                double py = height * Math.cos(t); // Y is up/down
                double pz = width * Math.sin(t) + swimZ; // Z is depth, factoring in the swim wave
                
                // Two-tone counter-shading coloring (Blue on top, Silver on bottom)
                int colorIdx = (py > 0.1) ? 0 : 1;

                // Approximate normals
                double nx = (0.5 - normX); // Curvature towards ends
                double ny = Math.cos(t);
                double nz = Math.sin(t);
                double len = Math.sqrt(nx*nx + ny*ny + nz*nz);

                drawPoint(x, py, pz, nx/len, ny/len, nz/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, colorIdx);
            }
        }

        // ==========================================
        // 2. CAUDAL FIN (The crescent tail)
        // ==========================================
        for (double tx = -2.8; tx <= -2.1; tx += 0.02) {
            double normT = (-2.1 - tx) / 0.7; // 0 at base, 1 at tips
            double tailHeight = 1.0 * Math.pow(normT, 0.7);
            
            // Tail matches the maximum amplitude of the swim wave
            double swimZ = (0.5 * Math.pow(1.0 - 0.0, 1.5)) * Math.sin(tx * 3.0 + frameTick * 0.25);

            for (double ty = -tailHeight; ty <= tailHeight; ty += 0.03) {
                // Crescent cutout in the middle
                if (Math.abs(ty) < 0.2 + 0.6 * (1.0 - normT)) continue;

                drawPoint(tx, ty, swimZ, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 0);
            }
        }

        // ==========================================
        // 3. DORSAL & ANAL FINS (Top & Bottom)
        // ==========================================
        for (double fx = -0.5; fx <= 0.8; fx += 0.04) {
            double normF = (fx - (-0.5)) / 1.3;
            // Swim wave offset at this exact X position
            double swimAmplitude = 0.5 * Math.pow(1.0 - ((fx - bodyStartX) / bodyLength), 1.5);
            double swimZ = swimAmplitude * Math.sin(fx * 3.0 + frameTick * 0.25);

            // Dorsal Fin (Top)
            double dorsalH = 0.6 * (1.0 - Math.pow(normF * 2.0 - 1.0, 2));
            if (dorsalH > 0) {
                double baseTopY = 0.85 * Math.sin(((fx - bodyStartX) / bodyLength) * Math.PI) * (1.1 - 0.2 * ((fx - bodyStartX) / bodyLength));
                for (double fy = baseTopY; fy <= baseTopY + dorsalH; fy += 0.04) {
                    drawPoint(fx, fy, swimZ, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                }
            }

            // Anal Fin (Bottom)
            double analH = 0.4 * (1.0 - Math.pow(normF * 2.0 - 1.0, 2));
            if (analH > 0) {
                double baseBotY = -(0.85 * Math.sin(((fx - bodyStartX) / bodyLength) * Math.PI) * (1.1 - 0.2 * ((fx - bodyStartX) / bodyLength)));
                for (double fy = baseBotY - analH; fy <= baseBotY; fy += 0.04) {
                    drawPoint(fx, fy, swimZ, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
                }
            }
        }

        // ==========================================
        // 4. PECTORAL FINS (Side fins)
        // ==========================================
        double pecBaseX = 1.0;
        double pecBaseY = -0.1;
        double pecAmplitude = 0.5 * Math.pow(1.0 - ((pecBaseX - bodyStartX) / bodyLength), 1.5);
        double pecBaseZ = pecAmplitude * Math.sin(pecBaseX * 3.0 + frameTick * 0.25);

        for (double px = 0.4; px <= pecBaseX; px += 0.04) {
            double normP = (px - 0.4) / (pecBaseX - 0.4);
            double pecExtend = 0.4 * normP;
            
            // Left side
            drawPoint(px, pecBaseY, pecBaseZ + 0.3 + pecExtend, 0.0, -1.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            drawPoint(px, pecBaseY + 0.05, pecBaseZ + 0.3 + pecExtend, 0.0, -1.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            
            // Right side
            drawPoint(px, pecBaseY, pecBaseZ - 0.3 - pecExtend, 0.0, -1.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
            drawPoint(px, pecBaseY + 0.05, pecBaseZ - 0.3 - pecExtend, 0.0, -1.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, 2);
        }

        B -= 0.008;
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz, double sinA, double cosA,
                           double sinB, double cosB, String[] outputBuffer, double[] zBuffer, int colorIndex) {
        
        // Yaw (B) -> Pitch (A) Rotation
        double x1 = x * cosB - z * sinB;
        double z1 = x * sinB + z * cosB;
        
        double y2 = y * cosA - z1 * sinA;
        double z2 = y * sinA + z1 * cosA;
        double x2 = x1;
        
        double distance = 3.9;
        double ooZ = 1.0 / (z2 + distance);
        
        // Orthographic/Perspective Projection Screen Mapping
        int xp = (int) (window_width / 2.0 + 38 * ooZ * x2);
        int yp = (int) (window_height / 2.0 - 18 * ooZ * y2); // Note: Y axis flip for console output
        int bufferIndex = xp + window_width * yp;

        if (yp >= 0 && yp < window_height && xp >= 0 && xp < window_width) {
            
            // Rotate the normal vectors to keep lighting anchored to the camera
            double nx1 = nx * cosB - nz * sinB;
            double nz1 = nx * sinB + nz * cosB;
            double ny2 = ny * cosA - nz1 * sinA;
            double nz2 = ny * sinA + nz1 * cosA;
            
            // Basic directional lighting from Top-Front-Right
            double lx = 0.5, ly = 0.8, lz = 0.3;
            double length = Math.sqrt(lx*lx + ly*ly + lz*lz);
            lx /= length; ly /= length; lz /= length;

            double luminance = nx1 * lx + ny2 * ly + nz2 * lz;

            if (ooZ > zBuffer[bufferIndex]) {
                zBuffer[bufferIndex] = ooZ;
                
                int charIndex = (int) ((luminance + 1.0) / 2.0 * (LUMINANCE_CHARS.length() - 1));
                charIndex = Math.max(0, Math.min(LUMINANCE_CHARS.length() - 1, charIndex));
                
                outputBuffer[bufferIndex] = cellCache[colorIndex][charIndex];
            }
        }
    }
}