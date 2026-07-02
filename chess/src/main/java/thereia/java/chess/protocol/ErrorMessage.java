package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class ErrorMessage {

    private final String messageType;
    private final int code;
    private final String message;

    public ErrorMessage(String messageType, int code, String message) {
        this.messageType = messageType;
        this.code = code;
        this.message = message;
    }

    public static ErrorMessage of(int code, String message) {
        return new ErrorMessage(MessageType.error.name(), code, message);
    }

}
