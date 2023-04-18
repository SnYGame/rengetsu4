package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class GameManageCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public GameManageCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "gamemanage";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.getOption("reset").map(option -> subReset(event, option))
                .or(() -> event.getOption("load").map(option -> subLoad(event, option)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subReset(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        rengetsu.getAgmManager().resetGameState(event.getInteraction().getUser().getId().asLong());
        return event.reply("Reset").withEphemeral(true);
    }

    private Mono<Void> subLoad(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }
}
