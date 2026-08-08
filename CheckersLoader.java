import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CheckersLoader extends Loader {
    private static final StatusStage[] CHECKERS_STAGES = {
            new StatusStage(25, "Structuring spatial 8x8 checkers matrix:"),
            new StatusStage(50, "Calibrating piece forward progression weight:"),
            new StatusStage(75, "Constructing mandatory jump search trees:"),
            new StatusStage(100, "Checkers Simulation Engine Online!")
    };

    private static final int EMPTY = 0;
    private static final int RED_PAWN = 1, RED_KING = 2;
    private static final int BLACK_PAWN = 3, BLACK_KING = 4;
    private static final int RED_SIDE = 8, BLACK_SIDE = 16;

    private final int[] board = new int[64];
    private boolean isRedTurn = true; // Red moves first (standard Checkers rule)
    private final List<String> moveHistory = new ArrayList<>();
    private int totalMovesPlayed = 0;

    private long lastMoveTime = 0;
    private static final long MOVE_DELAY_MS = 800;
    private boolean gameOver = false;
    private String gameResultText = "";

    private static final String LIGHT_SQUARE = "\u001B[48;5;250m";
    private static final String DARK_SQUARE = "\u001B[48;5;238m";
    private static final String COLOR_RED_PIECE = "\u001B[38;5;196m"; // Bright Red
    private static final String COLOR_BLACK_PIECE = "\u001B[38;5;232m"; // True Black Text
    private static final String COLOR_TEXT = "\u001B[38;5;244m";
    private static final String COLOR_TURN = "\u001B[38;5;82m";
    private static final String COLOR_OVER = "\u001B[38;5;196m";

    private static final String[] PIECE_SYMBOLS = {
            " ", "\u26C2", "\u26C3", "\u26C2", "\u26C3" // ⛂ Pawn, ⛃ King
    };

    // Encourages pieces to step out of back-rows and march down fields securely
    private static final int[] PROGRESSION_TABLE = {
            0, 5, 0, 5, 0, 5, 0, 5,
            10, 0, 10, 0, 10, 0, 10, 0,
            0, 15, 0, 20, 0, 20, 0, 15,
            15, 0, 25, 0, 25, 0, 15, 0,
            0, 20, 0, 25, 0, 20, 0, 20,
            15, 0, 15, 0, 15, 0, 15, 0,
            0, 10, 0, 10, 0, 10, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static class Move {
        int from, to, score, guessScore;
        boolean isJump;
        int capturedIdx = -1;

        Move(int from, int to, boolean isJump, int capturedIdx) {
            this.from = from;
            this.to = to;
            this.isJump = isJump;
            this.capturedIdx = capturedIdx;
        }

        Move(int score) {
            this.score = score;
        }
    }

    public CheckersLoader() {
        // This uses 80x22 specifically
        super(CHECKERS_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        resetGame();
    }

    private void resetGame() {
        gameOver = false;
        isRedTurn = true;
        moveHistory.clear();
        totalMovesPlayed = 0;

        for (int i = 0; i < 64; i++)
            board[i] = EMPTY;

        // Populate checkers layout fields on alternate dark squares
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 != 0) {
                    int idx = col + row * 8;
                    if (row < 3)
                        board[idx] = BLACK_PAWN | BLACK_SIDE;
                    else if (row > 4)
                        board[idx] = RED_PAWN | RED_SIDE;
                }
            }
        }
        lastMoveTime = System.currentTimeMillis();
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (gameOver) {
            if (currentTime - lastMoveTime >= 4000) {
                resetGame();
            } else {
                drawScene(outputBuffer);
                return;
            }
        }

        if (currentTime - lastMoveTime >= MOVE_DELAY_MS) {
            lastMoveTime = currentTime;
            executeAIMove();
        }

        drawScene(outputBuffer);
    }

    private void executeAIMove() {
        Move bestMove = null;

        // Roll a die. (chance to "make a mistake")
        if (Math.random() * 2.0 < 1.0) { // Made the chance 1/2, its hard to mess up checkers tbh
            // Generate the exact same legal moves minimax would look at
            List<Move> legalMoves = generateAllLegalMoves(isRedTurn ? RED_SIDE : BLACK_SIDE);
            if (!legalMoves.isEmpty()) {
                // Pick a completely random legal move from the list
                bestMove = legalMoves.get((int) (Math.random() * legalMoves.size()));
            }
        }

        // Did not make a mistake, use Minimax
        else {
            bestMove = minimax(4, isRedTurn, -100000, 100000);
        }

        if (bestMove == null || bestMove.from == bestMove.to) {
            gameOver = true;
            gameResultText = isRedTurn ? "BLACK WINS BY ELIMINATION!" : "RED WINS BY ELIMINATION!";
            lastMoveTime = System.currentTimeMillis();
            return;
        }

        String sideStr = isRedTurn ? "R" : "B";
        String tag = (board[bestMove.from] == RED_KING || board[bestMove.from] == BLACK_KING) ? "K" : sideStr;
        String notation = tag + " " + indexToNotation(bestMove.from) + (bestMove.isJump ? " transition ⚔ " : "→")
                + indexToNotation(bestMove.to);
        moveHistory.add(notation);
        totalMovesPlayed++;
        if (moveHistory.size() > 12) {
            moveHistory.remove(0);
        }
        // Apply piece move assignments
        board[bestMove.to] = board[bestMove.from];
        board[bestMove.from] = EMPTY;
        // Process capture deletion if step triggered a leap jump
        if (bestMove.isJump && bestMove.capturedIdx != -1) {
            board[bestMove.capturedIdx] = EMPTY;
        }
        // Handle King promotions at back ranks using your exact constants
        // Strip the side flags (RED_SIDE/BLACK_SIDE) to look at just the piece type
        int plainPieceType = board[bestMove.to] & 7;
        if (plainPieceType == RED_PAWN && (bestMove.to / 8 == 0)) {
            board[bestMove.to] = RED_KING | RED_SIDE; // Keeps the side flag intact
        } else if (plainPieceType == BLACK_PAWN && (bestMove.to / 8 == 7)) {
            board[bestMove.to] = BLACK_KING | BLACK_SIDE;
        }

        isRedTurn = !isRedTurn;
    }

    private Move minimax(int depth, boolean isMax, int alpha, int beta) {
        List<Move> legalMoves = generateAllLegalMoves(isMax ? RED_SIDE : BLACK_SIDE);
        if (depth == 0 || legalMoves.isEmpty()) {
            return new Move(evaluateBoard());
        }

        scoreMoves(legalMoves);
        legalMoves.sort(new Comparator<Move>() {
            @Override
            public int compare(Move m1, Move m2) {
                return Integer.compare(m2.guessScore, m1.guessScore);
            }
        });

        Move bestMove = new Move(isMax ? -99999 : 99999);

        for (Move move : legalMoves) {
            int savedPiece = board[move.to];
            int savedCaptured = (move.capturedIdx != -1) ? board[move.capturedIdx] : EMPTY;

            // Apply scratchpad tracking
            board[move.to] = board[move.from];
            board[move.from] = EMPTY;
            if (move.isJump && move.capturedIdx != -1)
                board[move.capturedIdx] = EMPTY;

            int score = minimax(depth - 1, !isMax, alpha, beta).score;

            // Roll back state values safely
            board[move.from] = board[move.to];
            board[move.to] = savedPiece;
            if (move.isJump && move.capturedIdx != -1)
                board[move.capturedIdx] = savedCaptured;

            if (isMax) {
                if (score > bestMove.score) {
                    bestMove.score = score;
                    bestMove.from = move.from;
                    bestMove.to = move.to;
                    bestMove.isJump = move.isJump;
                    bestMove.capturedIdx = move.capturedIdx;
                }
                alpha = Math.max(alpha, score);
            } else {
                if (score < bestMove.score) {
                    bestMove.score = score;
                    bestMove.from = move.from;
                    bestMove.to = move.to;
                    bestMove.isJump = move.isJump;
                    bestMove.capturedIdx = move.capturedIdx;
                }
                beta = Math.min(beta, score);
            }
            if (beta <= alpha)
                break;
        }
        return bestMove;
    }

    private void scoreMoves(List<Move> moves) {
        for (Move m : moves) {
            // Mandatory jumps receive maximum priority pruning sort weight
            m.guessScore = m.isJump ? 5000 : 0;
        }
    }

    private List<Move> generateAllLegalMoves(int side) {
        List<Move> jumps = new ArrayList<>();
        List<Move> simpleMoves = new ArrayList<>();

        for (int i = 0; i < 64; i++) {
            if (board[i] != EMPTY && (board[i] & side) != 0) {
                generatePieceMoves(i, jumps, simpleMoves);
            }
        }
        // Under Checkers guidelines, if a jump capture is legal, simple steps are
        // banned
        return !jumps.isEmpty() ? jumps : simpleMoves;
    }

    private void generatePieceMoves(int from, List<Move> jumps, List<Move> simpleMoves) {
        int piece = board[from] & 7;
        int side = board[from] & (RED_SIDE | BLACK_SIDE);
        int opposingSide = (side == RED_SIDE) ? BLACK_SIDE : RED_SIDE;

        int row = from / 8, col = from % 8;

        // Define direction tracking matrices based on piece hierarchy
        int[] rowsDir = (piece == RED_PAWN) ? new int[] { -1 }
                : (piece == BLACK_PAWN) ? new int[] { 1 } : new int[] { -1, 1 };
        int[] colsDir = { -1, 1 };

        for (int dr : rowsDir) {
            for (int dc : colsDir) {
                int nextR = row + dr, nextC = col + dc;

                // Simple Diagonal move lookup parameters
                if (nextR >= 0 && nextR < 8 && nextC >= 0 && nextC < 8) {
                    int toIdx = nextC + nextR * 8;
                    if (board[toIdx] == EMPTY) {
                        simpleMoves.add(new Move(from, toIdx, false, -1));
                    }
                }

                // 2-Step Jump Leap check variations
                int jumpR = row + dr * 2, jumpC = col + dc * 2;
                if (jumpR >= 0 && jumpR < 8 && jumpC >= 0 && jumpC < 8) {
                    int intermediateIdx = (col + dc) + (row + dr) * 8;
                    int targetIdx = jumpC + jumpR * 8;

                    if (board[targetIdx] == EMPTY && board[intermediateIdx] != EMPTY
                            && (board[intermediateIdx] & opposingSide) != 0) {
                        jumps.add(new Move(from, targetIdx, true, intermediateIdx));
                    }
                }
            }
        }
    }

    private int evaluateBoard() {
        int totalEval = 0;
        int[] pieceValues = { 0, 100, 175, 100, 175 };
        // Pawns value=100, Kings value=175
        for (int i = 0; i < 64; i++) {
            if (board[i] == EMPTY)
                continue;
            int piece = board[i] & 7;
            boolean isRed = (board[i] & RED_SIDE) != 0;
            int sign = isRed ? 1 : -1;
            int value = pieceValues[piece];
            int tableIdx = isRed ? i : ((7 - (i / 8)) * 8 + (i % 8));
            if (piece == RED_PAWN || piece == BLACK_PAWN) {
                value += PROGRESSION_TABLE[tableIdx];
            }
            totalEval += value * sign;
        }
        return totalEval;
    }

    private void drawScene(String[] outputBuffer) {
        for (int row = 0; row < 8; row++) {
            int bufferRowOffset = (8 + row) * 80 + 4;
            for (int col = 0; col < 8; col++) {
                int boardIdx = col + row * 8;
                int piece = board[boardIdx];
                String squareBG = ((row + col) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                String pieceFG = ((piece & RED_SIDE) != 0) ? COLOR_RED_PIECE : COLOR_BLACK_PIECE;
                char glyph = ' ';
                if (piece != EMPTY) {
                    glyph = PIECE_SYMBOLS[piece & 7].charAt(0);
                }
                for (int w = 0; w < 3; w++) {
                    outputBuffer[bufferRowOffset + col * 3 + w] = squareBG + pieceFG + (w == 1 ? glyph : ' ') + RESET;
                }
            }
        }
        int textColumnX = 33;
        writeText(outputBuffer, textColumnX, 2, "┌────────────────────────────────────────┐", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 3, "│        CHECKERS SIMULATION CORE        │", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 4, "├────────────────────────────────────────┤", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 5, "│ ", COLOR_TEXT);
        if (gameOver) {
            String gameMsg = String.format("%-38s", "STATUS: " + gameResultText);
            writeText(outputBuffer, textColumnX + 2, 5, gameMsg, COLOR_OVER);
        } else {
            String turnMsg = String.format("%-38s",
                    (isRedTurn ? "  " : " ") + "  ACTIVE MATCH TURN: " + (isRedTurn ? " RED PLAYER" : " BLACK PLAYER"));
            writeText(outputBuffer, textColumnX + 2, 5, turnMsg, COLOR_TURN);
        }
        writeText(outputBuffer, textColumnX + 41, 5, "│", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 6, "├────────────────────────────────────────┤", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 7, "│ RECORDED MATCH LIVE TELEMETRY LOGS:    │", COLOR_TEXT);
        int startingMoveNumber = Math.max(0, totalMovesPlayed - moveHistory.size());
        for (int i = 0; i < 12; i++) {
            String historyLine = "│                                        │";
            if (i < moveHistory.size()) {
                int trueMoveDisplayNum = startingMoveNumber + i + 1;
                historyLine = String.format("│  %3d. %-32s │", trueMoveDisplayNum, moveHistory.get(i));
            }
            writeText(outputBuffer, textColumnX, 8 + i, historyLine, COLOR_TEXT);
        }
        writeText(outputBuffer, textColumnX, 20, "└────────────────────────────────────────┘", COLOR_TEXT);
    }

    private void writeText(String[] outputBuffer, int startX, int row, String content, String colorCode) {
        int offset = row * 80 + startX;
        for (int i = 0; i < content.length(); i++) {
            if (offset + i < outputBuffer.length) {
                outputBuffer[offset + i] = colorCode + content.charAt(i) + RESET;
            }
        }
    }

    private String indexToNotation(int index) {
        int file = index % 8;
        int rank = 8 - (index / 8);
        return "" + (char) ('a' + file) + rank;
    }
}