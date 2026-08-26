import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PinballLoader extends InteractiveLoader {
    private static final StatusStage[] PINBALL_STAGES = {
            new StatusStage(100, "ARCADE BOOT SEQUENCE INITIATED... [Arrow Keys Control Flippers!]")
    };

    private static final int WIDTH = 110;
    private static final int HEIGHT = 30;

    // Ball Physics State (Untouched Math)
    private volatile double ballX = 55.0;
    private volatile double ballY = 6.0;
    private volatile double ballVX = 0.4;
    private volatile double ballVY = 0.0;
    private static final double GRAVITY = 0.002;
    private static final double MAX_VELOCITY = 1.4;

    // Flipper States
    private volatile boolean leftFlipperActive = false;
    private volatile boolean rightFlipperActive = false;
    private int leftFlipperTimer = 0;
    private int rightFlipperTimer = 0;

    // Game Matrix Flow
    private int score = 0;
    private int crashFreezeTimer = 0;
    private int frameTick = 0; // Universal clock for ambient pulsing/animations

    // Motion Trail Buffers
    private final double[] trailX = new double[6];
    private final double[] trailY = new double[6];

    // Playfield Bumpers
    private static class Bumper {
        int x, y;
        int scoreValue;
        int hitFlashTicks = 0;

        Bumper(int x, int y, int val) {
            this.x = x;
            this.y = y;
            this.scoreValue = val;
        }
    }

    private final List<Bumper> bumpers = new ArrayList<>();

    // Enhanced Synthwave / Neon Arcade Palette
    private static final String C_CAB_OUTER  = "\u001B[38;2;30;25;45m";   // Deep Cabinet Purple
    private static final String C_CAB_INNER  = "\u001B[38;2;0;200;255m";  // Neon Cyan Rails
    private static final String C_GRID_IDLE  = "\u001B[38;2;25;20;40m";   // Subtle Dark Playfield Grid
    private static final String C_GRID_GLOW  = "\u001B[38;2;55;35;75m";   // Pulsing Playfield Grid
    
    private static final String C_BUMP_CORE  = "\u001B[38;2;255;0;100m";  // Hot Pink Bumper Base
    private static final String C_BUMP_PULSE = "\u001B[38;2;255;80;180m"; // Soft Pink Idle Pulse
    private static final String C_BUMP_HIT   = "\u001B[97m";              // Blinding White Flash
    
    private static final String C_FLIP_JOINT = "\u001B[38;2;255;200;0m";  // Mechanical Orange Pivot
    private static final String C_FLIP_PAD   = "\u001B[38;2;0;255;150m";  // Bright Neon Green Paddles
    
    private static final String C_BALL       = "\u001B[38;2;240;240;255m"; // Silver Chrome
    private static final String C_TRAIL_1    = "\u001B[38;2;120;140;200m"; // Cool Blue Wake
    private static final String C_TRAIL_2    = "\u001B[38;2;60;80;140m";   // Fading Blue Wake
    private static final String C_TRAIL_3    = "\u001B[38;2;30;40;80m";    // Dark Blue Wake
    
    private static final String C_DMD_BG     = "\u001B[38;2;15;5;0m";      // Dark Marquee Glass
    private static final String C_DMD_TXT    = "\u001B[38;2;255;80;0m";    // Dot-Matrix Orange Text

    private final Random random = new Random();

    public PinballLoader() {
        super(PINBALL_STAGES, WIDTH, HEIGHT);
        // Playfield Layout (Kept Original Coordinates)
        bumpers.add(new Bumper(45, 8, 100));
        bumpers.add(new Bumper(65, 8, 100));
        bumpers.add(new Bumper(55, 13, 250));
        bumpers.add(new Bumper(40, 16, 50));
        bumpers.add(new Bumper(70, 16, 50));
    }

    @Override
    protected void onInitialize() {
        resetBall();
        this.score = 0;
        this.crashFreezeTimer = 0;
        this.frameTick = 0;
    }

    private void resetBall() {
        this.ballX = 40.0 + random.nextDouble() * 30.0;
        this.ballY = 4.0;
        this.ballVX = (random.nextBoolean() ? 0.3 : -0.3);
        this.ballVY = 0.1;
        
        // Flush trail off-screen
        Arrays.fill(trailX, -100.0);
        Arrays.fill(trailY, -100.0);
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        if (crashFreezeTimer > 0)
            return;

        switch (keyCode) {
            case 'D': // Arrow LEFT
                leftFlipperActive = true;
                leftFlipperTimer = 4;
                break;
            case 'C': // Arrow RIGHT
                rightFlipperActive = true;
                rightFlipperTimer = 4;
                break;
            case 'A':
            case 'B': // Arrow UP/DOWN (Both)
                leftFlipperActive = true;
                leftFlipperTimer = 4;
                rightFlipperActive = true;
                rightFlipperTimer = 4;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(zBuffer, 0.0);
        Arrays.fill(outputBuffer, " ");
        frameTick++;

        // Flipper Cooldowns
        if (leftFlipperTimer > 0) if (--leftFlipperTimer == 0) leftFlipperActive = false;
        if (rightFlipperTimer > 0) if (--rightFlipperTimer == 0) rightFlipperActive = false;

        // -------------------------------------------------------------
        // 1. BALL PHYSICS & MOTION RECORDING
        // -------------------------------------------------------------
        if (crashFreezeTimer > 0) {
            if (--crashFreezeTimer == 0) resetBall();
        } else {
            // Record Motion Trail before moving
            for (int i = trailX.length - 1; i > 0; i--) {
                trailX[i] = trailX[i - 1];
                trailY[i] = trailY[i - 1];
            }
            trailX[0] = ballX;
            trailY[0] = ballY;

            // Apply constant gravity vector steps
            ballVY += GRAVITY;

            // Velocity bounding clamps
            ballVX = Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, ballVX));
            ballVY = Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, ballVY));

            // Integrate step translations
            ballX += ballVX;
            ballY += ballVY;

            // Resolve Round Bumper Collisions
            for (Bumper b : bumpers) {
                if (b.hitFlashTicks > 0) b.hitFlashTicks--;

                double dx = ballX - b.x;
                double dy = ballY - b.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < 2.2) {
                    score += b.scoreValue;
                    b.hitFlashTicks = 8; // slightly longer hit flash

                    if (distance == 0) {
                        dx = 0.1; dy = 0.1; distance = 0.14;
                    }
                    ballVX = (dx / distance) * 0.95;
                    ballVY = (dy / distance) * 0.95;

                    ballX += ballVX * 0.5;
                    ballY += ballVY * 0.5;
                }
            }

            // Boundary Box Static Rail Collisions
            if (ballX <= 30.5) { ballX = 31.0; ballVX = -ballVX * 0.8; }
            if (ballX >= 79.5) { ballX = 79.0; ballVX = -ballVX * 0.8; }
            if (ballY <= 3.5)  { ballY = 4.0;  ballVY = -ballVY * 0.8; }

            // Slanted Inlane Rail Deflections
            if (ballX >= 30 && ballX <= 40 && ballY >= 21) {
                double expectedY = 21 + (ballX - 30) * 0.4;
                if (ballY >= expectedY) {
                    ballY = expectedY - 0.5;
                    ballVY = -ballVY * 0.4;
                    ballVX += 0.3;
                }
            }
            if (ballX >= 70 && ballX <= 80 && ballY >= 21) {
                double expectedY = 25 - (ballX - 70) * 0.4;
                if (ballY >= expectedY) {
                    ballY = expectedY - 0.5;
                    ballVY = -ballVY * 0.4;
                    ballVX -= 0.3;
                }
            }

            // EXPANDED FLIPPER PADDLES MECHANICS (Width 11)
            if (Math.round(ballY) == 25 && ballX >= 38 && ballX <= 49) {
                if (leftFlipperActive) { ballVY = -1.0; ballVX += 0.5; } 
                else { ballVY = -ballVY * 0.3; }
            }
            if (Math.round(ballY) == 25 && ballX >= 61 && ballX <= 72) {
                if (rightFlipperActive) { ballVY = -1.0; ballVX -= 0.5; } 
                else { ballVY = -ballVY * 0.3; }
            }

            // Check Bottom Void Drain Drop Event
            if (ballY >= HEIGHT - 1) crashFreezeTimer = 40;
        }

        // -------------------------------------------------------------
        // 2. RENDER PLAYFIELD GRID & AMBIENCE (Background Layer)
        // -------------------------------------------------------------
        boolean gridPulse = Math.sin(frameTick * 0.1) > 0.5;
        String gridColor = gridPulse ? C_GRID_GLOW : C_GRID_IDLE;
        
        for (int y = 4; y <= 27; y++) {
            for (int x = 31; x <= 79; x++) {
                int index = x + WIDTH * y;
                // Subtle dotted grid for retro aesthetic
                if ((x + y) % 4 == 0) outputBuffer[index] = gridColor + "." + RESET;
                if ((x - y) % 7 == 0) outputBuffer[index] = gridColor + "+" + RESET;
            }
        }

        // -------------------------------------------------------------
        // 3. RENDER CABINET WALLS & NEON RAILS
        // -------------------------------------------------------------
        for (int y = 2; y <= 27; y++) {
            for (int x = 28; x <= 82; x++) {
                int index = x + WIDTH * y;
                boolean isOuterFrame = (y == 2 || y == 27 || x == 28 || x == 82);
                boolean isRailWall = (y == 3 || x == 30 || x == 80);

                if (isOuterFrame) {
                    // Draw heavy cabinet casing
                    outputBuffer[index] = C_CAB_OUTER + "▓" + RESET;
                } else if (isRailWall && y <= 25) {
                    // Draw inner glowing guide rails
                    char railChar = (y == 3) ? '═' : '║';
                    if (y == 3 && x == 30) railChar = '╔';
                    if (y == 3 && x == 80) railChar = '╗';
                    outputBuffer[index] = C_CAB_INNER + railChar + RESET;
                }
            }
        }

        // Overlay Slant Lane Art Lines
        for (int i = 0; i <= 10; i++) {
            int lIdx = (30 + i) + WIDTH * (21 + (int) (i * 0.4));
            int rIdx = (80 - i) + WIDTH * (21 + (int) (i * 0.4));
            outputBuffer[lIdx] = C_CAB_INNER + "↘" + RESET;
            outputBuffer[rIdx] = C_CAB_INNER + "↙" + RESET;
        }

        // -------------------------------------------------------------
        // 4. RENDER REACTIVE BUMPERS
        // -------------------------------------------------------------
        for (Bumper b : bumpers) {
            int idx = b.x + WIDTH * b.y;
            if (b.hitFlashTicks > 0) {
                // Expanding hit shockwave visual
                outputBuffer[idx - 2] = C_BUMP_HIT + "<" + RESET;
                outputBuffer[idx - 1] = C_BUMP_HIT + "[" + RESET;
                outputBuffer[idx]     = C_BUMP_HIT + "X" + RESET;
                outputBuffer[idx + 1] = C_BUMP_HIT + "]" + RESET;
                outputBuffer[idx + 2] = C_BUMP_HIT + ">" + RESET;
            } else {
                // Idle pulsing visual
                boolean pulse = Math.sin(frameTick * 0.15 + b.x) > 0.3;
                String bColor = pulse ? C_BUMP_PULSE : C_BUMP_CORE;
                outputBuffer[idx - 1] = bColor + "(" + RESET;
                outputBuffer[idx]     = bColor + "O" + RESET;
                outputBuffer[idx + 1] = bColor + ")" + RESET;
            }
        }

        // -------------------------------------------------------------
        // 5. RENDER FLIPPER PADDLES & JOINTS (Width 11)
        // -------------------------------------------------------------
        int leftFX = 38;
        int rightFX = 61;
        int flipperY = 25;

        for (int w = 0; w < 11; w++) {
            // Left paddle
            int lY = flipperY - (leftFlipperActive ? w / 3 : 0);
            int leftIndex = (leftFX + w) + WIDTH * lY;
            
            if (w == 0) { // Render mechanical pivot joint
                outputBuffer[leftIndex] = C_FLIP_JOINT + "O" + RESET;
            } else {
                outputBuffer[leftIndex] = C_FLIP_PAD + "▄" + RESET;
            }

            // Right paddle
            int rY = flipperY - (rightFlipperActive ? (10 - w) / 3 : 0);
            int rightIndex = (rightFX + w) + WIDTH * rY;
            
            if (w == 10) { // Render mechanical pivot joint (anchored on right side)
                outputBuffer[rightIndex] = C_FLIP_JOINT + "O" + RESET;
            } else {
                outputBuffer[rightIndex] = C_FLIP_PAD + "▄" + RESET;
            }
        }

        // -------------------------------------------------------------
        // 6. RENDER KINETIC BALL & MOTION TRAIL
        // -------------------------------------------------------------
        // Draw trailing ghost particles (Oldest to Newest)
        String[] trailColors = {C_TRAIL_3, C_TRAIL_3, C_TRAIL_2, C_TRAIL_2, C_TRAIL_1, C_TRAIL_1};
        char[] trailChars = {'.', '.', '·', '·', 'o', 'o'};
        
        for (int i = trailX.length - 1; i >= 0; i--) {
            int tX = (int) Math.round(trailX[i]);
            int tY = (int) Math.round(trailY[i]);
            if (tX >= 31 && tX <= 79 && tY >= 4 && tY <= 27) { // Only draw trail inside playfield
                outputBuffer[tX + WIDTH * tY] = trailColors[i] + trailChars[i] + RESET;
            }
        }

        // Draw Chrome Ball Core (On Top)
        int bX = (int) Math.round(ballX);
        int bY = (int) Math.round(ballY);
        if (crashFreezeTimer == 0 && bX >= 0 && bX < WIDTH && bY >= 0 && bY < HEIGHT) {
            outputBuffer[bX + WIDTH * bY] = C_BALL + "●" + RESET;
        }

        // -------------------------------------------------------------
        // 7. DOT-MATRIX DISPLAY (DMD) HUD DASHBOARD
        // -------------------------------------------------------------
        String hudMsg = String.format(" SCORE : %07d ", score);
        if (crashFreezeTimer > 0) {
            boolean flash = (crashFreezeTimer % 10 < 5);
            hudMsg = flash ? "   ** BALL DRAINED! **   " : "   -- LAUNCHING --       ";
        }
        
        // Frame the Marquee display at the top center
        int hudWidth = hudMsg.length();
        int hudStartX = 55 - (hudWidth / 2);
        
        // Render DMD Background Block
        for (int y = 0; y <= 2; y++) {
            for (int x = hudStartX - 2; x <= hudStartX + hudWidth + 1; x++) {
                if (x >= 0 && x < WIDTH) outputBuffer[x + WIDTH * y] = C_DMD_BG + "█" + RESET;
            }
        }
        
        // Render Text over the background
        for (int i = 0; i < hudWidth; i++) {
            int idx = (hudStartX + i) + WIDTH * 1;
            if (idx >= 0 && idx < outputBuffer.length) {
                outputBuffer[idx] = C_DMD_TXT + hudMsg.charAt(i) + RESET;
            }
        }
    }
}