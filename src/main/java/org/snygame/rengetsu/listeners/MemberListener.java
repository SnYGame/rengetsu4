package org.snygame.rengetsu.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.data.RoleTimerData;
import org.snygame.rengetsu.data.ServerData;
import org.snygame.rengetsu.data.UserData;
import org.snygame.rengetsu.tasks.RoleTimerTask;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class MemberListener {
    public static Mono<Void> handleJoin(MemberJoinEvent event) {
        return Mono.just(event.getMember()).flatMap(member -> {
                    try {
                        UserData.setSetMemberLastMsg(member.getId().asLong(), event.getGuildId().asLong(),
                                System.currentTimeMillis() / UserData.DAY_MILLI);
                        List<Long> roleIds = RoleData.getRolesToAddOnJoin(event.getGuildId().asLong());
                        List<Long> channelIds = ServerData.getUserLogs(event.getGuildId().asLong());
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

    public static Mono<Void> handleLeave(MemberLeaveEvent event) {
        return Mono.just(event.getUser()).flatMap(user -> {
                    try {
                        List<Long> channelIds = ServerData.getUserLogs(event.getGuildId().asLong());
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

    public static Mono<Void> handleUpdate(MemberUpdateEvent event) {
        return event.getMember().mapNotNull(member -> {
            try {
                List<RoleTimerData.Data> timerIds = RoleTimerData.getTimerIds(member.getGuildId().asLong(), member.getId().asLong());
                Set<Snowflake> roles = member.getRoleIds();
                timerIds.stream().filter(timer -> !roles.contains(Snowflake.of(timer.roleId())))
                        .map(RoleTimerData.Data::timerId).forEach(RoleTimerTask::cancelTimer);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    public static Mono<Void> handleBan(BanEvent event) {
        return Mono.just(event.getUser()).flatMap(user -> {
                    try {
                        List<Long> channelIds = ServerData.getUserLogs(event.getGuildId().asLong());
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
