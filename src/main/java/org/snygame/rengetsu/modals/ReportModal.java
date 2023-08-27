package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionPresentModalMono;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.AllowedMentions;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class ReportModal extends InteractionListener.CommandDelegate<ModalSubmitInteractionEvent> {
    public static final String[] QUESTIONS = {
            "Who or what do you have an issue with?",
            "Tell us about your issue.",
            "Include relevant message(s)",
            "Do you have any suggestions for this?"
    };

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
                embed.addField(QUESTIONS[i], input.getValue().orElse(""), false);
            }
        }
        return event.getInteraction().getChannel().flatMap(c -> c.createMessage(embed.build()).withAllowedMentions(AllowedMentions.suppressAll()))
                .then(event.reply("Your report has been submitted.").withEphemeral(true))
                .onErrorResume(ClientException.class, e -> {
                    System.out.println(e.getClass());
                    return event.reply("Your report failed to be submitted. Please try again.").withEphemeral(true);
                });
    }

    public static InteractionPresentModalMono createModal(DeferrableInteractionEvent event, String answer1Default, String answer3Default) {
        return event.presentModal().withCustomId("report").withTitle("Anonymous Complaint Form").withComponents(
                ActionRow.of(TextInput.small("q1", ReportModal.QUESTIONS[0], 0, 1024).required(true)
                        .prefilled(answer1Default)),
                ActionRow.of(TextInput.small("q2", ReportModal.QUESTIONS[1], 0, 1024).required(true)),
                ActionRow.of(TextInput.paragraph("q3", ReportModal.QUESTIONS[2], 0, 1024).placeholder(
                                "Can be links to a messages or screenshots, or just type out the messages so we can search for them.")
                        .required(false).prefilled(answer3Default)),
                ActionRow.of(TextInput.small("q4", ReportModal.QUESTIONS[3], 0, 1024).required(false))
        );
    }
}
