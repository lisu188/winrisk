package com.winrisk.game.data;

import java.io.Serializable;

public class RulesOptions implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean incrementalCardSetValues;
    private boolean expandedManeuver;
    private boolean attackCardReroll;
    private boolean commanderDie;

    public boolean isIncrementalCardSetValues() {
        return incrementalCardSetValues;
    }

    public void setIncrementalCardSetValues(boolean incrementalCardSetValues) {
        this.incrementalCardSetValues = incrementalCardSetValues;
    }

    public boolean isExpandedManeuver() {
        return expandedManeuver;
    }

    public void setExpandedManeuver(boolean expandedManeuver) {
        this.expandedManeuver = expandedManeuver;
    }

    public boolean isAttackCardReroll() {
        return attackCardReroll;
    }

    public void setAttackCardReroll(boolean attackCardReroll) {
        this.attackCardReroll = attackCardReroll;
    }

    public boolean isCommanderDie() {
        return commanderDie;
    }

    public void setCommanderDie(boolean commanderDie) {
        this.commanderDie = commanderDie;
    }
}
