package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;
import reactor.core.publisher.Mono;

public abstract class ButtonInteraction extends RengClass {
    public ButtonInteraction(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public abstract String getName();

    public abstract Mono<Void> handle(ButtonInteractionEvent event);
}
