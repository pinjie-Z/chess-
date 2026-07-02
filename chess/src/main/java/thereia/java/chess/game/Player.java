package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.piece.ChessColor;

@Getter
public final class Player {

    private final String playerId;
    private final String nickName;
    private final ChessColor color;

    public Player(String playerId, String nickName, ChessColor color) {
        this.playerId = playerId;
        this.nickName = nickName;
        this.color = color;
    }

}
