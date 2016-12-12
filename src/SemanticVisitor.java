// Author: Ruben Vasconcelos      Date: 04 Dec 2015
//
// Visitor for semantic checks in an abstract syntax tree in the BasicL language

import java.util.*;

public class SemanticVisitor implements BasicLVisitor
{
    private static String scope = "global";
    private static Hashtable<String, Integer> funcs_calls = new Hashtable<>();
    private static Hashtable<String, Integer> funcs_params = new Hashtable<>();
    private static LinkedList<String> ids_without_func = new LinkedList<>();
    private static Boolean all_ids_declared_in_scope = true;
    private static Boolean all_assi_of_correct_type = true;
    private static Boolean all_const_assi_of_correct_type = true;
    private static Boolean all_correct_arithmetic = true;
    private static Boolean all_correct_boolean = true;
    private static Boolean all_correct_relational = true;
    private static Boolean all_correct_equality = true;
    private static Boolean all_correct_func_param_num = true;
    private static boolean called_all_funcs = true;

    public Object visit(SimpleNode node, Object data)
    {
        throw new RuntimeException("Visit SimpleNode");
    }

    public Object visit(ASTProgram node, Object data)
    {
        node.childrenAccept(this, data);

        if(all_ids_declared_in_scope)
            System.out.println("PASS: Every identifier was declared within scope before it was used.");

        Hashtable ST = (Hashtable) data;
        Enumeration st_keys = ST.keys();
        Boolean multi_dec = false;
        while (st_keys.hasMoreElements())
        {
            String tmp_scope = (String)st_keys.nextElement();
            Hashtable in_scope = (Hashtable)ST.get(tmp_scope);
            Enumeration in_scope_keys = in_scope.keys();

            while (in_scope_keys.hasMoreElements())
            {
                String temp = (String)in_scope_keys.nextElement();
                STC entry = (STC)in_scope.get(temp);
                
                if(entry.pre_declared)
                {
                    multi_dec = true;
                    System.out.println("  Error: Id \"" + entry.name + "\" declared more than once in scope \"" + tmp_scope + "\".");
                }
            }
        }

        if(!multi_dec)
            System.out.println("PASS: No identifier was declared more than once in the same scope.");

        if(all_assi_of_correct_type)
            System.out.println("PASS: All variables were assignment a variable of the correct type");

        if(all_const_assi_of_correct_type)
            System.out.println("PASS: All consts were assignment a variable of the correct type.");

        if(all_correct_arithmetic)
            System.out.println("PASS: The arguments of all arithmetic operations were integers.");

        if(all_correct_boolean)
            System.out.println("PASS: The arguments of all boolean operations were booleans.");

        if(all_correct_relational)
            System.out.println("PASS: The arguments of all relational operations were integers");

        if(all_correct_equality)
            System.out.println("PASS: The arguments of all equality operations were of the same type.");

        if(ids_without_func.size() == 0)
            System.out.println("PASS: There is a function for every invoked identifier.");
        else
        {
            for(String id : ids_without_func)
                System.out.println("  Error: No function for invoked id: \"" + id + "\".");
        }

        if(all_correct_func_param_num)
            System.out.println("PASS: All functions were called with the correct number of arguments.");


        Boolean read = true;
        Boolean written = true;
        st_keys = ST.keys();
        while (st_keys.hasMoreElements())
        {
            String tmp_scope = (String)st_keys.nextElement();
            Hashtable in_scope = (Hashtable)ST.get(tmp_scope);
            Enumeration in_scope_keys = in_scope.keys();

            while (in_scope_keys.hasMoreElements())
            {
                String temp = (String)in_scope_keys.nextElement();
                STC entry = (STC)in_scope.get(temp);

                if(!entry.written_to)
                {
                    written = false;
                    System.out.println("  Warning: Never wrote to \"" + entry.name + "\".");
                }
                
                if(!entry.read_from)
                {
                    read = false;
                    System.out.println("  Warning: Never read from \"" + entry.name + "\".");
                }
            }
        }

        if(written)
            System.out.println("PASS: Every variable was written to.");

        if(read)
            System.out.println("PASS: Every variable was read from.");


        /*iterate through the list of functions and 
        check if they were invoked at least once.(or not zero times)*/
        for(String func : funcs_calls.keySet())
            if (funcs_calls.get(func) == 0)
            {
                System.out.println("  Warning: The function \"" + func + "\" was declared but never called.");
                called_all_funcs = false;
            }
        
        if(called_all_funcs)
            System.out.println("PASS: All declared functions were called.");

        return DataType.Program;
    }

