package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.commands.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class SlashCommandListener extends Listener {
    //An array list of classes that implement the SlashCommand interface
    private final List<InteractionListener.CommandDelegate<ChatInputInteractionEvent>> commands;

    public SlashCommandListener(Rengetsu rengetsu) {
        super(rengetsu);

        commands = List.of(
                new DiceCommand(rengetsu),
                new HereCommand(rengetsu),
                new MathCommand(rengetsu),
                new SaltCommand(rengetsu),
                new TimerCommand(rengetsu),
                new RoleCommand(rengetsu),
                new RequestRoleCommand(rengetsu),
                new SettingsCommand(rengetsu),
                new PrepareCommand(rengetsu),
                new HelpCommand(rengetsu),
                new AgmCommand(rengetsu)
        );
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name of the command this event is for
                .filter(command -> command.getName().equals(event.getCommandName()))
                // Get the first (and only) item in the flux that matches our filter
                .next()
                //have our command class handle all the logic related to its specific command.
                .flatMap(command -> command.handle(event))
                .onErrorResume(Exception.class, e ->
                        event.reply("**[Error]** An uncaught exception has occurred. Please notify the bot manager.\n%s".formatted(e)).withEphemeral(true));
    }
}
