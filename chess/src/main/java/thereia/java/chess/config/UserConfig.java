package thereia.java.chess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import thereia.java.chess.auth.UserStore;

import java.nio.file.Path;

@Configuration
public class UserConfig {

    @Bean
    public UserStore userStore() {
        return new UserStore(Path.of("data", "users.json"));
    }
}
