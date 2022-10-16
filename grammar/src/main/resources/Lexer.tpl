import xqd.compiler.core.Lexer;
import java.io.Reader;

public class ${ClsName} extends Lexer {
    public static final short ${TermId};
    public static final String[] Constant = ${Constant};
    public ${ClsName}(Reader reader) {
        super(reader);
    }

    @Override
    public String[] constantList() {
        return Constant;
    }

}