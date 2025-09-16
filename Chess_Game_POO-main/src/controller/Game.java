package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import model.board.Board;
import model.board.Move;
import model.board.Position;
import model.pieces.*;

public class Game {

    private final Stack<Move> historyStack = new Stack<>();
    private Board board;
    private boolean whiteToMove = true;
    private boolean gameOver = false;

    private Position enPassantTarget = null;
    private final List<String> history = new ArrayList<>();

    public Game() {
        this.board = new Board();
        setupPieces();
    }

    public void undoLastMove() {
        if (!historyStack.isEmpty()) {
            Move lastMove = historyStack.pop();
            board.put(lastMove.getTo(), lastMove.captured);
            board.put(lastMove.from, lastMove.moving);
        }
    }

    private Game(boolean empty) { /* intentionally empty */ }

    public Board board() { return board; }
    public boolean whiteToMove() { return whiteToMove; }
    public boolean isGameOver() { return gameOver; }
    public List<String> history() { return Collections.unmodifiableList(history); }

    public void newGame() {
        this.board = new Board();
        this.whiteToMove = true;
        this.gameOver = false;
        this.enPassantTarget = null;
        this.history.clear();
        setupPieces();
    }

    // New method: a public gateway for move legality checks.
    public boolean isLegal(Position from, Position to) {
        Piece p = board.get(from);
        if (p == null || p.isWhite() != whiteToMove) {
            return false;
        }
        return legalMovesFrom(from).contains(to);
    }

    public List<Position> legalMovesFrom(Position from) {
        return legalMovesFromWithSpecials(from);
    }

    public boolean isPromotion(Position from, Position to) {
        Piece p = board.get(from);
        if (!(p instanceof Pawn)) return false;
        return p.isWhite() ? to.getRow() == 0 : to.getRow() == 7;
    }

    public void move(Position from, Position to, Character promotion) {
        if (gameOver) return;

        Piece p = board.get(from);
        if (p == null || p.isWhite() != whiteToMove) return;

        List<Position> legal = legalMovesFromWithSpecials(from);
        if (!legal.contains(to)) return;

        boolean isKing = p instanceof King;
        boolean isPawn = p instanceof Pawn;
        int dCol = Math.abs(to.getColumn() - from.getColumn());

        Piece capturedBefore = board.get(to);
        boolean targetIsKing = (capturedBefore instanceof King);

        if (isKing && dCol == 2) {
            int row = from.getRow();
            historyStack.push(new Move(from, to, p, capturedBefore, false, false, false, promotion));
            board.set(to, p);
            board.set(from, null);
            p.setMoved(true);

            String san;
            if (to.getColumn() == 6) {
                Piece rook = board.get(new Position(row, 7));
                board.set(new Position(row, 5), rook);
                board.set(new Position(row, 7), null);
                if (rook != null) rook.setMoved(true);
                san = "O-O";
            } else {
                Piece rook = board.get(new Position(row, 0));
                board.set(new Position(row, 3), rook);
                board.set(new Position(row, 0), null);
                if (rook != null) rook.setMoved(true);
                san = "O-O-O";
            }

            enPassantTarget = null;
            whiteToMove = !whiteToMove;

            if (isCheckmate(whiteToMove)) {
                san += "#";
                gameOver = true;
            } else if (inCheck(whiteToMove)) {
                san += "+";
            }
            addHistory(san);

            if (!gameOver) checkGameEnd();
            return;
        }

        boolean diagonal = from.getColumn() != to.getColumn();
        boolean toIsEmpty = board.get(to) == null;
        boolean isEnPassant = isPawn && diagonal && toIsEmpty && to.equals(enPassantTarget);

        String moveStr;

        if (isEnPassant) {
            board.set(to, p);
            board.set(from, null);
            int dir = p.isWhite() ? 1 : -1;
            Position victim = new Position(to.getRow() + dir, to.getColumn());
            board.set(victim, null);
            p.setMoved(true);
            moveStr = coord(from) + "x" + coord(to) + " e.p.";
            enPassantTarget = null;
            
            whiteToMove = !whiteToMove;

            if (isCheckmate(whiteToMove)) {
                moveStr += "#";
                gameOver = true;
            } else if (inCheck(whiteToMove)) {
                moveStr += "+";
            }
            addHistory(moveStr);

            if (!gameOver) checkGameEnd();
            return;
        }

        if (isPawn && isPromotion(from, to)) {
            char ch = (promotion == null) ? 'Q' : Character.toUpperCase(promotion);
            Piece np = switch (ch) {
                case 'R' -> new Rook(board, p.isWhite());
                case 'B' -> new Bishop(board, p.isWhite());
                case 'N' -> new Knight(board, p.isWhite());
                default  -> new Queen(board, p.isWhite());
            };
            np.setMoved(true);
            board.set(from, null);
            board.set(to, np);

            if (targetIsKing) {
                String san = coord(from) + "x" + coord(to) + "=" + np.getSymbol() + "#";
                addHistory(san);
                gameOver = true;
                return;
            }

            moveStr = coord(from) + (capturedBefore != null ? "x" : "-") + coord(to) + "=" + np.getSymbol();
        } else {
            board.set(to, p);
            board.set(from, null);
            p.setMoved(true);

            if (targetIsKing) {
                String san = coord(from) + "x" + coord(to) + "#";
                addHistory(san);
                gameOver = true;
                return;
            }

            moveStr = coord(from) + (capturedBefore != null ? "x" : "-") + coord(to);
        }

        if (isPawn && Math.abs(to.getRow() - from.getRow()) == 2) {
            int mid = (to.getRow() + from.getRow()) / 2;
            enPassantTarget = new Position(mid, from.getColumn());
        } else {
            enPassantTarget = null;
        }

        whiteToMove = !whiteToMove;

        if (isCheckmate(whiteToMove)) {
            moveStr += "#";
            gameOver = true;
        } else if (inCheck(whiteToMove)) {
            moveStr += "+";
        }

        addHistory(moveStr);
        if (!gameOver) checkGameEnd();
    }

