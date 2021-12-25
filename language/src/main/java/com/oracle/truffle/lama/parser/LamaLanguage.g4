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
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
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
//import:'import';
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
 (d=definition { body.addAll($d.res); })* e=expression {body.add($e.res);};

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
{$res = new ArrayList<>();}
d=variableDefinitionItem ( ',' d2=variableDefinitionItem ) * {
    if ($d.res != null) { $res.add($d.res); }
    if ($d2.res != null) { $res.add($d2.res); }
};

variableDefinitionItem returns [LamaExpressionNode res] : n=Lident ('=' e=basicExpression)? {
    factory.registerVar($n.getText());
    $res = factory.createAssignment(factory.createStringLiteral($n, false), $e.res);
};

functionDefinition returns [LamaExpressionNode res]: 'public'?  fun n=Lident e='(' args=functionArguments ')' b=functionBody {
    $res = factory.createCall($n.getText(), $b.res, $args.res, $e);
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
    $res = factory.createCall(null, $b.res, $args.res, $b.res);
};

infixHead returns [LamaExpressionNode res]: 'public'? infixity op=infixop level {
    $res = factory.createStringLiteral(op, false);
};


infixity :
    infix
    | infixl
    | infixr;

level : ( at | before | after )? infixop;

expression returns [LamaExpressionNode res]: {
    ArrayList<LamaExpressionNode> data = new ArrayList<>();
    int start = 0;                             // perhaps it's bad
} s=basicExpression {data.add($s.res); start = $s.res.getSourceCharIndex();} ( ';' s2=expression )? {
    if ($s2.res != null) data.add($s2.res);
    $res = factory.consumeBlock(data, start, start*2 + 1);
    data.clear();
    data.add($res);
};

basicExpression returns [LamaExpressionNode res]: d=disjunction {
    $res = $d.res;
};

disjunction returns [LamaExpressionNode res] : c=conjunction {
    $res = $c.res;
} | c=conjunction {$res = $c.res;} ( op='!!' c2=conjunction ) + {
    $res = factory.createBinary($op, $res, $c2.res);
};

conjunction returns [LamaExpressionNode res] : eq=equality {
    $res = $eq.res;
} | eq=equality { $res = $eq.res; } ( op='&&' eq2=equality )+ {
    $res = factory.createBinary($op, $res, $eq2.res);
};

equality returns [LamaExpressionNode res]: c=comparison {
   $res = $c.res;
} | c=comparison {$res = $c.res;} (op=('==' | '!=') t=comparison ) + {
    $res = factory.createBinary($op, $res, $t.res);
};

comparison returns [LamaExpressionNode res]: a=additive {
    $res = $a.res;
} | a=additive {$res = $a.res;} ( op=('<' | '>' | '<=' | '>=') a2=additive ) + {
    $res = factory.createBinary($op, $res, $a2.res);
};

additive returns [LamaExpressionNode res]: m=multiplicative {
    $res = $m.res;
}| m=multiplicative {$res = $m.res;} (op=('+' | '-') m2=multiplicative ) + {
    $res = factory.createBinary($op, $res, $m2.res);
};

multiplicative returns [LamaExpressionNode res]: o=customOperatorExpression {
    $res = $o.res;
} | o=customOperatorExpression {$res = $o.res;} (op=('*' | '/' | '%') o2=customOperatorExpression )+ {
    $res = factory.createBinary($op, $res, $o2.res);
};

customOperatorExpression returns [LamaExpressionNode res]: dt=dotNotation {
    $res = $dt.res;
} | dt=dotNotation {$res = $dt.res;} ( i=infixop dt2=dotNotation ) + {
    $res = factory.createCall(null, $res, $dt2.res.nodes, $dt2.res.end);
};

// here's bug in rules
dotNotation returns [LamaExpressionNode res]: p=postfixExpression? {
    if ($p.res != null) $res = $p.res;
} | a=arrayIndexing {
    $res = $a.res;
}
| (p=postfixExpression? {if ($p.res != null) $res = $p.res;} | a=arrayIndexing) {
    $res = $a.res;
} ( '.' {List<LamaExpressionNode> args = new ArrayList<>(); args.add($res); } (c=functionCall | li=Lident) )+ {
    Token stop = $li;
    if ($c.res != null) {
        args.addAll($c.res.nodes);
        stop = $c.res.nodes.end;
    }
    $res = factory.createCall(factory.createRead(factory.createStringLiteral($li, false)), args, stop);
};


