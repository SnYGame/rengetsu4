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
import reactor.core.publisher.Mono;

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

            InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
            EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
            embed.title(data.name);
            embed.description(data.description);



            embed.fields(data.dicerolls.stream().map(rollData -> {
                switch (rollData) {
                    case PrepData.Data.DicerollData dicerollData -> {
                        Diceroll diceroll = Diceroll.parse(dicerollData.query);
                        if (diceroll.getRepeat() == 1) {
                            return EmbedCreateFields.Field.of(dicerollData.description,
                                    "`%s` %s".formatted(dicerollData.query, diceroll.roll().toString())
                                    , false);
                        } else {
                            return EmbedCreateFields.Field.of(dicerollData.description,
                                    "`%s`\n%s".formatted(dicerollData.query, IntStream.range(0, diceroll.getRepeat())
                                            .mapToObj(__ ->diceroll.roll().toString()).collect(Collectors.joining("\n")))
                                    , false);
                        }
                    }
                    case PrepData.Data.CalculationData calculationData -> {
                        try {
                            return EmbedCreateFields.Field.of(calculationData.description,
                                    "`%s` %s".formatted(calculationData.query,
                                            Interpreter.interpret(calculationData.bytecode)), false);
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
