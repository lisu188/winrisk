package com.winrisk.game.view;

import com.winrisk.game.TestUtil;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

public class GameHandlersTest {
    @Test
    public void testHandlers() throws Exception {
        Game game = TestUtil.createNewGame();
        Field a = game.getFields().get(0);
        Field b = game.getFields().get(1);
        game.onFieldDrag(a, b);
        game.onFromField(a);
        game.onLongClick(a);
        game.onShortClick(a);
        game.onVoidClick(new PointF(0,0));
        game.onVoidDrag(new PointF(0,0), new PointF(1,1));
        game.onAction();
    }
}
