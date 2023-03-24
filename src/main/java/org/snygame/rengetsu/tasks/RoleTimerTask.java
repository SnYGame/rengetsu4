package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.data.RoleTimerData;
import reactor.core.publisher.Flux;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RoleTimerTask {
    private static final Map<Long, ScheduledFuture<?>> tasks = new HashMap<>();

    public static void startup(GatewayDiscordClient client) {
        RoleTimerData roleTimerData = DatabaseManager.getRoleTimerData();
        try {
            for (RoleTimerData.Data data: roleTimerData.getAllTimers()) {
                startTask(client, data.timerId(), data.endOn().toEpochMilli() - System.currentTimeMillis());
            }
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("Error trying to load timers", e);
        }
    }

    public static void startTask(GatewayDiscordClient client, long timerId, long duration) {
        RoleData roleData = DatabaseManager.getRoleData();
        RoleTimerData roleTimerData = DatabaseManager.getRoleTimerData();
        ScheduledFuture<?> task = TaskManager.service.schedule(() -> {
            try {
                RoleTimerData.Data data = roleTimerData.getData(timerId);
                if (data != null) {
                    List<Long> ids = roleData.getRolesToAddWhenRemoved(data.roleId(), data.serverId());
                    client.getMemberById(Snowflake.of(data.serverId()), Snowflake.of(data.userId()))
                            .flatMap(member -> member.removeRole(Snowflake.of(data.roleId()), "Role request expired")
                                    .then(Flux.fromIterable(ids).map(Snowflake::of).flatMap(id ->
                                            member.addRole(id, "Added after removing role")).then()
                            )).subscribe();
                }
                roleTimerData.removeData(timerId);
                tasks.remove(timerId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, duration, TimeUnit.MILLISECONDS);
        tasks.put(timerId, task);
    }

    public static boolean cancelTimer(long timerId) {
        RoleTimerData roleTimerData = DatabaseManager.getRoleTimerData();
        ScheduledFuture<?> task;
        if ((task = tasks.remove(timerId)) != null) {
            task.cancel(false);
            if (task.isCancelled()) {
                try {
                    roleTimerData.removeData(timerId);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        return false;
    }
}
