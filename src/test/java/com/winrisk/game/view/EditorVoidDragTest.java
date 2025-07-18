package com.winrisk.game.view;

import com.winrisk.game.map.Map;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import static org.junit.Assert.*;

/** Ensure Editor.onVoidDrag simply returns true. */
public class EditorVoidDragTest {
    @Test
    public void testVoidDrag() {
        Map map = new Map();
        Editor editor = new Editor(map);
        assertTrue(editor.onVoidDrag(new PointF(0,0), new PointF(1,1)));
    }
}
