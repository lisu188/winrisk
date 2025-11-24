package com.winrisk.game.ai;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

import java.security.SecureRandom;
import java.util.Random;

@ArtificialIntelligence
public class RandomAI implements PlayerInterface {

    private final Random random = new SecureRandom();

    @Override
    public void attack(Game game) {
        Player player = game.getPlayer();
        FieldList borders = player.getBorders(game);
        if (borders.isEmpty()) {
            return;
        }
        Field attacker = borders.get(random.nextInt(borders.size()));
        FieldList enemies = attacker.getEnemy();
        if (!enemies.isEmpty()) {
            Field target = enemies.get(random.nextInt(enemies.size()));
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
        Field source = fields.get(random.nextInt(fields.size()));
        Field destination = fields.get(random.nextInt(fields.size()));
        if (source != destination && source.getArmy() > 1) {
            source.move(destination, random.nextInt(source.getArmy()));
        }
    }

    @Override
    public void reinforce(Game game) {
        Player player = game.getPlayer();
        FieldList fields = player.getFields(game);
        while (player.getCurrentReinforcements() > 0) {
            Field target = fields.get(random.nextInt(fields.size()));
            target.rein(1);
        }
    }
}
