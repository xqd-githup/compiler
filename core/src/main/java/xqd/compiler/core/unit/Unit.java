package xqd.compiler.core.unit;

public class Unit {
    public RuleUnit parent;
    public short id;

    public Unit(RuleUnit parent, short id) {
        this.parent = parent;
        this.id = id;
    }
}
