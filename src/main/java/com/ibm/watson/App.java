package com.ibm.watson;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.apache.commons.logging.*;

import com.ibm.watson.analyzers.LowerCaseAnalyzer;

/**
 * Hello world!
 *
 */
public class App {
	private static Log log = LogFactory.getLog(App.class);
	
    public static void main( String[] args ) throws IOException {
        List<String> result = new ArrayList<>();
        Analyzer analyzer = new LowerCaseAnalyzer();
        Analyzer analyzer1 = new Analyzer() {
			
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				Tokenizer source = new WhitespaceTokenizer();
				TokenStream filter = new LowerCaseFilter(source);
				filter = new StopFilter(filter, StandardAnalyzer.ENGLISH_STOP_WORDS_SET);
				filter = new SnowballFilter(filter, new EnglishStemmer());
				return new TokenStreamComponents(source, filter);
			}
		};
        String str = "<start> Cancer of the Breast <end>";
        try {
          TokenStream stream  = analyzer.tokenStream(null, new StringReader(str));
          stream.reset();
          while (stream.incrementToken()) {
            result.add(stream.getAttribute(CharTermAttribute.class).toString());
          }
        } catch (IOException e) {
        	analyzer.close();
        	throw new RuntimeException(e);
        }
        analyzer.close();
        System.out.println(result);
        
        
		Path baseDir = Paths.get("/Users/singhv/Documents/Data/lucene_nci");
		Path indexDir = baseDir.resolve("index");
		Directory directory = FSDirectory.open(indexDir);
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		Query q1 = new TermQuery(new Term("code", "C4872"));
		ScoreDoc[] hits = isearcher.search(q1, 30, Sort.RELEVANCE, true, true).scoreDocs;
		Document doc = ireader.document(hits[0].doc);
		
		for (IndexableField field : doc.getFields()) {
			
		}
		
		System.out.println(doc);
		/*
		Terms termVector = ireader.getTermVector(hits[0].doc, "name");
		TermsEnum termsEnum = termVector.iterator();
		BytesRef ref;
		while ((ref = termsEnum.next()) != null) {
			System.out.println(ref.utf8ToString() + ": " + ref.offset);
		}
		*/
	    Term term = new Term("name", "breast");
	    long freq = ireader.docFreq(term);
	    
	    System.out.println(freq);
	    
    }
}
