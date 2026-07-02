package thereia.java.chess.move;

import lombok.Getter;

@Getter
public final class MoveValidationResult {

    private final boolean valid;
    private final int code;
    private final String message;

    public MoveValidationResult(boolean valid, int code, String message) {
        this.valid = valid;
        this.code = code;
        this.message = message;
    }

    public static MoveValidationResult ok() {
        return new MoveValidationResult(true, 0, "ok");
    }

    public static MoveValidationResult illegal(String message) {
        return new MoveValidationResult(false, 2001, message);
    }

}
