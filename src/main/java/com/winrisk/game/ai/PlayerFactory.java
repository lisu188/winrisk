package com.winrisk.game.ai;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class PlayerFactory {
    private static final Random RANDOM = new SecureRandom();

    private static final List<Class<? extends PlayerInterface>> AI;
    private static final Class<? extends PlayerInterface> HUMAN;

    static {
        List<Class<? extends PlayerInterface>> all = List.of(
                PlayerAI.class,
                EasyAI.class,
                ContinentAI.class,
                BalancedAI.class,
                BorderGuardAI.class,
                RandomAI.class);
        AI = all.stream()
                .filter(clas -> clas
                        .isAnnotationPresent(ArtificialIntelligence.class))
                .collect(Collectors.toList());
        HUMAN = all
                .stream()
                .filter(clas -> !clas
                        .isAnnotationPresent(ArtificialIntelligence.class))
                .collect(Collectors.toList()).get(0);
    }

    private static <T> T buildClass(Class<? extends T> clas) {
        try {
            return clas.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate " + clas.getName(), e);
        }
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

    /**
     * Rebuilds a player interface from its concrete class name. Used when
     * restoring a saved game so each seat keeps its original controller.
     */
    public static PlayerInterface byClassName(String className) {
        if (className == null) {
            return getHuman();
        }
        try {
            Class<?> clas = Class.forName(className);
            if (!PlayerInterface.class.isAssignableFrom(clas)) {
                throw new IllegalArgumentException("Not a PlayerInterface: " + className);
            }
            return (PlayerInterface) buildClass(clas.asSubclass(PlayerInterface.class));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown player interface: " + className, e);
        }
    }
}
