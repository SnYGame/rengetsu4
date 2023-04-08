package org.snygame.rengetsu.buttons;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.ServerData;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;

public class SettingsClearLogButton extends ButtonInteraction {
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
                }
                return event.edit(args[1].equals("usrlog") ? "User logging channels cleared." : "Message logging channels cleared.")
                        .withComponents();
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }
}
