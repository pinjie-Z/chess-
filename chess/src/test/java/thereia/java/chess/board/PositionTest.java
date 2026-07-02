package thereia.java.chess.board;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionTest {

    @Test
    void convertsProtocolCoordinateToArrayIndex() {
        Position redKing = Position.of("e", 0);
        assertThat(redKing.getRow()).isEqualTo(9);
        assertThat(redKing.getCol()).isEqualTo(4);
        assertThat(redKing.getX()).isEqualTo("e");
        assertThat(redKing.getY()).isEqualTo(0);

        Position blackKing = Position.of("e", 9);
        assertThat(blackKing.getRow()).isEqualTo(0);
        assertThat(blackKing.getCol()).isEqualTo(4);
    }

    @Test
    void convertsArrayIndexToProtocolCoordinate() {
        Position redKing = Position.fromArrayIndex(9, 4);
        assertThat(redKing.getX()).isEqualTo("e");
        assertThat(redKing.getY()).isEqualTo(0);

        Position blackKing = Position.fromArrayIndex(0, 4);
        assertThat(blackKing.getX()).isEqualTo("e");
        assertThat(blackKing.getY()).isEqualTo(9);
    }

    @Test
    void rejectsInvalidCoordinate() {
        assertThatThrownBy(() -> Position.of("j", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.of("a", 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.of("", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidArrayIndex() {
        assertThatThrownBy(() -> Position.fromArrayIndex(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromArrayIndex(0, 9)).isInstanceOf(IllegalArgumentException.class);
    }
}
