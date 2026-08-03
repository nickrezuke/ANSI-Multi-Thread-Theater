import java.util.Random;

public class PongLoader extends Loader {
    private static final StatusStage[] PONG_STAGES = {
        new StatusStage(25, "Calibrating paddle physics mechanics:"),
        new StatusStage(50, "Syncing computer response arrays:"),
        new StatusStage(75, "Initializing collision fields:"),
        new StatusStage(100, "Pong Matrix Online!")
    };

    // 1. Grid Structural Boundaries
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // 2. Game Entities Layout States
    private double ballX, ballY;
    private double ballVelX, ballVelY;
    private int paddleLeftY, paddleRightY;
    private static final int PADDLE_HEIGHT = 4;

    // 3. Game Management Variables
    private int scoreLeft = 0;
    private int scoreRight = 0;
    private long lastTickTime = 0;
    private static final long REFRESH_RATE_MS = 30; // ~33 Frames per second game speed
    private final Random rand = new Random();

    // Flash notification mechanism for goal highlights
    private int goalFlashTimer = 0;
    private String goalFlashText = "";

    // 4. Style Palette Color Coding
    private static final String COLOR_BALL = "\u001B[38;5;220m";   // Bright Yellow 
    private static final String COLOR_PADDLE = "\u001B[38;5;81m";  // Cyan Neon
    private static final String COLOR_NET = "\u001B[38;5;244m";     // Faint Gray Line
    private static final String COLOR_TEXT = "\u001B[38;5;255m";    // Soft White Accent

    public PongLoader() {
        super(PONG_STAGES);
    }

    @Override
    protected void initialize() {
        scoreLeft = 0;
        scoreRight = 0;
        paddleLeftY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
        paddleRightY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
        resetBall(1);
        lastTickTime = System.currentTimeMillis();
    }

    private void resetBall(int servingDirection) {
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        
        // Randomize launch vectors slightly to add mechanical variance
        ballVelX = servingDirection * 1.4;
        ballVelY = (rand.nextDouble() * 2.0 - 1.0) * 0.6;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        // Regulate specific internal game update logic frame intervals
        if (currentTime - lastTickTime >= REFRESH_RATE_MS) {
            lastTickTime = currentTime;
            updatePhysics();
        }

        drawScene(outputBuffer);
    }

    private void updatePhysics() {
        if (goalFlashTimer > 0) {
            goalFlashTimer--;
            return; 
        }
    
        // 1. Advance ball translation coordinates
        ballX += ballVelX;
        ballY += ballVelY;
    
        // 2. Ceiling / Floor Border Bounce Listeners
        if (ballY <= 0) {
            ballY = 0;
            ballVelY = -ballVelY;
        } else if (ballY >= HEIGHT - 1) {
            ballY = HEIGHT - 1;
            ballVelY = -ballVelY;
        }
    
        // 3. AI Paddle Tracking Behavior
        int targetLeft = (int) ballY - (PADDLE_HEIGHT / 2);
        if (paddleLeftY < targetLeft && paddleLeftY < HEIGHT - PADDLE_HEIGHT) paddleLeftY++;
        if (paddleLeftY > targetLeft && paddleLeftY > 0) paddleLeftY--;
    
        if (ballVelX > 0 && ballX > WIDTH / 3) {
            int targetRight = (int) ballY - (PADDLE_HEIGHT / 2);
            if (rand.nextInt(10) > 2) { 
                if (paddleRightY < targetRight && paddleRightY < HEIGHT - PADDLE_HEIGHT) paddleRightY++;
                if (paddleRightY > targetRight && paddleRightY > 0) paddleRightY--;
            }
        }
    
        // FIX: Left Paddle Collision Threshold
        // Check if the ball is moving left and has crossed behind the paddle's front face (x = 2)
        if (ballVelX < 0 && ballX <= 2) {
            if (ballY >= paddleLeftY && ballY < paddleLeftY + PADDLE_HEIGHT) {
                ballX = 2; // Snap ball to front of paddle
                ballVelX = -ballVelX * 1.05; // Deflect and slightly accelerate
                ballVelY += (ballY - (paddleLeftY + PADDLE_HEIGHT / 2.0)) * 0.25;
            }
        }
    
        // FIX: Right Paddle Collision Threshold
        // Check if the ball is moving right and has crossed behind the paddle's front face (x = WIDTH - 3)
        if (ballVelX > 0 && ballX >= WIDTH - 3) {
            if (ballY >= paddleRightY && ballY < paddleRightY + PADDLE_HEIGHT) {
                ballX = WIDTH - 3; // Snap ball to front of paddle
                ballVelX = -ballVelX * 1.05; // Deflect and slightly accelerate
                ballVelY += (ballY - (paddleRightY + PADDLE_HEIGHT / 2.0)) * 0.25;
            }
        }
    
        // 6. Score Processing Threshold Bounds
        if (ballX < 0) {
            scoreRight++;
            triggerGoalAlert("RIGHT PLAYER SCORES!");
            resetBall(1); 
        } else if (ballX >= WIDTH) {
            scoreLeft++;
            triggerGoalAlert("LEFT PLAYER SCORES!");
            resetBall(-1); 
        }
    }
    

