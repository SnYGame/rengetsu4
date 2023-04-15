package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

import java.sql.SQLException;

public class RoleCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public RoleCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "role";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        RoleData roleData = databaseManager.getRoleData();
        return Mono.justOrEmpty(event.getOption("role")
                .flatMap(ApplicationCommandInteractionOption::getValue))
                .flatMap(ApplicationCommandInteractionOptionValue::asRole)
                .flatMap(role -> {
                    if (role.isEveryone()) {
                        return event.reply("**[Error]** Cannot manipulate @everyone role").withEphemeral(true);
                    } else {
                        RoleData.Data data;

                        try {
                            data = roleData.getRoleData(role.getId().asLong(), role.getGuildId().asLong());
                        } catch (SQLException e) {
                            Rengetsu.getLOGGER().error("SQL Error", e);
                            return event.reply("**[Error]** Database error").withEphemeral(true);
                        }

                        roleData.putTempData(data);
                        return event.reply(RoleData.buildMenu(data));
                    }
                });
    }
}
