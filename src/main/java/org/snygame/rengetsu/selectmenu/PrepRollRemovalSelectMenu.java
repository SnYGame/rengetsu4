package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.entity.PartialMember;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.data.RoleData;
import reactor.core.publisher.Mono;

import java.util.List;

public class PrepRollRemovalSelectMenu extends SelectMenuInteraction {
    public PrepRollRemovalSelectMenu(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "prep";
    }

    @Override
    public Mono<Void> handle(SelectMenuInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        int[] remove = event.getValues().stream().mapToInt(Integer::parseInt).sorted().toArray();
        for (int i = remove.length - 1; i >= 0; i--) {
            data.dicerolls.remove(remove[i]);
        }

        return event.edit(PrepData.buildMenu(data));
    }
}
