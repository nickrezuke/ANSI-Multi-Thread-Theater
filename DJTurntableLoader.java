public class DJTurntableLoader extends Loader {
    private static final StatusStage[] DJ_STAGES = {
            new StatusStage(25, "Routing audio channel circuit boards:"),
            new StatusStage(50, "Mounting magnetic vinyl slipmats:"),
            new StatusStage(75, "Calibrating VU peak-decay level meters:"),
            new StatusStage(100, "Club Console Mixer Operational!")
    };

    private double timeClock = 0.0;
    private double vinylRotation = 0.0;

    // Fast-falling peak-decay matrix registers
    private double leftChannelPeak = 0.0;
    private double rightChannelPeak = 0.0;

    // High-fidelity industrial nightclub hardware coloring
    private static final int[] RGB_DECK_STEEL = { 35, 38, 44 }; // Brushed faceplate chassis
    private static final int[] RGB_VINYL_GLOSS = { 15, 16, 20 }; // Obsidian vinyl grooves
    private static final int[] RGB_STROBE_DOT = { 240, 245, 255 }; // Bright aluminum platter edge
    private static final int[] RGB_KNOB_CHROME = { 160, 170, 185 }; // Equalizer dials
    private static final int[] RGB_FADER_CAP = { 220, 225, 235 }; // Aluminum slider line
    private static final int[] RGB_LCD_PANEL = { 10, 24, 16 }; // Dark glass screen display

    // Multi-stage signal amplitude bar colors
    private static final int[] RGB_VU_GREEN = { 40, 220, 60 }; // Safe dB headroom
    private static final int[] RGB_VU_ORANGE = { 255, 150, 20 }; // Warning threshold
    private static final int[] RGB_VU_RED = { 255, 30, 50 }; // Peak clipping line

    // Typographic layout controls
    private static final char CH_CHASSIS = '\u2588'; // █ Solid metallic framework
    private static final char CH_SCREEN = '\u2591'; // ░ Light screen backdrop raster
    private static final char CH_VINYL = '\u2593'; // ▓ Matte heavy vinyl groove texture
    private static final char CH_KNOB = '\u00A4'; // ¤ Circular parametric dial indicator
    private static final char CH_VU_BAR = '\u2584'; // ▄ Solid audio segment block

    public DJTurntableLoader() {
        // This uses 80x22 specifically
        super(DJ_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.055; // Drives the volume frequency waves and screen graphs
        vinylRotation += 0.095; // Smooth 33/45 RPM record platter speed conversion

        int width = 80;
        int height = 22;

        // --- 1. PROCEDURAL SOUND WAVE GENERATOR (Bumping Volume Physics) ---
        double bassKick = Math.pow(Math.max(0.0, Math.sin(timeClock * 1.8)), 3.0);
        double hiHatSync = 0.25 * Math.abs(Math.sin(timeClock * 4.2 + 0.5));

        // Finalized absolute instantaneous signal amplitude limits (0.0 to 1.0)
        double currentLeftVolume = Math.min(1.0, bassKick * 0.85 + hiHatSync + 0.05);
        double currentRightVolume = Math.min(1.0, bassKick * 0.70 + hiHatSync * 1.3 + 0.04);

        // Peak Hold Decay Tracker
        double decayRate = 0.025;
        leftChannelPeak = Math.max(currentLeftVolume, leftChannelPeak - decayRate);
        rightChannelPeak = Math.max(currentRightVolume, rightChannelPeak - decayRate);

        // Main Hardware Canvas Rasterizer Loop
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + width * y;

                // ==================== REGION A: THE LEFT VINYL TURNTABLE PLATTER
                // ====================
                double dx = (x - 24) * 0.52;
                double dy = y - 11;
                double radialDist = Math.sqrt(dx * dx + dy * dy);

                if (radialDist <= 10.0) {
                    if (0.80 > zBuffer[index]) {
                        zBuffer[index] = 0.80;
                        int r = RGB_VINYL_GLOSS[0];
                        int g = RGB_VINYL_GLOSS[1];
                        int b = RGB_VINYL_GLOSS[2];
                        char texture = CH_VINYL;

                        // Calculate and normalize coordinates into a strict positive [0, 2PI] window
                        double angle = Math.atan2(dy, dx);
                        if (angle < 0) {
                            angle += (2.0 * Math.PI);
                        }
                        double normalizedAngle = angle / (2.0 * Math.PI);

                        if (radialDist > 9.4) {
                            // Outer aluminum platter edge featuring rotating timing strobe marks
                            double strobeMark = Math.sin(normalizedAngle * 2.0 * Math.PI * 85.0 + vinylRotation);
                            if (strobeMark > 0.15) {
                                r = RGB_STROBE_DOT[0];
                                g = RGB_STROBE_DOT[1];
                                b = RGB_STROBE_DOT[2];
                                texture = CH_CHASSIS;
                            } else {
                                r = RGB_DECK_STEEL[0];
                                g = RGB_DECK_STEEL[1];
                                b = RGB_DECK_STEEL[2];
                            }
                        } else if (radialDist < 1.4) {
                            // Solid central metallic chrome spindle node axis pin
                            r = RGB_KNOB_CHROME[0];
                            g = RGB_KNOB_CHROME[1];
                            b = RGB_KNOB_CHROME[2];
                            texture = CH_CHASSIS;
                        } else {
                            // The rotating record body with tracking distance verification
                            double recordStickerAngle = (vinylRotation * 0.25) % (2.0 * Math.PI);
                            double angularDistance = Math.abs(angle - recordStickerAngle);
                            if (angularDistance > Math.PI) {
                                angularDistance = (2.0 * Math.PI) - angularDistance;
                            }

                            if (angularDistance < 0.05 && radialDist > 4.0) {
                                r = 245;
                                g = 250;
                                b = 255;
                                texture = CH_CHASSIS;
                            }
                        }

                        // Superimpose the static playback tone-arm needle line tracing over the record
                        if (Math.abs((x - 24) * 0.65 + (y - 11) * 0.75) < 0.35 && y < 11 && x > 24) {
                            r = 255;
                            g = 40;
                            b = 40;
                            texture = CH_CHASSIS;
                        }

                        String vinylColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                        outputBuffer[index] = vinylColor + texture + RESET;
                    }
                    continue;
                }

                // ==================== REGION B: THE CENTRAL SOUND DISPLAY WINDOW
                // ====================
                boolean insideLcdScreen = (x >= 50 && x <= 72) && (y >= 2 && y <= 9);
                if (insideLcdScreen) {
                    if (0.90 > zBuffer[index]) {
                        zBuffer[index] = 0.90;
                        int r = RGB_LCD_PANEL[0];
                        int g = RGB_LCD_PANEL[1];
                        int b = RGB_LCD_PANEL[2];
                        char screenChar = CH_SCREEN;

                        boolean isLeftChannelRow = (y >= 4 && y <= 5);
                        boolean isRightChannelRow = (y >= 7 && y <= 8);

                        if (isLeftChannelRow || isRightChannelRow) {
                            double currentChannelVal = isLeftChannelRow ? currentLeftVolume : currentRightVolume;
                            double currentPeakVal = isLeftChannelRow ? leftChannelPeak : rightChannelPeak;
                            double rowProgress = (x - 50.0) / 22.0;

                            if (rowProgress <= currentChannelVal) {
                                screenChar = CH_VU_BAR;
                                if (rowProgress < 0.60) {
                                    r = RGB_VU_GREEN[0];
                                    g = RGB_VU_GREEN[1];
                                    b = RGB_VU_GREEN[2];
                                } else if (rowProgress < 0.85) {
                                    r = RGB_VU_ORANGE[0];
                                    g = RGB_VU_ORANGE[1];
                                    b = RGB_VU_ORANGE[2];
                                } else {
                                    r = RGB_VU_RED[0];
                                    g = RGB_VU_RED[1];
                                    b = RGB_VU_RED[2];
                                }
                            } else if (Math.abs(rowProgress - currentPeakVal) < 0.025) {
                                screenChar = CH_CHASSIS;
                                r = RGB_VU_RED[0];
                                g = RGB_VU_RED[1];
                                b = RGB_VU_RED[2];
                            }
                        }
                        String panelColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                        outputBuffer[index] = panelColor + screenChar + RESET;
                    }
                    continue;
                }

                // ==================== REGION C: HARDWARE CONTROLS, KNOBS & FADERS
                // ====================
                if (0.10 > zBuffer[index]) {
                    zBuffer[index] = 0.10;
                    int r = RGB_DECK_STEEL[0];
                    int g = RGB_DECK_STEEL[1];
                    int b = RGB_DECK_STEEL[2];
                    char elementChar = ' ';

                    if (x == 48 || x == 75) {
                        r = RGB_VINYL_GLOSS[0];
                        b = RGB_VINYL_GLOSS[2];
                        g = RGB_VINYL_GLOSS[1];
                        elementChar = '\u2503';
                    } else if ((x == 54 || x == 68) && (y == 12 || y == 14 || y == 16)) {
                        b = RGB_KNOB_CHROME[2];
                        g = RGB_KNOB_CHROME[1];
                        r = RGB_KNOB_CHROME[0];
                        elementChar = CH_KNOB;
                    } else if ((x == 58 || x == 64) && (y >= 12 && y <= 19)) {
                        r = RGB_VINYL_GLOSS[0];
                        g = RGB_VINYL_GLOSS[1];
                        b = RGB_VINYL_GLOSS[2];
                        elementChar = '|';

                        double targetFaderY = 19.0 - (x == 58 ? currentLeftVolume : currentRightVolume) * 6.0;
                        if (Math.abs(y - targetFaderY) < 0.5) {
                            r = RGB_FADER_CAP[0];
                            g = RGB_FADER_CAP[1];
                            b = RGB_FADER_CAP[2];
                            elementChar = CH_CHASSIS;
                        }
                    }
                    String faceplateColor = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                    outputBuffer[index] = faceplateColor + elementChar + RESET;
                }
            }
        }
    }
}