package org.snygame.rengetsu.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.AllowedMentions;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.util.DiceRoll;
import org.snygame.rengetsu.util.math.Interpreter;
import org.snygame.rengetsu.util.math.Type;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrepareCommand extends SlashCommand {
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
                .or(() -> event.getOption("list").map(option -> subList(event, option)))
                .or(() -> event.getOption("show").map(option -> subShow(event, option)))
                .or(() -> event.getOption("borrow").map(option -> subBorrow(event, option)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subCreate(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String key = option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        long userId = event.getInteraction().getUser().getId().asLong();
        boolean hasData;
        try {
            hasData = prepData.hasPrepData(userId, key);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
        if (hasData) {
            return event.reply("Prepared effect with key `%s` already exists.".formatted(key))
                    .withComponents(ActionRow.of(
                            Button.primary("prep:edit_instead:%s".formatted(key), "Edit instead")
                    )).withEphemeral(true);
        }

        return event.presentModal("Preparing", "prep:init:%s".formatted(key), List.of(
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

        PrepData.Data data;
        try {
            data = prepData.getPrepData(userId, key);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        if (data == null) {
            return event.reply("Prepared effect with key `%s` does not exists.".formatted(key))
                    .withComponents(ActionRow.of(
                            Button.primary("prep:create_instead:%s".formatted(key), "Create instead")
                    )).withEphemeral(true);
        }

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

        PrepData.Data data;
        try {
            data = prepData.getPrepData(userId, key);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        if (data == null) {
            return event.reply("Prepared effect with key `%s` does not exists.".formatted(key))
                    .withComponents(ActionRow.of(
                            Button.primary("prep:create_instead:%s".formatted(key), "Create instead")
                    )).withEphemeral(true);
        }

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
        boolean deleted;
        try {
            deleted = prepData.deletePrepData(userId, key);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        if (!deleted) {
            return event.reply("**[Error]** Prepared effect with key `%s` does not exists".formatted(key)).withEphemeral(true);
        }

        return event.reply("Deleted prepared effect with key `%s`.".formatted(key)).withEphemeral(true);
    }

    private Mono<Void> subList(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        Long userId = option.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake).map(Snowflake::asLong).orElse(null);

        if (userId == null) {
            List<PrepData.NameData> datas;
            try {
                datas = prepData.listPrepNames(event.getInteraction().getUser().getId().asLong());
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }

            if (datas.isEmpty()) {
                return event.reply("You do not have any prepared effects.");
            }

            return event.reply("Your prepared effects:\n" +
                    datas.stream().map(data -> "`%s`: %s".formatted(data.key(), data.name())).collect(Collectors.joining("\n")));
        } else {
            List<PrepData.NameData> datas;
            try {
                datas = prepData.listPrepNames(userId);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }

            if (datas.isEmpty()) {
                return event.reply("<@%d> does not have any prepared effects.".formatted(userId))
                        .withAllowedMentions(AllowedMentions.suppressAll());
            }

            return event.reply("<@%d>'s prepared effects:\n".formatted(userId) +
                            datas.stream().map(data -> "`%s`: %s".formatted(data.key(), data.name())).collect(Collectors.joining("\n")))
                    .withAllowedMentions(AllowedMentions.suppressAll());
        }
    }

    private Mono<Void> subShow(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        return Mono.justOrEmpty(event.getOptions().get(0).getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)).flatMap(key -> {
            long userId = event.getInteraction().getUser().getId().asLong();

            PrepData.Data data;
            try {
                data = prepData.getPrepData(userId, key);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }

            if (data == null) {
                return event.reply("Prepared effect with key `%s` does not exists.".formatted(key))
                        .withComponents(ActionRow.of(
                                Button.primary("prep:create_instead:%s".formatted(key), "Create instead")
                        )).withEphemeral(true);
            }

            return event.reply(PrepData.buildMenu(data).withComponents().withEphemeral(false));
        });
    }

    private Mono<Void> subBorrow(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        return option.getOption("cast").map(option2 -> subBorrowCast(event, option2))
                .or(() -> option.getOption("show").map(option2 -> subBorrowShow(event, option2)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subBorrowCast(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        long userId = option.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake).map(Snowflake::asLong).orElse(0L);

        String key = option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        PrepData.Data data;
        try {
            data = prepData.getPrepData(userId, key);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        if (data == null) {
            return event.reply("<@%d> does not have a prepared effect with key `%s`.".formatted(userId, key))
                    .withEphemeral(true).withAllowedMentions(AllowedMentions.suppressAll());
        }

        String arguments = option.getOption("arguments")
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

    private Mono<Void> subBorrowShow(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        long userId = option.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake).map(Snowflake::asLong).orElse(0L);

        String key = option.getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        PrepData.Data data;
        try {
            data = prepData.getPrepData(userId, key);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        if (data == null) {
            return event.reply("<@%d> does not have a prepared effect with key `%s`.".formatted(userId, key))
                    .withEphemeral(true).withAllowedMentions(AllowedMentions.suppressAll());
        }

        return event.reply(PrepData.buildMenu(data).withContent("Owner: <@%d>, Key: %s".formatted(userId, key))
                .withAllowedMentions(AllowedMentions.suppressAll()).withComponents().withEphemeral(false));
    }

    private Mono<Void> castEffect(ChatInputInteractionEvent event, PrepData.Data data, String[] args) {
        if (data.params.length != args.length) {
            return event.reply("**[Error]** Expected %d arguments but received %d instead".formatted(data.params.length, args.length))
                    .withEphemeral(true);
        }

        Object[] variables = new Object[data.varCount];
        Type.FixedType[] paramTypes = new Type.FixedType[args.length];

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

        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title(data.name);
        embed.description(data.description);

        if (args.length > 0) {
            embed.addField("Arguments", data.parameterData.stream().map(paramData -> "%s = %s"
                            .formatted(paramData.name(), variables[paramData.result()]))
                    .collect(Collectors.joining(", ")),false);
        }

        embed.addAllFields(data.rolls.stream().map(rollData -> {
            switch (rollData) {
                case PrepData.Data.DiceRollData diceRoll -> {
                    DiceRoll diceroll = DiceRoll.parse(diceRoll.query);
                    if (diceroll.getRepeat() == 1) {
                        DiceRoll.Result result = diceroll.roll();
                        if (diceRoll.variable != null) {
                            variables[diceRoll.result] = BigInteger.valueOf(result.actualSum());
                            return EmbedCreateFields.Field.of(diceRoll.description,
                                    "`%s = %s` %s".formatted(diceRoll.variable, diceRoll.query, result.toString())
                                    , false);
                        }

                        return EmbedCreateFields.Field.of(diceRoll.description,
                                "`%s` %s".formatted(diceRoll.query, result.toString())
                                , false);
                    } else {
                        return EmbedCreateFields.Field.of(diceRoll.description,
                                "`%s`\n%s".formatted(diceRoll.variable == null ? diceRoll.query : "%s = %s"
                                                .formatted(diceRoll.variable, diceRoll.query),
                                        IntStream.range(0, diceroll.getRepeat())
                                                .mapToObj(__ -> {
                                                    DiceRoll.Result result = diceroll.roll();
                                                    if (diceRoll.variable != null) {
                                                        variables[diceRoll.result] = BigInteger.valueOf(result.actualSum());
                                                    }
                                                    return result.toString();
                                                }).collect(Collectors.joining("\n")))
                                , false);
                    }
                }
                case PrepData.Data.CalculationData calculation -> {
                    try {
                        return EmbedCreateFields.Field.of(calculation.description,
                                "`%s` %s".formatted(calculation.query,
                                        Interpreter.interpret(calculation.bytecode, variables)), false);
                    } catch (Exception e) {
                        return EmbedCreateFields.Field.of(calculation.description,
                                "`%s` Error: %s".formatted(calculation.query,
                                        e.getMessage()), false);
                    }
                }
            }
            throw new RuntimeException();
        }).toList());

        return event.reply(builder.addEmbed(embed.build()).build());
    }
}
