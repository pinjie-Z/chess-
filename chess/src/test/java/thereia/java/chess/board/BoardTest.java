package thereia.java.chess.board;

import org.junit.jupiter.api.Test;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;

import static org.assertj.core.api.Assertions.assertThat;

class BoardTest {

    @Test
    void createsInitialJieqiBoardWithOnlyKingsVisible() {
        Board board = Board.initial();

        Piece redKing = board.pieceAt(Position.of("e", 0)).orElseThrow();
        Piece blackKing = board.pieceAt(Position.of("e", 9)).orElseThrow();
        Piece redLeftRook = board.pieceAt(Position.of("a", 0)).orElseThrow();
        Piece blackLeftRook = board.pieceAt(Position.of("a", 9)).orElseThrow();

        assertThat(redKing.isVisible()).isTrue();
        assertThat(redKing.getColor()).isEqualTo(ChessColor.RED);
        assertThat(redKing.getOriginalType()).isEqualTo(PieceType.KING);
        assertThat(redKing.getRevealedTypeOptional()).contains(PieceType.KING);

        assertThat(blackKing.isVisible()).isTrue();
        assertThat(blackKing.getColor()).isEqualTo(ChessColor.BLACK);
        assertThat(blackKing.getOriginalType()).isEqualTo(PieceType.KING);
        assertThat(blackKing.getRevealedTypeOptional()).contains(PieceType.KING);

        assertThat(redLeftRook.isVisible()).isFalse();
        assertThat(redLeftRook.getColor()).isEqualTo(ChessColor.RED);
        assertThat(redLeftRook.getOriginalType()).isEqualTo(PieceType.ROOK);
        assertThat(redLeftRook.getRevealedTypeOptional()).isEmpty();

        assertThat(blackLeftRook.isVisible()).isFalse();
        assertThat(blackLeftRook.getColor()).isEqualTo(ChessColor.BLACK);
        assertThat(blackLeftRook.getOriginalType()).isEqualTo(PieceType.ROOK);
        assertThat(blackLeftRook.getRevealedTypeOptional()).isEmpty();

        assertThat(board.occupiedCount()).isEqualTo(32);
        assertThat(board.isEmpty(Position.of("e", 4))).isTrue();
    }

    @Test
    void updatesBoardWithCopyOnWriteHelpers() {
        Position from = Position.of("a", 0);
        Position to = Position.of("a", 1);
        Piece piece = Piece.hidden("r-a0", ChessColor.RED, PieceType.ROOK);

        Board empty = Board.empty();
        Board withPiece = empty.put(from, piece);
        Board moved = withPiece.move(from, to, piece);
        Board removed = moved.remove(to);

        assertThat(empty.occupiedCount()).isZero();
        assertThat(empty.isEmpty(from)).isTrue();

        assertThat(withPiece.pieceAt(from)).contains(piece);
        assertThat(withPiece.isEmpty(to)).isTrue();

        assertThat(moved.isEmpty(from)).isTrue();
        assertThat(moved.pieceAt(to)).contains(piece);

        assertThat(removed.occupiedCount()).isZero();
        assertThat(removed.isEmpty(to)).isTrue();
    }

    @Test
    void countsPiecesBetweenSameFileOrRank() {
        Board board = Board.empty()
                .put(Position.of("a", 0), Piece.hidden("r-a0", ChessColor.RED, PieceType.ROOK))
                .put(Position.of("a", 3), Piece.hidden("r-a3", ChessColor.RED, PieceType.PAWN))
                .put(Position.of("a", 6), Piece.hidden("b-a6", ChessColor.BLACK, PieceType.PAWN))
                .put(Position.of("d", 0), Piece.hidden("r-d0", ChessColor.RED, PieceType.GUARD));

        assertThat(board.countBetween(Position.of("a", 0), Position.of("a", 6))).isEqualTo(1);
        assertThat(board.countBetween(Position.of("a", 6), Position.of("a", 0))).isEqualTo(1);
        assertThat(board.countBetween(Position.of("a", 0), Position.of("d", 0))).isZero();
    }

    @Test
    void returnsZeroBetweenDifferentFileAndRank() {
        Board board = Board.empty()
                .put(Position.of("a", 0), Piece.hidden("r-a0", ChessColor.RED, PieceType.ROOK))
                .put(Position.of("b", 1), Piece.hidden("r-b1", ChessColor.RED, PieceType.KNIGHT));

        assertThat(board.countBetween(Position.of("a", 0), Position.of("b", 1))).isZero();
    }
}
