package thereia.java.chess.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import thereia.java.chess.game.RoomManager;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.nio.file.Path;

@Configuration
public class GameCoreConfig {

    @Bean
    public RuleEngine ruleEngine() {
        return new RuleEngine();
    }

    @Bean
    public MoveExecutor moveExecutor() {
        return new MoveExecutor();
    }

    @Bean
    public GameRecorder gameRecorder(@Value("${chess.records.dir}") String recordsDir) {
        return new GameRecorder(Path.of(recordsDir));
    }

    @Bean
    public RoomManager roomManager(RuleEngine ruleEngine, MoveExecutor moveExecutor, GameRecorder gameRecorder) {
        return new RoomManager(ruleEngine, moveExecutor, gameRecorder);
    }
}
