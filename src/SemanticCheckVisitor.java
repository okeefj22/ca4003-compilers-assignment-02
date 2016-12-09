import java.util.*;

public class SemanticCheckVisitor implements CCALVisitor {

    private static String scope = "global";
    private static Hashtable<String, Integer> functionCalls = new HashTable<>();
    private static Hashtable<String, Integer> functionParams = new HashTable<>();
    private static LinkedList<String> idsWithoutFunc = new LinkedList<>();
    private static boolean idsDeclaredInScope = true;
    private static boolean assignmentTypeCorrect = true;
    private static boolean constAssignmentTypeCorrect = true;
    private static boolean correctBoolean = true;
    private static boolean correctArithmetic = true;
    private static boolean correctRelational = true;
    private static boolean correctEquality = true;
    private static boolean correctFuncParamNum = true;
    private static boolean allFuncsCalled = true;

    public Object visit(SimpleNode node, Object data) {
        throw new RuntimeException("Visit SimpleNode");
    }

    public Object visit(Prog node, Object data) {
        node.childrenAccept(this, data);

        if (idsDeclaredInScope) System.out.println("PASS: Every id was declared in scope before being used.");

        Hashtable symTable = (Hashtable) data;
        Enumeration symTableKeys = symTable.keys();
        boolean multipleDeclarations = false;
        while (symTableKeys.hasMoreElements()) {
            String tmpScope = (String) symTableKeys.nextElement();
            Hashtable inScope = (Hashtable) symTable.get(tmpScope);
            Enumeration inScopeKeys = inScope.keys();
            while (inScopeKeys.hasMoreElements()) {
                String s = (String) inScopeKeys.nextElement();
                STC tmp = (STC) inScope.get(s);

                if (tmp.preDeclared) {
                    multipleDeclarations = true;
                    System.out.println("\tError: ID \"" + tmp.name + "\" declared more than once in scope \"" + tmpScope + "\".");
                }
            }
        }

        if (!multipleDeclarations) System.out.println("PASS: No ID was declared more than once in the same scope.");

        if (assignmentTypeCorrect) System.out.println("PASS: All variables were assigned to a value of the correct type.");

        if (constAssignmentTypeCorrect) System.out.println("PASS: All constants were assigned to a value of the correct type.");

        if (correctArithmetic) System.out.println("PASS: ");
        if (correctArithmetic) System.out.println("");
        if (correctArithmetic) System.out.println("");
        if (correctArithmetic) System.out.println("");
        if (correctArithmetic) System.out.println("");

        if (idWithoutFunc.size() > 0) {
            for (String id: idsWithoutFunc) System.out.println("\tError: No function for invoked id \"" + id + "\"");
        }

        boolean read = true;
        boolean written = true;
        symTableKeys = symTable.keys();
        while (symTableKeys.hasMoreElements()) {
            String smpScope = (String) symTableKeys.nextElement();
            Hashtable inScope = (Hashtable) symTable.get(tmpScope);
            Enumeration inScopeKeys = inScope.keys();
            while (inScopeKeys.hasMoreElements()) {
                String s = (String) inScopeKeys.nextElement();
                STC tmp = (STC) inScope.get(s);

                if (!entry.writtenTo) {
                    written = false;
                    System.out.println("\tWarning: Never wrote to \"" + tmp.name + "\".");
                }

                if (!entry.readFrom) {
                    read = false;
                    System.out.println("\tWarning: Never read from \"" + tmp.name + "\".");
                }
            }
        }

        // if (written) continue;
        // if (read) continue;

        // iterate through the list of functions and check if they were invloked
        // TODO would func be in functionCalls if it hadn't been invoked?
        for (String func : functionCalls.keySet()) {
            if (functionCalls.get(func) == 0) {
                System.out.println("\tWarning: The function \"" + func + "\" was declared but never called.");
                allFunctionsCalled = false;
            }
        }

        //if (allFunctionsCalled) continue;

        return DataType.Prog;
    }

    // TODO this might need to be changed
    public Object visit(VarDecl node, Object data) {
        node.childrenAccept(this, data);
        return DataType.VarDecl;
    }

    public Object visit(ConstDecl node, Object data) {
        node.childrenAccept(this, data);
        int idIndex = 0;
        int typeIndex = 1;
        int valIndex = 2;
        while (valIndex < node.jjtGetNumChildren()) {
            SimpleNode idSn = (SimpleNode) node.jjtGetChild(idIndex);
            DataType child1DataType = (DataType) node.jjtGetChild(typeIndex).jjtAccept(this.data);
            DataType child2DataType = (DataType) node.jjtGetChild(valIndex).jjtAccept(this.data);

            if (child1DataType != child2DataType) {
                constAssignTypeCorrect = false;
                System.out.println("\tError: Const \"" + idSn.jjtGetValue() + "\" was assigned a value of the incorrect type.");
                System.out.println("\t\tWas expecting a \"" + child1DataType + "\" but instead encountered a \"" + child2DataType + "\"");
            }

            writeRead(idSn.jjtGetValue(), data, "write");

            idIndex += 3;
            typeIndex += 3;
            valIndex += 3;
        }

        return DataType.ConstDecl;
    }

    public Object visit(ConstAssign node, Object data) {
        DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
        return child1DataType;
    }

