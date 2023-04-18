package org.snygame.rengetsu.util.agm;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class EffectStack {
    private final LuaTable table;
    private final LuaTable buffer;

    public EffectStack() {
        table = LuaValue.tableOf();
        buffer = LuaValue.tableOf();
        table.set("buffer", buffer);
    }

    public void flushBuffer() {
        buffer.next(null);
    }

    public LuaTable getTable() {
        return table;
    }
}
