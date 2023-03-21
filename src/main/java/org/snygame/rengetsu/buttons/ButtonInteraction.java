package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface ButtonInteraction {
    String getName();

    Mono<Void> handle(ButtonInteractionEvent event);
}
