package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.commands.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class SlashCommandListener extends Listener {
    //An array list of classes that implement the SlashCommand interface
    private final List<SlashCommand> commands = new ArrayList<>();

    public SlashCommandListener(Rengetsu rengetsu) {
        super(rengetsu);

        commands.add(new DiceCommand(rengetsu));
        commands.add(new HereCommand(rengetsu));
        commands.add(new MathCommand(rengetsu));
        commands.add(new SaltCommand(rengetsu));
        commands.add(new TimerCommand(rengetsu));
        commands.add(new RoleCommand(rengetsu));
        commands.add(new RequestRoleCommand(rengetsu));
        commands.add(new SettingsCommand(rengetsu));
        commands.add(new PrepareCommand(rengetsu));
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name of the command this event is for
                .filter(command -> command.getName().equals(event.getCommandName()))
                // Get the first (and only) item in the flux that matches our filter
                .next()
                //have our command class handle all the logic related to its specific command.
                .flatMap(command -> command.handle(event));
    }
}
