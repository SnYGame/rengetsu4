package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class ManualPageButton extends InteractionListener.CommandDelegate<ButtonInteractionEvent> {
    public ManualPageButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "manual";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        String[] args = event.getCustomId().split(":");
        return event.edit(rengetsu.getManual().getPage(Integer.parseInt(args[1])));
    }
}
