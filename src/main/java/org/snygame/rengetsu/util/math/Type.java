package org.snygame.rengetsu.util.math;

public sealed interface Type permits Type.FixedType, Type.VarType {
    boolean unify(Type type);

    enum FixedType implements Type {
        VAR, ANY, BOOL, NUM;

        @Override
        public boolean unify(Type other) {
            if (other instanceof FixedType) {
                return this == other;
            }
            return other.unify(this);
        }
    }

    final class VarType implements Type {
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

        public String getName() {
            return name;
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
}
