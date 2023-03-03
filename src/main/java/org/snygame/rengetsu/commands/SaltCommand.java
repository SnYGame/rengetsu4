package org.snygame.rengetsu.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import org.snygame.rengetsu.data.UserData;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.stream.Collectors;

public class SaltCommand implements SlashCommand {
    @Override
    public String getName() {
        return "salt";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.getOption("balance").map(__ -> subBalance(event))
                .or(() -> event.getOption("claim").map(__ -> subClaim(event)))
                .or(() -> event.getOption("remind").map(__ -> subRemind(event)))
                .or(() -> event.getOption("give").map(__ -> subGive(event)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subBalance(ChatInputInteractionEvent event) {
        return event.getOptions().get(0).getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser).map(userMono -> userMono.flatMap(user -> {
                    if (user.isBot()) {
                        return event.reply("**[Error]** Bots cannot have salt").withEphemeral(true);
                    } else {
                        UserData.Data userData = UserData.getUserData(user.getId().asLong());
                        if (userData == null) {
                            return event.reply("**[Error]** Database error").withEphemeral(true);
                        }
                        return event.reply("%s has %d salt.".formatted(user.getMention(), userData.saltAmount()))
                                .withEphemeral(true);
                    }
        }).then()).orElseGet(() -> Mono.justOrEmpty(event.getInteraction().getMember())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    UserData.Data userData = UserData.getUserData(id);
                    if (userData == null) {
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }
                    return event.reply("You have %d salt.".formatted(userData.saltAmount())).withEphemeral(true);
                }).then());
    }

    private Mono<Void> subClaim(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getInteraction().getMember())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    UserData.Data userData = UserData.getUserData(id);
                    if (userData != null) {
                        long newAmt = userData.saltAmount() + 2000;
                        Timestamp lastClaim = userData.saltLastClaim();
                        long remain = lastClaim.getTime() + 1000 * 60 * 60 * 24 - System.currentTimeMillis();
                        if (remain > 0) {
                            long remainSeconds = remain / 1000;
                            long h = remainSeconds / 3600;
                            long m = (remainSeconds % 3600) / 60;
                            long s = remainSeconds % 60;
                            StringBuilder sb = new StringBuilder("Your next available claim is in");

                            if (h > 0)
                                sb.append(" %d hour%s".formatted(h, h != 1 ? "s" : ""));

                            if (m > 0)
                                sb.append(" %d minute%s".formatted(m, m != 1 ? "s" : ""));

                            if (s > 0 || (h == 0 && m == 0))
                                sb.append(" %d second%s".formatted(s, s != 1 ? "s" : ""));
                            return event.reply(sb.toString()).withEphemeral(true);
                        }
                        if (UserData.claimSalt(id, newAmt)) {
                            return event.reply("You now have %d salt.".formatted(newAmt)).withEphemeral(true);
                        }
                    }
                    return event.reply("**[Error]** Database error").withEphemeral(true);
                }).then();
    }

    private Mono<Void> subRemind(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }

    private Mono<Void> subGive(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }
}
