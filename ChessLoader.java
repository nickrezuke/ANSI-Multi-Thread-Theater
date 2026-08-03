// TODO: Investigate if the game is too deterministic and if the same game plays out each time
// TODO: Test what happens when game is played for very very very long times

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChessLoader extends Loader {
    private static final StatusStage[] CHESS_STAGES = {
            new StatusStage(25, "Structuring spatial 8x8 arrays:"),
            new StatusStage(50, "Calibrating piece heuristic evaluations:"),
            new StatusStage(75, "Constructing alpha-beta search trees:"),
            new StatusStage(100, "Chess Simulation Engine Online!")
    };

    private static final int EMPTY = 0;
    private static final int PAWN = 1, KNIGHT = 2, BISHOP = 3, ROOK = 4, QUEEN = 5, KING = 6;
    private static final int WHITE_PIECE = 8, BLACK_PIECE = 16;

    private final int[] board = new int[64];
    private boolean isWhiteTurn = true;
    private final List<String> moveHistory = new ArrayList<>();
    private int totalMovesPlayed = 0;

    private long lastMoveTime = 0;
    private static final long MOVE_DELAY_MS = 800;
    private boolean gameOver = false;
    private String gameResultText = "";

    private static final String LIGHT_SQUARE = "\u001B[48;5;250m";
    private static final String DARK_SQUARE = "\u001B[48;5;238m";
    private static final String COLOR_WHITE_PIECE = "\u001B[38;5;231m";
    private static final String COLOR_BLACK_PIECE = "\u001B[38;5;232m";
    private static final String COLOR_TEXT = "\u001B[38;5;244m";
    private static final String COLOR_TURN = "\u001B[38;5;82m";
    private static final String COLOR_OVER = "\u001B[38;5;196m";

    private static final String[] PIECE_SYMBOLS = {
            " ", "♙", "♘", "♗", "♖", "♕", "♔", " ",
            " ", "♟", "♞", "♝", "♜", "♛", "♚", " "
    };

    private static final int[] PAWN_TABLE = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] KNIGHT_TABLE = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    private static final int[] BISHOP_TABLE = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };

    private static class Move {
        int from, to, score, guessScore;

        Move(int from, int to) {
            this.from = from;
            this.to = to;
        }

        Move(int score) {
            this.score = score;
        }
    }

    public ChessLoader() {
        super(CHESS_STAGES);
    }

    @Override
    protected void initialize() {
        resetGame();
    }

    private void resetGame() {
        gameOver = false;
        isWhiteTurn = true;
        moveHistory.clear();
        totalMovesPlayed = 0;

        for (int i = 0; i < 64; i++) {
            board[i] = EMPTY;
        }

        int[] backRank = { ROOK, KNIGHT, BISHOP, QUEEN, KING, BISHOP, KNIGHT, ROOK };
        for (int i = 0; i < 8; i++) {
            board[i] = backRank[i] | BLACK_PIECE;
            board[8 + i] = PAWN | BLACK_PIECE;
            board[48 + i] = PAWN | WHITE_PIECE;
            board[56 + i] = backRank[i] | WHITE_PIECE;
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
        Move bestMove = minimax(4, isWhiteTurn, -100000, 100000);

        if (bestMove == null || bestMove.from == bestMove.to) {
            gameOver = true;
            gameResultText = isWhiteTurn ? "BLACK WINS BY MATING!" : "WHITE WINS BY MATING!";
            lastMoveTime = System.currentTimeMillis();
            return;
        }

        String pStr = getPieceName(board[bestMove.from]);
        String notation = pStr + " " + indexToNotation(bestMove.from) + "→" + indexToNotation(bestMove.to);
        if (board[bestMove.to] != EMPTY)
            notation += " ⚔";

        moveHistory.add(notation);
        totalMovesPlayed++;
        if (moveHistory.size() > 12) {
            moveHistory.remove(0);
        }

        board[bestMove.to] = board[bestMove.from];
        board[bestMove.from] = EMPTY;

        if ((board[bestMove.to] & 7) == PAWN) {
            int rank = bestMove.to / 8;
            if (rank == 0 || rank == 7) {
                board[bestMove.to] = QUEEN | (board[bestMove.to] & (WHITE_PIECE | BLACK_PIECE));
            }
        }

        isWhiteTurn = !isWhiteTurn;
    }

    private Move minimax(int depth, boolean isMax, int alpha, int beta) {
        List<Move> legalMoves = generateAllLegalMoves(isMax ? WHITE_PIECE : BLACK_PIECE);
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
            board[move.to] = board[move.from];
            board[move.from] = EMPTY;

            int score = minimax(depth - 1, !isMax, alpha, beta).score;

            board[move.from] = board[move.to];
            board[move.to] = savedPiece;

            if (isMax) {
                if (score > bestMove.score) {
                    bestMove.score = score;
                    bestMove.from = move.from;
                    bestMove.to = move.to;
                }
                alpha = Math.max(alpha, score);
            } else {
                if (score < bestMove.score) {
                    bestMove.score = score;
                    bestMove.from = move.from;
                    bestMove.to = move.to;
                }
                beta = Math.min(beta, score);
            }
            if (beta <= alpha)
                break;
        }
        return bestMove;
    }

    private void scoreMoves(List<Move> moves) {
        int[] pieceValues = { 0, 100, 320, 330, 500, 900, 20000 };
        for (Move m : moves) {
            int attacker = board[m.from] & 7;
            int target = board[m.to] & 7;
            if (target != EMPTY) {
                m.guessScore = 1000 * pieceValues[target] - pieceValues[attacker];
            } else {
                m.guessScore = 0;
            }
        }
    }

    private List<Move> generateAllLegalMoves(int side) {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (board[i] != EMPTY && (board[i] & side) != 0) {
                generatePieceMoves(i, moves);
            }
        }
        return moves;
    }

    private void generatePieceMoves(int from, List<Move> moves) {
        int piece = board[from] & 7;
        int side = board[from] & (WHITE_PIECE | BLACK_PIECE);
        int opposingSide = (side == WHITE_PIECE) ? BLACK_PIECE : WHITE_PIECE;

        int row = from / 8, col = from % 8;

        switch (piece) {
            case PAWN:
                int dir = (side == WHITE_PIECE) ? -1 : 1;
                int nextRow = row + dir;
                if (nextRow >= 0 && nextRow < 8) {
                    int straight = col + nextRow * 8;
                    if (board[straight] == EMPTY)
                        moves.add(new Move(from, straight));

                    if (col > 0 && board[(col - 1) + nextRow * 8] != EMPTY
                            && (board[(col - 1) + nextRow * 8] & opposingSide) != 0)
                        moves.add(new Move(from, (col - 1) + nextRow * 8));
                    if (col < 7 && board[(col + 1) + nextRow * 8] != EMPTY
                            && (board[(col + 1) + nextRow * 8] & opposingSide) != 0)
                        moves.add(new Move(from, (col + 1) + nextRow * 8));
                }
                break;
            case KNIGHT:
                int[][] kMoves = { { -2, -1 }, { -2, 1 }, { -1, -2 }, { -1, 2 }, { 1, -2 }, { 1, 2 }, { 2, -1 },
                        { 2, 1 } };
                for (int[] m : kMoves) {
                    // FIX: Correctly index into individual spatial offset arrays [0] and [1]
                    int r = row + m[0], c = col + m[1];
                    if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                        int to = c + r * 8;
                        if (board[to] == EMPTY || (board[to] & opposingSide) != 0)
                            moves.add(new Move(from, to));
                    }
                }
                break;
            case BISHOP:
            case ROOK:
            case QUEEN:
                int[][] slidingDirs = (piece == ROOK) ? new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } }
                        : (piece == BISHOP) ? new int[][] { { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 } }
                                : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }, { -1, -1 }, { -1, 1 },
                                        { 1, -1 }, { 1, 1 } };
                for (int[] d : slidingDirs) {
                    int r = row, c = col;
                    while (true) {
                        // FIX: Correctly index into step directional vectors [0] and [1]
                        r += d[0];
                        c += d[1];
                        if (r < 0 || r >= 8 || c < 0 || c >= 8)
                            break;
                        int to = c + r * 8;
                        if (board[to] == EMPTY) {
                            moves.add(new Move(from, to));
                        } else {
                            if ((board[to] & opposingSide) != 0)
                                moves.add(new Move(from, to));
                            break;
                        }
                    }
                }
                break;
            case KING:
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0)
                            continue;
                        int r = row + dr, c = col + dc;
                        if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                            int to = c + r * 8;
                            if (board[to] == EMPTY || (board[to] & opposingSide) != 0)
                                moves.add(new Move(from, to));
                        }
                    }
                }
                break;
        }
    }

    private int evaluateBoard() {
        int totalEval = 0;
        int[] pieceValues = { 0, 100, 320, 330, 500, 900, 20000 };
        for (int i = 0; i < 64; i++) {
            if (board[i] == EMPTY)
                continue;
            int piece = board[i] & 7;
            boolean isWhite = (board[i] & WHITE_PIECE) != 0;
            int sign = isWhite ? 1 : -1;
            int value = pieceValues[piece];
            int tableIdx = isWhite ? i : ((7 - (i / 8)) * 8 + (i % 8));
            // FIX: Correctly resolve conditions explicitly per piece types instead of using
            // compound assignments
            if (piece == PAWN)
                value += PAWN_TABLE[tableIdx];
            else if (piece == KNIGHT)
                value += KNIGHT_TABLE[tableIdx];
            else if (piece == BISHOP)
                value += BISHOP_TABLE[tableIdx];
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
                String pieceFG = ((piece & WHITE_PIECE) != 0) ? COLOR_WHITE_PIECE : COLOR_BLACK_PIECE;
                char glyph = ' ';
                if (piece != EMPTY) {
                    int symIdx = (piece & 7) + (((piece & BLACK_PIECE) != 0) ? 8 : 0);
                    glyph = PIECE_SYMBOLS[symIdx].charAt(0);
                }
                for (int w = 0; w < 3; w++) {
                    outputBuffer[bufferRowOffset + col * 3 + w] = squareBG + pieceFG + (w == 1 ? glyph : ' ') + RESET;
                }
            }
        }
        int textColumnX = 33;
        writeText(outputBuffer, textColumnX, 2, "┌────────────────────────────────────────┐", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 3, "│         CHESS SIMULATION CORE          │", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 4, "├────────────────────────────────────────┤", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 5, "│ ", COLOR_TEXT);
        if (gameOver) {
            String gameMsg = String.format("%-38s", "STATUS: " + gameResultText);
            writeText(outputBuffer, textColumnX + 2, 5, gameMsg, COLOR_OVER);
        } else {
            String turnMsg = String.format("%-38s",
                    "   ACTIVE MATCH TURN:  " + (isWhiteTurn ? "WHITE PLAYER" : "BLACK PLAYER"));
            writeText(outputBuffer, textColumnX + 2, 5, turnMsg, COLOR_TURN);
        }
        writeText(outputBuffer, textColumnX + 41, 5, "│", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 6, "├────────────────────────────────────────┤", COLOR_TEXT);
        writeText(outputBuffer, textColumnX, 7, "│ RECORDED MATCH LIVE TELEMETRY LOGS:    │", COLOR_TEXT);

        // Calculate what the true starting move number is for our current window slice
        int startingMoveNumber = Math.max(0, totalMovesPlayed - moveHistory.size());

        for (int i = 0; i < 12; i++) {
            String historyLine = "│                                        │";
            if (i < moveHistory.size()) {
                // Calculate the changing dynamic move number by adding index offsets to the
                // starting baseline
                int trueMoveDisplayNum = startingMoveNumber + i + 1;
                historyLine = String.format("│  %2d. %-32s  │", trueMoveDisplayNum, moveHistory.get(i));
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

    private String getPieceName(int val) {
        switch (val & 7) {
            case PAWN:
                return "P";
            case KNIGHT:
                return "N";
            case BISHOP:
                return "B";
            case ROOK:
                return "R";
            case QUEEN:
                return "Q";
            case KING:
                return "K";
            default:
                return " ";
        }
    }

    private String indexToNotation(int index) {
        int file = index % 8;
        int rank = 8 - (index / 8);
        return "" + (char) ('a' + file) + rank;
    }
}