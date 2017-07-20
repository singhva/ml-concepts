package com.ibm.watson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.ibm.watson.analyzers.ConceptNameAnalyzer;
import com.ibm.watson.analyzers.LowerCaseAnalyzer;


public class Ingest {

	public static void main(String[] args) throws IOException {
		Logger.getLogger("org").setLevel(Level.ERROR);
		Ingest ingest = new Ingest();
		ingest.process();
	}
	
	public void process() throws IOException {
		Path path = Paths.get("/Users/singhv/Desktop/Concept_Detection/datasheet_for_CUI_code_concept_detection.csv");
		SparkSession spark = SparkSession.builder().master("local").getOrCreate();
		Dataset<Row> df = spark.read().format("csv").option("header", "true").option("nullValue", "NA").csv(path.toString());
		System.out.println("Columns: " + Arrays.asList(df.columns()));
		
		//Row row1 = df.filter("CUI == 'C0000658'").first();
		//String code = row1.getAs("STR");
		//System.out.println(code);
		
		IndexWriter iwriter = getIndexWriter();
		
		int count = 0;
		Iterator<Row> it = df.toLocalIterator();

		while(it.hasNext()) {
			Row row = it.next();
			String cui = row.getAs("CUI");
			String nciCodes = StringUtils.stripToEmpty(row.getAs("NCI_CODES"));
			String nciCode = nciCodes.contains("|") ? nciCodes.split("|")[0] : nciCodes;
			String pCui = StringUtils.stripToEmpty(row.getAs("PCUI"));
			String pNciCodes = StringUtils.stripToEmpty(row.getAs("P_NCI_CODES"));
			String conceptName = row.getAs("STR");
			String source = row.getAs("SAB");
			String type = row.getAs("STY");
			String synonyms = row.getAs("SYNS");
			
			Document doc = new Document();
			doc.add(new StringField("cui", cui, Store.YES));
			doc.add(new StringField("code", nciCode, Store.YES));
			doc.add(new StringField("parentCode", pNciCodes, Store.YES));
			doc.add(new StringField("parentCui", pCui, Store.YES));
			doc.add(new StringField("source", source, Store.YES));
			doc.add(new StringField("type", type, Store.YES));
			
			for (String name : new HashSet<String>(Arrays.asList((conceptName + "|" + synonyms).split("\\|")))) {
				doc.add(new TextField("name_na", "<start> " + name.trim() + " <end>", Store.YES));
				doc.add(new TextField("name", name.trim(), Store.YES));	
			}
			
            iwriter.addDocument(doc);
            if (count++ % 1000 == 0) {
            	System.out.println("Processed: " + count + " concepts");
            }
		};
		
		iwriter.close();
	}
	
	public Analyzer getAnalyzer() {
		Map<String, Analyzer> analyzerMap = new HashMap<>();
		analyzerMap.put("name", new ConceptNameAnalyzer());
		analyzerMap.put("name_na", new LowerCaseAnalyzer());
		analyzerMap.put("definition", new StandardAnalyzer());
		
		PerFieldAnalyzerWrapper  analyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(), analyzerMap);
		return analyzer;
	}
	
	public IndexWriter getIndexWriter() throws IOException {
		String userHome = System.getProperty("user.home");
		Path baseDir = Paths.get(userHome, "Documents", "Data", "lucene_ncim");
		if (!Files.exists(baseDir)) Files.createDirectory(baseDir);
		
		Path indexDir = baseDir.resolve("index");
		if (!Files.exists(indexDir)) Files.createDirectory(indexDir);

	    Directory directory = FSDirectory.open(indexDir);
	    IndexWriterConfig config = new IndexWriterConfig(getAnalyzer());
	    IndexWriter iwriter = new IndexWriter(directory, config);
	    return iwriter;
	}

}