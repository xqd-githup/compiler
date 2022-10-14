package xqd.compiler.grammar;

import xqd.compiler.core.Lexer;
import xqd.compiler.core.Token;
import xqd.compiler.core.gm.Produce;
import xqd.compiler.core.gm.Rule;

import java.io.StringReader;
import java.util.*;

public class GmParser {
    static String[] defaultSymbol = "ID,STR1,Integer,Decimal,STR2,NewLine,NullTerm,EndTerm".split(",");
    Map<Short, Rule> ruleMap = new HashMap<>();
    GmLexer lexer;
    List<String> termList = new ArrayList<>(), ruleList = new ArrayList<>();
    String startRule;
    List<Token> allTokens = new ArrayList<>();
    short termIndex = Lexer.StrTermStart, ruleIndex = Lexer.RuleStart;
    Map<String, Short> symbolMap = new HashMap<>();
    int ruleSize=0 ;

    public GmParser(GmLexer lexer) {
        this.lexer = lexer;
        short id = 1;
        for (String s : defaultSymbol ) {
            symbolMap.put(s, id++);
        }
    }

    public void parse() {
        // 先扫一遍， 加载所有的符号
        loadAllSymbol();
        ruleSize = ruleIndex;
        // 开始创建Rule以及子Rule
        createRule();
        // 补充NullTerm
        ruleMap.values().forEach(r->{
            for (Produce produce : r.produces) {
                if (produce.nodes.size() == 0) {
                    produce.nodes.add(Lexer.NullTerm);
                }
            }
        });
        printRullMap();

        System.out.println("处理左递归");
        // 左递归处理
        Map<Short, Rule> map = ruleMap;
        ruleMap = new HashMap<>();
        map.values().forEach(r->{
            for (Produce produce : r.produces) {
                if ( produce.nodes.get(0) == r.id) {
                    zuoDiGui(r);
                    return;
                }
            }
        });
        map.putAll(ruleMap);
        ruleMap = map;
        printRullMap();

        // 提取左公因子
        System.out.println("提取左公因子");
         map = ruleMap;
        ruleMap = new HashMap<>();
        map.values().forEach(this::tiqu);
        map.putAll(ruleMap);
        ruleMap = map;
        printRullMap();

    }
    static class TreeNode{
        short value;
        HashMap<Short, TreeNode> childMap = new HashMap<>();

        public TreeNode(short value) {
            this.value = value;
        }
    }
    void tiqu(Rule rule) {
        HashSet<Short> shorts = new HashSet<>();
        rule.produces.forEach(p->shorts.add(p.nodes.get(0)));
        // 所有产生式的第一个元素都不一致，无需提取
        if (shorts.size() == rule.produces.size()) {
            return;
        }

        // 创建前缀树
        TreeNode node = new TreeNode((short) 0);
        TreeNode rootNode = node;
        for (Produce produce : rule.produces) {
            for (Short aShort : produce.nodes) {
                TreeNode treeNode = node.childMap.get(aShort);
                if (treeNode == null) {
                    treeNode = new TreeNode(aShort);
                    node.childMap.put(aShort, treeNode);
                }
                node = treeNode;
            }
            // 尾结点
            node.childMap.put(Lexer.EndTerm, null);
            node = rootNode;
        }

        rule.produces.clear();
        // 遍历前缀树，生成对应的产生式
        for (TreeNode treeNode : rootNode.childMap.values()) {
            Produce produce = new Produce(rule.id);
            rule.produces.add(produce);
            visitTree(treeNode, produce);
        }
    }

    void visitTree(TreeNode node, Produce produce) {
        if (node == null) {
            if (produce.nodes.size() == 0) {
                produce.nodes.add(Lexer.NullTerm);
            }
            return;
        }
        produce.nodes.add(node.value);
        Collection<TreeNode> values = node.childMap.values();
        if( values.size() == 0)
            return;
        else if (values.size() == 1) {
            for (TreeNode value : values) {
                visitTree(value,  produce);
            }
        }else{
            Rule rule = newRule("subRule@");
            rule.name = "subRule@"+rule.id;
            produce.nodes.add(rule.id);
            rule.produces.clear();
            for (TreeNode value : values) {
                produce = new Produce(rule.id);
                rule.produces.add(produce);
                visitTree(value,  produce);
            }
        }
    }

