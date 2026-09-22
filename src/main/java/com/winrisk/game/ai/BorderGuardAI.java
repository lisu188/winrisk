package com.winrisk.game.ai;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

@ArtificialIntelligence
public class BorderGuardAI implements PlayerInterface {

    @Override
    public void attack(Game game) {
        Player player = game.getPlayer();
        for (Field border : player.getBorders(game)) {
            FieldList enemies = border.getEnemy();
            if (enemies.isEmpty()) {
                continue;
            }
            enemies.sort(game);
            Field enemy = enemies.get(0);
            if (border.getArmy() > (enemy.getArmy() * 2)) {
                border.fight(enemy, game);
            }
        }
    }

    @Override
    public void move(Game game) {
        Player player = game.getPlayer();
        FieldList borders = player.getBorders(game);
        FieldList fields = player.getFields(game);
        for (Field field : fields) {
            if (!borders.contains(field) && field.getArmy() > 1) {
                Field target = borders.isEmpty() ? fields.getWeak(game) : borders.getWeak(game);
                game.maneuver(field, target, field.getArmy() - 1);
            }
        }
    }

    @Override
    public void reinforce(Game game) {
        Player player = game.getPlayer();
        FieldList borders = player.getBorders(game);
        int reinforcements = player.getCurrentReinforcements();
        if (reinforcements > 0) {
            borders.getWeak(game).rein(reinforcements);
        }
    }
}
