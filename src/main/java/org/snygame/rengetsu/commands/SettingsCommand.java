package org.snygame.rengetsu.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.ServerData;
import org.snygame.rengetsu.data.UserData;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        return event.getOption("show").map(__ -> subShow(event))
                .or(() -> event.getOption("inactive").map(__ -> subInactive(event)))
                .or(() -> event.getOption("userlog").map(__ -> subUserlog(event)))
                .or(() -> event.getOption("msglog").map(__ -> subMsglog(event)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subShow(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        return Mono.justOrEmpty(event.getInteraction().getGuildId().map(Snowflake::asLong)).flatMap(serverId -> {
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
                e.printStackTrace();
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }

    private Mono<Void> subInactive(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        return Mono.justOrEmpty(event.getOptions().get(0).getOption("days")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)).flatMap(days ->
                Mono.justOrEmpty(event.getInteraction().getGuildId()).map(Snowflake::asLong).flatMap(id -> {
                    try {
                        serverData.setInactiveDays(id, Math.toIntExact(days));
                        if (days > 0) {
                            return event.reply("Inactivity time set to %d days.".formatted(days));
                        }

                        return event.reply("Inactivity disabled.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return event.reply("**[Error]** Database error").withEphemeral(true);
                }).then()
        );
    }

    private Mono<Void> subUserlog(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).map(Snowflake::asLong).flatMap(serverId -> {
            List<Long> ids = IntStream.range(1, 4).mapToObj("channel%d"::formatted)
                    .flatMap(name -> event.getOptions().get(0).getOption(name).stream())
                    .flatMap(option -> option.getValue().stream()).map(ApplicationCommandInteractionOptionValue::asSnowflake)
                    .map(Snowflake::asLong)
                    .toList();

            try {
                serverData.setUserLogs(serverId, ids);
                if (ids.isEmpty()) {
                    return event.reply("Cleared user logging channels.");
                }

                return event.reply("User logging channels set to %s.".formatted(ids.stream().map(
                        "<#%d>"::formatted).collect(Collectors.joining(", "))));
            } catch (SQLException e) {
                e.printStackTrace();
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }

    private Mono<Void> subMsglog(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).map(Snowflake::asLong).flatMap(serverId -> {
            List<Long> ids = IntStream.range(1, 4).mapToObj("channel%d"::formatted)
                    .flatMap(name -> event.getOptions().get(0).getOption(name).stream())
                    .flatMap(option -> option.getValue().stream()).map(ApplicationCommandInteractionOptionValue::asSnowflake)
                    .map(Snowflake::asLong)
                    .toList();

            try {
                serverData.setMessageLogs(serverId, ids);
                if (ids.isEmpty()) {
                    return event.reply("Cleared message logging channels.");
                }

                return event.reply("Message logging channels set to %s.".formatted(ids.stream().map(
                        "<#%d>"::formatted).collect(Collectors.joining(", "))));
            } catch (SQLException e) {
                e.printStackTrace();
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }
}
