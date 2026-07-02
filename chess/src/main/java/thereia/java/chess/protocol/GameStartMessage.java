package thereia.java.chess.protocol;

import lombok.Getter;

import java.util.List;

@Getter
public final class GameStartMessage {

    private final String messageType;
    private final String redPlayerId;
    private final String blackPlayerId;
    private final String yourColor;
    private final boolean firstHand;
    private final List<InitialPieceMessage> initialBoard;

    public GameStartMessage(String messageType, String redPlayerId, String blackPlayerId, String yourColor,
                            boolean firstHand,
                            List<InitialPieceMessage> initialBoard) {
        this.messageType = messageType;
        this.redPlayerId = redPlayerId;
        this.blackPlayerId = blackPlayerId;
        this.yourColor = yourColor;
        this.firstHand = firstHand;
        this.initialBoard = initialBoard;
    }

    @Getter
    public static final class InitialPieceMessage {

        private final String x;
        private final int y;
        private final String color;
        private final String piece;
        private final boolean visible;

        public InitialPieceMessage(String x, int y, String color, String piece, boolean visible) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.piece = piece;
            this.visible = visible;
        }

    }
}
