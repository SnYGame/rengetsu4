package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import reactor.core.publisher.Mono;

public interface SelectMenuInteraction {
    String getName();

    Mono<Void> handle(SelectMenuInteractionEvent event);
}
