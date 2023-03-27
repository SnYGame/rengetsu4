package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.TimerData;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerTask extends RengTask {
    private final Map<Long, ScheduledFuture<?>> tasks = new HashMap<>();

    public TimerTask(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public void startup(GatewayDiscordClient client) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TimerData timerData = databaseManager.getTimerData();
        try {
            int cleared = timerData.cleanupTable();
            if (cleared > 0) {
                Rengetsu.getLOGGER().info("Cleared %d expired timers".formatted(cleared));
            }
            for (TimerData.Data data: timerData.getAllTimers()) {
                startTask(client, data.timerId(), data.endOn().toEpochMilli() - System.currentTimeMillis());
            }
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("Error trying to load timers", e);
        }
    }

    public void startTask(GatewayDiscordClient client, long timerId, long duration) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TimerData timerData = databaseManager.getTimerData();
        ScheduledFuture<?> task = TaskManager.service.schedule(() -> {
            try {
                TimerData.Data data = timerData.getData(timerId);
                if (data != null) {
                    client.getChannelById(Snowflake.of(data.channelId())).filter(channel -> channel instanceof MessageChannel)
                            .map(channel -> (MessageChannel) channel).flatMap(channel ->
                                    channel.createMessage(MessageCreateSpec.builder().content("<@%d>".formatted(data.userId()))
                                            .addEmbed(EmbedCreateSpec.builder()
                                                    .title("Timer #%d".formatted(timerId))
                                                    .description(data.message())
                                                    .footer(EmbedCreateFields.Footer.of("Set on", null))
                                                    .timestamp(data.setOn())
                                                    .build()).build())).subscribe();
                }
                timerData.removeData(timerId);
                tasks.remove(timerId);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
            }
        }, duration, TimeUnit.MILLISECONDS);
        tasks.put(timerId, task);
    }

    public boolean cancelTimer(long timerId) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TimerData timerData = databaseManager.getTimerData();
        ScheduledFuture<?> task;
        if ((task = tasks.remove(timerId)) != null) {
            task.cancel(false);
            if (task.isCancelled()) {
                try {
                    timerData.removeData(timerId);
                } catch (SQLException e) {
                    Rengetsu.getLOGGER().error("SQL Error", e);
                }
                return true;
            }
        }

        return false;
    }
}
