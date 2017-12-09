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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.biu.parser.js.ECMAScriptLexer;
import com.biu.parser.js.ECMAScriptParser;
import com.go.codegen.ScriptGenerator.GoTokenOverrideType;
import com.go.codegen.ScriptGenerator.GoTokenType;
import com.go.codegen.ScriptGenerator.GoValidationState;

class ScriptJSListener extends com.biu.parser.js.ECMAScriptBaseListener
{
	  
	/**
	 * 
	 */
	private ScriptGenerator scriptGenerator;

	/**
	 * @param scriptGenerator
	 */
	ScriptJSListener(ScriptGenerator scriptGenerator) {
		this.scriptGenerator = scriptGenerator;
	}




	List<Token> mAllTokens = null;
	GoValidationState mState = GoValidationState.BASE;
	
	List<Token> getAllTokens() { return mAllTokens; }
	
	class ArgumentExpressionInformation {
		
		ParseTree mNode;
		
		ArrayList<String>      mArguments = new ArrayList<String>();
		ArrayList<GoTokenType> mTokenTypes = new ArrayList<GoTokenType>();
		
		GoValidationState      mState = GoValidationState.BASE;
		
		int mStartTokenId;
		int mEndTokenId;
		
		void addArgument(String arg, GoTokenType ttype)
		{
			mArguments.add(arg);
			mTokenTypes.add(ttype);
		}
		
		ArgumentExpressionInformation(ParseTree node, int startTokenId, int endTokenId) {
			mNode         = node;
			mStartTokenId = startTokenId;
			mEndTokenId   = endTokenId;
		}
		
        int getStartTokenId() { return mStartTokenId; }
        int getEndTokenId()   { return mEndTokenId;   }

		void setState(GoValidationState state) { mState = state; }
		
		GoValidationState getState() { return mState; }
		
		int numArg() { return mArguments.size(); }
		
		String getArg(int i)          { return mArguments.get(i);  }
		GoTokenType getArgType(int i) { return mTokenTypes.get(i); }
	}
	
	
	private Stack<ArgumentExpressionInformation> mArgExpressionStack = new Stack<ArgumentExpressionInformation>();
	private PriorityQueue<TokenModifier>         mTokenOverrideHeap = new PriorityQueue<TokenModifier>();
	private HashMap<Integer, Token>              mTerminalTokenMap  = new HashMap<Integer, Token>();
	

	// Capture function arguments
	boolean mInFirstFunction   = false;
	boolean mInFirstFunctionDeclaration  = false;
	boolean mInFirstFunctionBody    = false;
	boolean mInFirstFormalParamList = false;
	
	
	ArrayList<String>  mContextArgNames = new ArrayList<String>();
	String  mTriggerFunctionName = null;
	int mTriggerFunctionTokenId = 0;
	
	
    PriorityQueue<TokenModifier> tokenOverrideHeap() { return mTokenOverrideHeap; }

	
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
	  
    @Override public void enterProgram(ECMAScriptParser.ProgramContext ctx)
	{
    	List<TerminalNode> tnodeList = new ArrayList<TerminalNode>();
    	addTerminalTokens(ctx, tnodeList);
    	
    	for (TerminalNode t : tnodeList) {
    		System.out.println("Terminal " + Integer.toString(t.getSymbol().getTokenIndex()) 
    		+ " Text: " + t.getSymbol().getText());
    		
    	}
    	mAllTokens = new ArrayList<Token>();
    	
    	for (TerminalNode tn : tnodeList) {;
    		mAllTokens.add(tn.getSymbol());
    	}
	}

    
	@Override public void enterFunctionDeclaration(ECMAScriptParser.FunctionDeclarationContext ctx) 
    { 
		if (!mInFirstFunctionDeclaration) {

			System.out.println("Function Decl: ");
			List<TerminalNode> tks = ctx.getTokens(ECMAScriptLexer.Identifier);
			mTriggerFunctionTokenId = tks.get(0).getSymbol().getTokenIndex();
			for (TerminalNode tn : tks)  {
				System.out.println(Integer.toString(tn.getSymbol().getTokenIndex()) + " " + tn.getText());
			}
			
			System.out.println(" First Function Name: " +  ctx.getChild(1).getText());
			mTriggerFunctionName = ctx.getChild(1).getText();
			mInFirstFunctionDeclaration = true;
		}
    }
	

