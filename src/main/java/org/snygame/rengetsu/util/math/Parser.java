package org.snygame.rengetsu.util.math;

import org.antlr.v4.runtime.*;
import org.snygame.rengetsu.parser.RengCalcLexer;
import org.snygame.rengetsu.parser.RengCalcParser;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static ParseTree parseCalculation(String query) {
        RengCalcLexer lexer = new RengCalcLexer(CharStreams.fromString(query));
        List<String> errors = new ArrayList<>();
        ANTLRErrorListener listener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add("%d: %s\n".formatted(charPositionInLine, msg));
            }
        };
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        RengCalcParser parser = new RengCalcParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        RengCalcParser.CalculationContext pt = parser.calculation();

        return new ParseTree(pt, errors);
    }

    public static String shortenText(String text) {
        if (text.endsWith("<EOF>")) {
            return text.substring(0, text.length() - 5);
        }
        return text;
    }

    public record ParseTree(RengCalcParser.CalculationContext parseTree, List<String> errors) {}
}
