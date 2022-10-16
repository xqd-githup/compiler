package xqd.compiler.core;

import xqd.compiler.core.gm.Produce;
import xqd.compiler.core.gm.Rule;
import xqd.compiler.core.unit.RuleUnit;
import xqd.compiler.core.unit.TermUnit;

import java.nio.ByteBuffer;
import java.util.*;

public class Parser {
    public static final int SubRuleMask = 1 << 6, DiguiRuleMask = 2 << 6;
    Map<Short, Rule> ruleMap = new HashMap<>();
    protected Lexer lexer;
    Token t;
    Map<Short, String> nodeNameMap = new HashMap<>();
    List<Token> tokens = new ArrayList<>();
    int tokenIndex = 0;

    public Parser(String ruleCode, Lexer lexer) {
        this.lexer = lexer;
        byte[] decode = Base64.getDecoder().decode(ruleCode);
        ByteBuffer wrap = ByteBuffer.wrap(decode);
        wrap.position(0);
        while (wrap.hasRemaining()) {
            short ruleId = wrap.getShort();
            Rule rule = new Rule(ruleId, "");
            ruleMap.put(ruleId, rule);
            byte b = wrap.get();
            int flag = b & (3 << 6);
            if (flag == SubRuleMask) {
                rule.isSub = true;
            } else if (flag == DiguiRuleMask) {
                rule.isDigui = true;
            }
            rule.first = new HashSet<>();
            rule.produces.clear();
            b &= (1 << 6) - 1;
            for (int i = 0; i < b; i++) {
                Produce produce = new Produce(ruleId);
                rule.produces.add(produce);
                byte len = wrap.get();
                for (byte m = 0; m < len; m++) {
                    produce.nodes.add(wrap.getShort());
                }
                len = wrap.get();
                for (byte m = 0; m < len; m++) {
                    produce.first.add(wrap.getShort());
                }
                rule.first.addAll(produce.first);
            }
            byte len = wrap.get();
            for (byte m = 0; m < len; m++) {
                rule.follow.add(wrap.getShort());
            }
        }

        Token token = null;
        while ((token = lexer.nextToken()) != null) {
            tokens.add(token);
        }
        tokens.add(new Token(Lexer.EndTerm, ""));
        next();
    }

    protected void setRuleNames(String[] ruleNames) {
        for (int i = 0; i < ruleNames.length; i++) {
            short id = (short) (Lexer.RuleStart + i);
            ruleMap.get(id).name = ruleNames[i];
        }
        for (Rule rule : ruleMap.values()) {
            if (rule.name.length() == 0) {
                if( rule.isSub )
                    rule.name = "subRule@" + rule.id;
                else if( rule.isDigui)
                    rule.name = "digui@" + rule.id;
            }
            nodeNameMap.put(rule.id, rule.name);
        }
        for (int i = 0; i < lexer.constantList().length; i++) {
            nodeNameMap.put((short) (Lexer.StrTermStart + i), "'"+lexer.constantList()[i]+"'");
        }
        String[] defaultSymbol = "ID,STR1,Integer,Decimal,STR2,NewLine,NullTerm,EndTerm".split(",");
        for (int i = 0; i < defaultSymbol.length; i++) {
            nodeNameMap.put((short) (i + 1), defaultSymbol[i]);
        }
        System.out.println(ruleMap);
    }

    void next() {
        t = tokens.get(tokenIndex++);
    }

    Stack<RuleUnit> parent = new Stack<>();
    Stack<Short> matchNode = new Stack<>();

    public RuleUnit parseTree(short r) {
        parent.add(new RuleUnit(null, (short) -1));
        matchNode.add(Lexer.EndTerm);
        parse(r);
        return (RuleUnit) parent.peek().unitList.get(0);
    }

    //
//
    RuleUnit ok(RuleUnit ruleNode) {
        Rule rule = ruleMap.get(ruleNode.id);
        if (!rule.isSub) {
            parent.pop();
        }
        return ruleNode;
    }

    //
//    void showNodeNames(List<Short> nodes) {
//        System.out.println(nodes.stream().map(nodeNameMap::get).collect(Collectors.joining(", ")));
//    }
    Stack<Integer> diguiStack = new Stack<>();

    void parseProduce(List<Short> nodes) {

        for (Short node : nodes) {
//                        System.out.print(nodeNameMap.get(node) + " ");
            if (t.type == Lexer.EndTerm) {
                System.out.println("End");
                return;
            }
            if (node < Lexer.RuleStart) {
                match(node);
                continue;
            }
            Rule rule = ruleMap.get(node);
            if (rule.isDigui) {
                while ((rule.first.contains((short) t.getType()))) {
                    for (int i = 1; i < rule.produces.size(); i++) {
                        Produce produce = rule.produces.get(i);
                        if (produce.first.contains((short) t.getType())) {
                            ArrayList<Short> diguiNodes = new ArrayList<>(produce.nodes);
                            diguiNodes.remove(diguiNodes.size() - 1);

                            int dgId = (i << 16) + node;
                            if (diguiNodes.get(diguiNodes.size() - 1) == parent.peek().id) {
                                if (!diguiStack.empty() && (diguiStack.peek() >> 16) < i && (short) (diguiStack.peek() & 0xffff) == node) {
                                    return;
                                }
                            }
                            diguiStack.add(dgId);
                            RuleUnit ruleNode = parent.pop();
                            RuleUnit ruleNode1 = new RuleUnit(ruleNode.parent, ruleNode.id);
                            ruleNode.parent.getUnitList().set(ruleNode.parent.getUnitList().size() - 1, ruleNode1);
                            ruleNode.parent = ruleNode1;
                            ruleNode1.getUnitList().add(ruleNode);
                            parent.push(ruleNode1);

                            parseProduce(diguiNodes);
                            diguiStack.pop();
                            break;
                        }
                    }
                }
            } else {
                parse(node);
            }
        }
    }

    RuleUnit parse(short r) {
        Rule rl = ruleMap.get(r);
        System.out.print(rl.name + "=>");
        RuleUnit ruleNode = new RuleUnit(parent.peek(), r);

        if (!rl.isSub) {
            parent.peek().getUnitList().add(ruleNode);
            parent.add(ruleNode);
        }

        short nodeid = (short) t.getType();
        if (!rl.first.contains(nodeid)) {
            if (rl.first.contains(Lexer.NullTerm) && rl.follow.contains(nodeid)) {
                return ok(ruleNode);
            } else {
                throw new TokenException("no match");
            }
        }
        for (Produce produce : rl.produces) {
            if (produce.first.contains(nodeid)) {
                int now = tokenIndex;
                try {
//                    ruleNode.setSeq(produce.seq);
                    produce.nodes.stream().map(nodeNameMap::get).forEach(n -> System.out.print(n + " "));
                    System.out.println();
                    parseProduce(produce.nodes);
                    return ok(ruleNode);
                } catch (TokenException e) {
                    tokenIndex = now - 1;
                    next();
                }
            }
        }
        throw new TokenException("no match");

    }

    void match(short node) {
        if (node == t.type) {
//            System.out.println(String.format("match:%s  %s", nodeNameMap.get(node), t.getText()));
            parent.peek().getUnitList().add(new TermUnit(parent.peek(), t));
            next();
        } else {
            throw new TokenException("no match");
        }

    }
}
