import xqd.compiler.core.Lexer;
import xqd.compiler.core.Parser;

public class ${ClsName} extends Parser {
    public static final short ${RuleId};
    public static final String[] RuleNames = {${RuleName}};
    public static final short[] Rules =  {${IdList}};
    public static final String RuleCode =
			${RuleCode};

    public ${ClsName}( Lexer lexer) {
        super(RuleCode, lexer);
		this.setRuleNames(RuleNames);
    }

}