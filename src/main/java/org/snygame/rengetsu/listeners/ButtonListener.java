package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.buttons.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ButtonListener extends Listener {
    private final List<ButtonInteraction> commands = new ArrayList<>();

    public ButtonListener(Rengetsu rengetsu) {
        super(rengetsu);

        commands.add(new SaltClaimButton(rengetsu));
        commands.add(new TimerButton(rengetsu));
        commands.add(new RoleSetButton(rengetsu));
        commands.add(new RequestRoleAgreementButton(rengetsu));
        commands.add(new PrepEditButton(rengetsu));
        commands.add(new ManualPageButton(rengetsu));
    }

    public Mono<Void> handle(ButtonInteractionEvent event) {
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
