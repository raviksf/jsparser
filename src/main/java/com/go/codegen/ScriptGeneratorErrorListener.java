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

import java.util.BitSet;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

class ScriptGeneratorErrorListener extends BaseErrorListener 
{

	public ScriptGeneratorErrorListener() {}

	int mErrorCount = 0;

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
			Object offendingSymbol,
			int line,
			int charPositionInLine,
			String msg,
			RecognitionException e)
	{
		System.out.println("*ERROR* Syntax Error: " + msg);
		mErrorCount++;
	}

	@Override
	public void reportAmbiguity(Parser recognizer,
			DFA dfa,
			int startIndex,
			int stopIndex,
			boolean exact,
			BitSet ambigAlts,
			ATNConfigSet configs)
	{
		// System.out.println("*ERROR* reportAmbiguityError");
		// mErrorCount++;
	}

	@Override
	public void reportAttemptingFullContext(Parser recognizer,
			DFA dfa,
			int startIndex,
			int stopIndex,
			BitSet conflictingAlts,
			ATNConfigSet configs)
	{
		// System.out.println("*ERROR* reportAttemptingFullContext");
		// mErrorCount++;
	}

	@Override
	public void reportContextSensitivity(Parser recognizer,
			DFA dfa,
			int startIndex,
			int stopIndex,
			int prediction,
			ATNConfigSet configs)
	{
		// System.out.println("*ERROR* reportContextSensitivity");
		// mErrorCount++;
	}

	int errorCount() { return mErrorCount; }

}
