package org.snygame.rengetsu.util.agm;

import discord4j.core.object.entity.channel.MessageChannel;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Arrays;

public class GameState {
    private final LuaTable agm;
    private final EffectStack effectStack;

    public GameState(long userId) {
        Globals globals = new Globals();
        agm = GameStateSetup.setupAgm(globals, userId);
        effectStack = new EffectStack(agm.get("stack").checktable(), userId);
    }

    public String runCommand(MessageChannel channel, long userId, String[] args) {
        agm.set("user", String.valueOf(userId));
        String output = agm.get("runcommand").invoke(Arrays.stream(args).map(LuaValue::valueOf).toArray(LuaValue[]::new)).tojstring();
        // TODO handle infinite loops
        if (effectStack.flushBuffer()) {
            effectStack.updateMessage(channel);
        }
        return output;
    }

    public String showSheet() {
        return agm.get("displaysheet").call().tojstring();
        // TODO handle infinite loops
    }

    public void resendStack(MessageChannel channel) {
        effectStack.resendMessage(channel);
    }

    public String processStack(MessageChannel channel) {
        try {
            return effectStack.process(channel);
        } catch (LuaError e) {
            effectStack.clearbuffer();
            throw e;
        }
    }
}
