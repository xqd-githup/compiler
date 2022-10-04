package xqd.compiler.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// 词法分析主类
public abstract class Lexer {
    public static final char EOF = 0xFFFF;
    public static final short ID = 1,  //标识符
            STR1 = 2,  // 单引号括起来的字符串常量
            STR2 = 3,  // 双引号括起来的字符串常量
            INT=4,  // 整形常量
            DEC = 5,  // 浮点型常量
            NewLine=6; // 换行
    public static final short ConstantId = 0x10;
    private final Reader reader;


    protected final Map<String, Short> tokenIdMap;
    public final String whitespaceStr ;
    public final Map<String,String > commentMap ;

    private char nextChar , currChar ;
    protected int line = 1, charNum = 0;

    public Lexer(Reader reader) {
        if (reader instanceof BufferedReader)
            this.reader =  reader;
        else
            this.reader = new BufferedReader(reader);
        tokenIdMap = createTokenMap(constantList());
        whitespaceStr = whitespace();
        commentMap = new HashMap<>();
        for (String[] strings : comment()) {
            commentMap.put(strings[0], strings[1]);
        }
        // 预读取， 使currChar指向第一个字符， nextChar指向第二个字符
        next();
        next();
    }

    /**
     * 字符串常量：关键字及特殊字符
     * @return 关键字及特殊字符列表
     */
    public abstract String[] constantList();

    /**
     * 需要忽略的空白字符，如：" \t\n\r"
     * @return 空白字符
     */
    public abstract String whitespace() ;

    /**
     * 注释格式，开始与结束字符串的数组
     * 如：{{"//","\n"}, {"/*", "* /" } }
     * 每个字符串不可以超过两个字符
     *
     * @return  注释格式
     */
    public abstract String[][] comment() ;

    public static Map<String, Short> createTokenMap(String[] strings) {
        HashMap<String, Short> map = new HashMap<>();
        for (short i = 0; i < strings.length; i++) {
            map.put(strings[i], (short) (i +ConstantId));
        }
        return Collections.unmodifiableMap(map);
    }

    private char next()  {
        currChar = nextChar;
        if (currChar == EOF) {
            return EOF;
        }
        try {
            nextChar = (char) reader.read();
            if (nextChar == EOF) {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (currChar == '\n') {
            line++;
            charNum = 0;
        }else{
            charNum += nextChar == EOF ? 0 : 1;
        }
        return currChar;
    }


    public Token nextToken()   {
        for (; ; next()) {
            if( currChar == EOF) return null ;

            String cment = commentMap.get(String.valueOf(currChar));
            if (cment == null) {
                cment = commentMap.get(String.valueOf(currChar) + nextChar);
            }
            // 当期匹配注释格式
            if (cment != null) {
                while (next() != EOF) {
                    if( cment.equals(String.valueOf(currChar))) break;
                    if( cment.equals(String.valueOf(currChar) + nextChar)){
                        next();break;
                    }
                }
                continue;
            }
            // 过滤空白字符
            if (whitespaceStr.indexOf(currChar) >= 0) {
                continue;
            } else if (currChar == '\n') {
                next();
                return new Token(NewLine, "\n");
            } else if (Character.isJavaIdentifierStart(currChar)) {
                StringBuilder stringBuilder = new StringBuilder().append( currChar);
                while (next() != EOF && Character.isJavaIdentifierPart(currChar)) {
                    stringBuilder.append( currChar);
                }
                String id = stringBuilder.toString();
                if (tokenIdMap.containsKey(id)) {
                    return new Token(tokenIdMap.get(id), id);
                }
                return new Token(ID, stringBuilder.toString());
            } else if (Character.isDigit(currChar)) {
                StringBuilder stringBuilder = new StringBuilder().append(currChar);
                while (next() != EOF && Character.isDigit(currChar)) {
                    stringBuilder.append( currChar);
                }
                if (currChar != '.')
                    return new Token(INT, stringBuilder.toString());
                else {
                    stringBuilder.append( currChar);
                    while (next() != EOF && Character.isDigit(currChar)) {
                        stringBuilder.append( currChar);
                    }
                    return new Token(DEC, stringBuilder.toString());
                }
            } else if (currChar == '\'') {
                StringBuilder stringBuilder = new StringBuilder();
                int lastCh = currChar;
                while (next() != EOF) {
                    if (lastCh != '\\' && currChar == '\'') {
                        next();
                        break;
                    }
                    stringBuilder.append( currChar);
                    lastCh = currChar;
                }
                String s = stringBuilder.toString();
                return new Token(STR1, s);
            } else if (currChar == '"') {
                StringBuilder stringBuilder = new StringBuilder();
                int lastCh = currChar;
                while (next() != EOF) {
                    if (lastCh != '\\' && currChar == '"') {
                        next();
                        break;
                    }
                    stringBuilder.append( currChar);
                    lastCh = currChar;
                }
                return new Token(STR2, stringBuilder.toString());
            } else {
                String t = String.valueOf( currChar);
                while (tokenIdMap.containsKey(t) && next() != EOF) {
                    t += String.valueOf( currChar);
                }
                if (t.length() > 1) {
                    t = t.substring(0, t.length() - 1);
                    return new Token(tokenIdMap.get(t), t);
                }
                System.out.println("ERROR:" + t);
            }
        }
    }
}