	@Override public void enterFormalParameterList(ECMAScriptParser.FormalParameterListContext ctx) 
	{ 
		if (!mInFirstFormalParamList) {
			
			// Eliminate all the arguments to the outermost function
			
			List<TerminalNode> identifierTokens = ctx.getTokens(ECMAScriptLexer.Identifier);
			for (TerminalNode tn : identifierTokens) {
				mContextArgNames.add(tn.getText());
				mTokenOverrideHeap.add(new TokenModifier(tn.getSymbol().getTokenIndex(), 
						0, GoTokenOverrideType.REPLACE_INCLUSIVE, ""));
			}
			List<TerminalNode> commaTokens = ctx.getTokens(ECMAScriptLexer.Comma);
			
 			for (TerminalNode tn : commaTokens) {
				mTokenOverrideHeap.add(new TokenModifier(tn.getSymbol().getTokenIndex(), 
						0, GoTokenOverrideType.REPLACE_INCLUSIVE, ""));
			}
 			
 			// Add the only argument
			mTokenOverrideHeap.add(new TokenModifier(ctx.getStart().getTokenIndex(), -3, 
					GoTokenOverrideType.INSERT_BEFORE, "__GoV8"));
			
			mInFirstFormalParamList = true;
		}
	}
	
	
	@Override public void enterFunctionBody(ECMAScriptParser.FunctionBodyContext ctx) 
	{ 
		if (!mInFirstFunctionBody) {
			mTokenOverrideHeap.add(new TokenModifier(this.mTriggerFunctionTokenId, 0, GoTokenOverrideType.REPLACE_INCLUSIVE, this.mTriggerFunctionName + "_" + this.mContextArgNames.get(0)));
			
			String triggerInfoParams = "[";
			boolean notFirstArg = false;
			for (String str : mContextArgNames) {
				if (notFirstArg) {
					triggerInfoParams += ", ";
				} else {
					notFirstArg = true;
				}
				triggerInfoParams += "'" + str + "'";
			}
			triggerInfoParams += "]";
			mTokenOverrideHeap.add(new TokenModifier(ctx.getStart().getTokenIndex(), -3, GoTokenOverrideType.INSERT_BEFORE, " _GO_VALIDATE(__GoV8, " + triggerInfoParams + ");"));
			mInFirstFormalParamList = true;
		}
	}
	
	
	@Override public void enterFunctionExpression(ECMAScriptParser.FunctionExpressionContext ctx) 
	{ 

		System.out.println("In enterFunctionExpression, Text: " + ctx.getText() + " Start Token Id:" + Integer.toString(ctx.getStart().getTokenIndex()) +
				" End Token Id: " + Integer.toString(ctx.getStop().getTokenIndex()));
	}
	
	private ArgumentExpressionInformation stackTop() 
	{ 
		if (mArgExpressionStack.size() == 0)
			return null;
		else
			return mArgExpressionStack.lastElement(); 
	}
	
	@Override public void enterIdentifierExpression(ECMAScriptParser.IdentifierExpressionContext ctx) 
	{ 
	
		if (stackTop() == null)
			return;
		
		int tokenId = ctx.getStart().getTokenIndex();
		
		GoValidationState state = stackTop().getState();

		System.out.println("\nIn enterIdentifierExpression " + ctx.getText() + " IdentifierExpression start token id = " + Integer.toString(ctx.getStart().getTokenIndex())
		+ " State: " + state.toString());
		
		//!RK No map of lookup tokens , can use a Hashmap for faster lookup
		if (ctx.getText().equals("GoV8") && state == GoValidationState.BASE) {
			
			stackTop().setState(GoValidationState.ACQUIRED_GLOBAL_OBJECT);
			mTokenOverrideHeap.add(new TokenModifier(tokenId,0, GoTokenOverrideType.REPLACE_INCLUSIVE, "return __GoV8;\n"));

		} else if (state == GoValidationState.BASE && ctx.getText().equals("insert")) { 
			
			stackTop().setState(GoValidationState.ACQUIRED_INSERT_METHOD);
			
		}else if (state == GoValidationState.ACQUIRED_GLOBAL_OBJECT && ctx.getText().equals("executeSelect")) {
			
			stackTop().setState(GoValidationState.ACQUIRED_SELECT_METHOD);
			
		} else if (state == GoValidationState.ACQUIRED_GLOBAL_OBJECT && ctx.getText().equals("getObject")) {
			
			stackTop().setState(GoValidationState.ACQUIRED_GET_OBJECT_METHOD);
			
		} else if (state == GoValidationState.ACQUIRED_GLOBAL_OBJECT && ctx.getText().equals("getReferencedObjects")) {
			
			stackTop().setState(GoValidationState.ACQUIRED_GET_OBJECT_REFERENCE_METHOD);
			
		} else if (state ==  GoValidationState.ACQUIRED_SELECT_LITERAL 
				|| state == GoValidationState.ACQUIRED_SELECT_ARGUMENT 
				|| state == GoValidationState.ACQUIRED_SELECT_METHOD) {


			stackTop().addArgument(ctx.getText(), GoTokenType.SELECT_ARGUMENT);
			stackTop().setState(GoValidationState.ACQUIRED_SELECT_ARGUMENT);

		} else if (state == GoValidationState.ACQUIRED_GET_OBJECT_METHOD) {
			
			stackTop().addArgument(ctx.getText(), GoTokenType.OBJECT_ARGUMENT);
			stackTop().setState(GoValidationState.ACQUIRED_GET_OBJECT_ARGUMENT);
			
		} else if (state == GoValidationState.ACQUIRED_INSERT_METHOD) {

			stackTop().addArgument(ctx.getText(), GoTokenType.INSERT_ARGUMENT);
			stackTop().setState(GoValidationState.ACQUIRED_INSERT_ARGUMENT);
			
		} 
	}
	
	
	
