package xqd.compiler.core.gm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Rule {
    public short id;
    public String name;
    public List<Produce> produces = new ArrayList<>();
    public Set<Short> first = new HashSet<>(), follow = new HashSet<>();
    public boolean isDigui = false, isSub = false;
    public Rule(short id, String name) {
        this.id = id;
        this.name = name;
        this.produces.add(new Produce(id));
    }
}