    public Object visit(Func node, Object data) {
        String val = (String) node.value;
        scope = val;

        SimpleNode paramListSn = (SimpleNode) node.jjtGetChild(2);

        functionCalls.put(val, 0);
        funcitonParams.put(val, (paramListSn.jjtGetNumChildren() / 2));

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
        if (val.equals("bool")) return DataType.BoolVal;
        if (val.equals("int")) return DataType.IntVal;

        return DataType.TypeUnknown;
    }
    
    public Object visit(ParamList node, Object data) {
        int numChild = node.jjtGetNumChildren();
        for (int i = 0; i < numChild; i++) {
            if (i % 2 == 0) {
                SimpleNode idSn = (SimpleNode) node.jjtGetChild(i);
                writeRead(idSn.jjtGetValue(), data, "write");
            }
        }

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
        String stmType = (String) node.value;
        if (stmType.equals(":=")) { // TODO don't think this is included in my Stm
            SimpleNode child1SimpleNode = (SimpleNode) node.jjtGetChild(0);
            DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
            DataType child2DataType = (DataType) node.jjtGetChild(1).jjtAccept(this, data);

            if (child1DataType != child2DataType) {
                assignmentTypeCorrect = false;
                System.out.println("\tError: Var \"" + child1SimpleNode.jjtGetValue() + "\" was assigned a value of the wrong type.");
                System.out.println("\t\tWas expecting \"" + child1DataType + "\" but instead encountered \"" + child2DataType + "\"");
            }

            SimpleNode child2SimpleNode = (SimpleNode) node.jjtGetChild(1);
            if (child2SimpleNode.toString().equals("Identifier")) writeRead(child2SimpleNode.jjtGetValue(), data, "read");

            writeRead(child1SimpleNode.jjtGetValue(), data, "write");
        }

        Hashtable symTable = (Hashtable) data;
        int numChild = node.jjtGetNumChildren();
        for (int i = 0; i < numChild; i++) {
            SimpleNode childNode = (SimpleNode) node.jjtGetChild(i);
            if (childNode.toString().equals("ArgList")) {
                SimpleNode idNode = (SimpleNode) node.jjtGetChild(i-1);
                if (!symTable.containsKey(idNode.value)) {
                    String id = (String) idNode.value;
                    idsWithoutFunctions.add(id);
                }

                if (functionParams.containsKey(idNode.value)) {
                    if (functionParams.get(idNode.value) != childNode.jjtGetNumChildren()) {
                        correctFunctionParamNum = false;
                        System.out.println("\tError: Wrong number of arguments passed into function \"" + idNode.value + "\".");
                        System.out.println("\t\tWas expecting " + functionParams.get(idNode.value) + " parameteres but encountered " + childNode.jjtGetNumChildren() + " parameters.");
                    }
                }
            }
        }

        node.childrenAccept(this, data);
        return DataType.Stm;
    }

    private DataType getDataType(SimpleNode node, Object data) {
        DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
        DataType child2DataType = (DataType) node.jjtGetChild(1).jjtAccept(this, data);

        if ((child1DataType == DataType.Number) && (child2DataType == DataType.Number)) return DataType.Num;

        // TODO bad design pattern
        correctArithmetic = false;
        System.out.println("\tError: Non numeric types used in arithmetic operation " + node);
        System.out.println("\t\tWas expecting two arguments of type number but encountered \"" + child1DataType + "\" and \"" + child2DataType + "\".");
        return DataType.TypeUnknown;
    }

    private void checkReadVars(SimpleNode node, Object data) {
        SimpleNode child1SimpleNode = (SimpleNode) node.jjtGetChild(0);
        if (child1SimpleNode.toString().equals("Identifier")) writeRead(child1SimpleNode.jjtGetValue(), data, "read");

        SimpleNode child2SimpleNode = (SimpleNode) node.jjtGetChild(1);
        if (child2SimpleNode.toString().equals("Identifier")) writeRead(child2SimpleNode.jjtGetValue(), data, "read");
    }

    public Object visit(PlusOp node, Object data) {
        DataType dt = getDataType(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(MinOp node, Object data) {
        DataType dt = getDataType(node, data);
        checkReadVars(node, data);
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
        correctBoolean = false;
        System.out.println("\tError: Non boolean types used in logical comparison " + node);
        System.out.println("\t\tWas expecting two arguments of type boolean but encountered \"" + child1DataType + "\" and \"" + child2DataType + "\".");
        return DataType.TypeUnknown;
    }

    public Object visit(OrOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(AndOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(EqOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(NotOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(LtOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(LtEqOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(GtOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(GtEqOp node, Object data) {
        DataType dt = getDataType2(node, data);
        checkReadVars(node, data);
        return dt;
    }

    public Object visit(ArgList node, Object data) {
        node.childrenAccept(this, data);
        return DataType.ArgList;
    }

    public Object visit(identifier node, Object data) {
        Hashtable symTable = (Hashtable) data;
        String value = (String) node.jjtGetValue();

        if (symTable.containsKey(value)) {
            SimpleNode parentNode = (SimpleNode) node.jjtGetParent();
            if (parentNode.toString().equals("Statement")) {
                if (!idsWithoutFunctions.contains(value)) {
                    System.out.println("\tError: ID not declared within scope: " + value);
                    idsDeclaredInScope = false;
                }
                return DataType.TypeUnknown;
            }
        }

        if (entry.type == "int") return DataType.Num;
        if (entry.type == "bool") return DataType.Bool;

        return DataType.TypeUnknown;
    }
}
