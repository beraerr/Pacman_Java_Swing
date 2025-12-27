package model;

import java.util.*;

public class UpgradeManager {
    private final BoardModel boardModel;
    private final Random rand = new Random();
    private final List<BoardModel.CellContent> upgradeTypes = Arrays.asList(
            BoardModel.CellContent.FREEZE_UPGRADE,
            BoardModel.CellContent.SLOWDOWN_UPGRADE,
            BoardModel.CellContent.SPEED_UPGRADE,
            BoardModel.CellContent.SHIELD_UPGRADE,
            BoardModel.CellContent.HEALTH_UPGRADE
    );
    private long lastUpgradeSpawnTime = System.currentTimeMillis();
    private static final int UPGRADE_SPAWN_INTERVAL_MS = 5000;

    private volatile boolean freezeActive = false;
    private volatile boolean slowdownActive = false;
    private volatile boolean speedActive = false;
    private volatile boolean shieldActive = false;
    private long freezeEndTime = 0;
    private long slowdownEndTime = 0;
    private long speedEndTime = 0;
    private long shieldEndTime = 0;
    private static final int FREEZE_DURATION_MS = 4000;
    private static final int SLOWDOWN_DURATION_MS = 4000;
    private static final int SPEED_DURATION_MS = 4000;
    private static final int SHIELD_DURATION_MS = 4000;
    private volatile boolean effectTimerRunning = true;
    private Thread effectTimerThread;

    private static final int FRUIT_INTERVAL = 2;
    private static final int FIRST_FRUIT_PELLETS = 1;
    private static final int FRUIT_DURATION = 600;
    private int fruitTimer = 0;
    private int fruitStage = 0;
    private int fruitRow = -1, fruitCol = -1;
    private final java.util.List<BoardModel.CellContent> eatenFruitsThisLevel = new java.util.ArrayList<>();
    public java.util.List<BoardModel.CellContent> getEatenFruitsThisLevel() { return new java.util.ArrayList<>(eatenFruitsThisLevel); }

