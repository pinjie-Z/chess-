# Project Long Card

## Purpose Of This Card
This card records the confirmed assignment requirements, constraints, planning context, completed discussion, and unresolved decisions for the Jieqi project.

It is not an implementation checklist yet. The project is currently still in discussion/design confirmation.

## Memory Card Rules
- In a new session, read both `PROJECT_SHORT_CARD.md` and `PROJECT_LONG_CARD.md` before project work.
- Read `PROJECT_SHORT_CARD.md` first, then read this long card for full context.
- Use the short card to decide the current task.
- If the short card is empty, stale, unclear, or complete, read this long card and update the short card with the next current task.
- Keep `PROJECT_SHORT_CARD.md` short: only current task, immediate actions, and completion criteria.
- Keep this long card structured: goal, requirements, decisions, completed discussion, unfinished decisions, risks.

## Source Files Rechecked
The current understanding is based on these root-directory files:

- `2026大作业——揭棋.docx`
- `2026大作业公共接口.docx`
- `问题回答.txt`
- `Unvei接口文档- 张恒基.pptx`

The Q&A file resolves several conflicts in the main assignment text and should be treated as authoritative when conflicts appear.

The PPTX is a detailed protocol proposal from another group. It states that the course WebSocket + JSON public interface is the main protocol and its TCP content is an extension. Treat the PPTX as useful supplemental background, not as higher priority than the course public-interface document or the Q&A answers.

## Assignment Goal
Build a Jieqi game program where two clients play a real-person game through a server.

The intended project choice is the "Jieqi game program" path, not the AI-bot competition path.

The implementation should be as simple as possible while satisfying the teacher's required conditions.

## Hard Requirements
- Use AI assistance for analysis, design, programming, and testing.
- Use object-oriented design.
- Include at least 5 domain classes.
- Support two clients playing through a server.
- Server and client should be interoperable with other groups as much as practical.
- Client and server must exchange JSON data.
- Server and client must be able to reject illegal moves.
- Server should record game moves as local game records.
- Use server-side timing as authoritative; client timestamps cannot be trusted.
- Final report must include team member division of labor and contribution percentages.

## Communication Requirements
- Public interface document confirms client-server communication should use JSON.
- Every JSON message should contain `messageType`.
- WebSocket is recommended and acceptable.
- Recommended WebSocket port is `8887`.
- Public interface document has a "运行与测试" section: start the WebSocket server first and verify it reports `ws://localhost:8887` / port `8887`.
- TCP is also possible, but Q&A recommends WebSocket because it avoids custom length-field framing.
- If TCP is used, messages need a 4-byte network-byte-order length prefix, but this project is expected to use WebSocket.
- Single frame should stay under 1 KB, otherwise the server may drop/close the connection.

## Error Handling Strategy
- Treat public-interface `error.code` values as business error codes, not HTTP status codes.
- In WebSocket communication, keep the WebSocket connection normal for ordinary business failures.
- Return business failures as JSON messages instead of letting exceptions become HTTP `500` or framework-level errors.
- Use `moveResult` with `success=false` / `valid=false` for move-specific failures such as illegal move or wrong turn.
- Use `error` messages for protocol-level failures such as bad JSON, unknown `messageType`, or missing room.
- Reuse public-interface error codes where possible:
  - `2001`: illegal move
  - `2002`: not this player's turn
  - `2003`: timeout
  - `3001`: room not found
  - `3002`: match failed
  - `4001`: JSON format error
- Add project-local codes only when needed, e.g. `4002` for unknown message type.

## Public Interface Messages
Public interface document defines these possible client-to-server messages:

- `Login`
- `register`
- `startMatch`
- `cancelMatch` optional
- `requestFirstHand` optional
- `move`
- `ping` optional
- `Resign`
- `Ready`

Public interface document defines these possible server-to-client messages:

- `loginResult`
- `matchSuccess`
- `gameStart`
- `moveResult`
- `timeout`
- `gameOver`
- `pong` optional
- `error` optional
- `roomInfo`

