package org.snygame.rengetsu.util.agm;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Arrays;

public class GameState {
    private final LuaTable agm;
    private final EffectStack effectStack;

    public GameState(long userId) {
        Globals globals = new Globals();
        effectStack = new EffectStack();
        agm = GameStateSetup.setupAgm(globals, effectStack, userId);
    }

    public String runCommand(long userId, String[] args) {
        agm.set("user", String.valueOf(userId));
        String output = agm.get("runcommand").invoke(Arrays.stream(args).map(LuaValue::valueOf).toArray(LuaValue[]::new)).tojstring();
        // TODO handle infinite loops
        return output;
    }

    public String showSheet() {
        return agm.get("displaysheet").call().tojstring();
        // TODO handle infinite loops
    }
}
