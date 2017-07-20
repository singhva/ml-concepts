package com.ibm.watson;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.ibm.watson.analyzers.ConceptNameAnalyzer;
import com.ibm.watson.analyzers.LowerCaseAnalyzer;

public class IngestNciThesaurus {

	public static void main(String[] args) throws IOException {
		String userHome = System.getProperty("user.home");
		
		Map<String, Analyzer> analyzerMap = new HashMap<>();
		analyzerMap.put("name", new ConceptNameAnalyzer());
		analyzerMap.put("name_na", new LowerCaseAnalyzer());
		analyzerMap.put("code", new KeywordAnalyzer());
		analyzerMap.put("parentCode", new KeywordAnalyzer());
		analyzerMap.put("definition", new StandardAnalyzer());
		//analyzerMap.put("synonym", new ConceptNameAnalyzer());
		//analyzerMap.put("synonym_na", new KeywordAnalyzer());
		
		PerFieldAnalyzerWrapper  analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerMap);
		
		Path baseDir = Paths.get(userHome, "Documents", "Data", "lucene_nci");
		if (!Files.exists(baseDir)) Files.createDirectory(baseDir);
		
		Path indexDir = baseDir.resolve("index");
		if (!Files.exists(indexDir)) Files.createDirectory(indexDir);
		
		Path sourceDir = Paths.get(userHome, "Documents", "Curation", "thesaurus");

	    Directory directory = FSDirectory.open(indexDir);
	    IndexWriterConfig config = new IndexWriterConfig(analyzer);
	    IndexWriter iwriter = new IndexWriter(directory, config);
	    
        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

	    int count = 0;
	    try (BufferedReader br = Files.newBufferedReader(sourceDir.resolve("Thesaurus.txt"), Charset.defaultCharset())) {
	        for (String line = null; (line = br.readLine()) != null;) {
	        	line = StringUtils.trim(line);
	        	String[] fields = StringUtils.split(line, "\t");
	            
	            if (line.indexOf("Retired_Concept") == -1) {
		            String nciCode = fields[0];
		            String parentCode = fields[2];
		            String[] synonymValues = fields[3].split("\\|");
		            String conceptName = synonymValues[0];
		            String conceptNameMod = "<start> " + conceptName + " <end>";
		            //String definition = fields[4];
		            if (nciCode.equals("C7688")) {
		            	System.out.println(nciCode);
		            }
		            String semType = fields[fields.length - 1];
		            
		            Document doc = new Document();		            
		            doc.add(new StringField("code", nciCode, Store.YES));
		            doc.add(new TextField("name", conceptName, Store.YES));
		           // doc.add(new Field("name", conceptName, type));
		            doc.add(new TextField("name_na", conceptNameMod, Store.YES));
		            doc.add(new StringField("parentCode", parentCode, Store.YES));
		            //doc.add(new TextField("definition", definition, Store.YES));
		            doc.add(new StringField("type", semType, Store.YES));
		            
		            if (synonymValues.length > 1) {
		            	for (String syn : Arrays.copyOfRange(synonymValues, 1, synonymValues.length)) {
		            		String synMod = "<start> " + syn + " <end>";
		            		//doc.add(new Field("name", syn, type));
		            		doc.add(new TextField("name", syn, Store.YES));
		            		doc.add(new TextField("name_na", synMod, Store.YES));
		            		//doc.add(new TextField("synonym_na", syn.toLowerCase(), Store.NO));
		            	}
		            }
		            
		            iwriter.addDocument(doc);
		            if (count % 1000 == 0) {
		            	System.out.println("Processed: " + count + " concepts");
		            }
		            count++;	            	
	            }
	           
	        }
	    }
	    
	   
	    iwriter.close();

	}

}