For the simplest version, the likely required subset is:

- Client to server: `startMatch`, `Ready`, `move`, `Resign`
- Server to client: `matchSuccess`, `roomInfo`, `gameStart`, `moveResult`, `timeout`, `gameOver`, `error`

Optional messages may be skipped if they are outside the required room and play flow. `roomInfo` should now be treated as part of the required ready-stage flow.

## Move Data Requirements
The main assignment describes a move as:

- `source`, e.g. `b3`
- `destination`, e.g. `b4`
- `type`, first move includes revealed piece type; later moves may omit or set `null`
- `turnStartTime`, server-side timestamp for timing

The public interface document describes a move as:

- `fromX`
- `fromY`
- `toX`
- `toY`
- `isFlip`

This conflict is not yet fully resolved. Current likely approach: external protocol should follow the public interface where possible for interoperability, while internal domain model can use `Move(source, destination, type, turnStartTime)`.

## Board And Coordinate Requirements
- Board is 9 columns by 10 rows.
- Columns from left to right are `a` through `i`.
- Rows from top to bottom are digits `9` through `0` according to the main assignment text.
- Public interface examples use `x: a-i` and `y: 0-9`.
- The PPTX gives a concrete compatible coordinate convention: display row `0` is red bottom line, display row `9` is black bottom line; internal array can use `row = 9 - displayY`, `col = x - 'a'`.
- First-hand side is at the bottom; clients may display "self at bottom" differently.
- All protocol coordinates should be server-global coordinates, not client-view coordinates. If a client renders "self at bottom", it must convert screen coordinates back to server-global protocol coordinates before sending moves.
- Initial board is known by both client and server; no need to send hidden secret information.

Coordinate mapping needs a deliberate final decision before coding, because the assignment text and public interface examples are easy to mix up.

## Piece Type Requirements
Main assignment integer type mapping:

- `0`: King / General
- `1`: Rook
- `2`: Knight
- `3`: Cannon
- `4`: Pawn
- `5`: Guard
- `6`: Bishop

Public interface piece names:

- `Rook`
- `Knight`
- `Cannon`
- `Bishop`
- `Guard`
- `King`
- `Pawn`

Q&A adds:

- `NULL` string is allowed when a receiver should not know a hidden captured piece's type.

## Jieqi Rule Requirements
- Game is based on Chinese chess.
- Initially, only both kings/generals are visible in their original palace positions.
- Each side's other 15 pieces are hidden and placed on that side's original Chinese-chess starting squares after randomization.
- A hidden piece's first move follows the Chinese-chess identity of the square it currently occupies.
- After a hidden piece moves or captures, the server reveals its real piece type using randomness based on remaining hidden pieces.
- Once revealed, that piece uses its revealed real type for later moves.
- In-place flip is not allowed, despite the main assignment text mentioning it; Q&A explicitly disallows it.
- Captures follow Chinese chess rules.
- Capturing side can know the type of a captured hidden piece.
- Captured side does not know the type of its captured hidden piece; use `NULL`/`null` when needed.
- Guard and Bishop become stronger after reveal: they may leave the palace / cross the river; their basic movement shape remains one diagonal step for Guard and elephant diagonal for Bishop.
- Horse-leg and elephant-eye blocking rules still apply.
- Kings/generals cannot face each other directly.
- The project does not need to handle "self-check is illegal" automatically; if a side exposes its king/general and the opponent can capture it, direct capture can win.

## Win/Loss/Draw Requirements
- Capturing king/general may directly win.
- Checkmate, stalemate/trapped, resign, and timeout are losing conditions in the main rule description.
- Timeout should use server-local time.
- Default move time is 1 minute unless optional custom timing is implemented.
- 40-round no-capture draw means both sides each make 40 moves, total 80 plies.
- Long check / long chase must be continuous; interruption resets counting.
- Pawn long-chasing any other piece draws.
- Pawn long-check loses.
- Draw offer: if opponent accepts, game ends as draw; otherwise continue.

