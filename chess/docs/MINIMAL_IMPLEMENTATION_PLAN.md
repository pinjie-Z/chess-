# Minimal Implementation Plan

## Goal
Build a minimal local two-client Jieqi game that satisfies the assignment requirements without unnecessary backend infrastructure.

## Project Structure

```text
src/main/java/thereia/java/chess
  config
    WebSocketConfig.java
  websocket
    GameWebSocketHandler.java
    SessionManager.java
  protocol
    MessageType.java
    ClientMessage.java
    ServerMessage.java
    MoveMessage.java
    MoveResultMessage.java
    GameStartMessage.java
    GameOverMessage.java
    ErrorMessage.java
  game
    RoomManager.java
    GameRoom.java
    GameState.java
    GameStatus.java
    Player.java
  board
    Board.java
    Position.java
  piece
    Piece.java
    PieceType.java
    ChessColor.java
    FlipPool.java
  move
    Move.java
    MoveRecord.java
    MoveExecutor.java
  rule
    RuleEngine.java
    GameResultChecker.java
  record
    GameRecorder.java

src/main/resources/static
  index.html
  app.js
  style.css
```

## Simple Runtime Flow
1. Player A opens the page and clicks start match.
2. Player B opens another page and clicks start match.
3. Server creates one `GameRoom`, assigns red/black, and sends `gameStart`.
4. Current player clicks source and destination.
5. Client sends a `move` message.
6. Server validates through `RuleEngine`.
7. Server applies through `MoveExecutor`.
8. Server broadcasts `moveResult`.
9. Server checks result through `GameResultChecker`.
10. Game ends by capture king, resign, timeout, or 80 no-capture plies.
11. `GameRecorder` writes a local record file.

## First Version Cut Line
Version 1 must include:

- Two browser clients on localhost.
- Server-side room state.
- Public JSON-style WebSocket protocol.
- Core domain classes.
- Legal move validation.
- Hidden-piece first move and flip.
- Capture, win, resign, timeout, and no-capture draw.
- Local file record.

Version 1 must not include:

- Database.
- Login/register.
- Redis.
- Public network deployment.
- Spectator mode.
- Chat.
- AI.

