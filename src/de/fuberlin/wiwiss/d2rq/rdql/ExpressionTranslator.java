/*
 * Created on 31.03.2005 by Joerg Garbers, FU-Berlin
 *
 */
package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.IndexValues;
import com.hp.hpl.jena.rdql.parser.*;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.rdql.ConstraintHandler.NodeMakerIterator;

/**
 * RDQLExpressionTranslator translates an RDQL expression into a SQL
 * expression that may be weaker than the RDQL expression
 * @author jgarbers
 *
 */
public class ExpressionTranslator {
    ConstraintHandler handler;
    VariableBindings variableBindings;
    Map variableNameToNodeConstraint=new HashMap();
    
    /** indicates that the translated sub-expression should be weaker or stronger
     * than the rdql sub-expression. (switched during negation).
     */
    boolean weaker=true;  
    int argType=BoolType;

    static Map opMap; // rdql operator to sql operator map. null means: no map 
//    static Map argTypeMap; // rdql operator to type map
    
    static int NoType=0;
    static int BoolType=1;
    static int BitType=2;
    static int StringType=4;
    static int NumberType=8;
    static int AnyType=15;
    static int LeftRightType=16; // different type for left and right operand
    
    
    public ExpressionTranslator(ConstraintHandler handler) {
        super();
        this.handler=handler;
        variableBindings=handler.bindings;
        // variableNameToNodes=variableBindings.variableNameToNodeMap;
        setupOperatorMap();
    }
    
    public String translateToString(Expression e) {
        Result r=translate(e);
        if (r==null)
            return null;
        if ((r.getType() & BoolType) != 0)
            return r.getString();
        return null;
    }
    
    /**
     * Translates a Jena RDQL Expression into an SQL expression.
     * We should try to resolve the strongest SQL condition possible.
     * Maybe for each expression we should calculate both the strongest and the weakest
     * condition, so that negation flips the meaning.
     * @param e
     * @return null if no 1:1 translation possible.
     * @see com.hp.hpl.jena.graph.query.Expression
     */
    public Result translate(Expression e) {
        String op=getSQLOp(e); // see at bottom
        if (op==null)
            return null;
        List list=translateArgs(e, true, null);
        if (list==null)
            return null;
        return applyInfix(op,list);        
    }
    
    /**
     * TODO check, if there are problems in SQL when comparing text fields 
     * with numeric values and text values with numeric fields
     * 
     * @param e
     * @return
     */
    public Result translate(ParsedLiteral e) {
        StringBuffer b=new StringBuffer();
        if (e.isInt())
            return newResult(Long.toString(e.getInt()),NumberType);
        else if (e.isDouble())
            return newResult(Double.toString(e.getDouble()),NumberType);
        else if (e.isBoolean())
            return e.getBoolean()? trueBuffer : falseBuffer ;
        // else if (e.isNode())   
        // else if (e.isURI())
        else if (e.isString())
            return newResult("\'"+e.getString()+"\'",StringType);
        else 
            return null;
    }
    
    // the case where "?x=?y" should be handled before triples are checked for shared variables
    /**
     * translate a variable.
     * To translate a variable, we must either resolve its value or a reference to a column
     * @param var
     * @return
     */
    public Result translate(Q_Var var) {
        if (variableBindings.isBound(var.getName()))
            return translateValue(var.eval(null,variableBindings.inputDomain));
        else
            return translateVarName(var.getName());
    }
    public Result translate(Expression.Variable var) {
        return translateVarName(var.getName());
    }
    public Result translate(WorkingVar var) {
        //throw new RuntimeException("WorkingVar in RDQLExpressionTranslator");
        return translateValue(var); 
    }
    
    public Result translateValue(NodeValue val) {
       String valueString=val.valueString();
       // TODO check for correctness of different node values
       return newResult(valueString,NoType);
    }

