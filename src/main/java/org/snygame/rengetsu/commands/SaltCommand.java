package org.snygame.rengetsu.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import org.snygame.rengetsu.data.UserData;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.sql.SQLException;

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
                    }

                    BigInteger saltAmount;
                    try {
                        saltAmount = UserData.getSaltAmount(user.getId().asLong());
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }

                    return event.reply("%s has %d salt.".formatted(user.getMention(), saltAmount))
                            .withEphemeral(true);
        }).then()).orElseGet(() -> Mono.justOrEmpty(event.getInteraction().getMember())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    BigInteger saltAmount;
                    try {
                        saltAmount = UserData.getSaltAmount(id);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }
                    return event.reply("You have %d salt.".formatted(saltAmount)).withEphemeral(true);
                }).then());
    }

    private Mono<Void> subClaim(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getInteraction().getMember())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    BigInteger result;
                    try {
                        result = UserData.claimSalt(id);
                    } catch (SQLException e) {
                        e.printStackTrace();
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
        return Mono.justOrEmpty(event.getInteraction().getMember())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    boolean remind;
                    try {
                        remind = UserData.toggleRemind(id);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }
                    return event.reply("You have turned %s daily salt claim reminders.".formatted(remind ? "on" : "off")).withEphemeral(true);
                }).then();
    }

    private Mono<Void> subGive(ChatInputInteractionEvent event) {
        long amount = event.getOptions().get(0).getOption("amount")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(0L);

        if (amount < 0) {
            return event.reply("**[Error]** You cannot give negative salt").withEphemeral(true);
        }

        return event.getOptions().get(0).getOption("recipient")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser).map(userMono -> userMono.flatMap(user -> {
                    if (user.isBot()) {
                        return event.reply("**[Error]** You cannot give salt to a bot").withEphemeral(true);
                    }

                    return Mono.justOrEmpty(event.getInteraction().getMember()).map(User::getId).map(Snowflake::asLong)
                            .flatMap(id -> {

                                if (user.getId().asLong() == id) {
                                    return event.reply("**[Error]** You cannot give salt to yourself")
                                            .withEphemeral(true);
                                }

                                BigInteger result;
                                try {
                                    result = UserData.giveSalt(id, user.getId().asLong(), BigInteger.valueOf(amount));
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    return event.reply("**[Error]** Database error").withEphemeral(true);
                                }

                                if (result.signum() == -1) {
                                    return event.reply("**[Error]** You don't have enough salt").withEphemeral(true);
                                }

                                return event.reply("You gave %d salt to %s. You now have %d salt."
                                                .formatted(amount, user.getMention(), result));
                            });
                })).orElse(Mono.empty());
    }
}
