package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.entity.PartialMember;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import reactor.core.publisher.Mono;

import java.util.List;

public class RoleRemovalSelectMenu extends SelectMenuInteraction {
    public RoleRemovalSelectMenu(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "role";
    }

    @Override
    public Mono<Void> handle(SelectMenuInteractionEvent event) {
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

                    List<Long> list = (args[3].equals("on_remove") ? data.addWhenRemoved : data.removeWhenAdded);
                    list.clear();
                    event.getValues().forEach(id -> list.add(Long.parseLong(id)));
                    return event.edit(RoleData.buildMenu(data));
                });
    }
}
