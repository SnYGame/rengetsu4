package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.snygame.rengetsu.dicerolls.Diceroll;
import reactor.core.publisher.Mono;

public class DiceCommand implements SlashCommand{
    @Override
    public String getName() {
        return "d";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.deferReply().then(delayedHandle(event));
    }

    private Mono<Void> delayedHandle(ChatInputInteractionEvent event) {
        boolean ephemeral = event.getOption("hide")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);

        StringBuilder response = new StringBuilder();
        for (String query: event.getOption("queries").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).get().split(";")) {
            Diceroll diceroll = Diceroll.parse(query.strip());
            for (int i = 0; i < diceroll.getRepeat(); i++) {
                response.append(diceroll.roll()).append('\n');
            }
        }

        return event.createFollowup(response.toString()).withEphemeral(ephemeral).then();
    }
}
