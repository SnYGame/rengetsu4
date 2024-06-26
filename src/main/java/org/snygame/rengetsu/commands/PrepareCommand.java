package org.snygame.rengetsu.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.DiceRoll;
import org.snygame.rengetsu.util.math.Interpreter;
import org.snygame.rengetsu.util.math.Type;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrepareCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public PrepareCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "prepare";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.getOption("create").map(option -> subCreate(event, option))
                .or(() -> event.getOption("edit").map(option -> subEdit(event, option)))
                .or(() -> event.getOption("cast").map(option -> subCast(event, option)))
                .or(() -> event.getOption("delete").map(option -> subDelete(event, option)))
                .or(() -> event.getOption("loaded").map(option -> subLoaded(event, option)))
                .or(() -> event.getOption("show").map(option -> subShow(event, option)))
                .or(() -> event.getOption("list").map(option -> subList(event, option)))
                .or(() -> event.getOption("namespace").map(option -> subNamespace(event, option)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subCreate(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String key = option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        String namespace = option.getOption("namespace")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal = prepData.validateCreate(userId, namespace, key);
        if (retVal != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(namespace, key)).withEphemeral(true);
        }

        return event.presentModal("Preparing", "prep:init:%s:%s".formatted(namespace, key), List.of(
                ActionRow.of(
                        TextInput.small("name", "Name", 0, 100)
                                .required(true)
                ),
                ActionRow.of(
                        TextInput.paragraph("description", "Effect description",
                                0, 2000).required(false)
                )
        ));
    }

    private Mono<Void> subEdit(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String key = option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal;
        PrepData.QueryResult<PrepData.Data> result = prepData.getLoadedPrepData(userId, key);
        if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(null, key)).withEphemeral(true);
        }
        PrepData.Data data = result.item();

        prepData.putTempData(data);
        return event.reply(PrepData.buildMenu(data));
    }

    private Mono<Void> subCast(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String key = option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal;
        PrepData.QueryResult<PrepData.Data> result = prepData.getLoadedOrImportedPrepData(userId, key);
        if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(null, key)).withEphemeral(true);
        }
        PrepData.Data data = result.item();

        String arguments = event.getOptions().get(0).getOption("arguments")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        String[] args;
        if (arguments.isBlank()) {
            args = new String[0];
        } else {
            args = arguments.split(",");
        }

        return castEffect(event, data, args);
    }

    private Mono<Void> subDelete(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String key = option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal = prepData.deleteLoadedPrepData(userId, key);
        if (retVal != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(null, key)).withEphemeral(true);
        }

        return event.reply("Deleted prepared effect with key `%s`.".formatted(key)).withEphemeral(true);
    }

    private Mono<Void> subLoaded(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        PrepData.ReturnValue retVal;
        PrepData.QueryResult<List<PrepData.NameData>> result = prepData.listLoadedPrepNames(event.getInteraction().getUser().getId().asLong());
        if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(null, null)).withEphemeral(true);
        }
        List<PrepData.NameData> datas = result.item();

        if (datas.isEmpty()) {
            return event.reply("You do not have any loaded prepared effects.");
        }

        return event.reply("Your loaded prepared effects:\n" +
                datas.stream().map(data -> "`[%s] %s`: %s".formatted(data.namespace(), data.key(), data.name())).collect(Collectors.joining("\n")));
    }

    private Mono<Void> subShow(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        return Mono.justOrEmpty(option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)).flatMap(key -> {
            long userId = event.getInteraction().getUser().getId().asLong();

            PrepData.ReturnValue retVal;
            PrepData.QueryResult<PrepData.Data> result = prepData.getLoadedPrepData(userId, key);
            if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
                return event.reply("**[Error]** " + retVal.format(null, key)).withEphemeral(true);
            }
            PrepData.Data data = result.item();

            return event.reply(PrepData.buildMenu(data).withComponents().withEphemeral(false));
        });
    }

    private Mono<Void> subList(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        Long userId = option.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake).map(Snowflake::asLong).orElse(null);

        return event.reply(prepData.getPrepList(userId == null ? event.getInteraction().getUser().getId().asLong() : userId,
                ""));
    }

    private Mono<Void> castEffect(ChatInputInteractionEvent event, PrepData.Data data, String[] args) {
        if (data.params.length < args.length) {
            return event.reply("**[Error]** Expected %d arguments but received %d instead".formatted(data.params.length, args.length))
                    .withEphemeral(true);
        }

        Object[] variables = new Object[data.varCount];
        Type.FixedType[] paramTypes = new Type.FixedType[data.params.length];

        for (int i = 0; i < args.length; i++) {
            PrepData.Data.ParameterData paramData = data.parameterData.get(i);
            String arg = args[i].strip();
            switch (paramData.type()) {
                case VAR, ANY -> {
                    if (arg.equalsIgnoreCase("true")) {
                        variables[paramData.result()] = Boolean.TRUE;
                        paramTypes[i] = Type.FixedType.BOOL;
                    } else if (arg.equalsIgnoreCase("false")) {
                        variables[paramData.result()] = Boolean.FALSE;
                        paramTypes[i] = Type.FixedType.BOOL;
                    } else {
                        try {
                            variables[paramData.result()] = new BigDecimal(arg);
                            paramTypes[i] = Type.FixedType.NUM;
                        } catch (NumberFormatException e) {
                            return event.reply("**[Error]** Argument %d (%s) expected to be of type NUM or BOOL but received %s instead"
                                            .formatted(i, paramData.name(), arg))
                                    .withEphemeral(true);
                        }
                    }

                    if (paramData.type() == Type.FixedType.VAR && paramData.ofVarType() > i) {
                        paramTypes[paramData.ofVarType()] = paramTypes[i];
                    }
                }
                case BOOL -> {
                    paramTypes[i] = Type.FixedType.BOOL;
                    if (arg.equalsIgnoreCase("true")) {
                        variables[paramData.result()] = Boolean.TRUE;
                    } else if (arg.equalsIgnoreCase("false")) {
                        variables[paramData.result()] = Boolean.FALSE;
                    } else {
                        return event.reply("**[Error]** Argument %d (%s) expected to be of type BOOL but received %s instead"
                                        .formatted(i, paramData.name(), arg))
                                .withEphemeral(true);
                    }
                }
                case NUM -> {
                    try {
                        paramTypes[i] = Type.FixedType.NUM;
                        variables[paramData.result()] = new BigDecimal(arg);
                    } catch (NumberFormatException e) {
                        return event.reply("**[Error]** Argument %d (%s) expected to be of type NUM but received %s instead"
                                        .formatted(i, paramData.name(), arg))
                                .withEphemeral(true);
                    }
                }
            }
        }

        for (int i = 0; i < args.length; i++) {
            PrepData.Data.ParameterData paramData = data.parameterData.get(i);
            if (paramData.type() == Type.FixedType.VAR && paramTypes[i] != paramTypes[paramData.ofVarType()]) {
                byte otherIndex = paramData.ofVarType();
                String otherName = data.parameterData.get(otherIndex).name();
                return event.reply("**[Error]** Argument %d (%s) expected to be of the same type as argument %d (%s), but %s = %s and %s = %s"
                                .formatted(i, paramData.name(), otherIndex, otherName, paramData.name(), paramTypes[i], otherName, paramTypes[otherIndex]))
                        .withEphemeral(true);
            }
        }

        // Insert default arguments
        for (int i = args.length; i < data.params.length; i++) {
            PrepData.Data.ParameterData paramData = data.parameterData.get(i);

            switch (paramData.type()) {
                case VAR -> {
                    byte other = paramData.ofVarType();
                    // If the matching argument has already been defined, copy its type
                    if (paramTypes[other] == Type.FixedType.NUM) {
                        variables[paramData.result()] = BigDecimal.ZERO;
                        paramTypes[i] = Type.FixedType.NUM;
                    } else if (paramTypes[other] == Type.FixedType.BOOL) {
                        variables[paramData.result()] = Boolean.FALSE;
                        paramTypes[i] = Type.FixedType.BOOL;
                    } else {
                        // Otherwise, default to number and propagate the type
                        variables[paramData.result()] = BigDecimal.ZERO;
                        paramTypes[other] = paramTypes[i] = Type.FixedType.NUM;
                    }
                }
                case ANY -> {
                    // If type has been predetermined due to a prior matching type, use it
                    if (paramTypes[i] == Type.FixedType.NUM) {
                        variables[paramData.result()] = BigDecimal.ZERO;
                    } else if (paramTypes[i] == Type.FixedType.BOOL) {
                        variables[paramData.result()] = Boolean.FALSE;
                    } else {
                        // Otherwise, default to number
                        variables[paramData.result()] = BigDecimal.ZERO;
                        paramTypes[i] = Type.FixedType.NUM;
                    }
                }
                case NUM -> {
                    variables[paramData.result()] = BigDecimal.ZERO;
                    paramTypes[i] = Type.FixedType.NUM;
                }
                case BOOL -> {
                    variables[paramData.result()] = Boolean.FALSE;
                    paramTypes[i] = Type.FixedType.BOOL;
                }
            }
        }

        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title(data.name);
        embed.description(data.description);

        if (data.params.length > 0) {
            embed.addField("Arguments", data.parameterData.stream().map(paramData -> "%s = %s"
                            .formatted(paramData.name(), variables[paramData.result()]))
                    .collect(Collectors.joining(", ")),false);
        }

        String rollDesc = "";
        StringJoiner joiner = new StringJoiner("\n");
        for (PrepData.Data.RollData rollData: data.rolls) {
            if (rollData.description != null) {
                if (joiner.length() > 0) {
                    embed.addField(rollDesc, joiner.toString(), false);
                    joiner = new StringJoiner("\n");
                }
                rollDesc = rollData.description;
            }
            joiner.add(rollValue(variables, rollData));
        }

        if (joiner.length() > 0) {
            embed.addField(rollDesc, joiner.toString(), false);
        }

        return event.reply(builder.addEmbed(embed.build()).build());
    }

    private String rollValue(Object[] variables, PrepData.Data.RollData rollData) {
        switch (rollData) {
            case PrepData.Data.DiceRollData diceRoll -> {
                DiceRoll diceroll = DiceRoll.parse(diceRoll.query);
                if (diceroll.getRepeat() == 1) {
                    DiceRoll.Result result = diceroll.roll();
                    if (diceRoll.variable != null) {
                        variables[diceRoll.result] = BigInteger.valueOf(result.actualSum());
                        return "`%s=%s` %s".formatted(diceRoll.variable, diceRoll.query, result.toString());
                    }

                    return "`%s` %s".formatted(diceRoll.query, result.toString());
                } else {
                    return "`%s`\n%s".formatted(diceRoll.variable == null ? diceRoll.query : "%s = %s"
                                    .formatted(diceRoll.variable, diceRoll.query),
                            Stream.generate(() -> {
                                DiceRoll.Result result = diceroll.roll();
                                if (diceRoll.variable != null) {
                                    variables[diceRoll.result] = BigInteger.valueOf(result.actualSum());
                                }
                                return result.toString();
                            }).limit(diceroll.getRepeat()).collect(Collectors.joining("\n")));
                }
            }
            case PrepData.Data.CalculationData calculation -> {
                try {
                    return "`%s` %s".formatted(calculation.query, Interpreter.interpret(calculation.bytecode, variables));
                } catch (Exception e) {
                    return "`%s` Error: %s".formatted(calculation.query, e.getMessage());
                }
            }
        }
        throw new RuntimeException();
    }

    private Mono<Void> subNamespace(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        return option.getOption("create").map(option2 -> subNamespaceCreate(event, option2))
                .or(() -> option.getOption("delete").map(option2 -> subNamespaceDelete(event, option2)))
                .or(() -> option.getOption("rename").map(option2 -> subNamespaceRename(event, option2)))
                .or(() -> option.getOption("load").map(option2 -> subNamespaceLoad(event, option2)))
                .or(() -> option.getOption("unload").map(option2 -> subNamespaceUnload(event, option2)))
                .or(() -> option.getOption("import").map(option2 -> subNamespaceImport(event, option2)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subNamespaceCreate(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String name = option.getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        if (!name.chars().allMatch(i -> Character.isAlphabetic(i) || Character.isDigit(i) || i == '_')) {
            return event.reply("**[Error]** Namespace may only has alphanumeric or underscore characters.").withEphemeral(true);
        }

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal = prepData.createNamespace(userId, name);
        if (retVal != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(name, null)).withEphemeral(true);
        }

        return event.reply("Created namespace \"%s\".".formatted(name)).withEphemeral(true);
    }

    private Mono<Void> subNamespaceDelete(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String name = option.getOption("namespace")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal = prepData.deleteNamespace(userId, name);
        if (retVal != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(name, null)).withEphemeral(true);
        }

        return event.reply("Deleted namespace \"%s\".".formatted(name)).withEphemeral(true);
    }

    private Mono<Void> subNamespaceRename(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String name = option.getOption("namespace")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        String newName = option.getOption("new")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        if (!newName.chars().allMatch(i -> Character.isAlphabetic(i) || Character.isDigit(i) || i == '_')) {
            return event.reply("**[Error]** Namespace may only has alphanumeric or underscore characters").withEphemeral(true);
        }

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal;
        PrepData.QueryResult<Void> result = prepData.renameNamespace(userId, name, newName);
        if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(result.arg(), null)).withEphemeral(true);
        }

        return event.reply("Renamed namespace \"%s\" to \"%s\".".formatted(name, newName)).withEphemeral(true);
    }

    private Mono<Void> subNamespaceLoad(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String name = option.getOption("namespace")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal;
        PrepData.QueryResult<Void> result = prepData.setNamespaceLoaded(userId, name, true);
        if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(name, result.arg())).withEphemeral(true);
        }

        return event.reply("Namespace \"%s\" has been loaded.".formatted(name)).withEphemeral(true);
    }

    private Mono<Void> subNamespaceUnload(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String name = option.getOption("namespace")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal;
        PrepData.QueryResult<Void> result = prepData.setNamespaceLoaded(userId, name, false);
        if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(name, result.arg())).withEphemeral(true);
        }

        return event.reply("Namespace \"%s\" has been unloaded.".formatted(name)).withEphemeral(true);
    }

    private Mono<Void> subNamespaceImport(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String name = option.getOption("namespace")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        String reference = option.getOption("reference")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        Long borrowId = option.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake).map(Snowflake::asLong).orElse(0L);

        long userId = event.getInteraction().getUser().getId().asLong();

        PrepData.ReturnValue retVal;
        PrepData.QueryResult<Void> result = prepData.importNamespace(userId, reference, borrowId, name);
        if ((retVal = result.retVal()) != PrepData.ReturnValue.SUCCESS) {
            return event.reply("**[Error]** " + retVal.format(result.arg(), null)).withEphemeral(true);
        }

        return event.reply("Namespace \"%s\" has been imported as \"%s\".".formatted(name, reference)).withEphemeral(true);
    }
}
