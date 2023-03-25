package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;
import reactor.core.publisher.Mono;

public abstract class SlashCommand extends RengClass {
    public SlashCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public abstract String getName();

    public abstract Mono<Void> handle(ChatInputInteractionEvent event);
}