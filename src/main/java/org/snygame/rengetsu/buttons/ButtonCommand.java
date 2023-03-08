package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface ButtonCommand {
    String getName();

    Mono<Void> handle(ButtonInteractionEvent event);
}
