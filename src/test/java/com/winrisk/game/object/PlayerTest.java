package com.winrisk.game.object;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

public class PlayerTest {

    @Test
    public void testGetFieldsCorrectPlayer() throws Exception {
        Game game = TestUtil.createNewGame();
        for (Player player : game.getPlayers()) {
            player.getFields(game).forEach(field -> {
                assert (player.equals(field.getPlayer()));
            });
        }
    }


    @Test
    public void testGetFieldsSameResult() throws Exception {
        Game game = TestUtil.createNewGame();
        for (Player player : game.getPlayers()) {
            assert (player.getFields(game).equals(player.getFields(game)));
        }
    }
}