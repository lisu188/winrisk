package com.winrisk.game.object;

import com.winrisk.game.TestUtil;
import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.util.PointF;
import com.winrisk.game.view.Game;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.cluster.PatchList;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class ObjectTests {
    @Test
    public void fieldOperationsMoveAndRein() {
        Player p = new Player(Color.RED, PlayerFactory.getHuman());
        Field from = new Field(new PointF(0,0));
        Field to = new Field(new PointF(1,1));
        from.setPlayer(p);
        to.setPlayer(p);
        from.addNext(to);
        from.setArmy(1);
        p.setRein(3);
        from.rein(3);
        assertEquals(4, from.getArmy());
        assertEquals(0, p.getCurrentReinforcements());
        assertTrue(from.move(to,2));
        assertEquals(2, from.getArmy());
        assertEquals(2, to.getArmy());
        assertTrue(from.move(to,5));
        assertEquals(1, from.getArmy());
        assertEquals(3, to.getArmy());
    }

    @Test
    public void playerBonuses() throws Exception {
        Game game = TestUtil.createNewGame();
        Player player = game.getPlayers().get(0);
        player.setRein(0);
        player.getCards()[0] = 3; // guarantee a bonus
        int bonus = player.getCardBonus();
        player.applyCardBonus();
        assertEquals(4, bonus);
        assertEquals(bonus, player.getCurrentReinforcements());
    }

    @Test
    public void playerExtraMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        Player p1 = game.getPlayers().get(0);
        assertFalse(p1.isDead(game));
        assertNotNull(p1.getBorders(game));
        PatchList patches = p1.getPatchList(game);
        assertTrue(patches.size() > 0);

        Player dead = new Player(Color.PINK, PlayerFactory.getHuman());
        p1.takeCards(dead, game); // should not throw
    }

    @Test
    public void playerFieldsCorrectPlayer() throws Exception {
        Game game = TestUtil.createNewGame();
        for (Player player : game.getPlayers()) {
            player.getFields(game).forEach(field -> assertEquals(player, field.getPlayer()));
        }
    }

    @Test
    public void playerFieldsSameResult() throws Exception {
        Game game = TestUtil.createNewGame();
        for (Player player : game.getPlayers()) {
            assertEquals(player.getFields(game), player.getFields(game));
        }
    }

    @Test
    public void advancedPlayerFunctions() throws Exception {
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

    @Test
    public void continentBasics() {
        Continent continent = new Continent(0, 2);
        Field a = new Field(new PointF(0,0));
        Field b = new Field(new PointF(1,1));
        Field c = new Field(new PointF(2,2));
        a.addNext(b);
        b.addNext(c);
        continent.addField(a);
        continent.addField(b);
        continent.addField(c);
        assertTrue(continent.getFields().contains(a));
        assertEquals(continent, a.getContinent());

        continent.incBonus();
        continent.decBonus();
        assertEquals(2, continent.getBonus());

        Continent other = new Continent(1,1);
        Field outside = new Field(new PointF(3,3));
        outside.addNext(b);
        other.addField(outside);
        assertEquals(1, continent.getBorderCount());

        Player p1 = new Player(Color.RED, PlayerFactory.getHuman());
        Player p2 = new Player(Color.BLUE, PlayerFactory.getRandomAI());
        a.setPlayer(p1);
        b.setPlayer(p1);
        c.setPlayer(p2);
        assertNull(continent.getPlayer());
        c.setPlayer(p1);
        assertEquals(p1, continent.getPlayer());
        c.setPlayer(p2);
        assertNull(continent.getPlayer());
        assertEquals(2f/3f, continent.getPlayerShare(p1), 0.001f);

        continent.removeField(a);
        assertFalse(continent.getFields().contains(a));
        assertNull(a.getContinent());
    }

    @Test(expected = RuntimeException.class)
    public void takeCardsFromLivingPlayerFails() throws Exception {
        Game game = TestUtil.createNewGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);
        p1.takeCards(p2, game);
    }
}
