package com.winrisk.game.object;

import com.winrisk.game.TestUtil;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.view.Game;
import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class PlayerAdvancedTest {
    @Test
    public void testPlayerFunctions() throws Exception {
        Game game = TestUtil.createNewGame();
        Player p = game.getPlayers().get(0);
        p.applyContinentBonus(game.getMap().getContinents());
        p.applyTerritoryBonus(game.getFields(), game);
        FieldList borders = p.getBorders(game);
        assertNotNull(borders);
        assertTrue(p.getVis(game).size() >= p.getFields(game).size());

        Player copy = new Player(p.getColor(), PlayerFactory.getHuman());
        assertEquals(p, copy);
        Player other = new Player(Color.PINK, PlayerFactory.getHuman());
        assertNotEquals(p, other);
        other.obtainField(new Field(new PointF(5,5)));
    }
}
