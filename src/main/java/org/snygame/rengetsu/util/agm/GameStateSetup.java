package org.snygame.rengetsu.util.agm;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.snygame.rengetsu.util.DiceRoll;
import org.snygame.rengetsu.util.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

public class GameStateSetup {

    private static final ZeroArgFunction DEFAULT_COMMAND;
    private static final ZeroArgFunction DEFAULT_STAT_SHEET;
    private static final OneArgFunction FUNCTION_ROLL_DICE;

    static {
        DEFAULT_COMMAND = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                throw new LuaError("Unimplemented runcommand");
            }
        };

        DEFAULT_STAT_SHEET = new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf("No stat sheet set.");
            }
        };

        FUNCTION_ROLL_DICE = new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                DiceRoll dice = DiceRoll.parse(arg.tojstring());
                if (dice.hasError()) {
                    throw new LuaError(dice.getError());
                }
                DiceRoll.Result result = dice.roll();
                LuaTable luaResult = LuaValue.listOf(Arrays.stream(result.rolls()).mapToObj(LuaValue::valueOf).toArray(LuaValue[]::new));
                luaResult.set("sum", result.sum());
                luaResult.set("msg", result.toString());
                return luaResult;
            }
        };
    }

    static LuaTable setupAgm(Globals globals, long userId) {
        HashSet<String> modules = new HashSet<>();

        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        globals.load(new DebugLib());
        LoadState.install(globals);
        LuaC.install(globals);

        LuaTable agm = LuaValue.tableOf();
        globals.set("agm", agm);
        agm.set("dm", String.valueOf(userId));
        agm.set("runcommand", DEFAULT_COMMAND);
        agm.set("displaysheet", DEFAULT_STAT_SHEET);
        agm.set("rolldice", FUNCTION_ROLL_DICE);
        agm.set("requiredm", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (!agm.get("dm").equals(agm.get("user"))) {
                    throw new LuaError("You do not have permission to run this command");
                }
                return null;
            }
        });
        agm.set("isdm", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(agm.get("dm").equals(agm.get("user")));
            }
        });
        agm.set("import", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                // TODO verify name
                String module = arg.tojstring();
                if (!modules.contains(module)) {
                    globals.loadfile("./agm_modules/%s.lua".formatted(module)).call();
                    modules.add(module);
                }
                return null;
            }
        });
        // globals.set("print", LuaValue.NIL); TODO uncomment when done
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);

        InputStream stackLib = Resources.getResourceFileAsStream("agm/stack.lua");
        globals.load(new InputStreamReader(stackLib), "stack.lua").call();

        globals.loadfile("./agm_modules/testing.lua").call(); // TODO remove when done

        return agm;
    }
}
