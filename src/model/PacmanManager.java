package model;

public class PacmanManager {
    private final BoardModel boardModel;
    private final Pacman pacman;

    public PacmanManager(BoardModel boardModel, int startRow, int startCol) {
        this.boardModel = boardModel;
        this.pacman = new Pacman(startRow, startCol);
    }

    public Pacman getPacman() {
        return pacman;
    }

    public boolean movePacmanTick() {
        boardModel.incrementTicksSinceStart();
        Pacman.Direction desired = pacman.getDesiredDirection();
        Pacman.Direction current = pacman.getDirection();
        int[] d = boardModel.getDelta(desired);
        int row = pacman.getRow();
        int col = pacman.getCol();
        int newRow = row + d[0];
        int newCol = col + d[1];
        if (boardModel.isPortalRow(row)) {
            if (d[1] == -1 && col == 0) {
                newCol = boardModel.getColumnCount() - 1;
            }
            if (d[1] == 1 && col == boardModel.getColumnCount() - 1) {
                newCol = 0;
            }
        }
        boolean moved = false;
        if (boardModel.isPathPortalAware(newRow, newCol, false)) {
            pacman.setDirection(desired);
            pacman.setPosition(newRow, newCol);
            pacman.setAnimationFrame((pacman.getAnimationFrame() + 1) % 4);
            moved = true;
        } else {
            d = boardModel.getDelta(current);
            newRow = row + d[0];
            newCol = col + d[1];
            if (boardModel.isPortalRow(row)) {
                if (d[1] == -1 && col == 0) {
                    newCol = boardModel.getColumnCount() - 1;
                }
                if (d[1] == 1 && col == boardModel.getColumnCount() - 1) {
                    newCol = 0;
                }
            }
            if (boardModel.isPathPortalAware(newRow, newCol, false)) {
                pacman.setPosition(newRow, newCol);
                pacman.setAnimationFrame((pacman.getAnimationFrame() + 1) % 4);
                moved = true;
            } else {
                pacman.setAnimationFrame((pacman.getAnimationFrame() + 1) % 4);
            }
        }
        if (moved) {
            if (boardModel.eatPelletIfPresent()) {
                boardModel.incrementPelletsEaten();
                if (boardModel.getUpgradeManager() != null) boardModel.getUpgradeManager().maybeSpawnFruit();
            }
            if (boardModel.getUpgradeManager() != null) boardModel.getUpgradeManager().eatFruitIfPresent();
            if (boardModel.getUpgradeManager() != null) boardModel.getUpgradeManager().eatUpgradeIfPresent();
        }
        boardModel.fireTableDataChanged();
        return moved;
    }

    public void resetPacmanPosition(int row, int col) {
        pacman.setPosition(row, col);
        pacman.setDirection(Pacman.Direction.LEFT);
        pacman.setDesiredDirection(Pacman.Direction.LEFT);
        pacman.setAnimationFrame(0);
    }
} 