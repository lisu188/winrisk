package com.winrisk.game.ai;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

@ArtificialIntelligence
public class RandomAI implements PlayerInterface {

    @Override
    public void attack(Game game) {
        Player player = game.getPlayer();
        FieldList borders = player.getBorders(game);
        if (borders.isEmpty()) {
            return;
        }
        Field attacker = borders.get(game.getRandom().nextInt(borders.size()));
        FieldList enemies = attacker.getEnemy();
        if (!enemies.isEmpty()) {
            Field target = enemies.get(game.getRandom().nextInt(enemies.size()));
            attacker.fight(target, game);
        }
    }

    @Override
    public void move(Game game) {
        Player player = game.getPlayer();
        FieldList fields = player.getFields(game);
        if (fields.size() < 2) {
            return;
        }
        Field source = fields.get(game.getRandom().nextInt(fields.size()));
        Field destination = fields.get(game.getRandom().nextInt(fields.size()));
        if (source != destination && source.getArmy() > 1) {
            game.maneuver(source, destination, game.getRandom().nextInt(source.getArmy()));
        }
    }

    @Override
    public void reinforce(Game game) {
        Player player = game.getPlayer();
        FieldList fields = player.getFields(game);
        while (player.getCurrentReinforcements() > 0) {
            Field target = fields.get(game.getRandom().nextInt(fields.size()));
            target.rein(1);
        }
    }
}
