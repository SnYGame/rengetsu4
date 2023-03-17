package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.TimerData;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerTask {
    private static Map<Long, ScheduledFuture<?>> tasks = new HashMap<>();

    public static void startup(GatewayDiscordClient client) {
        try {
            int cleared = TimerData.cleanupTable();
            if (cleared > 0) {
                Rengetsu.getLOGGER().info("Cleared %d expired timers".formatted(cleared));
            }
            for (TimerData.Timer timer: TimerData.getAllTimers()) {
                startTask(client, timer.timerId(), timer.endOn().toEpochMilli() - System.currentTimeMillis());
            }
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("Error trying to load timers", e);
        }
    }

    public static void startTask(GatewayDiscordClient client, long timerId, long duration) {
        ScheduledFuture<?> task = TaskManager.service.schedule(() -> {
            try {
                TimerData.Data data = TimerData.getData(timerId);
                if (data != null) {
                    client.getChannelById(Snowflake.of(data.channelId())).filter(channel -> channel instanceof MessageChannel)
                            .map(channel -> (MessageChannel) channel).flatMap(channel ->
                                    channel.createMessage(MessageCreateSpec.builder().content("<@%d>".formatted(data.userId()))
                                            .addEmbed(EmbedCreateSpec.builder()
                                                    .title("Timer")
                                                    .description(data.message())
                                                    .footer(EmbedCreateFields.Footer.of("Set on", null))
                                                    .timestamp(data.setOn())
                                                    .build()).build())).subscribe();
                }
                TimerData.removeData(timerId);
                tasks.remove(timerId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, duration, TimeUnit.MILLISECONDS);
        tasks.put(timerId, task);
    }

    public static boolean cancelTimer(long timerId) {
        ScheduledFuture<?> task;
        if ((task = tasks.remove(timerId)) != null) {
            task.cancel(false);
            if (task.isCancelled()) {
                try {
                    TimerData.removeData(timerId);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        return false;
    }
}