    private void triggerGoalAlert(String message) {
        goalFlashTimer = 25; // Displays flash text for roughly 750 milliseconds
        goalFlashText = message;
    }

    private void drawScene(String[] outputBuffer) {
        // 1. Draw Centered Dotted Net Line
        for (int y = 0; y < HEIGHT; y++) {
            if (y % 2 == 0) {
                outputBuffer[WIDTH / 2 + y * WIDTH] = COLOR_NET + "┆" + RESET;
            }
        }

        // 2. Draw Left Paddle Cell Indexes
        for (int i = 0; i < PADDLE_HEIGHT; i++) {
            int idx = 1 + (paddleLeftY + i) * WIDTH;
            if (idx >= 0 && idx < outputBuffer.length) {
                outputBuffer[idx] = COLOR_PADDLE + "█" + RESET;
            }
        }

        // 3. Draw Right Paddle Cell Indexes
        for (int i = 0; i < PADDLE_HEIGHT; i++) {
            int idx = (WIDTH - 2) + (paddleRightY + i) * WIDTH;
            if (idx >= 0 && idx < outputBuffer.length) {
                outputBuffer[idx] = COLOR_PADDLE + "█" + RESET;
            }
        }

        // 4. Draw Scoreboard Hud Characters (Centered on line index 1)
        String scoreString = scoreLeft + "   SCORE   " + scoreRight;
        int scoreStartX = (WIDTH / 2) - (scoreString.length() / 2);
        for (int s = 0; s < scoreString.length(); s++) {
            int targetIdx = (scoreStartX + s) + (1 * WIDTH);
            outputBuffer[targetIdx] = COLOR_TEXT + scoreString.charAt(s) + RESET;
        }

        // 5. Draw Dynamic Floating Ball Coordinate Element
        int bX = (int) Math.round(ballX);
        int bY = (int) Math.round(ballY);
        if (bX >= 0 && bX < WIDTH && bY >= 0 && bY < HEIGHT) {
            int ballIdx = bX + bY * WIDTH;
            outputBuffer[ballIdx] = COLOR_BALL + "●" + RESET;
        }

        // 6. Draw Goal Splash Banner text overlay overlays if triggered
        if (goalFlashTimer > 0) {
            int bannerY = HEIGHT / 2 + 2;
            int textStartX = (WIDTH / 2) - (goalFlashText.length() / 2);
            for (int t = 0; t < goalFlashText.length(); t++) {
                int targetIdx = (textStartX + t) + (bannerY * WIDTH);
                if (targetIdx >= 0 && targetIdx < outputBuffer.length) {
                    outputBuffer[targetIdx] = "\u001B[38;5;196;1m" + goalFlashText.charAt(t) + RESET;
                }
            }
        }
    }
}
