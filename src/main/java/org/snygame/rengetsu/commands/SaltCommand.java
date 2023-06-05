package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.rest.util.AllowedMentions;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.UserData;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.sql.SQLException;

public class SaltCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public SaltCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "salt";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.getOption("balance").map(option -> subBalance(event, option))
                .or(() -> event.getOption("claim").map(option -> subClaim(event, option)))
                .or(() -> event.getOption("remind").map(option -> subRemind(event, option)))
                .or(() -> event.getOption("give").map(option -> subGive(event, option)))
                .orElse(event.reply("**[Error]** Unimplemented subcommand").withEphemeral(true));
    }

    private Mono<Void> subBalance(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        UserData userData = databaseManager.getUserData();

        return Mono.justOrEmpty(option.getOption("user").flatMap(ApplicationCommandInteractionOption::getValue))
                .flatMap(ApplicationCommandInteractionOptionValue::asUser).flatMap(user -> {
                    if (user.isBot()) {
                        return event.reply("**[Error]** Bots cannot have salt").withEphemeral(true);
                    }

                    BigInteger saltAmount;
                    try {
                        saltAmount = userData.getSaltAmount(user.getId().asLong());
                    } catch (SQLException e) {
                        Rengetsu.getLOGGER().error("SQL Error", e);
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }

                    return event.reply("%s has %d salt.".formatted(user.getMention(), saltAmount))
                            .withEphemeral(true).withAllowedMentions(AllowedMentions.suppressAll());
        }).switchIfEmpty(Mono.just(event.getInteraction().getUser().getId().asLong()).flatMap(id -> {
                    BigInteger saltAmount;
                    try {
                        saltAmount = userData.getSaltAmount(id);
                    } catch (SQLException e) {
                        Rengetsu.getLOGGER().error("SQL Error", e);
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }
                    return event.reply("You have %d salt.".formatted(saltAmount)).withEphemeral(true);
                }).then());
    }

    private Mono<Void> subClaim(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        UserData userData = databaseManager.getUserData();
        long id = event.getInteraction().getUser().getId().asLong();
        BigInteger result;
        try {
            result = userData.claimSalt(id);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        if (result.signum() == -1) {
            int remain = result.negate().intValue();
            StringBuilder sb = new StringBuilder("Your next available claim is in ");
            sb.append(TimeStrings.secondsToEnglish(remain / 1000));
            return event.reply(sb.append('.').toString()).withEphemeral(true);
        }
        return event.reply("You now have %d salt.".formatted(result)).withEphemeral(true);
    }

    private Mono<Void> subRemind(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        UserData userData = databaseManager.getUserData();
        long id = event.getInteraction().getUser().getId().asLong();
        boolean remind;
        try {
            remind = userData.toggleRemind(id);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }
        return event.reply("You have turned %s daily salt claim reminders.".formatted(remind ? "on" : "off")).withEphemeral(true);
    }

    private Mono<Void> subGive(ChatInputInteractionEvent event, ApplicationCommandInteractionOption option) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        UserData userData = databaseManager.getUserData();
        long amount = option.getOption("amount")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(0L);

        if (amount <= 0) {
            return event.reply("**[Error]** You cannot give negative salt").withEphemeral(true);
        }

        return Mono.justOrEmpty(option.getOption("recipient")
                .flatMap(ApplicationCommandInteractionOption::getValue))
                .flatMap(ApplicationCommandInteractionOptionValue::asUser).flatMap(user -> {
                    if (user.isBot()) {
                        return event.reply("**[Error]** You cannot give salt to a bot").withEphemeral(true);
                    }

                    long id = event.getInteraction().getUser().getId().asLong();

                    if (user.getId().asLong() == id) {
                        return event.reply("**[Error]** You cannot give salt to yourself")
                                .withEphemeral(true);
                    }

                    BigInteger result;
                    try {
                        result = userData.giveSalt(id, user.getId().asLong(), BigInteger.valueOf(amount));
                    } catch (SQLException e) {
                        Rengetsu.getLOGGER().error("SQL Error", e);
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }

                    if (result.signum() == -1) {
                        return event.reply("**[Error]** You don't have enough salt").withEphemeral(true);
                    }

                    return event.reply("You gave %d salt to %s. You now have %d salt."
                            .formatted(amount, user.getMention(), result)).withAllowedMentions(AllowedMentions.suppressAll());
                });
    }
}
