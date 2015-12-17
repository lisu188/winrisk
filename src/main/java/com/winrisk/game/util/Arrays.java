package com.winrisk.game.util;

public class Arrays {
    public static float avg(float[] tab) {
        int av = 0;
        for (float element : tab) {
            av += element;
        }
        return av / (float) tab.length;
    }

    public static float avg(Integer[] tab) {
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
}
