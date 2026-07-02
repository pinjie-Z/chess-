package thereia.java.chess.move;

import lombok.Getter;
import thereia.java.chess.board.Position;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.PieceType;

import java.time.Instant;

@Getter
public final class MoveRecord {

    private final int moveNumber;
    private final ChessColor color;
    private final Position from;
    private final Position to;
    private final PieceType flipResult;
    private final PieceType capturedPiece;
    private final Instant serverTime;
    private final String endReason;

    public MoveRecord(int moveNumber, ChessColor color, Position from, Position to, PieceType flipResult,
                      PieceType capturedPiece, Instant serverTime, String endReason) {
        this.moveNumber = moveNumber;
        this.color = color;
        this.from = from;
        this.to = to;
        this.flipResult = flipResult;
        this.capturedPiece = capturedPiece;
        this.serverTime = serverTime;
        this.endReason = endReason;
    }

}
