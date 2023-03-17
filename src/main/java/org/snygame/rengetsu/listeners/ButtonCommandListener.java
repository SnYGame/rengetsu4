package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.snygame.rengetsu.buttons.ButtonCommand;
import org.snygame.rengetsu.buttons.CancelTimerCommand;
import org.snygame.rengetsu.buttons.SaltClaimCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ButtonCommandListener {
    private final static List<ButtonCommand> commands = new ArrayList<>();

    static {
        //We register our commands here when the class is initialized
        commands.add(new SaltClaimCommand());
        commands.add(new CancelTimerCommand());
    }

    public static Mono<Void> handle(ButtonInteractionEvent event) {
        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name of the command this event is for
                .filter(command -> command.getName().equals(event.getCustomId().split(":")[0]))
                // Get the first (and only) item in the flux that matches our filter
                .next()
                //have our command class handle all the logic related to its specific command.
                .flatMap(command -> command.handle(event));
    }
}