    public Object visit(ASTVar_decl node, Object data)
    {
        node.childrenAccept(this, data);
        return DataType.Var_decl;
    }

    public Object visit(ASTCont_decl node, Object data)
    {
        node.childrenAccept(this, data);
        int id_index = 0;
        int type_index = 1;
        int value_index = 2;

        while(value_index < node.jjtGetNumChildren())
        {
            SimpleNode id_sn = (SimpleNode)node.jjtGetChild(id_index);
            DataType c1_dt = (DataType)node.jjtGetChild(type_index).jjtAccept(this, data);
            DataType c2_dt = (DataType)node.jjtGetChild(value_index).jjtAccept(this, data);

            if(c1_dt != c2_dt)
            {
                all_const_assi_of_correct_type = false;
                System.out.println("  Error: Const \"" + id_sn.jjtGetValue() + "\" was assigned a variable with the WRONG type:");
                System.out.println("    Was expecting a \"" + c1_dt + "\" but instead was assigned a \"" + c2_dt + "\"");
            }

            write_read(id_sn.jjtGetValue(), data, "write");

            id_index += 3;
            type_index += 3;
            value_index += 3;
        }

        return DataType.Cont_decl;
    }

    public Object visit(ASTConst_assig node, Object data)
    {
        DataType c1_dt = (DataType)node.jjtGetChild(0).jjtAccept(this, data);
        return c1_dt;
    }

    public Object visit(ASTFunction node, Object data)
    {
        String value = (String)node.value;
        scope = value;

        SimpleNode paramL_sn = (SimpleNode)node.jjtGetChild(2);

        funcs_calls.put(value, 0);
        funcs_params.put(value, paramL_sn.jjtGetNumChildren()/2);

        node.childrenAccept(this, data);
        return DataType.Function;
    }

    public Object visit(ASTFunc_ret node, Object data)
    {
        node.childrenAccept(this, data);
        scope = "global";
        return DataType.TypeUnknown;
    }

    public Object visit(ASTParam_list node, Object data)
    {

        for(int i=0; i < node.jjtGetNumChildren(); i++)
        {
            if(i%2 == 0)
            {
                SimpleNode id_sn = (SimpleNode)node.jjtGetChild(i);
                write_read(id_sn.jjtGetValue(), data, "write");
            }
        }


        node.childrenAccept(this, data);
        return DataType.Param_list;
    }

    public Object visit(ASTType node, Object data)
    {
        String value = (String)node.value;
        if(value.equals("bool"))
            return DataType.Bool_val;
        else if(value.equals("int"))
            return DataType.Number;
        else if(value.equals("void"))
            return DataType.TypeVoid;

        return DataType.TypeUnknown;
    }

    public Object visit(ASTMain_prog node, Object data)
    {
        scope = "main";
        node.childrenAccept(this, data);

        scope = "global";
        return DataType.Main_prog;
    }

    private void write_read(Object var_name, Object data, String operation)
    {
        /*
            look for the variable in the ST.
            if found 
                update that variables written_to or read_from value
            else
                do nothing Because ASTIdentifier will define it as 
                an UNDEFINED VARIABLE 
        */

        Hashtable ST = (Hashtable) data;

        Hashtable in_scope_ST = (Hashtable) ST.get(scope);
        STC entry = (STC)in_scope_ST.get(var_name);

        if(entry == null)
        {
            Hashtable global_scope = (Hashtable) ST.get("global");
            entry = (STC)global_scope.get(var_name);

            if(entry == null)
                return; //do nothing UNDEFINED VARIABLE 
        }

        if(operation.equals("write"))
            entry.write();
        else if(operation.equals("read"))
            entry.read();
    }

