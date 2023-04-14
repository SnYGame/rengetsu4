package org.snygame.rengetsu.util.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ASTNode {
    private final ASTNode[] children;

    public ASTNode(ASTNode... children) {
        this.children = children;
    }

    public abstract <T> T accept(ASTVisitor<T> visitor);

    public Stream<ASTNode> children() {
        return Stream.of(children);
    }

    @Override
    public String toString() {
        return "%s[%s]".formatted(getClass().getSimpleName(), repr());
    }

    protected String repr() {
        return Stream.of(children).map(String::valueOf).collect(Collectors.joining(", "));
    }

    public static abstract class BoolOp extends ASTNode {
        final ASTNode lhs;
        final ASTNode rhs;
        final String name;

        BoolOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }
    }

    public static abstract class EqualityOp extends ASTNode {
        final ASTNode lhs;
        final ASTNode rhs;
        final String name;

        EqualityOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }
    }

    public static abstract class ComparisonOp extends ASTNode {
        final ASTNode lhs;
        final ASTNode rhs;
        final String name;

        ComparisonOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }
    }

    public static abstract class ArithmeticOp extends ASTNode {
        final ASTNode lhs;
        final ASTNode rhs;
        final String name;

        ArithmeticOp(ASTNode lhs, ASTNode rhs, String name) {
            super(lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }
    }

    public static abstract class Constant<T> extends ASTNode {
        public final T value;

        Constant(T value) {
            this.value = value;
        }

        @Override
        protected String repr() {
            return value.toString();
        }
    }

    public static class Assignment extends ASTNode {
        final String name;
        final ASTNode src;

        Assignment(String name, ASTNode src) {
            this.name = name;
            this.src = src;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
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
        final ASTNode value;

        Plus(ASTNode value) {
            super(value);
            this.value = value;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Minus extends ASTNode {
        final ASTNode value;

        Minus(ASTNode value) {
            super(value);
            this.value = value;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogicNot extends ASTNode {
        final ASTNode value;

        LogicNot(ASTNode value) {
            super(value);
            this.value = value;
        }
        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Power extends ArithmeticOp {
        Power(ASTNode lhs, ASTNode rhs) {
            super(lhs, rhs, "^");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Function extends ASTNode {
        final String name;
        final ASTNode[] arguments;

        Function(String name, ASTNode... arguments) {
            super(arguments);
            this.name = name;
            this.arguments = arguments;
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
        private static final Pattern DICE_RE = Pattern.compile("^(\\d+)d(\\d+)(dl\\d*)?(dh\\d*)?(dl\\d*)?(u)?$");
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

            String dlGroup = match.group(3) == null ? match.group(5) : match.group(3);
            String dhGroup = match.group(4);

            dropLowest = dlGroup == null ? 0 : (dlGroup.length() == 2 ? 1 : Integer.parseInt(dlGroup.substring(2)));
            dropHighest = dhGroup == null ? 0 : (dhGroup.length() == 2 ? 1 : Integer.parseInt(dhGroup.substring(2)));
            unique = match.group(6) != null;

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
            super(value);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FloatConst extends Constant<BigDecimal> {
        FloatConst(BigDecimal value) {
            super(value);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BoolConst extends Constant<Boolean> {
        BoolConst(Boolean value) {
            super(value);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Variable extends ASTNode {
        final String name;

        Variable(String name) {
            this.name = name;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