Long check/chase and draw-offer behavior are likely complex and may be treated as optional or later-stage unless needed for grading.

## Scope Allowed By Q&A
- Smallest server only needs to support one game room at the same time.
- Spectator mode is optional.
- Chat is optional.
- Console client is optional.
- Client UI does not need to be beautiful; grading focuses on correct functionality.
- Container delivery is recommended for bonus but not required for core completion.
- Server internals are self-designed; internal class structure does not need to match other groups.
- It is recommended to connect to one protocol server and not share `Board` class with other groups.

## Current Project Direction Under Discussion
Current preferred direction, still subject to final confirmation:

- One Spring Boot server.
- Two browser clients on the same computer for demonstration.
- Communication through WebSocket JSON.
- Server remains authoritative for state, rules, flip randomness, result, timeout, and record.
- No database in first version.
- No login/register in first version unless later required.
- No Redis, Spring Security, JWT, public deployment, spectator mode, chat, or AI in first version.
- Static frontend can live under `src/main/resources/static`.
- For future same-LAN interoperability, the client should not hard-code only `localhost`; the server can later listen on `0.0.0.0:8887`. Public internet deployment is not required for the first version.

Important boundary: same-computer demo is acceptable only if it still uses two separate clients connected to the server. A single local page where two people directly alternate moves without server mediation would not satisfy the intended client-server requirement.

## Agreed Design Sequence
Before implementation, complete design in this order:

1. API design / interface document.
   - WebSocket address.
   - Required message types.
   - JSON examples.
   - Field meanings.
   - Business error codes.
   - Hidden information handling.
2. Rule design.
   - Board and coordinate model.
   - Piece fields.
   - Hidden-piece first-move rule.
   - Legal move validation process.
   - Per-piece movement rules.
   - Win/loss/draw checks.
   - Which complex rules are first-version vs later-version.
3. Implementation design.
   - Package structure.
   - Class responsibilities.
   - First runnable milestone.
4. Code implementation.

## Current Technical Project State
- A Spring Boot Maven project exists under `chess/`.
- Root workspace also contains assignment documents.
- Memory workflow rules exist in root `AGENTS.md` and inner `chess/AGENTS.md`.
- Planning docs exist under `chess/docs/`.
- Implementation has started and core game-rule modules are being built incrementally.
- `docs/API_DESIGN.md` is approved as the first official API design.
- `docs/API_DESIGN_CN.md` is the Chinese review guide for the approved API design.
- `docs/RULE_DESIGN.md` and `docs/RULE_DESIGN_CN.md` are approved for first-version implementation scope.
- Implementation plan exists at `docs/superpowers/plans/2026-06-26-jieqi-first-version.md`.
- User chose inline execution for implementation.
- Task 1 runtime configuration is complete and verified on Spring Boot 3.4.3.
- Task 2 core coordinate and piece model is complete and verified.
- Task 2 through Task 7 have been implemented, verified, and committed.
- Task 8 core room/WebSocket/timeout flow is implemented and verified.
- Current active task is to finish any remaining mandatory public-interface alignment and prepare for frontend integration.

## New Session Handoff
When a new conversation starts, read `docs/PROJECT_SHORT_CARD.md` first. Read `docs/PROJECT_LONG_CARD.md` only if the short card is empty, stale, unclear, contradictory, complete, or the user asks for broader background.

Current status:

- Assignment requirements and source documents have been reviewed.
- Minimal project direction has been agreed: Spring Boot server, browser clients, WebSocket JSON, server-authoritative game state, local file records, no database/Redis/login in first version.
- API design is complete enough to use as the implementation reference.
- Rule design is approved. Implementation plan has been written. User chose inline execution.
- Task 6 and Task 7 are committed.
- Task 8 core flow is complete, including required `Ready` handling and scheduled timeout triggering.
- Next task is to verify the remaining mandatory public-interface details, then handle any disconnect/room-cleanup gaps needed before frontend integration.

Important approved API compatibility notes:

