package org.snygame.rengetsu.buttons;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.ServerData;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;

public class SettingsClearLogButton extends InteractionListener.CommandDelegate<ButtonInteractionEvent> {
    public SettingsClearLogButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        String[] args = event.getCustomId().split(":");
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).map(Snowflake::asLong).flatMap(serverId -> {
            try {
                switch (args[1]) {
                    case "usrlog" -> serverData.setUserLogs(serverId, Collections.emptyList());
                    case "msglog" -> serverData.setMessageLogs(serverId, Collections.emptyList());
                    case "reportlog" -> serverData.setReportLogs(serverId, Collections.emptyList());
                }

                String response = switch (args[1]) {
                    case "usrlog" -> "User logging channels cleared.";
                    case "msglog" -> "Message logging channels cleared.";
                    case "reportlog" -> "Report logging channels cleared.";
                    default -> "Unused";
                };
                return event.edit(response).withComponents();
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }
}
