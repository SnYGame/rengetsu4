package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.data.ServerData;
import org.snygame.rengetsu.data.UserData;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DailyTask extends RengTask {
    public DailyTask(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public void startTask(GatewayDiscordClient client) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        RoleData roleData = databaseManager.getRoleData();
        ServerData serverData = databaseManager.getServerData();
        UserData userData = databaseManager.getUserData();
        TaskManager.service.scheduleAtFixedRate(() -> {
            try {
                List<Snowflake> ids = userData.getRemindIds();
                Flux.fromIterable(ids).flatMap(client::getUserById).flatMap(User::getPrivateChannel)
                        .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder().content("Your daily salt is available to be claimed.").addComponent(
                                ActionRow.of(
                                        Button.primary("salt_claim:%d"
                                                        .formatted(System.currentTimeMillis() / TimeStrings.DAY_MILLI),
                                                "Claim")
                                )
                        ).build()))
                        .subscribe();
                long today = System.currentTimeMillis() / TimeStrings.DAY_MILLI;
                client.getGuilds().flatMap(server -> {
                    try {
                        System.out.println(server.getId());
                        int days = serverData.getInactiveDays(server.getId().asLong());
                        List<Long> idsToAdd = roleData.getRolesToAddOnInactive(server.getId().asLong());
                        List<Long> idsToRemove = idsToAdd.stream().map(id -> {
                            try {
                                return roleData.getRolesToRemoveWhenAdded(id, server.getId().asLong());
                            } catch (SQLException e) {
                                Rengetsu.getLOGGER().error("SQL Error", e);
                                return new ArrayList<Long>();
                            }
                        }).flatMap(List::stream).distinct().toList();
                        if (days > 0 && !idsToAdd.isEmpty()) {
                            return server.getMembers().filter(member -> !member.isBot()).flatMap(member -> {
                                try {
                                    long lastMsg = userData.getMemberLastMsg(member.getId().asLong(), server.getId().asLong());
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
                Rengetsu.getLOGGER().error("SQL Error", e);
            }
        }, TimeStrings.DAY_MILLI - System.currentTimeMillis() % TimeStrings.DAY_MILLI, TimeStrings.DAY_MILLI,
                TimeUnit.MILLISECONDS);
    }
}
