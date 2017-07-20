package com.ibm.watson;

import java.util.List;
import com.ibm.watson.analyzers.ConceptNameAnalyzer;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String query = "carcinoma bilateral breast";
		List<String> tokens = QueryLucene.createTokens(query, new ConceptNameAnalyzer());
		List<List<String>> nGrams = QueryLucene.createAllNGrams(tokens);
		System.out.println(nGrams);
	}

}