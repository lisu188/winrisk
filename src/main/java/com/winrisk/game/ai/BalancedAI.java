package com.winrisk.game.ai;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

@ArtificialIntelligence
public class BalancedAI implements PlayerInterface {

    @Override
    public void attack(Game game) {
        Player player = game.getPlayer();
        FieldList borders = player.getBorders(game);
        borders.sort(game);
        for (Field strong : borders) {
            FieldList enemies = strong.getEnemy();
            if (enemies.isEmpty()) {
                continue;
            }
            enemies.sort(game);
            Field target = enemies.get(0);
            if (strong.getArmy() > target.getArmy() + 1) {
                strong.fight(target, game);
            }
        }
    }

    @Override
    public void move(Game game) {
        Player player = game.getPlayer();
        FieldList borders = player.getBorders(game);
        FieldList fields = player.getFields(game);
        Field destination = borders.isEmpty() ? fields.getWeak(game) : borders.getWeak(game);
        for (Field field : fields) {
            if (!borders.contains(field) && field.getArmy() > 1) {
                field.move(destination, field.getArmy() - 1);
            }
        }
    }

    @Override
    public void reinforce(Game game) {
        Player player = game.getPlayer();
        FieldList borders = player.getBorders(game);
        FieldList targets = borders.isEmpty() ? player.getFields(game) : borders;
        int reinforcements = player.getCurrentReinforcements();
        if (reinforcements > 0) {
            targets.getWeak(game).rein(reinforcements);
        }
    }
}
