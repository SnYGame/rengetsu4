grammar RengCalc;

calculation
 : ternaryExpression EOF
 ;

ternaryExpression
 : logicOrExpression
 | logicOrExpression '?' ternaryExpression ':' ternaryExpression
 ;

logicOrExpression
 : logicAndExpression
 | logicAndExpression '||' logicOrExpression
 ;

logicAndExpression
 : equalityExpression
 | equalityExpression '&&' logicAndExpression
 ;

equalityExpression
 : comparisonExpression
 | comparisonExpression equalityOp equalityExpression
 ;

equalityOp
 : '=='
 | '!='
 ;

comparisonExpression
 : additiveExpression
 | additiveExpression comparisonOp comparisonExpression
 ;

comparisonOp
 : '<'
 | '<='
 | '>'
 | '>='
 ;

additiveExpression
 : multiplicativeExpression
 | multiplicativeExpression additiveOp additiveExpression
 ;

additiveOp
 : '+'
 | '-'
 ;

multiplicativeExpression
 : unaryExpression
 | unaryExpression multiplicativeOp multiplicativeExpression
 ;

multiplicativeOp
 : '*'
 | '/'
 | '//'
 | '%'
 ;

unaryExpression
 : exponentialExpression
 | unaryOp exponentialExpression
 ;

unaryOp
 : '+'
 | '-'
 | '!'
 ;

exponentialExpression
 : primaryExpression
 | exponentialExpression '^' primaryExpression
 ;

primaryExpression
 : '(' ternaryExpression ')'
 | callExpression
 | DiceRoll
 | IntegerConstant
 | FloatConstant
 | BoolConstant
 ;

callExpression
 : Function '(' parameterList ')'
 ;

parameterList
 : ((ternaryExpression ',')* ternaryExpression)?
 ;

DiceRoll
 : Digit+ 'd' Digit+ ('dl' Digit* ('dh' Digit*)? | 'dh' Digit* ('dl' Digit*)?)? 'u'?
 ;

IntegerConstant
 : DecimalConstant
 | OctalConstant
 | HexadecimalConstant
 | BinaryConstant
 ;

DecimalConstant
 : [1-9] Digit*
 ;

OctalConstant
 : '0' [0-7]*
 ;

HexadecimalConstant
 : '0' [xX] [0-9a-fA-F]+
 ;

BinaryConstant
 : '0' [bB] [0-1]+
 ;

FloatConstant
 : FractionalConstant Exponent?
 | Digit+ Exponent
 ;

FractionalConstant
 : Digit* '.' Digit+
 | Digit+ '.'
 ;

Exponent
 : [eE] [+-] Digit+
 ;

fragment
Digit
 : [0-9]
 ;

BoolConstant
 : 'true'
 | 'false'
 ;

Function
 : [a-zA-Z]+
 ;

WhiteSpaces
 : [ \t\r\n]+ -> skip
 ;