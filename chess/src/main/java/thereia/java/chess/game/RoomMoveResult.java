package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.protocol.GameOverMessage;
import thereia.java.chess.protocol.MoveResultMessage;

@Getter
public final class RoomMoveResult {

    private final boolean success;
    private final MoveResultMessage actorMoveResult;
    private final MoveResultMessage opponentMoveResult;
    private final GameOverMessage gameOver;

    public RoomMoveResult(boolean success, MoveResultMessage actorMoveResult, MoveResultMessage opponentMoveResult,
                          GameOverMessage gameOver) {
        this.success = success;
        this.actorMoveResult = actorMoveResult;
        this.opponentMoveResult = opponentMoveResult;
        this.gameOver = gameOver;
    }

}
