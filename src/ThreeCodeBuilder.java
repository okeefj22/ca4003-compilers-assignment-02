// Author: Ruben Vasconcelos      Date: 09 Dec 2015
//
// Visitor for building the 3-address code for the BasicL language

import java.io.*;
import java.util.*;

public class ThreeCodeBuilder implements BasicLVisitor
{
    private static PrintWriter writer;
    private static int tmp_num = 1;
    private static int label_num = 1;

    public Object visit(SimpleNode node, Object data)
    {
        throw new RuntimeException("Visit SimpleNode");
    }

    public Object visit(ASTProgram node, Object data)
    {
        try
        {
            writer = new PrintWriter("ThrAddCode.ir", "UTF-8");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        node.childrenAccept(this, data);

        writer.close();
        return DataType.TypeUnknown;
    }

    public Object visit(ASTVar_decl node, Object data)
    {
        node.childrenAccept(this, data);
        return DataType.TypeUnknown;
    }

    public Object visit(ASTCont_decl node, Object data)
    {
        int assi_index = 2;

        while(assi_index < node.jjtGetNumChildren())
        {
            String id  = (String)node.jjtGetChild(0).jjtAccept(this, data);
            String assi = (String)node.jjtGetChild(2).jjtAccept(this, data);

            String entry = "    " + id + " " + assi;
            writer.println(entry);

            assi_index += 3;
        }

        writer.println("    goto Main");

        return DataType.TypeUnknown;
    }

    public Object visit(ASTConst_assig node, Object data)
    {
        String c1 = (String)node.jjtGetChild(0).jjtAccept(this, data);
        return node.value + " " + c1;
    }

    public Object visit(ASTFunction node, Object data)
    {
        writer.println("func " + node.value);
        node.childrenAccept(this, data);
        return DataType.TypeUnknown;
    }

    public Object visit(ASTFunc_ret node, Object data)
    {
        String c1 = (String)node.jjtGetChild(0).jjtAccept(this, data);
        String entry = "    return " + c1;
        writer.println(entry);
        return (Object)entry;
    }

    public Object visit(ASTParam_list node, Object data)
    {
        node.childrenAccept(this, data);
        return DataType.Param_list;
    }

    public Object visit(ASTType node, Object data)
    {
        return node.value;
    }

    public Object visit(ASTMain_prog node, Object data)
    {
        writer.println("");
        writer.println("Main:");
        node.childrenAccept(this, data);
        return DataType.TypeUnknown;
    }

    public Object visit(ASTStatement node, Object data)
    {
        String stm_type = (String)node.value;
        if(stm_type.equals(":="))
        {
            String c1 = (String)node.jjtGetChild(0).jjtAccept(this, data);
            String c2 = (String)node.jjtGetChild(1).jjtAccept(this, data);

            String entry = "    " + c1 + " " + node.value + " " + c2;

            writer.println(entry);

            return (Object)entry;
        }
        else if(stm_type.equals("while"))
        {
            String c1 = (String)node.jjtGetChild(0).jjtAccept(this, data);


            String loop_start = "LB" + label_num;
            writer.println("LB" + label_num + ":");
            label_num++;
            String entry = "    if " + c1 + " goto LB" + label_num;
            writer.println(entry);
            label_num++;
            writer.println("    goto LB" + label_num);
            writer.println("LB" + (label_num-1) + ":");

            int while_end = label_num;
            label_num++;

            node.jjtGetChild(1).jjtAccept(this, data);

            writer.println("    goto " + loop_start);
            writer.println("LB" + while_end + ":");

            return (Object)entry;
        }
        else if(stm_type.equals("if"))
        {
            SimpleNode c_node = (SimpleNode)node.jjtGetChild(0);

            int index = 0;

            if(c_node.toString().equals("Identifier"))
            {
                Integer args = (Integer)node.jjtGetChild(1).jjtAccept(this, data);
                String entry = "    tmp" + tmp_num + " := call " + c_node.value + ", " + args;
                int else_lbl = label_num;
                String entry2 = "    ifFalse tmp" + tmp_num + " goto LB" + label_num;
                label_num++;

                tmp_num++;

                writer.println(entry);
                writer.println(entry2);

                node.jjtGetChild(2).jjtAccept(this, data);
                writer.println("    goto LB" + label_num);
                int if_end = label_num;
                label_num++;

                writer.println("LB" + else_lbl + ":");
                node.jjtGetChild(3).jjtAccept(this, data);
                writer.println("LB" + if_end + ":");


                return DataType.TypeUnknown;
            }
            else
            {
                String c1 = (String)node.jjtGetChild(0).jjtAccept(this, data);
                int else_lbl = label_num;
                String entry2 = "    ifFalse " + c1 + " goto LB" + label_num;
                writer.println(entry2);
                label_num++;

                node.jjtGetChild(1).jjtAccept(this, data);
                writer.println("    goto LB" + label_num);
                int if_end = label_num;
                label_num++;

                writer.println("LB" + else_lbl + ":");
                node.jjtGetChild(2).jjtAccept(this, data);
                writer.println("LB" + if_end + ":");

                return DataType.TypeUnknown;
            }
        }
        else if(stm_type.equals("proc_call"))
        {
            SimpleNode c_node = (SimpleNode)node.jjtGetChild(0);
            Integer args = (Integer)node.jjtGetChild(1).jjtAccept(this, data);
            String entry = "    call " + c_node.value + ", " + args;
            writer.println(entry);
            return DataType.TypeUnknown;
        }

        node.childrenAccept(this, data);
        return DataType.TypeUnknown;
    }

    private Object visitOpChild(SimpleNode node, Object data)
    {
        String c1 = (String)node.jjtGetChild(0).jjtAccept(this, data);
        String c2 = (String)node.jjtGetChild(1).jjtAccept(this, data);

        String tmp = "t" + tmp_num;
        tmp_num++;

        String entry = "    " + tmp + " := " + c1 + " " + node.value + " " + c2;
        writer.println(entry);

        return (Object)tmp;
    }
    
    public Object visit(ASTAdd_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTMin_op node, Object data)
    {

        return visitOpChild(node, data);
    }

    public Object visit(ASTMul_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTDiv_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTBool_val node, Object data)
    {
        return node.value;
    }

    public Object visit(ASTNumber node, Object data)
    {
        return node.value;
    }

    public Object visit(ASTAnd_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTOr_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTEQ_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTNOTEQ_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTLT_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTGT_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTLET_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTGET_op node, Object data)
    {
        return visitOpChild(node, data);
    }

    public Object visit(ASTArg_list node, Object data)
    {
        int i = 0;
        while(i < node.jjtGetNumChildren())
        {
            String c_val = (String)node.jjtGetChild(i).jjtAccept(this, data);

            String entry = "    param " + c_val;
            writer.println(entry);
            i++;
        }

        return (Object)i;
    }

    public Object visit(ASTIdentifier node, Object data)
    {
        return node.value;
    }
}