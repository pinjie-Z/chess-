# Rule Design

## 1. Purpose

This document defines the first-version Jieqi rule model for the Spring Boot server.

The server is authoritative for board state, legal move validation, hidden-piece reveal, timeout, result judgment, and game records. The browser client may help display possible moves, but the client must not be trusted for rule decisions.

This document follows `docs/API_DESIGN.md`. External WebSocket JSON uses the API design's coordinate system and piece names.

## 2. First-Version Scope

Implemented in the first version:

- 9 by 10 board.
- Global protocol coordinates.
- Standard Chinese-chess movement shapes for King, Rook, Knight, Cannon, Pawn, Guard, and Bishop, with Jieqi reveal rules.
- Hidden piece first move uses the original-square movement type.
- Hidden piece is revealed by the server after its first legal move or capture.
- Captures, including hidden captured-piece visibility differences for the two players.
- Kings/generals may not face each other directly after a move.
- Direct capture of King/General ends the game.
- Resign ends the game.
- Server-side per-turn timeout ends the game.
- 40 full rounds without capture, meaning 80 plies without capture, is a draw.
- Local move record data sufficient for game records and debugging.

Reserved for later:

- Long check.
- Long chase.
- Pawn long-check / long-chase special judgment.
- Draw offer and accept/reject flow.
- Full checkmate/stalemate search.
- Multi-room concurrency beyond the simplest one-room flow.

The first version deliberately does not reject every "self-check" or "self-exposure" case. If a player makes a legal-shape move that leaves their own King/General capturable, the move may be accepted; the opponent can then capture the King/General and win. However, a move is still illegal if it leaves the two Kings/Generals directly facing each other with no piece between them.

## 3. Coordinate Model

Protocol coordinates are global server coordinates:

- Columns are `a` through `i`, left to right from Red's point of view.
- Rows are `0` through `9`.
- `e0` is the Red King starting position.
- `e9` is the Black King starting position.
- Red is the first-hand side and starts at the bottom.
- Black starts at the top.

Internal board storage:

```text
cells[row][col]
row = 9 - y
col = x - 'a'
y = 9 - row
x = (char) ('a' + col)
```

Therefore internal `row = 0` is protocol row `9` and internal `row = 9` is protocol row `0`.

All move validation should operate on a `Position` value object rather than raw strings. Parsing from JSON should validate:

- `x` is one character from `a` to `i`.
- `y` is an integer from `0` to `9`.
- Source and destination are different.

## 4. Initial Board

Initial occupied squares follow Chinese chess starting positions.

Red side:

| Position | Original type |
|---|---|
| `a0`, `i0` | Rook |
| `b0`, `h0` | Knight |
| `c0`, `g0` | Bishop |
| `d0`, `f0` | Guard |
| `e0` | King |
| `b2`, `h2` | Cannon |
| `a3`, `c3`, `e3`, `g3`, `i3` | Pawn |

Black side:

| Position | Original type |
|---|---|
| `a9`, `i9` | Rook |
| `b9`, `h9` | Knight |
| `c9`, `g9` | Bishop |
| `d9`, `f9` | Guard |
| `e9` | King |
| `b7`, `h7` | Cannon |
| `a6`, `c6`, `e6`, `g6`, `i6` | Pawn |

Only the two Kings are visible initially. All other pieces are hidden.

For hidden pieces:

- `originalType` is the movement type of the starting square.
- `revealedType` is unknown until the first successful move or capture by that piece.
- Before reveal, movement validation uses `originalType`.
- After reveal, movement validation uses `revealedType`.

## 5. Domain Objects

The implementation should keep game rules out of the WebSocket handler. The expected domain/rule objects are:

| Object | Responsibility |
|---|---|
| `Position` | Immutable board coordinate, parse/format/validate `x,y`. |
| `PieceColor` | `RED` or `BLACK`; provides opponent lookup and forward direction. |
| `PieceType` | `KING`, `ROOK`, `KNIGHT`, `CANNON`, `PAWN`, `GUARD`, `BISHOP`. |
| `Piece` | Color, original type, optional revealed type, unique id. Visibility is derived from whether revealed type is present. |
| `Board` | 10 by 9 cells, piece lookup, move application, path/blocker helpers. |
| `Move` | Source, destination, mover id/color, server receive time, client `isFlip` hint. |
| `MoveValidationResult` | Legal/illegal result, error code, message, capture/reveal metadata. |
| `GameState` | Board, current turn, status, timers, no-capture ply count, move number. |
| `FlipPool` | Remaining hidden real types per side; server-side random reveal. |
| `MoveRecord` | Persisted record of accepted moves, reveal result, capture result, timestamp, outcome. |
| `RuleEngine` | Validates a move against `GameState` and returns a result without WebSocket concerns. |

This exceeds the assignment's five-domain-class requirement while keeping boundaries clear.

## 6. Flip Pool

Each side has one `FlipPool` containing the 15 non-King real piece types:

```text
Rook x2
Knight x2
Cannon x2
Bishop x2
Guard x2
Pawn x5
```

The King is not in the flip pool because both Kings start visible.

When a hidden piece completes its first successful move or capture:

1. The server draws one random type from that side's remaining pool.
2. The moved piece becomes visible.
3. The moved piece's `revealedType` is set to the drawn type.
4. The drawn type is removed from the pool.
5. The server sends that type as `flipResult` in `moveResult`.

If a hidden piece is captured before moving:

- It is removed from the board.
- Its real type should still be determined by that side's hidden pool so the capturing side can know what was captured.
- The captured type is removed from the captured side's pool.
- The capturing side receives `capturedPiece` as the real type.
- The captured side receives `capturedPiece: "NULL"`.

If a revealed piece is captured, both players may receive the revealed type.

## 7. Legal Move Validation

