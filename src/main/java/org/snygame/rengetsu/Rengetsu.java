package org.snygame.rengetsu;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snygame.rengetsu.dicerolls.Diceroll;
import org.snygame.rengetsu.listeners.SlashCommandListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Rengetsu {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rengetsu.class);

    public static final Random RNG = new Random();

    public static void main(String[] args) {
        //Creates the gateway client and connects to the gateway
        GatewayDiscordClient client = DiscordClient.create(System.getenv("TOKEN"))
                .login().block();

        /* Call our code to handle creating/deleting/editing our global slash commands.
        We have to hard code our list of command files since iterating over a list of files in a resource directory
         is overly complicated for such a simple demo and requires handling for both IDE and .jar packaging.
         Using SpringBoot we can avoid all of this and use their resource pattern matcher to do this for us.
         */
        List<String> commands = List.of("dice.json");
        try {
            new GlobalCommandRegistrar(client.getRestClient()).registerCommands(commands);
        } catch (Exception e) {
            LOGGER.error("Error trying to register global slash commands", e);
        }

        //Register our slash command listener
        client.on(ChatInputInteractionEvent.class, SlashCommandListener::handle)
                .then(client.onDisconnect())
                .block(); // We use .block() as there is not another non-daemon thread and the jvm would close otherwise.
    }
}
