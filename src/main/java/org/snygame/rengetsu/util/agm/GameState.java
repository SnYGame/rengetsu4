package org.snygame.rengetsu.util.agm;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Arrays;

public class GameState {
    private final LuaTable agm;

    public GameState(long userId) {
        Globals globals = new Globals();
        agm = GameStateSetup.setupAgm(globals, userId);
    }

    public void runCommand(long userId, String[] args) {
        agm.set("user", String.valueOf(userId));
        agm.get("runcommand").invoke(Arrays.stream(args).map(LuaValue::valueOf).toArray(LuaValue[]::new));
        // TODO handle infinite loops
    }

    public String showSheet() {
        return agm.get("displaysheet").call().tojstring();
        // TODO handle infinite loops
    }
}
