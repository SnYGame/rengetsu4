package org.snygame.rengetsu.util.agm;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

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

        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder().title("Effect Stack");
        for (int i = length; i > 0; i--) {
            LuaValue entry = stack.get(i);
            builder.addField("[%d] ".formatted(i) + entry.get("name").tojstring(),
                    entry.get("tostring").call(entry).tojstring(), false);
        }

        ActionRow buttons = ActionRow.of(
                Button.primary("stack:pop:%d".formatted(userId), "Process one"),
                Button.primary("stack:clear:%d".formatted(userId), "Process all")
        );

        if (stackMessage == null) {
            stackMessage = channel.createMessage(builder.build()).withComponents(buttons).block();
        } else {
            stackMessage.edit().withComponents(buttons).withEmbeds(builder.build()).block();
        }
    }

    public synchronized void resendMessage(MessageChannel channel) {
        if (stackMessage != null) {
            stackMessage.delete().subscribe();
            stackMessage = null;
        }

        updateMessage(channel);
    }

    public String process(MessageChannel channel) {
        LuaValue top = stack.method("top");
        String output = top.method("execute").tojstring();
        stack.method("pop");
        flushBuffer();
        updateMessage(channel);
        return output;
    }

    public void clearbuffer() {
        stack.set("buffer", LuaValue.tableOf());
    }
}
