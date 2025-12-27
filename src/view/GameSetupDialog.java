package view;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public class GameSetupDialog extends JDialog {
    public GameSetupDialog(JFrame parent, BiConsumer<Integer, Integer> onBoardSizeSelected) {
        super(parent, "Select Board Size", true);
        setLayout(new GridBagLayout());
        setSize(300, 200);
        setLocationRelativeTo(parent);

        JLabel rowsLabel = new JLabel("Rows (10-100):");
        JLabel colsLabel = new JLabel("Columns (10-100):");
        JTextField rowsField = new JTextField("20", 5);
        JTextField colsField = new JTextField("20", 5);
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0; add(rowsLabel, gbc);
        gbc.gridx = 1; add(rowsField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(colsLabel, gbc);
        gbc.gridx = 1; add(colsField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; add(okButton, gbc);
        gbc.gridx = 1; add(cancelButton, gbc);

        okButton.addActionListener(e -> {
            try {
                int rows = Integer.parseInt(rowsField.getText().trim());
                int cols = Integer.parseInt(colsField.getText().trim());
                if (rows < 10 || rows > 100 || cols < 10 || cols > 100) {
                    throw new NumberFormatException();
                }
                dispose();
                onBoardSizeSelected.accept(rows, cols);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers between 10 and 100.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dispose());
    }
} 