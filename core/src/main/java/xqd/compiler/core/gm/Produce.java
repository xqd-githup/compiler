package xqd.compiler.core.gm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Produce {
    public short id;
    public List<Short> nodes = new ArrayList<>();
    public Set<Short> first = new HashSet<>();

    public Produce(short ruleId ) {
        this.id = ruleId;
    }
}
