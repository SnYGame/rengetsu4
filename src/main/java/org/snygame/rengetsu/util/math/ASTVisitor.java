package org.snygame.rengetsu.util.math;

public interface ASTVisitor<T> {
    T visit(ASTNode.Ternary node);
    T visit(ASTNode.LogicOr node);
    T visit(ASTNode.LogicAnd node);
    T visit(ASTNode.Equals node);
    T visit(ASTNode.NotEquals node);
    T visit(ASTNode.LessThan node);
    T visit(ASTNode.LessEquals node);
    T visit(ASTNode.GreaterThan node);
    T visit(ASTNode.GreaterEquals node);
    T visit(ASTNode.Add node);
    T visit(ASTNode.Sub node);
    T visit(ASTNode.Mul node);
    T visit(ASTNode.Div node);
    T visit(ASTNode.IntDiv node);
    T visit(ASTNode.Mod node);
    T visit(ASTNode.Plus node);
    T visit(ASTNode.Minus node);
    T visit(ASTNode.LogicNot node);
    T visit(ASTNode.Power node);
    T visit(ASTNode.Function node);
    T visit(ASTNode.Dice node);
    T visit(ASTNode.IntConst node);
    T visit(ASTNode.FloatConst node);
    T visit(ASTNode.BoolConst node);
}
