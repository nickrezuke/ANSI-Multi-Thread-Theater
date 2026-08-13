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

    // Track if kings and rooks have moved to determine castling rights
    private boolean whiteKingMoved = false;
    private boolean whiteLeftRookMoved = false;
    private boolean whiteRightRookMoved = false;
    private boolean blackKingMoved = false;
    private boolean blackLeftRookMoved = false;
    private boolean blackRightRookMoved = false;

    private int enPassantTargetSquare = -1; // -1 means no en passant is available

    private final int[] board = new int[64];
    private boolean isWhiteTurn = true;
    private final List<String> moveHistory = new ArrayList<>();
    private int totalMovesPlayed = 0;

    private long lastMoveTime = 0;
    private static final long MOVE_DELAY_MS = 800;
    private boolean gameOver = false;
    private String gameResultText = "";

    // Draw detection state (for checking threefold repetition / 50-move rule)
    private final java.util.Map<String, Integer> positionHistory = new java.util.HashMap<>();
    private int halfmoveClock = 0;

    private static final int REPETITION_PENALTY = 45;

    private static final String LIGHT_SQUARE = "\u001B[48;5;250m";
    private static final String DARK_SQUARE = "\u001B[48;5;238m";
    private static final String COLOR_WHITE_PIECE = "\u001B[38;5;231m";
    private static final String COLOR_BLACK_PIECE = "\u001B[38;5;232m";
    private static final String COLOR_TEXT = "\u001B[38;5;244m";
    private static final String COLOR_TURN = "\u001B[38;5;82m";
    private static final String COLOR_OVER = "\u001B[38;5;196m";
    private static final String COLOR_DRAW = "\u001B[38;5;220m";

    private static final String[] PIECE_SYMBOLS = {
            " ", "\u265F", "\u265E", "\u265D", "\u265C", "\u265B", "\u265A", " ",
            " ", "\u265F", "\u265E", "\u265D", "\u265C", "\u265B", "\u265A", " "
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

    private static final int[] ROOK_TABLE = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            0, 0, 0, 5, 5, 0, 0, 0
    };

    private static final int[] QUEEN_TABLE = {
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20
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
        // This uses 80x22 specifically
        super(CHESS_STAGES, 80, 22);
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

        enPassantTargetSquare = -1;

        blackKingMoved = false;
        blackLeftRookMoved = false;
        blackRightRookMoved = false;
        whiteKingMoved = false;
        whiteLeftRookMoved = false;
        whiteRightRookMoved = false;

        positionHistory.clear();
        halfmoveClock = 0;
        positionHistory.put(getPositionKey(isWhiteTurn), 1);

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
        // Open a couple of different ways
        Move bestMove = getOpeningBookMove();

        // Otherwise, think using Minimax
        if (bestMove == null) {
            bestMove = minimax(4, isWhiteTurn, -100000, 100000);
        }

        // The game has ended
        if (bestMove == null || bestMove.from == bestMove.to) {
            gameOver = true;
            gameResultText = isWhiteTurn ? "BLACK WINS BY MATING!" : "WHITE WINS BY MATING!";
            lastMoveTime = System.currentTimeMillis();
            return;
        }

        String pStr = getPieceName(board[bestMove.from]);
        String notation = pStr + " " + indexToNotation(bestMove.from) + "→" + indexToNotation(bestMove.to);
        boolean wasCapture = board[bestMove.to] != EMPTY;
        if (wasCapture)
            notation += " ⚔";

        moveHistory.add(notation);
        totalMovesPlayed++;
        if (moveHistory.size() > 12) {
            moveHistory.remove(0);
        }

        // Keep track of the moving piece type
        int movedPieceType = board[bestMove.from] & 7;

        // Execute the board move
        board[bestMove.to] = board[bestMove.from];
        board[bestMove.from] = EMPTY;

        // En Passant state tracking
        int previousEnPassantTarget = enPassantTargetSquare; // Save for capture handling
        enPassantTargetSquare = -1; // Reset by default every turn

        if (movedPieceType == PAWN) {
            int fromRow = bestMove.from / 8;
            int toRow = bestMove.to / 8;

            // Check if it was a 2-square jump
            if (Math.abs(fromRow - toRow) == 2) {
                // The target square is exactly halfway between from and to
                enPassantTargetSquare = (bestMove.from + bestMove.to) / 2;
            }

            // Check if this move was an actual En Passant capture execution
            if (bestMove.to == previousEnPassantTarget) {
                // If a pawn moved diagonally to the target square, 
                // delete the victim pawn behind it
                int victimSquare = isWhiteTurn ? (bestMove.to + 8) : (bestMove.to - 8);
                board[victimSquare] = EMPTY;
            }

            // Handle normal promotion
            if (toRow == 0 || toRow == 7) {
                board[bestMove.to] = QUEEN | (board[bestMove.to] & (WHITE_PIECE | BLACK_PIECE));
            }
        }

        // Castling Logic
        if (movedPieceType == KING) {
            // If the king moved 2 squares, it's a castle! Jump the associated rook.
            if (bestMove.from == 60 && bestMove.to == 62) {
                board[61] = board[63];
                board[63] = EMPTY;
            } // White Kingside
            else if (bestMove.from == 60 && bestMove.to == 58) {
                board[59] = board[56];
                board[56] = EMPTY;
            } // White Queenside
            else if (bestMove.from == 4 && bestMove.to == 6) {
                board[5] = board[7];
                board[7] = EMPTY;
            } // Black Kingside
            else if (bestMove.from == 4 && bestMove.to == 2) {
                board[3] = board[0];
                board[0] = EMPTY;
            } // Black Queenside

            if (isWhiteTurn)
                whiteKingMoved = true;
            else
                blackKingMoved = true;
        }

        // Clear rights if rooks move or are captured
        if (bestMove.from == 56 || bestMove.to == 56)
            whiteLeftRookMoved = true;
        if (bestMove.from == 63 || bestMove.to == 63)
            whiteRightRookMoved = true;
        if (bestMove.from == 0 || bestMove.to == 0)
            blackLeftRookMoved = true;
        if (bestMove.from == 7 || bestMove.to == 7)
            blackRightRookMoved = true;

        // 50 Move Rule tracking
        // A pawn move or a capture (including en passant) resets the "no progress" clock.
        if (movedPieceType == PAWN || wasCapture) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        isWhiteTurn = !isWhiteTurn;

        // Threefold Repetition & 50 Move Draw Detection
        String positionKey = getPositionKey(isWhiteTurn);
        int occurrences = positionHistory.merge(positionKey, 1, Integer::sum);

        if (occurrences >= 3) {
            gameOver = true;
            gameResultText = "DRAW BY THREEFOLD REPETITION!";
            lastMoveTime = System.currentTimeMillis();
        } else if (halfmoveClock >= 100) {
            gameOver = true;
            gameResultText = "DRAW BY 50-MOVE RULE!";
            lastMoveTime = System.currentTimeMillis();
        }
    }

    /**
     * Builds a compact string key that uniquely identifies the current board
     * state for repetition purposes: piece placement, side to move, remaining
     * castling rights, and the en passant square. Two positions with the same
     * key are the same position for repetition purposes.
     *
     * Takes whiteToMoveNext explicitly (rather than reading the isWhiteTurn
     * field) so it can also be called safely from inside minimax(), which
     * simulates moves on the real board array without ever touching
     * isWhiteTurn itself.
     */
    private String getPositionKey(boolean whiteToMoveNext) {
        StringBuilder key = new StringBuilder(64 * 3);
        for (int i = 0; i < 64; i++) {
            key.append(board[i]).append(',');
        }
        key.append(whiteToMoveNext ? 'w' : 'b');
        key.append(whiteKingMoved ? '1' : '0');
        key.append(whiteLeftRookMoved ? '1' : '0');
        key.append(whiteRightRookMoved ? '1' : '0');
        key.append(blackKingMoved ? '1' : '0');
        key.append(blackLeftRookMoved ? '1' : '0');
        key.append(blackRightRookMoved ? '1' : '0');
        key.append('#').append(enPassantTargetSquare);
        return key.toString();
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
            int savedEnPassantTarget = enPassantTargetSquare; // Save historical target state

            // Handle simulated En Passant capture removal
            int epVictomSquare = -1;
            int movingPiece = board[move.from] & 7;
            if (movingPiece == PAWN && move.to == enPassantTargetSquare) {
                epVictomSquare = isMax ? (move.to + 8) : (move.to - 8);
            }
            int savedVictimPiece = (epVictomSquare != -1) ? board[epVictomSquare] : EMPTY;

            // Handle Castling
            boolean oldWKM = whiteKingMoved, oldWLRM = whiteLeftRookMoved, oldWRRM = whiteRightRookMoved;
            boolean oldBKM = blackKingMoved, oldBLRM = blackLeftRookMoved, oldBRRM = blackRightRookMoved;
            int rankOffset = (isMax) ? 56 : 0;

            // Handle simulated Rook warping during castling
            boolean isSimulatedCastle = false;
            int castledRookFrom = -1, castledRookTo = -1;
            if (movingPiece == KING && Math.abs(move.from - move.to) == 2) {
                isSimulatedCastle = true;
                castledRookFrom = (move.to > move.from) ? (rankOffset + 7) : rankOffset;
                castledRookTo = (move.to > move.from) ? (rankOffset + 5) : (rankOffset + 3);
            }

            // Execute simulated move
            board[move.to] = board[move.from];
            board[move.from] = EMPTY;

            if (isSimulatedCastle) {
                board[castledRookTo] = board[castledRookFrom];
                board[castledRookFrom] = EMPTY;
            }

            // Temporarily flag movement variables for deeper search branch evaluation
            if (movingPiece == KING) {
                if (isMax)
                    whiteKingMoved = true;
                else
                    blackKingMoved = true;
            }
            if (move.from == 56 || move.to == 56)
                whiteLeftRookMoved = true;
            if (move.from == 63 || move.to == 63)
                whiteRightRookMoved = true;
            if (move.from == 0 || move.to == 0)
                blackLeftRookMoved = true;
            if (move.from == 7 || move.to == 7)
                blackRightRookMoved = true;

            if (epVictomSquare != -1)
                board[epVictomSquare] = EMPTY;

            // Simulate updating the tracker for the deeper branches
            if (movingPiece == PAWN && Math.abs((move.from / 8) - (move.to / 8)) == 2) {
                enPassantTargetSquare = (move.from + move.to) / 2;
            } else {
                enPassantTargetSquare = -1;
            }

            // Call deeper search layer
            int score = minimax(depth - 1, !isMax, alpha, beta).score;

            // ---- NEW: Repetition-aware scoring ----
            // Check whether the position THIS move just created already occurred
            // earlier in the real game. Board state, castling rights, and en
            // passant square at this point in the loop all still reflect the
            // post-move position (the undo below hasn't run yet), so the key
            // built here is exactly the position this candidate move leads to.
            // Because the actual current position is itself already recorded in
            // positionHistory, this also catches the classic two-move shuffle:
            // a 4-ply line that walks the board right back to where it started
            // shows up as a match at the bottom of this search, letting the
            // engine route around it instead of only noticing after the fact.
            String simulatedKey = getPositionKey(!isMax);
            int priorOccurrences = positionHistory.getOrDefault(simulatedKey, 0);
            if (priorOccurrences > 0) {
                int penalty = REPETITION_PENALTY * priorOccurrences;
                // isMax's own move is what led here: make it look worse to the
                // side that just moved, not the side about to reply.
                score += isMax ? -penalty : penalty;
            }

            board[move.from] = board[move.to];
            board[move.to] = savedPiece;
            if (isSimulatedCastle) {
                board[castledRookFrom] = board[castledRookTo];
                board[castledRookTo] = EMPTY;
            }
            whiteKingMoved = oldWKM;
            whiteLeftRookMoved = oldWLRM;
            whiteRightRookMoved = oldWRRM;
            blackKingMoved = oldBKM;
            blackLeftRookMoved = oldBLRM;
            blackRightRookMoved = oldBRRM;

            if (epVictomSquare != -1)
                board[epVictomSquare] = savedVictimPiece;
            enPassantTargetSquare = savedEnPassantTarget; // Restore old tracker state

            if (isMax) {
                if (score > bestMove.score) {
                    bestMove.score = score;
                    bestMove.from = move.from;
                    bestMove.to = move.to;
                } else if (score == bestMove.score) {
                    // 50% chance to swap to a different move of equal strength
                    if ((int) (Math.random() * 2) % 2 == 0) {
                        bestMove.from = move.from;
                        bestMove.to = move.to;
                    }
                }
                alpha = Math.max(alpha, score);
            } else {
                if (score < bestMove.score) {
                    bestMove.score = score;
                    bestMove.from = move.from;
                    bestMove.to = move.to;
                } else if (score == bestMove.score) {
                    // 50% chance to swap to a different move of equal strength
                    if ((int) (Math.random() * 2) % 2 == 0) {
                        bestMove.from = move.from;
                        bestMove.to = move.to;
                    }
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
        List<Move> pseudoMoves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (board[i] != EMPTY && (board[i] & side) != 0) {
                generatePieceMoves(i, pseudoMoves, true);
            }
        }
        return filterLegalMoves(pseudoMoves, side);
    }

    private void generatePieceMoves(int from, List<Move> moves, boolean validateCastling) {
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

                    // 1-Square Forward Move
                    if (board[straight] == EMPTY) {
                        moves.add(new Move(from, straight));

                        // ---- NEW: Allow 2-Square Jump from Starting Ranks ----
                        int startRank = (side == WHITE_PIECE) ? 6 : 1;
                        int doubleStraight = col + (row + (dir * 2)) * 8;
                        if (row == startRank && board[doubleStraight] == EMPTY) {
                            moves.add(new Move(from, doubleStraight));
                        }
                    }

                    // Normal Left Capture & En Passant Left Capture
                    if (col > 0) {
                        int leftCapture = (col - 1) + nextRow * 8;
                        if ((board[leftCapture] != EMPTY && (board[leftCapture] & opposingSide) != 0)
                                || (leftCapture == enPassantTargetSquare)) {
                            moves.add(new Move(from, leftCapture));
                        }
                    }

                    // Normal Right Capture & En Passant Right Capture
                    if (col < 7) {
                        int rightCapture = (col + 1) + nextRow * 8;
                        if ((board[rightCapture] != EMPTY && (board[rightCapture] & opposingSide) != 0)
                                || (rightCapture == enPassantTargetSquare)) {
                            moves.add(new Move(from, rightCapture));
                        }
                    }
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
                if(validateCastling) {
                boolean kingMoved = (side == WHITE_PIECE) ? whiteKingMoved : blackKingMoved;
                if (!kingMoved && !isKingInCheck(side)) {
                    int enemySide = (side == WHITE_PIECE) ? BLACK_PIECE : WHITE_PIECE;
                    int rankOffset = (side == WHITE_PIECE) ? 56 : 0; // Row 7 for white, Row 0 for black

                    // Kingside (Right) Castling
                    boolean rightRookMoved = (side == WHITE_PIECE) ? whiteRightRookMoved : blackRightRookMoved;
                    if (!rightRookMoved && board[rankOffset + 5] == EMPTY && board[rankOffset + 6] == EMPTY) {
                        if (!isSquareAttacked(rankOffset + 5, enemySide)
                                && !isSquareAttacked(rankOffset + 6, enemySide)) {
                            moves.add(new Move(from, rankOffset + 6)); // King moves 2 squares right
                        }
                    }

                    // Queenside (Left) Castling
                    boolean leftRookMoved = (side == WHITE_PIECE) ? whiteLeftRookMoved : blackLeftRookMoved;
                    if (!leftRookMoved && board[rankOffset + 1] == EMPTY && board[rankOffset + 2] == EMPTY
                            && board[rankOffset + 3] == EMPTY) {
                        if (!isSquareAttacked(rankOffset + 3, enemySide)
                                && !isSquareAttacked(rankOffset + 2, enemySide)) {
                            moves.add(new Move(from, rankOffset + 2)); // King moves 2 squares left
                        }
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
            else if (piece == ROOK)
                value += ROOK_TABLE[tableIdx];
            else if (piece == QUEEN)
                value += QUEEN_TABLE[tableIdx];
            totalEval += value * sign;
        }
        return totalEval;
    }

    private boolean isKingInCheck(int side) {
        int kingIdx = -1;
        for (int i = 0; i < 64; i++) {
            if (board[i] != EMPTY && (board[i] & 7) == KING && (board[i] & side) != 0) {
                kingIdx = i;
                break;
            }
        }
        if (kingIdx == -1)
            return true; // King was captured (shouldn't happen in legal games)

        int opposingSide = (side == WHITE_PIECE) ? BLACK_PIECE : WHITE_PIECE;

        // Generate all immediate enemy attacks
        List<Move> enemyMoves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (board[i] != EMPTY && (board[i] & opposingSide) != 0) {
                generatePieceMoves(i, enemyMoves, false);
            }
        }

        // See if any enemy move hits our King's square
        for (Move m : enemyMoves) {
            if (m.to == kingIdx)
                return true;
        }
        return false;
    }

    private List<Move> filterLegalMoves(List<Move> pseudoLegalMoves, int side) {
        List<Move> legalMoves = new ArrayList<>();
        for (Move move : pseudoLegalMoves) {
            // Simulating the move
            int savedPiece = board[move.to];
            board[move.to] = board[move.from];
            board[move.from] = EMPTY;

            // If our king is safe, the move is legal
            if (!isKingInCheck(side)) {
                legalMoves.add(move);
            }

            // Undoing the move
            board[move.from] = board[move.to];
            board[move.to] = savedPiece;
        }
        return legalMoves;
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
            String statusColor = gameResultText.startsWith("DRAW") ? COLOR_DRAW : COLOR_OVER;
            writeText(outputBuffer, textColumnX + 2, 5, gameMsg, statusColor);
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

    private Move getOpeningBookMove() {
        java.util.Random rand = new java.util.Random();

        // --- TURN 1: WHITE ---
        if (totalMovesPlayed == 0) {
            int choice = rand.nextInt(3);
            if (choice == 0)
                return new Move(52, 36); // 1. e4 (King's Pawn Opening)
            if (choice == 1)
                return new Move(51, 35); // 1. d4 (Queen's Pawn Opening)
            return new Move(62, 45); // 1. Nf3 (Réti Opening)
        }

        // --- TURN 1: BLACK ---
        if (totalMovesPlayed == 1) {
            // If White played 1. e4
            if (board[36] == (PAWN | WHITE_PIECE) && board[52] == EMPTY) {
                int choice = rand.nextInt(3);
                if (choice == 0)
                    return new Move(12, 28); // 1... e5 (Open Game)
                if (choice == 1)
                    return new Move(10, 26); // 1... c5 (Sicilian Defense)
                return new Move(11, 27); // 1... d5 (Scandinavian Defense)
            }
            // If White played 1. d4
            if (board[35] == (PAWN | WHITE_PIECE) && board[51] == EMPTY) {
                int choice = rand.nextInt(2);
                if (choice == 0)
                    return new Move(11, 27); // 1... d5 (Closed Game)
                return new Move(6, 21); // 1... Nf6 (Indian Defense)
            }
        }

        // --- TURN 2: WHITE (After 1.e4 e5) ---
        if (totalMovesPlayed == 2 && board[36] == (PAWN | WHITE_PIECE) && board[28] == (PAWN | BLACK_PIECE)) {
            int choice = rand.nextInt(2);
            if (choice == 0)
                return new Move(62, 45); // 2. Nf3 (Main Line)
            return new Move(50, 34); // 2. d4 (Center Game)
        }

        // --- TURN 2: BLACK (After 1.e4 e5 2.Nf3) ---
        if (totalMovesPlayed == 3 && board[45] == (KNIGHT | WHITE_PIECE) && board[28] == (PAWN | BLACK_PIECE)) {
            int choice = rand.nextInt(2);
            if (choice == 0)
                return new Move(1, 18); // 2... Nc6 (Defending e5)
            return new Move(6, 21); // 2... Nf6 (Petrov's Defense)
        }

        return null; // Return null if out of book or position unrecognized
    }

    private boolean isSquareAttacked(int squareIndex, int enemySide) {
        List<Move> enemyMoves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (board[i] != EMPTY && (board[i] & enemySide) != 0) {
                // Use standard piece moves to check structural reach
                generatePieceMoves(i, enemyMoves, false);
            }
        }
        for (Move m : enemyMoves) {
            if (m.to == squareIndex)
                return true;
        }
        return false;
    }
}