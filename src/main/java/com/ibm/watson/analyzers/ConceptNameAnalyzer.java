package com.ibm.watson.analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharTokenizer;

public class ConceptNameAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer source = CharTokenizer.fromTokenCharPredicate(Character::isLetterOrDigit); //new LetterTokenizer(); // new WhitespaceTokenizer();
		TokenStream filter = new LowerCaseFilter(source);
		filter = new StopFilter(filter, StandardAnalyzer.ENGLISH_STOP_WORDS_SET);
		filter = new PorterStemFilter(filter);
		return new TokenStreamComponents(source, filter);
	}
	
	@Override
	public int getPositionIncrementGap(String fieldName) {
	    return 10;
	}

}