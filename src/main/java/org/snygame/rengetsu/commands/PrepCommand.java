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
import org.snygame.rengetsu.data.TimerData;
import org.snygame.rengetsu.tasks.TaskManager;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class PrepCommand extends SlashCommand {
    public PrepCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "prep";
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

            prepData.putTempData(data);
            return event.reply(PrepData.buildMenu(data));
        });
    }

    private Mono<Void> subCast(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }

    private Mono<Void> subDelete(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
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
                return event.reply("You have no prepared effects");
            }

            return event.reply("Your prepared effects:\n" + datas.stream().map(data -> "`%s`: %s".formatted(data.key(), data.name())).collect(Collectors.joining("\n")));
        });
    }
}
