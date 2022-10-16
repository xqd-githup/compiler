package xqd.compiler.core.unit;

import xqd.compiler.core.Token;

public class TermUnit extends Unit{
    public String text;
    public TermUnit(RuleUnit parent, Token t) {
        super(parent, t.type);
        this.text = t.text;
    }
}
