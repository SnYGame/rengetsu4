package org.snygame.rengetsu.util.functions;

import reactor.util.function.Tuple2;

import java.util.function.Function;

public class MapFirstElse<T, R> implements Function<Tuple2<Long, T>, R> {
    private final Function<T, R> funcFirst;
    private final Function<T, R> funcElse;

    private MapFirstElse(Function<T, R> funcFirst, Function<T, R> funcElse) {
        this.funcFirst = funcFirst;
        this.funcElse = funcElse;
    }

    @Override
    public R apply(Tuple2<Long, T> tuple) {
        return tuple.getT1() == 0 ? funcFirst.apply(tuple.getT2()) : funcElse.apply(tuple.getT2());
    }

    public static <T, R> MapFirstElse<T, R> get(Function<T, R> funcFirst, Function<T, R> funcElse) {
        return new MapFirstElse<>(funcFirst, funcElse);
    }
}
