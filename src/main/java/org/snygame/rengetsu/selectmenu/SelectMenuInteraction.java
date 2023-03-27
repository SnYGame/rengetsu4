package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;
import reactor.core.publisher.Mono;

public abstract class SelectMenuInteraction extends RengClass {
    public SelectMenuInteraction(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public abstract String getName();

    public abstract Mono<Void> handle(SelectMenuInteractionEvent event);
}
