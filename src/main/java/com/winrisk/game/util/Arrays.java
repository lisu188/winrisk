package com.winrisk.game.util;

public class Arrays {
    public static float avg(float[] tab) {
        validateAverageInput(tab);

        float av = 0f;
        for (float element : tab) {
            av += element;
        }
        return av / tab.length;
    }

    public static float avg(Integer[] tab) {
        validateAverageInput(tab);

        int av = 0;
        for (float element : tab) {
            av += element;
        }
        return av / (float) tab.length;
    }

    public static int indMax(float[] tab) {
        float max = tab[0];
        int ind = 0;
        for (int i = 0; i < tab.length; i++) {
            if (tab[i] > max) {
                ind = i;
                max = tab[i];
            }
        }
        return ind;
    }

    public static int indMin(float[] tab) {
        float min = tab[0];
        int ind = 0;
        for (int i = 0; i < tab.length; i++) {
            if (tab[i] < min) {
                ind = i;
                min = tab[i];
            }
        }
        return ind;
    }

    public static int max(int[] tab) {
        int max = tab[0];
        for (int element : tab) {
            if (element > max) {
                max = element;
            }
        }
        return max;
    }

    public static int min(int[] tab) {
        int min = tab[0];
        for (int element : tab) {
            if (element < min) {
                min = element;
            }
        }
        return min;
    }

    private static void validateAverageInput(Object[] tab) {
        if (tab == null || tab.length == 0) {
            throw new IllegalArgumentException("Input array must not be null or empty");
        }
    }

    private static void validateAverageInput(float[] tab) {
        if (tab == null || tab.length == 0) {
            throw new IllegalArgumentException("Input array must not be null or empty");
        }
    }
}
