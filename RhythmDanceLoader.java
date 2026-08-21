import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RhythmDanceLoader extends InteractiveLoader {
    private static final StatusStage[] RHYTHM_STAGES = {
            new StatusStage(100, "[Use ARROW KEYS to match the rhythm beat!]")
    };

    private static final int WIDTH = 110;
    private static final int HEIGHT = 30;

    // Symmetrical Highway Column Alignment (Outer Walls X=41, X=65)
    // Lane 0: 44 | Lane 1: 50 | Lane 2: 56 | Lane 3: 62
    private static final int[] LANE_X_START = { 44, 50, 56, 62 };
    private static final String[] LANE_SYMBOLS = { "◀◀", "▼▼", "▲▲", "▶▶" };
    private static final String[] SINGLE_SYMBOLS = { "◀", "▼", "▲", "▶" };
    private static final int TARGET_ZONE_Y = 25;

    // Hit Timing Configuration (Tuning Windows in Terminal Rows)
    private static final double WINDOW_PERFECT = 0.5; // Precision hit (center row)
    private static final double WINDOW_GREAT   = 1.0; // Tight window
    private static final double WINDOW_GOOD    = 1.8; // Moderate window
    private static final double MAX_HIT_WINDOW = 2.5; // Absolute cutoff; >2.5 triggers a Miss

    // Rhythm Timing Mechanics
    private static final int BEAT_PERIOD = 25;
    private static final double DROP_SPEED = 0.20; 

    // Core Gameplay State
    private int score = 0;
    private int combo = 0;
    private int maxCombo = 0;
    private int currentMultiplier = 1;
    private int grooveMeter = 80;

    // Accuracy Stats
    private int countPerfect = 0;
    private int countGreat = 0;
    private int countGood = 0;
    private int countMiss = 0;

    // Timing Offset Display
    private double lastHitOffset = 0.0;
    private boolean hasHitRecently = false;

    // Feedback Banner State
    private String feedbackText = "";
    private String feedbackColor = "\u001B[0m";
    private int feedbackTimer = 0;

    // Global Animation Tick
    private int globalTick = 0;

    // Thread-Safe Falling Note List
    private static class Note {
        int lane;
        double posY;
        boolean processed = false;
    }

    private final List<Note> activeNotes = Collections.synchronizedList(new ArrayList<>());
    private int noteSpawnTimer = 0;

    // ANSI Color Palette
    private static final String COLOR_BG = "\u001B[38;2;12;8;22m";
    private static final String COLOR_BORDER = "\u001B[38;2;80;70;120m";
    private static final String COLOR_TARGET = "\u001B[38;2;0;255;240m";
    private static final String COLOR_NOTE = "\u001B[38;2;255;0;150m";
    private static final String COLOR_GOLD = "\u001B[38;2;255;215;0m";
    private static final String COLOR_GREEN = "\u001B[38;2;0;255;120m";
    private static final String COLOR_CYAN = "\u001B[38;2;0;220;255m";
    private static final String COLOR_RED = "\u001B[38;2;255;60;80m";
    private static final String COLOR_DIM = "\u001B[38;2;90;90;120m";
    private static final String COLOR_WHITE = "\u001B[38;2;240;240;255m";

    private final Random random = new Random();

    public RhythmDanceLoader() {
        super(RHYTHM_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void onInitialize() {
        this.score = 0;
        this.combo = 0;
        this.maxCombo = 0;
        this.currentMultiplier = 1;
        this.grooveMeter = 80;
        this.countPerfect = 0;
        this.countGreat = 0;
        this.countGood = 0;
        this.countMiss = 0;
        this.lastHitOffset = 0.0;
        this.hasHitRecently = false;
        this.feedbackText = "GET READY!";
        this.feedbackColor = COLOR_CYAN;
        this.feedbackTimer = 35;
        this.activeNotes.clear();
        this.noteSpawnTimer = 10;
        this.globalTick = 0;
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        int targetLane = -1;

        // ANSI Arrow Keys ('D'=Left, 'B'=Down, 'A'=Up, 'C'=Right), WASD, & Swing Codes
        if (keyCode == 'D' || keyCode == 'd' || keyCode == 'a' || keyCode == 37) {
            targetLane = 0; // Left Arrow
        } else if (keyCode == 'B' || keyCode == 'b' || keyCode == 's' || keyCode == 40) {
            targetLane = 1; // Down Arrow
        } else if (keyCode == 'A' || keyCode == 'a' || keyCode == 'w' || keyCode == 38) {
            targetLane = 2; // Up Arrow
        } else if (keyCode == 'C' || keyCode == 'c' || keyCode == 'd' || keyCode == 39) {
            targetLane = 3; // Right Arrow
        }

        if (targetLane == -1) return;

        Note closestNote = null;
        double minimumDistance = 999.0;
        double signedOffset = 0.0;

        synchronized (activeNotes) {
            for (Note n : activeNotes) {
                if (!n.processed && n.lane == targetLane) {
                    double diff = n.posY - TARGET_ZONE_Y;
                    double dist = Math.abs(diff);
                    if (dist < minimumDistance) {
                        minimumDistance = dist;
                        signedOffset = diff;
                        closestNote = n;
                    }
                }
            }
        }

        // Strict Hit Window Validation
        if (closestNote != null && minimumDistance <= MAX_HIT_WINDOW) {
            closestNote.processed = true;
            this.lastHitOffset = signedOffset;
            this.hasHitRecently = true;
            evaluateHitTiming(minimumDistance);
        } else {
            triggerMiss(true); // Early tap or empty lane press
        }
    }

    private void evaluateHitTiming(double distance) {
        combo++;
        if (combo > maxCombo) maxCombo = combo;

        if (combo >= 40) currentMultiplier = 4;
        else if (combo >= 25) currentMultiplier = 3;
        else if (combo >= 10) currentMultiplier = 2;
        else currentMultiplier = 1;

        grooveMeter = Math.min(100, grooveMeter + 5);

        if (distance <= WINDOW_PERFECT) {
            feedbackText = "PERFECT!!";
            feedbackColor = COLOR_GOLD + "\u001B[1m";
            score += 1000 * currentMultiplier;
            countPerfect++;
        } else if (distance <= WINDOW_GREAT) {
            feedbackText = "GREAT!";
            feedbackColor = COLOR_GREEN;
            score += 500 * currentMultiplier;
            countGreat++;
        } else if (distance <= WINDOW_GOOD) {
            feedbackText = "GOOD";
            feedbackColor = COLOR_CYAN;
            score += 200 * currentMultiplier;
            countGood++;
        } else {
            feedbackText = "OKAY";
            feedbackColor = COLOR_DIM;
            score += 50 * currentMultiplier;
        }
        feedbackTimer = 22;
    }

    private void triggerMiss(boolean isActiveTap) {
        this.combo = 0;
        this.currentMultiplier = 1;
        this.countMiss++;
        this.grooveMeter = Math.max(0, grooveMeter - 10);

        if (isActiveTap) {
            this.feedbackText = "EARLY TAP";
            this.feedbackColor = COLOR_RED;
            this.feedbackTimer = 16;
        } else if (feedbackTimer <= 5) {
            this.feedbackText = "MISS...";
            this.feedbackColor = COLOR_RED;
            this.feedbackTimer = 16;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        globalTick++;

        Arrays.fill(zBuffer, 0.0);
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = COLOR_BG + " " + RESET;
        }

        // 1. GAME PHYSICS & MISS DETECTION UPDATES
        synchronized (activeNotes) {
            for (int i = activeNotes.size() - 1; i >= 0; i--) {
                Note n = activeNotes.get(i);
                n.posY += DROP_SPEED;

                // Cutoff threshold cleanly triggers miss before array cleanup
                if (!n.processed && n.posY > TARGET_ZONE_Y + 1.5) {
                    n.processed = true;
                    triggerMiss(false);
                }

                if (n.posY > HEIGHT - 1) {
                    activeNotes.remove(i);
                }
            }
        }

        // Synchronized Note Spawner on Beat Boundaries
        noteSpawnTimer--;
        if (noteSpawnTimer <= 0) {
            Note newNote = new Note();
            newNote.lane = random.nextInt(4);
            newNote.posY = 0;
            synchronized (activeNotes) {
                activeNotes.add(newNote);
            }

            // Spawn every 2 or 3 beats (40 or 60 ticks)
            int beatsToWait = random.nextBoolean() ? 2 : 3;
            noteSpawnTimer = beatsToWait * BEAT_PERIOD;
        }

        if (feedbackTimer > 0) feedbackTimer--;

        // 2. RENDER HIGHWAY & HUD PANELS
        renderHighway(outputBuffer);

        synchronized (activeNotes) {
            for (Note n : activeNotes) {
                if (n.processed) continue;

                int drawX = LANE_X_START[n.lane];
                int drawY = (int) Math.round(n.posY);

                if (drawY >= 1 && drawY < HEIGHT - 1) {
                    renderText(outputBuffer, LANE_SYMBOLS[n.lane], drawX, drawY, COLOR_NOTE + "\u001B[1m");
                }
            }
        }

        renderLeftHUD(outputBuffer);
        renderRightHUD(outputBuffer);
    }

    private void renderHighway(String[] buffer) {
        int beatLineOffset = (globalTick / 2) % 4;

        for (int y = 0; y < HEIGHT; y++) {
            renderText(buffer, "║", 41, y, COLOR_BORDER);
            renderText(buffer, "║", 65, y, COLOR_BORDER);

            for (int l = 0; l < 4; l++) {
                int x = LANE_X_START[l];
                if (l < 3) {
                    renderText(buffer, "│", x + 4, y, COLOR_BORDER);
                }
                if ((y + beatLineOffset) % 4 == 0 && y != TARGET_ZONE_Y) {
                    renderText(buffer, "──", x, y, COLOR_DIM);
                }
            }
        }

        // Target Zone
        renderText(buffer, "╠═══════════════════════╣", 41, TARGET_ZONE_Y, COLOR_TARGET);
        for (int l = 0; l < 4; l++) {
            int x = LANE_X_START[l];
            renderText(buffer, "[" + SINGLE_SYMBOLS[l] + "]", x - 1, TARGET_ZONE_Y, COLOR_TARGET + "\u001B[1m");
        }

        if (combo > 1) {
            String comboStr = combo + " STREAK!";
            renderText(buffer, comboStr, (WIDTH - comboStr.length()) / 2, TARGET_ZONE_Y - 4, COLOR_GOLD + "\u001B[1m");
        }

        if (feedbackTimer > 0) {
            renderText(buffer, feedbackText, (WIDTH - feedbackText.length()) / 2, TARGET_ZONE_Y - 2, feedbackColor);
        }
    }

    private void renderLeftHUD(String[] buffer) {
        renderText(buffer, "★ RHYTHM HERO ★", 8, 2, COLOR_GOLD + "\u001B[1m");
        renderText(buffer, "─────────────────────", 5, 3, COLOR_BORDER);

        renderText(buffer, "SCORE", 5, 5, COLOR_DIM);
        renderText(buffer, String.format("%07d", score), 5, 6, COLOR_WHITE + "\u001B[1m");

        renderText(buffer, "MAX STREAK", 22, 5, COLOR_DIM);
        renderText(buffer, String.format("%03d", maxCombo), 22, 6, COLOR_GOLD);

        String multBadge = " [ MULTIPLIER x" + currentMultiplier + " ] ";
        renderText(buffer, multBadge, 5, 8, currentMultiplier > 1 ? COLOR_GOLD + "\u001B[1m" : COLOR_DIM);

        renderText(buffer, "GROOVE METER", 5, 11, COLOR_DIM);
        int filledBars = (grooveMeter * 20) / 100;
        StringBuilder meter = new StringBuilder("[");
        for (int i = 0; i < 20; i++) {
            meter.append(i < filledBars ? "█" : "░");
        }
        meter.append("]");
        String meterColor = grooveMeter > 50 ? COLOR_GREEN : (grooveMeter > 20 ? COLOR_GOLD : COLOR_RED);
        renderText(buffer, meter.toString(), 5, 12, meterColor);

        renderText(buffer, "ACCURACY STATS", 5, 15, COLOR_WHITE);
        renderText(buffer, "─────────────────────", 5, 16, COLOR_BORDER);

        renderText(buffer, "PERFECT : " + countPerfect, 5, 17, COLOR_GOLD);
        renderText(buffer, "GREAT   : " + countGreat, 5, 18, COLOR_GREEN);
        renderText(buffer, "GOOD    : " + countGood, 5, 19, COLOR_CYAN);
        renderText(buffer, "MISS    : " + countMiss, 5, 20, COLOR_RED);

        int totalHits = countPerfect + countGreat + countGood + countMiss;
        double accuracy = totalHits == 0 ? 100.0 : ((double) (countPerfect + countGreat + countGood) / totalHits) * 100.0;
        renderText(buffer, String.format("ACCURACY: %.1f%%", accuracy), 5, 22, COLOR_WHITE + "\u001B[1m");
    }

    private void renderRightHUD(String[] buffer) {
        // VISUAL BEAT SYNC MONITOR (Rhythm Sound Sync Test Indicator)
        renderText(buffer, "┌── BEAT SYNC MONITOR ──┐", 72, 2, COLOR_BORDER);
        renderText(buffer, "│  EARLY   [BEAT]   LATE │", 72, 3, COLOR_DIM);

        // Sweep marker position relative to beat (-5 to +5)
        int ticksInBeat = globalTick % BEAT_PERIOD;
        int beatOffset = ticksInBeat <= (BEAT_PERIOD / 2) ? ticksInBeat : (ticksInBeat - BEAT_PERIOD);
        boolean isOnBeat = Math.abs(beatOffset) <= 1;

        // Build 15-character sync track
        char[] syncTrack = "───░░░  🎯  ░░░───".toCharArray();
        int cursorIdx = 8 + beatOffset;
        if (cursorIdx >= 0 && cursorIdx < syncTrack.length) {
            syncTrack[cursorIdx] = '█';
        }

        renderText(buffer, "│ [" + new String(syncTrack) + "] │", 72, 4, isOnBeat ? COLOR_GOLD + "\u001B[1m" : COLOR_WHITE);

        if (isOnBeat) {
            renderText(buffer, "│    >>> HIT NOW! <<<    │", 72, 5, COLOR_GREEN + "\u001B[1m");
        } else {
            renderText(buffer, "│      APPROACHING...    │", 72, 5, COLOR_DIM);
        }
        renderText(buffer, "└────────────────────────┘", 72, 6, COLOR_BORDER);

        // TIMING RADAR
        renderText(buffer, "TIMING RADAR", 72, 9, COLOR_WHITE);
        renderText(buffer, "────────────────────", 72, 10, COLOR_BORDER);

        if (hasHitRecently) {
            int centerPos = 81;
            int offsetShift = (int) Math.round(lastHitOffset * 2.0);
            int markerX = Math.max(73, Math.min(90, centerPos + offsetShift));

            renderText(buffer, "EARLY [    ┼    ] LATE", 72, 11, COLOR_DIM);
            renderText(buffer, "▲", markerX, 12, lastHitOffset < 0 ? COLOR_CYAN : COLOR_GOLD);

            String offsetLabel = String.format("OFFSET: %+.1f ROWS", lastHitOffset);
            renderText(buffer, offsetLabel, 72, 13, COLOR_WHITE);
        } else {
            renderText(buffer, "EARLY [    ┼    ] LATE", 72, 11, COLOR_DIM);
            renderText(buffer, "WAITING FOR TAP...", 72, 13, COLOR_DIM);
        }

        // BEAT-PULSED AUDIO EQUALIZER
        renderText(buffer, "BEAT EQUALIZER", 72, 16, COLOR_WHITE);
        renderText(buffer, "────────────────────", 72, 17, COLOR_BORDER);

        String[] eqBars = { " ", "▂", "▄", "▅", "▆", "▇", "█" };
        StringBuilder eqLine = new StringBuilder();

        // Decay amplitude between beats, spike on beat
        double beatDecay = 1.0 - ((double) ticksInBeat / BEAT_PERIOD);

        for (int i = 0; i < 18; i++) {
            double wave = (Math.sin((globalTick + i * 3) * 0.4) + 1.0) / 2.0;
            int height = (int) Math.round(wave * 6.0 * beatDecay);
            height = Math.max(0, Math.min(6, height));
            eqLine.append(eqBars[height]);
        }
        renderText(buffer, eqLine.toString(), 72, 18, isOnBeat ? COLOR_GOLD : COLOR_CYAN);

        // CONTROLS LEGEND
        renderText(buffer, "CONTROLS", 72, 21, COLOR_WHITE);
        renderText(buffer, "────────────────────", 72, 22, COLOR_BORDER);
        renderText(buffer, "[←]  [↓]  [↑]  [→]", 72, 23, COLOR_GOLD + "\u001B[1m");
        renderText(buffer, "MATCH ARROWS TO TARGET", 72, 24, COLOR_DIM);
    }

    private void renderText(String[] buffer, String text, int startX, int startY, String colorPrefix) {
        for (int i = 0; i < text.length(); i++) {
            int idx = (startX + i) + WIDTH * startY;
            if (idx >= 0 && idx < buffer.length) {
                buffer[idx] = colorPrefix + text.charAt(i) + RESET;
            }
        }
    }
}