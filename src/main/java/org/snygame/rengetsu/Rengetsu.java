package org.snygame.rengetsu;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.gateway.intent.IntentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.listeners.*;
import org.snygame.rengetsu.tasks.TaskManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class Rengetsu {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rengetsu.class);
    public static final Random RNG = new Random();

    private final GatewayDiscordClient client;
    private final DatabaseManager databaseManager;
    private final TaskManager taskManager;

    private Rengetsu(String token, String dbPath) throws SQLException, IOException {
        try {
            databaseManager = new DatabaseManager(this,dbPath, "tables.sql");
        } catch (Exception e) {
            LOGGER.error("Error trying to load database file", e);
            throw e;
        }

        //Creates the gateway client and connects to the gateway
        client = DiscordClient.create(token)
                .gateway().setEnabledIntents(IntentSet.all())
                .login().block();

        taskManager = new TaskManager(this);

        /* Call our code to handle creating/deleting/editing our global slash commands.
        We have to hard code our list of command files since iterating over a list of files in a resource directory
         is overly complicated for such a simple demo and requires handling for both IDE and .jar packaging.
         Using SpringBoot we can avoid all of this and use their resource pattern matcher to do this for us.
         */
        List<String> commands = List.of("dice.json", "here.json", "math.json", "salt.json", "timer.json", "role.json",
                "requestrole.json", "settings.json");
        try {
            new GlobalCommandRegistrar(client.getRestClient()).registerCommands(commands);
        } catch (Exception e) {
            LOGGER.error("Error trying to register global slash commands", e);
        }

        client.on(ReadyEvent.class, new ReadyEventListener(this)::handle).subscribe();
        client.on(ButtonInteractionEvent.class, new ButtonListener(this)::handle).subscribe();
        client.on(ModalSubmitInteractionEvent.class, new ModalListener(this)::handle).subscribe();
        client.on(SelectMenuInteractionEvent.class, new SelectMenuListener(this)::handle).subscribe();

        MemberListener memberListener = new MemberListener(this);
        client.on(MemberUpdateEvent.class, memberListener::handleUpdate).subscribe();
        client.on(MemberJoinEvent.class, memberListener::handleJoin).subscribe();
        client.on(MemberLeaveEvent.class, memberListener::handleLeave).subscribe();
        client.on(BanEvent.class, memberListener::handleBan).subscribe();

        MessageListener messageListener = new MessageListener(this);
        client.on(MessageCreateEvent.class, messageListener::handleCreate).subscribe();
        client.on(MessageDeleteEvent.class, messageListener::handleDelete).subscribe();
        //Register our slash command listener
        client.on(ChatInputInteractionEvent.class, new SlashCommandListener(this)::handle)
                .then(client.onDisconnect())
                .block(); // We use .block() as there is not another non-daemon thread and the jvm would close otherwise.
    }

    public static void main(String[] args) throws SQLException, IOException {
        new Rengetsu(args[0], "reng.db");
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public GatewayDiscordClient getClient() {
        return client;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }
}
