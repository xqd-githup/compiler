package xqd.compiler.grammar;

import xqd.compiler.core.Lexer;
import xqd.compiler.core.Parser;
import xqd.compiler.core.gm.Produce;
import xqd.compiler.core.gm.Rule;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GenCode {
    List<String> termList ;
    Map<Short, Rule> ruleMap;
    String name;
    static Map<String, String> specCharMap = new HashMap<>();
    static {
        try (InputStream stream = GenCode.class.getResourceAsStream("/fy.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line ;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split("\t");
                specCharMap.put(split[0], split[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GenCode(List<String> termList, Map<Short, Rule> ruleMap) {
        this.termList = termList;
        this.ruleMap = ruleMap;
    }

    void gen( String name, String p ) {
        this.name = name;
        Path lex = Paths.get(p, name + "Lexer.java");
        Path pas = Paths.get(p, name + "Parser.java");
        try(BufferedWriter writerLex = Files.newBufferedWriter(lex);
            BufferedWriter writerPas = Files.newBufferedWriter(pas)) {
            createLexer(writerLex);
            createPaser(writerPas);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void createLexer(Writer writer) throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("ClsName", name + "Lexer");
        StringBuilder constStr = new StringBuilder("{"), termId = new StringBuilder();

        int id = Lexer.StrTermStart;
        for (String s : termList) {
            constStr.append("\"").append(s).append("\", ");
            termId.append("T_");
            if (Character.isLetterOrDigit(s.charAt(0))) {
                termId.append(s);
            }else{
                for (char c : s.toCharArray()) {
                    if (Character.isLetterOrDigit(c)) {
                        termId.append(c);
                    }else{
                        termId.append(specCharMap.get(String.valueOf(c)));
                    }
                }
            }
            termId.append(" = 0x").append(Integer.toHexString(id++)).append(", ");
        }
        constStr.deleteCharAt(constStr.length() - 2);
        termId.deleteCharAt(termId.length() - 2);
        constStr.append("}");
        map.put("Constant", constStr.toString());
        map.put("TermId", termId.toString());
        parseTpl("/Lexer.tpl", writer, map);

    }

    void createPaser(Writer writer) throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("ClsName", name + "Parser");
        StringBuilder ruleId = new StringBuilder( ), ruleName = new StringBuilder(), idList = new StringBuilder();

        for (int i = Lexer.RuleStart; i<ruleMap.size()+Lexer.RuleStart; i++) {
            Rule rule = ruleMap.get((short)i);
            if (rule.isDigui || rule.isSub) {
                break;
            }
            ruleId.append("R_").append(rule.name).append(" = 0x").append(Integer.toHexString(rule.id)).append(", ");
            ruleName.append("\"").append(rule.name).append("\", ");
            idList.append("R_").append(rule.name).append(", ");
        }
        ruleId.deleteCharAt(ruleId.length() - 2);
        ruleName.deleteCharAt(ruleName.length() - 2);
        idList.deleteCharAt(idList.length() - 2);
        map.put("RuleId", ruleId.toString());
        map.put("RuleName", ruleName.toString());
        map.put("IdList", idList.toString());

        ByteBuffer allocate = ByteBuffer.allocate(ruleMap.size() * 200);
        for (Rule rule : ruleMap.values()) {
            //name
            allocate.putShort(rule.id);
            byte size = (byte) rule.produces.size();
            if (rule.isSub) {
                size |= Parser.SubRuleMask;
            } else if (rule.isDigui) {
                size |= Parser.DiguiRuleMask;
            }
            allocate.put(size);
            // rule
            AtomicInteger i = new AtomicInteger(0);
            for (Produce produce : rule.produces) {

                if (produce.nodes.size() == 1 && produce.nodes.get(0) == Lexer.NullTerm) {
                    allocate.put((byte) 0);
                }else{
                    allocate.put((byte) produce.nodes.size());
                    for (Short node : produce.nodes) {
                        allocate.putShort(node);
                    }
                }

                // first
                allocate.put((byte) produce.first.size());
                for (Short node : produce.first) {
                    allocate.putShort(node);
                }
            }
            // follow
            allocate.put((byte) rule.follow.size());
            for (Short aShort : rule.follow) {
                allocate.putShort(aShort);
            }
        }
        byte[] bytes = new byte[allocate.position()];
        allocate.position(0);
        allocate.get(bytes);
        String encode = Base64.getEncoder().encodeToString(bytes);
        StringBuilder r = new StringBuilder("\"");
        for (char c : encode.toCharArray()) {
            r.append(c);
            if (r.length() % 60 == 0) {
                r.append("\"\n\t\t\t+ \"");
            }
        }
        r.append("\"");
        allocate.clear();
        map.put("RuleCode", r.toString());

        parseTpl("/Parser.tpl", writer, map);
    }

    void parseTpl(String tpl, Writer writer, Map<String,String> valueMap) throws IOException {
        try (InputStream stream = this.getClass().getResourceAsStream(tpl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line ;
            while ((line = reader.readLine()) != null) {
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (c == '$' && i+1 < line.length()  && line.charAt(i+1) == '{' ) {
                        StringBuilder id = new StringBuilder();
                        for (i+=2; i < line.length() && line.charAt(i) != '}'; i++) {
                            id.append(line.charAt(i));
                        }
                        writer.append(valueMap.getOrDefault(id.toString().trim(), ""));
                    }else{
                        writer.append(c);
                    }
                }
                writer.append("\n");
            }
        }

    }

    void genCode() {
//        PrintStream out = System.out;
//        StringBuilder termIndex = new StringBuilder("public static final short ");
//        StringBuilder strTerm = new StringBuilder("public static final String[] ConstantList = new String[]{ \"");
//        StringBuilder ruleIndex = new StringBuilder("public static final short ");
//        StringBuilder nodeName = new StringBuilder("public static final String NodeName= \"");
//        AtomicInteger ti = new AtomicInteger(0), ri = new AtomicInteger(0);
//        allNodeMap.values().forEach(n->{
//            nodeName.append(n.index).append(":").append(n.name).append(",");
//            if (n.type == NodeType.Rule ) {
//                if( n.index > 0 && n.index < (1<<14)){
//                    ruleIndex.append("Rule_").append(n.name).append(" = ").append(n.index).append(", ");
//                    if (ri.incrementAndGet() % 10 == 0) {
//                        ruleIndex.append("\n");
//                    }
//                }
//            } else if( n.type != NodeType.StrTerm){
//                termIndex.append(n.name).append(" = 0x").append(Integer.toHexString((short)n.index-termMask).toUpperCase()).append(", ");
//                if (ti.incrementAndGet() % 10 == 0) {
//                    termIndex.append("\n");
//                }
//            }
//        });
//        ruleIndex.deleteCharAt(ruleIndex.length()-2).append(';');
//        termIndex.deleteCharAt(termIndex.length()-2).append(';');
//        nodeName.deleteCharAt(nodeName.length()-1).append("\";");
//        strTerm.append(String.join("\" , \"", termList)).append("\" };");
//        out.println(ruleIndex);
//        out.println(termIndex);
//        out.println(strTerm);
//        out.println(nodeName);
//
//
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        ByteBuffer allocate = ByteBuffer.allocate(ruleMap.size() * 200);
//        ruleMap.forEach((k,v)->{
//            //name
//            allocate.putShort((short) allNodeMap.get(k).index);
//            allocate.put((byte) v.size());
//            // rule
//            AtomicInteger i = new AtomicInteger(0);
//            v.forEach(list->{
//                allocate.put((byte) list.size());
//                list.forEach(n->{
//                    allocate.putShort((short) allNodeMap.get(n).index);
//                });
//                // first
//                String key = k + "#tmp#" + i.incrementAndGet();
//                Set<String> strings = firstMap.get(key);
//                allocate.put((byte) strings.size());
//                strings.forEach(n->{
//                    allocate.putShort((short) allNodeMap.get(n).index);
//                });
//            });
//            //follow
//            Set<String> strings = followMap.get(k);
//            allocate.put((byte) strings.size());
//            strings.forEach(n->{
//                allocate.putShort((short) allNodeMap.get(n).index);
//            });
//        });
//        byte[] bytes = new byte[allocate.position()];
//        allocate.position(0);
//        allocate.get(bytes);
//        String encode = Base64.getEncoder().encodeToString(bytes);
//        StringBuilder r = new StringBuilder("\"");
//        for (char c : encode.toCharArray()) {
//            r.append(c);
//            if (r.length() % 60 == 0) {
//                r.append("\"\n\t\t\t+ \"");
//            }
//        }
//        r.append("\"");
//        out.println("public static final String RuleCode = "+r+";");
//        allocate.clear();
    }
}
