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

import com.go.codegen.ScriptGenerator.GoTokenOverrideType;
import com.go.codegen.TokenModifier;

class TokenModifier  implements Comparable<TokenModifier> 
{
	
	private int mTokenId;
	private int mSubId;
	String      mVal;
	GoTokenOverrideType mType;
	
	// Can replace more than one token if we want to add that logic.
	
	@Override public boolean equals(Object o)
	{
		if (!(o instanceof  TokenModifier))
			return false;
		
		TokenModifier tmo = (TokenModifier) o;
		
		return mTokenId == tmo.mTokenId && mSubId == tmo.mSubId && mVal == tmo.mVal;
	}
	
	@Override public int compareTo(TokenModifier o)
	{
		if (mTokenId < o.mTokenId)
			return -1;
		else if (mTokenId > o.mTokenId)
			return 1;
		else if (mSubId < o.mSubId)
			return -1;
		else if (mSubId > o.mSubId)
			return 1;
		else
			return 0;
	}
	
	TokenModifier(int id, int subId, GoTokenOverrideType type, String value) {
		mTokenId = id;
		mSubId = subId;
		mType = type;
		mVal = value;
	}
	
	String getText() { return mVal; }
	int getTokenIndex() { return mTokenId; }
	GoTokenOverrideType getTokenOverrideType() { return mType; }
}
