package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.RoleTimerData;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RoleTimerTask {
    private static final Map<Long, ScheduledFuture<?>> tasks = new HashMap<>();

    public static void startup(GatewayDiscordClient client) {
        try {
            int cleared = RoleTimerData.cleanupTable();
            if (cleared > 0) {
                Rengetsu.getLOGGER().info("Cleared %d expired timers".formatted(cleared));
            }
            for (RoleTimerData.Data data: RoleTimerData.getAllTimers()) {
                startTask(client, data.timerId(), data.endOn().toEpochMilli() - System.currentTimeMillis());
            }
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("Error trying to load timers", e);
        }
    }

    public static void startTask(GatewayDiscordClient client, long timerId, long duration) {
        ScheduledFuture<?> task = TaskManager.service.schedule(() -> {
            try {
                RoleTimerData.Data data = RoleTimerData.getData(timerId);
                if (data != null) {
                    client.getMemberById(Snowflake.of(data.serverId()), Snowflake.of(data.userId()))
                            .flatMap(member -> member.removeRole(Snowflake.of(data.roleId()), "Request expired")).subscribe();
                }
                RoleTimerData.removeData(timerId);
                tasks.remove(timerId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, duration, TimeUnit.MILLISECONDS);
        tasks.put(timerId, task);
    }

    public static boolean cancelTimer(long timerId) {
        System.out.println(timerId);
        ScheduledFuture<?> task;
        if ((task = tasks.remove(timerId)) != null) {
            task.cancel(false);
            if (task.isCancelled()) {
                try {
                    RoleTimerData.removeData(timerId);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        return false;
    }
}