    public Result translateVarName(String varName) {
        // map var to a column-expression
        // we should choose the one that is most simple
        NodeConstraint c=(NodeConstraint)variableNameToNodeConstraint.get(varName);
        if (c==null) {
            Node n=(Node) variableBindings.variableNameToNodeMap.get(varName);
            c=(NodeConstraint)handler.variableToConstraint.get(n);
            if (c==null) {
                Set varIndexSet=(Set)variableBindings.bindVariableToShared.get(n);
                ConstraintHandler.NodeMakerIterator e=handler.makeNodeMakerIterator(varIndexSet);
                if (e.hasNext()) {
                    NodeMaker m=e.nextNodeMaker();
                    c=new NodeConstraint();
                    m.matchConstraint(c);
                } else
                    return null;
            }
            variableNameToNodeConstraint.put(varName,c);
        }
        return translateNodeConstraint(c);       
    }
    
    public Result translateNodeConstraint(NodeConstraint c) {
        if (!weaker)
            return null;
        if (c.fixedNode!=null) {
            return newResult(c.fixedNode.toString(),NoType);
        }
        // TODO
        return null;
    }
    
    public Result translateNode(Node n) {
        if (n.isLiteral()) {
            // TODO
        }
        return null;
    }
    
    /**
     * creates an sql expression that contains op at infix positions
     * @param op
     * @param args
     * @return
     */
    public Result applyInfix(String op, List args) {
        StringBuffer sb=new StringBuffer("(");
        Iterator it=args.iterator();
        boolean first=true;
        while (it.hasNext()) {
            if (first) 
                first=false;
            else {
                sb.append(' ');
                sb.append(op);
                sb.append(' ');
            }
            String item=(String)it.next();
            sb.append(item);
        }
        sb.append(")");
        return newResult(sb,NoType);
    }
    
    /**
     * creates a unary sql expression
     * @param op
     * @param arg
     * @return
     */
    public Result applyUnary(String op, Result arg) {
        StringBuffer result=new StringBuffer("(");
        result.append(op);
        result.append(" ");
        arg.appendTo(result);
        result.append(")");
        return newResult(result,NoType);
    }
    
    /**
     * translates an RDQL expression
     * @param e the RDQL expression
     * @param strict if set, all arguments must be translatable to create a complex SQL expression
     * @param neutral the neutral element of the current operation
     * @return
     */
    public List translateArgs(Expression e, boolean strict, Result neutral) {
        List list=new ArrayList();
        int count=e.argCount();
        for (int i=0; i<count;i++) {
           Expression ex=e.getArg(i);
           Result exStr=translate(ex);
           if (strict && exStr==null)
               return null;
           if ((exStr!=null) && (exStr!=neutral))
               list.add(exStr);
        }
        return list;
    }
    
    public static Result trueBuffer=newResult("true",BoolType);
    public static Result falseBuffer=newResult("false",BoolType);
        
    public Result translate(Q_LogicalAnd e) {
        List list=translateArgs(e, false, trueBuffer);
        if (list==null)
            return null;
        int count=list.size();
        if (!weaker && (e.argCount()>count)) // less is weaker!
            return null;
        if (count==0)
            return trueBuffer;
        if (count==1)
            return (Result) list.get(0);
        return applyInfix("AND",list);        
    }
    
    public Result translate(Q_LogicalOr e) {
        List list=translateArgs(e, true, falseBuffer);
        if (list==null)
            return null;
        int count=list.size();
        if (weaker && (e.argCount()>count)) // less args is stronger!
            return null;
        if (count==0)
            return falseBuffer;
        if (count==1)
            return (Result) list.get(0);
        return applyInfix("OR",list);        
    }
    
    /**
     * is this really the logical Not?
     * @param e
     * @return
     */
    public Result translate(Q_UnaryNot e) {
        boolean oldWeaker=weaker;
        weaker=!weaker;
        Expression ex=e.getArg(0);
        weaker=oldWeaker;
        Result exStr=translate(ex);
        Result result=null;
        if (exStr!=null) {
            if (exStr==trueBuffer)
                result=falseBuffer;
            else if (exStr==falseBuffer)
                result=trueBuffer;
            else 
                result=applyUnary("NOT",result);
        }
        return result;
    }
    
