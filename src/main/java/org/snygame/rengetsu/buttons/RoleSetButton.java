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

                    RoleData.Data data = roleData.getTempData(Long.parseLong(args[1]), Long.parseLong(args[2]));
                    if (data == null) {
                        return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
                    }

                    switch (args[3]) {
                        case "add_join" -> data.addJoin = Boolean.parseBoolean(args[4]);
                        case "add_inactive" -> data.addInactive = Boolean.parseBoolean(args[4]);
                        case "requestable" -> data.requestable = Boolean.parseBoolean(args[4]) ? new RoleData.Data.Requestable(false, null) : null;
                        case "temp" -> {
                            if (data.requestable != null) {
                                data.requestable.temp = Boolean.parseBoolean(args[4]);
                            }
                        }
                        case "agreement" -> {
                            return event.presentModal().withCustomId("role:%d:%d:agreement".formatted(data.roleId, data.serverId))
                                    .withTitle("Request agreement").withComponents(
                                            ActionRow.of(TextInput.paragraph("agreement", "Agreement (leave blank to remove)", 0, 1500)
                                                    .prefilled(data.requestable != null && data.requestable.agreement != null ?
                                                            data.requestable.agreement : "").required(false))
                                    );
                        }
                        case "on_remove", "on_add" -> {
                            return event.getClient().getGuildRoles(Snowflake.of(data.serverId))
                                    .filter(role -> role.getId().asLong() != data.roleId && !role.isEveryone())
                                    .map(role -> SelectMenu.Option.of("@%s".formatted(role.getName()), role.getId().asString())
                                            .withDefault((args[3].equals("on_remove") ? data.addWhenRemoved : data.removeWhenAdded)
                                                    .contains(role.getId().asLong())))
                                    .collectList().flatMap(options ->
                                                    event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                                    .content("Select roles to add when <@&%d> is %s."
                                                            .formatted(data.roleId, args[3].equals("on_remove") ? "removed" : "added"))
                                                    .embeds(Collections.emptyList())
                                                    .addComponent(ActionRow.of(
                                                            SelectMenu.of("role:%d:%d:%s".formatted(data.roleId, data.serverId, args[3]), options)
                                                                    .withMaxValues(options.size()).withMinValues(0)
                                                    )).addComponent(ActionRow.of(Button.danger("role:%d:%d:cancel_menu"
                                                                    .formatted(data.roleId, data.serverId), "Cancel")))
                                                    .build())
                                            );
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
                        case "no_save" -> {
                            roleData.removeTempData(data);
                            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                    .addEmbed(EmbedCreateSpec.builder()
                                            .title("Canceled changes to")
                                            .description("<@&%d>".formatted(data.roleId)).build()
                                    ).components(Collections.emptyList()).build());
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
