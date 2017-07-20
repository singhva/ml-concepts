package com.ibm.watson;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.ibm.watson.analyzers.ConceptNameAnalyzer;
import com.ibm.watson.analyzers.LowerCaseAnalyzer;

public class AnalyzeThesaurus {

	public static void main(String[] args) throws IOException, ParseException {
		String userHome = System.getProperty("user.home");
		Paths.get(userHome, "Documents", "Curation", "thesaurus");
		Path baseDir = Paths.get("/Users/singhv/Documents/Data/lucene_nci");
		Path indexDir = baseDir.resolve("index");
		
		Directory directory = FSDirectory.open(indexDir);
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		Pattern.compile("(<start> )(.*)( <end>)");

		String queryStr = "moderately differentiated invasive ductal carcinoma";
		List<String> split = QueryLucene.createTokens(queryStr, new LowerCaseAnalyzer());
		System.out.println(String.join("\n", createAllNGrams(split)));
		//List<Document> matches = findNonoverlappingMatches(isearcher, queryStr);
		
		QueryParser parser = new QueryParser("name", new ConceptNameAnalyzer());
		Query query = parser.parse(queryStr);
		System.out.println(query);
		ScoreDoc[] hits = isearcher.search(query, 4000, Sort.RELEVANCE, true, true).scoreDocs;
		
		System.out.println("Matches found: " + hits.length);
		
		for (ScoreDoc hit : hits) {
			Document doc = isearcher.doc(hit.doc);
			List<String> names = new ArrayList<>();
			for (String name : doc.getValues("name_na")) {
				names.add(name.substring(8, name.length() - 6).toLowerCase());
			}
			List<String> nGrams = createAllNGrams(split);
			Set<String> common = new HashSet<>(nGrams);
			if (common.retainAll(new HashSet<>(names)) && (common.size() > 0)) {
				System.out.println( doc.get("code") + ": " + common.iterator().next());
			}
			
			/*
			for (String name : names) {
				Matcher matcher = pattern.matcher(name);
				if (name.toLowerCase().equals("<start> moderately <end>"))
				if (matcher.find())
					System.out.println(String.format("%s (%s)", matcher.group(2), doc.get("code")));	
			}
			*/
		}
		
		
		/*
		for (String subStr : split) {
			Query query = buildQuery(Arrays.asList(subStr));
			ScoreDoc[] hits = isearcher.search(query, 1, Sort.RELEVANCE, true, true).scoreDocs;
			if (hits.length == 1) {
				Document hitDoc = isearcher.doc(hits[0].doc);
				Matcher matcher = pattern.matcher(hitDoc.get("name"));
				if (matcher.find())
					System.out.println(String.format("%s (%s)", matcher.group(2), hitDoc.get("code")));
			}
		}
		*/
		
	    ireader.close();
	    directory.close();
	    
		//iterate(sourceDir.resolve("Thesaurus.txt"));
	}
	
	public static List<String> createAllNGrams(List<String> original) {
		List<String> results = new ArrayList<>();
		
		for (int i = 0; i < original.size() - 1; i++) {
			if (i > 0) {
				results.add(String.join(" ", original.subList(0, i + 1)));
			}
			results.add(String.join(" ", original.subList(i, i + 1)));
			results.add(String.join(" ", original.subList(i + 1, original.size())));
		}
		
		return results;
	}
	
	public static List<Document> findNonoverlappingMatches(IndexSearcher isearcher, String queryStr) throws IOException {
		List<String> split = QueryLucene.createTokens(queryStr, new LowerCaseAnalyzer());
		int i = -1;
		List<String> tempQuery = new ArrayList<>();
		List<Document> matches = new ArrayList<Document>();
		Document previous = null;
		while(++i < split.size()) {
			tempQuery.add(split.get(i));
			Query query = buildQuery(tempQuery);
			System.out.println(query);
			ScoreDoc[] hits = isearcher.search(query, 1, Sort.RELEVANCE, true, true).scoreDocs;
			if (hits.length == 1) {
				previous = isearcher.doc(hits[0].doc);				
				
			}
			else {
				if (previous != null) {
					matches.add(previous);
					tempQuery = new ArrayList<>();
					tempQuery.add(split.get(i));
				}
				previous = null;
			}
		}
		
		if (previous != null) {
			matches.add(previous);
		}
		
		return matches;
	}
	
	public static void iterate(Path thesaurusPath) throws IOException {
		try (BufferedReader br = Files.newBufferedReader(thesaurusPath, Charset.defaultCharset())) {
	        for (String line = null; (line = br.readLine()) != null;) {
	        	line = StringUtils.trim(line);
	        	String[] fields = StringUtils.split(line, "\t");
	            
	            if (!("Retired_Concept").equals(fields[fields.length - 2])) {
		            String[] synonymValues = fields[3].split("\\|");
		            String conceptName = synonymValues[0];
		            
		            if (conceptName.endsWith("al") || conceptName.endsWith("an")) {
		            	System.out.println(conceptName);
		            }
		            
		            if (synonymValues.length > 1) {
		            	for (String syn : Arrays.copyOfRange(synonymValues, 1, synonymValues.length)) {

		            	}
		            }          	
	            }
	           
	        }
	    }
	}
	
	public static Query buildQuery(Iterable<String> tokens) {		  
		PhraseQuery.Builder pBuilder = new PhraseQuery.Builder();
		pBuilder.add(new Term("name_na", "<start>"));
		for (String text : tokens) {
			pBuilder.add(new Term("name_na", text));
		}		    
		
		pBuilder.add(new Term("name_na", "<end>"));
		DisjunctionMaxQuery disMaxQuery = new DisjunctionMaxQuery(Arrays.asList(pBuilder.build()), 2.0f);
		return disMaxQuery;
	}

}