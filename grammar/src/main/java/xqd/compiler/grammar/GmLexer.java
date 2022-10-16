package xqd.compiler.grammar;

import xqd.compiler.core.Lexer;

import java.io.Reader;

public class GmLexer extends Lexer {
    public static final short StrStart = Lexer.StrTermStart;
    public static final short RuleEnd= StrStart, SubStart= StrStart +1,SubEnd= StrStart +2, Or= StrStart +3, RuleSplit = StrStart + 4;
    public static final String[] Constant = {";","(",")","|",":"};
    public GmLexer(Reader reader) {
        super(reader);
    }
    @Override
    public String[] constantList() {
        return Constant;
    }
}
