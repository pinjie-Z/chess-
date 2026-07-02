package thereia.java.chess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import thereia.java.chess.game.RoomManager;
import thereia.java.chess.websocket.GameWebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChessApplicationTests {

    @Autowired
    private GameWebSocketHandler gameWebSocketHandler;

    @Autowired
    private RoomManager roomManager;

    @Test
    void contextLoads() {
        assertThat(gameWebSocketHandler).isNotNull();
        assertThat(roomManager).isNotNull();
    }

}
