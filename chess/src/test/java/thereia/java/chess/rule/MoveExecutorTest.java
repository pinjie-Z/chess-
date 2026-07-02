package thereia.java.chess.rule;

import org.junit.jupiter.api.Test;
import thereia.java.chess.board.Board;
import thereia.java.chess.board.Position;
import thereia.java.chess.game.GameState;
import thereia.java.chess.game.GameStatus;
import thereia.java.chess.move.Move;
import thereia.java.chess.move.MoveRecord;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MoveExecutorTest {

    private static final Instant NOW = Instant.parse("2026-06-30T08:00:00Z");

    private final MoveExecutor executor = new MoveExecutor();

    @Test
    void appliesVisibleNonCaptureMove() {
        Piece rook = Piece.visible("red-rook", ChessColor.RED, PieceType.ROOK);
        GameState state = state(Board.empty().put(pos("a", 0), rook), ChessColor.RED, 3, 7,
                FlipPool.initial(new Random(1)), FlipPool.initial(new Random(2)));

        MoveExecutor.MoveExecution execution = executor.apply(state, move("a", 0, "a", 3), ChessColor.RED, NOW);

        assertThat(execution.isSuccess()).isTrue();
        assertThat(execution.getValidation().isValid()).isTrue();
        assertThat(execution.getState().getBoard().isEmpty(pos("a", 0))).isTrue();
        assertThat(execution.getState().getBoard().pieceAt(pos("a", 3))).contains(rook);
        assertThat(execution.getState().getCurrentTurn()).isEqualTo(ChessColor.BLACK);
        assertThat(execution.getState().getMoveNumber()).isEqualTo(8);
        assertThat(execution.getState().getNoCapturePlyCount()).isEqualTo(4);
        assertThat(execution.getState().getTurnStartedAt()).isEqualTo(NOW);
        assertThat(execution.getState().getTurnDeadlineAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(execution.getFlipResult()).isNull();
        assertThat(execution.getCapturedPiece()).isNull();

        MoveRecord record = execution.getRecord();
        assertThat(record.getMoveNumber()).isEqualTo(8);
        assertThat(record.getColor()).isEqualTo(ChessColor.RED);
        assertThat(record.getFrom()).isEqualTo(pos("a", 0));
        assertThat(record.getTo()).isEqualTo(pos("a", 3));
        assertThat(record.getFlipResult()).isNull();
        assertThat(record.getCapturedPiece()).isNull();
    }

    @Test
    void revealsHiddenMovedPieceFromMoverPool() {
        Piece hiddenRook = Piece.hidden("red-hidden", ChessColor.RED, PieceType.ROOK);
        FlipPool redPool = FlipPool.initial(new Random(10));
        List<PieceType> before = redPool.remainingTypes();
        GameState state = state(Board.empty().put(pos("a", 0), hiddenRook), ChessColor.RED, 0, 1,
                redPool, FlipPool.initial(new Random(2)));

        MoveExecutor.MoveExecution execution = executor.apply(state, move("a", 0, "a", 1), ChessColor.RED, NOW);

        Piece moved = execution.getState().getBoard().pieceAt(pos("a", 1)).orElseThrow();
        assertThat(moved.isVisible()).isTrue();
        assertThat(moved.getRevealedTypeOptional()).contains(execution.getFlipResult());
        assertThat(before).contains(execution.getFlipResult());
        assertThat(execution.getState().getRedPool().remainingCount()).isEqualTo(14);
        assertThat(execution.getState().getBlackPool().remainingCount()).isEqualTo(15);
        assertThat(execution.getRecord().getFlipResult()).isEqualTo(execution.getFlipResult());
    }

    @Test
    void capturesHiddenTargetUsingCapturedSidePool() {
        Piece redRook = Piece.visible("red-rook", ChessColor.RED, PieceType.ROOK);
        Piece blackHidden = Piece.hidden("black-hidden", ChessColor.BLACK, PieceType.PAWN);
        FlipPool redPool = FlipPool.initial(new Random(1));
        FlipPool blackPool = FlipPool.initial(new Random(20));
        List<PieceType> blackPoolBefore = blackPool.remainingTypes();
        GameState state = state(Board.empty()
                        .put(pos("a", 0), redRook)
                        .put(pos("a", 3), blackHidden),
                ChessColor.RED, 5, 4, redPool, blackPool);

        MoveExecutor.MoveExecution execution = executor.apply(state, move("a", 0, "a", 3), ChessColor.RED, NOW);

        assertThat(execution.getState().getBoard().pieceAt(pos("a", 3))).contains(redRook);
        assertThat(execution.getState().getBoard().isEmpty(pos("a", 0))).isTrue();
        assertThat(blackPoolBefore).contains(execution.getCapturedPiece());
        assertThat(execution.getState().getBlackPool().remainingCount()).isEqualTo(14);
        assertThat(execution.getState().getRedPool().remainingCount()).isEqualTo(15);
        assertThat(execution.getState().getNoCapturePlyCount()).isZero();
        assertThat(execution.getRecord().getCapturedPiece()).isEqualTo(execution.getCapturedPiece());
    }

    @Test
    void capturesVisibleTargetWithoutDrawingFromPool() {
        Piece redRook = Piece.visible("red-rook", ChessColor.RED, PieceType.ROOK);
        Piece blackKnight = Piece.visible("black-knight", ChessColor.BLACK, PieceType.KNIGHT);
        GameState state = state(Board.empty()
                        .put(pos("a", 0), redRook)
                        .put(pos("a", 3), blackKnight),
                ChessColor.RED, 2, 9, FlipPool.initial(new Random(1)), FlipPool.initial(new Random(2)));

        MoveExecutor.MoveExecution execution = executor.apply(state, move("a", 0, "a", 3), ChessColor.RED, NOW);

        assertThat(execution.getCapturedPiece()).isEqualTo(PieceType.KNIGHT);
        assertThat(execution.getState().getRedPool().remainingCount()).isEqualTo(15);
        assertThat(execution.getState().getBlackPool().remainingCount()).isEqualTo(15);
        assertThat(execution.getState().getNoCapturePlyCount()).isZero();
    }

    private static GameState state(Board board, ChessColor turn, int noCapturePlyCount, int moveNumber,
                                   FlipPool redPool, FlipPool blackPool) {
        return new GameState(board, turn, GameStatus.PLAYING, redPool, blackPool, noCapturePlyCount, moveNumber,
                Instant.parse("2026-06-30T07:59:00Z"), Instant.parse("2026-06-30T08:01:00Z"), null, null);
    }

    private static Move move(String fromX, int fromY, String toX, int toY) {
        return new Move(pos(fromX, fromY), pos(toX, toY), false);
    }

    private static Position pos(String x, int y) {
        return Position.of(x, y);
    }
}
