package com.winrisk.game.mission;

import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

/**
 * A player-specific goal that can trigger an early win when completed.
 */
public class Mission {

    @FunctionalInterface
    public interface Condition {
        boolean achieved(Game game, Player player);
    }

    private final String description;
    private final Condition condition;
    private final Player eliminationTarget;
    private final MissionSpec spec;

    public Mission(String description, Condition condition) {
        this(description, condition, null, null);
    }

    public Mission(String description, Condition condition, Player eliminationTarget) {
        this(description, condition, eliminationTarget, null);
    }

    public Mission(String description, Condition condition, Player eliminationTarget, MissionSpec spec) {
        this.description = description;
        this.condition = condition;
        this.eliminationTarget = eliminationTarget;
        this.spec = spec;
    }

    /**
     * @return the serializable description of this mission, or {@code null}
     * for ad-hoc missions that were not built from a {@link MissionSpec}.
     */
    public MissionSpec getSpec() {
        return spec;
    }

    public boolean isCompleted(Game game, Player player) {
        return condition.achieved(game, player);
    }

    public String getDescription() {
        return description;
    }

    public Player getEliminationTarget() {
        return eliminationTarget;
    }

    public boolean targets(Player player) {
        return eliminationTarget != null && eliminationTarget.equals(player);
    }
}
