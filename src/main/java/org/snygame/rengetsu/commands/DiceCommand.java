package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.DiceRoll;
import org.snygame.rengetsu.util.functions.MapFirstElse;
import org.snygame.rengetsu.util.functions.StringSplitPredicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public DiceCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "dice";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        boolean ephemeral = event.getOption("hide")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);

        List<DiceRoll> diceRolls = event.getOption("input").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> str.split(";")).stream().flatMap(Arrays::stream)
                .filter(str -> !str.isBlank()).map(String::strip).map(DiceRoll::parse)
                .toList();

        if (diceRolls.isEmpty()) {
            return event.reply("**[Error]** No input").withEphemeral(true);
        }

        List<String> errors = diceRolls.stream().filter(DiceRoll::hasError)
                .map(diceRoll -> "`%s` **[Error]** %s\n".formatted(diceRoll.shortRepr(), diceRoll.getError()))
                .toList();

        if (!errors.isEmpty()) {
            return Flux.fromIterable(errors).windowUntil(StringSplitPredicate.get(2000), true)
                    .flatMap(stringFlux -> stringFlux.collect(Collectors.joining())).index()
                    .flatMap(MapFirstElse.get(msg -> event.reply(msg).withEphemeral(true),
                            msg -> event.createFollowup(msg).withEphemeral(true))).then();
        }

        if (diceRolls.stream().mapToInt(DiceRoll::getRepeat).sum() > DiceRoll.MAX_ROLLS) {
            return event.reply("**[Error]** Max rolls is %d".formatted(DiceRoll.MAX_ROLLS)).withEphemeral(true);
        } else {
            return event.deferReply().withEphemeral(ephemeral)
                    .then(delayedHandle(event, ephemeral, diceRolls));
        }
    }

    private Mono<Void> delayedHandle(ChatInputInteractionEvent event, boolean ephemeral, List<DiceRoll> diceRolls) {
        return Flux.fromStream(diceRolls.stream().flatMap(diceRoll -> IntStream.range(0, diceRoll.getRepeat()).mapToObj(
                        i -> "`%s%s` %s\n".formatted(diceRoll.shortRepr(),
                                diceRoll.getRepeat() > 1 ? "(%d)".formatted(i + 1) : "",
                                diceRoll.roll())
                ))).subscribeOn(Schedulers.boundedElastic())
                .windowUntil(StringSplitPredicate.get(2000), true)
                .flatMap(stringFlux -> stringFlux.collect(Collectors.joining()))
                .map(event::createFollowup).flatMap(mono -> mono.withEphemeral(ephemeral)).then()
                .onErrorResume(Exception.class, e -> {
                    Rengetsu.getLOGGER().error("Uncaught exception in command", e);
                    return event.createFollowup("**[Error]** An uncaught exception has occurred. Please notify the bot manager.\n%s".formatted(e)).withEphemeral(true).then();
                });
    }
}
