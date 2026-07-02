package thereia.java.chess.record;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thereia.java.chess.board.Position;
import thereia.java.chess.move.MoveRecord;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.PieceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GameRecorderTest {

    @TempDir
    Path dir;

    @Test
    void appendsMoveRecordAsJsonLine() throws IOException {
        GameRecorder recorder = new GameRecorder(dir);
        MoveRecord record = new MoveRecord(1, ChessColor.RED, Position.of("a", 0), Position.of("a", 1),
                PieceType.ROOK, PieceType.PAWN, Instant.parse("2026-06-30T08:00:00Z"), "checkmate");

        recorder.append("room-1", record);

        Path file = dir.resolve("room-1.jsonl");
        assertThat(Files.exists(file)).isTrue();

        String content = Files.readString(file);
        assertThat(content).contains("\"moveNumber\":1");
        assertThat(content).contains("\"from\"");
        assertThat(content).contains("\"to\"");
        assertThat(content).contains("\"flipResult\":\"ROOK\"");
        assertThat(content).contains("\"capturedPiece\":\"PAWN\"");
        assertThat(content).contains("\"endReason\":\"checkmate\"");
    }
}
