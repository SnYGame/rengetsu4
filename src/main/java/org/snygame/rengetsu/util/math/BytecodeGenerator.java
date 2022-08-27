package org.snygame.rengetsu.util.math;

import java.util.*;
import java.util.stream.Collectors;

public class BytecodeGenerator implements ASTVisitor<Void> {
    private List<Bytecode> constants;
    private List<Bytecode> code;
    private Map<Object, Short> constantMap;
    private int byteCounter = 0;

    public BytecodeGenerator() {
        constants = new ArrayList<>();
        code = new ArrayList<>();
        constantMap = new HashMap<>();
    }

    @Override
    public Void visit(ASTNode.Ternary node) {
        Bytecode.Placeholder thenJump = new Bytecode.Placeholder();
        Bytecode.Placeholder elseJump = new Bytecode.Placeholder();
        node.condition.accept(this);

        addPlaceholder(thenJump, 3);
        int thenPos = byteCounter;

        node.falseResult.accept(this);

        addPlaceholder(elseJump, 3);
        int elsePos = byteCounter;

        thenJump.replace(new Bytecode(Bytecode.Opcode.BRANCH, shortToBytes((short) (1 + byteCounter - thenPos))));
        node.trueResult.accept(this);

        elseJump.replace(new Bytecode(Bytecode.Opcode.JUMP, shortToBytes((short) (1 + byteCounter - elsePos))));
        return null;
    }

    @Override
    public Void visit(ASTNode.LogicOr node) {
        Bytecode.Placeholder thenJump = new Bytecode.Placeholder();
        Bytecode.Placeholder elseJump = new Bytecode.Placeholder();
        node.lhs.accept(this);

        addPlaceholder(thenJump, 3);
        int thenPos = byteCounter;

        node.rhs.accept(this);

        addPlaceholder(elseJump, 3);
        int elsePos = byteCounter;

        thenJump.replace(new Bytecode(Bytecode.Opcode.BRANCH, shortToBytes((short) (1 + byteCounter - thenPos))));
        loadBool(true);

        elseJump.replace(new Bytecode(Bytecode.Opcode.JUMP, shortToBytes((short) (1 + byteCounter - elsePos))));
        return null;
    }

    @Override
    public Void visit(ASTNode.LogicAnd node) {
        Bytecode.Placeholder thenJump = new Bytecode.Placeholder();
        Bytecode.Placeholder elseJump = new Bytecode.Placeholder();
        node.lhs.accept(this);

        addPlaceholder(thenJump, 3);
        int thenPos = byteCounter;

        loadBool(false);

        addPlaceholder(elseJump, 3);
        int elsePos = byteCounter;

        thenJump.replace(new Bytecode(Bytecode.Opcode.BRANCH, shortToBytes((short) (1 + byteCounter - thenPos))));
        node.rhs.accept(this);

        elseJump.replace(new Bytecode(Bytecode.Opcode.JUMP, shortToBytes((short) (1 + byteCounter - elsePos))));
        return null;
    }

