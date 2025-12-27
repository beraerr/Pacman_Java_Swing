package model;

public class Ghost {
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    public enum Type { BLINKY, PINKY, INKY, CLYDE }
    public enum State { IN_HOUSE, LEAVING_HOUSE, NORMAL, FRIGHTENED, EATEN }

    private int row;
    private int col;
    private Direction direction;
    private int animationFrame;
    private final Type type;
    private State state;
    private int prevRow;
    private int prevCol;

    public Ghost(Type type, int startRow, int startCol) {
        this.type = type;
        this.row = startRow;
        this.col = startCol;
        this.direction = Direction.UP;
        this.animationFrame = 0;
        this.state = State.IN_HOUSE;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public Direction getDirection() { return direction; }
    public int getAnimationFrame() { return animationFrame; }
    public Type getType() { return type; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public void setDirection(Direction dir) { this.direction = dir; }
    public void setAnimationFrame(int frame) { this.animationFrame = frame; }
    public void move(int dRow, int dCol) {
        prevRow = this.row;
        prevCol = this.col;
        this.row += dRow;
        this.col += dCol;
    }
    public void setPosition(int row, int col) {
        prevRow = this.row;
        prevCol = this.col;
        this.row = row;
        this.col = col;
    }
    public int getPrevRow() { return prevRow; }
    public int getPrevCol() { return prevCol; }
} 