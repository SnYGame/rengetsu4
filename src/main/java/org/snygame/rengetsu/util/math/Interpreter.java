package org.snygame.rengetsu.util.math;

import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.util.UniqueRandom;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Interpreter {
    static List<String> functions = List.of("sqrt", "ln", "sin", "cos", "tan", "floor", "ceil", "trunc", "abs", "fact");


    public static String interpret(List<Byte> bytecode) {
        List<Object> constants = new ArrayList<>();
        Stack<Object> stack = new Stack<>();

        List<String> diceResults = new ArrayList<>();

        int[] i = new int[1];
        while (i[0] < bytecode.size()) {
            switch (Bytecode.Opcode.values()[bytecode.get(i[0])]) {
                case LOAD -> {
                    short index = getShort(bytecode, i);
                    stack.push(constants.get(index));
                }
                case ADD -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(arithmetic(lhs, rhs, BigInteger::add, BigDecimal::add));
                }
                case SUB -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(arithmetic(lhs, rhs, BigInteger::subtract, BigDecimal::subtract));
                }
                case MUL -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(arithmetic(lhs, rhs, BigInteger::multiply, BigDecimal::multiply));
                }
                case DIV -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();

                    switch (lhs) {
                        case BigInteger ilhs -> {
                            switch(rhs) {
                                case BigInteger irhs -> {
                                    stack.push(new BigDecimal(ilhs).divide(new BigDecimal(irhs), 16, RoundingMode.HALF_EVEN).stripTrailingZeros());
                                }
                                case BigDecimal frhs -> {
                                    stack.push(new BigDecimal(ilhs).divide(frhs, 16, RoundingMode.HALF_EVEN).stripTrailingZeros());
                                }
                                default -> throw new IllegalStateException("Unexpected value: " + rhs);
                            }
                        }
                        case BigDecimal flhs -> {
                            switch(rhs) {
                                case BigInteger irhs -> {
                                    stack.push(flhs.divide(new BigDecimal(irhs), 160, RoundingMode.HALF_EVEN).stripTrailingZeros());
                                }
                                case BigDecimal frhs -> {
                                    stack.push(flhs.divide(frhs, 16, RoundingMode.HALF_EVEN).stripTrailingZeros());
                                }
                                default -> throw new IllegalStateException("Unexpected value: " + rhs);
                            }
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + lhs);
                    }
                }
                case IDIV -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(arithmetic(lhs, rhs, BigInteger::divide, BigDecimal::divideToIntegralValue));
                }
                case MOD -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(arithmetic(lhs, rhs, BigInteger::remainder, BigDecimal::remainder));
                }
                case POW -> {
                    Number rhs = (Number) stack.pop();
                    Number lhs = (Number) stack.pop();

                    try {
                        switch (lhs) {
                            case BigInteger ilhs -> {
                                if (rhs instanceof BigInteger) {
                                    stack.push(ilhs.pow(((BigInteger) rhs).intValueExact()));
                                } else {
                                    stack.push(BigDecimal.valueOf(Math.pow(lhs.doubleValue(), rhs.doubleValue())));
                                }
                            }
                            case BigDecimal flhs -> {
                                if (rhs instanceof BigInteger) {
                                    stack.push(flhs.pow(((BigInteger) rhs).intValueExact()));
                                } else {
                                    stack.push(BigDecimal.valueOf(Math.pow(lhs.doubleValue(), rhs.doubleValue())));
                                }
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + lhs);
                        }
                    } catch (ArithmeticException e) {
                        stack.push(BigDecimal.valueOf(Math.pow(lhs.doubleValue(), rhs.doubleValue())));
                    }
                }
                case NEG -> {
                    Object value = stack.pop();
                    switch (value) {
                        case Boolean bool -> stack.push(!bool);
                        case BigInteger bint -> stack.push(bint.negate());
                        case BigDecimal bdec -> stack.push(bdec.negate());
                        default -> throw new IllegalStateException("Unexpected value: " + value);
                    }
                }
                case EQ -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(compare(lhs, rhs) == 0);
                }
                case NE -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(compare(lhs, rhs) != 0);
                }
                case LT -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(compare(lhs, rhs) == -1);
                }
                case LE -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(compare(lhs, rhs) != 1);
                }
                case GT -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(compare(lhs, rhs) == 1);
                }
                case GE -> {
                    Object rhs = stack.pop();
                    Object lhs = stack.pop();
                    stack.push(compare(lhs, rhs) != -1);
                }
                case BRANCH -> {
                    boolean condition = (Boolean) stack.pop();
                    short skip = getShort(bytecode, i);
                    if (condition) {
                        i[0] += skip;
                        continue;
                    }
                }
                case JUMP -> {
                    short skip = getShort(bytecode, i);
                    i[0] += skip;
                    continue;
                }
                case CALL -> {
                    stack.push(functionCall(functions.get(bytecode.get(++i[0])), (Number) stack.pop()));
                }
                case ROLL -> {
                    short index = getShort(bytecode, i);
                    Dice dice = (Dice) constants.get(index);

                    if (dice.count > 16 || dice.count == 1) {
                        if (dice.dl + dice.dh > 0) {
                            int sum = dice.roll().skip(dice.dl).limit(dice.count - dice.dl - dice.dh).sum();
                            if (!diceResults.isEmpty()) {
                                diceResults.add(" |");
                                diceResults.add(" %s = %d".formatted(dice, sum));
                            } else {
                                diceResults.add("%s = %d".formatted(dice, sum));
                            }
                            stack.push(BigInteger.valueOf(sum));
                        } else {
                            int sum = dice.roll().sum();
                            if (!diceResults.isEmpty()) {
                                diceResults.add(" |");
                                diceResults.add(" %s = %d".formatted(dice, sum));
                            } else {
                                diceResults.add("%s = %d".formatted(dice, sum));
                            }
                            stack.push(BigInteger.valueOf(sum));
                        }
                    } else {
                        if (dice.dl + dice.dh > 0) {
                            int[] results = dice.roll().toArray();

                            Set<Integer> kept = IntStream.range(0, dice.count).boxed().sorted(Comparator.comparingInt(in -> results[in]))
                                    .skip(dice.dl).limit(dice.count - dice.dl - dice.dh).collect(Collectors.toSet());
                            int sum = IntStream.range(0, dice.count).filter(kept::contains).map(in -> results[in]).sum();

                            if (!diceResults.isEmpty()) {
                                diceResults.add(" |");
                                diceResults.add(" %s = %d".formatted(dice, sum));
                            } else {
                                diceResults.add("%s = %d".formatted(dice, sum));
                            }
                            diceResults.add(" (");
                            for (int in = 0; in < dice.count; in++) {
                                if (in > 0) {
                                    diceResults.add((kept.contains(in) ? ", %d" : ", ~~%d~~").formatted(results[in]));
                                } else {
                                    diceResults.add((kept.contains(in) ? "%d" : "~~%d~~").formatted(results[in]));
                                }
                            }
                            diceResults.add(")");
                            stack.push(BigInteger.valueOf(sum));
                        } else {
                            int[] results = dice.roll().toArray();
                            int sum = IntStream.of(results).sum();

                            if (!diceResults.isEmpty()) {
                                diceResults.add(" |");
                                diceResults.add(" %s = %d".formatted(dice, sum));
                            } else {
                                diceResults.add("%s = %d".formatted(dice, sum));
                            }
                            diceResults.add(" (");
                            for (int in = 0; in < dice.count; in++) {
                                if (in > 0) {
                                    diceResults.add(", %d".formatted(results[in]));
                                } else {
                                    diceResults.add("%d".formatted(results[in]));
                                }
                            }
                            diceResults.add(")");

                            stack.push(BigInteger.valueOf(sum));
                        }
                    }
                }
                case INT -> {
                    byte[] intBytes = new byte[bytecode.get(++i[0])];
                    for (int j = 0; j < intBytes.length; j++) {
                        intBytes[j] = bytecode.get(++i[0]);
                    }
                    constants.add(new BigInteger(intBytes));
                }
                case FLOAT -> {
                    byte[] intBytes = new byte[bytecode.get(++i[0])];
                    byte scale = bytecode.get(++i[0]);
                    for (int j = 0; j < intBytes.length; j++) {
                        intBytes[j] = bytecode.get(++i[0]);
                    }
                    constants.add(new BigDecimal(new BigInteger(intBytes), scale));
                }
                case BOOL -> {
                    constants.add(bytecode.get(++i[0]) != 0);
                }
                case DICE -> {
                    short countUnique = getShort(bytecode, i);
                    boolean unique = (countUnique & 0x8000) == 0x8000;
                    short count = (short) (countUnique & 0x7FFF);
                    short faces = getShort(bytecode, i);
                    short dl = getShort(bytecode, i);
                    short dh = getShort(bytecode, i);
                    constants.add(new Dice(count, faces, dl, dh, unique));
                }
            }
            i[0]++;
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Resulting stack has %d values".formatted(stack.size()));
        }
        if (diceResults.isEmpty()) {
            return "Result: **%s**".formatted(stack.pop());
        } else {
            return "Result: **%s** [%s]".formatted(stack.pop(), diceResults.stream().reduce("",
                    (a, b) -> a.length() > 175 ? (a.endsWith("\u2026") ? a : a + "\u2026") : a + b));
        }
    }

    private static short getShort(List<Byte> bytes, int[] i) {
        return (short) ((bytes.get(++i[0]) << 8) | bytes.get(++i[0]) & 0xFF);
    }

    private static Number arithmetic(Object lhs, Object rhs, BiFunction<BigInteger, BigInteger, BigInteger> intOp,
                              BiFunction<BigDecimal, BigDecimal, BigDecimal> floatOp) {
        switch (lhs) {
            case BigInteger ilhs -> {
                switch(rhs) {
                    case BigInteger irhs -> {
                        return intOp.apply(ilhs, irhs);
                    }
                    case BigDecimal frhs -> {
                        return floatOp.apply(new BigDecimal(ilhs), frhs);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + rhs);
                }
            }
            case BigDecimal flhs -> {
                switch(rhs) {
                    case BigInteger irhs -> {
                        return floatOp.apply(flhs, new BigDecimal(irhs));
                    }
                    case BigDecimal frhs -> {
                        return floatOp.apply(flhs, frhs);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + rhs);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + lhs);
        }
    }

    private static int compare(Object lhs, Object rhs) {
        switch (lhs) {
            case BigInteger ilhs -> {
                switch(rhs) {
                    case BigInteger irhs -> {
                        return ilhs.compareTo(irhs);
                    }
                    case BigDecimal frhs -> {
                        return new BigDecimal(ilhs).compareTo(frhs);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + rhs);
                }
            }
            case BigDecimal flhs -> {
                switch(rhs) {
                    case BigInteger irhs -> {
                        return flhs.compareTo(new BigDecimal(irhs));
                    }
                    case BigDecimal frhs -> {
                        return flhs.compareTo(frhs);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + rhs);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + lhs);
        }
    }

    private static Number functionCall(String name, Number argument) {
        switch (name) {
            case "sqrt" -> {
                switch (argument) {
                    case BigInteger bint -> {
                        return new BigDecimal(bint).sqrt(MathContext.DECIMAL64);
                    }
                    case BigDecimal bdec -> {
                        return bdec.sqrt(MathContext.DECIMAL64);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + argument);
                }
            }
            case "ln" -> {
                double value = argument.doubleValue();
                if (value < 0) {
                    throw new ArithmeticException("Attempted log of negative value");
                }
                return BigDecimal.valueOf(Math.log(value));
            }
            case "sin" -> {
                return BigDecimal.valueOf(Math.sin(argument.doubleValue()));
            }
            case "cos" -> {
                return BigDecimal.valueOf(Math.cos(argument.doubleValue()));
            }
            case "tan" -> {
                return BigDecimal.valueOf(Math.tan(argument.doubleValue()));
            }
            case "floor" -> {
                if (argument instanceof BigDecimal) {
                    return ((BigDecimal) argument).setScale(0, RoundingMode.FLOOR);
                }
                return argument;
            }
            case "ceil" -> {
                if (argument instanceof BigDecimal) {
                    return ((BigDecimal) argument).setScale(0, RoundingMode.CEILING);
                }
                return argument;
            }
            case "trunc" -> {
                if (argument instanceof BigDecimal) {
                    return ((BigDecimal) argument).setScale(0, RoundingMode.DOWN);
                }
                return argument;
            }
            case "abs" -> {
                switch (argument) {
                    case BigInteger bint -> {
                        return bint.abs();
                    }
                    case BigDecimal bdec -> {
                        return bdec.abs();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + argument);
                }
            }
            case "fact" -> {
                BigInteger bint = (BigInteger) argument;
                if (bint.signum() < 0 || bint.compareTo(BigInteger.valueOf(50   )) > 0) {
                    throw new IllegalArgumentException("Factorial argument must be between 0 and 50");
                }
                return IntStream.range(1, bint.intValueExact() + 1).mapToObj(BigInteger::valueOf)
                        .reduce(BigInteger.ONE, BigInteger::multiply);
            }
        }

        throw new IllegalStateException();
    }

    private record Dice(short count, short faces, short dl, short dh, boolean unique) {
        private IntStream roll() {
            if (unique) {
                UniqueRandom random = new UniqueRandom(Rengetsu.RNG, faces);
                return IntStream.range(0, count).map(i -> random.nextInt() + 1);
            } else {
                return IntStream.range(0, count).map(i -> Rengetsu.RNG.nextInt(faces) + 1);
            }
        }

        @Override
        public String toString() {
            return "%dd%d%s%s%s".formatted(count, faces,
                    dl == 0 ? "" : "dl%d".formatted(dl),
                    dh == 0 ? "" : "dh%d".formatted(dh),
                    unique ? "u" : "");
        }
    }
}