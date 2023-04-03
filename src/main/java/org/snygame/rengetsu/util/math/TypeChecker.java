package org.snygame.rengetsu.util.math;

import java.util.HashMap;

public class TypeChecker implements ASTVisitor<Type> {
    private final HashMap<String, Type> vMap = new HashMap<>();

    public Type.VarType[] addVariables(String... names) {
        Type.VarType[] types = new Type.VarType[names.length];
        for (int i = 0; i < names.length; i++) {
            types[i] = new Type.VarType(names[i]);
            vMap.put(names[i], types[i]);
        }
        return types;
    }

    public void addVariable(String name, Type type) {
        vMap.put(name, type);
    }

    private Type visitBoolOp(ASTNode.BoolOp node) {
        if (!node.lhs.accept(this).unify(Type.FixedType.BOOL) || !node.rhs.accept(this).unify(Type.FixedType.BOOL)) {
            throw new IllegalArgumentException("%s operands must be of type BOOL".formatted(node.name));
        }

        return Type.FixedType.BOOL;
    }

    private Type visitEqualityOp(ASTNode.EqualityOp node) {
        if (!node.lhs.accept(this).unify(node.rhs.accept(this))) {
            throw new IllegalArgumentException("%s operands must be of the same type".formatted(node.name));
        }

        return Type.FixedType.BOOL;
    }

    private Type visitComparisonOp(ASTNode.ComparisonOp node) {
        if (!node.lhs.accept(this).unify(Type.FixedType.NUM) || !node.rhs.accept(this).unify(Type.FixedType.NUM)) {
            throw new IllegalArgumentException("%s operands must be of type NUM".formatted(node.name));
        }

        return Type.FixedType.BOOL;
    }

    private Type visitArithmeticOp(ASTNode.ArithmeticOp node) {
        if (!node.lhs.accept(this).unify(Type.FixedType.NUM) || !node.rhs.accept(this).unify(Type.FixedType.NUM)) {
            throw new IllegalArgumentException("%s operands must be of type NUM".formatted(node.name));
        }

        return Type.FixedType.NUM;
    }

    @Override
    public Type visit(ASTNode.Assignment node) {
        Type type = node.src.accept(this);
        vMap.put(node.name, type);
        return type;
    }

    @Override
    public Type visit(ASTNode.Ternary node) {
        if (!node.condition.accept(this).unify(Type.FixedType.BOOL)) {
            throw new IllegalArgumentException("Condition must be of type BOOL");
        }

        if (!node.trueResult.accept(this).unify(node.falseResult.accept(this))) {
            throw new IllegalArgumentException("Ternary result must be of the same type");
        }

        return node.trueResult.accept(this);
    }

    @Override
    public Type visit(ASTNode.LogicOr node) {
        return visitBoolOp(node);
    }

    @Override
    public Type visit(ASTNode.LogicAnd node) {
        return visitBoolOp(node);
    }

    @Override
    public Type visit(ASTNode.Equals node) {
        return visitEqualityOp(node);
    }

    @Override
    public Type visit(ASTNode.NotEquals node) {
        return visitEqualityOp(node);
    }

    @Override
    public Type visit(ASTNode.LessThan node) {
        return visitComparisonOp(node);
    }

    @Override
    public Type visit(ASTNode.LessEquals node) {
        return visitComparisonOp(node);
    }

    @Override
    public Type visit(ASTNode.GreaterThan node) {
        return visitComparisonOp(node);
    }

    @Override
    public Type visit(ASTNode.GreaterEquals node) {
        return visitComparisonOp(node);
    }

    @Override
    public Type visit(ASTNode.Add node) {
        return visitArithmeticOp(node);
    }

    @Override
    public Type visit(ASTNode.Sub node) {
        return visitArithmeticOp(node);
    }

    @Override
    public Type visit(ASTNode.Mul node) {
        return visitArithmeticOp(node);
    }

    @Override
    public Type visit(ASTNode.Div node) {
        return visitArithmeticOp(node);
    }

    @Override
    public Type visit(ASTNode.IntDiv node) {
        return visitArithmeticOp(node);
    }

    @Override
    public Type visit(ASTNode.Mod node) {
        return visitArithmeticOp(node);
    }

    @Override
    public Type visit(ASTNode.Plus node) {
        if (node.value.accept(this).unify(Type.FixedType.NUM)) {
            throw new IllegalArgumentException("+ operand must be of type NUM");
        }

        return Type.FixedType.NUM;
    }

    @Override
    public Type visit(ASTNode.Minus node) {
        if (node.value.accept(this).unify(Type.FixedType.NUM)) {
            throw new IllegalArgumentException("- operand must be of type NUM");
        }

        return Type.FixedType.NUM;
    }

    @Override
    public Type visit(ASTNode.LogicNot node) {
        if (node.value.accept(this).unify(Type.FixedType.BOOL)) {
            throw new IllegalArgumentException("! operand must be of type BOOL");
        }

        return Type.FixedType.BOOL;
    }

    @Override
    public Type visit(ASTNode.Power node) {
        return visitArithmeticOp(node);
    }

    @Override
    public Type visit(ASTNode.Function node) {
        switch (node.name) {
            case "sqrt", "ln", "sin", "cos", "tan", "floor", "ceil", "trunc", "fact", "abs" -> {
                if (node.arguments.length != 1) {
                    throw new IllegalArgumentException("%s only takes 1 argument".formatted(node.name));
                }
                if (!node.arguments[0].accept(this).unify(Type.FixedType.NUM)) {
                    throw new IllegalArgumentException("%s argument must be of type NUM".formatted(node.name));
                }
                return Type.FixedType.NUM;
            }
            default -> throw new IllegalArgumentException("Unknown function: %s".formatted(node.name));
        }
    }

    @Override
    public Type visit(ASTNode.Dice node) {
        return Type.FixedType.NUM;
    }

    @Override
    public Type visit(ASTNode.IntConst node) {
        return Type.FixedType.NUM;
    }

    @Override
    public Type visit(ASTNode.FloatConst node) {
        return Type.FixedType.NUM;
    }

    @Override
    public Type visit(ASTNode.BoolConst node) {
        return Type.FixedType.BOOL;
    }

    @Override
    public Type visit(ASTNode.Variable node) {
        if (!vMap.containsKey(node.name)) {
            throw new IllegalArgumentException("Undefined variable: %s".formatted(node.name));
        }
        return vMap.get(node.name);
    }
}
