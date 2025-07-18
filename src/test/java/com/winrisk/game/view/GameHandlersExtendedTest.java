package com.winrisk.game.view;

import com.winrisk.game.TestUtil;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

public class GameHandlersExtendedTest {
    @Test
    public void testMoreHandlers() throws Exception {
        Game game = TestUtil.createNewGame();
        Field a = game.getFields().get(0);
        Field b = game.getFields().get(1);
        // reinforce phase
        game.onFieldDrag(a, b);
        game.onShortClick(a);
        game.onLongClick(a);
        game.onFromField(a);
        game.next(); // attack
        game.onFieldDrag(a, b);
        game.next(); // move
        game.onFieldDrag(a, b);
        game.next(); // reinforce again
        game.onToField(a);
        game.onVoidClick(new PointF(0,0));
    }
}
