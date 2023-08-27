package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.commands.ReportCommand;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class ReportModal extends InteractionListener.CommandDelegate<ModalSubmitInteractionEvent> {
    public ReportModal(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "report";
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title("Anonymous Complaint Form Response");

        for (int i = 0; i < 4; i++) {
            if (event.getComponents().get(i) instanceof ActionRow row && row.getChildren().get(0) instanceof TextInput input) {
                embed.addField(ReportCommand.QUESTIONS[i], input.getValue().orElse(""), false);
            }
        }
        return event.getInteraction().getChannel().flatMap(c -> c.createMessage(embed.build()))
                .then(event.reply("Your report has been submitted.").withEphemeral(true))
                .onErrorResume(ClientException.class, e -> {
                    System.out.println(e.getClass());
                    return event.reply("Your report failed to be submitted. Please try again.").withEphemeral(true);
                });
    }
}
