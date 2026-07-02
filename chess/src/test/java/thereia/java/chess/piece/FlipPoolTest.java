package thereia.java.chess.piece;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlipPoolTest {

    @Test
    void initialPoolContainsOneSideHiddenRevealTypes() {
        FlipPool pool = FlipPool.initial(new Random(1));

        assertThat(pool.remainingCount()).isEqualTo(15);
        assertThat(pool.remainingTypes())
                .containsExactlyInAnyOrderElementsOf(List.of(
                        PieceType.ROOK, PieceType.ROOK,
                        PieceType.KNIGHT, PieceType.KNIGHT,
                        PieceType.CANNON, PieceType.CANNON,
                        PieceType.BISHOP, PieceType.BISHOP,
                        PieceType.GUARD, PieceType.GUARD,
                        PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN
                ));
    }

    @Test
    void drawRemovesOneRemainingType() {
        FlipPool pool = FlipPool.initial(new Random(2));
        List<PieceType> before = pool.remainingTypes();

        PieceType drawn = pool.draw();

        assertThat(before).contains(drawn);
        assertThat(pool.remainingCount()).isEqualTo(14);
        assertThat(pool.remainingTypes()).containsExactlyInAnyOrderElementsOf(removeOne(before, drawn));
    }

    @Test
    void remainingTypesCannotModifyPool() {
        FlipPool pool = FlipPool.initial(new Random(3));
        List<PieceType> remaining = pool.remainingTypes();

        assertThatThrownBy(() -> remaining.add(PieceType.KING)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(pool.remainingCount()).isEqualTo(15);
    }

    @Test
    void cannotDrawFromEmptyPool() {
        FlipPool pool = FlipPool.initial(new Random(4));
        for (int i = 0; i < 15; i++) {
            pool.draw();
        }

        assertThat(pool.remainingCount()).isZero();
        assertThatThrownBy(pool::draw).isInstanceOf(IllegalStateException.class);
    }

    private static List<PieceType> removeOne(List<PieceType> types, PieceType removed) {
        List<PieceType> next = new java.util.ArrayList<>(types);
        next.remove(removed);
        return next;
    }
}
