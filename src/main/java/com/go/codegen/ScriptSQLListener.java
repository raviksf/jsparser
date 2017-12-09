/*
 Copyright (C) 2017 Ravinder Krishnaswamy

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice 
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH 
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, 
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS 
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER 
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF 
THIS SOFTWARE.
*/
package com.go.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.biu.parser.sql.PlSqlParser;
import com.go.codegen.ScriptGenerator.GoSqlValidationState;
import com.go.codegen.ScriptGenerator.GoTokenOverrideType;

class ScriptSQLListener extends com.biu.parser.sql.PlSqlParserBaseListener
{
	List<Token> mAllTokens = null;
	
	List<Token> getAllTokens() { return mAllTokens; }
	
	private PriorityQueue<TokenModifier> mTokenOverrideHeap = new PriorityQueue<TokenModifier>();
	PriorityQueue<TokenModifier> tokenOverrideHeap() { return mTokenOverrideHeap; }
	
    class SqlConstructInformation
    {
        ParseTree mNode;

        GoSqlValidationState mState = GoSqlValidationState.BASE;
        
        int mStartTokenId;
        int mEndTokenId;
        
        ArrayList<String> mTables = new ArrayList<String>();
        ArrayList<String> mColumns = new ArrayList<String>();
        ArrayList<String> mIdents = new ArrayList<String>();
        
        void addTable(String table)
        {
        	mTables.add(table);
        }
        
        void addColumn(String column)
        {
        	mColumns.add(column);
        }
        
        void addExpressionIdentifier(String identifier)
        {
        	mIdents.add(identifier);
        }
        
        int numTables()  { return mTables.size();  }
        int numColumns() { return mColumns.size(); }
        int numIdents()  { return mIdents.size();  }
        
        String getTable(int i)      { return mTables.get(i);  }
        String getColumn(int i)     { return mColumns.get(i); }
        String getIdentifier(int i) { return mIdents.get(i);  }

        SqlConstructInformation(ParseTree node, int startTokenId, int endTokenId)
        {
            mNode = node;
            mStartTokenId = startTokenId;
            mEndTokenId = endTokenId;
        }

        int getStartTokenId() { return mStartTokenId; }
        int getEndTokenId()   { return mEndTokenId;   }

        void setState(GoSqlValidationState state) { mState = state; }
        GoSqlValidationState getState() { return mState; }
    }

	
	private Stack<SqlConstructInformation> mArgExpressionStack = new Stack<SqlConstructInformation>();
	private HashMap<Integer, Token> mTerminalTokenMap  = new HashMap<Integer, Token>();
	
    private String mValidateFunctionStatements = "";


    String getValidateFunctionStatements() { return mValidateFunctionStatements; }
	

    // Place holder for sql element renaming. Could even replace by a subquery.
    private String remapIdentifier(String str)
    {
        return "__OG_" + str;
    }

    private void replaceTokensWithString(int startToken, int endToken, String str)
    {
        assert(endToken >= startToken);
        mTokenOverrideHeap.add(new TokenModifier(startToken ,0, GoTokenOverrideType.REPLACE_INCLUSIVE, str));
        for (int ti = startToken+1; ti <= endToken; ti++) {
            mTokenOverrideHeap.add(new TokenModifier(ti ,0, GoTokenOverrideType.REPLACE_INCLUSIVE, ""));
        }
    }


	private void addTerminalTokens(ParseTree root, List<TerminalNode> tlist) 
	{
		if (root instanceof TerminalNode) {
			tlist.add((TerminalNode) root);
			Token t = ((TerminalNode) root).getSymbol();
			mTerminalTokenMap.put(new Integer(t.getTokenIndex()), t);
			return;
		}

		for (int i = 0; i < root.getChildCount(); i++)
			addTerminalTokens(root.getChild(i), tlist);
	}
	  
	private void printContext(String funcName, ParserRuleContext ctx)
	{
		System.out.println("In " + funcName + " Text: " + ctx.getText() + " Start Token Id:" 
					+ Integer.toString(ctx.getStart().getTokenIndex()) 
					+ " End Token Id: " + Integer.toString(ctx.getStop().getTokenIndex()));
	}
	
	public void enterSql_statement(PlSqlParser.Sql_statementContext ctx)
	{
		printContext("enterSql_statement", ctx);

    	List<TerminalNode> tnodeList = new ArrayList<TerminalNode>();
    	addTerminalTokens(ctx, tnodeList);
    	
    	mAllTokens = new ArrayList<Token>();
    	
    	for (TerminalNode tn : tnodeList) {;
    		mAllTokens.add(tn.getSymbol());
    	}
	}
	
	public void exitSql_script(PlSqlParser.Sql_statementContext ctx)
	{
		printContext("exitSql_statement", ctx);
	}
	
	
	private SqlConstructInformation stackTop() 
	{ 
		if (mArgExpressionStack.size() == 0)
			return null;
		else
			return mArgExpressionStack.lastElement(); 
	}
	
	
	public void enterSelect_statement(PlSqlParser.Select_statementContext ctx)
	{
		printContext("enterSelect_statement", ctx);
        
        // Check for stack to be null
        if (stackTop() != null && stackTop().getState() != GoSqlValidationState.BASE && stackTop().getState() != GoSqlValidationState.AT_FROM) {
            // State machine error state
            System.out.println("** invalid sql parser state");
            return;
        }
        mArgExpressionStack.push(new SqlConstructInformation(ctx , ctx.getStart().getTokenIndex(), ctx.getStop().getTokenIndex()));
        stackTop().setState(GoSqlValidationState.AT_SELECT);
	}
	
