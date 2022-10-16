package xqd.compiler.core.unit;

import java.util.ArrayList;
import java.util.List;

public class RuleUnit extends Unit{
    public int seqno;

    public List<Unit> unitList = new ArrayList<>();

    public RuleUnit(RuleUnit parent, short id) {
        super(parent, id);
    }

    public List<Unit> getUnitList() {
        return unitList;
    }
}
