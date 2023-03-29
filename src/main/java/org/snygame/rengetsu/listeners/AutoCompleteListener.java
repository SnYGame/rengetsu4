package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class AutoCompleteListener extends Listener {
    public AutoCompleteListener(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public Mono<Void> handle(ChatInputAutoCompleteEvent event) {
        switch (event.getCommandName()) {
            case "prep" -> {
                return handlePrep(event);
            }
        }
        return Mono.empty();
    }

    private Mono<Void> handlePrep(ChatInputAutoCompleteEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        List<PrepData.NameData> datas;
        try {
            datas = prepData.getAutoCompleteData(event.getInteraction().getUser().getId().asLong());
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.respondWithSuggestions(Collections.emptyList());
        }

        String typing = event.getFocusedOption().getValue().map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");

        return event.respondWithSuggestions(datas.stream().filter(data -> data.key().startsWith(typing))
                .map(data -> ApplicationCommandOptionChoiceData.builder().name(data.name())
                        .value(data.key()).build()).map(d -> (ApplicationCommandOptionChoiceData)d).toList());
    }
}