    public Object visit(ASTStatement node, Object data)
    {
        String stm_type = (String)node.value;
        if(stm_type.equals(":="))
        {
            SimpleNode c1_sn = (SimpleNode)node.jjtGetChild(0);
            DataType c1_dt = (DataType)node.jjtGetChild(0).jjtAccept(this, data);
            DataType c2_dt = (DataType)node.jjtGetChild(1).jjtAccept(this, data);

            if(c1_dt != c2_dt)
            {
                all_assi_of_correct_type = false;
                System.out.println("  Error: Var \"" + c1_sn.jjtGetValue() + "\" was assigned a variable with the WRONG type:");
                System.out.println("    Was expecting a \"" + c1_dt + "\" but instead was assigned a \"" + c2_dt + "\"");
            }

            SimpleNode c2_sn = (SimpleNode)node.jjtGetChild(1);
            if(c2_sn.toString().equals("Identifier"))
                write_read(c2_sn.jjtGetValue(), data, "read");

            write_read(c1_sn.jjtGetValue(), data, "write");
        }
        Hashtable ST = (Hashtable) data;
        for(int i= 0; i<node.jjtGetNumChildren(); i++)
        {
            SimpleNode c_node = (SimpleNode)node.jjtGetChild(i);
            if(c_node.toString().equals("Arg_list"))
            {
                SimpleNode id_node = (SimpleNode)node.jjtGetChild(i-1);
                if(!ST.containsKey(id_node.value))
                {
                    String id = (String)id_node.value;
                    ids_without_func.add(id);
                }

                if(funcs_params.containsKey(id_node.value))
                {
                    if(funcs_params.get(id_node.value) == c_node.jjtGetNumChildren())
                    {
                        //System.out.println("Fuction \"" + id_node.value + "\" called with the correct number of arguments.");
                    }
                    else
                    {
                        all_correct_func_param_num = false;
                        System.out.println("  Error: Wrong number of arguments passed in to fuction \"" + id_node.value + "\":");
                        System.out.println("    Was expecting \"" + funcs_params.get(id_node.value) + "\" and NOT: \"" + c_node.jjtGetNumChildren() + "\"");
                    }
                }
            }
        }

        node.childrenAccept(this, data);

        return DataType.Statement;
    }

    private DataType arithmeticOP(SimpleNode node, Object data)
    {
        DataType c1_dt = (DataType)node.jjtGetChild(0).jjtAccept(this, data);
        DataType c2_dt = (DataType)node.jjtGetChild(1).jjtAccept(this, data);

        if((c1_dt == DataType.Number) && (c2_dt == DataType.Number))
        {
            //System.out.println("Correct arguments used in arithmetic operation " + node + "");
            return DataType.Number;
        }
        else
        {
            all_correct_arithmetic = false;
            System.out.println("  Error: Incorrect arguments used in arithmetic operation " + node);
            System.out.println("     was expecting 2 arguments of type Number but found: " + c1_dt + " " + c2_dt);
            return DataType.TypeUnknown;
        }
    }

    private void checkReadVars(SimpleNode node, Object data)
    {
        SimpleNode c1_sn = (SimpleNode)node.jjtGetChild(0);
            if(c1_sn.toString().equals("Identifier"))
                write_read(c1_sn.jjtGetValue(), data, "read");

        SimpleNode c2_sn = (SimpleNode)node.jjtGetChild(1);
            if(c2_sn.toString().equals("Identifier"))
                write_read(c2_sn.jjtGetValue(), data, "read");
    }

    public Object visit(ASTAdd_op node, Object data)
    {
        DataType result = arithmeticOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTMin_op node, Object data)
    {
        DataType result = arithmeticOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTMul_op node, Object data)
    {
        DataType result = arithmeticOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTDiv_op node, Object data)
    {
        DataType result = arithmeticOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTBool_val node, Object data)
    {
        return DataType.Bool_val;
    }

    public Object visit(ASTNumber node, Object data)
    {
        return DataType.Number;
    }

