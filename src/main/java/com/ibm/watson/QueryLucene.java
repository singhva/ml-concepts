package com.ibm.watson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class QueryLucene {

	public static void main(String[] args) throws IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer();
		Path baseDir = Paths.get("/Users/singhv/Documents/Data/lucene");
		Path indexDir = baseDir.resolve("index");
		Directory directory = FSDirectory.open(indexDir);
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
	    // Parse a simple query that searches for "text":
	    QueryParser parser = new QueryParser("abstract", analyzer);
	    Query query = parser.parse("EGFR");
	    ScoreDoc[] hits = isearcher.search(query, 5, Sort.RELEVANCE, true, true).scoreDocs;
	    // Iterate through the results:
	    for (int i = 0; i < hits.length; i++) {
	    	ScoreDoc doc = hits[i];
			Document hitDoc = isearcher.doc(hits[i].doc);
			System.out.println(hitDoc.get("abstract"));
			System.out.println(doc.score);
	    }
	    
	    Term term = new Term("TLX");
	    long freq = ireader.totalTermFreq(term);
	    //isearcher.termStatistics(term, context)
	    System.out.println(freq);
	    
	    ireader.close();
	    directory.close();
	}

}