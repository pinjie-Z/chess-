package thereia.java.chess.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class UserStore {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path path;
    private final ConcurrentMap<String, UserAccount> accounts = new ConcurrentHashMap<>();

    public UserStore(Path path) {
        this.path = path;
        load();
    }

    public synchronized Optional<UserAccount> register(String userId, String passWord, String nickName) {
        if (accounts.containsKey(userId)) {
            return Optional.empty();
        }
        UserAccount account = new UserAccount(userId, passWord, nickName);
        accounts.put(userId, account);
        save();
        return Optional.of(account);
    }

    public Optional<UserAccount> login(String userId, String passWord) {
        UserAccount account = accounts.get(userId);
        if (account == null) {
            return Optional.empty();
        }
        if (!account.getPassWord().equals(passWord)) {
            return Optional.empty();
        }
        return Optional.of(account);
    }

    private void load() {
        if (!Files.exists(path)) {
            return;
        }
        try {
            List<UserAccount> loaded = objectMapper.readValue(path.toFile(), new TypeReference<List<UserAccount>>() { });
            for (UserAccount account : loaded) {
                accounts.put(account.getUserId(), account);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to load users", exception);
        }
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), new ArrayList<>(accounts.values()));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to save users", exception);
        }
    }
}
