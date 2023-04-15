package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.antlr.v4.runtime.*;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.parser.RengCalcLexer;
import org.snygame.rengetsu.parser.RengCalcParser;
import org.snygame.rengetsu.util.functions.StringSplitPredicate;
import org.snygame.rengetsu.util.math.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MathCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
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

        List<String> queries = event.getOption("input").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> str.split(";")).stream().flatMap(Arrays::stream)
                .filter(str -> !str.isBlank()).map(String::strip).toList();

        if (queries.isEmpty()) {
            return event.reply("**[Error]** No input").withEphemeral(true);
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
        return Mono.just(parseTrees).flatMap(pts -> {
            ASTGenerator astGenerator = new ASTGenerator();
            ArrayList<ASTNode> asts = new ArrayList<>();
            TypeChecker typeChecker = new TypeChecker();

            for (RengCalcParser.CalculationContext pt : pts) {
                ASTNode ast = astGenerator.visit(pt);

                try {
                    ast.accept(typeChecker);
                } catch (Exception e) {
                    return event.createFollowup("`%s` Type Error: %s\n".formatted(pt.getText().substring(0, pt.getText().length() - 5),
                            e.getMessage())).withEphemeral(true);
                }

                asts.add(ast);
            }
            BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();
            List<byte[]> bytecodes = asts.stream().map(bytecodeGenerator::generate).toList();
            Object[] variables = new Object[bytecodeGenerator.getVarCount()];

            return Flux.fromStream(IntStream.range(0, pts.size()).mapToObj(i -> {
                RengCalcParser.CalculationContext pt = pts.get(i);
                try {
                    String result = Interpreter.interpret(bytecodes.get(i), variables);
                    return "`%s` %s\n".formatted(shorten(pt.getText().substring(0, pt.getText().length() - 5), 50), result);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "`%s` Error: %s\n".formatted(shorten(pt.getText().substring(0, pt.getText().length() - 5), 50), e.getMessage());
                }
            })).subscribeOn(Schedulers.boundedElastic()).windowUntil(StringSplitPredicate.get(2000), true)
                    .flatMap(stringFlux -> stringFlux.collect(Collectors.joining()))
                    .map(event::createFollowup).flatMap(mono -> mono.withEphemeral(ephemeral)).then();
        }).then().onErrorResume(Exception.class, e ->
                        event.createFollowup("**[Error]** An uncaught exception has occurred. Please notify the bot manager.\n%s".formatted(e)).withEphemeral(true).then());
    }

    private String shorten(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit - 1) + "\u2026" : text;
    }
}
