package thereia.java.chess.piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class FlipPool {

    private final Random random;
    private final List<PieceType> remainingTypes;

    private FlipPool(Random random, List<PieceType> remainingTypes) {
        this.random = random;
        this.remainingTypes = new ArrayList<>(remainingTypes);
    }

    public static FlipPool initial(Random random) {
        List<PieceType> types = new ArrayList<>(List.of(
                PieceType.ROOK, PieceType.ROOK,
                PieceType.KNIGHT, PieceType.KNIGHT,
                PieceType.CANNON, PieceType.CANNON,
                PieceType.BISHOP, PieceType.BISHOP,
                PieceType.GUARD, PieceType.GUARD,
                PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN
        ));
        Collections.shuffle(types, random);
        return new FlipPool(random, types);
    }

    public PieceType draw() {
        if (remainingTypes.isEmpty()) {
            throw new IllegalStateException("flip pool is empty");
        }
        return remainingTypes.remove(remainingTypes.size() - 1);
    }

    public int remainingCount() {
        return remainingTypes.size();
    }

    public List<PieceType> remainingTypes() {
        return List.copyOf(remainingTypes);
    }

    Random random() {
        return random;
    }
}
