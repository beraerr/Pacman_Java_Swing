package model;

import javax.swing.table.AbstractTableModel;
import java.util.Random;
import java.util.List;

public class BoardModel extends AbstractTableModel {
    public enum CellType { WALL, PATH }
    public enum CellContent { NONE, PELLET, POWER_PELLET, CHERRY, STRAWBERRY, APPLE, FREEZE_UPGRADE, SLOWDOWN_UPGRADE, SPEED_UPGRADE, SHIELD_UPGRADE, HEALTH_UPGRADE }
    private final int rows;
    private final int cols;
    private final CellType[][] board;
    private final CellContent[][] content;
    private PacmanManager pacmanManager;
    private int currentLevel = 1;
    private int score = 0;
    private int lives = 3;
    private final java.util.List<java.util.function.IntConsumer> scoreListeners = new java.util.ArrayList<>();
    private final java.util.List<java.util.function.IntConsumer> livesListeners = new java.util.ArrayList<>();
    private int ticksSinceStart = 0;
    private int pelletsEaten = 0;

    public int getScore() { return score; }
    public void addScoreListener(java.util.function.IntConsumer listener) { scoreListeners.add(listener); }
    public void fireScoreChanged() { for (var l : scoreListeners) l.accept(score); }
    public int getLives() { return lives; }
    public void addLivesListener(java.util.function.IntConsumer listener) { livesListeners.add(listener); }
    public void fireLivesChanged() { for (var l : livesListeners) l.accept(lives); }
    public void incrementLives() { this.lives++; fireLivesChanged(); }

    private final HighScoreManager highScoreManager = new HighScoreManager();
    private UpgradeManager upgradeManager;
    private GhostManager ghostManager;

