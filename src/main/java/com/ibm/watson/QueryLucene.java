package com.ibm.watson;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
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

public class QueryLucene {
	private DirectoryReader ireader;
	private Directory directory;
	private IndexSearcher isearcher;
	private static Log log = LogFactory.getLog(QueryLucene.class);
	//private static Logger log = Logger.getLogger(QueryLucene.class);
	
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
		PropertyConfigurator.configure(QueryLucene.class.getResourceAsStream("/log4j.properties"));
		String baseDir = "/Users/singhv/Documents/Data/lucene_ncim";
		QueryLucene queryLucene = new QueryLucene(baseDir);
		//QueryParser parser = new QueryParser("name", new LowerCaseAnalyzer());
	    String queryStr =  "adenocarcinoma lung"; // "moderately differentiated invasive lobular carcinoma";
	    String[] types = EntityType.getSemanticTypes("Anatomy_Specific_Entity");
	    System.out.println(String.join("\n", types));
	    System.out.println(SemanticType.get("neop"));
	    List<DocumentAndScore> results = queryLucene.doQuery(queryStr, EntityType.getSemanticTypes("Cancer_Entity"));
	    for (DocumentAndScore docAndScore : results) {
	    	Document doc = docAndScore.getDocument();
	    	log.info(doc.get("code") + ") " + doc.get("name") + ": " + doc.get("type") + " (" + docAndScore.getScore() + ")");
	    }
	    
	    //Term term = new Term("TLX");
	    //long freq = ireader.totalTermFreq(term);
	    //isearcher.termStatistics(term, context)
	    //System.out.println(freq);
	    
		//scanner.close();
	    //ireader.close();
	    //directory.close();
	    queryLucene.finish();
	}
	
	/**
	 * The basic method which queries lucene. It first looks for exact match and if not found looks for approximate
	 * match scored by Metamap's algorithm
	 * @param queryStr
	 * @param semanticType
	 * @return
	 */
	public List<DocumentAndScore> doQuery(String queryStr, String... semanticType) {
		Query query = buildPhraseQuery(queryStr, semanticType);
		log.debug(query);
		List<DocumentAndScore> result = new ArrayList<>();
		try {
			ScoreDoc[] hits = isearcher.search(query, 1, Sort.RELEVANCE, true, true).scoreDocs;
			if (hits.length > 0) {
				Document hitDoc = isearcher.doc(hits[0].doc);
				result.add(new DocumentAndScore(hitDoc, 1.0f));
			}
			else {
				query = buildTermQuery(queryStr, semanticType);
				log.debug(query);
				hits = isearcher.search(query, 10000, Sort.RELEVANCE, true, true).scoreDocs;
				
				log.info("Matches found: " + hits.length);
				Analyzer analyzer = new ConceptNameAnalyzer();
				List<String> queryStrTokens = createTokens(queryStr, analyzer);
				List<List<String>> queryStrNGrams = createAllNGrams(queryStrTokens);
				Map<Integer, Float> maxDocScores = new HashMap<Integer, Float>();
				
				for (int i = 0; i < hits.length; i++) {
					ScoreDoc hit = hits[i];
					Document doc = isearcher.doc(hit.doc);
					List<Float> scores = new ArrayList<>();
					for (String name : doc.getValues("name_na")) {
						name = name.substring(8, name.length() - 6).toLowerCase();
						//names.add(name);
						List<String> candidateStrTokens = createTokens(name, new ConceptNameAnalyzer());
						if (candidateStrTokens.size() > 0) {
							List<List<String>> candidateStrNGrams = createAllNGrams(candidateStrTokens);
							Set<String> commonTokens = new HashSet<>(candidateStrTokens);
							Set<List<String>> commonNGrams = new HashSet<>(candidateStrNGrams);
							commonTokens.retainAll(queryStrTokens);
							commonNGrams.retainAll(queryStrNGrams);
							if ((commonTokens.size() > 0)) {
								float coverageScore = (2.0f * commonTokens.size() / (float) candidateStrTokens.size() + 
														commonTokens.size() / (float) queryStrTokens.size()) / 3.0f;
								List<String> biggestNGram = Collections.max(commonNGrams, (a, b) -> new Integer(a.size()).compareTo(new Integer(b.size())));
								float cohesiveScore = ((2.0f * (float) Math.pow(biggestNGram.size() / (float) candidateStrTokens.size(), 2.0) +
														(float) Math.pow(biggestNGram.size() / (float) queryStrTokens.size(), 2.0))) / 3.0f;
								float finalScore = (2.0f * coverageScore + cohesiveScore) / 3.0f;
								scores.add( coverageScore );
							}	
						}
					}
					float maxScore = Collections.max(scores);
					maxDocScores.put(hit.doc, maxScore);
					result.add(new DocumentAndScore(doc, maxScore));
				}
				result.sort( (a, b) -> new Float(b.getScore()).compareTo(new Float(a.getScore())));
			}
		} catch (IOException e) {
			log.error("Error querying index: " + e);
		}
		
		return result.subList(0, Math.min(10, result.size()));
	}
	
	/**
	 * This method creates all NGrams of a tokenized string. It also add one additional nGram by combining the
	 * last word and the first word of the string as if rotating a string
	 * @param original
	 * @return
	 */
	public static List<List<String>> createAllNGrams(List<String> original) {
		List<List<String>> results = new ArrayList<>();
		results.add(original);
		
		for (int i = 0; i < original.size() - 1; i++) {
			if (i > 0) {
				results.add(original.subList(0, i + 1));
			}
			results.add(original.subList(i, i + 1));
			results.add(original.subList(i + 1, original.size()));
		}
		results.add(Arrays.asList(original.get(original.size() - 1), original.get(0)));
		
		return results;
	}
	
	public Query buildPhraseQuery(String queryStr, String... semanticType) {		  
		QueryBuilder builder = new QueryBuilder(new LowerCaseAnalyzer());
		Query query = builder.createPhraseQuery("name_na", "<start> " + queryStr + " <end>");
		if (semanticType.length > 0) {
			BooleanQuery.Builder typeQueryBuilder = new BooleanQuery.Builder();
			typeQueryBuilder.setMinimumNumberShouldMatch(1);
			for (String type : semanticType) {
				if (!type.equals("unknown"))
				typeQueryBuilder.add(new TermQuery(new Term("type", SemanticType.get(type))), Occur.SHOULD);
			}
			query = new BooleanQuery.Builder()
					.add(query, Occur.MUST)
					.add(typeQueryBuilder.build(), Occur.MUST)
					//.add(new TermQuery(new Term("type", semanticType[0])), Occur.MUST)
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
			BooleanQuery.Builder typeQueryBuilder = new BooleanQuery.Builder();
			typeQueryBuilder.setMinimumNumberShouldMatch(1);
			for (String type : semanticType) {
				if (!type.equals("unknown"))
				typeQueryBuilder.add(new TermQuery(new Term("type", SemanticType.get(type))), Occur.SHOULD);
			}
			query = new BooleanQuery.Builder()
					.add(query, Occur.MUST)
					.add(typeQueryBuilder.build(), Occur.MUST)
					//.add(new TermQuery(new Term("type", semanticType[0])), Occur.MUST)
					.build();
		}
		return query;
	}
	
	/**
	 * This method creates tokens by utilizing the provided analyzer
	 * @param str
	 * @param analyzer
	 * @return
	 */
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
	/*
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
	*/
}