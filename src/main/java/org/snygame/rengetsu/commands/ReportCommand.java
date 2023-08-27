package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class ReportCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public static final String[] QUESTIONS = {
            "Who or what do you have an issue with?",
            "Tell us about your issue.",
            "Include relevant message(s)",
            "Do you have any suggestions for this?"
    };

    public ReportCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "report";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.presentModal().withCustomId("report").withTitle("Anonymous Complaint Form").withComponents(
                ActionRow.of(TextInput.small("q1", QUESTIONS[0], 0, 1024).required(true)),
                ActionRow.of(TextInput.small("q2", QUESTIONS[1], 0, 1024).required(true)),
                ActionRow.of(TextInput.paragraph("q3", QUESTIONS[2], 0, 1024).placeholder(
                                "Can be links to a messages or screenshots, or just type out the messages so we can search for them.")
                        .required(false)),
                ActionRow.of(TextInput.small("q4", QUESTIONS[3], 0, 1024).required(false))
        );
    }
}