	@Override public void enterIdentifierName(ECMAScriptParser.IdentifierNameContext ctx) 
    { 
        System.out.println("\nIn enterIdentifierName text: " + ctx.getText() + " Identifier start token id = " + Integer.toString(ctx.getStart().getTokenIndex()));
        
        if (stackTop() == null)
        	return;
    
    	int tokenId = ctx.getStart().getTokenIndex();
		Token mt = mTerminalTokenMap.get(new Integer(tokenId) - 1);
		int mappedId = 0;
		String mtString = "none";
		if (mt != null) {
			mappedId = mt.getTokenIndex();
			mtString = mt.getText();
		}
		
		System.out.println("Token Ids: original = " + Integer.toString(tokenId) + " Mapped = " + Integer.toString(mappedId) + " Mapped text: " + mtString);
		
		GoValidationState state = stackTop().getState();
		String txt = ctx.getText();
		
        //!RK Replace with a hashmap
    	if (state == GoValidationState.ACQUIRED_GLOBAL_OBJECT && txt.equals("executeSelect")) {
    		
			stackTop().setState(GoValidationState.ACQUIRED_SELECT_METHOD);
			
		} else if (state == GoValidationState.ACQUIRED_GLOBAL_OBJECT && txt.equals("getReferencedObjects")) {
			
			stackTop().setState(GoValidationState.ACQUIRED_GET_OBJECT_REFERENCE_METHOD);
			
			// Insert a security context argument (see below for the 'insert' case)
			mTokenOverrideHeap.add(new TokenModifier(ctx.getStart().getTokenIndex() + 1 , -3, 
					GoTokenOverrideType.INSERT_AFTER, "__GoV8, "));
				
        } else if (state == GoValidationState.ACQUIRED_GLOBAL_OBJECT && txt.equals("getObject")) {
			
			stackTop().setState(GoValidationState.ACQUIRED_GET_OBJECT_METHOD);
			
		} else if (state == GoValidationState.BASE && txt.equals("insert")) {
			

        	// Insert a security context argument: 
			//!RK The +1 is a hack since we make the assumption tokens are sequential, a more robust method would be to add it before the first argument 
			//    by counting the number of arguments in the insert state. 
        	mTokenOverrideHeap.add(new TokenModifier(ctx.getStart().getTokenIndex() + 1 , -3, 
        			GoTokenOverrideType.INSERT_AFTER, "__GoV8, "));
        	
			stackTop().setState(GoValidationState.ACQUIRED_INSERT_METHOD);
		}
    }

	
    @Override public void enterArgumentsExpression(ECMAScriptParser.ArgumentsExpressionContext ctx)
    {
    	System.out.println("\n---In enterArgumentExpression " + ctx.getText() 
 		+ " Start Token: " + Integer.toString(ctx.getStart().getTokenIndex()) 
 		+ " End Token: " + Integer.toString(ctx.getStop().getTokenIndex()));
    	 
        mArgExpressionStack.push(new ArgumentExpressionInformation(ctx , ctx.getStart().getTokenIndex(), ctx.getStop().getTokenIndex()));
    }
    
