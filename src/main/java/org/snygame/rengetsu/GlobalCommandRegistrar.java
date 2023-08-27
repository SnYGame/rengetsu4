package org.snygame.rengetsu;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snygame.rengetsu.util.Resources;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GlobalCommandRegistrar {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestClient restClient;
    private final List<ApplicationCommandRequest> commands = new ArrayList<>();

    // The name of the folder the commands json is in, inside our resources folder
    private static final String commandsFolderName = "commands/";

    public GlobalCommandRegistrar(RestClient restClient) {
        this.restClient = restClient;
    }

    //Since this will only run once on startup, blocking is okay.
    protected void loadSlashCommands(List<String> fileNames) throws IOException {
        //Create an ObjectMapper that supports Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();

        //Get our commands json from resources as command data
        for (String json : getCommandsJson(fileNames)) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                    .readValue(json, ApplicationCommandRequest.class);

            commands.add(request); //Add to our array list
        }
    }

    protected void loadUserCommand(String name) {
        loadCommand(name, 2);
    }

    protected void loadMessageCommand(String name) {
        loadCommand(name, 3);
    }

    private void loadCommand(String name, int type) {
        commands.add(ApplicationCommandRequest.builder().name(name).type(type).build());
    }

    protected void registerCommands() {
        // Convenience variables for the sake of easier to read code below
        final ApplicationService applicationService = restClient.getApplicationService();
        final long applicationId = restClient.getApplicationId().block();

        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */
        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
                .doOnNext(cmd -> LOGGER.debug("Successfully registered Global Command " + cmd.name()))
                .doOnError(e -> LOGGER.error("Failed to register global commands", e))
                .subscribe();
    }

    /* The two below methods are boilerplate that can be completely removed when using Spring Boot */

    private static List<String> getCommandsJson(List<String> fileNames) throws IOException {
        // Confirm that the commands folder exists
        URL url = GlobalCommandRegistrar.class.getClassLoader().getResource(commandsFolderName);
        Objects.requireNonNull(url, commandsFolderName + " could not be found");

        //Get all the files inside this folder and return the contents of the files as a list of strings
        List<String> list = new ArrayList<>();
        for (String file : fileNames) {
            String resourceFileAsString = Resources.getResourceFileAsString(commandsFolderName + file);
            list.add(Objects.requireNonNull(resourceFileAsString, "Command file not found: " + file));
        }
        return list;
    }
}