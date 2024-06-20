package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.DiceRoll;
import org.snygame.rengetsu.util.math.ASTGenerator;
import org.snygame.rengetsu.util.math.ASTNode;
import org.snygame.rengetsu.util.math.Parser;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PrepModal extends InteractionListener.CommandDelegate<ModalSubmitInteractionEvent> {
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

        switch (args[1]) {
            case "init" -> {
                return handleInit(event, false);
            }
            case "init_instead" -> {
                return handleInit(event, true);
            }
            case "edit" -> {
                return handleEdit(event);
            }
            case "namespace" -> {
                return handleNamespace(event);
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

    private Mono<Void> handleInit(ModalSubmitInteractionEvent event, boolean edit) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.Data data = new PrepData.Data(userId, args[2]);
        data.name = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        data.description = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);
        String namespace = event.getComponents().get(2).getData().components().get().get(0).value().toOptional().orElse("").strip();
        if (namespace.isBlank()) {
            data.namespace = null;
        } else {
            try {
                if (!prepData.getNamespaceExists(userId, namespace)) {
                    return event.reply("**[Error]** Namespace \"%s\" does not exist.".formatted(namespace)).withEphemeral(true);
                }

                data.namespace = namespace;
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        }
        data.editing = false;

        prepData.putTempData(data);
        return edit ? event.edit(PrepData.buildMenu(data)) : event.reply(PrepData.buildMenu(data));
    }

    private Mono<Void> handleEdit(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        data.name = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        data.description = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);

        return event.edit(PrepData.buildMenu(data));
    }

    private Mono<Void> handleNamespace(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.Data data = prepData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        String namespace = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse("").strip();
        if (namespace.isBlank()) {
            data.namespace = null;
        } else {
            try {
                if (!prepData.getNamespaceExists(userId, namespace)) {
                    return event.reply("**[Error]** Namespace \"%s\" does not exist.".formatted(namespace)).withEphemeral(true);
                }

                data.namespace = namespace;
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        }

        return event.edit(PrepData.buildMenu(data));
    }

    private Mono<Void> handleAddRoll(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        String description = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        String queries = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);

        if (description != null && description.isBlank()) {
            description = null;
        }

        List<PrepData.Data.DiceRollData> diceRollData = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String query: queries.split(";|\n")) {
            if (query.isBlank()) {
                continue;
            }

            String variable = null;

            if (query.contains("=")) {
                String[] split = query.split("=", 2);
                variable = split[0].strip();
                query = split[1].strip();
            }

            DiceRoll diceroll = DiceRoll.parse(query);
            if (diceroll.hasError()) {
                errors.add("`%s` %s".formatted(diceroll.shortRepr(), diceroll.getError()));
                continue;
            }

            if (variable != null && !VAR_RE.matcher(variable).matches()) {
                errors.add("`%s` Variable name must start with a letter or underscore and only contain letters, numbers, or underscore".formatted(variable));
                continue;
            }

            diceRollData.add(new PrepData.Data.DiceRollData(description, diceroll.toString(), variable));
            description = null;
        }

        if (!errors.isEmpty()) {
            return event.reply("**[Error]** Syntax error(s)\n" + String.join("\n", errors)).withEphemeral(true);
        }

        data.rolls.addAll(diceRollData);
        return event.edit(PrepData.buildMenu(data));
    }

    private Mono<Void> handleAddCalc(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        String description = event.getComponents().get(0).getData().components().get().get(0).value().toOptional().orElse(null);
        String queries = event.getComponents().get(1).getData().components().get().get(0).value().toOptional().orElse(null);

        if (description != null && description.isBlank()) {
            description = null;
        }

        List<PrepData.Data.CalculationData> calculationData = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String query: queries.split(";|\n")) {
            if (query.isBlank()) {
                continue;
            }

            Parser.ParseTree pt = Parser.parseCalculation(query);

            if (!pt.errors().isEmpty()) {
                errors.add("`%s`\n%s".formatted(Parser.shortenText(pt.parseTree().getText()), String.join("\n", pt.errors())));
                continue;
            }

            ASTNode ast = new ASTGenerator().visit(pt.parseTree());
            calculationData.add(new PrepData.Data.CalculationData(description, Parser.shortenText(pt.parseTree().getText()), ast));
            description = null;
        }

        if (!errors.isEmpty()) {
            return event.reply("**[Error]** Syntax error(s)\n" + String.join("\n", errors)).withEphemeral(true);
        }

        data.rolls.addAll(calculationData);
        return event.edit(PrepData.buildMenu(data));
    }

    private Mono<Void> handleParams(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();
        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
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
