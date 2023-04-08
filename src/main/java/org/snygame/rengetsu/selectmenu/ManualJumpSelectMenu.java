package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import reactor.core.publisher.Mono;

public class ManualJumpSelectMenu extends SelectMenuInteraction {
    public ManualJumpSelectMenu(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "manual";
    }

    @Override
    public Mono<Void> handle(SelectMenuInteractionEvent event) {
        return event.edit(rengetsu.getManual().getPage(Integer.parseInt(event.getValues().get(0))));
    }
}