Validation order:

1. Parse and validate source/destination coordinates.
2. Ensure game status is `PLAYING`.
3. Ensure sender belongs to the game room.
4. Ensure it is sender's turn.
5. Ensure source contains a piece.
6. Ensure source piece belongs to sender.
7. Ensure destination is not occupied by sender's own piece.
8. Determine movement type:
   - Hidden piece: `originalType`.
   - Visible piece: `revealedType`.
9. Validate movement shape and blockers for that type.
10. Simulate the move and reject it if the two Kings/Generals face each other directly.
11. Apply capture, reveal, turn switch, timer reset, no-capture counter, and game-over checks.

Illegal moves do not change board state, turn, timers, flip pools, or records.

## 8. Piece Movement Rules

### 8.1 King

King moves one orthogonal step.

For the first version, King stays inside the palace:

- Red palace: columns `d`, `e`, `f`; rows `0`, `1`, `2`.
- Black palace: columns `d`, `e`, `f`; rows `7`, `8`, `9`.

The two Kings/Generals must not face each other on the same file with no piece between them after any accepted move.

### 8.2 Rook

Rook moves any number of squares horizontally or vertically. All squares between source and destination must be empty.

### 8.3 Knight

Knight moves in a Chinese-chess horse shape:

- Absolute delta `(1, 2)` or `(2, 1)`.
- The horse-leg square must be empty.

Horse-leg rule:

- If moving two rows and one column, the adjacent square in the row direction must be empty.
- If moving two columns and one row, the adjacent square in the column direction must be empty.

### 8.4 Cannon

Cannon moves horizontally or vertically.

For a non-capture move:

- Source and destination must be on the same row or file.
- There must be zero pieces between source and destination.
- Destination must be empty.

For a capture move:

- Source and destination must be on the same row or file.
- Destination must contain an enemy piece.
- There must be exactly one piece between source and destination.

### 8.5 Pawn

Pawn moves one square.

Red moves forward toward increasing `y`; Black moves forward toward decreasing `y`.

Before crossing the river:

- Red before crossing: `y <= 4`.
- Black before crossing: `y >= 5`.
- Only one forward step is allowed.

After crossing the river:

- One forward step is allowed.
- One horizontal step is allowed.
- Backward movement is never allowed.

### 8.6 Guard

Guard moves one diagonal step.

Jieqi strengthening rule:

- Hidden Guard first move uses original-square Guard movement and palace limit.
- Revealed Guard may leave the palace.

Therefore:

- If movement type comes from `originalType` while hidden, palace limit applies.
- If movement type comes from `revealedType` after reveal, palace limit does not apply.

### 8.7 Bishop

Bishop moves exactly two diagonal squares.

The elephant-eye square halfway between source and destination must be empty.

Jieqi strengthening rule:

- Hidden Bishop first move uses original-square Bishop movement and river limit.
- Revealed Bishop may cross the river.

Therefore:

- If movement type comes from `originalType` while hidden, river limit applies.
- If movement type comes from `revealedType` after reveal, river limit does not apply.

## 9. Move Application

For an accepted move:

1. Remove captured piece from destination, if any.
2. Move source piece to destination.
3. If the moved piece was hidden, reveal it from its side's `FlipPool`.
4. If a hidden enemy piece was captured, reveal/remove its real type from its side's `FlipPool` for record and captured-piece notification.
5. Update no-capture ply count:
   - Reset to `0` after any capture.
   - Otherwise increment by `1`.
6. Append `MoveRecord`.
7. Check game-over conditions.
8. Switch turn if the game continues.
9. Reset the next player's server-side turn deadline.

The client-provided `isFlip` field is ignored for authority. The server decides whether a reveal happened.

## 10. Result Rules

First-version game endings:

| Condition | Result |
|---|---|
| A King/General is captured | Capturing side wins. External `gameOver.reason` is `checkmate`. |
| Player sends `Resign` | Opponent wins. External `gameOver.reason` is `resign`. |
| Player exceeds server turn deadline | Opponent wins. Send `timeout`. |
| No capture for 80 plies | Draw in internal state and game record. If the API has no public draw reason, expose it through a project extension or final record first. |

The first version does not need full checkmate/stalemate search. Direct King/General capture is enough for the minimal playable loop.

## 11. Timeout

Default per-turn time is 60 seconds.

The server stores:

- `turnStartedAt`.
- `turnDeadlineAt`.
- current player color.

The server must use its own clock. Client timestamps are display-only and must not decide timeout.

When timeout occurs:

- The game status becomes ended.
- The timeout message is broadcast to both players.
- No later move is accepted for that game.

## 12. Game Records

The local record should be append-only JSON lines or one JSON file per game. Exact file format can be decided in implementation design, but each accepted move record should contain:

- `roomId`.
- move index.
- color.
- source and destination.
- movement type used for validation.
- whether the moved piece was hidden before move.
- `flipResult`, if any.
- captured piece type for server record, if any.
- public captured-piece values sent to each side, if different.
- server timestamp.
- no-capture ply count after the move.
- game result after the move, if ended.

Records may contain full hidden information because they are server-local files, not messages sent to clients.

## 13. Testing Targets

Rule implementation should be covered with focused unit tests before WebSocket integration:

- Coordinate parse and array conversion.
- Initial board setup.
- Rook path blocking.
- Knight horse-leg blocking.
- Cannon screen count for move/capture.
- Pawn river movement for both colors.
- Guard palace limit before reveal and no palace limit after reveal.
- Bishop elephant-eye blocking and river limit before reveal.
- Hidden first move uses original type.
- Reveal removes one type from `FlipPool`.
- Captured hidden piece visibility differs by receiver.
- Facing Kings/Generals move is rejected.
- King capture ends the game.
- No-capture counter resets/increments.
