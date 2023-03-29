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
import org.snygame.rengetsu.data.TimerData;
import org.snygame.rengetsu.tasks.TaskManager;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

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
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subCreate(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getOptions().get(0).getOption("key")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)).flatMap(key ->
                        event.presentModal("Preparing", "prep:%d:%s:init".formatted(event.getInteraction()
                                .getUser().getId().asLong(), key), List.of(
                                ActionRow.of(
                                        TextInput.small("name", "Name", 0, 100)
                                                .required(true)
                                ),
                                ActionRow.of(
                                        TextInput.paragraph("description", "Effect description",
                                                0, 2000).required(false)
                                )
                        )));
    }

    private Mono<Void> subEdit(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }

    private Mono<Void> subCast(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }

    private Mono<Void> subDelete(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }
}