    public boolean inCheck(boolean whiteSide) {
        Position k = findKing(whiteSide);
        if (k == null) return true;
        return isSquareAttacked(k, whiteSide);
    }

    public boolean isCheckmate(boolean whiteSide) {
        if (!inCheck(whiteSide)) return false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position from = new Position(row, col);
                Piece piece = board.get(from);
                if (piece != null && piece.isWhite() == whiteSide) {
                    for (Position to : legalMovesFromWithSpecials(from)) {
                        Game g = snapshotShallow();
                        g.forceMoveNoChecks(from, to);
                        if (!g.inCheck(whiteSide)) return false;
                    }
                }
            }
        }
        return true;
    }

    private void checkGameEnd() {
        if (isCheckmate(whiteToMove)) {
            gameOver = true;
            addHistory("Checkmate: " + (whiteToMove ? "White" : "Black") + " loses");
            return;
        }

        if (!inCheck(whiteToMove)) {
            boolean hasAny = false;
            for (int r = 0; r < 8 && !hasAny; r++) {
                for (int c = 0; c < 8 && !hasAny; c++) {
                    Position from = new Position(r, c);
                    Piece piece = board.get(from);
                    if (piece != null && piece.isWhite() == whiteToMove) {
                        if (!legalMovesFromWithSpecials(from).isEmpty()) {
                            hasAny = true;
                        }
                    }
                }
            }
            if (!hasAny) {
                gameOver = true;
                addHistory("Draw: stalemate");
            }
        }
    }

    private List<Position> legalMovesFromWithSpecials(Position from) {
        Piece p = board.get(from);
        if (p == null || p.isWhite() != whiteToMove) return List.of();

        List<Position> moves = new ArrayList<>(p.getPossibleMoves());

        if (p instanceof Pawn && enPassantTarget != null) {
            int dir = p.isWhite() ? -1 : 1;
            if (from.getRow() + dir == enPassantTarget.getRow()
                    && Math.abs(from.getColumn() - enPassantTarget.getColumn()) == 1) {
                Piece victim = board.get(new Position(enPassantTarget.getRow() - dir, enPassantTarget.getColumn()));
                if (victim instanceof Pawn && victim.isWhite() != p.isWhite()) {
                    moves.add(enPassantTarget);
                }
            }
        }

        if (p instanceof King && !p.hasMoved() && !inCheck(p.isWhite())) {
            int row = from.getRow();
            if (canCastle(row, 4, 7, 5, 6, p.isWhite())) moves.add(new Position(row, 6));
            if (canCastle(row, 4, 0, 3, 2, p.isWhite())) moves.add(new Position(row, 2));
        }

        moves.removeIf(to -> {
            Piece tgt = board.get(to);
            return (tgt instanceof King) && (tgt.isWhite() != p.isWhite());
        });

        moves.removeIf(to -> leavesKingInCheck(from, to));
        return moves;
    }

    private boolean canCastle(int row, int kingCol, int rookCol, int passCol1, int passCol2, boolean whiteSide) {
        Piece rook = board.get(new Position(row, rookCol));
        if (!(rook instanceof Rook) || rook.hasMoved()) return false;

        int step = (rookCol > kingCol) ? 1 : -1;
        for (int c = kingCol + step; c != rookCol; c += step) {
            if (board.get(new Position(row, c)) != null) return false;
        }

        Position p1 = new Position(row, passCol1);
        Position p2 = new Position(row, passCol2);
        if (isSquareAttacked(p1, whiteSide) || isSquareAttacked(p2, whiteSide)) return false;

        return true;
    }

    private boolean leavesKingInCheck(Position from, Position to) {
        Piece mover = board.get(from);
        if (mover == null) return true;

        Game g = snapshotShallow();
        g.forceMoveNoChecks(from, to);
        return g.inCheck(mover.isWhite());
    }

    // ========================= src/controller/Game.java =========================
