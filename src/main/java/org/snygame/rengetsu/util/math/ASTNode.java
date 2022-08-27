package org.snygame.rengetsu.util.math;

import org.snygame.rengetsu.util.Diceroll;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ASTNode {
    private final ASTNode[] children;

    public ASTNode(ASTNode... children) {
        this.children = children;
    }

    protected abstract Type validateType();

    public Type getType() {
        return validateType();
    }

    public abstract <T> T accept(ASTVisitor<T> visitor);

    public Stream<ASTNode> children() {
        return Stream.of(children);
    }

    @Override
    public String toString() {
        return "%s(%s)[%s]".formatted(getClass().getSimpleName(), getType(), repr());
    }

    protected String repr() {
        return Stream.of(children).map(String::valueOf).collect(Collectors.joining(", "));
    }

    public static enum Type {
        INT, FLOAT, BOOL;
    }

    public static abstract class BoolOp extends ASTNode {
        final ASTNode lhs;
        final ASTNode rhs;
        private final String name;

        BoolOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        protected Type validateType() {
            if (lhs.getType() != Type.BOOL || rhs.getType() != Type.BOOL) {
                throw new IllegalArgumentException("%s operands must be of type BOOL".formatted(name));
            }

            return Type.BOOL;
        }
    }

    public static abstract class EqualityOp extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;
        private final String name;

        EqualityOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        protected Type validateType() {
            if (lhs.getType() != rhs.getType() && (lhs.getType() == Type.BOOL || rhs.getType() == Type.BOOL)) {
                throw new IllegalArgumentException("%s operands must be of the same type".formatted(name));
            }

            return Type.BOOL;
        }
    }

    public static abstract class ComparisonOp extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;
        private final String name;

        ComparisonOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        protected Type validateType() {
            if (lhs.getType() == Type.BOOL || rhs.getType() == Type.BOOL) {
                throw new IllegalArgumentException("%s operands must be numerical".formatted(name));
            }

            return Type.BOOL;
        }
    }

    public static abstract class ArithmeticOp extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;
        private final String name;

        ArithmeticOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        protected Type validateType() {
            if (lhs.getType() == Type.BOOL || rhs.getType() == Type.BOOL) {
                throw new IllegalArgumentException("%s operands must be numerical".formatted(name));
            }

            if (lhs.getType() == Type.FLOAT || rhs.getType() == Type.FLOAT) {
                return Type.FLOAT;
            }

            return Type.INT;
        }
    }

    public static abstract class Constant<T> extends ASTNode {
        public final T value;
        private final Type type;

        Constant(T value, Type type) {
            this.value = value;
            this.type = type;
        }

        @Override
        protected Type validateType() {
            return type;
        }

        @Override
        protected String repr() {
            return value.toString();
        }
    }

    public static class Ternary extends ASTNode {
        final ASTNode condition;
        final ASTNode trueResult;
        final ASTNode falseResult;

        Ternary(ASTNode condition, ASTNode trueResult, ASTNode falseResult) {
            super(condition, trueResult, falseResult);
            this.condition = condition;
            this.trueResult = trueResult;
            this.falseResult = falseResult;
        }

        @Override
        protected Type validateType() {
            if (condition.getType() != Type.BOOL) {
                throw new IllegalArgumentException("Condition must be of type BOOL");
            }

            if (trueResult.getType() == falseResult.getType()) {
                return trueResult.getType();
            }

            if (trueResult.getType() == Type.BOOL || falseResult.getType() == Type.BOOL) {
                throw new IllegalArgumentException("Ternary result must be of the same type");
            }

            return Type.FLOAT;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogicOr extends BoolOp {
        LogicOr(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "||");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogicAnd extends BoolOp {
        LogicAnd(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "&&");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Equals extends EqualityOp {
        Equals(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "==");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class NotEquals extends EqualityOp {
        NotEquals(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "!=");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LessThan extends ComparisonOp {
        LessThan(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "<");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GreaterThan extends ComparisonOp {
        GreaterThan(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, ">");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LessEquals extends ComparisonOp {
        LessEquals(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "<=");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GreaterEquals extends ComparisonOp {
        GreaterEquals(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, ">=");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Add extends ArithmeticOp {
        Add(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "+");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Sub extends ArithmeticOp {
        Sub(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "-");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Mul extends ArithmeticOp {
        Mul(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "*");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Div extends ArithmeticOp {
        Div(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "/");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class IntDiv extends ArithmeticOp {
        IntDiv(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "//");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Mod extends ArithmeticOp {
        Mod(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "%");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Plus extends ASTNode {
        private final ASTNode value;

        Plus(ASTNode value) {
            super(value);
            this.value = value;
        }

        @Override
        protected Type validateType() {
            if (value.getType() == Type.BOOL) {
                throw new IllegalArgumentException("+ operand must be numerical");
            }

            return value.getType();
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Minus extends ASTNode {
        private final ASTNode value;

        Minus(ASTNode value) {
            super(value);
            this.value = value;
        }

        @Override
        protected Type validateType() {
            if (value.getType() == Type.BOOL) {
                throw new IllegalArgumentException("- operand must be numerical");
            }

            return value.getType();
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogicNot extends ASTNode {
        private final ASTNode value;

        LogicNot(ASTNode value) {
            super(value);
            this.value = value;
        }

        @Override
        protected Type validateType() {
            if (value.getType() != Type.BOOL) {
                throw new IllegalArgumentException("! operand must be of type BOOL");
            }

            return Type.BOOL;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Power extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;

        Power(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        protected Type validateType() {
            if (lhs.getType() == Type.BOOL || rhs.getType() == Type.BOOL) {
                throw new IllegalArgumentException("^ operands must be numerical");
            }

            return Type.FLOAT;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Function extends ASTNode {
        final String name;
        private final ASTNode[] arguments;

        Function(String name, ASTNode... arguments) {
            super(arguments);
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        protected Type validateType() {
            switch (name) {
                case "sqrt":
                case "ln":
                case "sin":
                case "cos":
                case "tan":
                    if (arguments.length != 1) {
                        throw new IllegalArgumentException("%s only takes 1 argument".formatted(name));
                    }
                    if (arguments[0].getType() == Type.BOOL) {
                        throw new IllegalArgumentException("%s argument must be numerical".formatted(name));
                    }
                    return Type.FLOAT;
                case "floor":
                case "ceil":
                case "trunc":
                    if (arguments.length != 1) {
                        throw new IllegalArgumentException("%s only takes 1 argument".formatted(name));
                    }
                    if (arguments[0].getType() == Type.BOOL) {
                        throw new IllegalArgumentException("%s argument must be numerical".formatted(name));
                    }
                    return Type.INT;
                case "abs":
                    if (arguments.length != 1) {
                        throw new IllegalArgumentException("%s only takes 1 argument".formatted(name));
                    }
                    if (arguments[0].getType() == Type.BOOL) {
                        throw new IllegalArgumentException("%s argument must be numerical".formatted(name));
                    }
                    return arguments[0].getType();
                case "fact":
                    if (arguments.length != 1) {
                        throw new IllegalArgumentException("%s only takes 1 argument".formatted(name));
                    }
                    if (arguments[0].getType() != Type.INT) {
                        throw new IllegalArgumentException("%s argument must be of type INT".formatted(name));
                    }
                    return Type.INT;
                default:
                    throw new IllegalArgumentException("Unknown function: %s".formatted(name));

            }
        }

        @Override
        protected String repr() {
            return "\"%s\", %s".formatted(name, super.repr());
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Dice extends ASTNode {
        private static final Pattern DICE_RE = Pattern.compile("^(\\d+)d(\\d+)(dl\\d*)?(dh\\d*)?(u)?$");
        private static final int MAX_DICE = Short.MAX_VALUE;
        private final int count;
        private final int faces;
        private final int dropLowest;
        private final int dropHighest;
        private final boolean unique;

        Dice(String dice) {
            Matcher match = DICE_RE.matcher(dice);

            if (!match.matches()) {
                throw new IllegalStateException();
            }

            count = Integer.parseInt(match.group(1));
            faces = Integer.parseInt(match.group(2));
            dropLowest = match.group(3) == null ? 0 : Integer.parseInt(match.group(3).substring(2));
            dropHighest = match.group(4) == null ? 0 : Integer.parseInt(match.group(4).substring(2));
            unique = match.group(5) != null;

            if (count > MAX_DICE) {
                throw new IllegalArgumentException("Max dice count is %d".formatted(MAX_DICE));
            } else if (count < dropHighest + dropLowest) {
                throw new IllegalArgumentException("Cannot drop %d dice if there is only %d".formatted(dropLowest + dropHighest, count));
            } else if (unique && count > faces) {
                throw new IllegalArgumentException("Cannot have %d unique rolls if each die has %d faces".formatted(count, faces));
            }
        }

        public long longValue() {
            return (unique ? 1L << 63 : 0) | ((long) count << 48) | ((long) faces << 32) |
                    ((long) dropLowest << 16) | dropHighest;
        }

        @Override
        protected Type validateType() {
            return Type.INT;
        }

        @Override
        protected String repr() {
            return "%dd%ddl%ddh%d%s".formatted(count, faces, dropLowest, dropHighest, unique ? "u" : "");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class IntConst extends Constant<BigInteger> {
        IntConst(BigInteger value) {
            super(value, Type.INT);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FloatConst extends Constant<BigDecimal> {
        FloatConst(BigDecimal value) {
            super(value, Type.FLOAT);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BoolConst extends Constant<Boolean> {
        BoolConst(Boolean value) {
            super(value, Type.BOOL);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
