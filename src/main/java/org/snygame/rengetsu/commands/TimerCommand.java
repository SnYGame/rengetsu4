package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.TimerData;
import org.snygame.rengetsu.tasks.TaskManager;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class TimerCommand extends SlashCommand {
    private static final int MAX_DURATION = 60 * 60 * 24 * 30;

    public TimerCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "timer";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.getOption("set").map(__ -> subSet(event))
                .or(() -> event.getOption("list").map(__ -> subList(event)))
                .or(() -> event.getOption("cancel").map(__ -> subCancel(event)))
                .or(() -> event.getOption("subscribe").map(__ -> subSubscribe(event)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subSet(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TaskManager taskManager = rengetsu.getTaskManager();
        TimerData timerData = databaseManager.getTimerData();
        return Mono.just(event.getOptions().get(0).getOption("duration").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).map(TimeStrings::readDuration)
                .orElse(0)).flatMap(duration -> {
            if (duration <= 0 || duration > MAX_DURATION) {
                return event.reply("**[Error]** Duration must represent time between 1 second and 30 days").withEphemeral(true);
            }
            String message = event.getOptions().get(0).getOption("message").flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString).orElse("Timer has completed");
            long time = System.currentTimeMillis();
            try {
                long userId = event.getInteraction().getUser().getId().asLong();
                long timerId = timerData.addTimer(event.getInteraction().getChannelId().asLong(), userId,
                        message, Instant.ofEpochMilli(time), Instant.ofEpochMilli(time + duration * 1000L));

                if (timerId == -1) {
                    return event.reply("**[Error]** You cannot set more than 5 timers").withEphemeral(true);
                }
                String response = "Your timer (ID: %d) has been set for %s.".formatted(timerId, TimeStrings.secondsToEnglish(duration));
                taskManager.getTimerTask().startTask(event.getClient(), timerId, duration * 1000L);
                return event.reply(InteractionApplicationCommandCallbackSpec.builder().content(response)
                        .addComponent(
                                ActionRow.of(
                                        Button.primary("timer:%d:%d:subscribe".formatted(timerId, userId),"Subscribe"),
                                        Button.danger("timer:%d:%d:cancel".formatted(timerId, userId),"Cancel")
                                )
                        ).build());
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }

    private Mono<Void> subList(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TimerData timerData = databaseManager.getTimerData();
        try {
            long userId = event.getInteraction().getUser().getId().asLong();
            List<TimerData.Data> timers = timerData.listTimers(userId);
            List<TimerData.Data> subscriptions = timerData.listSubscriptions(userId);

            if (timers.isEmpty()) {
                return event.reply("You have no active timers.");
            }

            return event.reply(InteractionApplicationCommandCallbackSpec.builder().addEmbed(
                    EmbedCreateSpec.builder()
                            .addAllFields(timers.stream().map(data -> EmbedCreateFields.Field.of("Timer #%d (%s remaining)".formatted(
                                    data.timerId(), TimeStrings.secondsToEnglish((int) ((data.endOn().toEpochMilli() - System.currentTimeMillis()) / 1000))),
                                    "%s".formatted(data.message()), false)).toList())
                            .addAllFields(subscriptions.stream().map(data -> EmbedCreateFields.Field.of("Timer subscription #%d (%s remaining)".formatted(
                                        data.timerId(), TimeStrings.secondsToEnglish((int) ((data.endOn().toEpochMilli() - System.currentTimeMillis()) / 1000))),
                                    "%s".formatted(data.message()), false)).toList())
                            .build()
            ).build());
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
    }

    private Mono<Void> subCancel(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TaskManager taskManager = rengetsu.getTaskManager();
        TimerData timerData = databaseManager.getTimerData();
        long timerId = event.getOptions().get(0).getOption("id").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(-1L);

        try {
            TimerData.Data timer = timerData.getData(timerId);
            if (timer != null) {
                long userId = event.getInteraction().getUser().getId().asLong();
                if (userId != timer.userId()) {
                    if (timerData.unsubscribeTimer(userId, timerId)) {
                        return event.reply("You have unsubscribed from timer %d.".formatted(timerId));
                    }

                    return event.reply("**[Error]** You are not subscribed to timer %d".formatted(timerId)).withEphemeral(true);
                }

                if (taskManager.getTimerTask().cancelTimer(timerId)) {
                    return event.reply("Timer %d has been canceled.".formatted(timerId));
                }
            }

            return event.reply("**[Error]** Timer has already completed or canceled, or does not exist").withEphemeral(true);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
    }

    private Mono<Void> subSubscribe(ChatInputInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TimerData timerData = databaseManager.getTimerData();
        long timerId = event.getOptions().get(0).getOption("id").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(-1L);

        try {
            TimerData.Data timer = timerData.getData(timerId);
            if (timer != null) {
                long userId = event.getInteraction().getUser().getId().asLong();
                if (userId == timer.userId()) {
                    return event.reply("**[Error]** You cannot subscribe to your own timer").withEphemeral(true);
                }

                if (timerData.subscribeTimer(userId, timerId)) {
                    return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .content("You have subscribed to timer %d.".formatted(timerId))
                            .addComponent(ActionRow.of(
                                    Button.danger("timer:%d:0:unsubscribe".formatted(timerId), "Unsubscribe"))
                            ).build());
                }

                return event.reply("**[Error]** You have already subscribed to timer %d".formatted(timerId)).withEphemeral(true);
            }

            return event.reply("**[Error]** Timer has already completed or canceled, or does not exist").withEphemeral(true);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
    }
}
