package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class LoginResultMessage {

    private final String messageType;
    private final boolean success;
    private final String reason;
    private final String userId;
    private final String nickName;

    public LoginResultMessage(String messageType, boolean success, String reason, String userId, String nickName) {
        this.messageType = messageType;
        this.success = success;
        this.reason = reason;
        this.userId = userId;
        this.nickName = nickName;
    }
}
