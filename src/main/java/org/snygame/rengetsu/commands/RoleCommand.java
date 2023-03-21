package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.data.RoleData;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Collectors;

public class RoleCommand implements SlashCommand {
    @Override
    public String getName() {
        return "role";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getOption("role")
                .flatMap(ApplicationCommandInteractionOption::getValue))
                .flatMap(ApplicationCommandInteractionOptionValue::asRole)
                .flatMap(role -> {
                    if (role.isEveryone()) {
                        return event.reply("**[Error]** Cannot manipulate @everyone role").withEphemeral(true);
                    } else {
                        RoleData.Data roleData;

                        try {
                            roleData = RoleData.getRoleData(role.getId().asLong(), role.getGuildId().asLong());
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return event.reply("**[Error]** Database error").withEphemeral(true);
                        }

                        RoleData.putTempData(roleData);
                        return event.reply(RoleData.buildMenu(roleData));
                    }
                });
    }
}
