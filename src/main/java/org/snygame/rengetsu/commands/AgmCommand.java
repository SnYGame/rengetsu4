package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class AgmCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public AgmCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "agm";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String[] args = event.getOption("command").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("").split(" ");
        long userId = event.getInteraction().getUser().getId().asLong(); // TODO get joined ID as well
        rengetsu.getAgmManager().getGameState(userId).runCommand(userId, args);
        return event.reply("placeholder");
    }
}
