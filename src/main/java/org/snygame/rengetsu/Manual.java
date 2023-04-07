package org.snygame.rengetsu;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.JacksonResources;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.util.Resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Manual {
    private final ArrayList<Page> pages = new ArrayList<>();
    private final ArrayList<Jump> jumps = new ArrayList<>();

    public Manual(String index) throws IOException {
        String[] files = Resources.getResourceFileAsString("manual/%s".formatted(index)).split(",");
        JacksonResources d4jMapper = JacksonResources.create();
        ObjectMapper objectMapper = d4jMapper.getObjectMapper();
        for (String filename: files) {
            String json = Resources.getResourceFileAsString("manual/%s".formatted(filename));
            Section section = objectMapper.readValue(json, Section.class);
            jumps.add(new Jump(section.name, pages.size()));
            pages.addAll(section.pages);
        }
    }

    public InteractionApplicationCommandCallbackSpec getPage(long userId, int index) {
        Page page = pages.get(index);
        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        builder.embeds(List.of(EmbedCreateSpec.builder().title(page.title).description(page.subtitle == null ? "" : page.subtitle).fields(
                page.fields.stream().map(field -> EmbedCreateFields.Field.of(field.name, field.value, false)).toList()
        ).footer("Page %d of %d".formatted(index + 1, pages.size()), null).build()));
        builder.components(List.of(
                ActionRow.of(
                        Button.primary("manual:%d:%d".formatted(userId, index - 1), "Prev page").disabled(index == 0),
                        Button.primary("manual:%d:%d".formatted(userId, index + 1), "Next page").disabled(index == pages.size() - 1)
                ),
                ActionRow.of(
                        SelectMenu.of("manual:%d".formatted(userId), jumps.stream().map(jump -> SelectMenu.Option.of(jump.name, String.valueOf(jump.page))).toList())
                                .withPlaceholder("Jump to section")
                )
        ));
        return builder.build();
    }

    private record Jump(String name, int page) {}
    private record Section(String name, List<Page> pages) {}
    private record Page(String title, String subtitle, List<Field> fields) {}
    private record Field(String name, String value) {}
}