    void loadAllSymbol() {
        Token token ;
        while ((token = lexer.nextToken()) != null) {
            allTokens.add(token);
            switch (token.type) {
                case GmLexer.RuleSplit :
                    Token lastToken = allTokens.get(allTokens.size() - 2);
                    Rule rule = newRule(lastToken.text);
//                    Symbol symbol = new Symbol(ruleIndex++, lastToken.text, true);
                    symbolMap.put(rule.name, rule.id);
                    ruleList.add(rule.name);
                    if (startRule == null) {
                        startRule = rule.name;
                    }
                    break;
                case Lexer.STR1:
                    symbolMap.put(token.text, termIndex++);
                    termList.add(token.text);
                    break;
            }
        }
    }

    // 消除直接左递归，暂不考虑间接左递归
    // 对于  A-> A a | b 替换为下面的式子
    // A-> bA`
    // A`->aA`|ε
    void zuoDiGui(Rule rule) {
        Rule ruleDigui = newRule("digui@");
        ruleDigui.isDigui = true;
        ruleDigui.produces.get(0).nodes.add(Lexer.NullTerm);
        List<Produce> produces = new ArrayList<>();
        Iterator<Produce> iterator1 = rule.produces.iterator();
        while (iterator1.hasNext()) {
            Produce produce = iterator1.next();
            if ( produce.nodes.get(0) == rule.id) {
                iterator1.remove();
                produce.id = ruleDigui.id;
                produce.nodes.remove(0);
                produce.nodes.add(ruleDigui.id);
                ruleDigui.produces.add(produce);
            }else{
                produce.nodes.add(ruleDigui.id);
            }
        }

    }

    public static void main(String[] args) {
        GmLexer lexer = new GmLexer(new StringReader("cal:\n" +
                "    exec| cal exec\n" +
                ";\n" +
                "exec:\n" +
                "    assign '\\n'|'\\n'\n" +
                "    ;\n" +
                "assign:\n" +
                "    ID '=' assign\n" +
                "    | newExp\n" +
                "    ;\n" +
                "newExp:\n" +
                "     '(' newExp ')'\n" +
                "    | prim\n" +
                "    | newExp ('*'|'/') newExp\n" +
                "    | newExp ('+'|'-') newExp\n" +
                "    ;\n" +
                " prim:\n" +
                "    Integer|Decimal|ID"));
        Token token = null;
        new GmParser(lexer).parse();
    }

    Rule newRule(String name) {
        Rule rule = new Rule(ruleIndex++, name);
        if (name.endsWith("@")) {
            rule.name = name + rule.id;
        }
        ruleMap.put(rule.id, rule);
        return rule;
    }
    //产生式
    void createRule() {
        Stack<Produce> produceStack = new Stack<>();
        for (Token token : allTokens) {
            if (produceStack.empty() && token.type == Lexer.ID) {
                Rule rule = ruleMap.get(symbolMap.get(token.text));
                produceStack.add(rule.produces.get(0));
            } else switch (token.type) {
                case GmLexer.SubStart:
                    Rule rule = newRule("subRule@");
                    rule.isSub = true;
                    produceStack.peek().nodes.add(rule.id);
                    produceStack.add(rule.produces.get(0));
                    break;
                case GmLexer.RuleEnd:
                case GmLexer.SubEnd:
                    produceStack.pop();
                    break;
                case GmLexer.Or:
                    Produce pop = produceStack.pop();
                    Produce produce = new Produce(pop.id);
                    ruleMap.get(pop.id).produces.add(produce);
                    produceStack.add(produce);
                    break;
                case GmLexer.STR1:
                case GmLexer.ID:
                    produceStack.peek().nodes.add(symbolMap.get(token.text));
                    break;
                default:
                    break;
            }
        }
    }

    void printRullMap() {
        ruleMap.forEach((k,v)->{
            StringBuilder builder = new StringBuilder(v.name).append("\t=>\t");
            for (Produce produce : v.produces) {
                if (produce.nodes.size() == 0) {
                    builder.append("NullTerm");
                }else for(Short n :produce.nodes){
                    String t ;
                    if (n < Lexer.RuleStart) {
                        if (n < Lexer.StrTermStart) {
                            t = defaultSymbol[n-1];
                        }else{
                            t = "'"+termList.get(n - Lexer.StrTermStart)+"'";
                        }
                    }else{
                        t = ruleMap.get(n).name;
                    }
                    builder.append(t).append("  ");
                }
                builder.append("\n\t\t\t");
            }
            System.out.println(builder);
        });
    }
}