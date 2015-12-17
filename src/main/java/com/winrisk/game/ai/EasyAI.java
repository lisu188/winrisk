package com.winrisk.game.ai;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.cluster.PatchList;
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
        int tmpR = player.getCurrentReinforcements();
        player.setRein(0);
        PatchList pl = player.getPatchList(game);
        int inc;
        for (FieldList aPl : pl) {
            inc = 0;
            if (aPl.size() == 1) {
                continue;
            }
            for (Field aPatch : aPl) {
                inc += aPatch.getArmy() - 1;
                aPatch.setArmy(1);
                aPatch.setMin();
            }
            player.rein(inc);

            while ((player.getCurrentReinforcements() > 0)
                    ) {
                aPl.getWeak(game).rein(1);
            }
        }
        player.setRein(tmpR);
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
