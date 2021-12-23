grammar LamaLanguage;

@lexer::header {
}

@parser::header {
import java.util.*;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.lama.LamaLanguage;
import com.oracle.truffle.lama.parser.LamaNodeFactory;
import com.oracle.truffle.lama.nodes.SLRootNode;
}

@parser::members {
private LamaNodeFactory factory;
private Source source;


public static SLRootNode parseLama(LamaLanguage language, Source source) {
    LamaLanguageLexer lexer = new LamaLanguageLexer(CharStreams.fromString(source.getCharacters().toString()));
    LamaLanguageParser parser = new LamaLanguageParser(new CommonTokenStream(lexer));
    lexer.removeErrorListeners();
    parser.removeErrorListeners();
    parser.factory = new LamaNodeFactory();
    parser.source = source;
    return parser.compilationUnit();
}

}

after:'after';
esac:'esac';
infixl:'infixl';
syntax:'syntax';
array:'array';
eta:'eta';
infixr:'infixr';
then:'then';
at:'at';
lazy:'lazy';
before:'before';
fi:'fi';
od:'od';
val:'val';
box:'box';
of:'of';
var:'var';
fun:'fun';
sexp:'sexp';
elif:'elif';
//import:'import';
skip:'skip';
infix:'infix';
str:'str';

// operators
hence:'->';
alt:'|';
sharp:'#';

// brackets
lb:'(';
rb:')';
lcurly:'{';
rcurly:'}';
lsquare:'[';
rsquare:']';

/*// Section 2.1.1
// orig
space:'regexp:\s+';
singleComment:'regexp:--[^\r\n]*';
multiComment:'regexp:\(\*(.|\n)*\*\)';

// Section 2.1.2
// orig
uident:'regexp:[A-Z][a-zA-Z_0-9]*';
lident:'regexp:[a-z][a-zA-Z_0-9]*';
decimal:'regexp:[0-9]+';
string:'regexp:\"([^\"]|\"\")*\"';
//char:'regexp:([^']|''|\\n|\\t)';*/

infixop:'regexp:[+*/%$#@!|&\^?<>:=\\-]+'; // todo


Space : [ \t\r\n\u000C]+ -> skip;
SingleComment : '//' ~[\r\n]* -> skip;
MultiComment : '(*' .*? '*)' -> skip;

fragment LETTER : [A-Z] | [a-z] | '_' | '$';
fragment DIGIT : [0-9];
fragment STRING_CHAR : ~('"' | '\t' | '\n');


Lident : LETTER | (LETTER | DIGIT)*;
Uident : LETTER | (LETTER | DIGIT)*;
Decimal : '0' | DIGIT*;
String : '"' STRING_CHAR* '"';
Char : ('^') | STRING_CHAR;




compilationUnit : importRule * scopeExpression;

importRule : 'import' Uident ';';

scopeExpression : definition* expression ;

definition :
    variableDefinition
    | functionDefinition
    | infixDefinition;

variableDefinition : ( var | 'public' ) variableDefinitionSeq ';';

variableDefinitionSeq : variableDefinitionItem ( ',' variableDefinitionItem ) * ;

variableDefinitionItem : Lident ('=' basicExpression)? ;

functionDefinition : 'public'?  fun Lident '(' functionArguments ')' functionBody;

functionArguments : ( functionArgument ( ',' functionArgument ) * )?;

functionArgument : pattern;

functionBody : '{' scopeExpression '}';

infixDefinition : infixHead '(' functionArguments ')' functionBody;

infixHead : 'public'? infixity infixop level;


infixity :
    infix
    | infixl
    | infixr;

level : ( at | before | after )? infixop;

expression : basicExpression ( ';' expression )?;

basicExpression : disjunction;

disjunction : conjunction ( '!!' conjunction ) *;

conjunction : equality ( '&&' equality ) * ;

equality : comparison (('==' | '!=') comparison ) * ;

comparison : additive ( ('<' | '>' | '<=' | '>=') additive ) * ;

additive : multiplicative ( ('+' | '-') multiplicative ) *;

multiplicative : customOperatorExpression ( ('*' | '/' | '%') customOperatorExpression ) * ;

customOperatorExpression : dotNotation ( infixop dotNotation ) * ;

// here's bug
dotNotation : ((primary | functionCall)? | arrayIndexing) ( '.' (functionCall | Lident) ) * ;


postfixExpression :
    primary      |
    functionCall;
//    arrayIndexing;

functionCall :
//    postfixExpression '(' ( expression ( ',' expression ) * )? ')' {extends=postfixExpression}; // was
    primary '(' ( expression ( ',' expression ) * )? ')' ;

arrayIndexing :
    postfixExpression '[' expression ']'; //{extends=postfixExpression};

lazyExpression : lazy basicExpression;

etaExpression : eta basicExpression;



primary :
    '-' ? Decimal           |
    String                  |
    Char                    |
    Lident                  |
    'true'                    |
    'false'                   |
    infix infixop           |
    fun '(' functionArguments ')' functionBody |
    skip                    |
    syntaxExpression        |
    '(' scopeExpression ')' |
    lazyExpression          |
    etaExpression           |
    listExpression          |
    arrayExpression         |
    sExpression             |
    ifExpression            |
    whileDoExpression       |
    doWhileExpression       |
    forExpression           |
    caseExpression;
    //{extends=postfixExpression};

listExpression :
    '{' ( expression ( ',' expression ) * )? '}';

arrayExpression :
    '[' ( expression ( ',' expression ) * )? ']';

sExpression :
    Uident ( '(' expression ( ( ',' expression ) * )? ')' )? ;

ifExpression :
    'if' expression then scopeExpression ( elsePart )? fi;

elsePart :
    elif expression then scopeExpression ( elsePart )? |
    'else' scopeExpression;

whileDoExpression : 'while' expression 'do' scopeExpression od;

doWhileExpression : 'do' scopeExpression 'while' expression od;

forExpression :
    'for' scopeExpression ',' expression ',' expression 'do' scopeExpression od;

caseExpression : 'case' expression of caseBranches esac;

caseBranches : caseBranch ( ( alt caseBranch ) * )?;

caseBranch : pattern hence scopeExpression;

pattern :
    consPattern
    | simplePattern;

consPattern : simplePattern  ':' pattern;

simplePattern :
    wildcardPattern
    | sExprPattern
    | arrayPattern
    | listPattern
    | Lident ( '@' pattern )?
    | ( '-' )? Decimal
    | String
    | Char
    | 'true'
    | 'false'
    | sharp box
    | sharp val
    | sharp str
    | sharp array
    | sharp sexp
    | sharp fun
    | '(' pattern ')';

wildcardPattern : '_';

sExprPattern : Uident ( '(' pattern ( ',' pattern ) * ')' )? ;

arrayPattern : '[' ( pattern ( ',' pattern ) * )? ']';

listPattern : '{' ( pattern ( ',' pattern ) * )? '}';

syntaxExpression : syntax '(' syntaxSeq (alt syntaxSeq ) * ')';

syntaxSeq : syntaxBinding + ( '{' expression '}' )?;

syntaxBinding : ( '-' )? ( pattern '=' )? syntaxPostfix;

syntaxPostfix : syntaxPrimary ( ('*' | '+' | '?' ) )?;

syntaxPrimary :
    Lident ( '[' ( expression ( ',' expression ) * )? ']' ) *
    | '(' syntaxExpression ')'
    | '$(' expression ')';




