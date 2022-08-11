package org.snygame.rengetsu.dicerolls;

import org.snygame.rengetsu.Rengetsu;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Diceroll {
    private static final Pattern DICE_RE = Pattern.compile("^(?:(\\d+)?d)?(?:(\\d+)|\\[(.*)\\])(.*)$");
    private static final Pattern OPTION_RE = Pattern.compile("(\\d+)|([a-zA-Z]+)");

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
                    e.printStackTrace();
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
                        case "droplow":
                        case "dl":
                            diceroll.dropLowest = 1;
                            break;
                        case "drophigh":
                        case "dh":
                            diceroll.dropHighest = 1;
                            break;
                        case "dldh":
                        case "dhdl":
                            diceroll.dropLowest = 1;
                            diceroll.dropHighest = 1;
                            break;
                        case "unique":
                        case "u":
                            diceroll.unique = true;
                            break;
                        case "sorted":
                            diceroll.sorted = true;
                            break;
                        case "nosum":
                            diceroll.noSum = true;
                            break;
                        case "sumonly":
                            diceroll.sumOnly = true;
                            break;
                        case "rep":
                            break;
                        default:
                            diceroll.error = "Unknown option: %s".formatted(prevOp);
                            return diceroll;
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
            e.printStackTrace();
            diceroll.error = "Max integer value is %d".formatted(Integer.MAX_VALUE);
            return diceroll;
        }

        diceroll.validate();
        return diceroll;
    }

    private void validate() {
        if (faces.size() == 0) {
            error = "Die has no faces";
        } else if (diceCount == 0) {
            error = "No dice";
        } else if (repeat == 0) {
            error = "Cannot perform roll 0 times";
        } else if (diceCount < dropHighest + dropLowest) {
            error = "Cannot drop %d dice if there is only %d".formatted(dropLowest + dropHighest, diceCount);
        } else if (unique && diceCount > faces.size()) {
            error = "Cannot have %d unique rolls if each die has %d faces".formatted(diceCount, faces.size());
        } else if (noSum && sumOnly) {
            error = "Options nosum and sumonly are incompatible";
        } else if (sorted && sumOnly) {
            error = "Options sorted and sumonly are incompatible";
        } else if (repeat == 0) {
            error = "Cannot perform roll 0 times";
        }
        // TODO add value restrictions
    }

    public int getRepeat() {
        return error == null ? repeat : 1;
    }

    public String roll() {
        if (error != null) {
            return error;
        }

        List<Integer> rolls = new ArrayList<>(diceCount);
        Random rng = Rengetsu.RNG;

        switch (faces) {
            case Fixed f -> {
                if (unique) {
                    List<Integer> faces = IntStream.range(0, f.faces()).boxed().collect(Collectors.toList());
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(faces.remove(rng.nextInt(faces.size())));
                    }
                } else {
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(Rengetsu.RNG.nextInt(f.faces()) + 1);
                    }
                }
            }
            case Ranges r -> {
                if (unique) {
                    List<Integer> indices = IntStream.range(0, r.size()).boxed().collect(Collectors.toList());
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(r.get(indices.remove(rng.nextInt(indices.size()))));
                    }
                } else {
                    for (int i = 0; i < diceCount; i++) {
                        rolls.add(r.get(Rengetsu.RNG.nextInt(r.size())));
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + faces);
        }

        boolean drops = dropLowest + dropHighest > 0;

        if (sorted) {
            Collections.sort(rolls);

            if (drops) {
                return "`%s` %s".formatted(this,
                        String.join(", ", IntStream.range(0, diceCount)
                                .mapToObj(
                                        i -> (i >= dropLowest && i < diceCount - dropHighest ? "**%d**" : "~~**%d**~~").formatted(rolls.get(i))
                                )
                                .toList()))
                        + (noSum ? "." :
                        " Total: **%d**.".formatted(IntStream.range(dropLowest, diceCount - dropHighest).map(rolls::get).sum()));
            } else {
                return "`%s` %s".formatted(this,
                        String.join(", ", rolls.stream().map("**%d**"::formatted).toList()))
                        + (noSum ? "." : " Total: **%d**.".formatted(rolls.stream().mapToInt(Integer::intValue).sum()));
            }

        } else if (sumOnly) {
            Collections.sort(rolls);
            return "`%s` Total: **%d**.".formatted(this,
                    drops ? IntStream.range(dropLowest, diceCount - dropHighest).map(rolls::get).sum()
                    : rolls.stream().mapToInt(Integer::intValue).sum());
        }

        if (drops) {
            List<Integer> indices = IntStream.range(0, diceCount).boxed().sorted(Comparator.comparingInt(rolls::get)).collect(Collectors.toList());
            Set<Integer> valid = new HashSet<>(indices.subList(dropLowest, diceCount - dropHighest));

            return "`%s` %s".formatted(this,
                    String.join(", ", IntStream.range(0, diceCount)
                            .mapToObj(
                                    i -> (valid.contains(i) ? "**%d**" : "~~**%d**~~").formatted(rolls.get(i))
                            )
                            .toList()))
                    + (noSum ? "." :
                    " Total: **%d**.".formatted(valid.stream().mapToInt(rolls::get).sum()));
        }

        return "`%s` %s".formatted(this, String.join(", ", rolls.stream().map("**%d**"::formatted).toList()))
                + (noSum ? "." : " Total: **%d**.".formatted(rolls.stream().mapToInt(Integer::intValue).sum()));
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

    private interface Faces {
        int size();
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
    }
    private static class Ranges implements Faces {
        private final Map<Integer, Range> ranges;
        private final int[] scale;
        private final int size;

        private Ranges(List<Range> ranges) {
            this.ranges = new HashMap<>();
            scale = new int[ranges.size()];

            int size = 0;
            for (int i = 0; i < ranges.size(); i++) {
                this.ranges.put(size, ranges.get(i));
                scale[i] = size;
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

        private int length() {
            return max - min + 1;
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
