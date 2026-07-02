package thereia.java.chess.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thereia.java.chess.auth.UserAccount;
import thereia.java.chess.board.Board;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RoomManagerTest {

    @TempDir
    Path dir;

    @Test
    void firstPlayerWaitsAndSecondCreatesPreparingRoom() {
        RoomManager roomManager = roomManager();

        MatchResult first = roomManager.startMatch(user("A", "Alice"));
        MatchResult second = roomManager.startMatch(user("B", "Bob"));

        assertThat(first.isMatched()).isFalse();
        assertThat(first.getWaitingPlayerId()).isEqualTo("A");
        assertThat(first.getRoom()).isEmpty();

        assertThat(second.isMatched()).isTrue();
        assertThat(second.getRoom()).isPresent();
        GameRoom room = second.getRoom().orElseThrow();
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.PREPARING);
        assertThat(room.getRedPlayer().getPlayerId()).isEqualTo("A");
        assertThat(room.getBlackPlayer().getPlayerId()).isEqualTo("B");
        assertThat(roomManager.roomForPlayer("A")).contains(room);
        assertThat(roomManager.roomForPlayer("B")).contains(room);
    }

    @Test
    void roomStartsPlayingOnlyAfterBothPlayersReady() {
        RoomManager roomManager = roomManager();
        roomManager.startMatch(user("A", "Alice"));
        GameRoom room = roomManager.startMatch(user("B", "Bob")).getRoom().orElseThrow();
        Instant now = Instant.parse("2026-07-01T10:00:00Z");

        ReadyResult firstReady = room.ready("A", now);
        ReadyResult secondReady = room.ready("B", now.plusSeconds(1));

        assertThat(firstReady.isStarted()).isFalse();
        assertThat(firstReady.getGameStartRed()).isNull();
        assertThat(firstReady.getGameStartBlack()).isNull();
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.PLAYING);

        assertThat(secondReady.isStarted()).isTrue();
        assertThat(secondReady.getGameStartRed()).isNotNull();
        assertThat(secondReady.getGameStartBlack()).isNotNull();
        assertThat(secondReady.getGameStartRed().getYourColor()).isEqualTo("red");
        assertThat(secondReady.getGameStartBlack().getYourColor()).isEqualTo("black");
        assertThat(secondReady.getGameStartRed().getRedPlayerId()).isEqualTo("A");
        assertThat(secondReady.getGameStartRed().getBlackPlayerId()).isEqualTo("B");
        assertThat(room.getState().getCurrentTurn()).isEqualTo(ChessColor.RED);
        assertThat(room.getState().getTurnStartedAt()).isEqualTo(now.plusSeconds(1));
    }

    private RoomManager roomManager() {
        return new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
    }

    private UserAccount user(String userId, String nickName) {
        return new UserAccount(userId, "pw", nickName);
    }
}
