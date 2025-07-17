package com.winrisk.game.view;

import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

public class EditorTest {
    @Test
    public void testEditorMethods() {
        Map map = new Map();
        map.addField(new PointF(0,0));
        map.addContinent(0);
        Field field = map.getFields().get(0);
        map.getContinents().get(0).addField(field);

        Editor editor = new Editor(map);
        editor.onAction();
        editor.onVoidClick(new PointF(1,1));
        Field second = map.getFields().get(1);
        editor.onFieldDrag(field, second);
        editor.onFieldDrag(field, second);
        editor.onShortClick(field);
        editor.onToField(field);
        editor.onFromField(field);
        editor.onLongClick(second);
    }
}
