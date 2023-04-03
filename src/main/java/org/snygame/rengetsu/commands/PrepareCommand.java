package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.util.Diceroll;
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
        return event.getOption("create").map(__ -> subCreate(event))
                .or(() -> event.getOption("edit").map(__ -> subEdit(event)))
                .or(() -> event.getOption("cast").map(__ -> subCast(event)))
                .or(() -> event.getOption("delete").map(__ -> subDelete(event)))
                .or(() -> event.getOption("list").map(__ -> subList(event)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subCreate(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        return Mono.justOrEmpty(event.getOptions().get(0).getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)).flatMap(key -> {
            long userId = event.getInteraction().getUser().getId().asLong();
            boolean hasData;
            try {
                hasData = prepData.hasPrepData(userId, key);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
            if (hasData) {
                return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .content("Prepared effect with key `%s` already exists.".formatted(key))
                                .addComponent(ActionRow.of(
                                        Button.primary("prep:%d:%s:edit_instead".formatted(userId, key), "Edit instead")
                                ))
                        .build());
            }

            return event.presentModal("Preparing", "prep:%d:%s:init".formatted(userId, key), List.of(
                    ActionRow.of(
                            TextInput.small("name", "Name", 0, 100)
                                    .required(true)
                    ),
                    ActionRow.of(
                            TextInput.paragraph("description", "Effect description",
                                    0, 2000).required(false)
                    )
            ));
        });
    }

    private Mono<Void> subEdit(ChatInputInteractionEvent event) {
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
                return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .content("Prepared effect with key `%s` does not exists.".formatted(key))
                        .addComponent(ActionRow.of(
                                Button.primary("prep:%d:%s:create_instead".formatted(userId, key), "Create instead")
                        ))
                        .build());
            }

            if (!prepData.putTempData(data)) {
                return event.reply("**[Error]** A prepared effect with that key is currently being edited").withEphemeral(true);
            }
            return event.reply(PrepData.buildMenu(data));
        });
    }

    private Mono<Void> subCast(ChatInputInteractionEvent event) {
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
                return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .content("Prepared effect with key `%s` does not exists.".formatted(key))
                        .addComponent(ActionRow.of(
                                Button.primary("prep:%d:%s:create_instead".formatted(userId, key), "Create instead")
                        ))
                        .build());
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

            embed.addAllFields(data.dicerolls.stream().map(rollData -> {
                switch (rollData) {
                    case PrepData.Data.DicerollData dicerollData -> {
                        Diceroll diceroll = Diceroll.parse(dicerollData.query);
                        if (diceroll.getRepeat() == 1) {
                            Diceroll.Result result = diceroll.roll();
                            if (dicerollData.variable != null) {
                                variables[dicerollData.result] = BigInteger.valueOf(result.sum());
                                return EmbedCreateFields.Field.of(dicerollData.description,
                                        "`%s = %s` %s".formatted(dicerollData.variable, dicerollData.query, result.toString())
                                        , false);
                            }

                            return EmbedCreateFields.Field.of(dicerollData.description,
                                    "`%s` %s".formatted(dicerollData.query, result.toString())
                                    , false);
                        } else {
                            return EmbedCreateFields.Field.of(dicerollData.description,
                                    "`%s`\n%s".formatted(dicerollData.variable == null ? dicerollData.query : "%s = %s"
                                                    .formatted(dicerollData.variable, dicerollData.query),
                                            IntStream.range(0, diceroll.getRepeat())
                                            .mapToObj(__ -> {
                                                Diceroll.Result result = diceroll.roll();
                                                if (dicerollData.variable != null) {
                                                    variables[dicerollData.result] = BigInteger.valueOf(result.sum());
                                                }
                                                return result.toString();
                                            }).collect(Collectors.joining("\n")))
                                    , false);
                        }
                    }
                    case PrepData.Data.CalculationData calculationData -> {
                        try {
                            return EmbedCreateFields.Field.of(calculationData.description,
                                    "`%s` %s".formatted(calculationData.query,
                                            Interpreter.interpret(calculationData.bytecode, variables)), false);
                        } catch (Exception e) {
                            return EmbedCreateFields.Field.of(calculationData.description,
                                    "`%s` Error: %s".formatted(calculationData.query,
                                            e.getMessage()), false);
                        }
                    }
                }
                throw new RuntimeException();
            }).toList());

            return event.reply(builder.addEmbed(embed.build()).build());
        });
    }

    private Mono<Void> subDelete(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        return Mono.justOrEmpty(event.getOptions().get(0).getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)).flatMap(key -> {
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

            return event.reply("Deleted prepared effect with key `%s`.".formatted(key));
        });
    }

    private Mono<Void> subList(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        return Mono.just(event.getInteraction().getUser().getId().asLong()).flatMap(id -> {
            List<PrepData.NameData> datas;
            try {
                datas = prepData.listPrepNames(id);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }

            if (datas.isEmpty()) {
                return event.reply("You do not have any prepared effects.");
            }

            return event.reply("Your prepared effects:\n" + datas.stream().map(data -> "`%s`: %s".formatted(data.key(), data.name())).collect(Collectors.joining("\n")));
        });
    }
}
