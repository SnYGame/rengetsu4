package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.rest.util.AllowedMentions;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.agm.GameState;
import reactor.core.publisher.Mono;

public class StackCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public StackCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "stack";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        long userId = event.getInteraction().getUser().getId().asLong();
        GameState gameState = rengetsu.getAgmManager().getGameState(userId);
        return event.getInteraction().getChannel().flatMap(channel -> {
            gameState.resendStack(channel);
            return event.reply("Done").withEphemeral(true);
        });
    }
}
