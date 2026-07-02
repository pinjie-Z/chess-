package thereia.java.chess.move;

import org.junit.jupiter.api.Test;
import thereia.java.chess.board.Position;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.PieceType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MoveRecordTest {

    @Test
    void recordsOperationAndRevealFacts() {
        Instant now = Instant.parse("2026-06-30T08:00:00Z");

        MoveRecord record = new MoveRecord(3, ChessColor.RED, Position.of("a", 0), Position.of("a", 1),
                PieceType.ROOK, PieceType.PAWN, now, null);

        assertThat(record.getMoveNumber()).isEqualTo(3);
        assertThat(record.getColor()).isEqualTo(ChessColor.RED);
        assertThat(record.getFrom()).isEqualTo(Position.of("a", 0));
        assertThat(record.getTo()).isEqualTo(Position.of("a", 1));
        assertThat(record.getFlipResult()).isEqualTo(PieceType.ROOK);
        assertThat(record.getCapturedPiece()).isEqualTo(PieceType.PAWN);
        assertThat(record.getServerTime()).isEqualTo(now);
        assertThat(record.getEndReason()).isNull();
    }
}
