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
    private final Map<String, Type> vMap;

    public ASTNode(Map<String, Type> vMap, ASTNode... children) {
        this.vMap = vMap;
        this.children = children;
    }

    public abstract Type getType();

    public abstract <T> T accept(ASTVisitor<T> visitor);

    public Stream<ASTNode> children() {
        return Stream.of(children);
    }

    public Map<String, Type> getVariableTypes() {
        return vMap;
    }

    @Override
    public String toString() {
        return "%s(%s)[%s]".formatted(getClass().getSimpleName(), getType(), repr());
    }

    protected String repr() {
        return Stream.of(children).map(String::valueOf).collect(Collectors.joining(", "));
    }

    public sealed interface Type permits FixedType, VarType {
        boolean unify(Type type);
    }

    public enum FixedType implements Type {
        ANY, BOOL, NUM;

        @Override
        public boolean unify(Type other) {
            if (other instanceof FixedType) {
                return this == other;
            }
            return other.unify(this);
        }
    }

    public static final class VarType implements Type {
        private Type type = null;
        private final String name;

        public VarType(String name) {
            this.name = name;
        }

        @Override
        public boolean unify(Type other) {
            if (this == other) {
                return true;
            }

            if (type == null) {
                switch (other) {
                    case FixedType fixedType -> type = fixedType;
                    case VarType varType -> {
                        while (varType.type instanceof VarType vType) {
                            varType = vType;
                        }
                        if (varType != this) {
                            type = varType.type == null ? varType : varType.type;
                        }
                    }
                }
                return true;
            }

            return type.unify(other);
        }

        public Type getInferredType() {
            if (type == null) {
                return FixedType.ANY;
            }

            VarType varType = this;
            while (varType.type instanceof VarType vType) {
                varType = vType;
            }

            if (varType.type == null) {
                return varType;
            }
            return varType.type;
        }

        @Override
        public String toString() {
            switch (getInferredType()) {
                case VarType type -> {
                    return "VarType(Same as %s)".formatted(type.name);
                }
                case FixedType type -> {
                    return "VarType(%s)".formatted(type);
                }
            }

            return null;
        }
    }

    public static abstract class BoolOp extends ASTNode {
        final ASTNode lhs;
        final ASTNode rhs;
        private final String name;

        BoolOp(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs, String name) {
            super(vMap, lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        public Type getType() {
            if (!lhs.getType().unify(FixedType.BOOL) || !rhs.getType().unify(FixedType.BOOL)) {
                throw new IllegalArgumentException("%s operands must be of type BOOL".formatted(name));
            }

            return FixedType.BOOL;
        }
    }

    public static abstract class EqualityOp extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;
        private final String name;

        EqualityOp(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs, String name) {
            super(vMap, lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        public Type getType() {
            if (!lhs.getType().unify(rhs.getType())) {
                throw new IllegalArgumentException("%s operands must be of the same type".formatted(name));
            }

            return FixedType.BOOL;
        }
    }

    public static abstract class ComparisonOp extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;
        private final String name;

        ComparisonOp(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs, String name) {
            super(vMap, lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        public Type getType() {
            if (!lhs.getType().unify(FixedType.NUM) || !rhs.getType().unify(FixedType.NUM)) {
                throw new IllegalArgumentException("%s operands must be numerical".formatted(name));
            }

            return FixedType.BOOL;
        }
    }

    public static abstract class ArithmeticOp extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;
        private final String name;

        ArithmeticOp(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs, String name) {
            super(vMap, lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
            this.name = name;
        }

        @Override
        public Type getType() {
            if (!lhs.getType().unify(FixedType.NUM) || !rhs.getType().unify(FixedType.NUM)) {
                throw new IllegalArgumentException("%s operands must be numerical".formatted(name));
            }

            return FixedType.NUM;
        }
    }

    public static abstract class Constant<T> extends ASTNode {
        public final T value;
        private final FixedType type;

        Constant(Map<String, Type> vMap, T value, FixedType type) {
            super(vMap);
            this.value = value;
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        protected String repr() {
            return value.toString();
        }
    }

    public static class Assignment extends ASTNode {
        final String name;
        final ASTNode src;

        Assignment(Map<String, Type> vMap, String name, ASTNode src) {
            super(vMap);
            this.name = name;
            this.src = src;
        }

        @Override
        public Type getType() {
            Type type = src.getType();
            getVariableTypes().put(name, type);
            return type;
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

        Ternary(Map<String, Type> vMap, ASTNode condition, ASTNode trueResult, ASTNode falseResult) {
            super(vMap, condition, trueResult, falseResult);
            this.condition = condition;
            this.trueResult = trueResult;
            this.falseResult = falseResult;
        }

        @Override
        public Type getType() {
            if (!condition.getType().unify(FixedType.BOOL)) {
                throw new IllegalArgumentException("Condition must be of type BOOL");
            }

            if (!trueResult.getType().unify(falseResult.getType())) {
                throw new IllegalArgumentException("Ternary result must be of the same type");
            }

            return trueResult.getType();
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogicOr extends BoolOp {
        LogicOr(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "||");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogicAnd extends BoolOp {
        LogicAnd(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "&&");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Equals extends EqualityOp {
        Equals(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "==");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class NotEquals extends EqualityOp {
        NotEquals(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "!=");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LessThan extends ComparisonOp {
        LessThan(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "<");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GreaterThan extends ComparisonOp {
        GreaterThan(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, ">");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LessEquals extends ComparisonOp {
        LessEquals(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "<=");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GreaterEquals extends ComparisonOp {
        GreaterEquals(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, ">=");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Add extends ArithmeticOp {
        Add(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "+");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Sub extends ArithmeticOp {
        Sub(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "-");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Mul extends ArithmeticOp {
        Mul(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "*");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Div extends ArithmeticOp {
        Div(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "/");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class IntDiv extends ArithmeticOp {
        IntDiv(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "//");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Mod extends ArithmeticOp {
        Mod(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs, "%");
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Plus extends ASTNode {
        private final ASTNode value;

        Plus(Map<String, Type> vMap, ASTNode value) {
            super(vMap, value);
            this.value = value;
        }

        @Override
        public Type getType() {
            if (value.getType().unify(FixedType.NUM)) {
                throw new IllegalArgumentException("+ operand must be numerical");
            }

            return FixedType.NUM;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Minus extends ASTNode {
        private final ASTNode value;

        Minus(Map<String, Type> vMap, ASTNode value) {
            super(vMap, value);
            this.value = value;
        }

        @Override
        public Type getType() {
            if (value.getType().unify(FixedType.NUM)) {
                throw new IllegalArgumentException("- operand must be numerical");
            }

            return FixedType.NUM;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogicNot extends ASTNode {
        private final ASTNode value;

        LogicNot(Map<String, Type> vMap, ASTNode value) {
            super(vMap, value);
            this.value = value;
        }

        @Override
        public Type getType() {
            if (value.getType().unify(FixedType.BOOL)) {
                throw new IllegalArgumentException("! operand must be of type BOOL");
            }

            return FixedType.BOOL;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Power extends ASTNode {
        private final ASTNode lhs;
        private final ASTNode rhs;

        Power(Map<String, Type> vMap, ASTNode lhs, ASTNode rhs) {
            super(vMap, lhs, rhs);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Type getType() {
            if (!lhs.getType().unify(FixedType.NUM) || !rhs.getType().unify(FixedType.NUM)) {
                throw new IllegalArgumentException("^ operands must be numerical");
            }

            return FixedType.NUM;
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Function extends ASTNode {
        final String name;
        private final ASTNode[] arguments;

        Function(Map<String, Type> vMap, String name, ASTNode... arguments) {
            super(vMap, arguments);
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public Type getType() {
            switch (name) {
                case "sqrt", "ln", "sin", "cos", "tan", "floor", "ceil", "trunc", "fact", "abs" -> {
                    if (arguments.length != 1) {
                        throw new IllegalArgumentException("%s only takes 1 argument".formatted(name));
                    }
                    if (!arguments[0].getType().unify(FixedType.NUM)) {
                        throw new IllegalArgumentException("%s argument must be numerical".formatted(name));
                    }
                    return FixedType.NUM;
                }
                default -> throw new IllegalArgumentException("Unknown function: %s".formatted(name));
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

        Dice(Map<String, Type> vMap, String dice) {
            super(vMap);
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
        public Type getType() {
            return FixedType.NUM;
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
        IntConst(Map<String, Type> vMap, BigInteger value) {
            super(vMap, value, FixedType.NUM);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FloatConst extends Constant<BigDecimal> {
        FloatConst(Map<String, Type> vMap, BigDecimal value) {
            super(vMap, value, FixedType.NUM);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BoolConst extends Constant<Boolean> {
        BoolConst(Map<String, Type> vMap, Boolean value) {
            super(vMap, value, FixedType.BOOL);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Variable extends ASTNode {
        final String name;

        Variable(Map<String, Type> vMap, String name) {
            super(vMap);
            this.name = name;
        }

        @Override
        public Type getType() {
            Map<String, Type> vMap = getVariableTypes();
            if (!vMap.containsKey(name)) {
                throw new IllegalStateException("Undefined variable: %s".formatted(name));
            }
            return vMap.get(name);
        }

        @Override
        public <T> T accept(ASTVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
