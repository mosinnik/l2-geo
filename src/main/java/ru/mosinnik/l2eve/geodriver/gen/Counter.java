package ru.mosinnik.l2eve.geodriver.gen;

public final class Counter implements Comparable<Counter> {
    private int value = 0;

    public Counter() {
    }

    public void increment() {
        this.value++;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int compareTo(Counter o) {
        return Integer.compare(value, o.value);
    }
}

