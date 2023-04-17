package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.agm.GameState;
import reactor.core.publisher.Mono;

public class AgmSheetCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public AgmSheetCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "agm-sheet";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        long userId = event.getInteraction().getUser().getId().asLong();
        GameState gameState = rengetsu.getAgmManager().getGameState(userId);
        return event.reply(gameState.showSheet());
    }
}
