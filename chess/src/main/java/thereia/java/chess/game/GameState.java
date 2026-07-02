package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.board.Board;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;

import java.time.Instant;

@Getter
public final class GameState {

    private final Board board;
    private final ChessColor currentTurn;
    private final GameStatus status;
    private final FlipPool redPool;
    private final FlipPool blackPool;
    private final int noCapturePlyCount;
    private final int moveNumber;
    private final Instant turnStartedAt;
    private final Instant turnDeadlineAt;
    private final String winnerColor;
    private final String endReason;

    public GameState(Board board, ChessColor currentTurn, GameStatus status, FlipPool redPool, FlipPool blackPool,
                     int noCapturePlyCount, int moveNumber, Instant turnStartedAt, Instant turnDeadlineAt,
                     String winnerColor, String endReason) {
        this.board = board;
        this.currentTurn = currentTurn;
        this.status = status;
        this.redPool = redPool;
        this.blackPool = blackPool;
        this.noCapturePlyCount = noCapturePlyCount;
        this.moveNumber = moveNumber;
        this.turnStartedAt = turnStartedAt;
        this.turnDeadlineAt = turnDeadlineAt;
        this.winnerColor = winnerColor;
        this.endReason = endReason;
    }

}
