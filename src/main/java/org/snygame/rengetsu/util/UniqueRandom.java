package org.snygame.rengetsu.util;

import java.util.*;
import java.util.stream.IntStream;

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
        if (bound <= 0) {
            throw new NoSuchElementException("Out of random numbers");
        }
        int i = rand.nextInt(bound--);
        int result = swapped.getOrDefault(i, i);
        swapped.put(i, swapped.getOrDefault(bound, bound));
        return result;
    }

    public static IntStream asStream(Random rand, int bound) {
        return new UniqueRandom(rand, bound).toStream();
    }

    public IntStream toStream() {
        return IntStream.generate(this::nextInt).limit(bound);
    }
}
