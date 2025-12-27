package model;

import java.io.*;
import java.util.*;

public class HighScoreManager implements Serializable {
    public static class HighScoreEntry implements Serializable {
        public final String name;
        public final int score;
        public HighScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
        @Override public String toString() { return name + " - " + score; }
    }

    private static final String HIGH_SCORE_FILE = "scores.ser";
    private static final int MAX_HIGH_SCORES = 10;
    private List<HighScoreEntry> highScores = new ArrayList<>();

    public HighScoreManager() { load(); }

    public void load() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(HIGH_SCORE_FILE))) {
            Object obj = in.readObject();
            if (obj instanceof List<?>) {
                List<?> list = (List<?>) obj;
                highScores = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof HighScoreEntry entry) highScores.add(entry);
                }
            }
        } catch (Exception e) {
            System.err.println("[HighScoreManager] Failed to load high scores: " + e);
            System.err.println("[HighScoreManager] Working directory: " + System.getProperty("user.dir"));
            highScores = new ArrayList<>();
        }
    }

    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(HIGH_SCORE_FILE))) {
            out.writeObject(highScores);
        } catch (Exception ignored) {}
    }

    public boolean isHighScore(int score) {
        load();
        if (highScores.size() < MAX_HIGH_SCORES) return true;
        return score > highScores.get(highScores.size() - 1).score;
    }

    public void addHighScore(String name, int score) {
        load();
        highScores.add(new HighScoreEntry(name, score));
        highScores.sort((a, b) -> b.score - a.score);
        if (highScores.size() > MAX_HIGH_SCORES) highScores = highScores.subList(0, MAX_HIGH_SCORES);
        save();
    }

    public List<HighScoreEntry> getHighScores() {
        load();
        return new ArrayList<>(highScores);
    }
} 