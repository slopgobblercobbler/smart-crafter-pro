package com.smartcrafter.config;

import com.smartcrafter.engine.compressor.CompressionCategory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration options and persistence for SmartCrafter Pro.
 */
public class SmartCrafterConfig {
    private static final SmartCrafterConfig INSTANCE = new SmartCrafterConfig();
    private static final Path CONFIG_PATH = Paths.get("config", "smartcrafter.json");

    private int actionDelayTicks = 2;
    private boolean safeMode = true;
    private boolean soundFeedback = true;
    private boolean autoCloseScreenWhenDone = false;
    private CompressionCategory defaultCategory = CompressionCategory.ALL;

    public static SmartCrafterConfig getInstance() {
        return INSTANCE;
    }

    public int getActionDelayTicks() {
        return actionDelayTicks;
    }

    public void setActionDelayTicks(int ticks) {
        this.actionDelayTicks = Math.max(0, Math.min(20, ticks));
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public void setSafeMode(boolean safeMode) {
        this.safeMode = safeMode;
    }

    public boolean isSoundFeedback() {
        return soundFeedback;
    }

    public void setSoundFeedback(boolean soundFeedback) {
        this.soundFeedback = soundFeedback;
    }

    public boolean isAutoCloseScreenWhenDone() {
        return autoCloseScreenWhenDone;
    }

    public void setAutoCloseScreenWhenDone(boolean autoClose) {
        this.autoCloseScreenWhenDone = autoClose;
    }

    public CompressionCategory getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(CompressionCategory defaultCategory) {
        this.defaultCategory = defaultCategory != null ? defaultCategory : CompressionCategory.ALL;
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\"actionDelayTicks\":")) {
                    setActionDelayTicks(parseNumber(line));
                } else if (line.startsWith("\"safeMode\":")) {
                    setSafeMode(parseBoolean(line));
                } else if (line.startsWith("\"soundFeedback\":")) {
                    setSoundFeedback(parseBoolean(line));
                } else if (line.startsWith("\"autoCloseScreenWhenDone\":")) {
                    setAutoCloseScreenWhenDone(parseBoolean(line));
                } else if (line.startsWith("\"defaultCategory\":")) {
                    String cat = parseString(line);
                    try {
                        setDefaultCategory(CompressionCategory.valueOf(cat));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[SmartCrafter] Failed to load config: " + e.getMessage());
        }
    }

    public void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                writer.write("{\n");
                writer.write("  \"actionDelayTicks\": " + actionDelayTicks + ",\n");
                writer.write("  \"safeMode\": " + safeMode + ",\n");
                writer.write("  \"soundFeedback\": " + soundFeedback + ",\n");
                writer.write("  \"autoCloseScreenWhenDone\": " + autoCloseScreenWhenDone + ",\n");
                writer.write("  \"defaultCategory\": \"" + defaultCategory.name() + "\"\n");
                writer.write("}\n");
            }
        } catch (Exception e) {
            System.err.println("[SmartCrafter] Failed to save config: " + e.getMessage());
        }
    }

    private int parseNumber(String line) {
        String num = line.replaceAll("[^0-9]", "");
        return num.isEmpty() ? 2 : Integer.parseInt(num);
    }

    private boolean parseBoolean(String line) {
        return line.contains("true");
    }

    private String parseString(String line) {
        int firstQuote = line.indexOf('\"', line.indexOf(':'));
        if (firstQuote == -1) return "";
        int secondQuote = line.indexOf('\"', firstQuote + 1);
        if (secondQuote == -1) return "";
        return line.substring(firstQuote + 1, secondQuote);
    }
}
