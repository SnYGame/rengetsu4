package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;
import reactor.core.publisher.Mono;

public abstract class ModalInteraction extends RengClass {
    public ModalInteraction(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public abstract String getName();

    public abstract Mono<Void> handle(ModalSubmitInteractionEvent event);
}
