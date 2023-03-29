package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.discordjson.possible.Possible;
import org.antlr.v4.runtime.*;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.parser.RengCalcLexer;
import org.snygame.rengetsu.parser.RengCalcParser;
import org.snygame.rengetsu.util.Diceroll;
import org.snygame.rengetsu.util.math.ASTGenerator;
import org.snygame.rengetsu.util.math.ASTNode;
import org.snygame.rengetsu.util.math.BytecodeGenerator;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PrepModal extends ModalInteraction {
    public PrepModal(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "prep";
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        String[] args = event.getCustomId().split(":");

        switch (args[3]) {
            case "init" -> {
                return handleInit(event);
            }
            case "edit" -> {
                return handleEdit(event);
            }
            case "add_roll" -> {
                return handleAddRoll(event);
            }
            case "add_calc" -> {
                return handleAddCalc(event);
            }
            default -> {
                return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
            }
        }
    }

    private Mono<Void> handleInit(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = new PrepData.Data(Long.parseLong(args[1]), args[2]);
        data.name = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        data.description = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);
        data.editing = false;

        prepData.putTempData(data);
        return event.reply(PrepData.buildMenu(data));
    }

    private Mono<Void> handleEdit(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Long.parseLong(args[1]), args[2]);
        if (data == null) {
            return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
        }

        data.name = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        data.description = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);

        return event.edit(PrepData.buildMenu(data));
    }

    private Mono<Void> handleAddRoll(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Long.parseLong(args[1]), args[2]);
        if (data == null) {
            return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
        }

        String description = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        String query = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);

        Diceroll diceroll = Diceroll.parse(query);
        if (diceroll.hasError()) {
            return event.reply("**[Error]** %s".formatted(diceroll.getError())).withEphemeral(true);
        }

        data.dicerolls.add(new PrepData.Data.DicerollData(description, diceroll.toString()));
        return event.edit(PrepData.buildMenu(data));
    }

    private Mono<Void> handleAddCalc(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Long.parseLong(args[1]), args[2]);
        if (data == null) {
            return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
        }

        String description = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        String query = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);

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

        RengCalcParser.CalculationContext pt = parser.calculation();

        if (!errors.isEmpty()) {
            return event.reply("**Syntax error(s)**\n" + String.join("\n", errors)).withEphemeral(true);
        }

        ASTNode ast = pt.accept(new ASTGenerator());
        ast.getType();
        BytecodeGenerator generator = new BytecodeGenerator();
        ast.accept(generator);
        byte[] bytecode = generator.getBytecode();

        data.dicerolls.add(new PrepData.Data.CalculationData(description, query, bytecode));
        return event.edit(PrepData.buildMenu(data));
    }
}
