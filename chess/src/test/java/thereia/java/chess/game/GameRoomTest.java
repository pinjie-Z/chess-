package thereia.java.chess.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thereia.java.chess.board.Board;
import thereia.java.chess.board.Position;
import thereia.java.chess.move.Move;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;
import thereia.java.chess.protocol.GameOverMessage;
import thereia.java.chess.protocol.MoveResultMessage;
import thereia.java.chess.protocol.TimeoutMessage;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.nio.file.Path;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GameRoomTest {

    @TempDir
    Path dir;

    @Test
    void returnsMoveResultAndGameOverAfterCapturingKing() {
        GameRoom room = new GameRoom("room-1", new Player("red", "red", ChessColor.RED), new Player("black", "black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 1), Piece.visible("r-e1", ChessColor.RED, PieceType.ROOK)),
                        ChessColor.RED, 0, 0),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        RoomMoveResult result = room.handleMove("red", move("e", 1, "e", 9), Instant.parse("2026-06-30T08:00:00Z"));

        MoveResultMessage moveResult = result.getActorMoveResult();
        GameOverMessage gameOver = result.getGameOver();

        assertThat(result.isSuccess()).isTrue();
        assertThat(moveResult).isNotNull();
        assertThat(moveResult.isSuccess()).isTrue();
        assertThat(moveResult.getMove().getToX()).isEqualTo("e");
        assertThat(moveResult.getMove().getToY()).isEqualTo(9);
        assertThat(gameOver).isNotNull();
        assertThat(gameOver.getWinner()).isEqualTo("red");
        assertThat(gameOver.getWinnerId()).isEqualTo("red");
        assertThat(gameOver.getReason()).isEqualTo("checkmate");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isEqualTo("RED");
        assertThat(room.getState().getEndReason()).isEqualTo("checkmate");
    }

    @Test
    void declaresDrawWhenNoCaptureCountReachesEighty() {
        GameRoom room = new GameRoom("room-2", new Player("red", "red", ChessColor.RED), new Player("black", "black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN))
                                .put(Position.of("a", 0), Piece.visible("r-a0", ChessColor.RED, PieceType.ROOK)),
                        ChessColor.RED, 79, 12),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        RoomMoveResult result = room.handleMove("red", move("a", 0, "a", 1), Instant.parse("2026-06-30T08:00:00Z"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getActorMoveResult()).isNotNull();
        assertThat(result.getGameOver()).isNotNull();
        assertThat(result.getGameOver().getWinner()).isNull();
        assertThat(result.getGameOver().getWinnerId()).isNull();
        assertThat(result.getGameOver().getReason()).isEqualTo("drawNoCapture");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isNull();
        assertThat(room.getState().getEndReason()).isEqualTo("drawNoCapture");
    }

    @Test
    void resignEndsGameForOpponent() {
        GameRoom room = new GameRoom("room-3", new Player("red", "red", ChessColor.RED), new Player("black", "black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN)),
                        ChessColor.RED, 10, 5),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        GameOverMessage gameOver = room.resign("red");

        assertThat(gameOver).isNotNull();
        assertThat(gameOver.getReason()).isEqualTo("resign");
        assertThat(gameOver.getWinner()).isEqualTo("black");
        assertThat(gameOver.getWinnerId()).isEqualTo("black");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isEqualTo("BLACK");
        assertThat(room.getState().getEndReason()).isEqualTo("resign");
    }

    @Test
    void timeoutEndsGameForOpponentAndReturnsTimeoutMessage() {
        GameRoom room = new GameRoom("room-4", new Player("red", "red", ChessColor.RED), new Player("black", "black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN)),
                        ChessColor.RED, 7, 9),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        TimeoutMessage timeout = room.timeout(ChessColor.RED, room.getState().getTurnDeadlineAt());

        assertThat(timeout).isNotNull();
        assertThat(timeout.getLoserId()).isEqualTo("red");
        assertThat(timeout.getWinnerId()).isEqualTo("black");
        assertThat(timeout.getReason()).isEqualTo("timeout");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isEqualTo("BLACK");
        assertThat(room.getState().getEndReason()).isEqualTo("timeout");
    }

    @Test
    void hidesCapturedHiddenPieceTypeFromDefenderView() {
        GameRoom room = new GameRoom("room-5", new Player("red", "red", ChessColor.RED), new Player("black", "black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN))
                                .put(Position.of("a", 0), Piece.visible("r-a0", ChessColor.RED, PieceType.ROOK))
                                .put(Position.of("a", 3), Piece.hidden("b-a3", ChessColor.BLACK, PieceType.PAWN)),
                        ChessColor.RED, 0, 0),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        RoomMoveResult result = room.handleMove("red", move("a", 0, "a", 3), Instant.parse("2026-06-30T08:00:00Z"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getActorMoveResult().getCapturedPiece()).isNotNull();
        assertThat(result.getOpponentMoveResult().getCapturedPiece()).isNull();
    }

    private GameState state(Board board, ChessColor currentTurn, int noCapturePlyCount, int moveNumber) {
        Instant now = Instant.parse("2026-06-30T08:00:00Z");
        return new GameState(board, currentTurn, GameStatus.PLAYING, FlipPool.initial(new Random(1)),
                FlipPool.initial(new Random(2)), noCapturePlyCount, moveNumber, now, now.plusSeconds(60), null, null);
    }

    private Move move(String fromX, int fromY, String toX, int toY) {
        return new Move(Position.of(fromX, fromY), Position.of(toX, toY), false);
    }
}
