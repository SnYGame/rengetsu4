package org.snygame.rengetsu.util.agm;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.Arrays;
import java.util.HashSet;

public class GameState {
    private final Globals globals;
    private final long userId;
    private final HashSet<String> modules = new HashSet<>();

    private static final ZeroArgFunction DEFAULT_COMMAND;
    private static final ZeroArgFunction DEFAULT_STAT_SHEET;

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
    }

    private void setupGlobals(Globals globals) {
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);

        globals.set("dm", String.valueOf(userId));
        globals.set("runcommand", DEFAULT_COMMAND);
        globals.set("displaysheet", DEFAULT_STAT_SHEET);
        globals.set("requiredm", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (!globals.get("dm").equals(globals.get("user"))) {
                    throw new LuaError("You do not have permission to run this command");
                }
                return null;
            }
        });
        globals.set("isdm", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(globals.get("dm").equals(globals.get("user")));
            }
        });
        globals.set("import", new OneArgFunction() {
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

        globals.loadfile("./agm_modules/testing.lua").call(); // TODO remove when done
    }

    public GameState(long userId) {
        globals = new Globals();
        this.userId = userId;
        setupGlobals(globals);
    }

    public void runCommand(long userId, String[] args) {
        globals.set("user", String.valueOf(userId));
        globals.get("runcommand").call(LuaValue.listOf(Arrays.stream(args).map(LuaValue::valueOf).toArray(LuaValue[]::new)));
        // TODO handle infinite loops
    }

    public String showSheet() {
        return globals.get("displaysheet").call().tojstring();
        // TODO handle infinite loops
    }
}
