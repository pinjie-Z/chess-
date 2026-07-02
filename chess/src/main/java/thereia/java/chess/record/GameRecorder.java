package thereia.java.chess.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import thereia.java.chess.move.MoveRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Getter
public final class GameRecorder {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private final Path recordsDir;

    public GameRecorder(Path recordsDir) {
        this.recordsDir = recordsDir;
    }

    public void append(String roomId, MoveRecord record) throws IOException {
        Files.createDirectories(recordsDir);
        Path file = recordsDir.resolve(roomId + ".jsonl");
        String json = OBJECT_MAPPER.writeValueAsString(record) + System.lineSeparator();
        Files.writeString(file, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

}
