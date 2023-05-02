package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class PrepRollRemovalSelectMenu extends InteractionListener.CommandDelegate<SelectMenuInteractionEvent> {
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
            int index = remove[i];
            PrepData.Data.RollData rollData = data.rolls.remove(index);
            PrepData.Data.RollData newDesc;
            if (rollData.description != null && index < data.rolls.size() && (newDesc = data.rolls.get(index)).description == null) {
                newDesc.description = rollData.description;
            }
        }

        return event.edit(PrepData.buildMenu(data));
    }
}
