package thereia.java.chess.move;

import lombok.Getter;
import thereia.java.chess.board.Position;

@Getter
public final class Move {

    private final Position from;
    private final Position to;
    private final boolean flipHint;

    public Move(Position from, Position to, boolean flipHint) {
        this.from = from;
        this.to = to;
        this.flipHint = flipHint;
    }

}
