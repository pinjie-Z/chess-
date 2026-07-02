package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class PongMessage {

    private final String messageType;

    public PongMessage() {
        this.messageType = MessageType.pong.name();
    }
}