package com.ibm.watson.analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class LowerCaseAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer source = new WhitespaceTokenizer();
		TokenStream filter = new LowerCaseFilter(source);
		filter = new StopFilter(filter, StandardAnalyzer.ENGLISH_STOP_WORDS_SET);
		return new TokenStreamComponents(source, filter);
	}
	
	@Override
	public int getPositionIncrementGap(String fieldName) {
	    return 10;
	}

}