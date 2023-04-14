package org.snygame.rengetsu.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Random;

public class UniqueRandom implements Iterable<Integer>, PrimitiveIterator.OfInt {
    private final Random rand;
    private int bound;

    private final HashMap<Integer, Integer> swapped = new HashMap<>();

    public UniqueRandom(Random rand, int bound) {
        this.rand = rand;
        this.bound = bound;
    }

    @Override
    public Iterator<Integer> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public int nextInt() {
        int i = rand.nextInt(bound--);
        int result = swapped.getOrDefault(i, i);
        swapped.put(i, swapped.getOrDefault(bound, bound));
        return result;
    }
}
