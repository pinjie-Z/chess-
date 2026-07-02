package thereia.java.chess.websocket;

import thereia.java.chess.auth.UserAccount;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class SessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UserAccount> loggedInUsers = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    public void add(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
        UserAccount removed = loggedInUsers.remove(sessionId);
        if (removed != null) {
            userSessions.remove(removed.getUserId());
        }
    }

    public Optional<WebSocketSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void bindUser(String sessionId, UserAccount account) {
        loggedInUsers.put(sessionId, account);
        userSessions.put(account.getUserId(), sessionId);
    }

    public Optional<UserAccount> userOf(String sessionId) {
        return Optional.ofNullable(loggedInUsers.get(sessionId));
    }

    public Optional<WebSocketSession> findByUserId(String userId) {
        String sessionId = userSessions.get(userId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return find(sessionId);
    }
}