    public BoardModel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.ghostManager = new GhostManager(this);
        this.board = new CellType[rows][cols];
        this.content = new CellContent[rows][cols];
        generateMaze();
        ensureExitConnectivity();
        int houseHeight = Math.min(3, rows - 4);
        int houseWidth = Math.min(5, cols - 4);
        int houseTop = rows / 2 - houseHeight / 2;
        int houseLeft = cols / 2 - houseWidth / 2;
        int pacmanRow = 1, pacmanCol = 1;
        outer: for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                boolean inHouse = r >= houseTop && r < houseTop + houseHeight && c >= houseLeft && c < houseLeft + houseWidth;
                if (board[r][c] == CellType.PATH && !inHouse) {
                    pacmanRow = r;
                    pacmanCol = c;
                    break outer;
                }
            }
        }
        boolean pacmanInHouse = pacmanRow >= houseTop && pacmanRow < houseTop + houseHeight && pacmanCol >= houseLeft && pacmanCol < houseLeft + houseWidth;
        if ((pacmanRow == 1 && pacmanCol == 1 && (board[1][1] != CellType.PATH || pacmanInHouse))) {
            throw new IllegalStateException("No valid starting cell for Pac-Man outside the ghost house. Maze generation or ghost house sizing may be invalid.");
        }
        pacmanManager = new PacmanManager(this, pacmanRow, pacmanCol);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == CellType.PATH) {
                    content[r][c] = CellContent.PELLET;
                } else {
                    content[r][c] = CellContent.NONE;
                }
            }
        }
        content[pacmanRow][pacmanCol] = CellContent.NONE;
        for (int r = houseTop; r < houseTop + houseHeight; r++) {
            for (int c = houseLeft; c < houseLeft + houseWidth; c++) {
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    content[r][c] = CellContent.NONE;
                }
            }
        }

        int[][] corners = { {1,1}, {1,cols-2}, {rows-2,1}, {rows-2,cols-2} };
        for (int[] corner : corners) {
            int r = corner[0], c = corner[1];
            if (r >= 0 && r < rows && c >= 0 && c < cols && board[r][c] == CellType.PATH) {
                content[r][c] = CellContent.POWER_PELLET;
            }
        }

        int centerR = rows / 2;
        int centerC = cols / 2;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int rr = centerR + dr;
                int cc = centerC + dc;
                if (rr >= 0 && rr < rows && cc >= 0 && cc < cols) {
                    board[rr][cc] = CellType.PATH;
                    content[rr][cc] = CellContent.NONE;
                }
            }
        }

        ghostManager.clearGhosts();
        int ghostStartRow1 = houseTop + houseHeight - 2;
        int ghostStartRow2 = houseTop + houseHeight - 1;
        int ghostStartCol1 = houseLeft;
        int ghostStartCol2 = houseLeft + 1;
        ghostManager.addGhost(new Ghost(Ghost.Type.BLINKY, ghostStartRow1, ghostStartCol1));
        ghostManager.addGhost(new Ghost(Ghost.Type.PINKY, ghostStartRow1, ghostStartCol2));
        ghostManager.addGhost(new Ghost(Ghost.Type.INKY, ghostStartRow2, ghostStartCol1));
        ghostManager.addGhost(new Ghost(Ghost.Type.CLYDE, ghostStartRow2, ghostStartCol2));
        int exitRow = houseTop - 1;
        int exitCol = houseLeft + houseWidth / 2;
        if (exitRow >= 0 && exitCol >= 0 && exitCol < cols) {
            board[exitRow][exitCol] = CellType.PATH;
            ensureExitConnectivity();
        }
        this.upgradeManager = new UpgradeManager(this);
    }

    private void generateMaze() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c] = CellType.WALL;
            }
        }
        int mazeRows = (rows % 2 == 0) ? rows - 1 : rows;
        int mazeCols = (cols % 2 == 0) ? cols - 1 : cols;
        boolean[][] visited = new boolean[mazeRows][mazeCols];
        dfs(1, 1, visited, mazeRows, mazeCols);

        if (rows % 2 == 0) {
            for (int c = 1; c < cols - 1; c++) board[rows - 2][c] = CellType.PATH;
        }
        if (cols % 2 == 0) {
            for (int r = 1; r < rows - 1; r++) board[r][cols - 2] = CellType.PATH;
        }

        int extraLoops;
        if (rows < 15 || cols < 15) extraLoops = Math.max(1, (rows * cols) / 30);
        else if (rows <= 25 || cols <= 25) extraLoops = (rows * cols) / 12;
        else extraLoops = (rows * cols) / 7;
        Random rand = new Random();
        for (int i = 0; i < extraLoops; i++) {
            int r = 1 + 2 * rand.nextInt((mazeRows - 1) / 2);
            int c = 1 + 2 * rand.nextInt((mazeCols - 1) / 2);
            int[][] dirs = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };
            int[] dir = dirs[rand.nextInt(4)];
            int nr = r + dir[0];
            int nc = c + dir[1];
            if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1) {
                if (board[r][c] == CellType.PATH && board[nr][nc] == CellType.WALL) {
                    int rr = r + dir[0];
                    int cc = c + dir[1];
                    if (rr > 0 && rr < rows - 1 && cc > 0 && cc < cols - 1) {
                        if (board[r - dir[0]][c - dir[1]] == CellType.PATH) {
                            board[nr][nc] = CellType.PATH;
                        }
                    }
                }
            }
        }

        int houseHeight = Math.max(2, Math.min(3, rows - 4));
        int houseWidth = Math.max(3, Math.min(5, cols - 4));
        int houseTop = Math.max(1, (rows - houseHeight) / 2);
        int houseLeft = Math.max(1, (cols - houseWidth) / 2);
        if (houseTop + houseHeight >= rows - 1) houseTop = rows - 1 - houseHeight;
        if (houseLeft + houseWidth >= cols - 1) houseLeft = cols - 1 - houseWidth;
        if (rows <= 12 || cols <= 12) {
        }
        for (int r = houseTop; r < houseTop + houseHeight; r++) {
            for (int c = houseLeft; c < houseLeft + houseWidth; c++) {
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    board[r][c] = CellType.PATH;
                }
            }
        }
        ensureExitConnectivity();


        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols / 2; c++) {
                board[r][cols - 1 - c] = board[r][c];
            }
        }

        int[] drc = {-1, 0, 1, 0, -1};
        int maxIterations = rows * cols;
        int iter = 0;
        while (iter++ < maxIterations) {
            boolean[][] visitedConn = new boolean[rows][cols];
            java.util.Queue<int[]> queue = new java.util.LinkedList<>();
            boolean foundStart = false;
            outer: for (int r = 1; r < rows - 1; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    if (board[r][c] == CellType.PATH) {
                        queue.add(new int[]{r, c});
                        visitedConn[r][c] = true;
                        foundStart = true;
                        break outer;
                    }
                }
            }
            if (!foundStart) break;
            while (!queue.isEmpty()) {
                int[] pos = queue.poll();
                int r = pos[0], c = pos[1];
                for (int d = 0; d < 4; d++) {
                    int nr = r + drc[d];
                    int nc = c + drc[d + 1];
                    if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && !visitedConn[nr][nc] && board[nr][nc] == CellType.PATH) {
                        visitedConn[nr][nc] = true;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
            boolean allConnected = true;
            int ur = -1, uc = -1;
            for (int r = 1; r < rows - 1 && allConnected; r++) {
                for (int c = 1; c < cols - 1 && allConnected; c++) {
                    if (board[r][c] == CellType.PATH && !visitedConn[r][c]) {
                        ur = r; uc = c;
                        allConnected = false;
                    }
                }
            }
            if (allConnected) break;
            java.util.Queue<int[]> bfs = new java.util.LinkedList<>();
            boolean[][] visitedBFS = new boolean[rows][cols];
            bfs.add(new int[]{ur, uc});
            visitedBFS[ur][uc] = true;
            boolean connected = false;
            while (!bfs.isEmpty() && !connected) {
                int[] pos = bfs.poll();
                int r = pos[0], c = pos[1];
                for (int d = 0; d < 4; d++) {
                    int nr = r + drc[d];
                    int nc = c + drc[d + 1];
                    if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && !visitedBFS[nr][nc]) {
                        if (board[nr][nc] == CellType.WALL) {
                            int nnr = nr + drc[d];
                            int nnc = nc + drc[d + 1];
                            if (nnr > 0 && nnr < rows - 1 && nnc > 0 && nnc < cols - 1 && visitedConn[nnr][nnc]) {
                                board[nr][nc] = CellType.PATH;
                                connected = true;
                                break;
                            }
                        } else if (board[nr][nc] == CellType.PATH) {
                            bfs.add(new int[]{nr, nc});
                            visitedBFS[nr][nc] = true;
                        }
                    }
                }
            }
        }

        int deadEndRemovals = (rows < 15 || cols < 15) ? Math.max(1, (rows * cols) / 40) : (rows * cols) / 20;
        for (int i = 0; i < deadEndRemovals; i++) {
            for (int r = 1; r < rows - 1; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    if (board[r][c] == CellType.PATH) {
                        int exits = 0, lastDir = -1;
                        for (int d = 0; d < 4; d++) {
                            int nr = r + drc[d];
                            int nc = c + drc[d + 1];
                            if (board[nr][nc] == CellType.PATH) {
                                exits++;
                                lastDir = d;
                            }
                        }
                        if (exits == 1) {
                            int nr = r + drc[lastDir];
                            int nc = c + drc[lastDir + 1];
                            if (board[nr][nc] == CellType.WALL) {
                                board[nr][nc] = CellType.PATH;
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i == 0 || i == rows-1 || j == 0 || j == cols-1) {
                    board[i][j] = CellType.WALL;
                }
            }
        }

        int numPortalRows = 1;
        if (rows >= 40) numPortalRows = 5;
        else if (rows >= 20) numPortalRows = 3;
        int[] portalRows;
        if (numPortalRows == 1) {
            portalRows = new int[] { rows / 2 };
        } else if (numPortalRows == 3) {
            portalRows = new int[] { rows / 4, rows / 2, (3 * rows) / 4 };
        } else if (numPortalRows == 5) {
            portalRows = new int[] { rows / 6, rows / 3, rows / 2, (2 * rows) / 3, (5 * rows) / 6 };
        } else {
            portalRows = new int[] { rows / 2 };
        }
        for (int r : portalRows) {
            if (r > 0 && r < rows - 1) {
                for (int c = 0; c < 3 && c < cols; c++) {
                    board[r][c] = CellType.PATH;
                    board[r][cols-1-c] = CellType.PATH;
                }
            }
        }
    }

    private void dfs(int r, int c, boolean[][] visited, int mazeRows, int mazeCols) {
        visited[r][c] = true;
        board[r][c] = CellType.PATH;
        int[] dr = { -2, 2, 0, 0 };
        int[] dc = { 0, 0, -2, 2 };
        Integer[] dirs = {0, 1, 2, 3};
        java.util.Collections.shuffle(java.util.Arrays.asList(dirs));
        for (int i = 0; i < 4; i++) {
            int dir = dirs[i];
            int nr = r + dr[dir];
            int nc = c + dc[dir];
            if (nr > 0 && nr < mazeRows && nc > 0 && nc < mazeCols && !visited[nr][nc]) {
                board[r + dr[dir]/2][c + dc[dir]/2] = CellType.PATH;
                dfs(nr, nc, visited, mazeRows, mazeCols);
            }
        }
    }

    public CellType getCell(int row, int col) {
        return board[row][col];
    }

    public CellContent getCellContent(int row, int col) {
        return content[row][col];
    }

    @Override
    public int getRowCount() {
        return rows;
    }

    @Override
    public int getColumnCount() {
        return cols;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return board[rowIndex][columnIndex];
    }

    public Pacman getPacman() {
        return pacmanManager.getPacman();
    }

    public boolean movePacmanTick() {
        boolean moved = pacmanManager.movePacmanTick();
        if (moved) {
            if (upgradeManager != null) {
                upgradeManager.eatFruitIfPresent();
            }
        }
        fireTableDataChanged();
        return moved;
    }

    public int[] getDelta(Pacman.Direction dir) {
        return switch (dir) {
            case UP -> new int[]{-1, 0};
            case DOWN -> new int[]{1, 0};
            case LEFT -> new int[]{0, -1};
            case RIGHT -> new int[]{0, 1};
        };
    }

    public boolean isPortalRow(int row) {
        return board[row][0] == CellType.PATH && board[row][cols-1] == CellType.PATH;
    }

    public boolean isPathPortalAware(int row, int col, boolean isGhost) {
        if (row < 0 || row >= rows) return false;
        if (col < 0 || col >= cols) return false;
        if (board[row][col] != CellType.PATH) return false;
        if (col == 0 || col == cols-1) {
            if (isGhost && currentLevel == 1) return false;
            return isPortalRow(row);
        }
        return true;
    }

    public void resetPositions() {
        int houseHeight = Math.min(3, rows - 4);
        int houseWidth = Math.min(5, cols - 4);
        int houseTop = rows / 2 - houseHeight / 2;
        int houseLeft = cols / 2 - houseWidth / 2;
        int pacmanRow = 1, pacmanCol = 1;
        outer: for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                boolean inHouse = r >= houseTop && r < houseTop + houseHeight && c >= houseLeft && c < houseLeft + houseWidth;
                if (board[r][c] == CellType.PATH && !inHouse) {
                    pacmanRow = r;
                    pacmanCol = c;
                    break outer;
                }
            }
        }
        pacmanManager.resetPacmanPosition(pacmanRow, pacmanCol);
        int exitRow = houseTop + houseHeight;
        int exitCol = houseLeft + houseWidth / 2;
        for (Ghost g : ghostManager.getGhosts()) {
            g.setPosition(exitRow, exitCol);
            g.setState(Ghost.State.LEAVING_HOUSE);
        }
        board[exitRow][exitCol] = CellType.PATH;
        ensureExitConnectivity();
        ticksSinceStart = 0;
        pelletsEaten = 0;
        if (upgradeManager != null) upgradeManager.resetFruitState();
    }

    public void addScore(int add) { this.score += add; fireScoreChanged(); }
    public void decrementLives() { this.lives--; fireLivesChanged(); }

    public boolean eatPelletIfPresent() {
        int r = getPacman().getRow();
        int c = getPacman().getCol();
        if (content[r][c] == CellContent.PELLET) {
            content[r][c] = CellContent.NONE;
            score += 10;
            incrementPelletsEaten();
            fireScoreChanged();
            return true;
        } else if (content[r][c] == CellContent.POWER_PELLET) {
            content[r][c] = CellContent.NONE;
            score += 50;
            ghostManager.setFrightenedMode();
            fireScoreChanged();
            return true;
        }
        return false;
    }

    public void eatFruitIfPresent() {
        int r = getPacman().getRow();
        int c = getPacman().getCol();
        if (content[r][c] == CellContent.CHERRY) {
            content[r][c] = CellContent.NONE;
            score += 100;
            fireScoreChanged();
        } else if (content[r][c] == CellContent.STRAWBERRY) {
            content[r][c] = CellContent.NONE;
            score += 300;
            fireScoreChanged();
        } else if (content[r][c] == CellContent.APPLE) {
            content[r][c] = CellContent.NONE;
            score += 700;
            fireScoreChanged();
        }
    }

    public void moveGhostsTick() {
        ghostManager.moveGhostsTick();
        if (upgradeManager != null) {
            upgradeManager.tick();
        }
    }
    public java.util.List<Ghost> getGhosts() { return ghostManager.getGhosts(); }
    public boolean handlePacmanGhostCollision() { return ghostManager.handlePacmanGhostCollision(); }

    private void ensureExitConnectivity() {
        int houseHeight = Math.max(2, Math.min(3, rows - 4));
        int houseWidth = Math.max(3, Math.min(5, cols - 4));
        int houseTop = rows / 2 - houseHeight / 2;
        int houseLeft = cols / 2 - houseWidth / 2;
        int exitRow = houseTop + houseHeight;
        int exitCol = houseLeft + houseWidth / 2;
        if (exitRow >= 0 && exitRow < rows && exitCol >= 0 && exitCol < cols && board[exitRow][exitCol] != CellType.PATH) {
            board[exitRow][exitCol] = CellType.PATH;
        }
    }

    public List<HighScoreManager.HighScoreEntry> getHighScores() {
        return highScoreManager.getHighScores();
    }


    public boolean allPelletsEaten() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (content[r][c] == CellContent.PELLET || content[r][c] == CellContent.POWER_PELLET) {
                    return false;
                }
            }
        }
        return true;
    }


    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public int getGhostSleep() {
        return (upgradeManager != null) ? upgradeManager.getGhostSleep(currentLevel) : 300;
    }
    public int getPacmanSleep() {
        return (upgradeManager != null) ? upgradeManager.getPacmanSleep() : 120;
    }
    public boolean isShieldActive() {
        return (upgradeManager != null) && upgradeManager.isShieldActive();
    }
    public boolean isFreezeActive() {
        return (upgradeManager != null) && upgradeManager.isFreezeActive();
    }
    public boolean isSlowdownActive() {
        return (upgradeManager != null) && upgradeManager.isSlowdownActive();
    }
    public boolean isSpeedActive() {
        return (upgradeManager != null) && upgradeManager.isSpeedActive();
    }

    public void setCellContent(int row, int col, CellContent content) {
        this.content[row][col] = content;
    }

    public GhostManager getGhostManager() { return ghostManager; }

    private Runnable ghostThreadInterruptCallback;
    public void setGhostThreadInterruptCallback(Runnable callback) {
        this.ghostThreadInterruptCallback = callback;
    }
    public void interruptGhostThreadIfPresent() {
        if (ghostThreadInterruptCallback != null) ghostThreadInterruptCallback.run();
    }

    public void incrementTicksSinceStart() { this.ticksSinceStart++; }
    public void incrementPelletsEaten() { this.pelletsEaten++; }

    public int getTicksSinceStart() { return ticksSinceStart; }
    public int getPelletsEaten() { return pelletsEaten; }
    public CellContent[][] getContentArray() { return content; }

    public java.util.List<CellContent> getEatenFruitsThisLevel() {
        return upgradeManager != null ? upgradeManager.getEatenFruitsThisLevel() : java.util.Collections.emptyList();
    }

    public HighScoreManager getHighScoreManager() { return highScoreManager; }

    public int getFruitRow() { return upgradeManager != null ? upgradeManager.getFruitRow() : -1; }
    public int getFruitCol() { return upgradeManager != null ? upgradeManager.getFruitCol() : -1; }
} 