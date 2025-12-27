package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import model.BoardModel;
import model.Pacman;
import javax.swing.table.TableCellRenderer;
import model.Ghost;
import java.util.Random;
import javax.swing.DefaultListModel;
import javax.sound.sampled.*;
import java.awt.event.ActionListener;
import controller.GameController;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class GameView extends JFrame {
    private final BoardModel boardModel;
    private final JTable table;
    private final Map<String, ImageIcon[]> pacmanImages = new HashMap<>();
    private final Map<String, ImageIcon[]> ghostImages = new HashMap<>();
    private static final int CELL_SIZE = 28;
    private static final int ANIMATION_FRAMES = 4;
    private int level = 1;
    private Thread pacmanThread;
    private Thread ghostThread;
    private volatile boolean running = true;
    private final ImageIcon pelletIcon = new ImageIcon("assets/images/other/dot.png");
    private final ImageIcon bigDotIcon = loadBigDotIcon();
    private final ImageIcon cherryIcon = new ImageIcon("assets/images/other/cherry.png");
    private final ImageIcon strawberryIcon = new ImageIcon("assets/images/other/strawberry.png");
    private final ImageIcon appleIcon = new ImageIcon("assets/images/other/apple.png");
    private static final int EXTRA_UI_HEIGHT = 80;
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JPanel livesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    private final ImageIcon lifeIcon = new ImageIcon("assets/images/pacman-right/1.png");
    private volatile boolean powerPelletVisible = true;
    private Thread powerPelletFlashThread;
    private final JLabel fruitLabel = new JLabel();
    private final JLabel levelLabel = new JLabel();
    private final java.util.List<ImageIcon> eatenFruits = new java.util.ArrayList<>();
    private final JPanel collectedFruitsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
    private boolean soundOn = true;
    private Clip chompClip;
    private JButton soundButton;
    private final ImageIcon soundOnIcon;
    private final ImageIcon soundOffIcon;
    private final ImageIcon freezeUpgradeIcon = getScaledIcon(new ImageIcon("assets/images/upgrades/freeze_upgrade.png"), 24, 24);
    private final ImageIcon slowdownUpgradeIcon = getScaledIcon(new ImageIcon("assets/images/upgrades/slowdown_upgrade.png"), 24, 24);
    private final ImageIcon speedUpgradeIcon = getScaledIcon(new ImageIcon("assets/images/upgrades/speed_upgrade.png"), 24, 24);
    private final ImageIcon shieldUpgradeIcon = getScaledIcon(new ImageIcon("assets/images/upgrades/shield_upgrade.png"), 24, 24);
    private final ImageIcon healthUpgradeIcon = getScaledIcon(new ImageIcon("assets/images/upgrades/health_upgrade.png"), 24, 24);
    private int lastLives = -1;

    public GameView(int rows, int cols) {
        this(rows, cols, 1);
    }

    public GameView(int rows, int cols, int level) {
        this.level = level;
        eatenFruits.clear();
        setTitle("Pacman - Game (Level " + level + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        java.io.File soundOnFile = new java.io.File("assets/images/other/sound_on.png");
        java.io.File soundOffFile = new java.io.File("assets/images/other/sound_off.png");
        soundOnIcon = loadSoundIconWithFallback(soundOnFile, Color.GREEN);
        soundOffIcon = loadSoundIconWithFallback(soundOffFile, Color.RED);

        boardModel = new BoardModel(rows, cols);
        boardModel.setGhostThreadInterruptCallback(() -> {
            if (ghostThread != null && ghostThread.isAlive()) ghostThread.interrupt();
        });
        boardModel.getUpgradeManager().startEffectTimerThread();
        table = new JTable(boardModel);
        table.setRowHeight(CELL_SIZE);
        for (int c = 0; c < cols; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(CELL_SIZE);
        }
        table.setEnabled(false);
        table.setCellSelectionEnabled(false);
        table.setFocusable(false);
        table.setTableHeader(null);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        loadPacmanImages();
        loadGhostImages();

        table.setDefaultRenderer(Object.class, (TableCellRenderer) (table1, value, isSelected, hasFocus, row, column) -> {
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    Color neonBlue = new Color(0, 40, 180);
                    int thickness = Math.max(4, getWidth() / 8);
                    BoardModel.CellType[][] board = (BoardModel.CellType[][]) null;
                    try {
                        java.lang.reflect.Field f = boardModel.getClass().getDeclaredField("board");
                        f.setAccessible(true);
                        board = (BoardModel.CellType[][]) f.get(boardModel);
                    } catch (Exception ignored) {}
                    if (board != null && board[row][column] == BoardModel.CellType.WALL) {
                        g.setColor(neonBlue);
                        for (int t = 0; t < thickness; t++) {
                            g.drawRect(t, t, getWidth() - 1 - 2*t, getHeight() - 1 - 2*t);
                        }
                    }
                    for (Ghost ghost : boardModel.getGhosts()) {
                        if (ghost.getRow() == row && ghost.getCol() == column) {
                            if (ghost.getState() == Ghost.State.EATEN) {
                                ImageIcon eyesIcon = new ImageIcon("assets/images/ghosts/eyes.png");
                                int pad = (int)(getWidth() * 0.1);
                                int size = (int)(getWidth() * 0.8);
                                g.drawImage(eyesIcon.getImage(), pad, pad, size, size, null);
                            } else if (ghost.getState() == Ghost.State.FRIGHTENED) {
                                ImageIcon blueIcon = new ImageIcon("assets/images/other/eat_ghost.jpg");
                                int pad = (int)(getWidth() * 0.1);
                                int size = (int)(getWidth() * 0.8);
                                g.drawImage(blueIcon.getImage(), pad, pad, size, size, null);
                            } else {
                                String type = ghost.getType().name().toLowerCase();
                                ImageIcon[] icons = ghostImages.get(type);
                                if (icons != null && icons[0] != null) {
                                    int pad = (int)(getWidth() * 0.1);
                                    int size = (int)(getWidth() * 0.8);
                                    g.drawImage(icons[0].getImage(), pad, pad, size, size, null);
                                } else {
                                    g.setColor(Color.RED);
                                    int pad = (int)(getWidth() * 0.1);
                                    int size = (int)(getWidth() * 0.8);
                                    g.fillOval(pad, pad, size, size);
                                }
                            }
                        }
                    }
                    BoardModel.CellContent content = boardModel.getCellContent(row, column);
                    if (content == BoardModel.CellContent.FREEZE_UPGRADE) {
                        if (freezeUpgradeIcon != null && freezeUpgradeIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(freezeUpgradeIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    } else if (content == BoardModel.CellContent.SLOWDOWN_UPGRADE) {
                        if (slowdownUpgradeIcon != null && slowdownUpgradeIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(slowdownUpgradeIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    } else if (content == BoardModel.CellContent.SPEED_UPGRADE) {
                        if (speedUpgradeIcon != null && speedUpgradeIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(speedUpgradeIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    } else if (content == BoardModel.CellContent.SHIELD_UPGRADE) {
                        if (shieldUpgradeIcon != null && shieldUpgradeIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(shieldUpgradeIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    } else if (content == BoardModel.CellContent.HEALTH_UPGRADE) {
                        if (healthUpgradeIcon != null && healthUpgradeIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(healthUpgradeIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    } else if (content == BoardModel.CellContent.PELLET) {
                        if (pelletIcon.getImage() != null) {
                            int size = getWidth() / 3;
                            g.drawImage(pelletIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        } else {
                            g.setColor(Color.ORANGE);
                            int size = getWidth() / 3;
                            g.fillOval(getWidth()/2 - size/2, getHeight()/2 - size/2, size, size);
                        }
                    } else if (content == BoardModel.CellContent.POWER_PELLET) {
                        if (powerPelletVisible) {
                            if (bigDotIcon != null && bigDotIcon.getImage() != null) {
                                int size = (int)(getWidth() * 0.45);
                                g.drawImage(bigDotIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                            } else {
                                g.setColor(Color.PINK);
                                int size = (int)(getWidth() * 0.45);
                                g.fillOval(getWidth()/2 - size/2, getHeight()/2 - size/2, size, size);
                            }
                        }
                    } else if (content == BoardModel.CellContent.CHERRY) {
                        if (cherryIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(cherryIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    } else if (content == BoardModel.CellContent.STRAWBERRY) {
                        if (strawberryIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(strawberryIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    } else if (content == BoardModel.CellContent.APPLE) {
                        if (appleIcon.getImage() != null) {
                            int size = (int)(getWidth() * 0.8);
                            g.drawImage(appleIcon.getImage(), getWidth()/2 - size/2, getHeight()/2 - size/2, size, size, null);
                        }
                    }
                    Pacman pacman = boardModel.getPacman();
                    if (pacman != null && pacman.getRow() == row && pacman.getCol() == column) {
                        String dir = pacman.getDirection().name().toLowerCase();
                        int frame = pacman.getAnimationFrame();
                        ImageIcon[] icons = pacmanImages.get(dir);
                        int pad = (int)(getWidth() * 0.1);
                        int size = (int)(getWidth() * 0.8);
                        if (icons != null && icons[frame] != null) {
                            g.drawImage(icons[frame].getImage(), pad, pad, size, size, null);
                        } else {
                            g.setColor(Color.YELLOW);
                            g.fillOval(pad, pad, size, size);
                        }
                    }
                }
            };
            return panel;
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        Font pacmanFont;
        try {
            pacmanFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets/pacman.ttf")).deriveFont(Font.BOLD, 24f);
        } catch (Exception e) {
            pacmanFont = new Font("Arial", Font.BOLD, 24);
        }
        scoreLabel.setFont(pacmanFont);
        scoreLabel.setForeground(Color.YELLOW);
        levelLabel.setFont(pacmanFont);
        levelLabel.setForeground(Color.YELLOW);

        JPanel scoreboardPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        scoreboardPanel.setBackground(Color.BLACK);
        scoreboardPanel.add(scoreLabel);
        scoreboardPanel.add(levelLabel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        statusPanel.setBackground(Color.BLACK);
        statusPanel.add(livesPanel);
        statusPanel.add(fruitLabel);
        statusPanel.add(collectedFruitsPanel);
        soundButton = new JButton(soundOnIcon);
        soundButton.setPreferredSize(new Dimension(20, 20));
        soundButton.setFocusable(false);
        soundButton.setContentAreaFilled(false);
        soundButton.setBorderPainted(false);
        soundButton.setOpaque(false);
        soundButton.addActionListener(e -> toggleSound());
        statusPanel.add(soundButton);

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(Color.BLACK);
        topPanel.add(scoreboardPanel);
        topPanel.add(statusPanel);
        add(topPanel, BorderLayout.NORTH);

        boardModel.addScoreListener(score -> SwingUtilities.invokeLater(() -> scoreLabel.setText("Score: " + score)));
        boardModel.addLivesListener(lives -> SwingUtilities.invokeLater(() -> updateLivesDisplay(lives)));
        updateLivesDisplay(boardModel.getLives());

        int topPanelHeight = topPanel.getPreferredSize().height;
        int tableHeight = table.getPreferredSize().height;
        int tableWidth = table.getPreferredSize().width;
        int scrollbarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
        int totalWidth = tableWidth + scrollbarWidth;
        int totalHeight = topPanelHeight + tableHeight + 40;
        setSize(totalWidth, totalHeight);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        SwingUtilities.invokeLater(() -> {
            if (soundOn && chompClip != null) {
                chompClip.setFramePosition(0);
                chompClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> boardModel.getPacman().setDesiredDirection(Pacman.Direction.UP);
                    case KeyEvent.VK_DOWN -> boardModel.getPacman().setDesiredDirection(Pacman.Direction.DOWN);
                    case KeyEvent.VK_LEFT -> boardModel.getPacman().setDesiredDirection(Pacman.Direction.LEFT);
                    case KeyEvent.VK_RIGHT -> boardModel.getPacman().setDesiredDirection(Pacman.Direction.RIGHT);
                }
            }
        });
        setFocusable(true);

        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new java.io.File("assets/music/pacman_chomp.wav"));
            chompClip = AudioSystem.getClip();
            chompClip.open(audioIn);
        } catch (Exception e) {
            chompClip = null;
        }

        pacmanThread = new Thread(() -> {
            if (lastLives == -1) lastLives = boardModel.getLives();
            while (running) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        int oldScore = boardModel.getScore();
                        boolean moved = boardModel.movePacmanTick();
                        boolean gameOver = boardModel.handlePacmanGhostCollision();
                        int currentLives = boardModel.getLives();
                        if (currentLives < lastLives) {
                            if (ghostThread != null && ghostThread.isAlive()) {
                                ghostThread.interrupt();
                            }
                        }
                        lastLives = currentLives;
                        if (boardModel.allPelletsEaten()) {
                            running = false;
                            stopThreadsAndDispose();
                            int score = boardModel.getScore();
                            showWinDialog(score);
                        } else if (gameOver) {
                            running = false;
                            stopThreadsAndDispose();
                            int score = boardModel.getScore();
                            showGameOverDialog(score);
                        } else if (!running) {
                        } else if (livesPanel.getComponentCount() == 0) {
                            running = false;
                            stopThreadsAndDispose();
                            int score = boardModel.getScore();
                            showGameOverDialog(score);
                        }
                    });
                    Thread.sleep(boardModel.getPacmanSleep());
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        pacmanThread.start();

        ghostThread = new Thread(() -> {
            long lastMove = System.currentTimeMillis();
            while (running) {
                try {
                    long now = System.currentTimeMillis();
                    int ghostSleep = boardModel.getGhostSleep();
                        SwingUtilities.invokeAndWait(() -> {
                        boardModel.moveGhostsTick();
                                checkAndAddCollectedFruit();
                        updateFruitDisplay();
                                boolean gameOver = boardModel.handlePacmanGhostCollision();
                                if (gameOver) {
                                    running = false;
                                    stopThreadsAndDispose();
                                    int score = boardModel.getScore();
                            showGameOverDialog(score);
                        }
                    });
                    if (now - lastMove >= ghostSleep) {
                        SwingUtilities.invokeAndWait(() -> {
                            boardModel.getGhostManager().moveGhostsActual();
                        });
                        lastMove = now;
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        ghostThread.start();

        powerPelletFlashThread = new Thread(() -> {
            try {
                while (running) {
                    powerPelletVisible = !powerPelletVisible;
                    table.repaint();
                    Thread.sleep(400);
                }
            } catch (InterruptedException ignored) {}
        });
        powerPelletFlashThread.setDaemon(true);
        powerPelletFlashThread.start();

        fruitLabel.setPreferredSize(new Dimension(32, 32));
        fruitLabel.setOpaque(false);
        fruitLabel.setBackground(new Color(0,0,0,0));

        KeyStroke quitKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(quitKeyStroke, "quitToMenu");
        getRootPane().getActionMap().put("quitToMenu", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                running = false;
                stopThreadsAndDispose();
                dispose();
                new MainMenuView();
            }
        });
    }

    private void loadPacmanImages() {
        String[] directions = {"up", "down", "left", "right"};
        for (String dir : directions) {
            ImageIcon[] frames = new ImageIcon[ANIMATION_FRAMES];
            for (int i = 0; i < ANIMATION_FRAMES; i++) {
                String path = String.format("assets/images/pacman-%s/%d.png", dir, i);
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    frames[i] = new ImageIcon(path);
                } else {
                    frames[i] = null;
                }
            }
            pacmanImages.put(dir, frames);
        }
    }

    private void loadGhostImages() {
        String[] types = {"blinky", "pinky", "inky", "clyde"};
        for (String type : types) {
            String path = String.format("assets/images/ghosts/%s.png", type);
            java.io.File file = new java.io.File(path);
            if (file.exists()) {
                ghostImages.put(type, new ImageIcon[] { new ImageIcon(path) });
            } else {
                ghostImages.put(type, new ImageIcon[] { null });
            }
        }
    }

    private int[] getAheadOfPacman(Pacman pacman, int n) {
        int row = pacman.getRow();
        int col = pacman.getCol();
        int[] d = switch (pacman.getDirection()) {
            case UP -> new int[]{-1, 0};
            case DOWN -> new int[]{1, 0};
            case LEFT -> new int[]{0, -1};
            case RIGHT -> new int[]{0, 1};
        };
        int aheadRow = row + d[0] * n;
        int aheadCol = col + d[1] * n;
        aheadRow = Math.max(0, Math.min(boardModel.getRowCount() - 1, aheadRow));
        aheadCol = Math.max(0, Math.min(boardModel.getColumnCount() - 1, aheadCol));
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
    private void checkPacmanGhostCollision() {
        Pacman pacman = boardModel.getPacman();
        for (Ghost ghost : boardModel.getGhosts()) {
            if (ghost.getRow() == pacman.getRow() && ghost.getCol() == pacman.getCol()) {
                if (boardModel.isShieldActive()) {
                    continue;
                }
                running = false;
                showGameOverDialog(boardModel.getScore());
                break;
            }
        }
    }

    private void updateLivesDisplay(int lives) {
        livesPanel.removeAll();
        for (int i = 0; i < lives; i++) {
            JLabel iconLabel = new JLabel(lifeIcon);
            iconLabel.setOpaque(false);
            iconLabel.setBackground(new Color(0,0,0,0));
            livesPanel.add(iconLabel);
        }
        livesPanel.revalidate();
        livesPanel.repaint();
    }

    public void showHighScoresDialog() { showHighScoresDialogImpl(); }
    private void showHighScoresDialogImpl() {
        java.util.List<model.HighScoreManager.HighScoreEntry> scores = boardModel.getHighScores();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        int rank = 1;
        for (model.HighScoreManager.HighScoreEntry entry : scores) {
            listModel.addElement(rank + ". " + entry.name + " - " + entry.score);
            rank++;
        }
        JList<String> scoreList = new JList<>(listModel);
        scoreList.setFont(new Font("Arial", Font.PLAIN, 18));
        scoreList.setBackground(Color.BLACK);
        scoreList.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(scoreList);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        JOptionPane.showMessageDialog(this, scrollPane, "High Scores", JOptionPane.PLAIN_MESSAGE);
    }

    private void updateFruitDisplay() {
        int r = boardModel.getFruitRow();
        int c = boardModel.getFruitCol();
        if (r >= 0 && c >= 0) {
            BoardModel.CellContent fruit = boardModel.getCellContent(r, c);
            switch (fruit) {
                case CHERRY -> fruitLabel.setIcon(getScaledIcon(cherryIcon, 32, 32));
                case STRAWBERRY -> fruitLabel.setIcon(getScaledIcon(strawberryIcon, 32, 32));
                case APPLE -> fruitLabel.setIcon(getScaledIcon(appleIcon, 32, 32));
                default -> fruitLabel.setIcon(null);
            }
        } else {
            fruitLabel.setIcon(null);
        }
        updateCollectedFruits();
        levelLabel.setText("Level: " + level);
    }

    private void updateCollectedFruits() {
        collectedFruitsPanel.removeAll();
        java.util.List<model.BoardModel.CellContent> eaten = boardModel.getEatenFruitsThisLevel();
        java.util.Set<model.BoardModel.CellContent> shown = new java.util.HashSet<>();
        for (model.BoardModel.CellContent fruit : eaten) {
            if ((fruit == model.BoardModel.CellContent.CHERRY || fruit == model.BoardModel.CellContent.STRAWBERRY || fruit == model.BoardModel.CellContent.APPLE) && !shown.contains(fruit)) {
                ImageIcon icon = switch (fruit) {
                    case CHERRY -> cherryIcon;
                    case STRAWBERRY -> strawberryIcon;
                    case APPLE -> appleIcon;
                    default -> null;
                };
                if (icon != null) {
                    JLabel iconLabel = new JLabel(getScaledIcon(icon, 24, 24));
                    iconLabel.setPreferredSize(new Dimension(24, 24));
                    iconLabel.setOpaque(false);
                    iconLabel.setBackground(new Color(0,0,0,0));
                    collectedFruitsPanel.add(iconLabel);
                    shown.add(fruit);
                }
            }
        }
        collectedFruitsPanel.revalidate();
        collectedFruitsPanel.repaint();
    }

    private void checkAndAddCollectedFruit() {
        updateCollectedFruits();
    }

    private void stopThreadsAndDispose() {
        running = false;
        if (pacmanThread != null && pacmanThread.isAlive()) {
            try { pacmanThread.join(200); } catch (InterruptedException ignored) {}
        }
        if (ghostThread != null && ghostThread.isAlive()) {
            ghostThread.interrupt();
            try { ghostThread.join(200); } catch (InterruptedException ignored) {}
        }
        if (powerPelletFlashThread != null && powerPelletFlashThread.isAlive()) {
            powerPelletFlashThread.interrupt();
            try { powerPelletFlashThread.join(200); } catch (InterruptedException ignored) {}
        }
        if (chompClip != null && chompClip.isRunning()) {
            chompClip.stop();
        }
    }

    private int getTickDebug() {
        try {
            java.lang.reflect.Field f = boardModel.getClass().getDeclaredField("ticksSinceStart");
            f.setAccessible(true);
            return (int) f.get(boardModel);
        } catch (Exception e) {
            return -1;
        }
    }

    private ImageIcon getScaledIcon(ImageIcon icon, int width, int height) {
        if (icon == null) return null;
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private ImageIcon loadBigDotIcon() {
        java.net.URL url = getClass().getClassLoader().getResource("assets/other/big_dot.png");
        if (url != null) {
            return new ImageIcon(url);
        } else {
            return null;
        }
    }

    public void toggleSound() { toggleSoundImpl(); }
    private void toggleSoundImpl() {
        soundOn = !soundOn;
        soundButton.setIcon(soundOn ? soundOnIcon : soundOffIcon);
        if (chompClip != null) {
            if (soundOn) {
                chompClip.setFramePosition(0);
                chompClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                chompClip.stop();
            }
        }
    }

    private ImageIcon loadSoundIconWithFallback(java.io.File file, Color fallbackColor) {
        if (file.exists()) {
            ImageIcon icon = new ImageIcon(file.getPath());
            if (icon.getImage() != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                return getScaledIcon(icon, 20, 20);
            }
        }
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(fallbackColor);
        g2.fillRect(0, 0, 20, 20);
        g2.dispose();
        return new ImageIcon(img);
    }

    private void showWinDialog(int score) {
        JDialog dialog = new JDialog(this, "Victory!", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.BLACK);
        dialog.setSize(935, 340);
        dialog.setLocationRelativeTo(this);
        Font pacmanFont;
        try {
            pacmanFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets/pacman.ttf")).deriveFont(Font.BOLD, 32f);
        } catch (Exception e) {
            pacmanFont = new Font("Arial", Font.BOLD, 32);
        }
        JLabel titleLabel = new JLabel("You win! All pellets eaten.");
        titleLabel.setFont(pacmanFont);
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        Font buttonFont;
        try {
            buttonFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets/pacman.ttf")).deriveFont(Font.BOLD, 22f);
        } catch (Exception e) {
            buttonFont = new Font("Arial", Font.BOLD, 22);
        }
        Color neonBlue = new Color(0, 200, 255);
        Color neonBlueHover = new Color(0, 255, 255);
        Color buttonText = Color.WHITE;
        JButton nextLevelButton = createStyledButton("Next Level", buttonFont, neonBlue, neonBlueHover, buttonText, 180, 54);
        JButton menuButton = createStyledButton("Return to Menu", buttonFont, neonBlue, neonBlueHover, buttonText, 180, 54);
        JButton exitButton = createStyledButton("Exit", buttonFont, neonBlue, neonBlueHover, buttonText, 180, 54);
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(18, 10, 18, 10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(titleLabel, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 30, 10, 30);
        gbc.gridy = 1; gbc.gridx = 0;
        contentPanel.add(nextLevelButton, gbc);
        gbc.gridy = 2;
        contentPanel.add(menuButton, gbc);
        gbc.gridy = 1; gbc.gridx = 1;
        contentPanel.add(exitButton, gbc);
        JPanel winButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 80, 0));
        winButtonPanel.setOpaque(false);
        winButtonPanel.add(nextLevelButton);
        winButtonPanel.add(menuButton);
        winButtonPanel.add(exitButton);
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(40, 0, 0, 0);
        contentPanel.add(winButtonPanel, gbc);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.setSize(935, 340);
        if (boardModel.getHighScoreManager().isHighScore(score)) {
            int result = JOptionPane.showConfirmDialog(dialog, "New high score! Do you want to save it?", "High Score", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                String name = JOptionPane.showInputDialog(dialog, "Enter your name:", "High Score", JOptionPane.PLAIN_MESSAGE);
                if (name != null && !name.trim().isEmpty()) {
                    boardModel.getHighScoreManager().addHighScore(name.trim(), score);
                }
            }
        }
        nextLevelButton.addActionListener(e -> {
            dialog.dispose();
            new GameView(boardModel.getRowCount(), boardModel.getColumnCount(), level + 1);
            dispose();
        });
        menuButton.addActionListener(e -> {
            dialog.dispose();
            new MainMenuView();
            dispose();
        });
        exitButton.addActionListener(e -> {
            dialog.dispose();
            stopThreadsAndDispose();
            System.exit(0);
        });
        dialog.setVisible(true);
    }

    private void showGameOverDialog(int score) {
        JDialog dialog = new JDialog(this, "Game Over!", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.BLACK);
        dialog.setSize(935, 340);
        dialog.setLocationRelativeTo(this);
        Font pacmanFont;
        try {
            pacmanFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets/pacman.ttf")).deriveFont(Font.BOLD, 32f);
        } catch (Exception e) {
            pacmanFont = new Font("Arial", Font.BOLD, 32);
        }
        JLabel titleLabel = new JLabel("Game Over!");
        titleLabel.setFont(pacmanFont);
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel messageLabel = new JLabel("Pac-Man was caught by a ghost.");
        messageLabel.setFont(pacmanFont.deriveFont(Font.BOLD, 20f));
        messageLabel.setForeground(Color.YELLOW);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        Font buttonFont;
        try {
            buttonFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets/pacman.ttf")).deriveFont(Font.BOLD, 22f);
        } catch (Exception e) {
            buttonFont = new Font("Arial", Font.BOLD, 22);
        }
        Color neonBlue = new Color(0, 200, 255);
        Color neonBlueHover = new Color(0, 255, 255);
        Color buttonText = Color.WHITE;
        JButton replayButton = createStyledButton("Replay", buttonFont, neonBlue, neonBlueHover, buttonText, 180, 54);
        JButton menuButton = createStyledButton("Return to Menu", buttonFont, neonBlue, neonBlueHover, buttonText, 180, 54);
        JButton exitButton = createStyledButton("Exit", buttonFont, neonBlue, neonBlueHover, buttonText, 180, 54);
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(titleLabel, gbc);
        gbc.gridy = 1;
        contentPanel.add(messageLabel, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 80, 10, 10);
        gbc.gridy = 2; gbc.gridx = 0; gbc.anchor = GridBagConstraints.LINE_START;
        contentPanel.add(replayButton, gbc);
        gbc.gridy = 3; gbc.gridx = 0; gbc.anchor = GridBagConstraints.LINE_START;
        contentPanel.add(menuButton, gbc);
        gbc.gridy = 2; gbc.gridx = 1; gbc.anchor = GridBagConstraints.LINE_END; gbc.insets = new Insets(10, 10, 10, 80);
        contentPanel.add(exitButton, gbc);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 80, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(replayButton);
        buttonPanel.add(menuButton);
        buttonPanel.add(exitButton);
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(40, 0, 0, 0);
        contentPanel.add(buttonPanel, gbc);
        dialog.setSize(935, 340);
        if (boardModel.getHighScoreManager().isHighScore(score)) {
            int result = JOptionPane.showConfirmDialog(dialog, "New high score! Do you want to save it?", "High Score", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                String name = JOptionPane.showInputDialog(dialog, "Enter your name:", "High Score", JOptionPane.PLAIN_MESSAGE);
                if (name != null && !name.trim().isEmpty()) {
                    boardModel.getHighScoreManager().addHighScore(name.trim(), score);
                }
            }
        }
        replayButton.addActionListener(e -> {
            dialog.dispose();
            new GameView(boardModel.getRowCount(), boardModel.getColumnCount(), 1);
            dispose();
        });
        menuButton.addActionListener(e -> {
            dialog.dispose();
            new MainMenuView();
            dispose();
        });
        exitButton.addActionListener(e -> {
            dialog.dispose();
            stopThreadsAndDispose();
            System.exit(0);
        });
        dialog.setVisible(true);
    }

    private JButton createStyledButton(String text, Font font, Color bg, Color hover, Color fg, int width, int height) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(width, height));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setForeground(hover);
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setForeground(fg);
                }
            }
        });
        button.addChangeListener(e -> {
            if (!button.isEnabled()) {
                button.setForeground(Color.GRAY);
                button.setCursor(Cursor.getDefaultCursor());
            } else {
                button.setForeground(fg);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        return button;
    }

    public void addMenuListener(ActionListener l) {
        for (MenuElement me : getJMenuBar().getSubElements()) {
            if (me instanceof JMenu menu) {
                for (MenuElement item : menu.getSubElements()) {
                    if (item instanceof JMenuItem mi) {
                        mi.addActionListener(l);
                    }
                }
            }
        }
    }
    public void addSoundButtonListener(ActionListener l) {
    }
    public void addGlobalQuitListener(ActionListener l) {
        getRootPane().getActionMap().put("quitToMenu", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                l.actionPerformed(new java.awt.event.ActionEvent(this, 0, "quitToMenu"));
            }
        });
    }
    public void quitToMenu() {
        running = false;
        stopThreadsAndDispose();
        dispose();
        new MainMenuView();
    }
} 