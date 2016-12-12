import java.util.*;

public class SemanticCheckVisitor implements CCALVisitor {

    private static String scope = "global";
    private static Hashtable<String, Integer> functionCalls = new Hashtable<>();
    private static Hashtable<String, Integer> functionParams = new Hashtable<>();
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
                    System.out.println("Error: ID \"" + tmp.name + "\" declared more than once in scope \"" + tmpScope + "\".");
                }
            }
        }

        if (!multipleDeclarations) System.out.println("PASS: No ID was declared more than once in the same scope.");

        if (assignmentTypeCorrect) System.out.println("PASS: All variables were assigned to a value of the correct type.");

        if (constAssignmentTypeCorrect) System.out.println("PASS: All constants were assigned to a value of the correct type.");

        if (correctArithmetic) System.out.println("PASS: ");
        if (correctRelational) System.out.println("");
        if (correctEquality) System.out.println("");
        if (correctFuncParamNum) System.out.println("");
        if (allFuncsCalled) System.out.println("");

        if (idsWithoutFunc.size() > 0) {
            for (String id: idsWithoutFunc) System.out.println("Error: No function for invoked id \"" + id + "\"");
        }

        boolean read = true;
        boolean written = true;
        symTableKeys = symTable.keys();
        while (symTableKeys.hasMoreElements()) {
            String tmpScope = (String) symTableKeys.nextElement();
            Hashtable inScope = (Hashtable) symTable.get(tmpScope);
            Enumeration inScopeKeys = inScope.keys();
            while (inScopeKeys.hasMoreElements()) {
                String s = (String) inScopeKeys.nextElement();
                STC tmp = (STC) inScope.get(s);

                if (!tmp.writtenTo) {
                    written = false;
                    System.out.println("Warning: Never wrote to \"" + tmp.name + "\".");
                }

                if (!tmp.readFrom) {
                    read = false;
                    System.out.println("Warning: Never read from \"" + tmp.name + "\".");
                }
            }
        }

        // if (written) continue;
        // if (read) continue;

        // iterate through the list of functions and check if they were invloked
        // TODO would func be in functionCalls if it hadn't been invoked?
        for (String func : functionCalls.keySet()) {
            if (functionCalls.get(func) == 0) {
                System.out.println("Warning: The function \"" + func + "\" was declared but never called.");
                allFuncsCalled = false;
            }
        }

        //if (allFuncsCalled) continue;

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
            DataType child1DataType = (DataType) node.jjtGetChild(typeIndex).jjtAccept(this, data);
            DataType child2DataType = (DataType) node.jjtGetChild(valIndex).jjtAccept(this, data);

            if (child1DataType != child2DataType) {
                constAssignmentTypeCorrect = false;
                System.out.println("Error: Const \"" + idSn.jjtGetValue() + "\" was assigned a value of the incorrect type.");
                System.out.println("Was expecting a \"" + child1DataType + "\" but instead encountered a \"" + child2DataType + "\"");
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
        functionParams.put(val, (paramListSn.jjtGetNumChildren() / 2));

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
        Hashtable symTable = (Hashtable) data;
        int numChild = node.jjtGetNumChildren();
        for (int i = 0; i < numChild; i++) {
            SimpleNode childNode = (SimpleNode) node.jjtGetChild(i);
            if (childNode.toString().equals("ArgList")) {
                SimpleNode idNode = (SimpleNode) node.jjtGetChild(i-1);
                if (!symTable.containsKey(idNode.value)) {
                    String id = (String) idNode.value;
                    idsWithoutFunc.add(id);
                }

                if (functionParams.containsKey(idNode.value)) {
                    if (functionParams.get(idNode.value) != childNode.jjtGetNumChildren()) {
                        correctFuncParamNum = false;
                        System.out.println("Error: Wrong number of arguments passed into function \"" + idNode.value + "\".");
                        System.out.println("Was expecting " + functionParams.get(idNode.value) + " parameteres but encountered " + childNode.jjtGetNumChildren() + " parameters.");
                    }
                }
            }
        }

        node.childrenAccept(this, data);
        return DataType.Stm;
    }

    public Object visit(Assign node, Object data) {
        if (node != null) { // TODO don't think this is included in my Stm
            String stmType = (String) node.value;
            SimpleNode child1SimpleNode = (SimpleNode) node.jjtGetChild(0);
            DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
            DataType child2DataType = (DataType) node.jjtGetChild(1).jjtAccept(this, data);

            if (child1DataType != child2DataType) {
                assignmentTypeCorrect = false;
                System.out.println("Error: Var \"" + child1SimpleNode.jjtGetValue() + "\" was assigned a value of the wrong type.");
                System.out.println("Was expecting \"" + child1DataType + "\" but instead encountered \"" + child2DataType + "\"");
            }

            SimpleNode child2SimpleNode = (SimpleNode) node.jjtGetChild(1);
            if (child2SimpleNode.toString().equals("Identifier")) writeRead(child2SimpleNode.jjtGetValue(), data, "read");

            writeRead(child1SimpleNode.jjtGetValue(), data, "write");
        }

        node.childrenAccept(this, data);
        return DataType.Assign;
    }

    private DataType getDataType(SimpleNode node, Object data) {
        DataType child1DataType = (DataType) node.jjtGetChild(0).jjtAccept(this, data);
        DataType child2DataType = (DataType) node.jjtGetChild(1).jjtAccept(this, data);

        if ((child1DataType == DataType.Num) && (child2DataType == DataType.Num)) return DataType.Num;

        // TODO bad design pattern
        correctArithmetic = false;
        System.out.println("Error: Non numeric types used in arithmetic operation " + node);
        System.out.println("Was expecting two arguments of type number but encountered \"" + child1DataType + "\" and \"" + child2DataType + "\".");
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
        System.out.println("Error: Non boolean types used in logical comparison " + node);
        System.out.println("Was expecting two arguments of type boolean but encountered \"" + child1DataType + "\" and \"" + child2DataType + "\".");
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
                if (functionCalls.containsKey(value)) {
                    functionCalls.put(value, (functionCalls.get(value) + 1));
                }
                return DataType.Func;
            }
        }

        Hashtable inScope = (Hashtable) symTable.get(scope);
        STC tmp = (STC) inScope.get(value);
        if (tmp == null) {
            Hashtable globalScope = (Hashtable) symTable.get("global");
            tmp = (STC) globalScope.get(value);
            if (scope.equals("global") || tmp == null) {
                if (!idsWithoutFunc.contains(value)) {
                    System.out.println("Error: Identifier not declared within scope: " + value);
                    idsDeclaredInScope = false;
                }
                return DataType.TypeUnknown;
            }
        }

        if (tmp.type == "int") return DataType.Num;
        if (tmp.type == "bool") return DataType.Bool;

        return DataType.TypeUnknown;
    }

    private void writeRead(Object varName, Object data, String operation) {
        Hashtable symTable = (Hashtable) data;

        Hashtable inScope = (Hashtable) symTable.get(scope);
        STC tmp = (STC) inScope.get(varName);
        if (tmp == null) {
            Hashtable globalScope = (Hashtable) symTable.get("global");
            tmp = (STC) globalScope.get(varName);
            if (tmp == null) return; // undefined var
        }

        if (operation.equals("write")) tmp.write();
        else if (operation.equals("read")) tmp.read();
    }
}
