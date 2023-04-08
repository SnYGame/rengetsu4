package org.snygame.rengetsu.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.ServerData;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsCommand extends SlashCommand {
    public SettingsCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.getOption("show").map(option -> subShow(event, option))
                .or(() -> event.getOption("inactive").map(option -> subInactive(event, option)))
                .or(() -> event.getOption("userlog").map(option -> subUserlog(event, option)))
                .or(() -> event.getOption("msglog").map(option -> subMsglog(event, option)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subShow(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();

        long serverId = event.getInteraction().getGuildId().map(Snowflake::asLong).orElse(0L);

        try {
            int inactive = serverData.getInactiveDays(serverId);
            List<Long> usrLogs = serverData.getUserLogs(serverId);
            List<Long> msgLogs = serverData.getMessageLogs(serverId);
            return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .addEmbed(EmbedCreateSpec.builder()
                            .addField("Inactivity Period", inactive == 0 ? "N/A" : String.valueOf(inactive), false)
                            .addField("User Logging Channels", usrLogs.isEmpty() ? "N/A" :
                                    usrLogs.stream().map("<#%d>"::formatted).collect(Collectors.joining(", ")), false)
                            .addField("Message Logging Channels", msgLogs.isEmpty() ? "N/A" :
                                    msgLogs.stream().map("<#%d>"::formatted).collect(Collectors.joining(", ")), false)
                            .build()).build());
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
    }

    private Mono<Void> subInactive(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();

        long days = option.getOption("days").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(0L);
        long serverId = event.getInteraction().getGuildId().map(Snowflake::asLong).orElse(0L);

        try {
            serverData.setInactiveDays(serverId, Math.toIntExact(days));
            if (days > 0) {
                return event.reply("Inactivity time set to %d days.".formatted(days)).withEphemeral(true);
            }

            return event.reply("Inactivity disabled.").withEphemeral(true);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
        }
        return event.reply("**[Error]** Database error").withEphemeral(true);
    }

    private Mono<Void> subUserlog(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        return event.reply().withComponents(List.of(
                ActionRow.of(
                        SelectMenu.ofChannel("settings:usrlog", Channel.Type.GUILD_TEXT).withMaxValues(25)
                ),
                ActionRow.of(
                        Button.danger("settings:usrlog:clear", "Clear")
                )
        )).withEphemeral(true);
    }

    private Mono<Void> subMsglog(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        return event.reply().withComponents(List.of(
                ActionRow.of(
                        SelectMenu.ofChannel("settings:msglog", Channel.Type.GUILD_TEXT).withMaxValues(25)
                ),
                ActionRow.of(
                        Button.danger("settings:msglog:clear", "Clear")
                )
        )).withEphemeral(true);
    }
}
