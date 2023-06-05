package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

import java.util.List;

public class RoleRemovalSelectMenu extends InteractionListener.CommandDelegate<SelectMenuInteractionEvent> {
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

        String[] args = event.getCustomId().split(":");

        RoleData.Data data = roleData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        List<Long> list = (args[1].equals("on_remove") ? data.addWhenRemoved : data.removeWhenAdded);
        list.clear();
        event.getValues().forEach(id -> list.add(Long.parseLong(id)));
        return event.edit(RoleData.buildMenu(data));
    }
}
