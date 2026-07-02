package thereia.java.chess.rule;

import thereia.java.chess.board.Board;
import thereia.java.chess.board.Position;
import thereia.java.chess.move.Move;
import thereia.java.chess.move.MoveValidationResult;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;

public final class RuleEngine {

    public MoveValidationResult validate(Board board, Move move, ChessColor moverColor) {
        Position from = move.getFrom();
        Position to = move.getTo();

        // 该死的防御性编程
        if (from.equals(to)) {
            return MoveValidationResult.illegal("source and destination must be different");
        }

        Piece piece = board.pieceAt(from).orElse(null);
        if (piece == null) {
            return MoveValidationResult.illegal("source is empty");
        }
        if (piece.getColor() != moverColor) {
            return MoveValidationResult.illegal("source piece does not belong to mover");
        }

        Piece target = board.pieceAt(to).orElse(null);
        if (target != null && target.getColor() == moverColor) {
            return MoveValidationResult.illegal("destination contains own piece");
        }

        if (!canMove(board, from, to, piece, target != null)) {
            return MoveValidationResult.illegal("illegal movement");
        }

        Board moved = board.move(from, to, piece);
        if (kingsFace(moved)) {
            return MoveValidationResult.illegal("kings cannot face each other");
        }

        return MoveValidationResult.ok();
    }

    // 优雅的暴力枚举
    private boolean canMove(Board board, Position from, Position to, Piece piece, boolean capture) {
        return switch (piece.getMovementType()) {
            case KING -> canKingMove(from, to, piece.getColor());
            case ROOK -> canRookMove(board, from, to);
            case CANNON -> canCannonMove(board, from, to, capture);
            case KNIGHT -> canKnightMove(board, from, to);
            case PAWN -> canPawnMove(from, to, piece.getColor());
            case GUARD -> canGuardMove(from, to, piece);
            case BISHOP -> canBishopMove(board, from, to, piece);
        };
    }

    private boolean canKingMove(Position from, Position to, ChessColor color) {
        return isOrthogonalOneStep(from, to) && isInPalace(to, color);
    }

    private boolean canRookMove(Board board, Position from, Position to) {
        return isStraight(from, to) && board.countBetween(from, to) == 0;
    }

    private boolean canCannonMove(Board board, Position from, Position to, boolean capture) {
        if (!isStraight(from, to)) {
            return false;
        }
        int between = board.countBetween(from, to);
        return capture ? between == 1 : between == 0;
    }

    private boolean canKnightMove(Board board, Position from, Position to) {
        int dx = from.deltaX(to);
        int dy = from.deltaY(to);
        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);
        if (!((absDx == 1 && absDy == 2) || (absDx == 2 && absDy == 1))) {
            return false;
        }

        Position leg;
        if (absDy == 2) {
            leg = Position.of(from.getX(), from.getY() + Integer.signum(dy));
        } else {
            leg = Position.of(xFromCol(from.getCol() + Integer.signum(dx)), from.getY());
        }
        return board.isEmpty(leg);
    }

    private boolean canPawnMove(Position from, Position to, ChessColor color) {
        int dx = from.deltaX(to);
        int dy = from.deltaY(to);
        if (dx == 0 && dy == color.forwardDy()) {
            return true;
        }
        return hasCrossedRiver(from, color) && Math.abs(dx) == 1 && dy == 0;
    }

    private boolean canGuardMove(Position from, Position to, Piece piece) {
        if (Math.abs(from.deltaX(to)) != 1 || Math.abs(from.deltaY(to)) != 1) {
            return false;
        }
        return piece.isVisible() || isInPalace(to, piece.getColor());
    }

    private boolean canBishopMove(Board board, Position from, Position to, Piece piece) {
        int dx = from.deltaX(to);
        int dy = from.deltaY(to);
        if (Math.abs(dx) != 2 || Math.abs(dy) != 2) {
            return false;
        }

        Position eye = Position.of(xFromCol(from.getCol() + dx / 2), from.getY() + dy / 2);
        if (!board.isEmpty(eye)) {
            return false;
        }
        return piece.isVisible() || !crossesRiver(to, piece.getColor());
    }

    private boolean kingsFace(Board board) {
        Position redKing = null;
        Position blackKing = null;

        for (int row = 0; row <= 9; row++) {
            for (int col = 0; col <= 8; col++) {
                Position position = Position.fromArrayIndex(row, col);
                Piece piece = board.pieceAt(position).orElse(null);
                if (piece != null && piece.getMovementType() == PieceType.KING) {
                    if (piece.getColor() == ChessColor.RED) {
                        redKing = position;
                    } else {
                        blackKing = position;
                    }
                }
            }
        }

        return redKing != null
                && blackKing != null
                && redKing.getCol() == blackKing.getCol()
                && board.countBetween(redKing, blackKing) == 0;
    }

    private boolean isStraight(Position from, Position to) {
        return from.getCol() == to.getCol() || from.getY() == to.getY();
    }

    private boolean isOrthogonalOneStep(Position from, Position to) {
        return Math.abs(from.deltaX(to)) + Math.abs(from.deltaY(to)) == 1;
    }

    private boolean isInPalace(Position position, ChessColor color) {
        boolean palaceFile = position.getCol() >= 3 && position.getCol() <= 5;
        if (color == ChessColor.RED) {
            return palaceFile && position.getY() >= 0 && position.getY() <= 2;
        }
        return palaceFile && position.getY() >= 7 && position.getY() <= 9;
    }

    private boolean hasCrossedRiver(Position position, ChessColor color) {
        return color == ChessColor.RED ? position.getY() >= 5 : position.getY() <= 4;
    }

    private boolean crossesRiver(Position to, ChessColor color) {
        return color == ChessColor.RED ? to.getY() >= 5 : to.getY() <= 4;
    }

    private String xFromCol(int col) {
        return String.valueOf((char) ('a' + col));
    }

    public boolean hasValidMove(Board board, ChessColor moverColor) {
        for (int row = 0; row <= 9; row++) {
            for (int col = 0; col <= 8; col++) {
                Position from = Position.fromArrayIndex(row, col);
                Piece piece = board.pieceAt(from).orElse(null);
                if (piece == null || piece.getColor() != moverColor) {
                    continue;
                }

                for (int toRow = 0; toRow <= 9; toRow++) {
                    for (int toCol = 0; toCol <= 8; toCol++) {
                        Position to = Position.fromArrayIndex(toRow, toCol);
                        if (from.equals(to)) {
                            continue;
                        }
                        Move move = new Move(from, to, true);
                        if (validate(board, move, moverColor).isValid()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
