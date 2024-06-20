package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class PrepListSelectMenu extends InteractionListener.CommandDelegate<SelectMenuInteractionEvent> {
    public PrepListSelectMenu(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "prep_list";
    }

    @Override
    public Mono<Void> handle(SelectMenuInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        String[] args = event.getCustomId().split(":");
        String[] values = event.getValues().get(0).split(":", -1);
        switch (args[1]) {
            case "prep" -> {
                return event.edit(prepData.getPrepList(Long.parseLong(values[0]), values[1], values[2]));
            }
            case "namespace" -> {
                return event.edit(prepData.getPrepList(Long.parseLong(values[0]), values[1]));
            }
        }
        return null;
    }
}
