package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

public interface ModalInteraction {
    String getName();

    Mono<Void> handle(ModalSubmitInteractionEvent event);
}
