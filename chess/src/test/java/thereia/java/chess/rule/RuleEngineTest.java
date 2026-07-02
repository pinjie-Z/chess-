package thereia.java.chess.rule;

import org.junit.jupiter.api.Test;
import thereia.java.chess.board.Board;
import thereia.java.chess.board.Position;
import thereia.java.chess.move.Move;
import thereia.java.chess.move.MoveValidationResult;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private final RuleEngine ruleEngine = new RuleEngine();

    @Test
    void rejectsBasicInvalidMoves() {
        Board board = Board.empty()
                .put(pos("a", 0), hidden("red-rook", ChessColor.RED, PieceType.ROOK))
                .put(pos("a", 1), hidden("red-pawn", ChessColor.RED, PieceType.PAWN));

        assertThat(validate(Board.empty(), "a", 0, "a", 1, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(board, "a", 0, "a", 0, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(board, "a", 0, "a", 1, ChessColor.BLACK).isValid()).isFalse();
        assertThat(validate(board, "a", 0, "a", 1, ChessColor.RED).isValid()).isFalse();
    }

    @Test
    void validatesRookStraightMovementAndBlockers() {
        Board clear = Board.empty()
                .put(pos("a", 0), hidden("red-rook", ChessColor.RED, PieceType.ROOK));
        Board blocked = clear.put(pos("a", 2), hidden("blocker", ChessColor.BLACK, PieceType.PAWN));

        assertThat(validate(clear, "a", 0, "a", 4, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(clear, "a", 0, "d", 3, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(blocked, "a", 0, "a", 4, ChessColor.RED).isValid()).isFalse();
    }

    @Test
    void validatesCannonScreenForMoveAndCapture() {
        Board board = Board.empty()
                .put(pos("a", 0), hidden("red-cannon", ChessColor.RED, PieceType.CANNON))
                .put(pos("a", 2), hidden("screen", ChessColor.RED, PieceType.PAWN))
                .put(pos("a", 4), hidden("black-pawn", ChessColor.BLACK, PieceType.PAWN));

        assertThat(validate(board, "a", 0, "a", 1, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(board, "a", 0, "a", 3, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(board, "a", 0, "a", 4, ChessColor.RED).isValid()).isTrue();
    }

    @Test
    void validatesKnightShapeAndHorseLeg() {
        Board clear = Board.empty()
                .put(pos("b", 0), hidden("red-knight", ChessColor.RED, PieceType.KNIGHT));
        Board blocked = clear.put(pos("b", 1), hidden("leg", ChessColor.RED, PieceType.PAWN));

        assertThat(validate(clear, "b", 0, "c", 2, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(clear, "b", 0, "c", 1, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(blocked, "b", 0, "c", 2, ChessColor.RED).isValid()).isFalse();
    }

    @Test
    void validatesPawnDirectionAndRiverCrossingForBothColors() {
        Board board = Board.empty()
                .put(pos("a", 3), hidden("red-before-river", ChessColor.RED, PieceType.PAWN))
                .put(pos("c", 5), hidden("red-after-river", ChessColor.RED, PieceType.PAWN))
                .put(pos("e", 6), hidden("black-before-river", ChessColor.BLACK, PieceType.PAWN))
                .put(pos("g", 4), hidden("black-after-river", ChessColor.BLACK, PieceType.PAWN));

        assertThat(validate(board, "a", 3, "a", 4, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(board, "a", 3, "b", 3, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(board, "c", 5, "d", 5, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(board, "c", 5, "c", 4, ChessColor.RED).isValid()).isFalse();

        assertThat(validate(board, "e", 6, "e", 5, ChessColor.BLACK).isValid()).isTrue();
        assertThat(validate(board, "e", 6, "f", 6, ChessColor.BLACK).isValid()).isFalse();
        assertThat(validate(board, "g", 4, "h", 4, ChessColor.BLACK).isValid()).isTrue();
        assertThat(validate(board, "g", 4, "g", 5, ChessColor.BLACK).isValid()).isFalse();
    }

    @Test
    void validatesKingPalaceAndFacingKings() {
        Board board = Board.empty()
                .put(pos("e", 0), Piece.visible("red-king", ChessColor.RED, PieceType.KING))
                .put(pos("e", 9), Piece.visible("black-king", ChessColor.BLACK, PieceType.KING))
                .put(pos("e", 4), hidden("screen", ChessColor.RED, PieceType.ROOK));

        assertThat(validate(board, "e", 0, "e", 1, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(board, "e", 0, "e", 3, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(board, "e", 4, "d", 4, ChessColor.RED).isValid()).isFalse();
    }

    @Test
    void validatesGuardPalaceOnlyBeforeReveal() {
        Board hiddenGuard = Board.empty()
                .put(pos("e", 1), hidden("hidden-guard", ChessColor.RED, PieceType.GUARD));
        Board hiddenGuardLeavingPalace = Board.empty()
                .put(pos("f", 2), hidden("hidden-guard-edge", ChessColor.RED, PieceType.GUARD));
        Board revealedGuard = Board.empty()
                .put(pos("f", 2), Piece.hidden("revealed-guard-source", ChessColor.RED, PieceType.PAWN)
                        .reveal(PieceType.GUARD));

        assertThat(validate(hiddenGuard, "e", 1, "f", 2, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(hiddenGuard, "e", 1, "f", 0, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(hiddenGuard, "e", 1, "f", 3, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(hiddenGuardLeavingPalace, "f", 2, "g", 3, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(revealedGuard, "f", 2, "g", 3, ChessColor.RED).isValid()).isTrue();
    }

    @Test
    void validatesBishopRiverLimitOnlyBeforeRevealAndElephantEye() {
        Board clearEye = Board.empty()
                .put(pos("c", 2), hidden("hidden-bishop", ChessColor.RED, PieceType.BISHOP));
        Board blockedEye = clearEye.put(pos("b", 3), hidden("eye-blocker", ChessColor.RED, PieceType.PAWN));
        Board hiddenCrossing = Board.empty()
                .put(pos("c", 4), hidden("hidden-bishop-crossing", ChessColor.RED, PieceType.BISHOP));
        Board revealedCrossing = Board.empty()
                .put(pos("c", 4), Piece.hidden("revealed-bishop-source", ChessColor.RED, PieceType.PAWN)
                        .reveal(PieceType.BISHOP));

        assertThat(validate(clearEye, "c", 2, "a", 4, ChessColor.RED).isValid()).isTrue();
        assertThat(validate(blockedEye, "c", 2, "a", 4, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(hiddenCrossing, "c", 4, "e", 6, ChessColor.RED).isValid()).isFalse();
        assertThat(validate(revealedCrossing, "c", 4, "e", 6, ChessColor.RED).isValid()).isTrue();
    }

    private MoveValidationResult validate(Board board, String fromX, int fromY, String toX, int toY,
                                          ChessColor moverColor) {
        return ruleEngine.validate(board, new Move(pos(fromX, fromY), pos(toX, toY), false), moverColor);
    }

    private static Position pos(String x, int y) {
        return Position.of(x, y);
    }

    private static Piece hidden(String id, ChessColor color, PieceType type) {
        return Piece.hidden(id, color, type);
    }
}
