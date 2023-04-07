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
        String[] args = event.getCustomId().split(":");
        long userId = Long.parseLong(args[1]);
        if (event.getInteraction().getUser().getId().asLong() != userId) {
            return event.reply("**[Error]** You do not have permission to do that").withEphemeral(true);
        }
        return event.edit(rengetsu.getManual().getPage(userId, Integer.parseInt(event.getValues().get(0))));
    }
}
