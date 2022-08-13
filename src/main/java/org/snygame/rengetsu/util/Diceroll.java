package org.snygame.rengetsu.util;

import org.snygame.rengetsu.Rengetsu;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Diceroll {
    private static final Pattern DICE_RE = Pattern.compile("^(?:(\\d+)?d)?(?:(\\d+)|\\[(.*)\\])(.*)$");
    private static final Pattern OPTION_RE = Pattern.compile("(\\d+)|([a-zA-Z]+)");

    private static final int MAX_DICE = 0x8000000;
    private static final int MAX_DICE_UNIQUE = 0x2000000;
    private static final int MAX_DICE_DISPLAY = 0x40;
    private static final int MAX_FACES = 0x8000000;
    private static final int MAX_REPEAT = 0x20;

    private int diceCount;
    private Faces faces;

    private int dropLowest;
    private int dropHighest;

    private boolean unique;

    private boolean sumOnly;
    private boolean noSum;
    private boolean sorted;

    private boolean hideDrop;

    private int repeat = 1;

    private String error;

    private Diceroll() {}

    public static Diceroll parse(String query) {
        Diceroll diceroll = new Diceroll();
        Matcher match = DICE_RE.matcher(query);

        if (!match.matches()) {
            diceroll.error = "Invalid query: %s".formatted(query);
            return diceroll;
        }

        try {
            diceroll.diceCount = match.group(1) == null ? 1 : Integer.parseInt(match.group(1));
            if (match.group(2) == null) {
                try {
                    List<Range> ranges = new ArrayList<>();
                    for (String range : match.group(3).split(",")) {
                        if (range.isBlank()) {
                            continue;
                        }

                        if (range.contains(":")) {
                            String[] values = range.split(":");

                            if (values.length != 2) {
                                diceroll.error = "Invalid die faces";
                                return diceroll;
                            }

                            ranges.add(new Range(Integer.parseInt(values[0].strip()), Integer.parseInt(values[1].strip())));
                        } else {
                            ranges.add(new Range(Integer.parseInt(range.strip())));
                        }
                    }

                    diceroll.faces = new Ranges(ranges);
                } catch (NumberFormatException e) {
                    diceroll.error = "Invalid die faces";
                    return diceroll;
                }
            } else {
                diceroll.faces = new Fixed(Integer.parseInt(match.group(2)));
            }

            Matcher options = OPTION_RE.matcher(match.group(4));

            String prevOp = null;
            while (options.find()) {
                if (options.group(2) != null) {
                    if ("rep".equals(prevOp)) {
                        diceroll.error = "Missing parameter after rep";
                        return diceroll;
                    }

                    prevOp = options.group(2);

                    switch (prevOp) {
                        case "droplow", "dl" -> diceroll.dropLowest = 1;
                        case "drophigh", "dh" -> diceroll.dropHighest = 1;
                        case "dldh", "dhdl" -> diceroll.dropLowest = diceroll.dropHighest = 1;
                        case "unique", "u" -> diceroll.unique = true;
                        case "sorted" -> diceroll.sorted = true;
                        case "nosum" -> diceroll.noSum = true;
                        case "sumonly" -> diceroll.sumOnly = true;
                        case "hidedrop" -> diceroll.hideDrop = true;
                        case "rep" -> {}
                        default -> {
                            diceroll.error = "Unknown option: %s".formatted(prevOp);
                            return diceroll;
                        }
                    }
                } else {
                    int i = Integer.parseInt(options.group(1));

                    if (prevOp == null) {
                        diceroll.error = "Parameter with no option: %d".formatted(i);
                        return diceroll;
                    }

                    switch (prevOp) {
                        case "droplow", "dl", "dhdl" -> diceroll.dropLowest = i;
                        case "drophigh", "dh", "dldh" -> diceroll.dropHighest = i;
                        case "rep" -> diceroll.repeat = i;
                        default -> {
                            diceroll.error = "Option %s takes no parameters".formatted(prevOp);
                            return diceroll;
                        }
                    }

                    prevOp = null;
                }
            }

            if ("rep".equals(prevOp)) {
                diceroll.error = "Missing parameter after rep";
                return diceroll;
            }
        } catch (NumberFormatException e) {
            diceroll.error = "Values must be between %d and %d".formatted(Integer.MIN_VALUE, Integer.MAX_VALUE);
            return diceroll;
        }

        diceroll.validate();
        return diceroll;
    }

    private void validate() {
        if (faces.trueSize() == 0) {
            error = "Die has no faces";
        } else if (diceCount == 0) {
            error = "No dice";
        } else if (repeat == 0) {
            error = "Cannot perform roll 0 times";
        } else if (diceCount < dropHighest + dropLowest) {
            error = "Cannot drop %d dice if there is only %d".formatted(dropLowest + dropHighest, diceCount);
        } else if (unique && diceCount > faces.trueSize()) {
            error = "Cannot have %d unique rolls if each die has %d faces".formatted(diceCount, faces.trueSize());
        } else if (noSum && sumOnly) {
            error = "Options nosum and sumonly are incompatible";
        } else if (sorted && sumOnly) {
            error = "Options sorted and sumonly are incompatible";
        } else if (repeat == 0) {
            error = "Cannot perform roll 0 times";
        } else if (diceCount > MAX_DICE) {
            error = "Max dice count is %d".formatted(MAX_DICE);
        } else if (diceCount > MAX_DICE_UNIQUE && unique) {
            error = "Cannot use unique for more than %d dice".formatted(MAX_DICE_UNIQUE);
        } else if (diceCount - (hideDrop ? dropLowest + dropHighest : 0) > MAX_DICE_DISPLAY && noSum) {
            error = "Cannot use nosum for more than %d displayed dice".formatted(MAX_DICE_DISPLAY);
        } else if (diceCount - (hideDrop ? dropLowest + dropHighest : 0) > MAX_DICE_DISPLAY && sorted) {
            error = "Cannot use sorted for more than %d displayed dice".formatted(MAX_DICE_DISPLAY);
        } else if (faces.trueSize() > MAX_FACES) {
            error = "Max faces on dice is %d".formatted(MAX_FACES);
        } else if (repeat > MAX_REPEAT) {
            error = "Max repeats is %d".formatted(MAX_REPEAT);
        }
    }

    public int getRepeat() {
        return error == null ? repeat : 1;
    }

    public Result roll() {
        if (error != null) {
            return new Result(error);
        }

        int displayedDiceCount = diceCount - (hideDrop ? dropLowest + dropHighest : 0);
        boolean sumOnly = this.sumOnly && displayedDiceCount != 1 || displayedDiceCount > MAX_DICE_DISPLAY;
        boolean noSum = this.noSum || displayedDiceCount == 1;

        int[] rolls = new int[diceCount];
        Random rng = Rengetsu.RNG;

        switch (faces) {
            case Fixed f -> {
                if (unique) {
                    UniqueRandom uniqueRand = new UniqueRandom(rng, f.faces());
                    for (int i = 0; i < diceCount; i++) {
                        rolls[i] = uniqueRand.nextInt() + 1;
                    }
                } else {
                    for (int i = 0; i < diceCount; i++) {
                        rolls[i] = rng.nextInt(f.faces()) + 1;
                    }
                }
            }
            case Ranges r -> {
                if (unique) {
                    UniqueRandom uniqueRand = new UniqueRandom(rng, r.size());
                    for (int i = 0; i < diceCount; i++) {
                        rolls[i] = r.get(uniqueRand.nextInt());
                    }
                } else {
                    for (int i = 0; i < diceCount; i++) {
                        rolls[i] = r.get(rng.nextInt(r.size()));
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + faces);
        }

        int drops = dropLowest + dropHighest;

        if (sorted) {
            Arrays.sort(rolls);

            if (drops > 0) {
                if (hideDrop) {
                    return new Result(Arrays.stream(rolls, dropLowest, diceCount - dropHighest).toArray(),
                            noSum ? null : Arrays.stream(rolls, dropLowest, diceCount - dropHighest)
                                    .mapToLong(i -> i).sum());
                }

                return new Result(rolls,
                        IntStream.range(dropLowest, diceCount - dropHighest).mapToLong(i -> 1L << i).sum(),
                        noSum ? null : Arrays.stream(rolls, dropLowest, diceCount - dropHighest)
                                .mapToLong(i -> i).sum());
            } else {
                return new Result(rolls, noSum ? null : IntStream.of(rolls).mapToLong(i -> i).sum());
            }

        } else if (sumOnly) {
            if (drops > 0) {
                Arrays.sort(rolls);
            }

            return new Result(null,
                    drops > 0 ? Arrays.stream(rolls, dropLowest, diceCount - dropHighest)
                            .mapToLong(i -> i).sum()
                            : IntStream.of(rolls).mapToLong(i -> i).sum());
        }

        if (drops > 0) {
            if (hideDrop) {
                Set<Integer> kept = IntStream.range(0, diceCount).boxed().sorted(Comparator.comparingInt(i -> rolls[i]))
                        .skip(dropLowest).limit(diceCount - drops).collect(Collectors.toSet());

                return new Result(IntStream.range(0, diceCount).filter(kept::contains).map(i -> rolls[i]).toArray(),
                        noSum ? null : IntStream.range(0, diceCount).filter(kept::contains).mapToLong(i -> rolls[i]).sum());
            }

            long dropped = IntStream.range(0, diceCount).boxed().sorted(Comparator.comparingInt(i -> rolls[i]))
                    .skip(dropLowest).limit(diceCount - drops).mapToLong(i -> 1L << i).sum();

            return new Result(rolls, dropped, noSum ? null : IntStream.range(0, diceCount).mapToLong(i -> (dropped >> i & 1) * rolls[i]).sum());
        }

        return new Result(rolls, noSum ? null : IntStream.of(rolls).mapToLong(i -> i).sum());
    }

    @Override
    public String toString() {
        if (error != null) {
            return "Dice error: %s".formatted(error);
        }

        return "%dd%s%s%s%s%s%s%s%s%s".formatted(diceCount, faces,
                dropLowest == 0 ? "" : "dl%d".formatted(dropLowest),
                dropHighest == 0 ? "" : "dh%d".formatted(dropHighest),
                unique ? "u" : "",
                sumOnly ? " sumonly" : "",
                noSum ? " nosum" : "",
                sorted ? " sorted" : "",
                hideDrop ? " hidedrop" : "",
                repeat == 1 ? "" : " rep %d".formatted(repeat));
    }

    public String shortRepr() {
        String repr = toString();
        return repr.length() > 50 ? repr.substring(0, 49) + "\u2026" : repr;
    }

    public static record Result(String error, int[] rolls, long dropped, Long sum) {
        private Result(String error) {
            this(error, null, 0, null);
        }

        private Result(int[] rolls, long dropped, Long sum) {
            this(null, rolls, dropped, sum);
        }

        private Result(int[] rolls, Long sum) {
            this(null, rolls, 0xFFFFFFFFFFFFFFFFL, sum);
        }

        public int count() {
            return rolls == null ? 0 : rolls.length;
        }

        @Override
        public String toString() {
            if (error != null) {
                return error;
            }

            if (rolls == null) {
                return "Total: **%d**".formatted(sum);
            } else {
                String start;
                if (dropped == 0xFFFFFFFFFFFFFFFFL) {
                    start = String.join(", ", IntStream.of(rolls).mapToObj("**%d**"::formatted).toList());
                } else {
                    start = String.join(", ", IntStream.range(0, count())
                            .mapToObj(i -> ((dropped >> i & 1) == 0 ? "~~%d~~" : "**%d**").formatted(rolls[i])).toList());
                }

                if (sum == null) {
                    return start;
                }

                return "%s Total: **%d**".formatted(start, sum);
            }
        }
    }

    private interface Faces {
        long trueSize();
    }

    private record Fixed(int faces) implements Faces {
        @Override
        public String toString() {
            return String.valueOf(faces);
        }

        @Override
        public long trueSize() {
            return faces;
        }
    }
    private static class Ranges implements Faces {
        private final Map<Integer, Range> ranges;
        private final int[] scale;
        private final long size;

        private Ranges(List<Range> ranges) {
            this.ranges = new HashMap<>();
            scale = new int[ranges.size()];

            long size = 0;
            for (int i = 0; i < ranges.size(); i++) {
                this.ranges.put((int) size, ranges.get(i));
                scale[i] = (int) size;
                size += ranges.get(i).length();
            }
            this.size = size;
        }
        @Override
        public String toString() {
            return "[%s]".formatted(String.join(", ", IntStream.of(scale).boxed().map(ranges::get).map(Object::toString).toList()));
        }

        public int size() {
            return (int) size;
        }

        @Override
        public long trueSize() {
            return size;
        }

        private int get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index %d out of bounds for length %d".formatted(index, size));
            }
            int i = Arrays.binarySearch(scale, index);
            int base = scale[i < 0 ? -2 - i : i];
            return ranges.get(base).get(index - base);
        }
    }

    private record Range (int min, int max) {
        private Range(int min, int max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }
        private Range(int flat) {
            this(flat, flat);
        }

        private long length() {
            return (long) max - min + 1;
        }

        private int get(int index) {
            return min + index;
        }

        @Override
        public String toString() {
            return min == max ? String.valueOf(min) : "%d:%d".formatted(min, max);
        }
    }
}