    private DataType booleanOP(SimpleNode node, Object data)
    {
        DataType c1_dt = (DataType)node.jjtGetChild(0).jjtAccept(this, data);
        DataType c2_dt = (DataType)node.jjtGetChild(1).jjtAccept(this, data);

        if((c1_dt == DataType.Bool_val) && (c2_dt == DataType.Bool_val))
        {
            //System.out.println("Correct arguments used in arithmetic operation " + node + "");
            return DataType.Bool_val;
        }
        else
        {
            all_correct_boolean = false;
            System.out.println("  Error: Incorrect arguments used in boolean operation " + node);
            System.out.println("    was expecting 2 arguments of type Bool_val but found: " + c1_dt + " " + c2_dt);
            return DataType.TypeUnknown;
        }
    }

    public Object visit(ASTAnd_op node, Object data)
    {
        DataType result = booleanOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTOr_op node, Object data)
    {
        DataType result = booleanOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    private DataType equalityOP(SimpleNode node, Object data)
    {
        DataType c1_dt = (DataType)node.jjtGetChild(0).jjtAccept(this, data);
        DataType c2_dt = (DataType)node.jjtGetChild(1).jjtAccept(this, data);

        if(c1_dt == c2_dt)
        {
            //System.out.println("Correct arguments used in equality operation " + node + "");
            return DataType.Bool_val;
        }
        else
        {
            all_correct_equality = false;
            System.out.println("  Error: Incorrect arguments used in equality operation " + node);
            System.out.println("    was expecting 2 arguments of the same type but found: " + c1_dt + " " + c2_dt);
            return DataType.TypeUnknown;
        }
    }

    public Object visit(ASTEQ_op node, Object data)
    {
        DataType result = equalityOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTNOTEQ_op node, Object data)
    {
        DataType result = equalityOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    private DataType relationalOP(SimpleNode node, Object data)
    {
        DataType c1_dt = (DataType)node.jjtGetChild(0).jjtAccept(this, data);
        DataType c2_dt = (DataType)node.jjtGetChild(1).jjtAccept(this, data);

        if((c1_dt == DataType.Number) && (c2_dt == DataType.Number))
        {
            //System.out.println("Correct arguments used in relational operation " + node + "");
            return DataType.Bool_val;
        }
        else
        {
            all_correct_relational = false;
            System.out.println("  Error: Incorrect arguments used in relational operation " + node);
            System.out.println("    was expecting 2 arguments of type Number but found: " + c1_dt + " " + c2_dt);
            return DataType.TypeUnknown;
        }
    }

    public Object visit(ASTLT_op node, Object data)
    {
        DataType result = relationalOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTGT_op node, Object data)
    {
        DataType result = relationalOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTLET_op node, Object data)
    {
        DataType result = relationalOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTGET_op node, Object data)
    {
        DataType result = relationalOP(node, data);

        checkReadVars(node, data);

        return result;
    }

    public Object visit(ASTArg_list node, Object data)
    {
        node.childrenAccept(this, data);
        return DataType.Arg_list;
    }

    public Object visit(ASTIdentifier node, Object data)
    {
        Hashtable ST = (Hashtable) data;
        String value = (String)node.jjtGetValue();

        if(ST.containsKey(value))
        {
            SimpleNode p_node = (SimpleNode)node.jjtGetParent();
            if(p_node.toString().equals("Statement"))
                if(funcs_calls.containsKey(value))
                    funcs_calls.put(value, (funcs_calls.get(value)+1));

            return DataType.TypeFunc;
        }

        Hashtable in_scope_ST = (Hashtable) ST.get(scope);
        STC entry = (STC)in_scope_ST.get(value);

        if(entry == null)
        {
            Hashtable global_scope = (Hashtable) ST.get("global");
            entry = (STC)global_scope.get(value);

            if(scope.equals("global") || entry == null)
            {
                if(!ids_without_func.contains(value))
                {
                    System.out.println("  Error: Identifier not declared within scope: " + value);
                    all_ids_declared_in_scope = false;
                }
                return DataType.TypeUnknown;
            }
        }

        if (entry.type == "int")
            return DataType.Number;
        else if (entry.type == "bool")
            return DataType.Bool_val;

        return DataType.TypeUnknown;
    }
}