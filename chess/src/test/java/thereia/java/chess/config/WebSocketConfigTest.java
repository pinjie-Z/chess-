package thereia.java.chess.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import thereia.java.chess.auth.UserStore;
import thereia.java.chess.game.RoomManager;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;
import thereia.java.chess.websocket.GameWebSocketHandler;
import thereia.java.chess.websocket.SessionRegistry;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class WebSocketConfigTest {

    @TempDir
    Path dir;

    @Test
    void registersOnlyRootWebSocketPath() {
        GameWebSocketHandler handler = new GameWebSocketHandler(new SessionRegistry(),
                new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir)),
                new UserStore(dir.resolve("users.json")));
        WebSocketConfig config = new WebSocketConfig(handler);
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        ArgumentCaptor<String[]> pathsCaptor = ArgumentCaptor.forClass(String[].class);

        doReturn(registration).when(registry).addHandler(any(), any(String[].class));
        doReturn(registration).when(registration).setAllowedOrigins("*");

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(org.mockito.ArgumentMatchers.same(handler), pathsCaptor.capture());
        assertThat(pathsCaptor.getValue()).containsExactly("/");
    }
}
