package org.snygame.rengetsu.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.ServerData;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;
import java.util.stream.Collectors;

public class SettingsLogSelectMenu extends SelectMenuInteraction {
    public SettingsLogSelectMenu(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public Mono<Void> handle(SelectMenuInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        String[] args = event.getCustomId().split(":");
        try {
            switch (args[1]) {
                case "usrlog" -> serverData.setUserLogs(event.getInteraction().getGuildId().get().asLong(), event.getValues().stream().map(Long::parseLong).toList());
                case "msglog" -> serverData.setMessageLogs(event.getInteraction().getGuildId().get().asLong(), event.getValues().stream().map(Long::parseLong).toList());
            }
            return event.edit((args[1].equals("usrlog") ? "Set user logging channels to %s." : "Set message logging channels to %s.")
                            .formatted(event.getValues().stream().map("<#%s>"::formatted).collect(Collectors.joining(" "))))
                    .withComponents();
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
    }
}
