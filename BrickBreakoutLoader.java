import java.util.Random;

public class BrickBreakoutLoader extends Loader {
    private static final StatusStage[] ARK_STAGES = {
        new StatusStage(25, "Loading breakout brick matrices:"),
        new StatusStage(50, "Calibrating tracking paddle logic:"),
        new StatusStage(75, "Syncing structural collision arrays:"),
        new StatusStage(100, "Breakout Core Operational!")
    };

    // 1. Grid Structural Dimensions
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;

    // 2. Brick Configuration Matrix
    private static final int BRICK_ROWS = 4;
    private static final int BRICK_COLS = 10;
    private static final int BRICK_WIDTH = 6; 
    private static final int BRICK_HEIGHT = 1;
    private static final int BRICK_START_X = 10;
    private static final int BRICK_START_Y = 3;

    // True/False matrix representing active or destroyed bricks
    private final boolean[][] bricks = new boolean[BRICK_ROWS][BRICK_COLS];
    
    // 3. Game State Entities
    private double ballX, ballY;
    private double ballVelX, ballVelY;
    private int paddleX;
    private static final int PADDLE_WIDTH = 8;
    private static final int PADDLE_Y = HEIGHT - 2;

    private int score = 0;
    private boolean isDead = false;
    private long lastTickTime = 0;
    private static final long REFRESH_RATE_MS = 25; // Continuous running speed
    private long deathTimestamp = 0;
    private final Random rand = new Random();

    // 4. Colorful Brick Palette Design Profile
    private static final String[] ROW_COLORS = {
        "\u001B[38;5;196m", // Row 0: Neon Red
        "\u001B[38;5;214m", // Row 1: Orange
        "\u001B[38;5;226m", // Row 2: Yellow
        "\u001B[38;5;46m"   // Row 3: Green
    };
    private static final String COLOR_BALL = "\u001B[38;5;255m";   // White Ball
    private static final String COLOR_PADDLE = "\u001B[38;5;81m";  // Cyan Paddle
    private static final String COLOR_TEXT = "\u001B[38;5;244m";    // Accent Gray

    public BrickBreakoutLoader() {
        super(ARK_STAGES);
    }

    @Override
    protected void initialize() {
        score = 0;
        resetBoard();
    }

    private void resetBoard() {
        isDead = false;
        paddleX = (WIDTH / 2) - (PADDLE_WIDTH / 2);
        
        // Regenerate full brick field grid structures
        for (int r = 0; r < BRICK_ROWS; r++) {
            for (int c = 0; c < BRICK_COLS; c++) {
                bricks[r][c] = true;
            }
        }
        resetBall();
        lastTickTime = System.currentTimeMillis();
    }

    private void resetBall() {
        ballX = WIDTH / 2;
        ballY = HEIGHT - 5;
        ballVelX = rand.nextBoolean() ? 0.9 : -0.9;
        ballVelY = -0.7; // Head straight upwards into the brick array block initially
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        // One second loop death freeze listener handler edge case
        if (isDead) {
            if (currentTime - deathTimestamp >= 1000) {
                resetBoard();
            } else {
                drawScene(outputBuffer);
                return;
            }
        }

        if (currentTime - lastTickTime >= REFRESH_RATE_MS) {
            lastTickTime = currentTime;
            updatePhysics();
        }

        drawScene(outputBuffer);
    }

