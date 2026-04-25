package com.winrisk.game.rules;

import com.winrisk.game.object.Field;

public class ManeuverChoice {
    private final Field source;
    private final Field destination;
    private final int troops;

    public ManeuverChoice(Field source, Field destination, int troops) {
        this.source = source;
        this.destination = destination;
        this.troops = troops;
    }

    public Field getSource() {
        return source;
    }

    public Field getDestination() {
        return destination;
    }

    public int getTroops() {
        return troops;
    }
}
