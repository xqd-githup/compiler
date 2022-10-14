package xqd.compiler.grammar;

import xqd.compiler.core.Lexer;

import java.io.Reader;

public class GmLexer extends Lexer {
    public static final short XXX = 0x10-1;
    public static final short RuleEnd= XXX + 1, SubStart=XXX +2,SubEnd=XXX +3, Or=XXX +4, RuleSplit = XXX + 5;
    public GmLexer(Reader reader) {
        super(reader);
    }

    @Override
    public String[] constantList() {
        return  new String[]{";","(",")","|",":"};
    }

}
