package com.winrisk.game.view;

import com.winrisk.game.map.Map;
import com.winrisk.game.util.PointF;
import com.winrisk.game.data.MotionEvent;
import org.junit.Test;

public class EditorEventTest {
    @Test
    public void testOnEvent() {
        Map map = new Map();
        map.addField(new PointF(0,0));
        Editor editor = new Editor(map);
        MotionEvent evDown = new MotionEvent(MotionEvent.ACTION_DOWN,0,0,0,0);
        MotionEvent evUp = new MotionEvent(MotionEvent.ACTION_UP,0,0,0,0);
        editor.onEvent(evDown);
        editor.onEvent(evUp);
    }
}
