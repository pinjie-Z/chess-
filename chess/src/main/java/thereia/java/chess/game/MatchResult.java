package thereia.java.chess.game;

import lombok.Getter;

import java.util.Optional;

@Getter
public final class MatchResult {

    private final boolean matched;
    private final String waitingPlayerId;
    private final Optional<GameRoom> room;

    public MatchResult(boolean matched, String waitingPlayerId, Optional<GameRoom> room) {
        this.matched = matched;
        this.waitingPlayerId = waitingPlayerId;
        this.room = room;
    }

}
