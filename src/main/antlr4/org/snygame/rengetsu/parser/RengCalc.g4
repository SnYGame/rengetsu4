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
 | logicOrExpression '||' logicAndExpression
 ;

logicAndExpression
 : equalityExpression
 | logicAndExpression '&&' equalityExpression
 ;

equalityExpression
 : comparisonExpression
 | equalityExpression equalityOp comparisonExpression
 ;

equalityOp
 : '=='
 | '!='
 ;

comparisonExpression
 : additiveExpression
 | comparisonExpression comparisonOp additiveExpression
 ;

comparisonOp
 : '<'
 | '<='
 | '>'
 | '>='
 ;

additiveExpression
 : multiplicativeExpression
 | additiveExpression additiveOp multiplicativeExpression
 ;

additiveOp
 : '+'
 | '-'
 ;

multiplicativeExpression
 : unaryExpression
 | multiplicativeExpression multiplicativeOp unaryExpression
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
 | primaryExpression '^' exponentialExpression
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
 : Digit+ 'd' Digit+ ('dl' Digit+)? ('dh' Digit*)? 'u'?
 ;

IntegerConstant
 : [1-9] Digit*
 ;

FloatConstant
 : Digit* '.' Digit+
 | Digit+ '.'
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