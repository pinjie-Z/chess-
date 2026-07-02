# Agent Instructions

## Project Location

The actual Spring Boot project is in:

```text
chess/
```

The outer directory contains assignment documents and workspace files.

## Required Memory Workflow

Before doing project work, read:

```text
chess/docs/PROJECT_SHORT_CARD.md
```

If the short card has a clear active task, follow it.

If the short card is empty, stale, unclear, or all tasks are complete, read:

```text
chess/docs/PROJECT_LONG_CARD.md
```

Then update `chess/docs/PROJECT_SHORT_CARD.md` with the next current task before continuing.

## Card Responsibilities

- `PROJECT_SHORT_CARD.md`: only the current task, immediate actions, and completion criteria.
- `PROJECT_LONG_CARD.md`: overall goal, planning, decisions, completed work, unfinished work, and risks.

When a task is completed:

1. Update `PROJECT_LONG_CARD.md`.
2. Refresh `PROJECT_SHORT_CARD.md`.
3. Keep the short card short.

## Project Direction

Build the simplest acceptable Jieqi assignment:

- One Spring Boot server.
- Two browser clients on the same computer.
- WebSocket JSON communication.
- Server-authoritative game state and rule validation.
- Local file game records.
- No database, Redis, login system, public deployment, spectator mode, chat, or AI in the first version.