    private void updatePhysics() {
        // 1. Advance Ball Vector Coordinates
        ballX += ballVelX;
        ballY += ballVelY;

        // 2. Wall Collision Boundaries Listener Checks
        if (ballX <= 0) {
            ballX = 0;
            ballVelX = -ballVelX;
        } else if (ballX >= WIDTH - 1) {
            ballX = WIDTH - 1;
            ballVelX = -ballVelX;
        }

        if (ballY <= 0) {
            ballY = 0;
            ballVelY = -ballVelY;
        }

        // 3. AI Smart Target Tracking Loop Logic
        // Calculates paddle destination points based on active horizontal ball indices
        int paddleTarget = (int) ballX - (PADDLE_WIDTH / 2);
        if (paddleX < paddleTarget && paddleX < WIDTH - PADDLE_WIDTH) paddleX += 2;
        if (paddleX > paddleTarget && paddleX > 0) paddleX -= 2;

        // 4. Proportional Deflection Paddle Bounce Listeners
        if (ballVelY > 0 && Math.round(ballY) == PADDLE_Y) {
            if (ballX >= paddleX && ballX < paddleX + PADDLE_WIDTH) {
                ballVelY = -ballVelY;
                // Calculate dynamic deflection angles relative to contact position along paddle face
                double hitFactor = (ballX - (paddleX + PADDLE_WIDTH / 2.0)) / (PADDLE_WIDTH / 2.0);
                ballVelX = hitFactor * 1.2;
            }
        }

        // 5. High-Precision Brick Grid Intersection Sweeper
        int bX = (int) ballX;
        int bY = (int) ballY;

        for (int r = 0; r < BRICK_ROWS; r++) {
            int brickTop = BRICK_START_Y + r * BRICK_HEIGHT;
            if (bY == brickTop) {
                for (int c = 0; c < BRICK_COLS; c++) {
                    if (!bricks[r][c]) continue;

                    int brickLeft = BRICK_START_X + c * BRICK_WIDTH;
                    int brickRight = brickLeft + BRICK_WIDTH;

                    if (bX >= brickLeft && bX < brickRight) {
                        bricks[r][c] = false; // Shatter structural node
                        ballVelY = -ballVelY;  // Reflect vertical translation path
                        score += 10;
                        return; // Process one single break event impact milestone per tick
                    }
                }
            }
        }

        // 6. Out-Of-Bounds Floor Death Threshold Listeners
        if (ballY >= HEIGHT) {
            isDead = true;
            deathTimestamp = System.currentTimeMillis();
        }
    }

    private void drawScene(String[] outputBuffer) {
        // 1. Render Scoreboard HUD Displays
        String hud = "SCORE: " + score;
        for (int i = 0; i < hud.length(); i++) {
            outputBuffer[2 + i] = COLOR_TEXT + hud.charAt(i) + RESET;
        }

        // 2. Render Brick Field Arrays Block Components
        for (int r = 0; r < BRICK_ROWS; r++) {
            String color = ROW_COLORS[r];
            int brickTop = BRICK_START_Y + r * BRICK_HEIGHT;
            int rowOffset = brickTop * WIDTH;

            for (int c = 0; c < BRICK_COLS; c++) {
                if (!bricks[r][c]) continue;

                int brickLeft = BRICK_START_X + c * BRICK_WIDTH;
                for (int w = 0; w < BRICK_WIDTH; w++) {
                    int cellIdx = (brickLeft + w) + rowOffset;
                    if (cellIdx >= 0 && cellIdx < outputBuffer.length) {
                        // Edge layout caps style blocks into separate individual elements cleanly
                        outputBuffer[cellIdx] = color + ((w == 0 || w == BRICK_WIDTH - 1) ? "█" : "▀") + RESET;
                    }
                }
            }
        }

        // 3. Render Tracking Paddle Matrix Block
        int paddleOffset = PADDLE_Y * WIDTH;
        for (int i = 0; i < PADDLE_WIDTH; i++) {
            int idx = (paddleX + i) + paddleOffset;
            if (idx >= 0 && idx < outputBuffer.length) {
                outputBuffer[idx] = COLOR_PADDLE + "═" + RESET;
            }
        }

        // 4. Render Active Floating Ball Elements
        int bX = (int) Math.round(ballX);
        int bY = (int) Math.round(ballY);
        if (bX >= 0 && bX < WIDTH && bY >= 0 && bY < HEIGHT) {
            int ballIdx = bX + bY * WIDTH;
            // Freeze head element to a critical death marker cross if boundary failure registers
            outputBuffer[ballIdx] = isDead ? "\u001B[31mX" + RESET : COLOR_BALL + "●" + RESET;
        }
    }
}
