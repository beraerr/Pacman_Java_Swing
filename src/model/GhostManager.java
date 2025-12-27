package model;

import java.util.*;

public class GhostManager {
    private final BoardModel boardModel;
    private final List<Ghost> ghosts = new ArrayList<>();
    private int frightenedTicks = 0;
    private static final int FRIGHTENED_DURATION_TICKS = 50; 
    private int ghostEatMultiplier = 0;
    private final Random rand = new Random();

    public GhostManager(BoardModel boardModel) {
        this.boardModel = boardModel;
        initializeGhosts();
    }

    private void initializeGhosts() {
        ghosts.clear();
        int rows = boardModel.getRowCount();
        int cols = boardModel.getColumnCount();
        int houseHeight = Math.min(3, rows - 4);
        int houseWidth = Math.min(5, cols - 4);
        int houseTop = rows / 2 - houseHeight / 2;
        int houseLeft = cols / 2 - houseWidth / 2;
        int ghostStartRow1 = houseTop + houseHeight - 2;
        int ghostStartRow2 = houseTop + houseHeight - 1;
        int ghostStartCol1 = houseLeft;
        int ghostStartCol2 = houseLeft + 1;
        ghosts.add(new Ghost(Ghost.Type.BLINKY, ghostStartRow1, ghostStartCol1));
        ghosts.add(new Ghost(Ghost.Type.PINKY, ghostStartRow1, ghostStartCol2));
        ghosts.add(new Ghost(Ghost.Type.INKY, ghostStartRow2, ghostStartCol1));
        ghosts.add(new Ghost(Ghost.Type.CLYDE, ghostStartRow2, ghostStartCol2));
    }

    public List<Ghost> getGhosts() {
        return ghosts;
    }

    public synchronized void moveGhostsTick() {
        if (frightenedTicks > 0) {
            frightenedTicks--;
            if (frightenedTicks == 0) {
                for (Ghost ghost : ghosts) {
                    if (ghost.getState() == Ghost.State.FRIGHTENED) {
                        ghost.setState(Ghost.State.NORMAL);
                    }
                }
                ghostEatMultiplier = 0; 
            }
        }
        boardModel.fireTableDataChanged();
    }

    public synchronized void moveGhostsActual() {
        Pacman pacman = boardModel.getPacman();
        int pacRow = pacman.getRow();
        int pacCol = pacman.getCol();
        int rows = boardModel.getRowCount();
        int cols = boardModel.getColumnCount();
        int houseHeight = Math.min(3, rows - 4);
        int houseWidth = Math.min(5, cols - 4);
        int houseTop = rows / 2 - houseHeight / 2;
        int houseLeft = cols / 2 - houseWidth / 2;
        int exitRow = houseTop + houseHeight;
        int exitCol = houseLeft + houseWidth / 2;
        for (Ghost ghost : ghosts) {
            if (ghost.getState() == Ghost.State.EATEN) {
                int gr = ghost.getRow(), gc = ghost.getCol();
                int[] home = getGhostHomeCell(ghost.getType());
                int targetRow = home[0], targetCol = home[1];
                if (gr == targetRow && gc == targetCol) {
                    ghost.setState(Ghost.State.IN_HOUSE);
                } else {
                    int[] move = bfsPathMove(gr, gc, targetRow, targetCol);
                    if (move != null) {
                        moveGhostWithPortal(ghost, move);
                    }
                }
                continue;
            }
            if (ghost.getState() == Ghost.State.IN_HOUSE) {
                if (ghost.getRow() == exitRow && ghost.getCol() == exitCol) {
                    ghost.setState(Ghost.State.LEAVING_HOUSE);
                } else {
                    int[] move = bfsPathMove(ghost.getRow(), ghost.getCol(), exitRow, exitCol);
                    if (move != null) {
                        moveGhostWithPortal(ghost, move);
                    }
                }
                continue;
            }
            if (ghost.getState() == Ghost.State.LEAVING_HOUSE) {
                if (ghost.getRow() > exitRow) {
                    ghost.setPosition(ghost.getRow() - 1, ghost.getCol());
                    ghost.setDirection(Ghost.Direction.UP);
                    ghost.setAnimationFrame((ghost.getAnimationFrame() + 1) % 4);
                } else {
                    ghost.setState(Ghost.State.NORMAL);
                }
                continue;
            }
            if (ghost.getState() == Ghost.State.FRIGHTENED) {
                java.util.List<int[]> validMoves = getValidMoves(ghost);
                int[] move = null;
                if (!validMoves.isEmpty()) move = validMoves.get(rand.nextInt(validMoves.size()));
                if (move != null) {
                    moveGhostWithPortal(ghost, move);
                }
                continue;
            }

            java.util.List<int[]> validMoves = getValidMoves(ghost);
            int[] move = null;
            switch (ghost.getType()) {
                case BLINKY -> {
                    move = bfsPathMove(ghost.getRow(), ghost.getCol(), pacRow, pacCol);
                    if (move == null) move = chooseMoveToward(ghost, pacRow, pacCol, validMoves);
                }
                case PINKY -> {
                    int[] ahead = getAheadOfPacman(pacman, 4);
                    move = bfsPathMove(ghost.getRow(), ghost.getCol(), ahead[0], ahead[1]);
                    if (move == null) move = chooseMoveToward(ghost, ahead[0], ahead[1], validMoves);
                }
                case INKY -> {
                    if (!validMoves.isEmpty()) move = validMoves.get(rand.nextInt(validMoves.size()));
                }
                case CLYDE -> {
                    int dist = Math.abs(ghost.getRow() - pacRow) + Math.abs(ghost.getCol() - pacCol);
                    if (dist > 6) {
                        move = bfsPathMove(ghost.getRow(), ghost.getCol(), pacRow, pacCol);
                        if (move == null) move = chooseMoveToward(ghost, pacRow, pacCol, validMoves);
                    } else {
                        move = bfsPathMove(ghost.getRow(), ghost.getCol(), rows - 2, 1);
                        if (move == null) move = chooseMoveToward(ghost, rows - 2, 1, validMoves);
                    }
                }
            }
            if (move == null && !validMoves.isEmpty()) move = validMoves.get(rand.nextInt(validMoves.size()));
            if (move != null) {
                moveGhostWithPortal(ghost, move);
            }
        }
        boardModel.fireTableDataChanged();
    }

