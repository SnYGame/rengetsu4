package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public abstract class InteractionListener<T extends ComponentInteractionEvent> extends Listener {
    private final List<CommandDelegate<T>> commands;

    public InteractionListener(Rengetsu rengetsu, List<CommandDelegate<T>> commands) {
        super(rengetsu);
        this.commands = commands;
    }

    public final Mono<Void> handle(T event) {
        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name of the command this event is for
                .filter(command -> command.getName().equals(event.getCustomId().split(":")[0]))
                // Get the first (and only) item in the flux that matches our filter
                .next()
                //have our command class handle all the logic related to its specific command.
                .flatMap(command -> command.handle(event))
                .onErrorResume(Exception.class, e -> {
                    Rengetsu.getLOGGER().error("Uncaught exception in command", e);
                    return event.reply("**[Error]** An uncaught exception has occurred. Please notify the bot manager.\n%s".formatted(e)).withEphemeral(true);
                });
    }

    public abstract static class CommandDelegate<T extends DeferrableInteractionEvent> extends RengClass {
        public CommandDelegate(Rengetsu rengetsu) {
            super(rengetsu);
        }

        public abstract String getName();

        public abstract Mono<Void> handle(T event);
    }
}
