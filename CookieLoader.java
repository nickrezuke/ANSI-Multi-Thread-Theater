public class CookieLoader extends Loader {

    private static final StatusStage[] COOKIE_STAGES = {
            new StatusStage(20, "Mixing the dough:"),
            new StatusStage(35, "Portioning dough balls:"),
            new StatusStage(55, "Baking in the oven:"),
            new StatusStage(75, "Cooling on the rack:"),
            new StatusStage(92, "Finishing touches:"),
            new StatusStage(100, "Fresh Batch & Ready!")
    };

    private static final String LUMINANCE_CHARS = ":;=!*#$@\u2592\u2593\u2588";

    private static final int PRIMARY = 0; 
    private static final int SECONDARY = 1;
    private static final int FILLING = 2; 
    private static final int ACCENT_A = 3; 
    private static final int ACCENT_B = 4; 
    private static final int ACCENT_C = 5; 

    private static final int cookieShuffleTime = 800;
    private long lastShuffle = System.currentTimeMillis();

    private static final double[][] CHOC_CHIPS = {
            { -1.6, 0.4 }, { -1.1, -0.7 }, { -0.6, 0.9 }, { -0.15, -0.25 }, { 0.35, 0.65 },
            { 0.75, -0.85 }, { 1.15, 0.2 }, { 1.5, -0.45 }, { 1.7, 0.7 }, { -1.9, -0.15 },
            { 0.05, 1.3 }, { -0.95, 1.35 }, { 1.85, -0.05 }, { 0.5, -1.5 }
    };

    private static final double[][] SUGAR_SPRINKLES = {
            { -1.6, 0.5 }, { -1.1, -0.8 }, { -0.5, 0.9 }, { 0.05, -0.3 }, { 0.45, 0.75 },
            { 0.95, -0.9 }, { 1.3, 0.3 }, { 1.7, -0.5 }, { -1.9, 0.1 }, { 0.2, 1.4 },
            { -0.8, 1.3 }, { 1.5, 0.9 }, { -0.3, -1.4 }, { 1.0, 1.15 }, { -1.4, -1.0 }
    };

    private int cookieType = -1;
    private String[][] cellCache;

    private double A = 1.05; 
    private double B = 0.0;

    public CookieLoader() {
        super(COOKIE_STAGES, 80, 22);
    }

    public CookieLoader(int w, int h) {
        super(COOKIE_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        int oldCookieType = cookieType;
        while (cookieType == oldCookieType) {
            // Never get two in a row
            cookieType = (int) (Math.random() * 8) + 1;
        }

        String primary, secondary, filling, accentA, accentB, accentC;
        //cookieType = 4; // TODO REMOVE THIS
        switch (cookieType) {
            case 1: // CHOCOLATE CHIP
                primary = "\u001B[38;5;222m";
                secondary = "\u001B[38;5;173m"; 
                filling = secondary;
                accentA = "\u001B[38;5;52m"; 
                accentB = accentA;
                accentC = accentA;
                break;

            case 2: // OREO
                // TODO: Add a design to the oreo cookie instead of circles
                primary = "\u001B[38;5;235m"; 
                secondary = "\u001B[38;5;239m"; 
                filling = "\u001B[38;5;230m"; 
                accentA = filling;
                accentB = filling;
                accentC = filling;
                break;

            case 3: { // FRENCH MACARON
                String[][] pairs = {
                        { "\u001B[38;5;217m", "\u001B[38;5;95m" }, 
                        { "\u001B[38;5;158m", "\u001B[38;5;255m" }, 
                        { "\u001B[38;5;183m", "\u001B[38;5;96m" }, 
                        { "\u001B[38;5;228m", "\u001B[38;5;204m" } 
                };
                String[] chosen = pairs[(int) (Math.random() * pairs.length)];
                primary = chosen[0];
                secondary = primary;
                filling = chosen[1];
                accentA = filling;
                accentB = filling;
                accentC = filling;
                break;
            }

            case 4: // STROOPWAFEL
                // TODO improve this one its not visually striking...
                primary = "\u001B[38;5;179m"; 
                secondary = "\u001B[38;5;136m"; 
                filling = "\u001B[38;5;166m"; 
                accentA = filling;
                accentB = filling;
                accentC = filling;
                break;

            case 5: // CHECKERBOARD
                // TODO Improve this it looks like a chessboard lmao
                primary = "\u001B[38;5;94m"; 
                secondary = "\u001B[38;5;230m"; 
                filling = "\u001B[38;5;215m"; 
                accentA = filling;
                accentB = filling;
                accentC = filling;
                break;

            case 6: { // SUGAR COOKIE
                String[] frostings = {
                        "\u001B[38;5;218m", 
                        "\u001B[38;5;153m", 
                        "\u001B[38;5;183m", 
                        "\u001B[38;5;158m" 
                };
                primary = "\u001B[38;5;223m"; 
                secondary = frostings[(int) (Math.random() * frostings.length)];
                filling = secondary;
                accentA = "\u001B[38;5;255m"; 
                accentB = "\u001B[38;5;196m"; 
                accentC = "\u001B[38;5;226m"; 
                break;
            }

            case 7: // SNICKERDOODLE
                primary = "\u001B[38;5;180m"; 
                secondary = "\u001B[38;5;130m"; 
                filling = secondary;
                accentA = secondary;
                accentB = secondary;
                accentC = secondary;
                break;

            case 8:
            default: // LINZER
                primary = "\u001B[38;5;222m"; 
                secondary = primary;
                filling = "\u001B[38;5;160m"; 
                accentA = "\u001B[38;5;255m"; 
                accentB = accentA;
                accentC = accentA;
                break;
        }

        String[] fullPalette = { primary, secondary, filling, accentA, accentB, accentC };
        cellCache = new String[fullPalette.length][LUMINANCE_CHARS.length()];
        for (int c = 0; c < fullPalette.length; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = fullPalette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long now = System.currentTimeMillis();
        if(now > lastShuffle + cookieShuffleTime) {
            lastShuffle = now;
            initialize();
        }

        double sinA = Math.sin(A), cosA = Math.cos(A);
        double sinB = Math.sin(B), cosB = Math.cos(B);

        switch (cookieType) {
            case 1: renderChocolateChip(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
            case 2: renderOreo(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
            case 3: renderMacaron(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
            case 4: renderStroopwafel(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
            case 5: renderCheckerboard(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
            case 6: renderSugarCookie(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
            case 7: renderSnickerdoodle(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
            case 8: default: renderLinzer(sinA, cosA, sinB, cosB, outputBuffer, zBuffer); break;
        }

        A += 0.02 * Math.sin(B);
        B += 0.03;
    }

    // ==========================================
    // 1. CHOCOLATE CHIP - domed top, shorter base, raised chip bumps
    // ==========================================
    private void renderChocolateChip(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double RADIUS = 2.5;
        final double BASE_H = 0.25;
        final double DOME_H = 0.40;
        final double step = 0.04;

        for (double x = -RADIUS; x <= RADIUS; x += step) {
            for (double y = -RADIUS; y <= RADIUS; y += step) {
                double r = Math.sqrt(x * x + y * y);
                if (r > RADIUS) continue;

                double rNorm = r / RADIUS;
                double term = Math.max(0.0001, 1.0 - rNorm * rNorm); 
                double domeZ = DOME_H * Math.sqrt(term);
                double zTop = BASE_H / 2.0 + domeZ;

                // Normals for the dome (approximated elliptical cap)
                double dzdx = -DOME_H * x / (RADIUS * RADIUS * Math.sqrt(term));
                double dzdy = -DOME_H * y / (RADIUS * RADIUS * Math.sqrt(term));
                double len = Math.sqrt(dzdx*dzdx + dzdy*dzdy + 1.0);

                boolean toasty = hashNoise(Math.floor(x * 9.0), Math.floor(y * 9.0)) > 0.82;
                int topColor = toasty ? SECONDARY : PRIMARY;
                drawPoint(x, y, zTop, -dzdx/len, -dzdy/len, 1.0/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, topColor);
                drawPoint(x, y, -BASE_H / 2.0, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, SECONDARY);
            }
        }

        for (double theta = 0; theta < 2 * Math.PI; theta += 0.03) {
            double cosT = Math.cos(theta), sinT = Math.sin(theta);
            for (double z = -BASE_H / 2.0; z <= BASE_H / 2.0; z += 0.05) {
                boolean toasty = hashNoise(Math.floor(theta * 10.0), Math.floor(z * 20.0)) > 0.82;
                int color = toasty ? SECONDARY : PRIMARY;
                drawPoint(RADIUS * cosT, RADIUS * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, color);
            }
        }

        for (double[] chip : CHOC_CHIPS) {
            double cx = chip[0], cy = chip[1];
            double rNorm = Math.sqrt(cx*cx + cy*cy) / RADIUS;
            double term = Math.max(0.0001, 1.0 - rNorm * rNorm);
            double zCenter = BASE_H / 2.0 + DOME_H * Math.sqrt(term);
            double chipR = 0.24;

            for (double t = 0.0; t < Math.PI / 2.0; t += 0.28) {
                double sinT = Math.sin(t), cosT = Math.cos(t);
                for (double p = 0; p < 2 * Math.PI; p += 0.3) {
                    double sinP = Math.sin(p), cosP = Math.cos(p);
                    double sx = cx + chipR * sinT * cosP;
                    double sy = cy + chipR * sinT * sinP;
                    double sz = zCenter + chipR * cosT * 0.7; 
                    drawPoint(sx, sy, sz, sinT * cosP, sinT * sinP, cosT, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, ACCENT_A);
                }
            }
        }
    }

    // ==========================================
    // 2. OREO - Double stuf, outer ridges, inner embossed geometric noise
    // ==========================================
    private void renderOreo(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double WAFER_R = 2.3;
        final double CREAM_R = 2.38; 
        final double WAFER_H = 0.16;
        final double CREAM_H = 0.45; // Double Stuf
        final double zTop = CREAM_H / 2 + WAFER_H;
        final double zBot = -zTop;
        final double step = 0.04;

        for (double x = -WAFER_R; x <= WAFER_R; x += step) {
            for (double y = -WAFER_R; y <= WAFER_R; y += step) {
                double r = Math.sqrt(x * x + y * y);
                if (r > WAFER_R) continue;

                double angle = Math.atan2(y, x);
                // Creates ridged edge, ring border, and a center embossed graphic
                boolean isRidge = (r > WAFER_R * 0.82 && Math.sin(angle * 70.0) > 0.0);
                boolean isRing = Math.abs(r - WAFER_R * 0.55) < 0.1;
                boolean isCenterNoise = r < WAFER_R * 0.45 && (hashNoise(Math.floor(x * 7.0), Math.floor(y * 7.0)) > 0.6);
                boolean emboss = isRidge || isRing || isCenterNoise;

                int topColor = emboss ? SECONDARY : PRIMARY;
                drawPoint(x, y, zTop, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, topColor);
                drawPoint(x, y, zBot, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }

        for (double theta = 0; theta < 2 * Math.PI; theta += 0.03) {
            double cosT = Math.cos(theta), sinT = Math.sin(theta);
            for (double z = zBot; z <= zBot + WAFER_H; z += 0.04) {
                drawPoint(WAFER_R * cosT, WAFER_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
            for (double z = -CREAM_H / 2; z <= CREAM_H / 2; z += 0.02) {
                drawPoint(CREAM_R * cosT, CREAM_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
            }
            for (double z = CREAM_H / 2; z <= zTop; z += 0.04) {
                drawPoint(WAFER_R * cosT, WAFER_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }
    }

    // ==========================================
    // 3. FRENCH MACARON
    // ==========================================
    private void renderMacaron(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double SHELL_R = 1.85;
        final double DOME_H = 0.55;
        final double FOOT_H = 0.22;

        for (double t = 0; t <= Math.PI / 2.0; t += 0.05) {
            double sinT = Math.sin(t), cosT = Math.cos(t);
            for (double p = 0; p < 2 * Math.PI; p += 0.04) {
                double sinP = Math.sin(p), cosP = Math.cos(p);
                double x = SHELL_R * sinT * cosP;
                double y = SHELL_R * sinT * sinP;
                double z = FOOT_H / 2 + DOME_H * cosT;
                drawPoint(x, y, z, sinT * cosP, sinT * sinP, cosT, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }

        for (double t = 0; t <= Math.PI / 2.0; t += 0.05) {
            double sinT = Math.sin(t), cosT = Math.cos(t);
            for (double p = 0; p < 2 * Math.PI; p += 0.04) {
                double sinP = Math.sin(p), cosP = Math.cos(p);
                double x = SHELL_R * sinT * cosP;
                double y = SHELL_R * sinT * sinP;
                double z = -FOOT_H / 2 - DOME_H * cosT;
                drawPoint(x, y, z, sinT * cosP, sinT * sinP, -cosT, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }

        final double ruffleFreq = 20.0;
        final double ruffleAmp = 0.09;
        for (double z = -FOOT_H / 2; z <= FOOT_H / 2; z += 0.02) {
            for (double p = 0; p < 2 * Math.PI; p += 0.03) {
                double radius = SHELL_R + ruffleAmp * Math.sin(p * ruffleFreq);
                double cosP = Math.cos(p), sinP = Math.sin(p);
                drawPoint(radius * cosP, radius * sinP, z, cosP, sinP, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
            }
        }
    }

    // ==========================================
    // 4. STROOPWAFEL - 3D cosine wave intersecting waffle pattern
    // ==========================================
    private void renderStroopwafel(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double SW_R = 2.7;
        final double WAFER_H = 0.10;
        final double CARAMEL_H = 0.14;
        final double PEEK = 0.05;
        final double zTop = CARAMEL_H / 2 + WAFER_H;
        final double zBot = -zTop;
        
        final double cell = 0.7;
        final double pi2C = 2.0 * Math.PI / cell;
        final double amp = 0.035;
        final double step = 0.035;

        for (double x = -SW_R; x <= SW_R; x += step) {
            for (double y = -SW_R; y <= SW_R; y += step) {
                if (x * x + y * y > SW_R * SW_R) continue;

                double U = pi2C * (x + y);
                double V = pi2C * (x - y);
                double wave = Math.cos(U) + Math.cos(V); // Interfering waves for the waffle block grid

                // Top Waffle
                double dzdx = -amp * pi2C * (Math.sin(U) + Math.sin(V));
                double dzdy = -amp * pi2C * (Math.sin(U) - Math.sin(V));
                double len = Math.sqrt(dzdx * dzdx + dzdy * dzdy + 1.0);
                int color = wave < -0.5 ? SECONDARY : PRIMARY; // Grooves are slightly darker

                drawPoint(x, y, zTop + amp * wave, -dzdx/len, -dzdy/len, 1.0/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, color);
                
                // Bottom Waffle (normal points outwards and wave points down)
                drawPoint(x, y, zBot - amp * wave, -dzdx/len, -dzdy/len, -1.0/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, color);
            }
        }

        for (double theta = 0; theta < 2 * Math.PI; theta += 0.03) {
            double cosT = Math.cos(theta), sinT = Math.sin(theta);
            for (double z = zBot; z <= zBot + WAFER_H; z += 0.03) {
                drawPoint(SW_R * cosT, SW_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
            for (double z = -CARAMEL_H / 2; z <= CARAMEL_H / 2; z += 0.03) {
                drawPoint((SW_R + PEEK) * cosT, (SW_R + PEEK) * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
            }
            for (double z = CARAMEL_H / 2; z <= zTop; z += 0.03) {
                drawPoint(SW_R * cosT, SW_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }
    }

    // ==========================================
    // 5. CHECKERBOARD - Square Schackrutor Swedish checkerboard block
    // ==========================================
    private void renderCheckerboard(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double CB_R = 2.4; 
        final double CB_H = 0.65;
        final double cell = 0.5;
        final double step = 0.035;

        // Flat Top and Bottom faces spanning the whole square
        for (double x = -CB_R; x <= CB_R; x += step) {
            for (double y = -CB_R; y <= CB_R; y += step) {
                int ix = (int) Math.floor((x + CB_R) / cell);
                int iy = (int) Math.floor((y + CB_R) / cell);
                int topColor = ((ix + iy) % 2 == 0) ? PRIMARY : SECONDARY;
                
                drawPoint(x, y, CB_H / 2, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, topColor);
                drawPoint(x, y, -CB_H / 2, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
            }
        }

        // Render the 4 square perimeter walls
        for (double s = -CB_R; s <= CB_R; s += step) {
            for (double z = -CB_H / 2; z <= CB_H / 2; z += 0.05) {
                drawPoint(CB_R, s, z, 1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
                drawPoint(-CB_R, s, z, -1.0, 0.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
                drawPoint(s, CB_R, z, 0.0, 1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
                drawPoint(s, -CB_R, z, 0.0, -1.0, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
            }
        }
    }

    // ==========================================
    // 6. SUGAR COOKIE - Domed top with slightly raised frosting cap & sprinkles
    // ==========================================
    private void renderSugarCookie(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double SC_R = 2.5;
        final double BASE_H = 0.25;
        final double DOME_H = 0.35;
        final double FROST_H = 0.09;
        final double FROST_R = SC_R * 0.90;
        final double step = 0.04;

        for (double x = -SC_R; x <= SC_R; x += step) {
            for (double y = -SC_R; y <= SC_R; y += step) {
                double r = Math.sqrt(x * x + y * y);
                if (r > SC_R) continue;

                double rNorm = r / SC_R;
                double term = Math.max(0.0001, 1.0 - rNorm * rNorm);
                double domeZ = DOME_H * Math.sqrt(term);
                double zTop = BASE_H / 2.0 + domeZ;

                double dzdx = -DOME_H * x / (SC_R * SC_R * Math.sqrt(term));
                double dzdy = -DOME_H * y / (SC_R * SC_R * Math.sqrt(term));
                double len = Math.sqrt(dzdx*dzdx + dzdy*dzdy + 1.0);

                if (r < FROST_R) {
                    drawPoint(x, y, zTop + FROST_H, -dzdx/len, -dzdy/len, 1.0/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, SECONDARY);
                } else {
                    drawPoint(x, y, zTop, -dzdx/len, -dzdy/len, 1.0/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
                }
                drawPoint(x, y, -BASE_H / 2.0, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }

        // Cookie Side Walls
        for (double theta = 0; theta < 2 * Math.PI; theta += 0.03) {
            double cosT = Math.cos(theta), sinT = Math.sin(theta);
            for (double z = -BASE_H / 2.0; z <= BASE_H / 2.0; z += 0.05) {
                drawPoint(SC_R * cosT, SC_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }

        // Frosting Edge Walls (to connect the cap physically to the dome)
        for (double theta = 0; theta < 2 * Math.PI; theta += 0.03) {
            double cosT = Math.cos(theta), sinT = Math.sin(theta);
            double rNorm = FROST_R / SC_R;
            double term = Math.max(0.0001, 1.0 - rNorm * rNorm);
            double baseZ = BASE_H / 2.0 + DOME_H * Math.sqrt(term);
            for (double z = baseZ; z <= baseZ + FROST_H; z += 0.04) {
                drawPoint(FROST_R * cosT, FROST_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, SECONDARY);
            }
        }

        for (int i = 0; i < SUGAR_SPRINKLES.length; i++) {
            double sx = SUGAR_SPRINKLES[i][0];
            double sy = SUGAR_SPRINKLES[i][1];
            double r = Math.sqrt(sx*sx + sy*sy);
            if (r > FROST_R * 0.95) continue;

            double rNorm = r / SC_R;
            double term = Math.max(0.0001, 1.0 - rNorm * rNorm);
            double zCenter = BASE_H / 2.0 + DOME_H * Math.sqrt(term) + FROST_H + 0.02;

            int color = (i % 3 == 0) ? ACCENT_A : (i % 3 == 1) ? ACCENT_B : ACCENT_C;
            for (double k = -1.0; k <= 1.0; k += 1.0) {
                drawPoint(sx + k * 0.05, sy + k * 0.03, zCenter, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, color);
            }
        }
    }

    // ==========================================
    // 7. SNICKERDOODLE - Domed top with baked cinnamon speckles
    // ==========================================
    private void renderSnickerdoodle(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double SD_R = 2.5;
        final double BASE_H = 0.25;
        final double DOME_H = 0.35;
        final double step = 0.04;

        for (double x = -SD_R; x <= SD_R; x += step) {
            for (double y = -SD_R; y <= SD_R; y += step) {
                double r = Math.sqrt(x * x + y * y);
                if (r > SD_R) continue;

                double rNorm = r / SD_R;
                double term = Math.max(0.0001, 1.0 - rNorm * rNorm);
                double domeZ = DOME_H * Math.sqrt(term);
                double zTop = BASE_H / 2.0 + domeZ;

                double dzdx = -DOME_H * x / (SD_R * SD_R * Math.sqrt(term));
                double dzdy = -DOME_H * y / (SD_R * SD_R * Math.sqrt(term));
                double len = Math.sqrt(dzdx*dzdx + dzdy*dzdy + 1.0);

                boolean topSpeck = hashNoise(Math.floor(x * 13.0), Math.floor(y * 13.0)) > 0.80;
                boolean botSpeck = hashNoise(Math.floor(x * 13.0 + 5.0), Math.floor(y * 13.0 + 5.0)) > 0.80;
                
                drawPoint(x, y, zTop, -dzdx/len, -dzdy/len, 1.0/len, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, topSpeck ? SECONDARY : PRIMARY);
                drawPoint(x, y, -BASE_H / 2.0, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, botSpeck ? SECONDARY : PRIMARY);
            }
        }

        for (double theta = 0; theta < 2 * Math.PI; theta += 0.03) {
            double cosT = Math.cos(theta), sinT = Math.sin(theta);
            for (double z = -BASE_H / 2.0; z <= BASE_H / 2.0; z += 0.05) {
                boolean speck = hashNoise(Math.floor(theta * 11.0), Math.floor(z * 22.0)) > 0.80;
                drawPoint(SD_R * cosT, SD_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, speck ? SECONDARY : PRIMARY);
            }
        }
    }

    // ==========================================
    // 8. LINZER
    // ==========================================
    private void renderLinzer(double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer) {
        final double LZ_R = 2.2;
        final double DOUGH_H = 0.28;
        final double JAM_H = 0.16;
        final double HOLE_R = 0.55;
        final double zTop = JAM_H / 2 + DOUGH_H;
        final double zBot = -zTop;
        final double step = 0.035;

        for (double x = -LZ_R; x <= LZ_R; x += step) {
            for (double y = -LZ_R; y <= LZ_R; y += step) {
                double r = Math.sqrt(x * x + y * y);
                if (r > LZ_R) continue;
                if (r < HOLE_R) {
                    drawPoint(x, y, JAM_H / 2, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
                } else {
                    boolean sugar = hashNoise(Math.floor(x * 10.0), Math.floor(y * 10.0)) > 0.82;
                    drawPoint(x, y, zTop, 0.0, 0.0, 1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, sugar ? ACCENT_A : PRIMARY);
                }
            }
        }

        for (double x = -LZ_R; x <= LZ_R; x += step) {
            for (double y = -LZ_R; y <= LZ_R; y += step) {
                if (x * x + y * y > LZ_R * LZ_R) continue;
                drawPoint(x, y, zBot, 0.0, 0.0, -1.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }

        for (double theta = 0; theta < 2 * Math.PI; theta += 0.03) {
            double cosT = Math.cos(theta), sinT = Math.sin(theta);
            for (double z = zBot; z <= zBot + DOUGH_H; z += 0.03) {
                drawPoint(LZ_R * cosT, LZ_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
            for (double z = -JAM_H / 2; z <= JAM_H / 2; z += 0.03) {
                drawPoint(LZ_R * cosT, LZ_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, FILLING);
            }
            for (double z = JAM_H / 2; z <= zTop; z += 0.03) {
                drawPoint(LZ_R * cosT, LZ_R * sinT, z, cosT, sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
            for (double z = JAM_H / 2; z <= zTop; z += 0.03) {
                drawPoint(HOLE_R * cosT, HOLE_R * sinT, z, -cosT, -sinT, 0.0, sinA, cosA, sinB, cosB, outputBuffer, zBuffer, PRIMARY);
            }
        }
    }

    private void drawPoint(double x, double y, double z, double nx, double ny, double nz,
            double sinA, double cosA, double sinB, double cosB,
            String[] outputBuffer, double[] zBuffer, int colorIndex) {

        double x1 = x * cosB - y * sinB;
        double y1 = x * sinB + y * cosB;

        double y2 = y1 * cosA - z * sinA;
        double z2 = y1 * sinA + z * cosA;
        double x2 = x1;

        double distance = 3.6;
        double ooZ = 1.0 / (z2 + distance);

        int xp = (int) (window_width / 2.0 + 35 * ooZ * x2);
        int yp = (int) (window_height / 2.0 + 15 * ooZ * y2);

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

    private static double hashNoise(double a, double b) {
        double n = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453;
        return n - Math.floor(n);
    }
}