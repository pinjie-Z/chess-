# Jieqi First Version Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first playable Jieqi assignment version: two browser clients connect to one Spring Boot server through WebSocket JSON, with server-authoritative rules, reveal logic, timing, and local records.

**Architecture:** Implement the pure game/rule core first, covered by unit tests, then connect it to protocol DTOs and a WebSocket room flow. Keep WebSocket handling thin; `RuleEngine`, `GameState`, `Board`, and `FlipPool` own game behavior.

**Tech Stack:** Java 17, Spring Boot 4.1.0, Spring WebSocket, JUnit 5, Jackson from Spring Boot, static HTML/CSS/JS under `src/main/resources/static`.

---

## File Structure

Create these packages under `src/main/java/thereia/java/chess`:

```text
config/
  WebSocketConfig.java

websocket/
  GameWebSocketHandler.java
  SessionRegistry.java

protocol/
  MessageType.java
  ClientMoveMessage.java
  ErrorMessage.java
  GameOverMessage.java
  GameStartMessage.java
  MatchSuccessMessage.java
  MoveResultMessage.java
  TimeoutMessage.java

game/
  GameRoom.java
  GameState.java
  GameStatus.java
  Player.java
  RoomManager.java

board/
  Board.java
  Position.java

piece/
  ChessColor.java
  Piece.java
  PieceType.java
  FlipPool.java

move/
  Move.java
  MoveRecord.java
  MoveValidationResult.java

rule/
  RuleEngine.java
  MoveExecutor.java
  GameResultChecker.java

record/
  GameRecorder.java
```

Create tests under matching packages in `src/test/java/thereia/java/chess`.

Static frontend files:

```text
src/main/resources/static/index.html
src/main/resources/static/app.js
src/main/resources/static/style.css
```

---

## Task 1: Runtime Configuration

**Files:**
- Modify: `src/main/resources/application.properties`
- Test: `src/test/java/thereia/java/chess/ChessApplicationTests.java`

- [ ] **Step 1: Configure server port and record path**

Set `application.properties` to:

```properties
spring.application.name=chess
server.port=8887
chess.records.dir=records
```

- [ ] **Step 2: Run the existing context test**

Run:

```bash
mvn -q -Dtest=ChessApplicationTests test
```

Expected: exit code `0`, with `Tests run: 1`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.properties src/test/java/thereia/java/chess/ChessApplicationTests.java
git commit -m "chore: configure server runtime"
```

---

## Task 2: Core Coordinate And Piece Model

**Files:**
- Create: `src/main/java/thereia/java/chess/board/Position.java`
- Create: `src/main/java/thereia/java/chess/piece/ChessColor.java`
- Create: `src/main/java/thereia/java/chess/piece/PieceType.java`
- Create: `src/main/java/thereia/java/chess/piece/Piece.java`
- Test: `src/test/java/thereia/java/chess/board/PositionTest.java`
- Test: `src/test/java/thereia/java/chess/piece/PieceTest.java`

- [ ] **Step 1: Write failing coordinate tests**

`PositionTest` must cover:

```java
@Test
void convertsProtocolCoordinateToArrayIndex() {
    Position redKing = Position.of("e", 0);
    assertThat(redKing.row()).isEqualTo(9);
    assertThat(redKing.col()).isEqualTo(4);
    assertThat(redKing.x()).isEqualTo("e");
    assertThat(redKing.y()).isEqualTo(0);

    Position blackKing = Position.of("e", 9);
    assertThat(blackKing.row()).isEqualTo(0);
    assertThat(blackKing.col()).isEqualTo(4);
}

