package org.snygame.rengetsu.buttons;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.data.RoleData;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class PrepEditButton extends ButtonInteraction {
    public PrepEditButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "prep";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        return Mono.just(event.getInteraction().getUser().getId().asLong()).flatMap(userId -> {
                    String[] args = event.getCustomId().split(":");

                    if (userId != Long.parseLong(args[1])) {
                        return event.reply("**[Error]** You do not have permission to do that").withEphemeral(true);
                    }

                    switch (args[3]) {
                        case "create_instead" -> {
                            return event.presentModal("Preparing", "prep:%d:%s:init".formatted(userId, args[2]), List.of(
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
                        case "edit_instead" -> {
                            PrepData.Data data;
                            String key = args[2];
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
                        }
                    }

                    PrepData.Data data = prepData.getTempData(Long.parseLong(args[1]), args[2]);
                    if (data == null) {
                        return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
                    }

                    switch (args[3]) {
                        case "edit" -> {
                            return event.presentModal("Edit descriptions", "prep:%d:%s:edit".formatted(data.userId,
                                    data.key), List.of(
                                    ActionRow.of(
                                            TextInput.small("name", "Name", 0, 100)
                                                    .required(true).prefilled(data.name)
                                    ),
                                    ActionRow.of(
                                            TextInput.paragraph("description", "Effect description",
                                                    0, 2000).required(false)
                                                    .prefilled(data.description)
                                    )
                            ));
                        }
                        case "add_roll" -> {
                            return event.presentModal("Add diceroll", "prep:%d:%s:add_roll".formatted(data.userId,
                                    data.key), List.of(
                                    ActionRow.of(
                                            TextInput.small("description", "Description", 0, 100)
                                                    .required(true)
                                    ),
                                    ActionRow.of(
                                            TextInput.paragraph("roll", "Diceroll",
                                                            0, 2000).required(true)
                                    )
                            ));
                        }
                        case "add_calc" -> {
                            return event.presentModal("Add calculation", "prep:%d:%s:add_calc".formatted(data.userId,
                                    data.key), List.of(
                                    ActionRow.of(
                                            TextInput.small("description", "Description", 0, 100)
                                                    .required(true)
                                    ),
                                    ActionRow.of(
                                            TextInput.paragraph("calc", "Calculation",
                                                    0, 2000).required(true)
                                    )
                            ));
                        }
                        case "del_roll" -> {
                            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                            .content("Select dicerolls or calculations to remove.")
                                            .addEmbed(EmbedCreateSpec.builder()
                                                    .fields(data.dicerolls.stream().map(rollData ->
                                                            EmbedCreateFields.Field.of(rollData.description,
                                                                    rollData.query, false)).toList())
                                                    .build())
                                            .addComponent(ActionRow.of(
                                                    SelectMenu.of("prep:%d:%s:del_roll".formatted(data.userId,
                                                                    data.key),
                                                            IntStream.range(0, data.dicerolls.size()).mapToObj(i ->
                                                                    SelectMenu.Option.of(data.dicerolls.get(i).description,
                                                                    String.valueOf(i))).toList()
                                                            ).withMaxValues(data.dicerolls.size())
                                            ))
                                            .addComponent(ActionRow.of(
                                                            Button.danger("prep:%d:%s:cancel_menu".formatted(
                                                                    data.userId, data.key), "Cancel")))
                                    .build());
                        }
                        case "cancel_menu" -> {}
                        case "save" -> {
                            try {
                                prepData.savePrepData(data);
                                prepData.removeTempData(data);
                                return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Data saved for %s".formatted(data.name)).build()
                                        ).components(Collections.emptyList()).build());
                            } catch (SQLException e) {
                                Rengetsu.getLOGGER().error("SQL Error", e);
                                return event.reply("**[Error]** Database error").withEphemeral(true);
                            }
                        }
                        case "no_save" -> {
                            prepData.removeTempData(data);
                            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                    .addEmbed(EmbedCreateSpec.builder()
                                            .title("Canceled %s %s".formatted(data.editing ? "changes to" : "creation of"
                                                    , data.name)).build()
                                    ).components(Collections.emptyList()).build());
                        }
                        case "delete" -> {
                            try {
                                prepData.deletePrepData(data.userId, data.key);
                                prepData.removeTempData(data);
                                return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Deleted %s".formatted(data.name)).build()
                                        ).components(Collections.emptyList()).build());
                            } catch (SQLException e) {
                                Rengetsu.getLOGGER().error("SQL Error", e);
                                return event.reply("**[Error]** Database error").withEphemeral(true);
                            }
                        }
                    }

                    return event.edit(PrepData.buildMenu(data));
                });
    }
}
