import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FlappyBirdLoader extends InteractiveLoader {
    private static final StatusStage[] FLAPPY_STAGES = {
        new StatusStage(100, "[Press Any Key to Flap!!]")
    };

    private static final int WIDTH = 110;
    private static final int HEIGHT = 30;

    // Game Control Matrix
    // 0 = Active Flight, 1 = Crash Halt (Frozen State)
    private volatile int gameState = 0;
    private int inputCooldown = 0;
    private int restartTimer = 0;

    // Bird Physics Variables
    private volatile double birdY = 12.0;
    private volatile double birdVelocity = 0.0;
    private static final double GRAVITY = 0.01;
    private static final double FLAP_IMPULSE = -0.24;

    private static final double FLY_SPEED = 0.35;

    // Score Trackers
    private int score = 0;
    private int highScore = 0;

    // Procedural Pipe Vector Structures
    private static class Pipe {
        double posX;       // Horizontal position on screen
        int gapCenterY;    // Vertical center row of the passage gap
        boolean scoreClaimed = false;
    }
    private final List<Pipe> pipes = new ArrayList<>();
    private int pipeSpawnTimer = 0;
    private static final int PIPE_GAP_SIZE = 7; // Height of the safe passage open window

    // Aesthetic Clean ASCII Palette
    private static final String COLOR_SKY = "\u001B[38;2;60;160;240m";    // Daylight Blue Sky
    private static final String COLOR_PIPE = "\u001B[38;2;40;200;40m";    // Classic Green Pipe
    private static final String COLOR_BIRD = "\u001B[38;2;255;220;0m";    // Yellow Bird Body
    private static final String COLOR_BIRD_HIT = "\u001B[38;2;255;50;50m"; // Red Crash Indicator
    private static final String COLOR_GROUND = "\u001B[38;2;140;100;40m";  // Brown Soil base
    private static final String COLOR_TEXT = "\u001B[38;2;255;255;255m";   // White HUD Metrics

    private final Random random = new Random();

    public FlappyBirdLoader() {
        super(FLAPPY_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void onInitialize() {
        resetGame();
        this.highScore = 0;
    }

    private void resetGame() {
        this.birdY = 10.0;
        this.birdVelocity = -0.2;
        this.score = 0;
        this.gameState = 0;
        this.inputCooldown = 0;
        this.restartTimer = 0;
        this.pipes.clear();
        this.pipeSpawnTimer = 0;
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        if (inputCooldown > 0) return;

        if (gameState == 1) {
            // If frozen after a crash, ignore immediate panic mashing until timer finishes
            return;
        }

        // Apply instant upward physics velocity vector
        birdVelocity = FLAP_IMPULSE;
        inputCooldown = 2; // Subtle micro-debounce
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear background matrix to solid sky fields
        Arrays.fill(zBuffer, 0.0);
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = COLOR_SKY + " " + RESET;
        }

        if (inputCooldown > 0) inputCooldown--;

        // 1. GAME PHYSICS UPDATES
        if (gameState == 0) {
            // Apply step-by-step acceleration downwards
            birdVelocity += GRAVITY;
            birdY += birdVelocity;

            // Handle procedural pipe movements (Scroll leftwards)
            for (int i = pipes.size() - 1; i >= 0; i--) {
                Pipe p = pipes.get(i);
                p.posX -= FLY_SPEED; // Scrolling animation speed tracking

                // Score checking tracking pass trigger
                if (!p.scoreClaimed && p.posX < 25) {
                    score++;
                    if (score > highScore) highScore = score;
                    p.scoreClaimed = true;
                }

                // Cull offscreen pipes
                if (p.posX < -10) {
                    pipes.remove(i);
                }
            }

            // Pipe Generation Intervals
            pipeSpawnTimer--;
            if (pipeSpawnTimer <= 0) {
                Pipe newPipe = new Pipe();
                newPipe.posX = WIDTH;
                // Keep the opening bounded safely away from the absolute top and bottom sky limits
                newPipe.gapCenterY = 6 + random.nextInt(13);
                pipes.add(newPipe);
                pipeSpawnTimer = 135; // Frame layout separation spacing match
            }

            // Check Environmental Boundary Crashes (Ground or Sky Ceilings)
            if (birdY >= HEIGHT - 4 || birdY < 0) {
                triggerCrash();
            }

            // Check Pipe Object Intersection Maps
            int bX = 25; // Locked bird horizontal screen coordinate matching avatar paint
            int bY = (int) Math.round(birdY);

            for (Pipe p : pipes) {
                int pipeLeft = (int) Math.round(p.posX);
                int pipeRight = pipeLeft + 6; // Pipe thickness diameter scale

                if (bX >= pipeLeft && bX <= pipeRight) {
                    int topLimit = p.gapCenterY - (PIPE_GAP_SIZE / 2);
                    int bottomLimit = p.gapCenterY + (PIPE_GAP_SIZE / 2);

                    // If bird row alignment sits outside the open safety slice -> impact collision!
                    if (bY <= topLimit || bY >= bottomLimit) {
                        triggerCrash();
                    }
                }
            }
        } else {
            // CRASH HALT RECOVERY CLOCK: Freeze everything for 50 frames (~2 seconds) then auto-revive
            restartTimer--;
            if (restartTimer <= 0) {
                resetGame();
            }
        }

        // 2. RENDER GRAPHICS GROUND MATRIX LAYOUT
        for (int x = 0; x < WIDTH; x++) {
            for (int y = HEIGHT - 3; y < HEIGHT; y++) {
                outputBuffer[x + WIDTH * y] = COLOR_GROUND + "█" + RESET;
            }
        }

        // 3. RENDER SCROLLING GREEN PIPES
        for (Pipe p : pipes) {
            int startX = (int) Math.round(p.posX);
            int topLimit = p.gapCenterY - (PIPE_GAP_SIZE / 2);
            int bottomLimit = p.gapCenterY + (PIPE_GAP_SIZE / 2);

            for (int w = 0; w < 7; w++) {
                int drawX = startX + w;
                if (drawX < 0 || drawX >= WIDTH) continue;

                // Draw Top Pipe half down from ceiling sky
                for (int y = 0; y <= topLimit; y++) {
                    char blockSymbol = (w == 0 || w == 6) ? '│' : '█';
                    outputBuffer[drawX + WIDTH * y] = COLOR_PIPE + blockSymbol + RESET;
                }

                // Draw Bottom Pipe half extending down to mud floor row
                for (int y = bottomLimit; y < HEIGHT - 3; y++) {
                    char blockSymbol = (w == 0 || w == 6) ? '│' : '█';
                    outputBuffer[drawX + WIDTH * y] = COLOR_PIPE + blockSymbol + RESET;
                }
            }
        }

        // 4. RENDER FLAPPY BIRD AVATAR (3 Rows x 4 Cols Multi-line Block Style)
        int birdScreenX = 25;
        int birdScreenY = (int) Math.round(birdY);
        
        // Flash alternate invisible frames if hit state is ticked active
        boolean blinkHideFrame = (gameState == 1) && (restartTimer % 6 < 3);
        String currentBirdColor = (gameState == 1) ? COLOR_BIRD_HIT : COLOR_BIRD;

        if (!blinkHideFrame && birdScreenY >= 1 && birdScreenY < HEIGHT - 4) {
            // Row 0: Eyes / Head Profile
            outputBuffer[birdScreenX + WIDTH * (birdScreenY - 1)] = currentBirdColor + "▄" + RESET;
            outputBuffer[birdScreenX + 1 + WIDTH * (birdScreenY - 1)] = currentBirdColor + "█" + RESET;
            outputBuffer[birdScreenX + 2 + WIDTH * (birdScreenY - 1)] = "\u001B[37m█\u001B[0m"; // White Eye
            
            // Row 1: Main Torso Core & Orange Beak
            outputBuffer[birdScreenX - 1 + WIDTH * birdScreenY] = currentBirdColor + "█" + RESET; // Wing
            outputBuffer[birdScreenX + WIDTH * birdScreenY] = currentBirdColor + "█" + RESET;
            outputBuffer[birdScreenX + 1 + WIDTH * birdScreenY] = currentBirdColor + "█" + RESET;
            outputBuffer[birdScreenX + 2 + WIDTH * birdScreenY] = "\u001B[38;2;255;100;0m" + "▶" + RESET; // Beak
            
            // Row 2: Underbelly Profile
            outputBuffer[birdScreenX + WIDTH * (birdScreenY + 1)] = currentBirdColor + "▀" + RESET;
            outputBuffer[birdScreenX + 1 + WIDTH * (birdScreenY + 1)] = currentBirdColor + "▀" + RESET;
        }

        // 5. OVERLAP HUD HEADS UP TEXT DASHBOARD (Score Counters)
        String scoreMessage = "SCORE: " + score + "   |   BEST: " + highScore;
        if (gameState == 1) {
            scoreMessage = "💥 CRASH! ACCELERATION HALTED... RESTARS SOON 💥";
        }

        int targetX = (WIDTH - scoreMessage.length()) / 2;
        int targetY = 2; // Position high up in the blue sky row index line

        for (int i = 0; i < scoreMessage.length(); i++) {
            int bufferIdx = (targetX + i) + WIDTH * targetY;
            if (bufferIdx >= 0 && bufferIdx < outputBuffer.length) {
                outputBuffer[bufferIdx] = COLOR_TEXT + scoreMessage.charAt(i) + RESET;
            }
        }
    }

    private void triggerCrash() {
        this.gameState = 1;         // Halt physics loop steps
        this.restartTimer = 50;     // Lock frames for full 2-second visual freeze
        this.birdVelocity = 0.0;    // Zero animation velocity tracking
    }
}
