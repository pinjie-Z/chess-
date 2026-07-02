package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class RoomInfoMessage {

    private final String messageType;
    private final boolean opponentReady;

    public RoomInfoMessage(String messageType, boolean opponentReady) {
        this.messageType = messageType;
        this.opponentReady = opponentReady;
    }
}
