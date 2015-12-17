package com.winrisk.game.ai;

import com.winrisk.game.cluster.ContinentList;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.util.Arrays;
import com.winrisk.game.view.Game;

import java.util.Optional;
import java.util.stream.Collectors;

@ArtificialIntelligence
public class ContinentAI implements PlayerInterface {

    private Optional<Continent> goal = Optional.empty();

    private void createGoal(Game game) {
        Player player = game.getPlayer();
        if (!goal.isPresent() || player.equals(goal.get().getPlayer())) {
            goal = new ContinentList(game.getMap()
                    .getContinents()).stream().filter(continent -> !player.equals(continent.getPlayer())).max((con1, con2) -> Float.compare(con1.getPlayerShare(player), con2.getPlayerShare(player)));
        }
    }

    @Override
    public void attack(Game game) {
        createGoal(game);
        if (goal.isPresent()) {
            Player player = game.getPlayer();
            FieldList playerFields = player.getFields(game);
            FieldList goalFields = goal.get().getFields();
            FieldList fieldsToAttack = game.getFields().stream().filter(f -> !playerFields.equals(f.getPlayer())).sorted((f1, f2) -> Float.compare(getAverageProximity(game, f1, goalFields), getAverageProximity(game, f2, goalFields))).collect(Collectors.toCollection(FieldList::new));
            playerFields.forEach(playerField ->
                    fieldsToAttack.forEach(attackField ->
                            playerField.fight(attackField, game)
                    ));
        }
    }

    private float getAverageProximity(Game game, Field field, FieldList list) {
        Integer[] objects = list.stream().map(f -> game.getMap().proximity(field, f)).toArray(Integer[]::new);
        return Arrays.avg(objects);
    }

    @Override
    public void move(Game game) {
        createGoal(game);
    }

    @Override
    public void reinforce(Game game) {
        createGoal(game);
        Player player = game.getPlayer();
        if (goal.isPresent()) {
            FieldList fieldsToReinforce = goal.get().getFields();
            FieldList collect = fieldsToReinforce.stream().filter(field -> player.equals(field.getPlayer())).collect(Collectors.toCollection(FieldList::new));
            if (collect.isEmpty()) {
                player.getFields(game).stream().sorted((field1, field2) -> Float.compare(field1.getScale(game), field2.getScale(game))).forEach(field -> field.rein(1));
            } else {
                collect.forEach(field -> field.rein(1));
            }
        }
    }

}
