package org.snygame.rengetsu.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.*;
import org.snygame.rengetsu.tasks.RoleTimerTask;
import org.snygame.rengetsu.tasks.TaskManager;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class MemberListener extends Listener {
    public MemberListener(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public Mono<Void> handleJoin(MemberJoinEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        RoleData roleData = databaseManager.getRoleData();
        ServerData serverData = databaseManager.getServerData();
        UserData userData = databaseManager.getUserData();
        return Mono.just(event.getMember()).flatMap(member -> {
                    try {
                        userData.setMemberLastMsg(member.getId().asLong(), event.getGuildId().asLong(),
                                System.currentTimeMillis() / TimeStrings.DAY_MILLI);
                        List<Long> roleIds = roleData.getRolesToAddOnJoin(event.getGuildId().asLong());
                        List<Long> channelIds = serverData.getUserLogs(event.getGuildId().asLong());
                        return Flux.fromIterable(roleIds).map(Snowflake::of).flatMap(id -> event.getMember().addRole(id,
                                "Added on join")).then(Flux.fromIterable(channelIds).map(Snowflake::of).flatMap(
                                        event.getClient()::getChannelById).filter(channel -> channel instanceof MessageChannel)
                                .map(channel -> (MessageChannel)channel).flatMap(channel ->
                                        channel.createMessage("%s (Username: %s) has joined the server.".formatted(
                                                member.getMention(), member.getTag()
                                        ))).then());
                    } catch (SQLException e) {
                        return Mono.error(e);
                    }
                }
        ).then();
    }

    public Mono<Void> handleLeave(MemberLeaveEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        return Mono.just(event.getUser()).flatMap(user -> {
                    try {
                        List<Long> channelIds = serverData.getUserLogs(event.getGuildId().asLong());
                        return Flux.fromIterable(channelIds).map(Snowflake::of).flatMap(
                                        event.getClient()::getChannelById).filter(channel -> channel instanceof MessageChannel)
                                .map(channel -> (MessageChannel)channel).flatMap(channel ->
                                        channel.createMessage("%s (Username: %s) has left the server.".formatted(
                                                user.getMention(), user.getTag()
                                        ))).then();
                    } catch (SQLException e) {
                        return Mono.error(e);
                    }
                }
        ).then();
    }

    public Mono<Void> handleUpdate(MemberUpdateEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TaskManager taskManager = rengetsu.getTaskManager();
        RoleTimerData roleTimerData = databaseManager.getRoleTimerData();
        return event.getMember().mapNotNull(member -> {
            try {
                List<RoleTimerData.Data> timerIds = roleTimerData.getTimerIds(member.getGuildId().asLong(), member.getId().asLong());
                Set<Snowflake> roles = member.getRoleIds();
                timerIds.stream().filter(timer -> !roles.contains(Snowflake.of(timer.roleId())))
                        .map(RoleTimerData.Data::timerId).forEach(taskManager.getRoleTimerTask()::cancelTimer);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
            }

            return null;
        });
    }

    public Mono<Void> handleBan(BanEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        ServerData serverData = databaseManager.getServerData();
        return Mono.just(event.getUser()).flatMap(user -> {
                    try {
                        List<Long> channelIds = serverData.getUserLogs(event.getGuildId().asLong());
                        return Flux.fromIterable(channelIds).map(Snowflake::of).flatMap(
                                        event.getClient()::getChannelById).filter(channel -> channel instanceof MessageChannel)
                                .map(channel -> (MessageChannel)channel).flatMap(channel ->
                                        channel.createMessage("%s (Username: %s) has been banned.".formatted(
                                                user.getMention(), user.getTag()
                                        ))).then();
                    } catch (SQLException e) {
                        return Mono.error(e);
                    }
                }
        ).then();
    }
}
