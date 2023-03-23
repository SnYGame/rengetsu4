package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.data.ServerData;
import org.snygame.rengetsu.data.UserData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DailyTask {
    public static void startTask(GatewayDiscordClient client) {
        TaskManager.service.scheduleAtFixedRate(() -> {
            try {
                List<Snowflake> ids = UserData.getRemindIds();
                Flux.fromIterable(ids).flatMap(client::getUserById).flatMap(User::getPrivateChannel)
                        .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder().content("Your daily salt is available to be claimed.").addComponent(
                                ActionRow.of(
                                        Button.primary("salt_claim:%d"
                                                        .formatted(System.currentTimeMillis() / UserData.DAY_MILLI),
                                                "Claim")
                                )
                        ).build()))
                        .subscribe();
                long today = System.currentTimeMillis() / UserData.DAY_MILLI;
                client.getGuilds().flatMap(server -> {
                    try {
                        System.out.println(server.getId());
                        int days = ServerData.getInactiveDays(server.getId().asLong());
                        List<Long> idsToAdd = RoleData.getRolesToAddOnInactive(server.getId().asLong());
                        List<Long> idsToRemove = idsToAdd.stream().map(id -> {
                            try {
                                return RoleData.getRolesToRemoveWhenAdded(id, server.getId().asLong());
                            } catch (SQLException e) {
                                e.printStackTrace();
                                return new ArrayList<Long>();
                            }
                        }).flatMap(List::stream).distinct().toList();
                        if (days > 0 && !idsToAdd.isEmpty()) {
                            return server.getMembers().filter(member -> !member.isBot()).flatMap(member -> {
                                try {
                                    long lastMsg = UserData.getSetMemberLastMsg(member.getId().asLong(), server.getId().asLong());
                                    if (lastMsg + days < today) {
                                        return Flux.fromIterable(idsToAdd).map(Snowflake::of).flatMap(id -> member.addRole(id, "Added on inactivity"))
                                                .then(Flux.fromIterable(idsToRemove).map(Snowflake::of).flatMap(id ->
                                                        member.removeRole(id, "Removed after adding new role")).then());
                                    }
                                    return Mono.empty();
                                } catch (SQLException e) {
                                    return Mono.error(e);
                                }
                            });
                        }
                        return Mono.empty();
                    } catch (SQLException e) {
                        return Mono.error(e);
                    }
                }).subscribe();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, UserData.DAY_MILLI - System.currentTimeMillis() % UserData.DAY_MILLI, UserData.DAY_MILLI,
                TimeUnit.MILLISECONDS);
    }
}
