package org.snygame.rengetsu.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.AttachmentData;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.ServerData;
import org.snygame.rengetsu.data.UserData;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class MessageListener {
    public static Mono<Void> handleCreate(MessageCreateEvent event) {
        UserData userData = DatabaseManager.getUserData();
        return Mono.justOrEmpty(event.getMember()).filter(member -> !member.isBot()).flatMap(member -> {
            try {
                userData.setMemberLastMsg(member.getId().asLong(), member.getGuildId().asLong(),
                        System.currentTimeMillis() / TimeStrings.DAY_MILLI);
                return Mono.empty();
            } catch (SQLException e) {
                return Mono.error(e);
            }
        }).then();
    }

    public static Mono<Void> handleDelete(MessageDeleteEvent event) {
        ServerData serverData = DatabaseManager.getServerData();
        return event.getGuild().flatMap(server -> {
            try {
                List<Long> channelIds = serverData.getMessageLogs(server.getId().asLong());
                if (channelIds.contains(event.getChannelId().asLong())) {
                    return Mono.empty();
                }
                return Flux.fromIterable(channelIds).map(Snowflake::of).flatMap(event.getClient()::getChannelById)
                        .filter(channel -> channel instanceof MessageChannel).map(channel -> (MessageChannel)channel)
                        .flatMap(channel -> event.getMessage().map(Message::getData).map(data -> {
                            MessageCreateSpec.Builder builder = MessageCreateSpec.builder();
                            builder.content(data.content());
                            for (AttachmentData attachment: data.attachments()) {
                                try {
                                    URL url = new URL(attachment.proxyUrl());
                                    builder.addFile(attachment.filename(), url.openStream());
                                    continue;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                try {
                                    URL url = new URL(attachment.url());
                                    builder.addFile(attachment.filename(), url.openStream());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            return channel.createMessage("Message deleted from channel: <#%d>\nAuthor: <@%d>\n\nMessage:"
                                            .formatted(data.channelId().asLong(), data.author().id().asLong()))
                                    .then(channel.createMessage(builder.build())
                                    );
                        }).orElse(channel.createMessage("Message deleted from channel: <#%d>\nMessage not found in cache"
                                .formatted(event.getChannelId().asLong())))).collectList().then();
            } catch (SQLException e) {
                return Mono.error(e);
            }
        }).then();
    }
}