    @Override
    public Void visit(ASTNode.Equals node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.EQ));
        return null;
    }

    @Override
    public Void visit(ASTNode.NotEquals node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.NE));
        return null;
    }

    @Override
    public Void visit(ASTNode.LessThan node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.LT));
        return null;
    }

    @Override
    public Void visit(ASTNode.LessEquals node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.LE));
        return null;
    }

    @Override
    public Void visit(ASTNode.GreaterThan node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.GT));
        return null;
    }

    @Override
    public Void visit(ASTNode.GreaterEquals node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.GE));
        return null;
    }

    @Override
    public Void visit(ASTNode.Add node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.ADD));
        return null;
    }

    @Override
    public Void visit(ASTNode.Sub node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.SUB));
        return null;
    }

    @Override
    public Void visit(ASTNode.Mul node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.MUL));
        return null;
    }

    @Override
    public Void visit(ASTNode.Div node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.DIV));
        return null;
    }

    @Override
    public Void visit(ASTNode.IntDiv node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.IDIV));
        return null;
    }

    @Override
    public Void visit(ASTNode.Mod node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.MOD));
        return null;
    }

    @Override
    public Void visit(ASTNode.Plus node) {
        node.children().forEachOrdered(child -> child.accept(this));
        return null;
    }

    @Override
    public Void visit(ASTNode.Minus node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.NEG));
        return null;
    }

    @Override
    public Void visit(ASTNode.LogicNot node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.NEG));
        return null;
    }

    @Override
    public Void visit(ASTNode.Power node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.POW));
        return null;
    }

    @Override
    public Void visit(ASTNode.Function node) {
        node.children().forEachOrdered(child -> child.accept(this));
        addBytecode(new Bytecode(Bytecode.Opcode.CALL, (byte) Interpreter.functions.indexOf(node.name)));
        return null;
    }

    @Override
    public Void visit(ASTNode.Dice node) {
        long value = node.longValue();
        if (!constantMap.containsKey(value)) {
            constantMap.put(value, (short) constants.size());
            constants.add(new Bytecode(Bytecode.Opcode.DICE, longToBytes(value)));
        }

        short index = constantMap.get(value);
        addBytecode(new Bytecode(Bytecode.Opcode.ROLL, shortToBytes(index)));
        return null;
    }

    @Override
    public Void visit(ASTNode.IntConst node) {
        if (!constantMap.containsKey(node.value)) {
            constantMap.put(node.value, (short) constants.size());

            byte[] byteArray = node.value.toByteArray();
            byte[] params = new byte[byteArray.length + 1];
            params[0] = (byte) byteArray.length;
            System.arraycopy(byteArray, 0, params, 1, params[0]);
            constants.add(new Bytecode(Bytecode.Opcode.INT, params));
        }

        short index = constantMap.get(node.value);
        addBytecode(new Bytecode(Bytecode.Opcode.LOAD, shortToBytes(index)));
        return null;
    }

    @Override
    public Void visit(ASTNode.FloatConst node) {
        if (!constantMap.containsKey(node.value)) {
            constantMap.put(node.value, (short) constants.size());

            byte[] byteArray = node.value.unscaledValue().toByteArray();
            byte[] params = new byte[byteArray.length + 2];
            params[0] = (byte) byteArray.length;
            params[1] = (byte) node.value.scale();
            System.arraycopy(byteArray, 0, params, 2, params[0]);
            constants.add(new Bytecode(Bytecode.Opcode.FLOAT, params));
        }

        short index = constantMap.get(node.value);
        addBytecode(new Bytecode(Bytecode.Opcode.LOAD, shortToBytes(index)));
        return null;
    }

    @Override
    public Void visit(ASTNode.BoolConst node) {
        loadBool(node.value);
        return null;
    }

    public List<Byte> getBytecode() {
        List<Byte> bytecode = new ArrayList<>();
        for (Bytecode bytes: constants) {
            for (byte b: bytes.toBytes()) {
                bytecode.add(b);
            }
        }

        for (Bytecode bytes: code) {
            for (byte b: bytes.toBytes()) {
                bytecode.add(b);
            }
        }
        return bytecode;
    }

    public String getAsm() {
        return constants.stream().map("%s\n"::formatted).collect(Collectors.joining())
                + code.stream().map(Bytecode::toString).collect(Collectors.joining("\n"));
    }

    private byte[] longToBytes(long l) {
        return new byte[] {(byte) (l >> 56), (byte) (l >> 48), (byte) (l >> 40), (byte) (l >> 32),
                (byte) (l >> 24), (byte) (l >> 16), (byte) (l >> 8), (byte) l};
    }

    private byte[] shortToBytes(short s) {
        return new byte[] {(byte) (s >> 8), (byte) s};
    }

    private void loadBool(boolean bool) {
        if (!constantMap.containsKey(bool)) {
            constantMap.put(bool, (short) constants.size());
            constants.add(new Bytecode(Bytecode.Opcode.BOOL, (byte) (bool ? 1 : 0)));
        }

        short index = constantMap.get(bool);
        addBytecode(new Bytecode(Bytecode.Opcode.LOAD, shortToBytes(index)));
    }

    private void addBytecode(Bytecode bytecode) {
        code.add(bytecode);
        byteCounter += bytecode.toBytes().length;
    }

    private void addPlaceholder(Bytecode.Placeholder placeholder, int expectedSize) {
        code.add(placeholder);
        byteCounter += expectedSize;
    }
}
