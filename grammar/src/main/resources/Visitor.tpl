import xqd.compiler.core.unit.RuleUnit;
import xqd.compiler.core.unit.TermUnit;
import xqd.compiler.core.unit.Unit;

public class ${ClsName}<T> {
    private void visitAll(RuleUnit ruleUnit) {
        for (Unit unit : ruleUnit.unitList) {
            visit(unit);
        }
    }

    public T visitTermUnit(TermUnit termUnit) {
        return null;
    }

<list rule in ruleList>
    public T visit${rule}(RuleUnit ruleUnit) {
        visitAll(ruleUnit);
        return null;
    }
</list>
    public T visit(Unit unit) {
        if (unit instanceof TermUnit) {
            return visitTermUnit((TermUnit) unit);
        }
        RuleUnit ruleUnit = (RuleUnit) unit;
        switch (ruleUnit.id) {
<list rule in  ruleList>
            case ${ParserName}.R_${rule}:
                return visit${rule}(ruleUnit);
</list>
            default:
                return null;
        }
    }
}
