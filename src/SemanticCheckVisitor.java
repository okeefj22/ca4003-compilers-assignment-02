import java.util.*;

public class SemanticCheckVisitor implements CCALVisitor {

    private static String scope = "global";
    private static SymbolTable st;

    private static boolean declaredBeforeUsed = true;
    private static boolean correctConstAssign = true;
    private static boolean correctVarAssign = true;
    private static boolean correctArithArgs = true;
    private static boolean correctBoolArgs = true;

    public Object visit(SimpleNode node, Object data) {
        throw new RuntimeException("Visit SimpleNode");
    }

    public Object visit(Prog node, Object data) {
        st = (SymbolTable) data;
        node.childrenAccept(this, data);

        st.checkForDups();
        
        if (declaredBeforeUsed) System.out.printf("Pass: All identifiers declared before use.\n");
        if (correctConstAssign) System.out.printf("Pass: All constants assigned a value of correct type.\n");
        if (correctVarAssign) System.out.printf("Pass: All variables assigned a value of correct type.\n");
        if (correctArithArgs) System.out.printf("Pass: All arithmetic operations are legal.\n");
        if (correctBoolArgs) System.out.printf("Pass: All logical operations are legal.\n");

        return DataType.Prog;
    }

    // TODO this might need to be changed
    public Object visit(VarDecl node, Object data) {
        node.childrenAccept(this, data);
        return DataType.VarDecl;
    }

    // TODO this might be specific to list of decls
    public Object visit(ConstDecl node, Object data) {
        node.childrenAccept(this, data);
        SimpleNode sn = (SimpleNode) node.jjtGetChild(0);
        DataType child1 = (DataType) node.jjtGetChild(1).jjtAccept(this, data);
        DataType child2 = (DataType) node.jjtGetChild(2).jjtAccept(this, data);

        if (child1 != child2) {
            correctConstAssign = false;
            System.out.printf("Error: Const \"%s\" was assigned a value of the incorrect type.\n", sn.jjtGetValue());
            System.out.printf("\tWas expecting \"%s\" but encountered \"%s\".", child1, child2);
        }

        return DataType.ConstDecl;
    }

    public Object visit(ConstAssign node, Object data) {
        DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
        return child1DataType;
    }

    public Object visit(Func node, Object data) {
        node.childrenAccept(this, data);
        return DataType.Func;
    }

    public Object visit(FuncRet node, Object data) {
        node.childrenAccept(this, data);
        scope = "global";
        return DataType.TypeUnknown; // TODO why?
    }
    
    public Object visit(Type node, Object data) {
        String val = (String) node.value;
        if (val.equals("bool")) return DataType.Bool;
        if (val.equals("int")) return DataType.Num;

        return DataType.TypeUnknown;
    }
    
    public Object visit(ParamList node, Object data) {
        node.childrenAccept(this, data);
        return DataType.ParamList;
    }

    public Object visit(Main node, Object data) {
        scope = "main";
        node.childrenAccept(this, data);

        scope = "global";
        return DataType.Main;
    }

    public Object visit(Stm node, Object data) {
        node.childrenAccept(this, data);
        return DataType.Stm;
    }

    public Object visit(Assign node, Object data) {
        if (node != null) { // TODO don't think this is included in my Stm
            String stmType = (String) node.value;
            SimpleNode child1SimpleNode = (SimpleNode) node.jjtGetChild(0);
            String child1DataType = st.typeLookup((String) child1SimpleNode.value, scope);
            String child2DataType = node.jjtGetChild(1).jjtAccept(this, data).toString();

            if (!child1DataType.equals(child2DataType)) {
                correctVarAssign = false;
                System.out.println("Error: Var \"" + child1SimpleNode.jjtGetValue() + "\" was assigned a value of the wrong type.");
                System.out.println("\tWas expecting \"" + child1DataType + "\" but instead encountered \"" + child2DataType + "\"");
            }
        }

        node.childrenAccept(this, data);
        return DataType.Assign;
    }

    private DataType getDataType(SimpleNode node, Object data) {
        DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
        DataType child2DataType = (DataType) node.jjtGetChild(1).jjtAccept(this, data);

        if ((child1DataType == DataType.Num) && (child2DataType == DataType.Num)) return DataType.Num;

        // TODO bad design pattern
        correctArithArgs = false;
        System.out.println("Error: Non numeric types used in arithmetic operation " + node);
        System.out.println("Was expecting two arguments of type number but encountered \"" + child1DataType + "\" and \"" + child2DataType + "\".");
        return DataType.TypeUnknown;
    }

    public Object visit(PlusOp node, Object data) {
        DataType dt = getDataType(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(MinOp node, Object data) {
        DataType dt = getDataType(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(Num node, Object data) {
        return DataType.Num;
    }

    public Object visit(Bool node, Object data) {
        return DataType.Bool;
    }

    // TODO merge with getDataType and handle printing elsewhere
    private DataType getDataType2(SimpleNode node, Object data) {
        DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
        DataType child2DataType = (DataType) node.jjtGetChild(1).jjtAccept(this, data);

        if ((child1DataType == DataType.Bool) && (child2DataType == DataType.Bool)) return DataType.Bool;

        // TODO bad design pattern
        correctBoolArgs = false;
        System.out.println("Error: Non boolean types used in logical comparison " + node);
        System.out.println("Was expecting two arguments of type boolean but encountered \"" + child1DataType + "\" and \"" + child2DataType + "\".");
        return DataType.TypeUnknown;
    }

    public Object visit(OrOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(AndOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(EqOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(NotOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(LtOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(LtEqOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(GtOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(GtEqOp node, Object data) {
        DataType dt = getDataType2(node, data);
        //checkReadVars(node, data);
        return dt;
    }

    public Object visit(ArgList node, Object data) {
        node.childrenAccept(this, data);
        return DataType.ArgList;
    }

    public Object visit(identifier node, Object data) {
        // check if parent is not a declaration
        SimpleNode parent = (SimpleNode) node.jjtGetParent();
        String type = parent.toString();
        if (type != "VarDecl" && type != "ConstDecl" && type != "Func") {
            String val = (String) node.jjtGetValue();
            boolean isInScope = st.inScope(val, scope);
            if (!isInScope) System.out.printf("Error: Identifier \"%s\" not declared in scope.\n", val);
        }

        return DataType.TypeUnknown;
    }
}
