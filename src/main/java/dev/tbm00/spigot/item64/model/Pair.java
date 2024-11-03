package dev.tbm00.spigot.item64.model;

public class Pair<K extends Comparable<K>, V> implements Comparable<Pair<K, V>> {
    private final K first;
    private final V second;

    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    @Override
    public int compareTo(Pair<K, V> other) {
        return this.first.compareTo(other.first);
    }
}
