package org.snygame.rengetsu.util.math;

import java.util.ArrayList;
import java.util.List;

public class Bytecode {
    private final Opcode opcode;
    private final byte[] params;

    public Bytecode(Opcode opcode, byte... params) {
        this.opcode = opcode;
        this.params = params;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[params.length + 1];
        bytes[0] = (byte) opcode.ordinal();
        System.arraycopy(params, 0, bytes, 1, params.length);
        return bytes;
    }

    @Override
    public String toString() {
        if (params.length == 0) {
            return opcode.name();
        }

        List<String> paramStr = new ArrayList<>();
        for (byte param: params) {
            paramStr.add(String.valueOf(Byte.toUnsignedInt(param)));
        }
        return "%s %s".formatted(opcode.name(), String.join(", ", paramStr));
    }

    public enum Opcode {
        NOP, LOAD, ADD, SUB, MUL, DIV, IDIV, MOD, POW, NEG, EQ, NE, LT, LE, GT, GE, BRANCH, JUMP,
        CALL, ROLL, INT, FLOAT, BOOL, DICE, STOVAR, LOADVAR;
    }

    public static class Placeholder extends Bytecode {
        private Bytecode replacement;

        public Placeholder() {
            super(Opcode.NOP);
        }

        public void replace(Bytecode replacement) {
            this.replacement = replacement;
        }

        @Override
        public byte[] toBytes() {
            return replacement.toBytes();
        }

        @Override
        public String toString() {
            return replacement.toString();
        }
    }
}
