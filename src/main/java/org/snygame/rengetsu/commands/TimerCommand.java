package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.data.TimerData;
import org.snygame.rengetsu.tasks.TimerTask;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class TimerCommand implements SlashCommand {
    private static final int MAX_DURATION = 60 * 60 * 24 * 30;
    @Override
    public String getName() {
        return "timer";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.getOption("set").map(__ -> subSet(event))
                .or(() -> event.getOption("list").map(__ -> subList(event)))
                .or(() -> event.getOption("cancel").map(__ -> subCancel(event)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subSet(ChatInputInteractionEvent event) {
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
                long timerId = TimerData.addTimer(event.getInteraction().getChannelId().asLong(), event.getInteraction().getUser().getId().asLong(),
                        message, Instant.ofEpochMilli(time), Instant.ofEpochMilli(time + duration * 1000L));

                if (timerId == -1) {
                    return event.reply("**[Error]** You cannot set more than 5 timers").withEphemeral(true);
                }
                String response = "Your timer (ID: %d) has been set for %s.".formatted(timerId, TimeStrings.secondsToEnglish(duration));
                TimerTask.startTask(event.getClient(), timerId, duration * 1000L);
                return event.reply(InteractionApplicationCommandCallbackSpec.builder().content(response)
                        .addComponent(
                                ActionRow.of(
                                        Button.danger("cancel_timer:%d:%d"
                                                        .formatted(timerId, event.getInteraction().getUser().getId().asLong()),
                                                "Cancel")
                                )
                        ).build());
            } catch (SQLException e) {
                e.printStackTrace();
                return event.reply("**[Error]** Database error").withEphemeral(true);
            }
        });
    }

    private Mono<Void> subList(ChatInputInteractionEvent event) {
        try {
            List<TimerData.Data> timers = TimerData.listTimers(event.getInteraction().getUser().getId().asLong());

            if (timers.isEmpty()) {
                return event.reply("You have no active timers.");
            }

            return event.reply(InteractionApplicationCommandCallbackSpec.builder().addEmbed(
                    EmbedCreateSpec.builder()
                            .addAllFields(timers.stream().map(data -> EmbedCreateFields.Field.of("Timer #%d (%s remaining)".formatted(
                                    data.timerId(), TimeStrings.secondsToEnglish((int) ((data.endOn().toEpochMilli() - System.currentTimeMillis()) / 1000))),
                                    "%s".formatted(data.message()), false)).toList())
                            .build()
            ).build());
        } catch (SQLException e) {
            e.printStackTrace();
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
    }

    private Mono<Void> subCancel(ChatInputInteractionEvent event) {
        long timerId = event.getOptions().get(0).getOption("id").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(-1L);

        try {
            TimerData.Data timer = TimerData.getData(timerId);
            if (timer != null) {
                if (event.getInteraction().getUser().getId().asLong() != timer.userId()) {
                    return event.reply("**[Error]** You do not have permission to do that").withEphemeral(true);
                }

                if (TimerTask.cancelTimer(timerId)) {
                    return event.reply("Timer %d has been canceled.".formatted(timerId));
                }
            }

            return event.reply("**[Error]** Timer has already completed or canceled, or does not exist").withEphemeral(true);
        } catch (SQLException e) {
            e.printStackTrace();
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
    }
}
