package thereia.java.chess.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import thereia.java.chess.board.Board;
import thereia.java.chess.board.Position;
import thereia.java.chess.auth.UserStore;
import thereia.java.chess.game.GameRoom;
import thereia.java.chess.game.GameState;
import thereia.java.chess.game.GameStatus;
import thereia.java.chess.game.Player;
import thereia.java.chess.game.RoomManager;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameWebSocketHandlerTest {

    @TempDir
    Path dir;

    @Test
    void registerReturnsLoginResultAndAllowsMatchWithRealUserIdentity() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);

        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"register\",\"userId\":\"alice\",\"passWord\":\"123\",\"nickName\":\"Alice\"}"));
        handler.handleTextMessage(sessionB,
                new TextMessage("{\"messageType\":\"register\",\"userId\":\"bob\",\"passWord\":\"456\",\"nickName\":\"Bob\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);

        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"loginResult\"")
                        && payload.contains("\"success\":true")
                        && payload.contains("\"userId\":\"alice\"")
                        && payload.contains("\"nickName\":\"Alice\""));
        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"matchSuccess\"")
                        && payload.contains("\"opponentId\":\"bob\"")
                        && payload.contains("\"opponentNickname\":\"Bob\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"matchSuccess\"")
                        && payload.contains("\"opponentId\":\"alice\"")
                        && payload.contains("\"opponentNickname\":\"Alice\""));
    }

    @Test
    void rejectsStartMatchBeforeLogin() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");

        handler.afterConnectionEstablished(sessionA);
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"error\"")
                        && payload.contains("\"code\":3002"));
    }

    @Test
    void startsRoomAfterBothPlayersMatchAndReady() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);

        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"matchSuccess\"")
                        && payload.contains("\"roomId\":\"room-1\"")
                        && payload.contains("\"opponentId\":\"B\"")
                        && payload.contains("\"opponentNickname\":\"B\""));
        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameStart\""));
        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"redPlayerId\":\"A\"")
                        && payload.contains("\"blackPlayerId\":\"B\"")
                        && payload.contains("\"yourColor\":\"red\"")
                        && payload.contains("\"firstHand\":true"));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"matchSuccess\"")
                        && payload.contains("\"roomId\":\"room-1\"")
                        && payload.contains("\"opponentId\":\"A\"")
                        && payload.contains("\"opponentNickname\":\"A\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"roomInfo\"")
                        && payload.contains("\"opponentReady\":true"));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameStart\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"redPlayerId\":\"A\"")
                        && payload.contains("\"blackPlayerId\":\"B\"")
                        && payload.contains("\"yourColor\":\"black\"")
                        && payload.contains("\"firstHand\":false"));
    }

    @Test
    void broadcastsMoveResultAfterValidMove() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);

        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"moveResult\"")
                        && payload.contains("\"move\":{\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"flip\":false}")
                        && payload.contains("\"valid\":true")
                        && !payload.contains("\"code\"")
                        && !payload.contains("\"message\":\"ok\"")
                        && !payload.contains("\"nextTurn\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"moveResult\"")
                        && payload.contains("\"move\":{\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"flip\":false}")
                        && payload.contains("\"valid\":true")
                        && !payload.contains("\"code\"")
                        && !payload.contains("\"message\":\"ok\"")
                        && !payload.contains("\"nextTurn\""));
    }

    @Test
    void broadcastsGameOverAfterResign() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Resign\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);

        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameOver\"")
                        && payload.contains("\"winner\":\"black\"")
                        && payload.contains("\"winnerId\":\"B\"")
                        && payload.contains("\"reason\":\"resign\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameOver\"")
                        && payload.contains("\"winner\":\"black\"")
                        && payload.contains("\"winnerId\":\"B\"")
                        && payload.contains("\"reason\":\"resign\""));
    }

    @Test
    void returnsRoomNotFoundWhenPlayerMovesWithoutRoom() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");

        handler.afterConnectionEstablished(sessionA);
        register(handler, sessionA, "A", "A");
        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"error\"")
                        && payload.contains("\"code\":3001"));
    }

    @Test
    void schedulesTimeoutAfterBothPlayersReady() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        TimeoutScheduler scheduler = mock(TimeoutScheduler.class);
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore, scheduler);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");
        doReturn(mockScheduledFuture()).when(scheduler)
                .schedule(org.mockito.ArgumentMatchers.any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));

        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(scheduler).schedule(org.mockito.ArgumentMatchers.any(Runnable.class), delayCaptor.capture(),
                eq(TimeUnit.MILLISECONDS));
        assertThat(delayCaptor.getValue()).isBetween(59000L, 60000L);
    }

    @Test
    void reschedulesTimeoutAfterAcceptedMoveAndCancelsPreviousTask() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        TimeoutScheduler scheduler = mock(TimeoutScheduler.class);
        ScheduledFuture<?> firstFuture = mock(ScheduledFuture.class);
        ScheduledFuture<?> secondFuture = mock(ScheduledFuture.class);
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore, scheduler);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");
        doReturn(firstFuture).doReturn(secondFuture).when(scheduler)
                .schedule(org.mockito.ArgumentMatchers.any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        verify(firstFuture).cancel(false);
        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(scheduler, org.mockito.Mockito.times(2))
                .schedule(org.mockito.ArgumentMatchers.any(Runnable.class), delayCaptor.capture(), eq(TimeUnit.MILLISECONDS));
        assertThat(delayCaptor.getAllValues()).allMatch(delay -> delay >= 59000L && delay <= 60000L);
    }

    @Test
    void ignoresStaleTimeoutTaskAfterMoveResetsDeadline() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        RecordingTimeoutScheduler scheduler = new RecordingTimeoutScheduler();
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore, scheduler);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));

        Runnable firstTask = scheduler.tasks().get(0);

        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        firstTask.run();

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .noneMatch(payload -> payload.contains("\"messageType\":\"timeout\""));
    }

    @Test
    void broadcastsPublicInterfaceTimeoutFields() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        RecordingTimeoutScheduler scheduler = new RecordingTimeoutScheduler();
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore, scheduler);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));

        scheduler.tasks().get(0).run();

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"timeout\"")
                        && payload.contains("\"loserId\":\"A\"")
                        && payload.contains("\"winnerId\":\"B\"")
                        && payload.contains("\"reason\":\"timeout\""));
    }

    @Test
    void broadcastsDifferentCapturedPieceVisibilityForHiddenCapture() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");
        installActiveRoom(roomManager);

        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":0,\"toX\":\"a\",\"toY\":3,\"isFlip\":false}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"moveResult\"")
                        && payload.contains("\"capturedPiece\":\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"moveResult\"")
                        && !payload.contains("\"capturedPiece\""));
    }

    @Test
    void returnsErrorWhenPlayerSendsReadyAfterGameAlreadyStarted() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"error\"")
                        && payload.contains("\"code\":5000"));
    }

    @Test
    void returnsErrorWhenPlayerResignsBeforeGameStarts() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        UserStore userStore = new UserStore(dir.resolve("users.json"));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, userStore);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);
        register(handler, sessionA, "A", "A");
        register(handler, sessionB, "B", "B");

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Resign\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"error\"")
                        && payload.contains("\"code\":5000"));
    }

    private static final class RecordingTimeoutScheduler implements TimeoutScheduler {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            tasks.add(command);
            return new NoOpScheduledFuture();
        }

        public List<Runnable> tasks() {
            return tasks;
        }
    }

    private static final class NoOpScheduledFuture implements ScheduledFuture<Object> {
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(60, TimeUnit.SECONDS);
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ScheduledFuture<?> mockScheduledFuture() {
        return mock(ScheduledFuture.class);
    }

    private void installActiveRoom(RoomManager roomManager) throws Exception {
        GameState state = new GameState(
                Board.empty()
                        .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                        .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                        .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN))
                        .put(Position.of("a", 0), Piece.visible("r-a0", ChessColor.RED, PieceType.ROOK))
                        .put(Position.of("a", 3), Piece.hidden("b-a3", ChessColor.BLACK, PieceType.PAWN)),
                ChessColor.RED, GameStatus.PLAYING, FlipPool.initial(new Random(1)), FlipPool.initial(new Random(2)),
                0, 0, Instant.parse("2026-06-30T08:00:00Z"), Instant.parse("2026-06-30T08:01:00Z"), null, null);
        GameRoom room = new GameRoom("room-1", new Player("A", "A", ChessColor.RED), new Player("B", "B", ChessColor.BLACK),
                state, new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        Field activeRoomField = RoomManager.class.getDeclaredField("activeRoom");
        activeRoomField.setAccessible(true);
        activeRoomField.set(roomManager, room);
    }

    private void register(GameWebSocketHandler handler, WebSocketSession session, String userId, String nickName)
            throws Exception {
        handler.handleTextMessage(session, new TextMessage(
                "{\"messageType\":\"register\",\"userId\":\"" + userId + "\",\"passWord\":\"pw\",\"nickName\":\""
                        + nickName + "\"}"));
    }
}
