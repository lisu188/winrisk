package com.winrisk.game.ai;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class PlayerFactory {
    private static final Random RANDOM = new SecureRandom();

    private static final List<Class<? extends PlayerInterface>> AI;
    private static final Class<? extends PlayerInterface> HUMAN;

    static {
        List<Class<? extends PlayerInterface>> ALL = new ArrayList<Class<? extends PlayerInterface>>() {
            {
                add(PlayerAI.class);
                add(EasyAI.class);
                add(ContinentAI.class);
                add(BalancedAI.class);
                add(BorderGuardAI.class);
                add(RandomAI.class);
            }
        };
        AI = ALL.stream()
                .filter(clas -> clas
                        .isAnnotationPresent(ArtificialIntelligence.class))
                .collect(Collectors.toList());
        HUMAN = ALL
                .stream()
                .filter(clas -> !clas
                        .isAnnotationPresent(ArtificialIntelligence.class))
                .collect(Collectors.toList()).get(0);
    }

    private static <T> T buildClass(Class<? extends T> clas) {
        try {
            return clas.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PlayerInterface getHuman() {
        return buildClass(HUMAN);
    }

    public static PlayerInterface getRandomAI() {
        return buildClass(AI.get(RANDOM.nextInt(AI.size())));

    }

    public static PlayerInterface getDefaultAI(int index) {
        return buildClass(AI.get(Math.floorMod(index, AI.size())));
    }
}
