package com.winrisk.game.rules;

import com.winrisk.game.object.Field;

import java.util.Objects;

public class RiskCard {
    private final int fieldIndex;
    private final String territoryName;
    private final CardSymbol symbol;
    private final Field field;

    private RiskCard(int fieldIndex, String territoryName, CardSymbol symbol, Field field) {
        this.fieldIndex = fieldIndex;
        this.territoryName = territoryName;
        this.symbol = symbol;
        this.field = field;
    }

    public static RiskCard territory(Field field) {
        return new RiskCard(field.getFieldIndex(), field.getDisplayName(), field.getCardSymbol(), field);
    }

    public static RiskCard wild() {
        return new RiskCard(-1, "Wild", CardSymbol.WILD, null);
    }

    public boolean isWild() {
        return symbol == CardSymbol.WILD;
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public CardSymbol getSymbol() {
        return symbol;
    }

    public Field getField() {
        return field;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RiskCard)) {
            return false;
        }
        RiskCard other = (RiskCard) obj;
        return fieldIndex == other.fieldIndex
                && symbol == other.symbol
                && Objects.equals(territoryName, other.territoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldIndex, territoryName, symbol);
    }

    @Override
    public String toString() {
        return isWild() ? "Wild" : territoryName + " (" + symbol + ")";
    }
}
