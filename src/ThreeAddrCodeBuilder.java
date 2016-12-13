public class ThreeAddrCodeBuilder implements CCALVisitor { 
    private static int labelCount = 1;
    private static int tmpCount = 1;

    private static boolean labelOnLine = false;

    private void printLabel(String label) {
        System.out.printf("%-10s", label + ":");
        labelOnLine = true;
                labelCount++;
    }

    private void printInstruction(String instruction) {
        if (labelOnLine) {
            System.out.println(instruction);
            labelOnLine = false;
        }
        else System.out.printf("%10s\n", instruction);
    }

    public Object visit(SimpleNode node, Object data) {
        throw new RuntimeException("Visit SimpleNode");
    }

    public Object visit(Prog node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    public Object visit(VarDecl node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    public Object visit(ConstDecl node, Object data) {
        String id = (String) node.jjtGetChild(0).jjtAccept(this, data);
        String val = (String) node.jjtGetChild(2).jjtAccept(this, data);

        printInstruction(id + " " + val);

        return null;
    }

    public Object visit(ConstAssign node, Object data) {
        return node.value + " " + ((String) node.jjtGetChild(0).jjtAccept(this, data));
    }

    public Object visit(Func node, Object data) {
        printLabel((String) node.value);
        node.childrenAccept(this, data);
        return null;
    }

    // TODO check this
    public Object visit(FuncRet node, Object data) {
        String in = "return " + ((String) node.jjtGetChild(0).jjtAccept(this, data));
        printInstruction(in);
        return (Object) in;
    }

    /*
    public Object visit(Expr node, Object data) {
        return null;
    }
    */

    public Object visit(Type node, Object data) {
        return node.value;
    }

    public Object visit(ParamList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    public Object visit(Main node, Object data) {
        printLabel("main");
        node.childrenAccept(this, data);
        return null;
    }

    public Object visit(Stm node, Object data) {
        String stm = (String) node.value;
        String beginLabel;
        String condition;
        switch (stm) {
            case "while":
                beginLabel = "L" + labelCount;
                printLabel(beginLabel);
                condition = (String) node.jjtGetChild(0).jjtAccept(this, data);
                printInstruction("ifFalse " + condition + " goto " + beginLabel);

                node.jjtGetChild(1).jjtAccept(this, data);

                return null;

            case "if": // TODO
                beginLabel = "L" + labelCount;
                String elseLabel = "L" + (labelCount + 1);
                printLabel(beginLabel);
                condition = (String) node.jjtGetChild(0).jjtAccept(this, data);
                printInstruction("ifFalse " + condition + " goto " + elseLabel);

                node.jjtGetChild(1).jjtAccept(this, data);
                printLabel(elseLabel);

                return null;

            default:
                return null;
        }
    }

    public Object visit(Assign node, Object data) {
        String child1 = (String) node.jjtGetChild(0).jjtAccept(this, data);
        String child2 = (String) node.jjtGetChild(1).jjtAccept(this, data);

        String in = child1 + " " + node.value + " " + child2;
        printInstruction(in);

        return (Object) in;
    }

    private Object visitOpHelper(SimpleNode node, Object data) {
        String child1 = (String) node.jjtGetChild(0).jjtAccept(this, data);
        String child2 = (String) node.jjtGetChild(1).jjtAccept(this, data);

        String tmp = "t" + tmpCount;
        printInstruction(tmp + " = " + child1 + " " + node.value + " " + child2);
        return tmp;
    }

    public Object visit(PlusOp node, Object data) {
        return visitOpHelper(node, data);
    }

    public Object visit(MinOp node, Object data) {
        return visitOpHelper(node, data);
    }

    public Object visit(Num node, Object data) {
        return node.value;
    }

    public Object visit(Bool node, Object data) {


        return visitOpHelper(node, data);
    }

    public Object visit(OrOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(AndOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(EqOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(NotOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(LtOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(LtEqOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(GtOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(GtEqOp node, Object data) {

        return visitOpHelper(node, data);
    }

    public Object visit(ArgList node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            printInstruction("param " + ((String) node.jjtGetChild(i).jjtAccept(this, data)));
        }
        return (Object) 1;
    }

    public Object visit(identifier node, Object data) {
        return node.value;
    }
}
