package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.move.Move;
import thereia.java.chess.move.MoveRecord;
import thereia.java.chess.move.MoveValidationResult;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.PieceType;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.protocol.MessageType;
import thereia.java.chess.protocol.GameStartMessage;
import thereia.java.chess.protocol.GameOverMessage;
import thereia.java.chess.protocol.MoveResultMessage;
import thereia.java.chess.protocol.RoomInfoMessage;
import thereia.java.chess.protocol.TimeoutMessage;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
public final class GameRoom {

    private final String roomId;
    private final Player redPlayer;
    private final Player blackPlayer;
    private final RuleEngine ruleEngine;
    private final MoveExecutor moveExecutor;
    private final GameRecorder gameRecorder;
    private GameState state;
    private boolean redReady;
    private boolean blackReady;

    public GameRoom(String roomId, Player redPlayer, Player blackPlayer, GameState state,
                    RuleEngine ruleEngine, MoveExecutor moveExecutor, GameRecorder gameRecorder) {
        this.roomId = roomId;
        this.redPlayer = redPlayer;
        this.blackPlayer = blackPlayer;
        this.state = state;
        this.ruleEngine = ruleEngine;
        this.moveExecutor = moveExecutor;
        this.gameRecorder = gameRecorder;
    }

    public RoomMoveResult handleMove(String playerId, Move move, Instant now) {
        Player player = playerFor(playerId);
        // 该死的防御性编程
        if (player == null) {
            return invalidMove(MoveValidationResult.illegal("player is not in room"), move);
        }
        if (state.getStatus() != GameStatus.PLAYING) {
            return invalidMove(MoveValidationResult.illegal("game is not playing"), move);
        }
        if (state.getCurrentTurn() != player.getColor()) {
            return invalidMove(new MoveValidationResult(false, 2002, "not this player's turn"), move);
        }

        MoveValidationResult validation = ruleEngine.validate(state.getBoard(), move, player.getColor());
        if (!validation.isValid()) {
            return invalidMove(validation, move);
        }

        MoveExecutor.MoveExecution execution = moveExecutor.apply(state, move, player.getColor(), now);
        GameState nextState = maybeFinishAfterMove(execution.getState(), execution);
        MoveRecord record = withEndReason(execution.getRecord(), nextState.getEndReason());
        this.state = nextState;
        appendRecord(record);

        String currentTurn = nextState.getStatus() == GameStatus.ENDED ? null : colorName(nextState.getCurrentTurn());
        MoveResultMessage actorMoveResult = successMoveResult(move, execution, false, currentTurn);
        MoveResultMessage opponentMoveResult = successMoveResult(move, execution, true, currentTurn);
        GameOverMessage gameOver = nextState.getStatus() == GameStatus.ENDED
                ? gameOverFor(nextState)
                : null;
        return new RoomMoveResult(true, actorMoveResult, opponentMoveResult, gameOver);
    }

    public ReadyResult ready(String playerId, Instant now) {
        Player player = playerFor(playerId);
        if (player == null) {
            throw new IllegalArgumentException("player is not in room");
        }
        if (state.getStatus() != GameStatus.PREPARING) {
            throw new IllegalStateException("game is not preparing");
        }

        if (player.getColor() == ChessColor.RED) {
            redReady = true;
        } else {
            blackReady = true;
        }

        if (!redReady || !blackReady) {
            return new ReadyResult(false, new RoomInfoMessage(MessageType.roomInfo.name(), true), null, null);
        }

        this.state = new GameState(state.getBoard(), ChessColor.RED, GameStatus.PLAYING, state.getRedPool(),
                state.getBlackPool(), state.getNoCapturePlyCount(), state.getMoveNumber(), now, now.plusSeconds(60),
                state.getWinnerColor(), state.getEndReason());
        return new ReadyResult(true, null, gameStartFor(redPlayer), gameStartFor(blackPlayer));
    }

    public GameOverMessage resign(String playerId) {
        Player player = playerFor(playerId);
        if (player == null) {
            throw new IllegalArgumentException("player is not in room");
        }
        if (state.getStatus() != GameStatus.PLAYING) {
            throw new IllegalStateException("game is not playing");
        }
        ChessColor winner = player.getColor().opponent();
        this.state = finishGame(state, colorName(winner), "resign");
        return gameOverFor(state);
    }

    public TimeoutMessage timeout(ChessColor expiredColor, Instant expectedDeadline) {
        if (state.getStatus() != GameStatus.PLAYING) {
            return null;
        }
        if (state.getCurrentTurn() != expiredColor) {
            return null;
        }
        if (!state.getTurnDeadlineAt().equals(expectedDeadline)) {
            return null;
        }

        ChessColor winner = expiredColor.opponent();
        this.state = finishGame(state, colorName(winner), "timeout");
        return new TimeoutMessage(MessageType.timeout.name(), playerIdFor(expiredColor), playerIdFor(winner), "timeout");
    }

