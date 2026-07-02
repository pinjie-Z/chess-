package thereia.java.chess.piece;

public enum ChessColor {
    RED,
    BLACK;

    public ChessColor opponent() {
        return this == RED ? BLACK : RED;
    }

    public int forwardDy() {
        return this == RED ? 1 : -1;
    }
}
