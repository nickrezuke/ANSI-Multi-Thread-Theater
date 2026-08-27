public class LouvreLoader extends Loader {
    private static final StatusStage[] LOUVRE_STAGES = {
            new StatusStage(25, "Calibrating real-time solar clock:"),
            new StatusStage(50, "Assembling space-frame ironwork:"),
            new StatusStage(75, "Balancing day-to-night palette registers:"),
            new StatusStage(100, "Parisian Dual-Cycle Louvre Operational!")
    };

    // Architectural character layout assets
    private static final char CH_NODE = '\u22C5'; // ⋅ Clean, isolated node intersections
    private static final char CH_STRUT = '\u00B7'; // · Razor-thin structural frame cables
    private static final char CH_GLASS = '\u2591'; // ░ Translucent glass panels
    private static final char CH_WATER = '\u2058'; // ⁘ Courtyard plaza concrete / basin floor texture

    // Some color choicess for the Day / Night cycle to fade between
    // Gleaming, modern, brushed aluminum/stainless steel structures 
    private static final int[] DAY_STRUT = { 100, 105, 110 }; 
    // Transparent, highly reflective extra-clear glass (Saint-Gobain Diamond Glass) catching the Parisian sky
    private static final int[] DAY_GLASS = { 165, 195, 210 }; 
    // The famous historic Cour Napoléon sandy French limestone (Charentes/Oise stone)
    private static final int[] DAY_FLOOR = { 230, 218, 198 }; 

    // Deep dark obsidian gray/black silhouette against the internal museum illumination
    private static final int[] NGT_STRUT = { 30, 32, 35 }; 
    // The rich, intense halogen/LED warm candle golden-amber designed by lighting architect I.M. Pei
    private static final int[] NGT_GLASS = { 255, 155, 20 }; 
    // Dark Parisian cobblestone/wet basin stone reflecting deep indigo midnight shadows
    private static final int[] NGT_FLOOR = { 12, 18, 28 }; 


    private double timeClock = 0.0;
    private double rotationY = 0.0;

    private static final double STATIC_TILT_X = 0.1;
    private static final double CAMERA_DISTANCE = 3.3;

    public LouvreLoader() {
        // This uses 80x22 specifically
        super(LOUVRE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
    }

    private static class SortedFace {
        int faceIndex;
        double avgZ;

        SortedFace(int faceIndex, double avgZ) {
            this.faceIndex = faceIndex;
            this.avgZ = avgZ;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        rotationY += 0.014;
        timeClock += 0.014;

        double cosX = Math.cos(STATIC_TILT_X);
        double sinX = Math.sin(STATIC_TILT_X);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // 1. TIMELINE FACTOR MATRIX: 0.0 = Crisp Noon Day, 1.0 = Glowing Midnight
        double nightFactor = 0.5 + 0.5 * Math.sin(timeClock);
        double dayFactor = 1.0 - nightFactor;

        // Soft breathing interior lamp pulse active exclusively during night hours
        double lightingPulse = 0.80 + 0.20 * Math.sin(rotationY * 2.0);
        double nightGlowWeight = nightFactor * lightingPulse;

        // STEP 1: Render Courtyard Plaza Floor underneath
        double floorY = 0.2;
        for (double fx = -1.8; fx <= 1.8; fx += 0.05) {
            for (double fz = -1.8; fz <= 1.8; fz += 0.05) {
                double rX_spun = fx * cosY + fz * sinY;
                double rY_spun = floorY;
                double rZ_spun = -fx * sinY + fz * cosY;

                double rx = rX_spun;
                double ry = rY_spun * cosX - rZ_spun * sinX;
                double rz = rY_spun * sinX + rZ_spun * cosX;

                double ooz = 1.0 / (rz + CAMERA_DISTANCE);
                int xp = (int) (40 + 52 * ooz * rx);
                int yp = (int) (11 + 26 * ooz * ry);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        int fr = (int) (DAY_FLOOR[0] * dayFactor + NGT_FLOOR[0] * nightFactor);
                        int fg = (int) (DAY_FLOOR[1] * dayFactor + NGT_FLOOR[1] * nightFactor);
                        int fb = (int) (DAY_FLOOR[2] * dayFactor + NGT_FLOOR[2] * nightFactor);

                        double reflectionFalloff = 0.16 * nightGlowWeight
                                * (1.0 - Math.min(1.0, Math.sqrt(fx * fx + fz * fz) / 1.6));
                        fr = Math.min(255, fr + (int) (NGT_GLASS[0] * reflectionFalloff));
                        fg = Math.min(255, fg + (int) (NGT_GLASS[1] * reflectionFalloff));
                        fb = Math.min(255, fb + (int) (NGT_GLASS[2] * reflectionFalloff));

                        String floorColor = String.format("\u001B[38;2;%d;%d;%dm", fr, fg, fb);
                        outputBuffer[index] = floorColor + CH_WATER + RESET;
                    }
                }
            }
        }

        // STEP 2: Sort pyramid faces from BACK to FRONT
        double[][] vertices = {
                { 0.0, -1.5, 0.0 }, // 0: Apex
                { -1.4, 0.2, -1.4 }, // 1: Front-Left
                { 1.4, 0.2, -1.4 }, // 2: Front-Right
                { 1.4, 0.2, 1.4 }, // 3: Back-Right
                { -1.4, 0.2, 1.4 } // 4: Back-Left
        };

        int[][] faces = {
                { 0, 1, 2 }, { 0, 2, 3 }, { 0, 3, 4 }, { 0, 4, 1 }
        };

        SortedFace[] sortedFaces = new SortedFace[4];
        for (int i = 0; i < faces.length; i++) {
            int[] faceVertices = faces[i];
            double zSum = 0.0;
            for (int vIdx : faceVertices) {
                double xLocal = vertices[vIdx][0];
                double yLocal = vertices[vIdx][1];
                double zLocal = vertices[vIdx][2];

                double z_spun = -xLocal * sinY + zLocal * cosY;
                double rz = yLocal * sinX + z_spun * cosX;
                zSum += rz;
            }
            sortedFaces[i] = new SortedFace(i, zSum / 3.0);
        }

        // Depth sort back-to-front so transparent mesh segments compile correctly over each other
        // Bubble Sort is perfectly fine here since we have < 10 faces...
        // Check out my other project for better sorting algorithm analysis
        for (int i = 0; i < sortedFaces.length - 1; i++) {
            for (int j = 0; j < sortedFaces.length - i - 1; j++) {
                // Compare adjacent elements (b.avgZ vs a.avgZ for descending order)
                if (sortedFaces[j].avgZ < sortedFaces[j + 1].avgZ) {
                    // Swap the face elements
                    var temp = sortedFaces[j];
                    sortedFaces[j] = sortedFaces[j + 1];
                    sortedFaces[j + 1] = temp;
                }
            }
        }
        

        // STEP 3: Render Sorted Crystalline Matrix Faces
        for (int f = 0; f < sortedFaces.length; f++) {
            int activeFaceIndex = sortedFaces[f].faceIndex;
            int[] faceVertices = faces[activeFaceIndex];
            double[] v0 = vertices[faceVertices[0]];
            double[] v1 = vertices[faceVertices[1]];
            double[] v2 = vertices[faceVertices[2]];

            for (double u = 0; u <= 1.0 + 0.015; u += 0.006) {
                for (double v = 0; v <= 1.0 - u + 0.015; v += 0.006) {

                    double clampedU = Math.max(0.0, Math.min(1.0, u));
                    double clampedV = Math.max(0.0, Math.min(1.0 - clampedU, v));
                    double w = 1.0 - clampedU - clampedV;

                    double scale = 4.5;
                    double fracU = (clampedU * scale) % 1.0;
                    double fracV = (clampedV * scale) % 1.0;

                    boolean isStrut1 = Math.abs(fracU - fracV) < 0.10;
                    boolean isStrut2 = Math.abs(fracU + fracV - 1.0) < 0.10;
                    boolean isNodeIntersection = isStrut1 && isStrut2;
                    boolean isHorizontalBeam = Math.abs((clampedV * scale) % 1.0) < 0.08;

                    char renderChar = CH_GLASS;

                    // --- DYNAMIC CONTRAST TUNING LAYER ---
                    double brightnessFactor = 0.45; // Ambient pane blend multiplier during the day

                    if (isNodeIntersection) {
                        renderChar = CH_NODE;
                        brightnessFactor = 1.0;
                    } else if (isStrut1 || isStrut2 || isHorizontalBeam) {
                        renderChar = CH_STRUT;
                        brightnessFactor = 0.85;
                    } else {
                        // FIX: When rendering the glass panes at night, bypass the dim 0.45 modifier.
                        // This allows day glass to stay soft and night glass to flare up at 100% full
                        // intensity.
                        brightnessFactor = 0.45 * dayFactor + 1.0 * nightFactor;
                    }

                    int r, g, b;
                    if (renderChar == CH_NODE || renderChar == CH_STRUT) {
                        r = (int) (((DAY_STRUT[0] * dayFactor) + (NGT_STRUT[0] * nightFactor)) * brightnessFactor);
                        g = (int) (((DAY_STRUT[1] * dayFactor) + (NGT_STRUT[1] * nightFactor)) * brightnessFactor);
                        b = (int) (((DAY_STRUT[2] * dayFactor) + (NGT_STRUT[2] * nightFactor)) * brightnessFactor);
                    } else {
                        r = (int) (((DAY_GLASS[0] * dayFactor) + (NGT_GLASS[0] * nightGlowWeight)) * brightnessFactor);
                        g = (int) (((DAY_GLASS[1] * dayFactor) + (NGT_GLASS[1] * nightGlowWeight)) * brightnessFactor);
                        b = (int) (((DAY_GLASS[2] * dayFactor) + (NGT_GLASS[2] * nightGlowWeight)) * brightnessFactor);
                    }

                    // Resolve coordinates
                    double x = clampedU * v0[0] + clampedV * v1[0] + w * v2[0];
                    double y = clampedU * v0[1] + clampedV * v1[1] + w * v2[1];
                    double z = clampedU * v0[2] + clampedV * v1[2] + w * v2[2];

                    double x_spun = x * cosY + z * sinY;
                    double y_spun = y;
                    double z_spun = -x * sinY + z * cosY;

                    double rx = x_spun;
                    double ry = y_spun * cosX - z_spun * sinX;
                    double rz = y_spun * sinX + z_spun * cosX;

                    double ooz = 1.0 / (rz + CAMERA_DISTANCE);
                    int xp = (int) (40 + 52 * ooz * rx);
                    int yp = (int) (11 + 26 * ooz * ry);

                    for (int ox = 0; ox <= (u > 1.0 || v > 1.0 - u ? 1 : 0); ox++) {
                        int finalXp = xp + ox;

                        if (finalXp >= 0 && finalXp < 80 && yp >= 0 && yp < 22) {
                            int index = finalXp + 80 * yp;

                            r = Math.max(0, Math.min(255, r));
                            g = Math.max(0, Math.min(255, g));
                            b = Math.max(0, Math.min(255, b));

                            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                            outputBuffer[index] = colorCode + renderChar + RESET;
                        }
                    }
                }
            }
        }
    }
}