// Mantenha o restante do código da classe Game intacto
// e substitua apenas o método isSquareAttacked() por este:

/**
 * Verifica se a casa especificada está sob ataque de uma peça do lado oposto.
 * @param sq A posição a ser verificada.
 * @param sideToProtect A cor da peça que estamos protegendo (true para branco, false para preto).
 * @return true se a casa estiver sob ataque, false caso contrário.
 */
public boolean isSquareAttacked(Position sq, boolean sideToProtect) {
    int r = sq.getRow();
    int c = sq.getColumn();

    // Verificação de ataque por PEÕES (melhorada)
    int pawnDir = sideToProtect ? -1 : 1;
    Position leftAttack  = new Position(r + pawnDir, c - 1);
    Position rightAttack = new Position(r + pawnDir, c + 1);

    if (leftAttack.isValid()) {
        Piece p = board.get(leftAttack);
        if (p instanceof Pawn && p.isWhite() != sideToProtect) {
            return true;
        }
    }
    if (rightAttack.isValid()) {
        Piece p = board.get(rightAttack);
        if (p instanceof Pawn && p.isWhite() != sideToProtect) {
            return true;
        }
    }

    // Verificação de ataque por CAVALOS
    int[][] knightJumps = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
    for (int[] d : knightJumps) {
        int rr = r + d[0];
        int cc = c + d[1];
        if (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
            Piece p = board.get(new Position(rr, cc));
            if (p instanceof Knight && p.isWhite() != sideToProtect) {
                return true;
            }
        }
    }

    // Verificação de ataque por REIS (para prevenir movimentos adjacentes)
    for (int dr = -1; dr <= 1; dr++) {
        for (int dc = -1; dc <= 1; dc++) {
            if (dr == 0 && dc == 0) continue;
            int rr = r + dr;
            int cc = c + dc;
            if (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
                Piece p = board.get(new Position(rr, cc));
                if (p instanceof King && p.isWhite() != sideToProtect) {
                    return true;
                }
            }
        }
    }

    // Verificação de ataque por TORRES e RAINHAS (horizontal/vertical)
    int[][] rookDirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
    for (int[] d : rookDirs) {
        int rr = r + d[0];
        int cc = c + d[1];
        while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
            Piece p = board.get(new Position(rr, cc));
            if (p != null) {
                if (p.isWhite() != sideToProtect && (p instanceof Rook || p instanceof Queen)) {
                    return true;
                }
                break; // Parar se encontrar uma peça
            }
            rr += d[0];
            cc += d[1];
        }
    }

    // Verificação de ataque por BISPOS e RAINHAS (diagonais)
    int[][] bishopDirs = {{-1,-1}, {-1,1}, {1,-1}, {1,1}};
    for (int[] d : bishopDirs) {
        int rr = r + d[0];
        int cc = c + d[1];
        while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
            Piece p = board.get(new Position(rr, cc));
            if (p != null) {
                if (p.isWhite() != sideToProtect && (p instanceof Bishop || p instanceof Queen)) {
                    return true;
                }
                break; // Parar se encontrar uma peça
            }
            rr += d[0];
            cc += d[1];
        }
    }

    return false;
}

    private void forceMoveNoChecks(Position from, Position to) {
        Piece p = board.get(from);
        if (p == null) return;

        int dCol = Math.abs(to.getColumn() - from.getColumn());
        boolean isPawn = p instanceof Pawn;
        boolean isKing = p instanceof King;

        boolean diagonal = from.getColumn() != to.getColumn();
        boolean toIsEmpty = board.get(to) == null;
        boolean ep = isPawn && diagonal && toIsEmpty && enPassantTarget != null && to.equals(enPassantTarget);

        boolean castle = isKing && dCol == 2;

        board.set(to, p);
        board.set(from, null);
        p.setMoved(true);

        if (ep) {
            int dir = p.isWhite() ? 1 : -1;
            Position victim = new Position(to.getRow() + dir, to.getColumn());
            board.set(victim, null);
        }

        if (castle) {
            int row = to.getRow();
            if (to.getColumn() == 6) {
                Piece rook = board.get(new Position(row, 7));
                board.set(new Position(row, 5), rook);
                board.set(new Position(row, 7), null);
                if (rook != null) rook.setMoved(true);
            } else if (to.getColumn() == 2) {
                Piece rook = board.get(new Position(row, 0));
                board.set(new Position(row, 3), rook);
                board.set(new Position(row, 0), null);
                if (rook != null) rook.setMoved(true);
            }
        }

        enPassantTarget = null;
    }

    private Position findKing(boolean whiteSide) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);
                Piece piece = board.get(pos);
                if (piece instanceof King && piece.isWhite() == whiteSide) {
                    return pos;
                }
            }
        }
        return null;
    }

    private Game snapshotShallow() {
        Game g = new Game(true);
        g.board = this.board.copy();
        g.whiteToMove = this.whiteToMove;
        g.gameOver = this.gameOver;
        g.enPassantTarget = (this.enPassantTarget == null)
                ? null
                : new Position(this.enPassantTarget.getRow(), this.enPassantTarget.getColumn());
        g.history.addAll(this.history);
        return g;
    }

    private void addHistory(String moveStr) {
        history.add(moveStr);
    }

    private String coord(Position p) {
        char file = (char) ('a' + p.getColumn());
        int rank = 8 - p.getRow();
        return "" + file + rank;
    }

    private void setupPieces() {
        board.placePiece(new Rook(board, true), new Position(7, 0));
        board.placePiece(new Knight(board, true), new Position(7, 1));
        board.placePiece(new Bishop(board, true), new Position(7, 2));
        board.placePiece(new Queen(board, true), new Position(7, 3));
        board.placePiece(new King(board, true), new Position(7, 4));
        board.placePiece(new Bishop(board, true), new Position(7, 5));
        board.placePiece(new Knight(board, true), new Position(7, 6));
        board.placePiece(new Rook(board, true), new Position(7, 7));
        for (int c = 0; c < 8; c++) {
            board.placePiece(new Pawn(board, true), new Position(6, c));
        }

        board.placePiece(new Rook(board, false), new Position(0, 0));
        board.placePiece(new Knight(board, false), new Position(0, 1));
        board.placePiece(new Bishop(board, false), new Position(0, 2));
        board.placePiece(new Queen(board, false), new Position(0, 3));
        board.placePiece(new King(board, false), new Position(0, 4));
        board.placePiece(new Bishop(board, false), new Position(0, 5));
        board.placePiece(new Knight(board, false), new Position(0, 6));
        board.placePiece(new Rook(board, false), new Position(0, 7));
        for (int c = 0; c < 8; c++) {
            board.placePiece(new Pawn(board, false), new Position(1, c));
        }
    }
}