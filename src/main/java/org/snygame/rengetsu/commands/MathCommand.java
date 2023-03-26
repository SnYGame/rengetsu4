package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.antlr.v4.runtime.*;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.parser.RengCalcLexer;
import org.snygame.rengetsu.parser.RengCalcParser;
import org.snygame.rengetsu.util.functions.StringSplitPredicate;
import org.snygame.rengetsu.util.math.ASTGenerator;
import org.snygame.rengetsu.util.math.ASTNode;
import org.snygame.rengetsu.util.math.BytecodeGenerator;
import org.snygame.rengetsu.util.math.Interpreter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MathCommand extends SlashCommand {
    public MathCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "math";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        boolean ephemeral = event.getOption("hide")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);

        List<String> queries = event.getOption("queries").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> str.split(";")).stream().flatMap(Arrays::stream)
                .map(String::strip).toList();

        if (queries.isEmpty()) {
            return event.reply("**[Error]** No queries").withEphemeral(true);
        }

        List<String> errorsList = new ArrayList<>();
        List<RengCalcParser.CalculationContext> parseTrees = new ArrayList<>();
        for (String query: queries) {
            RengCalcLexer lexer = new RengCalcLexer(CharStreams.fromString(query));
            List<String> errors = new ArrayList<>();
            ANTLRErrorListener listener = new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    errors.add("%d: %s\n".formatted(charPositionInLine, msg));
                }
            };
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);

            RengCalcParser parser = new RengCalcParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(listener);

            RengCalcParser.CalculationContext parseTree = parser.calculation();

            if (!errors.isEmpty()) {
                errorsList.add("`%s`\n".formatted(query));
                errorsList.addAll(errors);
            } else {
                parseTrees.add(parseTree);
            }
        }

        if (!errorsList.isEmpty()) {
            return event.reply(errorsList.stream().reduce("**Syntax Error(s)**\n",
                    (sb, msg) -> sb.length() + msg.length() > 2000 ? sb : sb + msg)).withEphemeral(true);
        }

        try {
            return event.deferReply().withEphemeral(ephemeral).then(delayedHandle(event, ephemeral, parseTrees));
        } catch (RuntimeException e) {
            return event.reply(e.getMessage()).withEphemeral(true);
        }
    }

    private Mono<Void> delayedHandle(ChatInputInteractionEvent event, boolean ephemeral, List<RengCalcParser.CalculationContext> parseTrees) {
        ASTGenerator astGenerator = new ASTGenerator();
        return Flux.fromIterable(parseTrees).map(pt -> {
            try {
                ASTNode ast = pt.accept(astGenerator);
                ast.getType();
                BytecodeGenerator generator = new BytecodeGenerator();
                ast.accept(generator);
                String result = Interpreter.interpret(generator.getBytecode());
                return "`%s` %s\n".formatted(shorten(pt.getText().substring(0, pt.getText().length() - 5), 50), result);
            } catch (Exception e) {
                Rengetsu.getLOGGER().error("Error parsing calculation", e);
                return "`%s` Error: %s\n".formatted(shorten(pt.getText().substring(0, pt.getText().length() - 5), 50), e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).windowUntil(StringSplitPredicate.get(2000), true)
                .flatMap(stringFlux -> stringFlux.collect(Collectors.joining()))
                .map(event::createFollowup).flatMap(mono -> mono.withEphemeral(ephemeral)).then();
    }

    private String shorten(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit - 1) + "\u2026" : text;
    }
}
