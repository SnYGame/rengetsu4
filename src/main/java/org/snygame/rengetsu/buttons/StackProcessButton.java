package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class StackProcessButton extends InteractionListener.CommandDelegate<ButtonInteractionEvent> {
    public StackProcessButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "stack";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        String[] args = event.getCustomId().split(":");
        long userId = Long.parseLong(args[2]);
        if (event.getInteraction().getUser().getId().asLong() != userId) {
            return event.reply("**[Error]** You do not have permission to do that").withEphemeral(true);
        }

        return event.getInteraction().getChannel().flatMap(channel -> {
            switch (args[1]) {
                case "pop" -> {
                    return event.reply(rengetsu.getAgmManager().getGameState(userId).processStack(channel));
                }
                case "clear" -> {
                    return event.reply(rengetsu.getAgmManager().getGameState(userId).processStackAll(channel));
                }
            }
            return Mono.error(new IllegalStateException());
        });
    }
}
