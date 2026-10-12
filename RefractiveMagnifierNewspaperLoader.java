public class RefractiveMagnifierNewspaperLoader extends Loader {
    private static final StatusStage[] NEWS_MAG_STAGES = {
        new StatusStage(20, "Typesetting 140x55 virtual broadsheet:"),
        new StatusStage(40, "Applying typographic down-sampling blur:"),
        new StatusStage(70, "Grinding astigmatic reading-glass curvature:"),
        new StatusStage(100, "Optical text-resolution matrix locked!")
    };

    private double timeClock = 0.0;
    private final int width = 80;
    private final int height = 22;

    // The virtual high-res newspaper layout
    private static final int NEWS_W = 140;
    private static final int NEWS_H = 55;
    private char[][] newsBuffer;

    public RefractiveMagnifierNewspaperLoader() {
        super(NEWS_MAG_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
        generateNewspaper();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.010; // Slow, deliberate scanning speed for reading

        // Sweeps broadly across the columns of the newspaper
        double glassCenterX = 1.05 * Math.sin(timeClock * 0.35);
        double glassCenterY = 0.35 * Math.sin(timeClock * 0.52) + 0.1;
        double glassCenterZ = -0.6;

        // Significantly larger lens
        double lensRadius = 0.85; 
        double rimThickness = 0.035;
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // Handle positioned further out to accommodate the large lens
        double handleAngle = Math.PI * 0.35; // down-and-right
        double handleDirX = Math.cos(handleAngle);
        double handleDirY = Math.sin(handleAngle);
        double handleStart = lensRadius + rimThickness - 0.02;
        double handleLength = 0.8;
        double handleEnd = handleStart + handleLength;

        for (int y = 0; y < height; y++) {
            double screenY = ((double) y / height) * 2.0 - 1.0;

            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                double screenX = (((double) x / width) * 2.0 - 1.0) * 2.3;

                double paperU = (screenX + 2.3) / 4.6;
                double paperV = (screenY + 1.0) / 2.0;

                double dx = screenX - glassCenterX;
                double dy = screenY - glassCenterY;
                double distToCenter = Math.sqrt(dx * dx + dy * dy);

                // -------------------------------------------------------------
                // OBJECT A: BRASS LENS RIM
                // -------------------------------------------------------------
                if (distToCenter >= lensRadius && distToCenter < lensRadius + rimThickness) {
                    double depthZ = 1.0 / (-glassCenterZ);
                    if (depthZ > zBuffer[idx]) {
                        zBuffer[idx] = depthZ;
                        double nx = dx / distToCenter;
                        double ny = dy / distToCenter;
                        double spec = Math.pow(Math.max(0, nx * lightX + ny * lightY), 12);

                        int r = clamp((int) (180 + spec * 75));
                        int g = clamp((int) (150 + spec * 85));
                        int b = clamp((int) (90 + spec * 65));

                        char renderChar = (spec > 0.4) ? '█' : '▓';
                        outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // OBJECT B: HIGH-POWER READING LENS
                // -------------------------------------------------------------
                if (distToCenter < lensRadius) {
                    double depthZ = 1.0 / (-glassCenterZ);
                    if (depthZ > zBuffer[idx]) {
                        zBuffer[idx] = depthZ;

                        double t = distToCenter / lensRadius; 

                        // Astigmatic (Anamorphic) magnification: 
                        // These exact constants perfectly counteract the aspect ratio difference
                        // between the screen bounds and the 140x55 char grid, achieving 
                        // exactly 1:1 pixel-to-character mapping at the center of the lens!
                        double magX = 0.57 + 0.43 * Math.pow(t, 2.2);
                        double magY = 0.40 + 0.60 * Math.pow(t, 2.2);

                        // Very subtle swirl, just enough to warp the edges
                        double swirlAngle = (1.0 - t) * 0.12;
                        double cosA = Math.cos(swirlAngle), sinA = Math.sin(swirlAngle);
                        double rotX = dx * cosA - dy * sinA;
                        double rotY = dx * sinA + dy * cosA;

                        double sampleX = glassCenterX + rotX * magX;
                        double sampleY = glassCenterY + rotY * magY;
                        double lensU = (sampleX + 2.3) / 4.6;
                        double lensV = (sampleY + 1.0) / 2.0;

                        ScenePixel px = sampleNews(lensU, lensV, true);
                        int r = px.r, g = px.g, b = px.b;
                        char renderChar = px.glyph;

                        // 3D Glass Dome Normal
                        double curveness = 0.85;
                        double nz = Math.sqrt(Math.max(0.0, 1.0 - t * t * curveness * curveness));
                        double nx = -(dx / lensRadius) * curveness;
                        double ny = -(dy / lensRadius) * curveness;
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        nx /= nLen; ny /= nLen; nz /= nLen;

                        double spec = Math.max(0.0, nx * lightX + ny * lightY + nz * lightZ);
                        double glint = Math.pow(spec, 24);
                        double fresnel = (1.0 - nz) * (1.0 - nz);

                        // Cyan/Blue glass tint
                        r = (int) (r * 0.88 + 190 * 0.12);
                        g = (int) (g * 0.88 + 220 * 0.12);
                        b = (int) (b * 0.88 + 250 * 0.12);

                        r = clamp(r + (int) (glint * 170) + (int) (fresnel * 40));
                        g = clamp(g + (int) (glint * 170) + (int) (fresnel * 45));
                        b = clamp(b + (int) (glint * 170) + (int) (fresnel * 55));

                        // Soft white highlight blob 
                        double hlOffX = -0.32, hlOffY = -0.36;
                        double hdx = (dx / lensRadius) - hlOffX;
                        double hdy = ((dy / lensRadius) - hlOffY) * 1.3; 
                        double hlDist = Math.sqrt(hdx * hdx + hdy * hdy);
                        double hlStrength = Math.max(0.0, 1.0 - hlDist / 0.28);
                        hlStrength = hlStrength * hlStrength * 0.70; 

                        r = clamp((int) (r + hlStrength * (255 - r)));
                        g = clamp((int) (g + hlStrength * (255 - g)));
                        b = clamp((int) (b + hlStrength * (255 - b)));
                        
                        if (hlStrength > 0.5) {
                            renderChar = '˚'; 
                        } else if (glint > 0.6) {
                            renderChar = '*'; 
                        }

                        outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // OBJECT C: HANDLE
                // -------------------------------------------------------------
                double along = dx * handleDirX + dy * handleDirY;
                if (along >= handleStart && along <= handleEnd) {
                    double perp = -dx * handleDirY + dy * handleDirX;
                    double s = (along - handleStart) / handleLength; 
                    double halfWidth = 0.055 * (1.0 - 0.25 * s); 

                    if (Math.abs(perp) < halfWidth) {
                        double depthZ = 1.0 / (-glassCenterZ);
                        if (depthZ > zBuffer[idx]) {
                            zBuffer[idx] = depthZ;

                            boolean wrap = Math.sin(s * 30.0) > 0.2;
                            double edgeShade = 1.0 - Math.abs(perp) / halfWidth; 

                            int r, g, b;
                            if (wrap) {
                                r = clamp((int) (90 + edgeShade * 50));
                                g = clamp((int) (50 + edgeShade * 35));
                                b = clamp((int) (30 + edgeShade * 20));
                            } else {
                                r = clamp((int) (50 + edgeShade * 30));
                                g = clamp((int) (30 + edgeShade * 20));
                                b = clamp((int) (15 + edgeShade * 10));
                            }

                            char renderChar = edgeShade > 0.6 ? '█' : (edgeShade > 0.3 ? '▓' : '▒');
                            outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                            continue;
                        }
                    }
                }

                // -------------------------------------------------------------
                // UNMAGNIFIED BACKDROP
                // -------------------------------------------------------------
                ScenePixel px = sampleNews(paperU, paperV, false);
                outputBuffer[idx] = ansi(px.r, px.g, px.b) + px.glyph + RESET;
            }
        }
    }

    // -----------------------------------------------------------------------
    // PROCEDURAL TEXT GENERATOR & RENDERER
    // -----------------------------------------------------------------------

    private ScenePixel sampleNews(double u, double v, boolean isMagnified) {
        int nx = Math.max(0, Math.min(NEWS_W - 1, (int) (u * NEWS_W)));
        int ny = Math.max(0, Math.min(NEWS_H - 1, (int) (v * NEWS_H)));

        char c = newsBuffer[ny][nx];

        // Colors
        int bgR = 210, bgG = 195, bgB = 175; // Faded aged paper
        int inkR = 135, inkG = 125, inkB = 110; // Blurry, faint ink
        
        int magBgR = 245, magBgG = 240, magBgB = 225; // Bright, illuminated paper
        int magInkR = 25, magInkG = 20, magInkB = 15; // Crisp, dark ink

        if (isMagnified) {
            // Inside the lens: High contrast and the exact actual characters
            if (c == ' ') return new ScenePixel(magBgR, magBgG, magBgB, ' ');
            return new ScenePixel(magInkR, magInkG, magInkB, c);
        } else {
            // Outside the lens: Low contrast and characters are replaced with blurry blocks
            if (c == ' ') return new ScenePixel(bgR, bgG, bgB, ' ');
            
            char renderChar = '░';
            if (Character.isUpperCase(c) || c == '=' || c == '|' || c == '_' || c == '-') {
                renderChar = '▒'; // Headers and bold features look slightly denser
            } else if (c == '.' || c == ',') {
                renderChar = '.';
            } else if (c == 'O' || c == '(' || c == ')') {
                renderChar = '▓'; // Keeps the ASCII art slightly recognizable as a blob
            }

            return new ScenePixel(inkR, inkG, inkB, renderChar);
        }
    }

    private void generateNewspaper() {
        newsBuffer = new char[NEWS_H][NEWS_W];
        for (int y = 0; y < NEWS_H; y++) {
            for (int x = 0; x < NEWS_W; x++) {
                newsBuffer[y][x] = ' ';
            }
        }

        // --- DRAW HEADERS ---
        writeString(newsBuffer, 1, 2,  "========================================================================================================================================");
        writeString(newsBuffer, 3, 22, "T H E    D A I L Y    T E R M I N A L    E X P R E S S");
        writeString(newsBuffer, 5, 2,  "========================================================================================================================================");
        writeString(newsBuffer, 6, 4,  "VOL. 42, NO. 9                                      FRIDAY, OCTOBER 3, 2026                                               $2.50");
        writeString(newsBuffer, 7, 2,  "========================================================================================================================================");
        
        writeString(newsBuffer, 10, 4,  "LENS TECH BREAKTHROUGH!             MARKETS SURGE ON NEW DATA             WEATHER: CLOUDY, 100% CHANCE OF ASCII");
        writeString(newsBuffer, 11, 4,  "-----------------------             -------------------------             -------------------------------------");

        // --- DRAW ASCII ART (Coffee Mug) ---
        String[] mug = {
            "   (  )   (   )  )   ",
            "    ) (   )  (  (    ",
            "    ( )  (    )   )  ",
            "  _............_     ",
            " |              |.-. ",
            " |    ASCII     |  | ",
            " |    BREW      |  | ",
            " |              |-'  ",
            "  \\            /     ",
            "   '----------'      "
        };
        for (int i = 0; i < mug.length; i++) {
            writeString(newsBuffer, 13 + i, 78, mug[i]);
        }
        writeString(newsBuffer, 24, 76, "Fig 1. Developers fuel up.");

        // --- DRAW BODY TEXT ---
        String article = "ASCII rendering is experiencing a renaissance. The discovery of a console-based character-to-artwork mapping "
            + "has allowed developers to create stunning optical illusions. By meticulously calculating the luminance of a specific ray "
            + "cast out from an observer, text can be used for illumination in real time. This newspaper serves as a primary test subject. "
            + "To the naked eye, these columns appear as a blur of low-resolution characters, a consequence of hypothetical down-sampling. "
            + "However, beneath the lens, the true fidelity of the high-resolution character matrix is revealed. Market analysts predict "
            + "a massive surge in terminal-based user interfaces. Legacy systems are being upgraded to support 24-bit color ANSI escape codes "
            + "simply to experience the full range of experience. The project is ongoing and contributions are highly sought after."
            + "In other news, a local developer finds a way to compile Java entirely in their mind, producing class files by whistling "
            + "modem tones. 'It is truly a paradigm shift,' said one senior engineer. The aesthetic appeal of monospace typography "
            + "combined with mathematical precision creates a unique visual texture that cannot be replicated by modern rasterization. "
            + "As we look to the future, one thing is absolutely certain: the command line is far from dead. It is evolving. This dense "
            + "matrix of glyphs provides the perfect foundation to test our anamorphic algorithms. Reading this through the sweeping glass "
            + "lens should feel incredibly satisfying, resolving chaos into clarity.";

        // Flow the article text into the 4 columns
        fillColumn(newsBuffer, article, 2, 35, 13, 53);   // Col 1
        fillColumn(newsBuffer, article, 39, 72, 13, 53);  // Col 2
        fillColumn(newsBuffer, article, 76, 108, 26, 53); // Col 3
        fillColumn(newsBuffer, article, 112, 138, 13, 53);// Col 4
    }

    private void writeString(char[][] buf, int r, int c, String s) {
        for (int i = 0; i < s.length() && c + i < NEWS_W; i++) {
            buf[r][c + i] = s.charAt(i);
        }
    }

    private void fillColumn(char[][] buf, String text, int startX, int endX, int startY, int endY) {
        int cursorX = startX;
        int cursorY = startY;
        String[] words = text.split(" ");
        int wordIdx = 0;

        while (cursorY <= endY) {
            String w = words[wordIdx];
            if (cursorX + w.length() > endX) {
                cursorX = startX;
                cursorY++;
                if (cursorY > endY) break;
            }
            for (int i = 0; i < w.length(); i++) {
                if (cursorX <= endX) {
                    buf[cursorY][cursorX] = w.charAt(i);
                    cursorX++;
                }
            }
            if (cursorX <= endX) {
                buf[cursorY][cursorX] = ' ';
                cursorX++;
            }
            wordIdx = (wordIdx + 1) % words.length; // Loop text endlessly
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static String ansi(int r, int g, int b) {
        return String.format("\u001B[38;2;%d;%d;%dm", clamp(r), clamp(g), clamp(b));
    }

    private static final class ScenePixel {
        final int r, g, b;
        final char glyph;

        ScenePixel(int r, int g, int b, char glyph) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.glyph = glyph;
        }
    }
}