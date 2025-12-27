package model;

public class Pacman {
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private int row;
    private int col;
    private Direction direction;
    private Direction desiredDirection;
    private int animationFrame;
    private int prevRow;
    private int prevCol;

    public Pacman(int startRow, int startCol) {
        this.row = startRow;
        this.col = startCol;
        this.direction = Direction.RIGHT;
        this.desiredDirection = Direction.RIGHT;
        this.animationFrame = 0;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public Direction getDirection() { return direction; }
    public int getAnimationFrame() { return animationFrame; }
    public Direction getDesiredDirection() { return desiredDirection; }
    public int getPrevRow() { return prevRow; }
    public int getPrevCol() { return prevCol; }

    public void setDirection(Direction dir) { this.direction = dir; }
    public void setAnimationFrame(int frame) { this.animationFrame = frame; }
    public void setDesiredDirection(Direction dir) { this.desiredDirection = dir; }

    public void setPosition(int row, int col) {
        prevRow = this.row;
        prevCol = this.col;
        this.row = row;
        this.col = col;
    }
} 