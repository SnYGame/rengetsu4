package org.snygame.rengetsu.util.agm;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.StringJoiner;

public class EffectStack {
    private final LuaTable stack;
    private Message stackMessage;
    private final long userId;

    public EffectStack(LuaTable table, long userId) {
        this.stack = table;
        this.userId = userId;
    }

    public synchronized boolean flushBuffer() {
        return stack.method("flushbuffer").toboolean();
    }

    public synchronized void updateMessage(MessageChannel channel) {
        int length = stack.length();
        ActionRow[] buttons = new ActionRow[0];

        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder().title("Effect Stack");

        if (length == 0) {
            builder.description("All effects resolved");
        }
        for (int i = length; i > 0; i--) {
            LuaValue entry = stack.get(i);
            builder.addField("[%d] ".formatted(i) + entry.get("name").tojstring(),
                    entry.get("tostring").call(entry).tojstring(), false);
        }

        if (length > 0) {
            buttons = new ActionRow[]{
                    ActionRow.of(
                            Button.primary("stack:pop:%d".formatted(userId), "Process one"),
                            Button.primary("stack:clear:%d".formatted(userId), "Process all")
                    )};
        }

        if (stackMessage == null) {
            stackMessage = channel.createMessage(builder.build()).withComponents(buttons).block();
        } else {
            stackMessage.edit().withComponents(buttons).withEmbeds(builder.build()).block();
        }

        if (length == 0) {
            stackMessage = null;
        }
    }

    public synchronized void resendMessage(MessageChannel channel) {
        if (stackMessage != null) {
            stackMessage.delete().subscribe();
            stackMessage = null;
        }

        updateMessage(channel);
    }

    public void process(MessageChannel channel) {
        LuaValue top = stack.method("top");
        top.method("execute");
        stack.method("pop");
        flushBuffer();
        updateMessage(channel);
    }

    public void processAll(MessageChannel channel) {
        while (stack.length() > 0) {
            LuaValue top = stack.method("top");
            top.method("execute");
            stack.method("pop");
            flushBuffer();
        }

        updateMessage(channel);
    }

    public void clearbuffer() {
        stack.set("buffer", LuaValue.tableOf());
    }
}
