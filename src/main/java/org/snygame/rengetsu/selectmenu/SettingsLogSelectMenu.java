package org.snygame.rengetsu.selectmenu;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.ServerData;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.stream.Collectors;

public class SettingsLogSelectMenu extends InteractionListener.CommandDelegate<SelectMenuInteractionEvent> {
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
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).map(Snowflake::asLong).flatMap(serverId -> {
            try {
                switch (args[1]) {
                    case "usrlog" -> serverData.setUserLogs(serverId, event.getValues().stream().map(Long::parseLong).toList());
                    case "msglog" -> serverData.setMessageLogs(serverId, event.getValues().stream().map(Long::parseLong).toList());
                }
                return event.edit((args[1].equals("usrlog") ? "Set user logging channels to %s." : "Set message logging channels to %s.")
                                .formatted(event.getValues().stream().map("<#%s>"::formatted).collect(Collectors.joining(" "))))
                        .withComponents();
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }
}