    private String getStackArgs()
    {
    	int numargs = stackTop().numArg();
    	if (numargs == 0)
    		return "";
    	
    	String args = stackTop().getArg(0);
    	
    	for (int i = 1; i < numargs; i++) {
    		args = args + ", ";
    		args = args + stackTop().getArg(i);
    	}
    	
    	return args;
    }
    
    
    @Override public void exitArgumentsExpression(ECMAScriptParser.ArgumentsExpressionContext ctx)
    {
    	System.out.println("\n---In exitArgumentExpression " 
 		+ " Start Token: " + Integer.toString(ctx.getStart().getTokenIndex()) 
 		+ " End Token: " + Integer.toString(ctx.getStop().getTokenIndex()));
    	 
    	System.out.println("-- exit argument state: " + stackTop().getState().toString());
    	
    	GoValidationState state = stackTop().getState();
    	
    	boolean addedOverride = true;
    	
        if (state == GoValidationState.ACQUIRED_SELECT_ARGUMENT || state == GoValidationState.ACQUIRED_SELECT_LITERAL) {
        	
            // Add validation code
            mTokenOverrideHeap.add(new TokenModifier(stackTop().getStartTokenId(), -2, GoTokenOverrideType.INSERT_BEFORE, "_GO_PRE_VALIDATE_SELECT(__GoV8, " +
            		getStackArgs() + ");\n"));
            
        } else if (state == GoValidationState.ACQUIRED_GET_OBJECT_LITERAL || state == GoValidationState.ACQUIRED_GET_OBJECT_ARGUMENT) {
        	
            mTokenOverrideHeap.add(new TokenModifier(stackTop().getStartTokenId(), -3, GoTokenOverrideType.INSERT_BEFORE, "__GO_PRE_VALIDATE_GET_OBJECT(__GoV8, " 
            		+ getStackArgs() + ");"));
            
        } else if (state == GoValidationState.ACQUIRED_INSERT_ARGUMENT || state == GoValidationState.ACQUIRED_INSERT_LITERAL) {

            mTokenOverrideHeap.add(new TokenModifier(stackTop().getStartTokenId(), -5, GoTokenOverrideType.INSERT_BEFORE, "__GO_PRE_VALIDATE_INSERT(__GoV8, "
            		+ getStackArgs() + ");"));
            
        } else if (state == GoValidationState.ACQUIRED_GET_OBJECT_REFERENCE_METHOD ) {

            mTokenOverrideHeap.add(new TokenModifier(stackTop().getStartTokenId(), -5, GoTokenOverrideType.INSERT_BEFORE, 
            		"__GO_PRE_VALIDATE_GET_REFERENCED_OBJECT(__GoV8);"));
            
        } else  {
        	addedOverride = false;
        }
        

        if (addedOverride) {
        	TokenModifier tmfunc = new TokenModifier(stackTop().getStartTokenId(), -10, GoTokenOverrideType.INSERT_BEFORE, "((function() {");
        	
        	if (!mTokenOverrideHeap.contains(tmfunc)) {
        		mTokenOverrideHeap.add(tmfunc);
        		mTokenOverrideHeap.add(new TokenModifier(stackTop().getStartTokenId(), 10, GoTokenOverrideType.INSERT_AFTER, "})())"));
        	}
        }
        
    	mArgExpressionStack.pop();
    }


	
	
	@Override public void enterLiteral(ECMAScriptParser.LiteralContext ctx)
    {
        System.out.println("\nIn enterLiteral content: " + ctx.getText() + " Literal start token id = " + Integer.toString(ctx.getStart().getTokenIndex()));

        if (stackTop() == null)
            return;

        GoValidationState state = stackTop().getState();
        String txt = ctx.getText();

        if (state == GoValidationState.ACQUIRED_SELECT_METHOD 
                || state == GoValidationState.ACQUIRED_SELECT_ARGUMENT 
                || state == GoValidationState.ACQUIRED_SELECT_LITERAL) 
        {

            stackTop().setState(GoValidationState.ACQUIRED_SELECT_LITERAL);

            if (state == GoValidationState.ACQUIRED_SELECT_METHOD) {
            	int nchars = txt.length();
            	String txtnoquotes = nchars > 0 ? (txt.substring(1, nchars-1)) : "";
            	String newtxt = this.scriptGenerator.processSelectLiteral(txtnoquotes);
                mTokenOverrideHeap.add(new TokenModifier(ctx.getStart().getTokenIndex(), 0, GoTokenOverrideType.REPLACE_INCLUSIVE, newtxt));
            }

            stackTop().addArgument(txt, GoTokenType.SELECT_LITERAL);

        } else if (state == GoValidationState.ACQUIRED_GET_OBJECT_METHOD) {

                stackTop().setState(GoValidationState.ACQUIRED_GET_OBJECT_LITERAL);
                stackTop().addArgument(txt, GoTokenType.OBJECT_LITERAL);

        } else if (state == GoValidationState.ACQUIRED_INSERT_METHOD) {

                // Insert a security context argument:
                mTokenOverrideHeap.add(new TokenModifier(ctx.getStart().getTokenIndex(), -1, GoTokenOverrideType.INSERT_BEFORE, "__GoV8, "));

                stackTop().setState(GoValidationState.ACQUIRED_INSERT_LITERAL);
                stackTop().addArgument(txt, GoTokenType.INSERT_LITERAL);
        }

    }

}
