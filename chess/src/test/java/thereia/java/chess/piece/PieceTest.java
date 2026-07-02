package thereia.java.chess.piece;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PieceTest {

    @Test
    void hiddenPieceUsesOriginalTypeUntilRevealed() {
        Piece piece = Piece.hidden("r-a0", ChessColor.RED, PieceType.ROOK);
        assertThat(piece.isVisible()).isFalse();
        assertThat(piece.getMovementType()).isEqualTo(PieceType.ROOK);

        Piece revealed = piece.reveal(PieceType.PAWN);
        assertThat(revealed.isVisible()).isTrue();
        assertThat(revealed.getRevealedTypeOptional()).contains(PieceType.PAWN);
        assertThat(revealed.getMovementType()).isEqualTo(PieceType.PAWN);
    }

    @Test
    void visiblePieceUsesItsVisibleType() {
        Piece piece = Piece.visible("r-e0", ChessColor.RED, PieceType.KING);

        assertThat(piece.isVisible()).isTrue();
        assertThat(piece.getOriginalType()).isEqualTo(PieceType.KING);
        assertThat(piece.getRevealedTypeOptional()).contains(PieceType.KING);
        assertThat(piece.getMovementType()).isEqualTo(PieceType.KING);
    }

    @Test
    void colorKnowsOpponentAndForwardDirection() {
        assertThat(ChessColor.RED.opponent()).isEqualTo(ChessColor.BLACK);
        assertThat(ChessColor.BLACK.opponent()).isEqualTo(ChessColor.RED);
        assertThat(ChessColor.RED.forwardDy()).isEqualTo(1);
        assertThat(ChessColor.BLACK.forwardDy()).isEqualTo(-1);
    }

    @Test
    void cannotRevealAlreadyVisiblePiece() {
        Piece piece = Piece.visible("r-e0", ChessColor.RED, PieceType.KING);

        assertThatThrownBy(() -> piece.reveal(PieceType.ROOK)).isInstanceOf(IllegalStateException.class);
    }
}
