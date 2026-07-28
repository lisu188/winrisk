package com.winrisk.web.game;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolves save-game and custom-map files inside their configured server-side
 * directories. Names are strictly validated and resolved paths are checked for
 * containment, so the API can never read or write outside those directories.
 */
@Component
public class SaveStore {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String SAVE_EXTENSION = ".save.json";

    private final File savesDir;
    private final File mapsDir;

    public SaveStore(@Value("${winrisk.saves-dir:saves}") String savesDir,
                     @Value("${winrisk.maps-dir:maps}") String mapsDir) {
        this.savesDir = new File(savesDir);
        this.mapsDir = new File(mapsDir);
    }

    public File saveFile(String name) {
        return contained(savesDir, requireValidName(name) + SAVE_EXTENSION);
    }

    public File existingSaveFile(String name) {
        File file = saveFile(name);
        if (!file.isFile()) {
            throw new IllegalArgumentException("No such save: " + name);
        }
        return file;
    }

    public List<String> listSaves() {
        File[] files = savesDir.listFiles(
                (dir, fileName) -> fileName.endsWith(SAVE_EXTENSION));
        List<String> names = new ArrayList<>();
        if (files != null) {
            Arrays.stream(files)
                    .map(file -> file.getName()
                            .substring(0, file.getName().length() - SAVE_EXTENSION.length()))
                    .sorted()
                    .forEach(names::add);
        }
        return names;
    }

    public File mapFile(String fileName) {
        if (fileName == null || !fileName.matches("[A-Za-z0-9_. -]{1,128}")
                || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid map file name");
        }
        File file = contained(mapsDir, fileName);
        if (!file.isFile()) {
            throw new IllegalArgumentException("No such map file: " + fileName);
        }
        return file;
    }

    public List<String> listMapFiles() {
        File[] files = mapsDir.listFiles(File::isFile);
        List<String> names = new ArrayList<>();
        if (files != null) {
            Arrays.stream(files).map(File::getName).sorted().forEach(names::add);
        }
        return names;
    }

    public void ensureSavesDir() {
        if (!savesDir.isDirectory() && !savesDir.mkdirs()) {
            throw new IllegalStateException("Cannot create saves directory: " + savesDir);
        }
    }

    private String requireValidName(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Save names must match [A-Za-z0-9_-]{1,64}");
        }
        return name;
    }

    private File contained(File dir, String child) {
        File file = new File(dir, child);
        try {
            if (!file.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
                throw new IllegalArgumentException("Path escapes storage directory");
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Unresolvable path", e);
        }
        return file;
    }
}
