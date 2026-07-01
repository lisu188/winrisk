package com.winrisk.gui;

import com.winrisk.game.ai.HumanChoiceProvider;
import com.winrisk.game.rules.RiskCard;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing-backed {@link HumanChoiceProvider} that prompts the user with modal
 * dialogs. Used only when a graphical UI is present.
 */
public class SwingHumanChoiceProvider implements HumanChoiceProvider {

    @Override
    public int chooseCount(String prompt, int min, int max, int suggested) {
        if (min >= max) {
            return min;
        }
        Integer[] options = new Integer[max - min + 1];
        for (int i = 0; i < options.length; i++) {
            options[i] = min + i;
        }
        Object result = JOptionPane.showInputDialog(null, prompt, "WinRisk",
                JOptionPane.QUESTION_MESSAGE, null, options, suggested);
        return result == null ? suggested : (Integer) result;
    }

    @Override
    public int chooseCardSet(List<List<RiskCard>> sets, boolean forced) {
        List<String> labels = new ArrayList<>();
        for (List<RiskCard> set : sets) {
            labels.add(describe(set));
        }
        if (!forced) {
            labels.add("Do not trade");
        }
        String[] options = labels.toArray(new String[0]);
        int selection = JOptionPane.showOptionDialog(null,
                forced ? "You must trade a set of cards." : "Trade a set of cards?",
                "WinRisk", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (selection < 0 || selection >= sets.size()) {
            return -1;
        }
        return selection;
    }

    private String describe(List<RiskCard> set) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < set.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(set.get(i));
        }
        return builder.toString();
    }
}
