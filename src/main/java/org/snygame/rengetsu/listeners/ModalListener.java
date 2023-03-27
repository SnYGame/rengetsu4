package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.modals.ModalInteraction;
import org.snygame.rengetsu.modals.RoleAgreementModal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ModalListener extends Listener {
    private final List<ModalInteraction> commands = new ArrayList<>();

    public ModalListener(Rengetsu rengetsu) {
        super(rengetsu);

        commands.add(new RoleAgreementModal(rengetsu));
    }

    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
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
