package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.messagecommands.ReportCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class MessageCommandListener extends Listener {
    private final List<InteractionListener.CommandDelegate<MessageInteractionEvent>> commands;

    public MessageCommandListener(Rengetsu rengetsu) {
        super(rengetsu);

        commands = List.of(new ReportCommand(rengetsu));
    }

    public Mono<Void> handle(MessageInteractionEvent event) {
        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name of the command this event is for
                .filter(command -> command.getName().equals(event.getCommandName()))
                // Get the first (and only) item in the flux that matches our filter
                .next()
                //have our command class handle all the logic related to its specific command.
                .flatMap(command -> command.handle(event))
                .onErrorResume(Exception.class, e -> {
                    Rengetsu.getLOGGER().error("Uncaught exception in command", e);
                    return event.reply("**[Error]** An uncaught exception has occurred. Please notify the bot manager.\n%s".formatted(e)).withEphemeral(true);
                });
    }
}
