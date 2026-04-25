package com.winrisk.game.ai;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

import java.util.Collections;

@ArtificialIntelligence
public class EasyAI implements PlayerInterface {
    @Override
    public void attack(Game game) {
        Player player = game.getPlayer();
        Field strong;
        FieldList borders = player.getBorders(game);
        FieldList enemies;
        borders.sort(game);
        int inc = 0;
        while (inc < borders.size()) {
            strong = borders.get(inc);
            enemies = strong.getEnemy();
            if (game.getParams().isSkynetMode()
                    && (enemies.getHuman().size() != 0)) {
                enemies = enemies.getHuman();
            }
            enemies.sort(game);
            Collections.reverse(enemies);
            for (Field weak : enemies) {
                if ((weak.getArmy() * 2) < strong.getArmy()) {
                    strong.fight(weak, game);
                    borders = player.getBorders(game);
                    borders.sort(game);
                    inc = -1;
                    break;
                }
            }
            inc++;
        }
    }

    @Override
    public void move(Game game) {
        Player player = game.getPlayer();
        FieldList fields = player.getFields(game);
        FieldList borders = player.getBorders(game);
        if (fields.size() < 2) {
            return;
        }
        Field source = fields.getStrong(game);
        Field destination = borders.isEmpty() ? fields.getWeak(game) : borders.getWeak(game);
        if (source != destination && source.getArmy() > 1) {
            game.maneuver(source, destination, source.getArmy() - 1);
        }
    }

    @Override
    public void reinforce(Game game) {
        Player player = game.getPlayer();
        while (player.getCurrentReinforcements() > 0) {
            FieldList fields = player.getFields(game);
            Field weakField = fields.getWeak(game);
            weakField.rein(1);
        }
    }
}