    private Player playerFor(String playerId) {
        if (redPlayer.getPlayerId().equals(playerId)) {
            return redPlayer;
        }
        if (blackPlayer.getPlayerId().equals(playerId)) {
            return blackPlayer;
        }
        return null;
    }

    private RoomMoveResult invalidMove(MoveValidationResult validation, Move move) {
        MoveResultMessage moveResult = new MoveResultMessage(MessageType.moveResult.name(), false, moveMessage(move),
                null, validation.isValid(), validation.getCode(), validation.getMessage(), null, null);
        return new RoomMoveResult(false, moveResult, null, null);
    }

    private GameState maybeFinishAfterMove(GameState nextState, MoveExecutor.MoveExecution execution) {
        if (execution.getCapturedPiece() == PieceType.KING) {
            return finishGame(nextState, colorName(state.getCurrentTurn()), "checkmate");
        }
        if (nextState.getNoCapturePlyCount() >= 80) {
            return finishGame(nextState, null, "drawNoCapture");
        }
        if (!ruleEngine.hasValidMove(nextState.getBoard(), nextState.getCurrentTurn())) {
            return finishGame(nextState, colorName(state.getCurrentTurn()), "checkmate");
        }
        return nextState;
    }

    private GameState finishGame(GameState baseState, String winnerColor, String endReason) {
        return new GameState(baseState.getBoard(), baseState.getCurrentTurn(), GameStatus.ENDED, baseState.getRedPool(),
                baseState.getBlackPool(), baseState.getNoCapturePlyCount(), baseState.getMoveNumber(),
                baseState.getTurnStartedAt(), baseState.getTurnDeadlineAt(), winnerColor, endReason);
    }

    private MoveRecord withEndReason(MoveRecord record, String endReason) {
        return new MoveRecord(record.getMoveNumber(), record.getColor(), record.getFrom(), record.getTo(),
                record.getFlipResult(), record.getCapturedPiece(), record.getServerTime(), endReason);
    }

    private void appendRecord(MoveRecord record) {
        try {
            gameRecorder.append(roomId, record);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to append move record", exception);
        }
    }

    private MoveResultMessage successMoveResult(Move move, MoveExecutor.MoveExecution execution,
                                                boolean hideCapturedPiece, String currentTurn) {
        return new MoveResultMessage(MessageType.moveResult.name(), true, moveMessage(move),
                pieceName(execution.getFlipResult()), true, null, null,
                hideCapturedPiece && execution.isCapturedHiddenPiece() ? null : pieceName(execution.getCapturedPiece()),
                currentTurn);
    }

    private MoveResultMessage.MoveMessage moveMessage(Move move) {
        return new MoveResultMessage.MoveMessage(move.getFrom().getX(), move.getFrom().getY(), move.getTo().getX(),
                move.getTo().getY(), move.isFlipHint());
    }

    private String colorName(ChessColor color) {
        return color == null ? null : color.name();
    }

    private String pieceName(PieceType pieceType) {
        return pieceType == null ? null : pieceType.name();
    }

    private GameStartMessage gameStartFor(Player player) {
        return new GameStartMessage(MessageType.gameStart.name(), redPlayer.getPlayerId(), blackPlayer.getPlayerId(),
                colorNameLower(player.getColor()), player.getColor() == ChessColor.RED, initialBoard());
    }

    private List<GameStartMessage.InitialPieceMessage> initialBoard() {
        List<GameStartMessage.InitialPieceMessage> pieces = new ArrayList<>();
        for (int row = 0; row <= 9; row++) {
            for (int col = 0; col <= 8; col++) {
                thereia.java.chess.board.Position position = thereia.java.chess.board.Position.fromArrayIndex(row, col);
                Piece piece = state.getBoard().pieceAt(position).orElse(null);
                if (piece == null) {
                    continue;
                }
                pieces.add(new GameStartMessage.InitialPieceMessage(position.getX(), position.getY(),
                        colorName(piece.getColor()), pieceName(piece.getOriginalType()), piece.isVisible()));
            }
        }
        return pieces;
    }

    private GameOverMessage gameOverFor(GameState finishedState) {
        if (finishedState.getWinnerColor() == null) {
            return new GameOverMessage(MessageType.gameOver.name(), null, finishedState.getEndReason(), null);
        }
        ChessColor winner = ChessColor.valueOf(finishedState.getWinnerColor());
        return new GameOverMessage(MessageType.gameOver.name(), colorNameLower(winner), finishedState.getEndReason(),
                playerIdFor(winner));
    }

    private String playerIdFor(ChessColor color) {
        if (color == null) {
            return null;
        }
        return color == ChessColor.RED ? redPlayer.getPlayerId() : blackPlayer.getPlayerId();
    }

    private String colorNameLower(ChessColor color) {
        return color == null ? null : color.name().toLowerCase();
    }
}
