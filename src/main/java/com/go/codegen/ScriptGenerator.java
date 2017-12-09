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

import java.util.List;
import java.util.PriorityQueue;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.biu.parser.js.ECMAScriptLexer;
import com.biu.parser.js.ECMAScriptParser;
import com.biu.parser.sql.PlSqlLexer;
import com.biu.parser.sql.PlSqlParser;

public class ScriptGenerator {

	

	enum GoValidationState {
		BASE,
		ACQUIRED_GLOBAL_OBJECT,
		ACQUIRED_GET_OBJECT_METHOD,
		ACQUIRED_INSERT_METHOD,
		ACQUIRED_GET_OBJECT_REFERENCE_METHOD,
		ACQUIRED_SELECT_METHOD,
		ACQUIRED_SELECT_ARGUMENT,
		ACQUIRED_SELECT_LITERAL,
		ACQUIRED_INSERT_LITERAL,
		ACQUIRED_INSERT_ARGUMENT,
		ACQUIRED_GET_OBJECT_LITERAL,
		ACQUIRED_GET_OBJECT_ARGUMENT
	}
	
	enum GoTokenType {
		V8_OBJECT,
		SELECT_METHOD,
		GET_OBJECT_METHOD,
		INSERT_METHOD,
		SELECT_LITERAL,
		SELECT_ARGUMENT,
		OBJECT_LITERAL,
		OBJECT_ARGUMENT,
		INSERT_ARGUMENT,
		INSERT_LITERAL
	}
	
	enum GoTokenOverrideType {
		INSERT_BEFORE,
		INSERT_AFTER,
		REPLACE_INCLUSIVE
	}
	
	
	private Script mScript;

	
	public ScriptGenerator(UserContext ctx, Script program) 
	{
		mScript = program;
	}
	
	
	public  ScriptGeneratorStatus generateScript(UserContext ctxt, 
			Script ins, Script outs)
	{
		return null;
	}
	
	enum GoSqlValidationState {
		BASE,
		AT_SELECT,
		AT_FROM,
		AT_WHERE
	}
	
	enum GoSqlTokenType {
		COLUMN,
        TABLE,
        EXPRESSION_IDENTIFIER
	}
	
	enum GoSqlTokenOverrideType {
	}
	

	
	private String applyTokenOverrides(List<Token> allTokens, PriorityQueue<TokenModifier> heap)
    {

        String strout = " ";

        TokenModifier tm = heap.poll();

        for (Token t : allTokens) {

            // Extra token information for debugging

            if (tm != null && tm.getTokenIndex() == t.getTokenIndex()) {

                while (tm != null 
                        && tm.getTokenOverrideType() == GoTokenOverrideType.INSERT_BEFORE 
                        && tm.getTokenIndex() == t.getTokenIndex()) {

                    strout += (tm.getText() + " ");
                    tm = heap.poll();
                }
                if (tm != null && tm.getTokenIndex() == t.getTokenIndex() 
                		&& tm.getTokenOverrideType() == GoTokenOverrideType.REPLACE_INCLUSIVE) {
                    strout += (tm.getText() + " ");
                    tm = heap.poll();
                } else {
                    strout += (t.getText() + " ");
                }
                while (tm != null 
                        && tm.getTokenIndex() == t.getTokenIndex() 
                        && tm.getTokenOverrideType() == GoTokenOverrideType.INSERT_AFTER) {

                    strout += (tm.getText() + " ");
                    tm = heap.poll();
                }
            } else {

                strout += (t.getText() + " " );
            }
        }

        return strout;
    }
            

	
    // Modifies a literal with an inline validation function
    //
	String processSelectLiteral(String sqlString)
    {

        CharStream is = CharStreams.fromString(sqlString);
        PlSqlLexer lex = new PlSqlLexer(is);

        TokenStream tokenStream = new CommonTokenStream(lex);

        PlSqlParser parser = new PlSqlParser(tokenStream);
        ScriptGeneratorErrorListener errorListener =  new  ScriptGeneratorErrorListener();

        parser.addErrorListener(errorListener);

        ParseTree tree = parser.sql_statement();


        if (errorListener.errorCount() > 0) {

            System.out.println("**ERRORS FOUND**");
            return "";
        }

        ParseTreeWalker walker = new ParseTreeWalker();
        ScriptSQLListener listener = new ScriptSQLListener();
        walker.walk(listener, tree);
        List<Token> allTokens = listener.getAllTokens();
        PriorityQueue<TokenModifier> heap = listener.tokenOverrideHeap();

        String sqlStringWithOverrides = applyTokenOverrides(allTokens, heap);
        String strout = "((function() {" + listener.getValidateFunctionStatements() + "return \"" + sqlStringWithOverrides + "\";})())";

        return strout;
    }
	
	/*
    private void testSqlParser(String str)
    {
        System.out.println(processSelectLiteral(str));
    }
    */
	
	private void test()
	{
    	CharStream is = CharStreams.fromString(mScript.getCode());
    	ECMAScriptLexer lex = new ECMAScriptLexer(is);
    	
    	TokenStream tokenStream = new CommonTokenStream(lex);
    	
    	ECMAScriptParser parser = new ECMAScriptParser(tokenStream);
    	ScriptGeneratorErrorListener errorListener =  new  ScriptGeneratorErrorListener();

    	parser.addErrorListener(errorListener);
    	
    	ParseTree tree = parser.program();
		

    	
    	
    	if (errorListener.errorCount() > 0) {
    		System.out.println("**ERRORS FOUND**");
    	} else {
    		
    		System.out.println("Parsing...");
    		ParseTreeWalker walker = new ParseTreeWalker();
    		ScriptJSListener listener = new ScriptJSListener(this);
    		walker.walk(listener, tree);
            System.out.println("Done Parsing");

            System.out.println("\nPrinting modified program...");

            List<Token> allTokens = listener.getAllTokens();
            PriorityQueue<TokenModifier> heap = listener.tokenOverrideHeap();
            
            System.out.println(applyTokenOverrides(allTokens, heap));

            System.out.println("Done printing modified program.");
    	}
    	
    	
	}
	
	public static void main( String[] args )
    {
		UserContext uctxt = new UserContext();
		
		Script scr = new Script(uctxt, "");
	    if (args.length > 0)
	        scr.readFromFile(args[0]);
		
		System.out.println(scr.getCode());
		ScriptGenerator testscr = new ScriptGenerator(uctxt, scr);

		testscr.test();
    }
}
