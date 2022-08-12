package org.snygame.rengetsu.dicerolls;

import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.util.UniqueRandom;

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
    private static final int MAX_REPEAT = 0x40;

    private int diceCount;
    private Faces faces;

    private int dropLowest;
    private int dropHighest;

    private boolean unique;

    private boolean sumOnly;
    private boolean noSum;
    private boolean sorted;

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
            diceroll.error = "Max integer value is %d".formatted(Integer.MAX_VALUE);
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
        } else if (diceCount > MAX_DICE_DISPLAY && noSum) {
            error = "Cannot use nosum for more than %d dice".formatted(MAX_DICE_DISPLAY);
        } else if (diceCount > MAX_DICE_DISPLAY && sorted) {
            error = "Cannot use sorted for more than %d dice".formatted(MAX_DICE_DISPLAY);
        } else if (faces.trueSize() > MAX_FACES) {
            error = "Max faces on dice is %d".formatted(MAX_FACES);
        } else if (repeat > MAX_REPEAT) {
            error = "Max repeats is %d".formatted(MAX_REPEAT);
        }
    }

    public int getRepeat() {
        return error == null ? repeat : 1;
    }

    public String roll() {
        if (error != null) {
            return "**[Error]** %s.".formatted(error);
        }

        boolean sumOnly = this.sumOnly && diceCount != 1 || diceCount > 64;
        boolean noSum = this.noSum || diceCount == 1;

        List<Integer> rolls = new ArrayList<>(diceCount);
        Random rng = Rengetsu.RNG;

        switch (faces) {
            case Fixed f -> {
                if (unique) {
                    UniqueRandom uniqueRand = new UniqueRandom(rng, f.faces());
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(uniqueRand.nextInt() + 1);
                    }
                } else {
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(rng.nextInt(f.faces()) + 1);
                    }
                }
            }
            case Ranges r -> {
                if (unique) {
                    UniqueRandom uniqueRand = new UniqueRandom(rng, r.size());
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(r.get(uniqueRand.nextInt()));
                    }
                } else {
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(r.get(rng.nextInt(r.size())));
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + faces);
        }

        boolean drops = dropLowest + dropHighest > 0;

        if (sorted) {
            Collections.sort(rolls);

            if (drops) {
                return "`%s` Rolled %s".formatted(shortRepr(),
                        String.join(", ", IntStream.range(0, diceCount)
                                .mapToObj(
                                        i -> (i >= dropLowest && i < diceCount - dropHighest ? "**%d**" : "~~**%d**~~").formatted(rolls.get(i))
                                )
                                .toList()))
                        + (noSum ? "." :
                        " Total: **%d**.".formatted(IntStream.range(dropLowest, diceCount - dropHighest).mapToLong(rolls::get).sum()));
            } else {
                return "`%s` Rolled %s".formatted(shortRepr(),
                        String.join(", ", rolls.stream().map("**%d**"::formatted).toList()))
                        + (noSum ? "." : " Total: **%d**.".formatted(rolls.stream().mapToLong(Integer::longValue).sum()));
            }

        } else if (sumOnly) {
            if (drops) {
                Collections.sort(rolls);
            }

            return "`%s` Total: **%d**.".formatted(shortRepr(),
                    drops ? IntStream.range(dropLowest, diceCount - dropHighest).mapToLong(rolls::get).sum()
                    : rolls.stream().mapToLong(Integer::longValue).sum());
        }

        if (drops) {
            List<Integer> indices = IntStream.range(0, diceCount).boxed().sorted(Comparator.comparingInt(rolls::get)).collect(Collectors.toList());
            Set<Integer> valid = new HashSet<>(indices.subList(dropLowest, diceCount - dropHighest));

            return "`%s` Rolled %s".formatted(shortRepr(),
                    String.join(", ", IntStream.range(0, diceCount)
                            .mapToObj(
                                    i -> (valid.contains(i) ? "**%d**" : "~~**%d**~~").formatted(rolls.get(i))
                            )
                            .toList()))
                    + (noSum ? "." :
                    " Total: **%d**.".formatted(valid.stream().mapToLong(rolls::get).sum()));
        }

        return "`%s` Rolled %s".formatted(shortRepr(), String.join(", ", rolls.stream().map("**%d**"::formatted).toList()))
                + (noSum ? "." : " Total: **%d**.".formatted(rolls.stream().mapToLong(Integer::longValue).sum()));
    }

    @Override
    public String toString() {
        if (error != null) {
            return "Dice error: %s".formatted(error);
        }

        return "%dd%s%s%s%s%s%s%s%s".formatted(diceCount, faces,
                dropLowest == 0 ? "" : "dl%d".formatted(dropLowest),
                dropHighest == 0 ? "" : "dh%d".formatted(dropHighest),
                unique ? "u" : "",
                sumOnly ? " sumonly" : "",
                noSum ? " nosum" : "",
                sorted ? " sorted" : "",
                repeat == 1 ? "" : " rep %d".formatted(repeat));
    }

    public String shortRepr() {
        String repr = toString();
        return repr.length() > 50 ? repr.substring(0, 49) + "\u2026" : repr;
    }

    private interface Faces {
        int size();
        long trueSize();
    }

    private record Fixed(int faces) implements Faces {
        @Override
        public String toString() {
            return String.valueOf(faces);
        }

        @Override
        public int size() {
            return faces;
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

        @Override
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
