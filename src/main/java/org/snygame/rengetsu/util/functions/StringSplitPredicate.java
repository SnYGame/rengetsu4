package org.snygame.rengetsu.util.functions;

import java.util.function.Predicate;

public class StringSplitPredicate implements Predicate<String> {
    private final int threshold;
    private int length = 0;

    private StringSplitPredicate(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean test(String msg) {
        if (length + msg.length() > threshold) {
            length = msg.length();
            return true;
        }
        length += msg.length();
        return false;
    }
    public static Predicate<String> get(int threshold) {
        return new StringSplitPredicate(threshold);
    }
}