    public UpgradeManager(BoardModel boardModel) {
        this.boardModel = boardModel;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastUpgradeSpawnTime >= UPGRADE_SPAWN_INTERVAL_MS) {
            lastUpgradeSpawnTime = now;
            spawnUpgrades();
        }
        if (fruitTimer > 0) {
            fruitTimer--;
            if (fruitTimer == 0 && fruitRow >= 0 && fruitCol >= 0) {
                BoardModel.CellContent cell = boardModel.getCellContent(fruitRow, fruitCol);
                if (cell == BoardModel.CellContent.CHERRY || cell == BoardModel.CellContent.STRAWBERRY || cell == BoardModel.CellContent.APPLE) {
                    boardModel.setCellContent(fruitRow, fruitCol, BoardModel.CellContent.NONE);
                }
                fruitRow = -1;
                fruitCol = -1;
            }
        }
    }

    public void spawnUpgrades() {
        List<Ghost> ghosts = boardModel.getGhosts();
        for (Ghost ghost : ghosts) {
            if (rand.nextInt(100) < 25) { // 25% chance
                int attempts = 0;
                while (attempts < 50) {
                    int r = 1 + rand.nextInt(boardModel.getRowCount() - 2);
                    int c = 1 + rand.nextInt(boardModel.getColumnCount() - 2);
                    if (boardModel.getCell(r, c) == BoardModel.CellType.PATH &&
                        boardModel.getCellContent(r, c) == BoardModel.CellContent.PELLET) {
                        BoardModel.CellContent upgrade = upgradeTypes.get(rand.nextInt(upgradeTypes.size()));
                        boardModel.setCellContent(r, c, upgrade);
                        break;
                    }
                    attempts++;
                }
            }
        }
    }

    public void eatUpgradeIfPresent() {
        Pacman pacman = boardModel.getPacman();
        int r = pacman.getRow();
        int c = pacman.getCol();
        BoardModel.CellContent cell = boardModel.getCellContent(r, c);
        switch (cell) {
            case FREEZE_UPGRADE -> {
                boardModel.setCellContent(r, c, BoardModel.CellContent.NONE);
                freezeActive = true;
                freezeEndTime = System.currentTimeMillis() + FREEZE_DURATION_MS;
                boardModel.interruptGhostThreadIfPresent();
            }
            case SLOWDOWN_UPGRADE -> {
                boardModel.setCellContent(r, c, BoardModel.CellContent.NONE);
                slowdownActive = true;
                slowdownEndTime = System.currentTimeMillis() + SLOWDOWN_DURATION_MS;
            }
            case SPEED_UPGRADE -> {
                boardModel.setCellContent(r, c, BoardModel.CellContent.NONE);
                speedActive = true;
                speedEndTime = System.currentTimeMillis() + SPEED_DURATION_MS;
            }
            case SHIELD_UPGRADE -> {
                boardModel.setCellContent(r, c, BoardModel.CellContent.NONE);
                shieldActive = true;
                shieldEndTime = System.currentTimeMillis() + SHIELD_DURATION_MS;
            }
            case HEALTH_UPGRADE -> {
                boardModel.setCellContent(r, c, BoardModel.CellContent.NONE);
                boardModel.incrementLives();
            }
            default -> {}
        }
    }

    public int getGhostSleep(int currentLevel) {
        if (slowdownActive) return Math.max(160, 300 - (currentLevel - 1) * 40 + 120);
        else if (freezeActive) {
            return 9999999;
        }
        else return Math.max(80, 300 - (currentLevel - 1) * 40);
    }
    public int getPacmanSleep() {
        if (speedActive) return 100;
        else return 140;
    }
    public boolean isShieldActive() { return shieldActive; }
    public boolean isFreezeActive() { return freezeActive; }
    public boolean isSlowdownActive() { return slowdownActive; }
    public boolean isSpeedActive() { return speedActive; }

    public void startEffectTimerThread() {
        effectTimerRunning = true;
        effectTimerThread = new Thread(() -> {
            while (effectTimerRunning) {
                long now = System.currentTimeMillis();
                if (freezeActive && now > freezeEndTime) {
                    freezeActive = false;
                }
                if (slowdownActive && now > slowdownEndTime) slowdownActive = false;
                if (speedActive && now > speedEndTime) speedActive = false;
                if (shieldActive && now > shieldEndTime) shieldActive = false;
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        });
        effectTimerThread.setDaemon(true);
        effectTimerThread.start();
    }

    public void maybeSpawnFruit() {
        int ticksSinceStart = boardModel.getTicksSinceStart();
        int pelletsEaten = boardModel.getPelletsEaten();
        int rows = boardModel.getRowCount();
        int cols = boardModel.getColumnCount();
        if (ticksSinceStart < 80) return;
        if (fruitTimer > 0) return;
        int requiredPellets = fruitStage == 0 ? FIRST_FRUIT_PELLETS : FIRST_FRUIT_PELLETS + fruitStage * FRUIT_INTERVAL;
        if (pelletsEaten >= requiredPellets && fruitStage < 3) {
            int centerR = rows / 2;
            int centerC = cols / 2;
            int[][] offsets = { {0,0}, {0,1}, {1,0}, {-1,0}, {0,-1}, {1,1}, {-1,-1}, {2,0}, {0,2}, {-2,0}, {0,-2} };
            outer: for (int[] off : offsets) {
                int rr = centerR + off[0];
                int cc = centerC + off[1];
                if (rr >= 0 && rr < rows && cc >= 0 && cc < cols && boardModel.getCellContent(rr, cc) == BoardModel.CellContent.NONE && boardModel.getCell(rr, cc) == BoardModel.CellType.PATH) {
                    boolean inHouse = false;
                    int houseHeight = Math.min(3, rows - 4);
                    int houseWidth = Math.min(5, cols - 4);
                    int houseTop = rows / 2 - houseHeight / 2;
                    int houseLeft = cols / 2 - houseWidth / 2;
                    if (rr >= houseTop && rr < houseTop + houseHeight && cc >= houseLeft && cc < houseLeft + houseWidth) inHouse = true;
                    boolean onGhost = false;
                    for (Ghost g : boardModel.getGhosts()) if (g.getRow() == rr && g.getCol() == cc) onGhost = true;
                    if (!inHouse && !onGhost) {
                        fruitRow = rr;
                        fruitCol = cc;
                        BoardModel.CellContent fruitType = switch (fruitStage) {
                            case 0 -> BoardModel.CellContent.CHERRY;
                            case 1 -> BoardModel.CellContent.STRAWBERRY;
                            default -> BoardModel.CellContent.APPLE;
                        };
                        boardModel.setCellContent(rr, cc, fruitType);
                        fruitTimer = FRUIT_DURATION;
                        fruitStage++;
                    }
                }
            }
        }
    }

    public void eatFruitIfPresent() {
        Pacman pacman = boardModel.getPacman();
        int r = pacman.getRow();
        int c = pacman.getCol();
        BoardModel.CellContent[][] content = boardModel.getContentArray();
        if (content[r][c] == BoardModel.CellContent.CHERRY) {
            content[r][c] = BoardModel.CellContent.NONE;
            boardModel.addScore(100);
            eatenFruitsThisLevel.add(BoardModel.CellContent.CHERRY);
            boardModel.fireScoreChanged();
            fruitTimer = 0;
            fruitRow = fruitCol = -1;
        } else if (content[r][c] == BoardModel.CellContent.STRAWBERRY) {
            content[r][c] = BoardModel.CellContent.NONE;
            boardModel.addScore(300);
            eatenFruitsThisLevel.add(BoardModel.CellContent.STRAWBERRY);
            boardModel.fireScoreChanged();
            fruitTimer = 0;
            fruitRow = fruitCol = -1;
        } else if (content[r][c] == BoardModel.CellContent.APPLE) {
            content[r][c] = BoardModel.CellContent.NONE;
            boardModel.addScore(700);
            eatenFruitsThisLevel.add(BoardModel.CellContent.APPLE);
            boardModel.fireScoreChanged();
            fruitTimer = 0;
            fruitRow = fruitCol = -1;
        }
    }

    public void resetFruitState() {
        fruitStage = 0;
        fruitTimer = 0;
        fruitRow = -1;
        fruitCol = -1;
        eatenFruitsThisLevel.clear();
    }

    public int getFruitRow() { return fruitRow; }
    public int getFruitCol() { return fruitCol; }

} 