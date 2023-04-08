package org.snygame.rengetsu.buttons;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;

public class RoleSetButton extends ButtonInteraction {
    public RoleSetButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "role";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        RoleData roleData = databaseManager.getRoleData();
        return Mono.justOrEmpty(event.getInteraction().getMember()).flatMap(PartialMember::getBasePermissions)
                .map(permissions -> permissions.and(PermissionSet.of(Permission.MANAGE_ROLES))).flatMap(permissions -> {
                    if (permissions.isEmpty()) {
                        return event.reply("**[Error]** You do not have permission to do that").withEphemeral(true);
                    }

                    String[] args = event.getCustomId().split(":");

                    RoleData.Data data = roleData.getTempData(Integer.parseInt(args[2]));
                    if (data == null) {
                        return event.edit("**[Error]** Cached data is missing, run the command again")
                                .withComponents().withEmbeds().withEphemeral(true);
                    }

                    switch (args[1]) {
                        case "add_join" -> data.addJoin = Boolean.parseBoolean(args[3]);
                        case "add_inactive" -> data.addInactive = Boolean.parseBoolean(args[3]);
                        case "requestable" -> data.requestable = Boolean.parseBoolean(args[3]) ? new RoleData.Data.Requestable(false, null) : null;
                        case "temp" -> {
                            if (data.requestable != null) {
                                data.requestable.temp = Boolean.parseBoolean(args[3]);
                            }
                        }
                        case "agreement" -> {
                            return event.presentModal().withCustomId("role:agreement:%d".formatted(data.uid))
                                    .withTitle("Request agreement").withComponents(
                                            ActionRow.of(TextInput.paragraph("agreement", "Agreement (leave blank to remove)", 0, 1500)
                                                    .prefilled(data.requestable != null && data.requestable.agreement != null ?
                                                            data.requestable.agreement : "").required(false))
                                    );
                        }
                        case "on_remove", "on_add" -> {
                            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                    .content(args[1].equals("on_remove") ? "Select roles to add when this role is removed." :
                                            "Select roles to remove when this role is added.")
                                    .embeds(Collections.emptyList())
                                    .addComponent(ActionRow.of(
                                            SelectMenu.ofRole("role:%s:%d".formatted(args[1], data.uid))
                                                    .withMaxValues(25)
                                                    .withPlaceholder("Select roles to %s".formatted(args[1].equals("on_remove") ? "add" : "remove"))
                                    )).addComponent(ActionRow.of(
                                            Button.danger("role:%s_none:%d".formatted(args[1], data.uid),
                                                    "Clear"),
                                            Button.danger("role:cancel_menu:%d".formatted(data.uid), "Cancel")))
                                    .build());
                        }
                        case "on_remove_none" -> {
                            data.addWhenRemoved.clear();
                        }
                        case "on_add_none" -> {
                            data.removeWhenAdded.clear();
                        }
                        case "cancel_menu" -> {}
                        case "save" -> {
                            try {
                                roleData.saveRoleData(data);
                                roleData.removeTempData(data);
                                return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Data saved for")
                                                .description("<@&%d>".formatted(data.roleId)).build()
                                        ).components(Collections.emptyList()).build());
                            } catch (SQLException e) {
                                Rengetsu.getLOGGER().error("SQL Error", e);
                                return event.reply("**[Error]** Database error").withEphemeral(true);
                            }
                        }
                        case "clear" -> {
                            try {
                                roleData.deleteRoleData(data.roleId, data.serverId);
                                roleData.removeTempData(data);
                                return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Cleared data for")
                                                .description("<@&%d>".formatted(data.roleId)).build()
                                        ).components(Collections.emptyList()).build());
                            } catch (SQLException e) {
                                Rengetsu.getLOGGER().error("SQL Error", e);
                                return event.reply("**[Error]** Database error").withEphemeral(true);
                            }
                        }
                    }

                    return event.edit(RoleData.buildMenu(data));
                });
    }
}
