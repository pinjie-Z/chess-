package thereia.java.chess.game;

import thereia.java.chess.auth.UserAccount;
import thereia.java.chess.board.Board;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

public final class RoomManager {

    private final RuleEngine ruleEngine;
    private final MoveExecutor moveExecutor;
    private final GameRecorder gameRecorder;

    private UserAccount waitingUser;
    private GameRoom activeRoom;

    public RoomManager(RuleEngine ruleEngine, MoveExecutor moveExecutor, GameRecorder gameRecorder) {
        this.ruleEngine = ruleEngine;
        this.moveExecutor = moveExecutor;
        this.gameRecorder = gameRecorder;
    }

    public MatchResult startMatch(UserAccount user) {
        if (activeRoom != null && roomForPlayer(user.getUserId()).isPresent()) {
            return new MatchResult(true, null, Optional.of(activeRoom));
        }
        if (waitingUser == null) {
            waitingUser = user;
            return new MatchResult(false, user.getUserId(), Optional.empty());
        }
        if (waitingUser.getUserId().equals(user.getUserId())) {
            return new MatchResult(false, user.getUserId(), Optional.empty());
        }

        activeRoom = new GameRoom("room-1", new Player(waitingUser.getUserId(), waitingUser.getNickName(), ChessColor.RED),
                new Player(user.getUserId(), user.getNickName(), ChessColor.BLACK), initialPreparingState(),
                ruleEngine, moveExecutor, gameRecorder);
        waitingUser = null;
        return new MatchResult(true, null, Optional.of(activeRoom));
    }

    public Optional<GameRoom> roomForPlayer(String playerId) {
        if (activeRoom == null) {
            return Optional.empty();
        }
        if (activeRoom.getRedPlayer().getPlayerId().equals(playerId)
                || activeRoom.getBlackPlayer().getPlayerId().equals(playerId)) {
            return Optional.of(activeRoom);
        }
        return Optional.empty();
    }

    private GameState initialPreparingState() {
        Instant now = Instant.now();
        return new GameState(Board.initial(), null, GameStatus.PREPARING, FlipPool.initial(new Random()),
                FlipPool.initial(new Random()), 0, 0, now, now.plusSeconds(60), null, null);
    }

    public void cancelMatch(String userId) {
        if (waitingUser != null && waitingUser.getUserId().equals(userId)) {
            waitingUser = null;
        }
    }

    public UserAccount getWaitingUser() {
        return waitingUser;
    }
}
