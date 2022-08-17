grammar RengCalc;

calculations
 : (tenaryExpression ';')* tenaryExpression ';'? EOF
 ;

tenaryExpression
 : logicOrExpression
 | logicOrExpression '?' tenaryExpression ':' tenaryExpression
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
 : '(' tenaryExpression ')'
 | callExpression
 | IntegerConstant
 | FloatConstant
 | BoolConstant
 | DiceRoll
 ;

callExpression
 : Function '(' parameterList ')'
 ;

parameterList
 : ((tenaryExpression ',')* tenaryExpression)?
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

Digit
 : [0-9]
 ;

BoolConstant
 : 'true'
 | 'false'
 ;

DiceRoll
 : Digit+ 'd' Digit+ ('dl' Digit* ('dh' Digit*)? | 'dh' Digit* ('dl' Digit*)?) 'u'?
 ;

Function
 : [a-zA-Z]+
 ;

WhiteSpaces
 : [ \t\r\n]+ -> skip
 ;