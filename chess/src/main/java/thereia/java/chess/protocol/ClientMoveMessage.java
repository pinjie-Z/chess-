package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class ClientMoveMessage {

    private final String messageType;
    private final String fromX;
    private final int fromY;
    private final String toX;
    private final int toY;
    private final boolean isFlip;

    public ClientMoveMessage(String messageType, String fromX, int fromY, String toX, int toY, boolean isFlip) {
        this.messageType = messageType;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.isFlip = isFlip;
    }

}