	public void exitSelect_statement(PlSqlParser.Select_statementContext ctx)
	{
		printContext("exitSelect_statement", ctx);
		assert(stackTop() != null);

        // Generate validation strings for the full script
		if (stackTop() != null) {
			
			String validateColumnList = "_OG_VALIDATE_COLUMN_LIST(__OG_SECURITY_CONTEXT, [";
			for(int i = 0; i < stackTop().numColumns(); i++) {
				if (i > 0)
					validateColumnList += " , ";
				validateColumnList += "'" + stackTop().getColumn(i) + "'";
			}
			validateColumnList += "]);";

			String validateTableList = "_OG_VALIDATE_TABLE_LIST(__OG_SECURITY_CONTEXT, [";
			for(int i = 0; i < stackTop().numTables(); i++) {
				if (i > 0)
					validateTableList += " , ";
				validateTableList += "'" + stackTop().getTable(i) + "'";
			}
			validateTableList += "]);";
			
			String validateExpressionIdentifierList = "_OG_VALIDATE_EXPRESSION_IDENTIFIERS(__OG_SECURITY_CONTEXT, [";
			for(int i = 0; i < stackTop().numIdents(); i++) {
				if (i > 0)
					validateExpressionIdentifierList += " , ";
				validateExpressionIdentifierList += "'" + stackTop().getIdentifier(i) + "'";
			}
			validateExpressionIdentifierList += "]);";
			
            mValidateFunctionStatements += validateColumnList + validateTableList + validateExpressionIdentifierList;

            mArgExpressionStack.pop();
			System.out.println("\n\n" + validateColumnList + "\n" + validateTableList + "\n" + validateExpressionIdentifierList);
		}
		
	}
	
	
	@Override public void enterGeneral_element(PlSqlParser.General_elementContext ctx) 
	{ 
		printContext("enterGeneral_element", ctx);
		
		assert(stackTop() != null);
		if (stackTop() == null)
			return;
		
	   if (stackTop() != null && stackTop().getState() == GoSqlValidationState.AT_SELECT) {

			stackTop().addColumn(ctx.getText());
            replaceTokensWithString(ctx.getStart().getTokenIndex(), ctx.getStop().getTokenIndex(), remapIdentifier(ctx.getText()));

		} else if (stackTop() != null && stackTop().getState() == GoSqlValidationState.AT_WHERE) {

			stackTop().addExpressionIdentifier(ctx.getText());
            replaceTokensWithString(ctx.getStart().getTokenIndex(), ctx.getStop().getTokenIndex(), remapIdentifier(ctx.getText()));

		} else {
            System.out.println("** invalid sql parser state");
            return;
        }
	}
	
	@Override public void enterConstant(PlSqlParser.ConstantContext ctx) 
	{ 
		printContext("enterConstant", ctx);
	}
	
	@Override public void enterTable_element(PlSqlParser.Table_elementContext ctx) 
	{
		printContext("enterTable_element", ctx);
	}
	
	@Override public void enterBind_variable(PlSqlParser.Bind_variableContext ctx) 
	{
		printContext("enterBind_variable", ctx);
		
	}
	
	
	public void enterColumn_alias(PlSqlParser.Column_aliasContext ctx) 
	{ 
		printContext("enterColumn_alias", ctx);
	}

	public void enterIdentifier(PlSqlParser.IdentifierContext ctx) 
	{
		printContext("enterIdentifer", ctx);

		assert(stackTop() != null);
		if (stackTop() == null)
			return;
		
		if (stackTop().getState() == GoSqlValidationState.AT_FROM) {
            stackTop().addTable(ctx.getText());
            replaceTokensWithString(ctx.getStart().getTokenIndex(), ctx.getStop().getTokenIndex(), remapIdentifier(ctx.getText()));
        }
	
	}
	
	public void enterWhere_clause(PlSqlParser.Where_clauseContext ctx) 
	{
		printContext("enterWhere_clause", ctx);
		assert(stackTop() != null);
		if (stackTop() != null && stackTop().getState() == GoSqlValidationState.AT_FROM)
			stackTop().setState(GoSqlValidationState.AT_WHERE);
		else
			System.out.println("** invalid sql parser state");
	}
	
	public void exitWhere_clause(PlSqlParser.Where_clauseContext ctx) 
	{
		printContext("exitWhere_clause", ctx);
	}
	
	public void enterFrom_clause(PlSqlParser.From_clauseContext ctx) 
	{ 
		printContext("enterFrom_clause", ctx);
		assert(stackTop() != null);
		if (stackTop() != null && stackTop().getState() == GoSqlValidationState.AT_SELECT)
			stackTop().setState(GoSqlValidationState.AT_FROM);
		else
			System.out.println("** invalid sql parser state");
	}
	
	
	public void enterExpression(PlSqlParser.ExpressionContext ctx)
	{
		printContext("enterExpression", ctx);
	} 
	
	public void enterRelational_expression(PlSqlParser.Relational_expressionContext ctx) 
	{
		printContext("enterRelational_expression", ctx);
	}
	
	public void enterCompound_expression(PlSqlParser.Compound_expressionContext ctx) 
	{ 
		printContext("enterCompound_expression", ctx);
		
	}
	
	@Override public void enterAtom(PlSqlParser.AtomContext ctx) 
	{ 
		printContext("enterAtom", ctx);
	}
	
}
