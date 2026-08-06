// TODO: Sometimes the game making a TETRIS doesnt move the other pieces doen correctly, I've occasionally seen floating things / empty bars
// TODO: Fast Drop doesnt really work when we're in a tight corridor and can't move to where we wanna go... maybe just make the regular speed that fast??

import java.util.Random;

public class TetrisLoader extends Loader {
    private static final StatusStage[] TETRIS_STAGES = {
            new StatusStage(25, "Booting tetromino matrices:"),
            new StatusStage(50, "Calibrating real-time action delays:"),
            new StatusStage(75, "Syncing row-clear bitwise operations:"),
            new StatusStage(100, "Tetris Real-Time CPU Operational!")
    };

    private static final int FIELD_W = 10;
    private static final int FIELD_H = 20;

    private static final int[][][] SHAPES = {
            { { 1, 1, 1, 1 } }, // I Piece
            { { 1, 1, 1 }, { 0, 1, 0 } }, // T Piece
            { { 1, 1, 1 }, { 1, 0, 0 } }, // L Piece
            { { 1, 1, 1 }, { 0, 0, 1 } }, // J Piece
            { { 1, 1, 0 }, { 0, 1, 1 } }, // Z Piece
            { { 0, 1, 1 }, { 1, 1, 0 } }, // S Piece
            { { 1, 1 }, { 1, 1 } } // O Piece
    };

    private static final String[] SHAPE_NAMES = { "I", "T", "L", "J", "Z", "S", "O" };

    private static final String[] SHAPE_COLORS = {
            "\u001B[38;5;81m", // I: Cyan
            "\u001B[38;5;99m", // T: Purple
            "\u001B[38;5;214m", // L: Orange
            "\u001B[38;5;27m", // J: Blue
            "\u001B[38;5;196m", // Z: Red
            "\u001B[38;5;46m", // S: Green
            "\u001B[38;5;226m" // O: Yellow
    };

    private final int[][] grid = new int[FIELD_H][FIELD_W];
    private int activeType = 0;
    private int nextType = 0;
    private int[][] currentPiece;
    private int pieceX, pieceY;

    private int targetX = 0;
    private int targetRotation = 0;
    private int currentRotation = 0;

    private int score = 0;
    private int linesCleared = 0;
    private int totalPiecesPlaced = 0;
    private boolean gameOver = false;

    private long lastTickTime = 0;
    private long lastAIActionTime = 0;

    private static final long GRAVITY_NORMAL_MS = 350; // Snappier baseline normal gravity
    private static final long GRAVITY_SOFT_DROP_MS = 20; // Turbo drop speed when aligned
    private static final long AI_ACTION_DELAY_MS = 50;

    private final Random rand = new Random();

    private static final String COLOR_BORDER = "\u001B[38;5;244m";
    private static final String COLOR_TEXT = "\u001B[38;5;250m";
    private static final String COLOR_VALUE = "\u001B[38;5;82m";

    public TetrisLoader() {
        super(TETRIS_STAGES);
    }

    @Override
    protected void initialize() {
        nextType = rand.nextInt(SHAPES.length);
        resetGame();
    }

    private void resetGame() {
        gameOver = false;
        score = 0;
        linesCleared = 0;
        totalPiecesPlaced = 0;
        for (int y = 0; y < FIELD_H; y++) {
            for (int x = 0; x < FIELD_W; x++) {
                grid[y][x] = 0;
            }
        }
        spawnNewPiece();
        lastTickTime = System.currentTimeMillis();
        lastAIActionTime = System.currentTimeMillis();
    }

