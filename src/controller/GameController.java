package controller;

import model.BoardModel;
import java.awt.event.*;
import model.Pacman;
import view.GameView;

public class GameController implements KeyListener, ActionListener {
    private final GameView view;
    private final BoardModel model;

    public GameController(GameView view, BoardModel model) {
        this.view = view;
        this.model = model;
        view.addKeyListener(this);
        view.addMenuListener(this);
        view.addSoundButtonListener(this);
        view.addGlobalQuitListener(this);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> model.getPacman().setDesiredDirection(Pacman.Direction.UP);
            case KeyEvent.VK_DOWN -> model.getPacman().setDesiredDirection(Pacman.Direction.DOWN);
            case KeyEvent.VK_LEFT -> model.getPacman().setDesiredDirection(Pacman.Direction.LEFT);
            case KeyEvent.VK_RIGHT -> model.getPacman().setDesiredDirection(Pacman.Direction.RIGHT);
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ("High Scores".equals(cmd)) {
            view.showHighScoresDialog();
        } else if ("SoundToggle".equals(cmd)) {
            view.toggleSound();
        } else if ("quitToMenu".equals(cmd)) {
            view.quitToMenu();
        }
    }
} 