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
            case "prepare" -> {
                return handlePrep(event);
            }
        }
        return Mono.empty();
    }

//    private Mono<Void> handlePrep(ChatInputAutoCompleteEvent event) {
//        return event.getOption("create").map(option -> subCreate(event, option))
//                .or(() -> event.getOption("edit").map(option -> subEdit(event, option)))
//                .or(() -> event.getOption("cast").map(option -> subCast(event, option)))
//                .or(() -> event.getOption("delete").map(option -> subDelete(event, option)))
//                .or(() -> event.getOption("loaded").map(option -> subLoaded(event, option)))
//                .or(() -> event.getOption("show").map(option -> subShow(event, option)))
//                .or(() -> event.getOption("list").map(option -> subList(event, option)))
//                .or(() -> event.getOption("namespace").map(option -> subNamespace(event, option)))
//                .orElse(event.respondWithSuggestions(Collections.emptyList()));
//    }

    private Mono<Void> handlePrep(ChatInputAutoCompleteEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        long userId = event.getInteraction().getUser().getId().asLong();

        String typing = event.getFocusedOption().getValue().map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");

        //TODO NEEDS MORE SOPHISTICATED METHOD
        switch (event.getFocusedOption().getName()) {
            case "namespace" -> {
                try {
                    return event.respondWithSuggestions(prepData.listNamespaces(userId).stream()
                            .filter(data -> data.key().startsWith(typing))
                            .map(data -> ApplicationCommandOptionChoiceData.builder().name(data.key())
                                    .value(data.key()).build()).map(d -> (ApplicationCommandOptionChoiceData)d).toList());
                } catch (SQLException e) {
                    Rengetsu.getLOGGER().error("SQL Error", e);
                    return event.respondWithSuggestions(Collections.emptyList());
                }
            }
            case "key" -> {
                PrepData.QueryResult<List<PrepData.NameData>> result = prepData.listLoadedPrepNames(event.getInteraction().getUser().getId().asLong());
                if (result.retVal() != PrepData.ReturnValue.SUCCESS) {
                    return event.respondWithSuggestions(Collections.emptyList());
                }
                List<PrepData.NameData> datas = result.item();
                return event.respondWithSuggestions(datas.stream().filter(data -> data.key().startsWith(typing))
                        .map(data -> ApplicationCommandOptionChoiceData.builder().name(data.name())
                                .value(data.key()).build()).map(d -> (ApplicationCommandOptionChoiceData)d).toList());
            }
        }
        return event.respondWithSuggestions(Collections.emptyList());
    }
}