    public Result translateUnary(Expression e, String sqlOp) {
        Expression ex=e.getArg(0);
        Result exStr=translate(ex);
        if (exStr==null)
            return null;
        Result result=applyUnary(sqlOp,exStr);
        return result;
    }
    public Result translate(Q_UnaryMinus e) {
        return translateUnary(e,"-");
    }
    public Result translate(Q_UnaryPlus e) {
        return translateUnary(e,"-");
    }

    
    /////////////   Massive Mapping /////////////////////////////
    
    // From rdql.parser.ExprNode
    static final String exprBaseURI = "urn:x-jena:expr:" ; 
    String constructURI(String className)
    {
        if ( className.lastIndexOf('.') > -1 )
            className = className.substring(className.lastIndexOf('.')+1) ;
        return exprBaseURI+className ;
    }
    String opURL(String className) {
        return constructURI(className);
    }
    String opURL(Object obj) {
        return constructURI(obj.getClass().getName());
    }
   
    void putOp(String className,String sqlOp) {
        putOp(className,sqlOp,NoType);
    }
    void putOp(String className,String sqlOp, int argTypes) {
        if (sqlOp==null) 
            return;
        String url=opURL(className);
        Operator op=new Operator();
        opMap.put(url,op);
        op.rdqlOperator=url;
        op.sqlOperator=sqlOp;
        op.argTypes=argType;
        if (argTypes>=LeftRightType) {
            op.leftTypes=argTypes/LeftRightType;
            op.rightTypes=argTypes % LeftRightType;
        }
    }
    /*
    Operator getOp(String className) {
        return (Operator)opMap.get(opURL(className));
    }
    Operator getOp(Object obj) {
        return (Operator)opMap.get(opURL(obj));
    }
    */
    String getSQLOp(Expression e) {
        Operator op=(Operator)opMap.get(e.getFun());
        return op.sqlOperator;
    }
    
    void setupOperatorMap() {
        if (opMap==null) {
            opMap=new HashMap();
            putOp("Q_Add","+",NumberType+StringType);
            putOp("Q_BitAnd",null,NumberType);
            putOp("Q_BitOr",null,NumberType); // "||"
            putOp("Q_BitXor",null,NumberType);
            putOp("Q_Divide","/",NumberType);
            putOp("Q_Equal","=",AnyType);
            putOp("Q_GreaterThan",">",NumberType);
            putOp("Q_GreaterThanOrEqual",">=");
            putOp("Q_LeftShift",null,NumberType);
            putOp("Q_LessThan","<");
            putOp("Q_LessThanOrEqual","<=");
            putOp("Q_LogicalAnd","AND");
            putOp("Q_LogicalOr","OR");
            putOp("Q_Modulus",null);
            putOp("Q_Multiply","*");
            putOp("Q_NotEqual","<>");
            putOp("Q_PatternLiteral",null);
            putOp("Q_RightSignedShift",null);
            putOp("Q_RightUnsignedShift",null);
            putOp("Q_StringEqual","=");
            putOp("Q_StringLangEqual",null);
            putOp("Q_StringMatch",null); // "LIKE"
            putOp("Q_StringNoMatch",null); // "NOT LIKE"
            putOp("Q_Subtract","-");
            putOp("Q_UnaryMinus","-");
            putOp("Q_UnaryNot","!");
            putOp("Q_UnaryPlus","+");
        }
    }
    
    class Operator {
        public String rdqlOperator; // unqualified class name
        public String sqlOperator;
        public int argTypes;
        public int leftTypes;
        public int rightTypes;
        public int returnTypes;
    }
    
    static public Result newResult(String expr, int type) {
        return new SQLExpr(expr,type);
        // return new StringResult(expr,type);
    }
    static public Result newResult(StringBuffer expr, int type) {
        return new SQLExpr(expr,type);
        // return new StringBufferResult(expr,type);
    }

    public interface Result {
        public int getType();
        public String getString();
        public StringBuffer getStringBuffer();
        public void appendTo(StringBuffer sb);
    }
}