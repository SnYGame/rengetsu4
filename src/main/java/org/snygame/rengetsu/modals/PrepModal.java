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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PrepModal extends ModalInteraction {
    private static final Pattern VAR_RE = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

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
            case "params" -> {
                return handleParams(event);
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

        if (!prepData.putTempData(data)) {
            return event.reply("**[Error]** A prepared effect with that key is currently being edited").withEphemeral(true);
        }
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
        String variable = event.getComponents().get(2).getData().components().get().get(0).value().toOptional().orElse(null);

        Diceroll diceroll = Diceroll.parse(query);
        if (diceroll.hasError()) {
            return event.reply("**[Error]** %s".formatted(diceroll.getError())).withEphemeral(true);
        }

        if (variable != null && !VAR_RE.matcher(variable).matches()) {
            return event.reply(
                    "**[Error]** Variable name must start with a letter or underscore and only contain letters, numbers, or underscore")
                    .withEphemeral(true);
        }

        data.dicerolls.add(new PrepData.Data.DicerollData(description, diceroll.toString(), variable));
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

        ASTNode ast = new ASTGenerator().visit(pt);
        data.dicerolls.add(new PrepData.Data.CalculationData(description,
                pt.getText().substring(0, pt.getText().length() - 5), ast));
        return event.edit(PrepData.buildMenu(data));
    }

    private Mono<Void> handleParams(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Long.parseLong(args[1]), args[2]);
        if (data == null) {
            return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
        }

        String paramsRaw = event.getComponents().get(0).getData().components().get().get(0).value().toOptional()
                .orElse("");
        if (paramsRaw.isBlank()) {
            data.params = new String[0];
            return event.edit(PrepData.buildMenu(data));
        }
        String[] params = paramsRaw.split(",");
        ArrayList<String> errors = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].strip();
            if (!VAR_RE.matcher(params[i]).matches()) {
                errors.add(params[i]);
            }
        }

        if (!errors.isEmpty()) {
            return event.reply(
                    "**[Error]** Parameters must start with a letter or underscore and only contain letters, numbers, or underscore\nInvalid parameters: %s"
                    .formatted(String.join(", ", errors))).withEphemeral(true);
        }

        if (params.length != Arrays.stream(params).distinct().count()) {
            return event.reply("**[Error]** Cannot have multiple parameters with the same name").withEphemeral(true);
        }

        data.params = params;
        return event.edit(PrepData.buildMenu(data));
    }
}
