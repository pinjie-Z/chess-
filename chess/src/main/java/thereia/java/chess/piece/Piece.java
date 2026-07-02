package thereia.java.chess.piece;

import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

@Getter
public final class Piece {

    private final String id;
    private final ChessColor color;
    private final PieceType originalType;
    private final PieceType revealedType;

    private Piece(String id, ChessColor color, PieceType originalType, PieceType revealedType) {
        this.id = id;
        this.color = color;
        this.originalType = originalType;
        this.revealedType = revealedType;
    }

    public static Piece visible(String id, ChessColor color, PieceType type) {
        return new Piece(id, color, type, type);
    }

    public static Piece hidden(String id, ChessColor color, PieceType originalType) {
        return new Piece(id, color, originalType, null);
    }

    public boolean isVisible() {
        return revealedType != null;
    }

    public PieceType getMovementType() {
        return isVisible() ? revealedType : originalType;
    }

    public Optional<PieceType> getRevealedTypeOptional() {
        return Optional.ofNullable(revealedType);
    }

    public Piece reveal(PieceType type) {
        if (isVisible()) {
            throw new IllegalStateException("piece is already visible");
        }
        return new Piece(id, color, originalType, type);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Piece piece)) {
            return false;
        }
        return Objects.equals(id, piece.id)
                && color == piece.color
                && originalType == piece.originalType
                && revealedType == piece.revealedType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, color, originalType, revealedType);
    }
}
