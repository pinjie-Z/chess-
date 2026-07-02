package thereia.java.chess.protocol;

public enum MessageType {
    Login,
    register,
    startMatch,
    cancelMatch,
    Ready,
    move,
    Resign,
    ping,
    loginResult,
    matchSuccess,
    roomInfo,
    gameStart,
    moveResult,
    timeout,
    gameOver,
    pong,
    error
}
