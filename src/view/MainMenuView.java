package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainMenuView extends JFrame {
    public MainMenuView() {
        setTitle("Pacman - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 420);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Color.BLACK);

        Font pacmanFont;
        try {
            pacmanFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets/pacman.ttf")).deriveFont(Font.BOLD, 48f);
        } catch (Exception e) {
            pacmanFont = new Font("Arial", Font.BOLD, 48);
        }
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("PAC-MAN");
        titleLabel.setFont(pacmanFont);
        titleLabel.setForeground(Color.YELLOW);
      
        ImageIcon pacmanIcon = new ImageIcon("assets/images/pacman-right/1.png");
        if (pacmanIcon.getImage() != null) {
            Image img = pacmanIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(img));
            titlePanel.add(iconLabel);
        }
        titlePanel.add(titleLabel);

        Font buttonFont;
        try {
            buttonFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets/pacman.ttf")).deriveFont(Font.BOLD, 28f);
        } catch (Exception e) {
            buttonFont = new Font("Arial", Font.BOLD, 28);
        }

        Color neonBlue = new Color(0, 200, 255);
        Color neonBlueHover = new Color(0, 255, 255);
        Color buttonText = Color.WHITE;

        JButton newGameButton = createStyledButton("New Game", buttonFont, neonBlue, neonBlueHover, buttonText);
        JButton highScoresButton = createStyledButton("High Scores", buttonFont, neonBlue, neonBlueHover, buttonText);
        JButton exitButton = createStyledButton("Exit", buttonFont, neonBlue, neonBlueHover, buttonText);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 10, 20, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        add(titlePanel, gbc);
        gbc.insets = new Insets(10, 60, 10, 60);
        gbc.gridy = 1;
        add(newGameButton, gbc);
        gbc.gridy = 2;
        add(highScoresButton, gbc);
        gbc.gridy = 3;
        add(exitButton, gbc);

        newGameButton.addActionListener(e -> {
            GameSetupDialog dialog = new GameSetupDialog(this, (rows, cols) -> {
                new GameView(rows, cols);
                dispose();
            });
            dialog.setVisible(true);
        });
        highScoresButton.addActionListener(e -> showHighScoresDialog());
        exitButton.addActionListener(e -> System.exit(0));

        setVisible(true);
    }

    private JButton createStyledButton(String text, Font font, Color bg, Color hover, Color fg) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(260, 54));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
            }
        });
        return button;
    }

    private void showHighScoresDialog() {
        model.BoardModel tempModel = new model.BoardModel(20, 20);
        java.util.List<model.HighScoreManager.HighScoreEntry> scores = tempModel.getHighScores();
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
} 