- Default cross-group WebSocket URL is `ws://host:8887`; current version keeps only the root path.
- Core message names follow the public interface: `startMatch`, `Ready`, `move`, `Resign`, `matchSuccess`, `roomInfo`, `gameStart`, `moveResult`, `timeout`, `gameOver`, `error`.
- `matchSuccess` does not include color; color is sent in `gameStart`.
- `roomInfo` is part of the required ready-stage flow and uses `opponentReady`.
- `gameStart.initialBoard` includes occupied initial squares. `color` is an accepted convenience field.
- `moveResult.capturedPiece` is an accepted project extension because the public interface is incomplete about captured hidden-piece visibility.
- External `gameOver.reason` stays compatible with public values: use `checkmate` or `resign`; timeout uses the separate `timeout` message.

## Code Style Decisions
- Use ordinary Java classes instead of Java `record` declarations for readability.
- Lombok is allowed for reducing boilerplate, mainly `@Getter`.
- Do not use `@Data`; avoid broad `@Setter`. Add setters only when mutability is truly needed.
- Use normal JavaBean accessors consistently, such as `getId()` and `isVisible()`.
- Keep internal model validation lightweight; preserve validation for coordinates, protocol boundaries, and real game-rule invariants.

## Completed Discussion
- Compared this project with a larger Spring Boot travel backend and concluded this project should not copy a heavy CRUD architecture.
- Discussed that the main difficulty is game rules and real-time state, not database CRUD.
- Discussed that "domain classes" means classes that model the Jieqi game itself, not framework/config/DTO classes.
- Discussed candidate domain classes: `GameRoom`, `GameState`, `Player`, `Board`, `Piece`, `Position`, `Move`, `MoveRecord`, `FlipPool`.
- Discussed that protocol compatibility matters; public JSON interface should be referenced, not ignored.
- Discussed that the project is still in design/discussion and should not be treated as implementation-ready yet.
- Agreed that API/interface design should happen before code and before detailed implementation.
- Agreed that rule design should happen after API design and before coding.
- Agreed that detailed complex rules such as long check / long chase can be planned with reserved state first, then implemented later if needed.
- Agreed that WebSocket business errors should be returned as JSON business codes rather than becoming HTTP `500` / `401` style responses.
- Added the PPTX as supplemental protocol background and confirmed it reinforces using WebSocket + JSON as the primary protocol, with TCP as an extension rather than our required path.
- Drafted `docs/API_DESIGN.md` for the minimal WebSocket JSON protocol.
- During API review, corrected the public-compatible WebSocket default to root `ws://host:8887` and removed the extra alias path.
- During API review, marked `capturedPiece` as a project extension field for Q&A hidden-capture visibility, not a course public-interface base field.
- During API review, corrected `matchSuccess` to follow the public interface and not include color; color is sent in `gameStart`.
- During API review, clarified that `initialBoard` should include occupied initial squares. For hidden pieces, `piece` is the original-square movement type, not the hidden real identity.
- During final API compatibility review, corrected external `gameOver.reason` for direct king/general capture to `checkmate` instead of project-local `captureKing`, because the public interface only lists `checkmate` and `resign`.
- During final API compatibility review, removed project-local external `gameOver.reason` values such as `timeout`, `drawNoCapture`, `drawAgreed`, and `serverError`; timeout is represented by the public `timeout` message.
- Approved `docs/API_DESIGN.md` as the first official API design for the minimal version, with only the accepted compatibility extensions recorded below.
- Updated inner `chess/AGENTS.md` so new sessions read both short and long project cards.
- Drafted `docs/RULE_DESIGN.md` for the minimal first-version rule scope.
- Added `docs/RULE_DESIGN_CN.md` as the Chinese review version for the user.
- Rule design draft confirms protocol coordinates `a-i` and `0-9`, with internal mapping `row = 9 - y`, `col = x - 'a'`.
- Rule design draft records first-version movement rules, hidden-piece first-move/reveal behavior, flip pool, validation order, timeout, no-capture draw, and local move-record data.
- Rule design draft intentionally postpones long check, long chase, draw offer, and full checkmate/stalemate search.
- Rule design draft clarifies that ordinary self-exposure is not rejected in the first version; the opponent may capture the King/General to win. Facing Kings/Generals directly remains illegal.
- User approved the rule design decisions, including first-version scope, ordinary self-exposure behavior, hidden capture reveal, Guard/Bishop strengthening, and 80-ply no-capture draw.
- Wrote implementation plan `docs/superpowers/plans/2026-06-26-jieqi-first-version.md`.
- Plan order is: runtime config, coordinate/piece model, board, flip pool, rule validation, move execution, recorder, protocol DTOs, WebSocket rooms, static client, end-to-end verification.
- Implementation architecture summary: build pure Java game/rule core first with unit tests, then protocol DTOs, then WebSocket room flow, then static browser client. Keep WebSocket handling thin; domain/rule classes own game behavior.
- Exact package and file list is recorded in `docs/superpowers/plans/2026-06-26-jieqi-first-version.md`, not duplicated in this long card.
- Downgraded Spring Boot project setup from 4.1.0 to 3.4.3-compatible dependencies because local Maven cache/environment successfully verifies that version.
- Task 1 added `server.port=8887` and `chess.records.dir=records`; `ChessApplicationTests` passed with 1 test, 0 failures, 0 errors.
- Task 2 added `Position`, `ChessColor`, `PieceType`, `Piece`, plus `PositionTest` and `PieceTest`.
- Task 2 tests passed with `mvn -q "-Dtest=PositionTest,PieceTest" test` when run through a temporary `subst X:` path, because Java/Maven classpath resolution fails under the project path containing `!`.
- Project-location fix completed: outer root is now `C:\Users\Asus\Downloads\chess-work`, removing the previous Java/Maven classpath issue caused by `!` in the path.
- Task 3 implemented `Board.initial()`, `Board.empty()`, `pieceAt`, `isEmpty`, `occupiedCount`, copy-on-write `put`/`remove`/`move`, and `countBetween`.
- Task 3 added `BoardTest` for initial Jieqi setup, copy-on-write helpers, and same-rank/file path counting.
- Task 3 focused verification passed with `mvn -q "-Dtest=PositionTest,PieceTest,BoardTest" test` when run through a temporary `subst X:` path.
- Task 3 was committed in `97ab08e` with message `完成Board`.
- Task 4 implemented `FlipPool` with one-side 15-piece reveal pool, shuffled initialization, draw-and-remove behavior, remaining count/type queries, and empty-pool failure.
- Task 4 added `FlipPoolTest` for initial type distribution, draw removal, immutable remaining type snapshots, and empty-pool behavior.
- Task 4 focused verification passed with `mvn -q "-Dtest=PositionTest,PieceTest,BoardTest,FlipPoolTest" test` when run through a temporary `subst X:` path.
- Task 4 was committed in `71d7b9f` with message `完成翻子池的构建`.
- Task 5 implemented `RuleEngine.validate` with server-side mover-color checks, source/destination validation, own-piece capture rejection, per-piece movement validation, blockers, hidden-vs-revealed Guard/Bishop restrictions, and facing-Kings rejection.
- Task 5 added `RuleEngineTest` for basic invalid moves, Rook, Cannon, Knight, Pawn, King, Guard, Bishop, and facing-Kings behavior.
- Task 5 focused verification passed with `mvn -q "-Dtest=PositionTest,PieceTest,BoardTest,FlipPoolTest,RuleEngineTest" test` when run through a temporary `subst X:` path.
- Task 5 was committed in `7a96bbc` with message `完成走子规则`.
- Task 6 design decision: `MoveExecutor` should execute moves already accepted by `RuleEngine`; it should not repeat move legality validation. `GameRoom` will call `RuleEngine` first and only call `MoveExecutor` for accepted moves.
- Task 6 implemented `MoveExecutor.apply` for accepted moves: board movement, moved hidden-piece reveal from mover pool, hidden captured-piece reveal from captured side pool, visible captures, turn switch, move number, no-capture counter, and `MoveRecord`.
- Task 6 added `MoveExecutorTest` for visible movement, hidden moved-piece reveal, hidden target capture, and visible target capture.
- Task 6 focused verification passed with `mvn -q "-Dtest=PositionTest,PieceTest,BoardTest,FlipPoolTest,RuleEngineTest,MoveExecutorTest" test` when run through a temporary `subst X:` path.
- Simplified `MoveRecord` to lightweight local move record data: move number, color, from/to, flip result, captured piece, server time, and end reason. Removed movement type and no-capture counter from the record because they can be derived when inspecting records or debugging.
- Verified the simplified `MoveRecord` together with Task 6 using `mvn -q "-Dtest=PositionTest,PieceTest,BoardTest,FlipPoolTest,RuleEngineTest,MoveExecutorTest,MoveRecordTest" test` through the temporary `subst X:` path.
- Removed project-doc wording that implied building a history-playback feature. Keep only local game records required by the assignment; do not add extra history APIs or UI.
- Task 6 commit status correction: `MoveExecutor` first-pass commit already existed in `b4599f9`, and the later `MoveRecord` simplification/doc cleanup was committed in `29ea9ea`.
- Task 7 implemented `GameRecorder.append` to write accepted move records as local JSON Lines files.
- Task 7 implemented room-level move flow in `GameRoom`: player/turn checks, rule validation, move execution, post-move finish checks for direct king capture and 80-ply no-capture draw, and response assembly.
- Task 7 implemented room-level `resign` plus safe `timeout(expiredColor, expectedDeadline)` settlement entrypoints.
- Task 7 added `GameRecorderTest` and `GameRoomTest` and passed focused verification with `mvn -q "-Dtest=MoveRecordTest,MoveExecutorTest,GameRecorderTest,GameRoomTest" test` through the temporary `subst X:` path.
- Task 8 completed the connected WebSocket room flow: `RoomManager` matchmaking, required `Ready` stage, `matchSuccess`, `roomInfo`, `gameStart`, server-side timeout scheduling, and timeout reset/ignore-stale behavior in `GameWebSocketHandler`.
- Task 8 focused verification passed with `mvn -q "-Dtest=GameWebSocketHandlerTest,RoomManagerTest,GameRoomTest,ChessApplicationTests" test` through the temporary `subst X:` path.
- Current local environment note: on Windows with the present Oracle JDK, `javac` classpath resolution under the real project path is unreliable for tests; using temporary `subst X:` mapping remains the stable workaround.

