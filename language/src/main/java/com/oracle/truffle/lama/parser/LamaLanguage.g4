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
import com.oracle.truffle.lama.nodes.*;
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
    return parser.compilationUnit().res;
}

}
compilationUnit returns [SLRootNode res]
: importRule * scopeExpression EOF;

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





importRule : 'import' Uident ';';

scopeExpression returns [LamaExpressionNode res]
 :
 { factory.enterScope();
   List<LamaExpressionNode> body = new ArrayList<>(); }
 (d=definition { body.addAll($d.res); })* e=expression {body.addAll($e.res);};

definition returns [List<LamaExpressionNode> res]:
    v=variableDefinition {$res = $v.res;}
    | f=functionDefinition {
        $res = new ArrayList<>();
        $res.add($f.res);
    }
    | i=infixDefinition {
        $res = new ArrayList<>();
        $res.add($i.res);
    };

variableDefinition returns [List<LamaExpressionNode> res]: ( var | 'public' ) vd=variableDefinitionSeq ';' {
    $res = $vd.res;
};

variableDefinitionSeq returns [List<LamaExpressionNode> res]:
{$res = newArrayList<>();}
d=variableDefinitionItem ( ',' d2=variableDefinitionItem ) * {
    if ($d.res != null) { $res.add($d.res); }
    if ($d2.res != null) { $res.add($d2.res); }
};

variableDefinitionItem returns [LamaExpressionNode res] : n=Lident ('=' e=basicExpression)? {
    factory.registerVariable($n.getText());
    $res = factory.createAssignment(factory.createStringLiteral($n, false), $e.res);
};

functionDefinition returns [LamaExpressionNode res]: 'public'?  fun n=Lident '(' args=functionArguments ')' b=functionBody {
    $res = factory.createCall($n, $args.res, $b.res);
};

functionArguments returns [List<LamaExpressionNode> res]: {$res = new ArrayList<>();} ( a=functionArgument ( ',' a2=functionArgument ) * )? {
    if ($a.res != null) { $res.add($a.res); }
    if ($a2.res != null) { $res.add($a2.res); }
};

functionArgument returns [LamaExpressionNode res]: p=pattern {
    if ($p.res != null) {
        $res = $p.res;
    }
};

functionBody returns [LamaExpressionNode res]: '{' s=scopeExpression '}' {$res = $s.res;};

infixDefinition returns [LamaExpressionNode res]: n=infixHead '(' args=functionArguments ')' b=functionBody {
    $res = factory.createCall($n.res, $args.res, $b.res);
};

infixHead returns [LamaExpressionNode res]: 'public'? infixity op=infixop level {
    $res = factory.createStringLiteral(op, false);
};


infixity :
    infix
    | infixl
    | infixr;

level : ( at | before | after )? infixop;

expression returns [List<LamaExpressionNode> res]: {
    $res = new ArrayList<>();
} s=basicExpression ( ';' s2=expression )? {
    $res.addAll($s.res);
    if ($s2.res != null) $res.addAll($s2.res);
};

basicExpression returns [List<LamaExpressionNode> res]: d=disjunction {
    $res = $d.res;
};

disjunction returns [List<LamaExpressionNode> res] : {
    $res = new ArrayList<>();
}
c=conjunction {
  $res.addAll($c.res);
}( '!!' c2=conjunction ) * {
   if ($c2.res != null) {
     $res.addAll($c2.res);
   }
};

conjunction returns [List<LamaExpressionNode> res] : {
    $res = new ArrayList<>();
}
eq=equality {
    $res.addAll($eq.res);
} ( '&&' eq2=equality ) * {
  if ($eq2.res != null) {
    $res.addAll($eq2.res);
  }
};

equality returns [List<LamaExpressionNode> res]: comparison (('==' | '!=') comparison ) * ;

comparison : additive ( ('<' | '>' | '<=' | '>=') additive ) * ;

additive : multiplicative ( ('+' | '-') multiplicative ) *;

multiplicative : customOperatorExpression ( ('*' | '/' | '%') customOperatorExpression ) * ;

customOperatorExpression : dotNotation ( infixop dotNotation ) * ;

// here's bug
dotNotation : (postfixExpression? | arrayIndexing) ( '.' (functionCall | Lident) ) * ;


postfixExpression :
    primary      |
    functionCall;
//    arrayIndexing;

functionCall :
//    postfixExpression '(' ( expression ( ',' expression ) * )? ')' {extends=postfixExpression}; // was
    primary ('[' expression ']')? '(' ( expression ( ',' expression ) * )? ')' ;

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

pattern returns [LamaExpressionNode res]:
    cp=consPattern { if ($cp.res != null) $res = $cp.res;}
    | sp=simplePattern { if ($sp.res != null) $res = $sp.res;};
// todo: tmp null
consPattern returns [LamaExpressionNode res]: simplePattern  ':' pattern {return null;};

simplePattern returns [LamaExpressionNode res]:
    wildcardPattern
    | sExprPattern
    | arrayPattern
    | listPattern
    | l=Lident ( '@' s=pattern )? {
        $res = factory.createStringLiteral($l, false);
    }
    | ( '-' )? d=Decimal {
        $res = factory.createNumericLiteral($d);
    }
    | p=String {
        $res = factory.createStringLiteral($p, false);
    }
    | p=Char {
        $res = factory.createStringLiteral($p, false);
    }
    | p='true' {
        $res = factory.createNumericLiteral($p, true);
    }
    | p='false' {
        $res = factory.createNumericLiteral($p, false);
    }
    | sharp a=box /*{
        $res = factory.createStringLiteral($a, false);
    }*/
    | sharp b=val /*{
        $res = factory.createStringLiteral($b, false);
    }*/
    | sharp c=str /*{
         $res = factory.createStringLiteral($c, false);
    }*/
    | sharp f=array /*{
        $res = factory.createStringLiteral($f, false);
    }*/
    | sharp g=sexp /*{
        $res = factory.createStringLiteral($g, false);
    }*/
    | sharp z=fun /*{
        $res = factory.createStringLiteral($z, false);
    }*/
    | '(' s=pattern ')' {$res = $s.res;};

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




