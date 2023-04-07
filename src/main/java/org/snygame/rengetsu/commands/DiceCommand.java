package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.util.Diceroll;
import org.snygame.rengetsu.util.functions.MapFirstElse;
import org.snygame.rengetsu.util.functions.StringSplitPredicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceCommand extends SlashCommand {
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

        List<Diceroll> dicerolls = event.getOption("queries").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> str.split(";")).stream().flatMap(Arrays::stream)
                .filter(str -> !str.isBlank()).map(String::strip).map(Diceroll::parse)
                .toList();

        if (dicerolls.isEmpty()) {
            return event.reply("**[Error]** No queries").withEphemeral(true);
        }

        List<String> errors = dicerolls.stream().filter(Diceroll::hasError)
                .map(diceroll -> "`%s` **[Error]** %s\n".formatted(diceroll.shortRepr(), diceroll.getError()))
                .toList();

        if (!errors.isEmpty()) {
            return Flux.fromIterable(errors).windowUntil(StringSplitPredicate.get(2000), true)
                    .flatMap(stringFlux -> stringFlux.collect(Collectors.joining())).index()
                    .flatMap(MapFirstElse.get(msg -> event.reply(msg).withEphemeral(true),
                            msg -> event.createFollowup(msg).withEphemeral(true))).then();
        }

        if (dicerolls.stream().mapToInt(Diceroll::getRepeat).sum() > Diceroll.MAX_ROLLS) {
            return event.reply("**[Error]** Max rolls is %d".formatted(Diceroll.MAX_ROLLS)).withEphemeral(true);
        } else {
            return event.deferReply().withEphemeral(ephemeral)
                    .then(delayedHandle(event, ephemeral, dicerolls));
        }
    }

    private Mono<Void> delayedHandle(ChatInputInteractionEvent event, boolean ephemeral, List<Diceroll> dicerolls) {
        return Flux.fromStream(dicerolls.stream().flatMap(diceroll -> IntStream.range(0, diceroll.getRepeat()).mapToObj(
                        i -> "`%s%s` %s\n".formatted(diceroll.shortRepr(),
                                diceroll.getRepeat() > 1 ? "(%d)".formatted(i + 1) : "",
                                diceroll.roll())
                ))).subscribeOn(Schedulers.boundedElastic())
                .windowUntil(StringSplitPredicate.get(2000), true)
                .flatMap(stringFlux -> stringFlux.collect(Collectors.joining()))
                .map(event::createFollowup).flatMap(mono -> mono.withEphemeral(ephemeral)).then();
    }
}
