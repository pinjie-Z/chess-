package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class TimeoutMessage {

    private final String messageType;
    private final String loserId;
    private final String winnerId;
    private final String reason;

    public TimeoutMessage(String messageType, String loserId, String winnerId, String reason) {
        this.messageType = messageType;
        this.loserId = loserId;
        this.winnerId = winnerId;
        this.reason = reason;
    }

}
