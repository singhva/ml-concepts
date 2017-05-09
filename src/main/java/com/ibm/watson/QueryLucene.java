package com.ibm.watson;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.ibm.watson.analyzers.ConceptNameAnalyzer;
import com.ibm.watson.analyzers.LowerCaseAnalyzer;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class QueryLucene {
	private DirectoryReader ireader;
	private Directory directory;
	private IndexSearcher isearcher;
	private static Log log = LogFactory.getLog(QueryLucene.class);
	
	public QueryLucene(String lucene_dir_name) throws IOException {
		Path baseDir = Paths.get(lucene_dir_name);
		Path indexDir = baseDir.resolve("index");
		directory = FSDirectory.open(indexDir);
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
	}
	
	public void finish() {
		try {
			directory.close();
			ireader.close();
		} catch(IOException ex) {
			log.error("Error encountered closing lucene index");
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		//Analyzer conceptNameAnalyzer = new ConceptNameAnalyzer();
		String baseDir = "/Users/singhv/Documents/Data/lucene_nci";
		QueryLucene queryLucene = new QueryLucene(baseDir);
		//QueryParser parser = new QueryParser("name", new LowerCaseAnalyzer());
	    String queryStr =  "moderately differentiated invasive lobular carcinoma"; // "moderately differentiated invasive lobular carcinoma";
	    //Query query = buildPhraseQuery(str);
	    //System.out.println(query);
	    List<DocumentAndScore> results = queryLucene.doQuery(queryStr, "Neoplastic Process");
	    for (DocumentAndScore docAndScore : results) {
	    	Document doc = docAndScore.getDocument();
	    	System.out.println(doc.get("code") + ") " + doc.get("name") + ": " + doc.get("type") + " (" + docAndScore.getScore() + ")");
	    }
	    
	    //test(str);
	    
	    //Query q1 = new TermQuery(new Term("code", "C8932"));
	    
	    //isearcher.setSimilarity(new BM25Similarity(1.2f, 1.0f));
		
	    /*
	    ScoreDoc[] hits = isearcher.search(query, 50, Sort.RELEVANCE, true, true).scoreDocs;
	    //Explanation expl = isearcher.explain(query, hits[0].doc);
	    //System.out.println(expl.toString());
	    //expl = isearcher.explain(query, hits[1].doc);
	    //System.out.println(expl.toString());
	    // Iterate through the results:
	    Set<String> semTypes = new HashSet<>();
	    for (int i = 0; i < hits.length; i++) {
	    	ScoreDoc doc = hits[i];
			Document hitDoc = isearcher.doc(hits[i].doc);
			semTypes.add(hitDoc.get("type"));
			System.out.println(i + ") " + hitDoc.get("name") + ": " + doc.score + ": " + hitDoc.get("type"));
	    }
	    
	    System.out.println("Semantic Types: " + String.join(", ", semTypes));
	    */
	    
	    
	    //Term term = new Term("TLX");
	    //long freq = ireader.totalTermFreq(term);
	    //isearcher.termStatistics(term, context)
	    //System.out.println(freq);
	    
		//scanner.close();
	    //ireader.close();
	    //directory.close();
	    queryLucene.finish();
	}
	
	public List<DocumentAndScore> doQuery(String queryStr, String... semanticType) {
		Query query = buildPhraseQuery(queryStr, semanticType);
		System.out.println(query);
		List<DocumentAndScore> result = new ArrayList<>();
		try {
			ScoreDoc[] hits = isearcher.search(query, 1, Sort.RELEVANCE, true, true).scoreDocs;
			if (hits.length > 0) {
				Document hitDoc = isearcher.doc(hits[0].doc);
				//System.out.println(hitDoc);
				//String name = hitDoc.get("name");
				//name = name.substring(8, name.length() - 6).toLowerCase();
				//System.out.println(hitDoc.get("code") + ") " + name + ": " + hitDoc.get("type"));
				result.add(new DocumentAndScore(hitDoc, 1.0f));
			}
			else {
				//QueryParser parser = new QueryParser("name", new ConceptNameAnalyzer());
				//query = parser.parse(queryStr);
				query = buildTermQuery(queryStr, semanticType);
				System.out.println(query);
				hits = isearcher.search(query, 10000, Sort.RELEVANCE, true, true).scoreDocs;
				
				log.info("Matches found: " + hits.length);
				Analyzer analyzer = new ConceptNameAnalyzer();
				List<String> queryStrTokens = createTokens(queryStr, analyzer);
				List<List<String>> queryStrNGrams = createAllNGrams(queryStrTokens);
				Map<Integer, Float> maxDocScores = new HashMap<Integer, Float>();
				
				for (int i = 0; i < hits.length; i++) {
					ScoreDoc hit = hits[i];
					Document doc = isearcher.doc(hit.doc);
					//if (i == 0) result.add(doc);
					//List<String> names = new ArrayList<>();
					List<Float> scores = new ArrayList<>();
					for (String name : doc.getValues("name_na")) {
						name = name.substring(8, name.length() - 6).toLowerCase();
						//names.add(name);
						List<String> candidateStrTokens = createTokens(name, new ConceptNameAnalyzer());
						List<List<String>> candidateStrNGrams = createAllNGrams(candidateStrTokens);
						Set<String> commonTokens = new HashSet<>(candidateStrTokens);
						Set<List<String>> commonNGrams = new HashSet<>(candidateStrNGrams);
						commonTokens.retainAll(queryStrTokens);
						commonNGrams.retainAll(queryStrNGrams);
						if ((commonTokens.size() > 0)) {
							float coverageScore = ((2.0f * commonTokens.size() / (float) candidateStrTokens.size() + 
													commonTokens.size() / (float) queryStrTokens.size()) / (float) queryStrTokens.size()) / 3.0f;
							List<String> biggestNGram = Collections.max(commonNGrams, (a, b) -> new Integer(a.size()).compareTo(new Integer(b.size())));
							float cohesiveScore = ((2.0f * (float) Math.pow(biggestNGram.size() / (float) candidateStrTokens.size(), 2.0) +
													(float) Math.pow(biggestNGram.size() / (float) queryStrTokens.size(), 2.0))) / 3.0f;
							float finalScore = (coverageScore + cohesiveScore) / 2.0f;
							scores.add( finalScore );
						}
					}
					float maxScore = Collections.max(scores);
					maxDocScores.put(hit.doc, maxScore);
					result.add(new DocumentAndScore(doc, maxScore));
					
					/*Set<String> common = new HashSet<>(nGrams);
					if (common.retainAll(new HashSet<>(names)) && (common.size() > 0)) {
						String matched = common.iterator().next();
						float score = createTokens(matched, new LowerCaseAnalyzer()).size() / (float) split.size(); 
						result.add(doc);
					}
					*/
				}
				result.sort( (a, b) -> new Float(b.getScore()).compareTo(new Float(a.getScore())));
				/*
				List<Entry<Integer, Float>> entrySet = new ArrayList<>(maxDocScores.entrySet());
				entrySet.sort((a, b) -> b.getValue().compareTo(a.getValue()));
				for (Entry<Integer, Float> entry : entrySet) {
					result.add(isearcher.doc(entry.getKey()));
				}
				*/
			}
		} catch (IOException e) {
			log.error("Error querying index: " + e);
		}
		
		return result.subList(0, Math.min(10, result.size()));
	}
	
	public static List<List<String>> createAllNGrams(List<String> original) {
		List<List<String>> results = new ArrayList<>();
		//results.add(String.join(" ", original));
		results.add(original);
		
		for (int i = 0; i < original.size() - 1; i++) {
			if (i > 0) {
				//results.add(String.join(" ", original.subList(0, i + 1)));
				results.add(original.subList(0, i + 1));
			}
			//results.add(String.join(" ", original.subList(i, i + 1)));
			results.add(original.subList(i, i + 1));
			//results.add(String.join(" ", original.subList(i + 1, original.size())));
			results.add(original.subList(i + 1, original.size()));
		}
		
		return results;
	}
	
	public Query buildPhraseQuery(String queryStr, String... semanticType) {		  
		QueryBuilder builder = new QueryBuilder(new LowerCaseAnalyzer());
		Query query = builder.createPhraseQuery("name_na", "<start> " + queryStr + " <end>");
		if (semanticType.length > 0) {
			query = new BooleanQuery.Builder()
					.add(query, Occur.MUST)
					.add(new TermQuery(new Term("type", semanticType[0])), Occur.MUST)
					.build();
		}
		
		//pBuilder.add(new Term("name_na", "<end>"));
		//DisjunctionMaxQuery disMaxQuery = new DisjunctionMaxQuery(Arrays.asList(pBuilder.build()), 2.0f);
		return query;
	}
	
	public Query buildTermQuery(String queryStr, String... semanticType) {
		QueryBuilder builder = new QueryBuilder(new ConceptNameAnalyzer());
		Query query = builder.createBooleanQuery("name", queryStr);
		if (semanticType.length > 0) {
			query = new BooleanQuery.Builder()
					.add(query, Occur.MUST)
					.add(new TermQuery(new Term("type", semanticType[0])), Occur.MUST)
					.build();
		}
		return query;
	}
	
	public static List<String> createTokens(String str, Analyzer analyzer) {
		List<String> result = new ArrayList<>();
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
		return result;
	}
	
	public static void test(String text) {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation document = new Annotation(text);
		pipeline.annotate(document);		
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		List<CoreLabel> tokens = sentences.get(0).get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String word = token.get(TextAnnotation.class);
			String pos = token.get(PartOfSpeechAnnotation.class);
			System.out.println("\tWord: " + word + ", POS: " + pos);
		}
	}

}