package org.snygame.rengetsu.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import org.snygame.rengetsu.data.UserData;
import reactor.core.publisher.Mono;

import java.math.BigInteger;

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
                        BigInteger saltAmount = UserData.getSaltAmount(user.getId().asLong());
                        if (saltAmount == null) {
                            return event.reply("**[Error]** Database error").withEphemeral(true);
                        }
                        return event.reply("%s has %d salt.".formatted(user.getMention(), saltAmount))
                                .withEphemeral(true);
                    }
        }).then()).orElseGet(() -> Mono.justOrEmpty(event.getInteraction().getMember())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    BigInteger saltAmount = UserData.getSaltAmount(id);
                    if (saltAmount == null) {
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }
                    return event.reply("You have %d salt.".formatted(saltAmount)).withEphemeral(true);
                }).then());
    }

    private Mono<Void> subClaim(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getInteraction().getMember())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    BigInteger result = UserData.claimSalt(id);
                    if (result == null) {
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }

                    if (result.signum() == -1) {
                        int remain = result.negate().intValue();
                        int remainSeconds = remain / 1000;
                        int h = remainSeconds / 3600;
                        int m = (remainSeconds % 3600) / 60;
                        int s = remainSeconds % 60;
                        StringBuilder sb = new StringBuilder("Your next available claim is in");

                        if (h > 0)
                            sb.append(" %d hour%s".formatted(h, h != 1 ? "s" : ""));

                        if (m > 0)
                            sb.append(" %d minute%s".formatted(m, m != 1 ? "s" : ""));

                        if (s > 0 || (h == 0 && m == 0))
                            sb.append(" %d second%s".formatted(s, s != 1 ? "s" : ""));
                        return event.reply(sb.append('.').toString()).withEphemeral(true);
                    }
                    return event.reply("You now have %d salt.".formatted(result)).withEphemeral(true);
                }).then();
    }

    private Mono<Void> subRemind(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }

    private Mono<Void> subGive(ChatInputInteractionEvent event) {
        return event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true);
    }
}
