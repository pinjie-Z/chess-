# API Draft

## Transport
- WebSocket.
- Planned local URL: `ws://localhost:8887`.
- Static page URL: `http://localhost:8887/index.html`.
- Message body format: JSON.
- Every message has `messageType`.

## Client To Server

### startMatch

```json
{
  "messageType": "startMatch"
}
```

### Ready

```json
{
  "messageType": "Ready"
}
```

### move

Use public-interface-style fields:

```json
{
  "messageType": "move",
  "fromX": "b",
  "fromY": 3,
  "toX": "b",
  "toY": 4,
  "isFlip": true
}
```

Notes:

- `fromX` and `toX` are `a` through `i`.
- `fromY` and `toY` are `0` through `9`.
- Client may send `isFlip`; server remains authoritative.

### Resign

```json
{
  "messageType": "Resign"
}
```

## Server To Client

### matchSuccess

```json
{
  "messageType": "matchSuccess",
  "roomId": "room-1",
  "opponentId": "player-2",
  "opponentNickname": "Player 2"
}
```

### roomInfo

```json
{
  "messageType": "roomInfo",
  "opponentReady": true
}
```

### gameStart

```json
{
  "messageType": "gameStart",
  "redPlayerId": "player-1",
  "blackPlayerId": "player-2",
  "yourColor": "red",
  "firstHand": true,
  "initialBoard": []
}
```

### moveResult

```json
{
  "messageType": "moveResult",
  "success": true,
  "valid": true,
  "move": {
    "fromX": "b",
    "fromY": 3,
    "toX": "b",
    "toY": 4,
    "isFlip": true
  },
  "flipResult": "Rook"
}
```

### gameOver

```json
{
  "messageType": "gameOver",
  "winner": "red",
  "reason": "checkmate",
  "winnerId": "player-1"
}
```

### timeout

```json
{
  "messageType": "timeout",
  "loserId": "player-1",
  "winnerId": "player-2",
  "reason": "timeout"
}
```

### error

```json
{
  "messageType": "error",
  "code": 2001,
  "message": "illegal move"
}
```

## Piece Type Enum

Use public names:

```text
Rook
Knight
Cannon
Bishop
Guard
King
Pawn
NULL
```

`NULL` is used when a hidden captured piece type must not be revealed to a receiver.
