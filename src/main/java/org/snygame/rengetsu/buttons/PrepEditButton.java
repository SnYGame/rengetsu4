package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.PrepData;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.math.BytecodeGenerator;
import org.snygame.rengetsu.util.math.Type;
import org.snygame.rengetsu.util.math.TypeChecker;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.IntStream;

public class PrepEditButton extends InteractionListener.CommandDelegate<ButtonInteractionEvent> {
    public PrepEditButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "prep";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        PrepData prepData = databaseManager.getPrepData();

        long userId = event.getInteraction().getUser().getId().asLong();

        String[] args = event.getCustomId().split(":");

        PrepData.Data data = prepData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        switch (args[1]) {
            case "edit" -> {
                return event.presentModal("Edit descriptions", "prep:edit:%d".formatted(data.uid), List.of(
                        ActionRow.of(
                                TextInput.small("name", "Name", 0, 100)
                                        .required(true).prefilled(data.name)
                        ),
                        ActionRow.of(
                                TextInput.paragraph("description", "Effect description",
                                                0, 2000).required(false)
                                        .prefilled(data.description)
                        )
                ));
            }
            case "namespace" -> {
                return event.presentModal("Change namespace", "prep:namespace:%d".formatted(data.uid), List.of(
                        ActionRow.of(
                                TextInput.paragraph("namespace", "Namespace",
                                        0, 50).required(false)
                                        .placeholder("Leave blank for default")
                                        .prefilled(data.namespace == null ? "" : data.namespace)
                        )
                ));
            }
            case "params" -> {
                return event.presentModal("Edit parameters", "prep:params:%d".formatted(data.uid), List.of(
                        ActionRow.of(
                                TextInput.small("params", "Parameters (comma separated)", 0, 1000)
                                        .required(false).prefilled(String.join(", ", data.params))
                        )
                ));
            }
            case "add_roll" -> {
                return event.presentModal("Add dice roll", "prep:add_roll:%d".formatted(data.uid), List.of(
                        ActionRow.of(
                                TextInput.small("description", "Description", 0, 100)
                                        .required(false)
                        ),
                        ActionRow.of(
                                TextInput.paragraph("roll", "Dice rolls",
                                        0, 500).required(true)
                        )
                ));
            }
            case "add_calc" -> {
                return event.presentModal("Add calculation", "prep:add_calc:%d".formatted(data.uid), List.of(
                        ActionRow.of(
                                TextInput.small("description", "Description", 0, 100)
                                        .required(false)
                        ),
                        ActionRow.of(
                                TextInput.paragraph("calc", "Calculations",
                                        0, 500).required(true)
                        )
                ));
            }
            case "del_roll" -> {
                EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();

                String rollDesc = "";
                StringJoiner joiner = new StringJoiner("\n");
                for (PrepData.Data.RollData rollData: data.rolls) {
                    if (rollData.description != null) {
                        if (joiner.length() > 0) {
                            embed.addField(rollDesc, joiner.toString(), false);
                            joiner = new StringJoiner("\n");
                        }
                        rollDesc = rollData.description;
                    }
                    if (rollData instanceof PrepData.Data.DiceRollData diceroll && diceroll.variable != null) {
                        joiner.add("%s=%s".formatted(diceroll.variable, rollData.query.replace("*", "\\*")));
                    } else {
                        joiner.add(rollData.query.replace("*", "\\*"));
                    }
                }

                if (joiner.length() > 0) {
                    embed.addField(rollDesc, joiner.toString(), false);
                }

                return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                        .content("")
                        .addEmbed(embed.build())
                        .addComponent(ActionRow.of(
                                SelectMenu.of("prep:del_roll:%d".formatted(data.uid),
                                                IntStream.range(0, data.rolls.size()).mapToObj(i ->
                                                        SelectMenu.Option.of(data.rolls.get(i).query,
                                                                String.valueOf(i))).toList()
                                        ).withMaxValues(data.rolls.size())
                                        .withPlaceholder("Select dice rolls or calculations to remove")
                        ))
                        .addComponent(ActionRow.of(
                                Button.danger("prep:cancel_menu:%d".formatted(data.uid),
                                        "Cancel")))
                        .build());
            }
            case "save" -> {
                TypeChecker typeChecker = new TypeChecker();
                Type.VarType[] paramTypes = typeChecker.addVariables(data.params);

                for (PrepData.Data.RollData rollData: data.rolls) {
                    switch (rollData) {
                        case PrepData.Data.DiceRollData diceRoll -> {
                            if (diceRoll.variable != null) {
                                typeChecker.addVariable(diceRoll.variable, Type.FixedType.NUM);
                            }
                        }
                        case PrepData.Data.CalculationData calculation -> {
                            try {
                                calculation.getAst().accept(typeChecker);
                            } catch (IllegalArgumentException e) {
                                return event.reply("`%s` Type Error: %s\n".formatted(calculation.query,
                                        e.getMessage())).withEphemeral(true);
                            }
                        }
                    }
                }

                BytecodeGenerator bytecodeGenerator = new BytecodeGenerator(data.params);
                for (PrepData.Data.RollData rollData: data.rolls) {
                    switch (rollData) {
                        case PrepData.Data.DiceRollData diceRoll -> {
                            if (diceRoll.variable != null) {
                                diceRoll.result = bytecodeGenerator.getVarIndex(diceRoll.variable);
                            }
                        }
                        case PrepData.Data.CalculationData calculation -> {
                            calculation.bytecode = bytecodeGenerator.generate(calculation.getAst());
                        }
                    }
                }

                data.parameterData.clear();
                for (int i = 0; i < data.params.length; i++) {
                    String param = data.params[i];
                    Type type = paramTypes[i].getInferredType();
                    switch (type) {
                        case Type.FixedType fixedType -> {
                            data.parameterData.add(new PrepData.Data.ParameterData(param, fixedType, (byte) 0,
                                    bytecodeGenerator.getVarIndex(param)));
                        }
                        case Type.VarType varType -> {
                            byte b;
                            for (b = 0; b < data.params.length; b++) {
                                if (data.params[b].equals(varType.getName())) {
                                    break;
                                }
                            }
                            data.parameterData.add(new PrepData.Data.ParameterData(param, Type.FixedType.VAR, b,
                                    bytecodeGenerator.getVarIndex(param)));
                        }
                    }
                }

                data.varCount = bytecodeGenerator.getVarCount();

                PrepData.ReturnValue retVal = prepData.savePrepData(data);
                if (retVal != PrepData.ReturnValue.SUCCESS) {
                    return event.reply("**[Error]** " + retVal.format(data.namespace, data.key)).withEphemeral(true);
                }
                prepData.removeTempData(data);
                return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Data saved for %s".formatted(data.name)).build()
                        ).components(Collections.emptyList()).build());
            }
            case "delete" -> {
                PrepData.ReturnValue retVal = prepData.deletePrepData(data.userId, data.namespace, data.key);
                if (retVal != PrepData.ReturnValue.SUCCESS) {
                    return event.reply("**[Error]** " + retVal.format(data.namespace, data.key)).withEphemeral(true);
                }
                prepData.removeTempData(data);
                return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Deleted %s".formatted(data.name)).build()
                        ).components(Collections.emptyList()).build());
            }
        }

        return event.edit(PrepData.buildMenu(data));
    }
}
