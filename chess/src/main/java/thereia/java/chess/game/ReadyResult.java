package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.protocol.GameStartMessage;
import thereia.java.chess.protocol.RoomInfoMessage;

@Getter
public final class ReadyResult {

    private final boolean started;
    private final RoomInfoMessage roomInfo;
    private final GameStartMessage gameStartRed;
    private final GameStartMessage gameStartBlack;

    public ReadyResult(boolean started, RoomInfoMessage roomInfo, GameStartMessage gameStartRed,
                       GameStartMessage gameStartBlack) {
        this.started = started;
        this.roomInfo = roomInfo;
        this.gameStartRed = gameStartRed;
        this.gameStartBlack = gameStartBlack;
    }
}