    private void spawnNewPiece() {
        activeType = nextType;
        nextType = rand.nextInt(SHAPES.length);
        currentPiece = SHAPES[activeType];

        pieceX = (FIELD_W - currentPiece[0].length) / 2;
        pieceY = 0;
        currentRotation = 0;

        if (checkCollision(currentPiece, pieceX, pieceY)) {
            gameOver = true;
            lastTickTime = System.currentTimeMillis();
            return;
        }

        calculateBestTarget();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (gameOver) {
            if (currentTime - lastTickTime >= 2500) {
                resetGame();
            } else {
                drawScene(outputBuffer);
                return;
            }
        }

        // 1. Process CPU Controller Actions
        if (currentTime - lastAIActionTime >= AI_ACTION_DELAY_MS) {
            if (processRealTimeAIAction()) {
                lastAIActionTime = currentTime;
            }
        }

        // 2. Dynamic Adaptive Gravity System
        // If alignment targets match perfectly, engage turbo drop interval speed
        boolean targetReached = (currentRotation == targetRotation && pieceX == targetX);
        long activeDropDelay = targetReached ? GRAVITY_SOFT_DROP_MS : GRAVITY_NORMAL_MS;

        if (currentTime - lastTickTime >= activeDropDelay) {
            lastTickTime = currentTime;
            advanceFallingPiece();
        }

        drawScene(outputBuffer);
    }

    private boolean processRealTimeAIAction() {
        if (currentRotation < targetRotation) {
            int[][] rotated = rotateMatrix(currentPiece);
            if (!checkCollision(rotated, pieceX, pieceY)) {
                currentPiece = rotated;
                currentRotation++;
                return true;
            }
        }

        if (pieceX < targetX) {
            if (!checkCollision(currentPiece, pieceX + 1, pieceY)) {
                pieceX++;
                return true;
            }
        } else if (pieceX > targetX) {
            if (!checkCollision(currentPiece, pieceX - 1, pieceY)) {
                pieceX--;
                return true;
            }
        }

        return false;
    }

    private void advanceFallingPiece() {
        if (!checkCollision(currentPiece, pieceX, pieceY + 1)) {
            pieceY++;
        } else {
            lockPiece();
            checkLineClears();
            spawnNewPiece();
        }
    }

    private void calculateBestTarget() {
        int bestRotation = 0;
        int bestX = pieceX;
        double bestScore = -999999.0;

        int[][] workingShape = SHAPES[activeType];

        for (int r = 0; r < 4; r++) {
            // Check column-width bounds instead of row-height count
            int maxValidX = FIELD_W - workingShape[0].length;

            for (int currX = 0; currX <= maxValidX; currX++) {
                int simulatedY = 0;
                while (!checkCollision(workingShape, currX, simulatedY + 1)) {
                    simulatedY++;
                }

                double evaluationValue = evaluatePlacementHeuristics(workingShape, currX, simulatedY);
                if (evaluationValue > bestScore) {
                    bestScore = evaluationValue;
                    bestRotation = r;
                    bestX = currX;
                }
            }
            workingShape = rotateMatrix(workingShape);
        }

        targetRotation = bestRotation;
        targetX = bestX;
    }

