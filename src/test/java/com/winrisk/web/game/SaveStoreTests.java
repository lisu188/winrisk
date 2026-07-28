package com.winrisk.web.game;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class SaveStoreTests {

    private File tempDir;
    private SaveStore store;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("winrisk-store-test").toFile();
        store = new SaveStore(new File(tempDir, "saves").getPath(),
                new File(tempDir, "maps").getPath());
    }

    @Test
    public void savesResolveInsideTheDirectory() throws Exception {
        store.ensureSavesDir();
        File file = store.saveFile("my-game_1");
        assertTrue(file.getCanonicalPath()
                .startsWith(new File(tempDir, "saves").getCanonicalPath()));
        Files.writeString(file.toPath(), "{}");
        assertEquals(java.util.List.of("my-game_1"), store.listSaves());
        assertEquals(file, store.existingSaveFile("my-game_1"));
    }

    @Test
    public void traversalAndBadNamesAreRejected() {
        for (String name : new String[]{"../evil", "a/b", "", null, "x".repeat(65), "a b"}) {
            try {
                store.saveFile(name);
                fail("expected rejection for " + name);
            } catch (IllegalArgumentException expected) {
            }
        }
        for (String name : new String[]{"../secret.map", "a/../../b.map"}) {
            try {
                store.mapFile(name);
                fail("expected rejection for " + name);
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void missingFilesAreReportedNotCreated() {
        try {
            store.existingSaveFile("nope");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            store.mapFile("nope.map");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        assertTrue(store.listSaves().isEmpty());
        assertTrue(store.listMapFiles().isEmpty());
    }
}
