package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.InteractionFollowupCreateMono;
import org.snygame.rengetsu.util.Diceroll;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

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

        List<Diceroll> dicerolls = event.getOption("queries").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> str.split(";")).stream().flatMap(Arrays::stream)
                .map(String::strip).map(Diceroll::parse)
                .toList();

        List<String> errors = new ArrayList<>();
        StringBuilder errorMsg = new StringBuilder();

        dicerolls.stream().filter(Diceroll::hasError)
                .map(diceroll -> "`%s` **[Error]** %s\n".formatted(diceroll.shortRepr(), diceroll.getError()))
                .forEach(msg -> {
                    if (errorMsg.length() + msg.length() > 2000) {
                        errors.add(errorMsg.toString());
                        errorMsg.setLength(0);
                    }
                    errorMsg.append(msg);
                });

        if (!errorMsg.isEmpty()) {
            errors.add(errorMsg.toString());
        }

        if (errors.isEmpty() && dicerolls.stream().mapToInt(Diceroll::getRepeat).sum() > Diceroll.MAX_ROLLS) {
            return event.reply("**[Error]** Max rolls is %d".formatted(Diceroll.MAX_ROLLS)).withEphemeral(true);
        } else if (errors.isEmpty()) {
            return event.deferReply().withEphemeral(ephemeral)
                    .then(delayedHandle(event, ephemeral, dicerolls));
        } else {
            return event.reply(errors.get(0)).withEphemeral(true)
                    .and(Flux.fromIterable(errors).skip(1).flatMap(msg -> event.createFollowup(msg).withEphemeral(true)));
        }
    }

    private Mono<Void> delayedHandle(ChatInputInteractionEvent event, boolean ephemeral, List<Diceroll> dicerolls) {
        StringBuilder sb = new StringBuilder();
        return Flux.fromStream(dicerolls.stream().flatMap(diceroll -> IntStream.range(0, diceroll.getRepeat()).mapToObj(
                i -> "`%s%s` %s\n".formatted(diceroll.shortRepr(),
                        diceroll.getRepeat() > 1 ? "(%d)".formatted(i + 1) : "",
                        diceroll.roll())
        ))).map(msg -> {
            String output = "";
            if (sb.length() + msg.length() > 2000) {
                output = sb.toString();
                sb.setLength(0);
            }
            sb.append(msg);
            return output;
        }).filter(Predicate.not(String::isEmpty))
                .map(event::createFollowup).flatMap(mono -> mono.withEphemeral(ephemeral))
                .then(Mono.just(sb).map(Object::toString).map(event::createFollowup)
                        .flatMap(mono -> mono.withEphemeral(ephemeral))).then();
    }
}
