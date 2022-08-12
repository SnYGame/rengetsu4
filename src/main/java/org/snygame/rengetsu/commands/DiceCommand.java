package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.snygame.rengetsu.dicerolls.Diceroll;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DiceCommand implements SlashCommand {
    @Override
    public String getName() {
        return "dice";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        boolean ephemeral = event.getOption("hide")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);

        return event.deferReply().withEphemeral(ephemeral).then(delayedHandle(event));
    }

    private Mono<Void> delayedHandle(ChatInputInteractionEvent event) {
        return Mono.fromRunnable(() -> {
            StringBuilder response = new StringBuilder();
            all: for (String query : event.getOption("queries").flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString).get().split(";")) {
                Diceroll diceroll = Diceroll.parse(query.strip());
                for (int i = 0; i < diceroll.getRepeat(); i++) {
                    String result = diceroll.roll() + '\n';
                    if (response.length() + result.length() > 2000) {
                        break;
                    }

                    response.append(result);
                }
            }

            if (response.isEmpty()) {
                event.createFollowup("**[Error]** Message is too long to display (>2000 characters)").subscribe();
            } else {
                event.createFollowup(response.toString()).subscribe();
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
