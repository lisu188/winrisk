package com.winrisk.game.rules;

import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

import java.util.Arrays;
import java.util.Random;

public class CombatResolver {
    private final Random random;

    public CombatResolver(Random random) {
        this.random = random;
    }

    public CombatResult attack(Game game, Field source, Field target) {
        CombatResult result = resolveOneBattle(game, source, target);
        if (!result.isLegal()) {
            return result;
        }
        while (game.getParams().isAttackWithAll()
                && source.getPlayer() == result.getAttacker()
                && target.getPlayer() != result.getAttacker()
                && source.getArmy() > 1
                && target.getArmy() > 0) {
            result = resolveOneBattle(game, source, target);
            if (!result.isLegal() || result.isCaptured()) {
                break;
            }
        }
        return result;
    }

    private CombatResult resolveOneBattle(Game game, Field source, Field target) {
        if (!isLegalAttack(source, target)) {
            return CombatResult.illegal();
        }
        Player attacker = source.getPlayer();
        Player defender = target.getPlayer();
        int maxAttackDice = Math.min(3, source.getArmy() - 1);
        int maxDefenseDice = Math.min(2, target.getArmy());
        int attackDiceCount = clamp(
                attacker.getPlayerInterface().chooseAttackDice(game, source, target, maxAttackDice),
                1,
                maxAttackDice);
        Player defenseController = game.getDefenseController(defender);
        int defenseDiceCount = clamp(
                defenseController.getPlayerInterface().chooseDefenseDice(game, source, target, maxDefenseDice),
                1,
                maxDefenseDice);

        int[] attackDice = roll(attackDiceCount);
        int[] defenseDice = roll(defenseDiceCount);
        applyAttackCardReroll(game, attacker, source, target, attackDice);
        applyCommanderDie(game, attackDice);
        Arrays.sort(attackDice);
        Arrays.sort(defenseDice);

        int pairs = Math.min(attackDice.length, defenseDice.length);
        int attackerLosses = 0;
        int defenderLosses = 0;
        for (int i = 0; i < pairs; i++) {
            int attack = attackDice[attackDice.length - 1 - i];
            int defend = defenseDice[defenseDice.length - 1 - i];
            if (attack > defend) {
                defenderLosses++;
            } else {
                attackerLosses++;
            }
        }
        source.setArmy(source.getArmy() - attackerLosses);
        target.setArmy(target.getArmy() - defenderLosses);

        boolean captured = target.getArmy() <= 0;
        if (captured) {
            capture(game, source, target, attacker, defender, attackDiceCount, attackerLosses);
        }

        return new CombatResult(true, captured, attacker, defender,
                attackDice, defenseDice, attackerLosses, defenderLosses);
    }

    private void capture(Game game,
                         Field source,
                         Field target,
                         Player attacker,
                         Player defender,
                         int attackDiceCount,
                         int attackerLosses) {
        int minimumOccupation = Math.max(1, attackDiceCount - attackerLosses);
        int maximumOccupation = source.getArmy() - 1;
        int occupation = clamp(
                attacker.getPlayerInterface().chooseOccupationTroops(
                        game, source, target, minimumOccupation, maximumOccupation),
                minimumOccupation,
                maximumOccupation);

        target.setPlayer(attacker);
        target.setArmy(0);
        source.move(target, occupation);
        attacker.setConqueredTerritoryThisTurn(true);

        if (defender != null && !defender.isNeutral() && defender.isDead(game)) {
            attacker.takeCards(defender, game);
        }
    }

    private boolean isLegalAttack(Field source, Field target) {
        return source != null
                && target != null
                && source.getPlayer() != null
                && target.getPlayer() != null
                && source.getPlayer() != target.getPlayer()
                && source.getNext().contains(target)
                && source.getArmy() >= 2
                && target.getArmy() >= 1;
    }

    private int[] roll(int dice) {
        int[] results = new int[dice];
        for (int i = 0; i < dice; i++) {
            results[i] = random.nextInt(6) + 1;
        }
        return results;
    }

    private void applyAttackCardReroll(Game game,
                                       Player attacker,
                                       Field source,
                                       Field target,
                                       int[] attackDice) {
        if (!game.getParams().getRulesOptions().isAttackCardReroll()) {
            return;
        }
        boolean hasMatchingCard = attacker.getRiskCards().stream()
                .filter(card -> !card.isWild())
                .anyMatch(card -> card.getField() == source || card.getField() == target);
        if (!hasMatchingCard || attackDice.length == 0) {
            return;
        }
        int lowest = lowestDieIndex(attackDice);
        int reroll = random.nextInt(6) + 1;
        if (reroll > attackDice[lowest]) {
            attackDice[lowest] = reroll;
        }
    }

    private void applyCommanderDie(Game game, int[] attackDice) {
        if (!game.getParams().getRulesOptions().isCommanderDie()
                || game.isCommanderDieUsed()
                || attackDice.length == 0) {
            return;
        }
        attackDice[lowestDieIndex(attackDice)] = 6;
        game.setCommanderDieUsed(true);
    }

    private int lowestDieIndex(int[] dice) {
        int lowest = 0;
        for (int i = 1; i < dice.length; i++) {
            if (dice[i] < dice[lowest]) {
                lowest = i;
            }
        }
        return lowest;
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
