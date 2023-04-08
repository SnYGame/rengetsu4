package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.selectmenu.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class SelectMenuListener extends Listener {
    private final List<SelectMenuInteraction> commands = new ArrayList<>();

    public SelectMenuListener(Rengetsu rengetsu) {
        super(rengetsu);

        commands.add(new RoleRemovalSelectMenu(rengetsu));
        commands.add(new PrepRollRemovalSelectMenu(rengetsu));
        commands.add(new ManualJumpSelectMenu(rengetsu));
        commands.add(new SettingsLogSelectMenu(rengetsu));
    }

    public Mono<Void> handle(SelectMenuInteractionEvent event) {
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
