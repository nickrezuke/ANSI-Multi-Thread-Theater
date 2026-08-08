//TODO: This is very barebones... Improve this...

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PinballLoader extends InteractiveLoader {
    private static final StatusStage[] PINBALL_STAGES = {
            new StatusStage(100, "[Arrow Keys control Flippers!!]")
    };

    private static final int WIDTH = 110;
    private static final int HEIGHT = 30;

    // Ball Physics State
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

    // Aesthetic Palette Strings
    private static final String COLOR_CHASSIS = "\u001B[38;2;60;65;80m"; // Steel Blue Machine
    private static final String COLOR_BUMPER = "\u001B[38;2;255;45;140m"; // Neon Pink Bumpers
    private static final String COLOR_BUMPER_HIT = "\u001B[97m"; // Flash Bright White
    private static final String COLOR_FLIPPER = "\u001B[38;2;0;240;180m"; // Cyber Cyan Paddles
    private static final String COLOR_BALL = "\u001B[38;2;240;240;255m"; // Silver Chrome Ball
    private static final String COLOR_TXT = "\u001B[38;2;255;210;0m"; // Matrix Gold Text

    private final Random random = new Random();

    public PinballLoader() {
        super(PINBALL_STAGES, WIDTH, HEIGHT);
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
    }

    private void resetBall() {
        this.ballX = 40.0 + random.nextDouble() * 30.0;
        this.ballY = 4.0;
        this.ballVX = (random.nextBoolean() ? 0.3 : -0.3);
        this.ballVY = 0.1;
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        if (crashFreezeTimer > 0)
            return;

        switch (keyCode) {
            case 'D': // Arrow LEFT -> Actuate Left Paddle
                leftFlipperActive = true;
                leftFlipperTimer = 4; // Keep raised for 4 frames
                break;
            case 'C': // Arrow RIGHT -> Actuate Right Paddle
                rightFlipperActive = true;
                rightFlipperTimer = 4;
                break;
            case 'A':
            case 'B': // Arrow UP&DOWN -> Actuate Both
                leftFlipperActive = true;
                leftFlipperTimer = 4; // Keep raised for 4 frames
                rightFlipperActive = true;
                rightFlipperTimer = 4;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear background array setup
        Arrays.fill(zBuffer, 0.0);
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = " ";
        }

        // Tick down flipper mechanics
        if (leftFlipperTimer > 0) {
            leftFlipperTimer--;
            if (leftFlipperTimer == 0)
                leftFlipperActive = false;
        }
        if (rightFlipperTimer > 0) {
            rightFlipperTimer--;
            if (rightFlipperTimer == 0)
                rightFlipperActive = false;
        }

        // 1. BALL PHYSICS RESOLUTIONS
        if (crashFreezeTimer > 0) {
            crashFreezeTimer--;
            if (crashFreezeTimer == 0) {
                resetBall(); // Endless continuous looping reset block
            }
        } else {
            // Apply constant gravity vector steps
            ballVY += GRAVITY;

            // Velocity bounding clamps
            if (ballVX > MAX_VELOCITY)
                ballVX = MAX_VELOCITY;
            if (ballVX < -MAX_VELOCITY)
                ballVX = -MAX_VELOCITY;
            if (ballVY > MAX_VELOCITY)
                ballVY = MAX_VELOCITY;
            if (ballVY < -MAX_VELOCITY)
                ballVY = -MAX_VELOCITY;

            // Integrate step translations
            ballX += ballVX;
            ballY += ballVY;

            // Resolve Round Bumper Collisions
            for (Bumper b : bumpers) {
                if (b.hitFlashTicks > 0)
                    b.hitFlashTicks--;

                double dx = ballX - b.x;
                double dy = ballY - b.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < 2.2) {
                    score += b.scoreValue;
                    b.hitFlashTicks = 6;

                    if (distance == 0) {
                        dx = 0.1;
                        dy = 0.1;
                        distance = 0.14;
                    }
                    ballVX = (dx / distance) * 0.95;
                    ballVY = (dy / distance) * 0.95;

                    ballX += ballVX * 0.5;
                    ballY += ballVY * 0.5;
                }
            }

            // Boundary Box Static Rail Collisions
            if (ballX <= 30.5) {
                ballX = 31.0;
                ballVX = -ballVX * 0.8;
            }
            if (ballX >= 79.5) {
                ballX = 79.0;
                ballVX = -ballVX * 0.8;
            }
            if (ballY <= 3.5) {
                ballY = 4.0;
                ballVY = -ballVY * 0.8;
            }

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

            // EXPANDED FLIPPER PADDLES MECHANICS & BALL OVERLAP CHECKS
            // Left Flipper Footprint closed in: Row 25, Columns 38 to 49 (Width 11)
            if (Math.round(ballY) == 25 && ballX >= 38 && ballX <= 49) {
                if (leftFlipperActive) {
                    ballVY = -1.0; // Upward physics vector launch impulse
                    ballVX += 0.5;
                } else {
                    ballVY = -ballVY * 0.3;
                }
            }

            // Right Flipper Footprint closed in: Row 25, Columns 61 to 72 (Width 11)
            if (Math.round(ballY) == 25 && ballX >= 61 && ballX <= 72) {
                if (rightFlipperActive) {
                    ballVY = -1.0;
                    ballVX -= 0.5;
                } else {
                    ballVY = -ballVY * 0.3;
                }
            }

            // Check Bottom Void Drain Drop Event
            if (ballY >= HEIGHT - 1) {
                crashFreezeTimer = 35; // Short structural halt before dropping fresh ball
            }
        }

        // 2. RENDER STATIC PLAYFIELD MATRIX LAYOUT
        for (int y = 2; y <= 27; y++) {
            for (int x = 28; x <= 82; x++) {
                int index = x + WIDTH * y;

                boolean isOuterFrame = (y == 2 || y == 27 || x == 28 || x == 82);
                boolean isRailWall = (y == 3 || x == 30 || x == 80);

                if (isOuterFrame) {
                    outputBuffer[index] = COLOR_CHASSIS + "█" + RESET;
                } else if (isRailWall && y <= 25) {
                    outputBuffer[index] = COLOR_CHASSIS + "║" + RESET;
                }
            }
        }

        // Overlay Slant Lane Art Lines
        for (int i = 0; i <= 10; i++) {
            outputBuffer[(30 + i) + WIDTH * (21 + (int) (i * 0.4))] = COLOR_CHASSIS + "\\" + RESET;
            outputBuffer[(80 - i) + WIDTH * (21 + (int) (i * 0.4))] = COLOR_CHASSIS + "/" + RESET;
        }

        // 3. RENDER REBOUND BUMPERS
        for (Bumper b : bumpers) {
            String color = b.hitFlashTicks > 0 ? COLOR_BUMPER_HIT : COLOR_BUMPER;
            int idx = b.x + WIDTH * b.y;
            outputBuffer[idx - 1] = color + "(" + RESET;
            outputBuffer[idx] = color + "O" + RESET;
            outputBuffer[idx + 1] = color + ")" + RESET;
        }

        // 4. RENDER EXTENDED FLIPPER PADDLES STATES (Width 11 Each)
        int leftFX = 38;
        int rightFX = 61;
        int flipperY = 25;
        int flipperWidth = 11;

        for (int w = 0; w < flipperWidth; w++) {
            // Left paddle extended block array footprint map
            int leftIndex = (leftFX + w) + WIDTH * (flipperY - (leftFlipperActive ? w / 3 : 0));
            outputBuffer[leftIndex] = COLOR_FLIPPER + "◢" + RESET;

            // Right paddle extended block array footprint map
            int rightIndex = (rightFX + w) + WIDTH * (flipperY - (rightFlipperActive ? (10 - w) / 3 : 0));
            outputBuffer[rightIndex] = COLOR_FLIPPER + "◣" + RESET;
        }

        // 5. RENDER CHROME BALL ENTITY
        int bX = (int) Math.round(ballX);
        int bY = (int) Math.round(ballY);
        if (crashFreezeTimer == 0 && bX >= 0 && bX < WIDTH && bY >= 0 && bY < HEIGHT) {
            outputBuffer[bX + WIDTH * bY] = COLOR_BALL + "●" + RESET;
        }

        // 6. OVERLAP DASHBOARD TEXT HUD LAYERS
        String hudMsg = "SCORE: " + score;
        if (crashFreezeTimer > 0) {
            hudMsg = "⚠️ BALL DRAINED! AUTO-LAUNCHING NEW STRIP... ⚠️";
        }
        int targetX = 30 + (50 - hudMsg.length()) / 2;
        for (int i = 0; i < hudMsg.length(); i++) {
            int idx = (targetX + i) + WIDTH * 1;
            if (idx >= 0 && idx < outputBuffer.length) {
                outputBuffer[idx] = COLOR_TXT + hudMsg.charAt(i) + RESET;
            }
        }
    }
}