@Test
void rejectsInvalidCoordinate() {
    assertThatThrownBy(() -> Position.of("j", 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Position.of("a", 10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Position.of("", 0)).isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Implement `Position`**

Required API:

```java
Position.of(String x, int y)
Position.fromArrayIndex(int row, int col)
Position.row()
Position.col()
Position.deltaX(Position other)
Position.deltaY(Position other)
```

Behavior: `of` validates `x` and `y`; `fromArrayIndex` validates `row` and `col`; `row` returns `9 - y`; `col` returns `x.charAt(0) - 'a'`.

- [ ] **Step 3: Write failing piece tests**

`PieceTest` must verify hidden and revealed movement type:

```java
@Test
void hiddenPieceUsesOriginalTypeUntilRevealed() {
    Piece piece = Piece.hidden("r-a0", ChessColor.RED, PieceType.ROOK);
    assertThat(piece.visible()).isFalse();
    assertThat(piece.movementType()).isEqualTo(PieceType.ROOK);

    Piece revealed = piece.reveal(PieceType.PAWN);
    assertThat(revealed.visible()).isTrue();
    assertThat(revealed.revealedType()).contains(PieceType.PAWN);
    assertThat(revealed.movementType()).isEqualTo(PieceType.PAWN);
}
```

- [ ] **Step 4: Implement enums and `Piece`**

Required APIs:

```java
ChessColor.RED
ChessColor.BLACK
ChessColor.opponent()
ChessColor.forwardDy()

PieceType.KING
PieceType.ROOK
PieceType.KNIGHT
PieceType.CANNON
PieceType.PAWN
PieceType.GUARD
PieceType.BISHOP

Piece.visible(String id, ChessColor color, PieceType type)
Piece.hidden(String id, ChessColor color, PieceType originalType)
Piece.movementType()
Piece.revealedType()
Piece.reveal(PieceType type)
```

Behavior: `ChessColor.RED.forwardDy()` returns `1`; `ChessColor.BLACK.forwardDy()` returns `-1`; `Piece.movementType()` returns `originalType` while hidden and `revealedType` after reveal; `reveal` returns a visible piece without mutating the original.

- [ ] **Step 5: Run tests**

```bash
mvn -q -Dtest=PositionTest,PieceTest test
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/thereia/java/chess/board src/main/java/thereia/java/chess/piece src/test/java/thereia/java/chess/board src/test/java/thereia/java/chess/piece
git commit -m "feat: add core coordinate and piece model"
```

---

## Task 3: Board Initialization And Helpers

**Files:**
- Create: `src/main/java/thereia/java/chess/board/Board.java`
- Test: `src/test/java/thereia/java/chess/board/BoardTest.java`

- [ ] **Step 1: Write failing initial-board tests**

Cover:

```java
@Test
void createsInitialJieqiBoardWithOnlyKingsVisible() {
    Board board = Board.initial();
    assertThat(board.pieceAt(Position.of("e", 0))).get().extracting(Piece::visible).isEqualTo(true);
    assertThat(board.pieceAt(Position.of("e", 9))).get().extracting(Piece::visible).isEqualTo(true);
    assertThat(board.pieceAt(Position.of("a", 0))).get().extracting(Piece::visible).isEqualTo(false);
    assertThat(board.pieceAt(Position.of("a", 0))).get().extracting(Piece::originalType).isEqualTo(PieceType.ROOK);
    assertThat(board.occupiedCount()).isEqualTo(32);
}
```

- [ ] **Step 2: Implement `Board.initial()`**

Required methods:

```java
Board.empty()
Board.initial()
Board.pieceAt(Position position)
Board.isEmpty(Position position)
Board.occupiedCount()
Board.countBetween(Position from, Position to)
Board.move(Position from, Position to, Piece movedPiece)
Board.remove(Position position)
Board.put(Position position, Piece piece)
```

Use immutable copy-on-write board updates for tests and simulation.

- [ ] **Step 3: Write failing path-helper tests**

Cover rook/cannon path counts:

```java
@Test
void countsPiecesBetweenSameFileOrRank() {
    Board board = Board.empty()
        .put(Position.of("a", 0), Piece.hidden("r-a0", ChessColor.RED, PieceType.ROOK))
        .put(Position.of("a", 3), Piece.hidden("r-a3", ChessColor.RED, PieceType.PAWN))
        .put(Position.of("a", 6), Piece.hidden("b-a6", ChessColor.BLACK, PieceType.PAWN));
    assertThat(board.countBetween(Position.of("a", 0), Position.of("a", 6))).isEqualTo(1);
}
```

- [ ] **Step 4: Run tests and commit**

```bash
mvn -q -Dtest=BoardTest test
git add src/main/java/thereia/java/chess/board src/test/java/thereia/java/chess/board
git commit -m "feat: initialize jieqi board"
```

---

## Task 4: Flip Pool

**Files:**
- Create: `src/main/java/thereia/java/chess/piece/FlipPool.java`
- Test: `src/test/java/thereia/java/chess/piece/FlipPoolTest.java`

- [ ] **Step 1: Write failing flip-pool tests**

Cover deterministic draw by injecting `Random`:

```java
@Test
void initialPoolHasFifteenNonKingPieces() {
    FlipPool pool = FlipPool.initial(new Random(0));
    assertThat(pool.remainingCount()).isEqualTo(15);
    assertThat(pool.remainingTypes()).doesNotContain(PieceType.KING);
}

@Test
void drawRemovesOneType() {
    FlipPool pool = FlipPool.initial(new Random(0));
    PieceType drawn = pool.draw();
    assertThat(drawn).isNotEqualTo(PieceType.KING);
    assertThat(pool.remainingCount()).isEqualTo(14);
}
```

- [ ] **Step 2: Implement `FlipPool`**

Required API:

```java
FlipPool.initial(Random random)
FlipPool.draw()
FlipPool.remainingCount()
FlipPool.remainingTypes()
```

Behavior: `initial` contains exactly 15 non-King types; `draw` removes and returns one random remaining type; drawing from an empty pool throws `IllegalStateException`.

- [ ] **Step 3: Run tests and commit**

```bash
mvn -q -Dtest=FlipPoolTest test
git add src/main/java/thereia/java/chess/piece/FlipPool.java src/test/java/thereia/java/chess/piece/FlipPoolTest.java
git commit -m "feat: add hidden piece flip pool"
```

---

## Task 5: Rule Engine Movement Validation

**Files:**
- Create: `src/main/java/thereia/java/chess/move/Move.java`
- Create: `src/main/java/thereia/java/chess/move/MoveValidationResult.java`
- Create: `src/main/java/thereia/java/chess/rule/RuleEngine.java`
- Test: `src/test/java/thereia/java/chess/rule/RuleEngineTest.java`

- [ ] **Step 1: Write failing rule tests**

Cover these exact test methods:

```java
@Test void rookRequiresClearPath()
@Test void knightRequiresHorseLegClear()
@Test void cannonMovesWithoutScreenAndCapturesWithOneScreen()
@Test void redAndBlackPawnMoveForwardBeforeRiver()
@Test void pawnCanMoveSidewaysOnlyAfterRiver()
@Test void hiddenGuardIsPalaceBoundBeforeReveal()
@Test void revealedGuardCanLeavePalace()
@Test void hiddenBishopCannotCrossRiverBeforeReveal()
@Test void revealedBishopCanCrossRiver()
@Test void facingKingsIsRejected()
```

Each test should build a small custom `Board.empty()` setup so failures point to one rule.

- [ ] **Step 2: Implement value classes**

Required APIs:

```java
public record Move(Position from, Position to, boolean flipHint) { }

public record MoveValidationResult(boolean valid, int code, String message) { }
```

Factories: `MoveValidationResult.valid()` returns `valid=true`, `code=0`, and message `ok`; `MoveValidationResult.illegal(String message)` returns `valid=false`, `code=2001`, and the provided message.

- [ ] **Step 3: Implement `RuleEngine` shape validation**

Required API:

```java
RuleEngine.validate(Board board, Move move, ChessColor moverColor)
```

Validation must use the order from `RULE_DESIGN.md` for board-level checks:

```text
source exists
source belongs to mover
destination is not own piece
movement shape and blockers
facing kings after simulated move
```

- [ ] **Step 4: Run focused tests**

```bash
mvn -q -Dtest=RuleEngineTest test
```

Expected: all movement tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/thereia/java/chess/move src/main/java/thereia/java/chess/rule src/test/java/thereia/java/chess/rule
git commit -m "feat: validate jieqi movement rules"
```

---

## Task 6: Game State, Move Execution, And Results

**Files:**
- Create: `src/main/java/thereia/java/chess/game/GameStatus.java`
- Create: `src/main/java/thereia/java/chess/game/GameState.java`
- Create: `src/main/java/thereia/java/chess/move/MoveRecord.java`
- Create: `src/main/java/thereia/java/chess/rule/MoveExecutor.java`
- Create: `src/main/java/thereia/java/chess/rule/GameResultChecker.java`
- Test: `src/test/java/thereia/java/chess/rule/MoveExecutorTest.java`

- [ ] **Step 1: Write failing execution tests**

Cover:

```java
@Test void hiddenMoverRevealsAfterSuccessfulMove()
@Test void hiddenCapturedPieceIsDrawnFromCapturedSidePool()
@Test void noCapturePlyCountIncrementsAndResets()
@Test void capturingKingEndsGameAsCheckmate()
@Test void illegalMoveDoesNotChangeState()
```

- [ ] **Step 2: Implement state records**

Required minimum APIs:

```java
public enum GameStatus { WAITING, PLAYING, ENDED }

public record GameState(
    Board board,
    ChessColor currentTurn,
    GameStatus status,
    FlipPool redPool,
    FlipPool blackPool,
    int noCapturePlyCount,
    int moveNumber,
    Instant turnStartedAt,
    Instant turnDeadlineAt,
    String winnerColor,
    String endReason
) { }

public record MoveRecord(
    int moveNumber,
    ChessColor color,
    Position from,
    Position to,
    PieceType movementType,
    PieceType flipResult,
    PieceType capturedPiece,
    Instant serverTime,
    int noCapturePlyCountAfterMove,
    String endReason
) { }
```

- [ ] **Step 3: Implement executor**

Required API:

```java
MoveExecutor.apply(GameState state, Move move, ChessColor moverColor, Instant now)

public record MoveExecution(
    boolean success,
    MoveValidationResult validation,
    GameState state,
    MoveRecord record,
    PieceType flipResult,
    PieceType capturedPiece
) { }
```

- [ ] **Step 4: Run focused tests and commit**

```bash
mvn -q -Dtest=MoveExecutorTest test
git add src/main/java/thereia/java/chess/game src/main/java/thereia/java/chess/rule src/main/java/thereia/java/chess/move src/test/java/thereia/java/chess/rule
git commit -m "feat: apply moves and game results"
```

---

## Task 7: Local Game Recorder

**Files:**
- Create: `src/main/java/thereia/java/chess/record/GameRecorder.java`
- Test: `src/test/java/thereia/java/chess/record/GameRecorderTest.java`

- [ ] **Step 1: Write failing recorder test**

Use `@TempDir` and verify one JSON line is written:

```java
@Test
void appendsMoveRecordAsJsonLine(@TempDir Path dir) throws IOException {
    GameRecorder recorder = new GameRecorder(dir);
    MoveRecord record = new MoveRecord(1, ChessColor.RED, Position.of("a", 0), Position.of("a", 1),
        PieceType.ROOK, PieceType.PAWN, null, Instant.parse("2026-06-26T00:00:00Z"), 1, null);

    recorder.append("room-1", record);

    Path file = dir.resolve("room-1.jsonl");
    assertThat(Files.readString(file)).contains("\"moveNumber\":1");
    assertThat(Files.readString(file)).contains("\"from\"");
}
```

- [ ] **Step 2: Implement `GameRecorder` with Jackson**

Required API:

```java
new GameRecorder(Path recordsDir)
GameRecorder.append(String roomId, MoveRecord record)
```

Create directories automatically and append UTF-8 without BOM.

- [ ] **Step 3: Run focused tests and commit**

```bash
mvn -q -Dtest=GameRecorderTest test
git add src/main/java/thereia/java/chess/record src/test/java/thereia/java/chess/record
git commit -m "feat: record accepted moves locally"
```

---

## Task 8: Protocol DTOs

**Files:**
- Create: all files under `src/main/java/thereia/java/chess/protocol/`
- Test: `src/test/java/thereia/java/chess/protocol/ProtocolSerializationTest.java`

- [ ] **Step 1: Write serialization tests**

Cover:

```java
@Test void parsesMoveMessageWithPublicFields()
@Test void serializesGameStartWithInitialBoard()
@Test void serializesMoveResultWithFlipAndCapturedPiece()
@Test void serializesErrorMessageWithBusinessCode()
```

JSON field names must match `docs/API_DESIGN.md` exactly: `messageType`, `fromX`, `fromY`, `toX`, `toY`, `isFlip`, `flipResult`, `capturedPiece`, `nextTurn`.

- [ ] **Step 2: Implement DTOs as records**

Use public-interface message names:

```java
public enum MessageType {
    startMatch, Ready, move, Resign,
    matchSuccess, gameStart, moveResult, timeout, gameOver, error
}
```

Each server record should expose `messageType` as a string field matching the API design.

- [ ] **Step 3: Run tests and commit**

```bash
mvn -q -Dtest=ProtocolSerializationTest test
git add src/main/java/thereia/java/chess/protocol src/test/java/thereia/java/chess/protocol
git commit -m "feat: add websocket protocol messages"
```

---

## Task 9: Room Manager And WebSocket Handler

**Files:**
- Create: `src/main/java/thereia/java/chess/game/Player.java`
- Create: `src/main/java/thereia/java/chess/game/GameRoom.java`
- Create: `src/main/java/thereia/java/chess/game/RoomManager.java`
- Create: `src/main/java/thereia/java/chess/websocket/SessionRegistry.java`
- Create: `src/main/java/thereia/java/chess/websocket/GameWebSocketHandler.java`
- Create: `src/main/java/thereia/java/chess/config/WebSocketConfig.java`
- Test: `src/test/java/thereia/java/chess/game/RoomManagerTest.java`

- [ ] **Step 1: Write failing room tests**

Cover:

```java
@Test void firstPlayerWaitsAndSecondCreatesRoom()
@Test void firstJoinedPlayerIsRed()
@Test void moveFromWrongTurnIsRejected()
@Test void resignEndsGame()
```

- [ ] **Step 2: Implement room model**

Required APIs:

```java
public record Player(String playerId, ChessColor color) { }

public final class GameRoom {
    public String roomId();
    public Player redPlayer();
    public Player blackPlayer();
    public GameState state();
    public RoomMoveResult handleMove(String playerId, Move move, Instant now);
    public GameOverMessage resign(String playerId);
}

public final class RoomManager {
    public MatchResult startMatch(String sessionId);
    public Optional<GameRoom> roomForPlayer(String sessionId);
}
```

- [ ] **Step 3: Implement WebSocket config**

Register the same handler for both root and project-local alias:

```java
registry.addHandler(gameWebSocketHandler, "/", "/ws/game").setAllowedOrigins("*");
```

- [ ] **Step 4: Implement handler message flow**

The handler must:

```text
on startMatch -> pair players -> send matchSuccess and enter preparing state
on Ready -> mark that player ready; when both players are ready, send gameStart and enter PLAYING
on move -> parse, call GameRoom.handleMove, send moveResult
on Resign -> end room and broadcast gameOver
on unknown messageType -> send error 4002
on malformed JSON -> send error 4001
```

Move-specific failures use `moveResult success=false valid=false`.

- [ ] **Step 5: Run tests and context test**

```bash
mvn -q -Dtest=RoomManagerTest,ChessApplicationTests test
```

Expected: all tests pass and Spring context starts.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/thereia/java/chess/game src/main/java/thereia/java/chess/websocket src/main/java/thereia/java/chess/config src/test/java/thereia/java/chess/game
git commit -m "feat: connect game rooms to websocket"
```

---

## Task 10: Static Browser Client

**Files:**
- Create: `src/main/resources/static/index.html`
- Create: `src/main/resources/static/app.js`
- Create: `src/main/resources/static/style.css`

- [ ] **Step 1: Build minimal UI**

The first screen must be the playable client, not a landing page. Required controls:

```text
WebSocket URL input default ws://localhost:8887
Connect button
Start Match button
Resign button
Status line
Current color/turn display
9x10 board
Move log
```

- [ ] **Step 2: Implement board rendering**

`app.js` must:

```text
render 9 columns by 10 rows
display occupied hidden pieces as covered pieces
display visible pieces using Chinese labels or English abbreviations
select source on first click
select destination on second click
send move JSON with fromX/fromY/toX/toY/isFlip
apply successful moveResult updates
show errors without closing the socket
```

- [ ] **Step 3: Manual browser smoke test**

Run:

```bash
mvn spring-boot:run
```

Open two browser tabs at:

```text
http://localhost:8887
```

Expected:

```text
Both clients connect.
Both click Start Match.
Both receive gameStart.
Red can move first.
Black cannot move first.
Illegal moves show a failure and do not move pieces.
Resign ends the game.
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static
git commit -m "feat: add minimal browser client"
```

---

## Task 11: End-To-End Verification

**Files:**
- Modify as needed only for defects found during verification.

- [ ] **Step 1: Run all automated tests**

```bash
mvn test
```

Expected: exit code `0`.

- [ ] **Step 2: Run application**

```bash
mvn spring-boot:run
```

Expected:

```text
Tomcat starts on port 8887.
No startup exception.
```

- [ ] **Step 3: Manual two-client game flow**

In two browser tabs:

```text
connect -> startMatch -> red legal move -> black legal move -> illegal move rejection -> resign
```

Expected:

```text
All messages are JSON.
Server is authoritative.
Business errors are JSON, not HTTP 500.
Move records appear under records/.
```

- [ ] **Step 4: Check Git status**

```bash
git status --short
```

Expected: only intended final changes remain, or clean after commit.

- [ ] **Step 5: Commit verification fixes**

```bash
git add .
git commit -m "test: verify first playable jieqi flow"
```

---

## Spec Coverage Check

- API design: covered by Tasks 8 and 9.
- Coordinate mapping: covered by Task 2.
- Initial board: covered by Task 3.
- Domain classes: covered by Tasks 2 through 7 and 9.
- Hidden first move and reveal: covered by Tasks 4, 5, and 6.
- Legal move validation: covered by Task 5.
- Win/loss/draw and timeout state: covered by Task 6 and Task 9.
- Local records: covered by Task 7 and Task 11.
- Browser clients: covered by Task 10.
- No database/login/Redis/chat/spectator/AI: preserved by file structure and task scope.

## Execution Notes

- Do not implement long check, long chase, draw offer, login/register, database, Redis, spectator mode, chat, or AI in this plan.
- Keep commits small and task-based.
- Prefer unit tests for rule behavior before WebSocket integration.
- If a rule in `docs/RULE_DESIGN.md` conflicts with this plan, stop and update the plan before coding.
