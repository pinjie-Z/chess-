package thereia.java.chess.rule;

import lombok.Getter;
import thereia.java.chess.board.Board;
import thereia.java.chess.board.Position;
import thereia.java.chess.game.GameState;
import thereia.java.chess.move.Move;
import thereia.java.chess.move.MoveRecord;
import thereia.java.chess.move.MoveValidationResult;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;

import java.time.Instant;

public final class MoveExecutor {

    public MoveExecution apply(GameState state, Move move, ChessColor moverColor, Instant now) {
        Position from = move.getFrom();
        Position to = move.getTo();
        Board board = state.getBoard();
        Piece movedPiece = board.pieceAt(from).orElseThrow();
        Piece targetPiece = board.pieceAt(to).orElse(null);

        boolean capturedHiddenPiece = targetPiece != null && !targetPiece.isVisible();
        PieceType capturedPiece = capturedPiece(targetPiece, state);
        PieceType flipResult = null;
        Piece pieceAfterMove = movedPiece;
        if (!movedPiece.isVisible()) {
            flipResult = poolFor(state, moverColor).draw();
            pieceAfterMove = movedPiece.reveal(flipResult);
        }

        Board nextBoard = board.move(from, to, pieceAfterMove);
        // 这么早就统计不吃子了？
        int nextNoCapturePlyCount = capturedPiece == null ? state.getNoCapturePlyCount() + 1 : 0;
        // 赫赫，回合数
        int nextMoveNumber = state.getMoveNumber() + 1;

        GameState nextState = new GameState(nextBoard, moverColor.opponent(), state.getStatus(), state.getRedPool(),
                state.getBlackPool(), nextNoCapturePlyCount, nextMoveNumber, now, now.plusSeconds(60),
                state.getWinnerColor(), state.getEndReason());
        MoveRecord record = new MoveRecord(nextMoveNumber, moverColor, from, to, flipResult, capturedPiece, now,
                state.getEndReason());

        return new MoveExecution(true, MoveValidationResult.ok(), nextState, record, flipResult, capturedPiece,
                capturedHiddenPiece);
    }

    private PieceType capturedPiece(Piece targetPiece, GameState state) {
        if (targetPiece == null) {
            return null;
        }
        if (targetPiece.isVisible()) {
            return targetPiece.getMovementType();
        }
        return poolFor(state, targetPiece.getColor()).draw();
    }

    private FlipPool poolFor(GameState state, ChessColor color) {
        return color == ChessColor.RED ? state.getRedPool() : state.getBlackPool();
    }

    @Getter
    public static final class MoveExecution {

        private final boolean success;
        private final MoveValidationResult validation;
        private final GameState state;
        private final MoveRecord record;
        private final PieceType flipResult;
        private final PieceType capturedPiece;
        private final boolean capturedHiddenPiece;

        public MoveExecution(boolean success, MoveValidationResult validation, GameState state, MoveRecord record,
                             PieceType flipResult, PieceType capturedPiece, boolean capturedHiddenPiece) {
            this.success = success;
            this.validation = validation;
            this.state = state;
            this.record = record;
            this.flipResult = flipResult;
            this.capturedPiece = capturedPiece;
            this.capturedHiddenPiece = capturedHiddenPiece;
        }

    }
}
