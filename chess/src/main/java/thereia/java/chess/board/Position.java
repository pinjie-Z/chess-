package thereia.java.chess.board;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class Position {

    private final String x;
    private final int y;

    private Position(String x, int y) {
        validateProtocolCoordinate(x, y);
        this.x = x;
        this.y = y;
    }

    public static Position of(String x, int y) {
        return new Position(x, y);
    }

    public static Position fromArrayIndex(int row, int col) {
        if (row < 0 || row > 9) {
            throw new IllegalArgumentException("row must be between 0 and 9");
        }
        if (col < 0 || col > 8) {
            throw new IllegalArgumentException("col must be between 0 and 8");
        }
        return new Position(String.valueOf((char) ('a' + col)), 9 - row);
    }

    public int getRow() {
        return 9 - y;
    }

    public int getCol() {
        return x.charAt(0) - 'a';
    }

    public int deltaX(Position other) {
        return other.getCol() - getCol();
    }

    public int deltaY(Position other) {
        return other.getY() - y;
    }

    private static void validateProtocolCoordinate(String x, int y) {
        if (x == null || x.length() != 1) {
            throw new IllegalArgumentException("x must be one character from a to i");
        }
        char file = x.charAt(0);
        if (file < 'a' || file > 'i') {
            throw new IllegalArgumentException("x must be between a and i");
        }
        if (y < 0 || y > 9) {
            throw new IllegalArgumentException("y must be between 0 and 9");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Position position)) {
            return false;
        }
        return y == position.y && Objects.equals(x, position.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return x + y;
    }
}
