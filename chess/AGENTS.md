# Local Agent Instructions

## Project Memory Cards

This project uses two memory cards under `docs/`:

- `docs/PROJECT_SHORT_CARD.md`
- `docs/PROJECT_LONG_CARD.md`

Before starting project work in a new session, read the short card first:

1. Read `PROJECT_SHORT_CARD.md`.
2. If the short card has a clear active task, follow it without reading the long card.
3. Read `PROJECT_LONG_CARD.md` only when the short card is empty, stale, unclear, contradictory, all listed tasks are complete, or the user explicitly asks for overall background/history.

Use the short card to decide the current task. Treat the long card as fallback context to conserve tokens.

If the short card is empty, unclear, stale, or all listed tasks are complete, decide the next task from the long card and update `PROJECT_SHORT_CARD.md` before continuing.

## Card Responsibilities

`PROJECT_SHORT_CARD.md` records only the current task and immediate next actions. Keep it short.

`PROJECT_LONG_CARD.md` records the overall goal, project plan, important decisions, completed work, unfinished work, risks, and historical context.

When a task is completed:

1. Mark or move it into the completed section of `PROJECT_LONG_CARD.md`.
2. Refresh `PROJECT_SHORT_CARD.md` with the next current task.
3. Do not let the short card become a second long card.

## Project Direction

Build the simplest acceptable Jieqi assignment:

- One Spring Boot server.
- Two browser clients on the same computer.
- WebSocket JSON communication.
- Server-authoritative game state and rule validation.
- Local file game records.
- No database, Redis, login system, public deployment, spectator mode, chat, or AI in the first version.

Prefer simple implementation that satisfies the teacher's requirements over unnecessary backend infrastructure.

## Java Style Preference

- Prefer ordinary Java classes with explicit fields, constructors, and accessor methods over Java `record` declarations.
- Keep code beginner-readable for this assignment, even when a `record` would be shorter.
- Use normal JavaBean accessors such as `getId()`, `getX()`, and `isVisible()` consistently.
- Lombok `@Getter` is acceptable to reduce getter boilerplate. Avoid fluent accessors, `@Data`, and broad `@Setter`; add setters only when mutability is truly needed.
- Avoid enterprise-style defensive validation for internal model constructors. Keep validation where it directly represents game rules or protects board coordinates/protocol boundaries.
- Do not default to immutable-object patterns, defensive encapsulation, or extra helper methods when a simple field update would be clearer for this assignment.
- Do not justify added complexity with network-attack, malicious-client, or bypass-call scenarios unless the user explicitly asks for that level of robustness. Keep server authority for normal game correctness, but do not overbuild.
- Prefer direct, beginner-readable state changes for domain objects when they match the agreed model. Add small methods only when they clearly simplify call sites or express a real game rule.