    public boolean handlePacmanGhostCollision() {
        Pacman pacman = boardModel.getPacman();
        int pacRow = pacman.getRow();
        int pacCol = pacman.getCol();
        int prevPacRow = pacman.getPrevRow();
        int prevPacCol = pacman.getPrevCol();
        for (Ghost ghost : ghosts) {
            int ghostRow = ghost.getRow();
            int ghostCol = ghost.getCol();
            int prevGhostRow = ghost.getPrevRow();
            int prevGhostCol = ghost.getPrevCol();
            if (ghostRow == pacRow && ghostCol == pacCol) {
                if (ghost.getState() == Ghost.State.FRIGHTENED) {
                    ghost.setState(Ghost.State.EATEN);
                    ghostEatMultiplier++;
                    int[] ghostScores = {200, 400, 800, 1600};
                    int add = ghostScores[Math.min(ghostEatMultiplier-1, 3)];
                    boardModel.addScore(add);
                    boardModel.fireTableDataChanged();
                    return false;
                } else if (ghost.getState() == Ghost.State.NORMAL) {
                    if (boardModel.isShieldActive()) {
                        continue;
                    }
                    boardModel.decrementLives();
                    if (boardModel.getLives() > 0) {
                        boardModel.resetPositions();
                        boardModel.fireTableDataChanged();
                        return false;
                    } else {
                        return true;
                    }
                } else if (ghost.getState() == Ghost.State.EATEN || ghost.getState() == Ghost.State.IN_HOUSE) {
                    continue;
                }
            }
            if (ghostRow == prevPacRow && ghostCol == prevPacCol && prevGhostRow == pacRow && prevGhostCol == pacCol) {
                if (ghost.getState() == Ghost.State.FRIGHTENED) {
                    ghost.setState(Ghost.State.EATEN);
                    ghostEatMultiplier++;
                    int[] ghostScores = {200, 400, 800, 1600};
                    int add = ghostScores[Math.min(ghostEatMultiplier-1, 3)];
                    boardModel.addScore(add);
                    boardModel.fireTableDataChanged();
                    return false;
                } else if (ghost.getState() == Ghost.State.NORMAL) {
                    if (boardModel.isShieldActive()) {
                        continue;
                    }
                    boardModel.decrementLives();
                    if (boardModel.getLives() > 0) {
                        boardModel.resetPositions();
                        boardModel.fireTableDataChanged();
                        return false;
                    } else {
                        return true;
                    }
                } else if (ghost.getState() == Ghost.State.EATEN || ghost.getState() == Ghost.State.IN_HOUSE) {
                    continue;
                }
            }
        }
        return false;
    }