postfixExpression returns [LamaExpressionNode res]:
    p=primary     {$res = $p.res;} |
    c=functionCall {
        $res = $c.res;
    };
//    arrayIndexing;

functionCall returns [LamaExpressionNode res]:
//    postfixExpression '(' ( expression ( ',' expression ) * )? ')' {extends=postfixExpression}; // was
{
    List<LamaExpressionNode> args = new ArrayList<>();
    Token stop;
}
    p=primary { $res = $p.res; } ('[' el=expression ']')? {
        if ($el.res != null) $res = factory.createElem($res, $el.res);
    } '(' ( a=expression {
        args.addAll($a.res);
        stop = $a.res.nodes.end;
    } ( ',' a2=expression ) * )? ')' {
        args.addAll($a2.res);
        stop = $a2.res.nodes.end;
        $res = factory.createCall(factory.createRead(factory.createStringLiteral($p.res, false)), args, stop);
    };

arrayIndexing returns [LamaExpressionNode res]:
    exp=postfixExpression {$res = $exp.res;} '[' in=expression ']' {
        $res = factory.createReference($res, $in.res)
    }; //{extends=postfixExpression};

lazyExpression : lazy basicExpression;

etaExpression : eta basicExpression;



primary returns [LamaExpressionNode res]:
    Decimal     {$res = factory.createNumericLiteral($Decimal, 1);}         |
    '-' Decimal {$res = factory.createNumericLiteral($Decimal, -1);}        |
    String      {$res = factory.createStringLiteral($String, true);}        |
    Char        {$res = factory.createStringLiteral($Char, true);}          |
    i=Lident    {$res = factory.createReference(factory.createStringLiteral($i, false)); }                |
    'true'      {$res = factory.createNumericLiteral(1); }              |
    'false'     {$res = factory.createNumericLiteral(0); }                |
    infix infixop           |
    fun '(' functionArguments ')' functionBody |
    sk='skip'  {$res = factory.createStringLiteral($sk, false);}                  |
    syntaxExpression        |
    '(' s=scopeExpression ')' {$res = $s.res;} |
    lazyExpression          |
    etaExpression           |
    listExpression          |
    arrayExpression         |
    sExpression             |
    iff=ifExpression  {$res = $iff.res;}          |
    wde=whileDoExpression  {$res = $wde.res;}     |
    dw=doWhileExpression  {$res = $dw.res;}     |
    fe=forExpression   {$res = $fe.res;}        |
    caseExpression;
    //{extends=postfixExpression};

listExpression :
    '{' ( expression ( ',' expression ) * )? '}';

arrayExpression :
    '[' ( expression ( ',' expression ) * )? ']';

sExpression :
    Uident ( '(' expression ( ( ',' expression ) * )? ')' )? ;

ifExpression returns [LamaExpressionNode res]:
    st='if' e=expression then b=scopeExpression ( ep=elsePart )? fi {
        $res = factory.createIf($st, $e.res, $b.res, $ep.res);
    };

elsePart returns [LamaExpressionNode res]:
    ei='elif' e=expression then b=scopeExpression ( ep=elsePart )? {
        $res = factory.createIf($ei, $e.res, $b.res, $ep.res);
    }|
    'else' s=scopeExpression {$res = $s.res;};

// todo: they should return LamaExprNode, not statement
whileDoExpression returns [LamaExpressionNode res]: st='while' e=expression 'do' b=scopeExpression od {
    $res = factory.createWhile($st, $e.res, $b.res);
};

doWhileExpression returns [LamaExpressionNode res]: 'do' e=scopeExpression st='while' b=expression od {
    $res = factory.createWhile($st, $e.res, $b.res);
};

forExpression returns [LamaExpressionNode res]:
    st='for' i=scopeExpression ',' inc=expression ',' c=expression 'do' b=scopeExpression od {
        $res = factory.createFor($st, $i.res, $inc.res, $c.res, $b.res);
};

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