    private double evaluatePlacementHeuristics(int[][] shape, int startX, int targetY) {
        int[][] tempGrid = new int[FIELD_H][FIELD_W];
        for (int y = 0; y < FIELD_H; y++) {
            System.arraycopy(grid[y], 0, tempGrid[y], 0, FIELD_W);
        }

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] > 0) {
                    tempGrid[targetY + row][startX + col] = 1;
                }
            }
        }

        int holesCount = 0;
        int cumulativeHeight = 0;

        for (int x = 0; x < FIELD_W; x++) {
            boolean blockFound = false;
            for (int y = 0; y < FIELD_H; y++) {
                if (tempGrid[y][x] > 0) {
                    if (!blockFound) {
                        blockFound = true;
                        cumulativeHeight += (FIELD_H - y);
                    }
                } else if (blockFound && tempGrid[y][x] == 0) {
                    holesCount++;
                }
            }
        }

        // Standardized balancing parameters maximizing clearance efficiency
        return (-0.5 * cumulativeHeight) - (9.5 * holesCount);
    }

    private boolean checkCollision(int[][] shape, int checkX, int checkY) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] > 0) {
                    int boardX = checkX + c;
                    int boardY = checkY + r;

                    if (boardX < 0 || boardX >= FIELD_W || boardY >= FIELD_H)
                        return true;
                    if (boardY >= 0 && grid[boardY][boardX] > 0)
                        return true;
                }
            }
        }
        return false;
    }

    private void lockPiece() {
        for (int r = 0; r < currentPiece.length; r++) {
            for (int c = 0; c < currentPiece[r].length; c++) {
                if (currentPiece[r][c] > 0) {
                    int boardY = pieceY + r;
                    int boardX = pieceX + c;
                    if (boardY >= 0 && boardY < FIELD_H && boardX >= 0 && boardX < FIELD_W) {
                        grid[boardY][boardX] = activeType + 1;
                    }
                }
            }
        }
        totalPiecesPlaced++;
    }

    private void checkLineClears() {
        int continuousLinesThisFrame = 0;
        for (int y = FIELD_H - 1; y >= 0; y--) {
            boolean rowIsFull = true;
            for (int x = 0; x < FIELD_W; x++) {
                if (grid[y][x] == 0) {
                    rowIsFull = false;
                    break;
                }
            }

            if (rowIsFull) {
                continuousLinesThisFrame++;
                linesCleared++;
                for (int ty = y; ty > 0; ty--) {
                    System.arraycopy(grid[ty - 1], 0, grid[ty], 0, FIELD_W);
                }
                for (int x = 0; x < FIELD_W; x++)
                    grid[y][x] = 0;
                y++;
            }
        }

        if (continuousLinesThisFrame == 1)
            score += 40;
        else if (continuousLinesThisFrame == 2)
            score += 100;
        else if (continuousLinesThisFrame == 3)
            score += 300;
        else if (continuousLinesThisFrame == 4)
            score += 1200;
    }

    private int[][] rotateMatrix(int[][] matrix) {
        int r = matrix.length;
        int c = matrix[0].length;
        int[][] target = new int[c][r];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                target[j][r - 1 - i] = matrix[i][j];
            }
        }
        return target;
    }

    private void drawScene(String[] outputBuffer) {
        int leftPaddingOffset = 4;
        for (int y = 0; y < FIELD_H; y++) {
            int lineOffset = (1 + y) * 80 + leftPaddingOffset;
            outputBuffer[lineOffset - 1] = COLOR_BORDER + "│" + RESET;
            for (int x = 0; x < FIELD_W; x++) {
                int cellVal = grid[y][x];
                int bufferX = lineOffset + (x * 2);
                if (cellVal > 0) {
                    String color = SHAPE_COLORS[cellVal - 1];
                    outputBuffer[bufferX] = color + "█";
                    outputBuffer[bufferX + 1] = "█" + RESET;
                } else {
                    boolean isPieceBlock = false;
                    if (!gameOver) {
                        int pieceRow = y - pieceY;
                        int pieceCol = x - pieceX;
                        if (pieceRow >= 0 && pieceRow < currentPiece.length && pieceCol >= 0
                                && pieceCol < currentPiece[pieceRow].length) {
                            if (currentPiece[pieceRow][pieceCol] > 0) {
                                isPieceBlock = true;
                            }
                        }
                    }
                    if (isPieceBlock) {
                        String color = SHAPE_COLORS[activeType];
                        outputBuffer[bufferX] = color + "█";
                        outputBuffer[bufferX + 1] = "█" + RESET;
                    } else {
                        outputBuffer[bufferX] = "\u001B[38;5;234m·";
                        outputBuffer[bufferX + 1] = "·\u001B[0m";
                    }
                }
            }
            outputBuffer[lineOffset + (FIELD_W * 2)] = COLOR_BORDER + "│" + RESET;
        }
        int floorOffset = (1 + FIELD_H) * 80 + leftPaddingOffset;
        outputBuffer[floorOffset - 1] = COLOR_BORDER + "└" + RESET;
        for (int i = 0; i < FIELD_W * 2; i++) {
            outputBuffer[floorOffset + i] = COLOR_BORDER + "─" + RESET;
        }
        outputBuffer[floorOffset + (FIELD_W * 2)] = COLOR_BORDER + "┘" + RESET;
        int textColumnX = 28;
        writeText(outputBuffer, textColumnX, 1, "┌────────────────────────────────────────┐", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 2, "│         TETRIS CORE ENGINE HUD         │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 3, "├────────────────────────────────────────┤", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 4, "│ ", COLOR_BORDER);
        if (gameOver) {
            writeText(outputBuffer, textColumnX + 2, 4, String.format("%-38s", "STATUS: SYSTEM OVERFLOW (DEAD)"),
                    "\u001B[38;5;196m");
        } else {
            writeText(outputBuffer, textColumnX + 2, 4, String.format("%-38s", "STATUS: REAL-TIME CONTROLLER"),
                    "\u001B[38;5;82m");
        }
        writeText(outputBuffer, textColumnX + 41, 4, "│", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 5, "├────────────────────────────────────────┤", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 6, "│ MATCH SCORE:                           │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX + 20, 6, String.format("%18d", score), COLOR_VALUE);
        writeText(outputBuffer, textColumnX, 7, "│ LINES CLEARED:                         │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX + 20, 7, String.format("%18d", linesCleared), COLOR_VALUE);
        writeText(outputBuffer, textColumnX, 8, "│ PLACED BLOCKS:                         │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX + 20, 8, String.format("%18d", totalPiecesPlaced), COLOR_VALUE);
        writeText(outputBuffer, textColumnX, 9, "│ CURRENT PIECE:                         │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX + 20, 9, String.format("%18s", SHAPE_NAMES[activeType]),
                SHAPE_COLORS[activeType]);
        writeText(outputBuffer, textColumnX, 10, "├────────────────────────────────────────┤", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 11, "│ UPCOMING NEXT PIECE GRAPHIC PLOT:      │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 12, "│        ┌──────────┐                    │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 13, "│        │          │                    │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 14, "│        │          │                    │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 15, "│        └──────────┘                    │", COLOR_BORDER);
        int[][] nextShape = SHAPES[nextType];
        String nextColor = SHAPE_COLORS[nextType];
        int nextBoxStartX = textColumnX + 10 + (4 - nextShape[0].length);
        int nextBoxStartY = 13 + (2 - nextShape.length) / 2;
        for (int r = 0; r < nextShape.length; r++) {
            for (int c = 0; c < nextShape[r].length; c++) {
                if (nextShape[r][c] > 0) {
                    int plotX = nextBoxStartX + (c * 2);
                    int plotY = nextBoxStartY + r;
                    int bufIdx = plotX + plotY * 80;
                    outputBuffer[bufIdx] = nextColor + "█";
                    outputBuffer[bufIdx + 1] = "█" + RESET;
                }
            }
        }
        writeText(outputBuffer, textColumnX, 16, "├────────────────────────────────────────┤", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 17, "│ REAL-TIME CORE ARCHITECTURE METRICS:   │", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 18, "│ ", COLOR_BORDER);
        writeText(outputBuffer, textColumnX + 2, 18,
                String.format("%-38s",
                        String.format("> controller_input_delay:     %2dms", AI_ACTION_DELAY_MS)),
                COLOR_TEXT);
        writeText(outputBuffer, textColumnX + 41, 18, "│", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 19, "│ ", COLOR_BORDER);
        writeText(outputBuffer, textColumnX + 2, 19,
                String.format("%-38s",
                        String.format("> intelligent_soft_drop:     ACTIVE")),
                COLOR_TEXT);
        writeText(outputBuffer, textColumnX + 41, 19, "│", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 20, "│ ", COLOR_BORDER);
        writeText(outputBuffer, textColumnX + 2, 20,
                String.format("%-38s",
                        String.format("> gravity_step_interval:     %2dms", GRAVITY_NORMAL_MS)),
                COLOR_TEXT);
        writeText(outputBuffer, textColumnX + 41, 20, "│", COLOR_BORDER);
        writeText(outputBuffer, textColumnX, 21, "└────────────────────────────────────────┘", COLOR_BORDER);
    }

    private void writeText(String[] outputBuffer, int startX, int row, String content, String colorCode) {
        int offset = row * 80 + startX;
        for (int i = 0; i < content.length(); i++) {
            if (offset + i < outputBuffer.length) {
                outputBuffer[offset + i] = colorCode + content.charAt(i) + RESET;
            }
        }
    }
}