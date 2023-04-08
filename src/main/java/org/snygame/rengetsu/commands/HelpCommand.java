package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import reactor.core.publisher.Mono;

public class HelpCommand extends SlashCommand {
    public HelpCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply(rengetsu.getManual().getPage(0));
    }
}
