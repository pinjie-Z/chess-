package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class GameOverMessage {

    private final String messageType;
    private final String winner;
    private final String reason;
    private final String winnerId;

    public GameOverMessage(String messageType, String winner, String reason, String winnerId) {
        this.messageType = messageType;
        this.winner = winner;
        this.reason = reason;
        this.winnerId = winnerId;
    }

}
