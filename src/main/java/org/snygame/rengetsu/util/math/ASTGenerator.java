package org.snygame.rengetsu.util.math;

import org.snygame.rengetsu.parser.RengCalcBaseVisitor;
import org.snygame.rengetsu.parser.RengCalcParser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

public class ASTGenerator extends RengCalcBaseVisitor<ASTNode> {
    private final HashMap<String, ASTNode.VarType> vMap = new HashMap<>();

    @Override
    public ASTNode visitCalculation(RengCalcParser.CalculationContext ctx) {
        return visitTernaryExpression(ctx.ternaryExpression());
    }

    @Override
    public ASTNode visitTernaryExpression(RengCalcParser.TernaryExpressionContext ctx) {
        if (ctx.ternaryExpression().isEmpty()) {
            return visit(ctx.logicOrExpression());
        }

        return new ASTNode.Ternary(vMap, visit(ctx.logicOrExpression()),
                visit(ctx.ternaryExpression(0)), visit(ctx.ternaryExpression(1)));
    }

    @Override
    public ASTNode visitLogicOrExpression(RengCalcParser.LogicOrExpressionContext ctx) {
        if (ctx.logicOrExpression() == null) {
            return visit(ctx.logicAndExpression());
        }

        return new ASTNode.LogicOr(vMap, visit(ctx.logicOrExpression()), visit(ctx.logicAndExpression()));
    }

    @Override
    public ASTNode visitLogicAndExpression(RengCalcParser.LogicAndExpressionContext ctx) {
        if (ctx.logicAndExpression() == null) {
            return visit(ctx.equalityExpression());
        }

        return new ASTNode.LogicAnd(vMap, visit(ctx.logicAndExpression()), visit(ctx.equalityExpression()));
    }

    @Override
    public ASTNode visitEqualityExpression(RengCalcParser.EqualityExpressionContext ctx) {
        if (ctx.equalityExpression() == null) {
            return visit(ctx.comparisonExpression());
        }

        switch (ctx.equalityOp().getText()) {
            case "==":
                return new ASTNode.Equals(vMap, visit(ctx.equalityExpression()), visit(ctx.comparisonExpression()));
            case "!=":
                return new ASTNode.NotEquals(vMap, visit(ctx.equalityExpression()), visit(ctx.comparisonExpression()));
        }

        throw new IllegalStateException();
    }

    @Override
    public ASTNode visitComparisonExpression(RengCalcParser.ComparisonExpressionContext ctx) {
        if (ctx.comparisonExpression() == null) {
            return visit(ctx.additiveExpression());
        }

        switch (ctx.comparisonOp().getText()) {
            case "<":
                return new ASTNode.LessThan(vMap, visit(ctx.comparisonExpression()), visit(ctx.additiveExpression()));
            case "<=":
                return new ASTNode.LessEquals(vMap, visit(ctx.comparisonExpression()), visit(ctx.additiveExpression()));
            case ">":
                return new ASTNode.GreaterThan(vMap, visit(ctx.comparisonExpression()), visit(ctx.additiveExpression()));
            case ">=":
                return new ASTNode.GreaterEquals(vMap, visit(ctx.comparisonExpression()), visit(ctx.additiveExpression()));
        }

        throw new IllegalStateException();
    }

    @Override
    public ASTNode visitAdditiveExpression(RengCalcParser.AdditiveExpressionContext ctx) {
        if (ctx.additiveExpression() == null) {
            return visit(ctx.multiplicativeExpression());
        }

        switch (ctx.additiveOp().getText()) {
            case "+":
                return new ASTNode.Add(vMap, visit(ctx.additiveExpression()), visit(ctx.multiplicativeExpression()));
            case "-":
                return new ASTNode.Sub(vMap, visit(ctx.additiveExpression()), visit(ctx.multiplicativeExpression()));
        }

        throw new IllegalStateException();
    }

    @Override
    public ASTNode visitMultiplicativeExpression(RengCalcParser.MultiplicativeExpressionContext ctx) {
        if (ctx.multiplicativeExpression() == null) {
            return visit(ctx.unaryExpression());
        }

        switch (ctx.multiplicativeOp().getText()) {
            case "*":
                return new ASTNode.Mul(vMap, visit(ctx.multiplicativeExpression()), visit(ctx.unaryExpression()));
            case "/":
                return new ASTNode.Div(vMap, visit(ctx.multiplicativeExpression()), visit(ctx.unaryExpression()));
            case "//":
                return new ASTNode.IntDiv(vMap, visit(ctx.multiplicativeExpression()), visit(ctx.unaryExpression()));
            case "%":
                return new ASTNode.Mod(vMap, visit(ctx.multiplicativeExpression()), visit(ctx.unaryExpression()));
        }

        throw new IllegalStateException();
    }

    @Override
    public ASTNode visitUnaryExpression(RengCalcParser.UnaryExpressionContext ctx) {
        if (ctx.unaryOp() == null) {
            return visit(ctx.exponentialExpression());
        }

        switch (ctx.unaryOp().getText()) {
            case "+":
                return new ASTNode.Plus(vMap, visit(ctx.exponentialExpression()));
            case "-":
                return new ASTNode.Minus(vMap, visit(ctx.exponentialExpression()));
            case "!":
                return new ASTNode.LogicNot(vMap, visit(ctx.exponentialExpression()));
        }

        throw new IllegalStateException();
    }

    @Override
    public ASTNode visitExponentialExpression(RengCalcParser.ExponentialExpressionContext ctx) {
        if (ctx.exponentialExpression() == null) {
            return visit(ctx.primaryExpression());
        }

        return new ASTNode.Power(vMap, visit(ctx.primaryExpression()), visit(ctx.exponentialExpression()));
    }

    @Override
    public ASTNode visitPrimaryExpression(RengCalcParser.PrimaryExpressionContext ctx) {
        if (ctx.ternaryExpression() != null) {
            return visit(ctx.ternaryExpression());
        }

        if (ctx.callExpression() != null) {
            return visit(ctx.callExpression());
        }

        if (ctx.DiceRoll() != null) {
            return new ASTNode.Dice(vMap, ctx.DiceRoll().getText());
        }

        if (ctx.IntegerConstant() != null) {
            return new ASTNode.IntConst(vMap, new BigInteger(ctx.IntegerConstant().getText()));
        }

        if (ctx.FloatConstant() != null) {
            return new ASTNode.FloatConst(vMap, new BigDecimal(ctx.FloatConstant().getText()));
        }

        if (ctx.BoolConstant() != null) {
            return new ASTNode.BoolConst(vMap, Boolean.valueOf(ctx.BoolConstant().getText()));
        }

        if (ctx.Variable() != null) {
            return new ASTNode.Variable(vMap, ctx.Variable().getText());
        }

        throw new IllegalStateException();
    }

    @Override
    public ASTNode visitCallExpression(RengCalcParser.CallExpressionContext ctx) {
        return new ASTNode.Function(vMap, ctx.Variable().getText(),
                ctx.parameterList().ternaryExpression().stream().map(this::visit).toArray(ASTNode[]::new));
    }
}
