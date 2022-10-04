package xqd.compiler.core;

// 词法单元
public class Token {
    /**
     *  `1-15 [0x1, 0xF]`表示预设的Token，就是上面提到的那些
     *  `16－511 [0x10,  0x1FF]`表示其他的Token，主要是关键字以及特殊符号
     *  `512－65535 [0x200, 0xFFFF]`表示语法单元（非终结符）
     */
    public short type; // 词法类别
    public String text; // 对应的文本

    public Token(short type, String text) {
        this.type = type;
        this.text = text;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", text='" + text + '\'' +
                '}';
    }
}
