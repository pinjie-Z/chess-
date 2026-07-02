package thereia.java.chess.board;

import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.PieceType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Board {

    private final Map<Position, Piece> pieces;

    private Board(Map<Position, Piece> pieces) {
        this.pieces = Map.copyOf(pieces);
    }

    public static Board empty() {
        return new Board(Collections.emptyMap());
    }

    public static Board initial() {
        Board board = Board.empty();
        board = putInitialSide(board, ChessColor.RED, 0, 2, 3, "r");
        return putInitialSide(board, ChessColor.BLACK, 9, 7, 6, "b");
    }

    public Optional<Piece> pieceAt(Position position) {
        return Optional.ofNullable(pieces.get(position));
    }

    public boolean isEmpty(Position position) {
        return !pieces.containsKey(position);
    }

    public int occupiedCount() {
        return pieces.size();
    }

    public int countBetween(Position from, Position to) {
        if (from.getRow() == to.getRow()) {
            return countBetweenSameRank(from, to);
        }
        if (from.getCol() == to.getCol()) {
            return countBetweenSameFile(from, to);
        }
        return 0;
    }

    public Board move(Position from, Position to, Piece movedPiece) {
        Map<Position, Piece> next = new HashMap<>(pieces);
        next.remove(from);
        next.put(to, movedPiece);
        return new Board(next);
    }

    public Board remove(Position position) {
        Map<Position, Piece> next = new HashMap<>(pieces);
        next.remove(position);
        return new Board(next);
    }

    public Board put(Position position, Piece piece) {
        Map<Position, Piece> next = new HashMap<>(pieces);
        next.put(position, piece);
        return new Board(next);
    }

    private static Board putInitialSide(Board board, ChessColor color, int backRank, int cannonRank, int pawnRank,
                                        String idPrefix) {
        board = board
                .put(initialPosition("a", backRank), hidden(idPrefix, "a", backRank, color, PieceType.ROOK))
                .put(initialPosition("b", backRank), hidden(idPrefix, "b", backRank, color, PieceType.KNIGHT))
                .put(initialPosition("c", backRank), hidden(idPrefix, "c", backRank, color, PieceType.BISHOP))
                .put(initialPosition("d", backRank), hidden(idPrefix, "d", backRank, color, PieceType.GUARD))
                .put(initialPosition("e", backRank), Piece.visible(id(idPrefix, "e", backRank), color, PieceType.KING))
                .put(initialPosition("f", backRank), hidden(idPrefix, "f", backRank, color, PieceType.GUARD))
                .put(initialPosition("g", backRank), hidden(idPrefix, "g", backRank, color, PieceType.BISHOP))
                .put(initialPosition("h", backRank), hidden(idPrefix, "h", backRank, color, PieceType.KNIGHT))
                .put(initialPosition("i", backRank), hidden(idPrefix, "i", backRank, color, PieceType.ROOK))
                .put(initialPosition("b", cannonRank), hidden(idPrefix, "b", cannonRank, color, PieceType.CANNON))
                .put(initialPosition("h", cannonRank), hidden(idPrefix, "h", cannonRank, color, PieceType.CANNON));

        for (String x : new String[]{"a", "c", "e", "g", "i"}) {
            board = board.put(initialPosition(x, pawnRank), hidden(idPrefix, x, pawnRank, color, PieceType.PAWN));
        }
        return board;
    }

    private static Position initialPosition(String x, int y) {
        return Position.of(x, y);
    }

    private static Piece hidden(String idPrefix, String x, int y, ChessColor color, PieceType originalType) {
        return Piece.hidden(id(idPrefix, x, y), color, originalType);
    }

    private static String id(String idPrefix, String x, int y) {
        return idPrefix + "-" + x + y;
    }

    private int countBetweenSameRank(Position from, Position to) {
        int row = from.getRow();
        int start = Math.min(from.getCol(), to.getCol()) + 1;
        int end = Math.max(from.getCol(), to.getCol());
        int count = 0;
        for (int col = start; col < end; col++) {
            if (!isEmpty(Position.fromArrayIndex(row, col))) {
                count++;
            }
        }
        return count;
    }

    private int countBetweenSameFile(Position from, Position to) {
        int col = from.getCol();
        int start = Math.min(from.getRow(), to.getRow()) + 1;
        int end = Math.max(from.getRow(), to.getRow());
        int count = 0;
        for (int row = start; row < end; row++) {
            if (!isEmpty(Position.fromArrayIndex(row, col))) {
                count++;
            }
        }
        return count;
    }
}