## Decisions Not Final Yet
- Confirmed that API design should use `capturedPiece` as a project extension field for captured-piece display and hidden captured-piece visibility.
- `capturedPiece` is clearer than overloading `move.type`: `flipResult` means moved-piece reveal, `capturedPiece` means captured-piece type.
- JSON protocol uses English piece names; Java internals may keep numeric `0-6` codes.
- Exact hidden captured piece payload difference between capturing side and captured side using `capturedPiece`.
- `Ready` is functionally required. After `matchSuccess`, the room stays in preparing state until both players send `Ready`, then the server sends `gameStart`.
- Whether failed `moveResult` should keep extra `code` / `message` fields or later trim them for stricter public-interface alignment.
- Exact domain class fields and responsibilities.
- Exact client UI representation.
- Exact game-record file format.

## Risks To Remember
- Overbuilding with database/login/Redis before the game loop works.
- Ignoring the public interface and losing interoperability.
- Treating same-computer play as pure single-page local play instead of two clients through server.
- Mixing coordinate systems incorrectly.
- Keeping the project under a path containing `!`, which can break Java/Maven test compilation classpath resolution on Windows.
- Revealing hidden captured piece types to the wrong receiver.
- Implementing hidden-piece movement incorrectly: first move uses original-square identity, later moves use revealed identity.
- Letting WebSocket handler absorb all business logic instead of keeping game rules in domain/rule classes.