    private int[] bfsPathMove(int startRow, int startCol, int targetRow, int targetCol) {
        int rows = boardModel.getRowCount();
        int cols = boardModel.getColumnCount();
        if (startRow == targetRow && startCol == targetCol) return null;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        boolean[][] visited = new boolean[rows][cols];
        int[][][] prev = new int[rows][cols][2];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0], c = pos[1];
            for (int d = 0; d < 4; d++) {
                int nr = r + dirs[d][0];
                int nc = c + dirs[d][1];
                if (boardModel.isPortalRow(r)) {
                    if (dirs[d][1] == -1 && c == 0) nc = cols - 1;
                    if (dirs[d][1] == 1 && c == cols - 1) nc = 0;
                }
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && boardModel.isPathPortalAware(nr, nc, true) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    prev[nr][nc][0] = r;
                    prev[nr][nc][1] = c;
                    if (nr == targetRow && nc == targetCol) {
                        int cr = nr, cc = nc;
                        while (!(prev[cr][cc][0] == startRow && prev[cr][cc][1] == startCol)) {
                            int pr = prev[cr][cc][0];
                            int pc = prev[cr][cc][1];
                            cr = pr;
                            cc = pc;
                        }
                        return new int[]{cr - startRow, cc - startCol};
                    }
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return null;
    }

    private java.util.List<int[]> getValidMoves(Ghost ghost) {
        int rows = boardModel.getRowCount();
        int cols = boardModel.getColumnCount();
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        java.util.List<int[]> validMoves = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int nr = ghost.getRow() + dirs[i][0];
            int nc = ghost.getCol() + dirs[i][1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && boardModel.isPathPortalAware(nr, nc, true)) {
                validMoves.add(dirs[i]);
            }
        }
        return validMoves;
    }

    private void setGhostDirectionFromMove(Ghost ghost, int[] move) {
        if (move[0] == -1) ghost.setDirection(Ghost.Direction.UP);
        else if (move[0] == 1) ghost.setDirection(Ghost.Direction.DOWN);
        else if (move[1] == -1) ghost.setDirection(Ghost.Direction.LEFT);
        else if (move[1] == 1) ghost.setDirection(Ghost.Direction.RIGHT);
    }

    private int[] getAheadOfPacman(Pacman pacman, int n) {
        int row = pacman.getRow();
        int col = pacman.getCol();
        int[] d = boardModel.getDelta(pacman.getDirection());
        int aheadRow = row + d[0] * n;
        int aheadCol = col + d[1] * n;
        int rows = boardModel.getRowCount();
        int cols = boardModel.getColumnCount();
        aheadRow = Math.max(0, Math.min(rows - 1, aheadRow));
        aheadCol = Math.max(0, Math.min(cols - 1, aheadCol));
        return new int[]{aheadRow, aheadCol};
    }

    private int[] chooseMoveToward(Ghost ghost, int targetRow, int targetCol, java.util.List<int[]> moves) {
        int minDist = Integer.MAX_VALUE;
        int[] best = null;
        int gr = ghost.getRow(), gc = ghost.getCol();
        for (int[] m : moves) {
            int nr = gr + m[0];
            int nc = gc + m[1];
            int dist = Math.abs(nr - targetRow) + Math.abs(nc - targetCol);
            if (dist < minDist) {
                minDist = dist;
                best = m;
            }
        }
        return best;
    }

    private int[] getGhostHomeCell(Ghost.Type type) {
        int rows = boardModel.getRowCount();
        int cols = boardModel.getColumnCount();
        return switch (type) {
            case BLINKY -> new int[] { rows / 2, cols / 2 };
            case PINKY -> new int[] { rows / 2, cols / 2 - 1 };
            case INKY -> new int[] { rows / 2, cols / 2 + 1 };
            case CLYDE -> new int[] { rows / 2 + 1, cols / 2 };
        };
    }

    private void moveGhostWithPortal(Ghost ghost, int[] move) {
        int cols = boardModel.getColumnCount();
        int row = ghost.getRow();
        int col = ghost.getCol();
        int newRow = row + move[0];
        int newCol = col + move[1];
        if (boardModel.isPortalRow(row)) {
            if (move[1] == -1 && col == 0) {
                newCol = cols - 1;
            }
            if (move[1] == 1 && col == cols - 1) {
                newCol = 0;
            }
        }
        if (boardModel.isPathPortalAware(newRow, newCol, true)) {
            ghost.move(move[0], move[1]);
            setGhostDirectionFromMove(ghost, move);
            ghost.setAnimationFrame((ghost.getAnimationFrame() + 1) % 4);
        }
    }

    public synchronized void setFrightenedMode() {
        this.frightenedTicks = FRIGHTENED_DURATION_TICKS;
        for (Ghost ghost : ghosts) {
            if (ghost.getState() != Ghost.State.EATEN && ghost.getState() != Ghost.State.IN_HOUSE) {
                ghost.setState(Ghost.State.FRIGHTENED);
            }
        }
    }

    public void clearGhosts() { ghosts.clear(); }
    public void addGhost(Ghost ghost) { ghosts.add(ghost); }

} 