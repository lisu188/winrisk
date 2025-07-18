package com.winrisk.game.view;

import com.winrisk.game.map.Map;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.io.File;

public class EditorSaveTest {
    @Test
    public void testSave() throws Exception {
        Map map = new Map();
        map.addField(new PointF(0,0));
        Editor editor = new Editor(map);
        File tmp = File.createTempFile("map",".dat");
        editor.onSave(tmp.getAbsolutePath());
        tmp.delete();
    }
}
