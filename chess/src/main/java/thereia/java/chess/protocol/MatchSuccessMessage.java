package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class MatchSuccessMessage {

    private final String messageType;
    private final String roomId;
    private final String opponentId;
    private final String opponentNickname;

    public MatchSuccessMessage(String messageType, String roomId, String opponentId, String opponentNickname) {
        this.messageType = messageType;
        this.roomId = roomId;
        this.opponentId = opponentId;
        this.opponentNickname = opponentNickname;
    }

}
